---
description: Functional Kotlin guidelines with Arrow for AI-generated code
globs: ["**/*.kt"]
alwaysApply: true
---
# Kotlin Functional Programming Style Guide

*Functional Kotlin guidelines with Arrow for AI-generated code.*

## 1. Core Principles

- **Immutability First**: Default to `val` and immutable collections
- **Expression Over Statement**: Favor expressions that return values over statements with side effects
- **Type Safety**: Make impossible states unrepresentable
- **Pure Functions**: Functions with no side effects, same output for same input
- **Explicit Error Handling**: Use Arrow's `Either`, `Option`, `Validated` instead of nullables and exceptions
- **Composition**: Build complex logic by composing smaller, focused functions
- **No Exception Throwing**: Use Arrow's `Either.catch {}` for exception handling

**FAVOR SIMPLE SOLUTIONS OVER COMPLEX ONES!**

## 2. Project & Code Organization

### File Structure

One file can contain multiple related classes/functions (cohesive unit)

### Naming Conventions
- **Packages**: lowercase (`io.sdkman.state.version`)
- **Classes/Interfaces**: PascalCase (`VersionRepository`)
- **Functions**: camelCase (`findById()`)
- **Properties**: camelCase (`username`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRY_ATTEMPTS`)

## 3. Type System & Error Handling

### Algebraic Data Types
```kotlin
sealed class Result<out A> {
    data class Success<A>(val value: A) : Result<A>()
    data class Failure(val error: DomainError) : Result<Nothing>()
}
```

### Avoiding Nulls(**IMPORTANT**)
Use Arrow's `Option<A>` instead of nullable types:
```kotlin
// Instead of: fun findUser(id: String): User?
fun findUser(id: String): Option<User>
```

### Error Handling
Use `Either<E, A>` for operations that can fail:
```kotlin
// NEVER do this:
try {
    parseData(input)
} catch (e: Exception) {
    handleError(e)
}

// ALWAYS do this:
Either.catch {
    parseData(input)
}.mapLeft { e -> DomainError.ParseError(e) }
```

### Smart Constructors
```kotlin
data class Email private constructor(val value: String) {
    companion object {
        fun of(value: String): Either<ValidationError, Email> =
            if (value.matches(EMAIL_REGEX)) Email(value).right()
            else ValidationError("Invalid email format").left()
    }
}
```

## 4. Functions and Collections

### Function Design
- Keep functions small and focused
- Prefer pure functions without side effects
- Specify explicit return types for public functions
- Prefer expression bodies for simple functions:
```kotlin
fun transform(input: String): Int = input.length
```

### Immutable Collections
- Use immutable collection interfaces (`List`, `Set`, `Map`)
- Create new collections instead of modifying existing ones:
```kotlin
// Instead of:
val items = mutableListOf<Item>()
items.add(newItem)

// Prefer:
val updatedItems = items + newItem
```

### Collection Operations
```kotlin
// Instead of:
val result = mutableListOf<String>()
for (item in items) {
    if (item.isValid()) {
        result.add(item.name.uppercase())
    }
}

// Prefer:
val result = items
    .filter { it.isValid() }
    .map { it.name.uppercase() }
```

## 5. Arrow for Functional Programming

### Option

Convert nullable types to `Option` **as soon as possible** to avoid null checks throughout the codebase:

```kotlin
fun findConfig(key: String): Option<Config> =
    configs[key].toOption()
```

### Either
```kotlin
fun validateInput(input: String): Either<ValidationError, Input> =
    when {
        input.isEmpty() -> ValidationError("Input cannot be empty").left()
        else -> Input(input).right()
    }
```

### Validated
For accumulating multiple errors:
```kotlin
fun validateForm(name: String, email: String): ValidatedNel<ValidationError, Form> =
    ValidatedNel.applicative(Nel.semigroup<ValidationError>()).map(
        validateName(name).toValidatedNel(),
        validateEmail(email).toValidatedNel()
    ) { (validName, validEmail) ->
        Form(validName, validEmail)
    }.fix()
```

## 6. Domain Modeling

### Data Classes
```kotlin
data class MyDomain(
    val a: String,
    val b: String,
)
```

### Sealed Classes
```kotlin
sealed class MyPlatform {
    data object Universal : Platform()
    data class Linux(val arch: Architecture) : Platform()
}
```

### Value Types
```kotlin
@JvmInline
value class EntityId(val value: String)
```

## 7. Repository Pattern & Effect Management

```kotlin
interface VersionRepository {
    fun findById(id: VersionId): Either<RepositoryError, Option<Version>>
    fun save(version: Version): Either<RepositoryError, Version>
}

class MongoVersionRepository(private val db: MongoDatabase) : VersionRepository {
    override fun findById(id: VersionId): Either<RepositoryError, Option<Version>> =
        Either.catch {
            db.collection("versions")
                .find(Filters.eq("_id", id.value))
                .firstOrNull()
                .toOption()
                .map{ it.toVersion() }
        }.mapLeft { RepositoryError.DatabaseError(it) }
}
```

## 8. Self-Documenting Code

### Clear Naming Over Comments
```kotlin
// AVOID this:
/**
 * Processes the customer order by validating it and charging the payment
 * @param orderId The ID of the order to process
 * @return true if order was processed successfully
 */
fun process(orderId: String): Boolean

// PREFER this:
fun validateAndProcessCustomerOrder(orderId: String): Either<OrderProcessingError, ProcessedOrder>
```

### Naming Guidelines
- **Classes**: Nouns describing the entity
- **Methods**: Verb phrases describing the action
- **Variables**: Descriptive nouns indicating purpose
- **Boolean variables/functions**: Use prefixes like `is`, `has`, or `should`

### Functions as Documentation
```kotlin
// AVOID:
// Check if user is active and has admin privileges
if (user.status == "ACTIVE" && user.role == "ADMIN") { /* ... */ }

// PREFER:
fun isActiveAdministrator(user: User): Boolean =
    user.status == "ACTIVE" && user.role == "ADMIN"

if (isActiveAdministrator(user)) { /* ... */ }
```

## TL;DR

1. **Immutability**: Use `val`, immutable collections, and data classes
2. **Type Safety**: Use sealed classes and custom types to prevent invalid states
3. **Functional Style**: Expressions over statements, pure functions, function composition
4. **Error Handling**: Use Arrow's `Either`, `Option`, `Validated`
5. **Null Avoidance**: Convert nullable types to `Option<A>` as soon as possible
6. **Collections**: Use functional operators (`map`, `filter`, `fold`) instead of loops
7. **Self-Documenting Code**: Clear naming over comments; no JavaDoc
