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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.Architecture.AARCH64;
import static io.foojay.api.pkg.Architecture.ARM;
import static io.foojay.api.pkg.Architecture.MIPS;
import static io.foojay.api.pkg.Architecture.PPC64;
import static io.foojay.api.pkg.Architecture.PPC64LE;
import static io.foojay.api.pkg.Architecture.SPARCV9;
import static io.foojay.api.pkg.Architecture.X64;
import static io.foojay.api.pkg.Architecture.X86;
import static io.foojay.api.pkg.OperatingSystem.AIX;
import static io.foojay.api.pkg.OperatingSystem.LINUX;
import static io.foojay.api.pkg.OperatingSystem.MACOS;
import static io.foojay.api.pkg.OperatingSystem.SOLARIS;
import static io.foojay.api.pkg.OperatingSystem.WINDOWS;
import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.PackageType.JRE;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;


public class Temurin implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(Temurin.class);

    private static final String                       PACKAGE_URL            = "https://projects.eclipse.org/projects/adoptium/downloads";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM     = "architecture";
    private static final String                       OPERATING_SYSTEM_PARAM = "os";
    private static final String                       ARCHIVE_TYPE_PARAM     = "";
    private static final String                       PACKAGE_TYPE_PARAM     = "image_type";
    private static final String                       RELEASE_STATUS_PARAM   = "release_type";
    private static final String                       SUPPORT_TERM_PARAM     = "";
    private static final String                       BITNESS_PARAM          = "";

    // Mappings for url parameters
    private static final Map<Architecture, String>
                                                      ARCHITECTURE_MAP     = Map.of(AARCH64, "aarch64", ARM, "arm", MIPS, "mips", PPC64, "ppc64", PPC64LE, "ppc64le", SPARCV9, "sparcv9", X64, "x64", X86, "x32");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP = Map.of(LINUX, "linux", MACOS, "mac", WINDOWS, "windows", SOLARIS, "solaris", AIX, "aix");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP     = Map.of(JDK, "jdk", JRE, "jre");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP   = Map.of(EA, "ea", GA, "ga");

    // JSON fields
    private static final String                       FIELD_BINARIES         = "binaries";
    private static final String                       FIELD_INSTALLER        = "installer";
    private static final String                       FIELD_PACKAGE          = "package";
    private static final String                       FIELD_LINK             = "link";
    private static final String                       FIELD_NAME             = "name";
    private static final String                       FIELD_VERSION_DATA     = "version_data";
    private static final String                       FIELD_MAJOR            = "major";
    private static final String                       FIELD_MINOR            = "minor";
    private static final String                       FIELD_SECURITY         = "security";
    private static final String                       FIELD_PATCH            = "patch";
    private static final String                       FIELD_SEMVER           = "semver";
    private static final String                       FIELD_RELEASE_TYPE     = "release_type";
    private static final String                       FIELD_RELEASE_NAME     = "release_name";
    private static final String                       FIELD_ARCHITECTURE     = "architecture";
    private static final String                       FIELD_JVM_IMPL         = "jvm_impl";
    private static final String                       FIELD_OS               = "os";
    private static final String                       FIELD_CHECKSUM         = "checksum";

    private static final HashAlgorithm HASH_ALGORITHM      = HashAlgorithm.NONE;
    private static final String        HASH_URI            = "";
    private static final SignatureType SIGNATURE_TYPE      = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI       = "";


    @Override public Distro getDistro() { return Distro.TEMURIN; }

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


    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.TEMURIN.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append(PACKAGE_URL);

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();
        return pkgs;
    }
}
