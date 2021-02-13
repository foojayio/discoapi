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

package io.foojay.api.pkg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import io.foojay.api.util.OutputFormat;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.OptionalInt;


public class Pkg {
    public  static final String            FIELD_ID                     = "id";
    public  static final String            FIELD_DISTRIBUTION           = "distribution";
    public  static final String            FIELD_MAJOR_VERSION          = "major_version";
    public  static final String            FIELD_JAVA_VERSION           = "java_version";
    public  static final String            FIELD_DISTRIBUTION_VERSION   = "distribution_version";
    public  static final String            FIELD_LATEST_BUILD_AVAILABLE = "latest_build_available";
    public  static final String            FIELD_ARCHITECTURE           = "architecture";
    public  static final String            FIELD_BITNESS                = "bitness";
    public  static final String            FIELD_OPERATING_SYSTEM       = "operating_system";
    public  static final String            FIELD_LIB_C_TYPE             = "lib_c_type";
    public  static final String            FIELD_PACKAGE_TYPE           = "package_type";
    public  static final String            FIELD_RELEASE_STATUS         = "release_status";
    public  static final String            FIELD_ARCHIVE_TYPE           = "archive_type";
    public  static final String            FIELD_TERM_OF_SUPPORT        = "term_of_support";
    public  static final String            FIELD_JAVAFX_BUNDLED         = "javafx_bundled";
    public  static final String            FIELD_DIRECTLY_DOWNLOADABLE  = "directly_downloadable";
    public  static final String            FIELD_FILENAME               = "filename";
    public  static final String            FIELD_DIRECT_DOWNLOAD_URI    = "direct_download_uri";
    public  static final String            FIELD_DOWNLOAD_SITE_URI      = "download_site_uri";
    public  static final String            FIELD_EPHEMERAL_ID           = "ephemeral_id";
    private              Distribution      distribution;
    private              VersionNumber     versionNumber;
    private              VersionNumber     javaVersion;
    private              VersionNumber     distributionVersion;
    private              SemVer            semver;
    private              Architecture      architecture;
    private              Bitness           bitness;
    private              OperatingSystem   operatingSystem;
    private              LibCType          libCType;
    private              PackageType       packageType;
    private              ReleaseStatus     releaseStatus;
    private              ArchiveType       archiveType;
    private              TermOfSupport     termOfSupport;
    private              Boolean           javafxBundled;
    private              Boolean           latestBuildAvailable;
    private              Boolean           directlyDownloadable;
    private              boolean           headless;
    private              String            filename;
    private              String            directDownloadUri;
    private              String            downloadSiteUri;


    public Pkg() {
        this(null, new VersionNumber(), Architecture.NONE, Bitness.NONE, OperatingSystem.NONE, PackageType.NONE, ReleaseStatus.NONE, ArchiveType.NONE, TermOfSupport.NONE, false, true, "", "", "");
    }
    public Pkg(final Distribution distribution, final VersionNumber versionNumber, final Architecture architecture, final Bitness bitness, final OperatingSystem operatingSystem, final PackageType packageType,
               final ReleaseStatus releaseStatus, final ArchiveType archiveType, final TermOfSupport termOfSupport, final boolean javafxBundled, final boolean directlyDownloadable, final String filename, final String directDownloadUri, final String downloadSiteUri) {
        this.distribution         = distribution;
        this.versionNumber        = versionNumber;
        this.javaVersion          = new VersionNumber();
        this.distributionVersion  = new VersionNumber();
        this.latestBuildAvailable = false;
        this.architecture         = architecture;
        this.bitness              = bitness;
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
        this.semver               = SemVer.fromText(versionNumber.toString()).getSemVer1();
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
        this.latestBuildAvailable = json.has(FIELD_LATEST_BUILD_AVAILABLE) ? json.get(FIELD_LATEST_BUILD_AVAILABLE).getAsBoolean() : Boolean.valueOf(false);
        this.architecture         = Architecture.fromText(json.get(FIELD_ARCHITECTURE).getAsString());
        this.bitness              = this.architecture.getBitness();
        this.operatingSystem      = OperatingSystem.fromText(json.get(FIELD_OPERATING_SYSTEM).getAsString());
        this.libCType             = LibCType.fromText(json.get(FIELD_LIB_C_TYPE).getAsString());
        this.packageType          = PackageType.fromText(json.get(FIELD_PACKAGE_TYPE).getAsString());
        this.releaseStatus        = ReleaseStatus.fromText(json.get(FIELD_RELEASE_STATUS).getAsString());
        this.archiveType          = ArchiveType.fromText(json.get(FIELD_ARCHIVE_TYPE).getAsString());
        this.termOfSupport        = TermOfSupport.fromText(json.get(FIELD_TERM_OF_SUPPORT).getAsString());
        this.javafxBundled        = json.get(FIELD_JAVAFX_BUNDLED).getAsBoolean();
        this.directlyDownloadable = json.has(FIELD_DIRECTLY_DOWNLOADABLE) ? json.get(FIELD_DIRECTLY_DOWNLOADABLE).getAsBoolean() : true;
        this.headless             = false;
        this.filename             = json.get(FIELD_FILENAME).getAsString();
        this.directDownloadUri    = json.get(FIELD_DIRECT_DOWNLOAD_URI).getAsString();
        this.downloadSiteUri      = json.get(FIELD_DOWNLOAD_SITE_URI).getAsString();
        this.semver               = SemVer.fromText(versionNumber.toString()).getSemVer1();

        if (ArchiveType.NOT_FOUND     == this.archiveType)     { this.archiveType     = ArchiveType.getFromFileName(this.filename); }
        if (TermOfSupport.NOT_FOUND   == this.termOfSupport)   { this.termOfSupport   = Helper.getTermOfSupport(this.versionNumber, distro); }
        if (OperatingSystem.NOT_FOUND == this.operatingSystem) { this.operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                                                                         .stream()
                                                                                                                         .filter(entry -> this.filename.contains(entry.getKey()))
                                                                                                                         .findFirst()
                                                                                                                         .map(Entry::getValue)
                                                                                                                         .orElse(OperatingSystem.NONE); }
    }


    public Distribution getDistribution() { return distribution; }
    public void setDistribution(final Distribution distribution) { this.distribution = distribution; }

    public String getDistributionName() { return this.distribution.getDistro().getName(); }

    public MajorVersion getMajorVersion() { return versionNumber.getMajorVersion(); }

    public VersionNumber getVersionNumber() { return versionNumber; }
    public void setVersionNumber(final VersionNumber versionNumber) {
        this.versionNumber = versionNumber;
        this.semver        = SemVer.fromText(versionNumber.toString()).getSemVer1();
    }

    public VersionNumber getJavaVersion() { return javaVersion; }
    public void setJavaVersion(final VersionNumber javaVersion) { this.javaVersion = javaVersion; }

    public VersionNumber getDistributionVersion() { return distributionVersion; }
    public void setDistributionVersion(final VersionNumber distributionVersion) { this.distributionVersion = distributionVersion; }

    public Boolean isLatestBuildAvailable() { return null == latestBuildAvailable ? false : latestBuildAvailable; }
    public void setLatestBuildAvailable(final Boolean latestBuildAvailable) { this.latestBuildAvailable = latestBuildAvailable; }

    public SemVer getSemver() { return semver; }

    public OptionalInt getFeatureVersion() { return versionNumber.getFeature(); }

    public OptionalInt getInterimVersion() { return versionNumber.getInterim(); }

    public OptionalInt getUpdateVersion() { return versionNumber.getUpdate(); }

    public OptionalInt getPatchVersion() { return versionNumber.getPatch(); }

    public Architecture getArchitecture() { return architecture; }
    public void setArchitecture(final Architecture architecture) { this.architecture = architecture; }

    public Bitness getBitness() { return bitness; }
    public void setBitness(final Bitness bitness) { this.bitness = bitness; }

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
        this.semver        = SemVer.fromText(versionNumber.toString()).getSemVer1();
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

    public String getId() {
        return directlyDownloadable ? Helper.getMD5Hex(directDownloadUri.getBytes()) : Helper.getMD5Hex(String.join("", directDownloadUri, filename).getBytes());
    }

    /**
     * Returns a json representation of the package depending on the given outputFormat
     * @param outputFormat The compressed versions do not contain the real download link but the current api url to track downloads
     * @return a json representation of the package depending on the given outputFormat
     */
    public final String toString(final OutputFormat outputFormat) {
        switch(outputFormat) {
            case FULL:
                return new StringBuilder().append("{\n")
                                          .append("  \"").append(FIELD_ID).append("\"").append(":").append("\"").append(getId()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_ARCHIVE_TYPE).append("\"").append(":").append("\"").append(archiveType.getUiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_DISTRIBUTION).append("\"").append(":").append("\"").append(distribution.getDistro().getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_MAJOR_VERSION).append("\"").append(":").append(versionNumber.getFeature().getAsInt()).append(",\n")
                                          .append("  \"").append(FIELD_JAVA_VERSION).append("\"").append(":").append("\"").append(semver).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_DISTRIBUTION_VERSION).append("\"").append(":").append("\"").append(distributionVersion.toStringInclBuild(true)).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_LATEST_BUILD_AVAILABLE).append("\"").append(":").append(null == latestBuildAvailable ? false : latestBuildAvailable).append(",\n")
                                          .append("  \"").append(FIELD_RELEASE_STATUS).append("\"").append(":").append("\"").append(releaseStatus.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_TERM_OF_SUPPORT).append("\"").append(":").append("\"").append(termOfSupport.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_OPERATING_SYSTEM).append("\"").append(":").append("\"").append(operatingSystem.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_LIB_C_TYPE).append("\"").append(":").append("\"").append(libCType.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_ARCHITECTURE).append("\"").append(":").append("\"").append(architecture.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_PACKAGE_TYPE).append("\"").append(":").append("\"").append(packageType.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_JAVAFX_BUNDLED).append("\"").append(":").append(javafxBundled).append(",\n")
                                          .append("  \"").append(FIELD_DIRECTLY_DOWNLOADABLE).append("\"").append(":").append(directlyDownloadable).append(",\n")
                                          .append("  \"").append(FIELD_FILENAME).append("\"").append(":").append("\"").append(filename).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_DIRECT_DOWNLOAD_URI).append("\"").append(":").append("\"").append(directDownloadUri).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_DOWNLOAD_SITE_URI).append("\"").append(":").append("\"").append(downloadSiteUri).append("\"").append("\n")
                                          .append("}")
                                          .toString();
            case REDUCED:
                return new StringBuilder().append("{\n")
                                          .append("  \"").append(FIELD_ID).append("\"").append(":").append("\"").append(getId()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_ARCHIVE_TYPE).append("\"").append(":").append("\"").append(archiveType.getUiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_DISTRIBUTION).append("\"").append(":").append("\"").append(distribution.getDistro().getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_MAJOR_VERSION).append("\"").append(":").append(versionNumber.getFeature().getAsInt()).append(",\n")
                                          .append("  \"").append(FIELD_JAVA_VERSION).append("\"").append(":").append("\"").append(semver).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_DISTRIBUTION_VERSION).append("\"").append(":").append("\"").append(distributionVersion.toStringInclBuild(true)).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_LATEST_BUILD_AVAILABLE).append("\"").append(":").append(null == latestBuildAvailable ? false : latestBuildAvailable).append(",\n")
                                          .append("  \"").append(FIELD_RELEASE_STATUS).append("\"").append(":").append("\"").append(releaseStatus.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_TERM_OF_SUPPORT).append("\"").append(":").append("\"").append(termOfSupport.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_OPERATING_SYSTEM).append("\"").append(":").append("\"").append(operatingSystem.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_LIB_C_TYPE).append("\"").append(":").append("\"").append(libCType.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_ARCHITECTURE).append("\"").append(":").append("\"").append(architecture.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_PACKAGE_TYPE).append("\"").append(":").append("\"").append(packageType.getApiString()).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_JAVAFX_BUNDLED).append("\"").append(":").append(javafxBundled).append(",\n")
                                          .append("  \"").append(FIELD_DIRECTLY_DOWNLOADABLE).append("\"").append(":").append(directlyDownloadable).append(",\n")
                                          .append("  \"").append(FIELD_FILENAME).append("\"").append(":").append("\"").append(filename).append("\"").append(",\n")
                                          .append("  \"").append(FIELD_EPHEMERAL_ID).append("\"").append(":").append("\"").append(CacheManager.INSTANCE.getEphemeralIdForPkg(getId())).append("\"").append("\n")
                                          .append("}")
                                          .toString();
            case FULL_COMPRESSED:
                return new StringBuilder().append("{")
                                          .append("\"").append(FIELD_ID).append("\"").append(":").append("\"").append(getId()).append("\"").append(",")
                                          .append("\"").append(FIELD_ARCHIVE_TYPE).append("\"").append(":").append("\"").append(archiveType.getUiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_DISTRIBUTION).append("\"").append(":").append("\"").append(distribution.getDistro().getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_MAJOR_VERSION).append("\"").append(":").append(versionNumber.getFeature().getAsInt()).append(",")
                                          .append("\"").append(FIELD_JAVA_VERSION).append("\"").append(":").append("\"").append(semver).append("\"").append(",")
                                          .append("\"").append(FIELD_DISTRIBUTION_VERSION).append("\"").append(":").append("\"").append(distributionVersion.toStringInclBuild(true)).append("\"").append(",")
                                          .append("\"").append(FIELD_LATEST_BUILD_AVAILABLE).append("\"").append(":").append(null == latestBuildAvailable ? false : latestBuildAvailable).append(",")
                                          .append("\"").append(FIELD_RELEASE_STATUS).append("\"").append(":").append("\"").append(releaseStatus.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_TERM_OF_SUPPORT).append("\"").append(":").append("\"").append(termOfSupport.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_OPERATING_SYSTEM).append("\"").append(":").append("\"").append(operatingSystem.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_LIB_C_TYPE).append("\"").append(":").append("\"").append(libCType.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_ARCHITECTURE).append("\"").append(":").append("\"").append(architecture.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_PACKAGE_TYPE).append("\"").append(":").append("\"").append(packageType.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_JAVAFX_BUNDLED).append("\"").append(":").append(javafxBundled).append(",")
                                          .append("\"").append(FIELD_DIRECTLY_DOWNLOADABLE).append("\"").append(":").append(directlyDownloadable).append(",")
                                          .append("\"").append(FIELD_FILENAME).append("\"").append(":").append("\"").append(filename).append("\"").append(",")
                                          .append("\"").append(FIELD_DIRECT_DOWNLOAD_URI).append("\"").append(":").append("\"").append(directDownloadUri).append("\"").append(",")
                                          .append("\"").append(FIELD_DOWNLOAD_SITE_URI).append("\"").append(":").append("\"").append(downloadSiteUri).append("\"")
                                          .append("}")
                                          .toString();
            case REDUCED_COMPRESSED:
            default:
                return new StringBuilder().append("{")
                                          .append("\"").append(FIELD_ID).append("\"").append(":").append("\"").append(getId()).append("\"").append(",")
                                          .append("\"").append(FIELD_ARCHIVE_TYPE).append("\"").append(":").append("\"").append(archiveType.getUiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_DISTRIBUTION).append("\"").append(":").append("\"").append(distribution.getDistro().getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_MAJOR_VERSION).append("\"").append(":").append(versionNumber.getFeature().getAsInt()).append(",")
                                          .append("\"").append(FIELD_JAVA_VERSION).append("\"").append(":").append("\"").append(semver).append("\"").append(",")
                                          .append("\"").append(FIELD_DISTRIBUTION_VERSION).append("\"").append(":").append("\"").append(distributionVersion.toStringInclBuild(true)).append("\"").append(",")
                                          .append("\"").append(FIELD_LATEST_BUILD_AVAILABLE).append("\"").append(":").append(null == latestBuildAvailable ? false : latestBuildAvailable).append(",")
                                          .append("\"").append(FIELD_RELEASE_STATUS).append("\"").append(":").append("\"").append(releaseStatus.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_TERM_OF_SUPPORT).append("\"").append(":").append("\"").append(termOfSupport.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_OPERATING_SYSTEM).append("\"").append(":").append("\"").append(operatingSystem.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_LIB_C_TYPE).append("\"").append(":").append("\"").append(libCType.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_ARCHITECTURE).append("\"").append(":").append("\"").append(architecture.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_PACKAGE_TYPE).append("\"").append(":").append("\"").append(packageType.getApiString()).append("\"").append(",")
                                          .append("\"").append(FIELD_JAVAFX_BUNDLED).append("\"").append(":").append(javafxBundled).append(",")
                                          .append("\"").append(FIELD_DIRECTLY_DOWNLOADABLE).append("\"").append(":").append(directlyDownloadable).append(",")
                                          .append("\"").append(FIELD_FILENAME).append("\"").append(":").append("\"").append(filename).append("\"").append(",")
                                          .append("\"").append(FIELD_EPHEMERAL_ID).append("\"").append(":").append("\"").append(CacheManager.INSTANCE.getEphemeralIdForPkg(getId())).append("\"")
                                          .append("}")
                                          .toString();
        }
    }

    @Override public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pkg pkg = (Pkg) o;
        return javafxBundled == pkg.javafxBundled && directlyDownloadable == pkg.directlyDownloadable && distribution.equals(pkg.distribution) && versionNumber.equals(pkg.versionNumber) &&
               architecture == pkg.architecture && bitness == pkg.bitness && operatingSystem == pkg.operatingSystem && packageType == pkg.packageType &&
               releaseStatus == pkg.releaseStatus && archiveType == pkg.archiveType && termOfSupport == pkg.termOfSupport && filename.equals(pkg.filename) &&
               directDownloadUri.equals(pkg.directDownloadUri);
    }

    @Override public int hashCode() {
        return Objects.hash(distribution, versionNumber, architecture, bitness, operatingSystem, packageType, releaseStatus, archiveType, termOfSupport, javafxBundled, directlyDownloadable, filename,
                            directDownloadUri);
    }

    @Override public String toString() {
        return toString(OutputFormat.REDUCED_COMPRESSED);
    }
}
