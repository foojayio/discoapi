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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.BasicScope;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import io.foojay.api.util.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.Architecture.*;
import static io.foojay.api.pkg.Bitness.*;
import static io.foojay.api.pkg.PackageType.*;
import static io.foojay.api.pkg.OperatingSystem.*;
import static io.foojay.api.pkg.ReleaseStatus.*;


public class SAPMachine implements Distribution {
    private static final Logger                       LOGGER                  = LoggerFactory.getLogger(SAPMachine.class);

    public  static final Pattern                      SAP_MACHINE_EA_PATTERN  = Pattern.compile("(-ea\\.|-eabeta\\.)([0-9]*)");
    private static final Pattern                      FILENAME_PREFIX_PATTERN = Pattern.compile("sapmachine-");
    private static final Matcher                      FILENAME_PREFIX_MATCHER = FILENAME_PREFIX_PATTERN.matcher("");
    private static final String                       GITHUB_USER             = "SAP";
    private static final String                       GITHUB_REPOSITORY       = "SapMachine";
    private static final String                       PACKAGE_URL             = "https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases";
    private static final String                       PACKAGE_JSON_URL        = "https://sap.github.io/SapMachine/assets/data/sapmachine_releases.json";
    public  static final List<String>                 PACKAGE_URLS            = List.of("https://github.com/SAP/SapMachine/releases/tag/sapmachine-10.0.2%2B13-1",
                                                                                        "https://github.com/SAP/SapMachine/releases/tag/sapmachine-12.0.2",
                                                                                        "https://github.com/SAP/SapMachine/releases/tag/sapmachine-13.0.2");

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM      = "";
    private static final String                       OPERATING_SYSTEM_PARAM  = "";
    private static final String                       ARCHIVE_TYPE_PARAM      = "";
    private static final String                       PACKAGE_TYPE_PARAM      = "";
    private static final String                       RELEASE_STATUS_PARAM    = "";
    private static final String                       SUPPORT_TERM_PARAM      = "";
    private static final String                       BITNESS_PARAM           = "";


    @Override public Distro getDistro() { return Distro.SAP_MACHINE; }

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
                                             .filter(pkg -> Distro.SAP_MACHINE.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        //queryBuilder.append(getPkgUrl()).append(latest ? "/latest" : "").append("?per_page=100");
        queryBuilder.append(PACKAGE_URL).append("?per_page=100");

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();

        TermOfSupport supTerm = null;
        if (!versionNumber.getFeature().isEmpty()) {
            supTerm = Helper.getTermOfSupport(versionNumber, Distro.SAP_MACHINE);
        }

        JsonArray assets = jsonObj.getAsJsonArray("assets");
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     fileName     = assetJsonObj.get("name").getAsString();

            if (fileName.endsWith("txt") || fileName.endsWith("symbols.tar.gz")) { continue; }

            String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(fileName).replaceAll("");

            VersionNumber vNumber = VersionNumber.fromText(withoutPrefix);
            if (latest) {
                if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
            } else {
                if (!versionNumber.equals(vNumber)) { return pkgs; }
            }

            String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

            Pkg pkg = new Pkg();

            ArchiveType ext = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> fileName.endsWith(entry.getKey()))
                                                                      .findFirst()
                                                                      .map(Entry::getValue)
                                                                      .orElse(ArchiveType.NONE);
            if (ArchiveType.NONE == ext) {
                LOGGER.debug("Archive Type not found in SAP Machine for filename: {}", fileName);
                return pkgs;
            }

            pkg.setArchiveType(ext);

            if (null == supTerm) { supTerm = Helper.getTermOfSupport(versionNumber, Distro.SAP_MACHINE); }
            pkg.setTermOfSupport(supTerm);

            pkg.setDistribution(Distro.SAP_MACHINE.get());
            pkg.setFileName(fileName);
            pkg.setDirectDownloadUri(downloadLink);
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);

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

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);

            if (Architecture.NONE == arch) {
                LOGGER.debug("Architecture not found in SAP Machine for filename: {}", fileName);
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
                LOGGER.debug("Operating System not found in SAP Machine for filename: {}", fileName);
                continue;
            }
            pkg.setOperatingSystem(os);

            pkgs.add(pkg);
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

                if (null == fileName || fileName.isEmpty() || fileName.endsWith("txt") || fileName.endsWith("symbols.tar.gz")) { continue; }

                String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(fileName).replaceAll("");

                VersionNumber vNumber = VersionNumber.fromText(withoutPrefix);

                String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

                Pkg pkg = new Pkg();

                ArchiveType ext = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                               .filter(entry -> fileName.endsWith(entry.getKey()))
                                                               .findFirst()
                                                               .map(Entry::getValue)
                                                               .orElse(ArchiveType.NONE);
                if (ArchiveType.NONE == ext) {
                    LOGGER.debug("Archive Type not found in SAP Machine for filename: {}", fileName);
                    continue;
                } else if (ArchiveType.SRC_TAR == ext) {
                    continue;
                }

                pkg.setArchiveType(ext);

                TermOfSupport supTerm = null;
                if (!vNumber.getFeature().isEmpty()) {
                    supTerm = Helper.getTermOfSupport(vNumber, Distro.SAP_MACHINE);
                }
                pkg.setTermOfSupport(supTerm);

                pkg.setDistribution(Distro.SAP_MACHINE.get());
                pkg.setFileName(fileName);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setVersionNumber(vNumber);
                pkg.setJavaVersion(vNumber);
                pkg.setDistributionVersion(vNumber);
                pkg.setPackageType(withoutPrefix.contains(Constants.JDK_PREFIX) ? JDK : JRE);
                pkg.setReleaseStatus(withoutPrefix.contains(Constants.EA_POSTFIX) ? EA : GA);


                Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                 .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                 .findFirst()
                                                                 .map(Entry::getValue)
                                                                 .orElse(Architecture.NONE);
                if (Architecture.NONE == arch) {
                    LOGGER.debug("Architecture not found in SAP Machine for filename: {}", fileName);
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
                    LOGGER.debug("Operating System not found in SAP Machine for filename: {}", fileName);
                    continue;
                }
                pkg.setOperatingSystem(os);

                pkgs.add(pkg);
            }
        }
        LOGGER.debug("Successfully fetched {} packages from {}", pkgs.size(), PACKAGE_URL);
        return pkgs;
    }

    public List<Pkg> getAllPkgsFromJsonUrl() {
        List<Pkg>   pkgs      = new ArrayList<>();
        HttpClient  clientSAP = HttpClient.newBuilder().followRedirects(Redirect.NEVER).version(java.net.http.HttpClient.Version.HTTP_2).build();
        HttpRequest request   = HttpRequest.newBuilder().uri(URI.create(PACKAGE_JSON_URL)).setHeader("User-Agent", "DiscoAPI").GET().build();
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
                        JsonObject    majorArray = assets.get(majorRelease).getAsJsonObject();
                        JsonArray          releases     = majorArray.get("releases").getAsJsonArray();
                        final MajorVersion majorVersion = new MajorVersion(Integer.valueOf(majorRelease));
                        for (int i = 0; i < releases.size(); i++) {
                            JsonObject          releaseObj    = releases.get(i).getAsJsonObject();
                            final String        tag           = releaseObj.get("tag").getAsString();
                            final String        withoutPrefix = tag.replace("sapmachine-", "");
                            final VersionNumber versionNumber = VersionNumber.fromText(withoutPrefix);
                            for (String imageType : imageTypes) {
                                JsonObject imageTypeObj = releaseObj.get(imageType).getAsJsonObject();
                                for (String os : operatingSystems) {
                                    if (imageTypeObj.has(os)) {
                                        final String downloadLink = imageTypeObj.get(os).getAsString();
                                        final String fileName = Helper.getFileNameFromText(downloadLink);
                                        Pkg          pkg      = new Pkg();
                                        pkg.setDistribution(Distro.SAP_MACHINE.get());
                                        pkg.setBitness(BIT_64);
                                        pkg.setVersionNumber(versionNumber);
                                        pkg.setJavaVersion(versionNumber);
                                        pkg.setDistributionVersion(versionNumber);
                                        pkg.setDirectDownloadUri(downloadLink);
                                        pkg.setFileName(fileName);
                                        pkg.setArchiveType(ArchiveType.getFromFileName(fileName));
                                        pkg.setJavaFXBundled(false);
                                        pkg.setTermOfSupport(majorVersion.getTermOfSupport());
                                        pkg.setReleaseStatus((fileName.contains("-ea.") || majorVersion.equals(MajorVersion.getLatest(true))) ? EA : GA);
                                        pkg.setPackageType(PackageType.fromText(imageType));
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
                                        }
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
        } catch (InterruptedException | IOException e) {
            LOGGER.error("Error fetching packages for distribution {} from {}", getName(), PACKAGE_URL);
        }
        LOGGER.debug("Successfully fetched {} packages from sap.github.io", pkgs.size());
        return pkgs;
    }

    public List<Pkg> getAllPkgs() {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            for (String packageUrl : PACKAGE_URLS) {
                String html = Helper.getTextFromUrl(packageUrl);
                pkgs.addAll(getAllPkgsFromHtml(html, packageUrl));
            }
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages from Oracle. {}", e);
        }
        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html, final String packageUrl) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }
        List<String> fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));
        List<String> fileNames = new ArrayList<>();
        for (String href : fileHrefs) { fileNames.add(Helper.getFileNameFromText(href)); }

        for (String href : fileHrefs) {
            final String          filename        = Helper.getFileNameFromText(href);
            final String          withoutPrefix   = filename.replace("sapmachine-", "");
            final VersionNumber   versionNumber   = VersionNumber.fromText(withoutPrefix);
            final MajorVersion    majorVersion    = versionNumber.getMajorVersion();
            final PackageType     packageType     = withoutPrefix.startsWith("jdk") ? JDK : JRE;

            OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                               .stream()
                                                                               .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                               .findFirst()
                                                                               .map(Entry::getValue)
                                                                               .orElse(OperatingSystem.NONE);
            if (OperatingSystem.NONE == operatingSystem) {
                LOGGER.debug("Operating System not found in SAP Machine for filename: {}", filename);
                continue;
            }

            final Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                                           .stream()
                                                                           .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                           .findFirst()
                                                                           .map(Entry::getValue)
                                                                           .orElse(Architecture.NONE);
            if (Architecture.NONE == architecture) {
                LOGGER.debug("Architecture not found in SAP Machine for filename: {}", filename);
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
            pkg.setDistribution(Distro.SAP_MACHINE.get());
            pkg.setArchitecture(architecture);
            pkg.setBitness(architecture.getBitness());
            pkg.setVersionNumber(versionNumber);
            pkg.setJavaVersion(versionNumber);
            pkg.setDistributionVersion(versionNumber);
            pkg.setDirectDownloadUri("https://github.com" + href);
            pkg.setFileName(filename);
            pkg.setArchiveType(archiveType);
            pkg.setJavaFXBundled(false);
            pkg.setTermOfSupport(majorVersion.getTermOfSupport());
            pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.equals(MajorVersion.getLatest(true))) ? EA : GA);
            pkg.setPackageType(packageType);
            pkg.setOperatingSystem(operatingSystem);
            pkgs.add(pkg);
        }

        return pkgs;
    }
}
