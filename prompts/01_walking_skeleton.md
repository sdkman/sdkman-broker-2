# Walking Skeleton Feature

Generate an application that implements the **smallest possible slice** of functionality, namely the **health check** feature. This health check should be **deep** and span all layers of the application:

* An acceptance test that calls the healthcheck endpoint
* An entry point application called `App` in the `io.sdkman.broker` package
* An HTTP handler that accepts a health check request
* Delegates to the `ApplicationRepo` to fetch the single record in that collection
* Reads the `alive` field and verifies that the value is `OK`

## Stack

Use the following tech stack to implement the app:
* JDK (and JVM) target version 21
* Kotlin (latest stable)
* Arrow
* Ktor
* Typesafe config with HOCON configuration
* Kotest
* TestContainers
* MongoDB 3.2 **IMPORTANT: DON'T USE LATEST VERSION**
* Java MongoDB sync driver compatible with MongoDB 3.2
* Gradle (latest 8.x)

Do NOT attepmpt to use an Arrow testing library, rather create own custom assertions!

It should re-implement the health check following exactly what is described in the @legacy_broker_service.md file, including URL, response codes etc.

## Carefully observe all the following Cursor rules:

* follow the Kotlin style guide rules @kotlin-rules.md
* follow the Kotlin Testing rules @testing-rules.md
* follow the DDD guideline rules @ddd-rules.md

## Extra considerations

* Run only the MongoDB fixture as a test container.
* Do not package the application in Docker during the test phase, but run it on localhost.
* Application to run against the MongoDB test container for healthcheck.
* Acceptance test to run against the application, running on a fixed port on `localhost`
* Use test data as described in the @legacy_broker_service.md
* We'll migrate to a different database in the future, so keep the persistence layer flexible with an SPI.
* Add an `.sdkmanrc` file to peg the JDK version

## BDD

We won't be using Cucumber, but this sums up the expected behaviour:

```gherkin
Feature: Alive
	Scenario: Database is healthy
		Given an initialised database
		When a GET request is made for "/health/alive"
		Then the service response status is 200

	Scenario: Database is inconsistent
		Given an uninitialised database
		When a GET request is made for "/health/alive"
		Then the service response status is 503

	Scenario: Database is inaccessible
		Given an inaccessible database
		When a GET request is made for "/health/alive"
		Then the service response status is 503
```

## Acceptance Criteria

* All tests must pass when running `./gradlew check`
* All code is formatted properly with `./gradlew ktlintFormat`
* We have a simple application, fit for deployment