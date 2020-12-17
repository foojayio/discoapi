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

package io.foojay.api.util;


import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.ReleaseStatus;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class Constants {
    public static final String        BASE_PATH                = "disco";
    public static final String        CURRENT_API_VERSION      = "v1.0";
    public static final String        PACKAGES_PATH            = "packages";
    public static final String        MAJOR_VERSIONS_PATH      = "major_versions";
    public static final String        EPHEMERAL_IDS_PATH       = "ephemeral_ids";

    public static final String        PACKAGES_COLLECTION      = "packages";
    public static final String        DOWNLOADS_COLLECTION     = "downloads";
    public static final String        DOWNLOADS_IP_COLLECTION  = "downloadsip";

    public static final String        CACHE_DATA_FILE          = "disco.json";
    public static final String        CACHE_DELTA_FILE         = "delta.json";

    public static final Pattern       POSITIVE_INTEGER_PATTERN = Pattern.compile("\\d+");

    public static final String        JDK                      = "jdk";
    public static final String        JRE                      = "jre";
    public static final String        JDK_PREFIX               = "jdk-";
    public static final String        JRE_PREFIX               = "jre-";
    public static final String        JDK_POSTFIX              = "-jdk";
    public static final String        JRE_POSTFIX              = "-jre";
    public static final String        EA_POSTFIX               = "-ea";
    public static final String        GA_POSTFIX               = "-ga";
    public static final String        FX_POSTFIX               = "-fx";
    public static final String        HEADLESS_POSTFIX         = "-headless";

    public static final String        MAINTAINED_PROPERTIES_URL = "https://github.com/foojay2020/maintained_major_versions/raw/main/maintained.properties";

    public static final Map<String, String> PARAMETER_LOOKUP = new HashMap<>() {{
        put(Pkg.FIELD_ARCHITECTURE, "aarch64, amd64, arm, arm64, mips, ppc, ppc64el, ppc64le, ppc64, riscv64, s390, s390x, sparc, sparcv9, x64, x86-64, x86, i386, i486, i586, i686, x86-32");
        put(Pkg.FIELD_ARCHIVE_TYPE, "cab, deb, dmg, exe, msi, pkg, rpm, tar, zip");
        put(Pkg.FIELD_BITNESS, "32, 64");
        put(Pkg.FIELD_DISTRIBUTION, "aoj, aoj_openj9, dragonwell, corretto, liberica, oracle, oracle_open_jdk, redhat, sap_machine, zulu");
        put(Pkg.FIELD_OPERATING_SYSTEM, "aix, alpine_linux, linux, linux_musl, macos, qnx, solaris, windows");
        put(Pkg.FIELD_LIB_C_TYPE, "c_std_lib, glibc, libc, musl");
        put(Pkg.FIELD_PACKAGE_TYPE, "jdk, jre");
        put(Pkg.FIELD_RELEASE_STATUS, "ea, ga");
        put(Pkg.FIELD_TERM_OF_SUPPORT, "sts, mts, lts");
    }};

    public static final String        FILE_ENDING_JAR          = "jar";
    public static final String        FILE_ENDING_TXT          = "txt";

    public static final long          EPHEMERAL_ID_DELAY       = 120; // [sec]
    public static final long          EPHEMERAL_ID_TIMEOUT     = 600; // [sec]


    public static final LinkedHashMap<String, ArchiveType> ARCHIVE_TYPE_LOOKUP = new LinkedHashMap<>() {{
        put(".cab", ArchiveType.CAB);
        put(".deb", ArchiveType.DEB);
        put(".dmg", ArchiveType.DMG);
        put(".msi", ArchiveType.MSI);
        put(".pkg", ArchiveType.PKG);
        put(".rpm", ArchiveType.RPM);
        put(".src.tar.gz", ArchiveType.SRC_TAR);
        put(".source.tar.gz", ArchiveType.SRC_TAR);
        put(".tar.gz", ArchiveType.TAR_GZ);
        put(".tar", ArchiveType.TAR_GZ);
        put(".zip", ArchiveType.ZIP);
        put(".exe", ArchiveType.EXE);
    }};

    public static final LinkedHashMap<String, Architecture> ARCHITECTURE_LOOKUP = new LinkedHashMap<>() {{
        put("aarch64", Architecture.AARCH64);
        put("aarch32sf", Architecture.ARM);
        put("aarch32hf", Architecture.ARM);
        put("x86-32", Architecture.X86);
        put("x86_32", Architecture.X86);
        put("x86-64", Architecture.X64);
        put("x86_64", Architecture.X64);
        put("x86", Architecture.X86);
        put("win64", Architecture.X64);
        put("x64", Architecture.X64);
        put("x32", Architecture.X86);
        put("amd64", Architecture.AMD64);
        put("arm64", Architecture.ARM64);
        put("arm32", Architecture.ARM);
        put("arm", Architecture.ARM);
        put("mips", Architecture.MIPS);
        put("i386", Architecture.X86);
        put("i586", Architecture.X86);
        put("i686", Architecture.X86);
        put("s390x", Architecture.S390X);
        put("ppc32spe", Architecture.PPC);
        put("ppc32hf", Architecture.PPC);
        put("ppc64le", Architecture.PPC64LE);
        put("ppc64", Architecture.PPC64);
        put("riscv64", Architecture.RISCV64);
        put("sparcv9", Architecture.SPARCV9);
        put("musl", Architecture.X64);
    }};

    public static final LinkedHashMap<String, OperatingSystem> OPERATING_SYSTEM_LOOKUP = new LinkedHashMap<>() {{
        put("macosx", OperatingSystem.MACOS);
        put("macos", OperatingSystem.MACOS);
        put("mac", OperatingSystem.MACOS);
        put("osx", OperatingSystem.MACOS);
        put("windows", OperatingSystem.WINDOWS);
        put("Windows", OperatingSystem.WINDOWS);
        put("win", OperatingSystem.WINDOWS);
        put("alpine-linux", OperatingSystem.ALPINE_LINUX);
        put("Alpine-Linux", OperatingSystem.ALPINE_LINUX);
        put("linux-musl", OperatingSystem.ALPINE_LINUX);
        put("musl", OperatingSystem.ALPINE_LINUX);
        put("linux", OperatingSystem.LINUX);
        put("Linux", OperatingSystem.LINUX);
        put("solaris", OperatingSystem.SOLARIS);
        put("qnx", OperatingSystem.QNX);
        put("aix", OperatingSystem.AIX);
    }};

    public static final LinkedHashMap<String, PackageType> PACKAGE_TYPE_LOOKUP = new LinkedHashMap<>() {{
        put("jdk", PackageType.JDK);
        put("-jdk", PackageType.JDK);
        put("jdk-", PackageType.JDK);
        put("jre", PackageType.JRE);
        put("-jre", PackageType.JRE);
        put("jre-", PackageType.JRE);
    }};

    public static final LinkedHashMap<String, ReleaseStatus> RELEASE_STATUS_LOOKUP = new LinkedHashMap<>() {{
        put("-ea", ReleaseStatus.EA);
        put("_ea", ReleaseStatus.EA);
        put("ea-", ReleaseStatus.EA);
        put("ea", ReleaseStatus.EA); 
        put("EA", ReleaseStatus.EA);
        put("/early_access/", ReleaseStatus.EA);
        put("/EA/", ReleaseStatus.GA);
        put("preview", ReleaseStatus.EA); 
        put("-ga", ReleaseStatus.GA);
        put("_ga", ReleaseStatus.GA);
        put("ga-", ReleaseStatus.GA); 
        put("ga", ReleaseStatus.GA); 
        put("GA", ReleaseStatus.GA);
        put("/GA/", ReleaseStatus.GA);
    }};
}
