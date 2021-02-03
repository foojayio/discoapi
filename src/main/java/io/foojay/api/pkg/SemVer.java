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

import io.foojay.api.util.Comparison;
import io.foojay.api.util.Error;
import io.foojay.api.util.Helper;
import io.foojay.api.util.OutputFormat;
import io.foojay.api.util.SemVerParser;
import io.foojay.api.util.SemVerParsingResult;

import java.util.regex.Matcher;


public class SemVer implements Comparable<SemVer> {
    private VersionNumber versionNumber;
    private ReleaseStatus releaseStatus;
    private String        pre;
    private String        metadata;
    private Comparison    comparison;


    public SemVer(final VersionNumber versionNumber) {
        this(versionNumber, ReleaseStatus.GA, "","");
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
        this.releaseStatus = releaseStatus;
        this.pre           = null == pre      ? ReleaseStatus.EA == releaseStatus ? "-ea": "" : pre;
        this.metadata      = null == metadata ? "" : metadata;
        this.comparison    = Comparison.EQUAL;

        if (null != this.pre && !this.pre.isEmpty() && !this.pre.startsWith("+") && !this.pre.startsWith("-")) {
            this.pre = "-" + pre;
        }
        if (null != this.metadata && !this.metadata.isEmpty() && !this.metadata.startsWith("-") && !this.metadata.startsWith("+")) {
            this.metadata = "+" + metadata;
        }

        if (!this.pre.isEmpty() && !this.pre.startsWith("-")) { throw new IllegalArgumentException("pre-release argument has to start with \"-\""); }
        if (!this.metadata.isEmpty() && !this.metadata.startsWith("+")) { throw new IllegalArgumentException("metadata argument has to start with \"+\""); }
        if (ReleaseStatus.EA == this.releaseStatus && !this.pre.isEmpty() && !this.pre.equalsIgnoreCase("-ea")) { throw new IllegalArgumentException("ReleaseStatus and pre-release argument cannot be different"); }
        if (ReleaseStatus.GA == this.releaseStatus && !this.pre.isEmpty() && this.pre.equalsIgnoreCase("-ea")) { throw new IllegalArgumentException("ReleaseStatus and pre-release argument cannot be different"); }

        Matcher m = Helper.NUMBER_IN_TEXT_PATTERN.matcher(metadata);
        if (m.find()) { versionNumber.setBuild(Integer.valueOf(m.group(2))); }
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
        String[] preParts1 = pre1.split(".");
        String[] preParts2 = pre2.split(".");

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
            if (!prePart2.isEmpty()) {
                return -1;
            }
            return 1;
        }

        if (prePart2.isEmpty()) {
            if (!prePart1.isEmpty()) {
                return 1;
            }
            return -1;
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
            if (prePart1Number > prePart2Number) {
                return 1;
            } else {
                return -1;
            }
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
        versionBuilder.append(versionNumber.toString(OutputFormat.REDUCED, javaFormat));
        versionBuilder.append(ReleaseStatus.EA == releaseStatus ? "-ea" : "");
        if (null != metadata && !metadata.isEmpty()) {
            versionBuilder.append(metadata.startsWith("+") ? metadata : ("+" + metadata));
        }
        return versionBuilder.toString();
    }

    @Override public String toString() {
        return toString(true);
    }
}
