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


public enum Architecture implements ApiFeature {
    AARCH64("AARCH64", "aarch64", Bitness.BIT_64),
    ARM("Arm", "arm", Bitness.BIT_32),
    ARM64("Arm64", "arm64", Bitness.BIT_64),
    MIPS("Mips", "mips", Bitness.BIT_32),
    PPC("Power PC", "ppc", Bitness.BIT_32),
    PPC64("PPC64", "ppc64", Bitness.BIT_64),
    PPC64LE("PPC64LE", "ppc64le", Bitness.BIT_64),
    RISCV64("RISCv64", "riscv64", Bitness.BIT_64),
    S390X("S390X", "s390x", Bitness.BIT_64),
    SPARC("Sparc", "sparc", Bitness.BIT_32),
    SPARCV9("Sparc V9", "sparcv9", Bitness.BIT_64),
    X64("X64", "x64", Bitness.BIT_64),
    X86("X86", "x86", Bitness.BIT_32),
    AMD64("AMD64", "amd64", Bitness.BIT_64),
    IA64("IA-64", "ia64", Bitness.BIT_64),
    NONE("-", "", Bitness.NONE),
    NOT_FOUND("", "", Bitness.NOT_FOUND);

    private final String   uiString;
    private final String   apiString;
    private final Bitness  bitness;


    Architecture(final String uiString, final String apiString, final Bitness bitness) {
        this.uiString  = uiString;
        this.apiString = apiString;
        this.bitness   = bitness;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public Architecture getDefault() { return Architecture.NONE; }

    @Override public Architecture getNotFound() { return Architecture.NOT_FOUND; }

    @Override public Architecture[] getAll() { return values(); }

    public static Architecture fromText(final String text) {
        switch (text) {
            case "aarch64":
            case "AARCH64":
                return AARCH64;
            case "amd64":
            case "AMD64":
            case "_amd64":
                return AMD64;
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
            case "x86-64":
            case "X64":
                return X64;
            case "x86":
            case "X86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
            case "x86-32":
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

    public static List<Architecture> getAsList() { return Arrays.asList(values()); }
}
