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


public enum PackageType implements ApiFeature {
    JDK("JDK", "jdk"),
    JRE("JRE", "jre"),
    NONE("-", ""),
    NOT_FOUND("", "");

    private final String uiString;
    private final String apiString;


    PackageType(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    public String getApiString() { return apiString; }

    @Override public PackageType getDefault() { return PackageType.NONE; }

    @Override public PackageType getNotFound() { return PackageType.NOT_FOUND; }

    @Override public PackageType[] getAll() { return values(); }

    @Override public String toString(final OutputFormat outputFormat) {
        StringBuilder msgBuilder = new StringBuilder();
        switch(outputFormat) {
            case FULL, REDUCED, REDUCED_ENRICHED -> {
                msgBuilder.append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                          .append(INDENTED_QUOTES).append("name").append(QUOTES).append(COLON).append(QUOTES).append(name()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append("ui_string").append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append("api_string").append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(NEW_LINE)
                          .append(CURLY_BRACKET_CLOSE);
            }
            case FULL_COMPRESSED, REDUCED_COMPRESSED, REDUCED_ENRICHED_COMPRESSED, REDUCED_MINIMIZED -> {
                msgBuilder.append(CURLY_BRACKET_OPEN)
                          .append(QUOTES).append("name").append(QUOTES).append(COLON).append(QUOTES).append(name()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append("ui_string").append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append("api_string").append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES)
                          .append(CURLY_BRACKET_CLOSE);
            }
        }
        return msgBuilder.toString();
    }

    @Override public String toString() { return toString(OutputFormat.FULL_COMPRESSED); }

    public static PackageType fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch (text) {
            case "-jdk":
            case "JDK":
            case "jdk":
            case "jdk+fx":
            case "JDK+FX":
                return JDK;
            case "-jre":
            case "JRE":
            case "jre":
            case "jre+fx":
            case "JRE+FX":
                return JRE;
            default:
                return NOT_FOUND;
        }
    }

    public static List<PackageType> getAsList() { return Arrays.asList(values()); }
}
