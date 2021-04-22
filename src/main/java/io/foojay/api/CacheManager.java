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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.util.Constants;
import io.foojay.api.util.EphemeralIdCache;
import io.foojay.api.util.Helper;
import io.foojay.api.util.NamedThreadFactory;
import io.foojay.api.util.PkgCache;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.foojay.api.util.Constants.SENTINEL_PKG_ID;


@Requires(notEnv = Environment.TEST) // Don't run in tests
public enum CacheManager {
    INSTANCE;

    private static final Logger                           LOGGER                        = LoggerFactory.getLogger(CacheManager.class);
    private static final String                           CHECK_FUTURE_STATUS_KEY       = "checkFutureStatus";
    private static final String                           UPDATE_EPHEMERAL_ID_CACHE_KEY = "updateEphemeralIdCache";
    private static final String                           REMOVE_SENTINEL_PKG_KEY       = "removeSentinelPkg";
    private static final String                           CLEANUP_PKG_CACHE_KEY         = "cleanupPkgCache";
    private static final String                           UPDATE_ALL_DISTROS            = "updateAllDistros";
    private final        ScheduledThreadPoolExecutor      scheduledExecutor             = new ScheduledThreadPoolExecutor(4);
    private final        Map<String, ScheduledFuture<?>>  futures                       = new ConcurrentHashMap<>();
    public final         PkgCache<String, Pkg>            pkgCache                      = new PkgCache<>();
    public final         EphemeralIdCache<String, String> ephemeralIdCache              = new EphemeralIdCache<>();
    public final         Map<Integer, Boolean>            maintainedMajorVersions       = new ConcurrentHashMap<>() {{
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
        put(15, false);
        put(16, true);
        put(17, true);
    }};
    public               AtomicBoolean                    initialized                   = new AtomicBoolean(false);
    public               AtomicBoolean                    loadingPkgCache               = new AtomicBoolean(false);
    public               AtomicBoolean                    ephemeralIdCacheIsUpdating    = new AtomicBoolean(false);
    public               AtomicBoolean                    cleaning                      = new AtomicBoolean(false);
    public               AtomicBoolean                    updateInProgress              = new AtomicBoolean(false);
    public               AtomicLong                       msToGetAllPkgsFromDB          = new AtomicLong(-1);
    public               AtomicReference<Duration>        durationForLastUpdateRun      = new AtomicReference();
    public               AtomicInteger                    distrosUpdatedInLastRun       = new AtomicInteger(-1);
    public               AtomicInteger                    numberOfPackages              = new AtomicInteger(-1);
    private final        List<MajorVersion>               majorVersions                 = new LinkedList<>();
    public               AtomicReference<LocalDateTime>   lastSentinelRemoved           = new AtomicReference<>();


    CacheManager() {
        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("CacheManager");
        scheduledExecutor.setRemoveOnCancelPolicy(true);
        scheduledExecutor.setThreadFactory(namedThreadFactory);
        scheduledExecutor.setKeepAliveTime(60, TimeUnit.SECONDS);
        scheduledExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduledExecutor.setMaximumPoolSize(8);
        scheduleTasks();
    }


    public void scheduleTasks() {
        futures.put(CHECK_FUTURE_STATUS_KEY, scheduledExecutor.scheduleWithFixedDelay(() -> checkFutureStatus(), 60, 5, TimeUnit.MINUTES));
        futures.put(UPDATE_EPHEMERAL_ID_CACHE_KEY, scheduledExecutor.scheduleWithFixedDelay(() -> updateEphemeralIdCache(), 10, 10, TimeUnit.MINUTES));
        futures.put(REMOVE_SENTINEL_PKG_KEY, scheduledExecutor.scheduleWithFixedDelay(() -> removeSentinelPkg(), 120, 120, TimeUnit.MINUTES));
        futures.put(CLEANUP_PKG_CACHE_KEY, scheduledExecutor.scheduleWithFixedDelay(() -> cleanupPkgCache(), 12420, 12420, TimeUnit.MINUTES));
        futures.put(UPDATE_ALL_DISTROS, scheduledExecutor.scheduleWithFixedDelay(() -> updateAllDistros(), 3, 5, TimeUnit.MINUTES));
    }

    public boolean isInitialized() { return initialized.get(); }

    public boolean preloadPkgCache() {
        try {
            loadingPkgCache.set(true);
            final long start = System.currentTimeMillis();

            List<Pkg> pkgsFromMongoDb = new ArrayList<>();
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
            initialized.set(true);
            loadingPkgCache.set(false);
            return true;
        } catch (IOException e) {
            LOGGER.error("Error loading packages from json file. {}", e.getMessage());
            loadingPkgCache.set(false);
            return false;
        }
    }

    public void updateAllDistros() {
        if (updateInProgress.get()) { return; }
        final Instant updateAllDistrosStart = Instant.now();
        updateInProgress.set(true);
        AtomicInteger counter = new AtomicInteger(0);
        final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        Distro.getAsListWithoutNoneAndNotFound().forEach(distro -> {
            final Instant lastDownloadForDistro = MongoDbManager.INSTANCE.getLastDownloadForDistro(distro);
            final double  delta                 = (lastDownloadForDistro.until(Instant.now(), ChronoUnit.MINUTES));
            if (delta > distro.getMinUpdateIntervalInMinutes()) {
                final List<Pkg>         pkgsFound = new CopyOnWriteArrayList<>();
                final Future<List<Pkg>> future    = singleThreadExecutor.submit(Helper.createTask(distro));
                try {
                    pkgsFound.addAll(future.get());
                } catch (CompletionException | ExecutionException | InterruptedException e) {
                    LOGGER.error("Error adding fetched packages for {} to cache. {}", distro.name(), e.getMessage());
                }

                // Find delta between found and existing packages
                final Map<String, Pkg> deltaPkgs = new ConcurrentHashMap<>();
                pkgsFound.forEach(pkg -> {
                    if (!pkgCache.containsKey(pkg.getId()) && !deltaPkgs.containsKey(pkg.getId())) {
                        if (ReleaseStatus.EA == pkg.getReleaseStatus()) {
                            pkg.setLatestBuildAvailable(true);
                        }

                        List<Pkg> otherPkgs = deltaPkgs.values()
                                                       .stream()
                                                       .filter(p -> p.getDistribution().equals(pkg.getDistribution()))
                                                       .filter(p -> p.getFeatureVersion().getAsInt() == pkg.getFeatureVersion().getAsInt())
                                                       .filter(p -> p.getOperatingSystem() == pkg.getOperatingSystem())
                                                       .filter(p -> p.getArchiveType() == pkg.getArchiveType())
                                                       .filter(p -> p.getArchitecture() == pkg.getArchitecture())
                                                       .filter(p -> p.getPackageType() == pkg.getPackageType())
                                                       .filter(p -> p.getReleaseStatus() == pkg.getReleaseStatus())
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
                    LOGGER.debug(successfullyAddedToMongoDb ? "Successfuly added new packages from " + distro.name() + " to mongodb" : "Failed adding new packages from " + distro.name() + " to mongodb");
                    deltaPkgs.clear();
                }
                if (Distro.getDistributionsBasedOnOpenJDK().contains(distro)) {
                    updateLatestBuild(ReleaseStatus.GA);
                    updateLatestBuild(ReleaseStatus.EA);
                    LOGGER.info("Latest build info updated for GA and EA releases with Java version number in package cache.");
                }

                // Check latest builds for GraalVM based distros and set latest_build_available=true
                if (Distro.getDistributionsBasedOnGraalVm().contains(distro)) {
                    for (int i = 19; i <= 40; i++) {
                        int featureVersion = i;
                        Distro.getDistributions()
                              .stream()
                              .filter(distribution -> distribution.getDistro() == Distro.GRAALVM_CE8 || distribution.getDistro() == Distro.GRAALVM_CE11 ||
                                                      distribution.getDistro() == Distro.LIBERICA_NATIVE || distribution.getDistro() == Distro.MANDREL)
                              .forEach(distribution -> {
                                  Optional<Pkg> pkgWithMaxVersion = pkgsFound.stream()
                                                                        .filter(pkg -> pkg.getDistribution().getDistro() == distribution.getDistro())
                                                                        .filter(pkg -> featureVersion == pkg.getJavaVersion().getFeature().getAsInt())
                                                                        .filter(pkg -> ReleaseStatus.GA == pkg.getReleaseStatus())
                                                                        .max(Comparator.comparing(Pkg::getJavaVersion));
                                  if (pkgWithMaxVersion.isPresent()) {
                                      SemVer maxVersion = pkgWithMaxVersion.get().getSemver();
                                      pkgsFound.stream()
                                          .filter(pkg -> pkg.getDistribution().getDistro() == distribution.getDistro())
                                          .filter(pkg -> maxVersion.compareTo(pkg.getSemver()) == 0)
                                          .forEach(pkg -> pkg.setLatestBuildAvailable(true));
                                  }
            });
                    }
                    LOGGER.debug("\"Latest build info updated for GraalVM based distros in package cache.");
                }

                // Synchronize latestBuildAvailable in mongodb database with cache
                MongoDbManager.INSTANCE.syncLatestBuildAvailableInDatabaseWithCache(pkgCache.getPkgs());

                MongoDbManager.INSTANCE.setLastDownloadForDistro(distro);

                distro.lastUpdate.set(Instant.now());
                counter.incrementAndGet();
                }
            });

        singleThreadExecutor.shutdown();
        try {
            if (!singleThreadExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                singleThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            singleThreadExecutor.shutdownNow();
        }

        LOGGER.debug("Synchronizing local cache from mongodb");
        final long startGettingAllPkgsFromMongoDb = System.currentTimeMillis();
        List<Pkg> pkgsFromMongoDb = MongoDbManager.INSTANCE.getPkgs();
        msToGetAllPkgsFromDB.set(System.currentTimeMillis() - startGettingAllPkgsFromMongoDb);
        numberOfPackages.set(pkgsFromMongoDb.size());
        
        Map<String, Pkg> pkgsToAdd = pkgsFromMongoDb.stream()
                                                    .filter(pkg -> !pkgCache.containsKey(pkg.getId()))
                                                    .collect(Collectors.toMap(Pkg::getId, pkg -> pkg));
        pkgCache.addAll(pkgsToAdd);

        distrosUpdatedInLastRun.set(counter.get());
        durationForLastUpdateRun.set(Duration.between(updateAllDistrosStart, Instant.now()));
        updateInProgress.set(false);
    }

    public void updateEphemeralIdCache() {
        LOGGER.debug("Updating ephemeral id cache (every 10m)");
        long startUpdating = System.currentTimeMillis();
        ephemeralIdCacheIsUpdating.set(true);
        ephemeralIdCache.clear();
        final long epoch = Instant.now().getEpochSecond();
        pkgCache.getKeys().forEach(id -> ephemeralIdCache.add(Helper.createEphemeralId(epoch, id), id));
        ephemeralIdCacheIsUpdating.set(false);
        LOGGER.debug("Finished updating EphemeralIDCache in {}ms", (System.currentTimeMillis() - startUpdating));

        // Update all available major versions
        updateMajorVersions();

        // Update maintained major versions
        updateMaintainedMajorVersions();
    }

    public void updateMajorVersions() {
        LOGGER.debug("Updating major versions");
        // Update all available major versions (exclude GraalVM based pkgs because they have different version numbers)
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

        if (!maintainedMajorVersions.isEmpty()) {
            Optional<Integer> maxMajorVersionInMaintainedMajorVersions = maintainedMajorVersions.keySet().stream().max(Comparator.comparingInt(Integer::intValue));
            Optional<Integer> maxMajorVersionInMajorVersions           = majorVersions.stream().max(Comparator.comparing(MajorVersion::getVersionNumber)).map(majorVersion -> majorVersion.getAsInt());
            if (maxMajorVersionInMaintainedMajorVersions.isPresent() && maxMajorVersionInMajorVersions.isPresent()) {
                if (maxMajorVersionInMaintainedMajorVersions.get() > maxMajorVersionInMajorVersions.get()) {
                    MajorVersion majorVersionToAdd = new MajorVersion(maxMajorVersionInMaintainedMajorVersions.get());
                majorVersions.add(0, majorVersionToAdd);
                LOGGER.debug("Added {} to major versions.", majorVersionToAdd);
            }
            } else {
                LOGGER.debug("Error updating major versions. Please check package cache.");
            }
        }

        LOGGER.debug("Successfully updated major versions");
    }

    public void updateMaintainedMajorVersions() {
        LOGGER.debug("Updating maintained major versions");
        final Properties maintainedProperties = new Properties();
        try {
            HttpResponse<String> response = Helper.get(Constants.MAINTAINED_PROPERTIES_URL);
            if (null == response) { return; }
            String maintainedPropertiesText = response.body();
            if (null == maintainedPropertiesText) { return; }
            maintainedProperties.load(new StringReader(maintainedPropertiesText));
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

    public void cleanupPkgCache() {
        LOGGER.debug("Cleanup cache and update database (every 3h27m)");

        if (cleaning.get()) { return; }
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

    public void removeSentinelPkg() {
        if (pkgCache.containsKey(SENTINEL_PKG_ID)) {
            pkgCache.remove(SENTINEL_PKG_ID);
            LOGGER.debug("Sentinel pkg was removed from cache and should be available after next update of cache");
            lastSentinelRemoved.set(LocalDateTime.now());
        }
    }

    private void updateLatestBuild(final ReleaseStatus releaseStatus) {
        Distro.getDistrosWithJavaVersioning()
              .stream()
              .forEach(distro -> MajorVersion.getAllMajorVersions().forEach(majorVersion -> {
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
              }));
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

    public void checkFutureStatus() {
        // Check for futures that have been canceled or that are done and re-schedule them
        final LocalDateTime now = LocalDateTime.now();
        futures.entrySet().stream().forEach(entry -> {
            final String key = entry.getKey();
            if (entry.getValue().isCancelled() || entry.getValue().isDone()) {
                if (key.equals(UPDATE_EPHEMERAL_ID_CACHE_KEY)) {
                    futures.put(UPDATE_EPHEMERAL_ID_CACHE_KEY, scheduledExecutor.scheduleWithFixedDelay(() -> updateEphemeralIdCache(), 10, 10, TimeUnit.MINUTES));
                } else if (key.equals(REMOVE_SENTINEL_PKG_KEY)) {
                    futures.put(REMOVE_SENTINEL_PKG_KEY, scheduledExecutor.scheduleWithFixedDelay(() -> removeSentinelPkg(), 5, 120, TimeUnit.MINUTES));
                } else if (key.equals(CLEANUP_PKG_CACHE_KEY)) {
                    futures.put(CLEANUP_PKG_CACHE_KEY, scheduledExecutor.scheduleWithFixedDelay(() -> cleanupPkgCache(), 10, 12420, TimeUnit.MINUTES));
                } else if (key.equals(UPDATE_ALL_DISTROS)) {
                    futures.put(UPDATE_ALL_DISTROS, scheduledExecutor.scheduleWithFixedDelay(() -> updateAllDistros(), 1, 5, TimeUnit.MINUTES));
                    }
            }
        });
    }
}
