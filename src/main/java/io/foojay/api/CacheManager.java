/*
 * Copyright (c) 2020.
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
 *     You should have received a copy of the GNU General Public License
 *     along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.util.EphemeralIdCache;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.util.PkgCache;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


@Requires(notEnv = Environment.TEST) // Don't run in tests
public enum CacheManager {
    INSTANCE;

    private static final Logger                           LOGGER                     = LoggerFactory.getLogger(CacheManager.class);
    private static       ExecutorService                  executor                   = Executors.newSingleThreadExecutor();
    private static       CompletionService<List<Pkg>>     service                    = new ExecutorCompletionService<>(executor);

    public final         PkgCache<String, Pkg>            pkgCache                   = new PkgCache<>();
    public final         EphemeralIdCache<String, String> ephemeralIdCache           = new EphemeralIdCache<>();
    public  final        Map<Integer, Boolean>            maintainedMajorVersions    = new ConcurrentHashMap<>(){{
        put(1, false);
        put(2, false);
        put(3, false);
        put(4, false);
        put(5, false);
        put(6, false);
        put(7, false);
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
    }};
    public               AtomicBoolean                    pkgCacheIsUpdating         = new AtomicBoolean(false);
    public               AtomicBoolean                    ephemeralIdCacheIsUpdating = new AtomicBoolean(false);
    private final        Map<Distro, Integer>             updateHourCounters         = new ConcurrentHashMap<>();
    private final        Map<String, Pkg>                 deltaPkgs                  = new ConcurrentHashMap<>();
    private final        ScheduledExecutorService         scheduler                  = Executors.newScheduledThreadPool(1);
    private final        List<MajorVersion>               majorVersions              = new LinkedList<>();


    CacheManager() {
        Arrays.stream(Distro.values())
              .filter(distro -> Distro.NONE != distro)
              .filter(distro -> Distro.NOT_FOUND != distro)
              .forEach(distro -> updateHourCounters.put(distro, 12));
        
        scheduler.scheduleAtFixedRate(() -> updateEphemeralIdCache(), Constants.EPHEMERAL_ID_DELAY, Constants.EPHEMERAL_ID_TIMEOUT, TimeUnit.SECONDS);
    }


    public boolean preloadPkgCache() {
        try {
            final long start = System.currentTimeMillis();

            List<Pkg> pkgsFromMongoDb = new CopyOnWriteArrayList<>();
            pkgsFromMongoDb.addAll(MongoDbManager.INSTANCE.getPkgs());

            if (pkgsFromMongoDb.isEmpty()) {
                final Gson gson = new Gson();
                final InputStream inputStream = CacheManager.class.getResourceAsStream(Constants.CACHE_DATA_FILE);
                if (null == inputStream) {
                    LOGGER.debug("Cache data file ({}) not found in resources.", Constants.CACHE_DATA_FILE);
                    return false;
                }
                final String    jsonText  = Helper.readFromInputStream(inputStream);
                final JsonArray jsonArray = gson.fromJson(jsonText, JsonArray.class);
                List<Pkg>       pkgs      = new ArrayList<>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject pkgJsonObj = jsonArray.get(i).getAsJsonObject();
                    Pkg pkg = new Pkg(pkgJsonObj.toString());
                    pkgs.add(pkg);
                    pkgCache.add(pkg.getId(), pkg);
                }
                LOGGER.debug("Successfully preloaded cache with {} packages from json file in {} ms", pkgCache.size(), (System.currentTimeMillis() - start));

                MongoDbManager.INSTANCE.insertAllPkgs(pkgs);
            } else {
                pkgsFromMongoDb.forEach(pkg -> pkgCache.add(pkg.getId(), pkg));
                LOGGER.debug("Successfully preloaded cache with {} packages from mongodb in {} ms", pkgCache.size(), (System.currentTimeMillis() - start));
            }
            updateEphemeralIdCache();
            return true;
        } catch (IOException e) {
            LOGGER.error("Error loading packages from json file. {}", e.getMessage());
            return false;
        }
    }

    public void updatePkgCache() {
        // Pre-Load cache if it is empty
        if (pkgCache.isEmpty()) { preloadPkgCache(); }

        if (executor.isShutdown()) {
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Executor termination interrupted");
            }
        }
        if (executor.isTerminated()) {
            LOGGER.debug("Executor is terminated and will be reinitialized");
            executor = Executors.newSingleThreadExecutor();
            service  = new ExecutorCompletionService<>(executor);
        }
        long start = System.currentTimeMillis();
        LOGGER.debug("Started updating package cache");
        pkgCacheIsUpdating.set(true);

        List<Pkg> pkgs = new CopyOnWriteArrayList<>();
        try {
            List<Callable<List<Pkg>>> callables = new ArrayList<>();
            // Update packages only if the updateHourCounter for each distro == the minUpdateIntervalInHours of that distro
            // Increase all counters by 1 on each update call
            Arrays.stream(Distro.values())
                  .filter(distro -> Distro.NONE != distro)
                  .filter(distro -> Distro.NOT_FOUND != distro)
                  .forEach(distro -> {
                      LOGGER.debug("Update hour counter for distro {} -> {}", distro, updateHourCounters.get(distro));
                updateHourCounters.computeIfPresent(distro, (k, v) -> v + 1);
            });

            // Only update the distros where the counter == minUpdateIntervalInHours
            Arrays.stream(Distro.values())
                  .filter(distro -> distro != Distro.NONE)
                  .filter(distro -> distro != Distro.NOT_FOUND)
                  .forEach(distro -> {
                if (updateHourCounters.get(distro) >= distro.getMinUpdateIntervalInHours()) {
                            callables.add(Helper.createTask(distro));
                            LOGGER.debug("Adding package fetch task to callables for {}", distro.name()); 
                            updateHourCounters.put(distro, 0);
                    LOGGER.debug("Reset hour counter for distro {} -> {}", distro, updateHourCounters.get(distro));
                }
            });

            LOGGER.debug("Number of distros to update {}", callables.size());

            for (final Callable<List<Pkg>> callable : callables) { service.submit(callable); }

            executor.shutdown();
            while (!executor.isTerminated()) {
                try {
                    pkgs.addAll(service.take().get());
                } catch (ExecutionException | InterruptedException e) {
                    LOGGER.error("Error adding fetched packages to cache. {}", e.getMessage());
                }
            }
        } finally {
            executor.shutdownNow();
        }

        pkgs.forEach(pkg -> {
            if (ArchiveType.NOT_FOUND == pkg.getArchiveType()) {
                pkg.setArchiveType(ArchiveType.getFromFileName(pkg.getFileName()));
            }
            if (TermOfSupport.NOT_FOUND == pkg.getTermOfSupport()) {
                pkg.setTermOfSupport(Helper.getTermOfSupport(pkg.getVersionNumber(), Distro.valueOf(pkg.getDistribution().getDistro().getName())));
            }
        });

        pkgs.forEach(pkg -> {
            if (!pkgCache.containsKey(pkg.getId()) && !deltaPkgs.containsKey(pkg.getId())) {
                deltaPkgs.put(pkg.getId(), pkg);
            }
            pkgCache.add(pkg.getId(), pkg);
        });

        pkgCacheIsUpdating.set(false);

        // Add the delta to mongodb
        if (!deltaPkgs.isEmpty()) {
            boolean successfullyAddedToMongoDb = MongoDbManager.INSTANCE.addNewPkgs(deltaPkgs.values());
            if (successfullyAddedToMongoDb) { deltaPkgs.clear(); }
        }
        LOGGER.info("Cache updated in {} ms, no of packages in cache {}", (System.currentTimeMillis() - start), pkgCache.size());
    }

    public void updateEphemeralIdCache() {
        LOGGER.debug("Updating EphemeralIdCache");
        ephemeralIdCacheIsUpdating.set(true);
        ephemeralIdCache.clear();
        final long epoch = Instant.now().getEpochSecond();
        pkgCache.getKeys().forEach(id -> ephemeralIdCache.add(Helper.createEphemeralId(epoch, id), id));
        ephemeralIdCacheIsUpdating.set(false);

        // Update all available major versions
        updateMajorVersions();

        // Update maintained major versions
        updateMaintainedMajorVersions();
    }

    public void updateMajorVersions() {
        LOGGER.debug("Updating major versions");
        // Update all available major versions
        while(CacheManager.INSTANCE.pkgCacheIsUpdating.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
                LOGGER.debug("Waiting for updating package cache");
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        majorVersions.clear(); 
        majorVersions.addAll(pkgCache.getPkgs().stream()
                                                            .map(pkg -> pkg.getVersionNumber().getFeature().getAsInt())
                                                            .distinct()
                                                            .map(majorVersion -> new MajorVersion(majorVersion))
                                                            .sorted(Comparator.comparing(MajorVersion::getVersionNumber).reversed())
                                                            .collect(Collectors.toList()));
    }

    public void updateMaintainedMajorVersions() {
        LOGGER.debug("Updating maintained major versions");
        final Properties maintainedProperties = new Properties();
        try {
            maintainedProperties.load(new StringReader(Helper.getTextFromUrl(Constants.MAINTAINED_PROPERTIES_URL)));
            maintainedProperties.entrySet().forEach(entry -> {
                Integer majorVersion = Integer.valueOf(entry.getKey().toString().replaceAll("jdk-", ""));
                Boolean maintained   = Boolean.valueOf(entry.getValue().toString().toLowerCase());
                maintainedMajorVersions.put(majorVersion, maintained);
            });
            LOGGER.debug("Successfully updated maintained major versions");
        } catch (Exception e) {
            LOGGER.error("Error loading maintained version properties from github. {}", e);
        }
    }

    public String getEphemeralIdForPkg(final String id) {
        return ephemeralIdCache.getEphemeralIdForPkgId(id);
    }

    public List<MajorVersion> getMajorVersions() {
        if (majorVersions.isEmpty()) {
            updateMajorVersions();
        }
        return majorVersions;
    }
}
