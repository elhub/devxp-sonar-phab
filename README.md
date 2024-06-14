> [!WARNING]  
> This project has been archived and is no longer being actively developed by Elhub.

# sonar-phab

[<img src="https://img.shields.io/badge/repo-github-blue" alt="">](https://github.com/elhub/devxp-sonar-phab)
[<img src="https://img.shields.io/badge/issues-jira-orange" alt="">](https://jira.elhub.cloud/issues/?jql=project%20%3D%20%22Team%20Dev%22%20AND%20component%20%3D%20devxp-sonar-phab%20AND%20status%20!%3D%20Done)
[<img src="https://teamcity.elhub.cloud/app/rest/builds/buildType:(id:DevXp_DevXpSonarPhab_AutoRelease)/statusIcon" alt="">](https://teamcity.elhub.cloud/project/DevXp_DevXpSonarPhab?mode=builds#all-projects)

## About

**sonar-phab** is a simple application that takes the result of a branch run of the sonarqube scanner (using the
report-task.txt file generated by a scanner run) from the Sonarqube server, and then posts the results as lint
comments in a Phabricator diff.

## Getting Started

### Prerequisites

This application requires Java 1.8 or later.

**sonar-phab** depends on a number of environment variables being set in order to run correctly. These are:

* **SONAR_RESULTS**: The path of the sonar report task summary file (report-task.txt).
* **PHABRICATOR_URI**: The URI for the Phabricator server.
* **PHABRICATOR_HARBORMASTER_PHID**: The phid of Harbormaster in Phabricator.
* **PHABRICATOR_CONDUIT_TOKEN**: The conduit token of the user that the process will run as.

### Installation

The latest version can be downloaded from Elhub's internal artifactory under _elhub-bin/sonar-phab/_.

To build the current version, run:

```sh
./gradlew assemble
```

To publish the executable jar to artifactory, run:

```sh
./gradlew publish
```

## Usage

Assuming environment variables have been set, using **sonar-phab** is simply a matter of running the binary
in the working directory of the project to be processed.

```sh
java -jar sonar-phab.jar
```

It is assumed that the sonarId (projectKey) and sonarBranch (branch - which should correspond to the diff) can be
extracted from report-task.txt.

The application thus reads report-task.txt, contacts the SonarQube server, waits for the sonar analysis to complete
(if it is still ongoing), then retrieves the issues detected and post them to the Diff on phabricator indicated by
the sonarBranch id.

## Testing

The unit tests can be run using:

```sh
./gradlew test
```

## Contributing

Contributing, issues and feature requests are welcome. See the
[Contributing](https://github.com/elhub/dev-tools-sonar-phab/blob/main/CONTRIBUTING.md) file.

## Owners

This project is developed by [Elhub](https://github.com/elhub). For the specific development group responsible for this
code, see the [Codeowners](https://github.com/elhub/dev-tools-sonar-phab/blob/main/CODEOWNERS) file.

## License

This project is [MIT](https://github.com/elhub/dev-tools-sonar-phab/blob/main/LICENSE.md) licensed.
