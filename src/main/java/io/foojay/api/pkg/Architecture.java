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

package io.foojay.api.pkg;


import io.foojay.api.util.OutputFormat;

import java.util.Arrays;
import java.util.List;

import static io.foojay.api.util.Constants.COLON;
import static io.foojay.api.util.Constants.COMMA;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.INDENTED_QUOTES;
import static io.foojay.api.util.Constants.NEW_LINE;
import static io.foojay.api.util.Constants.QUOTES;


public enum Architecture implements ApiFeature {
    AARCH64("AARCH64", "aarch64", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.ARM64); }
        },
    AARCH32("AARCH32", "aarch32", Bitness.BIT_32, false) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.ARM, Architecture.ARM32); }
    },
    ARM("Arm", "arm", Bitness.BIT_32, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.ARM32, Architecture.AARCH32); }
        },
    ARM32("Arm32", "arm32", Bitness.BIT_32, false) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.ARM, Architecture.AARCH32); }
    },
    ARM64("Arm64", "arm64", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.AARCH64); }
    },
    MIPS("Mips", "mips", Bitness.BIT_32, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    PPC("Power PC", "ppc", Bitness.BIT_32, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    PPC64("PPC64", "ppc64", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    PPC64LE("PPC64LE", "ppc64le", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    RISCV64("RISCv64", "riscv64", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    S390X("S390X", "s390x", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    SPARC("Sparc", "sparc", Bitness.BIT_32, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    SPARCV9("Sparc V9", "sparcv9", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    X64("X64", "x64", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.AMD64, Architecture.X86_64); }
    },
    X32("X32", "x32", Bitness.BIT_32, false) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.X86, Architecture.I386, Architecture.I586, Architecture.I686); }
    },
    I386("I386", "i386", Bitness.BIT_32, false) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.X86, Architecture.X32, Architecture.I586, Architecture.I686); }
    },
    I586("I586", "i386", Bitness.BIT_32, false) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.X86, Architecture.X32, Architecture.I386, Architecture.I686); }
    },
    I686("I686", "i386", Bitness.BIT_32, false) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.X86, Architecture.X32, Architecture.I386, Architecture.I686); }
    },
    X86("X86", "x86", Bitness.BIT_32, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.X32, Architecture.I386, Architecture.I586, Architecture.I686); }
    },
    X86_64("X86_64", "x86_64", Bitness.BIT_64, false) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.X64, Architecture.AMD64); }
    },
    AMD64("AMD64", "amd64", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(Architecture.X64, Architecture.X86_64); }
    },
    IA64("IA-64", "ia64", Bitness.BIT_64, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    NONE("-", "", Bitness.NONE, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    },
    NOT_FOUND("", "", Bitness.NOT_FOUND, true) {
        @Override public List<Architecture> getSynonyms() { return List.of(); }
    };

    private final String   uiString;
    private final String   apiString;
    private final Bitness  bitness;
    private final boolean standard;


    Architecture(final String uiString, final String apiString, final Bitness bitness, final boolean standard) {
        this.uiString  = uiString;
        this.apiString = apiString;
        this.bitness   = bitness;
        this.standard  = standard;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public Architecture getDefault() { return Architecture.NONE; }

    @Override public Architecture getNotFound() { return Architecture.NOT_FOUND; }

    @Override public Architecture[] getAll() { return Arrays.stream(values()).filter(Architecture::isStandard).toArray(Architecture[]::new); }

    @Override public String toString(final OutputFormat outputFormat) {
        StringBuilder msgBuilder = new StringBuilder();
        switch(outputFormat) {
            case FULL, REDUCED, REDUCED_ENRICHED -> {
                msgBuilder.append(CURLY_BRACKET_OPEN).append(NEW_LINE)
                          .append(INDENTED_QUOTES).append("name").append(QUOTES).append(COLON).append(QUOTES).append(name()).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append("ui_string").append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append("api_string").append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(COMMA_NEW_LINE)
                          .append(INDENTED_QUOTES).append("bitness").append(QUOTES).append(COLON).append(QUOTES).append(bitness.getApiString()).append(QUOTES).append(NEW_LINE)
                          .append(CURLY_BRACKET_CLOSE);
            }
            case FULL_COMPRESSED, REDUCED_COMPRESSED, REDUCED_ENRICHED_COMPRESSED, REDUCED_MINIMIZED -> {
                msgBuilder.append(CURLY_BRACKET_OPEN)
                          .append(QUOTES).append("name").append(QUOTES).append(COLON).append(QUOTES).append(name()).append(QUOTES).append(COMMA)
                          .append(QUOTES).append("ui_string").append(QUOTES).append(COLON).append(QUOTES).append(uiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append("api_string").append(QUOTES).append(COLON).append(QUOTES).append(apiString).append(QUOTES).append(COMMA)
                          .append(QUOTES).append("bitness").append(QUOTES).append(COLON).append(QUOTES).append(bitness.getApiString()).append(QUOTES)
                          .append(CURLY_BRACKET_CLOSE);
            }
        }
        return msgBuilder.toString();
    }

    @Override public String toString() { return toString(OutputFormat.FULL_COMPRESSED); }

    public static Architecture fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch (text) {
            case "aarch64":
            case "AARCH64":
                return AARCH64;
            case "amd64":
            case "AMD64":
            case "_amd64":
                return AMD64;
            case "aarch32":
            case "AARCH32":
            case "arm32":
            case "ARM32":
            case "arm":
            case "ARM":
                return ARM;
            case "arm64":
            case "ARM64":
                return ARM64;
            case "mips":
            case "MIPS":
                return MIPS;
            case "ppc":
            case "PPC":
                return PPC;
            case "ppc64el":
            case "PPC64EL":
            case "ppc64le":
            case "PPC64LE":
                return PPC64LE;
            case "ppc64":
            case "PPC64":
                return PPC64;
            case "riscv64":
            case "RISCV64":
                return RISCV64;
            case "s390" :
            case "s390x":
            case "S390X":
                return S390X;
            case "sparc":
            case "SPARC":
                return SPARC;
            case "sparcv9":
            case "SPARCV9":
                return SPARCV9;
            case "x64":
            case "X64":
            case "x86-64":
            case "X86-64":
            case "x86_64":
            case "X86_64":
            case "x86lx64":
            case "X86LX64":
                return X64;
            case "x86":
            case "X86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
            case "x86-32":
            case "x86lx32":
            case "X86LX32":
                return X86;
            case "ia64":
            case "IA64":
            case "ia-64":
            case "IA-64":
                return IA64;
            default:
                return NOT_FOUND;
        }
    }

    public Bitness getBitness() { return bitness; }

    public boolean isStandard() { return standard; }

    public static List<Architecture> getAsList() { return Arrays.asList(values()); }

    public abstract List<Architecture> getSynonyms();
}
