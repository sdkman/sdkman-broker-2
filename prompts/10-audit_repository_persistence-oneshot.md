# Audit Repository Persistence Layer

Implement the persistence layer for audit logging to capture every installation that takes place in the SDKMAN broker service. This includes the domain model, repository interface, PostgreSQL implementation, and comprehensive integration testing.

## Requirements

- Create `Audit` domain class representing audit log entries
- Create `AuditRepository` interface following hexagonal architecture patterns
- Implement `PostgresAuditRepository` with save functionality
- Create integration tests using existing PostgreSQL test patterns
- Follow existing code conventions and architectural patterns

## Rules

- rules/ddd-rules.md
- rules/hexagonal-architecture-rules.md
- rules/kotlin-rules.md
- rules/kotest-rules.md

## Domain

```kotlin
// Audit domain entity based on PostgreSQL schema
data class Audit(
    val id: UUID,
    val command: String,
    val candidate: String,
    val version: String,
    val platform: String,
    val vendor: Option<String>,
    val host: String,
    val agent: String,
    val dist: String,
    val timestamp: Instant
)

// Repository interface (port)
interface AuditRepository {
    fun save(audit: Audit): Either<PersistenceFailure, Unit>
}

// Persistence failure types
sealed class PersistenceFailure {
    data class DatabaseConnectionFailure(val exception: Throwable) : PersistenceFailure()
    data class QueryExecutionFailure(val exception: Throwable) : PersistenceFailure()
}
```

## Implementation Notes

- Use existing PostgreSQL configuration and DataSource setup
- Follow patterns from `PostgresHealthRepository` for error handling and logging
- Use Arrow's `Either` for functional error handling
- Implement proper SQL PreparedStatement usage for security
- The repository should NOT be wired into the application yet - that's for a future prompt
- Use the Jetbrains Exposed framework for persistence
- Use Exposed Tables for the persistence
- Use the latest version of Exposed
- Ensure that `PersistenceFailure` may be used by all future entities

## Testing Considerations

- Create integration test following `PostgresHealthRepositoryIntegrationSpec` patterns
- Use `PostgresTestListener` for test database setup
- Test both successful save operations and error conditions
- Assert reading back saved audit entries in happy path to verify persistence
- Use `@Tag("integration")` annotation

## Verification

- [ ] `Audit` domain class created with proper data types
- [ ] `AuditRepository` interface defined in domain layer
- [ ] `PostgresAuditRepository` implemented with *only* save functionality
- [ ] Integration test covers save operation and read-back verification via test support
- [ ] Integration test covers error scenarios (invalid connection, etc.)
- [ ] All tests pass
- [ ] Code follows existing patterns and conventions
