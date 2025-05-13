# Version Endpoint Feature

Generate an endpoint in the existing application that returns the current version of the SDKMAN! Broker service. This
endpoint should be accessible at `/version` and return a JSON response.

This response should be based on what the Axion plugin generates in the `build.gradle` file. The version should be read
from the `version.properties` file in the `gradle` directory. The `version.gradle` file is currently **not** written by
the Axion plugin, but it should be.

Requirements:

* An acceptance test that calls the version endpoint
* An HTTP handler that accepts a version request
* Reads the latest release version from a resource on the classpath
* Return this as a JSON response:
```json
{
"version": "1.0.0"
}
```

It should re-implement the version endpoint following what is described in the @legacy_broker_service.md file.
The only difference is in the response body, which is now a JSON response.

## Carefully observe all the following Cursor rules:

* follow the Kotlin style guide rules @kotlin-rules.md
* follow the Kotlin Testing rules @testing-rules.md
* follow the DDD guideline rules @ddd-rules.md
* follow the Hexagonal Architecture rules @hexagonal-architecture-rules.md

## Extra considerations

* The acceptance test should run in exactly the same way as the healthcheck acceptance test.
* The version endpoint should be added to the existing HTTP handler.
* The version for the test should be injected into the application using a test versions.properties file.
* The test version.properties file should be located in the `src/test/resources` directory.
* The test version.properties should override the default version.properties file.

## Specification by Example

You can use the following Gherkin and base the acceptance test on the behaviour described below:

```gherkin
Feature: Version
	Scenario: The version endpoint is called successfully
		Given a running application
		And the version.properties file is present
		When a GET request is made for "/version"
        Then the service response status is 200

	Scenario: The version endpoint is called unsuccessfully
        Given a running application
        And the version.properties file is not present
        When a GET request is made for "/version"
        Then the service response status is 500
```

## Acceptance Criteria

* All tests must pass when running `./gradlew check`
* All code is formatted properly with `./gradlew ktlintFormat`
* We have a version endpoint that can be invoked by `curl`
