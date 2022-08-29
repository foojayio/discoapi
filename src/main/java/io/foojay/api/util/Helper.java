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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.scopes.BasicScope;
import eu.hansolo.jdktools.scopes.BuildScope;
import eu.hansolo.jdktools.scopes.DownloadScope;
import eu.hansolo.jdktools.scopes.QualityScope;
import eu.hansolo.jdktools.scopes.Scope;
import eu.hansolo.jdktools.scopes.SignatureScope;
import eu.hansolo.jdktools.scopes.UsageScope;
import eu.hansolo.jdktools.util.OutputFormat;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.MongoDbManager;
import io.foojay.api.distribution.Zulu;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.scopes.IDEScope;
import io.foojay.api.scopes.YamlScopes;
import io.micronaut.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.Constants.COMMA;
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


public class Helper {
    private static final Logger     LOGGER                                 = LoggerFactory.getLogger(Helper.class);
    public  static final Pattern    FILE_URL_PATTERN                       = Pattern.compile("(JDK|JRE)(\\s+\\|\\s?\\[[a-zA-Z0-9\\-\\._]+\\]\\()(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)(\\.zip|\\.msi|\\.pkg|\\.dmg|\\.tar\\.gz(?!\\.sig)|\\.deb|\\.rpm|\\.cab|\\.7z))");
    public  static final Pattern    CORRETTO_SIG_URI_PATTERN               = Pattern.compile("((\\[Download\\])\\(?(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)(\\.zip\\.sig|\\.tar\\.gz\\.sig)))\\)");
    public  static final Pattern    HREF_FILE_PATTERN                      = Pattern.compile("href=\"([^\"]*(\\.zip|\\.msi|\\.pkg|\\.dmg|\\.tar\\.gz|\\.deb|\\.rpm|\\.cab|\\.7z))\"");
    public  static final Pattern    DOWNLOAD_LINK_PATTERN                  = Pattern.compile("downloadLink:\\s'(.*)'");
    public  static final Pattern    SIG_PATTERN                            = Pattern.compile("sig:\\s'(.*)'");
    public  static final Pattern    HREF_SIG_FILE_PATTERN                  = Pattern.compile("href=\"([^\"]*(\\.sig))\"");
    public  static final Pattern    HREF_SHA256_FILE_PATTERN               = Pattern.compile("href=\"([^\"]*(\\.sha256sum.txt))\"");
    public  static final Pattern    HREF_DOWNLOAD_PATTERN                  = Pattern.compile("(\\>)(\\s|\\h?(jdk|jre|serverjre)-(([0-9]+\\.[0-9]+\\.[0-9]+_[a-z]+-[a-z0-9]+_)|([0-9]+u[0-9]+-[a-z]+-[a-z0-9]+(-vfp-hflt)?)).*[a-zA-Z]+)(\\<)");
    private static final Pattern    JBANG_HEADER_PATTERN                   = Pattern.compile("(JBang)\\/([0-9]+\\.[0-9]+\\.[0-9]+(\\.[0-9]+)?)\\s+\\(([0-9A-Za-z\\s]+)\\/([a-zA-Z0-9_\\.\\-]+)\\/([a-z0-9A-Z_\\s]+)\\)\\s(Java\\/([0-9]+(\\.[0-9]+)?(\\.[0-9]+)?([_0-9]+)?))\\/(.*)");
    private static final Matcher    JBANG_HEADER_MATCHER                   = JBANG_HEADER_PATTERN.matcher("");
    private static final Matcher    FILE_URL_MATCHER                       = FILE_URL_PATTERN.matcher("");
    private static final Matcher    CORRETTO_SIG_URI_MATCHER               = CORRETTO_SIG_URI_PATTERN.matcher("");
    private static final Matcher    HREF_FILE_MATCHER                      = HREF_FILE_PATTERN.matcher("");
    private static final Matcher    DOWNLOAD_LINK_MATCHER                  = DOWNLOAD_LINK_PATTERN.matcher("");
    private static final Matcher    SIG_MATCHER                            = SIG_PATTERN.matcher("");
    private static final Matcher    HREF_SIG_FILE_MATCHER                  = HREF_SIG_FILE_PATTERN.matcher("");
    private static final Matcher    HREF_SHA256_FILE_MATCHER               = HREF_SHA256_FILE_PATTERN.matcher("");
    private static final Matcher    HREF_DOWNLOAD_MATCHER                  = HREF_DOWNLOAD_PATTERN.matcher("");
    private static       HttpClient httpClient;
    private static       HttpClient httpClientAsync;


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

    public static final Set<String> getDownloadLinkFromString(final String text) {
        Set<String> downloadLinksFound = new HashSet<>();
        DOWNLOAD_LINK_MATCHER.reset(text);
        while (DOWNLOAD_LINK_MATCHER.find()) {
            downloadLinksFound.add(DOWNLOAD_LINK_MATCHER.group(1));
        }
        return downloadLinksFound;
    }

    public static final Set<String> getSigFromString(final String text) {
        Set<String> sigsFound = new HashSet<>();
        SIG_MATCHER.reset(text);
        while (SIG_MATCHER.find()) {
            sigsFound.add(SIG_MATCHER.group(1));
        }
        return sigsFound;
    }

    public static final Set<String> getSigFileHrefsFromString(final String text) {
        Set<String> sigHrefsFound = new HashSet<>();
        HREF_SIG_FILE_MATCHER.reset(text);
        while (HREF_SIG_FILE_MATCHER.find()) {
            sigHrefsFound.add(HREF_SIG_FILE_MATCHER.group(1).toLowerCase());
        }
        return sigHrefsFound;
    }

    public static final Set<String> getSha256FileHrefsFromString(final String text) {
        Set<String> sha256HrefsFound = new HashSet<>();
        HREF_SHA256_FILE_MATCHER.reset(text);
        while (HREF_SHA256_FILE_MATCHER.find()) {
            sha256HrefsFound.add(HREF_SHA256_FILE_MATCHER.group(1).toLowerCase());
        }
        return sha256HrefsFound;
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

    public static final boolean isSTS(final MajorVersion majorVersion) {
        return isSTS(majorVersion.getAsInt());
    }
    public static final boolean isSTS(final int featureVersion) {
        if (featureVersion < 9) { return false; }
        switch(featureVersion) {
            case 9 :
            case 10: return true;
            default: return !isLTS(featureVersion);
        }
    }

    public static final boolean isMTS(final MajorVersion majorVersion) {
        return isMTS(majorVersion.getAsInt());
    }
    public static final boolean isMTS(final int featureVersion) {
        if (featureVersion < 13 || featureVersion > 15) { return false; }
        return (!isLTS(featureVersion)) && featureVersion % 2 != 0;
    }

    public static final boolean isLTS(final MajorVersion majorVersion) {
        return isLTS(majorVersion.getAsInt());
    }
    public static final boolean isLTS(final int featureVersion) {
        if (featureVersion < 1) { throw new IllegalArgumentException("Feature version number cannot be smaller than 1"); }
        if (featureVersion <= 8) { return true; }
        if (featureVersion < 11) { return false; }
        if (featureVersion < 17) { return ((featureVersion - 11.0) / 6.0) % 1 == 0; }
        return ((featureVersion - 17.0) / 4.0) % 1 == 0;
    }

    public static final OptionalInt getLatestGA() {
        final LocalDate                           now      = LocalDate.now();
        final Optional<Entry<Integer, LocalDate>> latestGA = Constants.GA_RELEASE_DATES.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).filter(entry -> entry.getValue().isEqual(now) || entry.getValue().isBefore(now)).findFirst();
        final OptionalInt                         opt;
        if (latestGA.isPresent()) {
            opt = OptionalInt.of(latestGA.get().getKey());
        } else {
            opt = OptionalInt.empty();
        }
        return opt;
    }

    public static final OptionalInt getNextEA() {
        final OptionalInt latestGA = getLatestGA();
        final OptionalInt nextEA;
        if (latestGA.isPresent()) {
            nextEA = OptionalInt.of(latestGA.getAsInt() + 1);
        } else {
            nextEA = OptionalInt.empty();
        }
        return nextEA;
    }

    public static final OptionalInt getNextButOneEA() {
        final OptionalInt nextEA = getNextEA();
        final OptionalInt nextButOneEA;
        if (nextEA.isPresent()) {
            nextButOneEA = OptionalInt.of(nextEA.getAsInt() + 1);
        } else {
            nextButOneEA = OptionalInt.empty();
        }
        return nextButOneEA;
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
        if (null == versionNumber || !versionNumber.getFeature().isPresent() || versionNumber.getFeature().isEmpty()) {
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
        } catch(IOException e) {
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

    public static final boolean isPositiveInteger(final String text) {
        if (null == text || text.isEmpty()) { return false; }
        return Constants.POSITIVE_INTEGER_PATTERN.matcher(text).matches();
    }

    public static final void removeEmptyItems(final List<String> list) {
        if (null == list) { return; }
        list.removeIf(item -> item == null || "".equals(item));
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
                                                                  .filter(p -> !p.getFilename().equals(pkg.getFilename()))
                                                                  .collect(Collectors.toList());
        return differentBuilds;
    }

    public static final Integer getPositiveIntFromText(final String text) {
        if (Helper.isPositiveInteger(text)) {
            return Integer.valueOf(text);
        } else {
            //LOGGER.info("Given text {} did not contain positive integer. Full text to parse was: {}", text, fullTextToParse);
            return -1;
        }
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
        final HttpClient  httpClient = createHttpClient();
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                                 .method("HEAD", HttpRequest.BodyPublishers.noBody())
                                 .uri(URI.create(uri))
                                 .timeout(Duration.ofSeconds(3))
                                 .build();
        } catch (Exception e) {
            LOGGER.debug("Uri request: {} failed with exception: {}", uri, e);
            return false;
        }
        try {
            HttpResponse<Void> response = httpClient.send(request, BodyHandlers.discarding());
            return 200 == response.statusCode();
        } catch (InterruptedException | IOException e) {
            LOGGER.debug("Uri request: {} failed with exception: {}", uri, e);
            return false;
        }
    }

    public static final String getAllPackagesMsgV2_OLD(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope) {
        return getAllPackagesMsgV2(allPkgs, downloadable, include_ea, scope, OutputFormat.REDUCED_COMPRESSED);
    }
    public static final String getAllPackagesMsgV2_OLD(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope, final OutputFormat outputFormat) {
        final List<Distro> publicDistros = null == downloadable || !downloadable ? Distro.getPublicDistros() : Distro.getPublicDistrosDirectlyDownloadable();
        final boolean       gaOnly       = null == include_ea || !include_ea;
        final StringBuilder msgBuilder   = new StringBuilder();
        final Scope         scopeToCheck = (BuildScope.BUILD_OF_OPEN_JDK == scope || BuildScope.BUILD_OF_GRAALVM == scope) ? scope : null;

        msgBuilder.append(CURLY_BRACKET_OPEN)
                  .append(QUOTES).append(RESULT).append(QUOTES).append(COLON)
                  .append(allPkgs.parallelStream()
                                 .filter(pkg -> null == scopeToCheck ? pkg != null : Constants.REVERSE_SCOPE_LOOKUP.get(scopeToCheck).contains(pkg.getDistribution().getDistro()))
                                 .filter(pkg -> publicDistros.contains(pkg.getDistribution().getDistro()))
                                 .filter(pkg -> gaOnly ? ReleaseStatus.GA == pkg.getReleaseStatus() : null != pkg.getReleaseStatus())
                                 .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                 .map(pkg -> CacheManager.INSTANCE.jsonCacheV2.get(pkg.getId()))
                                 .collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(COMMA)
                  .append(QUOTES).append(MESSAGE).append(QUOTES).append(COLON).append(QUOTES).append(QUOTES).append(NEW_LINE)
                  .append(CURLY_BRACKET_CLOSE);
        return msgBuilder.toString();
    }

    public static final String getAllPackagesMsgV2(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope) {
        return getAllPackagesMsgV2(allPkgs, downloadable, include_ea, scope, OutputFormat.REDUCED_COMPRESSED);
    }
    public static final String getAllPackagesMsgV2(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope, final OutputFormat outputFormat) {
        final List<Distro>   publicDistros = null == downloadable || !downloadable ? Distro.getPublicDistros() : Distro.getPublicDistrosDirectlyDownloadable();
        final boolean        gaOnly        = null == include_ea || !include_ea;
        final StringBuilder  chunkBuilder  = new StringBuilder();
        final StringBuilder  msgBuilder    = new StringBuilder();
        final Scope          scopeToCheck  = (BuildScope.BUILD_OF_OPEN_JDK == scope || BuildScope.BUILD_OF_GRAALVM == scope) ? scope : null;
        final Partition<Pkg> partition     = new Partition<>(allPkgs, 25000);

        msgBuilder.append(CURLY_BRACKET_OPEN)
                  .append(QUOTES).append(RESULT).append(QUOTES).append(COLON)
                  .append(SQUARE_BRACKET_OPEN);

        for (int i = 0 ; i < partition.size() ; i++) {
            List<Pkg> chunk = partition.get(i);
            chunkBuilder.append(chunk.parallelStream()
                                     .filter(pkg -> null == scopeToCheck ? pkg != null : Constants.REVERSE_SCOPE_LOOKUP.get(scopeToCheck).contains(pkg.getDistribution().getDistro()))
                                     .filter(pkg -> publicDistros.contains(pkg.getDistribution().getDistro()))
                                     .filter(pkg -> gaOnly ? ReleaseStatus.GA == pkg.getReleaseStatus() : null != pkg.getReleaseStatus())
                                     .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                     .map(pkg -> CacheManager.INSTANCE.jsonCacheV2.get(pkg.getId()))
                                     .collect(Collectors.joining(COMMA)));
            msgBuilder.append(chunkBuilder).append(COMMA);
            chunkBuilder.setLength(0);
        }
        msgBuilder.setLength(msgBuilder.length() - 1);
        msgBuilder.append(SQUARE_BRACKET_CLOSE).append(COMMA);
        msgBuilder.append(QUOTES).append(MESSAGE).append(QUOTES).append(COLON).append(QUOTES).append(QUOTES)
                  .append(CURLY_BRACKET_CLOSE);
        return msgBuilder.toString();
    }

    public static final String getAllPackagesMsgV3_OLD(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope) {
        return getAllPackagesMsgV3(allPkgs, downloadable, include_ea, scope, OutputFormat.REDUCED_COMPRESSED);
    }
    public static final String getAllPackagesMsgV3_OLD(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope, final OutputFormat outputFormat) {
        final List<Distro>  publicDistros = null == downloadable || !downloadable ? Distro.getPublicDistros() : Distro.getPublicDistrosDirectlyDownloadable();
        final boolean       gaOnly        = null == include_ea || !include_ea;
        final StringBuilder msgBuilder    = new StringBuilder();
        final Scope         scopeToCheck  = (BuildScope.BUILD_OF_OPEN_JDK == scope || BuildScope.BUILD_OF_GRAALVM == scope) ? scope : null;

        msgBuilder.append(CURLY_BRACKET_OPEN)
                  .append(QUOTES).append(RESULT).append(QUOTES).append(COLON)
                  .append(allPkgs.parallelStream()
                                 .filter(pkg -> null == scopeToCheck ? pkg != null : Constants.REVERSE_SCOPE_LOOKUP.get(scopeToCheck).contains(pkg.getDistribution().getDistro()))
                                 .filter(pkg -> publicDistros.contains(pkg.getDistribution().getDistro()))
                                 .filter(pkg -> gaOnly ? ReleaseStatus.GA == pkg.getReleaseStatus() : null != pkg.getReleaseStatus())
                                 .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                 .map(pkg -> CacheManager.INSTANCE.jsonCacheV3.get(pkg.getId()))
                                 .collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(COMMA)
                  .append(QUOTES).append(MESSAGE).append(QUOTES).append(COLON).append(QUOTES).append(QUOTES)
                  .append(CURLY_BRACKET_CLOSE);
        return msgBuilder.toString();
    }

    public static final String getAllPackagesMsgV3(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope) {
        return getAllPackagesMsgV3(allPkgs, downloadable, include_ea, scope, OutputFormat.REDUCED_COMPRESSED);
    }
    public static final String getAllPackagesMsgV3(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope, final OutputFormat outputFormat) {
        final List<Distro>  publicDistros = null == downloadable || !downloadable ? Distro.getPublicDistros() : Distro.getPublicDistrosDirectlyDownloadable();
        final boolean       gaOnly        = null == include_ea || !include_ea;
        final StringBuilder chunkBuilder  = new StringBuilder();
        final StringBuilder msgBuilder    = new StringBuilder();
        final Scope         scopeToCheck  = (BuildScope.BUILD_OF_OPEN_JDK == scope || BuildScope.BUILD_OF_GRAALVM == scope) ? scope : null;

        msgBuilder.append(CURLY_BRACKET_OPEN)
                  .append(QUOTES).append(RESULT).append(QUOTES).append(COLON)
                  .append(SQUARE_BRACKET_OPEN);

        Partition<Pkg> partition = new Partition<>(allPkgs, 25000);
        for (int i = 0 ; i < partition.size() ; i++) {
            List<Pkg> chunk = partition.get(i);
            chunkBuilder.append(chunk.parallelStream()
                                     .filter(pkg -> null == scopeToCheck ? pkg != null : Constants.REVERSE_SCOPE_LOOKUP.get(scopeToCheck).contains(pkg.getDistribution().getDistro()))
                                     .filter(pkg -> publicDistros.contains(pkg.getDistribution().getDistro()))
                                     .filter(pkg -> gaOnly ? ReleaseStatus.GA == pkg.getReleaseStatus() : null != pkg.getReleaseStatus())
                                     .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                     .map(pkg -> CacheManager.INSTANCE.jsonCacheV3.get(pkg.getId()))
                                     .collect(Collectors.joining(COMMA)));
            msgBuilder.append(chunkBuilder).append(COMMA);
            chunkBuilder.setLength(0);
        }
        msgBuilder.setLength(msgBuilder.length() - 1);
        msgBuilder.append(SQUARE_BRACKET_CLOSE).append(COMMA);
        msgBuilder.append(QUOTES).append(MESSAGE).append(QUOTES).append(COLON).append(QUOTES).append(QUOTES)
                  .append(CURLY_BRACKET_CLOSE);
        return msgBuilder.toString();
    }

    public static final String getAllPackagesMsgMinimizedV3_OLD(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope) {
        final List<Distro>  publicDistros = null == downloadable || !downloadable ? Distro.getPublicDistros() : Distro.getPublicDistrosDirectlyDownloadable();
        final boolean       gaOnly        = null == include_ea || !include_ea;
        final StringBuilder msgBuilder   = new StringBuilder();
        final Scope         scopeToCheck = (BuildScope.BUILD_OF_OPEN_JDK == scope || BuildScope.BUILD_OF_GRAALVM == scope) ? scope : null;

        msgBuilder.append(CURLY_BRACKET_OPEN)
                  .append(QUOTES).append(RESULT).append(QUOTES).append(COLON)
                  .append(INDENT).append(allPkgs.parallelStream()
                                                .filter(pkg -> null == scopeToCheck ? pkg != null : Constants.REVERSE_SCOPE_LOOKUP.get(scopeToCheck).contains(pkg.getDistribution().getDistro()))
                                                .filter(pkg -> publicDistros.contains(pkg.getDistribution().getDistro()))
                                                .filter(pkg -> gaOnly ? ReleaseStatus.GA == pkg.getReleaseStatus() : null != pkg.getReleaseStatus())
                                                .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                                .map(pkg -> CacheManager.INSTANCE.jsonCacheMinimizedV3.get(pkg.getId()))
                                                .collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(COMMA)
                  .append(QUOTES).append(MESSAGE).append(QUOTES).append(COLON).append(QUOTES).append(QUOTES)
                  .append(CURLY_BRACKET_CLOSE);
        return msgBuilder.toString();
    }

    public static final String getAllPackagesMsgMinimizedV3(final Collection<Pkg> allPkgs, final Boolean downloadable, final Boolean include_ea, final BuildScope scope) {
        final List<Distro>  publicDistros = null == downloadable || !downloadable ? Distro.getPublicDistros() : Distro.getPublicDistrosDirectlyDownloadable();
        final boolean       gaOnly        = null == include_ea || !include_ea;
        final StringBuilder chunkBuilder  = new StringBuilder();
        final StringBuilder msgBuilder    = new StringBuilder();
        final Scope         scopeToCheck  = (BuildScope.BUILD_OF_OPEN_JDK == scope || BuildScope.BUILD_OF_GRAALVM == scope) ? scope : null;

        msgBuilder.append(CURLY_BRACKET_OPEN)
                  .append(QUOTES).append(RESULT).append(QUOTES).append(COLON)
                  .append(SQUARE_BRACKET_OPEN);

        Partition<Pkg> partition = new Partition<>(allPkgs, 25000);
        for (int i = 0 ; i < partition.size() ; i++) {
            List<Pkg> chunk = partition.get(i);
            chunkBuilder.append(chunk.parallelStream()
                                     .filter(pkg -> null == scopeToCheck ? pkg != null : Constants.REVERSE_SCOPE_LOOKUP.get(scopeToCheck).contains(pkg.getDistribution().getDistro()))
                                     .filter(pkg -> publicDistros.contains(pkg.getDistribution().getDistro()))
                                     .filter(pkg -> gaOnly ? ReleaseStatus.GA == pkg.getReleaseStatus() : null != pkg.getReleaseStatus())
                                     .sorted(Comparator.comparing(Pkg::getDistributionName).reversed().thenComparing(Comparator.comparing(Pkg::getSemver).reversed()))
                                     .map(pkg -> CacheManager.INSTANCE.jsonCacheMinimizedV3.get(pkg.getId()))
                                     .collect(Collectors.joining(COMMA)));
            msgBuilder.append(chunkBuilder).append(COMMA);
            chunkBuilder.setLength(0);
        }
        msgBuilder.setLength(msgBuilder.length() - 1);
        msgBuilder.append(SQUARE_BRACKET_CLOSE).append(COMMA);
        msgBuilder.append(QUOTES).append(MESSAGE).append(QUOTES).append(COLON).append(QUOTES).append(QUOTES)
                  .append(CURLY_BRACKET_CLOSE);
        return msgBuilder.toString();
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

                Scope qualityScope = QualityScope.fromText(scopeString);
                if (Scope.NOT_FOUND != qualityScope) { scopesFound.add(qualityScope); }
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

    public static final String formatBytes(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    public static final long getFileSize(final String uri) {
        long size = -1;
        HttpResponse<String> response = Helper.httpHeadRequestSync(uri);
        if (null != response) {
            java.net.http.HttpHeaders headers   = response.headers();
            Map<String, List<String>> headerMap = headers.map();
            if (headerMap.containsKey("Content-Length")) {
                List<String> content = headerMap.get("Content-Length");
                if (content.size() > 0) {
                    try {
                        size = Long.parseLong(headerMap.get("Content-Length").get(0));
                    } catch (NumberFormatException e) {
                        LOGGER.debug("Error parsing file size from {}.", uri);
                    }
                }
            }
        }
        return size;
    }

    public static final String getReleaseDetailsUrl(final Semver semver) {
        if (null == semver) { return ""; }
        final int           majorVersion  = semver.getMajorVersion().getAsInt();
        final VersionNumber versionNumber = semver.getVersionNumber();
        final String releaseDetailsUrl;
        if (majorVersion < 7 || majorVersion == 9 || majorVersion == 10 || majorVersion == 12 || majorVersion > MajorVersion.getLatestAsInt(false)) {
            releaseDetailsUrl = "";
        } else if (majorVersion == 7 || majorVersion == 8) {
            releaseDetailsUrl = new StringBuilder().append(Constants.RELEASE_DETAILS_URL).append(majorVersion).append("/?tab=allissues&version=openjdk").append(majorVersion).append("u").append(versionNumber.getUpdate().getAsInt()).toString();
        } else {
            StringBuilder urlBuilder = new StringBuilder().append(Constants.RELEASE_DETAILS_URL).append(majorVersion).append("/?tab=allissues&version=").append(versionNumber.getFeature().getAsInt());
            if (versionNumber.getUpdate().getAsInt() > 0) {
                urlBuilder.append(".").append(versionNumber.getInterim().getAsInt()).append(".").append(versionNumber.getUpdate().getAsInt()).toString();
            } else if (versionNumber.getInterim().getAsInt() > 0) {
                urlBuilder.append(".").append(versionNumber.getInterim().getAsInt()).toString();
            }
            releaseDetailsUrl = urlBuilder.toString();
        }
        return releaseDetailsUrl;
    }

    public static final void checkPkgsForTooEarlyGA(final List<Pkg> pkgs) {
        final LocalDate now = LocalDate.now();
        Constants.GA_RELEASE_DATES.entrySet()
                                  .stream()
                                  .filter(entry -> entry.getValue().isAfter(now))
                                  .forEach(entry -> pkgs.stream()
                                                        .filter(pkg -> pkg.getMajorVersion().getAsInt() == entry.getKey())
                                                        .filter(pkg -> pkg.getReleaseStatus() == ReleaseStatus.GA)
                                                        .forEach(pkg -> pkg.setReleaseStatus(ReleaseStatus.EA)));
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
                                         .timeout(Duration.ofSeconds(10))
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
                                               .timeout(Duration.ofSeconds(10))
                                               .build();

        return httpClientAsync.sendAsync(request, BodyHandlers.ofString());
    }

    public static final HttpResponse<String> httpHeadRequestSync(final String uri) {
        if (null == httpClient) { httpClient = createHttpClient(); }

        final HttpRequest request = HttpRequest.newBuilder()
                                               .method("HEAD", HttpRequest.BodyPublishers.noBody())
                                               .uri(URI.create(uri))
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
    public static final  CompletableFuture<HttpResponse<String>> httpHeadRequestAsync(final String uri) {
        if (null == httpClientAsync) { httpClientAsync = createHttpClient(); }

        final HttpRequest request = HttpRequest.newBuilder()
                                               .method("HEAD", HttpRequest.BodyPublishers.noBody())
                                               .uri(URI.create(uri))
                                               .build();
        return httpClientAsync.sendAsync(request, BodyHandlers.ofString());
    }
}
