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

package io.foojay.api.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.distribution.AOJ_OPENJ9;
import io.foojay.api.distribution.OJDKBuild;
import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.distribution.AOJ;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.distribution.Oracle;
import io.foojay.api.distribution.OracleOpenJDK;
import io.foojay.api.distribution.RedHat;
import io.foojay.api.distribution.SAPMachine;
import io.foojay.api.distribution.Zulu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;


public class Helper {
    private static final Logger  LOGGER                 = LoggerFactory.getLogger(Helper.class);
    private static final Random  RND                    = new Random();
    public  static final Pattern FILE_URL_PATTERN       = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)(\\.zip|\\.msi|\\.pkg|\\.dmg|\\.tar\\.gz|\\.deb|\\.rpm|\\.cab|\\.7z)");
    public  static final Pattern HREF_FILE_PATTERN      = Pattern.compile("href=\"([^\"]*(\\.zip|\\.msi|\\.pkg|\\.dmg|\\.tar\\.gz|\\.deb|\\.rpm|\\.cab|\\.7z))\"");
    public  static final Pattern HREF_DOWNLOAD_PATTERN  = Pattern.compile("(>)(\\s?(jdk|jre|serverjre)-(([0-9]+\\.[0-9]+\\.[0-9]+_[a-z]+-[a-z0-9]+_)|([0-9]+u[0-9]+-[a-z]+-[a-z0-9]+(-vfp-hflt)?)).*[a-zA-Z]+)(<)");
    public  static final Pattern NUMBER_IN_TEXT_PATTERN = Pattern.compile("(.*)?([0-9]+)(.*)?");
    public  static final Matcher FILE_URL_MATCHER       = FILE_URL_PATTERN.matcher("");
    public  static final Matcher HREF_FILE_MATCHER      = HREF_FILE_PATTERN.matcher("");
    public  static final Matcher HREF_DOWNLOAD_MATCHER  = HREF_DOWNLOAD_PATTERN.matcher("");


    public static Callable<List<Pkg>> createTask(final Distro distro) {
        return () -> getPkgs(distro);
    }

    public static List<Pkg> getPkgs(final Distro distro) {
        final List<Pkg> pkgs = new LinkedList<>();
        switch(distro) {
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
                HttpClient  clientOJ    = HttpClient.newBuilder().followRedirects(Redirect.NEVER).version(java.net.http.HttpClient.Version.HTTP_2).build();
                HttpRequest request8 = HttpRequest.newBuilder().uri(URI.create(query8)).setHeader("User-Agent", "DiscoAPI").GET().build();
                try {
                    HttpResponse<String> response = clientOJ.send(request8, BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        String      bodyText = response.body();
                        Gson        gson     = new Gson();
                        JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                        if (element instanceof JsonArray) {
                            JsonArray jsonArray = element.getAsJsonArray();
                            pkgs.addAll(oracleOpenJDK.getAllPkgs(jsonArray));
                        }
                    } else {
                        // Problem with url request
                        LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
                    }
                } catch (InterruptedException | IOException e) {
                    LOGGER.error("Error fetching packages for distribution {} from {}", oracleOpenJDK.getName(), query8);
                }
                // Get all jdk 11 packages from github
                String       query11    = oracleOpenJDK.getGithubPkg11Url() + "/releases?per_page=100";
                HttpRequest request11 = HttpRequest.newBuilder().uri(URI.create(query11)).setHeader("User-Agent", "DiscoAPI").GET().build();
                try {
                    HttpResponse<String> response = clientOJ.send(request11, BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        String      bodyText = response.body();
                        Gson        gson     = new Gson();
                        JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                        if (element instanceof JsonArray) {
                            JsonArray jsonArray = element.getAsJsonArray();
                            pkgs.addAll(oracleOpenJDK.getAllPkgs(jsonArray));
                        }
                    } else {
                        // Problem with url request
                        LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
                    }
                } catch (InterruptedException | IOException e) {
                    LOGGER.error("Error fetching packages for distribution {} from {}", oracleOpenJDK.getName(), query8);
                }
                break;
            case SAP_MACHINE:
                SAPMachine  sapMachine = (SAPMachine) distro.get();
                // Fetch packages from sap.github.io -> sapmachine_releases.json
                pkgs.addAll(sapMachine.getAllPkgsFromJsonUrl());

                // Fetch major versions 10, 12, 13 from github
                pkgs.addAll(sapMachine.getAllPkgs());

                // Search through github release and fetch packages from there
                String      query      = sapMachine.getPkgUrl() + "?per_page=100";
                HttpClient  clientSAP  = HttpClient.newBuilder().followRedirects(Redirect.NEVER).version(java.net.http.HttpClient.Version.HTTP_2).build();
                HttpRequest request   = HttpRequest.newBuilder().uri(URI.create(query)).setHeader("User-Agent", "DiscoAPI").GET().build();
                try {
                    HttpResponse<String> response = clientSAP.send(request, BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        String      bodyText = response.body();
                        Gson        gson     = new Gson();
                        JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                        if (element instanceof JsonArray) {
                            JsonArray jsonArray = element.getAsJsonArray();
                            pkgs.addAll(sapMachine.getAllPkgs(jsonArray));
                        }
                    } else {
                        // Problem with url request
                        LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
                    }
                } catch (InterruptedException | IOException e) {
                    LOGGER.error("Error fetching packages for distribution {} from {}", sapMachine.getName(), query);
                }
                break;
            case AOJ:
                AOJ AOJ = (AOJ) distro.get();
                for (MajorVersion majorVersion : CacheManager.INSTANCE.getMajorVersions()) {
                    VersionNumber versionNumber = majorVersion.getVersionNumber();
                    pkgs.addAll(getPkgs(AOJ, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null, ReleaseStatus.GA, TermOfSupport.NONE));
                    pkgs.addAll(getPkgs(AOJ, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null, ReleaseStatus.EA, TermOfSupport.NONE));
                }
                break;
            case AOJ_OPENJ9:
                AOJ_OPENJ9 AOJ_OPENJ9 = (AOJ_OPENJ9) distro.get();
                for (MajorVersion majorVersion : CacheManager.INSTANCE.getMajorVersions()) {
                    VersionNumber versionNumber = majorVersion.getVersionNumber();
                    pkgs.addAll(getPkgs(AOJ_OPENJ9, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null, ReleaseStatus.GA, TermOfSupport.NONE));
                    pkgs.addAll(getPkgs(AOJ_OPENJ9, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null, ReleaseStatus.EA, TermOfSupport.NONE));
                }
                break;
            default:
                Distribution distribution = distro.get();
                for (MajorVersion majorVersion : CacheManager.INSTANCE.getMajorVersions()) {
                    pkgs.addAll(
                        getPkgs(distribution, majorVersion.getVersionNumber(), false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null, ReleaseStatus.NONE, TermOfSupport.NONE));
                }
                break;
        }

        List<Pkg> unique = pkgs.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(Pkg::getId))), LinkedList::new));
        return new LinkedList<>(unique);
    }

    public static Callable<List<Pkg>> createTask(final Distribution distribution, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                                 final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType, final Boolean fx, final ReleaseStatus releaseStatus,
                                                 final TermOfSupport termOfSupport) {
        return () -> getPkgs(distribution, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
    }

    private static List<Pkg> getPkgs(final Distribution distribution, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                     final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                     final Boolean fx, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        String query = distribution.getUrlForAvailablePkgs(versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);

        if (query.isEmpty()) { return List.of(); }

        HttpClient  client  = HttpClient.newBuilder()
                                        .followRedirects(Redirect.NEVER)
                                        .version(java.net.http.HttpClient.Version.HTTP_2)
                                        .build();
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(query))
                                         .setHeader("User-Agent", "DiscoAPI")
                                         .GET()
                                         .build();
        List<Pkg>   pkgs    = new LinkedList<>();
        try {
            List<Pkg> pkgsFound = new ArrayList<>();
            if (distribution.equals(Distro.ORACLE_OPEN_JDK.get())) {
                List<Pkg> pkgsInDistribution = distribution.getPkgFromJson(null, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
                pkgsFound.addAll(pkgsInDistribution.stream().filter(pkg -> isVersionNumberInPkg(versionNumber, pkg)).collect(Collectors.toList()));
            } else {
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String      bodyText = response.body();
                    Gson        gson     = new Gson();
                    JsonElement element  = gson.fromJson(bodyText, JsonElement.class);
                    if (element instanceof JsonArray) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject pkgJsonObj         = jsonArray.get(i).getAsJsonObject();
                            List<Pkg>  pkgsInDistribution = distribution.getPkgFromJson(pkgJsonObj, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
                        pkgsFound.addAll(pkgsInDistribution);
                        //pkgsFound.addAll(pkgsInDistribution.stream().filter(pkg -> isVersionNumberInPkg(versionNumber, pkg)).collect(Collectors.toList()));
                        }
                    } else if (element instanceof JsonObject) {
                        JsonObject    pkgJsonObj         = element.getAsJsonObject();
                        List<Pkg> pkgsInDistribution =
                            distribution.getPkgFromJson(pkgJsonObj, versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, fx, releaseStatus, termOfSupport);
                    pkgsFound.addAll(pkgsInDistribution);
                    //pkgsFound.addAll(pkgsInDistribution.stream().filter(pkg -> isVersionNumberInPkg(versionNumber, pkg)).collect(Collectors.toList()));
                    }
                } else {
                    // Problem with url request
                    LOGGER.debug("Error get packages for {} {} calling {}", distribution.getName(), versionNumber, query);
                    LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
                    return pkgs;
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
        } catch (InterruptedException | IOException e) {
            LOGGER.error("Error fetching packages for distribution {} from {}", distribution.getName(), query);
        }
        return pkgs;
    }

    private static List<Pkg> getPkgsAsync(final Distribution distribution, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                          final Architecture architecture, final Bitness bitness, final ArchiveType archiveType,
                                          final PackageType packageType, final boolean javaFX, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        String      query   = distribution.getUrlForAvailablePkgs(versionNumber, latest, operatingSystem, architecture, bitness, archiveType, packageType, javaFX, releaseStatus, termOfSupport);
        HttpClient  client  = HttpClient.newBuilder()
                                        .followRedirects(Redirect.NEVER)
                                        .version(java.net.http.HttpClient.Version.HTTP_1_1)
                                        .build();
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(query))
                                         .setHeader("User-Agent", "DiscoAPI")
                                         .GET()
                                         .build();
        List<Pkg>   pkgs    = new LinkedList<>();
        try {
            String      body     = getResponseAsync(client, request);
            Gson        gson     = new Gson();
            JsonElement element  = gson.fromJson(body, JsonElement.class);
            if (element instanceof JsonArray) {
                JsonArray jsonArray = element.getAsJsonArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject    pkgJsonObj         = jsonArray.get(i).getAsJsonObject();
                    List<Pkg> pkgsInDistribution = distribution.getPkgFromJson(pkgJsonObj, versionNumber, latest, operatingSystem, architecture, bitness, archiveType,
                                                                               packageType, javaFX, releaseStatus,
                                                                               termOfSupport);
                    List<Pkg> pkgsFound = pkgsInDistribution.stream().filter(pkg -> isVersionNumberInPkg(versionNumber, pkg)).collect(Collectors.toList());
                    pkgs.addAll(pkgsFound);
                }
            } else if (element instanceof JsonObject) {
                JsonObject    pkgJsonObj         = element.getAsJsonObject();
                List<Pkg> pkgsInDistribution = distribution.getPkgFromJson(pkgJsonObj, versionNumber, latest, operatingSystem, architecture, bitness, archiveType,
                                                                           packageType, javaFX, releaseStatus,
                                                                           termOfSupport);
                List<Pkg> pkgsFound = pkgsInDistribution.stream().filter(pkg -> isVersionNumberInPkg(versionNumber, pkg)).collect(Collectors.toList());
                pkgs.addAll(pkgsFound);
            }

            HashSet<Pkg> unique = new HashSet<>(pkgs);
            pkgs = new LinkedList<>(unique);
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Error fetching packages async for distribution {} from {}", distribution.getName(), query);
        }
        return pkgs;
    }

    private static String getResponseAsync(final HttpClient client, final HttpRequest request) throws ExecutionException, InterruptedException {
        return client.sendAsync(request, BodyHandlers.ofString()).thenApply(HttpResponse::body).get();
    }

    private static boolean isVersionNumberInPkg(final VersionNumber versionNumber, final Pkg pkg) {
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

    public static ArchiveType getFileEnding(final String fileName) {
        if (null == fileName || fileName.isEmpty()) { return ArchiveType.NONE; }
        for (ArchiveType ext : ArchiveType.values()) {
            for (String ending : ext.getFileEndings()) {
                if (fileName.endsWith(ending)) { return ext; }
            }
        }
        return ArchiveType.NONE;
    }

    public static Set<String> getFileUrlsFromString(final String text) {
        Set<String> urlsFound = new HashSet<>();
        FILE_URL_MATCHER.reset(text);
        while (FILE_URL_MATCHER.find()) {
            urlsFound.add(FILE_URL_MATCHER.group());
        }
        return urlsFound;
    }

    public static Set<String> getFileHrefsFromString(final String text) {
        Set<String> hrefsFound = new HashSet<>();
        HREF_FILE_MATCHER.reset(text);
        while (HREF_FILE_MATCHER.find()) {
            hrefsFound.add(HREF_FILE_MATCHER.group(1));
        }
        return hrefsFound;
    }

    public static Set<String> getDownloadHrefsFromString(final String text) {
        Set<String> hrefsFound = new HashSet<>();
        HREF_DOWNLOAD_MATCHER.reset(text);
        while (HREF_DOWNLOAD_MATCHER.find()) {
            hrefsFound.add(HREF_DOWNLOAD_MATCHER.group(2).strip());
        }
        return hrefsFound;
    }

    public static String getFileNameFromText(final String text) {
        if (getFileEnding(text) == ArchiveType.NONE) { return ""; }
        int    lastSlash = text.lastIndexOf("/") + 1;
        String fileName  = text.substring(lastSlash);
        return fileName;
    }

    public static boolean isReleaseTermOfSupport(final int featureVersion, final TermOfSupport termOfSupport) {
        switch(termOfSupport) {
            case LTS: return isLTS(featureVersion);
            case MTS: return isMTS(featureVersion);
            case STS: return isSTS(featureVersion);
            default : return false;
        }
    }

    public static boolean isSTS(final int featureVersion) {
        if (featureVersion < 9) { return false; }
        switch(featureVersion) {
            case 9 :
            case 10: return true;
            default: return !isLTS(featureVersion);
        }
    }

    public static boolean isMTS(final int featureVersion) {
        if (featureVersion < 13) { return false; }
        return (!isLTS(featureVersion)) && featureVersion % 2 != 0;
    }

    public static boolean isLTS(final int featureVersion) {
        if (featureVersion < 1) { throw new IllegalArgumentException("Feature version number cannot be smaller than 1"); }
        if (featureVersion <= 8) { return true; }
        if (featureVersion < 11) { return false; }
        return ((featureVersion - 11.0) / 6.0) % 1 == 0;
    }

    public static TermOfSupport getTermOfSupport(final VersionNumber versionNumber, final Distro distribution) {
        TermOfSupport termOfSupport = getTermOfSupport(versionNumber);
        switch(termOfSupport) {
            case LTS:
            case STS: return termOfSupport;
            case MTS: return Distro.ZULU == distribution ? termOfSupport : TermOfSupport.STS;
            default : return TermOfSupport.NOT_FOUND;
        }
    }
    public static TermOfSupport getTermOfSupport(final VersionNumber versionNumber) {
        if (!versionNumber.getFeature().isPresent() || versionNumber.getFeature().isEmpty()) {
            throw new IllegalArgumentException("VersionNumber need to have a feature version");
        }
        return getTermOfSupport(versionNumber.getFeature().getAsInt());
    }
    public static TermOfSupport getTermOfSupport(final int featureVersion) {
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

    public static void setTermOfSupport(final VersionNumber versionNumber, final Pkg pkg) {
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

    public static String getTextFromUrl(final String url) throws Exception {
        try (var stream = URI.create(url).toURL().openStream()) {
            return new String(stream.readAllBytes(), UTF_8);
        }
    }

    public static int getLeadingNumbers(final String text) {
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

    public static String readFromInputStream(final InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    public static boolean isPositiveInteger(final String text) {
        if (null == text || text.isEmpty()) { return false; }
        return Constants.POSITIVE_INTEGER_PATTERN.matcher(text).matches();
    }

    public static void removeEmptyItems(final List<String> list) {
        if (null == list) { return; }
        list.removeIf(item -> item == null || "".equals(item));
    }

    public static long getCRC32Checksum(final byte[] bytes) {
        Checksum crc32 = new CRC32();

        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    public static byte[] getMD5(final byte[] bytes) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        final byte[] result = md.digest(bytes);
        return result;
    }

    public static String getMD5Hex(final byte[] bytes) {
        return bytesToHex(getMD5(bytes));
    }

    public static String bytesToHex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : bytes) { builder.append(String.format("%02x", b)); }
        return builder.toString();
    }

    public static String createEphemeralId(final long number, final String  id) {
        return new StringBuilder().append(RND.nextInt(100) + 312).append(id).append(number).toString();
    }

    public static String trimPrefix(final String text, final String prefix) {
        return text.replaceFirst(prefix, "");
    }

    public static boolean isDifferentBuild(final Pkg pkg1, final Pkg pkg2) {
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

    public static List<Pkg> getAllBuildsOfPackage(final Pkg pkg) {
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

    public static List<Pkg> getAllBuildsOfPackageInList(final Collection<Pkg> packages, final Pkg pkg) {
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

    public static Optional<Pkg> getPkgWithMaxVersionForGivenPackage(final Pkg pkg) {
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

    public static Integer getPositiveIntFromText(final String text) {
        if (Helper.isPositiveInteger(text)) {
            return Integer.valueOf(text);
        } else {
            //LOGGER.info("Given text {} did not contain positive integer. Full text to parse was: {}", text, fullTextToParse);
            return -1;
        }
    }
    
    
    // ******************** REST calls ****************************************
    public static final String get(final String uri) {
        HttpClient  client  = HttpClient.newBuilder().followRedirects(Redirect.NEVER).version(java.net.http.HttpClient.Version.HTTP_2).build();
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(uri))
                                         .build();
        try {
            HttpResponse<String> response  = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                // Problem with url request
                LOGGER.debug("Error executing get request {}", uri);
                LOGGER.debug("Response ({}) {} ", response.statusCode(), response.body());
                return "";
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.error("Error executing get request {} : {}", uri, e.getMessage());
            return "";
        }
    }

    public static final CompletableFuture<String> getAsync(final String uri) {
        HttpClient  client  = HttpClient.newBuilder().followRedirects(Redirect.NEVER).version(java.net.http.HttpClient.Version.HTTP_2).build();
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(uri))
                                         .build();
        return client.sendAsync(request, BodyHandlers.ofString())
                     .thenApply(HttpResponse::body);
    }
}
