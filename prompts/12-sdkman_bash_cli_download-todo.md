# TODO List

Consider the following rules during execution of the tasks:
- rules/git-rules.md
- kotlin-rules.md
- kotest-rules.md

## Tasks

### Task 1: Move Service Interface to Domain Package

- [X] Refactor SdkmanCliDownloadService interface to follow hexagonal architecture

**Description**: The SdkmanCliDownloadService interface is currently located in the application.service package but should be moved to the domain package to properly follow hexagonal architecture principles. In hexagonal architecture, domain service interfaces should be defined in the domain layer, with implementations in the application layer.

**Prompt**: Move the `SdkmanCliDownloadService` interface from `io.sdkman.broker.application.service` package to the `io.sdkman.broker.domain.service` package. Create the `service` directory under the domain package if it doesn't exist. Update all import statements in the implementation class and any other files that reference this interface. Ensure the interface remains unchanged - only its package location should be modified to follow hexagonal architecture principles where domain interfaces belong in the domain layer.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/application/service/SdkmanCliDownloadService.kt`
- `src/main/kotlin/io/sdkman/broker/domain/service/SdkmanCliDownloadService.kt` (new file)

### Task 2: Improve Error Messages for Empty/Blank Input Validation

- [X] Update error messages for empty and blank input validation to use "[empty/blank]" format

**Description**: The test cases for empty and blank input validation currently use the actual empty/blank values as error messages. The TODO comments suggest these should be changed to a more descriptive "[empty/blank]" format for better error reporting and consistency.

**Prompt**: Update the error message handling for empty and blank input validation in the `SdkmanCliDownloadServiceImpl` class. When validating version and platform parameters, if the input is empty (zero length) or blank (only whitespace), the error message should be "[empty/blank]" instead of the actual empty/blank value. Update the corresponding test expectations in `SdkmanCliDownloadServiceSpec.kt` to expect "[empty/blank]" as the error message for both `InvalidVersion` and `InvalidPlatform` errors when the input is empty or blank.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/application/service/SdkmanCliDownloadService.kt`
- `src/test/kotlin/io/sdkman/broker/application/service/SdkmanCliDownloadServiceSpec.kt`

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
