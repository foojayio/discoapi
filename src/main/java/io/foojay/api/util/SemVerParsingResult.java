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

import io.foojay.api.pkg.SemVer;

import java.util.function.Predicate;


public class SemVerParsingResult {
    private SemVer            semVer1;
    private Error             error1;
    private SemVer            semVer2;
    private Error             error2;
    private Predicate<SemVer> filter;


    public SemVerParsingResult() {
        semVer1 = null;
        error1  = null;
        semVer2 = null;
        error2  = null;
        filter  = null;
    }


    public SemVer getSemVer1() { return semVer1; }
    public void setSemVer1(final SemVer semVer) { semVer1 = semVer; }

    public Error getError1() { return error1; }
    public void setError1(final Error error) { error1 = error; }

    public SemVer getSemVer2() { return semVer2; }
    public void setSemVer2(final SemVer semVer) { semVer2 = semVer; }

    public Error getError2() { return error2; }
    public void setError2(final Error error) { error2 = error; }

    public Predicate<SemVer> getFilter() { return filter; }
    public void setFilter(final Predicate<SemVer> filter) { this.filter = filter; }
}
