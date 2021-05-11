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


public enum Comparison {
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    EQUAL("="),
    GREATER_THAN_OR_EQUAL(">="),
    GREATER_THAN(">"),
    RANGE_INCLUDING("..."),
    RANGE_EXCLUDING_TO("..<"),
    RANGE_EXCLUDING_FROM(">.."),
    RANGE_EXCLUDING(">.<");

    private final String operator;

    Comparison(final String operator) {
        this.operator = operator;
    }

    public String getOperator() { return operator; }

    public static Comparison fromText(final String text) {
        switch (text) {
            case "<" : return LESS_THAN;
            case "<=" : return LESS_THAN_OR_EQUAL;
            case "=" : return EQUAL;
            case ">=" : return GREATER_THAN_OR_EQUAL;
            case ">" : return GREATER_THAN;
            case "...": return RANGE_INCLUDING;
            case "..<": return RANGE_EXCLUDING_TO;
            case ">..": return RANGE_EXCLUDING_FROM;
            case ">.<": return RANGE_EXCLUDING;
            default  : return EQUAL;
        }
    }
}
