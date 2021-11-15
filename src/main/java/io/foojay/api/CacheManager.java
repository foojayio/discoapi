/*
 * Copyright (c) 2021.
 *
 * This file is part of DiscoAPI.
 *
 *     DiscoAPI is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     DiscoAPI is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import io.foojay.api.mqtt.MqttEvt;
import io.foojay.api.mqtt.MqttEvtObserver;
import io.foojay.api.mqtt.MqttManager;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import io.foojay.api.util.JsonCache;
import io.foojay.api.util.OutputFormat;
import io.foojay.api.util.PkgCache;
import io.foojay.api.util.State;
import io.foojay.api.util.UpdateState;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;



@Requires(notEnv = Environment.TEST) // Don't run in tests
public enum CacheManager {
    INSTANCE;

    private static final Logger                       LOGGER                      = LoggerFactory.getLogger(CacheManager.class);
    public final         MqttManager                  mqttManager                 = new MqttManager();
    public final         MqttEvtObserver              mqttEvtObserver             = evt -> handleMqttEvt(evt);
    public final         PkgCache<String, Pkg>        pkgCache                    = new PkgCache<>();
    public final         JsonCache<String, String>    jsonCacheV2                 = new JsonCache<>();
    public final         Map<Integer, Boolean>        maintainedMajorVersions     = new ConcurrentHashMap<>() {{
        put(1, false);
        put(2, false);
        put(3, false);
        put(4, false);
        put(5, false);
        put(6, false);
        put(7, true);
        put(8, true);
        put(9, false);
        put(10, false);
        put(11, true);
        put(12, false);
        put(13, true);
        put(14, false);
        put(15, true);
        put(16, true);
        put(17, true);
        put(18, true);
    }};
    public final         AtomicBoolean                syncWithDatabaseInProgress  = new AtomicBoolean(false);
    public final         AtomicLong                   msToFillCacheWithPkgsFromDB = new AtomicLong(-1);
    public final         AtomicLong                   numberOfPackages            = new AtomicLong(-1);
    public final         AtomicReference<Instant>     lastSync                    = new AtomicReference<>(Instant.MIN);
    public final         AtomicReference<UpdateState> updateState                 = new AtomicReference<>(UpdateState.NOMINAL);
    private final        List<MajorVersion>           majorVersions               = new LinkedList<>();


    CacheManager() {
        mqttManager.subscribe(Constants.MQTT_PKG_UPDATE_TOPIC, MqttQos.EXACTLY_ONCE);
        mqttManager.subscribe(Constants.MQTT_EPHEMERAL_ID_UPDATE_TOPIC, MqttQos.EXACTLY_ONCE);
        mqttManager.addMqttObserver(mqttEvtObserver);
        maintainedMajorVersions.entrySet().forEach(entry-> majorVersions.add(new MajorVersion(entry.getKey(), Helper.getTermOfSupport(entry.getKey()), entry.getValue())));
    }


    public void updateMajorVersions() {
        StateManager.INSTANCE.setState(State.UPDATING, "Updating major versions");
        List<MajorVersion> majorVersionsFromDb = MongoDbManager.INSTANCE.getMajorVersions();
        if (null == majorVersionsFromDb || majorVersionsFromDb.isEmpty()) {
            LOGGER.error("Error updating major versions from mongodb");
        } else {
        majorVersions.clear();
            majorVersions.addAll(majorVersionsFromDb);
            majorVersions.forEach(majorVersion -> maintainedMajorVersions.put(majorVersion.getAsInt(), majorVersion.isMaintained()));
            LOGGER.debug("Successfully updated major versions");
        }
    }

    public void updateJsonCacheV2() {
        StateManager.INSTANCE.setState(State.UPDATING, "Updating Json Cache V2");
        pkgCache.getEntrySet().stream().forEach(entry -> jsonCacheV2.put(entry.getKey(), entry.getValue().toString(OutputFormat.REDUCED_COMPRESSED, Constants.API_VERSION_V2)));
        final List<String> keysToRemove = jsonCacheV2.getEntrySet().parallelStream().filter(entry -> !pkgCache.containsKey(entry.getKey())).map(entry -> entry.getKey()).collect(Collectors.toList());
        jsonCacheV2.remove(keysToRemove);
    }

    public List<MajorVersion> getMajorVersions() {
        if (majorVersions.isEmpty()) { updateMajorVersions(); }
        return majorVersions;
    }

    public void syncCacheWithDatabase() {
        if (syncWithDatabaseInProgress.get()) { return; }

        syncWithDatabaseInProgress.set(true);
        StateManager.INSTANCE.setState(State.SYNCRONIZING, "Synchronizing cache with db");

        final long startSyncronizingCache = System.currentTimeMillis();
        LOGGER.debug("Get last updates per distro from mongodb");
        Map<Distro, Instant> lastUpdates = MongoDbManager.INSTANCE.getLastUpdatesForDistros();
        Distro.getAsListWithoutNoneAndNotFound().forEach(distro -> distro.lastUpdate.set(lastUpdates.get(distro)));

        LOGGER.debug("Fill cache with packages from mongodb");
        final long      startRetrievingPkgFromMongodb = System.currentTimeMillis();
        final List<Pkg> pkgsFromMongoDb               = MongoDbManager.INSTANCE.getPkgs();
        LOGGER.debug("Got all pkgs from mongodb in {} ms", (System.currentTimeMillis() - startRetrievingPkgFromMongodb));

        Map<String, Pkg> patch = pkgsFromMongoDb.parallelStream().collect(Collectors.toMap(Pkg::getId, pkg -> pkg));
        pkgCache.setAll(patch);

        numberOfPackages.set(pkgCache.size());
        msToFillCacheWithPkgsFromDB.set(System.currentTimeMillis() - startSyncronizingCache);

        // Update all available major versions and maintained major versions
        updateMajorVersions();

        lastSync.set(Instant.now());
        syncWithDatabaseInProgress.set(false);
        }


    // ******************** MQTT Message handling *****************************
    public void handleMqttEvt(final MqttEvt evt) {
        final String topic = evt.getTopic();
        final String msg   = evt.getMsg();

        if (topic.equals(Constants.MQTT_PKG_UPDATE_TOPIC)) {
            switch(msg) {
                case Constants.MQTT_PKG_UPDATE_FINISHED_EMPTY_MSG -> {
                    updateState.set(UpdateState.NOMINAL);
                    if (!pkgCache.isEmpty()) { return; }
                        try {
                        LOGGER.debug("PkgCache is empty -> syncCacheWithDatabase(). MQTT event: {}", evt);

                            // Update cache with pkgs from mongodb
                            syncCacheWithDatabase();

                            // Update json cache
                            updateJsonCacheV2();
                        } catch (Exception e) {
                            syncWithDatabaseInProgress.set(false);
                        }
                    }
                case Constants.MQTT_PKG_UPDATE_FINISHED_MSG -> {
                    updateState.set(UpdateState.NOMINAL);
                    try {
                        LOGGER.debug("Database updated -> syncCacheWithDatabase(). MQTT event: {}", evt);

                        // Update cache with pkgs from mongodb
                        syncCacheWithDatabase();

                        // Update json cache
                        updateJsonCacheV2();
                    } catch (Exception e) {
                        syncWithDatabaseInProgress.set(false);
                    }
                }
                case Constants.MQTT_FORCE_PKG_UPDATE_MSG -> {
                    updateState.set(UpdateState.NOMINAL);
                    try {
                        LOGGER.debug("Force pkg update -> syncCacheWithDatabase(). MQTT event: {}", evt);

                        // Update cache with pkgs from mongodb
                        syncCacheWithDatabase();

                        // Update json cache
                        updateJsonCacheV2();
                    } catch (Exception e) {
                        syncWithDatabaseInProgress.set(false);
                    }
                }
            }
        }
    }
}
