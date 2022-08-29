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

package io.foojay.api.pkg;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Match;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.scopes.BuildScope;
import eu.hansolo.jdktools.scopes.Scope;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.CacheManager;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.foojay.api.util.Constants.COLON;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.INDENT;
import static io.foojay.api.util.Constants.INDENTED_QUOTES;
import static io.foojay.api.util.Constants.NEW_LINE;
import static io.foojay.api.util.Constants.QUOTES;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_OPEN;
import static java.util.stream.Collectors.toSet;


/**
 * Maintainance information is taken from: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
 */

public class MajorVersion implements Comparable<MajorVersion> {
    public  static final String        FIELD_MAJOR_VERSION     = "major_version";
    public  static final String        FIELD_TERM_OF_SUPPORT   = "term_of_support";
    public  static final String        FIELD_MAINTAINED        = "maintained";
    public  static final String        FIELD_EARLY_ACCESS_ONLY = "early_access_only";
    public  static final String        FIELD_RELEASE_STATUS    = "release_status";
    public  static final String        FIELD_SCOPE             = "scope";
    public  static final String        FIELD_VERSIONS          = "versions";
    private        final int           majorVersion;
    private        final TermOfSupport termOfSupport;
    private        final boolean       maintained;
    private              BuildScope    scope;


    public MajorVersion(final int majorVersion) {
        this(majorVersion, Helper.getTermOfSupport(majorVersion));
    }
    public MajorVersion(final int majorVersion, final TermOfSupport termOfSupport) {
        if (majorVersion <= 0) { throw new IllegalArgumentException("Major version cannot be <= 0"); }
        this.majorVersion    = majorVersion;
        this.termOfSupport   = termOfSupport;
        if (CacheManager.INSTANCE.maintainedMajorVersions.containsKey(majorVersion)) {
            this.maintained = CacheManager.INSTANCE.maintainedMajorVersions.get(majorVersion);
        } else {
            this.maintained = false;
        }
        this.scope = BuildScope.BUILD_OF_OPEN_JDK;
    }
    public MajorVersion(final int majorVersion, final TermOfSupport termOfSupport, final boolean maintained) {
        this(majorVersion, termOfSupport, maintained, BuildScope.BUILD_OF_OPEN_JDK);
    }
    public MajorVersion(final int majorVersion, final TermOfSupport termOfSupport, final boolean maintained, final BuildScope scope) {
        if (majorVersion <= 0) { throw new IllegalArgumentException("Major version cannot be <= 0"); }
        this.majorVersion  = majorVersion;
        this.termOfSupport = termOfSupport;
        this.maintained    = maintained;
        this.scope         = scope;
    }
    public MajorVersion(final String jsonText) {
        if (null == jsonText || jsonText.isEmpty()) { throw new IllegalArgumentException("Json text cannot be null or empty"); }
        final Gson       gson = new Gson();
        final JsonObject json = gson.fromJson(jsonText, JsonObject.class);

        this.majorVersion  = json.get(FIELD_MAJOR_VERSION).getAsInt();
        this.termOfSupport = TermOfSupport.fromText(json.get(FIELD_TERM_OF_SUPPORT).getAsString());
        this.maintained    = json.get(FIELD_MAINTAINED).getAsBoolean();
        this.scope         = json.has(FIELD_SCOPE) ? (BuildScope) BuildScope.fromText(json.get(FIELD_SCOPE).getAsString()) : BuildScope.BUILD_OF_OPEN_JDK;
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
            return CacheManager.INSTANCE.getMajorVersions().stream().sorted(Comparator.comparingInt(MajorVersion::getAsInt).reversed()).findFirst().get();
        } else {
            return CacheManager.INSTANCE.getMajorVersions().stream().filter(majorVersion -> !majorVersion.getVersions().isEmpty()).sorted(Comparator.comparingInt(MajorVersion::getAsInt).reversed()).findFirst().get();
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
        return getMaintainedMajorVersions(BuildScope.BUILD_OF_OPEN_JDK);
    }
    public static List<MajorVersion> getMaintainedMajorVersions(final BuildScope scope) {
        return CacheManager.INSTANCE.getMajorVersions()
                                    .stream()
                                    .filter(majorVersion -> majorVersion.scope == scope)
                                    .filter(majorVersion -> majorVersion.isMaintained())
                                    .sorted(Comparator.comparing(MajorVersion::getVersionNumber).reversed())
                                    .collect(Collectors.toList());
    }

    public static MajorVersion[] getMaintainedMajorVersionsAsArray() {
        return getMaintainedMajorVersions().toArray(new MajorVersion[0]);
    }

    public static List<MajorVersion> getGeneralAvailabilityOnlyMajorVersions() {
        return getGeneralAvailabilityOnlyMajorVersions(BuildScope.BUILD_OF_OPEN_JDK);
    }
    public static List<MajorVersion> getGeneralAvailabilityOnlyMajorVersions(final BuildScope scope) {
        return CacheManager.INSTANCE.getMajorVersions()
                                    .stream()
                                    .filter(majorVersion -> majorVersion.scope == scope)
                                    .filter(majorVersion -> !majorVersion.getVersions().isEmpty())
                                    .collect(Collectors.toList());
    }

    public static List<MajorVersion> getEarlyAccessOnlyMajorVersions() {
        return getEarlyAccessOnlyMajorVersions(BuildScope.BUILD_OF_OPEN_JDK);
    }
    public static List<MajorVersion> getEarlyAccessOnlyMajorVersions(final BuildScope scope) {
        return CacheManager.INSTANCE.getMajorVersions()
                                    .stream()
                                    .filter(majorVersion -> majorVersion.scope == scope)
                                    .filter(majorVersion -> majorVersion.getVersionsIncludingEarlyAccess().size() == 1)
                                    .filter(majorVersion -> majorVersion.getVersionsOnlyEarlyAccess().size() == 1)
                                    .collect(Collectors.toList());
    }

    public static List<MajorVersion> getUsefulMajorVersions() {
        return getUsefulMajorVersions(BuildScope.BUILD_OF_OPEN_JDK);
    }
    public static List<MajorVersion> getUsefulMajorVersions(final BuildScope scope) {
        List<MajorVersion> usefulVersions  = new ArrayList<>();
        List<MajorVersion> majorGAVersions = getGeneralAvailabilityOnlyMajorVersions(scope);

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

    public static boolean isMaintainedMajorVersion(final MajorVersion majorVersion) {
        return getMaintainedMajorVersions().stream().filter(mv -> mv.getAsInt() == majorVersion.getAsInt()).count() > 0;
    }
    public static boolean isMaintainedMajorVersion(final int majorVersion) {
        return getMaintainedMajorVersions().stream().filter(mv -> mv.getAsInt() == majorVersion).count() > 0;
    }

    public static Optional<MajorVersion> getMax() {
        return getAllMajorVersions().stream().max(Comparator.comparing(MajorVersion::getAsInt));
    }

    // VersionNumber
    public VersionNumber getVersionNumber() { return new VersionNumber(majorVersion); }

    // Maintained
    public Boolean isMaintained() { return maintained; }

    // Release Status
    public ReleaseStatus getReleaseStatus() { return isEarlyAccessOnly() ? ReleaseStatus.EA : ReleaseStatus.GA; }

    public BuildScope getScope() { return scope; }
    public void setScope(final BuildScope scope) { this.scope = scope; }

    // Early Access only
    public Boolean isEarlyAccessOnly() {
        return getVersions().stream().filter(semver -> ReleaseStatus.EA == semver.getReleaseStatus()).count() == getVersions().size();
    }

    // Versions
    public List<Semver> getVersions(final List<Scope> scopes, final Match match) {
        final Match scopeMatch = (null == match || Match.NONE == match || Match.NOT_FOUND == match) ? Match.ANY : match;
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Match.ANY == scopeMatch ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(scopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .filter(pkg -> ReleaseStatus.GA == pkg.getReleaseStatus())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }
    public List<Semver> getVersions() {
        return getVersions(BuildScope.BUILD_OF_OPEN_JDK);
    }
    public List<Semver> getVersions(final BuildScope scope) {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> scope == BuildScope.BUILD_OF_OPEN_JDK ? Distro.getDistributionsBasedOnOpenJDK().contains(pkg.getDistribution().getDistro()) : Distro.getDistributionsBasedOnGraalVm().contains(pkg.getDistribution().getDistro()))
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .filter(pkg -> ReleaseStatus.GA == pkg.getReleaseStatus())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream()
                                             .sorted(Comparator.comparing(Semver::getVersionNumber).reversed())
                                             .collect(Collectors.toList());
    }

    public List<Semver> getVersionsOnlyEarlyAccess(final List<Scope> scopes, final Match match) {
        final Match scopeMatch = (null == match || Match.NONE == match || Match.NOT_FOUND == match) ? Match.ANY : match;
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Match.ANY == scopeMatch ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(scopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .filter(pkg -> ReleaseStatus.EA == pkg.getReleaseStatus())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }
    public List<Semver> getVersionsOnlyEarlyAccess() {
        return getVersionsOnlyEarlyAccess(BuildScope.BUILD_OF_OPEN_JDK);
    }
    public List<Semver> getVersionsOnlyEarlyAccess(final BuildScope scope) {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> scope == BuildScope.BUILD_OF_OPEN_JDK ? Distro.getDistributionsBasedOnOpenJDK().contains(pkg.getDistribution().getDistro()) : Distro.getDistributionsBasedOnGraalVm().contains(pkg.getDistribution().getDistro()))
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .filter(pkg -> ReleaseStatus.EA == pkg.getReleaseStatus())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }

    public List<Semver> getVersionsIncludingEarlyAccess(final List<Scope> scopes, final Match match) {
        final Match scopeMatch = (null == match || Match.NONE == match || Match.NOT_FOUND == match) ? Match.ANY : match;
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> Match.ANY == scopeMatch ? Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).stream().anyMatch(scopes.stream().collect(toSet())::contains) : Constants.SCOPE_LOOKUP.get(pkg.getDistribution().getDistro()).containsAll(scopes))
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }
    public List<Semver> getVersionsIncludingEarlyAccess() {
        return getVersionsIncludingEarlyAccess(BuildScope.BUILD_OF_OPEN_JDK);
    }
    public List<Semver> getVersionsIncludingEarlyAccess(final BuildScope scope) {
        return CacheManager.INSTANCE.pkgCache.getPkgs()
                                             .stream()
                                             .filter(pkg -> scope == BuildScope.BUILD_OF_OPEN_JDK ? Distro.getDistributionsBasedOnOpenJDK().contains(pkg.getDistribution().getDistro()) : Distro.getDistributionsBasedOnGraalVm().contains(pkg.getDistribution().getDistro()))
                                             .filter(pkg -> majorVersion == pkg.getVersionNumber().getFeature().getAsInt())
                                             .map(pkg -> pkg.getSemver())
                                             .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Semver::toString)))).stream().sorted(Comparator.comparing(Semver::getVersionNumber).reversed()).collect(Collectors.toList());
    }

    public String toString(final boolean includingEarlyAccess, final BuildScope scope) {
        final List<Semver> versions = includingEarlyAccess ? getVersionsIncludingEarlyAccess(scope) : getVersions(scope);
        final StringBuilder majorVersionMsgBuilder = new StringBuilder().append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                                                                        .append(INDENTED_QUOTES).append(FIELD_MAJOR_VERSION).append(QUOTES).append(COLON).append(majorVersion).append(COMMA_NEW_LINE)
                                                                        .append(INDENTED_QUOTES).append(FIELD_TERM_OF_SUPPORT).append(QUOTES).append(COLON).append(QUOTES).append(termOfSupport.name()).append(QUOTES).append(COMMA_NEW_LINE)
                                                                        .append(INDENTED_QUOTES).append(FIELD_MAINTAINED).append(QUOTES).append(COLON).append(isMaintained()).append(COMMA_NEW_LINE)
                                                                        .append(INDENTED_QUOTES).append(FIELD_EARLY_ACCESS_ONLY).append(QUOTES).append(COLON).append(isEarlyAccessOnly()).append(COMMA_NEW_LINE)
                                                                        .append(INDENTED_QUOTES).append(FIELD_RELEASE_STATUS).append(QUOTES).append(COLON).append(QUOTES).append(getReleaseStatus().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                                        .append(INDENTED_QUOTES).append(FIELD_SCOPE).append(QUOTES).append(COLON).append(QUOTES).append(this.scope.getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                                                                        .append(INDENTED_QUOTES).append(FIELD_VERSIONS).append(QUOTES).append(COLON).append(" ").append(SQUARE_BRACKET_OPEN).append(versions.isEmpty() ? "" : NEW_LINE);
        versions.forEach(versionNumber -> majorVersionMsgBuilder.append(INDENT).append(INDENTED_QUOTES).append(versionNumber).append(QUOTES).append(COMMA_NEW_LINE));
        if (!versions.isEmpty()) {
            majorVersionMsgBuilder.setLength(majorVersionMsgBuilder.length() - 2);
            majorVersionMsgBuilder.append(NEW_LINE)
                                  .append(INDENT).append(SQUARE_BRACKET_CLOSE).append(NEW_LINE);
        } else {
            majorVersionMsgBuilder.append(SQUARE_BRACKET_CLOSE).append(NEW_LINE);
        }

        return majorVersionMsgBuilder.append(CURLY_BRACKET_CLOSE)
                                     .toString();
    }

    @Override public int compareTo(final MajorVersion other) {
        return Integer.compare(majorVersion, other.majorVersion);
    }

    @Override public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        MajorVersion that = (MajorVersion) o;
        return majorVersion == that.majorVersion;
    }

    @Override public int hashCode() {
        return Objects.hash(majorVersion, termOfSupport, maintained, scope);
    }

    @Override public String toString() {
        return toString(false, BuildScope.BUILD_OF_OPEN_JDK);
    }
}
