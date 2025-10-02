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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.Architecture.AARCH64;
import static eu.hansolo.jdktools.Architecture.PPC64;
import static eu.hansolo.jdktools.Architecture.PPC64LE;
import static eu.hansolo.jdktools.Architecture.X64;
import static eu.hansolo.jdktools.Bitness.BIT_64;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.LINUX_MUSL;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.PackageType.JRE;
import static eu.hansolo.jdktools.ReleaseStatus.EA;
import static eu.hansolo.jdktools.ReleaseStatus.GA;


public class SAPMachine implements Distribution {
    private static final Logger                       LOGGER                  = LoggerFactory.getLogger(SAPMachine.class);

    public  static final Pattern                      SAP_MACHINE_EA_PATTERN  = Pattern.compile("(-ea\\.|-eabeta\\.)([0-9]*)");
    private static final Pattern                      FILENAME_PREFIX_PATTERN = Pattern.compile("sapmachine-");
    private static final Matcher                      FILENAME_PREFIX_MATCHER = FILENAME_PREFIX_PATTERN.matcher("");
    private static final String                       GITHUB_USER             = "SAP";
    private static final String                       GITHUB_REPOSITORY       = "SapMachine";
    private static final String                       PACKAGE_URL             = "https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases";
    private static final String                       PACKAGE_JSON_URL        = "https://sapmachine.io/assets/data/sapmachine_releases.json";
    public  static final List<String>                 PACKAGE_URLS            = List.of("https://github.com/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases/tag/sapmachine-10.0.2%2B13-1",
                                                                                        "https://github.com/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases/tag/sapmachine-12.0.2",
                                                                                        "https://github.com/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases/tag/sapmachine-13.0.2");

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM      = "";
    private static final String                       OPERATING_SYSTEM_PARAM  = "";
    private static final String                       ARCHIVE_TYPE_PARAM      = "";
    private static final String                       PACKAGE_TYPE_PARAM      = "";
    private static final String                       RELEASE_STATUS_PARAM    = "";
    private static final String                       SUPPORT_TERM_PARAM      = "";
    private static final String                       BITNESS_PARAM           = "";

    private static final HashAlgorithm                HASH_ALGORITHM          = HashAlgorithm.NONE;
    private static final String                       HASH_URI                = "";
    private static final SignatureType                SIGNATURE_TYPE          = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM     = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI           = "";
    private static final String                       OFFICIAL_URI            = "https://sapmachine.io/";


    @Override public Distro getDistro() { return Distro.SAP_MACHINE; }

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
        return List.of("sap_machine", "sapmachine", "SAPMACHINE", "SAP_MACHINE", "SAPMachine", "SAP Machine", "sap-machine", "SAP-Machine", "SAP-MACHINE");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.SAP_MACHINE.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL).append("?per_page=100");

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        TermOfSupport supTerm = null;
        if (!versionNumber.getFeature().isEmpty()) {
            supTerm = Helper.getTermOfSupport(versionNumber, Distro.SAP_MACHINE);
        }

        if (jsonObj.has("message")) {
            LOGGER.debug("Github rate limit reached when trying to get packages for SAP Machine {}", versionNumber);
            return pkgs;
        }

        JsonArray assets = jsonObj.getAsJsonArray("assets");
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     filename     = assetJsonObj.get("name").getAsString();

            if (null == filename || filename.isEmpty() || filename.endsWith(Constants.FILE_ENDING_TXT) || filename.endsWith(Constants.FILE_ENDING_SYMBOLS_TAR_GZ) || filename.contains("beta") || filename.contains("internal")) { continue; }

            String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(filename).replaceAll("");

            VersionNumber vNumber = VersionNumber.fromText(withoutPrefix);
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

            ArchiveType ext = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> filename.endsWith(entry.getKey()))
                                                                      .findFirst()
                                                                      .map(Entry::getValue)
                                                                      .orElse(ArchiveType.NONE);
            if (ArchiveType.NONE == ext) {
                LOGGER.debug("Archive Type not found in SAP Machine for filename: {}", filename);
                return pkgs;
            }

            pkg.setArchiveType(ext);

            if (null == supTerm) { supTerm = Helper.getTermOfSupport(versionNumber, Distro.SAP_MACHINE); }
            pkg.setTermOfSupport(supTerm);

            pkg.setDistribution(Distro.SAP_MACHINE.get());
            pkg.setFileName(filename);
            pkg.setDirectDownloadUri(downloadLink);
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);
            pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));

            switch (packageType) {
                case NONE:
                    pkg.setPackageType(withoutPrefix.contains(Constants.JDK_PREFIX) ? JDK : JRE);
                    break;
                case JDK:
                    if (!withoutPrefix.contains(Constants.JDK_PREFIX)) { continue; }
                    pkg.setPackageType(JDK);
                    break;
                case JRE:
                    if (!withoutPrefix.contains(Constants.JRE_PREFIX)) { continue; }
                    pkg.setPackageType(JRE);
                    break;
            }

            switch (releaseStatus) {
                case NONE:
                    pkg.setReleaseStatus(withoutPrefix.contains(Constants.EA_POSTFIX) ? EA : GA);
                    break;
                case GA:
                    if (withoutPrefix.contains(Constants.EA_POSTFIX)) { continue; }
                    pkg.setReleaseStatus(GA);
                    break;
                case EA:
                    if (!withoutPrefix.contains(Constants.EA_POSTFIX)) { continue; }
                    pkg.setReleaseStatus(EA);
                    break;
            }
            if (pkg.getFilename().contains("snapshot") || pkg.getFilename().contains("SNAPSHOT")) {
                pkg.setReleaseStatus(EA);
            }

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);

            if (Architecture.NONE == arch) {
                LOGGER.debug("Architecture not found in SAP Machine for filename: {}", filename);
                return pkgs;
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
                    case DEB, RPM, TAR_GZ -> os = OperatingSystem.LINUX;
                    case MSI, ZIP         -> os = OperatingSystem.WINDOWS;
                    case DMG, PKG         -> os = OperatingSystem.MACOS;
                    default               -> { continue; }
                }
            }
            if (OperatingSystem.NONE == os) {
                LOGGER.debug("Operating System not found in SAP Machine for filename: {}", filename);
                continue;
            }
            pkg.setOperatingSystem(os);
            pkg.setFreeUseInProduction(Boolean.TRUE);
            pkg.setSize(Helper.getFileSize(downloadLink));

            if (ReleaseStatus.GA == pkg.getReleaseStatus() && Helper.isLTS(pkg.getMajorVersion())) {
                pkg.setTckTested(Verification.YES);
                pkg.setTckCertUri("https://github.com/SAP/SapMachine/wiki/Frequently-Asked-Questions#Are-SapMachine-builds-verified-by-the-Java-Compatibility-Kit-JCK");
            }

            if (pkg.getVersionNumber().getInterim().isPresent() && pkg.getVersionNumber().getInterim().getAsInt() != 0) { continue; }
            pkgs.add(pkg);
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
            JsonArray  assets  = jsonObj.getAsJsonArray("assets");
            for (JsonElement element : assets) {
                final JsonObject assetJsonObj = element.getAsJsonObject();
                final String     filename     = assetJsonObj.get("name").getAsString();

                if (!filename.startsWith("sapmachine-")) { continue; }
                if (null == filename ||
                    filename.isEmpty() ||
                    filename.endsWith(Constants.FILE_ENDING_TXT) ||
                    filename.endsWith(Constants.FILE_ENDING_SYMBOLS_TAR_GZ) ||
                    filename.contains("beta") ||
                    filename.contains("internal") ||
                    filename.contains("jdk-0.0.0") ||
                    filename.contains("jre-0.0.0")) {
                    continue;
                }

                final String        withoutPrefix = filename.replace("sapmachine-", "");
                final VersionNumber versionNumber = VersionNumber.fromText(withoutPrefix);
                final MajorVersion  majorVersion  = new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0);
                if (majorVersion.getAsInt() == 0) { continue; }

                final PackageType   packageType   = withoutPrefix.startsWith("jdk") ? JDK : JRE;
                final String        downloadLink  = assetJsonObj.get("browser_download_url").getAsString();

                if (onlyNewPkgs) {
                    if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
                }

                OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                                   .stream()
                                                                                   .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                               .findFirst()
                                                               .map(Entry::getValue)
                                                                                   .orElse(OperatingSystem.NOT_FOUND);
                
                if (OperatingSystem.NOT_FOUND == operatingSystem) {
                    if (filename.endsWith(ArchiveType.RPM.getApiString()) || filename.endsWith(ArchiveType.DEB.getApiString())) {
                        if (filename.contains("musl")) {
                            operatingSystem = LINUX_MUSL;
                        } else {
                            operatingSystem = LINUX;
                        }
                    } else if (filename.endsWith(ArchiveType.DMG.getApiString()) || filename.endsWith(ArchiveType.PKG.getApiString())) {
                        operatingSystem = MACOS;
                    } else if (filename.endsWith(ArchiveType.EXE.getApiString()) || filename.endsWith(ArchiveType.MSI.getApiString())) {
                        operatingSystem = WINDOWS;
                    } else {
                    LOGGER.debug("Operating System not found in SAP Machine for filename: {}", filename);
                    continue;
                }
                }

                final Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                                               .stream()
                                                                 .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                 .findFirst()
                                                                 .map(Entry::getValue)
                                                                               .orElse(Architecture.NOT_FOUND);
                if (Architecture.NOT_FOUND == architecture) {
                    LOGGER.debug("Architecture not found in SAP Machine for filename: {}", filename);
                    continue;
                }

                ArchiveType ext = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                               .filter(entry -> filename.endsWith(entry.getKey()))
                                                                      .findFirst()
                                                                      .map(Entry::getValue)
                                                               .orElse(ArchiveType.NOT_FOUND);
                if (ArchiveType.NOT_FOUND == ext) {
                    LOGGER.debug("Archive Type not found in SAP Machine for filename: {}", filename);
                    continue;
                } else if (ArchiveType.SRC_TAR == ext) {
                    continue;
                }

                ArchiveType archiveType = ArchiveType.getFromFileName(filename);
                if (OperatingSystem.MACOS == operatingSystem) {
                    switch(archiveType) {
                        case DEB, RPM      -> operatingSystem = OperatingSystem.LINUX;
                        case CAB, MSI, EXE -> operatingSystem = OperatingSystem.WINDOWS;
                    }
                }

                Pkg pkg = new Pkg();
                pkg.setDistribution(Distro.SAP_MACHINE.get());
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
                if (pkg.getFilename().contains("snapshot") || pkg.getFilename().contains("SNAPSHOT")) {
                    pkg.setReleaseStatus(EA);
                }
                pkg.setPackageType(packageType);
                pkg.setOperatingSystem(operatingSystem);
                pkg.setFreeUseInProduction(Boolean.TRUE);
                pkg.setSize(Helper.getFileSize(downloadLink));

                if (ReleaseStatus.GA == pkg.getReleaseStatus() && Helper.isLTS(pkg.getMajorVersion())) {
                    pkg.setTckTested(Verification.YES);
                    pkg.setTckCertUri("https://github.com/SAP/SapMachine/wiki/Frequently-Asked-Questions#Are-SapMachine-builds-verified-by-the-Java-Compatibility-Kit-JCK");
                }

                if (pkg.getVersionNumber().getInterim().isPresent() && pkg.getVersionNumber().getInterim().getAsInt() != 0) { continue; }
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

                if (null == filename || filename.isEmpty() || filename.contains("symbols")) {
                    continue;
                }

                String nameToMatch;
                String fn = filename;
                if (fn.endsWith(Constants.FILE_ENDING_SHA256_DMG_TXT)) {
                    nameToMatch = fn.replaceAll("." + Constants.FILE_ENDING_SHA256_DMG_TXT, "");
                } else if (fn.endsWith(Constants.FILE_ENDING_SHA256_TXT)) {
                    nameToMatch = fn.replaceAll("." + Constants.FILE_ENDING_SHA256_TXT, "");
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

    public List<Pkg> getAllPkgsFromJsonUrl(final boolean onlyNewPkgs) {
        List<Pkg>   pkgs      = new ArrayList<>();

        OptionalInt nextEA       = Helper.getNextEA();
        OptionalInt nextButOneEA = Helper.getNextButOneEA();

        HttpClient  clientSAP = HttpClient.newBuilder()
                                          .followRedirects(Redirect.NEVER)
                                          .version(java.net.http.HttpClient.Version.HTTP_2)
                                          .connectTimeout(Duration.ofSeconds(20))
                                          .build();
        HttpRequest request   = HttpRequest.newBuilder()
                                           .uri(URI.create(PACKAGE_JSON_URL))
                                           .setHeader("User-Agent", "DiscoAPI")
                                           .timeout(Duration.ofSeconds(60))
                                           .GET()
                                           .build();
        try {
            HttpResponse<String> response = clientSAP.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String      bodyText = response.body();
                Gson        gson     = new Gson();
                JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                if (element instanceof JsonObject) {
                    JsonObject jsonObject = element.getAsJsonObject();

                    List<String> majorReleases = new ArrayList<>();
                    JsonArray    majors        = jsonObject.get("majors").getAsJsonArray();
                    for (int i = 0; i < majors.size(); i++) {
                        JsonObject   majorObj = majors.get(i).getAsJsonObject();
                        final String id       = majorObj.get("id").getAsString();
                        majorReleases.add(id);
                    }

                    List<String> imageTypes = new ArrayList<>();
                    JsonArray    types      = jsonObject.get("imageTypes").getAsJsonArray();
                    for (int i = 0; i < types.size(); i++) {
                        JsonObject   imageTypeObj = types.get(i).getAsJsonObject();
                        final String key          = imageTypeObj.get("key").getAsString();
                        imageTypes.add(key);
                    }

                    List<String> operatingSystems = new ArrayList<>();
                    JsonArray    oss              = jsonObject.get("os").getAsJsonArray();
                    for (int i = 0; i < oss.size(); i++) {
                        JsonObject   osObj = oss.get(i).getAsJsonObject();
                        final String key   = osObj.get("key").getAsString();
                        operatingSystems.add(key);
                    }

                    JsonObject assets = jsonObject.get("assets").getAsJsonObject();
                    for (String majorRelease : majorReleases) {
                        JsonObject         majorArray   = assets.get(majorRelease).getAsJsonObject();
                        JsonArray          releases     = majorArray.get("releases").getAsJsonArray();
                        Integer            featureVersion = Integer.valueOf(majorRelease.replace("-ea", ""));
                        final MajorVersion majorVersion   = new MajorVersion(featureVersion);
                        for (int i = 0; i < releases.size(); i++) {
                            JsonObject          releaseObj    = releases.get(i).getAsJsonObject();
                            final String        tag           = releaseObj.get("tag").getAsString();
                            if (!tag.startsWith("sapmachine-")) { continue; }
                            final String        withoutPrefix = tag.replace("sapmachine-", "");
                            final VersionNumber versionNumber = VersionNumber.fromText(withoutPrefix);
                            for (String imageType : imageTypes) {
                                JsonObject imageTypeObj = releaseObj.get(imageType).getAsJsonObject();
                                for (String os : operatingSystems) {
                                    if (imageTypeObj.has(os)) {
                                        final String downloadLink = imageTypeObj.get(os).getAsString();
                                        final String filename     = Helper.getFileNameFromText(downloadLink);
                                        if (null == filename || filename.isEmpty() || filename.endsWith(Constants.FILE_ENDING_TXT) || filename.endsWith(Constants.FILE_ENDING_SYMBOLS_TAR_GZ) || filename.contains("beta") || filename.contains("internal")) { continue; }
                                        if (onlyNewPkgs) {
                                            if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
                                        }
                                        Pkg          pkg          = new Pkg();
                                        pkg.setDistribution(Distro.SAP_MACHINE.get());
                                        pkg.setBitness(BIT_64);
                                        pkg.setVersionNumber(versionNumber);
                                        pkg.setJavaVersion(versionNumber);
                                        pkg.setDistributionVersion(versionNumber);
                                        pkg.setJdkVersion(majorVersion);
                                        pkg.setDirectDownloadUri(downloadLink);
                                        pkg.setFileName(filename);
                                        pkg.setArchiveType(ArchiveType.getFromFileName(filename));
                                        pkg.setJavaFXBundled(false);
                                        pkg.setTermOfSupport(majorVersion.getTermOfSupport());
                                        if (nextEA.isPresent()) {
                                            pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.getAsInt() == nextEA.getAsInt() || majorVersion.getAsInt() == nextButOneEA.getAsInt()) ? EA : GA);
                                        } else {
                                            pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.equals(MajorVersion.getLatest(true))) ? EA : GA);
                                        }
                                        if (pkg.getFilename().contains("snapshot") || pkg.getFilename().contains("SNAPSHOT")) {
                                            pkg.setReleaseStatus(EA);
                                        }
                                        pkg.setPackageType(PackageType.fromText(imageType));
                                        pkg.setFreeUseInProduction(Boolean.TRUE);
                                        switch (os) {
                                            case "linux-x64":
                                                pkg.setOperatingSystem(LINUX);
                                                pkg.setArchitecture(X64);
                                                break;
                                            case "linux-aarch64":
                                                pkg.setOperatingSystem(LINUX);
                                                pkg.setArchitecture(AARCH64);
                                                break;
                                            case "linux-ppc64le":
                                                pkg.setOperatingSystem(LINUX);
                                                pkg.setArchitecture(PPC64LE);
                                                break;
                                            case "linux-ppc64":
                                                pkg.setOperatingSystem(LINUX);
                                                pkg.setArchitecture(PPC64);
                                                break;
                                            case "windows-x64":
                                                pkg.setOperatingSystem(WINDOWS);
                                                pkg.setArchitecture(X64);
                                                break;
                                            case "windows-x64-installer":
                                                pkg.setOperatingSystem(WINDOWS);
                                                pkg.setArchitecture(X64);
                                                break;
                                            case "osx-x64":
                                                pkg.setOperatingSystem(MACOS);
                                                pkg.setArchitecture(X64);
                                                break;
                                            case "osx-aarch64":
                                                pkg.setOperatingSystem(MACOS);
                                                pkg.setArchitecture(AARCH64);
                                                break;
                                            default: continue;
                                        }

                                        if (ReleaseStatus.GA == pkg.getReleaseStatus() && Helper.isLTS(pkg.getMajorVersion())) {
                                            pkg.setTckTested(Verification.YES);
                                            pkg.setTckCertUri("https://github.com/SAP/SapMachine/wiki/Frequently-Asked-Questions#Are-SapMachine-builds-verified-by-the-Java-Compatibility-Kit-JCK");
                                        }

                                        pkg.setSize(Helper.getFileSize(downloadLink));
                                        if (pkg.getVersionNumber().getInterim().isPresent() && pkg.getVersionNumber().getInterim().getAsInt() != 0) { continue; }
                                        pkgs.add(pkg);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Problem with url request
                LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
            }
        } catch (CompletionException | InterruptedException | IOException e) {
            LOGGER.error("Error fetching packages for distribution {} from {}", getName(), PACKAGE_JSON_URL);
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        LOGGER.debug("Successfully fetched {} packages from sapmachine.io", pkgs.size());
        return pkgs;
    }

    public List<Pkg> getAllPkgs(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            for (String packageUrl : PACKAGE_URLS) {
                final HttpResponse<String> response = Helper.get(packageUrl);
                if (null == response) { return pkgs; }
                final String html = response.body();
                if (html.isEmpty()) { return pkgs; }
                pkgs.addAll(getAllPkgsFromHtml(html, onlyNewPkgs));
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages from SAP Machine. {}", e);
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }

        OptionalInt nextEA       = Helper.getNextEA();
        OptionalInt nextButOneEA = Helper.getNextButOneEA();

        List<String> fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));
        for (String href : fileHrefs) {
            final String filename = Helper.getFileNameFromText(href);
            if (null == filename ||
                filename.isEmpty() ||
                filename.endsWith(Constants.FILE_ENDING_TXT) ||
                filename.endsWith(Constants.FILE_ENDING_SYMBOLS_TAR_GZ) ||
                filename.contains("beta") ||
                filename.contains("internal") ||
                filename.contains("jdk-0.0.0") ||
                filename.contains("jre-0.0.0")) { continue; }

            if (!filename.startsWith("sapmachine-")) { continue; }
            final String          withoutPrefix   = filename.replace("sapmachine-", "");
            final VersionNumber   versionNumber   = VersionNumber.fromText(withoutPrefix);
            final MajorVersion    majorVersion    = new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0);
            final PackageType     packageType     = withoutPrefix.startsWith("jdk") ? JDK : JRE;

            ArchiveType archiveType = ArchiveType.getFromFileName(filename);

            OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                               .stream()
                                                                               .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                               .findFirst()
                                                                               .map(Entry::getValue)
                                                                               .orElse(OperatingSystem.NOT_FOUND);
            if (OperatingSystem.NOT_FOUND == operatingSystem) {
                switch (archiveType) {
                    case DEB, RPM      -> operatingSystem = OperatingSystem.LINUX;
                    case CAB, MSI, EXE -> operatingSystem = OperatingSystem.WINDOWS;
                    case DMG, PKG      -> operatingSystem = OperatingSystem.MACOS;
                    default            -> operatingSystem = OperatingSystem.NOT_FOUND;
                }

                if (OperatingSystem.NOT_FOUND == operatingSystem) {
                LOGGER.debug("Operating System not found in SAP Machine for filename: {}", filename);
                continue;
            }
            }

            if (OperatingSystem.MACOS == operatingSystem) {
                switch(archiveType) {
                    case DEB, RPM      -> operatingSystem = OperatingSystem.LINUX;
                    case CAB, MSI, EXE -> operatingSystem = OperatingSystem.WINDOWS;
                }
            }

            final Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                                           .stream()
                                                                           .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                           .findFirst()
                                                                           .map(Entry::getValue)
                                                                           .orElse(Architecture.NOT_FOUND);
            if (Architecture.NOT_FOUND == architecture) {
                LOGGER.debug("Architecture not found in SAP Machine for filename: {}", filename);
                continue;
            }

            final String downloadLink = "https://github.com" + href;
            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
            }

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.SAP_MACHINE.get());
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
            if (pkg.getFilename().contains("snapshot") || pkg.getFilename().contains("SNAPSHOT")) {
                pkg.setReleaseStatus(EA);
            }
            pkg.setPackageType(packageType);
            pkg.setOperatingSystem(operatingSystem);
            pkg.setFreeUseInProduction(Boolean.TRUE);

            if (ReleaseStatus.GA == pkg.getReleaseStatus() && Helper.isLTS(pkg.getMajorVersion())) {
                pkg.setTckTested(Verification.YES);
                pkg.setTckCertUri("https://github.com/SAP/SapMachine/wiki/Frequently-Asked-Questions#Are-SapMachine-builds-verified-by-the-Java-Compatibility-Kit-JCK");
            }

            pkg.setSize(Helper.getFileSize(pkg.getDirectDownloadUri()));
            if (pkg.getVersionNumber().getInterim().isPresent() && pkg.getVersionNumber().getInterim().getAsInt() != 0) { continue; }
            pkgs.add(pkg);
        }

        return pkgs;
    }
}
