# HikariCP Connection Pool Starvation Fix

The PostgresHealthRepository is experiencing connection pool exhaustion due to database connections not being properly closed after health checks. The health check endpoint uses this repository periodically, causing database connections to accumulate until the HikariCP pool becomes fully occupied, leading to timeout exceptions and service degradation.

## Requirements

- Fix connection resource leak in PostgresHealthRepository.checkConnectivity() method
- Ensure database connections are properly closed using try-with-resources or Kotlin's `use` extension
- Maintain existing error handling and logging functionality
- Preserve the current Arrow Either-based return type and error handling patterns
- Verify fix resolves connection pool exhaustion under periodic health check load
- Ensure connections are returned to pool in both success and failure scenarios

## Rules

- rules/kotlin-rules.md

## Extra Considerations

- HikariCP connection pool configuration shows 10 max connections (total=10)
- Health checks run periodically from health endpoint, amplifying connection leak impact
- Connection timeout is set to 30 seconds (30001ms) indicating pool exhaustion
- Must handle connection closure in both success and failure scenarios
- Kotlin's `use` extension function provides automatic resource management for Closeable resources
- Original Arrow Either error handling patterns must be preserved
- Statement resources also need proper cleanup to prevent resource leaks
- Error occurs at PostgresHealthRepository.kt:21 suggesting the connection acquisition line

## Testing Considerations

- Integration tests should verify that pool starvation does not occur with repeated calls
- Verify existing unit tests continue to pass after resource management fixes
- Monitor HikariCP pool metrics to confirm idle connections are available after health checks

## Implementation Notes

- Use Kotlin's `use` extension function for automatic resource management of both Connection and Statement
- Maintain existing logging and error categorization logic (ConnectionFailure vs QueryFailure)
- Preserve current method signature and return types exactly
- Follow project's existing Kotlin coding standards and Arrow functional programming patterns
- Consider connection acquisition timing to minimize hold duration
- Ensure proper exception handling doesn't interfere with resource cleanup

## Specification by Example

**Error scenario showing pool exhaustion:**
```
15:33:36.447 [eventLoopGroupProxy-4-6] ERROR i.s.b.a.s.p.PostgresHealthRepository -
PostgreSQL health check failed: HikariPool-1 - Connection is not available,
request timed out after 30001ms (total=10, active=10, idle=0, waiting=0)
```

**Expected behavior after fix:**
```
HikariCP pool metrics should show: (total=10, active=0-2, idle=8-10, waiting=0)
```

## Verification

- [ ] PostgresHealthRepository.checkConnectivity() uses proper resource management with `use` blocks
- [ ] Database connections are automatically closed in success scenarios
- [ ] Database connections are automatically closed in failure scenarios
- [ ] Statement and ResultSet resources are also properly managed
- [ ] Existing error handling and logging functionality preserved
- [ ] Arrow Either return types and error categorization unchanged
- [ ] All existing unit tests pass without modification
- [ ] Integration tests verify connection pool stability under repeated health checks
- [ ] Health endpoint no longer causes connection pool exhaustion after sustained load
- [ ] HikariCP pool metrics show connections being returned (idle > 0) after health checks
- [ ] No regression in health check response times or functionality
