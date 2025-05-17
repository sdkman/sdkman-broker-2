# [Feature Name] Feature

## Preamble

Generate a new feature that implements [scope description], namely the **[feature name]**. This feature should be implemented across the following architectural components:

* API/Controller Layer
* Domain/Service Layer
* Repository/Data Access Layer
* Infrastructure Components
* Cross-cutting Concerns (logging, error handling, etc.)

Do NOT implement business logic in the controller layer or mix concerns across layers!

The implementation should follow existing patterns in the codebase and integrate with the current architecture.

## Technical Requirements

* API Specification (endpoints, request/response models)
* Data Models and DTOs
* Service Interfaces
* Error Handling Cases
* Performance Considerations
* Security Requirements

## AI Guidelines

* follow the Kotlin style guide rules @kotlin-rules.md
* follow the Testing rules @testing-rules.md
* follow the DDD guideline rules @ddd-rules.md
* follow the Hexagonal Architecture rules @hexagonal-architecture-rules.md

## Infrastructure Considerations

* Required External Services/Dependencies
* Configuration Requirements
* Deployment Considerations
* Monitoring/Observability Requirements
* Cache Strategy (if applicable)
* Rate Limiting Requirements (if applicable)

## Error Scenarios

* Invalid Input Handling
* External Service Failures
* Resource Not Found Cases
* Authorization Failures
* Rate Limit Exceeded
* System Resource Constraints

## Gherkin Scenarios

```gherkin
Feature: [Feature Name]
    Scenario: Successful Operation
        Given [precondition]
        When [action]
        Then [expected outcome]

    Scenario: Invalid Request
        Given [precondition]
        When [action]
        Then [expected error response]

    Scenario: Resource Not Found
        Given [precondition]
        When [action]
        Then [expected error response]

    Scenario: Authorization Failure
        Given [precondition]
        When [action]
        Then [expected error response]
```

## Testing Requirements

* Unit Tests Coverage
* Integration Tests Scope
* Acceptance Tests Scope
* Mock/Stub Requirements
* Supporting Test Containers
* Test Container Integration
* Test Data Setup

## Acceptance Criteria

* API Contract Compliance
* Error Handling Requirements
* Performance Thresholds
* Security Requirements
* Documentation Requirements
* Monitoring/Logging Requirements
