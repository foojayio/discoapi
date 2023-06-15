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
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
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
    private             Set<Feature>    features;


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
        this.features             = new HashSet<>(features);
        this.semver               = versionNumber.getFeature().isPresent() ? Semver.fromText(versionNumber.toString()).getSemver1() : new Semver(versionNumber);
    }
    public Pkg(final String jsonText) {
        if (null == jsonText || jsonText.isEmpty()) { throw new IllegalArgumentException("Json text cannot be null or empty"); }
        final Gson       gson = new Gson();
        final JsonObject json = gson.fromJson(jsonText, JsonObject.class);

        final Distro distro       = Distro.fromText(json.get(PkgField.DISTRIBUTION.fieldName()).getAsString());
        this.distribution         = distro.get();
        this.versionNumber        = VersionNumber.fromText(json.get(PkgField.JAVA_VERSION.fieldName()).getAsString());
        this.javaVersion          = VersionNumber.fromText(json.get(PkgField.JAVA_VERSION.fieldName()).getAsString());
        this.distributionVersion  = VersionNumber.fromText(json.get(PkgField.DISTRIBUTION_VERSION.fieldName()).getAsString());
        this.jdkVersion           = new MajorVersion(json.has(PkgField.JDK_VERSION.fieldName()) ? json.get(PkgField.JDK_VERSION.fieldName()).getAsInt() : this.javaVersion.getFeature().getAsInt());
        this.latestBuildAvailable = json.has(PkgField.LATEST_BUILD_AVAILABLE.fieldName()) ? json.get(PkgField.LATEST_BUILD_AVAILABLE.fieldName()).getAsBoolean() : Boolean.FALSE;
        this.architecture         = Architecture.fromText(json.get(PkgField.ARCHITECTURE.fieldName()).getAsString());
        this.bitness              = this.architecture.getBitness();
        this.fpu                  = json.has(PkgField.FPU.fieldName()) ? FPU.fromText(json.get(PkgField.FPU.fieldName()).getAsString()) : FPU.UNKNOWN;
        this.operatingSystem      = OperatingSystem.fromText(json.get(PkgField.OPERATING_SYSTEM.fieldName()).getAsString());
        this.libCType             = LibCType.fromText(json.get(PkgField.LIB_C_TYPE.fieldName()).getAsString());
        this.packageType          = PackageType.fromText(json.get(PkgField.PACKAGE_TYPE.fieldName()).getAsString());
        this.releaseStatus        = ReleaseStatus.fromText(json.get(PkgField.RELEASE_STATUS.fieldName()).getAsString());
        this.termOfSupport        = TermOfSupport.fromText(json.get(PkgField.TERM_OF_SUPPORT.fieldName()).getAsString());
        this.javafxBundled        = json.get(PkgField.JAVAFX_BUNDLED.fieldName()).getAsBoolean();
        this.directlyDownloadable = json.has(PkgField.DIRECTLY_DOWNLOADABLE.fieldName()) ? json.get(PkgField.DIRECTLY_DOWNLOADABLE.fieldName()).getAsBoolean() : Boolean.TRUE;
        this.headless             = Boolean.FALSE;
        this.filename             = json.get(PkgField.FILENAME.fieldName()).getAsString();
        this.archiveType          = json.get(PkgField.ARCHIVE_TYPE.fieldName()).getAsString().isEmpty() ? Helper.fetchArchiveType(this.filename) : ArchiveType.fromText(json.get(PkgField.ARCHIVE_TYPE.fieldName()).getAsString());
        this.directDownloadUri    = json.get(PkgField.DIRECT_DOWNLOAD_URI.fieldName()).getAsString();
        this.downloadSiteUri      = json.get(PkgField.DOWNLOAD_SITE_URI.fieldName()).getAsString();
        this.signatureUri         = json.has(PkgField.SIGNATURE_URI.fieldName()) ? json.get(PkgField.SIGNATURE_URI.fieldName()).getAsString() : "";
        this.checksumUri          = json.has(PkgField.CHECKSUM_URI.fieldName()) ? json.get(PkgField.CHECKSUM_URI.fieldName()).getAsString() : "";
        this.checksum             = json.has(PkgField.CHECKSUM.fieldName()) ? json.get(PkgField.CHECKSUM.fieldName()).getAsString() : "";
        this.checksumType         = json.has(PkgField.CHECKSUM_TYPE.fieldName()) ? HashAlgorithm.fromText(json.get(PkgField.CHECKSUM_TYPE.fieldName()).getAsString()) : HashAlgorithm.NONE;
        this.semver               = Semver.fromText(json.get(PkgField.JAVA_VERSION.fieldName()).getAsString()).getSemver1();
        this.freeUseInProduction  = json.has(PkgField.FREE_USE_IN_PROD.fieldName()) ? json.get(PkgField.FREE_USE_IN_PROD.fieldName()).getAsBoolean() : Boolean.FALSE;
        this.tckTested            = json.has(PkgField.TCK_TESTED.fieldName()) ? Verification.fromText(json.get(PkgField.TCK_TESTED.fieldName()).getAsString()) : Verification.UNKNOWN;
        this.tckCertUri           = json.has(PkgField.TCK_CERT_URI.fieldName()) ? json.get(PkgField.TCK_CERT_URI.fieldName()).getAsString() : "";
        this.aqavitCertified      = json.has(PkgField.AQAVIT_CERTIFIED.fieldName()) ? Verification.fromText(json.get(PkgField.AQAVIT_CERTIFIED.fieldName()).getAsString()) : Verification.UNKNOWN;
        this.aqavitCertUri        = json.has(PkgField.AQAVIT_CERT_URI.fieldName()) ? json.get(PkgField.AQAVIT_CERT_URI.fieldName()).getAsString() : "";
        this.validatedAt          = json.has(PkgField.VALIDATED_AT.fieldName()) ? json.get(PkgField.VALIDATED_AT.fieldName()).getAsLong() : Instant.now().getEpochSecond() - Constants.SECONDS_PER_MONTH;
        this.urlValid             = json.has(PkgField.URL_VALID.fieldName()) ? json.get(PkgField.URL_VALID.fieldName()).getAsBoolean() : Boolean.TRUE;
        this.size                 = json.has(PkgField.SIZE.fieldName()) ? json.get(PkgField.SIZE.fieldName()).getAsLong() : -1;
        if (json.has(PkgField.FEATURE.fieldName())) {
            features = new HashSet<>();
            JsonArray featureArray = json.getAsJsonArray(PkgField.FEATURE.fieldName());
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
            features = new HashSet<>();
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
        this.filename             = pkg.getFilename();
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
        this.semver               = versionNumber.getFeature().isPresent() ? Semver.fromText(versionNumber.toString()).getSemver1() : new Semver(versionNumber);
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

    public Boolean isLatestBuildAvailable() { return null == latestBuildAvailable ? Boolean.FALSE : latestBuildAvailable; }
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

    public String getFilename() { return filename; }
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

    public Set<Feature> getFeatures() { return features; }
    public void setFeatures(final List<Feature> features) { setFeatures(new HashSet<>(features)); }
    public void setFeatures(final Set<Feature> features) { this.features = features; }

    public String getId() {
        return directlyDownloadable ? Helper.getMD5(directDownloadUri.getBytes(StandardCharsets.UTF_8)) : Helper.getMD5((directDownloadUri + filename).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns a json representation of the package depending on the given outputFormat
     * @param outputFormat The compressed versions do not contain the real download link but the current api url to track downloads
     * @return a json representation of the package depending on the given outputFormat
     */
    public final String toString(final OutputFormat outputFormat, final String apiVersion) {
        switch(apiVersion) {
            case API_VERSION_V1, API_VERSION_V2 -> {
                switch(outputFormat) {
                    case FULL:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? Boolean.FALSE : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DIRECT_DOWNLOAD_URI).append(QUOTES).append(COLON).append(QUOTES).append(directDownloadUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DOWNLOAD_SITE_URI).append(QUOTES).append(COLON).append(QUOTES).append(downloadSiteUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(signatureUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.CHECKSUM_URI).append(QUOTES).append(COLON).append(QUOTES).append(checksumUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.CHECKSUM).append(QUOTES).append(COLON).append(QUOTES).append(checksum).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.CHECKSUM_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(checksumType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.VALIDATED_AT).append(QUOTES).append(COLON).append(validatedAt).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.URL_VALID).append(QUOTES).append(COLON).append(urlValid).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.EPHEMERAL_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(PkgField.DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(PkgField.REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(INDENT).append(CURLY_BRACKET_CLOSE).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_ENRICHED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FEATURE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.INTERIM_VERSION).append(QUOTES).append(COLON).append(versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.UPDATE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.PATCH_VERSION).append(QUOTES).append(COLON).append(versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.BUILD_VERSION).append(QUOTES).append(COLON).append(versionNumber.getBuild().isPresent() ? versionNumber.getBuild().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.EPHEMERAL_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(PkgField.DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(PkgField.REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(INDENT).append(CURLY_BRACKET_CLOSE).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case FULL_COMPRESSED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECT_DOWNLOAD_URI).append(QUOTES).append(COLON).append(QUOTES).append(directDownloadUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DOWNLOAD_SITE_URI).append(QUOTES).append(COLON).append(QUOTES).append(downloadSiteUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(signatureUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.CHECKSUM_URI).append(QUOTES).append(COLON).append(QUOTES).append(checksumUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.CHECKSUM).append(QUOTES).append(COLON).append(QUOTES).append(checksum).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.CHECKSUM_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(checksumType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.VALIDATED_AT).append(QUOTES).append(COLON).append(validatedAt).append(COMMA)
                                                  .append(QUOTES).append(PkgField.URL_VALID).append(QUOTES).append(COLON).append(urlValid).append(COMMA)
                                                  .append(QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_ENRICHED_COMPRESSED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FEATURE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.INTERIM_VERSION).append(QUOTES).append(COLON).append(versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.UPDATE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PATCH_VERSION).append(QUOTES).append(COLON).append(versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.BUILD_VERSION).append(QUOTES).append(COLON).append(versionNumber.getBuild().isPresent() ? versionNumber.getBuild().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.EPHEMERAL_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(QUOTES).append(PkgField.DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_COMPRESSED:
                    case MINIMIZED:
                    default:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.EPHEMERAL_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_EPHEMERAL_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                }
            }
            default -> {
                switch(outputFormat) {
                    case FULL:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? Boolean.FALSE : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DIRECT_DOWNLOAD_URI).append(QUOTES).append(COLON).append(QUOTES).append(directDownloadUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DOWNLOAD_SITE_URI).append(QUOTES).append(COLON).append(QUOTES).append(downloadSiteUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(signatureUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.CHECKSUM_URI).append(QUOTES).append(COLON).append(QUOTES).append(checksumUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.CHECKSUM).append(QUOTES).append(COLON).append(QUOTES).append(checksum).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.CHECKSUM_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(checksumType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.VALIDATED_AT).append(QUOTES).append(COLON).append(validatedAt).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.URL_VALID).append(QUOTES).append(COLON).append(urlValid).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(PkgField.DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(PkgField.REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(INDENT).append(CURLY_BRACKET_CLOSE).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_ENRICHED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FEATURE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.INTERIM_VERSION).append(QUOTES).append(COLON).append(versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.UPDATE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.PATCH_VERSION).append(QUOTES).append(COLON).append(versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.BUILD_VERSION).append(QUOTES).append(COLON).append(versionNumber.getBuild().isPresent() ? versionNumber.getBuild().getAsInt() : 0).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(PkgField.DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append(QUOTES).append(COMMA_NEW_LINE)
                                                  .append(INDENT).append(INDENT).append(QUOTES).append(PkgField.REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(INDENT).append(CURLY_BRACKET_CLOSE).append(COMMA_NEW_LINE)
                                                  .append(INDENTED_QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE))).append(NEW_LINE)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case FULL_COMPRESSED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECT_DOWNLOAD_URI).append(QUOTES).append(COLON).append(QUOTES).append(directDownloadUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DOWNLOAD_SITE_URI).append(QUOTES).append(COLON).append(QUOTES).append(downloadSiteUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(signatureUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.CHECKSUM_URI).append(QUOTES).append(COLON).append(QUOTES).append(checksumUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.CHECKSUM).append(QUOTES).append(COLON).append(QUOTES).append(checksum).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.CHECKSUM_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(checksumType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.VALIDATED_AT).append(QUOTES).append(COLON).append(validatedAt).append(COMMA)
                                                  .append(QUOTES).append(PkgField.URL_VALID).append(QUOTES).append(COLON).append(urlValid).append(COMMA)
                                                  .append(QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_ENRICHED_COMPRESSED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FEATURE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.INTERIM_VERSION).append(QUOTES).append(COLON).append(versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.UPDATE_VERSION).append(QUOTES).append(COLON).append(versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PATCH_VERSION).append(QUOTES).append(COLON).append(versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.BUILD_VERSION).append(QUOTES).append(COLON).append(versionNumber.getBuild().isPresent() ? versionNumber.getBuild().getAsInt() : 0).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case MINIMIZED:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                    case REDUCED_COMPRESSED:
                    default:
                        return new StringBuilder().append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHIVE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(archiveType.getUiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getDistro().getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.MAJOR_VERSION).append(QUOTES).append(COLON).append(versionNumber.getFeature().getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVA_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(semver).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DISTRIBUTION_VERSION).append(QUOTES).append(COLON).append(QUOTES).append(distributionVersion.toString(OutputFormat.REDUCED_COMPRESSED, false, false)).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JDK_VERSION).append(QUOTES).append(COLON).append(jdkVersion.getAsInt()).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LATEST_BUILD_AVAILABLE).append(QUOTES).append(COLON).append(null == latestBuildAvailable ? false : latestBuildAvailable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(releaseStatus.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.OPERATING_SYSTEM).append(QUOTES).append(COLON).append(QUOTES).append(operatingSystem.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LIB_C_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(libCType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.ARCHITECTURE).append(QUOTES).append(COLON).append(QUOTES).append(architecture.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FPU).append(QUOTES).append(COLON).append(QUOTES).append(fpu.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.PACKAGE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(packageType.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.JAVAFX_BUNDLED).append(QUOTES).append(COLON).append(javafxBundled).append(COMMA)
                                                  .append(QUOTES).append(PkgField.DIRECTLY_DOWNLOADABLE).append(QUOTES).append(COLON).append(directlyDownloadable).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FILENAME).append(QUOTES).append(COLON).append(QUOTES).append(filename).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.LINKS).append(QUOTES).append(COLON).append(CURLY_BRACKET_OPEN)
                                                  .append(QUOTES).append(PkgField.DOWNLOAD).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.REDIRECT).append(QUOTES).append(COLON).append(QUOTES).append(BASE_URL).append(SLASH).append("v").append(apiVersion).append("/").append(ENDPOINT_IDS).append("/").append(getId()).append("/redirect").append(QUOTES)
                                                  .append(CURLY_BRACKET_CLOSE).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FREE_USE_IN_PROD).append(QUOTES).append(COLON).append(freeUseInProduction).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_TESTED).append(QUOTES).append(COLON).append(QUOTES).append(tckTested.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.TCK_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(tckCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERTIFIED).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertified.getApiString()).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.AQAVIT_CERT_URI).append(QUOTES).append(COLON).append(QUOTES).append(aqavitCertUri).append(QUOTES).append(COMMA)
                                                  .append(QUOTES).append(PkgField.SIZE).append(QUOTES).append(COLON).append(size).append(COMMA)
                                                  .append(QUOTES).append(PkgField.FEATURE).append(QUOTES).append(COLON).append(features.stream().map(feature -> feature.toString()).collect(Collectors.joining(COMMA, SQUARE_BRACKET_OPEN, SQUARE_BRACKET_CLOSE)))
                                                  .append(CURLY_BRACKET_CLOSE)
                                                  .toString().replaceAll("\\\\", "");
                }
            }
        }
    }

    public boolean isNewerThan(final Pkg pkg) {
        return (equalsExceptUpdate(pkg) && getSemver().compareTo(pkg.getSemver()) >= 0);
    }

    public List<PkgField> diff(final Pkg pkg) {
        List<PkgField> diff = new ArrayList<>();
        if (!pkg.getId().equals(getId()))                                       { diff.add(PkgField.ID); }
        if (pkg.getDistribution().getDistro() != getDistribution().getDistro()) { diff.add(PkgField.DISTRIBUTION); }
        if (pkg.getOperatingSystem()          != getOperatingSystem())          { diff.add(PkgField.OPERATING_SYSTEM); }
        if (pkg.getArchitecture()             != getArchitecture())             { diff.add(PkgField.ARCHITECTURE); }
        if (pkg.getBitness()                  != getBitness())                  { diff.add(PkgField.BITNESS); }
        if (pkg.getLibCType()                 != getLibCType())                 { diff.add(PkgField.LIB_C_TYPE); }
        if (pkg.getArchiveType()              != getArchiveType())              { diff.add(PkgField.ARCHIVE_TYPE); }
        if (pkg.getPackageType()              != getPackageType())              { diff.add(PkgField.PACKAGE_TYPE); }
        if (!pkg.getFilename().equals(getFilename()))                           { diff.add(PkgField.FILENAME); }
        if (!pkg.getChecksum().equals(getChecksum()))                           { diff.add(PkgField.CHECKSUM); }
        if (pkg.getChecksumType()             != getChecksumType())             { diff.add(PkgField.CHECKSUM_TYPE); }
        if (!pkg.getChecksumUri().equals(getChecksumUri()))                     { diff.add(PkgField.CHECKSUM_URI); }
        if (pkg.getAqavitCertified()          != getAqavitCertified())          { diff.add(PkgField.AQAVIT_CERTIFIED); }
        if (!pkg.getAqavitCertUri().equals(getAqavitCertUri()))                 { diff.add(PkgField.AQAVIT_CERT_URI); }
        if (pkg.getTckTested()                != getTckTested())                { diff.add(PkgField.TCK_TESTED); }
        if (!pkg.getTckCertUri().equals(getTckCertUri()))                       { diff.add(PkgField.TCK_CERT_URI); }
        if (!pkg.getDirectDownloadUri().equals(getDirectDownloadUri()))         { diff.add(PkgField.DIRECT_DOWNLOAD_URI); }
        if (pkg.isDirectlyDownloadable()      != isDirectlyDownloadable())      { diff.add(PkgField.DIRECTLY_DOWNLOADABLE); }
        if (pkg.isJavaFXBundled()             != isJavaFXBundled())             { diff.add(PkgField.JAVAFX_BUNDLED); }
        if (pkg.isHeadless()                  != isHeadless())                  { diff.add(PkgField.HEADLESS); }
        if (!pkg.getDistributionVersion().equals(getDistributionVersion()))     { diff.add(PkgField.DISTRIBUTION_VERSION); }
        if (!pkg.getVersionNumber().equals(getVersionNumber()))                 { diff.add(PkgField.JAVA_VERSION); }
        if (pkg.getFeatureVersion().isPresent() && getFeatureVersion().isPresent()) {
            if (pkg.getFeatureVersion().getAsInt() != getFeatureVersion().getAsInt()) { diff.add(PkgField.FEATURE_VERSION); }
        }
        if (pkg.getInterimVersion().isPresent() && getInterimVersion().isPresent()) {
            if (pkg.getInterimVersion().getAsInt() != getInterimVersion().getAsInt()) { diff.add(PkgField.INTERIM_VERSION); }
        }
        if (pkg.getUpdateVersion().isPresent() && getUpdateVersion().isPresent()) {
            if (pkg.getUpdateVersion().getAsInt() != getUpdateVersion().getAsInt()) { diff.add(PkgField.UPDATE_VERSION); }
        }
        if (pkg.getPatchVersion().isPresent() && getPatchVersion().isPresent()) {
            if (pkg.getPatchVersion().getAsInt() != getPatchVersion().getAsInt()) { diff.add(PkgField.PATCH_VERSION); }
        }
        if (pkg.getJdkVersion().getAsInt()    != getJdkVersion().getAsInt())    { diff.add(PkgField.JDK_VERSION); }
        if (pkg.getMajorVersion().getAsInt()  != getMajorVersion().getAsInt())  { diff.add(PkgField.MAJOR_VERSION); }
        if (pkg.getTermOfSupport()            != getTermOfSupport())            { diff.add(PkgField.TERM_OF_SUPPORT); }
        if (pkg.getFPU()                      != getFPU())                      { diff.add(PkgField.FPU); }
        if (pkg.getFeatures().size()          != getFeatures().size())          { diff.add(PkgField.FEATURE); }
        if (pkg.getFreeUseInProduction()      != getFreeUseInProduction())      { diff.add(PkgField.FREE_USE_IN_PROD); }
        if (!pkg.getSignatureUri().equals(getSignatureUri()))                   { diff.add(PkgField.SIGNATURE_URI); }
        if (pkg.getSize()                     != getSize())                     { diff.add(PkgField.SIZE); }
        if (!pkg.getSemver().equals(getSemver()))                               { diff.add(PkgField.SEMVER); }
        if (pkg.getReleaseStatus()            != getReleaseStatus())            { diff.add(PkgField.RELEASE_STATUS); }
        if (!pkg.getDownloadSiteUri().equals(getDownloadSiteUri()))             { diff.add(PkgField.DOWNLOAD_SITE_URI); }

        return diff;
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
