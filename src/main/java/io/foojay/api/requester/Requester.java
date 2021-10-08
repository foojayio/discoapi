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

package io.foojay.api.requester;

import io.foojay.api.pkg.ApiFeature;

import java.util.Arrays;
import java.util.List;


public enum Requester implements ApiFeature {
    JBANG("JBang", "jbang"),
    NONE("-", ""),
    NOT_FOUND("", "");

    final String uiString;
    final String apiString;


    Requester(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    public String getUiString() { return uiString; }

    public String getApiString() { return apiString; }

    @Override public Requester getDefault() { return NONE; }

    @Override public ApiFeature getNotFound() { return NOT_FOUND; }

    @Override public ApiFeature[] getAll() { return values(); }

    public static Requester fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch (text) {
            case "jbang":
            case "JBANG":
            case "JBang":
                return JBANG;
            default:
                return NOT_FOUND;
        }
    }

    public static List<Requester> getAsList() { return Arrays.asList(values()); }

    @Override public String toString() { return new StringBuilder("\"").append(apiString).append("\"").toString(); }
}
