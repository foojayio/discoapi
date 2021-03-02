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


import java.util.Arrays;
import java.util.List;


public enum Bitness implements ApiFeature {
    BIT_32("32 Bit", "32", 32),
    BIT_64("64 Bit", "64", 64),
    NONE("-", "", 0),
    NOT_FOUND("", "", 0);

    private final String uiString;
    private final String apiString;
    private final int    bits;


    Bitness(final String uiString, final String apiString, final int bits) {
        this.uiString  = uiString;
        this.apiString = apiString;
        this.bits      = bits;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public Bitness getDefault() { return Bitness.NONE; }

    @Override public Bitness getNotFound() { return Bitness.NOT_FOUND; }

    @Override public Bitness[] getAll() { return values(); }

    public int getAsInt() { return bits; }

    public String getAsString() { return Integer.toString(bits); }

    public static Bitness fromText(final String text) {
        switch (text) {
            case "32":
            case "32bit":
            case "32Bit":
            case "32BIT":
                return BIT_32;
            case "64":
            case "64bit":
            case "64Bit":
            case "64BIT":
                return BIT_64;
            default:
                return NOT_FOUND;
        }
    }

    public static Bitness fromInt(final Integer bits) {
        switch (bits) {
            case 32: return BIT_32;
            case 64: return BIT_64;
            default: return NOT_FOUND;
        }
    }

    public static List<Bitness> getAsList() { return Arrays.asList(values()); }
}