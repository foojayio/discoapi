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


import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.scopes.BuildScope;
import io.foojay.api.scopes.DownloadScope;
import io.foojay.api.scopes.IDEScope;
import io.foojay.api.scopes.BasicScope;
import io.foojay.api.scopes.QualityScope;
import io.foojay.api.scopes.Scope;
import io.foojay.api.scopes.UsageScope;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public class Constants {
    public static final String        PACKAGES_COLLECTION                        = "packages";
    public static final String        DOWNLOADS_COLLECTION                       = "downloads";
    public static final String        DOWNLOADS_USER_AGENT_COLLECTION            = "downloadsUserAgent";
    public static final String        DOWNLOADS_PER_DAY_COLLECTION               = "downloadsPerDay";
    public static final String        DISTRO_UPDATES_COLLECTION                  = "distroupdates";
    public static final String        EPHEMERAL_IDS_COLLECTION                   = "ephemeralIds";
    public static final String        SHEDLOCK_COLLECTION                        = "shedLock";
    public static final String        STATE_COLLECTION                           = "state";
    public static final String        SENTINEL_COLLECTION                        = "sentinel";
    public static final String        MAJOR_VERSIONS_COLLECTION                  = "majorVersions";

    public static final String            ENDPOINT_PACKAGES                      = "packages";
    public static final String            ENDPOINT_EPHEMERAL_IDS                 = "ephemeral_ids";
    public static final String            SWAGGER_UI_URL                         = "https://api.foojay.io/swagger-ui/";

    public static final String            SQUARE_BRACKET_OPEN                    = "[";
    public static final String            SQUARE_BRACKET_CLOSE                   = "]";
    public static final String            CURLY_BRACKET_OPEN                     = "{";
    public static final String            CURLY_BRACKET_CLOSE                    = "}";
    public static final String            INDENTED_QUOTES                        = "  \"";
    public static final String            QUOTES                                 = "\"";
    public static final String            COLON                                  = ":";
    public static final String            COMMA                                  = ",";
    public static final String            SLASH                                  = "/";
    public static final String            NEW_LINE                               = "\n";
    public static final String            COMMA_NEW_LINE                         = ",\n";
    public static final String            INDENT                                 = "  ";

    public static final String            API_VERSION_V1                         = "1.0";
    public static final String            API_VERSION_V2                         = "2.0";

    public static final String            BASE_URL                               = null == Config.INSTANCE.getFoojayApiBaseUrl() ? "https://api.foojay.io/disco" : Config.INSTANCE.getFoojayApiBaseUrl();

    public static final String            MQTT_CLIENT_ID                         = "discoapi-" + Config.INSTANCE.getFoojayApiEnvironment() + "-" + UUID.randomUUID();
    public static final String            MQTT_TOPIC_SEPARATOR                   = "/";
    public static final String            MQTT_PLUS                              = "+";
    public static final String            MQTT_PRESENCE_TOPIC                    = new StringBuilder().append(Config.INSTANCE.getFoojayApiEnvironment()).append(MQTT_TOPIC_SEPARATOR).append("presence").append(MQTT_TOPIC_SEPARATOR).append(MQTT_CLIENT_ID).toString();
    public static final String            MQTT_PKG_UPDATE_TOPIC                  = String.join(MQTT_TOPIC_SEPARATOR, Config.INSTANCE.getFoojayApiEnvironment(), "discoupdater/update/pkg");
    public static final String            MQTT_EPHEMERAL_ID_UPDATE_TOPIC         = String.join(MQTT_TOPIC_SEPARATOR, Config.INSTANCE.getFoojayApiEnvironment(), "discoupdater/update/ephemeral_id");
    public static final String            MQTT_LAST_WILL_TOPIC                   = MQTT_PRESENCE_TOPIC;
    public static final String            MQTT_PKG_UPDATE_STARTED_MSG            = "pkg_update_started";
    public static final String            MQTT_PKG_UPDATE_FINISHED_MSG           = "pkg_update_finished";
    public static final String            MQTT_PKG_UPDATE_FINISHED_EMPTY_MSG     = "pkg_update_finished_empty";
    public static final String            MQTT_EPHEMERAL_ID_UPDATE_STARTED_MSG   = "ephemeral_id_update_started";
    public static final String            MQTT_EPEHMERAL_ID_UPDATE_FINISHED_MSG  = "ephemeral_id_update_finished";
    public static final String            MQTT_FORCE_PKG_UPDATE_MSG              = "force_pkg_update";
    public static final String            MQTT_ONLINE_MSG                        = "1";
    public static final String            MQTT_OFFLINE_MSG                       = "0";

    public static final long              UPDATE_TIMEOUT_IN_MINUTES              = 20;
    public static final long              PRELOAD_TIMEOUT_IN_MINUTES             = 15;
    public static final long              UPLOAD_TIMEOUT_IN_MINUTES              = 10;
    public static final long              SYNCHRONIZING_TIMEOUT_IN_MINUTES       = 15;

    public static final long              ONE_HOUR_IN_SECONDS                    = 3600;

    public static final Pattern           POSITIVE_INTEGER_PATTERN               = Pattern.compile("\\d+");

    public static final String            JDK                                    = "jdk";
    public static final String            JRE                                    = "jre";
    public static final String            JDK_PREFIX                             = "jdk-";
    public static final String            JRE_PREFIX                             = "jre-";
    public static final String            JDK_POSTFIX                            = "-jdk";
    public static final String            JRE_POSTFIX                            = "-jre";
    public static final String            EA_POSTFIX                             = "-ea";
    public static final String            GA_POSTFIX                             = "-ga";
    public static final String            FX_POSTFIX                             = "-fx";
    public static final String            HEADLESS_POSTFIX                       = "-headless";

    public static final String            MAINTAINED_PROPERTIES_URL              = "https://github.com/foojayio/maintained_major_versions/raw/main/maintained.properties";

    public static final String            IP_LOCATION_URL                        = "http://ip-api.com/json/";
    public static final String            COUNTRY_CODE_FIELD                     = "countryCode";

    public static final String            FILE_ENDING_JAR                        = "jar";
    public static final String            FILE_ENDING_TXT                        = "txt";
    public static final String            FILE_ENDING_SHA1                       = "sha1";
    public static final String            FILE_ENDING_SHA256                     = "sha256";
    public static final String            FILE_ENDING_MD5                        = "md5";
    public static final String            FILE_ENDING_SYMBOLS_TAR_GZ             = "symbols.tar.gz";
    public static final String            FILE_ENDING_SIG                        = "sig";

    public static final String            RESULT                                 = "result";
    public static final String            MESSAGE                                = "message";

    public static final String            SENTINEL_PKG_ID                        = "a2a505f4d8956eb730c1ef285b23c269"; //https://cdn.azul.com/zulu/bin/zulu6.2.0.9-ca-jdk6.0.42-linux.x86_64.rpm

    public static final DateTimeFormatter DTF                                    = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public static final Map<String, String> PARAMETER_LOOKUP = new HashMap<>() {{
        put(Pkg.FIELD_ARCHITECTURE, "aarch64, amd64, arm, armel, armhf, arm64, ia64, mips, mipsel, ppc, ppc64el, ppc64le, ppc64, riscv64, s390, s390x, sparc, sparcv9, x64, x86-64, x86, i386, i486, i586, i686, x86-32");
        put(Pkg.FIELD_ARCHIVE_TYPE, "apk, cab, deb, dmg, exe, msi, pkg, rpm, tar, tar.gz, tgz, tar.Z, zip");
        put(Pkg.FIELD_BITNESS, "32, 64");
        put(Pkg.FIELD_FPU, "hard_float, soft_float, unknown");
        put(Pkg.FIELD_DISTRIBUTION, "aoj, aoj_openj9, bisheng, corretto, debian, dragonwell, graalvm_ce8, graalvm_ce11, graalvm_ce16, graalvm_ce17, jetbrains, kona, liberica, liberica_native, mandrel, microsoft, ojdk_build, openlogic, oracle, oracle_open_jdk, redhat, sap_machine, semeru, semeru_certified, temurin, trava, zulu, zulu_prime");
        put(Pkg.FIELD_OPERATING_SYSTEM, "aix, alpine_linux, linux, linux_musl, macos, qnx, solaris, windows");
        put(Pkg.FIELD_LIB_C_TYPE, "c_std_lib, glibc, libc, musl");
        put(Pkg.FIELD_PACKAGE_TYPE, "jdk, jre");
        put(Pkg.FIELD_RELEASE_STATUS, "ea, ga");
        put(Pkg.FIELD_TERM_OF_SUPPORT, "sts, mts, lts");
        put(Pkg.FIELD_FEATURE, "panama, loom, lanai, valhalla");
    }};

    public static final LinkedHashMap<String, ArchiveType> ARCHIVE_TYPE_LOOKUP = new LinkedHashMap<>() {{
        put(".apk", ArchiveType.APK);
        put(".bin", ArchiveType.BIN);
        put(".cab", ArchiveType.CAB);
        put(".deb", ArchiveType.DEB);
        put(".dmg", ArchiveType.DMG);
        put(".msi", ArchiveType.MSI);
        put(".pkg", ArchiveType.PKG);
        put(".rpm", ArchiveType.RPM);
        put(".source.tar.gz", ArchiveType.SRC_TAR);
        put(".src.tar.gz", ArchiveType.SRC_TAR);
        put(".tar.gz", ArchiveType.TAR_GZ);
        put(".tar.Z", ArchiveType.TAR_Z);
        put(".tar", ArchiveType.TAR);
        put(".zip", ArchiveType.ZIP);
        put(".exe", ArchiveType.EXE);
    }};

    public static final LinkedHashMap<String, Architecture> ARCHITECTURE_LOOKUP = new LinkedHashMap<>() {{
        put("ia64", Architecture.IA64);
        put("aarch64", Architecture.AARCH64);
        put("aarch32sf", Architecture.ARM);
        put("aarch32hf", Architecture.ARM);
        put("aarch32", Architecture.ARM);
        put("x86-32", Architecture.X86);
        put("x86_32", Architecture.X86);
        put("x86lx32", Architecture.X86);
        put("x86-64", Architecture.X64);
        put("x86_64", Architecture.X64);
        put("x86lx64", Architecture.X64);
        put("x86", Architecture.X86);
        put("win64", Architecture.X64);
        put("x64", Architecture.X64);
        put("x32", Architecture.X86);
        put("amd64", Architecture.AMD64);
        put("armel", Architecture.ARMEL);
        put("armhf", Architecture.ARMHF);
        put("armv8", Architecture.AARCH64);
        put("arm64", Architecture.ARM64);
        put("armv6", Architecture.ARM);
        put("armv7", Architecture.ARM);
        put("arm32", Architecture.ARM);
        put("arm", Architecture.ARM);
        put("mips", Architecture.MIPS);
        put("mipsel", Architecture.MIPSEL);
        put("i386", Architecture.X86);
        put("i486", Architecture.X86);
        put("i586", Architecture.X86);
        put("i686", Architecture.X86);
        put("s390x", Architecture.S390X);
        put("ppc32spe", Architecture.PPC);
        put("ppc32hf", Architecture.PPC);
        put("ppc64le", Architecture.PPC64LE);
        put("ppc64", Architecture.PPC64);
        put("ppc", Architecture.PPC);
        put("riscv64", Architecture.RISCV64);
        put("sparcv9", Architecture.SPARCV9);
        put("sparc", Architecture.SPARC);
    }};

    public static final LinkedHashMap<String, OperatingSystem> OPERATING_SYSTEM_LOOKUP = new LinkedHashMap<>() {{
        put("darwin", OperatingSystem.MACOS);
        put("windows", OperatingSystem.WINDOWS);
        put("Windows", OperatingSystem.WINDOWS);
        put("win", OperatingSystem.WINDOWS);
        put("alpine-linux", OperatingSystem.ALPINE_LINUX);
        put("Alpine-Linux", OperatingSystem.ALPINE_LINUX);
        put("alpine_linux", OperatingSystem.ALPINE_LINUX);
        put("Alpine_Linux", OperatingSystem.ALPINE_LINUX);
        put("linux-musl", OperatingSystem.ALPINE_LINUX);
        put("Linux-MUSL", OperatingSystem.ALPINE_LINUX);
        put("Linux-Musl", OperatingSystem.ALPINE_LINUX);
        put("linux_musl", OperatingSystem.ALPINE_LINUX);
        put("Linux_MUSL", OperatingSystem.ALPINE_LINUX);
        put("Linux_Musl", OperatingSystem.ALPINE_LINUX);
        put("musl", OperatingSystem.ALPINE_LINUX);
        put("linux", OperatingSystem.LINUX);
        put("Linux", OperatingSystem.LINUX);
        put("unix", OperatingSystem.LINUX);
        put("Unix", OperatingSystem.LINUX);
        put("solaris", OperatingSystem.SOLARIS);
        put("qnx", OperatingSystem.QNX);
        put("aix", OperatingSystem.AIX);
        put("macosx", OperatingSystem.MACOS);
        put("macos", OperatingSystem.MACOS);
        put("osx", OperatingSystem.MACOS);
        put("mac", OperatingSystem.MACOS);
    }};

    public static final LinkedHashMap<String, OperatingSystem> OPERATING_SYSTEM_BY_ARCHIVE_TYPE_LOOKUP = new LinkedHashMap<>() {{
        put("apk", OperatingSystem.LINUX);
        put("deb", OperatingSystem.LINUX);
        put("rpm", OperatingSystem.LINUX);
        put("tar.gz", OperatingSystem.LINUX);
        put("pkg", OperatingSystem.MACOS);
        put("dmg", OperatingSystem.MACOS);
        put("exe", OperatingSystem.WINDOWS);
        put("msi", OperatingSystem.WINDOWS);
        put("bin", OperatingSystem.WINDOWS);
        put("cab", OperatingSystem.WINDOWS);
        put("zip", OperatingSystem.WINDOWS);
    }};

    public static final LinkedHashMap<String, PackageType> PACKAGE_TYPE_LOOKUP = new LinkedHashMap<>() {{
        put("jdk", PackageType.JDK);
        put("-jdk", PackageType.JDK);
        put("jdk-", PackageType.JDK);
        put("JDK", PackageType.JDK);
        put("jre", PackageType.JRE);
        put("-jre", PackageType.JRE);
        put("jre-", PackageType.JRE);
        put("JRE", PackageType.JRE);
        put("serverjre", PackageType.JRE);
    }};

    public static final LinkedHashMap<String, ReleaseStatus> RELEASE_STATUS_LOOKUP = new LinkedHashMap<>() {{
        put("/early_access/", ReleaseStatus.EA);
        put("/EA/", ReleaseStatus.GA);
        put("preview", ReleaseStatus.EA);
        put("-ea", ReleaseStatus.EA);
        put("_ea", ReleaseStatus.EA);
        put("ea-", ReleaseStatus.EA);
        put("ea", ReleaseStatus.EA);
        put("EA", ReleaseStatus.EA);
        put("/GA/", ReleaseStatus.GA);
        put("-ga", ReleaseStatus.GA);
        put("_ga", ReleaseStatus.GA);
        put("ga-", ReleaseStatus.GA);
        put("ga", ReleaseStatus.GA);
        put("GA", ReleaseStatus.GA);
    }};

    public static final ConcurrentHashMap<Distro, List<Scope>> SCOPE_LOOKUP = new ConcurrentHashMap<>() {{
        // Builds of OpenJDK
        put(Distro.AOJ, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.AOJ_OPENJ9, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.BISHENG, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.CORRETTO, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.DEBIAN, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.NOT_DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.DRAGONWELL, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.LIBERICA, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.JETBRAINS, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.KONA, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.MICROSOFT, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.OJDK_BUILD, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.OPEN_LOGIC, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.ORACLE, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, DownloadScope.NOT_DIRECTLY, UsageScope.LICENSE_NEEDED_FOR_PRODUCTION));
        put(Distro.ORACLE_OPEN_JDK, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.RED_HAT, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.NOT_DIRECTLY, UsageScope.LICENSE_NEEDED_FOR_PRODUCTION));
        put(Distro.SAP_MACHINE, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.SEMERU, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.SEMERU_CERTIFIED, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.LICENSE_NEEDED_FOR_PRODUCTION));
        put(Distro.TEMURIN, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.TRAVA, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.ZULU, List.of(BasicScope.PUBLIC, IDEScope.VISUAL_STUDIO_CODE, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.ZULU_PRIME, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_OPEN_JDK, DownloadScope.DIRECTLY, UsageScope.LICENSE_NEEDED_FOR_PRODUCTION));
        // Builds of GraalVM
        put(Distro.GRAALVM_CE8, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_GRAALVM, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.GRAALVM_CE11, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_GRAALVM, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.GRAALVM_CE16, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_GRAALVM, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.GRAALVM_CE17, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_GRAALVM, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.LIBERICA_NATIVE, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_GRAALVM, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
        put(Distro.MANDREL, List.of(BasicScope.PUBLIC, BuildScope.BUILD_OF_GRAALVM, DownloadScope.DIRECTLY, UsageScope.FREE_TO_USE_IN_PRODUCTION));
    }};

    public static final ConcurrentHashMap<Scope, List<Distro>> REVERSE_SCOPE_LOOKUP = new ConcurrentHashMap<>() {{
        put(BasicScope.PUBLIC, List.of(Distro.AOJ, Distro.AOJ_OPENJ9, Distro.BISHENG, Distro.CORRETTO, Distro.DEBIAN, Distro.DRAGONWELL, Distro.GRAALVM_CE8, Distro.GRAALVM_CE11, Distro.GRAALVM_CE16, Distro.GRAALVM_CE17, Distro.JETBRAINS, Distro.KONA, Distro.LIBERICA, Distro.LIBERICA_NATIVE, Distro.MANDREL, Distro.MICROSOFT, Distro.OJDK_BUILD, Distro.OPEN_LOGIC, Distro.ORACLE, Distro.ORACLE_OPEN_JDK, Distro.RED_HAT, Distro.SAP_MACHINE, Distro.SEMERU, Distro.SEMERU_CERTIFIED, Distro.TEMURIN, Distro.TRAVA, Distro.ZULU, Distro.ZULU_PRIME));
        put(DownloadScope.DIRECTLY, List.of(Distro.AOJ, Distro.AOJ_OPENJ9, Distro.BISHENG, Distro.CORRETTO, Distro.DRAGONWELL, Distro.GRAALVM_CE8, Distro.GRAALVM_CE11, Distro.GRAALVM_CE16, Distro.GRAALVM_CE17, Distro.JETBRAINS, Distro.KONA, Distro.LIBERICA, Distro.LIBERICA_NATIVE, Distro.MANDREL, Distro.MICROSOFT, Distro.OJDK_BUILD, Distro.OPEN_LOGIC, Distro.ORACLE, Distro.ORACLE_OPEN_JDK, Distro.SAP_MACHINE, Distro.SEMERU, Distro.SEMERU_CERTIFIED, Distro.TEMURIN, Distro.TRAVA, Distro.ZULU, Distro.ZULU_PRIME));
        put(DownloadScope.NOT_DIRECTLY, List.of(Distro.DEBIAN, Distro.ORACLE, Distro.RED_HAT));
        put(BuildScope.BUILD_OF_OPEN_JDK, List.of(Distro.AOJ, Distro.AOJ_OPENJ9, Distro.BISHENG, Distro.CORRETTO, Distro.DEBIAN, Distro.DRAGONWELL, Distro.JETBRAINS, Distro.KONA, Distro.LIBERICA, Distro.MICROSOFT, Distro.OJDK_BUILD, Distro.OPEN_LOGIC, Distro.ORACLE, Distro.ORACLE_OPEN_JDK, Distro.RED_HAT, Distro.SAP_MACHINE, Distro.SEMERU, Distro.SEMERU_CERTIFIED, Distro.TEMURIN, Distro.TRAVA, Distro.ZULU, Distro.ZULU_PRIME));
        put(BuildScope.BUILD_OF_GRAALVM, List.of(Distro.GRAALVM_CE8, Distro.GRAALVM_CE11, Distro.GRAALVM_CE16, Distro.GRAALVM_CE17, Distro.LIBERICA_NATIVE, Distro.MANDREL));
        put(IDEScope.VISUAL_STUDIO_CODE, List.of(Distro.AOJ, Distro.AOJ_OPENJ9, Distro.CORRETTO, Distro.DRAGONWELL, Distro.KONA, Distro.LIBERICA, Distro.MICROSOFT, Distro.ORACLE, Distro.ORACLE_OPEN_JDK, Distro.RED_HAT, Distro.SAP_MACHINE, Distro.TEMURIN, Distro.SEMERU, Distro.SEMERU_CERTIFIED, Distro.ZULU));
        put(UsageScope.FREE_TO_USE_IN_PRODUCTION, List.of(Distro.AOJ, Distro.AOJ_OPENJ9, Distro.BISHENG, Distro.CORRETTO, Distro.DEBIAN, Distro.DRAGONWELL, Distro.GRAALVM_CE8, Distro.GRAALVM_CE11, Distro.GRAALVM_CE16, Distro.GRAALVM_CE17, Distro.JETBRAINS, Distro.KONA, Distro.LIBERICA, Distro.LIBERICA_NATIVE, Distro.MANDREL, Distro.MICROSOFT, Distro.OJDK_BUILD, Distro.OPEN_LOGIC, Distro.ORACLE_OPEN_JDK, Distro.SAP_MACHINE, Distro.SEMERU, Distro.TEMURIN, Distro.TRAVA, Distro.ZULU));
        put(UsageScope.LICENSE_NEEDED_FOR_PRODUCTION, List.of(Distro.ORACLE, Distro.RED_HAT, Distro.SEMERU_CERTIFIED, Distro.ZULU_PRIME));
        put(QualityScope.TCK_TESTED, List.of());
        put(QualityScope.AQAVIT_CERTIFIED, List.of());
    }};
}
