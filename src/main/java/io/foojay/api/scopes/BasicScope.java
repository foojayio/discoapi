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

package io.foojay.api.scopes;

import io.foojay.api.pkg.ApiFeature;

import java.util.Arrays;
import java.util.List;


public enum BasicScope implements Scope, ApiFeature {
    PUBLIC("Public", "public"),
    NONE("-", ""),
    NOT_FOUND("", "");

    private final String uiString;
    private final String apiString;


    BasicScope(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public ApiFeature getDefault() { return PUBLIC; }

    @Override public ApiFeature getNotFound() { return BasicScope.NOT_FOUND; }

    @Override public ApiFeature[] getAll() { return values(); }

    @Override public String getName() { return uiString; }

    @Override public String getToken() { return apiString; }


    public static BasicScope fromToken(final String token) { return "public".equals(token) ? PUBLIC : NOT_FOUND; }

    public static List<BasicScope> getAsList() { return Arrays.asList(values()); }
}
