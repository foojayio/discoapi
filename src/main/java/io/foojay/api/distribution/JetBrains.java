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
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.ArchiveType.getFromFileName;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.ReleaseStatus.EA;
import static eu.hansolo.jdktools.ReleaseStatus.GA;


public class JetBrains implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(JetBrains.class);

    private static final String        PACKAGE_URL            = "https://cache-redirector.jetbrains.com/intellij-jbr/";
    private static final String        GITHUB_USER            = "JetBrains";
    private static final String        GITHUB_REPOSITORY      = "JetBrainsRuntime";
    public  static final String        PKGS_PROPERTIES        = "https://github.com/foojayio/openjdk_releases/raw/main/jetbrains.properties";
    public  static final String        GITHUB_RELEASES        = "https://github.com/JetBrains/JetBrainsRuntime/releases/latest";
    //public  static final String        GITHUB_RELEASES_URL    = "https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases/latest";
    public  static final String        GITHUB_RELEASES_URL    = "https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases?pages=100";



    // URL parameters
    private static final String        ARCHITECTURE_PARAM     = "";
    private static final String        OPERATING_SYSTEM_PARAM = "";
    private static final String        ARCHIVE_TYPE_PARAM     = "";
    private static final String        PACKAGE_TYPE_PARAM     = "";
    private static final String        RELEASE_STATUS_PARAM   = "";
    private static final String        SUPPORT_TERM_PARAM     = "";
    private static final String        BITNESS_PARAM          = "";

    private static final HashAlgorithm HASH_ALGORITHM         = HashAlgorithm.NONE;
    private static final String        HASH_URI               = "";
    private static final SignatureType SIGNATURE_TYPE         = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM    = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI          = "";
    private static final String        OFFICIAL_URI           = "https://confluence.jetbrains.com/display/JBR/JetBrains+Runtime";

    private static final Pattern       JBRSDK_PATTERN         = Pattern.compile("JBRSDK\\s+\\|\\s+\\[([0-9a-zA-Z_.-]+)\\]\\(([0-9a-z:/._-]+)\\)");
    private static final Matcher       JBRSDK_MATCHER         = JBRSDK_PATTERN.matcher("");

    @Override public Distro getDistro() { return Distro.JETBRAINS; }

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
        return List.of("jetbrains", "JetBrains", "JETBRAINS");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.JETBRAINS.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {

        StringBuilder queryBuilder = new StringBuilder(PACKAGE_URL);
        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType bundleType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        return pkgs;
    }

    public List<Pkg> getAllPkgs(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        List<String> downloadLinks = new ArrayList<>();

        // Load jdk properties
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
                String downloadLink = value.toString();
                downloadLinks.add(downloadLink);
            });
        } catch (Exception e) {
            LOGGER.error("Error reading jdk properties file from github for {}. {}", getName(), e.getMessage());
        }


        for(String downloadLink : downloadLinks) {
            String strippedDownloadLink = "";
            if (downloadLink.startsWith("https://cache-redirector")) {
                strippedDownloadLink = downloadLink.replaceAll("\\?_.*", "");
            } else if (downloadLink.startsWith("https://bintray.com")) {
                strippedDownloadLink = downloadLink.replaceFirst("https://bintray.com/jetbrains/intellij-jbr/download_file\\?file_path=", "");
            }
            if (strippedDownloadLink.isEmpty()) { continue; }

            String   filename         = Helper.getFileNameFromText(strippedDownloadLink);
            String   strippedFilename = filename.replaceFirst("jbrsdk-", "").replaceAll("(\\.tar\\.gz|\\.zip)", "");
            String[] filenameParts    = strippedFilename.split("-");

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
            }

            Pkg pkg = new Pkg();

            pkg.setDistribution(Distro.JETBRAINS.get());
            pkg.setFileName(filename);
            pkg.setDirectDownloadUri(downloadLink);

            ArchiveType ext = getFromFileName(filename);
            pkg.setArchiveType(ext);

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> strippedFilename.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);

            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());


            VersionNumber vNumber = VersionNumber.fromText(filenameParts[0].replaceAll("_", "."));
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);
            pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));

            pkg.setTermOfSupport(TermOfSupport.LTS);

            pkg.setPackageType(JDK);

            pkg.setReleaseStatus(GA);

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                  .filter(entry -> strippedFilename.contains(entry.getKey()))
                                                                  .findFirst()
                                                                  .map(Entry::getValue)
                                                                  .orElse(OperatingSystem.NONE);

            if (OperatingSystem.NONE == os) {
                LOGGER.debug("Operating System not found in {} for filename: {}", getName(), filename);
                continue;
            }
            pkg.setOperatingSystem(os);

            if (WINDOWS == os) {
                pkg.setLibCType(LibCType.C_STD_LIB);
            } else if (LINUX == os ) {
                pkg.setLibCType(LibCType.GLIBC);
            } else if (MACOS == os) {
                pkg.setLibCType(LibCType.LIBC);
            }

            pkg.setFreeUseInProduction(Boolean.TRUE);

            pkg.setSize(Helper.getFileSize(downloadLink));

            pkgs.add(pkg);
        }

        

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromString(final String bodyText, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        JBRSDK_MATCHER.reset(bodyText);
        while(JBRSDK_MATCHER.find()) {
            if (JBRSDK_MATCHER.groupCount() >= 2) {
                String   filename         = JBRSDK_MATCHER.group(1);
                String   strippedFilename = filename.replaceFirst("jbrsdk-", "").replaceAll("(\\.tar\\.gz|\\.zip)", "");
                String[] filenameParts    = strippedFilename.split("-");
                String   downloadLink     = JBRSDK_MATCHER.group(2);

                if (null == filename || filename.isEmpty() || filename.endsWith("checksum")) { continue; }

                if (onlyNewPkgs) {
                    if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
                }

                Pkg pkg = new Pkg();

                pkg.setDistribution(Distro.JETBRAINS.get());
                pkg.setFileName(filename);
                pkg.setDirectDownloadUri(downloadLink);

                ArchiveType ext = getFromFileName(filename);
                pkg.setArchiveType(ext);

                Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                 .filter(entry -> filename.contains(entry.getKey()))
                                                                 .findFirst()
                                                                 .map(Entry::getValue)
                                                                 .orElse(Architecture.NONE);

                pkg.setArchitecture(arch);
                pkg.setBitness(arch.getBitness());


                VersionNumber vNumber = VersionNumber.fromText(filenameParts[0].replaceAll("_", "."));
                pkg.setVersionNumber(vNumber);
                pkg.setJavaVersion(vNumber);
                pkg.setDistributionVersion(vNumber);
                pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));

                pkg.setTermOfSupport(TermOfSupport.LTS);

                pkg.setPackageType(JDK);

                pkg.setReleaseStatus(GA);

                OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> strippedFilename.contains(entry.getKey()))
                                                                      .findFirst()
                                                                      .map(Entry::getValue)
                                                                      .orElse(OperatingSystem.NONE);

                if (OperatingSystem.NONE == os) {
                    LOGGER.debug("Operating System not found in {} for filename: {}", getName(), filename);
                    continue;
                }
                pkg.setOperatingSystem(os);

                if (WINDOWS == os) {
                    pkg.setLibCType(LibCType.C_STD_LIB);
                } else if (LINUX == os ) {
                    pkg.setLibCType(LibCType.GLIBC);
                } else if (MACOS == os) {
                    pkg.setLibCType(LibCType.LIBC);
                }

                pkg.setFreeUseInProduction(Boolean.TRUE);

                pkg.setSize(Helper.getFileSize(downloadLink));

                pkgs.add(pkg);
            }
        }

        

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }

        List<String> fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));
        for (String href : fileHrefs) {
            final String filename = Helper.getFileNameFromText(href);
            if (null == filename || !filename.startsWith("jbrsdk")) { continue; }

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(href)).count() > 0) { continue; }
            }

            final String        withoutPrefix = filename.replace("jbrsdk-", "");
            final String        withoutSuffix = withoutPrefix.replace(".tar.gz", "");
            final String[]      filenameParts = withoutSuffix.split("-");
            final String        versionString = filenameParts[0].replaceAll("_", "\\.") + (filenameParts.length == 4 ? "+" + filenameParts[3] : "");
            final Semver        semver        = Semver.fromText(filenameParts[0].replaceAll("_", "\\.") + (filenameParts.length == 4 ? "+" + filenameParts[3] : "")).getSemver1();
            final VersionNumber versionNumber = VersionNumber.fromText(versionString);
            final MajorVersion  majorVersion  =new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0);
            final PackageType   packageType   = JDK;

            OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                               .stream()
                                                                               .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                               .findFirst()
                                                                               .map(Entry::getValue)
                                                                               .orElse(OperatingSystem.NOT_FOUND);
            if (OperatingSystem.NOT_FOUND == operatingSystem) {
                LOGGER.debug("Operating System not found in JetBrains for filename: {}", filename);
                continue;
            }

            final Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                                           .stream()
                                                                           .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                           .findFirst()
                                                                           .map(Entry::getValue)
                                                                           .orElse(Architecture.NOT_FOUND);
            if (Architecture.NOT_FOUND == architecture) {
                LOGGER.debug("Architecture not found in JetBrains for filename: {}", filename);
                continue;
            }

            ArchiveType archiveType = ArchiveType.getFromFileName(filename);
            if (OperatingSystem.MACOS == operatingSystem) {
                switch(archiveType) {
                    case DEB:
                    case RPM: operatingSystem = OperatingSystem.LINUX; break;
                    case CAB:
                    case MSI:
                    case EXE: operatingSystem = OperatingSystem.WINDOWS; break;
                }
            }

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.JETBRAINS.get());
            pkg.setArchitecture(architecture);
            pkg.setBitness(architecture.getBitness());
            pkg.setVersionNumber(versionNumber);
            pkg.setJavaVersion(versionNumber);
            pkg.setDistributionVersion(versionNumber);
            pkg.setJdkVersion(new MajorVersion(versionNumber.getFeature().getAsInt()));
            pkg.setDirectDownloadUri(href);
            pkg.setFileName(filename);
            pkg.setArchiveType(archiveType);
            pkg.setJavaFXBundled(false);
            pkg.setTermOfSupport(majorVersion.getTermOfSupport());
            pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.equals(MajorVersion.getLatest(true))) ? EA : GA);
            pkg.getSemver().setMetadata(semver.getMetadata());
            pkg.setPackageType(packageType);
            pkg.setOperatingSystem(operatingSystem);
            pkg.setFreeUseInProduction(Boolean.TRUE);
            pkg.setSize(Helper.getFileSize(href));
            pkgs.add(pkg);
        }

        

        return pkgs;
    }
}
