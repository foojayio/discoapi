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

import io.foojay.api.distribution.AOJ;
import io.foojay.api.distribution.AOJ_OPENJ9;
import io.foojay.api.distribution.Corretto;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.distribution.Dragonwell;
import io.foojay.api.distribution.Liberica;
import io.foojay.api.distribution.Oracle;
import io.foojay.api.distribution.OracleOpenJDK;
import io.foojay.api.distribution.RedHat;
import io.foojay.api.distribution.SAPMachine;
import io.foojay.api.distribution.Zulu;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public enum Distro implements ApiFeature {
    AOJ("AOJ", "aoj", new AOJ(), 1),
    AOJ_OPENJ9("AOJ OpenJ9", "aoj_openj9", new AOJ_OPENJ9(), 1),
    CORRETTO("Corretto", "corretto", new Corretto(), 6),
    DRAGONWELL("Dragonwell", "dragonwell", new Dragonwell(), 6),
    LIBERICA("Liberica", "liberica", new Liberica(), 1),
    ORACLE_OPEN_JDK("Oracle OpenJDK", "oracle_open_jdk", new OracleOpenJDK(), 6),
    ORACLE("Oracle", "oracle", new Oracle(), 1),
    RED_HAT("Red Hat", "redhat", new RedHat(), 12),
    SAP_MACHINE("SAP Machine", "sap_machine", new SAPMachine(), 6),
    ZULU("Zulu", "zulu", new Zulu(), 1),
    NONE("-", "", null, 0),
    NOT_FOUND("", "", null, 0);

    public  static final String       FIELD_VERSIONS = "versions";
    private        final String       uiString;
    private        final String       apiString;
    private        final Distribution distribution;
    private        final int          minUpdateIntervalInHours;


    Distro(final String uiString, final String apiString, final Distribution distribution, final int minUpdateIntervalInHours) {
        this.uiString                 = uiString;
        this.apiString                = apiString;
        this.distribution             = distribution;
        this.minUpdateIntervalInHours = minUpdateIntervalInHours;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public Distro getDefault() { return Distro.NONE; }

    @Override public Distro getNotFound() { return Distro.NOT_FOUND; }

    @Override public Distro[] getAll() { return values(); }

    public String getName() { return name().toUpperCase(); }

    public static Distribution distributionFromText(final String text) { return fromText(text).get(); }

    public static Distro fromText(final String text) {
        switch (text) {
            case "aoj":
            case "AOJ":
            case "adopt":
            case "ADOPT":
            case "adoptopenjdk":
            case "Adopt":
            case "AdoptOpenJDK":
                return AOJ;
            case "aoj_openj9":
            case "AOJ_OPENJ9":
            case "adopt_openj9":
            case "ADOPT_OPENJ9":
            case "adoptopenjdk_openj9":
            case "Adopt_OpenJ9":
            case "AdoptOpenJDK_OpenJ9":
                return AOJ_OPENJ9;
            case "corretto":
            case "CORRETTO":
            case "Corretto":
                return CORRETTO;
            case "dragonwell":
            case "DRAGONWELL":
            case "Dragonwell":
                return DRAGONWELL;
            case "liberica":
            case "LIBERICA":
            case "Liberica":
                return LIBERICA;
            case "sap_machine":
            case "sapmachine":
            case "SAPMACHINE":
            case "SAP_MACHINE":
            case "SAPMachine":
            case "SAP Machine":
            case "sap-machine":
            case "SAP-Machine":
            case "SAP-MACHINE":
                return SAP_MACHINE;
            case "zulu":
            case "ZULU":
            case "Zulu":
                return ZULU;
            case "oracle_open_jdk":
            case "ORACLE_OPEN_JDK":
            case "oracle_openjdk":
            case "ORACLE_OPENJDK":
            case "Oracle_OpenJDK":
            case "Oracle OpenJDK":
            case "oracle openjdk":
            case "ORACLE OPENJDK":
            case "open_jdk":
            case "openjdk":
            case "OpenJDK":
            case "Open JDK":
            case "OPEN_JDK":
            case "open-jdk":
            case "OPEN-JDK":
            case "Oracle-OpenJDK":
            case "oracle-openjdk":
            case "ORACLE-OPENJDK":
            case "oracle-open-jdk":
            case "ORACLE-OPEN-JDK":
                return ORACLE_OPEN_JDK;
            case "oracle":
            case "Oracle":
            case "ORACLE":
                return ORACLE;
            case "RedHat":
            case "redhat":
            case "REDHAT":
            case "Red Hat":
            case "red hat":
            case "RED HAT":
            case "Red_Hat":
            case "red_hat":
            case "red-hat":
            case "Red-Hat":
            case "RED-HAT":
                return RED_HAT;
            default:
                return NOT_FOUND;
        }
    }

    public Distribution get() { return distribution; }

    public int getMinUpdateIntervalInHours() { return minUpdateIntervalInHours; }

    public static List<Distribution> getDistributions() {
        return Arrays.stream(values())
                     .filter(distro -> Distro.NONE != distro)
                     .filter(distro -> Distro.NOT_FOUND != distro)
                     .map(Distro::get).collect(Collectors.toList());
    }

    public static List<Distro> getAsList() { return Arrays.asList(values()); }

    @Override public String toString() {
        if (Distro.NOT_FOUND == Distro.this || Distro.NONE == Distro.this) { return ""; }
        final List<SemVer> versions = get().getVersions();
        StringBuilder distributionMsgBuilder = new StringBuilder().append("{").append("\n")
                                                                  .append("  \"name\"").append(":").append("\"").append(uiString).append("\"").append(",\n")
                                                                  .append("  \"api_parameter\"").append(":").append("\"").append(apiString).append("\"").append(",\n")
                                                                  .append("  \"").append(FIELD_VERSIONS).append("\"").append(": [").append(versions.isEmpty() ? "" : "\n");
        versions.forEach(versionNumber -> distributionMsgBuilder.append("    \"").append(versionNumber).append("\"").append(",\n"));
        if (!versions.isEmpty()) {
            distributionMsgBuilder.setLength(distributionMsgBuilder.length() - 2);
            distributionMsgBuilder.append("\n")
                                  .append("  ]\n");
        } else {
            distributionMsgBuilder.append("]\n");
        }

        return distributionMsgBuilder.append("}")
                                     .toString();
    }
}
