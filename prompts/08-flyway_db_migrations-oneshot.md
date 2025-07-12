# Flyway Database Migrations

*Add Flyway database migration support to the SDKMAN Broker application to enable versioned database schema changes. This will provide a systematic way to evolve the database schema over time, ensuring consistency across different environments and enabling rollback capabilities when needed.*

## Requirements

- Flyway dependency is added to the build configuration
- Database migration configuration is properly set up with datasource connection
- Migration scripts directory structure is created following Flyway conventions
- Initial baseline migration is created for existing database schema
- Initial migration should create `audit` table as specified in `specs/legacy_broker_service.md`
- Flyway migrations execute automatically at application startup
- Migration status can be queried and verified
- Failed migrations prevent application startup
- Migration versioning follows semantic naming conventions

## Extra Considerations

- Migration scripts must be immutable once applied to production
- Database connection pooling compatibility with migration execution
- Transaction handling during migration execution
- Migration script naming conventions for proper ordering
- Handling of existing database schema if present
- Migration script validation and testing
- Rollback strategy for failed migrations
- Keep it simple! Try to do this in the fewest lines of code possible
- No need for new components like services
- The application should _always_ migrate on start
- Do not make `migrate-on-start` configurable

## Implementation Notes

- Use Flyway Community Edition for open-source compatibility
- Configure Flyway through application configuration (not embedded programmatically)
- Place migration scripts in `src/main/resources/db/migration/` following Flyway conventions
- Use standard versioning for migration scripts: `V1__Initial_schema.sql`
- Use a standard generated UUID Primary Key for the `audit` table
- No indexes on the `audit` table needed at this stage
- Implement migration execution as part of application startup lifecycle
- Use database-specific SQL for optimal performance
- Configure Flyway in the simplest way possible
- Do not configure Flyway programmatically

## Specification by Example

The legacy application has a MongoDB collection for the `audit` entity. We should provide the equivalent as specified in the legacy specification.
The following is a sample document of the `audit` collection:

```mongodb
{
  "_id": ObjectId("..."),
  "command": "install",
  "candidate": "java",
  "version": "17.0.2-tem",
  "host": "203.0.113.195",
  "agent": "curl/7.68.0",
  "platform": "DarwinARM64",
  "dist": "MAC_ARM64",
  "timestamp": 1642532429843
}
```

## Verification

- [ ] Flyway dependency is added to build.sbt
- [ ] Database migration configuration is present in application.yml
- [ ] Migration scripts directory exists with proper structure
- [ ] Initial migration script creates expected database schema
- [ ] Application starts successfully with fresh database
- [ ] Application starts successfully with existing database
- [ ] Failed migration prevents application startup
- [ ] Migration versioning follows `V{version}__{description}.sql` pattern
- [ ] All tests pass including migration-related scenarios
- [ ] Code is formatted with `./gradlew ktlintFormat`
- [ ] Documentation is updated with migration procedures
