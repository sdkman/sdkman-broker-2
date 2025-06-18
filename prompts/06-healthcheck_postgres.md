# PostgreSQL Health Check Feature

## Context

This application is a replacement for the [SDKMAN! Broker service](https://github.com/sdkman/sdkman-broker) and aims
to be a like-for-like replacement built on a modern JVM stack. It will be deployed on the DigitalOcean Apps platform
and will run in a Docker container. It uses both MongoDB and PostgreSQL for persistence.

The existing health check only validates MongoDB connectivity. We need to extend the health check to also validate
PostgreSQL connectivity, ensuring both databases are healthy before the service reports as UP.

## Tasks

Enhance the existing health check functionality to include PostgreSQL connectivity validation alongside the current
MongoDB health check. Both databases must be healthy for the service to report as UP.

## Constraints

* Follow the same patterns as the existing MongoDB health check implementation
* Create a lightweight PostgreSQL repository that performs a simple connectivity test using `SELECT 1` or `SELECT version()`
* The PostgreSQL health check should integrate seamlessly with the existing `HealthService`
* Both MongoDB and PostgreSQL must be healthy for the overall health check to pass
* If either database is unavailable, the service should return HTTP 503
* The health check endpoint should return a JSON response body with the status of both databases
* The PostgreSQL repository should be located in the same package as other repository classes
* Use the existing `PostgresConnectivity` class to obtain database connections
* Follow the same error handling patterns as the MongoDB implementation
* Implement appropriate unit and integration tests
* Use the existing test patterns with TestContainers for PostgreSQL integration tests

## Carefully observe all the following rules:

Rules files are under `rules/`
* follow the Kotlin style guide rules `kotlin-rules.md`
* follow the Kotlin Testing rules `testing-rules.md`
* follow the DDD guideline rules `ddd-rules.md`
* follow the Hexagonal Architecture rules `hexagonal-architecture-rules.md`

## Implementation Details

* Create a `PostgresHealthRepository` interface and implementation in the `adapter.secondary.persistence` package
* The repository should have a simple `checkConnectivity(): Either<Throwable, Unit>` method
* Use a simple query like `SELECT 1` to validate database connectivity
* Modify the existing `HealthService` to check both MongoDB and PostgreSQL connectivity
* Update the `HealthServiceImpl` to orchestrate both database health checks
* Both databases must be healthy for the service to report `HealthStatus.UP`
* If either database fails, return appropriate `HealthCheckError` with database-specific context
* Enhance the health check response to include a JSON body with individual database statuses
* The JSON response should include `mongodb` and `postgres` fields showing their individual health status
* Update the REST endpoint to return the detailed health information in JSON format
* Update the dependency injection in `App.kt` to wire the new PostgreSQL health repository

## Specification by Example

```gherkin
Scenario: Both databases are healthy
    Given: MongoDB is accessible and contains valid application data
    And: PostgreSQL is accessible and responsive
    When: a GET request is made for "/health/alive"
    Then: the service response status is 200
    And: the response body contains JSON with mongodb: "UP" and postgres: "UP"

Scenario: MongoDB is healthy but PostgreSQL is inaccessible
    Given: MongoDB is accessible and contains valid application data
    And: PostgreSQL is inaccessible due to connectivity issues
    When: a GET request is made for "/health/alive"
    Then: the service response status is 503
    And: the response body contains JSON with mongodb: "UP" and postgres: "DOWN"

Scenario: PostgreSQL is healthy but MongoDB is inaccessible
    Given: PostgreSQL is accessible and responsive
    And: MongoDB is inaccessible due to connectivity issues
    When: a GET request is made for "/health/alive"
    Then: the service response status is 503
    And: the response body contains JSON with mongodb: "DOWN" and postgres: "UP"

Scenario: Both databases are inaccessible
    Given: MongoDB is inaccessible due to connectivity issues
    And: PostgreSQL is inaccessible due to connectivity issues
    When: a GET request is made for "/health/alive"
    Then: the service response status is 503
    And: the response body contains JSON with mongodb: "DOWN" and postgres: "DOWN"
```

## Testing Requirements

* Create unit tests for the new `PostgresHealthRepository`
* Create integration tests using TestContainers for PostgreSQL connectivity
* Update existing `HealthService` tests to cover the new dual-database scenarios
* Update acceptance tests to validate the new health check behavior
* Ensure all tests pass when running `./gradlew check`
* Follow the existing test patterns and structure

## Acceptance Criteria

* Health check validates both MongoDB and PostgreSQL connectivity
* Service returns 200 only when both databases are healthy
* Service returns 503 if either database is unhealthy
* All existing tests continue to pass
* New tests cover the PostgreSQL health check functionality
* Integration tests use TestContainers for PostgreSQL
* Code follows existing patterns and architectural guidelines
* All code is properly formatted with `./gradlew ktlintFormat`
