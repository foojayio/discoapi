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

import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.Pkg;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
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
    public void findFileNameInCorrettoBodyText() {
        final String text = "October 2019 quarterly security patch and bug fix update.\r\n\r\n### Download Links\r\n|Platform   |Type   |Download Link  |Checksum (MD5) |Sig File   |\r\n|---    |---    |---    |---    |---    |\r\n|[Linux x64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/generic-linux-install.html)  |JDK    | [java-1.8.0-amazon-corretto-jdk_8.232.09-1_amd64.deb](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/java-1.8.0-amazon-corretto-jdk_8.232.09-1_amd64.deb)   | `88ea4a5a1dbdf8b11437cf945552f14c`    |   |\r\n|[Linux x64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/generic-linux-install.html)  |JDK    | [java-1.8.0-amazon-corretto-devel-1.8.0_232.b09-1.x86_64.rpm](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/java-1.8.0-amazon-corretto-devel-1.8.0_232.b09-1.x86_64.rpm)   | `180d8020d3d61aa050cfad1cf54193c9`    |   |\r\n|[Linux x64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/generic-linux-install.html)  |JDK    |[amazon-corretto-8.232.09.1-linux-x64.tar.gz](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-linux-x64.tar.gz) | `3511152bd52c867f8b550d7c8d7764aa`  | [Download](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-linux-x64.tar.gz.sig)    |\r\n|[Linux aarch64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/generic-linux-install.html)  |JDK    |[java-1.8.0-amazon-corretto-jdk_8.232.09-1_arm64.deb](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/java-1.8.0-amazon-corretto-jdk_8.232.09-1_arm64.deb) |`f71a7cdbaf4dd6a61afae5d9b28d78b1`  |   |\r\n|[Linux aarch64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/generic-linux-install.html)  |JDK    |[java-1.8.0-amazon-corretto-devel-1.8.0_232.b09-1.aarch64.rpm](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/java-1.8.0-amazon-corretto-devel-1.8.0_232.b09-1.aarch64.rpm)   |`b4234fc4ca167b8ca74497188a212dd1`  |   |\r\n|[Linux aarch64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/generic-linux-install.html)  |JDK    |[amazon-corretto-8.232.09.1-linux-aarch64.tar.gz](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-linux-aarch64.tar.gz) |`18228a7ba3ca63fc102d6e35ae5c4a13`  |[Download](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-linux-aarch64.tar.gz.sig)    |\r\n|[Windows x64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JDK    |[amazon-corretto-8.232.09.1-windows-x64.msi](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x64.msi)   |`620ee139aac5f05ab404006b5e33378f`  |   |\r\n|[Windows x64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JDK    |[amazon-corretto-8.232.09.1-windows-x64-jdk.zip](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x64-jdk.zip)   |`b0d375cbcfcda6d04e87888c6c6763d3`  |[Download](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x64-jdk.zip.sig) |\r\n|[Windows x64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JRE    |[amazon-corretto-8.232.09.1-windows-x64-jre.zip](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x64-jre.zip)   |`f4b3613af15508d4e6d3f8965a1be8a3`  |[Download](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x64-jre.zip.sig) |\r\n|[Windows x86](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JDK    |[amazon-corretto-8.232.09.1-windows-x86.msi](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x86.msi)   |`f94a12381f63284bcec017f2a9ebfe3c`  |   |\r\n|[Windows x86](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JDK    |[amazon-corretto-8.232.09.1-windows-x86-jdk.zip](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x86-jdk.zip)   |`463d8c7d19bafbf9b307c19e7b516c44`  |[Download](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x86-jdk.zip.sig) |\r\n|[Windows x86](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/windows-7-install.html)    |JRE    | [amazon-corretto-8.232.09.1-windows-x86-jre.zip](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x86-jre.zip) |`6e846838ae5189b433a38be2731af735` | [Download](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-windows-x86-jre.zip.sig)   |\r\n|[macOS x64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/macos-install.html)  |JDK    | [amazon-corretto-8.232.09.1-macosx-x64.pkg](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-macosx-x64.pkg)   |`2095075f02d71587de181cb824c2497f` |   |\r\n|[macOS x64](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/macos-install.html)  |JDK    | [amazon-corretto-8.232.09.1-macosx-x64.tar.gz](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-macosx-x64.tar.gz) | `db967586a3bd61ad190686258bedfa81`  | [Download](https://d3pxv6yz143wms.cloudfront.net/8.232.09.1/amazon-corretto-8.232.09.1-macosx-x64.tar.gz.sig) |";

        List<Pair<PackageType,String>> pairs = List.of(new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.2-macosx-x64.pkg"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.2-macosx-x64.tar.gz"),
                                                       new Pair<>(PackageType.JDK, "java-1.8.0-amazon-corretto-jdk_8.232.09-1_amd64.deb"),
                                                       new Pair<>(PackageType.JDK, "java-1.8.0-amazon-corretto-devel-1.8.0_232.b09-1.x86_64.rpm"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.1-linux-x64.tar.gz"),
                                                       new Pair<>(PackageType.JDK, "java-1.8.0-amazon-corretto-jdk_8.232.09-1_arm64.deb"),
                                                       new Pair<>(PackageType.JDK, "java-1.8.0-amazon-corretto-devel-1.8.0_232.b09-1.aarch64.rpm"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.1-linux-aarch64.tar.gz"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.1-windows-x64.msi"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.1-windows-x64-jdk.zip"),
                                                       new Pair<>(PackageType.JRE, "amazon-corretto-8.232.09.1-windows-x64-jre.zip"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.1-windows-x86.msi"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.1-windows-x86-jdk.zip"),
                                                       new Pair<>(PackageType.JRE, "amazon-corretto-8.232.09.1-windows-x86-jre.zip"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.1-macosx-x64.pkg"),
                                                       new Pair<>(PackageType.JDK, "amazon-corretto-8.232.09.1-macosx-x64.tar.gz"));

        Set<Pair<String,String>>      pairsFound               = Helper.getPackageTypeAndFileUrlFromString(text);
        Set<Pair<PackageType,String>> packageTypeFilenamePairs = new HashSet<>();

        for (Pair<String,String> pair : pairsFound) {
            String filename = Helper.getFileNameFromText(pair.getValue());
            if (filename.isEmpty()) { continue; }
            packageTypeFilenamePairs.add(new Pair<>(PackageType.fromText(pair.getKey()), filename));
        }

        for (Pair pair : packageTypeFilenamePairs) {
            assert pairs.stream().filter(p -> p.getKey() == pair.getKey()).filter(p -> p.getValue().equals(pair.getValue())).count() == 1;
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
    public void testExtractingFilenames() {
        try {
            String html = Helper.getTextFromUrl("https://www.oracle.com/java/technologies/java-archive-javase10-downloads.html");
            List<String> fileNamesFoundInUrl = new ArrayList<>(Helper.getDownloadHrefsFromString(html));
            assert fileNamesFoundInUrl.size() == 30;

            html = Helper.getTextFromUrl("https://www.oracle.com/java/technologies/javase/jdk13-archive-downloads.html");
            fileNamesFoundInUrl = new ArrayList<>(Helper.getDownloadHrefsFromString(html));
            assert fileNamesFoundInUrl.size() == 15;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void ltsTest() {
        assert Helper.isLTS(6);
        assert Helper.isLTS(8);
        assert Helper.isLTS(11);
        assert Helper.isLTS(17);
        assert Helper.isLTS(21);
    }

    @Test
    public void mtsTest() {
        assert Helper.isMTS(13);
        assert Helper.isMTS(15);
        assert Helper.isMTS(19);
        assert Helper.isMTS(23);
        assert Helper.isMTS(27);
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
            final String OPEN_JDK_PROPERTIES = "https://github.com/foojay2020/openjdk_releases/raw/main/openjdk.properties";
            Properties properties = new Properties();
            properties.load(new StringReader(Helper.getTextFromUrl(OPEN_JDK_PROPERTIES)));
            List<Pkg> pkgs = Distro.ORACLE_OPEN_JDK.get().getPkgFromJson(null, new VersionNumber(16), false, OperatingSystem.NONE,
                                                                         Architecture.NONE, Bitness.NONE, ArchiveType.NONE, PackageType.NONE, false, ReleaseStatus.NONE, TermOfSupport.NONE, true);
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
