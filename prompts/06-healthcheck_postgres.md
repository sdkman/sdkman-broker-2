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
* Create a lightweight PostgreSQL repository that performs a simple connectivity test
* The PostgreSQL health check should be integrated into the existing `HealthService`
* Both MongoDB and PostgreSQL must be healthy for the overall health check to pass
* If either database is unavailable, the service should return HTTP 503
* The health check endpoint should return a JSON response body with the status of both databases
* Follow the same error handling pattern as the MongoDB implementation

## Carefully observe all the following rules:

Rules files are under `rules/`
* follow the Kotlin style guide rules `kotlin-rules.md`
* follow the Kotlin Testing rules `kotest-rules.md`
* follow the DDD guideline rules `ddd-rules.md`
* follow the Hexagonal Architecture rules `hexagonal-architecture-rules.md`

## Implementation Details

* Create a `HealthRepository` interface with implementation as `PostgresHealthRepository`
* The repository should have a simple `checkConnectivity()` method
* Use a simple query like `SELECT 1` or `SELECT isValid()` to validate database connectivity
* Modify the existing `HealthServiceImpl` to check both MongoDB and PostgreSQL connectivity
* Both databases must be healthy for the service to report `HealthStatus.UP`
* Enhance the health check response to include a JSON body with individual database statuses
* The JSON response should include `mongodb` and `postgres` fields showing their individual health status
* Use `UP` for healthy databases and `DOWN` for unhealthy ones
* Update the REST endpoint to return the detailed health information in JSON format
* Update the dependency injection in `App.kt` to wire the new PostgreSQL health repository
* Do not modify the existing MongoDB health check logic
* Use the existing `PostgresConnectivity` class to obtain the datasource
* If either database fails, return appropriate `HealthCheckError` with database-specific context
* Change the health check URL from `/meta/alive` to `/meta/health`

## Specification by Example

```gherkin
Scenario: Both databases are healthy
    Given: MongoDB is accessible and contains valid application data
    And: PostgreSQL is accessible and responsive
    When: a GET request is made for "/meta/health"
    Then: the service response status is 200
    And: the response body contains JSON with "mongodb": "UP" and "postgres": "UP"

Scenario: MongoDB is healthy but PostgreSQL is inaccessible
    Given: MongoDB is accessible and contains valid application data
    And: PostgreSQL is inaccessible due to connectivity issues
    When: a GET request is made for "/meta/health"
    Then: the service response status is 503
    And: the response body contains JSON with "mongodb": "UP" and "postgres": "DOWN"

Scenario: PostgreSQL is healthy but MongoDB is inaccessible
    Given: PostgreSQL is accessible and responsive
    And: MongoDB is inaccessible due to connectivity issues
    When: a GET request is made for "/meta/health"
    Then: the service response status is 503
    And: the response body contains JSON with "mongodb": "DOWN" and "postgres": "UP"

Scenario: Both databases are inaccessible
    Given: MongoDB is inaccessible due to connectivity issues
    And: PostgreSQL is inaccessible due to connectivity issues
    When: a GET request is made for "/meta/health"
    Then: the service response status is 503
    And: the response body contains JSON with "mongodb": "DOWN" and "postgres": "DOWN"
```

## Testing Requirements

* Create unit tests for the new `PostgresHealthRepository`
* Create an integration test using TestContainers for PostgreSQL connectivity
* The integration test should cover a scenario for successful PostgreSQL connectivity
* The integration test should cover a scenario where PostgreSQL is unavailable
* Update existing `HealthServiceImpl` tests to cover the new dual-database scenarios
* Update acceptance tests to validate all health check permutations or behaviors
* Follow the existing test patterns and structure
* Use TestContainers for PostgreSQL integration tests

## Validation

- [ ] Health check validates both MongoDB and PostgreSQL connectivity
- [ ] Service returns 200 only when both databases are healthy
- [ ] Service returns 503 if either database is unhealthy
- [ ] All existing tests continue to pass
- [ ] New tests cover the PostgreSQL health check functionality
- [ ] Integration test uses TestContainers for PostgreSQL
- [ ] Code follows existing patterns and architectural guidelines
- [ ] All tests pass when running `./gradlew test`
- [ ] All code is properly formatted with `./gradlew ktlintFormat`
