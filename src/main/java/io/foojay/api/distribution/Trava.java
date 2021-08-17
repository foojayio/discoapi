/*
 * Copyright (c) 2021 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import io.foojay.api.util.GithubTokenPool;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.PackageType.JRE;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;


public class Trava implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(Trava.class);

    private static final Pattern      DOWNLOAD_PATTERN = Pattern.compile("(.*\\/download\\/dcevm)(\\-)?(.*)(\\/.*)");
    private static final Matcher      DOWNLOAD_MATCHER = DOWNLOAD_PATTERN.matcher("");
    private static final String       GITHUB_USER      = "TravaOpenJDK";
    private static final String       PACKAGE_URL      = "https://github.com/TravaOpenJDK/";
    public  static final List<String> PACKAGE_URLS     = List.of("https://api.github.com/repos/" + GITHUB_USER + "/trava-jdk-8-dcevm/releases?per_page=100",
                                                                 "https://api.github.com/repos/" + GITHUB_USER + "/trava-jdk-11-dcevm/releases?per_page=100",
                                                                 "https://api.github.com/repos/" + GITHUB_USER + "/trava-jdk-11-dcevm-newgen/releases?per_page=100");


    // URL parameters
    private static final String        ARCHITECTURE_PARAM      = "";
    private static final String        OPERATING_SYSTEM_PARAM  = "";
    private static final String        ARCHIVE_TYPE_PARAM      = "";
    private static final String        PACKAGE_TYPE_PARAM      = "";
    private static final String        RELEASE_STATUS_PARAM    = "";
    private static final String        SUPPORT_TERM_PARAM      = "";
    private static final String        BITNESS_PARAM           = "";

    private static final HashAlgorithm HASH_ALGORITHM      = HashAlgorithm.NONE;
    private static final String        HASH_URI            = "";
    private static final SignatureType SIGNATURE_TYPE      = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI           = "";


    @Override public Distro getDistro() { return Distro.TRAVA; }

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

    @Override public List<String> getSynonyms() {
        return List.of("trava", "TRAVA", "Trava", "trava_openjdk", "TRAVA_OPENJDK", "trava openjdk", "TRAVA OPENJDK");
    }

    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.TRAVA.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString))))
                                             .stream()
                                             .sorted(Comparator.comparing(SemVer::getVersionNumber).reversed())
                                             .collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
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
            supTerm = Helper.getTermOfSupport(versionNumber, Distro.TRAVA);
        }

        if (jsonObj.has("message")) {
            LOGGER.debug("Github rate limit reached when trying to get packages for Trava {}", versionNumber);
            return pkgs;
        }

        JsonArray assets = jsonObj.getAsJsonArray("assets");
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     fileName     = assetJsonObj.get("name").getAsString();

            if (fileName.endsWith("txt") || fileName.endsWith("symbols.tar.gz")) { continue; }

            String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

            VersionNumber vNumber = new VersionNumber();
            DOWNLOAD_MATCHER.reset(downloadLink);
            final List<MatchResult> results = DOWNLOAD_MATCHER.results().collect(Collectors.toList());
            if (results.size() > 0) {
                MatchResult result = results.get(0);
                vNumber = VersionNumber.fromText(result.group(3));
            }

            if (latest) {
                if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
            } else {
                if (!versionNumber.equals(vNumber)) { return pkgs; }
            }


            Pkg pkg = new Pkg();

            ArchiveType ext = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                           .filter(entry -> fileName.endsWith(entry.getKey()))
                                                           .findFirst()
                                                           .map(Entry::getValue)
                                                           .orElse(ArchiveType.NONE);
            if (ArchiveType.NONE == ext) {
                LOGGER.debug("Archive Type not found in Trava for filename: {}", fileName);
                return pkgs;
            }

            pkg.setArchiveType(ext);

            if (null == supTerm) { supTerm = Helper.getTermOfSupport(versionNumber, Distro.TRAVA); }
            pkg.setTermOfSupport(supTerm);

            pkg.setDistribution(Distro.TRAVA.get());
            pkg.setFileName(fileName);
            pkg.setDirectDownloadUri(downloadLink);
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);

            switch (packageType) {
                case NONE:
                    pkg.setPackageType(fileName.contains(Constants.JDK_PREFIX) ? JDK : JRE);
                    break;
                case JDK:
                    if (!fileName.contains(Constants.JDK_PREFIX)) { continue; }
                    pkg.setPackageType(JDK);
                    break;
                case JRE:
                    if (!fileName.contains(Constants.JRE_PREFIX)) { continue; }
                    pkg.setPackageType(JRE);
                    break;
            }

            switch (releaseStatus) {
                case NONE:
                    pkg.setReleaseStatus(fileName.contains(Constants.EA_POSTFIX) ? EA : GA);
                    break;
                case GA:
                    if (fileName.contains(Constants.EA_POSTFIX)) { continue; }
                    pkg.setReleaseStatus(GA);
                    break;
                case EA:
                    if (!fileName.contains(Constants.EA_POSTFIX)) { continue; }
                    pkg.setReleaseStatus(EA);
                    break;
            }

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> fileName.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);

            if (Architecture.NONE == arch) {
                LOGGER.debug("Architecture not found in Trava for filename: {}", fileName);
                return pkgs;
            }

            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                  .filter(entry -> fileName.contains(entry.getKey()))
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
                LOGGER.debug("Operating System not found in Trava for filename: {}", fileName);
                continue;
            }
            pkg.setOperatingSystem(os);

            pkgs.add(pkg);
        }

        return pkgs;
    }

    public List<Pkg> getAllPkgs() {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            for (String packageUrl : PACKAGE_URLS) {
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
                            pkgs.addAll(getAllPkgsFromJson(jsonArray));
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
            LOGGER.error("Error fetching all packages from Trava. {}", e);
        }
        return pkgs;
    }

    public List<Pkg> getAllPkgsFromJson(final JsonArray jsonArray) {
        List<Pkg> pkgs = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObj = jsonArray.get(i).getAsJsonObject();
            JsonArray assets = jsonObj.getAsJsonArray("assets");
            for (JsonElement element : assets) {
                JsonObject assetJsonObj = element.getAsJsonObject();
                String     filename     = assetJsonObj.get("name").getAsString();

                if (null == filename || filename.isEmpty() || filename.endsWith("txt") || filename.endsWith("debuginfo.zip") || filename.endsWith("sha256")) { continue; }
                if (filename.contains("-debug-")) { continue; }

                String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

                VersionNumber vNumber = new VersionNumber();
                DOWNLOAD_MATCHER.reset(downloadLink);
                final List<MatchResult> results = DOWNLOAD_MATCHER.results().collect(Collectors.toList());
                if (results.size() > 0) {
                    MatchResult result = results.get(0);
                    vNumber = VersionNumber.fromText(result.group(3));
                }

                Pkg pkg = new Pkg();

                ArchiveType ext = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                               .filter(entry -> filename.endsWith(entry.getKey()))
                                                               .findFirst()
                                                               .map(Entry::getValue)
                                                               .orElse(ArchiveType.NONE);
                if (ArchiveType.NONE == ext) {
                    LOGGER.debug("Archive Type not found in Trava for filename: {}", filename);
                    continue;
                } else if (ArchiveType.SRC_TAR == ext) {
                    continue;
                }

                pkg.setArchiveType(ext);

                TermOfSupport supTerm = null;
                if (!vNumber.getFeature().isEmpty()) {
                    supTerm = Helper.getTermOfSupport(vNumber, Distro.TRAVA);
                }
                pkg.setTermOfSupport(supTerm);

                pkg.setDistribution(Distro.TRAVA.get());
                pkg.setFileName(filename);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setVersionNumber(vNumber);
                pkg.setJavaVersion(vNumber);
                pkg.setDistributionVersion(vNumber);
                pkg.setPackageType(filename.contains(Constants.JRE_POSTFIX) ? JRE : JDK);
                pkg.setReleaseStatus(filename.contains(Constants.EA_POSTFIX) ? EA : GA);


                Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                 .filter(entry -> filename.contains(entry.getKey()))
                                                                 .findFirst()
                                                                 .map(Entry::getValue)
                                                                 .orElse(Architecture.NONE);
                if (Architecture.NONE == arch) {
                    LOGGER.debug("Architecture not found in Trava for filename: {}", filename);
                    arch = Architecture.X64;
                }
                pkg.setArchitecture(arch);
                pkg.setBitness(arch.getBitness());

                OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> filename.contains(entry.getKey()))
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
                    LOGGER.debug("Operating System not found in Trava for filename: {}", filename);
                    continue;
                }
                pkg.setOperatingSystem(os);

                pkg.setFreeUseInProduction(Boolean.TRUE);

                pkgs.add(pkg);
            }
        }

        LOGGER.debug("Successfully fetched {} packages from {}", pkgs.size(), PACKAGE_URL);
        return pkgs;
    }
}
