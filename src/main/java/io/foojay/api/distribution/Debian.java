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
import eu.hansolo.jdktools.FPU;
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
import io.foojay.api.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.hansolo.jdktools.Architecture.AMD64;
import static eu.hansolo.jdktools.Architecture.ARM;
import static eu.hansolo.jdktools.Architecture.ARMEL;
import static eu.hansolo.jdktools.Architecture.ARMHF;
import static eu.hansolo.jdktools.Architecture.I386;
import static eu.hansolo.jdktools.Architecture.MIPS;
import static eu.hansolo.jdktools.Architecture.MIPSEL;
import static eu.hansolo.jdktools.Architecture.PPC;
import static eu.hansolo.jdktools.Architecture.PPC64LE;
import static eu.hansolo.jdktools.Architecture.S390X;
import static eu.hansolo.jdktools.ArchiveType.DEB;
import static eu.hansolo.jdktools.Bitness.BIT_32;
import static eu.hansolo.jdktools.Bitness.BIT_64;
import static eu.hansolo.jdktools.OperatingSystem.LINUX;
import static eu.hansolo.jdktools.PackageType.JDK;
import static eu.hansolo.jdktools.PackageType.JRE;
import static eu.hansolo.jdktools.ReleaseStatus.EA;
import static eu.hansolo.jdktools.ReleaseStatus.GA;
import static eu.hansolo.jdktools.TermOfSupport.LTS;
import static eu.hansolo.jdktools.TermOfSupport.MTS;
import static eu.hansolo.jdktools.TermOfSupport.STS;


public class Debian implements Distribution {
    private static final Logger                       LOGGER                 = LoggerFactory.getLogger(Debian.class);
    private static final String                       CDN_URL                = "http://ftp.debian.org/debian/pool/main/o/";
    private static final Pattern                      DEB_PKG_PATTERN        = Pattern.compile("(openjdk-)([0-9]{1,2})-(jre|jdk)_(([1-9]\\d*)((u(\\d+))|(\\.?(\\d+)?\\.?(\\d+)?\\.?(\\d+)?\\.?(\\d+)?\\.(\\d+)))?((_|b)(\\d+))?((-|\\+|\\.)([a-zA-Z0-9\\-\\+]+)(\\.[0-9]+)?)?)_(.*)(\\.deb)");
    private static final Matcher                      DEB_PKG_MATCHER        = DEB_PKG_PATTERN.matcher("");

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM     = "";
    private static final String                       OPERATING_SYSTEM_PARAM = "";
    private static final String                       ARCHIVE_TYPE_PARAM     = "";
    private static final String                       PACKAGE_TYPE_PARAM     = "";
    private static final String                       RELEASE_STATUS_PARAM   = "";
    private static final String                       SUPPORT_TERM_PARAM     = "";
    private static final String                       BITNESS_PARAM          = "";

    // Mappings for url parameters
    private static final Map<Architecture, String>    ARCHITECTURE_MAP       = Map.of(ARM, "arm", ARMEL, "armel", ARMHF, "armhf", MIPS, "mips", MIPSEL, "mipsel", PPC, "ppc", PPC64LE, "ppc64le", S390X, "s390x", I386, "i386", AMD64, "amd64");
    private static final Map<OperatingSystem, String> OPERATING_SYSTEM_MAP   = Map.of(LINUX, "linux");
    private static final Map<ArchiveType, String>     ARCHIVE_TYPE_MAP       = Map.of(DEB, "deb");
    private static final Map<PackageType, String>     PACKAGE_TYPE_MAP       = Map.of(JDK, "jdk", JRE, "jre");
    private static final Map<ReleaseStatus, String>   RELEASE_STATUS_MAP     = Map.of(EA, "ea", GA, "ga");
    private static final Map<TermOfSupport, String>   TERMS_OF_SUPPORT_MAP   = Map.of(STS, "sts", MTS, "mts", LTS, "lts");
    private static final Map<Bitness, String>         BITNESS_MAP            = Map.of(BIT_32, "32", BIT_64, "64");

    private static final HashAlgorithm                HASH_ALGORITHM         = HashAlgorithm.NONE;
    private static final String                       HASH_URI               = "";
    private static final SignatureType                SIGNATURE_TYPE         = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM    = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI          = "";
    private static final String                       OFFICIAL_URI           = "https://packages.debian.org/search?keywords=openjdk";


    @Override public Distro getDistro() { return Distro.DEBIAN; }

    @Override public String getName() { return Distro.DEBIAN.getUiString(); }

    @Override public String getPkgUrl() { return CDN_URL; }

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

    @Override public List<String> getSynonyms() { return List.of("debian", "DEBIAN", "Debian"); }

    @Override public List<Semver> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.DEBIAN.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }

    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem, final Architecture architecture, final Bitness bitness, final ArchiveType archiveType,
                                         final PackageType packageType, final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        return CDN_URL;
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem, final Architecture architecture, final Bitness bitness,
                                    final ArchiveType archiveType, final PackageType packageType, final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport, final boolean onlyNewPkgs) {
        return new ArrayList<>();
    }

    public List<Pkg> getAllPackages() {
        List<Pkg> pkgs = new ArrayList<>();
        int latestMajorVersion = CacheManager.INSTANCE.getMajorVersions().stream().max(Comparator.comparing(MajorVersion::getAsInt)).get().getAsInt();
        for (int i = 7 ; i < latestMajorVersion ; i++) {
            final String cdnUrl = CDN_URL + "openjdk-"+ i + "/";
            try {
                pkgs.addAll(getAllPackagesFromCDN(cdnUrl));
            } catch (Exception e) {
                LOGGER.debug("Error fetching packages from {} url {}. {}", getName(), cdnUrl, e.getMessage());
            }
        }
        return pkgs;
    }

    public List<Pkg> getAllPackagesFromCDN(final String cdnUrl) {
        List<Pkg> pkgs = new ArrayList<>();
        try {
            final HttpResponse<String> response = Helper.get(cdnUrl);
            if (null == response) { return pkgs; }
            final String html = response.body();
            if (html.isEmpty()) { return pkgs; }

            final List<String> fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));
            for (String href : fileHrefs) {
                String filename = Helper.getFileNameFromText(href);
                if (!filename.endsWith("deb")) { continue; }

                DEB_PKG_MATCHER.reset(filename);
                final List<MatchResult> results     = DEB_PKG_MATCHER.results().collect(Collectors.toList());
                final int               noOfResults = results.size();
                if (noOfResults > 0) {
                    MatchResult   result        = results.get(0);
                    VersionNumber versionNumber = VersionNumber.fromText(result.group(4));
                    Architecture  architecture  = Architecture.fromText(result.group(22));
                    PackageType   packageType   = PackageType.fromText(result.group(3));

                    TermOfSupport termOfSupport = Helper.getTermOfSupport(versionNumber);
                    //String        downloadLink  = cdnUrl + filename;

                    Pkg pkg = new Pkg();
                    pkg.setDistribution(Distro.DEBIAN.get());
                    pkg.setVersionNumber(versionNumber);
                    pkg.setJavaVersion(versionNumber);
                    pkg.setDistributionVersion(versionNumber);

                    if (PackageType.NOT_FOUND == packageType) { packageType = PackageType.JDK; }
                    pkg.setPackageType(packageType);

                    if (Architecture.NOT_FOUND == architecture) { continue; }
                    pkg.setArchitecture(architecture);
                    pkg.setBitness(architecture.getBitness());

                    FPU fpu;
                    if (ARMEL == architecture) {
                        fpu = FPU.SOFT_FLOAT;
                    } else if (ARMHF == architecture) {
                        fpu = FPU.HARD_FLOAT;
                    } else {
                        fpu = FPU.UNKNOWN;
                    }
                    pkg.setFPU(fpu);
                    pkg.setArchiveType(ArchiveType.DEB);
                    pkg.setOperatingSystem(OperatingSystem.LINUX);
                    pkg.setReleaseStatus(ReleaseStatus.GA);
                    pkg.setTermOfSupport(termOfSupport);
                    pkg.setFileName(filename);

                    //pkg.setDirectDownloadUri(downloadLink);
                    pkg.setDownloadSiteUri(cdnUrl);
                    pkg.setDirectlyDownloadable(false);

                    pkg.setJavaFXBundled(false);

                    pkg.setFreeUseInProduction(Boolean.TRUE);

                    pkgs.add(pkg);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error fetching packages from Debian CDN. {}", e.getMessage());
        }
        return pkgs;
    }
}
