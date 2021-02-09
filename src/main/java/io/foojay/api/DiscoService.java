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

import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.LibCType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.Latest;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.util.Comparison;
import io.foojay.api.scopes.Scope;
import io.foojay.api.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public enum DiscoService {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoService.class);


    public List<Pkg> getPkgsFromCache(final VersionNumber fromVersionNumber, final VersionNumber toVersionNumber, final List<Distribution> distributions, final List<Architecture> architectures, final List<ArchiveType> archiveTypes,
                                      final PackageType packageType, final List<OperatingSystem> operatingSystems, final List<LibCType> libCTypes, final List<ReleaseStatus> releaseStatus, final List<TermOfSupport> termsOfSupport,
                                      final Bitness bitness, final Boolean javafxBundled, final Boolean directlyDownloadable, final List<Scope> scopes) {
        final VersionNumber minVersionNumber = null == fromVersionNumber ? new VersionNumber(6)                                : fromVersionNumber;
        final VersionNumber maxVersionNumber = null == toVersionNumber   ? new VersionNumber(MajorVersion.getLatest(true).getAsInt()) : toVersionNumber;

        List<Pkg> pkgsFound = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                            .stream()
                                                            .filter(pkg -> distributions.isEmpty()                  ? pkg.getDistribution()        != null         : distributions.contains(pkg.getDistribution()))
                                                            .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                                            .filter(pkg -> architectures.isEmpty()                  ? pkg.getArchitecture()        != null         : architectures.contains(pkg.getArchitecture()))
                                                            .filter(pkg -> archiveTypes.isEmpty()                   ? pkg.getArchiveType()         != null         : archiveTypes.contains(pkg.getArchiveType()))
                                                            .filter(pkg -> operatingSystems.isEmpty()               ? pkg.getOperatingSystem()     != null         : operatingSystems.contains(pkg.getOperatingSystem()))
                                                            .filter(pkg -> libCTypes.isEmpty()                      ? pkg.getLibCType()            != null         : libCTypes.contains(pkg.getLibCType()))
                                                            .filter(pkg -> termsOfSupport.isEmpty()                 ? pkg.getTermOfSupport()       != null         : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                            .filter(pkg -> PackageType.NONE == packageType          ? pkg.getPackageType()         != packageType  : pkg.getPackageType()         == packageType)
                                                            .filter(pkg -> releaseStatus.isEmpty()                  ? pkg.getReleaseStatus()       != null         : releaseStatus.contains(pkg.getReleaseStatus()))
                                                            .filter(pkg -> Bitness.NONE     == bitness              ? pkg.getBitness()             != bitness      : pkg.getBitness()             == bitness)
                                                            .filter(pkg -> null             == javafxBundled        ? pkg.isJavaFXBundled()        != null         : pkg.isJavaFXBundled()        == javafxBundled)
                                                            .filter(pkg -> null             == directlyDownloadable ? pkg.isDirectlyDownloadable() != null         : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                            .filter(pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0)
                                                            .filter(pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0)
                                                            .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getVersionNumber).reversed()))
                                                            .collect(Collectors.toList());
        return pkgsFound;
    }

    public List<Pkg> getPkgsFromCache(final VersionNumber versionNumber, final Comparison comparison, final List<Distribution> distributions, final List<Architecture> architectures, final List<ArchiveType> archiveTypes,
                                      final PackageType packageType, final List<OperatingSystem> operatingSystems, final List<LibCType> libCTypes, final List<ReleaseStatus> releaseStatus, final List<TermOfSupport> termsOfSupport,
                                      final Bitness bitness, final Boolean javafxBundled, final Boolean directlyDownloadable, final Latest latest, final List<Scope> scopes) {
        List<Pkg> pkgsFound;
        if (Comparison.EQUAL == comparison) {
            switch(latest) {
                case OVERALL:
                    final VersionNumber maxNumber;
                    if (null == versionNumber || versionNumber.getFeature().isEmpty()) {
                        Optional<Pkg> pkgWithMaxVersionNumber = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                                              .stream()
                                                                                              .filter(pkg -> distributions.isEmpty()                    ? (pkg.getDistribution() != null && !pkg.getDistribution().getName().equals(Distro.GRAALVM_CE8.getUiString()) && !pkg.getDistribution().getName().equals(Distro.GRAALVM_CE11.getUiString())) : distributions.contains(pkg.getDistribution()))
                                                                                              .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                                                                              .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                                                              .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                                                              .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                                                              .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                                                              .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                                                              .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                                                              .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                                                              .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                                                              .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                                                              .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                                                              .max(Comparator.comparing(Pkg::getVersionNumber));
                        if (pkgWithMaxVersionNumber.isPresent()) {
                            maxNumber = pkgWithMaxVersionNumber.get().getVersionNumber();
                        } else {
                            maxNumber = versionNumber;
                        }
                    } else {
                        int featureVersion = versionNumber.getFeature().getAsInt();
                        Optional<Pkg> pkgWithMaxVersionNumber = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                                              .stream()
                                                                                              .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                                                                              .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                                                                              .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                                                              .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                                                              .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                                                              .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                                                              .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                                                              .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                                                              .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                                                              .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                                                              .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                                                              .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                                                              .filter(pkg -> featureVersion     == pkg.getVersionNumber().getFeature().getAsInt())
                                                                                              .max(Comparator.comparing(Pkg::getVersionNumber));
                        if (pkgWithMaxVersionNumber.isPresent()) {
                            maxNumber = pkgWithMaxVersionNumber.get().getVersionNumber();
                        } else {
                            maxNumber = versionNumber;
                        }
                    }
                    pkgsFound = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                              .stream()
                                                              .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                                              .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                                              .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                              .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                              .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                              .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                              .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                              .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                              .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                              .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                              .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                              .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                              .filter(pkg -> pkg.getVersionNumber().compareTo(maxNumber) == 0)
                                                              .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getVersionNumber).reversed()))
                                                              .collect(Collectors.toList());
                    break;
                case PER_DISTRIBUTION:
                    List<Distribution>               distributionsToCheck      = distributions.isEmpty() ? Distro.getDistributions().stream().filter(distribution -> Constants.SCOPE_LOOKUP.get(distribution.getDistro()).containsAll(scopes)).collect(Collectors.toList()) : distributions.stream().filter(distribution -> Constants.SCOPE_LOOKUP.get(distribution.getDistro()).containsAll(scopes)).collect(Collectors.toList());
                    List<Pkg>                        pkgs                      = new ArrayList<>();
                    Map<Distribution, VersionNumber> maxVersionPerDistribution = new ConcurrentHashMap<>();
                    distributionsToCheck.forEach(distro -> {
                        Optional<Pkg> pkgFound = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                                                                               .stream()
                                                                                                                               .filter(pkg -> pkg.getDistribution().equals(distro))
                                                                                                                               .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                                                                                               .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                                                                                               .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                                                                                               .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                                                                                               .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                                                                                               .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                                                                                               .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                                                                                               .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                                                                                               .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                                                                                               .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                                               .max(Comparator.comparing(Pkg::getVersionNumber));
                        if (pkgFound.isPresent()) { maxVersionPerDistribution.put(distro, pkgFound.get().getVersionNumber()); }
                    });

                    distributionsToCheck.forEach(distro -> pkgs.addAll(CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                                                     .stream()
                                                                                                     .filter(pkg -> pkg.getDistribution().equals(distro))
                                                                                                     .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                                                                                     .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                                                                     .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                                                                     .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                                                                     .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                                                                     .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                                                                     .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                                                                     .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                                                                     .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                                                                     .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                                                                     .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                                                                     .filter(pkg -> pkg.getVersionNumber().equals(maxVersionPerDistribution.get(distro)))
                                                                                                     .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getVersionNumber).reversed()))
                                                                                                     .collect(Collectors.toList())));
                    pkgsFound = pkgs;
                    break;
                case PER_VERSION:
                    pkgsFound = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                              .stream()
                                                              .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                                              .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                                              .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                              .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                              .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                              .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                              .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                              .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                              .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                              .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                              .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                              .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                              .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == versionNumber.getFeature().getAsInt())
                                                              .filter(pkg -> pkg.isLatestBuildAvailable())
                                                              .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getVersionNumber).reversed()))
                                                              .collect(Collectors.toList());
                    break;
                case NONE:
                case NOT_FOUND:
                default:
                    pkgsFound = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                              .stream()
                                                              .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                                              .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                                              .filter(pkg -> pkg.getVersionNumber() != null)
                                                              .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                              .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                              .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                              .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                              .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                              .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                              .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                              .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                              .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                              .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                              .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getVersionNumber).reversed()))
                                                              .collect(Collectors.toList());
                    
                    if (null != versionNumber) {
                        int featureVersion = versionNumber.getFeature().getAsInt();
                        int interimVersion = versionNumber.getInterim().getAsInt();
                        int updateVersion  = versionNumber.getUpdate().getAsInt();
                        int patchVersion   = versionNumber.getPatch().getAsInt();
                        if (0 != patchVersion) {
                            // e.g. 11.N.N.3
                            pkgsFound = pkgsFound.stream()
                                                       .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == featureVersion)
                                                       .filter(pkg -> pkg.getVersionNumber().getInterim().getAsInt() == interimVersion)
                                                       .filter(pkg -> pkg.getVersionNumber().getUpdate().getAsInt()  == updateVersion)
                                                       .filter(pkg -> pkg.getVersionNumber().getPatch().getAsInt()   == patchVersion)
                                                       .collect(Collectors.toList());
                        } else if (0 != updateVersion) {
                            // e.g. 11.N.2.N
                            pkgsFound = pkgsFound.stream()
                                                       .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == featureVersion)
                                                       .filter(pkg -> pkg.getVersionNumber().getInterim().getAsInt() == interimVersion)
                                                       .filter(pkg -> pkg.getVersionNumber().getUpdate().getAsInt()  == updateVersion)
                                                       .collect(Collectors.toList());
                        } else if (0 != interimVersion) {
                            // e.g. 11.1.N.N
                            pkgsFound = pkgsFound.stream()
                                                       .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == featureVersion)
                                                       .filter(pkg -> pkg.getVersionNumber().getInterim().getAsInt() == interimVersion)
                                                       .collect(Collectors.toList());
                        } else {
                            // e.g. 11.N.N.N
                            pkgsFound = pkgsFound.stream()
                                                       .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == featureVersion)
                                                       .collect(Collectors.toList());
                        }
                    }
                    break;
            }
        } else {
            VersionNumber  minVersionNumber;
            VersionNumber  maxVersionNumber;
            Predicate<Pkg> greaterCheck;
            Predicate<Pkg> smallerCheck;
            switch (comparison) {
                case EQUAL:
                    minVersionNumber = versionNumber;
                    maxVersionNumber = versionNumber;
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0;
                    break;
                case LESS_THAN:
                    minVersionNumber = new VersionNumber(6);
                    maxVersionNumber = versionNumber;
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) < 0;
                    break;
                case LESS_THAN_OR_EQUAL:
                    minVersionNumber = new VersionNumber(6);
                    maxVersionNumber = versionNumber;
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0;
                    break;
                case GREATER_THAN:
                    minVersionNumber = versionNumber;
                    maxVersionNumber = new VersionNumber(MajorVersion.getLatest(true).getAsInt());
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) > 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0;
                    break;
                case GREATER_THAN_OR_EQUAL:
                    minVersionNumber = versionNumber;
                    maxVersionNumber = new VersionNumber(MajorVersion.getLatest(true).getAsInt());
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0;
                    break;
                default:
                    minVersionNumber = new VersionNumber(6);
                    maxVersionNumber = new VersionNumber(MajorVersion.getLatest(true).getAsInt());
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0;
                    break;
            }

            pkgsFound = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                      .stream()
                                                      .filter(pkg -> distributions.isEmpty()                  ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                                      .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                                      .filter(pkg -> architectures.isEmpty()                  ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                      .filter(pkg -> archiveTypes.isEmpty()                   ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                      .filter(pkg -> operatingSystems.isEmpty()               ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                      .filter(pkg -> libCTypes.isEmpty()                      ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                      .filter(pkg -> termsOfSupport.isEmpty()                 ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                      .filter(pkg -> PackageType.NONE == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                      .filter(pkg -> releaseStatus.isEmpty()                  ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                      .filter(pkg -> Bitness.NONE     == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                      .filter(pkg -> null             == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                      .filter(pkg -> null             == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                      .filter(greaterCheck)
                                                      .filter(smallerCheck)
                                                      .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getVersionNumber).reversed()))
                                                      .collect(Collectors.toList());
        }
        return pkgsFound;
    }
}
