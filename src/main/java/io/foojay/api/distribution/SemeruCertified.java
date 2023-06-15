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
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.SignatureType;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.Verification;
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

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.ReleaseStatus.EA;
import static eu.hansolo.jdktools.ReleaseStatus.GA;


public class SemeruCertified implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(SemeruCertified.class);

    private static final String        PACKAGE_URL            = "https://developer.ibm.com/languages/java/semeru-runtimes/downloads/";

    // URL parameters
    private static final String        ARCHITECTURE_PARAM     = "architecture";
    private static final String        OPERATING_SYSTEM_PARAM = "os";
    private static final String        ARCHIVE_TYPE_PARAM     = "";
    private static final String        PACKAGE_TYPE_PARAM     = "image_type";
    private static final String        RELEASE_STATUS_PARAM   = "release_type";
    private static final String        SUPPORT_TERM_PARAM     = "";
    private static final String        BITNESS_PARAM          = "";

    private static final HashAlgorithm HASH_ALGORITHM         = HashAlgorithm.NONE;
    private static final String        HASH_URI               = "";
    private static final SignatureType SIGNATURE_TYPE         = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM    = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI          = "";
    private static final String        OFFICIAL_URI           = "https://developer.ibm.com/languages/java/semeru-runtimes/";


    @Override public Distro getDistro() { return Distro.SEMERU_CERTIFIED; }

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
        return List.of("semeru_certified", "SEMERU_CERTIFIED", "Semeru_Certified", "Semeru_certified", "semeru certified", "SEMERU CERTIFIED", "Semeru Certified", "Semeru certified");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.SEMERU_CERTIFIED.get().equals(pkg.getDistribution()))
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

        queryBuilder.append(PACKAGE_URL);

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        return pkgs;
    }

    public List<Pkg> getAllPkgs(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            try {
                final HttpResponse<String> response = Helper.get(PACKAGE_URL);
                if (null == response) { return pkgs; }
                final String htmlAllJDKs  = response.body();
                if (!htmlAllJDKs.isEmpty()) {
                    pkgs.addAll(getAllPkgsFromHtml(htmlAllJDKs, onlyNewPkgs));
                }
            } catch (Exception e) {
                LOGGER.error("Error fetching all packages from {}. {}", getName(), e);
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages from Semeru Certified. {}", e);
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromJson(final JsonArray jsonArray, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        OptionalInt nextEA       = Helper.getNextEA();
        OptionalInt nextButOneEA = Helper.getNextButOneEA();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObj = jsonArray.get(i).getAsJsonObject();
            if (jsonObj.has("prerelease")) {
                boolean prerelease = jsonObj.get("prerelease").getAsBoolean();
                if (prerelease) { continue; }
            }
            JsonArray assets = jsonObj.getAsJsonArray("assets");
            for (JsonElement element : assets) {
                JsonObject assetJsonObj = element.getAsJsonObject();
                String     filename     = assetJsonObj.get("name").getAsString();

                if (null == filename || filename.isEmpty() || filename.endsWith("txt") || filename.contains("debugimage") || filename.contains("testimage") || filename.endsWith("json")) { continue; }
                if (filename.contains("-debug-")) { continue; }
                if (null == filename || !filename.startsWith("ibm-semeru-open")) { continue; }

                final String withoutPrefix    = filename.replaceAll("ibm-semeru-open-", "");
                final String withoutLeadingNo = withoutPrefix.replaceAll("^[0-9]+-", "");

                PackageType packageType = Constants.PACKAGE_TYPE_LOOKUP.entrySet().stream()
                                                                       .filter(entry -> withoutLeadingNo.contains(entry.getKey()))
                                                                       .findFirst()
                                                                       .map(Entry::getValue)
                                                                       .orElse(PackageType.NOT_FOUND);
                if (PackageType.NOT_FOUND == packageType) {
                    LOGGER.debug("Package type not found in Semeru Certified for filename: {}", filename);
                    continue;
                }

                if (filename.endsWith("rpm")) { continue; }

                final String   withoutSuffix = withoutLeadingNo.substring(4);

                final String[] filenameParts = withoutSuffix.split("_");

                final VersionNumber versionNumber = VersionNumber.fromText(filenameParts[2] + (filenameParts.length == 6 ? ("+b" + filenameParts[3]) : ""));
                final MajorVersion  majorVersion  = new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0);

                String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

                if (onlyNewPkgs) {
                    if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
                }

                OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                                   .filter(entry -> withoutSuffix.contains(entry.getKey()))
                                                                                   .findFirst()
                                                                                   .map(Entry::getValue)
                                                                                   .orElse(OperatingSystem.NOT_FOUND);
                if (OperatingSystem.NOT_FOUND == operatingSystem) {
                    LOGGER.debug("Operating System not found in Semeru for filename: {}", filename);
                    continue;
                }


                final Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                               .filter(entry -> withoutSuffix.contains(entry.getKey()))
                                                                               .findFirst()
                                                                               .map(Entry::getValue)
                                                                               .orElse(Architecture.NOT_FOUND);
                if (Architecture.NOT_FOUND == architecture) {
                    LOGGER.debug("Architecture not found in Semeru Certified for filename: {}", filename);
                    continue;
                }

                final ArchiveType archiveType = Helper.getFileEnding(filename);
                if (OperatingSystem.MACOS == operatingSystem) {
                    switch(archiveType) {
                        case DEB, RPM      -> operatingSystem = OperatingSystem.LINUX;
                        case CAB, MSI, EXE -> operatingSystem = OperatingSystem.WINDOWS;
                    }
                }

                Pkg pkg = new Pkg();
                pkg.setDistribution(Distro.SEMERU_CERTIFIED.get());
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
                if (nextEA.isPresent()) {
                    pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.getAsInt() == nextEA.getAsInt() || majorVersion.getAsInt() == nextButOneEA.getAsInt()) ? EA : GA);
                } else {
                    pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.equals(MajorVersion.getLatest(true))) ? EA : GA);
                }
                if (ReleaseStatus.GA == pkg.getReleaseStatus()) {
                    pkg.setTckTested(Verification.YES);
                    pkg.setTckCertUri("https://www.ibm.com/support/pages/semeru-runtimes-getting-started");
                } else {
                    pkg.setTckTested(Verification.NO);
                    pkg.setTckCertUri("");
                }
                pkg.setPackageType(packageType);
                pkg.setOperatingSystem(operatingSystem);
                pkg.setFreeUseInProduction(Boolean.TRUE);
                pkg.setSize(Helper.getFileSize(downloadLink));
                pkgs.add(pkg);
            }
        }

        // Fetch checksums
        for (int i = 0 ; i < jsonArray.size(); i++) {
            JsonObject jsonObj = jsonArray.get(i).getAsJsonObject();
            JsonArray  assets  = jsonObj.getAsJsonArray("assets");
            for (JsonElement element : assets) {
                JsonObject assetJsonObj = element.getAsJsonObject();
                String     filename     = assetJsonObj.get("name").getAsString();
                if (null == filename || filename.isEmpty() || !filename.endsWith(Constants.FILE_ENDING_SHA256_TXT)) { continue; }
                String nameToMatch;
                if (filename.endsWith(Constants.FILE_ENDING_SHA256_TXT)) {
                    nameToMatch = filename.replaceAll("." + Constants.FILE_ENDING_SHA256_DMG_TXT, "");
                } else if (filename.endsWith(Constants.FILE_ENDING_SHA256_TXT)) {
                    nameToMatch = filename.replaceAll("." + Constants.FILE_ENDING_SHA256_TXT, "");
                } else {
                    continue;
                }

                final String  downloadLink = assetJsonObj.get("browser_download_url").getAsString();
                Optional<Pkg> optPkg       = pkgs.stream().filter(pkg -> pkg.getFilename().contains(nameToMatch)).findFirst();
                if (optPkg.isPresent()) {
                    Pkg pkg = optPkg.get();
                    pkg.setChecksumUri(downloadLink);
                    pkg.setChecksumType(HashAlgorithm.SHA256);
                }
            }
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        LOGGER.debug("Successfully fetched {} packages from {}", pkgs.size(), PACKAGE_URL);
        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }

        OptionalInt nextEA       = Helper.getNextEA();
        OptionalInt nextButOneEA = Helper.getNextButOneEA();

        List<String> downloadLinks = new ArrayList<>(Helper.getDownloadLinkFromString(html));
        List<String> signatureUris = new ArrayList<>(Helper.getSigFromString(html));
        for (String downloadLink : downloadLinks) {
            String filename = Helper.getFileNameFromText(downloadLink.replaceAll("'", ""));

            if (null == filename || filename.isEmpty() || filename.endsWith("txt") || filename.contains("debugimage") || filename.contains("testimage") || filename.endsWith("json") || filename.endsWith("bin")) { continue; }
            if (filename.contains("-debug-")) { continue; }
            if (null == filename || !filename.startsWith("ibm-semeru-certified")) { continue; }

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
            }

            final String withoutPrefix    = filename.replaceAll("ibm-semeru-certified-", "");
            final String withoutLeadingNo = withoutPrefix.replaceAll("^[0-9]+-", "");

            PackageType packageType = Constants.PACKAGE_TYPE_LOOKUP.entrySet().stream()
                                                                   .filter(entry -> withoutLeadingNo.contains(entry.getKey()))
                                                                   .findFirst()
                                                                   .map(Entry::getValue)
                                                                   .orElse(PackageType.NOT_FOUND);
            if (PackageType.NOT_FOUND == packageType) {
                LOGGER.debug("Package type not found in Semeru Certified for filename: {}", filename);
                continue;
            }

            if (filename.endsWith("rpm")) { continue; }

            final String   withoutSuffix = withoutLeadingNo.substring(4);

            final String[] filenameParts = withoutSuffix.split("_");

            final VersionNumber versionNumber = VersionNumber.fromText(filenameParts[2] + (filenameParts.length == 6 ? ("+b" + filenameParts[3]) : ""));
            final MajorVersion  majorVersion  = new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0);

            OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                               .filter(entry -> withoutSuffix.contains(entry.getKey()))
                                                                               .findFirst()
                                                                               .map(Entry::getValue)
                                                                               .orElse(OperatingSystem.NOT_FOUND);
            if (OperatingSystem.NOT_FOUND == operatingSystem) {
                LOGGER.debug("Operating System not found in Semeru for filename: {}", filename);
                continue;
            }


            final Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                           .filter(entry -> withoutSuffix.contains(entry.getKey()))
                                                                           .findFirst()
                                                                           .map(Entry::getValue)
                                                                           .orElse(Architecture.NOT_FOUND);
            if (Architecture.NOT_FOUND == architecture) {
                LOGGER.debug("Architecture not found in Semeru Certified for filename: {}", filename);
                continue;
            }

            final ArchiveType archiveType = Helper.getFileEnding(filename);
            if (OperatingSystem.MACOS == operatingSystem) {
                switch(archiveType) {
                    case DEB, RPM      -> operatingSystem = OperatingSystem.LINUX;
                    case CAB, MSI, EXE -> operatingSystem = OperatingSystem.WINDOWS;
                }
            }

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.SEMERU_CERTIFIED.get());
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
            if (nextEA.isPresent()) {
                pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.getAsInt() == nextEA.getAsInt() || majorVersion.getAsInt() == nextButOneEA.getAsInt()) ? EA : GA);
            } else {
                pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.equals(MajorVersion.getLatest(true))) ? EA : GA);
            }
            if (ReleaseStatus.GA == pkg.getReleaseStatus()) {
                pkg.setTckTested(Verification.YES);
                pkg.setTckCertUri("https://www.ibm.com/support/pages/semeru-runtimes-getting-started");
            } else {
                pkg.setTckTested(Verification.NO);
                pkg.setTckCertUri("");
            }
            pkg.setPackageType(packageType);
            pkg.setOperatingSystem(operatingSystem);
            pkg.setFreeUseInProduction(Boolean.FALSE);
            if (signatureUris.contains(downloadLink + ".sig")) { pkg.setSignatureUri(downloadLink + ".sig"); }
            pkg.setSize(Helper.getFileSize(downloadLink));
            pkgs.add(pkg);
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }
}
