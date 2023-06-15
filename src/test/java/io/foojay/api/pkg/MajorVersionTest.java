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

import eu.hansolo.jdktools.Match;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.scopes.Scope;
import io.foojay.api.CacheManager;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class MajorVersionTest {

    @Test public void releasesSupportTermsTest() {
        MajorVersion majorVersion1  = new MajorVersion(1);
        MajorVersion majorVersion2  = new MajorVersion(2);
        MajorVersion majorVersion3  = new MajorVersion(3);
        MajorVersion majorVersion4  = new MajorVersion(4);
        MajorVersion majorVersion5  = new MajorVersion(5);
        MajorVersion majorVersion6  = new MajorVersion(6);
        MajorVersion majorVersion7  = new MajorVersion(7);
        MajorVersion majorVersion8  = new MajorVersion(8);
        MajorVersion majorVersion9  = new MajorVersion(9);
        MajorVersion majorVersion10 = new MajorVersion(10);
        MajorVersion majorVersion11 = new MajorVersion(11);
        MajorVersion majorVersion12 = new MajorVersion(12);
        MajorVersion majorVersion13 = new MajorVersion(13);
        MajorVersion majorVersion14 = new MajorVersion(14);
        MajorVersion majorVersion15 = new MajorVersion(15);
        MajorVersion majorVersion16 = new MajorVersion(16);
        MajorVersion majorVersion17 = new MajorVersion(17);
        MajorVersion majorVersion18 = new MajorVersion(18);
        MajorVersion majorVersion19 = new MajorVersion(19);
        MajorVersion majorVersion20 = new MajorVersion(20);
        MajorVersion majorVersion21 = new MajorVersion(21);
        MajorVersion majorVersion22 = new MajorVersion(22);
        MajorVersion majorVersion23 = new MajorVersion(23);
        MajorVersion majorVersion24 = new MajorVersion(24);
        MajorVersion majorVersion25 = new MajorVersion(25);

        assert (TermOfSupport.LTS == majorVersion1.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion2.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion3.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion4.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion5.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion6.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion7.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion8.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion9.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion10.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion11.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion12.getTermOfSupport());
        assert (TermOfSupport.MTS == majorVersion13.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion14.getTermOfSupport());
        assert (TermOfSupport.MTS == majorVersion15.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion16.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion17.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion18.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion19.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion20.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion21.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion22.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion23.getTermOfSupport());
        assert (TermOfSupport.STS == majorVersion24.getTermOfSupport());
        assert (TermOfSupport.LTS == majorVersion25.getTermOfSupport());
    }

    @Test public void maintainedMajorVersionTest() {
        final Map<Integer, Boolean> maintainedMajorVersions = new ConcurrentHashMap<>();
        final Properties            maintainedProperties    = new Properties();

        try {
            HttpResponse<String> response = Helper.get(Constants.MAINTAINED_PROPERTIES_URL);
            assert response != null;
            maintainedProperties.load(new StringReader(response.body()));
            maintainedMajorVersions.clear();
            maintainedProperties.entrySet().forEach(entry -> {
                Integer majorVersion = Integer.valueOf(entry.getKey().toString().replaceAll("jdk-", ""));
                Boolean maintained   = Boolean.valueOf(entry.getValue().toString().toLowerCase());
                maintainedMajorVersions.put(majorVersion, maintained);
            });
        } catch (Exception e) {
        }
        assert maintainedMajorVersions.containsKey(9);
        assert !maintainedMajorVersions.get(9);

        MajorVersion majorVersion = new MajorVersion(6);
        assert !majorVersion.isMaintained();
    }
}
