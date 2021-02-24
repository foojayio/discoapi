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

package io.foojay.api.pkg;

import io.foojay.api.CacheManager;
import io.foojay.api.util.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 * Maintainance information is taken from: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
 */

public class MajorVersion {
    public  static final String        FIELD_MAJOR_VERSION   = "major_version";
    public  static final String        FIELD_TERM_OF_SUPPORT = "term_of_support";
    public  static final String        FIELD_MAINTAINED      = "maintained";
    public  static final String        FIELD_VERSIONS        = "versions";
    private        final int           majorVersion;
    private        final TermOfSupport termOfSupport;
    private        final boolean       maintained;


    public MajorVersion(final int majorVersion) {
        this(majorVersion, Helper.getTermOfSupport(majorVersion));
    }
    public MajorVersion(final int majorVersion, final TermOfSupport termOfSupport) {
        if (majorVersion <= 0) { throw new IllegalArgumentException("Major version cannot be <= 0"); }
        this.majorVersion  = majorVersion;
        this.termOfSupport = termOfSupport;
        if (CacheManager.INSTANCE.maintainedMajorVersions.containsKey(majorVersion)) {
            this.maintained = CacheManager.INSTANCE.maintainedMajorVersions.get(majorVersion);
        } else {
            this.maintained = false;
        }
    }


    public int getAsInt() { return majorVersion; }

    public TermOfSupport getTermOfSupport() { return termOfSupport; }

    public static List<MajorVersion> getAllMajorVersions() {
        return CacheManager.INSTANCE.getMajorVersions();
    }

    // Latest releases
    public static int getLatestAsInt(final boolean includingEa) {
        return getLatest(includingEa).getAsInt();
    }
    public static MajorVersion getLatest(final boolean includingEa) {
        if (includingEa) {
        return CacheManager.INSTANCE.getMajorVersions().get(0);
        } else {
            return CacheManager.INSTANCE.getMajorVersions().stream().filter(majorVersion -> !majorVersion.getVersions().isEmpty()).findFirst().get();
        }
    }

    public static MajorVersion getLatest(final TermOfSupport termOfSupport, final boolean includingEa) {
        int featureVersion = 1;
        for (MajorVersion majorVersion : CacheManager.INSTANCE.getMajorVersions()) {
            if (includingEa) {
            if (termOfSupport == Helper.getTermOfSupport(majorVersion.getAsInt())) {
                featureVersion = majorVersion.getAsInt();
                break;
            }
            } else {
                if (termOfSupport == Helper.getTermOfSupport(majorVersion.getAsInt()) && !majorVersion.getVersions().isEmpty()) {
                    featureVersion = majorVersion.getAsInt();
                    break;
                }
            }
        }
        return new MajorVersion(featureVersion);
    }

    public static MajorVersion getLatestSts(final boolean includingEa) {
        return getLatest(TermOfSupport.STS, includingEa);
    }

    public static MajorVersion getLatestMts(final boolean includingEa) {
        return getLatest(TermOfSupport.MTS, includingEa);
    }

    public static MajorVersion getLatestLts(final boolean includingEa) {
        return getLatest(TermOfSupport.LTS, includingEa);
    }

    // Get maintained versions
    public static List<MajorVersion> getMaintainedMajorVersions() {
        return CacheManager.INSTANCE.getMajorVersions()
                                    .stream()
                                    .filter(majorVersion -> majorVersion.isMaintained())
                                    .sorted(Comparator.comparing(MajorVersion::getVersionNumber).reversed())
                                    .collect(Collectors.toList());
    }

    public static MajorVersion[] getMaintainedMajorVersionsAsArray() {
        return getMaintainedMajorVersions().toArray(new MajorVersion[0]);
    }

    public static List<MajorVersion> getGeneralAvailabilityOnlyMajorVersions() {
        return CacheManager.INSTANCE.getMajorVersions()
                                    .stream()
                                    .filter(majorVersion -> !majorVersion.getVersions().isEmpty())
                                    .collect(Collectors.toList());
    }

    public static List<MajorVersion> getEarlyAccessOnlyMajorVersions() {
        return CacheManager.INSTANCE.getMajorVersions()
                                    .stream()
                                    .filter(majorVersion -> majorVersion.getVersionsIncludingEarlyAccess().size() == 1)
                                    .filter(majorVersion -> majorVersion.getVersionsOnlyEarlyAccess().size() == 1)
                                    .collect(Collectors.toList());
    }

    public static List<MajorVersion> getUsefulMajorVersions() {
        List<MajorVersion> usefulVersions  = new ArrayList<>();
        List<MajorVersion> majorGAVersions = getGeneralAvailabilityOnlyMajorVersions();

        // Add version 8
        MajorVersion version8 = majorGAVersions.stream().filter(majorVersion -> majorVersion.getAsInt() == 8).findFirst().get();
        usefulVersions.add(version8);

        // Add latest LTS version and if available also the previous LTS version
        List<MajorVersion> ltsVersions = majorGAVersions.stream()
                                                        .filter(majorVersion -> majorVersion.getAsInt() > 8)
                                                        .filter(majorVersion -> TermOfSupport.LTS == majorVersion.getTermOfSupport())
                                                        .sorted(Comparator.comparing(MajorVersion::getAsInt).reversed())
                                                        .collect(Collectors.toList());
        if (!ltsVersions.isEmpty()) {
            usefulVersions.add(ltsVersions.get(0));
            if (ltsVersions.size() > 2) {
                usefulVersions.add(ltsVersions.get(1));
            }
        }

        // Added latest STS or MTS version if it is larger than latest LTS version
        List<MajorVersion> stsMtsVersions = majorGAVersions.stream()
                                                           .filter(majorVersion -> majorVersion.getAsInt() > 8)
                                                           .filter(majorVersion -> TermOfSupport.LTS != majorVersion.getTermOfSupport())
                                                           .filter(majorVersion -> usefulVersions.stream()
                                                                                                 .filter(usefulVersion -> majorVersion.getAsInt() > usefulVersion.getAsInt())
                                                                                                 .count() > 0)
                                                           .sorted(Comparator.comparing(MajorVersion::getAsInt).reversed())
                                                           .collect(Collectors.toList());
        if (!stsMtsVersions.isEmpty()) {
            usefulVersions.add(stsMtsVersions.get(0));
        }

        Collections.sort(usefulVersions, Comparator.comparing(MajorVersion::getAsInt).reversed());

        return usefulVersions;
    }

    // VersionNumber
    public VersionNumber getVersionNumber() { return new VersionNumber(majorVersion); }

    // Maintained
    public Boolean isMaintained() { return maintained; }

    // Versions
    public List<SemVer> getVersions() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .filter(pkg -> ReleaseStatus.GA == pkg.getReleaseStatus())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }

    public List<SemVer> getVersionsOnlyEarlyAccess() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .filter(pkg -> ReleaseStatus.EA == pkg.getReleaseStatus())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }

    public List<SemVer> getVersionsIncludingEarlyAccess() {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(SemVer::toString)))).stream().sorted(Comparator.comparing(SemVer::getVersionNumber).reversed()).collect(Collectors.toList());
    }

    public String toString(final boolean includingEarlyAccess) {
        final List<SemVer> versions = includingEarlyAccess ? getVersionsIncludingEarlyAccess() : getVersions();
        final StringBuilder majorVersionMsgBuilder = new StringBuilder().append("{\n")
                                                                        .append("  \"").append(FIELD_MAJOR_VERSION).append("\"").append(":").append(majorVersion).append(",\n")
                                                                        .append("  \"").append(FIELD_TERM_OF_SUPPORT).append("\"").append(":").append("\"").append(termOfSupport.name()).append("\"").append(",\n")
                                                                        .append("  \"").append(FIELD_MAINTAINED).append("\"").append(":").append(isMaintained()).append(",\n")
                                                                        .append("  \"").append(FIELD_VERSIONS).append("\"").append(": [").append(versions.isEmpty() ? "" : "\n");
        versions.forEach(versionNumber -> majorVersionMsgBuilder.append("    \"").append(versionNumber).append("\"").append(",\n"));
        if (!versions.isEmpty()) {
            majorVersionMsgBuilder.setLength(majorVersionMsgBuilder.length() - 2);
            majorVersionMsgBuilder.append("\n")
                                  .append("  ]\n");
        } else {
            majorVersionMsgBuilder.append("]\n");
        }

        return majorVersionMsgBuilder.append("}")
                                     .toString();
    }

    @Override public String toString() {
        return toString(false);
    }
}
