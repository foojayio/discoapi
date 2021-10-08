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

import java.util.HashMap;
import java.util.Map;


public class JBang {
    private final String                         version;
    private final String                         environment;
    private final String                         environmentVersion;
    private final String                         architecture;
    private final String                         versionNumber;
    private final String                         vendor;
    private final Map<String, Map<String, Long>> downloads;


    public JBang(final String version, final String environment, final String environmentVersion, final String architecture, final String versionNumber, final String vendor) {
        this.version            = version;
        this.environment        = environment;
        this.environmentVersion = environmentVersion;
        this.architecture       = architecture;
        this.versionNumber      = versionNumber;
        this.vendor             = vendor;
        downloads               = new HashMap<>();
    }


    public String getVersion() { return version; }

    public String getEnvironment() { return environment; }

    public String getEnvironmentVersion() { return environmentVersion; }

    public String getArchitecture() { return architecture; }

    public String getVersionNumber() { return versionNumber; }

    public String getVendor() { return vendor; }

    public Map<String, Map<String, Long>> getDownloads() { return downloads; }


    public String toHeaderText() {
        return new StringBuilder("JBang").append("/")
                                         .append(version)
                                         .append(" (")
                                         .append(environment)
                                         .append("/")
                                         .append(environmentVersion)
                                         .append("/")
                                         .append(architecture)
                                         .append(") Java/")
                                         .append(versionNumber)
                                         .append("/")
                                         .append(vendor)
                                         .toString();
    }

    @Override public String toString() {
        StringBuilder msgBuilder = new StringBuilder().append("{")
                                                      .append("\"").append("jbang").append("\":\"").append(version).append("\",")
                                                      .append("\"").append("environment").append("\":\"").append(environment).append("\",")
                                                      .append("\"").append("environment_version").append("\":\"").append(environmentVersion).append("\",")
                                                      .append("\"").append("architecture").append("\":\"").append(architecture).append("\",")
                                                      .append("\"").append("java").append("\":\"").append(versionNumber).append("\",")
                                                      .append("\"").append("vendor").append("\":\"").append(vendor).append("\",")
                                                      .append("\"").append("downloads").append("\": [");
        downloads.entrySet().forEach(entry1 -> {
            msgBuilder.append("{")
                      .append("\"").append("jdk").append("\":").append("\"").append(entry1.getKey()).append("\",");
            entry1.getValue().entrySet().forEach(entry2 -> {
                msgBuilder.append("\"").append(entry2.getKey()).append("\":").append(entry2.getValue()).append(",");
            });
            if (entry1.getValue().size() > 0) { msgBuilder.setLength(msgBuilder.length() - 1); }
            msgBuilder.append("}").append(",");
        });
        if (downloads.size() > 0) { msgBuilder.setLength(msgBuilder.length() - 1); }

        msgBuilder.append("]")
                  .append("}");
        return msgBuilder.toString();
    }
}
