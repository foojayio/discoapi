# foojay Disco API


![Logo](https://github.com/foojay2020/discoapi/raw/main/discoduke.png)

The foojay Disco API is a general purpose API to discover builds of OpenJDK from different distributions

#### At the moment the following distributions are supported by the API:
* AdoptOpenJDK
* AdoptOpenJDK OpenJ9
* Corretto
* Dragonwell
* Liberica
* GraalVM CE
* OJDKBuild  
* Oracle (contains only links to download site)
* Oracle OpenJDK
* Red Hat (contains only links to download site)
* SAP Machine
* Zulu
  
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
* **distribution** stands for the name of the distribution (aoj, corretto, dragonwell, liberica, oracle, oracle_open_jdk, sap_machine, zulu etc.)
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

### How to download a package ?
1. Search for a package using the API (e.g. JDK 1.8.0_275 from Zulu for Windows as msi incl. JavaFX => https://api.foojay.io/disco/v1.0/packages?version=1.8.0_275&distro=zulu&archive_type=msi&package_type=jdk&operating_system=windows&javafx_bundled=true&latest=per_version)
2. Once you have found the package of your choice, get it's ephemeral id from the json response
3. Call the endpoint https://api.foojay.io/disco/v1.0/ephemeral_ids/PACKAGE_EPHEMERAL_ID
4. Get the download link from the json response

The plan is to provide useful statistics about download numbers. For that reason we store the ip address in combination with the package names when packages will be downloaded via the API. At the moment we evaluating if this information is valuable and we will add these statistics to the packages soon.

### Here are some use cases and ways how to handle them using the disco API:

#### 1. What major versions are available incl. early access builds?
```console
curl "https://api.foojay.io/disco/v1.0/major_versions?ea=true&maintained=true"
```

The json response will look as follows (excerpt):
```json
[
  {
    "major_version": 17,
    "term_of_support": "LTS",
    "maintained": true,
    "versions": [
      "17-ea"
    ]
  },
  {
    "major_version": 16,
    "term_of_support": "STS",
    "maintained": true,
    "versions": [
      "16-ea"
    ]
  },
  {
    "major_version": 15,
    "term_of_support": "MTS",
    "maintained": true,
    "versions": [
      "15.0.1",
      "15.0.1-ea",
      "15",
      "15-ea"
    ]
  },
  ...
]
```
---
#### 2. What is the latest long term stable major version?
```console
curl "https://api.foojay.io/disco/v1.0/major_versions/latest_lts"
```
The json response will look as follows:
```json
{
  "major_version": 11,
  "term_of_support": "LTS",
  "maintained": true,
  "versions": [
    "11.0.9.1",
    "11.0.9",
    "11.0.8",
    "11.0.7",
    "11.0.6",
    "11.0.5",
    "11.0.4",
    "11.0.3",
    "11.0.2",
    "11.0.1",
    "11"
  ]
}
```
---
#### 3. What is the latest early access major version?
```console
curl "https://api.foojay.io/disco/v1.0/major_versions/latest_ea"
```
The json response will look as follows:
```json
{
  "major_version": 17,
  "term_of_support": "LTS",
  "maintained": true,
  "versions": [
    "17-ea"
  ]
}
```
---
#### 4. Is there an update available for my Zulu version 11.0.8 incl. JavaFX on MacOS?
```console
curl "https://api.foojay.io/disco/v1.0/packages?version=11.0.8&operating_system=macos&latest=overall&release_status=ga&distro=zulu&archive_type=dmg&package_type=jdk&javafx_bundled=true"
```
The json response will look as follows:
```json
[
  {
    "id": "3edab9b6f6661cc5a64ea19e439959e6",
    "archive_type": "dmg",
    "distribution": "zulu",
    "major_version": 11,
    "java_version": "11.0.9.1",
    "distribution_version": "11.43.55.0",
    "latest_build_available": true,
    "release_status": "ga",
    "term_of_support": "lts",
    "operating_system": "macos",
    "lib_c_type": "libc",
    "architecture": "x64",
    "package_type": "jdk",
    "javafx_bundled": true,
    "directly_downloadable": true,
    "filename": "zulu11.43.55-ca-fx-jdk11.0.9.1-macosx_x64.dmg",
    "ephemeral_id": "3173edab9b6f6661cc5a64ea19e439959e61608732413"
  }
]
```
---
#### 5. What versions does the Zulu distribution offer?
```console
curl "http://81.169.252.235:8080/disco/v1.0/distributions/zulu"
```
The json response will look as follows:
```json
{
  "name": "Zulu",
  "api_parameter": "zulu",
  "versions": [
    "17-ea",
    "16-ea",
    "15.0.1",
    "15",
    "15-ea",
    "14.0.2",
    "14.0.1",
    "14",
    "14-ea",
    "13.0.5.1",
    "13.0.5",
    "13.0.4",
    "13.0.3",
    "13.0.2",
    "13.0.1",
    "13",
    "12.0.2",
    "12.0.1",
    "12",
    "11.0.9.1",
    "11.0.9",
    "11.0.8",
    "11.0.7",
    "11.0.6",
    "11.0.5",
    "11.0.4",
    "11.0.3",
    "11.0.2",
    "11.0.1",
    "11",
    "10.0.2",
    "10.0.1",
    "10",
    "9.0.7",
    "9.0.4",
    "9.0.1",
    "9",
    "8.0.275",
    "8.0.272",
    "8.0.265",
    "8.0.262",
    "8.0.252",
    "8.0.242",
    "8.0.232",
    "8.0.222",
    "8.0.212",
    "8.0.202",
    "8.0.201",
    "8.0.192",
    "8.0.181",
    "8.0.172",
    "8.0.163",
    "8.0.162",
    "8.0.153",
    "8.0.152",
    "8.0.144",
    "8.0.131",
    "8.0.121",
    "8.0.112",
    "8.0.102",
    "8.0.101",
    "8.0.92",
    "8.0.91",
    "8.0.72",
    "8.0.71",
    "8.0.66",
    "8.0.65",
    "8.0.60",
    "8.0.51",
    "8.0.45",
    "8.0.40",
    "8.0.31",
    "8.0.25",
    "8.0.20",
    "8.0.11",
    "8.0.5",
    "8",
    "7.7.0.2",
    "7.7.0.1",
    "7.6.0.7",
    "7.6.0.2",
    "7.0.285",
    "7.0.282",
    "7.0.272",
    "7.0.262",
    "7.0.252",
    "7.0.242",
    "7.0.232",
    "7.0.222",
    "7.0.211",
    "7.0.201",
    "7.0.191",
    "7.0.181",
    "7.0.171",
    "7.0.161",
    "7.0.154",
    "7.0.141",
    "7.0.131",
    "7.0.121",
    "7.0.111",
    "7.0.101",
    "7.0.95",
    "7.0.91",
    "7.0.85",
    "7.0.80",
    "7.0.79",
    "7.0.76",
    "7.0.72",
    "7.0.65",
    "7.0.60",
    "7.0.55",
    "7.0.51",
    "7.0.45",
    "6.0.119",
    "6.0.113",
    "6.0.107",
    "6.0.103",
    "6.0.99",
    "6.0.97",
    "6.0.93",
    "6.0.89",
    "6.0.87",
    "6.0.83",
    "6.0.79",
    "6.0.77",
    "6.0.73",
    "6.0.69",
    "6.0.63",
    "6.0.59",
    "6.0.56",
    "6.0.53",
    "6.0.49",
    "6.0.47",
    "6.0.42"
  ]
}
```
---
