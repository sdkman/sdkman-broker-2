# SDKMAN Broker Service Specification

## Purpose

The SDKMAN Broker service is responsible for resolving and redirecting download requests for SDK binaries (both SDKMAN's own CLI tools and third-party SDKs) via a unified HTTP API. It acts as a central hub that:

1. Determines the appropriate download location for requested software versions
2. Issues HTTP 302 redirects to the actual binary locations
3. Adds appropriate checksum headers for download verification
4. Records audit log entries for all successful downloads
5. Provides metadata about current CLI versions
6. Serves health and version information

The service serves as a critical component in the SDKMAN ecosystem, enabling the CLI tools to locate and download the correct artifacts without having to hardcode URLs or manage binary locations directly.

## Domain Model

The SDKMAN Broker's domain model consists of three primary entities:

### Version

Represents a specific version of a candidate SDK with platform-specific details:

```
Version {
  candidate: String          // Candidate identifier (e.g., "java", "groovy")
  version: String            // Version string (e.g., "17.0.2", "4.0.0")
  platform: String           // Platform identifier (e.g., "LINUX_64", "MAC_OSX", "UNIVERSAL")
  url: String                // Binary download URL
  vendor: String?            // Optional vendor information (e.g., "tem", "open", "grl")
  visible: Boolean?          // Whether this version is visible to users (default true)
  checksums: Map<String, String>? // Optional map of checksums with algorithm as keys
}
```

### App

Single document containing CLI version information:

```
App {
  alive: String                     // Status indicator, always "OK"
  stableCliVersion: String          // Current stable version of the SDKMAN Bash CLI
  betaCliVersion: String            // Current beta version of the SDKMAN Bash CLI 
  stableNativeCliVersion: String    // Current stable version of the SDKMAN Native CLI
  betaNativeCliVersion: String      // Current beta version of the SDKMAN Native CLI
}
```

### AuditEntry

Records download events:

```
AuditEntry {
  command: String             // Command name (typically "install" or "selfupdate")
  candidate: String           // Candidate name
  version: String             // Version downloaded
  host: String                // IP address of requester (from X-Real-IP header)
  agent: String               // User-Agent of requester
  platform: String            // Normalized platform identifier
  dist: String                // Distribution type that was served
  timestamp: Long             // Time of download in epoch milliseconds
}
```

## MongoDB Collections

The broker interacts with three MongoDB collections that directly correspond to the domain model:

### `versions` Collection

Stores all candidate versions with their platform-specific details:

```js
{
  "_id": ObjectId("..."),
  "candidate": "java", 
  "version": "17.0.2-tem",
  "platform": "MAC_ARM64",
  "url": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.2_8.tar.gz",
  "vendor": "tem",
  "visible": true,
  "checksums": {
    "sha256": "..."
  }
}
```

Example of a universal distribution:
```js
{
  "_id": ObjectId("..."),
  "candidate": "groovy",
  "version": "4.0.0",
  "platform": "UNIVERSAL",
  "url": "https://groovy.jfrog.io/artifactory/dist-release-local/groovy-zips/apache-groovy-binary-4.0.0.zip",
  "checksums": {
    "sha256": "..."
  }
}
```

### `application` Collection

Contains a single document with CLI version information:

```js
{
  "_id": ObjectId("56fe8926bab4a07d6edea175"),
  "alive": "OK",
  "stableCliVersion": "5.19.0",
  "betaCliVersion": "latest+b8d230b",
  "stableNativeCliVersion": "0.7.4",
  "betaNativeCliVersion": "0.7.4"
}
```

### `audit` Collection

Records all successful download requests:

```js
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

## Platform Identifiers

The broker recognizes the following platform identifiers in requests:

| URL Path Parameter | Normalized Identifier | Description |
|-------------------|----------------------|-------------|
| `linuxx64`        | `LinuxX64`           | 64-bit Linux |
| `linuxarm64`      | `LinuxARM64`         | ARM64 Linux |
| `linuxx32`        | `LinuxX32`           | 32-bit Linux |
| `darwinx64`       | `DarwinX64`          | 64-bit macOS (Intel) |
| `darwinarm64`     | `DarwinARM64`        | ARM64 macOS (Apple Silicon) |
| `windowsx64`      | `WindowsX64`         | 64-bit Windows |
| `exotic`          | `Exotic`             | Fallback for unsupported platforms |

For the native CLI, these platform codes are mapped to Rust platform triples:

| Platform Code | Rust Platform Triple |
|--------------|---------------------|
| `linuxx64`   | `x86_64-unknown-linux-gnu` |
| `linuxarm64` | `aarch64-unknown-linux-gnu` |
| `linuxx32`   | `i686-unknown-linux-gnu` |
| `darwinx64`  | `x86_64-apple-darwin` |
| `darwinarm64`| `aarch64-apple-darwin` |
| `windowsx64` | `x86_64-pc-windows-msvc` |

## Handlers

### GET `/health`

Returns the broker's health status.

- **Status Codes**:
  - `200 OK`: Service is healthy

- **Response Headers**:
  - `Content-Type: application/json`

- **Response Body**:
```json
{
  "status": "UP"
}
```

### GET `/version`

Returns the broker service version.

- **Status Codes**:
  - `200 OK`: Version retrieved successfully

- **Response Headers**:
  - `Content-Type: text/plain`

- **Response Body**:
  Plain text banner with version information

### GET `/download/{candidate}/{version}/{platform}`

Main endpoint for all candidate downloads including third-party SDKs.

- **Parameters**:
  - `candidate`: Candidate identifier (e.g., "java", "kotlin")
  - `version`: Version string (e.g., "17.0.2-tem", "1.5.31")
  - `platform`: Platform identifier (e.g., "linuxx64", "darwinarm64")

- **Status Codes**:
  - `302 Found`: Successful resolution with redirect
  - `400 Bad Request`: Invalid platform provided
  - `404 Not Found`: Candidate, version, or platform-specific binary not found

- **Response Headers (on 302)**:
  - `Location`: URL to the binary
  - `X-Sdkman-Checksum-<ALGO>`: Checksum values if available (e.g., `X-Sdkman-Checksum-SHA-256`)
  - `X-Sdkman-ArchiveType`: Archive format (e.g., "zip", "tar.gz")

- **Audit Entry**: Created on successful resolution (status 302)

- **Behavior**:
  1. Validates the platform parameter
  2. Looks up the candidate
  3. Looks up the specified version for the candidate
  4. Attempts to find a matching platform-specific binary
  5. If not found, attempts to use a UNIVERSAL binary as fallback
  6. If no suitable binary exists, returns 404
  7. If found, sets checksum and archive type headers
  8. Issues a 302 redirect to the binary URL
  9. Records an audit entry

### GET `/download/sdkman/{command}/{version}/{platform}`

Downloads the SDKMAN Bash CLI binary.

- **Parameters**:
  - `command`: Either "install" or "selfupdate"
  - `version`: CLI version string (e.g., "5.19.0" or "latest+b8d230b")
  - `platform`: Platform identifier (same as above)

- **Status Codes**:
  - `302 Found`: Successful resolution with redirect
  - `400 Bad Request`: Invalid parameters
  - `404 Not Found`: Version not found

- **Response Headers (on 302)**:
  - `Location`: URL to the CLI binary
  - `X-Sdkman-ArchiveType`: Always "zip"

- **Audit Entry**: Created with `command`, `candidate = "sdkman"`, and version/platform

- **Behavior**:
  1. Validates the version and platform
  2. Constructs a GitHub release URL for the CLI ZIP
  3. For stable versions: `https://github.com/sdkman/sdkman-cli/releases/download/{version}/sdkman-cli-{version}.zip`
  4. For beta versions: `https://github.com/sdkman/sdkman-cli/releases/download/latest/sdkman-cli-{version}.zip`
  5. Issues a 302 redirect to the constructed URL
  6. Records an audit entry

### GET `/download/native/{command}/{version}/{platform}`

Downloads the SDKMAN Native CLI binary.

- **Parameters**:
  - `command`: Either "install" or "selfupdate"
  - `version`: Native CLI version string (e.g., "0.7.4")
  - `platform`: Platform identifier (same as above)

- **Status Codes**:
  - `302 Found`: Successful resolution with redirect
  - `400 Bad Request`: Invalid parameters
  - `404 Not Found`: Version or platform-specific native binary not found

- **Response Headers (on 302)**:
  - `Location`: URL to the native CLI binary
  - `X-Sdkman-ArchiveType`: Always "zip"

- **Audit Entry**: Created with `command`, `candidate = "native"`, and version/platform

- **Behavior**:
  1. Validates the version and platform
  2. Maps the platform code to a Rust platform triple
  3. Constructs a GitHub release URL for the native CLI ZIP: 
     `https://github.com/sdkman/sdkman-cli-native/releases/download/{version}/cli-native-{version}-{platform-triple}.zip`
  4. Issues a 302 redirect to the constructed URL
  5. Records an audit entry

### GET `/download/sdkman/version/{channel}`

Legacy endpoint to get the current SDKMAN Bash CLI version.

- **Parameters**:
  - `channel`: Either "stable" or "beta"

- **Status Codes**:
  - `200 OK`: Version retrieved successfully
  - `404 Not Found`: Invalid channel

- **Response Headers**:
  - `Content-Type: text/plain`

- **Response Body**:
  Plain text containing only the version string (e.g., "5.19.0" or "latest+b8d230b")

- **Behavior**:
  1. Retrieves the App document from the database
  2. Returns the appropriate CLI version based on the channel:
     - For "stable": `stableCliVersion`
     - For "beta": `betaCliVersion`

### GET `/version/sdkman/{type}/{channel}`

Gets the current CLI version information.

- **Parameters**:
  - `type`: Either "bash" (shell-based) or "native" (Rust-based)
  - `channel`: Either "stable" or "beta"

- **Status Codes**:
  - `200 OK`: Version retrieved successfully
  - `404 Not Found`: Invalid type or channel

- **Response Headers**:
  - `Content-Type: text/plain`

- **Response Body**:
  Plain text containing only the version string

- **Behavior**:
  1. Retrieves the App document from the database
  2. Returns the appropriate CLI version based on type and channel:
     - For "bash"/"stable": `stableCliVersion`
     - For "bash"/"beta": `betaCliVersion`
     - For "native"/"stable": `stableNativeCliVersion`
     - For "native"/"beta": `betaNativeCliVersion`

## Redirect Strategy

The broker uses a 302 Found redirect strategy for all binary downloads. The redirect URL is constructed differently based on the type of artifact:

### Third-Party Candidates

For regular SDKs (e.g., Java, Kotlin, Groovy):
- The URL is retrieved directly from the database record for the specific version/platform
- No URL construction is performed; the stored URL is used verbatim

### SDKMAN Bash CLI

For SDKMAN's shell-based CLI:
- The URL is constructed using a template format
- For stable versions: `https://github.com/sdkman/sdkman-cli/releases/download/{version}/sdkman-cli-{version}.zip`
- For beta versions (containing "+"): `https://github.com/sdkman/sdkman-cli/releases/download/latest/sdkman-cli-{version}.zip`

### SDKMAN Native CLI

For SDKMAN's Rust-based CLI:
- The URL is constructed using a template that includes the platform-specific Rust triple
- Format: `https://github.com/sdkman/sdkman-cli-native/releases/download/{version}/cli-native-{version}-{platform-triple}.zip`
- The platform triple is mapped from the normalized platform ID:
  - `LinuxX64` → `x86_64-unknown-linux-gnu`
  - `LinuxARM64` → `aarch64-unknown-linux-gnu` 
  - `DarwinX64` → `x86_64-apple-darwin`
  - etc.

## URL Composition

### Candidate SDK URLs

Stored directly in the database and used as-is. Examples:
- Java: `https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.2_8.tar.gz`
- Groovy: `https://groovy.jfrog.io/artifactory/dist-release-local/groovy-zips/apache-groovy-binary-4.0.0.zip`

### SDKMAN Bash CLI URLs

Constructed using the following template logic:
```java
String baseUrl = "https://github.com/sdkman/sdkman-cli/releases/download";
String artifactName = "sdkman-cli-" + version + ".zip";

// If version contains '+' (beta), use 'latest' tag
if (version.contains("+")) {
    return baseUrl + "/latest/" + artifactName;
} else {
    return baseUrl + "/" + version + "/" + artifactName;
}
```

Examples:
- Stable (5.19.0): `https://github.com/sdkman/sdkman-cli/releases/download/5.19.0/sdkman-cli-5.19.0.zip`
- Beta (latest+b8d230b): `https://github.com/sdkman/sdkman-cli/releases/download/latest/sdkman-cli-latest+b8d230b.zip`

### SDKMAN Native CLI URLs

Constructed using platform-specific Rust triples:
```java
String baseUrl = "https://github.com/sdkman/sdkman-cli-native/releases/download";
String platformTriple = mapPlatformToTriple(platform); // Maps platform code to Rust triple
String artifactName = "cli-native-" + version + "-" + platformTriple + ".zip";
return baseUrl + "/" + version + "/" + artifactName;
```

Examples:
- Linux x64 (0.7.4): `https://github.com/sdkman/sdkman-cli-native/releases/download/0.7.4/cli-native-0.7.4-x86_64-unknown-linux-gnu.zip`
- macOS ARM64 (0.7.4): `https://github.com/sdkman/sdkman-cli-native/releases/download/0.7.4/cli-native-0.7.4-aarch64-apple-darwin.zip`

## Platform Resolution

When a download request is received, the broker follows this resolution strategy:

1. Attempt to find an exact platform match for the requested candidate/version
2. If not found, check if a UNIVERSAL binary exists for the candidate/version
3. If a UNIVERSAL binary is found, use it as a fallback for any platform
4. If no suitable binary is found, return 404 Not Found

This platform fallback logic applies to all downloads except the native CLI, which requires exact platform matches (no UNIVERSAL fallback exists for native executables).

## Audit Logging

The broker maintains a comprehensive audit log of all successful downloads:

### Audit Entry Fields

- `_id`: Automatically generated MongoDB ObjectId
- `command`: The command being executed (usually "install" or "selfupdate")
- `candidate`: The candidate being downloaded
- `version`: The specific version downloaded
- `host`: Client IP address from the X-Real-IP header
- `agent`: User-Agent string from the request
- `platform`: The normalized platform ID (e.g., "LinuxX64", "DarwinARM64")
- `dist`: The distribution type that was served (e.g., "UNIVERSAL", "MAC_ARM64")
- `timestamp`: Unix epoch time in milliseconds

### Audit Process

1. After successfully resolving a download request (just before issuing the 302 redirect)
2. The appropriate handler constructs an AuditEntry with all required fields
3. The AuditRepo asynchronously inserts the entry into the "audit" collection
4. Audit entries are not created for failed requests (400/404 responses)

### Audit Implementation

```java
// In various handler classes
private void audit(RequestDetails details, String platform, String dist) {
    auditRepo.insertAudit(
        AuditEntry.of(
            command, details.getCandidate(), details.getVersion(), details.getHost(),
            details.getAgent(), platform, dist));
}

// In AuditRepo.java
public void insertAudit(AuditEntry auditEntry) {
    Blocking.exec(() -> {
        mongoProvider.database()
            .getCollection("audit", BasicDBObject.class)
            .insertOne(
                new BasicDBObject()
                    .append("_id", ObjectId.get())
                    .append("command", auditEntry.getCommand())
                    .append("candidate", auditEntry.getCandidate())
                    .append("version", auditEntry.getVersion())
                    .append("host", auditEntry.getHost())
                    .append("agent", auditEntry.getAgent())
                    .append("platform", auditEntry.getPlatform())
                    .append("dist", auditEntry.getDist())
                    .append("timestamp", auditEntry.getTimestamp()));
        logger.debug("Logged: {}", auditEntry);
    });
}
```

## Checksums

The broker supports multiple checksum algorithms for binary verification:

### Supported Checksum Algorithms

- MD5 (lowest priority)
- SHA-1
- SHA-224
- SHA-256 (highest priority)
- SHA-384
- SHA-512

### Checksum Storage

Checksums are stored in the `versions` collection as a nested map:

```js
{
  // other version fields...
  "checksums": {
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "sha1": "da39a3ee5e6b4b0d3255bfef95601890afd80709"
  }
}
```

### Checksum Headers

When a download is resolved, all available checksums are included as headers in the redirect response, with algorithm priority determining the order:

```
X-Sdkman-Checksum-SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
X-Sdkman-Checksum-SHA-1: da39a3ee5e6b4b0d3255bfef95601890afd80709
```

The SDKMAN CLI uses these headers to verify the integrity of downloaded artifacts.

## Archive Types

The broker provides archive type information through a dedicated header:

### Archive Type Header

```
X-Sdkman-ArchiveType: zip
```

Possible values:
- `zip` - ZIP archive
- `tar.gz` - Gzipped TAR archive
- Other archive types as needed

### Archive Type Detection

The archive type is determined by analyzing the binary URL:

```java
public static ArchiveType fromUrl(String url) {
    if (url.endsWith(".zip")) {
        return ArchiveType.ZIP;
    } else if (url.endsWith(".tar.gz") || url.endsWith(".tgz")) {
        return ArchiveType.TGZ;
    } else if (url.endsWith(".bz2")) {
        return ArchiveType.TBZ2;
    } else if (url.endsWith(".xz")) {
        return ArchiveType.XZ;
    } else {
        // Default fallback
        return ArchiveType.ZIP;
    }
}
```

This information helps the SDKMAN CLI determine the appropriate extraction method for the downloaded artifact.

## Error Handling

The broker handles errors consistently across all endpoints:

### 400 Bad Request

Returned when:
- Invalid platform codes are provided
- Required parameters are missing
- URL format is incorrect

### 404 Not Found

Returned when:
- A requested candidate doesn't exist
- A requested version doesn't exist for a candidate
- A platform-specific binary doesn't exist and no UNIVERSAL fallback is available
- An invalid type/channel is requested for version endpoints

Error responses contain no body and set only the standard HTTP status code.

## Persistence Layer Implementation

The broker uses MongoDB as its persistence layer, accessed through synchronous and asynchronous operations:

### Repository Pattern

Each domain entity has a corresponding repository:
- `VersionRepo`: Handles queries for candidate versions and binaries
- `AppRepo`: Retrieves CLI version information
- `AuditRepo`: Records download events

### Database Operations

- Read operations use `find()` with appropriate filters
- Write operations for audit use `insertOne()` via `Blocking.exec()`
- All operations are handled using the MongoDB Java driver
- No explicit transaction handling for single-document operations
- Connection pool and database connections are managed through a `MongoProvider`

### Asynchronous Access

- Non-blocking operations are used where possible to maximize throughput
- Audit logging is fully asynchronous to avoid impacting download performance 