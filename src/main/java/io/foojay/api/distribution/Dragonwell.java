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
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.BasicScope;
import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import io.foojay.api.util.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.Architecture.*;
import static io.foojay.api.pkg.Bitness.*;
import static io.foojay.api.pkg.PackageType.*;
import static io.foojay.api.pkg.ArchiveType.*;
import static io.foojay.api.pkg.OperatingSystem.*;
import static io.foojay.api.pkg.ReleaseStatus.*;


public class Dragonwell implements Distribution {
    private static final Logger                       LOGGER = LoggerFactory.getLogger(Dragonwell.class);

    private static final String                       GITHUB_USER            = "alibaba";
    private static final String                       PACKAGE_URL            = "https://api.github.com/repos/" + GITHUB_USER + "/dragonwell";
    private static final List<Architecture>           ARCHITECTURES          = List.of(X64);
    private static final List<OperatingSystem>        OPERATING_SYSTEMS      = List.of(LINUX, WINDOWS);
    private static final List<ArchiveType>            ARCHIVE_TYPES          = List.of(TAR_GZ);
    private static final List<PackageType>            PACKAGE_TYPES          = List.of(JDK);
    private static final List<ReleaseStatus>          RELEASE_STATUSES       = List.of(GA);
    private static final List<TermOfSupport>          TERMS_OF_SUPPORT       = List.of(TermOfSupport.LTS);
    private static final List<Bitness>                BITNESSES              = List.of(BIT_64);
    private static final Boolean                      BUNDLED_WITH_JAVA_FX   = false;

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM     = "";
    private static final String                       OPERATING_SYSTEM_PARAM = "";
    private static final String                       ARCHIVE_TYPE_PARAM     = "";
    private static final String                       PACKAGE_TYPE_PARAM     = "";
    private static final String                       RELEASE_STATUS_PARAM   = "";
    private static final String                       SUPPORT_TERM_PARAM     = "";
    private static final String                       BITNESS_PARAM          = "";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP       = Map.of(X64, "x64");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP   = Map.of(LINUX, "linux", WINDOWS, "windows");
    private static final Map<ArchiveType, String>     ARCHIVE_TYPE_MAP       = Map.of(TAR_GZ, "tar.gz");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP       = Map.of(JDK, "jdk");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP     = Map.of(GA, "ga");
    private static final Map<TermOfSupport, String>   SUPPORT_TERM_MAP       = Map.of(TermOfSupport.LTS, "lts");
    private static final Map<Bitness, String>         BITNESS_MAP            = Map.of(BIT_64, "64");


    @Override public Distro getDistro() { return Distro.DRAGONWELL; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public List<Scope> getScopes() { return List.of(BasicScope.PUBLIC); }

    @Override public List<Architecture> getArchitectures() { return ARCHITECTURES; }

    @Override public List<OperatingSystem> getOperatingSystems() { return OPERATING_SYSTEMS; }

    @Override public List<ArchiveType> getArchiveTypes() { return ARCHIVE_TYPES; }

    @Override public List<PackageType> getPackageTypes() { return PACKAGE_TYPES; }

    @Override public List<ReleaseStatus> getReleaseStatuses() { return RELEASE_STATUSES; }

    @Override public List<TermOfSupport> getTermsOfSupport() { return TERMS_OF_SUPPORT; }

    @Override public List<Bitness> getBitnesses() { return BITNESSES; }

    @Override public Boolean bundledWithJavaFX() { return BUNDLED_WITH_JAVA_FX; }


    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return SUPPORT_TERM_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }


    @Override public Map<Architecture, String> getArchitectureMap() { return ARCHITECTURE_MAP; }

    @Override public Map<OperatingSystem, String> getOperatingSystemMap() { return OPERATING_SYSTEM_MAP; }

    @Override public Map<ArchiveType, String> getArchiveTypeMap() { return ARCHIVE_TYPE_MAP; }

    @Override public Map<PackageType, String> getPackageTypeMap() { return PACKAGE_TYPE_MAP; }

    @Override public Map<ReleaseStatus, String> getReleaseStatusMap() { return RELEASE_STATUS_MAP; }

    @Override public Map<TermOfSupport, String> getTermOfSupportMap() { return SUPPORT_TERM_MAP; }

    @Override public Map<Bitness, String> getBitnessMap() { return BITNESS_MAP; }


    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.DRAGONWELL.get().equals(pkg.getDistribution()))
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
                queryBuilder.append(versionNumber.getFeature().getAsInt()).append("/releases");
                break;
            default:
                return "";
        }

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();

        if (Architecture.NONE != architecture && !ARCHITECTURE_MAP.containsKey(architecture)) { return pkgs; }

        if (OperatingSystem.NONE != operatingSystem && !OPERATING_SYSTEM_MAP.containsKey(operatingSystem)) { return pkgs; }

        if (ArchiveType.NONE != archiveType && !ARCHIVE_TYPE_MAP.containsKey(archiveType)) { return pkgs; }

        if (null != javafxBundled && javafxBundled) { return pkgs; }

        if (PackageType.NONE != packageType && !PACKAGE_TYPE_MAP.containsKey(packageType)) { return pkgs; }

        TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);
        supTerm = TermOfSupport.MTS == supTerm ? TermOfSupport.STS : supTerm;
        if (TermOfSupport.NONE != termOfSupport && termOfSupport != supTerm) { return pkgs; }

        String name = jsonObj.get("name").getAsString();
        ReleaseStatus rs = Constants.RELEASE_STATUS_LOOKUP.entrySet().stream()
                                                          .filter(entry -> name.endsWith(entry.getKey()))
                                                          .findFirst()
                                                          .map(Entry::getValue)
                                                          .orElse(ReleaseStatus.NONE);

        VersionNumber vNumber = null;
        String tag = jsonObj.get("tag_name").getAsString();
        if (tag.contains("_jdk")) {
            tag = tag.substring(tag.lastIndexOf("_jdk")).replace("_jdk", "");
            vNumber = VersionNumber.fromText(tag);
        }

        JsonArray assets = jsonObj.getAsJsonArray("assets");
        for (JsonElement element : assets) {
            JsonObject assetJsonObj = element.getAsJsonObject();
            String     fileName     = assetJsonObj.get("name").getAsString();
            if (fileName.endsWith(Constants.FILE_ENDING_TXT) || fileName.endsWith(Constants.FILE_ENDING_JAR)) { continue; }

            String downloadLink = assetJsonObj.get("browser_download_url").getAsString();

            Pkg pkg = new Pkg();

            ArchiveType ext = getFromFileName(fileName);
            if (SRC_TAR == ext || (ArchiveType.NONE != archiveType && ext != archiveType)) { continue; }
            pkg.setArchiveType(ext);

            pkg.setDistribution(Distro.DRAGONWELL.get());
            pkg.setFileName(fileName);
            pkg.setDirectDownloadUri(downloadLink);

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> fileName.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);
            if (Architecture.NONE != architecture && architecture != arch) { continue; }
            if (Bitness.NONE != bitness && bitness != arch.getBitness()) { continue; }
            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());

            if (null == vNumber) {
                vNumber = VersionNumber.fromText(downloadLink);
            }
            if (latest) {
                if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { continue; }
            } else {
                if (!versionNumber.equals(vNumber)) { continue; }
            }
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            VersionNumber dNumber = VersionNumber.fromText(fileName);
            pkg.setDistributionVersion(dNumber);

            pkg.setTermOfSupport(supTerm);

            pkg.setPackageType(JDK);

            if (ReleaseStatus.NONE != releaseStatus && rs != releaseStatus) { continue; }
            pkg.setReleaseStatus(rs);

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
            if (OperatingSystem.NONE != operatingSystem && operatingSystem != os) { continue; }
            pkg.setOperatingSystem(os);

            pkgs.add(pkg);
        }

        return pkgs;
    }
}
