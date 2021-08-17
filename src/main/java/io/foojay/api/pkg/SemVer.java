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
 * You should have received a copy of the GNU General Public License
 * along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.pkg;

import io.foojay.api.util.Comparison;
import io.foojay.api.util.Error;
import io.foojay.api.util.Helper;
import io.foojay.api.util.OutputFormat;
import io.foojay.api.util.SemVerParser;
import io.foojay.api.util.SemVerParsingResult;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SemVer implements Comparable<SemVer> {
    public static final Pattern EA_PATTERN           = Pattern.compile("(ea|EA)((\\.|\\+|\\-)([0-9]+))?");
    public static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("\\+?(b|B)([0-9]+)");

    private VersionNumber versionNumber;
    private ReleaseStatus releaseStatus;
    private String        pre;
    private String        preBuild;
    private String        metadata;
    private Comparison    comparison;


    public SemVer(final VersionNumber versionNumber) {
        this(versionNumber, versionNumber.getReleaseStatus() != null && versionNumber.getReleaseStatus().isPresent() ? versionNumber.getReleaseStatus().get() : ReleaseStatus.GA, versionNumber.getReleaseStatus().isPresent() ? ReleaseStatus.EA == versionNumber.getReleaseStatus().get() ? "-ea" : "" : "", (versionNumber.getBuild() != null && versionNumber.getBuild().isPresent() && versionNumber.getBuild().getAsInt() > 0) ? "+" + versionNumber.getBuild().getAsInt() : "");
    }
    public SemVer(final VersionNumber versionNumber, final ReleaseStatus releaseStatus) {
        this(versionNumber, releaseStatus, ReleaseStatus.EA == releaseStatus ? "ea" : "", "");
    }
    public SemVer(final VersionNumber versionNumber, final String pre, final String metadata) {
        this(versionNumber, (null != pre && !pre.isEmpty()) ? ReleaseStatus.EA : ReleaseStatus.GA, pre, metadata);
    }
    public SemVer(final VersionNumber versionNumber, final ReleaseStatus releaseStatus, final String metadata) {
        this(versionNumber, releaseStatus, ReleaseStatus.EA == releaseStatus ? "ea" : "", metadata);
    }
    public SemVer(final VersionNumber versionNumber, final ReleaseStatus releaseStatus, final String pre, final String metadata) {
        this.versionNumber = versionNumber;
        this.releaseStatus = versionNumber.getReleaseStatus().isPresent() ? versionNumber.getReleaseStatus().get() : releaseStatus;
        this.pre           = null == pre      ? ReleaseStatus.EA == releaseStatus ? "-ea": "" : pre;
        this.metadata      = null == metadata ? "" : metadata;
        this.comparison    = Comparison.EQUAL;
        this.preBuild      = "";

        if (versionNumber.getBuild().isPresent() && versionNumber.getBuild().getAsInt() > 0) {
            this.preBuild = Integer.toString(versionNumber.getBuild().getAsInt());
        }

        if (this.preBuild.isEmpty() && !metadata.isEmpty()) {
            if (Helper.isPositiveInteger(this.metadata)) {
                this.preBuild = this.metadata;
                Integer build = Integer.valueOf(this.preBuild);
                if (build > 0) {
                    this.versionNumber.setBuild(build);
                }
            }
        }

        //if (this.versionNumber.getReleaseStatus().isPresent() && this.versionNumber.getReleaseStatus().get() != this.releaseStatus) {
        //    this.versionNumber.setReleaseStatus(this.releaseStatus);
        //}

        // Extract early access preBuild
        if (null != this.pre) {
            final Matcher           eaMatcher = EA_PATTERN.matcher(this.pre);
            final List<MatchResult> eaResults = eaMatcher.results().collect(Collectors.toList());
            if (eaResults.size() > 0) {
                final MatchResult eaResult = eaResults.get(0);
                if (null != eaResult.group(1)) {
                    this.versionNumber.setReleaseStatus(ReleaseStatus.EA);
                    if (null != eaResult.group(4)) {
                        this.preBuild = !eaResult.group(4).equals("0") ? eaResult.group(4) : "";
                        if (null == this.versionNumber.getBuild() || this.versionNumber.getBuild().isEmpty()) {
                            if (!this.preBuild.isEmpty()) {
                                Integer build = Integer.parseInt(this.preBuild);
                                if (build > 0) {
                                    this.versionNumber.setBuild(build);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (null != this.pre && !this.pre.isEmpty() && !this.pre.startsWith("+") && !this.pre.startsWith("-")) {
            this.pre = "-" + pre;
        }
        if (null != this.metadata && !this.metadata.isEmpty() && !this.metadata.startsWith("-") && !this.metadata.startsWith("+")) {
            this.metadata = "+" + metadata;
        }

        if (!this.pre.isEmpty() && !this.pre.startsWith("-")) { throw new IllegalArgumentException("pre-release argument has to start with \"-\""); }
        if (!this.metadata.isEmpty() && !this.metadata.startsWith("+")) { throw new IllegalArgumentException("metadata argument has to start with \"+\""); }
        if (ReleaseStatus.EA == this.releaseStatus && !this.pre.isEmpty() && !this.pre.toLowerCase().startsWith("-ea")) { throw new IllegalArgumentException("ReleaseStatus and pre-release argument cannot be different"); }
        if (ReleaseStatus.GA == this.releaseStatus && !this.pre.isEmpty() && this.pre.toLowerCase().startsWith("-ea")) { throw new IllegalArgumentException("ReleaseStatus and pre-release argument cannot be different"); }

        // Extract metadata e.g. build number
        if (null != this.metadata) {
            final Matcher           buildNumberMatcher = BUILD_NUMBER_PATTERN.matcher(this.metadata);
            final List<MatchResult> buildNumberResults = buildNumberMatcher.results().collect(Collectors.toList());
            if (buildNumberResults.size() > 0) {
                final MatchResult buildNumberResult = buildNumberResults.get(0);
                if (null != buildNumberResult.group(1)) {
                    if (null != buildNumberResult.group(2)) {
                        if (null == this.versionNumber.getBuild() || this.versionNumber.getBuild().isEmpty()) {
                            Integer build = Integer.parseInt(buildNumberResult.group(2));
                            if (build > 0) {
                                this.versionNumber.setBuild(build);
                                this.preBuild = Integer.toString(build);
                            }
                        }
                    }
                }
            }
        }
    }


    public VersionNumber getVersionNumber() { return versionNumber; }

    public int getFeature() { return versionNumber.getFeature().isPresent() ? versionNumber.getFeature().getAsInt() : 0; }
    public void setFeature(final int feature) { versionNumber.setFeature(feature); }

    public int getInterim() { return versionNumber.getInterim().isPresent() ? versionNumber.getInterim().getAsInt() : 0; }
    public void setInterim(final int interim) { versionNumber.setInterim(interim); }

    public int getUpdate() { return versionNumber.getUpdate().isPresent() ? versionNumber.getUpdate().getAsInt() : 0; }
    public void setUpdate(final int update) { versionNumber.setUpdate(update); }

    public int getPatch() { return versionNumber.getPatch().isPresent() ? versionNumber.getPatch().getAsInt() : 0; }
    public void setPatch(final int patch) { versionNumber.setPatch(patch); }

    public int getFifth() { return versionNumber.getFifth().isPresent() ? versionNumber.getFifth().getAsInt() : 0; }
    public void setFifth(final int fifth) { versionNumber.setFifth(fifth); }

    public int getSixth() { return versionNumber.getSixth().isPresent() ? versionNumber.getSixth().getAsInt() : 0; }
    public void setSixth(final int sixth) { versionNumber.setSixth(sixth); }

    public ReleaseStatus getReleaseStatus() { return releaseStatus; }

    public MajorVersion getMajorVersion() { return new MajorVersion(getFeature()); }

    public String getPre() { return pre; }
    public void setPre(final String pre) {
        if (null == pre && pre.length() > 0) {
            Error err = validatePrerelease(pre);
            if (null != err) {
                throw new IllegalArgumentException(err.getMessage());
            }
        }
        this.pre           = null == pre ? "" : pre;
        this.releaseStatus = (null == pre || pre.isEmpty()) ? ReleaseStatus.GA : ReleaseStatus.EA;
    }

    public String getPreBuild() { return preBuild; }
    public void setPreBuild(final String preBuild) {
        if (preBuild.matches("[0-9]+")) {
            if (preBuild.length() > 1 && (preBuild.startsWith("0") || preBuild.startsWith("b0"))) {
                throw new IllegalArgumentException("preBuild must be larger than 0");
            }
            this.preBuild = preBuild;
        } else {
            throw new IllegalArgumentException("Invalid preBuild: " + preBuild + ". It should only contain integers > 0.");
        }
    }

    public Integer getPreBuildAsInt() {
        if (null == preBuild || preBuild.isEmpty()) { return -1; }
        return Integer.valueOf(preBuild);
    }

    public String getMetadata() { return metadata; }
    public void setMetadata(final String metadata) {
        if (null == metadata && metadata.length() > 0) {
            Error err = validateMetadata(metadata);
            if (null != err) {
                throw new IllegalArgumentException(err.getMessage());
            }
        }
        this.metadata = metadata;
    }

    public Comparison getComparison() { return comparison; }
    public void setComparison(final Comparison comparison) { this.comparison = comparison; }

    public SemVer incSixth() {
        SemVer vNext = SemVer.this;
        if (null != pre && !pre.isEmpty()) {
            vNext.setMetadata("");
            vNext.setPre("");
        } else {
            vNext.metadata = "";
            vNext.pre      = "";
            vNext.setSixth(getSixth() + 1);
        }
        return vNext;
    }

    public SemVer incFifth() {
        SemVer vNext = SemVer.this;
        vNext.setMetadata("");
        vNext.setPre("");
        vNext.setSixth(0);
        vNext.setFifth(getFifth() + 1);
        return vNext;
    }

    public SemVer incPatch() {
        SemVer vNext = SemVer.this;
        vNext.setMetadata("");
        vNext.setPre("");
        vNext.setFifth(0);
        vNext.setPatch(getPatch() + 1);
        return vNext;
    }

    public SemVer incUpdate() {
        SemVer vNext = SemVer.this;
        vNext.setMetadata("");
        vNext.setPre("");
        vNext.setPatch(0);
        vNext.setUpdate(getUpdate() + 1);
        return vNext;
    }

    public SemVer incInterim() {
        SemVer vNext = SemVer.this;
        vNext.setMetadata("");
        vNext.setPre("");
        vNext.setPatch(0);
        vNext.setUpdate(0);
        vNext.setInterim(getInterim() + 1);
        return vNext;
    }

    public SemVer incFeature() {
        SemVer vNext = SemVer.this;
        vNext.setMetadata("");
        vNext.setPre("");
        vNext.setPatch(0);
        vNext.setUpdate(0);
        vNext.setInterim(0);
        vNext.setFeature(getFeature() + 1);
        return vNext;
    }

    public boolean lessThan(final SemVer semVer) {
        return compareTo(semVer) < 0;
    }

    public boolean greaterThan(final SemVer semVer) {
        return compareTo(semVer) > 0;
    }

    public boolean equalTo(final SemVer semVer) {
        return compareTo(semVer) == 0;
    }


    public static SemVerParsingResult fromText(final String text) {
        return SemVerParser.fromText(text);
    }


    private Error validatePrerelease(final String prerelease) {
        String[] eparts = prerelease.split(".");
        for (String p : eparts) {
            if (p.matches("[0-9]+")) {
                if (p.length() > 1 && p.startsWith("0")) {
                    return new Error("Segment starts with 0: " + p);
                }
            } else if (!p.matches("[a-zA-Z-0-9]+")) {
                return new Error("Invalid prerelease: " + prerelease);
            }
        }
        return null;
    }

    private Error validateMetadata(final String metadata) {
        String[] eparts = metadata.split(".");
        for (String p : eparts) {
            if (!p.matches("[a-zA-Z-0-9]")) {
                return new Error("Invalid metadata: " + metadata);
            }
        }
        return null;
    }

    @Override public int compareTo(final SemVer semVer) {
        int d;
        d = compareSegment(getFeature(), semVer.getFeature());
        if (d != 0) { return d; }

        d = compareSegment(getInterim(), semVer.getInterim());
        if (d != 0) { return d; }

        d = compareSegment(getUpdate(), semVer.getUpdate());
        if (d != 0) { return d; }

        d = compareSegment(getPatch(), semVer.getPatch());
        if (d != 0) { return d; }

        d = compareSegment(getFifth(), semVer.getFifth());
        if (d != 0) { return d; }

        d = compareSegment(getSixth(), semVer.getSixth());
        if (d != 0) { return d; }

        if (ReleaseStatus.EA == releaseStatus) {
            d = compareSegment(getPreBuildAsInt(), semVer.getPreBuildAsInt());
            if (d != 0) { return d; }
        }

        if ((null != pre && pre.isEmpty()) && (null != semVer.getPre() && semVer.getPre().isEmpty())) { return 0; }
        if (null == pre || pre.isEmpty()) { return 1; }
        if (null == semVer.getPre() || semVer.getPre().isEmpty()) { return -1; }

        return comparePrerelease(pre, semVer.getPre());
    }


    private int compareSegment(final int s1, final int s2) {
        if (s1 < s2) {
            return -1;
        } else if (s1 > s2) {
            return 1;
        } else {
            return 0;
        }
    }

    private int comparePrerelease(final String pre1, final String pre2) {
        String[] preParts1 = pre1.split("\\.");
        String[] preParts2 = pre2.split("\\.");

        int preParts1Length = preParts1.length;
        int preParts2Length = preParts2.length;

        int l = preParts2Length > preParts1Length ? preParts2Length : preParts1Length;

        for (int i = 0 ; i < l ; i++) {
            String tmp1 = "";
            if (i < preParts1Length) {
                tmp1 = preParts1[i];
            }

            String tmp2 = "";
            if (i < preParts2Length) {
                tmp2 = preParts2[i];
            }

            int d = comparePrePart(tmp1, tmp2);
            if (d != 0) { return d; }
        }
        return 0;
    }

    private int comparePrePart(final String prePart1, final String prePart2) {
        if (prePart1.equals(prePart2)) {
            return 0;
        }

        if (prePart1.isEmpty()) {
            return !prePart2.isEmpty() ? -1 : 1;
        }

        if (prePart2.isEmpty()) {
            return !prePart1.isEmpty() ? 1 : -1;
        }

        Integer prePart1Number;
        try {
            prePart1Number = Integer.valueOf(prePart1);
        } catch (NumberFormatException e) {
            prePart1Number = null;
        }
        Integer prePart2Number;
        try {
            prePart2Number = Integer.valueOf(prePart2);
        } catch (NumberFormatException e) {
            prePart2Number = null;
        }

        if (null != prePart1Number && null != prePart2Number) {
            return prePart1Number > prePart2Number ? 1 : -1;
        } else if (null != prePart2Number) {
            return -1;
        } else if (null != prePart1Number) {
            return 1;
        }
        return -1;
    }

    public String toString(final boolean javaFormat) {
        StringBuilder versionBuilder = new StringBuilder();
        versionBuilder.append(Comparison.EQUAL != comparison ? comparison.getOperator() : "");
        versionBuilder.append(versionNumber.toString(OutputFormat.REDUCED, javaFormat, false));
        if (ReleaseStatus.EA == releaseStatus) {
            versionBuilder.append("-ea");
        }

        if (null == preBuild || preBuild.isEmpty()) {
        if (null != metadata && !metadata.isEmpty()) {
            versionBuilder.append(metadata.startsWith("+") ? metadata : ("+" + metadata));
        }
        } else {
            if (preBuild.startsWith("+")) {
                preBuild = preBuild.substring(1);
            }
            try {
                Integer pb = Integer.valueOf(preBuild);
                if (pb > 0) {
                    versionBuilder.append("+").append(pb);
                }
            } catch (NumberFormatException e) {
                versionBuilder.append(preBuild.startsWith("+") ? preBuild : ("+" + preBuild));
            }
        }

        return versionBuilder.toString();
    }

    @Override public String toString() {
        return toString(true);
    }
}
