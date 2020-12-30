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

package io.foojay.api.distribution;

import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.BasicScope;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import io.foojay.api.util.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.Architecture.*;
import static io.foojay.api.pkg.Bitness.*;
import static io.foojay.api.pkg.PackageType.*;
import static io.foojay.api.pkg.ArchiveType.*;
import static io.foojay.api.pkg.OperatingSystem.*;
import static io.foojay.api.pkg.ReleaseStatus.*;
import static io.foojay.api.pkg.TermOfSupport.*;


public class Liberica implements Distribution {
    private static final Logger                       LOGGER                          = LoggerFactory.getLogger(Liberica.class);

    private static final String                       PACKAGE_URL                     = "https://api.bell-sw.com/v1/liberica/releases";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM              = "arch";
    private static final String                       OPERATING_SYSTEM_PARAM          = "os";
    private static final String                       ARCHIVE_TYPE_PARAM              = "package-type";
    private static final String                       PACKAGE_TYPE_PARAM              = "bundle-type";
    private static final String                       RELEASE_STATUS_PARAM            = "build-type";
    private static final String                       SUPPORT_TERM_PARAM              = "release-type";
    private static final String                       BITNESS_PARAM                   = "bitness";
    private static final String                       FX_PARAM                        = "fx";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP                = Map.of(ARM, "arm", PPC, "ppc", SPARC, "sparc", X86, "x86", X64, "x64", AMD64, "amd64", AARCH64, "aarch64");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP            = Map.of(LINUX, "linux", LINUX_MUSL, "linux_musl", MACOS, "macos", WINDOWS, "windows", SOLARIS, "solaris");
    private static final Map<ArchiveType, String>     ARCHIVE_TYPE_MAP                = Map.of(DEB, "deb", DMG, "dmg", MSI, "msi", PKG, "pkg", RPM, "rpm", SRC_TAR, "src.tar.gz", TAR_GZ, "tar.gz", ZIP, "zip");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP                = Map.of(JDK, "jdk", JRE, "jre");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP              = Map.of(EA, "ea", GA, "all");
    private static final Map<TermOfSupport, String>   TERMS_OF_SUPPORT_MAP            = Map.of(STS, "sts", MTS, "mts", LTS, "lts");
    private static final Map<Bitness, String>         BITNESS_MAP                     = Map.of(BIT_32, "32", BIT_64, "64");
    
    // JSON fields
    private static final String                       FIELD_FILENAME                  = "filename";
    private static final String                       FIELD_DOWNLOAD_URL              = "downloadUrl";
    private static final String                       FIELD_FEATURE_VERSION           = "featureVersion";
    private static final String                       FIELD_INTERIM_VERSION           = "interimVersion";
    private static final String                       FIELD_UPDATE_VERSION            = "updateVersion";
    private static final String                       FIELD_PATCH_VERSION             = "patchVersion";
    private static final String                       FIELD_BUILD_VERSION             = "buildVersion";
    private static final String                       FIELD_FX                        = "FX";
    private static final String                       FIELD_PACKAGE_TYPE              = "packageType";
    private static final String                       FIELD_BUNDLE_TYPE               = "bundleType";
    private static final String                       FIELD_GA                        = "GA";
    private static final String                       FIELD_LTS                       = "LTS";
    private static final String                       FIELD_BITNESS                   = "bitness";
    private static final String                       FIELD_OS                        = "os";
    private static final String                       FIELD_ARCHITECTURE              = "architecture";


    @Override public Distro getDistro() { return Distro.LIBERICA; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public List<Scope> getScopes() { return List.of(BasicScope.PUBLIC); }

    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return SUPPORT_TERM_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }


    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.LIBERICA.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL);
        int initialSize = queryBuilder.length();

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("version-feature=").append(versionNumber.getFeature().getAsInt());

        /*
        if (versionNumber.getInterim().isPresent()) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append("version-interim=").append(versionNumber.getInterim().getAsInt());
        }

        if (versionNumber.getUpdate().isPresent()) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append("version-update=").append(versionNumber.getUpdate().getAsInt());
        }

        if (latest) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append("version-modifier=").append("latest");
        }
        */

        if (bitness != Bitness.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            if (Architecture.X64 == architecture) {
                queryBuilder.append(BITNESS_PARAM).append("=").append(architecture.getBitness().getAsString());
            } else {
                queryBuilder.append(BITNESS_PARAM).append("=").append(BITNESS_MAP.get(bitness));
            }
        }

        if (null != javafxBundled) {
        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(FX_PARAM).append("=").append(javafxBundled);
        }

        if (releaseStatus != ReleaseStatus.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(RELEASE_STATUS_PARAM).append("=").append(RELEASE_STATUS_MAP.get(releaseStatus));
        }

        if (termOfSupport != TermOfSupport.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(SUPPORT_TERM_PARAM).append("=").append(TERMS_OF_SUPPORT_MAP.get(termOfSupport));
        }

        if (operatingSystem != OperatingSystem.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(OPERATING_SYSTEM_PARAM).append("=").append(OPERATING_SYSTEM_MAP.get(operatingSystem));
        }

        if (architecture != Architecture.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(ARCHITECTURE_PARAM).append("=").append(ARCHITECTURE_MAP.get(architecture));
            if (Architecture.X64 == architecture && !queryBuilder.toString().contains(BITNESS_PARAM)) {
                queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
                queryBuilder.append(BITNESS_PARAM).append("=").append(architecture.getBitness().getAsString());
            }
        }

        if (archiveType != ArchiveType.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(ARCHIVE_TYPE_PARAM).append("=").append(ARCHIVE_TYPE_MAP.get(archiveType));
        }

        if (packageType != PackageType.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(PACKAGE_TYPE_PARAM).append("=").append(PACKAGE_TYPE_MAP.get(packageType));
        }

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType bundleType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();

        String        fileName      = jsonObj.get(FIELD_FILENAME).getAsString();
        String        downloadLink  = jsonObj.get(FIELD_DOWNLOAD_URL).getAsString();
        VersionNumber vNumber       = new VersionNumber(jsonObj.get(FIELD_FEATURE_VERSION).getAsInt(), jsonObj.get(FIELD_INTERIM_VERSION).getAsInt(), jsonObj.get(FIELD_UPDATE_VERSION).getAsInt(), jsonObj.get(FIELD_PATCH_VERSION).getAsInt());
        VersionNumber dNumber       = new VersionNumber(versionNumber);
        Integer       buildVersion  = jsonObj.get(FIELD_BUILD_VERSION).getAsInt();
        dNumber.setVendorSpecific("+" + buildVersion);
        vNumber.setBuild(buildVersion);
        String        packageType   = jsonObj.get(FIELD_PACKAGE_TYPE).toString().replaceAll("\"", "");
        String        bundleTyp     = jsonObj.get(FIELD_BUNDLE_TYPE).toString().replaceAll("\"", "");
        boolean       isGA          = jsonObj.get(FIELD_GA).getAsBoolean();
        boolean       isFX          = jsonObj.get(FIELD_FX).getAsBoolean();
        boolean       isLTS         = jsonObj.get(FIELD_LTS).getAsBoolean();
        Integer       bits          = jsonObj.get(FIELD_BITNESS).getAsInt();
        String        os            = jsonObj.get(FIELD_OS).getAsString();
        String        arc           = jsonObj.get(FIELD_ARCHITECTURE).getAsString();

        if (latest) {
            if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
        }
        /*else { // Leads to problems since default interim, update and patch will be set to 0
            if (!versionNumber.equals(vNumber)) { return pkgs; }
        }
        */

        TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);
        supTerm = TermOfSupport.MTS == supTerm ? TermOfSupport.STS : supTerm;

        if (null != javafxBundled) {
            if (javafxBundled != isFX) { return pkgs; }
        }

        if (OperatingSystem.NONE != operatingSystem && !OPERATING_SYSTEM_MAP.containsKey(operatingSystem)) { return pkgs; }

        Pkg pkg = new Pkg();
        pkg.setDistribution(Distro.LIBERICA.get());
        pkg.setVersionNumber(vNumber);
        pkg.setJavaVersion(vNumber);
        pkg.setDistributionVersion(dNumber);

        switch (bundleType) {
            case JDK:
                if (bundleTyp.toLowerCase().contains(Constants.JRE)) { return pkgs; }
                pkg.setPackageType(JDK);
                break;
            case JRE:
                if (bundleTyp.toLowerCase().contains(Constants.JDK)) { return pkgs; }
                pkg.setPackageType(JRE);
                break;
            case NONE:
                pkg.setPackageType(bundleTyp.toLowerCase().contains(Constants.JRE) ? JRE : JDK);
                break;
        }

        if (ArchiveType.NONE != archiveType && !packageType.equals(archiveType.getUiString())) { return pkgs; }
        ArchiveType ext = ArchiveType.fromText(packageType);
        if (ArchiveType.SRC_TAR == ext) { return pkgs; }
        pkg.setArchiveType(ArchiveType.fromText(packageType));

        Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                         .filter(entry -> fileName.contains(entry.getKey()))
                                                         .findFirst()
                                                         .map(Entry::getValue)
                                                         .orElse(Architecture.NONE);
        Bitness bit = arch.getBitness();

        if (Architecture.NONE == arch) {
            LOGGER.debug("Architecture not found in Liberica for filename: {}", fileName);
            return pkgs;
        }

        pkg.setArchitecture(arch);
        pkg.setBitness(bit);

        OperatingSystem osFound = OperatingSystem.fromText(os);
        if (OperatingSystem.NONE == osFound) {
            osFound = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                       .filter(entry -> fileName.contains(entry.getKey()))
                                                       .findFirst()
                                                       .map(Entry::getValue)
                                                       .orElse(OperatingSystem.NONE);
        }
        if (OperatingSystem.NONE == osFound) {
            LOGGER.debug("Operating Sytsem not found in Liberica for filename: {}", fileName);
            return pkgs;
        }
        pkg.setOperatingSystem(OperatingSystem.NONE == osFound ? OperatingSystem.fromText(os) : osFound);

        pkg.setJavaFXBundled(isFX);
        pkg.setReleaseStatus(isGA ? ReleaseStatus.GA : releaseStatus);

        pkg.setTermOfSupport(supTerm);

        pkg.setFileName(fileName);
        pkg.setDirectDownloadUri(downloadLink);

        pkgs.add(pkg);
        return pkgs;
    }
}

