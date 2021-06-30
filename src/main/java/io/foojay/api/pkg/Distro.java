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

import io.foojay.api.distribution.AOJ;
import io.foojay.api.distribution.AOJ_OPENJ9;
import io.foojay.api.distribution.JetBrains;
import io.foojay.api.distribution.Temurin;
import io.foojay.api.distribution.Corretto;
import io.foojay.api.distribution.Distribution;
import io.foojay.api.distribution.Dragonwell;
import io.foojay.api.distribution.GraalVMCE16;
import io.foojay.api.distribution.GraalVMCE11;
import io.foojay.api.distribution.GraalVMCE8;
import io.foojay.api.distribution.Liberica;
import io.foojay.api.distribution.LibericaNative;
import io.foojay.api.distribution.Mandrel;
import io.foojay.api.distribution.Microsoft;
import io.foojay.api.distribution.OJDKBuild;
import io.foojay.api.distribution.OpenLogic;
import io.foojay.api.distribution.Oracle;
import io.foojay.api.distribution.OracleOpenJDK;
import io.foojay.api.distribution.RedHat;
import io.foojay.api.distribution.SAPMachine;
import io.foojay.api.distribution.Trava;
import io.foojay.api.distribution.Zulu;
import io.foojay.api.distribution.ZuluPrime;
import io.foojay.api.scopes.BuildScope;
import io.foojay.api.scopes.UsageScope;
import io.foojay.api.util.OutputFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.foojay.api.util.Constants.COLON;
import static io.foojay.api.util.Constants.COMMA;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.INDENT;
import static io.foojay.api.util.Constants.INDENTED_QUOTES;
import static io.foojay.api.util.Constants.NEW_LINE;
import static io.foojay.api.util.Constants.QUOTES;
import static io.foojay.api.util.Constants.REVERSE_SCOPE_LOOKUP;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_OPEN;


public enum Distro implements ApiFeature {
    AOJ("AOJ", "aoj", new AOJ(), 60),
    AOJ_OPENJ9("AOJ OpenJ9", "aoj_openj9", new AOJ_OPENJ9(), 60),
    CORRETTO("Corretto", "corretto", new Corretto(), 720),
    DRAGONWELL("Dragonwell", "dragonwell", new Dragonwell(), 1440),
    GRAALVM_CE8("Graal VM CE 8", "graalvm_ce8", new GraalVMCE8(), 720),
    GRAALVM_CE11("Graal VM CE 11", "graalvm_ce11", new GraalVMCE11(), 720),
    GRAALVM_CE16("Graal VM CE 16", "graalvm_ce16", new GraalVMCE16(), 725),
    JETBRAINS("JetBrains", "jetbrains", new JetBrains(), 360),
    LIBERICA("Liberica", "liberica", new Liberica(), 60),
    LIBERICA_NATIVE("Liberica Native", "liberica_native", new LibericaNative(), 720),
    MANDREL("Mandrel", "mandrel", new Mandrel(), 1440),
    MICROSOFT("Microsoft OpenJDK", "microsoft", new Microsoft(), 720),
    OJDK_BUILD("OJDKBuild", "ojdk_build", new OJDKBuild(), 1440),
    OPEN_LOGIC("OpenLogic", "openlogic", new OpenLogic(), 1440),
    ORACLE_OPEN_JDK("Oracle OpenJDK", "oracle_open_jdk", new OracleOpenJDK(), 180),
    ORACLE("Oracle", "oracle", new Oracle(), 720),
    RED_HAT("Red Hat", "redhat", new RedHat(), 720),
    SAP_MACHINE("SAP Machine", "sap_machine", new SAPMachine(), 720),
    TEMURIN("Temurin", "temurin", new Temurin(), 60),
    TRAVA("Trava", "trava", new Trava(), 1440),
    ZULU("Zulu", "zulu", new Zulu(), 15),
    ZULU_PRIME("ZuluPrime", "zulu_prime", new ZuluPrime(), 360),
    NONE("-", "", null, 0),
    NOT_FOUND("", "", null, 0);

    public  static final String       FIELD_NAME                = "name";
    public  static final String       FIELD_API_PARAMETER       = "api_parameter";
    public  static final String       FIELD_HASH_ALGORITHM      = "hash_algorithm";
    public  static final String       FIELD_HASH_URI            = "hash_uri";
    public  static final String       FIELD_SIGNATURE_TYPE      = "signature_type";
    public  static final String       FIELD_SIGNATURE_ALGORITHM = "signature_algorithm";
    public  static final String       FIELD_SIGNATURE_URI       = "signature_uri";
    public  static final String       FIELD_SYNONYMS            = "synonyms";
    public  static final String       FIELD_VERSIONS            = "versions";
    private        final String       uiString;
    private        final String       apiString;
    private        final Distribution distribution;
    private        final int          minUpdateIntervalInMinutes;
    public         final AtomicReference<Instant> lastUpdate;
    public         final AtomicReference<Duration> lastUpdateDuration;


    Distro(final String uiString, final String apiString, final Distribution distribution, final int minUpdateIntervalInMinutes) {
        this.uiString                   = uiString;
        this.apiString                  = apiString;
        this.distribution               = distribution;
        this.minUpdateIntervalInMinutes = minUpdateIntervalInMinutes;
        this.lastUpdate                 = new AtomicReference<>();
        this.lastUpdateDuration         = new AtomicReference<>(Duration.ofSeconds(0));
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public Distro getDefault() { return Distro.NONE; }

    @Override public Distro getNotFound() { return Distro.NOT_FOUND; }

    @Override public Distro[] getAll() { return values(); }

    public String getName() { return name().toUpperCase(); }

    public static Distribution distributionFromText(final String text) { return fromText(text).get(); }

    public static Distro fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch (text) {
            case "zulu":
            case "ZULU":
            case "Zulu":
            case "zulucore":
            case "ZULUCORE":
            case "ZuluCore":
            case "zulu_core":
            case "ZULU_CORE":
            case "Zulu_Core":
            case "zulu core":
            case "ZULU CORE":
            case "Zulu Core":
                return ZULU;
            case "zing":
            case "ZING":
            case "Zing":
            case "prime":
            case "PRIME":
            case "Prime":
            case "zuluprime":
            case "ZULUPRIME":
            case "ZuluPrime":
            case "zulu_prime":
            case "ZULU_PRIME":
            case "Zulu_Prime":
            case "zulu prime":
            case "ZULU PRIME":
            case "Zulu Prime":
                return ZULU_PRIME;
            case "aoj":
            case "AOJ":
                return AOJ;
            case "aoj_openj9":
            case "AOJ_OpenJ9":
            case "AOJ_OPENJ9":
            case "AOJ OpenJ9":
            case "AOJ OPENJ9":
            case "aoj openj9":
                return AOJ_OPENJ9;
            case "corretto":
            case "CORRETTO":
            case "Corretto":
                return CORRETTO;
            case "dragonwell":
            case "DRAGONWELL":
            case "Dragonwell":
                return DRAGONWELL;
            case "graalvm_ce8":
            case "graalvmce8":
            case "GraalVM CE 8":
            case "GraalVMCE8":
            case "GraalVM_CE8":
                return GRAALVM_CE8;
            case "graalvm_ce11":
            case "graalvmce11":
            case "GraalVM CE 11":
            case "GraalVMCE11":
            case "GraalVM_CE11":
                return GRAALVM_CE11;
            case "graalvm_ce16":
            case "graalvmce16":
            case "GraalVM CE 16":
            case "GraalVMCE16":
            case "GraalVM_CE16":
                return GRAALVM_CE16;
            case "jetbrains":
            case "JetBrains":
            case "JETBRAINS":
                return JETBRAINS;
            case "liberica":
            case "LIBERICA":
            case "Liberica":
                return LIBERICA;
            case "liberica_native":
            case "LIBERICA_NATIVE":
            case "libericaNative":
            case "LibericaNative":
            case "liberica native":
            case "LIBERICA NATIVE":
            case "Liberica Native":
                return LIBERICA_NATIVE;
            case "mandrel":
            case "MANDREL":
            case "Mandrel":
                return MANDREL;
            case "microsoft":
            case "Microsoft":
            case "MICROSOFT":
            case "Microsoft OpenJDK":
            case "Microsoft Build of OpenJDK":
                return MICROSOFT;
            case "ojdk_build":
            case "OJDK_BUILD":
            case "OJDK Build":
            case "ojdk build":
            case "ojdkbuild":
            case "OJDKBuild":
                return OJDK_BUILD;
            case "openlogic":
            case "OPENLOGIC":
            case "OpenLogic":
            case "open_logic":
            case "OPEN_LOGIC":
            case "Open Logic":
            case "OPEN LOGIC":
            case "open logic":
                return OPEN_LOGIC;
            case "oracle":
            case "Oracle":
            case "ORACLE":
                return ORACLE;
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
            case "temurin":
            case "Temurin":
            case "TEMURIN":
                return TEMURIN;
            case "trava":
            case "TRAVA":
            case "Trava":
            case "trava_openjdk":
            case "TRAVA_OPENJDK":
            case "trava openjdk":
            case "TRAVA OPENJDK":
                return TRAVA;
            default:
                return NOT_FOUND;
        }
    }

    public Distribution get() { return distribution; }

    public int getMinUpdateIntervalInMinutes() { return minUpdateIntervalInMinutes; }

    public static List<Distribution> getDistributions() {
        return Arrays.stream(values())
                     .filter(distro -> Distro.NONE != distro)
                     .filter(distro -> Distro.NOT_FOUND != distro)
                     .map(Distro::get).collect(Collectors.toList());
    }

    public static List<Distro> getAsList() { return Arrays.asList(values()); }

    public static List<Distro> getAsListWithoutNoneAndNotFound() {
        return getAsList().stream()
                          .filter(distro -> Distro.NONE != distro)
                          .filter(distro -> Distro.NOT_FOUND != distro)
                          .sorted(Comparator.comparing(Distro::name).reversed())
                          .collect(Collectors.toList());
    }

    public static List<Distro> getPublicDistros() {
        return Arrays.stream(values())
                     .filter(distro -> Distro.NONE       != distro)
                     .filter(distro -> Distro.NOT_FOUND  != distro)
                     .collect(Collectors.toList());
    }

    public static List<Distro> getPublicDistrosDirectlyDownloadable() {
        return Arrays.stream(values())
                     .filter(distro -> Distro.NONE       != distro)
                     .filter(distro -> Distro.NOT_FOUND  != distro)
                     .filter(distro -> Distro.ORACLE     != distro)
                     .filter(distro -> Distro.RED_HAT    != distro)
                     .collect(Collectors.toList());
    }

    public static List<Distro> getDistrosWithJavaVersioning() {
        return Arrays.stream(values())
                     .filter(distro -> Distro.NONE         != distro)
                     .filter(distro -> Distro.NOT_FOUND    != distro)
                     .filter(distro -> Distro.GRAALVM_CE16 != distro)
                     .filter(distro -> Distro.GRAALVM_CE11 != distro)
                     .filter(distro -> Distro.GRAALVM_CE8  != distro)
                     .filter(distro -> Distro.MANDREL      != distro)
                     .collect(Collectors.toList());
    }

    public static List<Distro> getDistributionsBasedOnOpenJDK() {
        return REVERSE_SCOPE_LOOKUP.get(BuildScope.BUILD_OF_OPEN_JDK);
    }

    public static List<Distro> getDistributionsBasedOnGraalVm() {
        return REVERSE_SCOPE_LOOKUP.get(BuildScope.BUILD_OF_GRAALVM);
    }

    public static List<Distro> getDistributionsFreeForProduction() {
        return REVERSE_SCOPE_LOOKUP.get(UsageScope.FREE_TO_USE_IN_PRODUCTION);
    }


    public String toString(final OutputFormat outputFormat) {
        return toString(outputFormat, false);
    }
    public String toString(final OutputFormat outputFormat, final boolean latest_per_update) {
        final StringBuilder msgBuilder = new StringBuilder();
        final List<SemVer> versions;
        if (latest_per_update) {
            final List<SemVer> allVersions = get().getVersions();
            versions = allVersions.stream()
                                  .map(semver -> semver.getVersionNumber().toString(OutputFormat.REDUCED_COMPRESSED, true, false))
                                  .collect(Collectors.toList())
                                  .stream()
                                  .distinct()
                                  .map(vtext -> SemVer.fromText(vtext).getSemVer1())
                                  .collect(Collectors.toSet())
                                  .stream()
                                  .map(unique -> allVersions.stream()
                                                            .filter(semver -> semver.getVersionNumber().equals(unique.getVersionNumber()))
                                                            .max(Comparator.comparing(SemVer::getVersionNumber)))
                                  .filter(Optional::isPresent)
                                  .map(Optional::get)
                                  .sorted(Comparator.comparing(SemVer::getVersionNumber).reversed())
                                  .collect(Collectors.toList());
        } else {
            versions = get().getVersions();
        }

        List<String> synonyms = get().getSynonyms();

        switch(outputFormat) {
            case FULL:
                msgBuilder.append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                          .append(INDENTED_QUOTES).append(FIELD_NAME).append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(FIELD_API_PARAMETER).append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_HASH_ALGORITHM).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getHashAlgorithm().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_HASH_URI).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getHashUri()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_SIGNATURE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureType().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_SIGNATURE_ALGORITHM).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureAlgorithm().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureUri()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(FIELD_SYNONYMS).append(QUOTES).append(COLON).append(" ").append(SQUARE_BRACKET_OPEN).append(synonyms.isEmpty() ? "" : NEW_LINE);
                synonyms.forEach(synonym -> msgBuilder.append(INDENT).append(INDENTED_QUOTES).append(synonym).append(QUOTES).append(COMMA_NEW_LINE));
                if (!synonyms.isEmpty()) {
                    msgBuilder.setLength(msgBuilder.length() - 2);
                    msgBuilder.append(NEW_LINE)
                              .append(INDENT).append(SQUARE_BRACKET_CLOSE).append(COMMA_NEW_LINE);
                } else {
                    msgBuilder.append(SQUARE_BRACKET_CLOSE).append(COMMA_NEW_LINE);
                }

                msgBuilder.append(INDENTED_QUOTES).append(FIELD_VERSIONS).append(QUOTES).append(COLON).append(" ").append(SQUARE_BRACKET_OPEN).append(versions.isEmpty() ? "" : NEW_LINE);
                versions.forEach(versionNumber -> msgBuilder.append(INDENT).append(INDENTED_QUOTES).append(versionNumber).append(QUOTES).append(COMMA_NEW_LINE));
        if (!versions.isEmpty()) {
                    msgBuilder.setLength(msgBuilder.length() - 2);
                    msgBuilder.append(NEW_LINE)
                              .append(INDENT).append(SQUARE_BRACKET_CLOSE).append(NEW_LINE);
        } else {
                    msgBuilder.append(SQUARE_BRACKET_CLOSE).append(NEW_LINE);
                }
                return msgBuilder.append(CURLY_BRACKET_CLOSE).toString();
            case FULL_COMPRESSED:
                msgBuilder.append(CURLY_BRACKET_OPEN)
                          .append(QUOTES).append(FIELD_NAME).append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(FIELD_API_PARAMETER).append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_HASH_ALGORITHM).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getHashAlgorithm().getApiString()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_HASH_URI).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getHashUri()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_SIGNATURE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureType().getApiString()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_SIGNATURE_ALGORITHM).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureAlgorithm().getApiString()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureUri()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(FIELD_SYNONYMS).append(QUOTES).append(COLON).append(SQUARE_BRACKET_OPEN);
                synonyms.forEach(synonym -> msgBuilder.append(QUOTES).append(synonym).append(QUOTES).append(COMMA));
                if (!synonyms.isEmpty()) {
                    msgBuilder.setLength(msgBuilder.length() - 1);
                }
                msgBuilder.append(SQUARE_BRACKET_CLOSE).append(COMMA);

                msgBuilder.append(QUOTES).append(FIELD_VERSIONS).append(QUOTES).append(COLON).append(SQUARE_BRACKET_OPEN);
                versions.forEach(versionNumber -> msgBuilder.append(QUOTES).append(versionNumber).append(QUOTES).append(COMMA));
                if (!versions.isEmpty()) {
                    msgBuilder.setLength(msgBuilder.length() - 1);
                }
                    msgBuilder.append(SQUARE_BRACKET_CLOSE);
                return msgBuilder.append(CURLY_BRACKET_CLOSE).toString();
            case REDUCED:
                msgBuilder.append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                          .append(INDENTED_QUOTES).append(FIELD_NAME).append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(FIELD_API_PARAMETER).append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_HASH_ALGORITHM).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getHashAlgorithm().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_HASH_URI).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getHashUri()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_SIGNATURE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureType().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_SIGNATURE_ALGORITHM).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureAlgorithm().getApiString()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append(Distro.FIELD_SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureUri()).append(QUOTES).append(NEW_LINE)
                          .append(CURLY_BRACKET_CLOSE);
                return msgBuilder.toString();
            case REDUCED_COMPRESSED:
            default:
                msgBuilder.append(CURLY_BRACKET_OPEN)
                          .append(QUOTES).append(FIELD_NAME).append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(FIELD_API_PARAMETER).append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_HASH_ALGORITHM).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getHashAlgorithm().getApiString()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_HASH_URI).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getHashUri()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_SIGNATURE_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureType().getApiString()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_SIGNATURE_ALGORITHM).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureAlgorithm().getApiString()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append(Distro.FIELD_SIGNATURE_URI).append(QUOTES).append(COLON).append(QUOTES).append(distribution.getSignatureUri()).append(QUOTES)
                          .append(CURLY_BRACKET_CLOSE);
                return msgBuilder.toString();
        }
        }

    @Override public String toString() {
        return toString(OutputFormat.FULL);
    }
}
