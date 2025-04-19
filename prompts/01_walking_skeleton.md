# Walking Skeleton Feature

Generate an application that implements the **smallest possible slice** of functionality, namely the **health check** feature. This health check should be **deep** and span all layers of the application:

* An acceptance test that calls the healthcheck endpoint
* An HTTP handler that accepts a health check request
* Delegates to the `ApplicationRepo` to fetch the single record in that collection
* Reads the `alive` field and verifies that the value is `OK`

## Stack

Use the following tech stack to implement the app:

* Kotlin
* Arrow
* KMongo
* Kotest
* TestContainers
* Gradle

It should re-implement the health check following exactly what is described in the @legacy_broker_services.md file, including URL, response codes etc.

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

