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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.OptionalInt;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.PackageType.JRE;
import static eu.hansolo.jdktools.ReleaseStatus.EA;
import static eu.hansolo.jdktools.ReleaseStatus.GA;


public class Microsoft implements Distribution {
    private static final Logger        LOGGER = LoggerFactory.getLogger(Microsoft.class);

    private static final Pattern       FILENAME_PREFIX_PATTERN = Pattern.compile("microsoft-");
    private static final Matcher       FILENAME_PREFIX_MATCHER = FILENAME_PREFIX_PATTERN.matcher("");
    private static final String        PACKAGE_URL             = "https://docs.microsoft.com/java/openjdk/download";
    private static final String        OLDER_PACKAGES_URL      = "https://docs.microsoft.com/en-us/java/openjdk/older-releases";
    public  static final String        PKGS_PROPERTIES         = "https://github.com/foojayio/openjdk_releases/raw/main/microsoft.properties";

    // URL parameters
    private static final String        ARCHITECTURE_PARAM      = "";
    private static final String        OPERATING_SYSTEM_PARAM  = "";
    private static final String        ARCHIVE_TYPE_PARAM      = "";
    private static final String        PACKAGE_TYPE_PARAM      = "";
    private static final String        RELEASE_STATUS_PARAM    = "";
    private static final String        SUPPORT_TERM_PARAM      = "";
    private static final String        BITNESS_PARAM           = "";

    private static final HashAlgorithm HASH_ALGORITHM          = HashAlgorithm.NONE;
    private static final String        HASH_URI                = "";
    private static final SignatureType SIGNATURE_TYPE          = SignatureType.NONE;
    private static final HashAlgorithm SIGNATURE_ALGORITHM     = HashAlgorithm.NONE;
    private static final String        SIGNATURE_URI           = "";
    private static final String        OFFICIAL_URI            = "https://www.microsoft.com/openjdk";


    @Override public Distro getDistro() { return Distro.MICROSOFT; }

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
        return List.of("microsoft", "Microsoft", "MICROSOFT", "Microsoft OpenJDK", "Microsoft Build of OpenJDK");
    }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.MICROSOFT.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        StringBuilder queryBuilder = new StringBuilder(PACKAGE_URL);

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder.toString());

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();

        return pkgs;
    }

    public List<Pkg> getAllPkgs(final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            String htmlCurrentPkgs = Helper.getTextFromUrl(PACKAGE_URL);
            pkgs.addAll(getAllPkgsFromHtml(htmlCurrentPkgs, onlyNewPkgs));

            String htmlOlderPkgs = Helper.getTextFromUrl(OLDER_PACKAGES_URL);
            pkgs.addAll(getAllPkgsFromHtml(htmlOlderPkgs, onlyNewPkgs));
        } catch (Exception e) {
            LOGGER.error("Error fetching all packages from Microsoft. {}", e);
        }

        Helper.checkPkgsForTooEarlyGA(pkgs);

        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html, final boolean onlyNewPkgs) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }

        OptionalInt nextEA       = Helper.getNextEA();
        OptionalInt nextButOneEA = Helper.getNextButOneEA();

        List<String> fileHrefs       = new ArrayList<>(Helper.getFileHrefsFromString(html));
        List<String> sigFileHrefs    = new ArrayList<>(Helper.getSigFileHrefsFromString(html));
        List<String> sha256FileHrefs = new ArrayList<>(Helper.getSha256FileHrefsFromString(html));
        for (String href : fileHrefs) {
            final String filename = Helper.getFileNameFromText(href);
            if (filename.contains("debugsymbols") || filename.startsWith("jdk") || filename.contains("sources")) { continue; }

            if (onlyNewPkgs) {
                if (CacheManager.INSTANCE.pkgCache.getPkgs().stream().filter(p -> p.getFilename().equals(filename)).filter(p -> p.getDirectDownloadUri().equals(href)).count() > 0) { continue; }
            }

            final String          withoutPrefix   = filename.replace("microsoft-", "");
            final VersionNumber   versionNumber   = VersionNumber.fromText(withoutPrefix);

            if (versionNumber.getInterim().getAsInt() > 0) {
                versionNumber.setBuild(versionNumber.getInterim().getAsInt());
                versionNumber.setInterim(0);
                versionNumber.setUpdate(0);
                versionNumber.setPatch(0);
            } else if (versionNumber.getPatch().isPresent() && versionNumber.getPatch().getAsInt() != 0 &&
                       versionNumber.getFifth().isPresent() && versionNumber.getFifth().getAsInt() != 0) {
                versionNumber.setPatch(0);
                versionNumber.setFifth(0);
            } else if (versionNumber.getPatch().isPresent() && versionNumber.getPatch().getAsInt() != 0 &&
                       versionNumber.getFifth().isPresent() && versionNumber.getFifth().getAsInt() != 0 &&
                       versionNumber.getSixth().isPresent() && versionNumber.getSixth().getAsInt() != 0) {
                versionNumber.setFifth(0);
                versionNumber.setSixth(0);
            }

            final MajorVersion    majorVersion    = new MajorVersion(versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0);
            final PackageType     packageType     = withoutPrefix.startsWith("jdk") ? JDK : JRE;

            OperatingSystem operatingSystem = Constants.OPERATING_SYSTEM_LOOKUP.entrySet()
                                                                               .stream()
                                                                               .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                               .findFirst()
                                                                               .map(Entry::getValue)
                                                                               .orElse(OperatingSystem.NOT_FOUND);
            if (OperatingSystem.NOT_FOUND == operatingSystem) {
                LOGGER.debug("Operating System not found in {} for filename: {}", getName(), filename);
                continue;
            }

            final Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet()
                                                                           .stream()
                                                                           .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                           .findFirst()
                                                                           .map(Entry::getValue)
                                                                           .orElse(Architecture.NOT_FOUND);
            if (Architecture.NOT_FOUND == architecture) {
                LOGGER.debug("Architecture not found in {} for filename: {}", getName(), filename);
                continue;
            }

            ArchiveType archiveType = ArchiveType.getFromFileName(filename);
            if (OperatingSystem.MACOS == operatingSystem) {
                switch(archiveType) {
                    case DEB:
                    case RPM: operatingSystem = OperatingSystem.LINUX; break;
                    case CAB:
                    case MSI:
                    case EXE: operatingSystem = OperatingSystem.WINDOWS; break;
                }
            }

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.MICROSOFT.get());
            pkg.setArchitecture(architecture);
            pkg.setBitness(architecture.getBitness());
            pkg.setVersionNumber(versionNumber);
            pkg.setJavaVersion(versionNumber);
            pkg.setDistributionVersion(versionNumber);
            pkg.setJdkVersion(new MajorVersion(versionNumber.getFeature().getAsInt()));
            pkg.setDirectDownloadUri(href);
            pkg.setFileName(filename);
            if (sigFileHrefs.contains(href.toLowerCase() + ".sig")) { pkg.setSignatureUri(href.toLowerCase() + ".sig"); }
            if (sha256FileHrefs.contains(href.toLowerCase() + ".sha256sum.txt")) {
                pkg.setChecksumUri(href.toLowerCase() + ".sha256sum.txt");
                pkg.setChecksumType(HashAlgorithm.SHA256);
            }
            pkg.setArchiveType(archiveType);
            pkg.setJavaFXBundled(false);
            pkg.setTermOfSupport(majorVersion.getTermOfSupport());
            if (nextEA.isPresent()) {
                pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.getAsInt() == nextEA.getAsInt() || majorVersion.getAsInt() == nextButOneEA.getAsInt()) ? EA : GA);
            } else {
                pkg.setReleaseStatus((filename.contains("-ea.") || majorVersion.equals(MajorVersion.getLatest(true))) ? EA : GA);
            }
            pkg.setPackageType(packageType);
            pkg.setOperatingSystem(operatingSystem);
            pkg.setFreeUseInProduction(Boolean.TRUE);
            pkg.setSize(Helper.getFileSize(href));

            pkgs.add(pkg);
        }

        return pkgs;
    }
}

