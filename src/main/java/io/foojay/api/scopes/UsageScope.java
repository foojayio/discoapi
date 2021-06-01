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

import java.util.Arrays;
import java.util.List;


public enum UsageScope implements Scope {
    FREE_TO_USE_IN_PRODUCTION("Free to use in production", "free_to_use_in_production"),
    LICENSE_NEEDED_FOR_PRODUCTION("License needed for production", "license_needed_for_production");

    private final String uiString;
    private final String apiString;


    UsageScope(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }


    public static Scope fromText(final String text) {
        switch(text) {
            case "free":
            case "free_to_use":
            case "free_to_use_in_production":
                return FREE_TO_USE_IN_PRODUCTION;
            case "license":
            case "license_needed":
            case "license_needed_for_production":
                return LICENSE_NEEDED_FOR_PRODUCTION;
            default:
                return NOT_FOUND;
        }
    }

    public static List<UsageScope> getAsList() { return Arrays.asList(values()); }
}
