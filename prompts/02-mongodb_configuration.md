# MongoDB Configuration Feature

## Context

This application is a replacement for the [SDKMAN! Broker service](https://github.com/sdkman/sdkman-broker) and aims
to be a like-for-like replacement built on a modern JVM stack. It will be deployed on the DigitalOcean Apps platform
and will run in a Docker container. It uses MongoDB for persistence.

The service is build with Kotlin and Ktor and integrates with an old version of MongoDB (v3.2). This forces us to use
an older synchronous Java client instead of the modern async Kotlin driver.

Our current MongoDB connectivity is naive, and only allows connecting to a local MongoDB instance on the default port
without an auth mechanism or credentials.

## Tasks

Implement flexible configuration that can connect both to a local MongodDB instance and a remote secured MongoDB.

## Constraints

* the application should  optional `MONGODB_HOST` `MONGODB_PORT` environment variables
* the `MONGODB_URI` is made redundant and is removed
* the application should consume optional environment variables: `MONGODB_USERNAME` and `MONGODB_PASSWORD`
* propagates the username and password if they are detected
* propagates an `authMechanism` of `SCRAM` if running in a production environment
* omits username, password and auth mechanism when running locally or in a CI environment where MongodDB is bound to `localhost`
* modify `AppConfig` to handle the new configuration
* use `Option` when dealing with environment variables
* implement this logic in a collaborator class called `MongoConnectivity`
* the `MongoConnectivity` should be instantiated in the `App` main method with the `Appconfig` parameter.
* The `MongoConnectivity` class should be located in a package near the Mongo adapter(s)
* The `MongoConnectivity` class should provide an already-configured `MongoDatabase` instance that may be injected into repository classes

## Specification by Example

```gherkin
Scenario: The application is running in a development or CI mode
    Given: an application is agnostic of the environment it runs in
    And: a `MONGODB_USERNAME` environment variable is not present
    And: a `MONGODB_PASSWORD` environment variable is not present
    And: the datastore is located at `localhost`
    And: the datastore is exposed on port `27017`
    When: the application connects to the datastore
    Then: the connection string `mongodb://localhost:27017/sdkman` is applied

Scenario: The datastore is running in a deployed mode
    Given: an application is agnostic of the environment it runs in
    And: a `MONGODB_USERNAME` environment variable is provided as `broker`
    And: a `MONGODB_PASSWORD` environment variable is provided as `password123`
    And: the datastore is located at `mongo.sdkman.io`
    And: the datastore is exposed on port `16434`
    When: the application connects to the datastore
    Then: the connection string `mongodb://broker:password123@mongo.sdkman.io:16434/sdkman?authMechanism=SCRAM-SHA-1` is applied
```
