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

import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.HashAlgorithm;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.SignatureType;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;


public class Oracle implements Distribution {
    private static final Logger                       LOGGER                  = LoggerFactory.getLogger(Oracle.class);

    private static final String                       PACKAGE_URL             = "";
    public  static final String                       PKGS_PROPERTIES         = "https://github.com/foojayio/openjdk_releases/raw/main/oracle.properties";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM      = "";
    private static final String                       OPERATING_SYSTEM_PARAM  = "";
    private static final String                       ARCHIVE_TYPE_PARAM      = "";
    private static final String                       PACKAGE_TYPE_PARAM      = "";
    private static final String                       RELEASE_STATUS_PARAM    = "";
    private static final String                       TERM_OF_SUPPORT_PARAM   = "";
    private static final String                       BITNESS_PARAM           = "";

    private static final HashAlgorithm                HASH_ALGORITHM          = HashAlgorithm.NONE;
    private static final String                       HASH_URI                = "";
    private static final SignatureType                SIGNATURE_TYPE          = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM     = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI           = "";
    private static final String                       OFFICIAL_URI            = "https://www.oracle.com/java/";

    // Mappings for url parameters
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP      = Map.of(EA, "early_access", GA, "GA");


    @Override public Distro getDistro() { return Distro.ORACLE; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return TERM_OF_SUPPORT_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }

    @Override public HashAlgorithm getHashAlgorithm() { return HASH_ALGORITHM; }

    @Override public String getHashUri() { return HASH_URI; }

    @Override public SignatureType getSignatureType() { return SIGNATURE_TYPE; }

    @Override public HashAlgorithm getSignatureAlgorithm() { return SIGNATURE_ALGORITHM; }

    @Override public String getSignatureUri() { return SIGNATURE_URI; }

    @Override public String getOfficialUri() { return OFFICIAL_URI; }

    @Override public List<String> getSynonyms() {
        return List.of("oracle", "Oracle", "ORACLE");
    }

    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.ORACLE.get().equals(pkg.getDistribution()))
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

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();

        if (null == jsonObj) {

        }

        return pkgs;
    }

    public List<Pkg> getAllPkgs() {
        List<Pkg> pkgs = new ArrayList<>();

        // Load jdk properties
        List<String> pkgUrls = new ArrayList<>();
        try {
            final Properties           propertiesPkgs = new Properties();
            final HttpResponse<String> response       = Helper.get(PKGS_PROPERTIES);
            if (null == response) {
                LOGGER.debug("No jdk properties found for {}", getName());
                return pkgs;
            }
            final String propertiesText = response.body();
            if (propertiesText.isEmpty()) {
                LOGGER.debug("jdk properties are empty for {}", getName());
                return pkgs;
            }
            propertiesPkgs.load(new StringReader(propertiesText));

            propertiesPkgs.forEach((key, value) -> {
                String pkgUrl = value.toString();
                pkgUrls.add(pkgUrl);
            });
        } catch (Exception e) {
            LOGGER.error("Error reading jdk properties file from github for {}. {}", getName(), e.getMessage());
        }

        try {
            for (String packageUrl : pkgUrls) {
                final HttpResponse<String> response = Helper.get(packageUrl);
                if (null == response) { return pkgs; }
                final String html = response.body();
                if (!html.isEmpty()) {
                    pkgs.addAll(getAllPkgsFromHtml(html, packageUrl));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages of {}. {}", getName(), e);
        }

        // Load all packages from MajorVersion 17 and above
        pkgs.addAll(getAllPkgsFrom17AndAbove());

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html, final String packageUrl) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }

        List<String> fileNames = new ArrayList<>(Helper.getDownloadHrefsFromString(html));
        for (String filename : fileNames) {
            if (filename.contains("-demos") || filename.contains("-p-")) { continue; }
            if (filename.endsWith(".sh") || filename.endsWith("iftw.exe")) { continue; }
            PackageType     packageType;
            String[]        nameParts;
            VersionNumber   versionNumber;
            String[]        osArchParts;
            OperatingSystem operatingSystem;
            Architecture    architecture;
            Bitness         bitness;
            ArchiveType     archiveType;
            TermOfSupport   termOfSupport;
            boolean         javafxBundled;
            if (filename.contains("_") && !filename.contains("javafx")) {
                // > JDK 8
                packageType = filename.startsWith("jdk") ? PackageType.JDK : PackageType.JRE;
                nameParts   = filename.split("_");

                if (filename.startsWith("jdk")) {
                    versionNumber = VersionNumber.fromText(nameParts[0].replace("jdk-", ""));
                } else if (filename.startsWith("jre")) {
                    versionNumber = VersionNumber.fromText(nameParts[0].replace("jre-", ""));
                } else if (filename.startsWith("serverjre")) {
                    versionNumber = VersionNumber.fromText(nameParts[0].replace("serverjre-", ""));
                } else {
                    continue;
                }

                osArchParts     = nameParts[1].split("-");
                operatingSystem = OperatingSystem.fromText(osArchParts[0]);
                architecture    = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                               .filter(entry -> filename.contains(entry.getKey()))
                                                               .findFirst()
                                                               .map(Entry::getValue)
                                                               .orElse(Architecture.NONE);

                bitness     = architecture.getBitness();
                archiveType = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                           .filter(entry -> filename.endsWith(entry.getKey()))
                                                           .findFirst()
                                                           .map(Entry::getValue)
                                                           .orElse(ArchiveType.NONE);
                termOfSupport = Helper.getTermOfSupport(versionNumber);
                javafxBundled = versionNumber.getMajorVersion().getAsInt() < 11;
            } else {
                // <= JDK 8
                packageType = filename.startsWith("jdk") ? PackageType.JDK : PackageType.JRE;
                if (filename.startsWith("jdk")) {
                    nameParts = filename.replace("jdk-", "").split("-");
                } else if (filename.startsWith("jre")) {
                    nameParts = filename.replace("jre-", "").split("-");
                } else if (filename.startsWith("serverjre")) {
                    nameParts = filename.replace("serverjre-", "").split("-");
                } else {
                    continue;
                }

                versionNumber   = VersionNumber.fromText(nameParts[0]);

                operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                   .stream()
                                                                   .filter(entry -> filename.contains(entry.getKey()))
                                                                   .findFirst()
                                                                   .map(Entry::getValue)
                                                                   .orElse(OperatingSystem.NONE);

                architecture    = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                               .filter(entry -> filename.contains(entry.getKey()))
                                                               .findFirst()
                                                               .map(Entry::getValue)
                                                               .orElse(Architecture.NONE);

                bitness     = architecture.getBitness();
                archiveType = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                           .filter(entry -> filename.endsWith(entry.getKey()))
                                                           .findFirst()
                                                           .map(Entry::getValue)
                                                           .orElse(ArchiveType.NONE);
                termOfSupport = Helper.getTermOfSupport(versionNumber);
                if (filename.contains("javafx")) {
                    javafxBundled = true;
                } else if (versionNumber.getMajorVersion().getAsInt() >= 7) {
                    javafxBundled = true;
                } else {
                    javafxBundled = false;
                }
            }
            if (ArchiveType.NONE == archiveType) {
                LOGGER.debug("Archive Type not found in {} for filename: {}", getName(), filename);
                continue;
            }
            if (Architecture.NONE == architecture) {
                LOGGER.debug("Architecture not found in {} for filename: {}", getName(), filename);
                continue;
            }
            if (OperatingSystem.NONE == operatingSystem) {
                LOGGER.debug("Operating System not found in {} for filename: {}", getName(), filename);
                continue;
            }

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.ORACLE.get());
            pkg.setVersionNumber(versionNumber);
            pkg.setJavaVersion(versionNumber);
            pkg.setDistributionVersion(versionNumber);
            pkg.setPackageType(packageType);
            pkg.setArchitecture(architecture);
            pkg.setBitness(bitness);
            pkg.setOperatingSystem(operatingSystem);
            pkg.setReleaseStatus(ReleaseStatus.GA);
            pkg.setTermOfSupport(termOfSupport);
            pkg.setFileName(filename);
            pkg.setArchiveType(archiveType);
            pkg.setDownloadSiteUri(packageUrl);
            pkg.setJavaFXBundled(javafxBundled);
            pkg.setDirectlyDownloadable(false);
            pkg.setFreeUseInProduction(Boolean.FALSE);
            pkg.setTckTested(Boolean.TRUE);

            pkgs.add(pkg);
        }

        return pkgs;
                }

    public List<Pkg> getAllPkgsFrom17AndAbove() {
        final List<Pkg>                                                  pkgs             = new ArrayList<>();
        final String                                                     baseUrl          = "https://download.oracle.com/java/";
        final List<OperatingSystem>                                      operatingSystems = List.of(OperatingSystem.LINUX, OperatingSystem.MACOS, OperatingSystem.WINDOWS);
        final Map<OperatingSystem, Map<Architecture, List<ArchiveType>>> archiveTypeMap   = Map.of(OperatingSystem.LINUX, Map.of(Architecture.AARCH64, List.of(ArchiveType.RPM, ArchiveType.TAR_GZ),
                                                                                                                                 Architecture.X64, List.of(ArchiveType.DEB, ArchiveType.RPM, ArchiveType.TAR_GZ)),
                                                                                                   OperatingSystem.MACOS, Map.of(Architecture.AARCH64, List.of(ArchiveType.DMG, ArchiveType.TAR_GZ),
                                                                                                                                 Architecture.X64, List.of(ArchiveType.DMG, ArchiveType.TAR_GZ)),
                                                                                                   OperatingSystem.WINDOWS, Map.of(Architecture.X64, List.of(ArchiveType.EXE, ArchiveType.MSI, ArchiveType.ZIP)));

        CacheManager.INSTANCE.getMajorVersions()
                             .stream()
                             .filter(majorVersion -> majorVersion.getAsInt() >= 17)
                             .forEach(majorVersion -> {
                                 final List<SemVer> versions = majorVersion.getVersions();
                                 versions.stream().map(semver -> new VersionNumber(semver.getFeature(), semver.getInterim(), semver.getUpdate(), semver.getPatch())).collect(Collectors.toSet()).forEach(version -> {
                                     final int           featureVersion = version.getFeature().getAsInt();
                                     final int           update         = version.getUpdate().getAsInt();
                                     final VersionNumber versionNumber  = new VersionNumber(featureVersion, 0, update, 0);
                                     StringBuilder uriBuilder = new StringBuilder(baseUrl).append(featureVersion).append("/archive/jdk-").append(featureVersion);
                                     if (0 == update) {
                                         uriBuilder.append("_");
                                     } else {
                                         uriBuilder.append(".0.").append(update).append("_");
                                     }
                                     int length1 = uriBuilder.length();
                                     operatingSystems.forEach(operatingSystem -> {
                                         uriBuilder.setLength(length1);
                                         uriBuilder.append(operatingSystem.getApiString()).append("-");
                                         int length2 = uriBuilder.length();
                                         archiveTypeMap.get(operatingSystem).entrySet().forEach(entry -> {
                                             uriBuilder.setLength(length2);
                                             Architecture      architecture = entry.getKey();
                                             List<ArchiveType> archiveTypes = entry.getValue();
                                             int length3 = uriBuilder.length();
                                             archiveTypes.forEach(archiveType -> {
                                                 uriBuilder.setLength(length3);
                                                 uriBuilder.append(architecture.getApiString()).append("_").append("bin").append(".").append(archiveType.getApiString());
                                                 final String fileDownloadUri = uriBuilder.toString();
                                                 final String filename        = fileDownloadUri.substring(fileDownloadUri.lastIndexOf("/") + 1);

                                                 if (Helper.isUriValid(fileDownloadUri)) {
                                                     // Create pkg
                                                     Pkg pkg = new Pkg();
                                                     pkg.setDistribution(Distro.ORACLE.get());
                                                     pkg.setVersionNumber(versionNumber);
                                                     pkg.setJavaVersion(versionNumber);
                                                     pkg.setDistributionVersion(versionNumber);
                                                     pkg.setPackageType(JDK);
                                                     pkg.setArchitecture(architecture);
                                                     pkg.setBitness(architecture.getBitness());
                                                     pkg.setOperatingSystem(operatingSystem);
                                                     pkg.setReleaseStatus(ReleaseStatus.GA);
                                                     pkg.setTermOfSupport(versionNumber.getMajorVersion().getTermOfSupport());
                                                     pkg.setFileName(filename);
                                                     pkg.setArchiveType(archiveType);
                                                     pkg.setJavaFXBundled(false);
                                                     pkg.setDirectlyDownloadable(true);
                                                     pkg.setFreeUseInProduction(Boolean.TRUE);
                                                     pkg.setTckTested(Boolean.TRUE);
                                                     pkg.setDirectDownloadUri(fileDownloadUri);

                                                     pkgs.add(pkg);
                                                 }
                                             });
                                         });
                                     });
                                 });
                             });
        return pkgs;
    }
}
