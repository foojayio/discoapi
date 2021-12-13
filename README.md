# foojay Disco API 

![Logo](https://github.com/foojay2020/discoapi/raw/main/discoduke.png)

The foojay Disco API is a general purpose API to discover builds of OpenJDK from different distributions

Presentation about the [Disco API](https://de.slideshare.net/han_solo/disco-api-openjdk-distributions-as-a-service).

#### At the moment the following distributions are supported by the API:
* AdoptOpenJDK
* AdoptOpenJDK OpenJ9
* Bi Sheng
* Corretto
* Debian (contains only links to download site)
* Dragonwell
* GraalVM CE (8, 11, 16)
* JetBrains
* Kona
* Liberica
* Liberica Native  
* Mandrel  
* Microsoft  
* OJDKBuild
* OpenLogic  
* Oracle (contains only links to download site)
* Oracle OpenJDK
* Red Hat (contains only links to download site)
* SAP Machine
* Semeru
* Semeru Certified
* Temurin
* Trava  
* Zulu
* Zulu Prime
  
---
### Terms that are used in this document:
* LTS stands for Long Term Support. It means the version will receive security updates for a long time
* MTS stands for Mid Term Support. It means the version will receive security updates at least until the next LTS will be released
* STS stands for Short Term Support. It means the version will receive updates for the next 6 months (Dec 2020)
* GA stands for General Availability. It means that the release is stable
* EA stands for Early Access. It means that the release is not stable and will change probably every week
* Bitness describes 32- or 64-bit architecture
---
### Terms and parameters used in the disco API:
* **archive_type** stands more or less for the file extension of the package e.g. tar.gz, zip, dmg, msi etc.
* **distribution** stands for the name of the distribution (aoj, aoj_openj9, corretto, dragonwell, liberica, liberica_native, oracle, oracle_open_jdk, sap_machine, semeru, temurin, trava, zulu etc.)
* **major_version** stands for the major version of a package e.g. 8, 11, 13, 15 (it is the feature version in the [JEP 322](https://openjdk.java.net/jeps/322))
* **java_version** stands for the full version without trailing '0' which can also include '-ea' for early access builds (e.g. 15-ea, 13.0.5.1 etc.)
* **distribution_version** stands for a distribution specific version number which could also contain additional information
* **release_status** stands for the status of the release which can be either GA (General Availability) or EA (Early Access)
* **term_of_support** stands for the time the version will be supported with updates (e.g. STS, MTS, LTS)
* **operating_system** stands for the operating system the package was build for (e.g. linux, windows, macos etc.)
* **lib_c_type** stands for the type of the standard c library that is used for the build (e.g. glibc, libc, musl or c_std_lib)
* **architecture** stands for the architecture that the package was build for (e.g. aarch64, arm, x64, x86 etc.)
* **package_type** stands for the type of package (e.g. jdk or jre)
* **javafx_bundled** indicates if the package comes bundled with javafx (e.g. true, false)
* **directly_downloadable** indicates if the package can directly be downloaded or if you have to go to the download page of the distribution to get it
* **ephemeral_id** stands for an id that changes every 10 min and will be used to get the real download link (this is done to be able to count download numbers)
---
There are different endpoints that can be used to drill down to the package the user would like to download.

Please find more information here:
[foojay API Swagger doc](https://api.foojay.io/swagger-ui)

---
### REST endpoints 
/api.foojay.io/disco/v2.0/major_versions
/api.foojay.io/disco/v2.0/distributions
/api.foojay.io/disco/v2.0/packages
/api.foojay.io/disco/v2.0/packages/jdks
/api.foojay.io/disco/v2.0/packages/jres
/api.foojay.io/disco/v2.0/ephemeral_ids

---
### Endpoint: major_versions
<b>/api.foojay.io/disco/v2.0/major_versions</b> => Returns all major versions

<b>/api.foojay.io/disco/v2.0/major_versions?ea=true</b> => Returns all major versions including early access builds

<b>/api.foojay.io/disco/v2.0/major_versions?ga=true</b> => Returns all major versions including only general availability builds

<b>/api.foojay.io/disco/v2.0/major_versions?maintained=true</b> => Returns all major versions that are maintained at the moment (e.g. 7, 8, 11, 13, 15, 16, 17-ea, 18-ea)


### Endpoint: distributions
<b>/api.foojay.io/disco/v2.0/distributions</b> => Returns all available distributions incl. their available versions

<b>/api.foojay.io/disco/v2.0/distributions/zulu</b> => Returns the given distribution (here Zulu) with it's available versions


### Endpoint: packages
The packages endpoint can be used with the following url parameters:
- <b>version</b> (e.g. 1.8.0_262, 11.0.9.1, 17-ea.1, 11.0.8..<11.0.10)
  

- <b>distro</b> (e.g. aoj, aoj_openj9, corretto, dragonwell, graalvm_ce8, graalvm_ce11, graalvm_ce16, jetbrains, liberica, liberica_native, mandrel, microsoft, ojdk_build, openlogic, oracle, oracle_open_jdk, redhat, sap_machine, semeru, temurin, trava, zulu, zulu_prime)
  

- <b>architecture</b> (e.g. aarch64, amd64, arm, arm64, ia64, mips, ppc, ppc64el, ppc64le, ppc64, riscv64, s390, s390x, sparc, sparcv9, x64, x86-64, x86, i386, i486, i586, i686, x86-32)
  

- <b>archive_type</b> (e.g. apk, cab, deb, dmg, exe, msi, pkg, rpm, tar, tar.gz, tar.Z, zip)
  

- <b>package_type</b> (e.g. jdk, jre)
  

- <b>operating_system</b> (e.g. aix, alpine_linux, linux, linux_musl, macos, qnx, solaris, windows)
  

- <b>libc_type</b> (e.g. c_std_lib, glibc, libc, musl)
  

- <b>release_status</b> (e.g. ea, ga)
  

- <b>term_of_support</b> (e.g. sts, mts, lts)
  

- <b>bitness</b> (e.g. 32, 64)
  

- <b>javafx_bundled</b> (e.g. true, false)
  

- <b>directly_downloadable</b> (e.g. true, false)
  

- <b>latest</b> (e.g. all_of_version, per_distro, overall, available)

### Get the download link of a package
Let's assume we are looking for the latest version of JDK 11, including JavaFX for MacOS with Intel processor and we would like to use an installer, so it should be either dmg or pkg.
The url parameters will look as follows:
- latest=available
- package_type=jdk
- version=11
- javafx_bundled=true
- operating_system=macos
- architecture=x64 (because of the Intel processor)
- archive_type=dmg
- archive_type=pkg

So the http request will look as follows:
https://api.foojay.io/disco/v2.0/packages?package_type=jdk&latest=available&version=11&javafx_bundled=true&operating_system=macos&architecture=x64&archive_type=dmg&archive_type=pkg

The response to this request is the following:
```JSON
{
  "result": [
    {
      "id": "3fa2a57fff2224a7eba3c8d2c354ec05",
      "archive_type": "dmg",
      "distribution": "zulu",
      "major_version": 11,
      "java_version": "11.0.12+7",
      "distribution_version": "11.50.19",
      "latest_build_available": true,
      "release_status": "ga",
      "term_of_support": "lts",
      "operating_system": "macos",
      "lib_c_type": "libc",
      "architecture": "x64",
      "package_type": "jdk",
      "javafx_bundled": true,
      "directly_downloadable": true,
      "filename": "zulu11.50.19-ca-fx-jdk11.0.12-macosx_x64.dmg",
      "ephemeral_id": "8085c64617b251f5ac6d4cc4143ee3c6fa39ce10",
      "links": {
        "pkg_info_uri": "https://api.foojay.io/disco/v2.0/ephemeral_ids/8085c64617b251f5ac6d4cc4143ee3c6fa39ce10",
        "pkg_download_redirect": "https://api.foojay.io/disco/v2.0/ephemeral_ids/8085c64617b251f5ac6d4cc4143ee3c6fa39ce10/redirect"
      },
      "free_use_in_production": true,
      "tck_tested": true,
      "feature": []
    },
    {
      "id": "d8e231fb3774256719fcc4110020a352",
      "archive_type": "pkg",
      "distribution": "liberica",
      "major_version": 11,
      "java_version": "11.0.12+7",
      "distribution_version": "11+7",
      "latest_build_available": true,
      "release_status": "ga",
      "term_of_support": "lts",
      "operating_system": "macos",
      "lib_c_type": "libc",
      "architecture": "amd64",
      "package_type": "jdk",
      "javafx_bundled": true,
      "directly_downloadable": true,
      "filename": "bellsoft-jdk11.0.12+7-macos-amd64-full.pkg",
      "ephemeral_id": "2ace657a700ed1c3217c00a67913e339b0be3923",
      "links": {
        "pkg_info_uri": "https://api.foojay.io/disco/v2.0/ephemeral_ids/2ace657a700ed1c3217c00a67913e339b0be3923",
        "pkg_download_redirect": "https://api.foojay.io/disco/v2.0/ephemeral_ids/2ace657a700ed1c3217c00a67913e339b0be3923/redirect"
      },
      "free_use_in_production": true,
      "tck_tested": true,
      "feature": []
    },
    {
      "id": "f9800831cd027768ff99e83c7128b729",
      "archive_type": "dmg",
      "distribution": "liberica",
      "major_version": 11,
      "java_version": "11.0.12+7",
      "distribution_version": "11+7",
      "latest_build_available": true,
      "release_status": "ga",
      "term_of_support": "lts",
      "operating_system": "macos",
      "lib_c_type": "libc",
      "architecture": "amd64",
      "package_type": "jdk",
      "javafx_bundled": true,
      "directly_downloadable": true,
      "filename": "bellsoft-jdk11.0.12+7-macos-amd64-full.dmg",
      "ephemeral_id": "34b7158fc7f31c6d6c1a55d5ae7719a40fe2ec91",
      "links": {
        "pkg_info_uri": "https://api.foojay.io/disco/v2.0/ephemeral_ids/34b7158fc7f31c6d6c1a55d5ae7719a40fe2ec91",
        "pkg_download_redirect": "https://api.foojay.io/disco/v2.0/ephemeral_ids/34b7158fc7f31c6d6c1a55d5ae7719a40fe2ec91/redirect"
      },
      "free_use_in_production": true,
      "tck_tested": true,
      "feature": []
    }
  ],
  "message": "3 package(s) found"
}
```
As you can see the API found 3 packages in 2 distributions, Zulu and Liberica.

<b>Attention:</b>

The list of packages will always be in reverse alphabetical order. This will lead to the fact that in most cases the
first package that will be shown will be from the Zulu distribution. The reason for this is simple and it is the fact that
Zulu has the most packages available for all versions and there always is a good chance that if you need a specific Java
version that there is a package from Zulu.

If you know that you would like to have a package from Liberica you simply add the url parameter distro=liberica to the call
above and you will only get the packages from Liberica for the given parameters.

As you can see there is no download link in the response and the reason for that is that we somehow need a way to create some
kind of statistics. For this reason you have to do another request to the ephemeral_ids endpoint with the ephemeral_id of the
package you would like to download.
Please keep in mind that the ephemeral ids for each package will change every couple of minutes to avoid caching.
To make it easier for you to get the download link you simply can call the url that is in the response in the "pkg_info_uri".

So if we make the following request: https://api.foojay.io/disco/v2.0/ephemeral_ids/30dba32311753589fb67efb6222ec2e9b7635b68
we will get this response back:
```JSON
{
  "result": [
    {
      "filename": "zulu11.50.19-ca-fx-jdk11.0.12-macosx_x64.dmg",
      "direct_download_uri": "https://cdn.azul.com/zulu/bin/zulu11.50.19-ca-fx-jdk11.0.12-macosx_x64.dmg",
      "download_site_uri": "",
      "signature_uri": "http://api.azul.com/zulu/download/community/v1.0/bundles/sha256/2437e7da00991c69060d444eb8d2f6aa73b4a6ff65150ddc5b206a0d9f958c0e/signature-binary/?sig_index=0"
    }
  ],
  "message": ""
}
```
In this response you will now get the direct_download_uri which will let you download the package.

---
### IDE Plugins
There are several plugins and extensions available that already make use of the DiscoAPI and that can help you to get
the JDK of your choice even faster.

#### IntelliJ Idea plugin
In the IntelliJ Idea Plugin marketplace you will find the [DiscoIdea](https://plugins.jetbrains.com/plugin/16787-discoidea) plugin.

#### Eclipse plugin
In Eclipse you can find the [DiscoEclipse](https://marketplace.eclipse.org/content/discoeclipse) plugin on the marketplace

#### Visual Studio Code
In Visual Studio Code you can find the [DiscoVSC](https://marketplace.visualstudio.com/items?itemName=GerritGrunwald.discovsc&ssr=false#overview) plugin on the marketplace

---
### Browser plugins
More or less the same plugin that is available for the different IDE's is also available as a browser plugin.

#### Chrome
For Google Chrome please look for [DiscoChrome](https://chrome.google.com/webstore/detail/discochrome/cikmnphhlggceijbbdeohhlkbdagjjce) in the chrome web store

#### Firefox
For Firefox please look for [DiscoFox](https://addons.mozilla.org/addon/discofox/) on the firefox addons page

#### Safari
For Safari please look for [DiscoSafari](https://apps.apple.com/de/app/discosafari/id1571307341?mt=12) on the Mac app store

#### Edge
For Microsoft Edge please look for [DiscoEdge](https://microsoftedge.microsoft.com/addons/detail/efeaimfkdbolmkhafogcoocbidfhdkcm) on Edge plugins page
