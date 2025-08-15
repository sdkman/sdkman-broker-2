# TODO List

Consider the following rules during execution of the tasks:
- rules/git-rules.md
- rules/kotlin-rules.md
- rules/kotest-rules.md

## Task 1: Move DownloadInfo to Domain Layer

- [X] Relocate DownloadInfo class from application to domain layer

**Description**: The DownloadInfo class is currently located in the application service package but should be moved to the domain layer to follow Domain-Driven Design principles. This class represents core business data and should be part of the domain model.

**Prompt**: Move the `DownloadInfo` data class from `src/main/kotlin/io/sdkman/broker/application/service/DownloadInfo.kt` to the domain layer at `src/main/kotlin/io/sdkman/broker/domain/model/DownloadInfo.kt`. Update all import statements across the codebase to reference the new location. Ensure the class maintains its current structure and functionality while being properly placed in the domain layer according to hexagonal architecture principles.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/application/service/DownloadInfo.kt`
- `src/main/kotlin/io/sdkman/broker/application/service/CandidateDownloadService.kt`
- `src/main/kotlin/io/sdkman/broker/domain/model/DownloadInfo.kt` (new file)

## Task 2: Move CandidateDownloadService Interface to Domain Layer

- [X] Relocate interface from application to domain service package

**Description**: The CandidateDownloadService interface is currently located in the application service package but should be moved to the domain service package to properly separate domain contracts from application layer implementations.

**Prompt**: Move the `CandidateDownloadService` interface from `src/main/kotlin/io/sdkman/broker/application/service/CandidateDownloadService.kt` to `src/main/kotlin/io/sdkman/broker/domain/service/CandidateDownloadService.kt`. Keep the implementation class in the application layer but update its import to reference the interface from the domain layer. Update all other files that import this interface to use the new domain layer location.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/application/service/CandidateDownloadService.kt`
- `src/main/kotlin/io/sdkman/broker/domain/service/CandidateDownloadService.kt` (new file)

## Task 3: Replace NativeDownloadInfo with DownloadInfo

- [X] Remove NativeDownloadInfo and use DownloadInfo instead

**Description**: The NativeDownloadInfo data class duplicates functionality already provided by DownloadInfo. This violates the DRY principle and creates unnecessary code duplication. The services should be unified to use a single data transfer object.

**Prompt**: Remove the `NativeDownloadInfo` data class from `SdkmanNativeDownloadService.kt` and update the `SdkmanNativeDownloadService` interface to return `DownloadInfo` instead. Update all implementations and usages of this service to work with the unified `DownloadInfo` type. Ensure that any specific functionality of NativeDownloadInfo is preserved by adapting the DownloadInfo usage appropriately.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/domain/service/SdkmanNativeDownloadService.kt`

## Task 4: Replace SdkmanCliDownloadInfo with DownloadInfo

- [X] Remove SdkmanCliDownloadInfo and use DownloadInfo instead

**Description**: The SdkmanCliDownloadInfo data class duplicates functionality already provided by DownloadInfo. This violates the DRY principle and creates unnecessary code duplication. The services should be unified to use a single data transfer object.

**Prompt**: Remove the `SdkmanCliDownloadInfo` data class from `SdkmanCliDownloadService.kt` and update the `SdkmanCliDownloadService` interface to return `DownloadInfo` instead. Update all implementations and usages of this service to work with the unified `DownloadInfo` type. Ensure that any specific functionality of SdkmanCliDownloadInfo is preserved by adapting the DownloadInfo usage appropriately.

**Files affected**:
- `src/main/kotlin/io/sdkman/broker/domain/service/SdkmanCliDownloadService.kt`

## Task 5: Standardize Download Services Return Type

- [ ] Update all download services to use DownloadInfo as return type

**Description**: After completing the previous tasks, ensure all download services consistently use DownloadInfo as their return type. This provides a unified interface across all download service implementations and eliminates code duplication.

**Prompt**: Review all download service interfaces and implementations to ensure they consistently use `DownloadInfo` as the return type. Update any remaining services that may still be using custom data classes instead of the unified `DownloadInfo`. Verify that all service implementations properly construct and return DownloadInfo objects with the correct data.

**Files affected**:
- All files in `src/main/kotlin/io/sdkman/broker/domain/service/` that implement download services
- All files in `src/main/kotlin/io/sdkman/broker/adapter/` that use download services

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
