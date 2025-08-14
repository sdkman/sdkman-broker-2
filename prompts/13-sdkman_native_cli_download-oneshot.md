# SDKMAN Native CLI Download Handler

*Implement the SDKMAN Native CLI download endpoint that redirects to GitHub releases for both stable and beta channels. This endpoint is used by SDKMAN to download its own Native SDK binary, composing URLs that point to GitHub releases with platform-specific Rust target triples and returning them as Location headers with 302 responses.*

## Requirements

- Implement `GET /download/native/{command}/{version}/{platform}` endpoint
- Return 302 Found redirects with Location header pointing to GitHub releases
- Support platform-specific URL construction using Rust target triples
- Return appropriate HTTP status codes (302, 400, 404)
- Set X-Sdkman-ArchiveType header to "zip"
- Handle validation of command, version, and platform parameters
- Map platform codes to appropriate Rust target triples
- No audit functionality required (will be added later as separate feature)

## Rules

- rules/ddd-rules.md
- rules/hexagonal-architecture-rules.md
- rules/kotlin-rules.md
- rules/kotest-rules.md

## Domain

No domain changes required apart from the service port.

## Extra Considerations

- Platform parameter must be validated and mapped to Rust target triples for URL construction
- Platform parameter will be used for auditing in later feature
- Command parameter should be validated but not impact URL construction
- Command parameter must be one of `install` or `selfupdate` and will be used for audit purposes
- Must follow hexagonal architecture patterns with proper handler/use case separation
- Use Arrow Option types instead of nullable types
- URLs always follow the pattern: `https://github.com/sdkman/sdkman-cli-native/releases/download/v{versisdkman-on}/cli-native-{version}-{platform-triple}.zip`
- Platform mapping must handle all supported platforms: linuxx64, linuxarm64, linuxx32, darwinx64, darwinarm64, windowsx64
- No beta version handling needed (native CLI doesn't use beta versioning like bash CLI)

## Testing Considerations

- Unit tests for URL construction logic with target triple mapping
- Acceptance tests verifying complete request/response flow
- Acceptance tests verify HTTP response codes and headers
- Parameter validation tested for invalid commands, empty versions, invalid platforms
- Platform mapping validation for all supported platform codes
- Make `TestConfig` more flexible by providing default parameters for any test services in `configureAppForTesting`.
- Default parameters will allow us to call `configureAppForTesting()` without parameters in most tests
- Update all tests to make use of `configureAppForTesting()` where possible
- Only pass in mock/test service overrides if needed for a test

## Implementation Notes

- Follow existing handler patterns in the codebase
- Use proper HTTP status codes and response headers
- Implement proper error handling with appropriate status codes
- Use dependency injection for any external dependencies
- Maintain stateless handler design
- target triple mapping should be consistent with legacy specification
- Introduce a `TargetTriple` enum to represent the Rust target triples
- Use a new `Command` enum for available commands (`install`, `selfupdate`)

## Specification by Example

```http
GET /download/native/install/0.7.4/linuxx64
→ 302 Found
→ Location: https://github.com/sdkman/sdkman-cli-native/releases/download/v0.7.4/sdkman-cli-native-0.7.4-x86_64-unknown-linux-gnu.zip
→ X-Sdkman-ArchiveType: zip

GET /download/native/selfupdate/0.7.4/darwinarm64
→ 302 Found
→ Location: https://github.com/sdkman/sdkman-cli-native/releases/download/v0.7.4/sdkman-cli-native-0.7.4-aarch64-apple-darwin.zip
→ X-Sdkman-ArchiveType: zip

GET /download/native/install/0.8.0/windowsx64
→ 302 Found
→ Location: https://github.com/sdkman/sdkman-cli-native/releases/download/v0.8.0/sdkman-cli-native-0.8.0-x86_64-pc-windows-msvc.zip
→ X-Sdkman-ArchiveType: zip

GET /download/native/invalid/0.7.4/linuxx64
→ 400 Bad Request

GET /download/native/install//linuxx64
→ 400 Bad Request

GET /download/native/install/0.7.4/invalidplatform
→ 400 Bad Request

GET /download/native/install/0.7.4/exotic
→ 400 Bad Request
```

## Target triple Mapping

The following platform codes must be mapped to their corresponding Rust target triples:

| Platform Code | Rust Target Triple          |
|--------------|-----------------------------|
| `linuxx64`   | `x86_64-unknown-linux-gnu`  |
| `linuxarm64` | `aarch64-unknown-linux-gnu` |
| `linuxx32`   | `i686-unknown-linux-gnu`    |
| `darwinx64`  | `x86_64-apple-darwin`       |
| `darwinarm64`| `aarch64-apple-darwin`      |
| `windowsx64` | `x86_64-pc-windows-msvc`    |

Note: The `exotic` platform is not supported for native CLI downloads and should return 400 Bad Request.

## Verification

- [ ] Handler responds to GET /download/native/{command}/{version}/{platform}
- [ ] Returns 302 Found for valid requests
- [ ] Location header contains correct GitHub URL with proper target triple
- [ ] X-Sdkman-ArchiveType header is set to "zip"
- [ ] Returns 400 Bad Request for invalid commands (not "install" or "selfupdate")
- [ ] Returns 400 Bad Request for empty/null version strings
- [ ] Returns 400 Bad Request for invalid platform identifiers including "exotic"
- [ ] Returns 400 Bad Request for unsupported platform codes
- [ ] URL construction works correctly with target triple mapping
- [ ] All supported platform codes (linuxx64, linuxarm64, linuxx32, darwinx64, darwinarm64, windowsx64) map correctly
- [ ] Handler integration works within the existing application structure
- [ ] All tests pass including unit, integration, and acceptance tests
