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

package io.foojay.api.scopes;

import java.util.List;
import java.util.stream.Collectors;


public class ScopeConfig {
    private String       name;
    private List<String> distributions;
    private List<String> basicScopes;
    private List<String> downloadScopes;
    private List<String> usageScopes;
    private List<String> buildScopes;
    private String       match;
    private String       version;
    private List<String> architectures;
    private List<String> archiveTypes;
    private List<String> packageTypes;
    private List<String> operatingSystems;
    private List<String> libcTypes;
    private List<String> releaseStatus;
    private List<String> termsOfSupport;
    private String       bitness;
    private String       javafxBundled;
    private String       directlyDownloadable;
    private String       signatureAvailable;
    private String       latest;


    public String getName() { return null == name ? "" : name; }
    public void setName(final String name) { this.name = name; }

    public List<String> getDistributions() { return null == distributions ? List.of() : distributions; }
    public void setDistributions(final List<String> distributions) { this.distributions = distributions; }

    public List<String> getBasicScopes() { return null == basicScopes ? List.of() : basicScopes; }
    public void setBasicScopes(final List<String> basicScopes) { this.basicScopes = basicScopes; }

    public List<String> getDownloadScopes() { return null == downloadScopes ? List.of() : downloadScopes; }
    public void setDownloadScopes(final List<String> downloadScopes) { this.downloadScopes = downloadScopes; }

    public List<String> getUsageScopes() { return null == usageScopes ? List.of() : usageScopes; }
    public void setUsageScopes(final List<String> usageScopes) { this.usageScopes = usageScopes; }

    public List<String> getBuildScopes() { return null == buildScopes ? List.of() : buildScopes; }
    public void setBuildScopes(final List<String> buildScopes) { this.buildScopes = buildScopes; }

    public String getMatch() { return null == match ? "": match; }
    public void setMatch(final String match) { this.match = match; }

    public String getVersion() { return null == version ? "" : version; }
    public void setVersion(final String version) { this.version = version; }

    public List<String> getArchitectures() { return null == architectures ? List.of() : architectures; }
    public void setArchitectures(final List<String> architectures) { this.architectures = architectures; }

    public List<String> getArchiveTypes() { return null == archiveTypes ? List.of() : archiveTypes; }
    public void setArchiveTypes(final List<String> archiveTypes) { this.archiveTypes = archiveTypes; }

    public List<String> getPackageTypes() { return null == packageTypes ? List.of() : packageTypes; }
    public void setPackageTypes(final List<String> packageTypes) { this.packageTypes = packageTypes; }

    public List<String> getOperatingSystems() { return null == operatingSystems ? List.of() : operatingSystems; }
    public void setOperatingSystems(final List<String> operatingSystems) { this.operatingSystems = operatingSystems; }

    public List<String> getLibcTypes() { return null == libcTypes ? List.of() : libcTypes; }
    public void setLibcTypes(final List<String> libcTypes) { this.libcTypes = libcTypes; }

    public List<String> getReleaseStatus() { return null == releaseStatus ? List.of() : releaseStatus; }
    public void setReleaseStatus(final List<String> releaseStatus) { this.releaseStatus = releaseStatus; }

    public List<String> getTermsOfSupport() { return null == termsOfSupport ? List.of() : termsOfSupport; }
    public void setTermsOfSupport(final List<String> termsOfSupport) { this.termsOfSupport = termsOfSupport; }

    public String getBitness() { return null == bitness ? "" : bitness; }
    public void setBitness(final String bitness) { this.bitness = bitness; }

    public String getJavafxBundled() { return null == javafxBundled ? "" : javafxBundled; }
    public void setJavafxBundled(final String javafxBundled) { this.javafxBundled = javafxBundled; }

    public String getDirectlyDownloadable() { return null == directlyDownloadable ? "" : directlyDownloadable; }
    public void setDirectlyDownloadable(final String directlyDownloadable) { this.directlyDownloadable = directlyDownloadable; }

    public String getSignatureAvailable() { return null == signatureAvailable ? "" : signatureAvailable; }
    public void setSignatureAvailable(final String signatureAvailable) { this.signatureAvailable = signatureAvailable; }

    public String getLatest() { return null == latest ? "" : latest; }
    public void setLatest(final String latest) { this.latest = latest; }

    @Override public String toString() {
        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append("{")
                  .append("\"").append("name").append("\"").append(":").append("\"").append(getName()).append("\"").append(",")
                  .append("\"").append("distribution").append("\"").append(":").append(getDistributions().isEmpty() ? "[]" : getDistributions().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("basic_scope").append("\"").append(":").append(getBasicScopes().isEmpty() ? "[]" : getBasicScopes().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("build_scope").append("\"").append(":").append(getBuildScopes().isEmpty() ? "[]" : getBuildScopes().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("download_scope").append("\"").append(":").append(getDownloadScopes().isEmpty() ? "[]" : getDownloadScopes().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("usage_scope").append("\"").append(":").append(getUsageScopes().isEmpty() ? "[]" : getUsageScopes().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("match").append("\"").append(":").append("\"").append(getMatch()).append("\"").append(",")
                  .append("\"").append("version").append("\"").append(":").append("\"").append(getVersion()).append("\"").append(",")
                  .append("\"").append("architecture").append("\"").append(":").append(getArchitectures().isEmpty() ? "[]" : getArchitectures().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("archive_type").append("\"").append(":").append(getArchiveTypes().isEmpty() ? "[]" : getArchiveTypes().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("package_type").append("\"").append(":").append(getPackageTypes().isEmpty() ? "[]" : getPackageTypes().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("operating_system").append("\"").append(":").append(getOperatingSystems().isEmpty() ? "[]" : getOperatingSystems().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("lib_c_type").append("\"").append(":").append(getLibcTypes().isEmpty() ? "[]" : getLibcTypes().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("release_status").append("\"").append(":").append(getReleaseStatus().isEmpty() ? "[]" : getReleaseStatus().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("term_of_support").append("\"").append(":").append(getTermsOfSupport().isEmpty() ? "[]" : getTermsOfSupport().stream().collect(Collectors.joining("\",\"","[\"","\"]"))).append(",")
                  .append("\"").append("bitness").append("\"").append(":").append("\"").append(getBitness()).append("\"").append(",")
                  .append("\"").append("javafx_bundled").append("\"").append(":").append("\"").append(getJavafxBundled()).append("\"").append(",")
                  .append("\"").append("directly_downloadable").append("\"").append(":").append("\"").append(getDirectlyDownloadable()).append("\"").append(",")
                  .append("\"").append("signature_available").append("\"").append(":").append("\"").append(getSignatureAvailable()).append("\"").append(",")
                  .append("\"").append("latest").append("\"").append(":").append("\"").append(getLatest()).append("\"").append("")
                  .append("}");
        return msgBuilder.toString();
    }
}
