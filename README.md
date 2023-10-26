# SonarQube Ballerina Plugin (Sonar Plugin API)

SonarQube plugins allow developers to provide static code analysis support for new languages.

This plugin consist of 2 parts to function, the bal scan tool and the SonarQube ballerina plugin.
The SonarQube Ballerina Plugin is dependent on the bal scan tool, as the scanner is responsible
for performing the static code analysis for ballerina projects and the plugin is responsible for
aggregating and reporting the results on the scan to the SonarQube server.

## Prerequisites

The following software should be installed locally

- [Java version: 17](https://adoptium.net/temurin/releases/?version=17)
- [SonarQube version: 9.9 (LTS)](https://www.sonarsource.com/products/sonarqube/downloads/lts/9-9-lts)
- [Ballerina version: 2201.8.2](https://ballerina.io/downloads/archived/#swan-lake-archived-versions)

## Getting started

1. Create the bal scan tool by following the instructions [here](https://github.com/Xenowa/sonar-ballerina/ScanCommand/)
2. Create the SonarQube Ballerina Plugin by following the instructions [here](https://github.com/Xenowa/sonar-ballerina/sonar-ballerina-plugin)
3. Run SonarQube server instance
4. Initialize a sonar scan in a project, [example](https://github.com/SonarDance/SonarQube-scans-testing)
