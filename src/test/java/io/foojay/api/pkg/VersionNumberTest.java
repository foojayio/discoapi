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

import io.foojay.api.util.OutputFormat;
import org.junit.jupiter.api.Test;


public class VersionNumberTest {
    @Test
    public void versionNumberSmallerThanOtherVersionNumber() {
        VersionNumber versionNumber1 = new VersionNumber(8, 0, 0, 0);
        VersionNumber versionNumber2 = new VersionNumber(8, 0, 0, 1);
        assert versionNumber1.compareTo(versionNumber2) == -1;

        VersionNumber versionNumber3 = new VersionNumber(8, 0, 1, 0);
        VersionNumber versionNumber4 = new VersionNumber(8, 0, 1, 1);
        assert versionNumber3.compareTo(versionNumber4) == -1;

        VersionNumber versionNumber5 = new VersionNumber(8, 1, 1, 0);
        VersionNumber versionNumber6 = new VersionNumber(8, 1, 1, 1);
        assert versionNumber5.compareTo(versionNumber6) == -1;

        VersionNumber versionNumber7 = new VersionNumber(8, 1, 1, 0);
        VersionNumber versionNumber8 = new VersionNumber(9, 1, 1, 1);
        assert versionNumber7.compareTo(versionNumber8) == -1;

        VersionNumber versionNumber9  = new VersionNumber(1, 0, 1);
        VersionNumber versionNumber10 = new VersionNumber(1, 1);
        assert versionNumber9.compareTo(versionNumber10) == -1;

        VersionNumber versionNumber11 = new VersionNumber(1, 1, 34);
        VersionNumber versionNumber12 = new VersionNumber(1, 12, 1);
        assert versionNumber11.compareTo(versionNumber12) == -1;
    }

    @Test
    public void versionNumberEqualsOtherVersionNumber() {
        VersionNumber versionNumber1 = new VersionNumber(8, 0, 0, 0);
        VersionNumber versionNumber2 = new VersionNumber(8, 0, 0, 0);
        assert versionNumber1.compareTo(versionNumber2) == 0;

        VersionNumber versionNumber3 = new VersionNumber(8, 0, 1, 0);
        VersionNumber versionNumber4 = new VersionNumber(8, 0, 1, 0);
        assert versionNumber3.compareTo(versionNumber4) == 0;

        VersionNumber versionNumber5 = new VersionNumber(8, 1, 0, 0);
        VersionNumber versionNumber6 = new VersionNumber(8, 1, 0, 0);
        assert versionNumber5.compareTo(versionNumber6) == 0;

        VersionNumber versionNumber7 = new VersionNumber(9, 1, 1, 0);
        VersionNumber versionNumber8 = new VersionNumber(9, 1, 1, 0);
        assert versionNumber7.compareTo(versionNumber8) == 0;

        VersionNumber versionNumber9  = new VersionNumber(2, 06);
        VersionNumber versionNumber10 = new VersionNumber(2, 060);
        assert versionNumber7.compareTo(versionNumber8) == 0;
    }

    @Test
    public void versionNumberEqualsOtherVersionNumber2() {
        final VersionNumber versionNumber1 = new VersionNumber(1);
        final VersionNumber versionNumber2 = new VersionNumber(1);
        final VersionNumber versionNumber3 = new VersionNumber(1, 2);
        final VersionNumber versionNumber4 = new VersionNumber(1, 2);
        final VersionNumber versionNumber5 = new VersionNumber(1, 2, 3);
        final VersionNumber versionNumber6 = new VersionNumber(1, 2, 3);
        final VersionNumber versionNumber7 = new VersionNumber(1, 2, 3, 4);
        final VersionNumber versionNumber8 = new VersionNumber(1, 2, 3, 4);

        assert versionNumber1.equals(versionNumber2);
        assert versionNumber3.equals(versionNumber4);
        assert versionNumber5.equals(versionNumber6);
        assert versionNumber7.equals(versionNumber8);
    }

    @Test
    public void versionNumberLargerThanOtherVersioNumber() {
        VersionNumber versionNumber1 = new VersionNumber(8, 0, 0, 1);
        VersionNumber versionNumber2 = new VersionNumber(8, 0, 0, 0);
        assert versionNumber1.compareTo(versionNumber2) == 1;

        VersionNumber versionNumber3 = new VersionNumber(8, 0, 1, 1);
        VersionNumber versionNumber4 = new VersionNumber(8, 0, 1, 0);
        assert versionNumber3.compareTo(versionNumber4) == 1;

        VersionNumber versionNumber5 = new VersionNumber(8, 1, 1, 1);
        VersionNumber versionNumber6 = new VersionNumber(8, 1, 1, 0);
        assert versionNumber5.compareTo(versionNumber6) == 1;

        VersionNumber versionNumber7 = new VersionNumber(9, 1, 1, 1);
        VersionNumber versionNumber8 = new VersionNumber(8, 1, 1, 0);
        assert versionNumber7.compareTo(versionNumber8) == 1;
    }

    @Test
    public void versionNumberFromString() {
        final String versionNumber1String  = "8";                        // 8
        final String versionNumber2String  = "8.2";                      // 8.2
        final String versionNumber3String  = "1.2.3";                    // 1.2.3
        final String versionNumber4String  = "8.2.3.4";                  // 8.2.3.4
        final String versionNumber5String  = "11.26.2-DEBUG";            // 11.26.2
        final String versionNumber6String  = "11.0.2+13-LTS";            // 11.0.2.13
        final String versionNumber7String  = "signed.7.5.4.3.2.1.0";     // 7.5.4.3
        final String versionNumber8String  = "20.30.0";                  // 20.30.0
        final String versionNumber9String  = "11.25.3.DEBUG";            // 11.25.3
        final String versionNumber10String = "1.8.0_262";                // 8.0.262
        final String versionNumber11String = "1.8u262";                  // 8.0.262
        final String versionNumber12String = "8u262";                    // 8.0.262
        final String versionNumber13String = "11";                       // 11
        final String versionNumber14String = "8.0.272-ea+10";            // 8.0.272 b10
        final String versionNumber15String = "1.8.0_275.b01-x86.rpm";    // 8.0.275 b1
        final String versionNumber16String = "8u272b09_ea.tar.gz";       // 8.0.272 b9
        final String versionNumber17String = "11.0.9.1_1.tar.gz";        // 11.0.9.1 b1
        final String versionNumber18String = "11.0.9.12-1_amd64.deb";    // 11.0.9.12 b1
        final String versionNumber19String = "13.0.5.1-macosx_x64.dmg";  // 13.0.5.1
        final String versionNumber20String = "13.0.5.1-win_i686.zip";    // 13.0.5.1
        final String versionNumber21String = "1.7.0_25-b15";             // 7.0.25 b15
        final String versionNumber22String = "7u25";                     // 7.0.25
        final String versionNumber23String = "8u172-b11";                // 8.0.162 b12
        final String versionNumber24String = "8u162-b12_openj9-0.8.0";   // 8.0.162 b12
        final String versionNumber25String = "11.0.1+13";                // 11.0.1 b13
        final String versionNumber26String = "11+28";                    // 11 b28
        final String versionNumber27String = "14.0.0-ea.28";             // 14.0.0.0 ea build 28
        final String versionNumber28String = "15.0.0-ea";                // 15.0.0. ea
        final String versionNumber29String = "11.0.9.1.5.2";             // 11.0.9.1.5.2
        final String versionNumber30String = "11.0.9.1.5.2-ea";          // 11.0.9.1.5.2 ea

        final VersionNumber versionNumber1  = new VersionNumber(8);
        final VersionNumber versionNumber2  = new VersionNumber(8, 2);
        final VersionNumber versionNumber3  = new VersionNumber(2, 3);
        final VersionNumber versionNumber4  = new VersionNumber(8, 2, 3, 4);
        final VersionNumber versionNumber5  = new VersionNumber(11, 26, 2, null);
        final VersionNumber versionNumber6  = new VersionNumber(11, 0, 2, null);
        final VersionNumber versionNumber7  = new VersionNumber(7, 5, 4, 3, 2, 1);
        final VersionNumber versionNumber8  = new VersionNumber(20, 30, 0);
        final VersionNumber versionNumber9  = new VersionNumber(11, 25, 3, null);
        final VersionNumber versionNumber10 = new VersionNumber(8, 0, 262);
        final VersionNumber versionNumber11 = new VersionNumber(8, 0, 262);
        final VersionNumber versionNumber12 = new VersionNumber(8, 0, 262);
        final VersionNumber versionNumber13 = new VersionNumber(11);
        final VersionNumber versionNumber14 = new VersionNumber(8, 0, 272);
        final VersionNumber versionNumber15 = new VersionNumber(8, 0, 275);
        final VersionNumber versionNumber16 = new VersionNumber(8, 0, 272,0);
        final VersionNumber versionNumber17 = new VersionNumber(11, 0, 9, 1);
        final VersionNumber versionNumber18 = new VersionNumber(11, 0, 9, 12);
        final VersionNumber versionNumber19 = new VersionNumber(13, 0, 5, 1);
        final VersionNumber versionNumber20 = new VersionNumber(13, 0, 5, 1);
        final VersionNumber versionNumber21 = new VersionNumber(7, 0, 25, 0);
        final VersionNumber versionNumber22 = new VersionNumber(7, 0, 25, 0);
        final VersionNumber versionNumber23 = new VersionNumber(8, 0, 172, 0);
        final VersionNumber versionNumber24 = new VersionNumber(8, 0, 162, 0);
        final VersionNumber versionNumber25 = new VersionNumber(11, 0, 1, 0);
        final VersionNumber versionNumber26 = new VersionNumber(11, 0, 0, 0);
        final VersionNumber versionNumber27 = new VersionNumber(14, 0, 0);
        final VersionNumber versionNumber28 = new VersionNumber(15, 0, 0);
        final VersionNumber versionNumber29 = new VersionNumber(11, 0, 9, 1, 5, 2);
        final VersionNumber versionNumber30 = new VersionNumber(11, 0, 9, 1, 5, 2);

        assert versionNumber1.compareTo(VersionNumber.fromText(versionNumber1String))   == 0;
        assert versionNumber2.compareTo(VersionNumber.fromText(versionNumber2String))   == 0;
        assert versionNumber3.compareTo(VersionNumber.fromText(versionNumber3String))   == 0;
        assert versionNumber4.compareTo(VersionNumber.fromText(versionNumber4String))   == 0;
        assert versionNumber5.compareTo(VersionNumber.fromText(versionNumber5String))   == 0;
        assert versionNumber6.compareTo(VersionNumber.fromText(versionNumber6String))   == 0;
        assert versionNumber7.compareTo(VersionNumber.fromText(versionNumber7String))   == 0;
        assert versionNumber8.compareTo(VersionNumber.fromText(versionNumber8String))   == 0;
        assert versionNumber9.compareTo(VersionNumber.fromText(versionNumber9String))   == 0;
        assert versionNumber10.compareTo(VersionNumber.fromText(versionNumber10String)) == 0;
        assert versionNumber11.compareTo(VersionNumber.fromText(versionNumber11String)) == 0;
        assert versionNumber12.compareTo(VersionNumber.fromText(versionNumber12String)) == 0;
        assert versionNumber13.compareTo(VersionNumber.fromText(versionNumber13String)) == 0;
        assert versionNumber14.compareTo(VersionNumber.fromText(versionNumber14String)) == 0;
        assert versionNumber15.compareTo(VersionNumber.fromText(versionNumber15String)) == 0;
        assert versionNumber16.compareTo(VersionNumber.fromText(versionNumber16String)) == 0;
        assert versionNumber17.compareTo(VersionNumber.fromText(versionNumber17String)) == 0;
        assert versionNumber18.compareTo(VersionNumber.fromText(versionNumber18String)) == 0;
        assert versionNumber19.compareTo(VersionNumber.fromText(versionNumber19String)) == 0;
        assert versionNumber20.compareTo(VersionNumber.fromText(versionNumber20String)) == 0;
        assert versionNumber21.compareTo(VersionNumber.fromText(versionNumber21String)) == 0;
        assert versionNumber22.compareTo(VersionNumber.fromText(versionNumber22String)) == 0;
        assert versionNumber23.compareTo(VersionNumber.fromText(versionNumber23String)) == 0;
        assert versionNumber24.compareTo(VersionNumber.fromText(versionNumber24String)) == 0;
        assert versionNumber25.compareTo(VersionNumber.fromText(versionNumber25String)) == 0;
        assert versionNumber26.compareTo(VersionNumber.fromText(versionNumber26String)) == 0;
        assert versionNumber27.compareTo(VersionNumber.fromText(versionNumber27String)) == 0;
        assert versionNumber28.compareTo(VersionNumber.fromText(versionNumber28String)) == 0;
        assert versionNumber29.compareTo(VersionNumber.fromText(versionNumber29String)) == 0;
        assert versionNumber30.compareTo(VersionNumber.fromText(versionNumber30String)) == 0;
    }

    @Test
    public void normalizedVersionNumber() {
        final String versionNumber1String = "8";
        final String versionNumber2String = "11.2";
        final String versionNumber3String = "11.2.3";

        final VersionNumber versionNumber1 = new VersionNumber(8, 0, 0, 0);
        final VersionNumber versionNumber2 = new VersionNumber(11, 2, 0, 0);
        final VersionNumber versionNumber3 = new VersionNumber(11, 2, 3, 0);

        assert VersionNumber.fromText(versionNumber1String).getNormalizedVersionNumber().equals(versionNumber1.getNormalizedVersionNumber());
        assert VersionNumber.fromText(versionNumber2String).getNormalizedVersionNumber().equals(versionNumber2.getNormalizedVersionNumber());
        assert VersionNumber.fromText(versionNumber3String).getNormalizedVersionNumber().equals(versionNumber3.getNormalizedVersionNumber());
    }

    @Test
    public void shortenedVersionNumber() {
        VersionNumber v1 = new VersionNumber(1,2,3,4);
        VersionNumber v2 = new VersionNumber(1,2,3,0);
        VersionNumber v3 = new VersionNumber(1,2,0,4);
        VersionNumber v4 = new VersionNumber(1,0,3,4);
        VersionNumber v5 = new VersionNumber(1,2,0,0);
        VersionNumber v6 = new VersionNumber(1,0,0,0);
        VersionNumber v7 = new VersionNumber(1,0,0,4);

        assert v1.toString(OutputFormat.REDUCED, true).equals("1.2.3.4");
        assert v2.toString(OutputFormat.REDUCED, true).equals("1.2.3");
        assert v3.toString(OutputFormat.REDUCED, true).equals("1.2.0.4");
        assert v4.toString(OutputFormat.REDUCED, true).equals("1.0.3.4");
        assert v5.toString(OutputFormat.REDUCED, true).equals("1.2");
        assert v6.toString(OutputFormat.REDUCED, true).equals("1");
        assert v7.toString(OutputFormat.REDUCED, true).equals("1.0.0.4");
    }
}
