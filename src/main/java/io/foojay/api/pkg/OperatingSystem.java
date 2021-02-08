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


public enum OperatingSystem implements ApiFeature {
    ALPINE_LINUX("Alpine Linux", "linux", LibCType.MUSL) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(OperatingSystem.LINUX, OperatingSystem.LINUX_MUSL); }
    },
    LINUX("Linux", "linux", LibCType.GLIBC) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(); }
    },
    LINUX_MUSL("Linux Musl", "linux", LibCType.MUSL) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(OperatingSystem.LINUX, OperatingSystem.ALPINE_LINUX); }
    },
    MACOS("Mac OS", "macos", LibCType.LIBC) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(); }
    },
    WINDOWS("Windows", "windows", LibCType.C_STD_LIB) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(); }
    },
    SOLARIS("Solaris", "solaris", LibCType.LIBC) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(); }
    },
    QNX("QNX", "qnx", LibCType.LIBC) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(); }
    },
    AIX("AIX", "aix", LibCType.LIBC) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(); }
    },
    NONE("-", "", LibCType.NONE) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(); }
    },
    NOT_FOUND("", "", LibCType.NOT_FOUND) {
        @Override public List<OperatingSystem> getSynonyms() { return List.of(); }
    };

    private final String   uiString;
    private final String   apiString;
    private final LibCType libCType;


    OperatingSystem(final String uiString, final String apiString, final LibCType libCType) {
        this.uiString  = uiString;
        this.apiString = apiString;
        this.libCType  = libCType;
    }


    @Override public String getUiString() { return uiString; }

    public String getApiString() { return apiString; }

    @Override public OperatingSystem getDefault() { return OperatingSystem.NONE; }

    @Override public OperatingSystem getNotFound() { return OperatingSystem.NOT_FOUND; }

    @Override public OperatingSystem[] getAll() { return values(); }

    public static OperatingSystem fromText(final String text) {
        switch (text) {
            case "-linux":
            case "linux":
            case "Linux":
            case "LINUX":
                return LINUX;
            case "-linux-musl":
            case "-linux_musl":
            case "Linux-Musl":
            case "linux-musl":
            case "Linux_Musl":
            case "LINUX_MUSL":
            case "linux_musl":
            case "alpine-linux":
            case "ALPINE-LINUX":
            case "alpine_linux":
            case "Alpine_Linux":
            case "ALPINE_LINUX":
            case "Alpine Linux":
            case "alpine linux":
            case "ALPINE LINUX":
                return ALPINE_LINUX;
            case "-solaris":
            case "solaris":
            case "SOLARIS":
            case "Solaris":
                return SOLARIS;
            case "-qnx":
            case "qnx":
            case "QNX":
                return QNX;
            case"-aix":
            case "aix":
            case "AIX":
                return AIX;
            case "darwin":
            case "-darwin":
            case "-macosx":
            case "-MACOSX":
            case "MacOS":
            case "Mac OS":
            case "mac_os":
            case "Mac_OS":
            case "mac-os":
            case "Mac-OS":
            case "mac":
            case "MAC":
            case "macos":
            case "MACOS":
            case "osx":
            case "OSX":
            case "macosx":
            case "MACOSX":
            case "Mac OSX":
            case "mac osx":
                return MACOS;
            case "-win":
            case "windows":
            case "Windows":
            case "WINDOWS":
            case "win":
            case "Win":
            case "WIN":
                return WINDOWS;
            default:
                return NOT_FOUND;
        }
    }

    public LibCType getLibCType() { return libCType; }

    public static List<OperatingSystem> getAsList() { return Arrays.asList(values()); }

    public abstract List<OperatingSystem> getSynonyms();
}

