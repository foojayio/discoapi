/*
 * Copyright (c) 2020.
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


public enum TermOfSupport implements ApiFeature {
    STS("short term stable", "sts"),
    MTS("mid term stable", "mts"),
    LTS("long term stable", "lts"),
    NONE("-", ""),
    NOT_FOUND("", "");

    private final String uiString;
    private final String apiString;


    TermOfSupport(final String uiString, final String apiString) {
        this.uiString = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public TermOfSupport getDefault() { return TermOfSupport.NONE; }

    @Override public TermOfSupport getNotFound() { return TermOfSupport.NOT_FOUND; }

    @Override public TermOfSupport[] getAll() { return values(); }

    public static TermOfSupport fromText(final String text) {
        switch(text) {
            case "long_term_stable":
            case "LongTermStable":
            case "lts":
            case "LTS":
            case "Lts":
                return LTS;
            case "mid_term_stable":
            case "MidTermStable":
            case "mts":
            case "MTS":
            case "Mts":
                return MTS;
            case "short_term_stable":
            case "ShortTermStable":
            case "sts":
            case "STS":
            case "Sts":
                return STS;
            default: return NOT_FOUND;

        }
    }

    public static List<TermOfSupport> getAsList() { return Arrays.asList(values()); }
}