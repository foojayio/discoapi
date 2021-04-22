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


public enum Feature implements ApiFeature {
    LOOM("Loom", "loom"),
    PANAMA("Panama", "panama"),
    NONE("-", ""),
    NOT_FOUND("", "");

    private final String uiString;
    private final String apiString;


    Feature(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public Feature getDefault() { return Feature.NONE; }

    @Override public Feature getNotFound() { return Feature.NOT_FOUND; }

    @Override public Feature[] getAll() { return values(); }

    public static Feature fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch (text) {
            case "loom":
            case "LOOM":
            case "Loom":
                return LOOM;
            case "panama":
            case "PANAMA":
            case "Panama":
                return PANAMA;
            default:
                return NOT_FOUND;
        }
    }

    public static List<Feature> getAsList() { return Arrays.asList(values()); }

    @Override public String toString() { return new StringBuilder("\"").append(apiString).append("\"").toString(); }
}
