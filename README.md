# SonarQube Ballerina Plugin (Sonar Plugin API)

SonarQube plugins allow developers to provide static code analysis support for new languages.

This analyzer consist of 2 parts to function, the bal scan tool and the SonarQube ballerina analyzer.
The SonarQube Ballerina Plugin is dependent on the bal scan tool, as the scanner is responsible
for performing the static code analysis for ballerina projects and the analyzer is responsible for
aggregating and reporting the results on the scan to the SonarQube server.

## Prerequisites

The following software should be installed locally

- [Java version: 17](https://adoptium.net/temurin/releases/?version=17)
- [SonarQube version: 9.9 (LTS)](https://www.sonarsource.com/products/sonarqube/downloads/lts/9-9-lts)
- [Sonar scanner](https://docs.sonarsource.com/sonarqube/9.9/analyzing-source-code/scanners/sonarscanner/)
- [Ballerina version: 2201.8.5](https://ballerina.io/downloads/archived/#swan-lake-archived-versions)

## Getting started

1. Create the bal scan tool by following the
   instructions [here](https://github.com/Xenowa/ballerina-scan-tool/tree/main/ScanCommand)
2. Create the SonarQube Ballerina Plugin by following the
   instructions [here](https://github.com/Xenowa/ballerina-scan-tool/tree/main/sonar-ballerina-analyzer)
3. Run SonarQube server instance
4. Initialize a sonar scan in a project, [example](https://github.com/SonarDance/SonarQube-scans-testing)
