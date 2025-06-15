# PostgreSQL Configuration Feature

## Context

This application is a replacement for the [SDKMAN! Broker service](https://github.com/sdkman/sdkman-broker) and aims
to be a like-for-like replacement built on a modern JVM stack. It will be deployed on the DigitalOcean Apps platform
and will run in a Docker container. It uses PostgreSQL for persistence alongside the existing MongoDB.

The service is built with Kotlin and Ktor and needs to integrate with PostgreSQL using a modern async driver.
We need to implement flexible configuration that can connect both to a local PostgreSQL instance and a remote secured PostgreSQL database.

## Tasks

Implement flexible PostgreSQL configuration that follows the same patterns as the existing MongoDB configuration.

## Constraints

* the application should consume optional `POSTGRES_HOST` and `POSTGRES_PORT` environment variables
* the application should consume optional environment variables: `POSTGRES_USERNAME` and `POSTGRES_PASSWORD`
* the application should consume optional `POSTGRES_DATABASE` environment variable, defaulting to `sdkman`
* propagates the username and password if they are detected
* propagates SSL mode configuration if running in a production environment
* omits SSL requirements when running locally or in a CI environment where PostgreSQL is bound to `localhost`
* modify `AppConfig` to handle the new PostgreSQL configuration alongside existing MongoDB configuration
* use `Option` when dealing with environment variables
* implement this logic in a collaborator class called `PostgresConnectivity`
* the `PostgresConnectivity` should be instantiated in the `App` main method with the `AppConfig` parameter
* The `PostgresConnectivity` class should be located in the same package as `MongoConnectivity`
* The `PostgresConnectivity` class should provide an already-configured database connection pool instance that may be injected into repository classes
* Follow the same error handling patterns as `MongoConnectivity`

## Specification by Example

```gherkin
Scenario: The application is running in a development or CI mode
    Given: an application is agnostic of the environment it runs in
    And: a `POSTGRES_USERNAME` environment variable is not present
    And: a `POSTGRES_PASSWORD` environment variable is not present
    And: the datastore is located at `localhost`
    And: the datastore is exposed on port `5432`
    When: the application connects to the datastore
    Then: the connection string `jdbc:postgresql://localhost:5432/sdkman` is applied

Scenario: The datastore is running in a deployed mode
    Given: an application is agnostic of the environment it runs in
    And: a `POSTGRES_USERNAME` environment variable is provided as `broker`
    And: a `POSTGRES_PASSWORD` environment variable is provided as `password123`
    And: the datastore is located at `postgres.sdkman.io`
    And: the datastore is exposed on port `5432`
    When: the application connects to the datastore
    Then: the connection string `jdbc:postgresql://broker:password123@postgres.sdkman.io:5432/sdkman?sslmode=require` is applied
```