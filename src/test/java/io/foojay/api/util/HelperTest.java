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

import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Bitness;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.TermOfSupport;
import io.foojay.api.pkg.VersionNumber;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;


public class HelperTest {

    @Test
    public void findFileUrl() {
        final String text = "|JDK | [amazon-corretto-8.232.09.1-macosx-x64.tar.gz](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-macosx-x64.tar.gz) |"
                            + "\"download_count\": 10969,\"link\": \"https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.8%2B10/OpenJDK11U-jre_x86-32_windows_hotspot_11.0.8_10.msi\","
                            + "[{\"id\":20722,\"name\":\"zulu8.44.0.9-ca-jdk8.0.242-win_i686.zip\",\"url\":\"https://cdn.azul.com/zulu/bin/zulu8.44.0.9-ca-jdk8.0.242-win_i686.zip\",\"jdk_version\":[8,0,242,20],\"zulu_version\":[8,44,0,9]}"
                            + " \"downloadUrl\": \"https://github.com/bell-sw/Liberica/releases/download/11.0.8+10/bellsoft-jre11.0.8+10-windows-i586.zip\","
                            + "|[Windows x86](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JDK    | [amazon-corretto-8.222.10.3-windows-x86-jdk.zip](https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jdk.zip)   | `85dfaf1ee4117649d9bdf94fe9c05a64`  | [Download](https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jdk.zip.sig) |\\r\\n|[Windows x86](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JRE    | [amazon-corretto-8.222.10.3-windows-x86-jre.zip](https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jre.zip)   | `79d13957b148178295f01f14b4f52b5b`  | [Download](https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jre.zip.sig) |\\r\\n\\r\\n### Signature Verification\\r\\nThe public key to verify the SIGNATURE file can be downloaded from [";

        List<String> urls = List.of("https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.8%2B10/OpenJDK11U-jre_x86-32_windows_hotspot_11.0.8_10.msi",
                                    "https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jre.zip",
                                    "https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-macosx-x64.tar.gz",
                                    "https://github.com/bell-sw/Liberica/releases/download/11.0.8+10/bellsoft-jre11.0.8+10-windows-i586.zip",
                                    "https://cdn.azul.com/zulu/bin/zulu8.44.0.9-ca-jdk8.0.242-win_i686.zip",
                                    "https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jdk.zip");

        Set<String> urlsFound = Helper.getFileUrlsFromString(text);
        for (String url : urlsFound) {
            assert urls.contains(url);
        }
    }

    @Test
    public void findFileNameInUrl() {
        final String text = "|JDK | [amazon-corretto-8.232.09.1-macosx-x64.tar.gz](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-macosx-x64.tar.gz) |"
                            + "\"download_count\": 10969,\"link\": \"https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.8%2B10/OpenJDK11U-jre_x86-32_windows_hotspot_11.0.8_10.msi\","
                            + "[{\"id\":20722,\"name\":\"zulu8.44.0.9-ca-jdk8.0.242-win_i686.zip\",\"url\":\"https://cdn.azul.com/zulu/bin/zulu8.44.0.9-ca-jdk8.0.242-win_i686.zip\",\"jdk_version\":[8,0,242,20],\"zulu_version\":[8,44,0,9]}"
                            + " \"downloadUrl\": \"https://github.com/bell-sw/Liberica/releases/download/11.0.8+10/bellsoft-jre11.0.8+10-windows-i586.zip\","
                            + "|[Windows x86](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JDK    | [amazon-corretto-8.222.10.3-windows-x86-jdk.zip](https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jdk.zip)   | `85dfaf1ee4117649d9bdf94fe9c05a64`  | [Download](https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jdk.zip.sig) |\\r\\n|[Windows x86](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JRE    | [amazon-corretto-8.222.10.3-windows-x86-jre.zip](https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jre.zip)   | `79d13957b148178295f01f14b4f52b5b`  | [Download](https://d3pxv6yz143wms.cloudfront.net/8.222.10.1/amazon-corretto-8.222.10.3-windows-x86-jre.zip.sig) |\\r\\n\\r\\n### Signature Verification\\r\\nThe public key to verify the SIGNATURE file can be downloaded from ["
                            + "https://amazon-corretto-8.265.01.2-alpine-linux-x64.tar.gz";

        List<String> fileNames = List.of("OpenJDK11U-jre_x86-32_windows_hotspot_11.0.8_10.msi",
                                         "amazon-corretto-8.222.10.3-windows-x86-jre.zip",
                                         "amazon-corretto-8.232.09.1-macosx-x64.tar.gz",
                                         "bellsoft-jre11.0.8+10-windows-i586.zip",
                                         "zulu8.44.0.9-ca-jdk8.0.242-win_i686.zip",
                                         "amazon-corretto-8.222.10.3-windows-x86-jdk.zip",
                                         "amazon-corretto-8.265.01.2-alpine-linux-x64.tar.gz");

        Set<String> urlsFound      = Helper.getFileUrlsFromString(text);
        Set<String> fileNamesFound = new HashSet<>();

        for (String url : urlsFound) {
            String fileName = Helper.getFileNameFromText(url);
            if (fileName.isEmpty()) { continue; }
            fileNamesFound.add(fileName);
        }

        for (String fileName : fileNamesFound) {
            assert fileNames.contains(fileName);
        }
    }

    @Test
    public void ltsTest() {
        assert Helper.isLTS(6);
        assert Helper.isLTS(8);
        assert Helper.isLTS(11);
        assert Helper.isLTS(17);
        assert Helper.isLTS(23);
    }

    @Test
    public void mtsTest() {
        assert Helper.isMTS(13);
        assert Helper.isMTS(15);
        assert Helper.isMTS(19);
        assert Helper.isMTS(21);
        assert Helper.isMTS(25);
    }

    @Test
    public void stsTest() {
        assert Helper.isSTS(9);
        assert Helper.isSTS(10);
        assert Helper.isSTS(12);
        assert Helper.isSTS(14);
        assert Helper.isSTS(20);
    }

    @Test
    public void loadPropertiesTest() {
        try {
            final String OPEN_JDK_PROPERTIES = "https://github.com/HanSolo/openjdkreleases/raw/main/openjdk.properties";
            Properties properties = new Properties();
            properties.load(new StringReader(Helper.getTextFromUrl(OPEN_JDK_PROPERTIES)));
            List<Pkg> pkgs = Distro.ORACLE_OPEN_JDK.get().getPkgFromJson(null, new VersionNumber(16), false, OperatingSystem.NONE,
                                                                         Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, false, ReleaseStatus.NONE, TermOfSupport.NONE);
            assert pkgs.size() == 5;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void isPositiveIntegerTest() {
        final String numberString1 = "18";
        assert Helper.isPositiveInteger(numberString1);

        final String numberString2 = "-12";
        assert !Helper.isPositiveInteger(numberString2);

        final String numberString3 = "text";
        assert !Helper.isPositiveInteger(numberString3);
    }
}
