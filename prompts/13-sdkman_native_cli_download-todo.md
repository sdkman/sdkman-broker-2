# TODO List

Consider the following rules during execution of the tasks:
- rules/git-rules.md
- kotlin-rules.md
- kotest-rules.md

## Task 1: Rename Native Download Service Classes to Use Sdkman Prefix

- [X] Rename all service classes and interfaces to use "Sdkman" prefix for consistency

**Description**: All native download service classes and interfaces need to be renamed to use the "Sdkman" prefix for better naming consistency and clarity. This includes updating the interface, implementation, and test classes along with all variable names and references.

**Prompt**: Rename the native download service classes and interfaces to use the "Sdkman" prefix for better naming consistency. Update `NativeDownloadService` interface to `SdkmanNativeDownloadService`, `NativeDownloadServiceImpl` class to `SdkmanNativeDownloadServiceImpl`, `NativeDownloadServiceSpec` to `SdkmanNativeDownloadServiceSpec`, and `NativeDownloadAcceptanceSpec` to `SdkmanNativeDownloadAcceptanceSpec`. Also update all associated variable names, imports, and references throughout the codebase to maintain consistency with the new naming convention.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/domain/service/NativeDownloadService.kt`
- `src/main/kotlin/io/sdkman/broker/application/service/NativeDownloadServiceImpl.kt`
- `src/test/kotlin/io/sdkman/broker/application/service/NativeDownloadServiceSpec.kt`
- `src/test/kotlin/io/sdkman/broker/acceptance/NativeDownloadAcceptanceSpec.kt`

## Task 2: Refactor Acceptance Tests to Focus on Essential Cases

- [X] Simplify acceptance tests to cover only essential cases and move detailed testing to unit level

**Description**: The acceptance test currently contains too many detailed test cases that should be moved to the unit test level. The acceptance test should focus on essential end-to-end scenarios while fine-grained testing should be done at the unit level.

**Prompt**: Refactor the `SdkmanNativeDownloadAcceptanceSpec` (after renaming) to test only essential cases at the acceptance level. Move fine-grained testing aspects to the unit test level in `SdkmanNativeDownloadServiceSpec`. The acceptance test should focus on end-to-end scenarios and critical path validation, while detailed parameter validation, edge cases, and specific business logic should be thoroughly tested at the unit level. Ensure both test suites maintain comprehensive coverage while avoiding duplication.

**Files affected**:
- `src/test/kotlin/io/sdkman/broker/acceptance/NativeDownloadAcceptanceSpec.kt`
- `src/test/kotlin/io/sdkman/broker/application/service/NativeDownloadServiceSpec.kt`

## Execution plan workflow

The following workflow applies when executing this TODO list:
- Execute one task at a time
- Implement the task in **THE SIMPLEST WAY POSSIBLE**
- Run the tests, format and perform static analysis on the code:
    - ./gradlew ktlintFormat
    - ./gradlew test
    - ./gradlew detekt
- **Ask me to review the task once you have completed and then WAIT FOR ME**
- Mark the TODO item as complete with [X]
- Commit the change to Git when I've approved and/or amended the code
- Move on to the next task
