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
import io.foojay.api.pkg.FPU;
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
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.stream.Collectors;


public class BiSheng implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiSheng.class);

    private static final String                       GITHUB_USER            = "openeuler";
    private static final String                       PACKAGE_URL            = "https://www.gitee.com/" + GITHUB_USER + "/bishengjdk";
    private static final String                       CDN_URL                = "https://mirror.iscas.ac.cn/kunpeng/archive/compiler/bisheng_jdk/";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM     = "";
    private static final String                       OPERATING_SYSTEM_PARAM = "";
    private static final String                       ARCHIVE_TYPE_PARAM     = "";
    private static final String                       PACKAGE_TYPE_PARAM     = "";
    private static final String                       RELEASE_STATUS_PARAM   = "";
    private static final String                       SUPPORT_TERM_PARAM     = "";
    private static final String                       BITNESS_PARAM          = "";

    private static final HashAlgorithm HASH_ALGORITHM      = HashAlgorithm.NONE;
    private static final String        HASH_URI            = "";
    private static final SignatureType SIGNATURE_TYPE      = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI          = "";
    private static final String                       OFFICIAL_URI           = "https://www.hikunpeng.com/en/developer/devkit/compiler?data=JDK";


    @Override public Distro getDistro() { return Distro.BISHENG; }

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

    @Override public String getOfficialUri() { return OFFICIAL_URI; }

    @Override public List<String> getSynonyms() {
        return List.of("bisheng", "BISHENG", "BiSheng", "bi_sheng", "BI_SHENG", "bi-sheng", "BI-SHENG", "bi sheng", "BI SHENG", "Bi Sheng");
    }

    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.BISHENG.get().equals(pkg.getDistribution()))
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
            case 17:
                queryBuilder.append("-").append(versionNumber.getFeature().getAsInt());
                break;
            default:
                return "";
        }

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);
        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();
        return pkgs;
    }

    /**
     * Returns all packages found on the Kunpeng CDN
     * @return all packages found on the Kunpeng CDN
     */
    public List<Pkg> getAllPackagesFromCDN() {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            final HttpResponse<String> response = Helper.get(CDN_URL);
            if (null == response) { return pkgs; }
            final String html = response.body();
            if (html.isEmpty()) { return pkgs; }

            final List<String> fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));
            for (String href : fileHrefs) {
                String filename = Helper.getFileNameFromText(href);
                if (filename.endsWith(Constants.FILE_ENDING_SHA256)) { continue; }

                String[]        filenameParts       = filename.split("-");
                PackageType     packageType         = PackageType.fromText(filenameParts[1]);
                VersionNumber   versionNumber       = VersionNumber.fromText(filenameParts[2]);
                OperatingSystem os                  = OperatingSystem.fromText(filenameParts[3]);
                Architecture    architecture        = Architecture.fromText(filenameParts[4]);
                TermOfSupport   termOfSupport       = Helper.getTermOfSupport(versionNumber);
                String          downloadLink        = CDN_URL + filename;
                VersionNumber   distroVersionNumber = versionNumber;

                Pkg pkg = new Pkg();
                pkg.setDistribution(Distro.BISHENG.get());
                pkg.setVersionNumber(versionNumber);
                pkg.setJavaVersion(versionNumber);
                pkg.setDistributionVersion(distroVersionNumber);

                FPU fpu;
                if (filename.contains("32sf.")) {
                    fpu = FPU.SOFT_FLOAT;
                } else if (filename.contains("32hf.")) {
                    fpu = FPU.HARD_FLOAT;
                } else {
                    fpu = FPU.UNKNOWN;
                }
                pkg.setFPU(fpu);

                pkg.setPackageType(packageType);

                ArchiveType archiveType = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                                       .filter(entry -> filename.endsWith(entry.getKey()))
                                                                       .findFirst()
                                                                       .map(Entry::getValue)
                                                                       .orElse(ArchiveType.NOT_FOUND);
                if (ArchiveType.NOT_FOUND == archiveType) { continue; }
                pkg.setArchiveType(archiveType);

                if (OperatingSystem.NOT_FOUND == os) {
                    os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                          .stream()
                                                          .filter(entry -> filename.contains(entry.getKey()))
                                                          .findFirst()
                                                          .map(Entry::getValue)
                                                          .orElse(OperatingSystem.NOT_FOUND);
                    if (OperatingSystem.NOT_FOUND == os) {
                        os = Helper.fetchOperatingSystemByArchiveType(archiveType.getUiString());
                    }
                }

                if (OperatingSystem.NOT_FOUND == os) { continue; }
                pkg.setOperatingSystem(os);

                if (Architecture.NOT_FOUND == architecture) {
                    architecture = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                                .stream()
                                                                .filter(entry -> filename.contains(entry.getKey()))
                                                                .findFirst()
                                                                .map(Entry::getValue)
                                                                .orElse(Architecture.NOT_FOUND);
                }
                if (Architecture.NOT_FOUND == architecture) {
                    if (OperatingSystem.MACOS == pkg.getOperatingSystem()) {
                        architecture = Architecture.X64;
                    } else {
                        continue;
                    }
                }
                pkg.setArchitecture(architecture);
                pkg.setBitness(architecture.getBitness());

                pkg.setReleaseStatus(ReleaseStatus.GA);

                pkg.setTermOfSupport(termOfSupport);
                pkg.setFileName(filename);
                pkg.setArchiveType(archiveType);
                pkg.setDirectDownloadUri(downloadLink);
                pkg.setJavaFXBundled(Boolean.FALSE);

                pkg.setFreeUseInProduction(Boolean.TRUE);

                pkgs.add(pkg);
            }
        } catch (Exception e) {
            LOGGER.debug("Error fetching packages from Bisheng CDN. {}", e.getMessage());
        }
        return pkgs;
    }
}
