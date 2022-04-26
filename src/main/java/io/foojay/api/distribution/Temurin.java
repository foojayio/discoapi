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
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.HashAlgorithm;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.SignatureType;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.GithubTokenPool;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.Architecture.AARCH64;
import static eu.hansolo.jdktools.Architecture.ARM;
import static eu.hansolo.jdktools.Architecture.MIPS;
import static eu.hansolo.jdktools.Architecture.PPC64;
import static eu.hansolo.jdktools.Architecture.PPC64LE;
import static eu.hansolo.jdktools.Architecture.SPARCV9;
import static eu.hansolo.jdktools.Architecture.X64;
import static eu.hansolo.jdktools.Architecture.X86;
import static eu.hansolo.jdktools.ArchiveType.getFromFileName;
import static eu.hansolo.jdktools.OperatingSystem.AIX;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.SOLARIS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.PackageType.JRE;
import static eu.hansolo.jdktools.ReleaseStatus.EA;
import static eu.hansolo.jdktools.ReleaseStatus.GA;
import static eu.hansolo.jdktools.TermOfSupport.MTS;
import static eu.hansolo.jdktools.TermOfSupport.STS;


public class Temurin implements Distribution {
    private static final Logger        LOGGER                 = LoggerFactory.getLogger(Temurin.class);

    private static final String        PACKAGE_URL            = "https://api.github.com/repos/adoptium/";
    private static final String        PACKAGE_API_URL        = "https://api.adoptium.net/v3/assets/feature_releases/";

    // URL parameters
    private static final String        ARCHITECTURE_PARAM     = "architecture";
    private static final String        OPERATING_SYSTEM_PARAM = "os";
    private static final String        ARCHIVE_TYPE_PARAM     = "";
    private static final String        PACKAGE_TYPE_PARAM     = "image_type";
    private static final String        RELEASE_STATUS_PARAM   = "release_type";
    private static final String        SUPPORT_TERM_PARAM     = "";
    private static final String        BITNESS_PARAM          = "";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP         = Map.of(AARCH64, "aarch64", ARM, "arm", MIPS, "mips", PPC64, "ppc64", PPC64LE, "ppc64le", SPARCV9, "sparcv9", X64, "x64", X86, "x32");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP     = Map.of(LINUX, "linux", MACOS, "mac", WINDOWS, "windows", SOLARIS, "solaris", AIX, "aix");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP         = Map.of(JDK, "jdk", JRE, "jre");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP       = Map.of(EA, "ea", GA, "ga");
    public  static final List<Integer>                NOT_SUPPORTED_VERSIONS   = List.of(6, 7, 9, 10, 12, 13, 14, 15);

    // JSON fields
    private static final String        FIELD_BINARIES         = "binaries";
    private static final String        FIELD_INSTALLER        = "installer";
    private static final String        FIELD_PACKAGE          = "package";
    private static final String        FIELD_LINK             = "link";
    private static final String        FIELD_NAME             = "name";
    private static final String        FIELD_VERSION_DATA     = "version_data";
    private static final String        FIELD_SEMVER           = "semver";
    private static final String        FIELD_RELEASE_TYPE     = "release_type";
    private static final String        FIELD_RELEASE_NAME     = "release_name";
    private static final String        FIELD_ARCHITECTURE     = "architecture";
    private static final String        FIELD_IMAGE_TYPE       = "image_type";
    private static final String        FIELD_JVM_IMPL         = "jvm_impl";
    private static final String        FIELD_OS               = "os";
    private static final String        FIELD_CHECKSUM         = "checksum";
    private static final String        FIELD_CHECKSUM_LINK    = "checksum_link";
    private static final String        FIELD_SIGNATURE_LINK   = "signature_link";

    private static final HashAlgorithm HASH_ALGORITHM         = HashAlgorithm.NONE;
    private static final String        HASH_URI               = "";
    private static final SignatureType SIGNATURE_TYPE         = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM    = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI          = "";
    private static final String        OFFICIAL_URI           = "https://adoptium.net/";


    @Override public Distro getDistro() { return Distro.TEMURIN; }

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
        return List.of("temurin", "Temurin", "TEMURIN");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.TEMURIN.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString))))
                                             .stream()
                                             .sorted(Comparator.comparing(Semver::getVersionNumber).reversed())
                                             .collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append(PACKAGE_API_URL);
        queryBuilder.append(versionNumber.getFeature().getAsInt()).append("/");

        if (null == RELEASE_STATUS_MAP.get(releaseStatus)) {
            final ReleaseStatus rs = new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).isEarlyAccessOnly() ? ReleaseStatus.EA : ReleaseStatus.GA;
            queryBuilder.append(RELEASE_STATUS_MAP.get(rs));
        } else {
            queryBuilder.append(RELEASE_STATUS_MAP.get(releaseStatus));
        }

        int initialSize = queryBuilder.length();

        if (architecture != Architecture.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(ARCHITECTURE_PARAM).append("=").append(ARCHITECTURE_MAP.get(architecture));
        }

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("heap_size=").append("normal");

        if (packageType != PackageType.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(PACKAGE_TYPE_PARAM).append("=").append(PACKAGE_TYPE_MAP.get(packageType));
        }

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("jvm_impl=").append("hotspot");

        if (null != operatingSystem && OperatingSystem.NONE != operatingSystem && OperatingSystem.NOT_FOUND != operatingSystem) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(OPERATING_SYSTEM_PARAM).append("=").append(OPERATING_SYSTEM_MAP.get(operatingSystem));
        }

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("page_size=").append("100");

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("project=").append("jdk");

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("vendor=").append("adoptium");

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);
        supTerm = MTS == supTerm ? STS : supTerm;

        JsonArray binaries = jsonObj.get(FIELD_BINARIES).getAsJsonArray();
        for (int i = 0 ; i < binaries.size() ; i++) {
            JsonObject    binariesObj    = binaries.get(i).getAsJsonObject();
            JsonObject    versionDataObj = jsonObj.get(FIELD_VERSION_DATA).getAsJsonObject();
            VersionNumber vNumber        = Semver.fromText(versionDataObj.get(FIELD_SEMVER).getAsString()).getSemver1().getVersionNumber();
            if (latest) {
                if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
            }
            VersionNumber dNumber = Semver.fromText(versionDataObj.get(FIELD_SEMVER).getAsString()).getSemver1().getVersionNumber();

            Architecture arc = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                            .stream()
                                                            .filter(entry -> entry.getKey().equals(binariesObj.get(FIELD_ARCHITECTURE).getAsString()))
                                                            .findFirst()
                                                            .map(Entry::getValue)
                                                            .orElse(Architecture.NONE);

            PackageType pkgTypeFound = PackageType.fromText(binariesObj.get(FIELD_IMAGE_TYPE).getAsString());

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                  .stream()
                                                                  .filter(entry -> entry.getKey().equals(binariesObj.get(FIELD_OS).getAsString()))
                                                                  .findFirst()
                                                                  .map(Entry::getValue)
                                                                  .orElse(OperatingSystem.NONE);

            if (OperatingSystem.NONE == os) {
                LOGGER.debug("Operating System not found in Temurin for field value: {}", binariesObj.get(FIELD_OS).getAsString());
                continue;
            }

            JsonElement installerElement = binariesObj.get(FIELD_INSTALLER);
            if (null != installerElement) {
                JsonObject installerObj      = installerElement.getAsJsonObject();
                String installerName         = installerObj.get(FIELD_NAME).getAsString();
                String installerDownloadLink = installerObj.get(FIELD_LINK).getAsString();

                if (installerName.contains("testimage") || installerName.contains("debugimage")) { continue; }

                if (Architecture.NONE == arc) {
                    arc = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                       .filter(entry -> installerName.contains(entry.getKey()))
                                                       .findFirst()
                                                       .map(Entry::getValue)
                                                       .orElse(Architecture.NONE);
                }

                if (Architecture.NONE == arc) {
                    LOGGER.debug("Architecture not found in Temurin for filename: {}", installerName);
                    continue;
                }

                if (PackageType.NONE == pkgTypeFound) {
                    LOGGER.debug("PackageType not found in Temurin json object");
                    continue;
                }

                Pkg installerPkg = new Pkg();
                installerPkg.setDistribution(Distro.TEMURIN.get());
                installerPkg.setVersionNumber(vNumber);
                installerPkg.setJavaVersion(vNumber);
                installerPkg.setDistributionVersion(vNumber);
                installerPkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));
                installerPkg.setTermOfSupport(supTerm);
                installerPkg.setPackageType(pkgTypeFound);
                installerPkg.setArchitecture(arc);
                installerPkg.setBitness(arc.getBitness());
                installerPkg.setOperatingSystem(os);
                installerPkg.setReleaseStatus(ReleaseStatus.NONE == releaseStatus ? GA : releaseStatus);
                Helper.setTermOfSupport(versionNumber, installerPkg);
                ArchiveType ext = getFromFileName(installerName);
                if (installerObj.has(FIELD_SIGNATURE_LINK)) {
                    String signatureLink = installerObj.get(FIELD_SIGNATURE_LINK).getAsString();
                    installerPkg.setSignatureUri(signatureLink.isEmpty() ? "" : signatureLink);
                }
                if (installerObj.has(FIELD_CHECKSUM)) {
                    String checksum = installerObj.get(FIELD_CHECKSUM).getAsString();
                    installerPkg.setChecksum(checksum.isEmpty() ? "" : checksum);
                    installerPkg.setChecksumType(checksum.isEmpty() ? HashAlgorithm.NONE : HashAlgorithm.SHA256);
                }
                if (installerObj.has(FIELD_CHECKSUM_LINK)) {
                    String checksumLink = installerObj.get(FIELD_CHECKSUM_LINK).getAsString();
                    installerPkg.setChecksumUri(checksumLink.isEmpty()  ? ""                 : checksumLink);
                    installerPkg.setChecksumType(checksumLink.isEmpty() ? HashAlgorithm.NONE : HashAlgorithm.SHA256);
                }
                installerPkg.setSize(Helper.getFileSize(installerDownloadLink));
                if(ArchiveType.NONE == archiveType || ext == archiveType) {
                    installerPkg.setArchiveType(ext);
                    installerPkg.setFileName(installerName);
                    installerPkg.setDirectDownloadUri(installerDownloadLink);
                    installerPkg.setFreeUseInProduction(Boolean.TRUE);
                    if (onlyNewPkgs) {
                        if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFileName().equals(installerName)).filter(p -> p.getDirectDownloadUri().equals(installerDownloadLink)).count() == 0) {
                            pkgs.add(installerPkg);
                        }
                    }
                }
            }

            JsonElement packageElement = binariesObj.get(FIELD_PACKAGE);
            if (null != packageElement) {
                JsonObject packageObj      = packageElement.getAsJsonObject();
                String packageName         = packageObj.get(FIELD_NAME).getAsString();
                String packageDownloadLink = packageObj.get(FIELD_LINK).getAsString();

                if (packageName.contains("testimage") || packageName.contains("debugimage")) { continue; }

                if (Architecture.NONE == arc) {
                    arc = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                       .filter(entry -> packageName.contains(entry.getKey()))
                                                       .findFirst()
                                                       .map(Entry::getValue)
                                                       .orElse(Architecture.NONE);
                }

                if (Architecture.NONE == arc) {
                    LOGGER.debug("Architecture not found in Temurin for filename: {}", packageName);
                    continue;
                }

                if (PackageType.NONE == pkgTypeFound) {
                    LOGGER.debug("PackageType not found in Temurin json object");
                    continue;
                }

                if (onlyNewPkgs) {
                    if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFileName().equals(packageName)).filter(p -> p.getDirectDownloadUri().equals(packageDownloadLink)).count() > 0) { continue; }
                }

                Pkg packagePkg = new Pkg();
                packagePkg.setDistribution(Distro.TEMURIN.get());
                packagePkg.setVersionNumber(vNumber);
                packagePkg.setJavaVersion(vNumber);
                packagePkg.setDistributionVersion(dNumber);
                packagePkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));
                packagePkg.setTermOfSupport(supTerm);
                packagePkg.setPackageType(pkgTypeFound);
                packagePkg.setArchitecture(arc);
                packagePkg.setBitness(arc.getBitness());
                packagePkg.setOperatingSystem(os);
                packagePkg.setReleaseStatus(ReleaseStatus.NONE == releaseStatus ? GA : releaseStatus);
                ArchiveType ext = getFromFileName(packageName);
                if(ArchiveType.NONE == archiveType || ext == archiveType) {
                    packagePkg.setArchiveType(ext);
                    packagePkg.setFileName(packageName);
                    packagePkg.setDirectDownloadUri(packageDownloadLink);
                    packagePkg.setFreeUseInProduction(Boolean.TRUE);
                    pkgs.add(packagePkg);
                }
                if (packageObj.has(FIELD_SIGNATURE_LINK)) {
                    String signatureLink = packageObj.get(FIELD_SIGNATURE_LINK).getAsString();
                    packagePkg.setSignatureUri(signatureLink.isEmpty() ? "" : signatureLink);
                }
                if (packageObj.has(FIELD_CHECKSUM)) {
                    String checksum = packageObj.get(FIELD_CHECKSUM).getAsString();
                    packagePkg.setChecksum(checksum.isEmpty() ? "" : checksum);
                    packagePkg.setChecksumType(checksum.isEmpty() ? HashAlgorithm.NONE : HashAlgorithm.SHA256);
                }
                if (packageObj.has(FIELD_CHECKSUM_LINK)) {
                    String checksumLink = packageObj.get(FIELD_CHECKSUM_LINK).getAsString();
                    packagePkg.setChecksumUri(checksumLink.isEmpty()  ? ""                 : checksumLink);
                    packagePkg.setChecksumType(checksumLink.isEmpty() ? HashAlgorithm.NONE : HashAlgorithm.SHA256);
                }
                packagePkg.setSize(Helper.getFileSize(packageDownloadLink));
            }
        }

        

        return pkgs;
    }

    public List<Pkg> getAllPkgs(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            for (int i = 8 ; i <= MajorVersion.getLatest(true).getAsInt() ; i++) {
                String packageUrl = PACKAGE_URL + "temurin" + i + "-binaries/releases";
                // Get all packages from github
                try {
                    HttpResponse<String> response = Helper.get(packageUrl, Map.of("accept", "application/vnd.github.v3+json",
                                                                                  "authorization", GithubTokenPool.INSTANCE.next()));
                    if (response.statusCode() == 200) {
                        String      bodyText = response.body();
                        Gson        gson     = new Gson();
                        JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                        if (element instanceof JsonArray) {
                            JsonArray jsonArray = element.getAsJsonArray();
                            pkgs.addAll(getAllPkgsFromJson(jsonArray, i, onlyNewPkgs));
                        }
                    } else {
                        // Problem with url request
                        LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
                    }
                } catch (CompletionException e) {
                    LOGGER.error("Error fetching packages for distribution {} from {}", getName(), packageUrl);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages from Temurin. {}", e);
        }

        

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromJson(final JsonArray jsonArray, final int featureVersion, final boolean onlyNewPkgs) {
        List<Pkg>              pkgs            = new ArrayList<>();
        Optional<MajorVersion> majorVersionOpt = CacheManager.INSTANCE.getMajorVersions().stream().filter(majorVersion -> majorVersion.getAsInt() == featureVersion).findFirst();
        if (majorVersionOpt.isPresent()) {
            boolean isEarlyAccessOnly = majorVersionOpt.get().isEarlyAccessOnly();
            LocalDateTime publishedAt     = LocalDateTime.MIN;
            LocalDateTime lastPublishedAt = publishedAt;
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObj = jsonArray.get(i).getAsJsonObject();
                if (jsonObj.has("prerelease")) {
                    boolean prerelease = jsonObj.get("prerelease").getAsBoolean();
                    if (prerelease && !isEarlyAccessOnly) { continue; }
                }
                if (jsonObj.has("published_at")) {
                    publishedAt = LocalDateTime.parse(jsonObj.get("published_at").getAsString(), DateTimeFormatter.ISO_DATE_TIME);
                }
                JsonArray assets = jsonObj.getAsJsonArray("assets");
                for (JsonElement element : assets) {
                    JsonObject assetJsonObj = element.getAsJsonObject();
                    String     filename     = assetJsonObj.get("name").getAsString();

                    if (null == filename || filename.isEmpty() || filename.endsWith("txt") || filename.contains("debugimage") || filename.contains("testimage") || filename.endsWith("json")) { continue; }
                    if (filename.contains("-debug-")) { continue; }
                    if (null == filename || !filename.startsWith("OpenJDK")) { continue; }

                    final String   withoutPrefix = filename.replaceAll("OpenJDK[0-9]+U?\\-", "");
                    final String   withoutSuffix = withoutPrefix.substring(0, withoutPrefix.lastIndexOf("."));
                    final String[] filenameParts = withoutSuffix.split("_");

                    final VersionNumber versionNumber;
                    final MajorVersion  majorVersion;
                    final ReleaseStatus releaseStatus;
                    if (isEarlyAccessOnly) {
                        versionNumber = majorVersionOpt.get().getVersionNumber();
                        majorVersion  = majorVersionOpt.get();
                        releaseStatus = EA;
                    } else {
                        versionNumber = VersionNumber.fromText(filenameParts[4] + (filenameParts.length == 6 ? ("+b" + filenameParts[5]) : ""));
                        majorVersion  = new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0);
                        releaseStatus = (filename.contains("-ea.") || majorVersion.equals(MajorVersion.getLatest(true))) ? EA : GA;
                    }
                    String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

                    if (onlyNewPkgs) {
                        if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFileName().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
                    }

                    PackageType packageType = PackageType.fromText(filenameParts[0]);

                    OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                                       .stream()
                                                                                       .filter(entry -> withoutSuffix.contains(entry.getKey()))
                                                                                       .findFirst()
                                                                                       .map(Entry::getValue)
                                                                                       .orElse(OperatingSystem.NOT_FOUND);
                    if (OperatingSystem.NOT_FOUND == operatingSystem) {
                        LOGGER.debug("Operating System not found in Temurin for filename: {}", filename);
                        continue;
                    }


                    final Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                                                   .stream()
                                                                                   .filter(entry -> withoutSuffix.contains(entry.getKey()))
                                                                                   .findFirst()
                                                                                   .map(Entry::getValue)
                                                                                   .orElse(Architecture.NOT_FOUND);
                    if (Architecture.NOT_FOUND == architecture) {
                        LOGGER.debug("Architecture not found in Temurin for filename: {}", filename);
                        continue;
                    }

                    final ArchiveType archiveType = Helper.getFileEnding(filename);
                    if (OperatingSystem.MACOS == operatingSystem) {
                        switch (archiveType) {
                            case DEB:
                            case RPM:
                                operatingSystem = OperatingSystem.LINUX;
                                break;
                            case CAB:
                            case MSI:
                            case EXE:
                                operatingSystem = OperatingSystem.WINDOWS;
                                break;
                        }
                    }

                    Pkg pkg = new Pkg();
                    pkg.setDistribution(Distro.TEMURIN.get());
                    pkg.setArchitecture(architecture);
                    pkg.setBitness(architecture.getBitness());
                    pkg.setVersionNumber(versionNumber);
                    pkg.setJavaVersion(versionNumber);
                    pkg.setDistributionVersion(versionNumber);
                    pkg.setJdkVersion(new MajorVersion(versionNumber.getFeature().getAsInt()));
                    pkg.setDirectDownloadUri(downloadLink);
                    pkg.setFileName(filename);
                    pkg.setArchiveType(archiveType);
                    pkg.setJavaFXBundled(false);
                    pkg.setTermOfSupport(majorVersion.getTermOfSupport());
                    pkg.setReleaseStatus(releaseStatus);
                    pkg.setPackageType(packageType);
                    pkg.setOperatingSystem(operatingSystem);
                    pkg.setFreeUseInProduction(Boolean.TRUE);
                    pkg.setSize(Helper.getFileSize(downloadLink));

                    if (isEarlyAccessOnly) {
                        if (publishedAt.isAfter(lastPublishedAt)) { pkgs.add(pkg); }
                    } else {
                        pkgs.add(pkg);
                    }
                }
                lastPublishedAt = publishedAt;
            }
        }

        

        LOGGER.debug("Successfully fetched {} packages from {}", pkgs.size(), PACKAGE_URL);
        return pkgs;
    }
}
