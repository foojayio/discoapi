/*
 * Copyright (c) 2026.
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

import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class PkgTest {

    @Test
    public void fifthElementCountsInSemver() {
        VersionNumber rebuild = new VersionNumber(25, 0, 5, 0); // 25.0.5.0.3+5
        rebuild.setFifth(3);
        rebuild.setBuild(5);
        VersionNumber regular = new VersionNumber(25, 0, 5, 0); // 25.0.5.0+11
        regular.setBuild(11);

        assertTrue(Pkg.toSemver(rebuild).compareTo(Pkg.toSemver(regular)) > 0);
    }

    @Test
    public void javaVersionKeepsFifthElement() {
        VersionNumber rebuild = new VersionNumber(25, 0, 5, 0);
        rebuild.setFifth(3);
        rebuild.setBuild(5);
        VersionNumber regular = new VersionNumber(25, 0, 5, 0);
        regular.setBuild(11);

        assertEquals("25.0.5.0.3+5", Pkg.toJavaVersion(Pkg.toSemver(rebuild), rebuild));
        assertEquals("25.0.5+11", Pkg.toJavaVersion(Pkg.toSemver(regular), regular));
    }

    @Test
    public void javaVersionUnchangedWithoutFifthElement() {
        VersionNumber temurinPatch = new VersionNumber(25, 0, 4, 1);
        temurinPatch.setBuild(1);
        VersionNumber three = new VersionNumber(21, 0, 13);
        three.setBuild(9);
        VersionNumber feature = new VersionNumber(27);
        VersionNumber eaWithFifthZero = new VersionNumber(25, 0, 5, 0);
        eaWithFifthZero.setFifth(0);
        eaWithFifthZero.setBuild(9);

        for (VersionNumber vn : new VersionNumber[] { temurinPatch, three, feature, eaWithFifthZero }) {
            assertEquals(Pkg.toSemver(vn).toString(), Pkg.toJavaVersion(Pkg.toSemver(vn), vn));
        }
        assertEquals("25.0.4.1+1", Pkg.toJavaVersion(Pkg.toSemver(temurinPatch), temurinPatch));
        assertEquals("21.0.13+9", Pkg.toJavaVersion(Pkg.toSemver(three), three));
    }

    @Test
    public void javaVersionKeepsZeroPatchWhenSemverLostFifthElement() {
        VersionNumber rebuild = new VersionNumber(25, 0, 5, 0);
        rebuild.setFifth(3);
        rebuild.setBuild(5);
        Semver lost = Semver.fromText(rebuild.toString()).getSemver1();

        assertEquals("25.0.5.0.3+5", Pkg.toJavaVersion(lost, rebuild));
    }
}
