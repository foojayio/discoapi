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
import io.foojay.api.distribution.SAPMachine;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.VersionNumber;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import java.util.regex.Matcher;
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
    public               AtomicBoolean                    cleaning                   = new AtomicBoolean(false);
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

        List<Pkg> pkgs = new CopyOnWriteArrayList<>(); // contains all packages found
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
                if (ReleaseStatus.EA == pkg.getReleaseStatus()) {
                    pkg.setLatestBuildAvailable(true);
                }

                List<Pkg> otherPkgs = deltaPkgs.values()
                                               .stream()
                                               .filter(p -> p.getDistribution().equals(pkg.getDistribution()))
                                               .filter(p -> p.getFeatureVersion().getAsInt() == pkg.getFeatureVersion().getAsInt())
                                               .filter(p -> p.getOperatingSystem()           == pkg.getOperatingSystem())
                                               .filter(p -> p.getArchiveType()               == pkg.getArchiveType())
                                               .filter(p -> p.getArchitecture()              == pkg.getArchitecture())
                                               .filter(p -> p.getPackageType()               == pkg.getPackageType())
                                               .filter(p -> p.getReleaseStatus()             == pkg.getReleaseStatus())
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
        pkgs.forEach(pkg -> pkgCache.add(pkg.getId(), pkg));

        // Add the delta to mongodb
        if (!deltaPkgs.isEmpty()) {
            // Insert new packages into database
            boolean successfullyAddedToMongoDb = MongoDbManager.INSTANCE.addNewPkgs(deltaPkgs.values());
            if (successfullyAddedToMongoDb) { deltaPkgs.clear(); }
        }

        //updateDistributionSpecificLatestBuild();

        updateLatestBuild(ReleaseStatus.GA);
        updateLatestBuild(ReleaseStatus.EA);
        LOGGER.info("Latest build info updated for GA and EA releases with Java version number in package cache.");

        LOGGER.info("Cache updated in {} ms, no of packages in cache {}", (System.currentTimeMillis() - start), pkgCache.size());
        pkgCacheIsUpdating.set(false);

        // Check latest builds for GraalVM and set latest_build_available=true
        for (int i = 19 ; i <= 30 ; i++) {
            int featureVersion = i;
            Distro.getDistributions()
                  .stream()
                  .filter(distribution -> distribution.getDistro() == Distro.GRAALVM_CE8 ||
                                          distribution.getDistro() == Distro.GRAALVM_CE11 ||
                                          distribution.getDistro() == Distro.LIBERICA_NATIVE ||
                                          distribution.getDistro() == Distro.MANDREL)
                  .forEach(distribution -> {
                      Optional<Pkg> pkgWithMaxVersion = pkgs.stream()
                                                            .filter(pkg -> pkg.getDistribution().getDistro() == distribution.getDistro())
                                                            .filter(pkg -> featureVersion   == pkg.getJavaVersion().getFeature().getAsInt())
                                                            .filter(pkg -> ReleaseStatus.GA == pkg.getReleaseStatus())
                                                            .max(Comparator.comparing(Pkg::getJavaVersion));
                      if (pkgWithMaxVersion.isPresent()) {
                          SemVer maxVersion = pkgWithMaxVersion.get().getSemver();
                          pkgs.stream()
                              .filter(pkg  -> pkg.getDistribution().getDistro() == distribution.getDistro())
                              .filter(pkg  -> maxVersion.compareTo(pkg.getSemver()) == 0)
                              .forEach(pkg -> pkg.setLatestBuildAvailable(true));
                      }
                  });
        }
        LOGGER.info("\"Latest build info updated GraalVM versions in package cache.");

        // Synchronize latestBuildAvailable in mongodb database with cache
        MongoDbManager.INSTANCE.syncLatestBuildAvailableInDatabaseWithCache(pkgCache.getPkgs());

        LOGGER.info("Cache updated in {} ms, no of packages in cache {}", (System.currentTimeMillis() - start), pkgCache.size());
    }

    public void cleanupPkgCache() {
        if (cleaning.get() || pkgCacheIsUpdating.get()) { return; }
        final long start = System.currentTimeMillis();
        LOGGER.debug("Started cleaning up the cache");
        cleaning.set(true);

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
            List<Pkg>    oraclePkgsToRemove     = oraclePkgsFromCache.stream().filter(pkgCache -> !oracleFilenamesFromWeb.contains(pkgCache.getFileName())).collect(Collectors.toList());
            LOGGER.debug("{} Oracle packages need to be removed from cache", oraclePkgsToRemove.size());
            CacheManager.INSTANCE.pkgCache.getPkgs().removeAll(oraclePkgsToRemove);
            MongoDbManager.INSTANCE.removePkgs(oraclePkgsToRemove);
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
            List<Pkg>    redhatPkgsToRemove     = redhatPkgsFromCache.stream().filter(pkgCache -> !redhatFilenamesFromWeb.contains(pkgCache.getFileName())).collect(Collectors.toList());
            LOGGER.debug("{} Redhat packages need to be removed from cache", redhatPkgsToRemove.size());
            CacheManager.INSTANCE.pkgCache.getPkgs().removeAll(redhatPkgsToRemove);
            MongoDbManager.INSTANCE.removePkgs(redhatPkgsToRemove);
        } else {
            LOGGER.debug("Redhat packages are up to date");
        }

        cleaning.set(false);
        LOGGER.debug("Cache cleaned up in {} ms", (System.currentTimeMillis() - start));
    }

    private void updateDistributionSpecificLatestBuild() {
        final int maxMajorVersionAvailable = getMajorVersions().get(0).getAsInt();

        // Update packages with latest builds in cache
        List<Pkg> maxVersions = new LinkedList<>();
        Distro.getDistributions().forEach(distribution -> {
            for (int i = 6 ; i <= maxMajorVersionAvailable ; i++) {
                VersionNumber versionNumber = new VersionNumber(i);

                final VersionNumber maxNumber;
                int featureVersion = versionNumber.getFeature().getAsInt();
                Optional<Pkg> pkgWithMaxVersionNumber = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                                      .stream()
                                                                                      .filter(pkg -> pkg.getDistribution().equals(distribution))
                                                                                      .filter(pkg -> featureVersion   == pkg.getVersionNumber().getFeature().getAsInt())
                                                                                      .max(Comparator.comparing(Pkg::getJavaVersion));
                if (pkgWithMaxVersionNumber.isPresent()) {
                    maxNumber = pkgWithMaxVersionNumber.get().getVersionNumber();
                maxVersions.addAll(CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                 .stream()
                                                                 .filter(pkg -> pkg.getDistribution().equals(distribution))
                                                                 .filter(pkg -> pkg.getVersionNumber().compareTo(maxNumber) == 0)
                                                                 .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getVersionNumber).reversed()))
                                                                 .collect(Collectors.toList()));
            }
            }
        });

        List<Pkg>          latestBuilds   = new LinkedList<>();
        LinkedHashSet<Pkg> multipleBuilds = new LinkedHashSet<>();
        maxVersions.forEach(pkg -> {
            List<Pkg> allBuildsOfPkg = Helper.getAllBuildsOfPackage(pkg);
            if (allBuildsOfPkg.isEmpty()) {
                latestBuilds.add(pkg);
            } else {
                multipleBuilds.addAll(allBuildsOfPkg);
            }
        });

        // Check multiple builds per distro for latest build and set latest_build_available=true
        for (int i = 6 ; i <= maxMajorVersionAvailable ; i++) {
            int featureVersion = i;
            // Corretto
            Optional<Pkg> pkgWithMaxBuildNumberCorretto = multipleBuilds.stream()
                                                                        .filter(p -> p.getDistributionName().equals(Distro.CORRETTO.getName()))
                                                                        .filter(pkg -> featureVersion   == pkg.getVersionNumber().getFeature().getAsInt())
                                                                        .max(Comparator.comparing(Pkg::getDistributionVersion));
            if (pkgWithMaxBuildNumberCorretto.isPresent()) {
                VersionNumber maxBuildNumber = pkgWithMaxBuildNumberCorretto.get().getDistributionVersion();
                pkgCache.getPkgs()
                        .stream()
                        .filter(p -> p.getDistributionName().equals(Distro.CORRETTO.getName()))
                        .filter(pkg -> pkg.getDistributionVersion().compareTo(maxBuildNumber) == 0)
                        .forEach(pkg -> pkg.setLatestBuildAvailable(true));
            }

            // SAP Machine
            Optional<Pkg> pkgWithMaxVersionNumberSapMachine = multipleBuilds.stream()
                                                                            .filter(p -> p.getDistributionName().equals(Distro.SAP_MACHINE.getName()))
                                                                            .filter(pkg -> featureVersion   == pkg.getVersionNumber().getFeature().getAsInt())
                                                                            .max(Comparator.comparing(Pkg::getJavaVersion));
            if (pkgWithMaxVersionNumberSapMachine.isPresent()) {
                VersionNumber maxVersionNumber = pkgWithMaxVersionNumberSapMachine.get().getVersionNumber();
                List<Pkg> maxVersionsSapMachine = pkgCache.getPkgs()
                                                          .stream()
                                                          .filter(p -> p.getDistributionName().equals(Distro.SAP_MACHINE.getName()))
                                                          .filter(pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) == 0)
                                                          .collect(Collectors.toList());

                Matcher           eaMatcher = SAPMachine.SAP_MACHINE_EA_PATTERN.matcher("");
                Map<Pkg, Integer> map       = new HashMap<>();
                maxVersionsSapMachine.forEach(pkg -> {
                    eaMatcher.reset(pkg.getFileName());
                    if (eaMatcher.find()) {
                        if (eaMatcher.groupCount() >= 2) {
                            if (Helper.isPositiveInteger(eaMatcher.group(2))) {
                                map.put(pkg, Integer.valueOf(eaMatcher.group(2)));
                            }
                        }
                    }
                });
                if (!map.isEmpty()) {
                    Integer maxBuild = map.entrySet()
                                          .stream()
                                          .max(Comparator.comparingInt(entry -> entry.getValue()))
                                          .get()
                                          .getValue();
                    map.entrySet()
                       .stream()
                       .filter(entry -> entry.getValue() == maxBuild)
                       .map(entry -> entry.getKey())
                       .forEach(pkg -> pkg.setLatestBuildAvailable(true));
                }
            }

            // Zulu
            Optional<Pkg> pkgWithMaxBuildNumberZulu = multipleBuilds.stream()
                                                                    .filter(p -> p.getDistributionName().equals(Distro.ZULU.getName()))
                                                                    .filter(pkg -> featureVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                                                    .max(Comparator.comparing(Pkg::getDistributionVersion));
            if (pkgWithMaxBuildNumberZulu.isPresent()) {
                VersionNumber maxBuildNumber = pkgWithMaxBuildNumberZulu.get().getDistributionVersion();

                pkgCache.getPkgs()
                        .stream()
                        .filter(p -> p.getDistributionName().equals(Distro.ZULU.getName()))
                        .filter(pkg -> pkg.getDistributionVersion().compareTo(maxBuildNumber) == 0)
                        .forEach(pkg -> pkg.setLatestBuildAvailable(true));
            }
        }

        // Check latest builds per distro and set latest_build_available=true
        for (int i = 6 ; i <= maxMajorVersionAvailable ; i++) {
            int featureVersion = i;
            Distro.getDistributions().forEach(distribution -> {
                // Set all latest general availability builds to latest_build_available=true
                Optional<Pkg> pkgWithMaxVersionGA = latestBuilds.stream()
                                                                .filter(pkg -> pkg.getDistributionName().equals(distribution.getDistro().getName()))
                                                                .filter(pkg -> featureVersion   == pkg.getVersionNumber().getFeature().getAsInt())
                                                                .filter(pkg -> ReleaseStatus.GA == pkg.getReleaseStatus())
                                                                .max(Comparator.comparing(Pkg::getJavaVersion));
                if (pkgWithMaxVersionGA.isPresent()) {
                    SemVer maxGaVersion = pkgWithMaxVersionGA.get().getSemver();
                    latestBuilds.stream()
                                .filter(pkg  -> pkg.getDistributionName().equals(distribution.getDistro().getName()))
                                .filter(pkg  -> maxGaVersion.compareTo(pkg.getSemver()) == 0)
                                .forEach(pkg -> pkg.setLatestBuildAvailable(true));
                }


                // Set all latest early access builds to latest_build_available=true
                Optional<Pkg> pkgWithMaxVersionEA = latestBuilds.stream()
                                                                .filter(pkg -> pkg.getDistributionName().equals(distribution.getDistro().getName()))
                                                                .filter(pkg -> featureVersion   == pkg.getVersionNumber().getFeature().getAsInt())
                                                                .filter(pkg -> ReleaseStatus.EA == pkg.getReleaseStatus())
                                                                .max(Comparator.comparing(Pkg::getJavaVersion));
                if (pkgWithMaxVersionEA.isPresent()) {
                    SemVer maxEaVersion = pkgWithMaxVersionEA.get().getSemver();
                    latestBuilds.stream()
                                .filter(pkg  -> pkg.getDistributionName().equals(distribution.getDistro().getName()))
                                .filter(pkg  -> maxEaVersion.equalTo(pkg.getSemver()))
                                .forEach(pkg -> pkg.setLatestBuildAvailable(true));
                }
            });

        }

        LOGGER.info("Updated latest build available for all packages.");
    }

    private void updateLatestBuild(final ReleaseStatus releaseStatus) {
        Distro.getDistrosWithJavaVersioning()
              .stream()
              .forEach(distro -> {
                  MajorVersion.getAllMajorVersions().forEach(majorVersion -> {
                      final int mv   = majorVersion.getAsInt();
                      List<Pkg> pkgs = pkgCache.getPkgs()
                                               .stream()
                                               .filter(pkg -> pkg.getReleaseStatus() == releaseStatus)
                                               .filter(pkg -> pkg.getDistribution().getDistro() == distro)
                                               .filter(pkg -> pkg.getJavaVersion().getMajorVersion().getAsInt() == mv)
                                               .collect(Collectors.toList());
                      SemVer maxSemVer = pkgs.stream().max(Comparator.comparing(Pkg::getSemver)).map(pkg -> pkg.getSemver()).orElse(null);
                      if (null != maxSemVer) {
                          pkgs.forEach(pkg -> pkg.setLatestBuildAvailable(false));
                          pkgs.stream()
                              .filter(pkg -> pkg.getSemver().compareTo(maxSemVer) == 0)
                              .collect(Collectors.toList())
                              .forEach(pkg -> pkg.setLatestBuildAvailable(true));
                      }
                  });
              });
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
        // Update all available major versions (exclude GraalVM because it has different version numbers)
        while(CacheManager.INSTANCE.pkgCacheIsUpdating.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
                LOGGER.debug("Waiting for updating package cache");
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        majorVersions.clear(); 
        majorVersions.addAll(pkgCache.getPkgs()
                                     .stream()
                                     .filter(pkg -> pkg.getDistribution().getDistro() != Distro.GRAALVM_CE8)
                                     .filter(pkg -> pkg.getDistribution().getDistro() != Distro.GRAALVM_CE11)
                                     .filter(pkg -> pkg.getDistribution().getDistro() != Distro.LIBERICA_NATIVE)
                                     .filter(pkg -> pkg.getDistribution().getDistro() != Distro.MANDREL)
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

    public String getEphemeralIdForPkg(final String pkgId) {
        return ephemeralIdCache.getEphemeralIdForPkgId(pkgId);
    }

    public List<MajorVersion> getMajorVersions() {
        if (majorVersions.isEmpty()) {
            updateMajorVersions();
        }
        return majorVersions;
    }
}
