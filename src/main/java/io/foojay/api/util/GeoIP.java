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

package io.foojay.api.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class GeoIP {
    private String countryCode2;
    private String city;


    public GeoIP() {
        this(null, null);
    }
    public GeoIP(final String jsonString) {
        final Gson        gson    = new Gson();
        final JsonElement element = gson.fromJson(jsonString, JsonElement.class);
        if (element instanceof JsonObject) {
            final JsonObject jsonObj = element.getAsJsonObject();
            this.countryCode2 = jsonObj.get(DownloadInfo.FIELD_COUNTRY_CODE2).getAsString();
            this.city         = jsonObj.get(DownloadInfo.FIELD_CITY).getAsString();
        } else {
            this.countryCode2 = null;
            this.city         = null;
        }
    }
    public GeoIP(final String countryCode2, final String city) {
        this.countryCode2 = countryCode2;
        this.city         = city;
    }


    public String getCountryCode2() { return countryCode2; }
    public void setCountryCode2(final String countryCode2) { this.countryCode2 = countryCode2; }

    public String getCity() { return city; }
    public void setCity(final String city) { this.city = city; }


    @Override public String toString() {
        return new StringBuilder().append("{")
                                  .append("\"").append(DownloadInfo.FIELD_COUNTRY_CODE2).append("\"").append(":").append("\"").append(countryCode2).append("\"").append(",")
                                  .append("\"").append(DownloadInfo.FIELD_CITY).append("\"").append(":").append("\"").append(city).append("\"").append(",")
                                  .append("}")
                                  .toString();
    }
}
