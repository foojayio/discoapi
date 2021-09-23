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

package io.foojay.api.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.distribution.AOJ;
import io.foojay.api.distribution.AOJ_OPENJ9;
import io.foojay.api.distribution.Corretto;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.distribution.JetBrains;
import io.foojay.api.distribution.LibericaNative;
import io.foojay.api.distribution.Microsoft;
import io.foojay.api.distribution.OJDKBuild;
import io.foojay.api.distribution.OpenLogic;
import io.foojay.api.distribution.Oracle;
import io.foojay.api.distribution.OracleOpenJDK;
import io.foojay.api.distribution.RedHat;
import io.foojay.api.distribution.SAPMachine;
import io.foojay.api.distribution.Semeru;
import io.foojay.api.distribution.Temurin;
import io.foojay.api.distribution.Trava;
import io.foojay.api.distribution.Zulu;
import io.foojay.api.distribution.ZuluPrime;
import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.HashAlgorithm;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.scopes.BasicScope;
import io.foojay.api.scopes.BuildScope;
import io.foojay.api.scopes.DownloadScope;
import io.foojay.api.scopes.IDEScope;
import io.foojay.api.scopes.Scope;
import io.foojay.api.scopes.SignatureScope;
import io.foojay.api.scopes.UsageScope;
import io.foojay.api.scopes.YamlScopes;
import io.micronaut.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static io.foojay.api.util.Constants.API_VERSION_V1;
import static io.foojay.api.util.Constants.API_VERSION_V2;
import static io.foojay.api.util.Constants.COLON;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.INDENT;
import static io.foojay.api.util.Constants.INDENTED_QUOTES;
import static io.foojay.api.util.Constants.MESSAGE;
import static io.foojay.api.util.Constants.NEW_LINE;
import static io.foojay.api.util.Constants.QUOTES;
import static io.foojay.api.util.Constants.RESULT;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_OPEN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;


public class Helper {
    private static final Logger     LOGGER                                 = LoggerFactory.getLogger(Helper.class);
    public  static final Pattern    FILE_URL_PATTERN                       = Pattern.compile("(JDK|JRE)(\\s+\\|\\s?\\[[a-zA-Z0-9\\-\\._]+\\]\\()(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)(\\.zip|\\.msi|\\.pkg|\\.dmg|\\.tar\\.gz(?!\\.sig)|\\.deb|\\.rpm|\\.cab|\\.7z))");
    public  static final Pattern    FILE_URL_MD5_PATTERN                   = Pattern.compile("(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)(\\.zip|\\.msi|\\.pkg|\\.dmg|\\.tar\\.gz|\\.deb|\\.rpm|\\.cab|\\.7z))\\)\\h+\\|\\h+`([0-9a-z]{32})`");
    public  static final Pattern    CORRETTO_SIG_URI_PATTERN               = Pattern.compile("((\\[Download\\])\\(?(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)(\\.zip\\.sig|\\.tar\\.gz\\.sig)))\\)");
    public  static final Pattern    DRAGONWELL_11_FILE_NAME_SHA256_PATTERN = Pattern.compile("(OpenJDK[0-9]+U[a-z0-9_\\-\\.]+)(\\.zip|\\.msi|\\.pkg|\\.dmg|\\.tar\\.gz|\\.deb|\\.rpm|\\.cab|\\.7z)(\\s+\\(Experimental ONLY\\))?\\h+\\|\\h+([0-9a-z]{64})");
    public  static final Pattern    DRAGONWELL_8_FILE_NAME_SHA256_PATTERN  = Pattern.compile("(\\()?(Alibaba_Dragonwell[0-9\\.A-Za-z_\\-]+)(\\)=\\s+)?|([\\\\r\\\\n]+)?([a-z0-9]{64})");
    public  static final Pattern    HREF_FILE_PATTERN                      = Pattern.compile("href=\"([^\"]*(\\.zip|\\.msi|\\.pkg|\\.dmg|\\.tar\\.gz|\\.deb|\\.rpm|\\.cab|\\.7z))\"");
    public  static final Pattern    HREF_SIG_FILE_PATTERN                  = Pattern.compile("href=\"([^\"]*(\\.sig))\"");
    public  static final Pattern    HREF_DOWNLOAD_PATTERN                  = Pattern.compile("(\\>)(\\s|\\h?(jdk|jre|serverjre)-(([0-9]+\\.[0-9]+\\.[0-9]+_[a-z]+-[a-z0-9]+_)|([0-9]+u[0-9]+-[a-z]+-[a-z0-9]+(-vfp-hflt)?)).*[a-zA-Z]+)(\\<)");
    public  static final Matcher    FILE_URL_MATCHER                       = FILE_URL_PATTERN.matcher("");
    public  static final Matcher    FILE_URL_MD5_MATCHER                   = FILE_URL_MD5_PATTERN.matcher("");
    public  static final Matcher    CORRETTO_SIG_URI_MATCHER               = CORRETTO_SIG_URI_PATTERN.matcher("");
    public  static final Matcher    DRAGONWELL_11_FILE_NAME_SHA256_MATCHER = DRAGONWELL_11_FILE_NAME_SHA256_PATTERN.matcher("");
    public  static final Matcher    DRAGONWELL_8_FILE_NAME_SHA256_MATCHER  = DRAGONWELL_8_FILE_NAME_SHA256_PATTERN.matcher("");
    public  static final Matcher    HREF_FILE_MATCHER                      = HREF_FILE_PATTERN.matcher("");
    public  static final Matcher    HREF_SIG_FILE_MATCHER                  = HREF_SIG_FILE_PATTERN.matcher("");
    public  static final Matcher    HREF_DOWNLOAD_MATCHER                  = HREF_DOWNLOAD_PATTERN.matcher("");
    private static       HttpClient httpClient;
    private static       HttpClient httpClientAsync;


    public static final Callable<List<Pkg>> createTask(final Distro distro) {
        return () -> getPkgsOfDistro(distro);
    }

    public static final List<Pkg> getPkgsOfDistro(final Distro distro) {
        final List<Pkg> pkgs = new LinkedList<>();
        try {
            switch (distro) {
                case ZULU:
                    Zulu zulu = (Zulu) distro.get();
                    // Get packages from API
                    for (MajorVersion majorVersion : CacheManager.INSTANCE.getMajorVersions()) {
                        pkgs.addAll(getPkgs(zulu, majorVersion.getVersionNumber(), false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null, ReleaseStatus.NONE, TermOfSupport.NONE));
                    }

                    // Get packages from CDN
                    List<Pkg> cdnPkgs   = ((Zulu) Distro.ZULU.get()).getAllPackagesFromCDN();
                    List<Pkg> pkgsToAdd = new LinkedList<>();
                    List<String> pkgsFilenames = pkgs.stream().map(pkg -> pkg.getFileName()).collect(Collectors.toList());
                    for (Pkg cdnPkg : cdnPkgs) {
                        if (!pkgsFilenames.contains(cdnPkg.getFileName())) {
                            pkgsToAdd.add(cdnPkg);
                        }
                    }
                    pkgs.addAll(pkgsToAdd);
                    break;
                case ZULU_PRIME:
                    ZuluPrime zuluPrime = (ZuluPrime) distro.get();
                    pkgs.addAll(zuluPrime.getAllPkgs());
                    break;
                case CORRETTO:
                    Corretto corretto = (Corretto) distro.get();
                    pkgs.addAll(corretto.getAllPkgs());
                    CacheManager.INSTANCE.getMajorVersions().stream().forEach(majorVersion ->
                        pkgs.addAll(getPkgs(corretto, majorVersion.getVersionNumber(), false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE,
                                            ArchiveType.NONE, PackageType.NONE, null, ReleaseStatus.NONE, TermOfSupport.NONE)));
                    break;
                case ORACLE:
                    Oracle oracle = (Oracle) distro.get();
                    pkgs.addAll(oracle.getAllPkgs());
                    break;
                case RED_HAT:
                    RedHat redhat = (RedHat) distro.get();
                    pkgs.addAll(redhat.getAllPkgs());
                    break;
                case OJDK_BUILD:
                    OJDKBuild ojdkBuild = (OJDKBuild) distro.get();
                    pkgs.addAll(ojdkBuild.getAllPkgs());
                    break;
                case ORACLE_OPEN_JDK:
                    OracleOpenJDK oracleOpenJDK = (OracleOpenJDK) distro.get();
                    pkgs.addAll(oracleOpenJDK.getAllPkgs());
                    // Get all jdk 8 packages from github
                    String       query8     = oracleOpenJDK.getGithubPkg8Url() + "/releases?per_page=100";
                    HttpResponse<String> response8 = get(query8);
                    if (null == response8) {
                        LOGGER.debug("Response (Oracle OpenJDK) returned null");
                    } else {
                    if (response8.statusCode() == 200) {
                        String      bodyText = response8.body();
                            Gson        gson     = new Gson();
                            JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                            if (element instanceof JsonArray) {
                                JsonArray jsonArray = element.getAsJsonArray();
                                pkgs.addAll(oracleOpenJDK.getAllPkgs(jsonArray));
                            }
                        } else {
                            // Problem with url request
                            LOGGER.debug("Response (Status Code {}) {} ", response8.statusCode(), response8.body());
                        }
                    }
                    // Get all jdk 11 packages from github
                    String       query11    = oracleOpenJDK.getGithubPkg11Url() + "/releases?per_page=100";
                    HttpResponse<String> response11 = get(query11);
                    if (null == response11) {
                        LOGGER.debug("Response (Oracle OpenJDK) returned null");
                    } else {
                    if (response11.statusCode() == 200) {
                        String      bodyText = response11.body();
                            Gson        gson     = new Gson();
                            JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                            if (element instanceof JsonArray) {
                                JsonArray jsonArray = element.getAsJsonArray();
                                pkgs.addAll(oracleOpenJDK.getAllPkgs(jsonArray));
                            }
                        } else {
                            // Problem with url request
                            LOGGER.debug("Response (Status Code {}) {} ", response11.statusCode(), response11.body());
                        }
                    }
                    break;
                case SAP_MACHINE:
                    SAPMachine  sapMachine = (SAPMachine) distro.get();

                    // Search through github release and fetch packages from there
                        String querySAPMachine = sapMachine.getPkgUrl() + "?per_page=100";
                        HttpResponse<String> responseSAPMachine = get(querySAPMachine, Map.of("accept", "application/vnd.github.v3+json",
                                                                                              "authorization", GithubTokenPool.INSTANCE.next()));
                        if (null == responseSAPMachine) {
                        LOGGER.debug("Response (SAP Machine) returned null");
                    } else {
                            if (responseSAPMachine.statusCode() == 200) {
                                String      bodyText = responseSAPMachine.body();
                                Gson        gson     = new Gson();
                                JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                                if (element instanceof JsonArray) {
                                    JsonArray jsonArray = element.getAsJsonArray();
                                    pkgs.addAll(sapMachine.getAllPkgsFromJson(jsonArray));
                                }
                            } else {
                                // Problem with url request
                                LOGGER.debug("Response (Status Code {}) {} ", responseSAPMachine.statusCode(), responseSAPMachine.body());
                            }
                        }
                    // Fetch packages from sap.github.io -> sapmachine_releases.json
                    pkgs.addAll(sapMachine.getAllPkgsFromJsonUrl());

                    // Fetch major versions 10, 12, 13 from github
                    pkgs.addAll(sapMachine.getAllPkgs());
                    break;
                case SEMERU:
                    Semeru semeru = (Semeru) distro.get();
                    pkgs.addAll(semeru.getAllPkgs());
                    break;
                case TRAVA:
                    Trava trava = (Trava) distro.get();
                    pkgs.addAll(trava.getAllPkgs());
                    break;
                case AOJ:
                    AOJ AOJ = (AOJ) distro.get();
                    for (MajorVersion majorVersion : CacheManager.INSTANCE.getMajorVersions()) {
                        VersionNumber versionNumber = majorVersion.getVersionNumber();
                            pkgs.addAll(getPkgs(AOJ, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null,
                                                ReleaseStatus.GA, TermOfSupport.NONE));
                            pkgs.addAll(getPkgs(AOJ, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null,
                                                ReleaseStatus.EA, TermOfSupport.NONE));
                    }
                    break;
                case AOJ_OPENJ9:
                    AOJ_OPENJ9 AOJ_OPENJ9 = (AOJ_OPENJ9) distro.get();
                    for (MajorVersion majorVersion : CacheManager.INSTANCE.getMajorVersions()) {
                        VersionNumber versionNumber = majorVersion.getVersionNumber();
                            pkgs.addAll(getPkgs(AOJ_OPENJ9, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null,
                                                ReleaseStatus.GA, TermOfSupport.NONE));
                            pkgs.addAll(getPkgs(AOJ_OPENJ9, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null,
                                                ReleaseStatus.EA, TermOfSupport.NONE));
                    }
                    break;
                case TEMURIN:
                    Temurin temurin = (Temurin) distro.get();
                    CacheManager.INSTANCE.getMajorVersions().stream().filter(majorVersion -> majorVersion.getAsInt() >= 8).forEach(majorVersion -> {
                        VersionNumber versionNumber = majorVersion.getVersionNumber();

                        pkgs.addAll(getPkgs(temurin, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null,
                                            ReleaseStatus.GA, TermOfSupport.NONE));

                        pkgs.addAll(getPkgs(temurin, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null,
                                            ReleaseStatus.EA, TermOfSupport.NONE));
                    });
                    /*
                        List<Pkg> temurinPkgs = temurin.getAllPkgs();
                        List<MajorVersion> earlyAccessOnly = CacheManager.INSTANCE.getMajorVersions().stream()
                                                                                  .filter(majorVersion -> majorVersion.isEarlyAccessOnly())
                                                                                  .collect(Collectors.toList());
                        earlyAccessOnly.forEach(majorVersion -> {
                            Optional<Pkg> optionalPkg = temurinPkgs.stream()
                                                                   .filter(pkg -> majorVersion.getAsInt() == pkg.getMajorVersion().getAsInt())
                                                                   .findFirst();
                            if (optionalPkg.isPresent()) {
                                VersionNumber javaVersion         = optionalPkg.get().getJavaVersion();
                                List<Pkg>     temurinPkgsToRemove = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                                                  .stream()
                                                                                                  .filter(pkg -> pkg.getDistribution().getDistro() == Distro.TEMURIN)
                                                                                                  .filter(pkg -> pkg.getJavaVersion().equals(javaVersion))
                                                                                                  .collect(Collectors.toList());
                            temurinPkgsToRemove.forEach(pkg -> CacheManager.INSTANCE.pkgCache.remove(pkg.getId()));
                        }
                    });
                    pkgs.addAll(temurinPkgs);
                    */
                    break;
                case MICROSOFT:
                    Microsoft microsoft = (Microsoft) distro.get();
                    pkgs.addAll(microsoft.getAllPkgs());
                    break;
                case JETBRAINS:
                    JetBrains jetbrains = (JetBrains) distro.get();
                    // Add all packages from properties file
                    pkgs.addAll(jetbrains.getAllPkgs());
                    // Add packages from latest github release
                    String queryJetbrains = jetbrains.GITHUB_RELEASES_URL;
                    HttpResponse<String> responseJetbrains = Helper.get(queryJetbrains, Map.of("accept", "application/vnd.github.v3+json",
                                                                                                   "authorization", GithubTokenPool.INSTANCE.next()));
                    if (null == responseJetbrains) {
                        LOGGER.debug("Response (JetBrains) returned null");
                    } else {
                        if (responseJetbrains.statusCode() == 200) {
                            String      bodyText = responseJetbrains.body();
                            Gson        gson    = new Gson();
                            JsonElement element = gson.fromJson(bodyText, JsonElement.class);
                            if (element instanceof JsonObject) {
                                JsonObject jsonObject = element.getAsJsonObject();
                                if (jsonObject.has("body")) {
                                    pkgs.addAll(jetbrains.getAllPkgsFromString(jsonObject.get("body").getAsString()));
                                }
                            }
                        } else {
                            // Problem with url request
                            LOGGER.debug("Response (Status Code {}) {} ", responseJetbrains.statusCode(), responseJetbrains.body());
                        }
                    }
                    break;
                case LIBERICA_NATIVE:
                    LibericaNative libericaNative = (LibericaNative) distro.get();
                    pkgs.addAll(libericaNative.getAllPkgs());
                    break;
                    case OPEN_LOGIC:
                        OpenLogic openLogic = (OpenLogic) distro.get();
                        pkgs.addAll(openLogic.getAllPkgs());
                    break;
                default:
                    Distribution distribution = distro.get();
                    CacheManager.INSTANCE.getMajorVersions().stream().forEach(majorVersion -> {
                        pkgs.addAll(getPkgs(distribution, majorVersion.getVersionNumber(), false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE,
                                           PackageType.NONE, null, ReleaseStatus.NONE, TermOfSupport.NONE));
                    });
                    break;
                }
        } catch (Exception e) {
            LOGGER.debug("There was a problem fetching packages for {}. {}", distro, e.getMessage());
        }
        List<Pkg> unique = pkgs.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(Pkg::getId))), LinkedList::new));
        return new LinkedList<>(unique);
    }

    private static final List<Pkg> getPkgs(final Distribution distribution, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                     final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                     final Boolean fx, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        String query = distribution.getUrlForAvailablePkgs(versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
        if (query.isEmpty()) { return List.of(); }

        try {
        List<Pkg>   pkgs    = new LinkedList<>();
            List<Pkg> pkgsFound = new ArrayList<>();
            if (distribution.equals(Distro.ORACLE_OPEN_JDK.get())) {
                List<Pkg> pkgsInDistribution = distribution.getPkgFromJson(null, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
                pkgsFound.addAll(pkgsInDistribution.stream().filter(pkg -> isVersionNumberInPkg(versionNumber, pkg)).collect(Collectors.toList()));
            } else {
                Map<String, String> headers = new HashMap<>();
                if (query.contains("api.github.com")) {
                    headers.put("accept", "application/vnd.github.v3+json");
                    headers.put("authorization", GithubTokenPool.INSTANCE.next());
                }
                HttpResponse<String> response = get(query, headers);
            if (null == response) {
                LOGGER.debug("Response {} returned null.", distribution.getDistro().getApiString());
            } else {
                if (response.statusCode() == 200) {
                String      body     = response.body();
                    Gson        gson     = new Gson();
                JsonElement element  = gson.fromJson(body, JsonElement.class);
                    if (element instanceof JsonArray) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject pkgJsonObj         = jsonArray.get(i).getAsJsonObject();
                                List<Pkg> pkgsInDistribution = distribution.getPkgFromJson(pkgJsonObj, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
                        pkgsFound.addAll(pkgsInDistribution);
                        }
                    } else if (element instanceof JsonObject) {
                        JsonObject pkgJsonObj         = element.getAsJsonObject();
                            List<Pkg> pkgsInDistribution = distribution.getPkgFromJson(pkgJsonObj, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
                    pkgsFound.addAll(pkgsInDistribution);
                    }
                } else {
                    // Problem with url request
                    LOGGER.debug("Error get packages for {} {} calling {}", distribution.getName(), versionNumber, query);
                    LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
                    return pkgs;
                }
            }
        }
            if (latest) {
                Optional<Pkg> pkgWithMaxVersionNumber = pkgsFound.stream().max(Comparator.comparing(Pkg::getVersionNumber));
                if (pkgWithMaxVersionNumber.isPresent()) {
                    VersionNumber maxNumber = pkgWithMaxVersionNumber.get().getVersionNumber();
                    pkgsFound = pkgsFound.stream().filter(pkg -> pkg.getVersionNumber().compareTo(maxNumber) == 0).collect(Collectors.toList());
                }
            }
            pkgs.addAll(pkgsFound);
            HashSet<Pkg> unique = new HashSet<>(pkgs);
            pkgs = new LinkedList<>(unique);

        return pkgs;
        } catch (Exception e) {
            LOGGER.debug("Error get packages for {} {} calling {}. {}", distribution.getName(), versionNumber, query, e.getMessage());
            return new ArrayList<>();
        }
    }

    private static final List<Pkg> getPkgsAsync(final Distribution distribution, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                          final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                          final Boolean fx, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        String query = distribution.getUrlForAvailablePkgs(versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
        try {
        Collection<CompletableFuture<List<Pkg>>> futures = Collections.synchronizedList(new ArrayList<>());
            Map<String, String> headers = new HashMap<>();
            if (query.contains("api.github.com")) { headers.put("accept", "application/vnd.github.v3+json"); }
            CompletableFuture<List<Pkg>> future = Helper.getAsync(query, headers).thenApply(response -> {
            List<Pkg> pkgs      = new LinkedList<>();
            List<Pkg> pkgsFound = new ArrayList<>();
            if (null == response) {
                LOGGER.debug("Response {} returned null.", distribution.getDistro().getApiString());
            } else {
                if (response.statusCode() == 200) {
                    String      body    = response.body();
                    Gson        gson    = new Gson();
                    JsonElement element = gson.fromJson(body, JsonElement.class);
                    if (element instanceof JsonArray) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        for (int j = 0; j < jsonArray.size(); j++) {
                            JsonObject pkgJsonObj = jsonArray.get(j).getAsJsonObject();
                                List<Pkg> pkgsInDistribution = distribution.getPkgFromJson(pkgJsonObj, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
                            pkgsFound.addAll(pkgsInDistribution);
                        }
                    } else if (element instanceof JsonObject) {
                        JsonObject pkgJsonObj = element.getAsJsonObject();
                            List<Pkg> pkgsInDistribution = distribution.getPkgFromJson(pkgJsonObj, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
                        pkgsFound.addAll(pkgsInDistribution);
                    }
                } else {
                    LOGGER.debug("Response (Status Code {}) {} ", response.statusCode(), response.body());
                }
            }

            if (latest) {
                Optional<Pkg> pkgWithMaxVersionNumber = pkgsFound.stream().max(Comparator.comparing(Pkg::getVersionNumber));
                if (pkgWithMaxVersionNumber.isPresent()) {
                    VersionNumber maxNumber = pkgWithMaxVersionNumber.get().getVersionNumber();
                    pkgsFound = pkgsFound.stream().filter(pkg -> pkg.getVersionNumber().compareTo(maxNumber) == 0).collect(Collectors.toList());
                }
            }
            pkgs.addAll(pkgsFound);
            HashSet<Pkg> unique = new HashSet<>(pkgs);
            pkgs = new LinkedList<>(unique);

            return pkgs;
        });
        futures.add(future);

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(f -> futures.stream()
                                                                                                 .map(completableFuture -> completableFuture.join())
                                                                                                 .collect(Collectors.toList()));

            List<Pkg> pkgs = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                                 .thenApply(f -> futures.stream()
                                                                        .map(completableFuture -> completableFuture.join())
                                                                        .flatMap(Collection::stream)
                                                                        .collect(Collectors.toList()))
                                                 .get();
            return pkgs;
        } catch (InterruptedException | CancellationException | ExecutionException e) {
            LOGGER.debug("Error get packages async for {} {} calling {}. {}", distribution.getName(), versionNumber, query, e.getMessage());
            return new ArrayList<>();
        }
    }

    private static final boolean isVersionNumberInPkg(final VersionNumber versionNumber, final Pkg pkg) {
        if (pkg.getVersionNumber().getFeature().isEmpty() || versionNumber.getFeature().isEmpty()) {
            throw new IllegalArgumentException("Version number must have feature number");
        }
        boolean featureFound = pkg.getVersionNumber().getFeature().getAsInt() == versionNumber.getFeature().getAsInt();
        if (versionNumber.getInterim().isPresent()) {
            boolean interimFound = false;
            if (pkg.getVersionNumber().getInterim().isPresent()) {
                interimFound = pkg.getVersionNumber().getInterim().getAsInt() == versionNumber.getInterim().getAsInt();
                if (versionNumber.getUpdate().isPresent()) {
                    boolean updateFound = false;
                    if (pkg.getVersionNumber().getUpdate().isPresent()) {
                        updateFound = pkg.getVersionNumber().getUpdate().getAsInt() == versionNumber.getUpdate().getAsInt();
                        if (versionNumber.getPatch().isPresent()) {
                            boolean patchFound = false;
                            if (pkg.getVersionNumber().getPatch().isPresent()) {
                                patchFound = pkg.getVersionNumber().getPatch().getAsInt() == versionNumber.getPatch().getAsInt();
                            }
                            return featureFound && interimFound && updateFound && patchFound;
                        }
                    }
                    return featureFound && interimFound && updateFound;
                }
            }
            return featureFound && interimFound;
        }
        return featureFound;
    }

    public static final ArchiveType getFileEnding(final String fileName) {
        if (null == fileName || fileName.isEmpty()) { return ArchiveType.NONE; }
        for (ArchiveType archiveType : ArchiveType.values()) {
            for (String ending : archiveType.getFileEndings()) {
                if (fileName.endsWith(ending)) { return archiveType; }
            }
        }
        return ArchiveType.NONE;
    }

    public static final Set<String> getFileUrlsFromString(final String text) {
        Set<String> urlsFound = new HashSet<>();
        FILE_URL_MATCHER.reset(text);
        while (FILE_URL_MATCHER.find()) {
            // JDK / JRE -> FILE_URL_MATCHER.group(1)
            // File URL  -> FILE_URL_MATCHER.group(3)
            urlsFound.add(FILE_URL_MATCHER.group(3));
        }
        return urlsFound;
    }

    public static final Set<Pair<String,String>> getPackageTypeAndFileUrlFromString(final String text) {
        Set<Pair<String,String>> pairsFound = new HashSet<>();
        FILE_URL_MATCHER.reset(text);
        while (FILE_URL_MATCHER.find()) {
            pairsFound.add(new Pair<>(FILE_URL_MATCHER.group(1), FILE_URL_MATCHER.group(3)));
        }
        return pairsFound;
    }

    public static final Set<Pair<String,String>> getFileUrlsAndMd5sFromString(final String text) {
        Set<Pair<String,String>> pairsFound = new HashSet<>();
        FILE_URL_MD5_MATCHER.reset(text);
        while(FILE_URL_MD5_MATCHER.find()) {
            pairsFound.add(new Pair<>(FILE_URL_MD5_MATCHER.group(1), FILE_URL_MD5_MATCHER.group(5)));
        }
        return pairsFound;
    }

    public static final Set<Pair<String,String>> getFileNameAndSha256FromStringDragonwell8(final String text) {
        Set<Pair<String,String>> pairsFound = new HashSet<>();
        DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.reset(text);
        while(DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.find()) {
            pairsFound.add(new Pair<>(DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.group(1) + DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.group(2), DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.group(4)));
        }
        return pairsFound;
    }

    public static final Set<Pair<String,String>> getDragonwell11FileNameAndSha256FromString(final String text) {
        Set<Pair<String,String>> pairsFound = new HashSet<>();
        DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.reset(text);
        while(DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.find()) {
            pairsFound.add(new Pair<>(DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.group(1) + DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.group(2), DRAGONWELL_11_FILE_NAME_SHA256_MATCHER.group(4)));
        }
        return pairsFound;
    }

    public static final Set<Pair<String,String>> getDragonwell8FileNameAndSha256FromString(final String text) {
        Set<Pair<String,String>> pairsFound = new HashSet<>();
        DRAGONWELL_8_FILE_NAME_SHA256_MATCHER.reset(text);
        final List<MatchResult> results = DRAGONWELL_8_FILE_NAME_SHA256_MATCHER.results().collect(Collectors.toList());
        boolean filenameFound = false;
        String  filename      = "";
        boolean sha256Found   = false;
        String  sha256        = "";
        for (MatchResult result : results) {
            String group0 = result.group(0);
            if (null != group0) {
                if (group0.length() == 64) {
                    sha256Found = true;
                    sha256 = group0.trim();
                    if (filenameFound) {
                        pairsFound.add(new Pair<>(filename, sha256));
                        sha256Found = false;
                        filenameFound = false;
                    }
                }
            }
            String group2 = result.group(2);
            if (null != group2) {
                if (group2.startsWith("Alibaba")) {
                    filenameFound = true;
                    filename      = group2.trim();
                    if (sha256Found) {
                        pairsFound.add(new Pair<>(filename, sha256));
                        filenameFound = false;
                        sha256Found   = false;
                    }
                }
            }
        }
        return pairsFound;
    }

    public static final Map<String,String> getCorrettoSignatureUris(final String text) {
        Map signatureUrisFound = new HashMap<>();
        CORRETTO_SIG_URI_MATCHER.reset(text);
        while(CORRETTO_SIG_URI_MATCHER.find()) {
            String sigUri   = CORRETTO_SIG_URI_MATCHER.group(3);
            String filename = (sigUri.substring(sigUri.lastIndexOf("/") + 1)).replaceAll("\\.sig|\\.SIG", "");
            signatureUrisFound.put(filename, sigUri);
        }
        return signatureUrisFound;
    }

    public static final Set<String> getFileHrefsFromString(final String text) {
        Set<String> hrefsFound = new HashSet<>();
        HREF_FILE_MATCHER.reset(text);
        while (HREF_FILE_MATCHER.find()) {
            hrefsFound.add(HREF_FILE_MATCHER.group(1));
        }
        return hrefsFound;
    }

    public static final Set<String> getSigFileHrefsFromString(final String text) {
        Set<String> sigHrefsFound = new HashSet<>();
        HREF_SIG_FILE_MATCHER.reset(text);
        while (HREF_SIG_FILE_MATCHER.find()) {
            sigHrefsFound.add(HREF_SIG_FILE_MATCHER.group(1).toLowerCase());
        }
        return sigHrefsFound;
    }

    public static final Set<String> getDownloadHrefsFromString(final String text) {
        Set<String> hrefsFound = new HashSet<>();
        HREF_DOWNLOAD_MATCHER.reset(text);
        while (HREF_DOWNLOAD_MATCHER.find()) {
            hrefsFound.add(HREF_DOWNLOAD_MATCHER.group(2).trim().replaceFirst("\\h", ""));
        }
        return hrefsFound;
    }

    public static final String getFileNameFromText(final String text) {
        ArchiveType archiveTypeFound = getFileEnding(text);
        if (ArchiveType.NONE == archiveTypeFound || ArchiveType.NOT_FOUND == archiveTypeFound) { return ""; }
        int    lastSlash = text.lastIndexOf("/") + 1;
        String fileName  = text.substring(lastSlash);
        return fileName;
    }

    public static final boolean isReleaseTermOfSupport(final int featureVersion, final TermOfSupport termOfSupport) {
        switch(termOfSupport) {
            case LTS: return isLTS(featureVersion);
            case MTS: return isMTS(featureVersion);
            case STS: return isSTS(featureVersion);
            default : return false;
        }
    }

    public static final boolean isSTS(final int featureVersion) {
        if (featureVersion < 9) { return false; }
        switch(featureVersion) {
            case 9 :
            case 10: return true;
            default: return !isLTS(featureVersion);
        }
    }

    public static final boolean isMTS(final int featureVersion) {
        if (featureVersion < 13) { return false; }
        return (!isLTS(featureVersion)) && featureVersion % 2 != 0;
    }

    public static final boolean isLTS(final int featureVersion) {
        if (featureVersion < 1) { throw new IllegalArgumentException("Feature version number cannot be smaller than 1"); }
        if (featureVersion <= 8) { return true; }
        if (featureVersion < 11) { return false; }
        if (featureVersion < 17) { return ((featureVersion - 11.0) / 6.0) % 1 == 0; }
        return ((featureVersion - 17.0) / 4.0) % 1 == 0;
    }

    public static final TermOfSupport getTermOfSupport(final VersionNumber versionNumber, final Distro distribution) {
        TermOfSupport termOfSupport = getTermOfSupport(versionNumber);
        switch(termOfSupport) {
            case LTS:
            case STS: return termOfSupport;
            case MTS: return Distro.ZULU == distribution ? termOfSupport : TermOfSupport.STS;
            default : return TermOfSupport.NOT_FOUND;
        }
    }
    public static final TermOfSupport getTermOfSupport(final VersionNumber versionNumber) {
        if (!versionNumber.getFeature().isPresent() || versionNumber.getFeature().isEmpty()) {
            throw new IllegalArgumentException("VersionNumber need to have a feature version");
        }
        return getTermOfSupport(versionNumber.getFeature().getAsInt());
    }
    public static final TermOfSupport getTermOfSupport(final int featureVersion) {
        if (featureVersion < 1) { throw new IllegalArgumentException("Feature version number cannot be smaller than 1"); }
        if (isLTS(featureVersion)) {
            return TermOfSupport.LTS;
        } else if (isMTS(featureVersion)) {
            return TermOfSupport.MTS;
        } else if (isSTS(featureVersion)) {
            return TermOfSupport.STS;
        } else {
            return TermOfSupport.NOT_FOUND;
        }
    }

    public static final void setTermOfSupport(final VersionNumber versionNumber, final Pkg pkg) {
        int     featureVersion = versionNumber.getFeature().getAsInt();
        boolean isZulu         = pkg.getDistribution() instanceof Zulu;

        if (isLTS(featureVersion)) {
            pkg.setTermOfSupport(TermOfSupport.LTS);
        } else if (isZulu) {
            if (isMTS(featureVersion)) {
                pkg.setTermOfSupport(TermOfSupport.MTS);
            } else {
                pkg.setTermOfSupport(TermOfSupport.STS);
            }
        } else {
            pkg.setTermOfSupport(TermOfSupport.STS);
        }
    }

    public static final String getTextFromUrl(final String uri) {
        try (var stream = URI.create(uri).toURL().openStream()) {
            return new String(stream.readAllBytes(), UTF_8);
        } catch(Exception e) {
            LOGGER.debug("Error reading text from uri {}", uri);
            return "";
        }
    }

    public static final YamlScopes loadYamlScopes(final String uri) {
        Constructor constructor = new Constructor(YamlScopes.class);
        Yaml        yaml        = new Yaml(constructor );
        HttpResponse<String> response = get(uri);
        if (null == response) { return null; }
        if (null == response.body() || response.body().isEmpty()) { return null; }
        return yaml.loadAs(response.body(), YamlScopes.class);
    }

    public static final int getLeadingNumbers(final String text) {
        String[]      parts = text.split("");
        StringBuilder numberBuilder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if((parts[i].matches("[0-9]+"))) {
                numberBuilder.append(parts[i]);
            } else {
                return Integer.parseInt(numberBuilder.toString());
            }
        }
        return 0;
    }

    public static final String readFromInputStream(final InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    public static final boolean isPositiveInteger(final String text) {
        if (null == text || text.isEmpty()) { return false; }
        return Constants.POSITIVE_INTEGER_PATTERN.matcher(text).matches();
    }

    public static final void removeEmptyItems(final List<String> list) {
        if (null == list) { return; }
        list.removeIf(item -> item == null || "".equals(item));
    }

    public static final long getCRC32Checksum(final byte[] bytes) {
        Checksum crc32 = new CRC32();

        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    public static final String getHash(final HashAlgorithm hashAlgorithm, final String text) {
        switch (hashAlgorithm) {
            case MD5     : return getMD5(text);
            case SHA1    : return getSHA1(text);
            case SHA256  : return getSHA256(text);
            case SHA3_256: return getSHA3_256(text);
            default      : return "";
        }
    }

    public static final String getMD5(final String text) { return bytesToHex(getMD5Bytes(text.getBytes(UTF_8))); }
    public static final String getMD5(final byte[] bytes) {
        return bytesToHex(getMD5Bytes(bytes));
    }
    public static final byte[] getMD5Bytes(final byte[] bytes) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Error getting MD5 algorithm. {}", e.getMessage());
            return new byte[]{};
        }
        final byte[] result = md.digest(bytes);
        return result;
    }
    public static final String getMD5ForFile(final File file) throws Exception {
        final MessageDigest md  = MessageDigest.getInstance("MD5");
        final InputStream   fis = new FileInputStream(file);
        try {
            int n = 0;
            byte[] buffer = new byte[4096];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    md.update(buffer, 0, n);
                }
            }
        } finally {
            fis.close();
        }
        byte byteData[] = md.digest();
        return getMD5(bytesToHex(byteData));
    }

    public static final String getSHA1(final String text) { return bytesToHex(getSHA1Bytes(text.getBytes(UTF_8))); }
    public static final String getSHA1(final byte[] bytes) {
        return bytesToHex(getSHA1Bytes(bytes));
    }
    public static final byte[] getSHA1Bytes(final byte[] bytes) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Error getting SHA-1 algorithm. {}", e.getMessage());
            return new byte[]{};
        }
        final byte[] result = md.digest(bytes);
        return result;
    }
    public static final String getSHA1ForFile(final File file) throws Exception {
        final MessageDigest md  = MessageDigest.getInstance("SHA-1");
        final InputStream   fis = new FileInputStream(file);
        try {
            int n = 0;
            byte[] buffer = new byte[4096];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    md.update(buffer, 0, n);
                }
            }
        } finally {
            fis.close();
        }
        byte byteData[] = md.digest();
        return getSHA1(bytesToHex(byteData));
    }

    public static final String getSHA256(final String text) { return bytesToHex(getSHA256Bytes(text.getBytes(UTF_8))); }
    public static final String getSHA256(final byte[] bytes) {
        return bytesToHex(getSHA256Bytes(bytes));
    }
    public static final byte[] getSHA256Bytes(final byte[] bytes) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Error getting SHA2-256 algorithm. {}", e.getMessage());
            return new byte[]{};
        }
        final byte[] result = md.digest(bytes);
        return result;
    }
    public static final String getSHA256ForFile(final File file) throws Exception {
        final MessageDigest md  = MessageDigest.getInstance("SHA-256");
        final InputStream   fis = new FileInputStream(file);
        try {
            int n = 0;
            byte[] buffer = new byte[4096];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    md.update(buffer, 0, n);
                }
            }
        } finally {
            fis.close();
        }
        byte byteData[] = md.digest();
        return getSHA256(bytesToHex(byteData));
    }

    public static final String getSHA3_256(final String text) { return bytesToHex(getSHA3_256Bytes(text.getBytes(UTF_8))); }
    public static final String getSHA3_256(final byte[] bytes) {
        return bytesToHex(getSHA3_256Bytes(bytes));
    }
    public static final byte[] getSHA3_256Bytes(final byte[] bytes) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA3-256");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Error getting SHA3-256 algorithm. {}", e.getMessage());
            return new byte[]{};
        }
        final byte[] result = md.digest(bytes);
        return result;
    }
    public static final String getSHA3_256ForFile(final File file) throws Exception {
        final MessageDigest md  = MessageDigest.getInstance("SHA3-256");
        final InputStream   fis = new FileInputStream(file);
        try {
            int n = 0;
            byte[] buffer = new byte[4096];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    md.update(buffer, 0, n);
                }
            }
        } finally {
            fis.close();
        }
        byte byteData[] = md.digest();
        return getSHA3_256(bytesToHex(byteData));
    }

    public static final String bytesToHex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : bytes) { builder.append(String.format("%02x", b)); }
        return builder.toString();
    }

    public static final String createEphemeralId(final long number, final String id) {
        return getSHA1(number + id);
    }

    public static final String trimPrefix(final String text, final String prefix) {
        return text.replaceFirst(prefix, "");
    }

    public static final boolean isDifferentBuild(final Pkg pkg1, final Pkg pkg2) {
        return !pkg1.getDistribution().equals(pkg2.getDistribution())           ||
                pkg1.getVersionNumber().compareTo(pkg2.getVersionNumber()) !=0  ||
                pkg1.getArchitecture()    != pkg2.getArchitecture()             ||
                pkg1.getBitness()         != pkg2.getBitness()                  ||
                pkg1.getOperatingSystem() != pkg2.getOperatingSystem()          ||
                pkg1.getLibCType()        != pkg2.getLibCType()                 ||
                pkg1.getArchiveType()     != pkg2.getArchiveType()              ||
                pkg1.getPackageType()     != pkg2.getPackageType()              ||
                pkg1.getReleaseStatus()   != pkg2.getReleaseStatus()            ||
                pkg1.isJavaFXBundled()    != pkg2.isJavaFXBundled()             ||
                pkg1.getTermOfSupport()   != pkg2.getTermOfSupport();
    }

    public static final List<Pkg> getAllBuildsOfPackage(final Pkg pkg) {
        List<Pkg> differentBuilds = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                  .stream()
                                                                  .filter(p -> p.getDistribution().equals(pkg.getDistribution()))
                                                                  //.filter(p -> p.getVersionNumber().compareTo(pkg.getVersionNumber()) == 0)
                                                                  .filter(p -> p.getSemver().compareTo(pkg.getSemver()) == 0)
                                                                  .filter(p -> p.getArchitecture()    == pkg.getArchitecture())
                                                                  .filter(p -> p.getBitness()         == pkg.getBitness())
                                                                  .filter(p -> p.getOperatingSystem() == pkg.getOperatingSystem())
                                                                  .filter(p -> p.getLibCType()        == pkg.getLibCType())
                                                                  .filter(p -> p.getArchiveType()     == pkg.getArchiveType())
                                                                  .filter(p -> p.getPackageType()     == pkg.getPackageType())
                                                                  .filter(p -> p.getReleaseStatus()   == pkg.getReleaseStatus())
                                                                  .filter(p -> p.getTermOfSupport()   == pkg.getTermOfSupport())
                                                                  .filter(p -> p.isJavaFXBundled()    == pkg.isJavaFXBundled())
                                                                  .filter(p -> !p.getFileName().equals(pkg.getFileName()))
                                                                  .collect(Collectors.toList());
        return differentBuilds;
    }

    public static final List<Pkg> getAllBuildsOfPackageInList(final Collection<Pkg> packages, final Pkg pkg) {
        List<Pkg> differentBuilds = packages.stream()
                                            .filter(p -> p.getDistribution().equals(pkg.getDistribution()))
                                            .filter(p -> p.getVersionNumber().compareTo(pkg.getVersionNumber()) == 0)
                                            .filter(p -> p.getArchitecture()    == pkg.getArchitecture())
                                            .filter(p -> p.getBitness()         == pkg.getBitness())
                                            .filter(p -> p.getOperatingSystem() == pkg.getOperatingSystem())
                                            .filter(p -> p.getLibCType()        == pkg.getLibCType())
                                            .filter(p -> p.getArchiveType()     == pkg.getArchiveType())
                                            .filter(p -> p.getPackageType()     == pkg.getPackageType())
                                            .filter(p -> p.getReleaseStatus()   == pkg.getReleaseStatus())
                                            .filter(p -> p.getTermOfSupport()   == pkg.getTermOfSupport())
                                            .filter(p -> p.isJavaFXBundled()    == pkg.isJavaFXBundled())
                                            .filter(p -> !p.getFileName().equals(pkg.getFileName()))
                                            .collect(Collectors.toList());
        return differentBuilds;
    }

    public static final Optional<Pkg> getPkgWithMaxVersionForGivenPackage(final Pkg pkg) {
        int             featureVersion          = pkg.getVersionNumber().getFeature().getAsInt();
        Optional<Pkg>   pkgWithMaxVersionNumber = CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                                .stream()
                                                                                .filter(p -> p.getDistribution().equals(pkg.getDistribution()))
                                                                                .filter(p -> p.getArchitecture()    == pkg.getArchitecture())
                                                                                .filter(p -> p.getArchiveType()     == pkg.getArchiveType())
                                                                                .filter(p -> p.getOperatingSystem() == pkg.getOperatingSystem())
                                                                                .filter(p -> p.getLibCType()        == pkg.getLibCType())
                                                                                .filter(p -> p.getTermOfSupport()   == pkg.getTermOfSupport())
                                                                                .filter(p -> p.getPackageType()     == pkg.getPackageType())
                                                                                .filter(p -> p.getReleaseStatus()   == pkg.getReleaseStatus())
                                                                                .filter(p -> p.getBitness()         == pkg.getBitness())
                                                                                .filter(p -> p.isJavaFXBundled()    == pkg.isJavaFXBundled())
                                                                                .filter(p -> featureVersion         == pkg.getVersionNumber().getFeature().getAsInt())
                                                                                .max(Comparator.comparing(Pkg::getVersionNumber));
        return pkgWithMaxVersionNumber;
    }

    public static final List<Pkg> getPkgsWithLatestBuild(final List<Pkg> pkgs, final ReleaseStatus releaseStatus) {
        List<Pkg> pkgsWithLatestBuild = new ArrayList<>();
        Distro.getDistrosWithJavaVersioning()
              .stream()
              .forEach(distro -> MajorVersion.getAllMajorVersions().forEach(majorVersion -> {
                  final int mv   = majorVersion.getAsInt();
                  List<Pkg> filteredPkgs = pkgs.stream()
                                               .filter(pkg -> pkg.getReleaseStatus() == releaseStatus)
                                               .filter(pkg -> pkg.getDistribution().getDistro() == distro)
                                               .filter(pkg -> pkg.getJavaVersion().getMajorVersion().getAsInt() == mv)
                                               .collect(Collectors.toList());
                  SemVer maxSemVer = filteredPkgs.stream().max(Comparator.comparing(Pkg::getSemver)).map(pkg -> pkg.getSemver()).orElse(null);
                  if (null != maxSemVer) {
                      filteredPkgs.forEach(pkg -> pkg.setLatestBuildAvailable(false));
                      pkgsWithLatestBuild.addAll(filteredPkgs.stream()
                                                             .filter(pkg -> pkg.getSemver().compareTo(maxSemVer) == 0)
                                                             .collect(Collectors.toList()));
                  }
              }));
        return pkgsWithLatestBuild;
    }

    public static final Integer getPositiveIntFromText(final String text) {
        if (Helper.isPositiveInteger(text)) {
            return Integer.valueOf(text);
        } else {
            //LOGGER.info("Given text {} did not contain positive integer. Full text to parse was: {}", text, fullTextToParse);
            return -1;
        }
    }

    public static final Set<String> getFilesFromFolder(final String folder) throws IOException {
        Set<String> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folder))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path.getFileName().toString());
                }
            }
        }
        return fileList;
    }

    public static final List<String> readTextFile(final String filename, final Charset charset) throws IOException {
        //Charset charset = Charset.forName("UTF-8");
        return Files.readAllLines(Paths.get(filename), charset);
    }

    public static final OperatingSystem fetchOperatingSystem(final String text) {
        return Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                .stream()
                                                .filter(entry -> text.contains(entry.getKey()))
                                                .findFirst()
                                                .map(Entry::getValue)
                                                .orElse(OperatingSystem.NOT_FOUND);
    }

    public static final OperatingSystem fetchOperatingSystemByArchiveType(final String text) {
        return Constants.OPERATING_SYSTEM_BY_ARCHIVE_TYPE_LOOKUP.entrySet()
                                                                .stream()
                                                                .filter(entry -> text.toLowerCase().equals(entry.getKey()))
                                                                .findFirst()
                                                                .map(Entry::getValue)
                                                                .orElse(OperatingSystem.NOT_FOUND);
    }

    public static final Architecture fetchArchitecture(final String text) {
        return Constants.ARCHITECTURE_LOOKUP.entrySet()
                                            .stream()
                                            .filter(entry -> text.contains(entry.getKey()))
                                            .findFirst()
                                            .map(Entry::getValue)
                                            .orElse(Architecture.NOT_FOUND);
    }

    public static final ArchiveType fetchArchiveType(final String text) {
        return Constants.ARCHIVE_TYPE_LOOKUP.entrySet()
                                            .stream()
                                            .filter(entry -> text.endsWith(entry.getKey()))
                                            .findFirst()
                                            .map(Entry::getValue)
                                            .orElse(ArchiveType.NOT_FOUND);
    }

    public static final PackageType fetchPackageType(final String text) {
        return Constants.PACKAGE_TYPE_LOOKUP.entrySet()
                                            .stream()
                                            .filter(entry -> text.contains(entry.getKey()))
                                            .findFirst()
                                            .map(Entry::getValue)
                                            .orElse(PackageType.NOT_FOUND);
    }

    public static final ReleaseStatus fetchReleaseStatus(final String text) {
        return Constants.RELEASE_STATUS_LOOKUP.entrySet()
                                              .stream()
                                              .filter(entry -> text.contains(entry.getKey()))
                                              .findFirst()
                                              .map(Entry::getValue)
                                              .orElse(ReleaseStatus.NOT_FOUND);
    }

    public static final boolean isUriValid(final String uri) {
        if (null == httpClient) { httpClient = createHttpClient(); }
        final HttpRequest request = HttpRequest.newBuilder()
                                               .method("HEAD", HttpRequest.BodyPublishers.noBody())
                                               .uri(URI.create(uri))
                                               .timeout(Duration.ofSeconds(60))
                                               .build();
        try {
            HttpResponse<Void> responseFuture = httpClient.send(request, BodyHandlers.discarding());
            return 200 == responseFuture.statusCode();
        } catch (InterruptedException | IOException e) {
            return false;
        }
    }


    public static final String getAllPackagesV2Msg(final Boolean downloadable, final Boolean include_ea, final BuildScope scope) {
        final List<Distro> publicDistros = null == downloadable || !downloadable ? Distro.getPublicDistros() : Distro.getPublicDistrosDirectlyDownloadable();
        final boolean gaOnly = null == include_ea || !include_ea;
        final StringBuilder msgBuilder = new StringBuilder();
        final Scope         scopeToCheck = (BuildScope.BUILD_OF_OPEN_JDK == scope || BuildScope.BUILD_OF_GRAALVM == scope) ? scope : null;

        msgBuilder.append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                  .append(INDENTED_QUOTES).append(RESULT).append(QUOTES).append(COLON).append(NEW_LINE)
                  .append(INDENT).append(CacheManager.INSTANCE.pkgCache.getPkgs()
                                                                       .parallelStream()
                                                                       .filter(pkg -> null == scopeToCheck ? pkg != null : Constants.REVERSE_SCOPE_LOOKUP.get(scopeToCheck).contains(pkg.getDistribution().getDistro()))
                                                                       .filter(pkg -> publicDistros.contains(pkg.getDistribution().getDistro()))
                                                                       .filter(pkg -> gaOnly ? ReleaseStatus.GA == pkg.getReleaseStatus() : null != pkg.getReleaseStatus())
                                                                       .sorted(Comparator.comparing(Pkg::getDistributionName).reversed())
                                                                       .map(pkg -> pkg.toString(OutputFormat.REDUCED_COMPRESSED, API_VERSION_V2))
                                                                       .collect(Collectors.joining(COMMA_NEW_LINE, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(COMMA_NEW_LINE)
                  .append(INDENTED_QUOTES).append(MESSAGE).append(QUOTES).append(COLON).append(QUOTES).append(QUOTES).append(NEW_LINE)
                  .append(CURLY_BRACKET_CLOSE);
        return msgBuilder.toString();
    }

    public static final Collection<Pkg> updateLatestBuildAvailable(final Collection<Pkg> pkgsToModify) {
        // Copy list of pkgs
        List<Pkg> pkgs = new ArrayList<>(pkgsToModify);

        // Set latestBuildAvailable=false in all pkgs
        pkgs.forEach(pkg -> pkg.setLatestBuildAvailable(false));

        // Find latest builds available
        Collection<Pkg> pkgsFound = pkgs.parallelStream()
                                        .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                        .collect(Collectors.toList());
        final Set<Pkg>  filteredPkgsFound = new CopyOnWriteArraySet<>();
        final List<Pkg> pkgsToCheck       = new CopyOnWriteArrayList<>(pkgsFound);
        final Set<Pkg>  diffPkgs          = new CopyOnWriteArraySet<>();
        pkgsFound.forEach(pkg -> {
            List<Pkg> pkgsWithDifferentUpdate = pkgsToCheck.parallelStream()
                                                           .filter(pkg1 -> pkg.equalsExceptUpdate(pkg1))
                                                           .collect(Collectors.toList());
            diffPkgs.addAll(pkgsWithDifferentUpdate);

            final Pkg pkgWithMaxVersion = pkgsWithDifferentUpdate.parallelStream()
                                                                 .max(Comparator.comparing(Pkg::getVersionNumber))
                                                                 .orElse(null);
            if (null != pkgWithMaxVersion) {
                List<Pkg> pkgsWithSmallerVersions = filteredPkgsFound.parallelStream()
                                                                     .filter(pkg2 -> pkg2.equalsExceptUpdate(pkgWithMaxVersion))
                                                                     .filter(pkg2 -> pkg2.getSemver().compareTo(pkgWithMaxVersion.getSemver()) < 0)
                                                                     .collect(Collectors.toList());
                if (!pkgsWithSmallerVersions.isEmpty()) { filteredPkgsFound.removeAll(pkgsWithSmallerVersions); }
                filteredPkgsFound.add(pkgWithMaxVersion);
            }
        });

        pkgsToCheck.removeAll(diffPkgs);
        filteredPkgsFound.addAll(pkgsToCheck);

        // Set latestBuildAvailable=true for pkgs found
        pkgs.parallelStream().filter(pkg -> filteredPkgsFound.contains(pkg)).forEach(pkg -> pkg.setLatestBuildAvailable(true));

        return pkgs;
    }

    public static final String getUserAgent(final io.micronaut.http.HttpRequest request) {
        String      userAgent = "unknown";
        HttpHeaders headers   = request.getHeaders();
        if (null != headers) {
            Optional<String> optUserAgent = headers.findFirst("User-Agent");
            if (optUserAgent.isPresent()) { userAgent = optUserAgent.get(); }
            Optional<String> optUserInfo = headers.findFirst("Disco-User-Info");
            if (optUserInfo.isPresent()) { userAgent = optUserInfo.get(); }

        }
        return userAgent;
    }

    public static final List<Scope> getDistributionScopes(final List<String> discovery_scope_id) {
        Helper.removeEmptyItems(discovery_scope_id);
        Set<String> disco_scope_ids = (null == discovery_scope_id || discovery_scope_id.isEmpty()) ? new HashSet<>() : new HashSet<>(discovery_scope_id);

        // Extract scopes
        List<Scope> scopes;
        if (disco_scope_ids.isEmpty()) {
            scopes = List.of(BasicScope.PUBLIC);
        } else {
            Set<Scope> scopesFound = new HashSet<>();
            for (String scopeString : disco_scope_ids) {
                Scope basicScope = BasicScope.fromText(scopeString);
                if (BasicScope.NOT_FOUND != basicScope && BasicScope.NONE != basicScope) { scopesFound.add(basicScope); }

                Scope ideScope = IDEScope.fromText(scopeString);
                if (Scope.NOT_FOUND != ideScope) { scopesFound.add(ideScope); }

                Scope buildScope = BuildScope.fromText(scopeString);
                if (Scope.NOT_FOUND != buildScope) { scopesFound.add(buildScope); }

                Scope downloadScope = DownloadScope.fromText(scopeString);
                if (Scope.NOT_FOUND != downloadScope) { scopesFound.add(downloadScope); }

                Scope usageScope = UsageScope.fromText(scopeString);
                if (Scope.NOT_FOUND != usageScope) { scopesFound.add(usageScope); }
            }
            if (scopesFound.isEmpty()) {
                scopes = List.of(BasicScope.PUBLIC);
            } else {
                if (scopesFound.size() != 1 || !scopesFound.contains(IDEScope.VISUAL_STUDIO_CODE)) {
                    if (!scopesFound.contains(BasicScope.PUBLIC)) {
                        scopesFound.add(BasicScope.PUBLIC);
                    }
                }
                scopes = new ArrayList<>(scopesFound);
            }
        }
        return scopes;
    }

    public static final List<Scope> getPackageScopes(final List<String> discovery_scope_id) {
        Helper.removeEmptyItems(discovery_scope_id);
        Set<String> disco_scope_ids = (null == discovery_scope_id || discovery_scope_id.isEmpty()) ? new HashSet<>() : new HashSet<>(discovery_scope_id);

        // Extract package related scopes
        List<Scope> scopes = new ArrayList<>();
        if (!disco_scope_ids.isEmpty()) {
            Set<Scope> scopesFound = new HashSet<>();
            for (String scopeString : disco_scope_ids) {
                Scope signatureScope = SignatureScope.fromText(scopeString);
                if (Scope.NOT_FOUND != signatureScope) { scopesFound.add(signatureScope); }
            }
            scopes.addAll(scopesFound);
        }
        return scopes;
    }

    public static final String getCountryCode(final String ipAddress) {
        if (null == ipAddress || ipAddress.isEmpty()) { return ""; }
        HttpResponse<String> response = get(Constants.IP_LOCATION_URL + ipAddress);
        if (null == response) {
            return "";
        } else {
            if (response.statusCode() == 200) {
                String      bodyText = response.body();
                Gson        gson     = new Gson();
                JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                if (element instanceof JsonObject) {
                    JsonObject json = element.getAsJsonObject();
                    if (json.has(Constants.COUNTRY_CODE_FIELD)) {
                        return json.get(Constants.COUNTRY_CODE_FIELD).getAsString().toLowerCase();
                    }
                }
            } else {
                // Problem with url request
                LOGGER.debug("Response (Status Code {}) {} ", response.statusCode(), response.body());
            }
        }
        return "";
    }


    // ******************** REST calls ****************************************
    private static HttpClient createHttpClient() {
        return HttpClient.newBuilder()
                               .connectTimeout(Duration.ofSeconds(20))
                               .version(Version.HTTP_2)
                               .followRedirects(Redirect.NORMAL)
                               //.executor(Executors.newFixedThreadPool(4))
                               .build();
    }

    public static final HttpResponse<String> get(final String uri) {
        return get(uri, new HashMap<>());
    }
    public static final HttpResponse<String> get(final String uri, final Map<String,String> headers) {
        if (null == httpClient) { httpClient = createHttpClient(); }

        List<String> requestHeaders = new LinkedList<>();
        requestHeaders.add("User-Agent");
        requestHeaders.add("DiscoAPI");
        headers.entrySet().forEach(entry -> {
            final String name  = entry.getKey();
            final String value = entry.getValue();
            if (null != name && !name.isEmpty() && null != value && !value.isEmpty()) {
                requestHeaders.add(name);
                requestHeaders.add(value);
            }
        });

        final HttpRequest request = HttpRequest.newBuilder()
                                         .GET()
                                         .uri(URI.create(uri))
                                         .headers(requestHeaders.toArray(new String[0]))
                                         .timeout(Duration.ofSeconds(60))
                                         .build();

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response;
            } else {
                // Problem with url request
                LOGGER.debug("Error executing get request {}", uri);
                LOGGER.debug("Response (Status Code {}) {} ", response.statusCode(), response.body());
                return response;
            }
        } catch (CompletionException | InterruptedException | IOException e) {
            LOGGER.error("Error executing get request {} : {}", uri, e.getMessage());
            return null;
        }
    }

    public static final CompletableFuture<HttpResponse<String>> getAsync(final String uri) {
        return getAsync(uri, new HashMap<>());
    }
    public static final CompletableFuture<HttpResponse<String>> getAsync(final String uri, final Map<String, String> headers) {
        if (null == httpClientAsync) { httpClientAsync = createHttpClient(); }

        List<String> requestHeaders = new LinkedList<>();
        requestHeaders.add("User-Agent");
        requestHeaders.add("DiscoAPI");
        headers.entrySet().forEach(entry -> {
            final String name  = entry.getKey();
            final String value = entry.getValue();
            if (null != name && !name.isEmpty() && null != value && !value.isEmpty()) {
                requestHeaders.add(name);
                requestHeaders.add(value);
            }
        });

        final HttpRequest request = HttpRequest.newBuilder()
                                               .GET()
                                               .uri(URI.create(uri))
                                               .headers(requestHeaders.toArray(new String[0]))
                                               .timeout(Duration.ofSeconds(60))
                                               .build();

        return httpClientAsync.sendAsync(request, BodyHandlers.ofString());
    }
}
