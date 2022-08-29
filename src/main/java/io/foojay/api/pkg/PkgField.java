/*
 * Copyright (c) 2022.
 *
 * This file is part of DiscoAPI.
 *
 *     DiscoAPI is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     DiscoAPI is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY), without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.pkg;


public enum PkgField {
    ID("id"),
    DISTRIBUTION("distribution"),
    MAJOR_VERSION("major_version"),
    JAVA_VERSION("java_version"),
    SEMVER("semver"),
    FEATURE_VERSION("feature_version"),
    INTERIM_VERSION("interim_version"),
    UPDATE_VERSION("update_version"),
    PATCH_VERSION("patch_version"),
    BUILD_VERSION("build_version"),
    DISTRIBUTION_VERSION("distribution_version"),
    JDK_VERSION("jdk_version"),
    LATEST_BUILD_AVAILABLE("latest_build_available"),
    ARCHITECTURE("architecture"),
    BITNESS("bitness"),
    FPU("fpu"),
    OPERATING_SYSTEM("operating_system"),
    LIB_C_TYPE("lib_c_type"),
    PACKAGE_TYPE("package_type"),
    RELEASE_STATUS("release_status"),
    ARCHIVE_TYPE("archive_type"),
    TERM_OF_SUPPORT("term_of_support"),
    JAVAFX_BUNDLED("javafx_bundled"),
    DIRECTLY_DOWNLOADABLE("directly_downloadable"),
    FILENAME("filename"),
    DIRECT_DOWNLOAD_URI("direct_download_uri"),
    DOWNLOAD_SITE_URI("download_site_uri"),
    SIGNATURE_URI("signature_uri"),
    CHECKSUM_URI("checksum_uri"),
    CHECKSUM("checksum"),
    CHECKSUM_TYPE("checksum_type"),
    EPHEMERAL_ID("ephemeral_id"),
    LINKS("links"),
    DOWNLOAD("pkg_info_uri"),
    REDIRECT("pkg_download_redirect"),
    FREE_USE_IN_PROD("free_use_in_production"),
    TCK_TESTED("tck_tested"),
    TCK_CERT_URI("tck_cert_uri"),
    AQAVIT_CERTIFIED("aqavit_certified"),
    AQAVIT_CERT_URI("aqavit_cert_uri"),
    VALIDATED_AT("validated_at"),
    URL_VALID("url_valid"),
    SIZE("size"),
    HEADLESS("headless"),
    FEATURE("feature");

    private final String fieldName;


    PkgField(final String fieldName) {
        this.fieldName = fieldName;
    }


    public final String fieldName() { return fieldName; }

    @Override public String toString() { return fieldName; }
}
