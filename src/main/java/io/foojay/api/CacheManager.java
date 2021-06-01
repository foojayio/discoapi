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
import io.foojay.api.util.Constants;
import io.foojay.api.util.EphemeralIdCache;
import io.foojay.api.util.Helper;
import io.foojay.api.util.OutputFormat;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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

    private static final Logger                           LOGGER                             = LoggerFactory.getLogger(CacheManager.class);
    public final         PkgCache<String, Pkg>            pkgCache                           = new PkgCache<>();
    public final         EphemeralIdCache<String, String> ephemeralIdCache                   = new EphemeralIdCache<>();
    public final         Map<Integer, Boolean>            maintainedMajorVersions            = new ConcurrentHashMap<>() {{
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
    }};
    public               AtomicBoolean                    ephemeralIdCacheIsUpdating         = new AtomicBoolean(false);
    public               AtomicReference<String>          publicPkgs                         = new AtomicReference<>();
    public               AtomicReference<String>          publicPkgsIncldugingEa             = new AtomicReference<>();
    public               AtomicReference<String>          publicPkgsDownloadable             = new AtomicReference<>();
    public               AtomicReference<String>          publicPkgsIncldugingEaDownloadable = new AtomicReference<>();
    public               AtomicLong                       msToGetAllPkgsFromDB               = new AtomicLong(-1);
    public               AtomicReference<Duration>        durationToUpdateMongoDb            = new AtomicReference<>();
    public               AtomicReference<Duration>        durationForLastUpdateRun           = new AtomicReference();
    public               AtomicInteger                    distrosUpdatedInLastRun            = new AtomicInteger(-1);
    public               AtomicInteger                    packagesAddedInUpdate              = new AtomicInteger(0);
    public               AtomicLong                       numberOfPackages                   = new AtomicLong(-1);
    private final        List<MajorVersion>               majorVersions                      = new LinkedList<>();



    public void updateAllDistros() {
        final State state = MongoDbManager.INSTANCE.getState();
        if (State.IDLE != state) { return; }

        MongoDbManager.INSTANCE.setState(State.UPDATING);

        packagesAddedInUpdate.set(0);
        final Instant updateAllDistrosStart = Instant.now();
        final List<Pkg> pkgsFound             = new CopyOnWriteArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);

        Distro.getAsListWithoutNoneAndNotFound().forEach(distro -> {
            final Instant lastUpdateForDistro = MongoDbManager.INSTANCE.getLastUpdateForDistro(distro);
            final double  delta               = (lastUpdateForDistro.until(Instant.now(), ChronoUnit.MINUTES));
            if (delta > distro.getMinUpdateIntervalInMinutes()) {
                final Instant updateDistroStart = Instant.now();
                pkgsFound.addAll(Helper.getPkgsOfDistro(distro));
                distro.lastUpdateDuration.set(Duration.between(updateDistroStart, Instant.now()));

                MongoDbManager.INSTANCE.setLastUpdateForDistro(distro);

                distro.lastUpdate.set(Instant.now());
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
        }
        MongoDbManager.INSTANCE.setState(State.IDLE);

        numberOfPackages.set(pkgCache.size());

        Instant now = Instant.now();
        durationToUpdateMongoDb.set(Duration.between(updateMongoDbStart, now));
        distrosUpdatedInLastRun.set(counter.get());
        durationForLastUpdateRun.set(Duration.between(updateAllDistrosStart, now));

        if (packagesAddedInUpdate.get() > 0 || null == publicPkgs.get() || null == publicPkgsIncldugingEa.get() || null == publicPkgsDownloadable.get() || null == publicPkgsIncldugingEaDownloadable.get()) {
            updateAllPkgsMsgs();
        }
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
                                     .filter(pkg -> pkg.getDistribution().getDistro() != Distro.GRAALVM_CE16)
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

    public void updateAllPkgsMsgs() {
        publicPkgs.set(Helper.getAllPackagesV2Msg(false, false));
        publicPkgsIncldugingEa.set(Helper.getAllPackagesV2Msg(false, true));
        publicPkgsDownloadable.set(Helper.getAllPackagesV2Msg(true, false));
        publicPkgsIncldugingEaDownloadable.set(Helper.getAllPackagesV2Msg(true, true));
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
        return ephemeralIdCache.getEphemeralIdForPkgId(pkgId);
    }

    public List<MajorVersion> getMajorVersions() {
        if (majorVersions.isEmpty()) {
            updateMajorVersions();
        }
        return majorVersions;
    }

    public void syncCacheWithDatabase() {
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(3500));
        } catch (InterruptedException e) {
            LOGGER.debug("Synchronizing wait interrupted. {}", e.getMessage());
        }

        final State state = MongoDbManager.INSTANCE.getState();
        if (State.IDLE != state) { return; }

        LOGGER.debug("Synchronizing local cache with data from mongodb");
        final long startGettingAllPkgsFromMongoDb = System.currentTimeMillis();
        MongoDbManager.INSTANCE.setState(State.SYNCRONIZING);
        List<Pkg> pkgsFromMongoDb = MongoDbManager.INSTANCE.getPkgs();
        MongoDbManager.INSTANCE.setState(State.IDLE);
        msToGetAllPkgsFromDB.set(System.currentTimeMillis() - startGettingAllPkgsFromMongoDb);

        Map<String, Pkg> pkgsToAdd = pkgsFromMongoDb.stream()
                                                    .filter(pkg -> !pkgCache.containsKey(pkg.getId()))
                                                    .collect(Collectors.toMap(Pkg::getId, pkg -> pkg));
        pkgCache.addAll(pkgsToAdd);
        numberOfPackages.set(pkgCache.size());
    }
}
