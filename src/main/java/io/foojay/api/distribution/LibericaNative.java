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
import io.foojay.api.pkg.LibCType;
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

import java.io.StringReader;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.ArchiveType.getFromFileName;
import static io.foojay.api.pkg.OperatingSystem.LINUX;
import static io.foojay.api.pkg.OperatingSystem.MACOS;
import static io.foojay.api.pkg.OperatingSystem.WINDOWS;
import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.ReleaseStatus.GA;


public class LibericaNative implements Distribution {
    private static final Logger        LOGGER                 = LoggerFactory.getLogger(LibericaNative.class);

    private static final String        PACKAGE_URL            = "https://download.bell-sw.com/vm/21.0.0.2";
    public  static final String        PKGS_PROPERTIES        = "https://github.com/foojay2020/openjdk_releases/raw/main/liberica_native.properties";

    // URL parameters
    private static final String        ARCHITECTURE_PARAM     = "";
    private static final String        OPERATING_SYSTEM_PARAM = "";
    private static final String        ARCHIVE_TYPE_PARAM     = "";
    private static final String        PACKAGE_TYPE_PARAM     = "";
    private static final String        RELEASE_STATUS_PARAM   = "";
    private static final String        SUPPORT_TERM_PARAM     = "";
    private static final String        BITNESS_PARAM          = "";

    private static final HashAlgorithm HASH_ALGORITHM         = HashAlgorithm.NONE;
    private static final String        HASH_URI               = "";
    private static final SignatureType SIGNATURE_TYPE         = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM    = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI          = "";


    @Override public Distro getDistro() { return Distro.LIBERICA_NATIVE; }

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
                                             .filter(pkg -> Distro.LIBERICA_NATIVE.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {

        StringBuilder queryBuilder = new StringBuilder(PACKAGE_URL);
        switch(operatingSystem) {
            case LINUX:
                switch(architecture) {
                    case X64    :
                    case AMD64  : queryBuilder.append("/bellsoft-liberica-vm-openjdk11-21.0.0.2-linux-amd64.tar.gz"); break;
                    case ARM64  :
                    case AARCH64: queryBuilder.append("/bellsoft-liberica-vm-openjdk11-21.0.0.2-linux-aarch64.tar.gz");break;
                }
                break;
            case ALPINE_LINUX:
            case LINUX_MUSL:
                switch(architecture) {
                    case X64    :
                    case AMD64  : queryBuilder.append("/bellsoft-liberica-vm-openjdk11-21.0.0.2-linux-x64-musl.tar.gz"); break;
                    case ARM64  :
                    case AARCH64: queryBuilder.append("/bellsoft-liberica-vm-openjdk11-21.0.0.2-linux-aarch64-musl.tar.gz"); break;
                }
            case MACOS:
                switch(architecture) {
                    case X64  :
                    case AMD64: queryBuilder.append("/bellsoft-liberica-vm-openjdk11-21.0.0.2-macos-amd64.zip"); break;
                }
                break;
        }

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType bundleType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();
        return pkgs;
    }

    public List<Pkg> getAllPkgs() {
        List<Pkg> pkgs = new ArrayList<>();

        List<String> downloadLinks = new ArrayList<>();

        // Load jdk properties
        try {
            final Properties           propertiesPkgs = new Properties();
            final HttpResponse<String> response       = Helper.get(PKGS_PROPERTIES);
            if (null == response) {
                LOGGER.debug("No jdk properties found for {}", getName());
                return pkgs;
            }
            final String propertiesText = response.body();
            if (propertiesText.isEmpty()) {
                LOGGER.debug("jdk properties are empty for {}", getName());
                return pkgs;
            }
            propertiesPkgs.load(new StringReader(propertiesText));

            propertiesPkgs.forEach((key, value) -> {
                String downloadLink = value.toString();
                downloadLinks.add(downloadLink);
            });
        } catch (Exception e) {
            LOGGER.error("Error reading jdk properties file from github for {}. {}", getName(), e.getMessage());
        }


        for(String downloadLink : downloadLinks) {
            String   filename         = Helper.getFileNameFromText(downloadLink);
            String   strippedFilename = filename.replaceFirst("bellsoft-liberica-vm-openjdk11-", "").replaceAll("(\\.tar\\.gz|\\.zip)", "");
            String[] filenameParts    = strippedFilename.split("-");

            Pkg pkg = new Pkg();

            pkg.setDistribution(Distro.LIBERICA_NATIVE.get());
            pkg.setFileName(filename);
            pkg.setDirectDownloadUri(downloadLink);

            ArchiveType ext = getFromFileName(filename);
            pkg.setArchiveType(ext);

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> strippedFilename.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);

            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());


            VersionNumber vNumber = VersionNumber.fromText(filenameParts[0]);
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);

            pkg.setTermOfSupport(TermOfSupport.LTS);

            pkg.setPackageType(JDK);

            pkg.setReleaseStatus(GA);

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                  .filter(entry -> strippedFilename.contains(entry.getKey()))
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
            if (OperatingSystem.NONE == os) {
                LOGGER.debug("Operating System not found in {} for filename: {}", getName(), filename);
                continue;
            }
            pkg.setOperatingSystem(os);

            if (WINDOWS == os) {
                pkg.setLibCType(LibCType.C_STD_LIB);
            } else {
                if (filenameParts.length == 4) {
                    pkg.setLibCType(LibCType.MUSL);
                } else {
                    pkg.setLibCType(LibCType.GLIBC);
                }
            }

            pkgs.add(pkg);
        }

        return pkgs;
    }
}
