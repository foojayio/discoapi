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

package io.foojay.api.distribution;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.FPU;
import io.foojay.api.pkg.HashAlgorithm;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.SignatureType;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.Architecture.ARM;
import static io.foojay.api.pkg.Architecture.X86;
import static io.foojay.api.pkg.ArchiveType.APK;
import static io.foojay.api.pkg.ArchiveType.DEB;
import static io.foojay.api.pkg.ArchiveType.DMG;
import static io.foojay.api.pkg.ArchiveType.MSI;
import static io.foojay.api.pkg.ArchiveType.PKG;
import static io.foojay.api.pkg.ArchiveType.RPM;
import static io.foojay.api.pkg.ArchiveType.SRC_TAR;
import static io.foojay.api.pkg.ArchiveType.TAR_GZ;
import static io.foojay.api.pkg.ArchiveType.ZIP;
import static io.foojay.api.pkg.Bitness.BIT_32;
import static io.foojay.api.pkg.Bitness.BIT_64;
import static io.foojay.api.pkg.OperatingSystem.LINUX;
import static io.foojay.api.pkg.OperatingSystem.LINUX_MUSL;
import static io.foojay.api.pkg.OperatingSystem.MACOS;
import static io.foojay.api.pkg.OperatingSystem.WINDOWS;
import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;
import static io.foojay.api.pkg.TermOfSupport.LTS;


public class LibericaNative implements Distribution {
    private static final Logger                       LOGGER                        = LoggerFactory.getLogger(LibericaNative.class);

    private static final String                       PACKAGE_URL                   = "https://api.bell-sw.com/v1/nik/releases";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM            = "";
    private static final String                       OPERATING_SYSTEM_PARAM        = "";
    private static final String                       ARCHIVE_TYPE_PARAM            = "";
    private static final String                       PACKAGE_TYPE_PARAM            = "";
    private static final String                       RELEASE_STATUS_PARAM          = "";
    private static final String                       SUPPORT_TERM_PARAM            = "";
    private static final String                       BITNESS_PARAM                 = "";

    private static final String                       FIELD_BITNESS                 = "bitness";
    private static final String                       FIELD_ARCHITCTURE             = "architecture";
    private static final String                       FIELD_LATEST_LTS              = "latestLTS";
    private static final String                       FIELD_OS                      = "os";
    private static final String                       FIELD_LATEST_IN_ANUAL_VERSION = "latestInAnnualVersion";
    private static final String                       FIELD_DOWNLOAD_URL            = "downloadUrl";
    private static final String                       FIELD_LTS                     = "LTS";
    private static final String                       FIELD_BUNDLE_TYPE             = "bundleType";
    private static final String                       FIELD_PACKAGE_TYPE            = "packageType";
    private static final String                       FIELD_FEATURE_VERSION         = "featureVersion";
    private static final String                       FIELD_PATCH_VERSION           = "patchVersion";
    private static final String                       FIELD_VERSION                 = "version";
    private static final String                       FIELD_ANNUAL_VERSION          = "annualVersion";
    private static final String                       FIELD_FILENAME                = "filename";
    private static final String                       FIELD_SHA1                    = "sha1";
    private static final String                       FIELD_GA                      = "GA";
    private static final String                       FIELD_LATEST                  = "latest";

    private static final Map<Architecture, String>    ARCHITECTURE_MAP              = Map.of(ARM, "arm", X86, "x86");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP          = Map.of(LINUX, "linux", LINUX_MUSL, "linux_musl", MACOS, "macos", WINDOWS, "windows");
    private static final Map<ArchiveType, String>     ARCHIVE_TYPE_MAP              = Map.of(APK, "apk", DEB, "deb", DMG, "dmg", MSI, "msi", PKG, "pkg", RPM, "rpm", SRC_TAR, "src.tar.gz", TAR_GZ, "tar.gz", ZIP, "zip");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP            = Map.of(EA, "ea", GA, "all");
    private static final Map<Bitness, String>         BITNESS_MAP                   = Map.of(BIT_32, "32", BIT_64, "64");

    private static final HashAlgorithm                HASH_ALGORITHM                = HashAlgorithm.NONE;
    private static final String                       HASH_URI                      = "";
    private static final SignatureType                SIGNATURE_TYPE                = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM           = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI                 = "";
    private static final String                       OFFICIAL_URI                  = "https://bell-sw.com/";


    @Override public Distro getDistro() { return Distro.LIBERICA_NATIVE; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return SUPPORT_TERM_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }

    @Override public HashAlgorithm getHashAlgorithm() { return HASH_ALGORITHM; }

    @Override public String getHashUri() { return HASH_URI; }

    @Override public SignatureType getSignatureType() { return SIGNATURE_TYPE; }

    @Override public HashAlgorithm getSignatureAlgorithm() { return SIGNATURE_ALGORITHM; }

    @Override public String getSignatureUri() { return SIGNATURE_URI; }

    @Override public String getOfficialUri() { return OFFICIAL_URI; }

    @Override public List<String> getSynonyms() {
        return List.of("liberica_native", "LIBERICA_NATIVE", "libericaNative", "LibericaNative", "liberica native", "LIBERICA NATIVE", "Liberica Native");
    }

    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.LIBERICA_NATIVE.get().equals(pkg.getDistribution()))
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

        if (releaseStatus != ReleaseStatus.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(RELEASE_STATUS_PARAM).append("=").append(RELEASE_STATUS_MAP.get(releaseStatus));
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

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType bundleType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();
        return pkgs;
    }

    public List<Pkg> getAllPkgs() {
        final List<Pkg>           pkgsFound = new ArrayList<>();

        final String              apiUrl    = "https://api.bell-sw.com/v1/nik/releases?bundle-type=standard";
        final Map<String, String> headers   = new HashMap<>();
        headers.put("accept", "application/json");

        HttpResponse<String> response = Helper.get(apiUrl, headers);

        if (null == response) {
            LOGGER.debug("Response {} returned null.", Distro.LIBERICA_NATIVE.getApiString());
        } else {
            if (response.statusCode() == 200) {
                String      body    = response.body();
                Gson        gson    = new Gson();
                JsonElement element = gson.fromJson(body, JsonElement.class);
                if (element instanceof JsonArray) {
                    JsonArray jsonArray = element.getAsJsonArray();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject pkgJsonObj = jsonArray.get(i).getAsJsonObject();

                        Pkg pkg = new Pkg();
                        pkg.setDistribution(Distro.LIBERICA_NATIVE.get());
                        pkg.setFPU(FPU.UNKNOWN);
                        pkg.setDirectlyDownloadable(true);
                        pkg.setFreeUseInProduction(true);
                        pkg.setJavaFXBundled(false);
                        pkg.setTermOfSupport(LTS);
                        pkg.setPackageType(JDK);

                        if (pkgJsonObj.has(FIELD_OS)) {
                            OperatingSystem operatingSystem = OperatingSystem.fromText(pkgJsonObj.get(FIELD_OS).getAsString());
                            if (OperatingSystem.NOT_FOUND == operatingSystem) {
                                continue;
                            } else {
                                pkg.setOperatingSystem(operatingSystem);
                                pkg.setLibCType(operatingSystem.getLibCType());
                            }
                        } else {
                            continue;
                        }

                        if (pkgJsonObj.has(FIELD_ARCHITCTURE)) {
                            Architecture architecture = Architecture.fromText(pkgJsonObj.get(FIELD_ARCHITCTURE).getAsString());
                            if (Architecture.NOT_FOUND == architecture) {
                                continue;
                            } else {
                                pkg.setArchitecture(architecture);
                                pkg.setBitness(architecture.getBitness());
                            }
                        } else {
                            continue;
                        }

                        if (pkgJsonObj.has(FIELD_GA)) {
                            ReleaseStatus releaseStatus = pkgJsonObj.get(FIELD_GA).getAsBoolean() ? ReleaseStatus.GA : ReleaseStatus.EA;
                            pkg.setReleaseStatus(releaseStatus);
                        } else {
                            continue;
                        }

                        if (pkgJsonObj.has(FIELD_VERSION)) {
                            VersionNumber versionNumber = VersionNumber.fromText(pkgJsonObj.get(FIELD_VERSION).getAsString());
                            pkg.setVersionNumber(versionNumber);
                            pkg.setJavaVersion(versionNumber);
                            pkg.setDistributionVersion(versionNumber);
                        } else {
                            continue;
                        }

                        if (pkgJsonObj.has(FIELD_LATEST)) {
                            pkg.setLatestBuildAvailable(pkgJsonObj.get(FIELD_LATEST).getAsBoolean());
                        }

                        if (pkgJsonObj.has(FIELD_PACKAGE_TYPE)) {
                            ArchiveType archiveType = ArchiveType.fromText(pkgJsonObj.get(FIELD_PACKAGE_TYPE).getAsString());
                            if (ArchiveType.NOT_FOUND == archiveType) {
                                continue;
                            } else {
                                pkg.setArchiveType(archiveType);
                            }
                        } else {
                            continue;
                        }

                        if (pkgJsonObj.has(FIELD_FILENAME)) {
                            String filename = pkgJsonObj.get(FIELD_FILENAME).getAsString();
                            if (null == filename || filename.isEmpty()) {
                                continue;
                            } else {
                                pkg.setFileName(filename);
                            }
                        } else {
                            continue;
                        }

                        if (pkgJsonObj.has(FIELD_DOWNLOAD_URL)) {
                            String downloadUrl = pkgJsonObj.get(FIELD_DOWNLOAD_URL).getAsString();
                            if (null == downloadUrl || downloadUrl.isEmpty()) {
                                continue;
                            } else {
                                pkg.setDirectDownloadUri(downloadUrl);
                            }
                        } else {
                            continue;
                        }

                        if (pkgJsonObj.has(FIELD_SHA1)) {
                            String hash = pkgJsonObj.get(FIELD_SHA1).getAsString();
                            pkg.setChecksum(hash.isEmpty() ? "" : hash);
                            pkg.setChecksumType(hash.isEmpty() ? HashAlgorithm.NONE : HashAlgorithm.SHA1);
                        }

                        pkgsFound.add(pkg);
                    }
                }
            } else {
                // Problem with url request
                LOGGER.debug("Error get packages for {} calling {}", Distro.LIBERICA_NATIVE.getName(), apiUrl);
                LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
            }
        }
        return pkgsFound;
    }
}
