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
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.scopes.BuildScope;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import io.foojay.api.util.PkgCache;
import io.foojay.api.util.State;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.foojay.api.util.Constants.API_VERSION_V1;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_OPEN;


@Requires(notEnv = Environment.TEST) // Don't run in tests
public enum CacheManager {
    INSTANCE;

    private static final Logger                           LOGGER                                    = LoggerFactory.getLogger(CacheManager.class);
    public  final        MqttManager                      mqttManager                               = new MqttManager();
    public  final        MqttEvtObserver                  mqttEvtObserver                           = evt -> handleMqttEvt(evt);
    public  final        PkgCache<String, Pkg>            pkgCache                                  = new PkgCache<>();
    public  final        Map<Integer, Boolean>            maintainedMajorVersions                   = new ConcurrentHashMap<>() {{
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
    public  final        AtomicBoolean                    syncWithDatabaseInProgress                = new AtomicBoolean(false);
    public  final        AtomicBoolean                    ephemeralIdCacheIsUpdating                = new AtomicBoolean(false);
    public  final        AtomicReference<String>          publicPkgs                                = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsIncldugingEa                    = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsDownloadable                    = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsIncldugingEaDownloadable        = new AtomicReference<>();

    public  final        AtomicReference<String>          publicPkgsOpenJDK                         = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsOpenJDKIncldugingEa             = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsOpenJDKDownloadable             = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsOpenJDKIncldugingEaDownloadable = new AtomicReference<>();

    public  final        AtomicReference<String>          publicPkgsGraalVM                         = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsGraalVMIncldugingEa             = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsGraalVMDownloadable             = new AtomicReference<>();
    public  final        AtomicReference<String>          publicPkgsGraalVMIncldugingEaDownloadable = new AtomicReference<>();

    public  final        AtomicLong                       msToFillCacheWithPkgsFromDB               = new AtomicLong(-1);
    public  final        AtomicReference<Duration>        durationToUpdateMongoDb                   = new AtomicReference<>();
    public  final        AtomicReference<Duration>        durationForLastUpdateRun                  = new AtomicReference();
    public  final        AtomicInteger                    distrosUpdatedInLastRun                   = new AtomicInteger(-1);
    public  final        AtomicInteger                    packagesAddedInUpdate                     = new AtomicInteger(0);
    public  final        AtomicLong                       numberOfPackages                          = new AtomicLong(-1);
    private final        List<MajorVersion>               majorVersions                             = new LinkedList<>();


    CacheManager() {
        mqttManager.subscribe(Constants.MQTT_PKG_UPDATE_TOPIC, MqttQos.EXACTLY_ONCE);
        mqttManager.subscribe(Constants.MQTT_EPHEMERAL_ID_UPDATE_TOPIC, MqttQos.EXACTLY_ONCE);
        mqttManager.addMqttObserver(mqttEvtObserver);
        maintainedMajorVersions.entrySet().forEach(entry-> majorVersions.add(new MajorVersion(entry.getKey(), Helper.getTermOfSupport(entry.getKey()), entry.getValue())));
    }


    public void updateAllDistros(final boolean forceUpdate) {
        final State state = MongoDbManager.INSTANCE.getState();
        if (State.IDLE != state) { return; }

        MongoDbManager.INSTANCE.setState(State.UPDATING);

        packagesAddedInUpdate.set(0);
        final Instant updateAllDistrosStart = Instant.now();
        final List<Pkg> pkgsFound             = new CopyOnWriteArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);

        Distro.getAsListWithoutNoneAndNotFound().forEach(distro -> {
            final Instant now                 = Instant.now();
            final Instant lastUpdateForDistro = MongoDbManager.INSTANCE.getLastUpdateForDistro(distro);
            final double  delta               = (lastUpdateForDistro.until(now, ChronoUnit.MINUTES));
            if (delta > distro.getMinUpdateIntervalInMinutes() || forceUpdate) {
                List<Pkg> pkgsOfDistro = Helper.getPkgsOfDistro(distro);
                pkgsFound.addAll(pkgsOfDistro);
                LOGGER.debug("{} packages added from {}", pkgsOfDistro.size(), distro.getName());

                MongoDbManager.INSTANCE.setLastUpdateForDistro(distro);

                distro.lastUpdate.set(now);
                counter.incrementAndGet();
                }
        });

        final Instant updateMongoDbStart = Instant.now();
                // Find delta between found and existing packages
                final Map<String, Pkg> deltaPkgs = new ConcurrentHashMap<>();
                pkgsFound.forEach(pkg -> {
                    if (!pkgCache.containsKey(pkg.getId()) && !deltaPkgs.containsKey(pkg.getId())) {
                        if (ReleaseStatus.EA == pkg.getReleaseStatus()) {
                            pkg.setLatestBuildAvailable(true);
                        }

                        List<Pkg> otherPkgs = deltaPkgs.values()
                                                       .stream()
                                                       .filter(p -> p.equalsExceptUpdate(pkg))
                                                       .collect(Collectors.toList());

                        if (!otherPkgs.isEmpty()) {
                            otherPkgs.forEach(p -> {
                                if (p.getVersionNumber().compareTo(pkg.getVersionNumber()) < 0) {
                                    p.setLatestBuildAvailable(false);
                                } else {
                                    pkg.setLatestBuildAvailable(false);
                                }
                            });
                        }
                        deltaPkgs.put(pkg.getId(), pkg);
                    } else {
                        // Default value for latest_build_available is false.
                        // Therefore we need to set the latest_build_available for all pks found to the value from the cache
                        pkg.setLatestBuildAvailable(pkgCache.get(pkg.getId()).isLatestBuildAvailable());
                    }
                });

                // Check for each pkg in deltaPkgs if new version or new build and Update packages in pkgCache
                List<Pkg> pkgsToUpdate = new ArrayList<>();
                deltaPkgs.entrySet().forEach(entry -> {
                    List<Pkg> differentBuilds = Helper.getAllBuildsOfPackage(entry.getValue());
                    if (differentBuilds.isEmpty()) {
                        // New version
                        Optional<Pkg> pkgWithMaxVersionOptional = Helper.getPkgWithMaxVersionForGivenPackage(entry.getValue());
                        if (pkgWithMaxVersionOptional.isPresent()) {
                            Pkg pkgWithMaxVersion = pkgWithMaxVersionOptional.get();
                            if (pkgWithMaxVersion.isLatestBuildAvailable() && pkgWithMaxVersion.getReleaseStatus() == entry.getValue().getReleaseStatus()) {
                                pkgsToUpdate.add(pkgWithMaxVersion);
            }
        }
                    } else {
                        // New build of existing package
                        pkgsToUpdate.addAll(differentBuilds.stream()
                                                           .filter(pkg -> pkg.getReleaseStatus() == entry.getValue().getReleaseStatus())
                                                           .filter(pkg -> pkg.isLatestBuildAvailable())
                                                           .collect(Collectors.toList()));
        }
                });
                pkgsToUpdate.forEach(pkg -> pkg.setLatestBuildAvailable(false));

                // Finally add all new packages to pkgCache
                pkgsFound.forEach(pkg -> pkgCache.add(pkg.getId(), pkg));

                // Add the delta to mongodb
                if (!deltaPkgs.isEmpty()) {
                    // Insert new packages into database
                    boolean successfullyAddedToMongoDb = MongoDbManager.INSTANCE.addNewPkgs(deltaPkgs.values());
                    packagesAddedInUpdate.set(deltaPkgs.size());
                    LOGGER.debug(successfullyAddedToMongoDb ? "Successfuly added " + deltaPkgs.size() + " new packages to mongodb" : "Failed adding new packages to mongodb");
                    deltaPkgs.clear();
                }

        // Update latestBuildAvailable
        if (packagesAddedInUpdate.get() > 0) {
            Map<String, Pkg> pkgCacheCopy               = pkgCache.getCopy();
            Collection<Pkg>  pkgsEA                     = pkgCacheCopy.values().parallelStream().filter(pkg -> ReleaseStatus.EA == pkg.getReleaseStatus()).collect(Collectors.toList());
            Collection<Pkg>  pkgsGA                     = pkgCacheCopy.values().parallelStream().filter(pkg -> ReleaseStatus.GA == pkg.getReleaseStatus()).collect(Collectors.toList());
            Collection<Pkg>  eaPkgsAfterModification    = Helper.updateLatestBuildAvailable(pkgsEA);
            Collection<Pkg>  gaPkgsAfterModification    = Helper.updateLatestBuildAvailable(pkgsGA);
            Collection<Pkg>  eaGaPkgsAfterModification  = Stream.concat(eaPkgsAfterModification.stream(), gaPkgsAfterModification.stream()).collect(Collectors.toList());
            List<Pkg>        latestBuildsAvailableAfter = eaGaPkgsAfterModification.parallelStream().filter(Pkg::isLatestBuildAvailable).collect(Collectors.toList());

            pkgCacheCopy.values().forEach(pkg -> pkg.setLatestBuildAvailable(false));
            latestBuildsAvailableAfter.forEach(pkg -> pkgCacheCopy.get(pkg.getId()).setLatestBuildAvailable(true));
            pkgCache.setAll(pkgCacheCopy);

                // Synchronize latestBuildAvailable in mongodb database with cache
                MongoDbManager.INSTANCE.syncLatestBuildAvailableInDatabaseWithCache(pkgCache.getPkgs());

            // Update all available major versions and maintained major versions
            updateMajorVersions();
        }
        MongoDbManager.INSTANCE.setState(State.IDLE);

        numberOfPackages.set(pkgCache.size());

        Instant now = Instant.now();
        durationToUpdateMongoDb.set(Duration.between(updateMongoDbStart, now));
        distrosUpdatedInLastRun.set(counter.get());
        durationForLastUpdateRun.set(Duration.between(updateAllDistrosStart, now));

        if (packagesAddedInUpdate.get() > 0 || null == publicPkgs.get() || null == publicPkgsIncldugingEa.get() || null == publicPkgsDownloadable.get() || null == publicPkgsIncldugingEaDownloadable.get() ||
            null == publicPkgsOpenJDK.get() || null == publicPkgsOpenJDKIncldugingEa.get() || null == publicPkgsOpenJDKDownloadable.get() || null == publicPkgsOpenJDKIncldugingEaDownloadable.get() ||
            null == publicPkgsGraalVM.get() || null == publicPkgsGraalVMIncldugingEa.get() || null == publicPkgsGraalVMDownloadable.get() || null == publicPkgsGraalVMIncldugingEaDownloadable.get()) {
            updateAllPkgsMsgs();
        }
    }

    public void updateEphemeralIdCache() {
        ephemeralIdCacheIsUpdating.set(true);
        MongoDbManager.INSTANCE.updateEphemeralIds();
        ephemeralIdCacheIsUpdating.set(false);
    }

    public void updateMajorVersions() {
        List<MajorVersion> majorVersionsFromDb = MongoDbManager.INSTANCE.getMajorVersions();
        if (null == majorVersionsFromDb || majorVersionsFromDb.isEmpty()) {
            LOGGER.error("Error updating major versions from mongodb");
        } else {
        majorVersions.clear();
            majorVersions.addAll(majorVersionsFromDb);
            majorVersions.forEach(majorVersion -> maintainedMajorVersions.put(majorVersion.getAsInt(), majorVersion.isMaintained()));
        }

        LOGGER.debug("Successfully updated major versions");
    }

    public void updateAllPkgsMsgs() {
        publicPkgs.set(Helper.getAllPackagesV2Msg(false, false, null));
        publicPkgsIncldugingEa.set(Helper.getAllPackagesV2Msg(false, true, null));
        publicPkgsDownloadable.set(Helper.getAllPackagesV2Msg(true, false, null));
        publicPkgsIncldugingEaDownloadable.set(Helper.getAllPackagesV2Msg(true, true, null));

        publicPkgsOpenJDK.set(Helper.getAllPackagesV2Msg(false, false, BuildScope.BUILD_OF_OPEN_JDK));
        publicPkgsOpenJDKIncldugingEa.set(Helper.getAllPackagesV2Msg(false, true, BuildScope.BUILD_OF_OPEN_JDK));
        publicPkgsOpenJDKDownloadable.set(Helper.getAllPackagesV2Msg(true, false, BuildScope.BUILD_OF_OPEN_JDK));
        publicPkgsOpenJDKIncldugingEaDownloadable.set(Helper.getAllPackagesV2Msg(true, true, BuildScope.BUILD_OF_OPEN_JDK));

        publicPkgsGraalVM.set(Helper.getAllPackagesV2Msg(false, false, BuildScope.BUILD_OF_GRAALVM));
        publicPkgsGraalVMIncldugingEa.set(Helper.getAllPackagesV2Msg(false, true, BuildScope.BUILD_OF_GRAALVM));
        publicPkgsGraalVMDownloadable.set(Helper.getAllPackagesV2Msg(true, false, BuildScope.BUILD_OF_GRAALVM));
        publicPkgsGraalVMIncldugingEaDownloadable.set(Helper.getAllPackagesV2Msg(true, true, BuildScope.BUILD_OF_GRAALVM));
    }

    public void cleanupPkgCache() {
        LOGGER.debug("Cleanup cache and update database");

        final State state = MongoDbManager.INSTANCE.getState();
        if (State.IDLE != state) { return; }

        final long start = System.currentTimeMillis();
        LOGGER.debug("Started cleaning up the cache");

        List<Pkg> pkgsToRemove = new CopyOnWriteArrayList<>();

        // Oracle packages
        ExecutorService              oracleExecutor    = Executors.newSingleThreadExecutor();
        CompletionService<List<Pkg>> oracleService     = new ExecutorCompletionService<>(oracleExecutor);
        List<Pkg>                    oraclePkgsFromWeb = new ArrayList<>();
        try {
            // Get packages from Oracle
            oracleService.submit(Helper.createTask(Distro.ORACLE));
            oracleExecutor.shutdown();
            while (!oracleExecutor.isTerminated()) {
                try {
                    oraclePkgsFromWeb.addAll(oracleService.take().get());
                } catch (ExecutionException | InterruptedException e) {
                    LOGGER.error("Error adding fetched Oracle packages to list. {}", e.getMessage());
                }
            }
        } finally {
            oracleExecutor.shutdownNow();
        }

        List<Pkg> oraclePkgsFromCache = CacheManager.INSTANCE.pkgCache.getPkgs()
              .stream()
                                                                      .filter(pkg -> Distro.ORACLE.get().equals(pkg.getDistribution()))
                                                                      .collect(Collectors.toList());

        if (oraclePkgsFromCache.size() > oraclePkgsFromWeb.size()) {
            List<String> oracleFilenamesFromWeb = oraclePkgsFromWeb.stream().map(pkgWeb -> pkgWeb.getFileName()).collect(Collectors.toList());
            pkgsToRemove.addAll(oraclePkgsFromCache.stream().filter(pkgCache -> !oracleFilenamesFromWeb.contains(pkgCache.getFileName())).collect(Collectors.toList()));
            LOGGER.debug("Oracle packages need to be removed from cache");
        } else {
            LOGGER.debug("Oracle packages are up to date");
        }

        // Redhat
        ExecutorService              redhatExecutor    = Executors.newSingleThreadExecutor();
        CompletionService<List<Pkg>> redhatService     = new ExecutorCompletionService<>(redhatExecutor);
        List<Pkg>                    redhatPkgsFromWeb = new ArrayList<>();
        try {
            // Get packages from Redhat
            redhatService.submit(Helper.createTask(Distro.RED_HAT));
            redhatExecutor.shutdown();
            while (!redhatExecutor.isTerminated()) {
                try {
                    redhatPkgsFromWeb.addAll(redhatService.take().get());
                } catch (ExecutionException | InterruptedException e) {
                    LOGGER.error("Error adding fetched Redhat packages to list. {}", e.getMessage());
                }
            }
        } finally {
            redhatExecutor.shutdownNow();
        }

        List<Pkg> redhatPkgsFromCache = CacheManager.INSTANCE.pkgCache.getPkgs()
                                               .stream()
                                                                      .filter(pkg -> Distro.RED_HAT.get().equals(pkg.getDistribution()))
                                               .collect(Collectors.toList());

        if (redhatPkgsFromCache.size() > redhatPkgsFromWeb.size()) {
            List<String> redhatFilenamesFromWeb = redhatPkgsFromWeb.stream().map(pkgWeb -> pkgWeb.getFileName()).collect(Collectors.toList());
            pkgsToRemove.addAll(redhatPkgsFromCache.stream().filter(pkgCache -> !redhatFilenamesFromWeb.contains(pkgCache.getFileName())).collect(Collectors.toList()));
            LOGGER.debug("Redhat packages need to be removed from cache");
        } else {
            LOGGER.debug("Redhat packages are up to date");
        }

        // SAP Machine
        List<Pkg> sapPkgsFromCache = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                   .stream()
                                                                   .filter(pkg -> Distro.SAP_MACHINE.get().equals(pkg.getDistribution()))
                                                                   .collect(Collectors.toList());
        pkgsToRemove.addAll(sapPkgsFromCache.stream().filter(pkg -> pkg.getFileName().contains("internal")).collect(Collectors.toList()));


        // Remove pkgs from cache and database
        CacheManager.INSTANCE.pkgCache.getPkgs().removeAll(pkgsToRemove);
        MongoDbManager.INSTANCE.removePkgs(pkgsToRemove);

        LOGGER.debug("Cache cleaned up in {} ms", (System.currentTimeMillis() - start));
    }

    public String getEphemeralIdForPkg(final String pkgId) {
        return MongoDbManager.INSTANCE.ephemeralIdCache.getEphemeralIdForPkgId(pkgId);
    }

    public List<MajorVersion> getMajorVersions() {
        if (majorVersions.isEmpty()) {
            updateMajorVersions();
        }
        return majorVersions;
    }

    public void syncCacheWithDatabase() {
        if (syncWithDatabaseInProgress.get()) { return; }

        syncWithDatabaseInProgress.set(true);

        final long startSyncronizingCache = System.currentTimeMillis();
        LOGGER.debug("Get last updates per distro from mongodb");
        Distro.getAsListWithoutNoneAndNotFound().forEach(distro -> {
            final Instant lastUpdateForDistro = MongoDbManager.INSTANCE.getLastUpdateForDistro(distro);
            distro.lastUpdate.set(lastUpdateForDistro);
        });

        LOGGER.debug("Fill cache with packages from mongodb");
        List<Pkg> pkgsFromMongoDb = MongoDbManager.INSTANCE.getPkgs();
        pkgCache.setAll(pkgsFromMongoDb.stream().collect(Collectors.toMap(Pkg::getId, pkg -> pkg)));
        numberOfPackages.set(pkgCache.size());
        msToFillCacheWithPkgsFromDB.set(System.currentTimeMillis() - startSyncronizingCache);

        // Update all available major versions and maintained major versions
        updateMajorVersions();

        syncWithDatabaseInProgress.set(false);
        }


    // ******************** MQTT Message handling *****************************
    public void handleMqttEvt(final MqttEvt evt) {
        final String topic = evt.getTopic();
        final String msg   = evt.getMsg();

        if (topic.equals(Constants.MQTT_PKG_UPDATE_TOPIC)) {
            switch(msg) {
                case Constants.MQTT_PKG_UPDATE_FINISHED_EMPTY_MSG -> {
                    if (!pkgCache.isEmpty()) { return; }
                        try {
                        LOGGER.debug("PkgCache is empty -> syncCacheWithDatabase(). MQTT event: {}", evt);

                            // Update cache with pkgs from mongodb
                            syncCacheWithDatabase();

                            // Update msgs that contain all pkgs
                            updateAllPkgsMsgs();
                        } catch (Exception e) {
                            syncWithDatabaseInProgress.set(false);
                        }
                    }
                case Constants.MQTT_PKG_UPDATE_FINISHED_MSG -> {
                    try {
                        LOGGER.debug("Database updated -> syncCacheWithDatabase(). MQTT event: {}", evt);

                        // Update cache with pkgs from mongodb
                        syncCacheWithDatabase();

                        // Update msgs that contain all pkgs
                        updateAllPkgsMsgs();
                    } catch (Exception e) {
                        syncWithDatabaseInProgress.set(false);
                    }
                }
                case Constants.MQTT_FORCE_PKG_UPDATE_MSG -> {
                    try {
                        LOGGER.debug("Force pkg update -> syncCacheWithDatabase(). MQTT event: {}", evt);

                        // Update cache with pkgs from mongodb
                        syncCacheWithDatabase();

                        // Update msgs that contain all pkgs
                        updateAllPkgsMsgs();
                    } catch (Exception e) {
                        syncWithDatabaseInProgress.set(false);
                    }
                }
            }
        } /*else if (topic.equals(Constants.MQTT_EPHEMERAL_ID_UPDATE_TOPIC)) {

            switch(msg) {
                case Constants.MQTT_EPEHMERAL_ID_UPDATE_FINISHED_MSG -> {
                }
            }
        }
        */
    }
}
