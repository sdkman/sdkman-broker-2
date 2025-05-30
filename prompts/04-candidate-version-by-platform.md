# Download Candidate Version by Platform Feature

Generate the main download endpoint that implements the **core functionality** of the SDKMAN Broker service. This endpoint handles resolving and redirecting download requests for SDK binaries via HTTP 302 redirects.

The feature should implement:

* An acceptance test that calls the download endpoint with various scenarios
* An HTTP handler that accepts download requests at `/download/{candidate}/{version}/{platform}`
* Delegates to the `VersionRepo` to fetch version records from the `versions` collection
* Implements platform resolution strategy with UNIVERSAL fallback
* Issues HTTP 302 redirects with appropriate headers
* Handles all error cases with proper HTTP status codes

## Requirements

The endpoint must implement exactly what is described in the @legacy_broker_service.md file under the `/download/{candidate}/{version}/{platform}` section, including:

* **Platform validation**: Validate platform parameter against supported identifiers
* **Version lookup**: Find exact candidate/version/platform match in database
* **UNIVERSAL fallback**: Use UNIVERSAL platform as fallback when specific platform not found
* **Checksum headers**: Include all available checksums as `X-Sdkman-Checksum-<ALGO>` headers
* **Archive type header**: Include `X-Sdkman-ArchiveType` header based on URL analysis
* **Error handling**: Return 400 for invalid platforms, 404 for not found

## Domain Model

Implement the `Version` domain entity as specified:

```kotlin
data class Version(
    val candidate: String,
    val version: String, 
    val platform: String,
    val url: String,
    val vendor: String? = null,
    val visible: Boolean? = true,
    val checksums: Map<String, String>? = null
)
```

## Platform Mapping

Implement the exact platform identifier mapping as specified:

| URL Parameter | Normalized Identifier | Description |
|---------------|----------------------|-------------|
| `linuxx64`    | `LinuxX64`           | 64-bit Linux |
| `linuxarm64`  | `LinuxARM64`         | ARM64 Linux |
| `linuxx32`    | `LinuxX32`           | 32-bit Linux |
| `darwinx64`   | `DarwinX64`          | 64-bit macOS (Intel) |
| `darwinarm64` | `DarwinARM64`        | ARM64 macOS (Apple Silicon) |
| `windowsx64`  | `WindowsX64`         | 64-bit Windows |
| `exotic`      | `Exotic`             | Fallback for unsupported platforms |

## Carefully observe all the following Cursor rules:

* follow the Kotlin style guide rules @kotlin-rules.md
* follow the Kotlin Testing rules @testing-rules.md
* follow the DDD guideline rules @ddd-rules.md
* follow the Hexagonal Architecture rules @hexagonal-architecture-rules.md

## Extra considerations

* Use the existing MongoDB test container setup
* Implement `VersionRepo` following the SPI pattern used for `ApplicationRepo`
* Create archive type detection utility for determining file types from URLs
* Implement checksum header generation with algorithm priority (SHA-256 highest)
* Use test data that matches the examples in @legacy_broker_service.md

## Test Data Setup

Use the following test data in your acceptance tests:

```kotlin
// Java version with platform-specific binary
Version(
    candidate = "java",
    version = "17.0.2-tem", 
    platform = "MAC_ARM64",
    url = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.2_8.tar.gz",
    vendor = "tem",
    visible = true,
    checksums = mapOf("sha256" to "abc123def456")
)

// Groovy version with UNIVERSAL binary
Version(
    candidate = "groovy",
    version = "4.0.0",
    platform = "UNIVERSAL", 
    url = "https://groovy.jfrog.io/artifactory/dist-release-local/groovy-zips/apache-groovy-binary-4.0.0.zip",
    checksums = mapOf("sha256" to "def456ghi789", "sha1" to "ghi789jkl012")
)
```

## Specification by Example

We won't be using Cucumber, but this sums up the expected behaviour to be tested in the acceptance test:

```gherkin
Feature: Download Candidate Version by Platform
	Scenario: Successful download with exact platform match
		Given a version record exists for "java/17.0.2-tem/MAC_ARM64"
		When a GET request is made for "/download/java/17.0.2-tem/darwinarm64"
		Then the service response status is 302
		And the Location header contains the binary URL
		And checksum headers are present
		And archive type header is present

	Scenario: Successful download with UNIVERSAL fallback
		Given a version record exists for "groovy/4.0.0/UNIVERSAL"
		And no platform-specific record exists for "groovy/4.0.0/LINUX_64"
		When a GET request is made for "/download/groovy/4.0.0/linuxx64"
		Then the service response status is 302
		And the Location header contains the UNIVERSAL binary URL

	Scenario: Invalid platform parameter
		When a GET request is made for "/download/java/17.0.2-tem/invalidplatform"
		Then the service response status is 400

	Scenario: Candidate not found
		Given no version record exists for "nonexistent"
		When a GET request is made for "/download/nonexistent/1.0.0/linuxx64"
		Then the service response status is 404

	Scenario: Version not found
		Given version records exist for "java" but not for version "99.0.0"
		When a GET request is made for "/download/java/99.0.0/linuxx64"
		Then the service response status is 404

	Scenario: Platform not found (no UNIVERSAL fallback)
		Given a version record exists for "java/17.0.2-tem/MAC_ARM64"
		And no UNIVERSAL record exists for "java/17.0.2-tem"
		When a GET request is made for "/download/java/17.0.2-tem/linuxx64"
		Then the service response status is 404

	Scenario: Multiple checksum algorithms
		Given a version record with multiple checksums (SHA-256, SHA-1, MD5)
		When a successful download request is made
		Then all checksum headers are present in priority order
		And SHA-256 appears before SHA-1
		And SHA-1 appears before MD5

	Scenario: Archive type detection
		Given version records with different URL extensions (.zip, .tar.gz, .tgz)
		When successful download requests are made
		Then the X-Sdkman-ArchiveType header reflects the correct type
		And .zip URLs return "zip"
		And .tar.gz URLs return "tar.gz"
		And .tgz URLs return "tar.gz"
```

## Implementation Notes

* **Platform Resolution Strategy**: Always try exact platform match first, then UNIVERSAL fallback
* **Checksum Priority**: SHA-256 > SHA-512 > SHA-384 > SHA-224 > SHA-1 > MD5
* **Archive Type Detection**: Parse URL extension to determine archive type
* **Error Responses**: Return only HTTP status codes, no response body for errors
* **Header Formatting**: Use exact header names as specified (e.g., `X-Sdkman-Checksum-SHA-256`)

## Acceptance Criteria

* All tests must pass when running `./gradlew check`
* All code is formatted properly with `./gradlew ktlintFormat`
* The endpoint handles all specified scenarios correctly
* Platform validation and mapping work as specified
* Checksum and archive type headers are generated correctly
* UNIVERSAL fallback strategy is implemented properly