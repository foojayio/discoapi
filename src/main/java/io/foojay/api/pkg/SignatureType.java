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

import java.util.Arrays;
import java.util.List;


public enum SignatureType implements ApiFeature {
    RSA("RSA", "rsa"), // Rivest Shamir Adleman
    DSA("DSA", "dsa"), // Digital Signature Algorithm
    ECDSA("ECDSA", "ecdsa"), // Elliptic Curve Cryptography DSA
    EDDSA("EdDSA", "eddsa"), // Edwards Curve DSA
    NONE("-", ""),
    NOT_FOUND("", "");

    private final String uiString;
    private final String apiString;


    SignatureType(final String uiString, final String apiString) {
        this.uiString  = uiString;
        this.apiString = apiString;
    }


    @Override public String getUiString() { return uiString; }

    @Override public String getApiString() { return apiString; }

    @Override public SignatureType getDefault() { return SignatureType.NONE; }

    @Override public SignatureType getNotFound() { return SignatureType.NOT_FOUND; }

    @Override public SignatureType[] getAll() { return values(); }

    public static SignatureType fromText(final String text) {
        if (null == text) { return NOT_FOUND; }
        switch (text) {
            case "rsa":
            case "RSA":
                return RSA;
            case "dsa":
            case "DSA":
                return DSA;
            case "ecdsa":
            case "ECDSA":
                return ECDSA;
            case "eddsa":
            case "EdDSA":
            case "EDDSA":
                return EDDSA;
            default:
                return NOT_FOUND;
        }
    }

    public static List<SignatureType> getAsList() { return Arrays.asList(values()); }
}
