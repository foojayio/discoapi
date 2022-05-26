/*
 * Copyright (c) 2021 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.foojay.api.distribution;

import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.HashAlgorithm;
import eu.hansolo.jdktools.LibCType;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.SignatureType;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.MajorVersion;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.ArchiveType.getFromFileName;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.OperatingSystem.MACOS;
import static eu.hansolo.jdktools.OperatingSystem.WINDOWS;
import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.PackageType.JRE;
import static eu.hansolo.jdktools.ReleaseStatus.GA;


public class ZuluPrime implements Distribution {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZuluPrime.class);

    private static final String        PACKAGE_URL            = "https://docs.azul.com/prime/prime-quick-start-tar";
    public  static final String        PKGS_PROPERTIES        = "https://github.com/foojayio/openjdk_releases/raw/main/zulu_prime.properties";

    // URL parameters
    private static final String        ARCHITECTURE_PARAM     = "";
    private static final String        OPERATING_SYSTEM_PARAM = "";
    private static final String        ARCHIVE_TYPE_PARAM     = "";
    private static final String        PACKAGE_TYPE_PARAM     = "";
    private static final String        RELEASE_STATUS_PARAM   = "";
    private static final String        SUPPORT_TERM_PARAM     = "";
    private static final String        BITNESS_PARAM          = "";

    private static final HashAlgorithm HASH_ALGORITHM      = HashAlgorithm.NONE;
    private static final String        HASH_URI            = "";
    private static final SignatureType SIGNATURE_TYPE      = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI       = "";
    private static final String        OFFICIAL_URI        = "https://www.azul.com/products/prime/stream-download/";


    @Override public Distro getDistro() { return Distro.ZULU_PRIME; }

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
        return List.of("zing", "ZING", "Zing", "prime", "PRIME", "Prime", "zuluprime", "ZULUPRIME", "ZuluPrime", "zulu_prime","ZULU_PRIME", "Zulu_Prime", "zulu prime", "ZULU PRIME", "Zulu Prime");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.ZULU_PRIME.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {

        StringBuilder queryBuilder = new StringBuilder(PACKAGE_URL);

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType bundleType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        return pkgs;
    }

    public List<Pkg> getAllPkgs(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        Map<String, String> downloadLinkMap = new HashMap<>();

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
                downloadLinkMap.put(key.toString(), value.toString());
            });
        } catch (Exception e) {
            LOGGER.error("Error reading jdk properties file from github for {}. {}", getName(), e.getMessage());
        }


        for(Entry<String, String> entry : downloadLinkMap.entrySet()) {
            String   key              = entry.getKey();
            String   downloadLink     = entry.getValue();

            String[] keyParts         = key.split("-");

            String   filename         = Helper.getFileNameFromText(downloadLink);
            String   strippedFilename = filename.replaceFirst("zing[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*-[0-9]*-ca-jdk", "").replaceAll("(\\.tar\\.gz)", "");
            String[] filenameParts    = strippedFilename.split("-");

            Pkg pkg = new Pkg();

            pkg.setDistribution(Distro.ZULU_PRIME.get());
            pkg.setFileName(filename);
            pkg.setDirectDownloadUri(downloadLink);

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(downloadLink)).count() > 0) { continue; }
            }

            ArchiveType ext = getFromFileName(filename);
            pkg.setArchiveType(ext);

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(e -> keyParts[2].contains(e.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);

            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());


            VersionNumber vNumber = VersionNumber.fromText(keyParts[0].replaceAll("_", "\\."));
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);
            pkg.setJdkVersion(new MajorVersion(vNumber.getFeature().getAsInt()));

            pkg.setTermOfSupport(Helper.getTermOfSupport(vNumber.getFeature().getAsInt()));

            pkg.setPackageType(JDK);

            pkg.setReleaseStatus(GA);

            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                  .filter(e -> keyParts[1].contains(e.getKey()))
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

            pkg.setFreeUseInProduction(Boolean.FALSE);
            pkg.setSize(Helper.getFileSize(downloadLink));

            pkgs.add(pkg);
        }

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        final String html;
        try {
            final HttpResponse<String> response = Helper.get(PACKAGE_URL);
            if (null == response) { return pkgs; }
            html = response.body();
            if (null == html || html.isEmpty()) { return pkgs; }
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages from {}. {}", getName(), e);
            return pkgs;
        }

        List<String> fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));
        Pattern zingPattern = Pattern.compile("zing[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*-[0-9]*-");
        for (String fileHref : fileHrefs) {
            String filename = Helper.getFileNameFromText(fileHref.replaceAll("\"", ""));

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(fileHref)).count() > 0) { continue; }
            }

            String withoutPrefix = zingPattern.matcher(filename).replaceAll("");

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.ZULU_PRIME.get());
            pkg.setJavaFXBundled(false);
            pkg.setPackageType(withoutPrefix.contains("jre") ? JRE : JDK);

            withoutPrefix = withoutPrefix.replace(JDK == pkg.getPackageType() ? "jdk" : "jre", "");

            Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                     .filter(entry -> filename.contains(entry.getKey()))
                                                                     .findFirst()
                                                                     .map(Entry::getValue)
                                                                     .orElse(Architecture.NOT_FOUND);
            if (Architecture.NOT_FOUND == architecture) {
                LOGGER.debug("Architecture not found in {} for filename: {}", getName(), filename);
                continue;
            }
            pkg.setArchitecture(architecture);
            pkg.setBitness(architecture.getBitness());

            OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                               .filter(entry -> filename.contains(entry.getKey()))
                                                                               .findFirst()
                                                                               .map(Entry::getValue)
                                                                               .orElse(OperatingSystem.NOT_FOUND);
            if (OperatingSystem.NOT_FOUND == operatingSystem) {
                LOGGER.debug("Operating System not found in {} for filename: {}", getName(), filename);
                continue;
            }
            pkg.setOperatingSystem(operatingSystem);

            ArchiveType archiveType = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                                   .filter(entry -> filename.contains(entry.getKey()))
                                                                   .findFirst()
                                                                   .map(Entry::getValue)
                                                                   .orElse(ArchiveType.NOT_FOUND);
            if (ArchiveType.NOT_FOUND == archiveType) {
                LOGGER.debug("Archive Type not found in {} for filename: {}", getName(), filename);
                continue;
            }
            pkg.setArchiveType(archiveType);

            // No support for Feature.Interim.Update.Path but only Major.Minor.Update => setPatch(0)
            VersionNumber versionNumber = VersionNumber.fromText(withoutPrefix.substring(0, withoutPrefix.indexOf("-")));
            pkg.setVersionNumber(versionNumber);
            pkg.setJavaVersion(versionNumber);

            VersionNumber distributionVersion = VersionNumber.fromText(filename.substring(4, filename.indexOf("-")));
            pkg.setDistributionVersion(distributionVersion);
            pkg.setJdkVersion(new MajorVersion(versionNumber.getFeature().getAsInt()));
            pkg.setReleaseStatus(GA);

            pkg.setTermOfSupport(Helper.getTermOfSupport(versionNumber));

            pkg.setDirectlyDownloadable(true);

            pkg.setFileName(Helper.getFileNameFromText(filename));
            pkg.setDirectDownloadUri(fileHref);

            pkg.setFreeUseInProduction(Boolean.FALSE);

            pkg.setSize(Helper.getFileSize(fileHref));

            pkgs.add(pkg);
        }
        return pkgs;
    }
}
