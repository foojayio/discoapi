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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.Architecture.ARM;
import static io.foojay.api.pkg.Architecture.MIPS;
import static io.foojay.api.pkg.Architecture.PPC;
import static io.foojay.api.pkg.Architecture.SPARCV9;
import static io.foojay.api.pkg.Architecture.X64;
import static io.foojay.api.pkg.Architecture.X86;
import static io.foojay.api.pkg.ArchiveType.CAB;
import static io.foojay.api.pkg.ArchiveType.DEB;
import static io.foojay.api.pkg.ArchiveType.DMG;
import static io.foojay.api.pkg.ArchiveType.MSI;
import static io.foojay.api.pkg.ArchiveType.RPM;
import static io.foojay.api.pkg.ArchiveType.TAR_GZ;
import static io.foojay.api.pkg.ArchiveType.ZIP;
import static io.foojay.api.pkg.Bitness.BIT_32;
import static io.foojay.api.pkg.Bitness.BIT_64;
import static io.foojay.api.pkg.OperatingSystem.ALPINE_LINUX;
import static io.foojay.api.pkg.OperatingSystem.LINUX;
import static io.foojay.api.pkg.OperatingSystem.LINUX_MUSL;
import static io.foojay.api.pkg.OperatingSystem.MACOS;
import static io.foojay.api.pkg.OperatingSystem.QNX;
import static io.foojay.api.pkg.OperatingSystem.SOLARIS;
import static io.foojay.api.pkg.OperatingSystem.WINDOWS;
import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.PackageType.JRE;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;
import static io.foojay.api.pkg.TermOfSupport.LTS;
import static io.foojay.api.pkg.TermOfSupport.MTS;
import static io.foojay.api.pkg.TermOfSupport.STS;


public class Zulu implements Distribution {
    private static final Logger                       LOGGER                     = LoggerFactory.getLogger(Zulu.class);

    private static final Pattern                      FILENAME_PREFIX_PATTERN    = Pattern.compile("(zulu|zre)(\\d+)\\.(\\d+)\\.(\\d+)(\\.|_?)(\\d+)?");
    private static final Matcher                      FILENAME_PREFIX_MATCHER    = FILENAME_PREFIX_PATTERN.matcher("");
    private static final Pattern                      FILENAME_PREFIX_VN_PATTERN = Pattern.compile("(zulu-repo-|zulu-repo_|zulu|zre)[0-9]{1,3}\\.[0-9]{1,3}(\\.|\\+)[0-9]{1,4}(\\.|-|_)([0-9]{1,3}-)?([0-9]{1,4}_[0-9]{1,4}-)?(ca-|ea-)?(fx-)?(dbg-)?(hl)?(cp(1|2|3)-)?(oem-)?(-|jre|jdk)?");
    private static final Pattern                      FEATURE_PREFIX_PATTERN     = Pattern.compile("^((-ea)|(-ca)|(-jdk)|(-jre)|(-fx)|(-))?((-ea)|(-ca)|(-jdk)|(-jre)|(-fx)|(-))?((-ea)|(-ca)|(-jdk)|(-jre)|(-fx)|(-))?");
    private static final Matcher                      FEATURE_PREFIX_MATCHER     = FEATURE_PREFIX_PATTERN.matcher("");
    private static final String                       PACKAGE_URL                = "https://api.azul.com/zulu/download/community/v1.0/bundles/";
    private static final String                       CDN_URL                    = "https://cdn.azul.com/zulu/bin/";

    // URL parameters
    private static final String                       JDK_VERSION_PARAM          = "jdk_version";
    private static final String                       ARCHITECTURE_PARAM         = "arch";
    private static final String                       OPERATING_SYSTEM_PARAM     = "os";
    private static final String                       ARCHIVE_TYPE_PARAM         = "ext";
    private static final String                       PACKAGE_TYPE_PARAM         = "bundle_type";
    private static final String                       RELEASE_STATUS_PARAM       = "release_status";
    private static final String                       TERM_OF_SUPPORT_PARAM      = "support_term";
    private static final String                       BITNESS_PARAM              = "hw_bitness";
    private static final String                       JAVAFX_PARAM               = "javafx";
    private static final String                       INCLUDE_FIELDS_PARAM       = "include_fields";
    private static final String                       SIGNATURES_PARAM           = "signatures";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP           = Map.of(ARM, "arm", MIPS, "mips", PPC, "ppc", SPARCV9, "sparcv9", X86, "x86", X64, "x86");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP       = Map.of(LINUX, "linux", LINUX_MUSL, "linux_musl", ALPINE_LINUX, "linux_musl", MACOS, "macos", WINDOWS, "windows", SOLARIS, "solaris", QNX, "qnx");
    private static final Map<ArchiveType, String>     ARCHIVE_TYPE_MAP           = Map.of(CAB, "cab", DEB, "deb", DMG, "dmg", MSI, "msi", RPM, "rpm", TAR_GZ, "tar.gz", ZIP, "zip");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP           = Map.of(JDK, "jdk", JRE, "jre");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP         = Map.of(EA, "ea", GA, "ga");
    private static final Map<TermOfSupport, String>   TERMS_OF_SUPPORT_MAP       = Map.of(STS, "sts", MTS, "mts", LTS, "lts");
    private static final Map<Bitness, String>         BITNESS_MAP                = Map.of(BIT_32, "32", BIT_64, "64");

    // JSON fields
    private static final String                       FIELD_ID                   = "id";
    private static final String                       FIELD_NAME                 = "name";
    private static final String                       FIELD_URL                  = "url";
    private static final String                       FIELD_JDK_VERSION          = "jdk_version";
    private static final String                       FIELD_JAVA_VERSION         = "java_version";
    private static final String                       FIELD_ZULU_VERSION         = "zulu_version";
    private static final String                       FIELD_SHA_256_HASH         = "sha256_hash";
    private static final String                       FIELD_SIGNATURES           = "signatures";
    private static final String                       FIELD_TYPE                 = "type";
    private static final String                       FIELD_OPEN_JDK_BUILD_NO    = "openjdk_build_number";

    private static final HashAlgorithm                HASH_ALGORITHM             = HashAlgorithm.NONE;
    private static final String                       HASH_URI                   = "";
    private static final SignatureType                SIGNATURE_TYPE             = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM        = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI              = "";


    @Override public Distro getDistro() { return Distro.ZULU; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return TERM_OF_SUPPORT_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }

    @Override public HashAlgorithm getHashAlgorithm() { return HASH_ALGORITHM; }

    @Override public String getHashUri() { return HASH_URI; }

    @Override public SignatureType getSignatureType() { return SIGNATURE_TYPE; }

    @Override public HashAlgorithm getSignatureAlgorithm() { return SIGNATURE_ALGORITHM; }

    @Override public String getSignatureUri() { return SIGNATURE_URI; }

    @Override public List<String> getSynonyms() {
        return List.of("zulu", "ZULU", "Zulu", "zulucore", "ZULUCORE", "ZuluCore", "zulu_core", "ZULU_CORE", "Zulu_Core", "zulu core", "ZULU CORE", "Zulu Core");
    }

    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.ZULU.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL);
        int initialSize = queryBuilder.length();

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append(JDK_VERSION_PARAM).append("=").append(versionNumber.getFeature().getAsInt());

        if (operatingSystem != OperatingSystem.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(OPERATING_SYSTEM_PARAM).append("=").append(OPERATING_SYSTEM_MAP.get(operatingSystem));
        }

        if (architecture != Architecture.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(ARCHITECTURE_PARAM).append("=").append(ARCHITECTURE_MAP.get(architecture));
        }

        if (bitness != Bitness.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(BITNESS_PARAM).append("=").append(BITNESS_MAP.get(bitness));
        }

        if (archiveType != ArchiveType.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(ARCHIVE_TYPE_PARAM).append("=").append(ARCHIVE_TYPE_MAP.get(archiveType));
        }

        if (packageType != PackageType.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(PACKAGE_TYPE_PARAM).append("=").append(PACKAGE_TYPE_MAP.get(packageType));
        }

        if (null != javafxBundled) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(JAVAFX_PARAM).append("=").append(javafxBundled);
        }

        if (ReleaseStatus.NONE == releaseStatus) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(RELEASE_STATUS_PARAM).append("=").append("both");
        } else {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(RELEASE_STATUS_PARAM).append("=").append(RELEASE_STATUS_MAP.get(releaseStatus));
        }

        if (termOfSupport != TermOfSupport.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(TERM_OF_SUPPORT_PARAM).append("=").append(TERMS_OF_SUPPORT_MAP.get(termOfSupport));
        }

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append(INCLUDE_FIELDS_PARAM).append("=").append(SIGNATURES_PARAM);

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);
        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();

        String fileName     = jsonObj.get(FIELD_NAME).getAsString();
        String downloadLink = jsonObj.get(FIELD_URL).getAsString();


        JsonArray jdkVersionArray = jsonObj.get(FIELD_JDK_VERSION).getAsJsonArray();
        VersionNumber vNumber;
        if (fileName.toLowerCase().startsWith("zulu1.")) {
            vNumber = new VersionNumber(jdkVersionArray.get(0).getAsInt(), jdkVersionArray.get(1).getAsInt(), jdkVersionArray.get(2).getAsInt(), 0);
        } else {
            //Get the real version number from the filename without the prefix
            final String fileNameWithoutPrefix = fileName.replaceAll(FILENAME_PREFIX_VN_PATTERN.pattern(), "");
            vNumber = VersionNumber.fromText(fileNameWithoutPrefix);
        }

        if (jsonObj.has(FIELD_OPEN_JDK_BUILD_NO)) {
            vNumber.setBuild(jsonObj.get(FIELD_OPEN_JDK_BUILD_NO).getAsInt());
        }

        JsonArray zuluVersionArray = jsonObj.get(FIELD_ZULU_VERSION).getAsJsonArray();
        VersionNumber dNumber = new VersionNumber(zuluVersionArray.get(0).getAsInt(), zuluVersionArray.get(1).getAsInt(), zuluVersionArray.get(2).getAsInt(), zuluVersionArray.get(3).getAsInt());

        if (!latest && versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
        if (latest) {
            if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
        }

        TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);

        Pkg pkg = new Pkg();
        pkg.setDistribution(Distro.ZULU.get());
        pkg.setVersionNumber(vNumber);
        pkg.setJavaVersion(vNumber);
        pkg.setDistributionVersion(dNumber);
        pkg.setFileName(fileName);
        pkg.setDirectDownloadUri(downloadLink);

        if (jsonObj.has(FIELD_SIGNATURES)) {
            JsonArray signaturesArray = jsonObj.get(FIELD_SIGNATURES).getAsJsonArray();
            if (signaturesArray.size() > 0) {
                for (int i = 0 ; i < signaturesArray.size() ; i++) {
                    JsonObject signatureJson = signaturesArray.get(i).getAsJsonObject();
                    if (signatureJson.has(FIELD_TYPE)) {
                        if (signatureJson.get(FIELD_TYPE).getAsString().equals("openpgp")) {
                            if (signatureJson.has(FIELD_URL)) {
                                String signatureUri = signatureJson.get(FIELD_URL).getAsString();
                                pkg.setSignatureUri(signatureUri);
                            }
                        }
                    }
                }
            }
        }

        String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(fileName).replaceAll("");

        if (null != javafxBundled && javafxBundled && !withoutPrefix.contains(Constants.FX_POSTFIX)) { return pkgs; }
        pkg.setJavaFXBundled(withoutPrefix.contains(Constants.FX_POSTFIX));

        ArchiveType ext = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                       .filter(entry -> fileName.endsWith(entry.getKey()))
                                                       .findFirst()
                                                       .map(Entry::getValue)
                                                       .orElse(ArchiveType.NONE);

        if (ArchiveType.NONE == ext) {
            LOGGER.debug("Archive Type not found in Zulu for filename: {}", fileName);
            return pkgs;
        }

        pkg.setArchiveType(ext);

        switch (packageType) {
            case JDK:
                if (withoutPrefix.contains(Constants.JRE_POSTFIX)) { return pkgs; }
                pkg.setPackageType(JDK);
                break;
            case JRE:
                if (withoutPrefix.contains(Constants.JDK_POSTFIX)) { return pkgs; }
                pkg.setPackageType(JRE);
                break;
            case NONE:
                pkg.setPackageType(withoutPrefix.contains(Constants.JRE_POSTFIX) ? JRE : JDK);
                break;
        }

        switch (releaseStatus) {
            case NONE:
                pkg.setReleaseStatus(withoutPrefix.contains(Constants.EA_POSTFIX) ? EA : GA);
                break;
            case EA:
                if (!withoutPrefix.contains(Constants.EA_POSTFIX)) { return pkgs; }
                pkg.setReleaseStatus(EA);
                break;
            case GA:
                if (withoutPrefix.contains(Constants.EA_POSTFIX)) { return pkgs; }
                pkg.setReleaseStatus(EA);
                break;
        }

        String withoutFeaturePrefix = FEATURE_PREFIX_MATCHER.reset(withoutPrefix).replaceAll("");

        pkg.setHeadless(withoutFeaturePrefix.contains(Constants.HEADLESS_POSTFIX));

        Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                         .filter(entry -> fileName.contains(entry.getKey()))
                                                         .findFirst()
                                                         .map(Entry::getValue)
                                                         .orElse(Architecture.NONE);

        if (Architecture.NONE == arch && fileName.contains("macos")) {
            arch = X64;
        }

        if (Architecture.NONE == arch) {
            LOGGER.debug("Architecture not found in Zulu for filename: {}", fileName);
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
            LOGGER.debug("Operating System not found in Zulu for filename: {}", fileName);
            return pkgs;
        }

        pkg.setOperatingSystem(os);

        pkg.setTermOfSupport(supTerm);

        pkg.setFreeUseInProduction(Boolean.TRUE);

        pkgs.add(pkg);

        return pkgs;
    }

    /**
     * Returns all packages found on the Azul Zulu Community CDN
     * @return all packages found on the Azul Zulu Community CDN
     */
    public List<Pkg> getAllPackagesFromCDN() {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            final HttpResponse<String> response = Helper.get(CDN_URL);
            if (null == response) { return pkgs; }
            final String html = response.body();
            if (html.isEmpty()) { return pkgs; }

            final Pattern      filenamePrefixVersion       = Pattern.compile("(zulu|zre|zulu-repo|zulurepo)((-|_)?)(\\d+)\\.(\\d+)(\\.|\\+)(\\d+)(\\.|_?)(\\d+)?(-|_)([0-9]+-)?((ca|ea)(-))?(hl-)?(fx-)?(cp[0-9]+-)?(jdk|jre)?");
            final Pattern      filenamePrefixDistroVersion = Pattern.compile("(zulu|zre|zulu-repo|zulurepo)");
            final List<String> fileHrefs                   = new ArrayList<>(Helper.getFileHrefsFromString(html));
            for (String href : fileHrefs) {
                String filename = Helper.getFileNameFromText(href);
                if (filename.contains("noarch")) { continue; }

                String          reducedToVersionFilename       = filename.startsWith("zulu1.") ? filename.replaceAll(filenamePrefixDistroVersion.pattern(), "") : filename.replaceAll(filenamePrefixVersion.pattern(), "");
                VersionNumber   versionNumber                  = VersionNumber.fromText(reducedToVersionFilename);
                TermOfSupport   termOfSupport                  = Helper.getTermOfSupport(versionNumber);
                String          downloadLink                   = CDN_URL + filename;

                String          reducedToDistroVersionFilename = filename.startsWith("zulu1.") ? filename.replaceAll(filenamePrefixVersion.pattern(), "") : filename.replaceAll(filenamePrefixDistroVersion.pattern(), "");
                VersionNumber   distroVersionNumber            = VersionNumber.fromText(reducedToDistroVersionFilename);

                Pkg pkg = new Pkg();
                pkg.setDistribution(Distro.ZULU.get());
                pkg.setVersionNumber(versionNumber);
                pkg.setJavaVersion(versionNumber);
                pkg.setDistributionVersion(distroVersionNumber);

                PackageType packageType = Constants.PACKAGE_TYPE_LOOKUP.entrySet().stream()
                                                                       .filter(entry -> filename.contains(entry.getKey()))
                                                                       .findFirst()
                                                                       .map(Entry::getValue)
                                                                       .orElse(PackageType.NOT_FOUND);
                if (PackageType.NOT_FOUND == packageType) { packageType = PackageType.JDK; }
                pkg.setPackageType(packageType);

                ArchiveType archiveType = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                                       .filter(entry -> filename.endsWith(entry.getKey()))
                                                                       .findFirst()
                                                                       .map(Entry::getValue)
                                                                       .orElse(ArchiveType.NOT_FOUND);
                if (ArchiveType.NOT_FOUND == archiveType) { continue; }
                pkg.setArchiveType(archiveType);

                OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                      .filter(entry -> filename.contains(entry.getKey()))
                                                                      .findFirst()
                                                                      .map(Entry::getValue)
                                                                      .orElse(OperatingSystem.NOT_FOUND);
                if (OperatingSystem.NOT_FOUND == os) {
                    os = Helper.fetchOperatingSystemByArchiveType(archiveType.getUiString());
                }

                if (OperatingSystem.NOT_FOUND == os) { continue; }
                pkg.setOperatingSystem(os);

                Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                         .filter(entry -> filename.contains(entry.getKey()))
                                                                         .findFirst()
                                                                         .map(Entry::getValue)
                                                                         .orElse(Architecture.NOT_FOUND);
                if (Architecture.NOT_FOUND == architecture) {
                    if (OperatingSystem.MACOS == pkg.getOperatingSystem()) {
                        architecture = Architecture.X64;
                    } else {
                        continue;
                    }
                }
                pkg.setArchitecture(architecture);
                pkg.setBitness(architecture.getBitness());

                pkg.setReleaseStatus(filename.contains("ea") ? ReleaseStatus.EA : ReleaseStatus.GA);

                pkg.setTermOfSupport(termOfSupport);
                pkg.setFileName(filename);
                pkg.setArchiveType(archiveType);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setJavaFXBundled(filename.contains("-fx"));

                pkg.setFreeUseInProduction(Boolean.TRUE);

                pkgs.add(pkg);
            }
        } catch (Exception e) {
            LOGGER.debug("Error fetching packages from Zulu CDN. {}", e.getMessage());
        }
        return pkgs;
    }
}
