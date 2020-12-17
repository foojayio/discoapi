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

import static io.foojay.api.pkg.Architecture.AARCH64;
import static io.foojay.api.pkg.Architecture.X64;
import static io.foojay.api.pkg.Architecture.X86;
import static io.foojay.api.pkg.Bitness.BIT_64;
import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.PackageType.JRE;
import static io.foojay.api.pkg.ArchiveType.MSI;
import static io.foojay.api.pkg.ArchiveType.TAR_GZ;
import static io.foojay.api.pkg.ArchiveType.ZIP;
import static io.foojay.api.pkg.OperatingSystem.LINUX;
import static io.foojay.api.pkg.OperatingSystem.LINUX_MUSL;
import static io.foojay.api.pkg.OperatingSystem.MACOS;
import static io.foojay.api.pkg.OperatingSystem.WINDOWS;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;


public class RedHat implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedHat.class);

    public  static final String                       PACKAGE_ALL_URL        = "https://developers.redhat.com/products/openjdk/download";
    private static final String                       DOWNLOAD_PREFIX        = "https://developers.redhat.com/download-manager/file/";
    private static final String                       PACKAGE_URL            = "";
    private static final List<Architecture>           ARCHITECTURES          = List.of(X64, AARCH64);
    private static final List<OperatingSystem>        OPERATING_SYSTEMS      = List.of(LINUX_MUSL, LINUX, WINDOWS, MACOS);
    private static final List<ArchiveType>            ARCHIVE_TYPES          = List.of(TAR_GZ, ZIP);
    private static final List<PackageType>            PACKAGE_TYPES          = List.of(JDK, JRE);
    private static final List<ReleaseStatus>          RELEASE_STATUSES       = List.of(EA, GA);
    private static final List<TermOfSupport>          TERM_OF_SUPPORT        = List.of(TermOfSupport.STS, TermOfSupport.LTS);
    private static final List<Bitness>                BITNESSES              = List.of(BIT_64);
    private static final Boolean                      BUNDLED_WITH_JAVA_FX   = false;

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM     = "";
    private static final String                       OPERATING_SYSTEM_PARAM = "";
    private static final String                       ARCHIVE_TYPE_PARAM     = "";
    private static final String                       PACKAGE_TYPE_PARAM     = "";
    private static final String                       RELEASE_STATUS_PARAM   = "";
    private static final String                       TERM_OF_SUPPORT_PARAM  = "";
    private static final String                       BITNESS_PARAM          = "";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP       = Map.of(X64, "x64", X86, "x86");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP   = Map.of(WINDOWS, "windows");
    private static final Map<ArchiveType, String>     ARCHIVE_TYPE_MAP       = Map.of(MSI, "msi", ZIP, "zip");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP       = Map.of(JDK, "jdk");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP     = Map.of(EA, "early_access", GA, "GA");
    private static final Map<TermOfSupport, String>   TERMS_OF_SUPPORT_MAP   = Map.of(TermOfSupport.LTS, "lts");
    private static final Map<Bitness, String>         BITNESS_MAP            = Map.of(BIT_64, "64");


    public RedHat() {

    }

    @Override public Distro getDistro() { return Distro.RED_HAT; }

    @Override public String getName() { return getDistro().getUiString(); }

    @Override public String getPkgUrl() { return PACKAGE_URL; }

    @Override public List<Scope> getScopes() { return List.of(BasicScope.PUBLIC); }

    @Override public List<Architecture> getArchitectures() { return ARCHITECTURES; }

    @Override public List<OperatingSystem> getOperatingSystems() { return OPERATING_SYSTEMS; }

    @Override public List<ArchiveType> getArchiveTypes() { return ARCHIVE_TYPES; }

    @Override public List<PackageType> getPackageTypes() { return PACKAGE_TYPES; }

    @Override public List<ReleaseStatus> getReleaseStatuses() { return RELEASE_STATUSES; }

    @Override public List<TermOfSupport> getTermsOfSupport() { return TERM_OF_SUPPORT; }

    @Override public List<Bitness> getBitnesses() { return BITNESSES; }

    @Override public Boolean bundledWithJavaFX() { return BUNDLED_WITH_JAVA_FX; }


    @Override public String getArchitectureParam() { return ARCHITECTURE_PARAM; }

    @Override public String getOperatingSystemParam() { return OPERATING_SYSTEM_PARAM; }

    @Override public String getArchiveTypeParam() { return ARCHIVE_TYPE_PARAM; }

    @Override public String getPackageTypeParam() { return PACKAGE_TYPE_PARAM; }

    @Override public String getReleaseStatusParam() { return RELEASE_STATUS_PARAM; }

    @Override public String getTermOfSupportParam() { return TERM_OF_SUPPORT_PARAM; }

    @Override public String getBitnessParam() { return BITNESS_PARAM; }


    @Override public Map<Architecture, String> getArchitectureMap() { return ARCHITECTURE_MAP; }

    @Override public Map<OperatingSystem, String> getOperatingSystemMap() { return OPERATING_SYSTEM_MAP; }

    @Override public Map<ArchiveType, String> getArchiveTypeMap() { return ARCHIVE_TYPE_MAP; }

    @Override public Map<PackageType, String> getPackageTypeMap() { return PACKAGE_TYPE_MAP; }

    @Override public Map<ReleaseStatus, String> getReleaseStatusMap() { return RELEASE_STATUS_MAP; }

    @Override public Map<TermOfSupport, String> getTermOfSupportMap() { return TERMS_OF_SUPPORT_MAP; }

    @Override public Map<Bitness, String> getBitnessMap() { return BITNESS_MAP; }


    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.RED_HAT.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL);
        if (ReleaseStatus.NONE == releaseStatus || GA == releaseStatus) {
            queryBuilder.append(RELEASE_STATUS_MAP.get(GA));
        } else {
            queryBuilder.append(RELEASE_STATUS_MAP.get(EA));
        }
        queryBuilder.append("/");

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();

        if (null == jsonObj) {

        }

        return pkgs;
    }

    public List<Pkg> getAllPkgs() {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            final String htmlAllJDKs  = Helper.getTextFromUrl(PACKAGE_ALL_URL);
            pkgs.addAll(getAllPkgsFromHtml(htmlAllJDKs));
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages from RedHat. {}", e);
        }
        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }
        List<String> fileNames = new ArrayList<>(Helper.getFileHrefsFromString(html));
        for (String filename : fileNames) {
            if (filename.endsWith("sources.zip") || filename.endsWith("src.zip")) { continue; }

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.RED_HAT.get());

            String withoutPrefix = filename.replace(DOWNLOAD_PREFIX, "");

            pkg.setJavaFXBundled(withoutPrefix.startsWith("openjfx"));
            withoutPrefix = withoutPrefix.replaceAll("((java)-.*openjdk-)|(openjfx-)", "");

            if (withoutPrefix.startsWith("jre-")) {
                pkg.setPackageType(JRE);
                withoutPrefix = withoutPrefix.replace("jre-", "");
            } else {
                pkg.setPackageType(JDK);
            }

            Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                     .filter(entry -> filename.contains(entry.getKey()))
                                                                     .findFirst()
                                                                     .map(Entry::getValue)
                                                                     .orElse(Architecture.NONE);
            pkg.setArchitecture(architecture);
            pkg.setBitness(architecture.getBitness());

            OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                               .filter(entry -> filename.contains(entry.getKey()))
                                                                               .findFirst()
                                                                               .map(Entry::getValue)
                                                                               .orElse(OperatingSystem.NONE);
            pkg.setOperatingSystem(operatingSystem);

            ArchiveType archiveType = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                                   .filter(entry -> filename.contains(entry.getKey()))
                                                                   .findFirst()
                                                                   .map(Entry::getValue)
                                                                   .orElse(ArchiveType.NONE);
            pkg.setArchiveType(archiveType);

            pkg.setReleaseStatus(withoutPrefix.contains(".dev.") ? EA : GA);

            // No support for Feature.Interim.Update.Path but only Major.Minor.Update => setPatch(0)
            VersionNumber vNumber = VersionNumber.fromText(withoutPrefix);
            VersionNumber versionNumber = vNumber;
            versionNumber.setPatch(0);
            pkg.setVersionNumber(versionNumber);
            pkg.setJavaVersion(versionNumber);
            pkg.setDistributionVersion(vNumber);

            pkg.setTermOfSupport(Helper.getTermOfSupport(versionNumber));

            pkg.setDirectlyDownloadable(false);

            pkg.setFileName(Helper.getFileNameFromText(filename));
            pkg.setDownloadSiteUri("https://developers.redhat.com/products/openjdk/download");

            pkgs.add(pkg);
        }

        return pkgs;
    }
}
