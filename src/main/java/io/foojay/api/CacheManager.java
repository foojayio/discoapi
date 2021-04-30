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

import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.util.Constants;
import io.foojay.api.util.EphemeralIdCache;
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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Requires(notEnv = Environment.TEST) // Don't run in tests
public enum CacheManager {
    INSTANCE;

    private static final Logger                           LOGGER                        = LoggerFactory.getLogger(CacheManager.class);
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
        put(15, true);
        put(16, true);
        put(17, true);
    }};
    public               AtomicBoolean                    initialized                   = new AtomicBoolean(false);
    public               AtomicBoolean                    ephemeralIdCacheIsUpdating    = new AtomicBoolean(false);
    public               AtomicReference<Duration>        durationToUpdateMongoDb       = new AtomicReference<>();
    public               AtomicReference<Duration>        durationForLastUpdateRun      = new AtomicReference();
    public               AtomicInteger                    distrosUpdatedInLastRun       = new AtomicInteger(-1);
    public               AtomicInteger                    packagesAddedInUpdate         = new AtomicInteger(0);
    private final        List<MajorVersion>               majorVersions                 = new LinkedList<>();


    public boolean isInitialized() { return initialized.get(); }

    public void updateAllDistros() {
        final State state = MongoDbManager.INSTANCE.getState();
        if (State.IDLE != state) { return; }

        MongoDbManager.INSTANCE.setState(State.UPDATING);

        packagesAddedInUpdate.set(0);
        final Instant updateAllDistrosStart = Instant.now();
        final List<Pkg> pkgsFound             = new CopyOnWriteArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);

        Distro.getAsListWithoutNoneAndNotFound().forEach(distro -> {
            final Instant lastDownloadForDistro = MongoDbManager.INSTANCE.getLastDownloadForDistro(distro);
            final double  delta                 = (lastDownloadForDistro.until(Instant.now(), ChronoUnit.MINUTES));
            if (delta > distro.getMinUpdateIntervalInMinutes()) {
                final Instant updateDistroStart = Instant.now();
                pkgsFound.addAll(Helper.getPkgsOfDistro(distro));
                distro.lastUpdateDuration.set(Duration.between(updateDistroStart, Instant.now()));

                MongoDbManager.INSTANCE.setLastDownloadForDistro(distro);

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
                    packagesAddedInUpdate.set(deltaPkgs.size());
                    LOGGER.debug(successfullyAddedToMongoDb ? "Successfuly added " + deltaPkgs.size() + " new packages to mongodb" : "Failed adding new packages to mongodb");
                    deltaPkgs.clear();
                }

        final List<Distro> distrosBasedOnOpenJDK = Distro.getDistributionsBasedOnOpenJDK();
        final List<Distro> distrosBasedOnGraalVM = Distro.getDistributionsBasedOnGraalVm();
        Distro.getAsListWithoutNoneAndNotFound()
              .forEach(distro -> {
                  if (distrosBasedOnOpenJDK.contains(distro)) {
                    updateLatestBuild(ReleaseStatus.GA);
                    updateLatestBuild(ReleaseStatus.EA);
                    LOGGER.info("Latest build info updated for GA and EA releases with Java version number in package cache.");
                  } else if (distrosBasedOnGraalVM.contains(distro)) {
                // Check latest builds for GraalVM based distros and set latest_build_available=true
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
              });

                // Synchronize latestBuildAvailable in mongodb database with cache
                MongoDbManager.INSTANCE.syncLatestBuildAvailableInDatabaseWithCache(pkgCache.getPkgs());

        Instant now = Instant.now();
        durationToUpdateMongoDb.set(Duration.between(updateMongoDbStart, now));
        distrosUpdatedInLastRun.set(counter.get());
        durationForLastUpdateRun.set(Duration.between(updateAllDistrosStart, now));

        MongoDbManager.INSTANCE.setState(State.IDLE);
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
}
