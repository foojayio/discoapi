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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.HashAlgorithm;
import eu.hansolo.jdktools.LibCType;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.SignatureType;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.Feature;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.Architecture.AARCH64;
import static eu.hansolo.jdktools.Architecture.X64;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.LINUX_MUSL;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.PackageType.JRE;
import static eu.hansolo.jdktools.ReleaseStatus.EA;
import static eu.hansolo.jdktools.ReleaseStatus.GA;


public class OracleOpenJDK implements Distribution {
    private static final Logger                       LOGGER                     = LoggerFactory.getLogger(OracleOpenJDK.class);

    private static final String                       PACKAGE_URL                = "https://download.java.net/java/";
    private static final String                       JDK_URL                    = "https://jdk.java.net/";
    private static final String                       JDK_ARCHIVE_URL            = "https://jdk.java.net/archive";
    private static final String                       GITHUB_USER                = "AdoptOpenJDK";
    private static final String                       GITHUB_PACKAGE_8_URL       = "https://api.github.com/repos/" + GITHUB_USER + "/openjdk8-upstream-binaries";
    private static final String                       GITHUB_PACKAGE_11_URL      = "https://api.github.com/repos/" + GITHUB_USER + "/openjdk11-upstream-binaries";
    private static final String                       FILENAME_PREFIX            = "openjdk-";
    private static final Pattern                      FILENAME_PREFIX_PATTERN    = Pattern.compile("OpenJDK(8|11)U-");
    private static final Matcher                      FILENAME_PREFIX_MATCHER    = FILENAME_PREFIX_PATTERN.matcher("");
    private static final Pattern                      BUILD_NUMBER_PATTERN       = Pattern.compile("\\/([0-9]{1,3})\\/GPL\\/");
    private static final Matcher                      BUILD_NUMBER_MATCHER       = BUILD_NUMBER_PATTERN.matcher("");

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM         = "";
    private static final String                       OPERATING_SYSTEM_PARAM     = "";
    private static final String                       ARCHIVE_TYPE_PARAM         = "";
    private static final String                       PACKAGE_TYPE_PARAM         = "";
    private static final String                       RELEASE_STATUS_PARAM       = "";
    private static final String                       SUPPORT_TERM_PARAM         = "";
    private static final String                       BITNESS_PARAM              = "";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP           = Map.of(X64, "x64", AARCH64, "aarch64");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP       = Map.of(LINUX_MUSL, "linux", LINUX, "linux", WINDOWS, "windows", MACOS, "osx");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP         = Map.of(EA, "early_access", GA, "GA");

    private static final String                       OPEN_JDK_ARCHIVE_URL       = "https://jdk.java.net/archive/";
    public  static final String                       PKGS_PROPERTIES            = "https://github.com/foojayio/openjdk_releases/raw/main/openjdk.properties";
    private        final Properties                   propertiesPkgs             = new Properties();

    private static final HashAlgorithm                HASH_ALGORITHM             = HashAlgorithm.NONE;
    private static final String                       HASH_URI                   = "";
    private static final SignatureType                SIGNATURE_TYPE             = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM        = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI              = "";
    private static final String                       OFFICIAL_URI               = "https://openjdk.org/";


    public OracleOpenJDK() {
        try {
            HttpResponse<String> response = Helper.get(PKGS_PROPERTIES);
            if (null != response) {
                String propertiesText = response.body();
                if (!propertiesText.isEmpty()) {
                    this.propertiesPkgs.load(new StringReader(propertiesText));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error reading {} properties file from github. {}", getName(), e.getMessage());
        }
    }

    @Override public Distro getDistro() { return Distro.ORACLE_OPEN_JDK; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    public String getGithubPkg8Url() { return GITHUB_PACKAGE_8_URL; }

    public String getGithubPkg11Url() { return GITHUB_PACKAGE_11_URL; }

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
        return List.of("oracle_open_jdk", "ORACLE_OPEN_JDK", "oracle_openjdk", "ORACLE_OPENJDK", "Oracle_OpenJDK", "Oracle OpenJDK", "oracle openjdk", "ORACLE OPENJDK", "open_jdk",
                       "openjdk", "OpenJDK", "Open JDK", "OPEN_JDK", "open-jdk", "OPEN-JDK", "Oracle-OpenJDK", "oracle-openjdk", "ORACLE-OPENJDK", "oracle-open-jdk", "ORACLE-OPEN-JDK");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.ORACLE_OPEN_JDK.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL);
        if (ReleaseStatus.NONE == releaseStatus || GA == releaseStatus) {
            queryBuilder.append(RELEASE_STATUS_MAP.get(GA));
        } else {
            queryBuilder.append(RELEASE_STATUS_MAP.get(EA));
        }
        queryBuilder.append("/");

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    public String getUrlForMoreAvailablePkgs(final VersionNumber versionNumber) {
        StringBuilder queryBuilder = new StringBuilder();

        switch(versionNumber.getFeature().getAsInt()) {
            case 8 : queryBuilder.append(GITHUB_PACKAGE_8_URL).append("/releases"); break;
            case 11: queryBuilder.append(GITHUB_PACKAGE_11_URL).append("/releases"); break;
            default: return "";
        }

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        if (null == jsonObj) {

            TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);
            supTerm = TermOfSupport.MTS == supTerm ? TermOfSupport.STS : supTerm;

            if (OperatingSystem.NONE == operatingSystem) {
                for (OperatingSystem os : OPERATING_SYSTEM_MAP.keySet()) {
                    if (Architecture.NONE == architecture) {
                        for (Architecture arch : ARCHITECTURE_MAP.keySet()) {
                            Pkg pkg = getPkgForOperatingSystem(versionNumber, os, arch, releaseStatus);
                            if (null == pkg) { continue; }
                            pkgs.add(pkg);
                        }
                    } else {
                        Pkg pkg = getPkgForOperatingSystem(versionNumber, os, architecture, releaseStatus);
                        if (null == pkg) { continue; }
                        pkgs.add(pkg);
                    }
                }
            } else {
                Pkg pkg = getPkgForOperatingSystem(versionNumber, operatingSystem, architecture, releaseStatus);
                if (null != pkg) { pkgs.add(pkg); }
            }
        } else {
            // For 8 and 11 use the openjdk upstream repos from AdoptOpenJDK on github
            TermOfSupport supTerm = null;
            if (!versionNumber.getFeature().isEmpty()) {
                supTerm = Helper.getTermOfSupport(versionNumber, Distro.ORACLE_OPEN_JDK);
            }

            JsonArray assets = jsonObj.getAsJsonArray("assets");
            for (JsonElement element : assets) {
                JsonObject assetJsonObj = element.getAsJsonObject();
                String     filename     = assetJsonObj.get("name").getAsString();

                if (filename.contains("debuginfo") || filename.contains("sources") || filename.contains("static-libs") || filename.contains("testimage") || filename.endsWith("sign")) { continue; }

                String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(filename).replaceAll("");

                String[] nameParts = withoutPrefix.split("_");

                VersionNumber vNumber = VersionNumber.fromText(nameParts[3]);

                if (latest) {
                    if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
                } else {
                    if (!versionNumber.equals(vNumber)) { return pkgs; }
                }

                String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

                if (onlyNewPkgs) {
                    if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
                }

                Pkg pkg = new Pkg();

                ArchiveType ext = ArchiveType.getFromFileName(filename);
                if (ArchiveType.SRC_TAR == ext || (ArchiveType.NONE != archiveType && ext != archiveType)) { continue; }
                pkg.setArchiveType(ext);

                if (null == supTerm) { supTerm = Helper.getTermOfSupport(versionNumber, Distro.ORACLE_OPEN_JDK); }
                pkg.setTermOfSupport(supTerm);

                pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
                pkg.setFileName(filename);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setVersionNumber(vNumber);
                pkg.setJavaVersion(vNumber);
                pkg.setDistributionVersion(vNumber);
                pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));


                PackageType packageTypeFound = Constants.PACKAGE_TYPE_LOOKUP.entrySet()
                                                                            .stream()
                                                                            .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                            .findFirst()
                                                                            .map(Entry::getValue)
                                                                            .orElse(PackageType.NONE);

                switch (packageType) {
                    case NONE:
                        pkg.setPackageType(packageTypeFound);
                        break;
                    case JDK:
                        if (packageTypeFound != JDK) { continue; }
                        pkg.setPackageType(JDK);
                        break;
                    case JRE:
                        if (packageTypeFound != JRE) { continue; }
                        pkg.setPackageType(JRE);
                        break;
                }

                ReleaseStatus releaseStatusFound = Constants.RELEASE_STATUS_LOOKUP.entrySet()
                                                                                  .stream()
                                                                                  .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                                  .findFirst()
                                                                                  .map(Entry::getValue)
                                                                                  .orElse(ReleaseStatus.GA);

                switch (releaseStatus) {
                    case NONE:
                        pkg.setReleaseStatus(releaseStatusFound);
                        break;
                    case GA:
                        if (releaseStatusFound != GA) { continue; }
                        pkg.setReleaseStatus(GA);
                        break;
                    case EA:
                        if (releaseStatusFound != EA) { continue; }
                        pkg.setReleaseStatus(EA);
                        break;
                }

                Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                 .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                 .findFirst()
                                                                 .map(Entry::getValue)
                                                                 .orElse(Architecture.NONE);
                if (Architecture.NONE == arch) {
                    LOGGER.debug("Architecture not found in {} for filename: {}", getName(), filename);
                    continue;
                }

                pkg.setArchitecture(arch);
                pkg.setBitness(arch.getBitness());

                OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                      .findFirst()
                                                                      .map(Entry::getValue)
                                                                      .orElse(OperatingSystem.NONE);
                if (OperatingSystem.NONE == os) {
                    switch (pkg.getArchiveType()) {
                        case DEB:
                        case RPM:
                        case TAR_GZ:
                            os = OperatingSystem.LINUX;
                            break;
                        case MSI:
                        case ZIP:
                            os = OperatingSystem.WINDOWS;
                            break;
                        case DMG:
                        case PKG:
                            os = OperatingSystem.MACOS;
                            break;
                    }
                }
                if (OperatingSystem.NONE == os) {
                    LOGGER.debug("Operating System not found in {} for filename: {}", getName(), filename);
                    continue;
                }
                pkg.setOperatingSystem(os);

                pkg.setFreeUseInProduction(Boolean.TRUE);

                pkg.setSize(Helper.getFileSize(downloadLink));

                pkgs.add(pkg);
            }
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }

    public List<Pkg> getAllPkgs(final JsonArray jsonArray, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObj = jsonArray.get(i).getAsJsonObject();
            JsonArray assets = jsonObj.getAsJsonArray("assets");
            for (JsonElement element : assets) {
                JsonObject assetJsonObj = element.getAsJsonObject();
                String     filename     = assetJsonObj.get("name").getAsString();

                if (filename.contains("debuginfo") || filename.contains("sources") || filename.contains("static-libs") || filename.contains("testimage") || filename.endsWith("sign")) { continue; }

                ArchiveType archiveType = ArchiveType.getFromFileName(filename);
                if (ArchiveType.SRC_TAR == archiveType) { continue; }

                String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(filename).replaceAll("");
                String withoutSuffix = withoutPrefix.replaceAll(archiveType.getFileEndings().get(0), "");

                String[] nameParts = withoutSuffix.split("_");
                if (!nameParts[0].equals("jre") && !nameParts[0].equals("jdk")) { continue; }

                VersionNumber vNumber = VersionNumber.fromText(nameParts[3]);

                if (vNumber.getBuild().isEmpty() && nameParts.length >= 5) {
                    int buildNumber = Helper.getPositiveIntFromText(nameParts[4]);
                    if (buildNumber > 0) { vNumber.setBuild(buildNumber); }
                }

                String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

                if (onlyNewPkgs) {
                    if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
                }

                Pkg pkg = new Pkg();

                pkg.setArchiveType(archiveType);

                TermOfSupport supTerm = null;
                if (!vNumber.getFeature().isEmpty()) {
                    supTerm = Helper.getTermOfSupport(vNumber, Distro.ORACLE_OPEN_JDK);
                }
                pkg.setTermOfSupport(supTerm);

                pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
                pkg.setFileName(filename);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setVersionNumber(vNumber);
                pkg.setJavaVersion(vNumber);
                pkg.setDistributionVersion(vNumber);
                pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));

                ReleaseStatus releaseStatus = Constants.RELEASE_STATUS_LOOKUP.entrySet()
                                                                             .stream()
                                                                             .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                             .findFirst()
                                                                             .map(Entry::getValue)
                                                                             .orElse(ReleaseStatus.GA);
                pkg.setReleaseStatus(releaseStatus);

                PackageType packageType = Constants.PACKAGE_TYPE_LOOKUP.entrySet()
                                                                       .stream()
                                                                       .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                       .findFirst()
                                                                       .map(Entry::getValue)
                                                                       .orElse(PackageType.NONE);
                if (PackageType.NONE == packageType) {
                    LOGGER.debug("Package Type not found in {} for filename: {}", getName(), filename);
                    continue;
                }
                pkg.setPackageType(packageType);

                Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                 .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                 .findFirst()
                                                                 .map(Entry::getValue)
                                                                 .orElse(Architecture.NONE);
                if (Architecture.NONE == arch) {
                    LOGGER.debug("Architecture not found in {} for filename: {}", getName(), filename);
                    continue;
                }

                pkg.setArchitecture(arch);
                pkg.setBitness(arch.getBitness());

                OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                      .findFirst()
                                                                      .map(Entry::getValue)
                                                                      .orElse(OperatingSystem.NONE);
                if (OperatingSystem.NONE == os) {
                    switch (pkg.getArchiveType()) {
                        case DEB:
                        case RPM:
                        case TAR_GZ:
                            os = OperatingSystem.LINUX;
                            break;
                        case MSI:
                        case ZIP:
                            os = OperatingSystem.WINDOWS;
                            break;
                        case DMG:
                        case PKG:
                            os = OperatingSystem.MACOS;
                            break;
                    }
                }
                if (OperatingSystem.NONE == os) {
                    LOGGER.debug("Operating System not found in {} for filename: {}", getName(), filename);
                    continue;
                }
                pkg.setOperatingSystem(os);

                pkg.setFreeUseInProduction(Boolean.TRUE);

                pkg.setSize(Helper.getFileSize(downloadLink));

                pkgs.add(pkg);
            }
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromJavaDotNet(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new CopyOnWriteArrayList<>();

        // Get packages from archive
        try {
            HttpResponse<String> response = Helper.get(JDK_ARCHIVE_URL);
            if (null != response) {
                String html = response.body();
                if (!html.isEmpty()) {
                    pkgs.addAll(extractPackagesFromHtml(html, false, onlyNewPkgs));
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error fetching packages from {} archive url. {}", getName(), e.getMessage());
        }

        // Get packages from latest 3 versions
        int latestMajorVersion = CacheManager.INSTANCE.getMajorVersions().stream().max(Comparator.comparing(MajorVersion::getAsInt)).get().getAsInt();
        for (int i = latestMajorVersion ; i > latestMajorVersion - 3 ; i--) {
            String jdkUrl = JDK_URL + i + "/";
            boolean isReleaseCandidate = false;
            try {
                HttpResponse<String> response = Helper.get(jdkUrl);
                if (null != response) {
                    String html = response.body();
                    if (!html.isEmpty()) {
                        isReleaseCandidate = html.contains("Release-Candidate");
                        pkgs.addAll(extractPackagesFromHtml(html, isReleaseCandidate, onlyNewPkgs));
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Error fetching packages from {} url {}. {}", getName(), jdkUrl, e.getMessage());
            }
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }

    public Collection<Pkg> getAllPkgs(final boolean onlyNewPkgs) {
        Map<String, Pkg> pkgMap = new HashMap<>();

        // Get all pkgs from jdk.java.not and jdk.java.net/archive
        getAllPkgsFromJavaDotNet(onlyNewPkgs).forEach(pkg -> pkgMap.put(pkg.getId(), pkg));

        // Reload openjdk properties
        try {
            final HttpResponse<String> response = Helper.get(PKGS_PROPERTIES);
            if (null == response) {
                LOGGER.debug("No jdk properties found for {}", getName());
                return pkgMap.values();
            }
            final String propertiesText = response.body();
            if (propertiesText.isEmpty()) {
                LOGGER.debug("jdk properties are empty for {}", getName());
                return pkgMap.values();
            }
            propertiesPkgs.load(new StringReader(propertiesText));
        } catch (Exception e) {
            LOGGER.error("Error reading jdk properties file for {} from github. {}", getName(), e.getMessage());
        }

        propertiesPkgs.forEach((key, value) -> {
            String downloadLink = value.toString();
            String filename     = Helper.getFileNameFromText(downloadLink);
            boolean isMusl      = false;

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { return; }
            }

            Pkg pkg = new Pkg();

            ArchiveType ext = ArchiveType.getFromFileName(filename);
            pkg.setArchiveType(ext);

            ReleaseStatus rs = Constants.RELEASE_STATUS_LOOKUP.entrySet().stream()
                                                              .filter(entry -> downloadLink.contains(entry.getKey()))
                                                              .findFirst()
                                                              .map(Entry::getValue)
                                                              .orElse(ReleaseStatus.NONE);
            if (ReleaseStatus.NONE == rs) {
                LOGGER.debug("Release Status not found in {} for downloadLink: {}", getName(), downloadLink);
            } else {
                pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
                pkg.setFileName(filename);
                pkg.setDirectDownloadUri(downloadLink);

                Architecture arch;
                String[]     keyParts     = key.toString().split("-");
                int          noOfKeyParts = keyParts.length;
                if (keyParts[noOfKeyParts - 1].equals("musl")) {
                    arch   = Architecture.fromText(keyParts[noOfKeyParts - 2]);
                    isMusl = true;
                } else if (keyParts[noOfKeyParts - 1].equals(Feature.PANAMA.getApiString())) {
                    arch   = Architecture.fromText(keyParts[noOfKeyParts - 2]);
                    pkg.getFeatures().add(Feature.PANAMA);
                } else if (keyParts[noOfKeyParts - 1].equals(Feature.LOOM.getApiString())) {
                    arch   = Architecture.fromText(keyParts[noOfKeyParts - 2]);
                    pkg.getFeatures().add(Feature.LOOM);
                } else if (keyParts[noOfKeyParts - 1].equals(Feature.LANAI.getApiString())) {
                    arch   = Architecture.fromText(keyParts[noOfKeyParts - 2]);
                    pkg.getFeatures().add(Feature.LANAI);
                } else if (keyParts[noOfKeyParts - 1].equals(Feature.VALHALLA.getApiString())) {
                    arch   = Architecture.fromText(keyParts[noOfKeyParts - 2]);
                    pkg.getFeatures().add(Feature.VALHALLA);
                } else {
                    arch = Architecture.fromText(keyParts[noOfKeyParts - 1]);
                }

                pkg.setArchitecture(arch);
                pkg.setBitness(arch.getBitness());

                VersionNumber versionNumber = VersionNumber.fromText(filename);

                BUILD_NUMBER_MATCHER.reset(downloadLink);
                while(BUILD_NUMBER_MATCHER.find()) {
                    if (BUILD_NUMBER_MATCHER.groupCount() > 0) {
                        try {
                            Integer buildNo = Integer.valueOf(BUILD_NUMBER_MATCHER.group(1));
                            if (versionNumber.getBuild().isEmpty()) {
                                    versionNumber.setBuild(buildNo);
                                }
                        } catch (NumberFormatException e) {
                            LOGGER.debug("Error parsing Oracle OpenJDK build number: {}", BUILD_NUMBER_MATCHER.group(1));
                        }
                    }
                }

                pkg.setVersionNumber(versionNumber);
                pkg.setJavaVersion(versionNumber);
                pkg.setDistributionVersion(versionNumber);
                pkg.setJdkVersion(new MajorVersion(versionNumber.getFeature().getAsInt()));

                Helper.setTermOfSupport(versionNumber, pkg);

                pkg.setPackageType(PackageType.JDK);

                pkg.setReleaseStatus(rs);

                OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> filename.contains(entry.getKey()))
                                                                      .findFirst()
                                                                      .map(Entry::getValue)
                                                                      .orElse(OperatingSystem.NONE);
                pkg.setOperatingSystem(os);
                    switch (os) {
                    case WINDOWS: pkg.setLibCType(LibCType.C_STD_LIB); break;
                    case LINUX  : pkg.setLibCType(isMusl ? LibCType.MUSL : LibCType.GLIBC); break;
                    case MACOS  : pkg.setLibCType(LibCType.LIBC); break;
                    }

                String[] fileNameParts = filename.split("_");
                if (fileNameParts.length > 1) {
                    String versionText = fileNameParts[0].replace("openjdk", "").replaceFirst("-", "");
                    Semver semVer = Semver.fromText(versionText).getSemver1();
                    if (null != semVer) {
                        pkg.setReleaseStatus(semVer.getReleaseStatus());
                    }
                }
                if (keyParts.length > 1 && keyParts[1].equals("rc")) {
                    pkg.setReleaseStatus(EA);
                }

                pkg.setFreeUseInProduction(Boolean.TRUE);

                pkg.setSize(Helper.getFileSize(downloadLink));

                if (!pkgMap.containsKey(pkg)) { pkgMap.put(pkg.getId(), pkg); }
            }
        });

        List<Pkg> pkgs = new ArrayList<>(pkgMap.values());
        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }

    private Pkg getPkgForOperatingSystem(final VersionNumber versionNumber, final OperatingSystem operatingSystem,
                                         final Architecture architecture, final ReleaseStatus releaseStatus) {
        StringBuilder keyBuilder = new StringBuilder().append(versionNumber.getFeature().getAsInt()).append("-")
                                                      .append(OPERATING_SYSTEM_MAP.get(operatingSystem)).append("-")
                                                      .append(ARCHITECTURE_MAP.get(architecture));
        if (LINUX_MUSL == operatingSystem) { keyBuilder.append("-musl"); }

        String downloadLink = propertiesPkgs.getProperty(keyBuilder.toString());

        OptionalInt latestGAOpt = Helper.getLatestGA();
        int         latestGA    = latestGAOpt.isPresent() ? latestGAOpt.getAsInt() : MajorVersion.getLatest(false).getAsInt();
        if (null == downloadLink && latestGA < versionNumber.getMajorVersion().getAsInt()) {
            // Might be Release Candidate
            keyBuilder.setLength(0);
            keyBuilder = new StringBuilder().append(versionNumber.getFeature().getAsInt()).append("-")
                                            .append("rc").append("-")
                                            .append(OPERATING_SYSTEM_MAP.get(operatingSystem)).append("-")
                                            .append(ARCHITECTURE_MAP.get(architecture));
            if (LINUX_MUSL == operatingSystem) { keyBuilder.append("-musl"); }
            downloadLink = propertiesPkgs.getProperty(keyBuilder.toString());
        }

        String fileName     = Helper.getFileNameFromText(downloadLink);

        if (null == downloadLink) { return null; }

        String directDownloadLink = downloadLink;

        Pkg pkg = new Pkg();

        ArchiveType ext = ArchiveType.getFromFileName(fileName);
        pkg.setArchiveType(ext);

        ReleaseStatus rs = Constants.RELEASE_STATUS_LOOKUP.entrySet().stream()
                                                          .filter(entry -> directDownloadLink.contains(entry.getKey()))
                                                          .findFirst()
                                                          .map(Entry::getValue)
                                                          .orElse(ReleaseStatus.NONE);
        if (ReleaseStatus.NONE == rs) {
            LOGGER.debug("Release Status not found in {} for downloadLink: {}", getName(), downloadLink);
            return null;
        }

        pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
        pkg.setFileName(fileName);
        pkg.setDirectDownloadUri(downloadLink);

        Architecture arch;
        String[] keyParts = keyBuilder.toString().split("-");
        int noOfKeyParts = keyParts.length;
        if (keyParts[noOfKeyParts - 1].equals("musl")) {
            arch = Architecture.fromText(keyParts[noOfKeyParts - 2]);
        } else {
            arch = Architecture.fromText(keyParts[noOfKeyParts - 1]);
        }
        pkg.setArchitecture(arch);
        pkg.setBitness(arch.getBitness());

        if (null == fileName || fileName.isEmpty()) { return null; }
        VersionNumber vNumber = VersionNumber.fromText(fileName);
        pkg.setVersionNumber(vNumber);
        pkg.setJavaVersion(vNumber);
        pkg.setDistributionVersion(vNumber);
        pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));

        Helper.setTermOfSupport(versionNumber, pkg);

        pkg.setPackageType(PackageType.JDK);

        pkg.setReleaseStatus(rs);
        pkg.setOperatingSystem(operatingSystem);

        pkg.setFreeUseInProduction(Boolean.TRUE);

        pkg.setSize(Helper.getFileSize(downloadLink));

        return pkg;
    }

    private List<Pkg> extractPackagesFromHtml(final String html, final boolean isReleaseCandidate, final boolean onlyNewPkgs) {
        final List<Pkg> pkgs      = new ArrayList<>();
        List<String>    fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));
        for (String href : fileHrefs) {
            String          filename        = Helper.getFileNameFromText(href);
            String[]        nameParts       = filename.split("_");
            VersionNumber   versionNumber   = VersionNumber.fromText(nameParts[0].replace(FILENAME_PREFIX, ""));
            String[]        osArchParts     = nameParts[1].split("-");
            OperatingSystem operatingSystem = OperatingSystem.fromText(osArchParts[0]);
            Architecture    architecture    = Architecture.fromText(osArchParts[1]);
            boolean         isMusl          = (osArchParts.length > 2 && osArchParts[2].equals("musl"));
            Bitness         bitness         = architecture.getBitness();
            ArchiveType     archiveType     = Helper.getFileEnding(filename);
            TermOfSupport   termOfSupport   = Helper.getTermOfSupport(versionNumber);
            ReleaseStatus   releaseStatus   = (href.contains("/GA/") || href.contains("/ga/")) ? ReleaseStatus.GA : ReleaseStatus.EA;
            if (isReleaseCandidate) { releaseStatus = EA; }
            String          downloadLink    = href.replaceAll("\"", "").replace("href=", "");
            String          checksumUri     = Helper.isUriValid(downloadLink + ".sha256") ? downloadLink + ".sha256" : "";

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
            }

            BUILD_NUMBER_MATCHER.reset(downloadLink);
            while(BUILD_NUMBER_MATCHER.find()) {
                if (BUILD_NUMBER_MATCHER.groupCount() > 0) {
                    try {
                        Integer buildNo = Integer.valueOf(BUILD_NUMBER_MATCHER.group(1));
                        if (versionNumber.getBuild().isEmpty()) {
                        versionNumber.setBuild(buildNo);
                            }
                    } catch (NumberFormatException e) {
                        LOGGER.debug("Error parsing Oracle OpenJDK build number: {}", BUILD_NUMBER_MATCHER.group(1));
                    }
                }
            }

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
            pkg.setVersionNumber(versionNumber);
            pkg.setJavaVersion(versionNumber);
            pkg.setDistributionVersion(versionNumber);
            pkg.setJdkVersion(new MajorVersion(versionNumber.getFeature().getAsInt()));
            pkg.setPackageType(PackageType.JDK);
            pkg.setArchitecture(architecture);
            pkg.setBitness(bitness);
            pkg.setOperatingSystem(operatingSystem);
            switch (operatingSystem) {
                case WINDOWS: pkg.setLibCType(LibCType.C_STD_LIB); break;
                case LINUX  : pkg.setLibCType(isMusl ? LibCType.MUSL : LibCType.GLIBC); break;
                case MACOS  : pkg.setLibCType(LibCType.LIBC); break;
            }
            pkg.setReleaseStatus(releaseStatus);
            pkg.setTermOfSupport(termOfSupport);
            pkg.setFileName(filename);
            pkg.setArchiveType(archiveType);
            pkg.setDirectDownloadUri(downloadLink);
            pkg.setJavaFXBundled(versionNumber.getFeature().getAsInt() <= 10);
            if (!checksumUri.isEmpty()) {
                pkg.setChecksumUri(checksumUri);
                pkg.setChecksumType(HashAlgorithm.SHA256);
            }

            pkg.setFreeUseInProduction(Boolean.TRUE);
            pkg.setSize(Helper.getFileSize(downloadLink));
            pkgs.add(pkg);
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }
}
