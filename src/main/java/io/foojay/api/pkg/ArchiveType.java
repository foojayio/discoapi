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


public enum ArchiveType implements ApiFeature {
    BIN("bin", "bin", "bin"),
    CAB("cab", "cab",".cab"),
    DEB("deb", "deb",".deb"),
    DMG("dmg", "dmg",".dmg"),
    MSI("msi", "msi",".msi"),
    PKG("pkg", "pkg",".pkg"),
    RPM("rpm", "rpm",".rpm"),
    SRC_TAR("src.tar.gz", "src_tar",".src.tar.gz", ".source.tar.gz", "source.tar.gz"),
    TAR("tar", "tar", ".tar"),
    TAR_GZ("tar.gz", "tar.gz", ".tar.gz"),
    TAR_Z("tar.Z", "tar.z", ".tar.Z"),
    ZIP("zip", "zip", ".zip"),
    EXE("exe", "exe", ".exe"),
    NONE("-", "", "-"),
    NOT_FOUND("", "", "");

    private final String       uiString;
    private final String       apiString;
    private final List<String> fileEndings;


    ArchiveType(final String uiString, final String apiString, final String... fileEndings) {
        this.uiString    = uiString;
        this.apiString   = apiString;
        this.fileEndings = List.of(fileEndings);
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public ArchiveType getDefault() { return ArchiveType.NONE; }

    @Override public ArchiveType getNotFound() { return ArchiveType.NOT_FOUND; }

    @Override public ArchiveType[] getAll() { return values(); }

    public static ArchiveType fromText(final String text) {
        switch (text) {
            case "bin":
            case ".bin":
            case "BIN":
                return BIN;
            case "cab":
            case ".cab":
            case "CAB":
                return CAB;
            case "deb":
            case ".deb":
            case "DEB":
                return DEB;
            case "dmg":
            case ".dmg":
            case "DMG":
                return DMG;
            case "msi":
            case ".msi":
            case "MSI":
                return MSI;
            case "pkg":
            case ".pkg":
            case "PKG":
                return PKG;
            case "rpm":
            case ".rpm":
            case "RPM":
                return RPM;
            case "src.tar.gz":
            case ".src.tar.gz":
            case "source.tar.gz":
            case "SRC.TAR.GZ":
            case "src_tar":
            case "SRC_TAR":
                return SRC_TAR;
            case "tar.Z":
            case ".tar.Z":
            case "TAR.Z":
                return TAR_Z;
            case "tar.gz":
            case ".tar.gz":
            case "TAR.GZ":
                return TAR_GZ;
            case "tar":
            case ".tar":
            case "TAR":
                return TAR;
            case "zip":
            case ".zip":
            case "ZIP":
                return ZIP;
            default:
                return NOT_FOUND;
        }
    }

    public List<String> getFileEndings() { return fileEndings; }

    public static ArchiveType getFromFileName(final String fileName) {
        for (ArchiveType ext : values()) {
            for (String ending : ext.getFileEndings()) {
                if (fileName.toLowerCase().endsWith(ending)) { return ext; }
            }

        }
        return ArchiveType.NONE;
    }

    public static List<ArchiveType> getAsList() { return Arrays.asList(values()); }
}

