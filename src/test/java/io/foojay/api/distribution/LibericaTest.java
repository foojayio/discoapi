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

package io.foojay.api.distribution;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class LibericaTest {

    @Test
    public void fifthElement() {
        JsonObject json = new JsonObject();
        assertEquals(OptionalInt.empty(), Liberica.getFifthElement(json));
        json.add("extraVersion", JsonNull.INSTANCE);
        assertEquals(OptionalInt.empty(), Liberica.getFifthElement(json));
        json.addProperty("extraVersion", 0);
        assertEquals(OptionalInt.empty(), Liberica.getFifthElement(json));
        json.addProperty("extraVersion", 3);
        assertEquals(OptionalInt.of(3), Liberica.getFifthElement(json));
    }

    @Test
    public void distinctVersionsKeepsRebuild() {
        Semver regular = new Semver(new VersionNumber(25, 0, 5, 0));
        Semver rebuild = new Semver(new VersionNumber(25, 0, 5, 0, 3));
        List<Semver> versions = Liberica.distinctVersions(List.of(regular, rebuild, rebuild));
        assertEquals(List.of(rebuild, regular), versions);
    }
}
