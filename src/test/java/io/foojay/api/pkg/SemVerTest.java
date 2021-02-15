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
import io.foojay.api.util.OutputFormat;
import io.foojay.api.util.Pair;
import io.foojay.api.util.Error;
import io.foojay.api.util.SemVerParser;
import io.foojay.api.util.SemVerParsingResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class SemVerTest {
    @Test
    public void semVerFromTextTest() {
        String t1 = ">=11.0.9.0-ea+b1";
        SemVer semVer1 = SemVerParser.fromText(t1).getSemVer1();
        assert semVer1.toString().equals(">=11.0.9-ea+b1");
        assert semVer1.getVersionNumber().toString(OutputFormat.REDUCED, true, false).equals("11.0.9");
        assert ReleaseStatus.EA == semVer1.getReleaseStatus();
        assert semVer1.getPre().equals("-ea");
        assert semVer1.getMetadata().equals("+b1");
        assert Comparison.GREATER_THAN_OR_EQUAL == semVer1.getComparison();

        String t2 = "1.8.0.252-ea";
        SemVer semVer2 = SemVerParser.fromText(t2).getSemVer1();
        assert semVer2.toString().equals("8.0.252-ea");

        List<SemVer> versions = List.of(new SemVer(new VersionNumber(11, 0, 7, 0)),
                                        new SemVer(new VersionNumber(11, 0, 7, 5)),
                                        new SemVer(new VersionNumber(11, 0, 8, 0)),
                                        new SemVer(new VersionNumber(11, 0, 8, 0), ReleaseStatus.EA),
                                        new SemVer(new VersionNumber(11, 0, 8, 0), ReleaseStatus.EA, "meta"),
                                        new SemVer(new VersionNumber(11, 0, 8, 2)),
                                        new SemVer(new VersionNumber(11, 0, 8, 5)),
                                        new SemVer(new VersionNumber(11, 0, 8, 9)),
                                        new SemVer(new VersionNumber(11, 0, 9, 1)),
                                        new SemVer(new VersionNumber(11, 0, 9, 1), ReleaseStatus.EA),
                                        new SemVer(new VersionNumber(11, 0, 10, 0)));

        String              t3              = ">11.0.8-ea+meta<=11.0.9.1-ea+meta";
        SemVerParsingResult result3         = SemVerParser.fromText(t3);
        List<SemVer>        versionsBetween = versions.stream().filter(result3.getFilter()).collect(Collectors.toList());
        assert versionsBetween.toString().equals("[11.0.8, 11.0.8.2, 11.0.8.5, 11.0.8.9, 11.0.9.1-ea]");

        String              t4                 = ">11.0.8.2";
        SemVerParsingResult result4            = SemVerParser.fromText(t4);
        List<SemVer>        versionsLargerThan = versions.stream().filter(result4.getFilter()).collect(Collectors.toList());
        assert versionsLargerThan.toString().equals("[11.0.8.5, 11.0.8.9, 11.0.9.1, 11.0.9.1-ea, 11.0.10]");

        String              t5                  = "<=11.0.8";
        SemVerParsingResult result5             = SemVerParser.fromText(t5);
        List<SemVer>        versionsSmallerThan = versions.stream().filter(result5.getFilter()).collect(Collectors.toList());
        assert versionsSmallerThan.toString().equals("[11.0.7, 11.0.7.5, 11.0.8, 11.0.8-ea, 11.0.8-ea+meta]");

        SemVer semVer3 = new SemVer(new VersionNumber(11, 0, 8, 0), ReleaseStatus.EA);
        SemVer semVer4 = new SemVer(new VersionNumber(11, 0, 8, 0), ReleaseStatus.EA, "meta");
        assert semVer3.equalTo(semVer4);
        assert semVer3.toString().equals("11.0.8-ea");
        assert semVer4.toString().equals("11.0.8-ea+meta");

        String              t6                  = "11.0.8.0.1-ea+meta";
        SemVerParsingResult result6             = SemVerParser.fromText(t6);
        SemVer              semVer6             = result6.getSemVer1();
        assert semVer6.toString(false).equals("11.0.8.0.1-ea+meta");

        String              t7                  = "12.0.9.0.5.1-ea+meta";
        SemVerParsingResult result7             = SemVerParser.fromText(t7);
        SemVer              semVer7             = result7.getSemVer1();
        assert semVer7.toString(false).equals("12.0.9.0.5.1-ea+meta");

        String              t8                  = "14.0.0-ea.36+meta";
        SemVerParsingResult result8             = SemVerParser.fromText(t8);
        SemVer              semVer8             = result8.getSemVer1();
        assert ReleaseStatus.EA == semVer8.getReleaseStatus();
        assert semVer8.getPreBuild().equals("36");
        assert semVer8.toString(false).equals("14-ea.36+meta");
    }

    @Test
    public void semVerToStringTest() {
        SemVer semVer = new SemVer(new VersionNumber(11, 0, 9, 1, 0, 5), ReleaseStatus.EA,"", "+b1");
        assert "11.0.9.1-ea+b1".equals(semVer.toString());
        assert "11.0.9.1.0.5-ea+b1".equals(semVer.toString(false));

        SemVer semVer1 = SemVer.fromText("14.0.0-ea.36").getSemVer1();
        assert "14-ea.36".equals(semVer1.toString());
    }

    @Test
    public void semVerEqualsToOtherSemVerTest() {
        SemVer semver1 = SemVer.fromText(new VersionNumber(17, null, null, null, null, null, null, ReleaseStatus.EA, 28).toString()).getSemVer1();
        SemVer semver2 = SemVer.fromText("17-ea.28").getSemVer1();
        SemVer semver3 = SemVer.fromText("17-ea.34").getSemVer1();

        assert semver1.equalTo(semver2);
        assert semver1.compareTo(semver2) == 0;

        assert !semver1.equalTo(semver3);
        assert semver1.compareTo(semver3) != 0;

        assert semver1.compareTo(semver3) < 0;
        assert semver3.compareTo(semver1) > 0;
    }

    @Test
    public void semVerConstructorTest() {
        VersionNumber versionNumber = new VersionNumber(11, 0, 0, 0, 0, 0, 13, ReleaseStatus.EA, 5);
        SemVer        semver        = new SemVer(versionNumber);
        assert versionNumber.toString(OutputFormat.REDUCED, true, true).equals(semver.toString(true));
    }
}
