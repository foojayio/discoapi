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

package io.foojay.api.distribution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import static io.foojay.api.pkg.ArchiveType.MSI;
import static io.foojay.api.pkg.ArchiveType.PKG;
import static io.foojay.api.pkg.ArchiveType.TAR_GZ;
import static io.foojay.api.pkg.ArchiveType.ZIP;
import static io.foojay.api.pkg.ArchiveType.getFromFileName;
import static io.foojay.api.pkg.Bitness.BIT_32;
import static io.foojay.api.pkg.Bitness.BIT_64;
import static io.foojay.api.pkg.OperatingSystem.AIX;
import static io.foojay.api.pkg.OperatingSystem.LINUX;
import static io.foojay.api.pkg.OperatingSystem.MACOS;
import static io.foojay.api.pkg.OperatingSystem.SOLARIS;
import static io.foojay.api.pkg.OperatingSystem.WINDOWS;
import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.PackageType.JRE;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;
import static io.foojay.api.pkg.TermOfSupport.LTS;
import static io.foojay.api.pkg.TermOfSupport.MTS;
import static io.foojay.api.pkg.TermOfSupport.STS;


public class AOJ_OPENJ9 implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(AOJ_OPENJ9.class);

    private static final String                PACKAGE_URL          = "https://api.adoptopenjdk.net/v3/assets/feature_releases/";
    private static final List<Architecture>    ARCHITECTURES        = List.of(AARCH64, ARM, MIPS, PPC64, PPC64LE, SPARCV9, X86, X64);
    private static final List<OperatingSystem> OPERATING_SYSTEMS    = List.of(AIX, LINUX, MACOS, SOLARIS, WINDOWS);
    private static final List<ArchiveType>     ARCHIVE_TYPES        = List.of(PKG, MSI, TAR_GZ, ZIP);
    private static final List<PackageType>     PACKAGE_TYPES        = List.of(JDK, JRE);
    private static final List<ReleaseStatus>   RELEASE_STATUSES     = List.of(EA, GA);
    private static final List<TermOfSupport>   TERMS_OF_SUPPORT     = List.of(STS, LTS);
    private static final List<Bitness>         BITNESSES            = List.of(BIT_32, BIT_64);
    private static final Boolean               BUNDLED_WITH_JAVA_FX = false;

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
                                                      ARCHITECTURE_MAP       = Map.of(AARCH64, "aarch64", ARM, "arm", MIPS, "mips", PPC64, "ppc64", PPC64LE, "ppc64le", SPARCV9, "sparcv9", X64, "x64", X86, "x32");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP   = Map.of(LINUX, "linux", MACOS, "mac", WINDOWS, "windows", SOLARIS, "solaris", AIX, "aix");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP       = Map.of(JDK, "jdk", JRE, "jre");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP     = Map.of(EA, "ea", GA, "ga");

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


    @Override public Distro getDistro() { return Distro.AOJ_OPENJ9; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return SUPPORT_TERM_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }


    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.AOJ_OPENJ9.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append(PACKAGE_URL);
        queryBuilder.append(versionNumber.getFeature().getAsInt()).append("/");

        if (null == RELEASE_STATUS_MAP.get(releaseStatus)) {
            queryBuilder.append(RELEASE_STATUS_MAP.get(ReleaseStatus.GA));
        } else {
            queryBuilder.append(RELEASE_STATUS_MAP.get(releaseStatus));
        }

        int initialSize = queryBuilder.length();

        if (architecture != Architecture.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(ARCHITECTURE_PARAM).append("=").append(ARCHITECTURE_MAP.get(architecture));
        }

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("heap_size=").append("normal");

        if (packageType == PackageType.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(PACKAGE_TYPE_PARAM).append("=").append(PACKAGE_TYPE_MAP.get(JDK));
            queryBuilder.append("&");
            queryBuilder.append(PACKAGE_TYPE_PARAM).append("=").append(PACKAGE_TYPE_MAP.get(JRE));
        } else {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(PACKAGE_TYPE_PARAM).append("=").append(PACKAGE_TYPE_MAP.get(packageType));
        }

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("jvm_impl=").append("openj9");

        if (operatingSystem != OperatingSystem.NONE) {
            queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
            queryBuilder.append(OPERATING_SYSTEM_PARAM).append("=").append(OPERATING_SYSTEM_MAP.get(operatingSystem));
        }

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("page=").append("0");

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("page_size=").append("10");

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("project=").append("jdk");
        /*
        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("sort_method=").append("DEFAULT");

        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("sort_order=").append("DESC");
        */
        queryBuilder.append(queryBuilder.length() == initialSize ? "?" : "&");
        queryBuilder.append("vendor=").append("adoptopenjdk");

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();

        if (null != javafxBundled && javafxBundled) { return pkgs; }

        TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);
        supTerm = MTS == supTerm ? STS : supTerm;

        JsonArray binaries = jsonObj.get(FIELD_BINARIES).getAsJsonArray();
        for (int i = 0 ; i < binaries.size() ; i++) {
            JsonObject    binariesObj    = binaries.get(i).getAsJsonObject();
            JsonObject    versionDataObj = jsonObj.get(FIELD_VERSION_DATA).getAsJsonObject();
            VersionNumber vNumber        = new VersionNumber(versionDataObj.get(FIELD_MAJOR).getAsInt(), versionDataObj.get(FIELD_MINOR).getAsInt(), versionDataObj.get(FIELD_SECURITY).getAsInt(), versionDataObj.has(FIELD_PATCH) ? versionDataObj.get(FIELD_PATCH).getAsInt() : null);
            if (latest) {
                if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { return pkgs; }
            } /*else {
                if (!versionNumber.equals(vNumber)) { return pkgs; }
            }
            */
            VersionNumber dNumber = VersionNumber.fromText(versionDataObj.get(FIELD_SEMVER).getAsString());

            Architecture arc = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                            .stream()
                                                            .filter(entry -> entry.getKey().equals(binariesObj.get(FIELD_ARCHITECTURE).getAsString()))
                                                            .findFirst()
                                                            .map(Entry::getValue)
                                                            .orElse(Architecture.NONE);
            if (Architecture.NONE == arc) {
                LOGGER.debug("Architecture not found in AOJ for field value: {}", binariesObj.get(FIELD_ARCHITECTURE).getAsString());
                continue;
            }

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                  .stream()
                                                                  .filter(entry -> entry.getKey().equals(binariesObj.get(FIELD_OS).getAsString()))
                                                                  .findFirst()
                                                                  .map(Entry::getValue)
                                                                  .orElse(OperatingSystem.NONE);

            if (OperatingSystem.NONE == os) {
                LOGGER.debug("Operating System not found in AOJ for field value: {}", binariesObj.get(FIELD_OS).getAsString());
                continue;
            }

            JsonElement installerElement = binariesObj.get(FIELD_INSTALLER);
            if (null != installerElement) {
                JsonObject installerObj      = installerElement.getAsJsonObject();
                String installerName         = installerObj.get(FIELD_NAME).getAsString();
                String installerDownloadLink = installerObj.get(FIELD_LINK).getAsString();

                String withoutPrefix = installerName.replace("OpenJDK" + vNumber.getFeature().getAsInt() + "U", "");

                String[] nameParts = withoutPrefix.split("_");

                Pkg installerPkg = new Pkg();
                installerPkg.setDistribution(Distro.AOJ_OPENJ9.get());
                installerPkg.setVersionNumber(vNumber);
                installerPkg.setJavaVersion(vNumber);
                installerPkg.setDistributionVersion(vNumber);
                installerPkg.setTermOfSupport(supTerm);

                switch (packageType) {
                    case JDK:
                        if (withoutPrefix.contains(Constants.JRE_POSTFIX)) { continue; }
                        installerPkg.setPackageType(JDK);
                        break;
                    case JRE:
                        if (!withoutPrefix.contains(Constants.JRE_POSTFIX)) { continue; }
                        installerPkg.setPackageType(JRE);
                        break;
                    case NONE:
                        installerPkg.setPackageType(withoutPrefix.contains(Constants.JRE_POSTFIX) ? JRE : JDK);
                        break;
                }

                installerPkg.setArchitecture(arc);
                installerPkg.setBitness(arc.getBitness());

                installerPkg.setOperatingSystem(os);

                installerPkg.setReleaseStatus(ReleaseStatus.NONE == releaseStatus ? GA : releaseStatus);

                Helper.setTermOfSupport(versionNumber, installerPkg);

                ArchiveType ext = getFromFileName(installerName);
                if(ArchiveType.NONE == archiveType || ext == archiveType) {
                    installerPkg.setArchiveType(ext);
                    installerPkg.setFileName(installerName);
                    installerPkg.setDirectDownloadUri(installerDownloadLink);

                    pkgs.add(installerPkg);
                }
            }

            JsonElement packageElement = binariesObj.get(FIELD_PACKAGE);
            if (null != packageElement) {
                JsonObject packageObj      = packageElement.getAsJsonObject();
                String packageName         = packageObj.get(FIELD_NAME).getAsString();
                String packageDownloadLink = packageObj.get(FIELD_LINK).getAsString();

                String withoutPrefix = packageName.replace("OpenJDK" + vNumber.getFeature().getAsInt() + "U", "");

                Pkg packagePkg = new Pkg();
                packagePkg.setDistribution(Distro.AOJ_OPENJ9.get());
                packagePkg.setVersionNumber(vNumber);
                packagePkg.setJavaVersion(vNumber);
                packagePkg.setDistributionVersion(dNumber);
                packagePkg.setTermOfSupport(supTerm);

                switch (packageType) {
                    case JDK:
                        if (withoutPrefix.contains(Constants.JRE_POSTFIX)) { continue; }
                        packagePkg.setPackageType(JDK);
                        break;
                    case JRE:
                        if (!withoutPrefix.contains(Constants.JRE_POSTFIX)) { continue; }
                        packagePkg.setPackageType(JRE);
                        break;
                    case NONE:
                        packagePkg.setPackageType(withoutPrefix.contains(Constants.JRE_POSTFIX) ? JRE : JDK);
                        break;
                }

                packagePkg.setArchitecture(arc);
                packagePkg.setBitness(arc.getBitness());

                packagePkg.setOperatingSystem(os);

                packagePkg.setReleaseStatus(ReleaseStatus.NONE == releaseStatus ? GA : releaseStatus);

                ArchiveType ext = getFromFileName(packageName);
                if(ArchiveType.NONE == archiveType || ext == archiveType) {
                    packagePkg.setArchiveType(ext);
                    packagePkg.setFileName(packageName);
                    packagePkg.setDirectDownloadUri(packageDownloadLink);

                    pkgs.add(packagePkg);
                }
            }
        }

        return pkgs;
    }
}
