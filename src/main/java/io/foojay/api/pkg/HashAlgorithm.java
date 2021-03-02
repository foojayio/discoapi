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

public enum HashAlgorithm implements ApiFeature {
    MD5("MSD5", "md5", 128),
    SHA1("SHA-1", "sha1", 160),
    SHA2_256("SHA-2 256", "sha2_256", 256),
    SHA3_256("SHA-3 256", "sha3_256", 256),
    NONE("-", "", 0),
    NOT_FOUND("", "", 0);

    private final String uiString;
    private final String apiString;
    private final int    bit;


    HashAlgorithm(final String uiString, final String apiString, final int bit) {
        this.uiString  = uiString;
        this.apiString = apiString;
        this.bit       = bit;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public ApiFeature getDefault() { return HashAlgorithm.NONE; }

    @Override public ApiFeature getNotFound() { return HashAlgorithm.NOT_FOUND; }

    @Override public ApiFeature[] getAll() { return values(); }

    public int getBit() { return bit; }

    public static HashAlgorithm fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch(text) {
            case "md5":
            case "MD5":
            case "md-5":
            case "md_5":
            case "MD-5":
            case "MD_5":
                return MD5;
            case "sha1":
            case "SHA1":
            case "sha-1":
            case "SHA-1":
            case "sha_1":
            case "SHA_1":
                return SHA1;
            case "sha2_256":
            case "SHA2_256":
            case "sha-2-256":
            case "SHA-2-256":
            case "sha_2_256":
            case "SHA_2_256":
                return SHA2_256;
            case "sha3_256":
            case "SHA3_256":
            case "sha-3-256":
            case "SHA-3-256":
            case "sha_3_256":
            case "SHA_3_256":
                return SHA3_256;
            default:
                return NOT_FOUND;
        }
    }
}
