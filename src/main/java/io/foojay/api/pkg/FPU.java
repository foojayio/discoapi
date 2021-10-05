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


public enum FPU implements ApiFeature {
    HARD_FLOAT("hardfloat", "hard_float"),
    SOFT_FLOAT("softfloat", "soft_float"),
    UNKNOWN("unknown", "unknown"),
    NONE("-", ""),
    NOT_FOUND("", "");

    private final String uiString;
    private final String apiString;


    FPU(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public FPU getDefault() { return FPU.UNKNOWN; }

    @Override public FPU getNotFound() { return FPU.NOT_FOUND; }

    @Override public FPU[] getAll() { return values(); }

    public static FPU fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch (text) {
            case "hard_float":
            case "HARD_FLOAT":
            case "hard-float":
            case "HARD-FLOAT":
            case "hardfloat":
            case "HARDFLOAT":
                return HARD_FLOAT;
            case "soft_float":
            case "SOFT_FLOAT":
            case "soft-float":
            case "SOFT-FLOAT":
            case "softfloat":
            case "SOFTFLOAT":
                return SOFT_FLOAT;
            case "unknown":
            case "UNKNOWN":
                return UNKNOWN;
            default:
                return NOT_FOUND;
        }
    }

    public static List<FPU> getAsList() { return Arrays.asList(values()); }
}
