# Flyway DB Migrations Test-Only

Move flyway migration execution from main application to test-only setup. External migration processes will handle production database schema, but tests need local migration for integration/acceptance testing against PostgreSQL testcontainers.

## Requirements

- Move audit table migration from `src/main/resources/db/migration/` to `src/test/resources/db/migration/`
- Remove flyway execution from main application startup (`App.kt`)
- Remove flyway configuration from `AppConfig` and `application.conf`
- Integrate flyway migration into `PostgresTestListener.beforeSpec()` for test execution
- Maintain same migration file naming convention and content

## Rules

- rules/kotlin-rules.md
- rules/kotest-rules.md

## Extra Considerations

- Migration should only run once per PostgreSQL testcontainer lifecycle
- Use testcontainer connection details for flyway configuration in tests
- Remove all flyway dependencies from main application configuration
- Preserve existing test database setup patterns

## Testing Considerations

- Integration and acceptance tests must pass with migrated audit table schema
- Verify no flyway execution occurs in main application startup
- Confirm audit table exists in test database after migration

## Implementation Notes

- Use existing PostgresTestListener infrastructure for migration integration
- Follow test-only dependency patterns already established in the project
- Remove flyway configuration properties from AppConfig interface and DefaultAppConfig

## Verification

- [ ] Migration file moved to `src/test/resources/db/migration/` with correct naming
- [ ] Flyway execution removed from `App.kt` main method
- [ ] Flyway configuration removed from `AppConfig` and `DefaultAppConfig`
- [ ] Flyway migration integrated into `PostgresTestListener.beforeSpec()`
- [ ] Integration tests pass with audit table available
- [ ] Main application starts without running migrations
