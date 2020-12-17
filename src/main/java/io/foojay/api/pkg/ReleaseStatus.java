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


public enum ReleaseStatus implements ApiFeature {
    GA("General Access", "ga",""),
    EA("Early Access", "ea", "-ea"),
    NONE("-", "", ""),
    NOT_FOUND("", "", "");

    private final String uiString;
    private final String apiString;
    private final String preReleaseId;


    ReleaseStatus(final String uiString, final String apiString, final String preReleaseId) {
        this.uiString     = uiString;
        this.apiString    = apiString;
        this.preReleaseId = preReleaseId;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public ReleaseStatus getDefault() { return ReleaseStatus.NONE; }

    @Override public ReleaseStatus getNotFound() { return ReleaseStatus.NOT_FOUND; }

    @Override public ReleaseStatus[] getAll() { return values(); }

    public static ReleaseStatus fromText(final String text) {
        switch (text) {
            case "-ea":
            case "-EA":
            case "_ea":
            case "_EA":
            case "ea":
            case "EA":
            case "ea_":
            case "EA_":
                return EA;
            case "-ga":
            case "-GA":
            case "_ga":
            case "_GA":
            case "ga":
            case "GA":
            case "ga_":
            case "GA_":
                return GA;
            default:
                return NOT_FOUND;
        }
    }

    public String getPreReleaseId() { return preReleaseId; }

    public static List<ReleaseStatus> getAsList() { return Arrays.asList(values()); }
}