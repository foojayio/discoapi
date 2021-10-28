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


public enum Latest implements ApiFeature{
    ALL_OF_VERSION("all of version", "all_of_version"),
    OVERALL("overall", "overall"),
    PER_DISTRIBUTION("per distribution", "per_distro"),
    PER_VERSION("per version", "per_version"),
    AVAILABLE("available", "available"),
    NONE("-", ""),
    NOT_FOUND("", "");;

    private final String uiString;
    private final String apiString;

    Latest(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() {
        return uiString;
    }

    @Override public String getApiString() { return apiString; }

    @Override public Latest getDefault() { return Latest.NONE; }

    @Override public Latest getNotFound() { return Latest.NOT_FOUND; }

    @Override public ApiFeature[] getAll() { return values(); }

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

    public static Latest fromText(final String text) {
        switch (text) {
            case "per_distro":
            case "per-distro":
            case "per-distribution":
            case "per_distribution":
            case "perdistro":
            case "PER_DISTRO":
            case "PER-DISTRO":
            case "PER_DISTRIBUTION":
            case "PER-DISTRIBUTION":
            case "PERDISTRO":
                return PER_DISTRIBUTION;
            case "overall":
            case "OVERALL":
            case "in_general":
            case "in-general":
            case "IN_GENERAL":
            case "IN-GENERAL":
                return OVERALL;
            case "per_version":
            case "per-version":
            case "perversion":
            case "PER_VERSION":
            case "PER-VERSION":
            case "PERVERSION":
                return PER_VERSION;
            case "available":
            case "AVAILABLE":
            case "Available":
                return AVAILABLE;
            case "all_of_version":
            case "ALL_OF_VERSION":
                return ALL_OF_VERSION;
            default:
                return NOT_FOUND;
        }
    }

    public static List<Latest> getAsList() { return Arrays.asList(values()); }
}
