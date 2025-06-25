# TODO Tasks

## Task 1: Refactor Health Error Handling in MetaRoutes
- [ ] **Completed**

### Description
Refactor the health error handling logic in MetaRoutes to eliminate magic strings, improve functional programming approach, and extract helper functions into separate files.

### Prompt
Refactor the health error handling in MetaRoutes by:
1. Move `handleDatabaseHealthStatus` and `handleHealthError` functions out of MetaRoutes into a new dedicated file for better separation of concerns
2. Replace magic strings with enums for database names ("MongoDB", "PostgreSQL") and statuses ("UP", "DOWN")
3. Replace the if-statement approach in error handling with a functional approach using pattern matching or when expressions
4. Ensure the code follows Kotlin best practices and maintains the same functionality

### Affected Files
- `src/main/kotlin/io/sdkman/broker/adapter/primary/rest/MetaRoutes.kt`
- New file to be created for extracted functions

## Task 2: Refactor PostgreSQL Repository to Use Functional Programming
- [ ] **Completed**

### Description
Refactor PostgresHealthRepository to eliminate nullable types, use Arrow's Option and Either combinators instead of the `use` combinator, and improve functional programming approach.

### Prompt
Refactor the PostgresHealthRepository by:
1. Replace nullable types with Arrow's `Option` type throughout the codebase
2. Replace the `use` combinator with Arrow's `Option` and `Either` combinators where possible
3. Add a well-named extension method to ResultSet that hides the operations (next() and getInt(1))
4. Change the method signature of `executeHealthCheck` to return `Either` instead of throwing exceptions
5. Maintain the same functionality while improving the functional programming approach

### Affected Files
- `src/main/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepository.kt`

## Task 3: Refactor HealthService to Use Functional Programming
- [ ] **Completed**

### Description
Refactor HealthService to eliminate nullable types, use zipOrAccumulate for error accumulation, and improve functional programming approach.

### Prompt
Refactor the HealthService by:
1. Replace all nullable types with Arrow's `Option` type
2. Replace `isLeft()` checks with proper functional programming patterns
3. Implement error accumulation using `zipOrAccumulate` as described in the Arrow documentation for fail-first vs accumulation
4. Ensure the BothDatabasesUnavailable, MongoDatabaseUnavailable, and PostgresDatabaseUnavailable error types use `Option` instead of nullable types
5. Maintain the same functionality while improving the functional programming approach

### Affected Files
- `src/main/kotlin/io/sdkman/broker/application/service/HealthService.kt`

## Task 4: Refactor PostgreSQL Test Infrastructure
- [ ] **Completed**

### Description
Refactor PostgreSQL test infrastructure to improve test organization, eliminate redundant tests, and create cleaner DataSource management.

### Prompt
Refactor the PostgreSQL test infrastructure by:
1. Move the `beforeTest` setup logic into the `PostgresTestListener` where before hooks belong
2. Instantiate the repository normally as a field inside the `init` block
3. Remove the `init` block after the `beforeTest` and `repository` refactoring is complete
4. Remove redundant tests that are marked as "so unlikely that we will remove the test"
5. Find a cleaner way of obtaining a DataSource that aligns with current project patterns
6. Move the DataSource creation logic to PostgresTestListener and apply the same pattern in TestDependencyInjection

### Affected Files
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepositoryIntegrationSpec.kt`
- `src/test/kotlin/io/sdkman/broker/adapter/secondary/persistence/PostgresHealthRepositorySpec.kt`
- `src/test/kotlin/io/sdkman/broker/support/TestDependencyInjection.kt`
- `src/test/kotlin/io/sdkman/broker/support/PostgresTestListener.kt` (to be modified)