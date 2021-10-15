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
import io.foojay.api.pkg.MajorVersion;
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
import io.foojay.api.util.OutputFormat;
import io.foojay.api.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.foojay.api.pkg.PackageType.JDK;
import static io.foojay.api.pkg.PackageType.JRE;
import static io.foojay.api.pkg.ReleaseStatus.EA;
import static io.foojay.api.pkg.ReleaseStatus.GA;


public class Corretto implements Distribution {
    private static final Logger                       LOGGER                  = LoggerFactory.getLogger(Corretto.class);

    private static final Pattern                      FILENAME_PREFIX_PATTERN = Pattern.compile("(java-(\\d+)?\\.?(\\d+)?\\.?(\\d+)?\\.?-)|(amazon-corretto-)(jdk_|devel-)?");
    private static final Matcher                      FILENAME_PREFIX_MATCHER = FILENAME_PREFIX_PATTERN.matcher("");
    private static final String                       PACKAGE_URL             = "https://api.github.com/repos/corretto/";// jdk8: corretto-8, jdk11: corretto-11, jdk15,jdk16: corretto-jdk

    private static final String                       PREFIX                  = "amazon-corretto-";

    // URL parameters
    private static final String                       ARCHITECTURE_PARAM      = "";
    private static final String                       OPERATING_SYSTEM_PARAM  = "";
    private static final String                       ARCHIVE_TYPE_PARAM      = "";
    private static final String                       PACKAGE_TYPE_PARAM      = "";
    private static final String                       RELEASE_STATUS_PARAM    = "";
    private static final String                       SUPPORT_TERM_PARAM      = "";
    private static final String                       BITNESS_PARAM           = "";

    private static final HashAlgorithm                HASH_ALGORITHM          = HashAlgorithm.NONE;
    private static final String                       HASH_URI                = "";
    private static final SignatureType                SIGNATURE_TYPE          = SignatureType.NONE;
    private static final HashAlgorithm                SIGNATURE_ALGORITHM     = HashAlgorithm.NONE;
    private static final String                       SIGNATURE_URI           = "";//https://corretto.aws/downloads/resources/11.0.6.10.1/B04F24E3.pub";
    private static final String                       OFFICIAL_URI            = "https://aws.amazon.com/de/corretto/";


    @Override public Distro getDistro() { return Distro.CORRETTO; }

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
        return List.of("corretto", "CORRETTO", "Corretto");
    }

    @Override public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Distro.CORRETTO.get().equals(pkg.getDistribution()))
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }


    @Override public String getUrlForAvailablePkgs(final VersionNumber versionNumber,
                                                   final boolean latest, final OperatingSystem operatingSystem,
                                                   final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                                   final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PACKAGE_URL);
        int featureVersion = versionNumber.getFeature().getAsInt();

        if (TermOfSupport.LTS == versionNumber.getMajorVersion().getTermOfSupport()) {
            queryBuilder.append("corretto-").append(featureVersion).append("/releases").append("?per_page=100");
        } else {
            queryBuilder.append("corretto-jdk").append("/releases").append("?per_page=100");
        }

        LOGGER.debug("Query string for {}: {}", this.getName(), queryBuilder);

        return queryBuilder.toString();
    }

    @Override public List<Pkg> getPkgFromJson(final JsonObject jsonObj, final VersionNumber versionNumber, final boolean latest, final OperatingSystem operatingSystem,
                                              final Architecture architecture, final Bitness bitness, final ArchiveType archiveType, final PackageType packageType,
                                              final Boolean javafxBundled, final ReleaseStatus releaseStatus, final TermOfSupport termOfSupport) {
        List<Pkg> pkgs = new ArrayList<>();

        if (versionNumber.getMajorVersion().getAsInt() < 8) { return pkgs; }

        TermOfSupport supTerm = Helper.getTermOfSupport(versionNumber);
        supTerm = TermOfSupport.MTS == supTerm ? TermOfSupport.STS : supTerm;

        if (jsonObj.has("message")) {
            LOGGER.debug("Github rate limit reached when trying to get packages for Corretto {}", versionNumber);
            return pkgs;
        }
        if (jsonObj.has("prerelease") && jsonObj.get("prerelease").getAsBoolean()) {
            LOGGER.debug("Corretto version {} is a prerelease", versionNumber.toString(OutputFormat.REDUCED_COMPRESSED, true, true));
            return pkgs;
        }
        if (!jsonObj.has("body")) {
            LOGGER.debug("No body element found in response for Corretto version {}", versionNumber.toString(OutputFormat.REDUCED_COMPRESSED, true, true));
            return pkgs;
        }
        String bodyText = jsonObj.get("body").getAsString();

        Set<Pair<String,String>> pairsFound         = Helper.getPackageTypeAndFileUrlFromString(bodyText);
        Map<String,String>       signatureUrisFound = Helper.getCorrettoSignatureUris(bodyText);

        for (Pair<String, String> pair : pairsFound) {
            final PackageType pkgType = PackageType.fromText(pair.getKey());
            final String      url     = pair.getValue();
            if (url.contains("latest_checksum")) { continue; }

            Pkg pkg = new Pkg();

            String filename = Helper.getFileNameFromText(url);

            String withoutPrefix = FILENAME_PREFIX_MATCHER.reset(filename).replaceAll("");

            pkg.setDistribution(Distro.CORRETTO.get());
            pkg.setFileName(filename);
            pkg.setDirectDownloadUri(url);
            if (signatureUrisFound.containsKey(filename)) {
                pkg.setSignatureUri(signatureUrisFound.get(filename));
            }

            ArchiveType ext = ArchiveType.getFromFileName(filename);
            if (ArchiveType.NONE != archiveType && ext != archiveType) { continue; }
            pkg.setArchiveType(ext);

            // Corretto Version: 11.0.XX.YY.Z, 15.0.XX.YY.Z
            // XX -> OpenJDK 11 update number
            // YY -> OpenJDK 11 build number
            // Z  -> Corretto specific revision number

            // Corretto Version: 8.XXX.YY.Z
            // XXX -> OpenJDK 8 update number
            // YY  -> OpenJDK 8 build number
            // Z   -> Corretto specific revision number

            VersionNumber correttoNumber = VersionNumber.fromText(withoutPrefix);
            VersionNumber vNumber;
            if (correttoNumber.getFeature().getAsInt() > 8) {
                vNumber = new VersionNumber(correttoNumber.getFeature().getAsInt(), 0, correttoNumber.getUpdate().getAsInt(), 0);
                vNumber.setBuild(correttoNumber.getPatch().getAsInt());
            } else {
                vNumber = new VersionNumber(correttoNumber.getFeature().getAsInt(), 0, correttoNumber.getInterim().getAsInt(), 0);
                vNumber.setBuild(correttoNumber.getUpdate().getAsInt());
                pkg.setJavaFXBundled(true);
            }

            if (latest) {
                if (versionNumber.getFeature().getAsInt() != vNumber.getFeature().getAsInt()) { continue; }
            } /*else {
                if (!versionNumber.equals(vNumber)) { continue; }
            }*/
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);

            pkg.setDistributionVersion(correttoNumber);

            pkg.setTermOfSupport(supTerm);

            switch (packageType) {
                case JDK:
                    if (withoutPrefix.contains(Constants.JRE_POSTFIX)) { continue; }
                    pkg.setPackageType(JDK);
                    break;
                case JRE:
                    if (!withoutPrefix.contains(Constants.JRE_POSTFIX)) { continue; }
                    pkg.setPackageType(JRE);
                    break;
                case NONE:
                    pkg.setPackageType(pkgType);
                    break;
            }


            if (releaseStatus == ReleaseStatus.NONE || releaseStatus == GA) {
                pkg.setReleaseStatus(GA);
            } else if (releaseStatus == EA) {
                continue;
            }

            Architecture arch = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                             .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                             .findFirst()
                                                             .map(Entry::getValue)
                                                             .orElse(Architecture.NONE);
            if (Architecture.NONE == arch) {
                LOGGER.debug("Architecture not found in Corretto for filename: {}", filename);
                continue;
            }

            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());


            OperatingSystem os = Constants.OPERATING_SYSTEM_LOOKUP.entrySet().stream()
                                                                  .filter(entry -> withoutPrefix.contains(entry.getKey()))
                                                                  .findFirst()
                                                                  .map(Entry::getValue)
                                                                  .orElse(OperatingSystem.NONE);
            if (OperatingSystem.NONE == os) {
                switch (pkg.getArchiveType()) {
                    case DEB:
                    case RPM:
                    case TAR_GZ:
                        os = OperatingSystem.LINUX;
                        break;
                    case MSI:
                    case ZIP:
                        os = OperatingSystem.WINDOWS;
                        break;
                    case DMG:
                    case PKG:
                        os = OperatingSystem.MACOS;
                        break;
                }
            }
            if (OperatingSystem.NONE == os) {
                LOGGER.debug("Operating System not found in Corretto for filename: {}", filename);
                continue;
            }
            pkg.setOperatingSystem(os);

            pkg.setFreeUseInProduction(Boolean.TRUE);

            pkgs.add(pkg);
        }

        return pkgs;
    }

    public List<Pkg> getAllPkgs() {
        final String gaPackageUrl = "https://docs.aws.amazon.com/corretto/latest/corretto-";
        List<Pkg> pkgs = new ArrayList<>();
        for (MajorVersion majorVersion : CacheManager.INSTANCE.getMajorVersions()) {
            if (majorVersion.getAsInt() < 8) { continue; }
            StringBuilder queryBuilder = new StringBuilder().append(gaPackageUrl).append(majorVersion.getAsInt()).append("-ug/downloads-list.html");
            String query = queryBuilder.toString();
            try {
                final HttpResponse<String> response = Helper.get(query);
                if (null == response) { continue; }
                final String htmlAllJDKs  = response.body();
                if (!htmlAllJDKs.isEmpty()) {
                    pkgs.addAll(getAllPkgsFromHtml(htmlAllJDKs));
                }
            } catch (Exception e) {
                LOGGER.error("Error fetching all packages from {}. {}", getName(), e);
            }
        }
        return pkgs;
    }

    public List<Pkg> getAllPkgsFromHtml(final String html) {
        List<Pkg> pkgs = new ArrayList<>();
        if (null == html || html.isEmpty()) { return pkgs; }
        List<String> fileHrefs = new ArrayList<>(Helper.getFileHrefsFromString(html));

        for (String fileHref : fileHrefs) {
            if (fileHref.contains("latest_checksum")) { continue; }

            String filename = Helper.getFileNameFromText(fileHref.replaceAll("\"", ""));

            Pkg pkg = new Pkg();
            pkg.setDistribution(Distro.CORRETTO.get());

            String withoutPrefix = filename.replace(PREFIX, "");

            pkg.setJavaFXBundled(false);
            pkg.setPackageType(withoutPrefix.contains("jre") ? JRE : JDK);

            Architecture architecture = Constants.ARCHITECTURE_LOOKUP.entrySet().stream()
                                                                     .filter(entry -> filename.contains(entry.getKey()))
                                                                     .findFirst()
                                                                     .map(Entry::getValue)
                                                                     .orElse(Architecture.NOT_FOUND);
            if (Architecture.NOT_FOUND == architecture) {
                LOGGER.debug("Architecture not found in Corretto for filename: {}", filename);
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
                LOGGER.debug("Operating System not found in Corretto for filename: {}", filename);
                continue;
            }
            pkg.setOperatingSystem(operatingSystem);

            ArchiveType archiveType = Constants.ARCHIVE_TYPE_LOOKUP.entrySet().stream()
                                                                   .filter(entry -> filename.contains(entry.getKey()))
                                                                   .findFirst()
                                                                   .map(Entry::getValue)
                                                                   .orElse(ArchiveType.NOT_FOUND);
            if (ArchiveType.NOT_FOUND == archiveType) {
                LOGGER.debug("Archive Type not found in Corretto for filename: {}", filename);
                continue;
            }
            pkg.setArchiveType(archiveType);

            // No support for Feature.Interim.Update.Path but only Major.Minor.Update => setPatch(0)
            VersionNumber vNumber = VersionNumber.fromText(withoutPrefix);
            VersionNumber versionNumber = vNumber;
            versionNumber.setPatch(0);
            pkg.setVersionNumber(versionNumber);
            pkg.setJavaVersion(versionNumber);
            pkg.setDistributionVersion(vNumber);
            pkg.setReleaseStatus(GA);

            pkg.setTermOfSupport(Helper.getTermOfSupport(versionNumber));

            pkg.setDirectlyDownloadable(true);

            pkg.setFileName(Helper.getFileNameFromText(filename));
            pkg.setDirectDownloadUri(fileHref);

            pkg.setFreeUseInProduction(Boolean.TRUE);

            pkgs.add(pkg);
        }

        pkgs.forEach(pkg -> {
            String signatureUri = pkg.getDirectDownloadUri() + ".sig";
            if (html.contains(signatureUri)) {
                pkg.setSignatureUri(signatureUri);
            }
        });

        return pkgs;
    }
}
