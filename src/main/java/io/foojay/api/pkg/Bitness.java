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
 *     You should have received a copy of the GNU General Public License
 *     along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.pkg;


import io.foojay.api.util.OutputFormat;

import java.util.Arrays;
import java.util.List;

import static io.foojay.api.util.Constants.COLON;
import static io.foojay.api.util.Constants.COMMA;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.INDENTED_QUOTES;
import static io.foojay.api.util.Constants.NEW_LINE;
import static io.foojay.api.util.Constants.QUOTES;


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

    @Override public String toString(final OutputFormat outputFormat) {
        StringBuilder msgBuilder = new StringBuilder();
        switch(outputFormat) {
            case FULL, REDUCED, REDUCED_ENRICHED -> {
                msgBuilder.append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                          .append(INDENTED_QUOTES).append("name").append(QUOTES).append(COLON).append(QUOTES).append(name()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append("ui_string").append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append("api_string").append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append("bits").append(QUOTES).append(COLON).append(bits).append(NEW_LINE)
                          .append(CURLY_BRACKET_CLOSE);
            }
            case FULL_COMPRESSED, REDUCED_COMPRESSED, REDUCED_ENRICHED_COMPRESSED, REDUCED_MINIMIZED -> {
                msgBuilder.append(CURLY_BRACKET_OPEN)
                          .append(QUOTES).append("name").append(QUOTES).append(COLON).append(QUOTES).append(name()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append("ui_string").append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append("api_string").append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append("bits").append(QUOTES).append(COLON).append(bits)
                          .append(CURLY_BRACKET_CLOSE);
            }
        }
        return msgBuilder.toString();
    }

    @Override public String toString() { return toString(OutputFormat.FULL_COMPRESSED); }

    public int getAsInt() { return bits; }

    public String getAsString() { return Integer.toString(bits); }

    public static Bitness fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
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