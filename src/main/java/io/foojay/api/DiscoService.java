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
 *     You should have received a copy of the GNU General Public License
 *     along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api;

import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.FPU;
import eu.hansolo.jdktools.Latest;
import eu.hansolo.jdktools.LibCType;
import eu.hansolo.jdktools.Match;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.Verification;
import eu.hansolo.jdktools.scopes.Scope;
import eu.hansolo.jdktools.util.Comparison;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.Feature;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;


public enum DiscoService {
    INSTANCE;

    public List<Pkg> getPkgsFromCache(final VersionNumber fromVersionNumber, final VersionNumber toVersionNumber, final List<Distribution> distributions, final List<Architecture> architectures, final List<FPU> fpus, final List<ArchiveType> archiveTypes,
                                      final PackageType packageType, final List<OperatingSystem> operatingSystems, final List<LibCType> libCTypes, final List<ReleaseStatus> releaseStatus, final List<TermOfSupport> termsOfSupport,
                                      final Bitness bitness, final Boolean javafxBundled, final Boolean withFxIfAvailable, final Boolean directlyDownloadable, final List<Feature> features, final Boolean signatureAvailable,
                                      final Boolean freeToUseInProduction, final Verification tckTested, final Verification aqavitCertified, final List<Scope> distroScopes, final Match match, final List<Scope> pkgScopes) {
        Collection<Pkg> selection = CacheManager.INSTANCE.pkgCache.getPkgs();
        if (null != pkgScopes && !pkgScopes.isEmpty()) {
            for (Scope scope : pkgScopes) {
                switch (scope.getApiString()) {
                    case "signature_available":
                        selection = selection.parallelStream().filter(pkg -> !pkg.getSignatureUri().isEmpty()).collect(Collectors.toList());
                        break;
                    case "signature_not_available":
                        selection = selection.parallelStream().filter(pkg -> pkg.getSignatureUri().isEmpty()).collect(Collectors.toList());
                        break;
                }
            }
        }
        Collection<Pkg> pkgSelection = selection;

        final VersionNumber minVersionNumber = null == fromVersionNumber ? new VersionNumber(6)                                : fromVersionNumber;
        final VersionNumber maxVersionNumber = null == toVersionNumber   ? new VersionNumber(MajorVersion.getLatest(true).getAsInt()) : toVersionNumber;
        List<Pkg> pkgsFound = pkgSelection.parallelStream()
                                          .filter(pkg -> distributions.isEmpty()                  ? pkg.getDistribution()        != null        : distributions.contains(pkg.getDistribution()))
                                          .filter(pkg -> Match.ANY == match                       ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains))
                                          .filter(pkg -> architectures.isEmpty()                  ? pkg.getArchitecture()        != null        : architectures.contains(pkg.getArchitecture()))
                                          .filter(pkg -> archiveTypes.isEmpty()                   ? pkg.getArchiveType()         != null        : archiveTypes.contains(pkg.getArchiveType()))
                                          .filter(pkg -> operatingSystems.isEmpty()               ? pkg.getOperatingSystem()     != null        : operatingSystems.contains(pkg.getOperatingSystem()))
                                          .filter(pkg -> libCTypes.isEmpty()                      ? pkg.getLibCType()            != null        : libCTypes.contains(pkg.getLibCType()))
                                          .filter(pkg -> termsOfSupport.isEmpty()                 ? pkg.getTermOfSupport()       != null        : termsOfSupport.contains(pkg.getTermOfSupport()))
                                          .filter(pkg -> PackageType.NONE == packageType          ? pkg.getPackageType()         != packageType : pkg.getPackageType()         == packageType)
                                          .filter(pkg -> releaseStatus.isEmpty()                  ? pkg.getReleaseStatus()       != null        : releaseStatus.contains(pkg.getReleaseStatus()))
                                          .filter(pkg -> Bitness.NONE     == bitness              ? pkg.getBitness()             != bitness     : pkg.getBitness()             == bitness)
                                          .filter(pkg -> fpus.isEmpty()                           ? pkg.getFPU()                 != null        : fpus.contains(pkg.getFPU()))
                                          .filter(pkg -> null             == javafxBundled        ? pkg.isJavaFXBundled()        != null        : pkg.isJavaFXBundled()        == javafxBundled)
                                          .filter(pkg -> null             == directlyDownloadable ? pkg.isDirectlyDownloadable() != null        : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                          .filter(pkg -> features.isEmpty()                       ? pkg.getFeatures()            != null        : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                          .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                          .filter(pkg -> null == freeToUseInProduction            ? pkg.getFreeUseInProduction() != null        : pkg.getFreeUseInProduction())
                                          .filter(pkg -> Verification.NONE == tckTested           ? pkg.getTckTested()           != null        : pkg.getTckTested()           == tckTested)
                                          .filter(pkg -> Verification.NONE == aqavitCertified     ? pkg.getAqavitCertified()     != null        : pkg.getAqavitCertified()     == aqavitCertified)
                                          .filter(pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0)
                                          .filter(pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0)
                                          .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                          .collect(Collectors.toList());
        if (null == javafxBundled && withFxIfAvailable) {
            List<Pkg> pkgsToRemove = pkgsFound.stream()
                                              .filter(Predicate.not(Pkg::isJavaFXBundled))
                                              .filter(pkg -> pkgsFound.stream().filter(p -> p.equalsExceptJavaFXAndPackageType(pkg)).count() > 0)
                                              .collect(Collectors.toList());
            pkgsFound.removeAll(pkgsToRemove);
        }

        return pkgsFound;
    }

    public List<Pkg> getPkgsFromCache(final VersionNumber versionNumber, final Comparison comparison, final List<Distribution> distributions, final List<Architecture> architectures, final List<FPU> fpus, final List<ArchiveType> archiveTypes,
                                      final PackageType packageType, final List<OperatingSystem> operatingSystems, final List<LibCType> libCTypes, final List<ReleaseStatus> releaseStatus, final List<TermOfSupport> termsOfSupport,
                                      final Bitness bitness, final Boolean javafxBundled, final Boolean withFxIfAvailable, final Boolean directlyDownloadable, final Latest latest, final List<Feature> features, final Boolean signatureAvailable,
                                      final Boolean freeToUseInProduction, final Verification tckTested, final Verification aqavitCertified, final List<Scope> distroScopes, final Match match, final List<Scope> pkgScopes) {
        return getPkgsFromCache(versionNumber, null, comparison, distributions, architectures, fpus, archiveTypes, packageType, operatingSystems, libCTypes, releaseStatus, termsOfSupport, bitness, javafxBundled, withFxIfAvailable, directlyDownloadable, latest, features, signatureAvailable, freeToUseInProduction, tckTested, aqavitCertified, distroScopes, match, pkgScopes);
    }

    public List<Pkg> getPkgsFromCache(final VersionNumber versionNumber, final VersionNumber toVersionNumber, final Comparison comparison, final List<Distribution> distributions, final List<Architecture> architectures, final List<FPU> fpus, final List<ArchiveType> archiveTypes,
                                      final PackageType packageType, final List<OperatingSystem> operatingSystems, final List<LibCType> libCTypes, final List<ReleaseStatus> releaseStatus, final List<TermOfSupport> termsOfSupport,
                                      final Bitness bitness, final Boolean javafxBundled, final Boolean withFxIfAvailable, final Boolean directlyDownloadable, final Latest latest, final List<Feature> features, final Boolean signatureAvailable,
                                      final Boolean freeToUseInProduction, final Verification tckTested, final Verification aqavitCertified, final List<Scope> distroScopes, final Match match, final List<Scope> pkgScopes) {
        Collection<Pkg> selection = CacheManager.INSTANCE.pkgCache.getPkgs();
        if (null != pkgScopes && !pkgScopes.isEmpty()) {
            for (Scope scope : pkgScopes) {
                switch (scope.getApiString()) {
                    case "signature_available":
                        selection = selection.parallelStream().filter(pkg -> !pkg.getSignatureUri().isEmpty()).collect(Collectors.toList());
                        break;
                    case "signature_not_available":
                        selection = selection.parallelStream().filter(pkg -> pkg.getSignatureUri().isEmpty()).collect(Collectors.toList());
                        break;
                }
            }
        }
        Collection<Pkg> pkgSelection = selection;
        List<Pkg> pkgsFound;
        if (Comparison.EQUAL == comparison) {
            switch(latest) {
                case OVERALL:
                case ALL_OF_VERSION:
                    final VersionNumber maxNumber;
                    if (null == versionNumber || versionNumber.getFeature().isEmpty()) {
                        Optional<Pkg> pkgWithMaxVersionNumber = pkgSelection.parallelStream()
                                                                            .filter(pkg -> distributions.isEmpty()                    ? (pkg.getDistribution() != null &&
                                                                                                                                         pkg.getDistribution().getDistro() != Distro.GRAALVM_CE8 &&
                                                                                                                                         pkg.getDistribution().getDistro() != Distro.GRAALVM_CE11 &&
                                                                                                                                         pkg.getDistribution().getDistro() != Distro.GRAALVM_CE16 &&
                                                                                                                                         pkg.getDistribution().getDistro() != Distro.GRAALVM_CE17 &&
                                                                                                                                         pkg.getDistribution().getDistro() != Distro.LIBERICA_NATIVE &&
                                                                                                                                         pkg.getDistribution().getDistro() != Distro.MANDREL) : distributions.contains(pkg.getDistribution()))
                                                                            .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains))
                                                                            .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                                            .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                                            .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                                            .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                                            .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                                            .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                                            .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                                            .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                                            .filter(pkg -> fpus.isEmpty()                             ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                                                            .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                                            .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                                            .filter(pkg -> features.isEmpty()                         ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                                                            .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                                                            .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                                                            .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                                                            .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                                                            .max(Comparator.comparing(Pkg::getSemver));
                        if (pkgWithMaxVersionNumber.isPresent()) {
                            maxNumber = pkgWithMaxVersionNumber.get().getVersionNumber();
                        } else {
                            maxNumber = versionNumber;
                        }
                    } else {
                        int featureVersion = versionNumber.getFeature().getAsInt();
                        Optional<Pkg> pkgWithMaxVersionNumber = pkgSelection.parallelStream()
                                                                            .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                                                            .filter(pkg -> Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains))
                                                                            .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                                            .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                                            .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                                            .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                                            .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                                            .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                                            .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                                            .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                                            .filter(pkg -> fpus.isEmpty()                             ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                                                            .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                                            .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                                            .filter(pkg -> features.isEmpty()                         ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                                                            .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                                                            .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                                                            .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                                                            .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                                                            .filter(pkg -> featureVersion     == pkg.getVersionNumber().getFeature().getAsInt())
                                                                            .max(Comparator.comparing(Pkg::getSemver));
                        if (pkgWithMaxVersionNumber.isPresent()) {
                            maxNumber = pkgWithMaxVersionNumber.get().getVersionNumber();
                        } else {
                            maxNumber = versionNumber;
                        }
                    }
                    if (Latest.OVERALL == latest) {
                        pkgsFound = pkgSelection.parallelStream()
                                                .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                                .filter(pkg -> Match.ANY == match                         ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains))
                                                .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                .filter(pkg -> fpus.isEmpty()                             ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                                .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                .filter(pkg -> features.isEmpty()                         ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                                .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                                .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                                .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                                .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                                .filter(pkg -> pkg.getVersionNumber().compareTo(maxNumber) == 0)
                                                .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                                .collect(Collectors.toList());
                    } else {
                        pkgsFound = pkgSelection.parallelStream()
                                                .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                                .filter(pkg -> Match.ANY == match                         ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains))
                                                .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                .filter(pkg -> fpus.isEmpty()                             ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                                .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                .filter(pkg -> features.isEmpty()                         ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                                .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                                .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                                .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                                .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                                .filter(pkg -> (pkg.getVersionNumber().getFeature().getAsInt() >= maxNumber.getFeature().getAsInt() && pkg.getVersionNumber().compareTo(maxNumber) <= 0))
                                                .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                                .collect(Collectors.toList());
                    }
                    break;
                case PER_DISTRIBUTION:
                    List<Distribution> distributionsToCheck;
                    switch(match) {
                        case ALL:
                            distributionsToCheck = distributions.isEmpty() ? Distro.getDistributions().stream().filter(distribution -> Constants.SCOPE_LOOKUP.get(distribution.getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains)).collect(Collectors.toList()) : distributions.stream().filter(distribution -> Constants.SCOPE_LOOKUP.get(distribution.getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains)).collect(Collectors.toList());
                            break;
                        case ANY:
                        default:
                            distributionsToCheck = distributions.isEmpty() ? Distro.getDistributions().stream().filter(distribution -> Constants.SCOPE_LOOKUP.get(distribution.getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains)).collect(Collectors.toList()) : distributions.stream().filter(distribution -> Constants.SCOPE_LOOKUP.get(distribution.getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains)).collect(Collectors.toList());
                            break;
                    }

                    List<Pkg>                        pkgs                      = new ArrayList<>();
                    Map<Distribution, VersionNumber> maxVersionPerDistribution = new ConcurrentHashMap<>();
                    distributionsToCheck.forEach(distro -> {
                        Optional<Pkg> pkgFound = pkgSelection.parallelStream()
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
                                                             .max(Comparator.comparing(Pkg::getSemver));
                        if (pkgFound.isPresent()) { maxVersionPerDistribution.put(distro, pkgFound.get().getVersionNumber()); }
                    });

                    distributionsToCheck.forEach(distro -> pkgs.addAll(pkgSelection.parallelStream()
                                                                                   .filter(pkg -> pkg.getDistribution().equals(distro))
                                                                                   .filter(pkg -> Match.ANY == match                         ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains))
                                                                                   .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                                                                   .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                                                                   .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                                                                   .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                                                                   .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                                                                   .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                                                                   .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                                                                   .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                                                                   .filter(pkg -> fpus.isEmpty()                             ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                                                                   .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                                                                   .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                                                                   .filter(pkg -> features.isEmpty()                         ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                                                                   .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                                                                   .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                                                                   .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                                                                   .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                                                                   .filter(pkg -> pkg.getVersionNumber().equals(maxVersionPerDistribution.get(distro)))
                                                                                   .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                                                                   .collect(Collectors.toList())));
                    pkgsFound = pkgs;
                    break;
                case PER_VERSION:
                    pkgsFound = pkgSelection.parallelStream()
                                            .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                            .filter(pkg -> Match.ANY == match                         ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains))
                                            .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                            .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                            .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                            .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                            .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                            .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                            .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                            .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                            .filter(pkg -> fpus.isEmpty()                             ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                            .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                            .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                            .filter(pkg -> features.isEmpty()                         ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                            .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                            .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                            .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                            .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                            .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == versionNumber.getFeature().getAsInt())
                                            .filter(pkg -> pkg.isLatestBuildAvailable())
                                            .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                            .collect(Collectors.toList());
                    break;
                case AVAILABLE:
                    pkgsFound = pkgSelection.parallelStream()
                                            .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                            .filter(pkg -> Match.ANY == match                         ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains))
                                            .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                            .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                            .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                            .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                            .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                            .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                            .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                            .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                            .filter(pkg -> fpus.isEmpty()                             ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                            .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                            .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                            .filter(pkg -> features.isEmpty()                         ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                            .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                            .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                            .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                            .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                            .filter(pkg -> null               == versionNumber        ? pkg.getVersionNumber()       != null          : pkg.getVersionNumber().getFeature().getAsInt() == versionNumber.getFeature().getAsInt())
                                            .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                            .collect(Collectors.toList());
                    final Set<Pkg>  filteredPkgsFound = new CopyOnWriteArraySet<>();
                    final List<Pkg> pkgsToCheck       = new CopyOnWriteArrayList<>(pkgsFound);
                    final Set<Pkg>  diffPkgs          = new CopyOnWriteArraySet<>();
                    pkgsFound.forEach(pkg -> {
                        List<Pkg> pkgsWithDifferentUpdate = pkgsToCheck.parallelStream()
                                                                       .filter(pkg1 -> pkg.equalsExceptUpdate(pkg1))
                                                                       .collect(Collectors.toList());
                        diffPkgs.addAll(pkgsWithDifferentUpdate);

                        Pkg pkgWithMaxVersion = pkgsWithDifferentUpdate.parallelStream()
                                                                       .max(Comparator.comparing(Pkg::getVersionNumber))
                                                                       .orElse(null);
                        if (null != pkgWithMaxVersion) {
                            List<Pkg> pkgsWithSmallerVersions = filteredPkgsFound.parallelStream()
                                                                                 .filter(pkg3 -> pkg3.equalsExceptUpdate(pkgWithMaxVersion))
                                                                                 .filter(pkg3 -> pkg3.getSemver().compareTo(pkgWithMaxVersion.getSemver()) < 0)
                                                                                 .collect(Collectors.toList());
                            if (!pkgsWithSmallerVersions.isEmpty()) { filteredPkgsFound.removeAll(pkgsWithSmallerVersions); }
                            filteredPkgsFound.add(pkgWithMaxVersion);
                        }
                    });

                    pkgsToCheck.removeAll(diffPkgs);
                    filteredPkgsFound.addAll(pkgsToCheck);

                    pkgsFound.clear();
                    pkgsFound.addAll(filteredPkgsFound);
                    break;
                case NONE:
                case NOT_FOUND:
                default:
                    pkgsFound = pkgSelection.parallelStream()
                                            .filter(pkg -> distributions.isEmpty()                    ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                            .filter(pkg -> Match.ANY == match                         ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains))
                                            .filter(pkg -> null != versionNumber ? versionNumber.getBuild().isPresent() ? pkg.getVersionNumber().compareTo(versionNumber) == 0 : pkg.getVersionNumber().equals(versionNumber) : null != pkg.getVersionNumber())
                                            .filter(pkg -> architectures.isEmpty()                    ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                            .filter(pkg -> archiveTypes.isEmpty()                     ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                            .filter(pkg -> operatingSystems.isEmpty()                 ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                            .filter(pkg -> libCTypes.isEmpty()                        ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                            .filter(pkg -> termsOfSupport.isEmpty()                   ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                            .filter(pkg -> PackageType.NONE   == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                            .filter(pkg -> releaseStatus.isEmpty()                    ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                            .filter(pkg -> Bitness.NONE       == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                            .filter(pkg -> fpus.isEmpty()                             ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                            .filter(pkg -> null               == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                            .filter(pkg -> null               == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                            .filter(pkg -> features.isEmpty()                         ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                            .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                            .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                            .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                            .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                            .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                            .collect(Collectors.toList());

                    if (null != versionNumber) {
                        int featureVersion = versionNumber.getFeature().getAsInt();
                        int interimVersion = versionNumber.getInterim().getAsInt();
                        int updateVersion  = versionNumber.getUpdate().getAsInt();
                        int patchVersion   = versionNumber.getPatch().getAsInt();
                        if (0 != patchVersion) {
                            // e.g. 11.N.N.3
                            pkgsFound = pkgsFound.parallelStream()
                                                 .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == featureVersion)
                                                 .filter(pkg -> pkg.getVersionNumber().getInterim().getAsInt() == interimVersion)
                                                 .filter(pkg -> pkg.getVersionNumber().getUpdate().getAsInt()  == updateVersion)
                                                 .filter(pkg -> pkg.getVersionNumber().getPatch().isPresent())
                                                 .filter(pkg -> pkg.getVersionNumber().getPatch().getAsInt()   == patchVersion)
                                                 .collect(Collectors.toList());
                        } else if (0 != updateVersion) {
                            // e.g. 11.N.2.N
                            pkgsFound = pkgsFound.parallelStream()
                                                 .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == featureVersion)
                                                 .filter(pkg -> pkg.getVersionNumber().getInterim().getAsInt() == interimVersion)
                                                 .filter(pkg -> pkg.getVersionNumber().getUpdate().isPresent())
                                                 .filter(pkg -> pkg.getVersionNumber().getUpdate().getAsInt()  == updateVersion)
                                                 .collect(Collectors.toList());
                        } else if (0 != interimVersion) {
                            // e.g. 11.1.N.N
                            pkgsFound = pkgsFound.parallelStream()
                                                 .filter(pkg -> pkg.getVersionNumber().getFeature().getAsInt() == featureVersion)
                                                 .filter(pkg -> pkg.getVersionNumber().getInterim().isPresent())
                                                 .filter(pkg -> pkg.getVersionNumber().getInterim().getAsInt() == interimVersion)
                                                 .collect(Collectors.toList());
                        } else {
                            // e.g. 11.N.N.N
                            pkgsFound = pkgsFound.parallelStream()
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
                case RANGE_INCLUDING:
                    minVersionNumber = versionNumber;
                    maxVersionNumber = null == toVersionNumber ? new VersionNumber(MajorVersion.getLatest(true).getAsInt()) : toVersionNumber;
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0;
                    break;
                case RANGE_EXCLUDING_TO:
                    minVersionNumber = versionNumber;
                    maxVersionNumber = null == toVersionNumber ? new VersionNumber(MajorVersion.getLatest(true).getAsInt()) : toVersionNumber;
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) < 0;
                    break;
                case RANGE_EXCLUDING_FROM:
                    minVersionNumber = versionNumber;
                    maxVersionNumber = null == toVersionNumber ? new VersionNumber(MajorVersion.getLatest(true).getAsInt()) : toVersionNumber;
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) > 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0;
                    break;
                case RANGE_EXCLUDING:
                    minVersionNumber = versionNumber;
                    maxVersionNumber = null == toVersionNumber ? new VersionNumber(MajorVersion.getLatest(true).getAsInt()) : toVersionNumber;
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) > 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) < 0;
                    break;
                default:
                    minVersionNumber = new VersionNumber(6);
                    maxVersionNumber = new VersionNumber(MajorVersion.getLatest(true).getAsInt());
                    greaterCheck     = pkg -> pkg.getVersionNumber().compareTo(minVersionNumber) >= 0;
                    smallerCheck     = pkg -> pkg.getVersionNumber().compareTo(maxVersionNumber) <= 0;
                    break;
            }

            pkgsFound = pkgSelection.parallelStream()
                                    .filter(pkg -> distributions.isEmpty()                  ? pkg.getDistribution()        != null          : distributions.contains(pkg.getDistribution()))
                                    .filter(pkg -> Match.ANY == match                         ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(distroScopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().allMatch(distroScopes.stream().collect(toSet())::contains))
                                    .filter(pkg -> architectures.isEmpty()                  ? pkg.getArchitecture()        != null          : architectures.contains(pkg.getArchitecture()))
                                    .filter(pkg -> archiveTypes.isEmpty()                   ? pkg.getArchiveType()         != null          : archiveTypes.contains(pkg.getArchiveType()))
                                    .filter(pkg -> operatingSystems.isEmpty()               ? pkg.getOperatingSystem()     != null          : operatingSystems.contains(pkg.getOperatingSystem()))
                                    .filter(pkg -> libCTypes.isEmpty()                      ? pkg.getLibCType()            != null          : libCTypes.contains(pkg.getLibCType()))
                                    .filter(pkg -> termsOfSupport.isEmpty()                 ? pkg.getTermOfSupport()       != null          : termsOfSupport.contains(pkg.getTermOfSupport()))
                                    .filter(pkg -> PackageType.NONE == packageType          ? pkg.getPackageType()         != packageType   : pkg.getPackageType()         == packageType)
                                    .filter(pkg -> releaseStatus.isEmpty()                  ? pkg.getReleaseStatus()       != null          : releaseStatus.contains(pkg.getReleaseStatus()))
                                    .filter(pkg -> Bitness.NONE     == bitness              ? pkg.getBitness()             != bitness       : pkg.getBitness()             == bitness)
                                    .filter(pkg -> fpus.isEmpty()                           ? pkg.getFPU()                 != null          : fpus.contains(pkg.getFPU()))
                                    .filter(pkg -> null             == javafxBundled        ? pkg.isJavaFXBundled()        != null          : pkg.isJavaFXBundled()        == javafxBundled)
                                    .filter(pkg -> null             == directlyDownloadable ? pkg.isDirectlyDownloadable() != null          : pkg.isDirectlyDownloadable() == directlyDownloadable)
                                    .filter(pkg -> features.isEmpty()                       ? pkg.getFeatures()            != null          : features.stream().anyMatch(feature -> pkg.getFeatures().contains(feature)))
                                    .filter(pkg -> null == signatureAvailable ? (pkg != null) : !signatureAvailable ? (null == pkg.getSignatureUri() || pkg.getSignatureUri().isEmpty()) : (pkg.getSignatureUri() != null && !pkg.getSignatureUri().isEmpty()))
                                    .filter(pkg -> null == freeToUseInProduction              ? pkg.getFreeUseInProduction() != null          : pkg.getFreeUseInProduction())
                                    .filter(pkg -> Verification.NONE == tckTested             ? pkg.getTckTested()           != null          : pkg.getTckTested()           == tckTested)
                                    .filter(pkg -> Verification.NONE == aqavitCertified       ? pkg.getAqavitCertified()     != null          : pkg.getAqavitCertified()     == aqavitCertified)
                                    .filter(greaterCheck)
                                    .filter(smallerCheck)
                                    .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                    .collect(Collectors.toList());
        }

        if (null == javafxBundled && null != withFxIfAvailable && withFxIfAvailable) {
            List<Pkg> finalPkgsFound = pkgsFound;
            List<Pkg> pkgsToRemove = pkgsFound.stream()
                                              .filter(Predicate.not(Pkg::isJavaFXBundled))
                                              .filter(pkg -> finalPkgsFound.stream().filter(p -> p.equalsExceptJavaFXAndPackageType(pkg)).count() > 0)
                                              .collect(Collectors.toList());
            pkgsFound.removeAll(pkgsToRemove);
        }

        return pkgsFound;
    }
}
