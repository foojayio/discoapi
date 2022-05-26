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

package io.foojay.api.distribution;

import com.google.gson.JsonObject;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Bitness;
import eu.hansolo.jdktools.HashAlgorithm;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.SignatureType;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.versioning.Semver;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.Pkg;

import java.util.List;


public interface Distribution {

    Distro getDistro();

    String getName();

    String getPkgUrl();

    String getArchitectureParam();

    String getOperatingSystemParam();

    String getArchiveTypeParam();

    String getPackageTypeParam();

    String getReleaseStatusParam();

    String getTermOfSupportParam();

    String getBitnessParam();

    HashAlgorithm getHashAlgorithm();

    String getHashUri();

    SignatureType getSignatureType();

    HashAlgorithm getSignatureAlgorithm();

    String getSignatureUri();

    String getOfficialUri();

    List<String> getSynonyms();

    List<Semver> getVersions();

    String getUrlForAvailablePkgs(VersionNumber versionNumber, boolean latest, OperatingSystem operatingSystem,
                                  Architecture architecture, Bitness bitness,
                                  ArchiveType archiveType, PackageType packageType, Boolean javafxBundled,
                                  ReleaseStatus releaseStatus, TermOfSupport termOfSupport);

    List<Pkg> getPkgFromJson(JsonObject jsonObj, VersionNumber versionNumber, boolean latest, OperatingSystem operatingSystem,
                             Architecture architecture, Bitness bitness, ArchiveType archiveType, PackageType packageType,
                             Boolean javafxBundled, ReleaseStatus releaseStatus, TermOfSupport termOfSupport, boolean onlyNewPkgs);
}
