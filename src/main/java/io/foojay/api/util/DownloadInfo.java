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


public class DownloadInfo {
    public static final String FIELD_TIMESTAMP        = "timestamp";
    public static final String FIELD_TOKEN            = "token";
    public static final String FIELD_PACKAGE_ID       = "pkg_id";
    public static final String FIELD_DISTRIBUTION     = "distribution";
    public static final String FIELD_PACKAGE_TYPE     = "package_type";
    public static final String FIELD_RELEASE_STATUS   = "release_status";
    public static final String FIELD_JAVA_VERSION     = "java_version";
    public static final String FIELD_OPERATING_SYSTEM = "operating_system";
    public static final String FIELD_ARCHITECTURE     = "architecture";
    public static final String FIELD_COUNTRY_CODE2    = "country_code2";
    public static final String FIELD_CITY             = "city";

    private long   timestamp;
    private String token;
    private String pkgId;
    private String distribution;
    private String packageType;
    private String releaseStatus;
    private String javaVersion;
    private String operatingSystem;
    private String architecture;
    private String countryCode2;
    private String city;


    public DownloadInfo() {
        this(-1, "", "", "", "", "", "", "", "", "", "");
    }
    public DownloadInfo(final long timestamp, final String pkgId, final String token, final String distribution, final String packageType, final String releaseStatus,
                        final String javaVersion, final String operatingSystem, final String architecture, final String countryCode2, final String city) {
        this.timestamp       = timestamp;
        this.pkgId           = pkgId;
        this.token           = null == token ? "" : token;
        this.distribution    = distribution;
        this.packageType     = packageType;
        this.releaseStatus   = releaseStatus;
        this.javaVersion     = javaVersion;
        this.operatingSystem = operatingSystem;
        this.architecture    = architecture;
        this.countryCode2    = countryCode2;
        this.city            = city;
    }


    public long getTimestamp() { return timestamp; }
    public void setTimestamp(final long timestamp) { this.timestamp = timestamp; }

    public String getPkgId() { return pkgId; }
    public void setPkgId(final String pkgId) { this.pkgId = pkgId; }

    public String getToken() { return token; }
    public void setToken(final String token) { this.token = null == token ? "" : token;}

    public String getDistribution() { return distribution; }
    public void setDistribution(final String distribution) { this.distribution = distribution; }

    public String getPackageType() { return packageType; }
    public void setPackageType(final String packageType) { this.packageType = packageType; }

    public String getReleaseStatus() { return releaseStatus; }
    public void setReleaseStatus(final String releaseStatus) { this.releaseStatus = releaseStatus; }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(final String javaVersion) { this.javaVersion = javaVersion; }

    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(final String operatingSystem) { this.operatingSystem = operatingSystem; }

    public String getArchitecture() { return architecture; }
    public void setArchitecture(final String architecture) { this.architecture = architecture; }

    public String getCountryCode2() { return countryCode2; }
    public void setCountryCode2(final String countryCode2) { this.countryCode2 = countryCode2; }

    public String getCity() { return city; }
    public void setCity(final String city) { this.city = city; }


    @Override public String toString() {
        return new StringBuilder().append("{")
                                  .append("\"").append(FIELD_TIMESTAMP).append("\"").append(":").append(timestamp).append(",")
                                  .append("\"").append(FIELD_PACKAGE_ID).append("\"").append(":").append("\"").append(pkgId).append("\"").append(",")
                                  .append("\"").append(FIELD_TOKEN).append("\"").append(":").append("\"").append(token).append("\"").append(",")
                                  .append("\"").append(FIELD_DISTRIBUTION).append("\"").append(":").append("\"").append(distribution).append("\"").append(",")
                                  .append("\"").append(FIELD_PACKAGE_TYPE).append("\"").append(":").append("\"").append(packageType).append("\"").append(",")
                                  .append("\"").append(FIELD_RELEASE_STATUS).append("\"").append(":").append("\"").append(releaseStatus).append("\"").append(",")
                                  .append("\"").append(FIELD_JAVA_VERSION).append("\"").append(":").append("\"").append(javaVersion).append("\"").append(",")
                                  .append("\"").append(FIELD_OPERATING_SYSTEM).append("\"").append(":").append("\"").append(operatingSystem).append("\"").append(",")
                                  .append("\"").append(FIELD_ARCHITECTURE).append("\"").append(":").append("\"").append(architecture).append("\"").append(",")
                                  .append("\"").append(FIELD_COUNTRY_CODE2).append("\"").append(":").append("\"").append(countryCode2).append("\"").append(",")
                                  .append("\"").append(FIELD_CITY).append("\"").append(":").append("\"").append(city).append("\"").append(",")
                                  .append("}")
                                  .toString();
    }
}
