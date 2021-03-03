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

package io.foojay.api.distribution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.HashAlgorithm;
import io.foojay.api.pkg.LibCType;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.Architecture.AARCH64;
import static io.foojay.api.pkg.Architecture.X64;
import static io.foojay.api.pkg.OperatingSystem.LINUX;
import static io.foojay.api.pkg.OperatingSystem.LINUX_MUSL;
import static io.foojay.api.pkg.OperatingSystem.MACOS;
import static io.foojay.api.pkg.OperatingSystem.WINDOWS;
import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.PackageType.JRE;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;


public class OracleOpenJDK implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(OracleOpenJDK.class);

    private static final String                       PACKAGE_URL             = "https://download.java.net/java/";
    private static final String                       GITHUB_USER             = "AdoptOpenJDK";
    private static final String                       GITHUB_PACKAGE_8_URL    = "https://api.github.com/repos/" + GITHUB_USER + "/openjdk8-upstream-binaries";
    private static final String                       GITHUB_PACKAGE_11_URL   = "https://api.github.com/repos/" + GITHUB_USER + "/openjdk11-upstream-binaries";
    private static final String                       FILENAME_PREFIX         = "openjdk-";
    private static final Pattern                      FILENAME_PREFIX_PATTERN = Pattern.compile("OpenJDK(8|11)U-");
    private static final Matcher                      FILENAME_PREFIX_MATCHER = FILENAME_PREFIX_PATTERN.matcher("");

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM      = "";
    private static final String                       OPERATING_SYSTEM_PARAM  = "";
    private static final String                       ARCHIVE_TYPE_PARAM      = "";
    private static final String                       PACKAGE_TYPE_PARAM      = "";
    private static final String                       RELEASE_STATUS_PARAM    = "";
    private static final String                       SUPPORT_TERM_PARAM      = "";
    private static final String                       BITNESS_PARAM           = "";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP        = Map.of(X64, "x64", AARCH64, "aarch64");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP    = Map.of(LINUX_MUSL, "linux", LINUX, "linux", WINDOWS, "windows", MACOS, "osx");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP      = Map.of(EA, "early_access", GA, "GA");

    private static final String                       OPEN_JDK_ARCHIVE_URL    = "https://jdk.java.net/archive/";
    public  static final String                       OPEN_JDK_PKGS_PROPERTIES   = "https://github.com/foojay2020/openjdk_releases/raw/main/openjdk.properties";
    public  static final String                       OPEN_JDK_HASHES_PROPERTIES = "https://github.com/foojay2020/openjdk_releases/raw/main/openjdk-hashes.properties";
    private        final Properties                   propertiesPkgs             = new Properties();
    private        final Properties                   propertiesHashes           = new Properties();


    public OracleOpenJDK() {
        try {
            this.propertiesPkgs.load(new StringReader(Helper.getTextFromUrl(OPEN_JDK_PKGS_PROPERTIES)));
        } catch (Exception e) {
            LOGGER.error("Error reading OpenJDK properties file from github. {}", e.getMessage());
        }
        try {
            this.propertiesHashes.load(new StringReader(Helper.getTextFromUrl(OPEN_JDK_HASHES_PROPERTIES)));
        } catch (Exception e) {
            LOGGER.error("Error reading OpenJDK hashes properties file from github. {}", e.getMessage());
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


    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.ORACLE_OPEN_JDK.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
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
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
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
                String     fileName     = assetJsonObj.get("name").getAsString();

                if (fileName.contains("debuginfo") || fileName.contains("sources") || fileName.contains("static-libs") || fileName.contains("testimage") || fileName.endsWith("sign")) { continue; }

                String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(fileName).replaceAll("");

                String[] nameParts = withoutPrefix.split("_");

                VersionNumber vNumber = VersionNumber.fromText(nameParts[3]);

                if (latest) {
                    if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
                } else {
                    if (!versionNumber.equals(vNumber)) { return pkgs; }
                }

                String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

                Pkg pkg = new Pkg();

                ArchiveType ext = ArchiveType.getFromFileName(fileName);
                if (ArchiveType.SRC_TAR == ext || (ArchiveType.NONE != archiveType && ext != archiveType)) { continue; }
                pkg.setArchiveType(ext);

                if (null == supTerm) { supTerm = Helper.getTermOfSupport(versionNumber, Distro.ORACLE_OPEN_JDK); }
                pkg.setTermOfSupport(supTerm);

                pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
                pkg.setFileName(fileName);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setVersionNumber(vNumber);
                pkg.setJavaVersion(vNumber);
                pkg.setDistributionVersion(vNumber);


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
                    LOGGER.debug("Architecture not found in Oracle OpenJDK for filename: {}", fileName);
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
                    LOGGER.debug("Operating System not found in Oracle OpenJDK for filename: {}", fileName);
                    continue;
                }
                pkg.setOperatingSystem(os);

                pkgs.add(pkg);
            }
        }

        return pkgs;
    }

    public List<Pkg> getAllPkgs(final JsonArray jsonArray) {
        List<Pkg> pkgs = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObj = jsonArray.get(i).getAsJsonObject();
            JsonArray assets = jsonObj.getAsJsonArray("assets");
            for (JsonElement element : assets) {
                JsonObject assetJsonObj = element.getAsJsonObject();
                String     fileName     = assetJsonObj.get("name").getAsString();

                if (fileName.contains("debuginfo") || fileName.contains("sources") || fileName.contains("static-libs") || fileName.contains("testimage") || fileName.endsWith("sign")) { continue; }

                String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(fileName).replaceAll("");

                String[] nameParts = withoutPrefix.split("_");
                if (!nameParts[0].equals("jre") && !nameParts[0].equals("jdk")) { continue; }

                VersionNumber vNumber = VersionNumber.fromText(nameParts[3]);

                String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

                Pkg pkg = new Pkg();

                ArchiveType ext = ArchiveType.getFromFileName(fileName);
                if (ArchiveType.SRC_TAR == ext) { continue; }
                pkg.setArchiveType(ext);

                TermOfSupport supTerm = null;
                if (!vNumber.getFeature().isEmpty()) {
                    supTerm = Helper.getTermOfSupport(vNumber, Distro.ORACLE_OPEN_JDK);
                }
                pkg.setTermOfSupport(supTerm);

                pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
                pkg.setFileName(fileName);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setVersionNumber(vNumber);
                pkg.setJavaVersion(vNumber);
                pkg.setDistributionVersion(vNumber);

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
                    LOGGER.debug("Package Type not found in Oracle OpenJDK for filename: {}", fileName);
                    continue;
                }
                pkg.setPackageType(packageType);

                Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                 .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                 .findFirst()
                                                                 .map(Entry::getValue)
                                                                 .orElse(Architecture.NONE);
                if (Architecture.NONE == arch) {
                    LOGGER.debug("Architecture not found in Oracle OpenJDK for filename: {}", fileName);
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
                    LOGGER.debug("Operating System not found in Oracle OpenJDK for filename: {}", fileName);
                    continue;
                }
                pkg.setOperatingSystem(os);

                pkgs.add(pkg);
            }
        }

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }
        List<String> fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));

        for (String href : fileHrefs) {
            if (href.contains("/GA/") || href.contains("/ga/")) {
                String          filename        = Helper.getFileNameFromText(href);
                String[]        nameParts       = filename.split("_");
                VersionNumber   versionNumber   = VersionNumber.fromText(nameParts[0].replace(FILENAME_PREFIX, ""));
                String[]        osArchParts     = nameParts[1].split("-");
                OperatingSystem operatingSystem = OperatingSystem.fromText(osArchParts[0]);
                Architecture    architecture    = Architecture.fromText(osArchParts[1]);
                Bitness         bitness         = architecture.getBitness();
                ArchiveType     archiveType     = Helper.getFileEnding(filename);
                TermOfSupport   termOfSupport   = Helper.getTermOfSupport(versionNumber);
                String          downloadLink    = href.replaceAll("\"", "").replace("href=", "");

                Pkg pkg = new Pkg();
                pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
                pkg.setVersionNumber(versionNumber);
                pkg.setJavaVersion(versionNumber);
                pkg.setDistributionVersion(versionNumber);
                pkg.setPackageType(PackageType.JDK);
                pkg.setArchitecture(architecture);
                pkg.setBitness(bitness);
                pkg.setOperatingSystem(operatingSystem);
                pkg.setReleaseStatus(ReleaseStatus.GA);
                pkg.setTermOfSupport(termOfSupport);
                pkg.setFileName(filename);
                pkg.setArchiveType(archiveType);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setJavaFXBundled(false);

                pkgs.add(pkg);
            }
        }

        return pkgs;
    }

    public List<Pkg> getAllPkgs() {
        List<Pkg> pkgs = new ArrayList<>();

        // Reload openjdk properties
        try {
            propertiesPkgs.load(new StringReader(Helper.getTextFromUrl(OPEN_JDK_PKGS_PROPERTIES)));
        } catch (Exception e) {
            LOGGER.error("Error reading OpenJDK properties file from github. {}", e.getMessage());
        }

        try {
            propertiesHashes.load(new StringReader(Helper.getTextFromUrl(OPEN_JDK_HASHES_PROPERTIES)));
        } catch (Exception e) {
            LOGGER.error("Error reading OpenJDK hashes properties file from github. {}", e.getMessage());
        }

        propertiesPkgs.forEach((key, value) -> {
            String downloadLink = value.toString();
            String fileName     = Helper.getFileNameFromText(downloadLink);
            boolean isMusl      = false;

            Pkg pkg = new Pkg();

            ArchiveType ext = ArchiveType.getFromFileName(fileName);
            pkg.setArchiveType(ext);

            ReleaseStatus rs = Constants.RELEASE_STATUS_LOOKUP.entrySet().stream()
                                                              .filter(entry -> downloadLink.contains(entry.getKey()))
                                                              .findFirst()
                                                              .map(Entry::getValue)
                                                              .orElse(ReleaseStatus.NONE);
            if (ReleaseStatus.NONE == rs) {
                LOGGER.debug("Releas Status not found in Oracle OpenJDK for downloadLink: {}", downloadLink);
            } else {
                pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
                pkg.setFileName(fileName);
                pkg.setDirectDownloadUri(downloadLink);

                Architecture arch;
                String[]     keyParts     = key.toString().split("-");
                int          noOfKeyParts = keyParts.length;
                if (keyParts[noOfKeyParts - 1].equals("musl")) {
                    arch   = Architecture.fromText(keyParts[noOfKeyParts - 2]);
                    isMusl = true;
                } else {
                    arch = Architecture.fromText(keyParts[noOfKeyParts - 1]);
                }
                pkg.setArchitecture(arch);
                pkg.setBitness(arch.getBitness());

                VersionNumber vNumber = VersionNumber.fromText(fileName);
                pkg.setVersionNumber(vNumber);
                pkg.setJavaVersion(vNumber);
                pkg.setDistributionVersion(vNumber);

                Helper.setTermOfSupport(vNumber, pkg);

                pkg.setPackageType(PackageType.JDK);

                pkg.setReleaseStatus(rs);

                OperatingSystem os;
                String[]        fileNameParts = fileName.split("_");
                if (fileNameParts.length > 1) {
                    String[] osArchParts = fileNameParts[1].split("-");
                    os = OperatingSystem.fromText(osArchParts[0]);
                    pkg.setOperatingSystem(os);

                    switch (os) {
                        case WINDOWS:
                            pkg.setLibCType(LibCType.C_STD_LIB);
                            break;
                        case LINUX:
                            pkg.setLibCType(LibCType.GLIBC);
                            break;
                        case MACOS:
                            pkg.setLibCType(LibCType.LIBC);
                            break;
                    }
                    if (isMusl) { pkg.setLibCType(LibCType.MUSL); }

                    SemVer semVer = SemVer.fromText(fileNameParts[0].replace("openjdk", "")).getSemVer1();
                    if (null != semVer) {
                        pkg.setReleaseStatus(semVer.getReleaseStatus());
                    }
                }
                if (keyParts.length > 1 && keyParts[1].equals("rc")) {
                    pkg.setReleaseStatus(EA);
                }

                // Set hash
                if (propertiesHashes.containsKey(key)) {
                    String hash = propertiesHashes.getProperty(key.toString(), "");
                    if (null == hash || hash.isEmpty()) {
                        pkg.setHash("");
                        pkg.setHashAlgorithm(HashAlgorithm.NONE);
                    } else {
                        pkg.setHash(hash);
                        pkg.setHashAlgorithm(HashAlgorithm.SHA256);
                    }
                }

                pkgs.add(pkg);
            }
        });

        return pkgs;
    }

    private Pkg getPkgForOperatingSystem(final VersionNumber versionNumber, final OperatingSystem operatingSystem,
                                         final Architecture architecture, final ReleaseStatus releaseStatus) {
        StringBuilder keyBuilder = new StringBuilder().append(versionNumber.getFeature().getAsInt()).append("-")
                                                      .append(OPERATING_SYSTEM_MAP.get(operatingSystem)).append("-")
                                                      .append(ARCHITECTURE_MAP.get(architecture));
        if (LINUX_MUSL == operatingSystem) { keyBuilder.append("-musl"); }

        String downloadLink = propertiesPkgs.getProperty(keyBuilder.toString());

        if (null == downloadLink && MajorVersion.getLatest(false).getAsInt() < versionNumber.getMajorVersion().getAsInt()) {
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
            LOGGER.debug("Release Status not found in Oracle OpenJDK for downloadLink: {}", downloadLink);
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

        Helper.setTermOfSupport(versionNumber, pkg);

        pkg.setPackageType(PackageType.JDK);

        pkg.setReleaseStatus(rs);
        pkg.setOperatingSystem(operatingSystem);

        // Set hash
        String key = keyBuilder.toString();
        if (propertiesHashes.containsKey(key)) {
            String hash = propertiesHashes.getProperty(key, "");
            if (null == hash || hash.isEmpty()) {
                pkg.setHash("");
                pkg.setHashAlgorithm(HashAlgorithm.NONE);
            } else {
                pkg.setHash(hash);
                pkg.setHashAlgorithm(HashAlgorithm.SHA256);
            }
        }

        return pkg;
    }
}
