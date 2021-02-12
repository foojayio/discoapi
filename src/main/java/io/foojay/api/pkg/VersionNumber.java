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

import io.foojay.api.util.Helper;
import io.foojay.api.util.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class VersionNumber implements Comparable<VersionNumber> {
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionNumber.class);

    public static final Pattern VERSION_NO_PATTERN  = Pattern.compile("([1-9]\\d*)((u(\\d+))|(\\.?(\\d+)?\\.?(\\d+)?\\.?(\\d+)?\\.?(\\d+)?\\.(\\d+)))?((_|b)(\\d+))?((-|\\+|\\.)([a-zA-Z0-9\\-\\+]+)(\\.[0-9]+)?)?");
    public static final Pattern LEADING_INT_PATTERN = Pattern.compile("^[0-9]*");

    @NotNull
    @Positive
    private OptionalInt feature;
    @Positive
    private OptionalInt interim;
    @Positive
    private OptionalInt update;
    @Positive
    private OptionalInt patch;
    @Positive
    private OptionalInt fifth;
    @Positive
    private OptionalInt sixth;
    @Positive
    private OptionalInt build;

    private Optional<ReleaseStatus> releaseStatus;


    public VersionNumber() {
        this(OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), Optional.empty(), OptionalInt.empty());
    }
    public VersionNumber(@NotNull VersionNumber versionNumber) {
        this(versionNumber.getFeature(), versionNumber.getInterim(), versionNumber.getUpdate(), versionNumber.getPatch(), versionNumber.getFifth(), versionNumber.getSixth(), versionNumber.getReleaseStatus(), versionNumber.getBuild());
    }
    public VersionNumber(@NotNull @Positive final Integer feature) {
        this(feature, 0, 0, 0, 0, 0, null, null);
    }
    public VersionNumber(@NotNull @Positive final Integer feature, @Positive final Integer interim) {
        this(feature, interim, 0, 0, 0, 0, null, null);
    }
    public VersionNumber(@NotNull @Positive final Integer feature, @Positive final Integer interim, @Positive final Integer update) {
        this(feature, interim, update, 0, 0, 0, null, null);
    }
    public VersionNumber(@NotNull @Positive final Integer feature, @Positive final Integer interim, @Positive final Integer update, @Positive final Integer patch) throws IllegalArgumentException {
        this(feature, interim, update, patch, 0, 0, null, null);
    }
    public VersionNumber(@NotNull @Positive final Integer feature, @Positive final Integer interim, @Positive final Integer update, @Positive final Integer patch, @Positive final Integer fifth, @Positive final Integer sixth) {
        this(feature, interim, update, patch, fifth, sixth, null, null);
    }
    public VersionNumber(@NotNull @Positive final Integer feature, @Positive final Integer interim, @Positive final Integer update, @Positive final Integer patch, @Positive final Integer fifth, @Positive final Integer sixth, final ReleaseStatus releaseStatus, final Integer build) throws IllegalArgumentException {
        if (null == feature) { throw new IllegalArgumentException("Feature version cannot be null"); }
        if (0 >= feature) { throw new IllegalArgumentException("Feature version cannot be smaller than 0"); }
        if (null != interim && 0 > interim) { throw new IllegalArgumentException("Interim version cannot be smaller than 0"); }
        if (null != update && 0 > update) { throw new IllegalArgumentException("Update version cannot be smaller than 0"); }
        if (null != patch && 0 > patch) { throw new IllegalArgumentException("Patch version cannot be smaller than 0"); }
        if (null != fifth && 0 > fifth)     { throw new IllegalArgumentException("Fifth number cannot be smaller than 0"); }
        if (null != sixth && 0 > sixth)     { throw new IllegalArgumentException("Sixth number cannot be smaller than 0"); }
        this.feature        = OptionalInt.of(feature);
        this.interim        = null == interim        ? OptionalInt.of(0) : OptionalInt.of(interim);
        this.update         = null == update         ? OptionalInt.of(0) : OptionalInt.of(update);
        this.patch          = null == patch          ? OptionalInt.of(0) : OptionalInt.of(patch);
        this.fifth   = null == fifth   ? OptionalInt.of(0) : OptionalInt.of(fifth);
        this.sixth   = null == sixth   ? OptionalInt.of(0) : OptionalInt.of(sixth);
        this.releaseStatus  = null == releaseStatus ? Optional.empty()    : Optional.of(releaseStatus);
        this.build          = null == build         ? OptionalInt.empty() : OptionalInt.of(build);
    }
    public VersionNumber(final OptionalInt feature, final OptionalInt interim, final OptionalInt update, final OptionalInt patch) {
        this(feature, interim, update, patch, OptionalInt.of(0), OptionalInt.of(0), Optional.empty(), OptionalInt.empty());
    }
    public VersionNumber(final OptionalInt feature, final OptionalInt interim, final OptionalInt update, final OptionalInt patch, final OptionalInt fifth, final OptionalInt sixth) {
        this(feature, interim, update, patch, fifth, sixth, Optional.empty(), OptionalInt.empty());
    }
    public VersionNumber(final OptionalInt feature, final OptionalInt interim, final OptionalInt update, final OptionalInt patch, final OptionalInt fifth, final OptionalInt sixth, final Optional<ReleaseStatus> releaseStatus, final OptionalInt build) {
        this.feature       = null == feature       ? OptionalInt.empty() : feature;
        this.interim       = null == interim       ? OptionalInt.of(0)   : interim;
        this.update        = null == update        ? OptionalInt.of(0)   : update;
        this.patch         = null == patch         ? OptionalInt.of(0)   : patch;
        this.fifth         = null == fifth         ? OptionalInt.of(0)   : fifth;
        this.sixth         = null == sixth         ? OptionalInt.of(0)   : sixth;
        this.releaseStatus = null == releaseStatus ? Optional.empty()    : releaseStatus;
        this.build         = null == build         ? OptionalInt.empty() : build;
    }


    public OptionalInt getFeature() { return feature; }
    public void setFeature(final Integer feature) throws IllegalArgumentException {
        if (null == feature) { throw new IllegalArgumentException("Feature version cannot be null"); }
        if (0 >= feature) { throw new IllegalArgumentException("Feature version cannot be smaller than 0"); }
        this.feature = OptionalInt.of(feature);
    }

    public OptionalInt getInterim() { return interim; }
    public void setInterim(final Integer interim) throws IllegalArgumentException {
        if (null != interim && 0 > interim) { throw new IllegalArgumentException("Interim version cannot be smaller than 0"); }
        this.interim = null == interim ? OptionalInt.empty() : OptionalInt.of(interim);
    }

    public OptionalInt getUpdate() { return update; }
    public void setUpdate(final Integer update) throws IllegalArgumentException {
        if (null != update &&  0 > update) { throw new IllegalArgumentException("Update version cannot be smaller than 0"); }
        this.update = null == update ? OptionalInt.empty() : OptionalInt.of(update);
    }

    public OptionalInt getPatch() { return patch; }
    public void setPatch(final Integer patch) throws IllegalArgumentException {
        if (null != patch && 0 > patch) { throw new IllegalArgumentException("Patch version cannot be smaller than 0"); }
        this.patch = null == patch ? OptionalInt.empty() : OptionalInt.of(patch);
    }

    public OptionalInt getFifth() { return fifth; }
    public void setFifth(final Integer fifth) throws IllegalArgumentException {
        if (null != fifth && 0 > fifth) { throw new IllegalArgumentException("Fifth number cannot be smaller than 0"); }
        this.fifth = null == fifth ? OptionalInt.empty() : OptionalInt.of(fifth);
    }

    public OptionalInt getSixth() { return sixth; }
    public void setSixth(final Integer sixth) throws IllegalArgumentException {
        if (null != sixth && 0 > sixth) { throw new IllegalArgumentException("Sixth number cannot be smaller than 0"); }
        this.sixth = null == sixth ? OptionalInt.empty() : OptionalInt.of(sixth);
    }

    public Optional<ReleaseStatus> getReleaseStatus() { return releaseStatus; }
    public void setReleaseStatus(final ReleaseStatus releaseStatus) {
        if (null == releaseStatus) { throw new IllegalArgumentException("Release status cannot be null"); }
        this.releaseStatus = Optional.of(releaseStatus);
    }

    public OptionalInt getBuild() { return build; }
    public void setBuild(final Integer build) throws IllegalArgumentException {
        if (null != build && 0 > build) { throw new IllegalArgumentException("Build number cannot be smaller than 0"); }
        this.build = null == build ? OptionalInt.empty() : OptionalInt.of(build);
    }

    public MajorVersion getMajorVersion() { return new MajorVersion(feature.isPresent() ? feature.getAsInt() : 0); }

    public String getNormalizedVersionNumber() {
        StringBuilder versionBuilder = new StringBuilder();
        if (feature.isPresent()) {
            versionBuilder.append(feature.getAsInt());
        } else {
            throw new IllegalArgumentException("Feature version number cannot be null");
        }
        versionBuilder.append(".").append(interim.isPresent() ? interim.getAsInt() : "0");
        versionBuilder.append(".").append(update.isPresent() ? update.getAsInt() : "0");
        versionBuilder.append(".").append(patch.isPresent() ? patch.getAsInt() : "0");
        versionBuilder.append(".").append(fifth.isPresent() ? fifth.getAsInt() : "0");
        versionBuilder.append(".").append(sixth.isPresent() ? sixth.getAsInt() : "0");
        return versionBuilder.toString();
    }

    public static VersionNumber fromText(final String text) throws IllegalArgumentException {
        return fromText(text, 0);
    }
    /**
     * Returns a version number parsed from the given text. If the matcher finds more than 1 result, the
     * resultToMatch variable will be taken into account. For example if the given text matches 2 times,
     * the resultToMatch variable defines which result should be taken to parse the version number.
     * @param text           Text to parse
     * @param resultToMatch  The result that should be taken for parsing if there are more than 1
     * @return Returns a version number parsed from the given text
     * @throws IllegalArgumentException
     */
    public static VersionNumber fromText(final String text, final int resultToMatch) throws IllegalArgumentException {
        if (null == text || text.isEmpty()) {
            LOGGER.warn("No version number can be parsed because given text is null or empty.");
            return new VersionNumber();
        }

        // Remove leading "1." to get correct version number e.g. 1.8u262 -> 8u262
        String version = text.startsWith("1.") ? text.replace("1.", "") : text;

        final Matcher           versionNoMatcher = VERSION_NO_PATTERN.matcher(version);
        final List<MatchResult> results          = versionNoMatcher.results().collect(Collectors.toList());
        final int               noOfResults  = results.size();
        final int               resultToTake = noOfResults > resultToMatch ? resultToMatch : 0;
        List<VersionNumber>     numbersFound = new ArrayList<>();
        if (noOfResults > 0) {
            MatchResult result = results.get(resultToTake);
            VersionNumber versionNumber = new VersionNumber(Integer.valueOf(result.group(1)));
            if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(7) && null != result.group(9) && null != result.group(10) && null != result.group(11) && null != result.group(12) && null != result.group(13) && null != result.group(14) && null != result.group(15)) {
                //System.out.println("match: 1, 2, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(7), version));
                versionNumber.setPatch(getPositiveIntFromText(result.group(9), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(7) && null != result.group(10) && null != result.group(11) && null != result.group(12) && null != result.group(13) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(7), version));
                versionNumber.setPatch(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(7) && null != result.group(8) && null != result.group(9) && null != result.group(10) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 5, 6, 7, 8, 9, 10, 14, 15, 16");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(7), version));
                versionNumber.setPatch(getPositiveIntFromText(result.group(8), version));
                versionNumber.setFifth(getPositiveIntFromText(result.group(9), version));
                versionNumber.setSixth(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(7) && null != result.group(8) && null != result.group(10) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 5, 6, 7, 8, 10, 14, 15, 16");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(7), version));
                versionNumber.setPatch(getPositiveIntFromText(result.group(8), version));
                versionNumber.setFifth(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(10) && null != result.group(11) && null != result.group(12) && null != result.group(13) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 5, 10, 11, 12, 13, 14, 15, 16");
                versionNumber.setInterim(getPositiveIntFromText(result.group(10), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(13), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(7) && null != result.group(10) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 5, 6, 7, 10, 14, 15, 16");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(7), version));
                versionNumber.setPatch(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(10) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 5, 6, 10, 14, 15, 16");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(7) && null != result.group(8) && null != result.group(9) && null != result.group(10)) {
                //System.out.println("match: 1, 2, 5, 6, 7, 8, 9, 10");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(7), version));
                versionNumber.setPatch(getPositiveIntFromText(result.group(8), version));
                versionNumber.setFifth(getPositiveIntFromText(result.group(9), version));
                versionNumber.setSixth(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(7) && null != result.group(8) && null != result.group(10)) {
                //System.out.println("match: 1, 2, 5, 6, 7, 8, 10");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(7), version));
                versionNumber.setPatch(getPositiveIntFromText(result.group(8), version));
                versionNumber.setFifth(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(3) && null != result.group(4) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 3, 4, 14, 15, 16");
                versionNumber.setInterim(0);
                versionNumber.setUpdate(getPositiveIntFromText(result.group(4), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(3) && null != result.group(4) && null != result.group(11) && null != result.group(12) && null != result.group(13)) {
                //System.out.println("match: 1, 2, 3, 4, 11, 12, 13");
                versionNumber.setInterim(0);
                versionNumber.setUpdate(getPositiveIntFromText(result.group(4), version));
            } /*else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(10) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 5, 6, 10, 14, 15, 16");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(10), version));
            } */else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(10) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 2, 5, 6, 10, 14, 15, 16");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(10) && null != result.group(11) && null != result.group(12) && null != result.group(13)) {
                //System.out.println("match: 1, 2, 5, 10, 11, 12, 13");
                versionNumber.setInterim(0);
                versionNumber.setUpdate(getPositiveIntFromText(result.group(13), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(7) && null != result.group(10)) {
                //System.out.println("match: 1, 2, 5, 6, 7, 10");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(7), version));
                versionNumber.setPatch(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(10)) {
                //System.out.println("match: 1, 2, 5, 6, 10");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(6) && null != result.group(10)) {
                //System.out.println("match: 1, 2, 5, 6, 10");
                versionNumber.setInterim(getPositiveIntFromText(result.group(6), version));
                versionNumber.setUpdate(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(3) && null != result.group(4)) {
                //System.out.println("match: 1, 2, 3, 4");
                versionNumber.setInterim(0);
                versionNumber.setUpdate(getPositiveIntFromText(result.group(4), version));
            } else if (null != result.group(1) && null != result.group(2) && null != result.group(5) && null != result.group(10)) {
                //System.out.println("match: 1, 2, 5, 9");
                versionNumber.setInterim(getPositiveIntFromText(result.group(10), version));
            } else if (null != result.group(1) && null != result.group(14) && null != result.group(15) && null != result.group(16)) {
                //System.out.println("match: 1, 14, 15, 16");
            }

            if (null != result.group(16) && "ea".equals(result.group(16))) {
                // Early Access
                versionNumber.setReleaseStatus(ReleaseStatus.EA);
                if (null != result.group(17)) {
                    if (result.group(17).startsWith(".")) {
                        String part = result.group(17).replaceFirst(".", "");
                        if (part.length() > 0) {
                            if (part.matches("[0-9]+")) {
                                if (part.length() > 1 && part.startsWith("0")) {
                                    part = part.replaceFirst("^0+(?!$)", "");
                                }
                                versionNumber.setBuild(Integer.parseInt(part));
                            }
                        }
                    }
                }
            }

            if (!versionNumber.getInterim().isPresent() || versionNumber.getInterim().isEmpty()) {
                versionNumber.setInterim(0);
            }
            if (!versionNumber.getUpdate().isPresent() || versionNumber.getUpdate().isEmpty()) {
                versionNumber.setUpdate(0);
            }
            if (!versionNumber.getPatch().isPresent() || versionNumber.getPatch().isEmpty()) {
                versionNumber.setPatch(0);
            }
            if (!versionNumber.getFifth().isPresent() || versionNumber.getFifth().isEmpty()) {
                versionNumber.setFifth(0);
            }
            if (!versionNumber.getSixth().isPresent() || versionNumber.getSixth().isEmpty()) {
                versionNumber.setSixth(0);
            }

            numbersFound.add(versionNumber);
        }
        if (numbersFound.isEmpty()) {
            LOGGER.error("No suitable version number found in String: {}", text);
            return new VersionNumber();
        } else {
            return numbersFound.stream().max(Comparator.comparingInt(VersionNumber::numbersAvailable)).get();
        }
    }

    /**
     * Returns the numbers that are available in the version number
     * e.g. Feature                      -> 1
     *      Feature.Interim              -> 2
     *      Feature.Interim.Update       -> 3
     *      Feature.Interim.Update.Patch -> 4
     * @return the numbers that are available in the version number
     */
    private int numbersAvailable() {
        return 1 + (interim.isPresent() ? 1 : 0) + (update.isPresent() ? 1 : 0) + (patch.isPresent() ? 1 : 0);
    }

    private static Integer getPositiveIntFromText(final String text, final String fullTextToParse) {
        if (Helper.isPositiveInteger(text)) {
            return Integer.valueOf(text);
        } else {
            //LOGGER.info("Given text {} did not contain positive integer. Full text to parse was: {}", text, fullTextToParse);
            return -1;
        }
    }

    private static Integer getLeadingIntFromText(final String text, final String fullTextToParse) {
        if (null == text || text.isEmpty()) { return -1; }
        Matcher matcher = LEADING_INT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).isEmpty() ? -1 : Integer.valueOf(matcher.group(0));
        } else {
            LOGGER.debug("Given text {} did not start with integer. Full text to parse was: {}", text, fullTextToParse);
            return -1;
        }
    }

    /**
     * Returns 0 if given version number is equal to this. But with just a number like 11, it will
     * also return 0 for values like 11.0.2, 11.4.0 etc. This is used in the DiscoService to make sure
     * to filter results for version numbers.
     * @param otherVersionNumber
     * @return 0 if given version number is equel to this. But also returns 0 if only feature number is equal to given feature number
     */
    public int compareForFilterTo(final VersionNumber otherVersionNumber) {
        int comparisonResult = 0;
        if (!feature.isPresent() || !otherVersionNumber.getFeature().isPresent()) { return comparisonResult; }
        String[] version1Splits = toString().split("\\.");
        String[] version2Splits = otherVersionNumber.toString().split("\\.");
        int maxLengthOfVersionSplits = Math.min(version1Splits.length, version2Splits.length);

        for (int i = 0; i < maxLengthOfVersionSplits; i++) {
            Integer v1      = i < version1Splits.length ? Integer.parseInt(version1Splits[i]) : 0;
            Integer v2      = i < version2Splits.length ? Integer.parseInt(version2Splits[i]) : 0;
            int     compare = v1.compareTo(v2);
            if (compare != 0) {
                comparisonResult = compare;
                break;
            }
        }
        return comparisonResult;
    }

    @Override public int hashCode() {
        return Objects.hash(feature.getAsInt(), interim.orElse(0), update.orElse(0), patch.orElse(0));
    }

    @Override public boolean equals(final Object obj) {
        if (obj == VersionNumber.this) { return true; }
        if (!(obj instanceof VersionNumber)) { return false; }
        VersionNumber other = (VersionNumber) obj;
        boolean isEqual;
        if (feature.getAsInt() == other.getFeature().getAsInt()) {
            if (interim.isPresent()) {
                if (other.getInterim().isPresent()) {
                    if (interim.getAsInt() == other.getInterim().getAsInt()) {
                        if (update.isPresent()) {
                            if (other.getUpdate().isPresent()) {
                                if (update.getAsInt() == other.getUpdate().getAsInt()) {
                                    if (patch.isPresent()) {
                                        if (other.getPatch().isPresent()) {
                                            if (patch.getAsInt() == other.getPatch().getAsInt()) {
                                                if (fifth.isPresent()) {
                                                    if (other.getFifth().isPresent()) {
                                                        if (fifth.getAsInt() == other.getFifth().getAsInt()) {
                                                            if (sixth.isPresent()) {
                                                                if (other.getSixth().isPresent()) {
                                                                    isEqual = sixth.getAsInt() == other.getSixth().getAsInt();
                                                                } else {
                                                                    isEqual = false;
                                                                }
                                                            } else {
                                                                isEqual = true;
                                                            }
                                                        } else {
                                                            isEqual = false;
                                                        }
                                        } else {
                                                        isEqual = false;
                                        }
                                    } else {
                                                    isEqual = true;
                                    }
                                } else {
                                                isEqual = false;
                                }
                                        } else {
                                            isEqual = false;
                                        }
                                    } else {
                                        isEqual = true;
                                    }
                                } else {
                                    isEqual = false;
                                }
                            } else {
                                isEqual = false;
                            }
                        } else {
                            isEqual = true;
                        }
                    } else {
                        isEqual = false;
                    }
                } else {
                    isEqual = false;
                }
            } else {
                isEqual = true;
            }
        } else {
            isEqual = false;
        }
        if (isEqual &&
            releaseStatus.isPresent() && ReleaseStatus.EA == releaseStatus.get() && build.isPresent() &&
            other.getReleaseStatus().isPresent() && ReleaseStatus.EA == other.getReleaseStatus().get() && other.getBuild().isPresent()) {
            isEqual = getBuild().getAsInt() == other.getBuild().getAsInt();
        }
        return isEqual;
    }

    public String toStringInclBuild(final boolean javaFormat) {
        return toString(OutputFormat.REDUCED, javaFormat, true);
    }

    public String toString(final OutputFormat outputFormat, final boolean javaFormat, final boolean includeReleaseStatusAndBuild) {
        String        pre            = releaseStatus.isPresent() ? (ReleaseStatus.EA == releaseStatus.get() ? "-ea" : "") : "";
        String        preBuild       = releaseStatus.isPresent() && build.isPresent() ? ("." + build.getAsInt()) : "";
        StringBuilder versionBuilder = new StringBuilder();
        switch(outputFormat) {
            case REDUCED:
            case REDUCED_COMPRESSED:
                if (feature.isPresent()) { versionBuilder.append(feature.getAsInt()); }
                if (sixth.isPresent() && sixth.getAsInt() != 0) {
                    if (interim.isPresent()) { versionBuilder.append(".").append(interim.getAsInt()); }
                    if (update.isPresent())  { versionBuilder.append(".").append(update.getAsInt()); }
                    if (patch.isPresent())   { versionBuilder.append(".").append(patch.getAsInt()); }
                    if (!javaFormat) {
                        if (fifth.isPresent()) { versionBuilder.append(".").append(fifth.getAsInt()); }
                        if (sixth.isPresent()) { versionBuilder.append(".").append(sixth.getAsInt()); }
                    }
                    if (includeReleaseStatusAndBuild) { versionBuilder.append(pre).append(preBuild); }
                    return versionBuilder.toString();
                } else if (fifth.isPresent() && fifth.getAsInt() != 0) {
                    if (interim.isPresent()) { versionBuilder.append(".").append(interim.getAsInt()); }
                    if (update.isPresent())  { versionBuilder.append(".").append(update.getAsInt()); }
                    if (patch.isPresent())   { versionBuilder.append(".").append(patch.getAsInt()); }
                    if (!javaFormat) {
                        if (fifth.isPresent()) { versionBuilder.append(".").append(fifth.getAsInt()); }
                    }
                    if (includeReleaseStatusAndBuild) { versionBuilder.append(pre).append(preBuild); }
                    return versionBuilder.toString();
                } else if (patch.isPresent() && patch.getAsInt() != 0) {
                    if (interim.isPresent()) { versionBuilder.append(".").append(interim.getAsInt()); }
                    if (update.isPresent()) { versionBuilder.append(".").append(update.getAsInt()); }
                    if (patch.isPresent()) { versionBuilder.append(".").append(patch.getAsInt()); }
                    if (includeReleaseStatusAndBuild) { versionBuilder.append(pre).append(preBuild); }
                    return versionBuilder.toString();
                } else if (update.isPresent() && update.getAsInt() != 0) {
                    if (interim.isPresent()) { versionBuilder.append(".").append(interim.getAsInt()); }
                    if (update.isPresent()) { versionBuilder.append(".").append(update.getAsInt()); }
                    if (includeReleaseStatusAndBuild) { versionBuilder.append(pre).append(preBuild); }
                    return versionBuilder.toString();
                } else if (interim.isPresent() && interim.getAsInt() != 0) {
                    if (interim.isPresent()) { versionBuilder.append(".").append(interim.getAsInt()); }
                    if (includeReleaseStatusAndBuild) { versionBuilder.append(pre).append(preBuild); }
                    return versionBuilder.toString();
                } else {
                    if (includeReleaseStatusAndBuild) { versionBuilder.append(pre).append(preBuild); }
                    return versionBuilder.toString();
                }
            case FULL:
            case FULL_COMPRESSED:
            default:
                if (feature.isPresent()) { versionBuilder.append(feature.getAsInt()); }
                if (interim.isPresent()) { versionBuilder.append(".").append(interim.getAsInt()); }
                if (update.isPresent()) { versionBuilder.append(".").append(update.getAsInt()); }
                if (patch.isPresent()) { versionBuilder.append(".").append(patch.getAsInt()); }
                if (!javaFormat) {
                    if (fifth.isPresent()) { versionBuilder.append(".").append(fifth.getAsInt()); }
                    if (sixth.isPresent()) { versionBuilder.append(".").append(sixth.getAsInt()); }
                }
                if (includeReleaseStatusAndBuild) { versionBuilder.append(pre).append(preBuild); }
                return versionBuilder.toString();
        }
    }

    @Override public String toString() {
        return toString(OutputFormat.FULL, true, true);
    }

    @Override public int compareTo(final VersionNumber otherVersionNumber) {
        final int equal       = 0;
        final int smallerThan = -1;
        final int largerThan  = 1;
        int ret;

        if (feature.isPresent() && otherVersionNumber.getFeature().isPresent()) {
            if (feature.getAsInt() > otherVersionNumber.getFeature().getAsInt()) {
                ret = largerThan;
            } else if (feature.getAsInt() < otherVersionNumber.getFeature().getAsInt()) {
                ret = smallerThan;
            } else {
                if (interim.isPresent() && otherVersionNumber.getInterim().isPresent()) {
                    if (interim.getAsInt() > otherVersionNumber.getInterim().getAsInt()) {
                        ret = largerThan;
                    } else if (interim.getAsInt() < otherVersionNumber.getInterim().getAsInt()) {
                        ret = smallerThan;
                    } else {
                        if (update.isPresent() && otherVersionNumber.getUpdate().isPresent()) {
                            if (update.getAsInt() > otherVersionNumber.getUpdate().getAsInt()) {
                                ret = largerThan;
                            } else if (update.getAsInt() < otherVersionNumber.getUpdate().getAsInt()) {
                                ret = smallerThan;
                            } else {
                                if (patch.isPresent() && otherVersionNumber.getPatch().isPresent()) {
                                    if (patch.getAsInt() > otherVersionNumber.getPatch().getAsInt()) {
                                        ret = largerThan;
                                    } else if (patch.getAsInt() < otherVersionNumber.getPatch().getAsInt()) {
                                        ret = smallerThan;
                                    } else {
                                        if (fifth.isPresent() && otherVersionNumber.getFifth().isPresent()) {
                                            if (fifth.getAsInt() > otherVersionNumber.getFifth().getAsInt()) {
                                                ret = largerThan;
                                            } else if (fifth.getAsInt() < otherVersionNumber.getFifth().getAsInt()) {
                                                ret = smallerThan;
                                            } else {
                                                if (sixth.isPresent() && otherVersionNumber.getSixth().isPresent()) {
                                                    if (sixth.getAsInt() > otherVersionNumber.getSixth().getAsInt()) {
                                                        ret = largerThan;
                                                    } else if (sixth.getAsInt() < otherVersionNumber.getSixth().getAsInt()) {
                                                        ret = smallerThan;
                                                    } else {
                                                        ret = equal;
                                    }
                                                } else if (sixth.isPresent() && !otherVersionNumber.getSixth().isPresent()) {
                                                    ret = largerThan;
                                                } else if (!sixth.isPresent() && otherVersionNumber.getSixth().isPresent()) {
                                                    ret = smallerThan;
                                                } else {
                                                    ret = equal;
                                                }
                                            }
                                        } else if (fifth.isPresent() && !otherVersionNumber.getFifth().isPresent()) {
                                            ret = largerThan;
                                        } else if (!fifth.isPresent() && otherVersionNumber.getFifth().isPresent()) {
                                            ret = smallerThan;
                                        } else {
                                            ret= equal;
                                    }
                                    }
                                } else if (patch.isPresent() && !otherVersionNumber.getPatch().isPresent()) {
                                    ret = largerThan;
                                } else if (!patch.isPresent() && otherVersionNumber.getPatch().isPresent()) {
                                    ret = smallerThan;
                                } else {
                                    ret = equal;
                                }
                            }
                        } else if (update.isPresent() && !otherVersionNumber.getUpdate().isPresent()) {
                            ret = largerThan;
                        } else if (!update.isPresent() && otherVersionNumber.getUpdate().isPresent()) {
                            ret = smallerThan;
                        } else {
                            ret = equal;
                        }
                    }
                } else if (interim.isPresent() && !otherVersionNumber.getInterim().isPresent()) {
                    ret = largerThan;
                } else if (!interim.isPresent() && otherVersionNumber.getInterim().isPresent()) {
                    ret = smallerThan;
                } else {
                    ret = equal;
                }
            }
        } else if (feature.isPresent() && !otherVersionNumber.getFeature().isPresent()) {
            ret = largerThan;
        } else if (!feature.isPresent() && !otherVersionNumber.getFeature().isPresent()) {
            ret = smallerThan;
        } else {
            ret = equal;
        }
        if (ret == equal) {
            if (releaseStatus.isPresent() && ReleaseStatus.EA == releaseStatus.get() && build.isPresent() &&
                otherVersionNumber.getReleaseStatus().isPresent() && ReleaseStatus.EA == otherVersionNumber.getReleaseStatus().get() && otherVersionNumber.getBuild().isPresent()) {
                int buildNumber      = getBuild().getAsInt();
                int otherBuildNumber = otherVersionNumber.getBuild().getAsInt();
                if (buildNumber > otherBuildNumber) {
                    ret = largerThan;
                } else if (buildNumber < otherBuildNumber) {
                    ret = smallerThan;
        } else {
                    ret = equal;
                }
            } else if (releaseStatus.isPresent() && ReleaseStatus.EA == releaseStatus.get() && build.isPresent() &&
                       otherVersionNumber.getReleaseStatus().isPresent() && ReleaseStatus.EA == otherVersionNumber.getReleaseStatus().get() && otherVersionNumber.getBuild().isEmpty()) {
                ret = largerThan;
            } else if (releaseStatus.isPresent() && ReleaseStatus.EA == releaseStatus.get() && build.isEmpty() &&
                       otherVersionNumber.getReleaseStatus().isPresent() && ReleaseStatus.EA == otherVersionNumber.getReleaseStatus().get() && otherVersionNumber.getBuild().isPresent()) {
                ret = smallerThan;
            } else {
                ret = equal;
            }
        }
        return ret;
    }
}
