# Corrective actions

## TODO List

### Task 1: Update Database Schema Column Types

- [ ] **Convert VARCHAR columns to TEXT in audit table schema**

**Prompt**: Update the Flyway migration script `V1__Initial_audit_table.sql` to change all VARCHAR column definitions to TEXT type. The current schema uses VARCHAR with specific length constraints (VARCHAR(50), VARCHAR(100), etc.) but these should be changed to TEXT for better flexibility and consistency with PostgreSQL best practices.

**Files affected**:
- `src/main/resources/db/migration/V1__Initial_audit_table.sql`

### Task 2: Refactor Flyway Configuration to Use AppConfig

- [ ] **Move access to Flyway configuration values to AppConfig**

**Prompt**: Refactor the Flyway configuration in `App.kt` to properly use the AppConfig class instead of hardcoded getOrElse fallback values. Currently, the code uses `config.postgresUsername.getOrElse { "postgres" }` and `config.postgresPassword.getOrElse { "postgres" }` which bypasses proper configuration management. Update the AppConfig class to include dedicated Flyway configuration properties and modify the App.kt file to use these properties directly without fallback defaults. Ensure that all Flyway-related configuration is centralized in the AppConfig class following the existing configuration patterns in the codebase.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/App.kt`
- `src/main/kotlin/io/sdkman/broker/config/DefaultAppConfig.kt` (or equivalent config file)
