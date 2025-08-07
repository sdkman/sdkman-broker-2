# SDKMAN Bash CLI Download Handler

*Implement the SDKMAN Bash CLI download endpoint that redirects to GitHub releases for both stable and beta channels. This endpoint is used by SDKMAN to download its own SDK binary, composing URLs that point to GitHub releases and returning them as Location headers with 302 responses.*

## Requirements

- Implement `GET /download/sdkman/{command}/{version}/{platform}` endpoint
- Return 302 Found redirects with Location header pointing to GitHub releases
- Support both stable and beta version URL construction patterns
- Return appropriate HTTP status codes (302, 400, 404)
- Set X-Sdkman-ArchiveType header to "zip"
- Handle validation of command, version, and platform parameters
- No audit functionality required (will be added later as separate feature)

## Rules

- rules/ddd-rules.md
- rules/hexagonal-architecture-rules.md
- rules/kotlin-rules.md
- rules/kotest-rules.md

## Domain

No domain changes required apart from the service port.

## Extra Considerations

- Platform parameter should be validated but not used in URL construction (SDKMAN CLI is platform-agnostic)
- Platform parameter will be used for auditing in later feature
- Beta versions consist of the prefix "latest+" followed by the abbreviated commit hash
- Command parameter should be validated but not impact URL construction
- Command parameter must be one of `install` or `selfupdate` and will be used for audit purposes
- Must follow hexagonal architecture patterns with proper handler/use case separation
- Use Arrow Option types instead of nullable types
- URLs always follow the following pattern:
  - Stable: https://github.com/sdkman/sdkman-cli/releases/download/5.12.0/sdkman-cli-5.12.0.zip
  - Beta: https://github.com/sdkman/sdkman-cli/releases/download/latest/sdkman-cli-latest+b8d230b.zip

## Testing Considerations

- Unit tests for URL construction logic with stable and beta versions
- Acceptance tests verifying complete request/response flow
- Acceptance tests verify HTTP response codes and headers
- Parameter validation tested for invalid commands, empty versions, invalid platforms
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

## Specification by Example

```http
GET /download/sdkman/install/5.19.0/linuxx64
→ 302 Found
→ Location: https://github.com/sdkman/sdkman-cli/releases/download/5.19.0/sdkman-cli-5.19.0.zip
→ X-Sdkman-ArchiveType: zip

GET /download/sdkman/selfupdate/latest+b8d230b/darwinarm64
→ 302 Found
→ Location: https://github.com/sdkman/sdkman-cli/releases/download/latest/sdkman-cli-latest+b8d230b.zip
→ X-Sdkman-ArchiveType: zip

GET /download/sdkman/invalid/5.19.0/linuxx64
→ 400 Bad Request

GET /download/sdkman/install//linuxx64
→ 400 Bad Request

GET /download/sdkman/install/5.19.0/invalidplatform
→ 400 Bad Request
```

## Verification

- [ ] Handler responds to GET /download/sdkman/{command}/{version}/{platform}
- [ ] Returns 302 Found for valid requests
- [ ] Location header contains correct GitHub URL for stable versions
- [ ] Location header contains correct GitHub URL with 'latest' tag for beta versions
- [ ] X-Sdkman-ArchiveType header is set to "zip"
- [ ] Returns 400 Bad Request for invalid commands (not "install" or "selfupdate")
- [ ] Returns 400 Bad Request for empty/null version strings
- [ ] Returns 400 Bad Request for invalid platform identifiers
- [ ] URL construction works correctly for versions containing '+' character
- [ ] URL construction works correctly for standard semantic versions
- [ ] Handler integration works within the existing application structure
- [ ] All tests pass including unit, integration, and acceptance tests
