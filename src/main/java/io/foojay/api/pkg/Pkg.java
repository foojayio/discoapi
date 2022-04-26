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

package io.foojay.api.pkg;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.FPU;
import eu.hansolo.jdktools.HashAlgorithm;
import eu.hansolo.jdktools.LibCType;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.Verification;
import eu.hansolo.jdktools.util.OutputFormat;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.SimpleMajorVersion;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static io.foojay.api.util.Constants.API_VERSION_V1;
import static io.foojay.api.util.Constants.API_VERSION_V2;
import static io.foojay.api.util.Constants.BASE_URL;
import static io.foojay.api.util.Constants.COLON;
import static io.foojay.api.util.Constants.COMMA;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.ENDPOINT_EPHEMERAL_IDS;
import static io.foojay.api.util.Constants.ENDPOINT_IDS;
import static io.foojay.api.util.Constants.INDENT;
import static io.foojay.api.util.Constants.INDENTED_QUOTES;
import static io.foojay.api.util.Constants.NEW_LINE;
import static io.foojay.api.util.Constants.QUOTES;
import static io.foojay.api.util.Constants.SLASH;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_OPEN;


public class Pkg {
    public static final String          FIELD_ID                     = "id";
    public static final String          FIELD_DISTRIBUTION           = "distribution";
    public static final String          FIELD_MAJOR_VERSION          = "major_version";
    public static final String          FIELD_JAVA_VERSION           = "java_version";
    public static final String          FIELD_FEATURE_VERSION        = "feature_version";
    public static final String          FIELD_INTERIM_VERSION        = "interim_version";
    public static final String          FIELD_UPDATE_VERSION         = "update_version";
    public static final String          FIELD_PATCH_VERSION          = "patch_version";
    public static final String          FIELD_BUILD_VERSION          = "build_version";
    public static final String          FIELD_DISTRIBUTION_VERSION   = "distribution_version";
    public static final String          FIELD_JDK_VERSION            = "jdk_version";
    public static final String          FIELD_LATEST_BUILD_AVAILABLE = "latest_build_available";
    public static final String          FIELD_ARCHITECTURE           = "architecture";
    public static final String          FIELD_BITNESS                = "bitness";
    public static final String          FIELD_FPU                    = "fpu";
    public static final String          FIELD_OPERATING_SYSTEM       = "operating_system";
    public static final String          FIELD_LIB_C_TYPE             = "lib_c_type";
    public static final String          FIELD_PACKAGE_TYPE           = "package_type";
    public static final String          FIELD_RELEASE_STATUS         = "release_status";
    public static final String          FIELD_ARCHIVE_TYPE           = "archive_type";
    public static final String          FIELD_TERM_OF_SUPPORT        = "term_of_support";
    public static final String          FIELD_JAVAFX_BUNDLED         = "javafx_bundled";
    public static final String          FIELD_DIRECTLY_DOWNLOADABLE  = "directly_downloadable";
    public static final String          FIELD_FILENAME               = "filename";
    public static final String          FIELD_DIRECT_DOWNLOAD_URI    = "direct_download_uri";
    public static final String          FIELD_DOWNLOAD_SITE_URI      = "download_site_uri";
    public static final String          FIELD_SIGNATURE_URI          = "signature_uri";
    public static final String          FIELD_CHECKSUM_URI           = "checksum_uri";
    public static final String          FIELD_CHECKSUM               = "checksum";
    public static final String          FIELD_CHECKSUM_TYPE          = "checksum_type";
    public static final String          FIELD_EPHEMERAL_ID           = "ephemeral_id";
    public static final String          FIELD_LINKS                  = "links";
    public static final String          FIELD_DOWNLOAD               = "pkg_info_uri";
    public static final String          FIELD_REDIRECT               = "pkg_download_redirect";
    public static final String          FIELD_FREE_USE_IN_PROD       = "free_use_in_production";
    public static final String          FIELD_TCK_TESTED             = "tck_tested";
    public static final String          FIELD_TCK_CERT_URI           = "tck_cert_uri";
    public static final String          FIELD_AQAVIT_CERTIFIED       = "aqavit_certified";
    public static final String          FIELD_AQAVIT_CERT_URI        = "aqavit_cert_uri";
    public static final String          FIELD_VALIDATED_AT           = "validated_at";
    public static final String          FIELD_URL_VALID              = "url_valid";
    public static final String          FIELD_SIZE                   = "size";
    public static final String          FIELD_FEATURE                = "feature";
    private             Distribution    distribution;
    private             VersionNumber   versionNumber;
    private             VersionNumber   javaVersion;
    private             VersionNumber   distributionVersion;
    private             Semver          semver;
    private             MajorVersion    jdkVersion;
    private             Architecture    architecture;
    private             Bitness         bitness;
    private             FPU             fpu;
    private             OperatingSystem operatingSystem;
    private             LibCType        libCType;
    private             PackageType     packageType;
    private             ReleaseStatus   releaseStatus;
    private             ArchiveType     archiveType;
    private             TermOfSupport   termOfSupport;
    private             Boolean         javafxBundled;
    private             Boolean         latestBuildAvailable;
    private             Boolean         directlyDownloadable;
    private             Boolean         headless;
    private             String          filename;
    private             String          directDownloadUri;
    private             String          downloadSiteUri;
    private             String          signatureUri;
    private             String          checksumUri;
    private             String          checksum;
    private             HashAlgorithm   checksumType;
    private             Boolean         freeUseInProduction;
    private             Verification    tckTested;
    private             String          tckCertUri;
    private             Verification    aqavitCertified;
    private             String          aqavitCertUri;
    private             long            validatedAt;
    private             Boolean         urlValid;
    private             long            size;
    private             List<Feature>   features;


    public Pkg() {
        this(null, new VersionNumber(), new MajorVersion(1), Architecture.NONE, Bitness.NONE, FPU.UNKNOWN, OperatingSystem.NONE, PackageType.NONE, ReleaseStatus.NONE, ArchiveType.NONE, TermOfSupport.NONE, Boolean.FALSE, Boolean.TRUE, "", "", "", "", "", "", HashAlgorithm.NONE, Boolean.FALSE, Verification.UNKNOWN, "", Verification.UNKNOWN, "",
             Instant.now().getEpochSecond() - Constants.SECONDS_PER_MONTH, Boolean.TRUE, -1, new ArrayList<>());
    }
    public Pkg(final Distribution distribution, final VersionNumber versionNumber, final MajorVersion jdkVersion, final Architecture architecture, final Bitness bitness, final FPU fpu, final OperatingSystem operatingSystem, final PackageType packageType,
               final ReleaseStatus releaseStatus, final ArchiveType archiveType, final TermOfSupport termOfSupport, final boolean javafxBundled, final boolean directlyDownloadable, final String filename,
               final String directDownloadUri, final String downloadSiteUri, final String signatureUri, final String checksumUri, final String checksum, final HashAlgorithm checksumType, final Boolean freeUseInProduction,
               final Verification tckTested, final String tckCertUri, final Verification aqavitCertified, final String aqavitCertUri, final long validatedAt, final boolean urlValid, final long size, final List<Feature> features) {
        this.distribution         = distribution;
        this.versionNumber        = versionNumber;
        this.javaVersion          = new VersionNumber();
        this.distributionVersion  = new VersionNumber();
        this.jdkVersion           = jdkVersion;
        this.latestBuildAvailable = false;
        this.architecture         = architecture;
        this.bitness              = bitness;
        this.fpu                  = fpu;
        this.operatingSystem      = operatingSystem;
        this.libCType             = operatingSystem.getLibCType();
        this.packageType          = packageType;
        this.releaseStatus        = releaseStatus;
        this.archiveType          = archiveType;
        this.termOfSupport        = releaseStatus == ReleaseStatus.EA ? TermOfSupport.NONE : termOfSupport; // LTS cannot be early access version (?)
        this.javafxBundled        = javafxBundled;
        this.directlyDownloadable = directlyDownloadable;
        this.headless             = false;
        this.filename             = filename;
        this.directDownloadUri    = directDownloadUri;
        this.downloadSiteUri      = downloadSiteUri;
        this.signatureUri         = signatureUri;
        this.checksumUri          = checksumUri;
        this.checksum             = checksum;
        this.checksumType         = checksumType;
        this.freeUseInProduction  = freeUseInProduction;
        this.tckTested            = tckTested;
        this.tckCertUri           = tckCertUri;
        this.aqavitCertified      = aqavitCertified;
        this.aqavitCertUri        = aqavitCertUri;
        this.validatedAt          = validatedAt;
        this.urlValid             = urlValid;
        this.size                 = size;
        this.features             = features;
        this.semver               = Semver.fromText(versionNumber.toString()).getSemver1();
    }
    public Pkg(final String jsonText) {
        if (null == jsonText || jsonText.isEmpty()) { throw new IllegalArgumentException("Json text cannot be null or empty"); }
        final Gson       gson = new Gson();
        final JsonObject json = gson.fromJson(jsonText, JsonObject.class);

        final Distro distro       = Distro.fromText(json.get(FIELD_DISTRIBUTION).getAsString());
        this.distribution         = distro.get();
        this.versionNumber        = VersionNumber.fromText(json.get(FIELD_JAVA_VERSION).getAsString());
        this.javaVersion          = VersionNumber.fromText(json.get(FIELD_JAVA_VERSION).getAsString());
        this.distributionVersion  = VersionNumber.fromText(json.get(FIELD_DISTRIBUTION_VERSION).getAsString());
        this.jdkVersion           = new MajorVersion(json.has(FIELD_JDK_VERSION) ? json.get(FIELD_JDK_VERSION).getAsInt() : this.javaVersion.getFeature().getAsInt());
        this.latestBuildAvailable = json.has(FIELD_LATEST_BUILD_AVAILABLE) ? json.get(FIELD_LATEST_BUILD_AVAILABLE).getAsBoolean() : Boolean.FALSE;
        this.architecture         = Architecture.fromText(json.get(FIELD_ARCHITECTURE).getAsString());
        this.bitness              = this.architecture.getBitness();
        this.fpu                  = json.has(FIELD_FPU) ? FPU.fromText(json.get(FIELD_FPU).getAsString()) : FPU.UNKNOWN;
        this.operatingSystem      = OperatingSystem.fromText(json.get(FIELD_OPERATING_SYSTEM).getAsString());
        this.libCType             = LibCType.fromText(json.get(FIELD_LIB_C_TYPE).getAsString());
        this.packageType          = PackageType.fromText(json.get(FIELD_PACKAGE_TYPE).getAsString());
        this.releaseStatus        = ReleaseStatus.fromText(json.get(FIELD_RELEASE_STATUS).getAsString());
        this.archiveType          = ArchiveType.fromText(json.get(FIELD_ARCHIVE_TYPE).getAsString());
        this.termOfSupport        = TermOfSupport.fromText(json.get(FIELD_TERM_OF_SUPPORT).getAsString());
        this.javafxBundled        = json.get(FIELD_JAVAFX_BUNDLED).getAsBoolean();
        this.directlyDownloadable = json.has(FIELD_DIRECTLY_DOWNLOADABLE) ? json.get(FIELD_DIRECTLY_DOWNLOADABLE).getAsBoolean() : Boolean.TRUE;
        this.headless             = Boolean.FALSE;
        this.filename             = json.get(FIELD_FILENAME).getAsString();
        this.directDownloadUri    = json.get(FIELD_DIRECT_DOWNLOAD_URI).getAsString();
        this.downloadSiteUri      = json.get(FIELD_DOWNLOAD_SITE_URI).getAsString();
        this.signatureUri         = json.has(FIELD_SIGNATURE_URI) ? json.get(FIELD_SIGNATURE_URI).getAsString() : "";
        this.checksumUri          = json.has(FIELD_CHECKSUM_URI) ? json.get(FIELD_CHECKSUM_URI).getAsString() : "";
        this.checksum             = json.has(FIELD_CHECKSUM) ? json.get(FIELD_CHECKSUM).getAsString() : "";
        this.checksumType         = json.has(FIELD_CHECKSUM_TYPE) ? HashAlgorithm.fromText(json.get(FIELD_CHECKSUM_TYPE).getAsString()) : HashAlgorithm.NONE;
        this.semver               = Semver.fromText(json.get(FIELD_JAVA_VERSION).getAsString()).getSemver1();
        this.freeUseInProduction  = json.has(FIELD_FREE_USE_IN_PROD) ? json.get(FIELD_FREE_USE_IN_PROD).getAsBoolean() : Boolean.FALSE;
        this.tckTested            = json.has(FIELD_TCK_TESTED) ? Verification.fromText(json.get(FIELD_TCK_TESTED).getAsString()) : Verification.UNKNOWN;
        this.tckCertUri           = json.has(FIELD_TCK_CERT_URI) ? json.get(FIELD_TCK_CERT_URI).getAsString() : "";
        this.aqavitCertified      = json.has(FIELD_AQAVIT_CERTIFIED) ? Verification.fromText(json.get(FIELD_AQAVIT_CERTIFIED).getAsString()) : Verification.UNKNOWN;
        this.aqavitCertUri        = json.has(FIELD_AQAVIT_CERT_URI) ? json.get(FIELD_AQAVIT_CERT_URI).getAsString() : "";
        this.validatedAt          = json.has(FIELD_VALIDATED_AT) ? json.get(FIELD_VALIDATED_AT).getAsLong() : Instant.now().getEpochSecond() - Constants.SECONDS_PER_MONTH;
        this.urlValid             = json.has(FIELD_URL_VALID) ? json.get(FIELD_URL_VALID).getAsBoolean() : Boolean.TRUE;
        this.size                 = json.has(FIELD_SIZE) ? json.get(FIELD_SIZE).getAsLong() : -1;
        if (json.has(FIELD_FEATURE)) {
            features = new ArrayList<>();
            JsonArray featureArray = json.getAsJsonArray(FIELD_FEATURE);
            for (int i = 0 ; i < featureArray.size() ; i++) {
                if (featureArray.get(i).isJsonObject()) {
                    JsonObject featureObj = featureArray.get(i).getAsJsonObject();
                    Feature feat = Feature.fromText(featureObj.get("name").getAsString());
                    if (Feature.NOT_FOUND == feat || Feature.NONE == feat) {
                        continue;
                    }
                    features.add(feat);
                } else {
                    String  featureString = featureArray.get(i).getAsString();
                    Feature feat          = Feature.fromText(featureString);
                    if (Feature.NOT_FOUND == feat || Feature.NONE == feat) {
                        continue;
                    }
                    features.add(feat);
                }
            }
        } else {
            features = new ArrayList<>();
        }

        if (ArchiveType.NOT_FOUND     == this.archiveType)     { this.archiveType     = ArchiveType.getFromFileName(this.filename); }
        if (TermOfSupport.NOT_FOUND   == this.termOfSupport)   { this.termOfSupport   = Helper.getTermOfSupport(this.versionNumber, distro); }
        if (OperatingSystem.NOT_FOUND == this.operatingSystem) { this.operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                                                                         .stream()
                                                                                                                         .filter(entry -> this.filename.contains(entry.getKey()))
                                                                                                                         .findFirst()
                                                                                                                         .map(Entry::getValue)
                                                                                                                         .orElse(OperatingSystem.NONE); }
    }
    public Pkg(final Pkg pkg) {
        this.distribution         = pkg.getDistribution();
        this.versionNumber        = VersionNumber.fromText(pkg.getVersionNumber().toString(OutputFormat.FULL_COMPRESSED, true, true));
        this.javaVersion          = versionNumber;
        this.distributionVersion  = VersionNumber.fromText(pkg.getDistributionVersion().toString(OutputFormat.FULL_COMPRESSED, true, true));
        this.jdkVersion           = new MajorVersion(pkg.getJdkVersion().getAsInt());
        this.latestBuildAvailable = pkg.isLatestBuildAvailable();
        this.architecture         = Architecture.fromText(pkg.getArchitecture().getApiString());
        this.bitness              = architecture.getBitness();
        this.fpu                  = FPU.fromText(pkg.getFPU().getApiString());
        this.operatingSystem      = OperatingSystem.fromText(pkg.getOperatingSystem().getApiString());
        this.libCType             = operatingSystem.getLibCType();
        this.packageType          = PackageType.fromText(pkg.getPackageType().getApiString());
        this.releaseStatus        = ReleaseStatus.fromText(pkg.getReleaseStatus().getApiString());
        this.archiveType          = ArchiveType.fromText(pkg.getArchiveType().getApiString());
        this.termOfSupport        = TermOfSupport.fromText(pkg.getTermOfSupport().getApiString());
        this.javafxBundled        = pkg.isJavaFXBundled();
        this.directlyDownloadable = pkg.isDirectlyDownloadable();
        this.headless             = pkg.isHeadless();
        this.filename             = pkg.getFileName();
        this.directDownloadUri    = pkg.getDirectDownloadUri();
        this.downloadSiteUri      = pkg.getDownloadSiteUri();
        this.signatureUri         = pkg.getSignatureUri();
        this.checksumUri          = pkg.getChecksumUri();
        this.checksum             = pkg.getChecksum();
        this.checksumType         = pkg.getChecksumType();
        this.freeUseInProduction  = pkg.getFreeUseInProduction();
        this.tckTested            = Verification.fromText(pkg.getTckTested().getApiString());
        this.tckCertUri           = pkg.getTckCertUri();
        this.aqavitCertified      = Verification.fromText(pkg.getAqavitCertified().getApiString());
        this.aqavitCertUri        = pkg.getAqavitCertUri();
        this.validatedAt          = pkg.getValidatedAt();
        this.urlValid             = pkg.isUrlValid();
        this.size                 = pkg.getSize();
        pkg.getFeatures().forEach(feature -> this.features.add(Feature.fromText(feature.getApiString())));
        this.semver               = Semver.fromText(versionNumber.toString()).getSemver1();
    }


    public Distribution getDistribution() { return distribution; }
    public void setDistribution(final Distribution distribution) { this.distribution = distribution; }

    public String getDistributionName() { return this.distribution.getDistro().getName(); }

    public MajorVersion getMajorVersion() { return new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0); }

    public VersionNumber getVersionNumber() { return versionNumber; }
    public void setVersionNumber(final VersionNumber versionNumber) {
        this.versionNumber = versionNumber;
        this.semver        = Semver.fromText(versionNumber.toString()).getSemver1();
    }

    public VersionNumber getJavaVersion() { return javaVersion; }
    public void setJavaVersion(final VersionNumber javaVersion) { this.javaVersion = javaVersion; }

    public VersionNumber getDistributionVersion() { return distributionVersion; }
    public void setDistributionVersion(final VersionNumber distributionVersion) { this.distributionVersion = distributionVersion; }

    public Boolean isLatestBuildAvailable() { return null == latestBuildAvailable ? false : latestBuildAvailable; }
    public void setLatestBuildAvailable(final Boolean latestBuildAvailable) { this.latestBuildAvailable = latestBuildAvailable; }

    public Semver getSemver() { return semver; }

    public MajorVersion getJdkVersion() { return jdkVersion; }
    public void setJdkVersion(final MajorVersion jdkVersion) { this.jdkVersion = jdkVersion; }

    public OptionalInt getFeatureVersion() { return versionNumber.getFeature(); }

    public OptionalInt getInterimVersion() { return versionNumber.getInterim(); }

    public OptionalInt getUpdateVersion() { return versionNumber.getUpdate(); }

    public OptionalInt getPatchVersion() { return versionNumber.getPatch(); }

    public Architecture getArchitecture() { return architecture; }
    public void setArchitecture(final Architecture architecture) { this.architecture = architecture; }

    public Bitness getBitness() { return bitness; }
    public void setBitness(final Bitness bitness) { this.bitness = bitness; }

    public FPU getFPU() { return fpu; }
    public void setFPU(final FPU fpu) { this.fpu = fpu; }

    public OperatingSystem getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(final OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
        this.libCType        = operatingSystem.getLibCType();
    }

    public LibCType getLibCType() { return libCType; }
    public void setLibCType(final LibCType libCType) { this.libCType = libCType; }

    public PackageType getPackageType() { return packageType; }
    public void setPackageType(final PackageType packageType) { this.packageType = packageType; }

    public ReleaseStatus getReleaseStatus() { return releaseStatus; }
    public void setReleaseStatus(final ReleaseStatus releaseStatus) {
        this.releaseStatus = releaseStatus;
        this.versionNumber.setReleaseStatus(releaseStatus);
        this.semver        = Semver.fromText(versionNumber.toString()).getSemver1();
    }

    public ArchiveType getArchiveType() { return archiveType; }
    public void setArchiveType(final ArchiveType archiveType) { this.archiveType = archiveType; }

    public TermOfSupport getTermOfSupport() { return termOfSupport; }
    public void setTermOfSupport(final TermOfSupport termOfSupport) { this.termOfSupport = termOfSupport; }

    public Boolean isJavaFXBundled() { return javafxBundled; }
    public void setJavaFXBundled(final Boolean fx) { this.javafxBundled = fx; }

    public Boolean isDirectlyDownloadable() { return directlyDownloadable; }
    public void setDirectlyDownloadable(final Boolean directlyDownloadable) { this.directlyDownloadable = directlyDownloadable; }

    public boolean isHeadless() { return headless; }
    public void setHeadless(final boolean headless) { this.headless = headless; }

    public String getFileName() { return filename; }
    public void setFileName(final String filename) { this.filename = filename; }

    public String getDirectDownloadUri() { return directDownloadUri; }
    public void setDirectDownloadUri(final String directDownloadUri) { this.directDownloadUri = directDownloadUri; }

    public String getDownloadSiteUri() { return downloadSiteUri; }
    public void setDownloadSiteUri(final String downloadSiteUri) { this.downloadSiteUri = downloadSiteUri; }

    public String getSignatureUri() { return signatureUri; }
    public void setSignatureUri(final String signatureUri) { this.signatureUri = signatureUri; }

    public String getChecksumUri() { return checksumUri; }
    public void setChecksumUri(final String checksumUri) { this.checksumUri = checksumUri; }

    public String getChecksum() { return checksum; }
    public void setChecksum(final String checksum) { this.checksum = checksum; }

    public HashAlgorithm getChecksumType() { return checksumType; }
    public void setChecksumType(final HashAlgorithm checksumType) { this.checksumType = checksumType; }

    public Boolean getFreeUseInProduction() { return freeUseInProduction; }
    public void setFreeUseInProduction(final Boolean freeUseInProduction) { this.freeUseInProduction = null == freeUseInProduction ? Boolean.FALSE : freeUseInProduction; }

    public Verification getTckTested() { return tckTested; }
    public void setTckTested(final Verification verification) { this.tckTested = verification; }

    public String getTckCertUri() { return tckCertUri; }
    public void setTckCertUri(final String tckCertUri) { this.tckCertUri = tckCertUri; }

    public Verification getAqavitCertified() { return aqavitCertified; }
    public void setAqavitCertified(final Verification verification) { this.aqavitCertified = verification; }

    public String getAqavitCertUri() { return aqavitCertUri; }
    public void setAqavitCertUri(final String aqavitCertUri) { this.aqavitCertUri = aqavitCertUri; }

    public long getValidatedAt() { return validatedAt; }
    public void setValidatedAt(final long validatedAt) { this.validatedAt = validatedAt; }

    public Boolean isUrlValid() { return urlValid; }
    public void setUrlValid(final boolean urlValid) { this.urlValid = urlValid; }

    public long getSize() { return size; }
    public void setSize(final long size) { this.size = size; }

    public List<Feature> getFeatures() { return features; }
    public void setFeatures(final List<Feature> features) { this.features = features; }

    public String getId() {
        return directlyDownloadable ? Helper.getMD5(directDownloadUri.getBytes(StandardCharsets.UTF_8)) : Helper.getMD5(String.join("", directDownloadUri, filename).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns a json representation of the package depending on the given outputFormat
     * @param outputFormat The compressed versions do not contain the real download link but the current api url to track downloads
     * @return a json representation of the package depending on the given outputFormat
     */
    public final String toString(final OutputFormat outputFormat, final String API_VERSION) {
        switch(API_VERSION) {
            case API_VERSION_V1, API_VERSION_V2 -> {
                switch(outputFormat) {
                    case FULL:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? Boolean.FALSE : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DIRECT_DOWNLOAD_URI).append(QUOTES).append(COLON).append(QUOTES).append(directDownloadUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DOWNLOAD_SITE_URI).append(QUOTES).append(COLON).append(QUOTES).append(downloadSiteUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(signatureUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_CHECKSUM_URI).append(QUOTES).append(COLON).append(QUOTES).append(checksumUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_CHECKSUM).append(QUOTES).append(COLON).append(QUOTES).append(checksum).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_CHECKSUM_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(checksumType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_VALIDATED_AT).append(QUOTES).append(COLON).append(validatedAt).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_URL_VALID).append(QUOTES).append(COLON).append(urlValid).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_EPHEMERAL_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(FIELD_DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(FIELD_REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(INDENT).append(CURLY_BRACKET_CLOSE).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_ENRICHED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FEATURE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_INTERIM_VERSION).append(QUOTES).append(COLON).append(versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_UPDATE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_PATCH_VERSION).append(QUOTES).append(COLON).append(versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_BUILD_VERSION).append(QUOTES).append(COLON).append(versionNumber.getBuild().isPresent() ? versionNumber.getBuild().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_EPHEMERAL_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(FIELD_DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(FIELD_REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(INDENT).append(CURLY_BRACKET_CLOSE).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case FULL_COMPRESSED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECT_DOWNLOAD_URI).append(QUOTES).append(COLON).append(QUOTES).append(directDownloadUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DOWNLOAD_SITE_URI).append(QUOTES).append(COLON).append(QUOTES).append(downloadSiteUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(signatureUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_CHECKSUM_URI).append(QUOTES).append(COLON).append(QUOTES).append(checksumUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_CHECKSUM).append(QUOTES).append(COLON).append(QUOTES).append(checksum).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_CHECKSUM_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(checksumType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_VALIDATED_AT).append(QUOTES).append(COLON).append(validatedAt).append(COMMA)
                                                  .append(QUOTES).append(FIELD_URL_VALID).append(QUOTES).append(COLON).append(urlValid).append(COMMA)
                                                  .append(QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_ENRICHED_COMPRESSED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FEATURE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_INTERIM_VERSION).append(QUOTES).append(COLON).append(versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_UPDATE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PATCH_VERSION).append(QUOTES).append(COLON).append(versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_BUILD_VERSION).append(QUOTES).append(COLON).append(versionNumber.getBuild().isPresent() ? versionNumber.getBuild().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_EPHEMERAL_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(QUOTES).append(FIELD_DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_COMPRESSED:
                    case MINIMIZED:
                    default:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_EPHEMERAL_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(QUOTES).append(FIELD_DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                }
            }
            default -> {
                switch(outputFormat) {
                    case FULL:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? Boolean.FALSE : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DIRECT_DOWNLOAD_URI).append(QUOTES).append(COLON).append(QUOTES).append(directDownloadUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DOWNLOAD_SITE_URI).append(QUOTES).append(COLON).append(QUOTES).append(downloadSiteUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(signatureUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_CHECKSUM_URI).append(QUOTES).append(COLON).append(QUOTES).append(checksumUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_CHECKSUM).append(QUOTES).append(COLON).append(QUOTES).append(checksum).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_CHECKSUM_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(checksumType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_VALIDATED_AT).append(QUOTES).append(COLON).append(validatedAt).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_URL_VALID).append(QUOTES).append(COLON).append(urlValid).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(FIELD_DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(FIELD_REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(INDENT).append(CURLY_BRACKET_CLOSE).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_ENRICHED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FEATURE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_INTERIM_VERSION).append(QUOTES).append(COLON).append(versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_UPDATE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_PATCH_VERSION).append(QUOTES).append(COLON).append(versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_BUILD_VERSION).append(QUOTES).append(COLON).append(versionNumber.getBuild().isPresent() ? versionNumber.getBuild().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(FIELD_DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(FIELD_REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(INDENT).append(CURLY_BRACKET_CLOSE).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case FULL_COMPRESSED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECT_DOWNLOAD_URI).append(QUOTES).append(COLON).append(QUOTES).append(directDownloadUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DOWNLOAD_SITE_URI).append(QUOTES).append(COLON).append(QUOTES).append(downloadSiteUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(signatureUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_CHECKSUM_URI).append(QUOTES).append(COLON).append(QUOTES).append(checksumUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_CHECKSUM).append(QUOTES).append(COLON).append(QUOTES).append(checksum).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_CHECKSUM_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(checksumType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_VALIDATED_AT).append(QUOTES).append(COLON).append(validatedAt).append(COMMA)
                                                  .append(QUOTES).append(FIELD_URL_VALID).append(QUOTES).append(COLON).append(urlValid).append(COMMA)
                                                  .append(QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_ENRICHED_COMPRESSED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FEATURE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_INTERIM_VERSION).append(QUOTES).append(COLON).append(versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_UPDATE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PATCH_VERSION).append(QUOTES).append(COLON).append(versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_BUILD_VERSION).append(QUOTES).append(COLON).append(versionNumber.getBuild().isPresent() ? versionNumber.getBuild().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(QUOTES).append(FIELD_DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case MINIMIZED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_COMPRESSED:
                    default:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(FIELD_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toStringInclBuild(true)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(FIELD_DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(QUOTES).append(FIELD_DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(API_VERSION).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(FIELD_SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(FIELD_FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                }
            }
        }
    }

    public boolean isNewerThan(final Pkg pkg) {
        return (equalsExceptUpdate(pkg) && getSemver().compareTo(pkg.getSemver()) >= 0);
    }

    public boolean equalsExceptUpdate(final Pkg pkg) {
        if (this.equals(pkg)) { return false; }
        if (null == pkg) { return false; }
        return distribution.equals(pkg.distribution) &&
               getFeatureVersion().getAsInt() == pkg.getFeatureVersion().getAsInt() &&
               getInterimVersion().getAsInt() == pkg.getInterimVersion().getAsInt() &&
               architecture                   == pkg.getArchitecture() &&
               operatingSystem                == pkg.getOperatingSystem() &&
               libCType                       == pkg.getLibCType() &&
               packageType                    == pkg.getPackageType() &&
               releaseStatus                  == pkg.getReleaseStatus() &&
               archiveType                    == pkg.getArchiveType() &&
               termOfSupport                  == pkg.getTermOfSupport() &&
               javafxBundled                  == pkg.isJavaFXBundled() &&
               directlyDownloadable           == pkg.isDirectlyDownloadable() &&
               !getId().equals(pkg.getId());
    }

    public boolean equalsExceptJavaFXAndPackageType(final Pkg pkg) {
        if (this.equals(pkg)) { return false; }
        if (null == pkg) { return false; }
        return distribution.equals(pkg.distribution) &&
               getJavaVersion().compareTo(pkg.getJavaVersion()) == 0 &&
               architecture                   == pkg.getArchitecture() &&
               operatingSystem                == pkg.getOperatingSystem() &&
               libCType                       == pkg.getLibCType() &&
               packageType                    == pkg.getPackageType() &&
               releaseStatus                  == pkg.getReleaseStatus() &&
               termOfSupport                  == pkg.getTermOfSupport() &&
               directlyDownloadable           == pkg.isDirectlyDownloadable() &&
               javafxBundled                  != pkg.isJavaFXBundled() &&
               !getId().equals(pkg.getId());
    }

    @Override public boolean equals(final Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;
        Pkg pkg = (Pkg) o;
        return distribution.equals(pkg.distribution) &&
               versionNumber.equals(pkg.versionNumber) &&
               architecture         == pkg.architecture &&
               operatingSystem      == pkg.operatingSystem &&
               libCType             == pkg.getLibCType() &&
               packageType          == pkg.packageType &&
               releaseStatus        == pkg.releaseStatus &&
               archiveType          == pkg.archiveType &&
               termOfSupport        == pkg.termOfSupport &&
               javafxBundled        == pkg.javafxBundled &&
               directlyDownloadable == pkg.directlyDownloadable &&
               filename.equals(pkg.filename) &&
               directDownloadUri.equals(pkg.directDownloadUri) &&
               getId().equals(pkg.getId());
    }

    @Override public int hashCode() {
        return Objects.hash(distribution, versionNumber, architecture, bitness, operatingSystem, packageType, releaseStatus, archiveType, termOfSupport, javafxBundled, directlyDownloadable, filename, directDownloadUri);
    }

    @Override public String toString() {
        return toString(OutputFormat.REDUCED_COMPRESSED, API_VERSION_V2);
    }
}
