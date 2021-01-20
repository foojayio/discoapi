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

package io.foojay.api.util;

import io.foojay.api.pkg.SemVer;
import io.foojay.api.pkg.VersionNumber;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SemVerParser {
    private static final Pattern SEM_VER_PATTERN = Pattern.compile("^(<|<=|>|>=|=)?v?([0-9]+)(\\.[0-9]+)?(\\.[0-9]+)?(\\.[0-9]+)?(-([0-9A-Za-z\\-]+(\\.[0-9A-Za-z\\-]+)*))?(\\+([0-9A-Za-z\\-]+(\\.[0-9A-Za-z\\-]+)*))?((<|<=|>|>=|=)?v?([0-9]+)(\\.[0-9]+)?(\\.[0-9]+)?(\\.[0-9]+)?(-([0-9A-Za-z\\-]+(\\.[0-9A-Za-z\\-]+)*))?(\\+([0-9A-Za-z\\-]+(\\.[0-9A-Za-z\\-]+)*))?)?$");
    private static final Matcher SEM_VER_MATCHER = SEM_VER_PATTERN.matcher("");


    public static SemVerParsingResult fromText(final String text) {
        SemVerParsingResult parsingResult = new SemVerParsingResult();

        // ******************** Parsing 1st SemVer ****************************

        // Remove leading "1." to get correct version number e.g. 1.8u262 -> 8u262
        String versionText1 = text.startsWith("1.") ? text.replace("1.", "") : text;
        if (versionText1.contains("_")) {
            versionText1 = versionText1.replace("_", ".");
        }
        if (versionText1.matches("[0-9]+u[0-9]+.*")) {
            versionText1 = versionText1.replace("u", ".0.");
        }

        SEM_VER_MATCHER.reset(versionText1);
        final List<MatchResult> results = SEM_VER_MATCHER.results().collect(Collectors.toList());

        if (results.isEmpty()) {
            parsingResult.setError1(new Error("Invalid semver: " + versionText1));
            return parsingResult;
        }

        MatchResult result = results.get(0);

        String metadata1 = null != result.group(10) ? result.group(10) : "";
        String pre1      = null != result.group(7)  ? result.group(7)  : "";

        VersionNumber versionNumber1 = new VersionNumber();

        Comparison comparison1;
        if (null == result.group(1)) {
            comparison1 = Comparison.EQUAL;
        } else {
            comparison1 = Comparison.fromText(result.group(1));
        }

        try {
            if (null == result.group(2)) {
                parsingResult.setError1(new Error("Feature version cannot be null"));
                return parsingResult;
            }
            versionNumber1.setFeature(Integer.parseInt(result.group(2)));
        } catch (NumberFormatException e) {
            parsingResult.setError1(new Error("Error when parsing feature version " + result.group(2) + ": " + e));
            return parsingResult;
        }

        try {
            if (null == result.group(3)) {
                versionNumber1.setInterim(0);
            } else {
                versionNumber1.setInterim(Integer.parseInt(Helper.trimPrefix(result.group(3), "\\.")));
            }
        } catch (NumberFormatException e) {
            parsingResult.setError1(new Error("Error when parsing interim version " + result.group(3) + ": " + e));
            return parsingResult;
        }

        try {
            if (null == result.group(4)) {
                versionNumber1.setUpdate(0);
            } else {
                versionNumber1.setUpdate(Integer.parseInt(Helper.trimPrefix(result.group(4), "\\.")));
            }
        } catch (NumberFormatException e) {
            parsingResult.setError1(new Error("Error when parsing update version " + result.group(4) + ": " + e));
            return parsingResult;
        }

        try {
            if (null == result.group(5)) {
                versionNumber1.setPatch(0);
            } else {
                versionNumber1.setPatch(Integer.parseInt(Helper.trimPrefix(result.group(5), "\\.")));
            }
        } catch (NumberFormatException e) {
            parsingResult.setError1(new Error("Error when parsing patch version " + result.group(5) + ": " + e));
            return parsingResult;
        }

        // Validate prerelease
        Error err1;
        if (null != pre1 && !pre1.isEmpty()) {
            err1 = validatePrerelease(pre1);
            if (null != err1) {
                parsingResult.setError1(err1);
                return parsingResult;
            }
        }

        // Validate metadata
        if (null != metadata1 && !metadata1.isEmpty()) {
            err1 = validateMetadata(metadata1);
            if (null != err1) {
                parsingResult.setError1(err1);
                return parsingResult;
            }
        }
        SemVer semVer1 = new SemVer(versionNumber1, pre1, metadata1);
        semVer1.setComparison(comparison1);
        parsingResult.setSemVer1(semVer1);

        Predicate<SemVer> filter = null;

        // ******************** Parsing 2nd SemVer ****************************
        if (result.groupCount() == 23 && null != result.group(12)) {
            String metadata2 = null != result.group(22) ? result.group(22) : "";
            String pre2      = null != result.group(19) ? result.group(19) : "";

            VersionNumber versionNumber2 = new VersionNumber();

            Comparison comparison2;
            if (null == result.group(13)) {
                comparison2 = Comparison.EQUAL;
            } else {
                comparison2 = Comparison.fromText(result.group(13));
            }

            boolean oldFormat;
            try {
                if (null == result.group(14)) {
                    parsingResult.setError2(new Error("Feature version cannot be null"));
                    return parsingResult;
                }
                oldFormat = Integer.parseInt(result.group(14)) == 1;
                versionNumber2.setFeature(Integer.parseInt(result.group(14)));
            } catch (NumberFormatException e) {
                parsingResult.setError2(new Error("Error when parsing feature version " + result.group(14) + ": " + e));
                return parsingResult;
            }

            try {
                if (null == result.group(15)) {
                    versionNumber2.setInterim(0);
                } else {
                    versionNumber2.setInterim(Integer.parseInt(Helper.trimPrefix(result.group(15), "\\.")));
                }
            } catch (NumberFormatException e) {
                parsingResult.setError2(new Error("Error when parsing interim version " + result.group(15) + ": " + e));
                return parsingResult;
            }

            try {
                if (null == result.group(16)) {
                    versionNumber2.setUpdate(0);
                } else {
                    versionNumber2.setUpdate(Integer.parseInt(Helper.trimPrefix(result.group(16), "\\.")));
                }
            } catch (NumberFormatException e) {
                parsingResult.setError2(new Error("Error when parsing update version " + result.group(16) + ": " + e));
                return parsingResult;
            }

            try {
                if (null == result.group(17)) {
                    versionNumber2.setPatch(0);
                } else {
                    versionNumber2.setPatch(Integer.parseInt(Helper.trimPrefix(result.group(17), "\\.")));
                }
            } catch (NumberFormatException e) {
                parsingResult.setError2(new Error("Error when parsing patch version " + result.group(17) + ": " + e));
                return parsingResult;
            }

            // Remove leading "1." to get correct version number e.g. 1.8u262 -> 8u262
            if (oldFormat) {
                versionNumber2.setFeature(versionNumber2.getInterim().getAsInt());
                versionNumber2.setInterim(versionNumber2.getUpdate().getAsInt());
                versionNumber2.setUpdate(versionNumber2.getPatch().getAsInt());
                versionNumber2.setPatch(0);
            }

            // Validate prerelease
            Error err2;
            if (null != pre2 && !pre2.isEmpty()) {
                err2 = validatePrerelease(pre2);
                if (null != err2) {
                    parsingResult.setError2(err2);
                    return parsingResult;
                }
            }

            // Validate metadata
            if (null != metadata2 && !metadata2.isEmpty()) {
                err2 = validateMetadata(metadata2);
                if (null != err2) {
                    parsingResult.setError2(err2);
                    return parsingResult;
                }
            }
            SemVer semVer2 = new SemVer(versionNumber2, pre2, metadata2);
            semVer2.setComparison(comparison2);

            // Define filter
            switch(comparison1) {
                case LESS_THAN:
                    filter = semVer -> semVer.lessThan(semVer1); break;
                case LESS_THAN_OR_EQUAL:
                    filter = semVer -> (semVer.lessThan(semVer1) || semVer.equalTo(semVer1)); break;
                case GREATER_THAN:
                    switch(comparison2) {
                        case LESS_THAN         : filter = semVer -> semVer.greaterThan(semVer1) && semVer.lessThan(semVer2); break;
                        case LESS_THAN_OR_EQUAL: filter = semVer -> semVer.greaterThan(semVer1) && (semVer.lessThan(semVer2) || semVer.equalTo(semVer2)); break;
                        default                : filter = semVer -> semVer.greaterThan(semVer1); break;
                    }
                    break;
                case GREATER_THAN_OR_EQUAL:
                    switch(comparison2) {
                        case LESS_THAN         : filter = semVer -> (semVer.equalTo(semVer1) || semVer.greaterThan(semVer1)) && semVer.lessThan(semVer2); break;
                        case LESS_THAN_OR_EQUAL: filter = semVer -> (semVer.equalTo(semVer1) || semVer.greaterThan(semVer1)) && (semVer.lessThan(semVer2) || semVer.equalTo(semVer2)); break;
                        default                : filter = semVer -> (semVer.equalTo(semVer1) || semVer.greaterThan(semVer1)); break;
                    }
                    break;
            }
            parsingResult.setFilter(filter);

            parsingResult.setSemVer2(semVer2);
            return parsingResult;
        }

        // Define filter
        switch(comparison1) {
            case LESS_THAN            : filter = semVer -> semVer.lessThan(semVer1); break;
            case LESS_THAN_OR_EQUAL   : filter = semVer -> (semVer.lessThan(semVer1) || semVer.equalTo(semVer1)); break;
            case GREATER_THAN         : filter = semVer -> semVer.greaterThan(semVer1); break;
            case GREATER_THAN_OR_EQUAL: filter = semVer -> (semVer.equalTo(semVer1) || semVer.greaterThan(semVer1)); break;
        }
        parsingResult.setFilter(filter);

        return parsingResult;
    }

    private static Error validatePrerelease(final String prerelease) {
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

    private static Error validateMetadata(final String metadata) {
        String[] eparts = metadata.split(".");
        for (String p : eparts) {
            if (!p.matches("[a-zA-Z-0-9]")) {
                return new Error("Invalid metadata: " + metadata);
            }
        }
        return null;
    }
}
