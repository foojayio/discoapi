/*
 * Copyright (c) 2026.
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

package io.foojay.api.distribution;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.Pkg;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MandrelTest {

    private static final Mandrel MANDREL = new Mandrel();

    /**
     * Builds a GitHub-release-style JsonObject for a Mandrel release.
     */
    private JsonObject buildReleaseJson(String tagName, boolean prerelease, String[][] assets) {
        JsonObject release = new JsonObject();
        release.addProperty("tag_name", tagName);
        release.addProperty("prerelease", prerelease);

        JsonArray assetsArray = new JsonArray();
        for (String[] asset : assets) {
            JsonObject assetObj = new JsonObject();
            assetObj.addProperty("name", asset[0]);
            assetObj.addProperty("browser_download_url", asset[1]);
            assetsArray.add(assetObj);
        }
        release.add("assets", assetsArray);
        return release;
    }

    /**
     * Builds a standard set of assets for a Mandrel release, including
     * the .sha1 and .sha256 files that should be filtered out.
     */
    private String[][] buildStandardAssets(String tagName, int javaVersion) {
        String version = tagName.replace("mandrel-", "");
        String base = "https://github.com/graalvm/mandrel/releases/download/" + tagName + "/";
        return new String[][] {
                { "mandrel-java" + javaVersion + "-linux-aarch64-" + version + ".tar.gz",
                        base + "mandrel-java" + javaVersion + "-linux-aarch64-" + version + ".tar.gz" },
                { "mandrel-java" + javaVersion + "-linux-aarch64-" + version + ".tar.gz.sha1",
                        base + "mandrel-java" + javaVersion + "-linux-aarch64-" + version + ".tar.gz.sha1" },
                { "mandrel-java" + javaVersion + "-linux-aarch64-" + version + ".tar.gz.sha256",
                        base + "mandrel-java" + javaVersion + "-linux-aarch64-" + version + ".tar.gz.sha256" },
                { "mandrel-java" + javaVersion + "-linux-amd64-" + version + ".tar.gz",
                        base + "mandrel-java" + javaVersion + "-linux-amd64-" + version + ".tar.gz" },
                { "mandrel-java" + javaVersion + "-linux-amd64-" + version + ".tar.gz.sha1",
                        base + "mandrel-java" + javaVersion + "-linux-amd64-" + version + ".tar.gz.sha1" },
                { "mandrel-java" + javaVersion + "-linux-amd64-" + version + ".tar.gz.sha256",
                        base + "mandrel-java" + javaVersion + "-linux-amd64-" + version + ".tar.gz.sha256" },
                { "mandrel-java" + javaVersion + "-macos-aarch64-" + version + ".tar.gz",
                        base + "mandrel-java" + javaVersion + "-macos-aarch64-" + version + ".tar.gz" },
                { "mandrel-java" + javaVersion + "-macos-aarch64-" + version + ".tar.gz.sha1",
                        base + "mandrel-java" + javaVersion + "-macos-aarch64-" + version + ".tar.gz.sha1" },
                { "mandrel-java" + javaVersion + "-macos-aarch64-" + version + ".tar.gz.sha256",
                        base + "mandrel-java" + javaVersion + "-macos-aarch64-" + version + ".tar.gz.sha256" },
                { "mandrel-java" + javaVersion + "-windows-amd64-" + version + ".zip",
                        base + "mandrel-java" + javaVersion + "-windows-amd64-" + version + ".zip" },
                { "mandrel-java" + javaVersion + "-windows-amd64-" + version + ".zip.sha1",
                        base + "mandrel-java" + javaVersion + "-windows-amd64-" + version + ".zip.sha1" },
                { "mandrel-java" + javaVersion + "-windows-amd64-" + version + ".zip.sha256",
                        base + "mandrel-java" + javaVersion + "-windows-amd64-" + version + ".zip.sha256" },
        };
    }

    @Test
    public void getPkgFromJsonParsesReleases() {
        String[][] releases = {
                { "mandrel-25.0.2.0-Final", "25" },
                { "mandrel-24.2.2.0-Final", "24" },
                { "mandrel-24.1.2.0-Final", "23" },
                { "mandrel-23.1.10.0-Final", "21" },
                { "mandrel-23.0.6.0-Final", "17" },
        };

        for (String[] release : releases) {
            String tagName = release[0];
            int javaVer = Integer.parseInt(release[1]);

            String[][] assets = buildStandardAssets(tagName, javaVer);
            JsonObject releaseJson = buildReleaseJson(tagName, false, assets);

            List<Pkg> pkgs = MANDREL.getPkgFromJson(
                    releaseJson, null, false, null, null, null, null, null, false, null, null, false);

            // Each release has 4 actual binaries (sha1/sha256 filtered out)
            assertEquals(4, pkgs.size(), "Expected 4 packages for " + tagName);

            for (Pkg pkg : pkgs) {
                // Distribution
                assertEquals(Distro.MANDREL.get(), pkg.getDistribution(),
                        "Distribution mismatch for " + pkg.getFilename());

                // Package type is always JDK
                assertEquals(PackageType.JDK, pkg.getPackageType(),
                        "PackageType mismatch for " + pkg.getFilename());

                // Release status is always GA
                assertEquals(ReleaseStatus.GA, pkg.getReleaseStatus(),
                        "ReleaseStatus mismatch for " + pkg.getFilename());

                // Free use in production
                assertTrue(pkg.getFreeUseInProduction(),
                        "FreeUseInProduction should be true for " + pkg.getFilename());

                // JDK version matches the java version in the filename
                assertEquals(javaVer, pkg.getJdkVersion().getAsInt(),
                        "JDK version mismatch for " + pkg.getFilename());

                // Filename should start with mandrel-java
                assertTrue(pkg.getFilename().startsWith("mandrel-java" + javaVer),
                        "Filename prefix mismatch: " + pkg.getFilename());

                // Direct download URI should be set
                assertFalse(pkg.getDirectDownloadUri().isEmpty(),
                        "Download URI should not be empty for " + pkg.getFilename());

                // Architecture and OS should be resolved
                assertTrue(pkg.getArchitecture() != Architecture.NONE,
                        "Architecture should be resolved for " + pkg.getFilename());
                assertTrue(pkg.getOperatingSystem() != OperatingSystem.NONE,
                        "OS should be resolved for " + pkg.getFilename());
            }

            // Verify specific platform combinations
            long linuxAarch64 = pkgs.stream()
                    .filter(p -> p.getOperatingSystem() == OperatingSystem.LINUX
                            && p.getArchitecture() == Architecture.AARCH64)
                    .count();
            long linuxAmd64 = pkgs.stream()
                    .filter(p -> p.getOperatingSystem() == OperatingSystem.LINUX
                            && p.getArchitecture() == Architecture.AMD64)
                    .count();
            long macosAarch64 = pkgs.stream()
                    .filter(p -> p.getOperatingSystem() == OperatingSystem.MACOS
                            && p.getArchitecture() == Architecture.AARCH64)
                    .count();
            long windowsAmd64 = pkgs.stream()
                    .filter(p -> p.getOperatingSystem() == OperatingSystem.WINDOWS
                            && p.getArchitecture() == Architecture.AMD64)
                    .count();

            assertEquals(1, linuxAarch64, "Expected 1 linux-aarch64 pkg for " + tagName);
            assertEquals(1, linuxAmd64, "Expected 1 linux-amd64 pkg for " + tagName);
            assertEquals(1, macosAarch64, "Expected 1 macos-aarch64 pkg for " + tagName);
            assertEquals(1, windowsAmd64, "Expected 1 windows-amd64 pkg for " + tagName);

            // Verify archive types
            long tarGzCount = pkgs.stream().filter(p -> p.getArchiveType() == ArchiveType.TAR_GZ).count();
            long zipCount = pkgs.stream().filter(p -> p.getArchiveType() == ArchiveType.ZIP).count();
            assertEquals(3, tarGzCount, "Expected 3 tar.gz packages for " + tagName);
            assertEquals(1, zipCount, "Expected 1 zip package for " + tagName);
        }
    }

    @Test
    public void getPkgFromJsonSkipsPrerelease() {
        String[][] assets = buildStandardAssets("mandrel-25.0.2.0-Final", 25);
        JsonObject releaseJson = buildReleaseJson("mandrel-25.0.2.0-Final", true, assets);

        List<Pkg> pkgs = MANDREL.getPkgFromJson(
                releaseJson, null, false, null, null, null, null, null, false, null, null, false);

        assertTrue(pkgs.isEmpty(), "Prerelease should produce no packages");
    }

    @Test
    public void getPkgFromJsonSkipsNonMatchingFilenames() {
        String base = "https://github.com/graalvm/mandrel/releases/download/mandrel-25.0.2.0-Final/";
        String[][] assets = {
                { "some-random-file.txt", base + "some-random-file.txt" },
                { "mandrel-sources-25.0.2.0.source.tar.gz", base + "mandrel-sources.source.tar.gz" },
                { "release-notes.jar", base + "release-notes.jar" },
        };
        JsonObject releaseJson = buildReleaseJson("mandrel-25.0.2.0-Final", false, assets);

        List<Pkg> pkgs = MANDREL.getPkgFromJson(
                releaseJson, null, false, null, null, null, null, null, false, null, null, false);

        assertTrue(pkgs.isEmpty(), "Non-matching filenames should produce no packages");
    }
}
