# Change Log
This file contains all the notable changes done to the Ballerina Email package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- [API Docs Updated](https://github.com/ballerina-platform/ballerina-standard-library/issues/3463)

## [2.2.4] - 2022-04-15

### Fixed
- [[#2820] Html content sends as raw text if content type is not provided](https://github.com/ballerina-platform/ballerina-standard-library/issues/2820)

## [2.2.3] - 2022-03-23

### Fixed
- [[#2778] Could not able to send html template as the email body](https://github.com/ballerina-platform/ballerina-standard-library/issues/2778)
- [[#2777] IMAP listener could not properly identify the content-type of the received emails](https://github.com/ballerina-platform/ballerina-standard-library/issues/2777)

## [2.2.2] - 2022-03-09

### Fixed
- [[#2739] Error when optional body not present in sendMessage of email module](https://github.com/ballerina-platform/ballerina-standard-library/issues/2739)

## [0.2.0-beta.1] - 2021-06-02

### Fixed
 - [[#1136] Allow reflection access to sun.security.util.HostnameChecker](https://github.com/ballerina-platform/ballerina-standard-library/issues/1136)
 - [[#1574] Avoid POP listener continuously receiving the same email message](https://github.com/ballerina-platform/ballerina-standard-library/issues/1574)
 - [[#2055] Remove Java stacktrace getting logged while Ballerina code execution](https://github.com/ballerina-platform/ballerina-standard-library/issues/2055)
 - [[#1162] Automatically set the attachment file name from the attachment file path](https://github.com/ballerina-platform/ballerina-standard-library/issues/1162)
 - [[#2633] `readonly & email:Message` type is accepted from the email listener](https://github.com/ballerina-platform/ballerina-standard-library/issues/2633)

### Changed
 - [[#2398] Mark Service type as distinct](https://github.com/ballerina-platform/ballerina-standard-library/issues/2398)

## [1.1.0-alpha8] - 2021-04-22

### Added
- [[#1248] Make possible to identify Compiler Plugin when email listener is given as a variable to the service](https://github.com/ballerina-platform/ballerina-standard-library/issues/1248)
- [[#1113] Add compiler extension for Email module](https://github.com/ballerina-platform/ballerina-standard-library/issues/1113)
