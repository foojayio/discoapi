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

package io.foojay.api.distribution;

import io.foojay.api.pkg.Architecture;
import io.foojay.api.pkg.ArchiveType;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.pkg.PackageType;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.OperatingSystem;
import io.foojay.api.pkg.ReleaseStatus;
import io.foojay.api.pkg.VersionNumber;
import io.foojay.api.util.Constants;
import io.foojay.api.util.Helper;
import org.junit.jupiter.api.Test;

import java.util.Map.Entry;
import java.util.Properties;


public class FilenameTest {
    @Test
    public void fileName() {
        Properties properties = new Properties();
        properties.put("15_0_1-linux-aarch64","https://download.java.net/java/GA/jdk15.0.1/51f4f36ad4ef43e39d0dfdbaf6549e32/9/GPL/openjdk-15.0.1_linux-aarch64_bin.tar.gz");
        properties.put("15_0_1-linux-x64","https://download.java.net/java/GA/jdk15.0.1/51f4f36ad4ef43e39d0dfdbaf6549e32/9/GPL/openjdk-15.0.1_linux-x64_bin.tar.gz");
        properties.put("15_0_1-osx-x64","https://download.java.net/java/GA/jdk15.0.1/51f4f36ad4ef43e39d0dfdbaf6549e32/9/GPL/openjdk-15.0.1_osx-x64_bin.tar.gz");
        properties.put("15_0_1-windows-x64","https://download.java.net/java/GA/jdk15.0.1/51f4f36ad4ef43e39d0dfdbaf6549e32/9/GPL/openjdk-15.0.1_windows-x64_bin.zip");

        properties.put("15-linux-aarch64","https://download.java.net/java/GA/jdk15/779bf45e88a44cbd9ea6621d33e33db1/36/GPL/openjdk-15_linux-aarch64_bin.tar.gz");
        properties.put("15-linux-x64","https://download.java.net/java/GA/jdk15/779bf45e88a44cbd9ea6621d33e33db1/36/GPL/openjdk-15_linux-x64_bin.tar.gz");
        properties.put("15-osx-x64","https://download.java.net/java/GA/jdk15/779bf45e88a44cbd9ea6621d33e33db1/36/GPL/openjdk-15_osx-x64_bin.tar.gz");
        properties.put("15-windows-x64","https://download.java.net/java/GA/jdk15/779bf45e88a44cbd9ea6621d33e33db1/36/GPL/openjdk-15_windows-x64_bin.zip");

        properties.forEach((key, value) -> {
            String downloadLink = value.toString();
            String fileName     = Helper.getFileNameFromText(downloadLink);

            Pkg pkg = new Pkg();

            ArchiveType ext = ArchiveType.getFromFileName(fileName);
            pkg.setArchiveType(ext);

            ReleaseStatus rs = Constants.RELEASE_STATUS_LOOKUP.entrySet().stream()
                                                              .filter(entry -> downloadLink.contains(entry.getKey()))
                                                              .findFirst()
                                                              .map(Entry::getValue)
                                                              .orElse(ReleaseStatus.NONE);

            pkg.setDistribution(Distro.ORACLE_OPEN_JDK.get());
            pkg.setFileName(fileName);
            pkg.setDirectDownloadUri(downloadLink);

            Architecture arch;
            String[]     keyParts = key.toString().split("-");
            int noOfKeyParts = keyParts.length;
            if (keyParts[noOfKeyParts - 1].equals("musl")) {
                arch = Architecture.fromText(keyParts[noOfKeyParts - 2]);
            } else {
                arch = Architecture.fromText(keyParts[noOfKeyParts - 1]);
            }
            pkg.setArchitecture(arch);
            pkg.setBitness(arch.getBitness());

            VersionNumber vNumber = VersionNumber.fromText(fileName);
            pkg.setVersionNumber(vNumber);
            pkg.setJavaVersion(vNumber);
            pkg.setDistributionVersion(vNumber);

            Helper.setTermOfSupport(vNumber, pkg);

            pkg.setPackageType(PackageType.JDK);

            pkg.setReleaseStatus(rs);

            OperatingSystem os;
            String[]        fileNameParts = fileName.toString().split("_");
            if (fileNameParts.length > 1) {
                String[] osArchParts = fileNameParts[1].split("-");
                os = OperatingSystem.fromText(osArchParts[0]);
                pkg.setOperatingSystem(os);
            }
        });
    }
}
