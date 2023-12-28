# ScanCommand

## About

- ScanCommand is a ballerina tool
- This tool adds "bal scan" command support for ballerina
- The goal of the tool is to be able to provide static code analysis functionality for ballerina

## How it works

1. First a ballerina project should be opened

2. Next run the following command in the console:

```cmd
bal scan [--platformPlugin=<option>] [<ballerina-file/ballerina-build-project-folder>]
```

3. The tool itself will determine if the file is in a ballerina project and perform the static code analysis

4. Finally, the analysis results will be displayed in the console output

## Features

- Generate a SonarQube plugin API analysis output
- Generate a CodeQL analysis output (coming soon...)
- Generate a SemGrep analysis output (coming soon...)

## Usage (Local)

1. Run and build jar file

```cmd
gradlew clean build -x check
```

2. Navigate to the tool-scan directory

```cmd
cd tool-scan
```

3. Generate a bala file

```cmd
bal pack
```

4. Push the bala file to local repository

```cmd
bal push --repository=local
```

5. Move the tool_scan to the central.ballerina.io, bala folder

```
📦<USER_HOME>/.ballerina/repositories/central.ballerina.io
 ┗ 📦bala
    ┗📦tharana_wanigaratne
      ┗**📦tool_scan**
📦local
```

6. modify the .config folders following files

```
📦<USER_HOME>/.ballerina
 ┗ 📦.config
    ┗**📜bal-tools.toml**
    ┗**📜dist-2201.8.2.toml**
```

7. Include the tool details in them as follows

```
# (bal-tools.toml)
[[tool]]
id = "scan"
org = "tharana_wanigaratne"
name = "tool_scan"
```

```
# (dist-2201.8.2.toml)
[[tool]]
id = "scan"
org = "tharana_wanigaratne"
name = "tool_scan"
version = "0.1.0"
```

9. Check if the tool is added using the cmd

```cmd
bal tool list
```

10. Try out the tool

```cmd
bal scan [--platformPlugin=<option>] [<ballerina-file/ballerina-build-project-folder>]
```

- --option--:

``` 
option 1: sonarqube
option 2: codeql
option 3: semgrep
```

- About ```bal scan --platformPlugin=sonarqube```:
    - this command will perform analysis and report issues to SonarQube
    - this command will require a running instance of SonarQube
    - this command will require a properly configured sonar-project.properties file in the initializing directory

## Usage (Remote - https://dev-central.ballerina.io/)

1. Run and build jar file

```cmd
gradlew clean build -x check
```

2. Navigate to the tool-scan directory

```cmd
cd tool-scan
```

3. Go to [ballerina-dev-central](dev-central.ballerina.io) and create an account

4. Follow the steps provided [here](https://ballerina.io/learn/publish-packages-to-ballerina-central/)

5. Find the settings.toml file in the user directory

```
📦<USER_HOME>/.ballerina/
 ┣ 📂.config
 ┣ 📂repositories
 ┣ 📜ballerina-version
 ┣ 📜command-notice
 ┣ 📜installer-version
 ┗ 📜**settings.toml**
```

6. Update the Settings toml file with the credentials

```settings.toml
[central]
accesstoken="TOKEN_FROM_CENTRAL"
```

7. Configure the Ballerina.toml file in the tool-scan project directory as follows

```Ballerina.toml
[package]
org = "DEV_CENTRAL_ORGANIZATION_NAME"
name = "tool_scan"
version = "0.1.0"
distribution = "2201.8.2"
```

8. Set the ballerina dev central environment variable to true

```cmd
set BALLERINA_DEV_CENTRAL=true
```

9. Create a bala file

```cmd
bal pack
```

10. Push the bala file to dev central

```cmd
bal push
```

11. pull to ballerina tool from dev central and use it