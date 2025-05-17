# TODO List

## Build Script Improvements

- [x] **Improve Resource Directory Handling in Gradle**
  - Replace the hardcoded resource directory path in build.gradle.kts
  - Use Gradle's built-in methods to reference the resources directory
  - Ensure the version.properties file is properly generated in the right location
  - Related to: `//TODO: Use a better way to get the resources directory` in build.gradle.kts

## Functional Programming Improvements

- [x] **Implement Arrow's Option Type for Nullable Handling**
  - Replace nullable handling with Arrow's Option type in VersionService
  - Refactor the version properties loading to use functional constructs
  - Ensure proper error handling with Either and Option types
  - Related to: `//TODO: Use an Option instead of nullables!!!` in VersionService.kt

- [ ] **Use Arrow Extensions for Either Creation**
  - Replace manual Either.left() creation with Arrow extension functions
  - Implement the appropriate Arrow extension function to improve readability
  - Related to: `//TODO: Create the Either.left() using an arrow extension function` in VersionEndpointAcceptanceSpec.kt

## Code Cleanup

- [ ] **Inline Helper Functions in VersionRoutes**
  - Refactor to inline the handleVersionSuccess and handleVersionError functions
  - Improve code readability and reduce unnecessary abstraction
  - Ensure the functionality remains unchanged
  - Related to: `//TODO: Inline this function` (appears twice) in VersionRoutes.kt

## Testing Improvements

- [ ] **Improve Test Mocking Strategy**
  - Replace the anonymous object-based mock with a proper mockk implementation where possible
  - Create a clean stub for the VersionService in tests
  - Ensure tests remain clear and focused on their intent
  - Related to: `//TODO: Use a mockk or a stub instead of a real file` in VersionEndpointAcceptanceSpec.kt
