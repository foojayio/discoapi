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

import java.util.Arrays;
import java.util.List;


public enum Match implements ApiFeature {
    ANY("Any", "any"),
    ALL("All", "all"),
    NONE("-", ""),
    NOT_FOUND("", "");

    private final String uiString;
    private final String apiString;


    Match(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public Match getDefault() { return Match.ANY; }

    @Override public Match getNotFound() { return Match.ANY; }

    @Override public Match[] getAll() { return values(); }

    public static Match fromText(final String text) {
        if (null == text) { return ANY; }
        switch (text) {
            case "any":
            case "ANY":
            case "Any":
                return ANY;
            case "all":
            case "ALL":
            case "All":
                return ALL;
            default:
                return ANY;
        }
    }

    public static List<Match> getAsList() { return Arrays.asList(values()); }
}
