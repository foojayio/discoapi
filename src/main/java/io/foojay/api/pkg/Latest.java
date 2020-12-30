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

public enum Latest {
    OVERALL("overall", "overall"),
    PER_DISTRIBUTION("per distribution", "per_distro"),
    PER_VERSION("per version", "per_version"),
    NONE("-", ""),
    NOT_FOUND("", "");;

    private final String uiString;
    private final String apiString;

    Latest(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    public String getUiString() {
        return uiString;
    }

    public String getApiString() { return apiString; }

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
            default:
                return NOT_FOUND;
        }
    }
}
