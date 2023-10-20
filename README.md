# SonarQube Ballerina Plugin (Sonar Plugin API)

## How it works

This Plugin provides Ballerina static code analysis support for SonarQube.

## Other ways to create a SonarQube plugin

- Using SSLR (SonarSource Language Recognizer)

  - [SSLR documentation](https://github.com/SonarSource/sslr)
  - [SonarQube Java Plugin](https://github.com/SonarSource/sonar-java)
  - [SonarQube Python Plugin](https://github.com/SonarSource/sonar-python)

- Using SLang (Sonar Lang)

  - [SLang documentation](https://github.com/SonarSource/slang)
  - [SonarQube jProperties Plugin](https://github.com/pepaproch/slang-jproperties)

- Using SonarQube Java Plugin API

  - [SonarQube Java Plugin API documentation](https://docs.sonarsource.com/sonarqube/9.9/extension-guide/developing-a-plugin/plugin-basics/)
  - [SonarQube Kotlin Plugin](https://github.com/SonarSource/SonarJS)
  - [SonarQube JavaScript Plugin](https://github.com/SonarSource/SonarJS)

## Plugin Creation steps

Steps will be added soon...

## Resources utilized for testing and creating the plugin

- Testing Source files analysis from SonarQube: [SonarQube Scans Project](https://github.com/SonarDance/SonarQube-scans-testing)
- Testing Ballerina Packages for integrations: [Shariff Project](https://github.com/SonarDance/ShariffCommand)

## SonarQube plugin integration steps

1. Run the gradle command to build a jar of the plugin

- With tests

```cmd
gradlew clean build
```

- Ignoring tests

```cmd
gradlew clean build -x test
```

2. The generated jar file can be found in the libs directory as follows:

```
📦sonar-ballerina
 ┣ 📂.gradle
 ┣ 📂.idea
 ┗ 📂build
    ┣ 📂classes
    ┣ 📂generated
    ┗ 📂libs
      ┗ **📜sonar-ballerina-1.0-SNAPSHOT-all.jar**
```

3. Place the generated jar file in the plugins directory of SonarQube as follows:

```
📦sonarqube-9.9.2.77730
 ┣ 📂bin
 ┣ 📂conf
 ┣ 📂data
 ┣ 📂elasticsearch
 ┣ 📂extensions
 ┃ ┣ 📂downloads
 ┃ ┣ 📂jdbc-driver
 ┃ ┗ 📂plugins
 ┃ ┃ ┣ 📜README.txt
 ┃ ┃ ┗ **📜sonar-ballerina-1.0-SNAPSHOT-all.jar**
```

4. Run SonarQube
