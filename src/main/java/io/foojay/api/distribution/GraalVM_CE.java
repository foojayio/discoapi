/*
 * Copyright (c) 2023.
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
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.GithubTokenPool;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.ArchiveType.SRC_TAR;
import static eu.hansolo.jdktools.ArchiveType.getFromFileName;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;


public abstract class GraalVM_CE implements Distribution {
    protected              Distro        distro;
    private                Logger        logger;
    protected        final String        distroName;
    protected        final int           jdkVersion;
    protected        final Pattern       filenamePattern;
    protected        final Matcher       filenameMatcher;
    protected static final String        GITHUB_USER             = "graalvm";
    protected static final String        PACKAGE_URL             = "https://api.github.com/repos/" + GITHUB_USER + "/graalvm-ce-builds/releases";
    protected static final String        PACKAGE_EA_URL          = "https://api.github.com/repos/" + GITHUB_USER + "/graalvm-ce-dev-builds/releases";

    // URL parameters
    protected static final String        ARCHITECTURE_PARAM      = "";
    protected static final String        OPERATING_SYSTEM_PARAM  = "";
    protected static final String        ARCHIVE_TYPE_PARAM      = "";
    protected static final String        PACKAGE_TYPE_PARAM      = "";
    protected static final String        RELEASE_STATUS_PARAM    = "";
    protected static final String        SUPPORT_TERM_PARAM      = "";
    protected static final String        BITNESS_PARAM           = "";

    protected static final HashAlgorithm HASH_ALGORITHM          = HashAlgorithm.NONE;
    protected static final String        HASH_URI                = "";
    protected static final SignatureType SIGNATURE_TYPE          = SignatureType.NONE;
    protected static final HashAlgorithm SIGNATURE_ALGORITHM     = HashAlgorithm.NONE;
    protected static final String        SIGNATURE_URI           = "";
    protected static final String        OFFICIAL_URI            = "https://www.graalvm.org/";


    public GraalVM_CE(final String distroName, final int jdkVersion) {
        this.distroName      = distroName;
        this.jdkVersion      = jdkVersion;
        this.filenamePattern = Pattern.compile(new StringBuilder().append("^(graalvm-ce-java").append(this.jdkVersion).append(")(.*)(\\.tar\\.gz|\\.zip)$").toString());
        this.filenameMatcher = filenamePattern.matcher("");
    }


    @Override public Distro getDistro() {
        if (null == this.distro) { this.distro = Distro.valueOf(this.distroName); }
        return this.distro;
    }

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
        return List.of("graalvm_ce" + this.jdkVersion, "graalvmce" + this.jdkVersion, "GraalVM CE " + this.jdkVersion, "GraalVMCE" + this.jdkVersion, "GraalVM_CE" + this.jdkVersion);
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> getDistro().get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream()
                                             .sorted(Comparator.comparing(Semver::getVersionNumber).reversed())
                                             .collect(Collectors.toList());
    }


    protected Logger getLogger() {
        if (null == logger) { this.logger = LoggerFactory.getLogger(getDistro().get().getClass()); }
        return logger;
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        getLogger().debug("Query string for {}: {}", this.getName(), PACKAGE_URL);
        return PACKAGE_URL;
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        TermOfSupport supTerm = Helper.getTermOfSupport(this.jdkVersion);
        supTerm = TermOfSupport.MTS == supTerm ? TermOfSupport.STS : supTerm;

        VersionNumber vNumber = null;
        String tag = jsonObj.get("tag_name").getAsString();
        if (tag.contains("vm-")) {
            tag = tag.substring(tag.lastIndexOf("vm-")).replace("vm-", "");
            vNumber = VersionNumber.fromText(tag);
        } else if (tag.contains("-dev")) {
            int build = Integer.parseInt(tag.substring(tag.lastIndexOf("_") + 1));
            tag = tag.substring(0, tag.indexOf("-"));
            vNumber = VersionNumber.fromText(tag);
            vNumber.setBuild(build);
        }

        boolean prerelease = false;
        if (jsonObj.has("prerelease")) {
            prerelease = jsonObj.get("prerelease").getAsBoolean();
        }
        if (prerelease && ReleaseStatus.EA != releaseStatus) { return pkgs; }

        JsonArray assets = jsonObj.getAsJsonArray("assets");
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     filename     = assetJsonObj.get("name").getAsString();
            if (filename.endsWith(Constants.FILE_ENDING_TXT) || filename.endsWith(Constants.FILE_ENDING_JAR) ||
                filename.endsWith(Constants.FILE_ENDING_SHA1) || filename.endsWith(Constants.FILE_ENDING_SHA256)) { continue; }

            filenameMatcher.reset(filename);
            if (!filenameMatcher.matches()) { continue; }
            String   strippedFilename = filename.replaceFirst(new StringBuilder("graalvm-ce-java").append(this.jdkVersion).append("-").toString(), "").replaceAll("(\\.tar\\.gz|\\.zip)", "");
            String[] filenameParts    = strippedFilename.split("-");

            String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
            }

            Pkg pkg = new Pkg();

            pkg.setDistribution(getDistro().get());
            pkg.setFileName(filename);
            pkg.setDirectDownloadUri(downloadLink);

            ArchiveType ext = getFromFileName(filename);
            if (SRC_TAR == ext || (ArchiveType.NONE != archiveType && ext != archiveType)) { continue; }
            pkg.setArchiveType(ext);

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> strippedFilename.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);
            if (Architecture.NONE == arch) {
                getLogger().debug("Architecture not found in GraalVM CE" + this.jdkVersion + " for filename: {}", filename);
                continue;
            }

            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());

            if (null == vNumber && filenameParts.length > 2) {
                vNumber = VersionNumber.fromText(filenameParts[2]);
            }
            if (latest) {
                if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { continue; }
            } else {
                //if (versionNumber.compareTo(vNumber) != 0) { continue; }
            }
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);
            pkg.setJdkVersion(new MajorVersion(jdkVersion));

            pkg.setTermOfSupport(supTerm);

            pkg.setPackageType(JDK);

            pkg.setReleaseStatus(ReleaseStatus.NONE == releaseStatus ? ReleaseStatus.GA : releaseStatus);

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                  .filter(entry -> strippedFilename.contains(entry.getKey()))
                                                                  .findFirst()
                                                                  .map(Entry::getValue)
                                                                  .orElse(OperatingSystem.NONE);

            if (OperatingSystem.NONE == os) {
                switch (pkg.getArchiveType()) {
                    case DEB:
                    case RPM:
                    case TAR_GZ:
                        os = LINUX;
                        break;
                    case MSI:
                    case ZIP:
                        os = WINDOWS;
                        break;
                    case DMG:
                    case PKG:
                        os = MACOS;
                        break;
                }
            }
            if (OperatingSystem.NONE == os) {
                getLogger().debug("Operating System not found in GraalVM CE" + this.jdkVersion + " for filename: {}", filename);
                continue;
            }
            pkg.setOperatingSystem(os);

            pkg.setFreeUseInProduction(Boolean.TRUE);

            pkg.setSize(Helper.getFileSize(downloadLink));

            pkgs.add(pkg);
        }

        // Fetch checksums
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     filename     = assetJsonObj.get("name").getAsString();

            if (null == filename || filename.isEmpty() || !filename.endsWith(Constants.FILE_ENDING_SHA256)) { continue; }
            String nameToMatch;
            if (filename.endsWith(Constants.FILE_ENDING_SHA256)) {
                nameToMatch = filename.replaceAll("." + Constants.FILE_ENDING_SHA256, "");
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

        return pkgs;
    }

    public List<Pkg> getAllPkgs(boolean includingEA, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        pkgs.addAll(getAllPkgs(PACKAGE_URL, ReleaseStatus.GA, onlyNewPkgs));
        if (includingEA) { pkgs.addAll(getAllPkgs(PACKAGE_EA_URL, ReleaseStatus.EA, onlyNewPkgs)); }
        return pkgs;
    }
    private List<Pkg> getAllPkgs(final String query, final ReleaseStatus releaseStatus, final boolean onlyNewPkgs) {
        VersionNumber versionNumber = new VersionNumber(this.jdkVersion);

        if (query.isEmpty()) { return List.of(); }
        try {
            List<Pkg>           pkgs      = new LinkedList<>();
            List<Pkg>           pkgsFound = new ArrayList<>();
            Map<String, String> headers   = new HashMap<>();
            if (query.contains("api.github.com")) {
                headers.put("accept", "application/vnd.github.v3+json");
                headers.put("authorization", GithubTokenPool.INSTANCE.next());
            }
            HttpResponse<String> response = Helper.get(query, headers);
            if (null == response) {
                getLogger().debug("Response {} returned null.", getDistro().getApiString());
            } else {
                if (response.statusCode() == 200) {
                    String      body    = response.body();
                    Gson        gson    = new Gson();
                    JsonElement element = gson.fromJson(body, JsonElement.class);
                    if (element instanceof JsonArray) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject pkgJsonObj = jsonArray.get(i).getAsJsonObject();
                            List<Pkg> pkgsInDistribution = getPkgFromJson(pkgJsonObj, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null, releaseStatus, TermOfSupport.NONE, onlyNewPkgs);
                            pkgsFound.addAll(pkgsInDistribution);
                        }
                    } else if (element instanceof JsonObject) {
                        JsonObject pkgJsonObj = element.getAsJsonObject();
                        List<Pkg> pkgsInDistribution = getPkgFromJson(pkgJsonObj, versionNumber, false, OperatingSystem.NONE, Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, null, releaseStatus, TermOfSupport.NONE, onlyNewPkgs);
                        pkgsFound.addAll(pkgsInDistribution);
                    }
                } else {
                    // Problem with url request
                    getLogger().debug("Error getting packages for {} {}, calling {}.Response ({}) {} ", getName(), versionNumber, query, response.statusCode(), response.body());
                    return pkgs;
                }
            }

            pkgs.addAll(pkgsFound);
            HashSet<Pkg> unique = new HashSet<>(pkgs);
            pkgs = new LinkedList<>(unique);

            return pkgs;
        } catch (Exception e) {
            System.out.println("Error get packages for " + getName() + " " + versionNumber + " calling " + query + ". Exception: " + e);
            getLogger().debug("Error get packages for {} {} calling {}. {}", getName(), versionNumber, query, e.getMessage());
            return new ArrayList<>();
        }
    }
}
