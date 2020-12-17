/*
 * Copyright (c) 2020 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
