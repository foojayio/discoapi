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
 * You should have received a copy of the GNU General Public License
 * along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api.pkg;


import java.util.Arrays;
import java.util.List;


public enum PackageType implements ApiFeature {
    JDK("JDK", "jdk"),
    JRE("JRE", "jre"),
    NONE("-", ""),
    NOT_FOUND("", "");

    private final String uiString;
    private final String apiString;


    PackageType(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    public String getApiString() { return apiString; }

    @Override public PackageType getDefault() { return PackageType.NONE; }

    @Override public PackageType getNotFound() { return PackageType.NOT_FOUND; }

    @Override public PackageType[] getAll() { return values(); }

    public static PackageType fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch (text) {
            case "-jdk":
            case "JDK":
            case "jdk":
            case "jdk+fx":
            case "JDK+FX":
                return JDK;
            case "-jre":
            case "JRE":
            case "jre":
            case "jre+fx":
            case "JRE+FX":
                return JRE;
            default:
                return NOT_FOUND;
        }
    }

    public static List<PackageType> getAsList() { return Arrays.asList(values()); }
}
