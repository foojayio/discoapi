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

import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.util.Comparison;
import eu.hansolo.jdktools.util.OutputFormat;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.SemverParser;
import eu.hansolo.jdktools.versioning.SemverParsingResult;
import eu.hansolo.jdktools.versioning.VersionNumber;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;


public class SemverTest {
    @Test
    public void semVerFromTextTest() {
        String t1 = ">=11.0.9.0-ea+b1";
        Semver semVer1 = SemverParser.fromText(t1).getSemver1();
        assert semVer1.toString().equals(">=11.0.9-ea+1");
        assert semVer1.getVersionNumber().toString(OutputFormat.REDUCED, true, false).equals("11.0.9");
        assert ReleaseStatus.EA == semVer1.getReleaseStatus();
        assert semVer1.getPre().equals("-ea");
        assert semVer1.getMetadata().equals("+b1");
        assert Comparison.GREATER_THAN_OR_EQUAL == semVer1.getComparison();

        String t2 = "1.8.0.252-ea";
        Semver semVer2 = SemverParser.fromText(t2).getSemver1();
        assert semVer2.toString().equals("8.0.252-ea");

        List<Semver> versions = List.of(new Semver(new VersionNumber(11, 0, 7, 0)),
                                        new Semver(new VersionNumber(11, 0, 7, 5)),
                                        new Semver(new VersionNumber(11, 0, 8, 0)),
                                        new Semver(new VersionNumber(11, 0, 8, 0), ReleaseStatus.EA),
                                        new Semver(new VersionNumber(11, 0, 8, 0), ReleaseStatus.EA, "meta"),
                                        new Semver(new VersionNumber(11, 0, 8, 2)),
                                        new Semver(new VersionNumber(11, 0, 8, 5)),
                                        new Semver(new VersionNumber(11, 0, 8, 9)),
                                        new Semver(new VersionNumber(11, 0, 9, 1)),
                                        new Semver(new VersionNumber(11, 0, 9, 1), ReleaseStatus.EA),
                                        new Semver(new VersionNumber(11, 0, 10, 0)));

        String              t3              = ">11.0.8-ea+meta<=11.0.9.1-ea+meta";
        SemverParsingResult result3         = SemverParser.fromText(t3);
        List<Semver>        versionsBetween = versions.stream().filter(result3.getFilter()).collect(Collectors.toList());
        assert versionsBetween.toString().equals("[11.0.8, 11.0.8.2, 11.0.8.5, 11.0.8.9, 11.0.9.1-ea]");

        String              t4                 = ">11.0.8.2";
        SemverParsingResult result4            = SemverParser.fromText(t4);
        List<Semver>        versionsLargerThan = versions.stream().filter(result4.getFilter()).collect(Collectors.toList());
        assert versionsLargerThan.toString().equals("[11.0.8.5, 11.0.8.9, 11.0.9.1, 11.0.9.1-ea, 11.0.10]");

        String              t5                  = "<=11.0.8";
        SemverParsingResult result5             = SemverParser.fromText(t5);
        List<Semver>        versionsSmallerThan = versions.stream().filter(result5.getFilter()).collect(Collectors.toList());
        assert versionsSmallerThan.toString().equals("[11.0.7, 11.0.7.5, 11.0.8, 11.0.8-ea, 11.0.8-ea+meta]");

        Semver semVer3 = new Semver(new VersionNumber(11, 0, 8, 0), ReleaseStatus.EA);
        Semver semVer4 = new Semver(new VersionNumber(11, 0, 8, 0), ReleaseStatus.EA, "meta");
        assert semVer3.equalTo(semVer4);
        assert semVer3.toString().equals("11.0.8-ea");
        assert semVer4.toString().equals("11.0.8-ea+meta");

        String              t6                  = "11.0.8.0.1-ea+meta";
        SemverParsingResult result6             = SemverParser.fromText(t6);
        Semver              semVer6             = result6.getSemver1();
        assert semVer6.toString(false).equals("11.0.8.0.1-ea+meta");

        String              t7                  = "12.0.9.0.5.1-ea+meta";
        SemverParsingResult result7             = SemverParser.fromText(t7);
        Semver              semVer7             = result7.getSemver1();
        assert semVer7.toString(false).equals("12.0.9.0.5.1-ea+meta");

        String              t8                  = "14.0.0-ea.36+meta";
        SemverParsingResult result8             = SemverParser.fromText(t8);
        Semver              semVer8             = result8.getSemver1();

        assert ReleaseStatus.EA == semVer8.getReleaseStatus();
        assert semVer8.getPreBuild().equals("36");
        assert semVer8.toString(false).equals("14-ea+36");

        String              t9                  = "18-ea+10";
        SemverParsingResult result9             = SemverParser.fromText(t9);
        Semver              semVer9             = result9.getSemver1();

        assert ReleaseStatus.EA == semVer9.getReleaseStatus();
        assert semVer9.getPreBuild().equals("10");
        assert semVer9.toString(false).equals("18-ea+10");

        String              t10                 = "1.8.0.302-b8";
        SemverParsingResult result10            = SemverParser.fromText(t10);
        Semver              semVer10            = result10.getSemver1();

        assert ReleaseStatus.GA == semVer10.getReleaseStatus();
        assert semVer10.getPreBuild().equals("8");
        assert semVer10.toString(true).equals("8.0.302+8");

        String              t11                 = "17.0.1-beta+12.0.202111240007";
        SemverParsingResult resultt11           = SemverParser.fromText(t11);
        Semver              semVer11            = resultt11.getSemver1();

        assert ReleaseStatus.EA == semVer11.getReleaseStatus();
        assert semVer11.toString(true).equals("17.0.1-ea+12.0.202111240007");
    }

    @Test
    public void semVerToStringTest() {
        Semver semVer = new Semver(new VersionNumber(11, 0, 9, 1, 0, 5), ReleaseStatus.EA,"", "+b1");
        assert "11.0.9.1-ea+1".equals(semVer.toString());
        assert "11.0.9.1.0.5-ea+1".equals(semVer.toString(false));

        Semver semVer1 = Semver.fromText("14.0.0-ea.36").getSemver1();

        assert "14-ea+36".equals(semVer1.toString());
    }

    @Test
    public void semVerEqualsToOtherSemverTest() {
        Semver semver1 = Semver.fromText(new VersionNumber(17, null, null, null, null, null, 28, ReleaseStatus.EA).toString()).getSemver1();
        Semver semver2 = Semver.fromText("17-ea.28").getSemver1();
        Semver semver3 = Semver.fromText("17-ea.34").getSemver1();
        Semver semver4 = Semver.fromText("17-ea+34").getSemver1();

        assert semver1.equalTo(semver2);
        assert semver1.compareTo(semver2) == 0;

        assert !semver1.equalTo(semver3);
        assert semver1.compareTo(semver3) != 0;

        assert semver1.compareTo(semver3) < 0;
        assert semver3.compareTo(semver1) > 0;

        assert semver3.compareTo(semver4) == 0;
        assert semver3.equalTo(semver4);
    }

    @Test
    public void semVerConstructorTest() {
        VersionNumber versionNumber = new VersionNumber(11, 0, 0, 0, 0, 0, 5, ReleaseStatus.EA);
        Semver        semver        = new Semver(versionNumber);
        assert versionNumber.toString(OutputFormat.REDUCED, true, true).equals(semver.toString(true));
    }
}
