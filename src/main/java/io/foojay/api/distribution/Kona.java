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
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.ArchiveType.getFromFileName;
import static io.foojay.api.pkg.OperatingSystem.LINUX;
import static io.foojay.api.pkg.OperatingSystem.MACOS;
import static io.foojay.api.pkg.OperatingSystem.WINDOWS;
import static io.foojay.api.pkg.PackageType.JDK;


public class Kona implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(Kona.class);

    private static final String                       GITHUB_USER            = "Tencent";
    private static final String                       PACKAGE_URL            = "https://api.github.com/repos/" + GITHUB_USER + "/TencentKona";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM     = "";
    private static final String                       OPERATING_SYSTEM_PARAM = "";
    private static final String                       ARCHIVE_TYPE_PARAM     = "";
    private static final String                       PACKAGE_TYPE_PARAM     = "";
    private static final String                       RELEASE_STATUS_PARAM   = "";
    private static final String                       SUPPORT_TERM_PARAM     = "";
    private static final String                       BITNESS_PARAM          = "";

    private static final HashAlgorithm HASH_ALGORITHM      = HashAlgorithm.NONE;
    private static final String        HASH_URI            = "";
    private static final SignatureType SIGNATURE_TYPE      = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI          = "";
    private static final String                       OFFICIAL_URI           = "https://github.com/Tencent/TencentKona-11/wiki";


    @Override public Distro getDistro() { return Distro.KONA; }

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
        return List.of("kona", "KONA", "Kona");
    }

    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.KONA.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL);

        switch(versionNumber.getFeature().getAsInt()) {
            case 8:
            case 11:
            case 17:
                queryBuilder.append("-").append(versionNumber.getFeature().getAsInt()).append("/releases").append("?per_page=100");
                break;
            default:
                return "";
        }

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);
        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();
            return pkgs;
        }

    public List<Pkg> getAllPkgs() {
        List<Pkg> pkgs = new ArrayList<>();

        final List<String> packageUrls = new ArrayList<>();

        CacheManager.INSTANCE.getMajorVersions().stream().filter(majorVersion -> majorVersion.getAsInt() > 7).forEach(majorVersion -> {
            switch(majorVersion.getAsInt()) {
                case 8:
                case 11:
                case 17:
                case 18:
                    packageUrls.add(new StringBuilder(PACKAGE_URL).append("-").append(majorVersion.getAsInt()).append("/releases").append("?per_page=100").toString());
                    break;
        }
        });

        try {
            for (String packageUrl : packageUrls) {
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
            LOGGER.error("Error fetching all packages from Kona. {}", e);
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
                if (filename.endsWith(Constants.FILE_ENDING_TXT) || filename.endsWith(Constants.FILE_ENDING_JAR) || filename.endsWith(Constants.FILE_ENDING_MD5) || filename.contains("javadoc")) { continue; }

            String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.KONA.get());
                pkg.setFileName(filename);
            pkg.setDirectDownloadUri(downloadLink);

                ArchiveType ext = getFromFileName(filename);
                pkg.setArchiveType(ext);

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                 .filter(entry -> filename.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);
                if (Architecture.NONE == arch) { continue; }
            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());

                VersionNumber vNumber = null;
                String        n       = filename.replaceAll("TencentKona", "");
                if (n.startsWith("-")) { n = n.substring(1); }
                if (n.startsWith("11")) {
                    n = n.substring(0, n.indexOf("_"));
                    if (n.endsWith("-jdk")) { n = n.replaceAll("-jdk", ""); }
                    vNumber = VersionNumber.fromText(n);
                } else if (n.startsWith("8")) {
                    n = n.replaceAll(ext.getFileEndings().get(0), "");
                    String[] parts = n.split("_");
                    if (!parts[parts.length - 1].equals("64")) {
                        vNumber = VersionNumber.fromText(parts[parts.length - 1]);
                    }
            }
                if (null == vNumber) { continue; }

            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
                VersionNumber dNumber = VersionNumber.fromText(filename);
            pkg.setDistributionVersion(dNumber);
                pkg.setTermOfSupport(Helper.getTermOfSupport(vNumber));
            pkg.setPackageType(JDK);
                pkg.setReleaseStatus(ReleaseStatus.GA);

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> filename.contains(entry.getKey()))
                                                                  .findFirst()
                                                                  .map(Entry::getValue)
                                                                  .orElse(OperatingSystem.NONE);

            if (OperatingSystem.NONE == os) {
                switch (pkg.getArchiveType()) {
                    case DEB:
                    case RPM:
                        case TGZ:
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
                if (OperatingSystem.NONE == os) { continue; }
            pkg.setOperatingSystem(os);

            pkg.setFreeUseInProduction(Boolean.TRUE);

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

                if (null == filename || filename.isEmpty() || !filename.endsWith(Constants.FILE_ENDING_MD5)) { continue; }
                String nameToMatch;
                if (filename.endsWith(Constants.FILE_ENDING_MD5)) {
                    nameToMatch = filename.replaceAll("." + Constants.FILE_ENDING_MD5, "");
                } else {
                    continue;
                }

                final String  downloadLink = assetJsonObj.get("browser_download_url").getAsString();
                Optional<Pkg> optPkg       = pkgs.stream().filter(pkg -> pkg.getFileName().contains(nameToMatch)).findFirst();
                if (optPkg.isPresent()) {
                    Pkg pkg = optPkg.get();
                    pkg.setChecksumUri(downloadLink);
                    pkg.setChecksumType(HashAlgorithm.MD5);
                }
            }
        }

        LOGGER.debug("Successfully fetched {} packages from {}", pkgs.size(), PACKAGE_URL);
        return pkgs;
    }
}
