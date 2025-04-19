---
description: 
globs: 
alwaysApply: false
---
 # Kotlin Functional Programming Style Guide

*Cursor rules file – functional Kotlin guidelines with Arrow for AI-generated code.*

> **Intent**  
> Provide a blueprint for writing **clean, functional Kotlin code** that prioritizes immutability, type safety, and explicit error handling.  
> Embrace functional programming principles while maintaining readability and practicality.  
> Leverage Arrow for robust functional abstractions instead of imperative patterns.

---

## 1 Core Principles

| Principle | Description |
|-----------|-------------|
| **Immutability First** | Default to immutable data structures (`val`, immutable collections) |
| **Expression Over Statement** | Favor expressions that return values over statements with side effects |
| **Type Safety** | Use the type system to prevent errors; make impossible states unrepresentable |
| **Pure Functions** | Functions with no side effects that return the same output for the same input |
| **Explicit Error Handling** | Use Arrow's `Either`, `Option`, `Validated` instead of nulls or exceptions |
| **Composition** | Build complex logic by composing smaller, focused functions |


**Favour SIMPLE solutions of COMPLEX ones!!!**

---

## 2 Project Structure

### 2.1 Package Organization

Organize packages by feature or domain concept, not by technical layer:

```
io.sdkman.state
├── version/           # Version-related functionality
│   ├── model/         # Domain models for versions
│   ├── service/       # Services for version operations
│   └── routes/        # HTTP routes for version endpoints
├── repos/             # Repository interfaces and implementations
├── config/            # Application configuration
└── util/              # Cross-cutting utilities
```

### 2.2 File Structure

* One file can contain multiple related classes/functions (cohesive unit)
* Group by feature rather than technical concerns
* Acceptable pairings in a single file:
  * Data class + its Repository interface
  * Domain model + its associated services
  * Related pure functions that operate on the same data structure

---

## 3 Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| **Packages** | All lowercase, no underscores | `io.sdkman.state.version` |
| **Classes/Interfaces** | PascalCase, descriptive nouns | `VersionRepository`, `StateService` |
| **Functions** | camelCase, verb phrases | `findById()`, `transformVersion()` |
| **Properties** | camelCase, nouns | `username`, `creationDate` |
| **Constants** | UPPER_SNAKE_CASE | `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT` |
| **Type Parameters** | Single uppercase letter or descriptive name | `T`, `E`, `RequestType` |

---

## 4 Type System

### 4.1 Algebraic Data Types

Use sealed classes/interfaces to represent finite sets of possibilities:

```kotlin
sealed class Result<out A> {
    data class Success<A>(val value: A) : Result<A>()
    data class Failure(val error: DomainError) : Result<Nothing>()
}

sealed class DomainError {
    data class NotFound(val id: String) : DomainError()
    data class ValidationFailed(val reason: String) : DomainError()
    data class SystemError(val cause: Throwable) : DomainError()
}
```

### 4.2 Avoiding Nulls

* Never use nullable types (`String?`, `Int?`, etc.) in your domain model
* Represent optional values with Arrow's `Option<A>`:

```kotlin
// Instead of:
fun findUser(id: String): User? 

// Prefer:
fun findUser(id: String): Option<User>
```

### 4.3 Error Handling

* Use `Either<E, A>` for operations that can fail with typed errors
* Use `Validated<E, A>` when accumulating multiple errors
* Avoid throwing exceptions; make failures explicit in the return type:

```kotlin
fun validateAndParse(input: String): Either<ValidationError, ParsedData> =
    if (!isValid(input)) {
        ValidationError(input).left()
    } else {
        parse(input).right()
    }
```

### 4.4 Smart Constructors

Use factory functions to ensure invariants for domain objects:

```kotlin
data class Email private constructor(val value: String) {
    companion object {
        fun of(value: String): Either<ValidationError, Email> =
            if (value.matches(EMAIL_REGEX)) {
                Email(value).right()
            } else {
                ValidationError("Invalid email format").left()
            }
    }
}
```

---

## 5 Functions and Lambdas

### 5.1 Function Design

* Keep functions small and focused on a single responsibility
* Prefer pure functions that don't cause side effects
* Use default parameters instead of function overloading
* Always specify explicit return types for public functions and methods
* Prefer expression bodies over block bodies for simple functions:

```kotlin
// Prefer:
fun transform(input: String): Int = input.length

// Instead of:
fun transform(input: String): Int {
    return input.length
}
```

### 5.2 Higher-Order Functions

* Use higher-order functions from the standard library (`map`, `filter`, `fold`)
* Extract complex lambdas to named functions for readability

### 5.3 Function Composition

* Compose functions using extensions or operators
* Use Arrow's function composition utilities for complex chains

```kotlin
val validateAndProcess: (RawData) -> Either<AppError, ProcessedData> = 
    ::validate andThen ::process
```

---

## 6 Collections

### 6.1 Immutable Collections

* Always use immutable collection interfaces (`List`, `Set`, `Map`)
* Avoid mutable collection operations (`.add()`, `.remove()`, etc.)
* Create new collections instead of modifying existing ones:

```kotlin
// Instead of:
val items = mutableListOf<Item>()
items.add(newItem)

// Prefer:
val updatedItems = items + newItem
```

### 6.2 Collection Operations

* Prefer functional operations over imperative loops:

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

* Chain operations for complex transformations
* Extract complex lambdas to named functions

---

## 7 Error Handling with Arrow

### 7.1 Option

Use `Option<A>` for values that might be absent:

```kotlin
fun findConfig(key: String): Option<Config> =
    configs[key].toOption()

val result = findConfig("api.url")
    .map { it.value }
    .getOrElse { DEFAULT_URL }
```

### 7.2 Either

Use `Either<E, A>` for operations that might fail with a specific error:

```kotlin
fun validateInput(input: String): Either<ValidationError, Input> =
    when {
        input.isEmpty() -> ValidationError("Input cannot be empty").left()
        input.length < 3 -> ValidationError("Input too short").left()
        else -> Input(input).right()
    }
```

### 7.3 Validated

Use `Validated<E, A>` to accumulate multiple errors:

```kotlin
fun validateForm(name: String, email: String, age: Int): ValidatedNel<ValidationError, Form> =
    ValidatedNel.applicative(Nel.semigroup<ValidationError>()).map(
        validateName(name).toValidatedNel(),
        validateEmail(email).toValidatedNel(),
        validateAge(age).toValidatedNel()
    ) { (validName, validEmail, validAge) ->
        Form(validName, validEmail, validAge)
    }.fix()
```

### 7.4 IO

Use `IO<E, A>` for effectful operations (database access, HTTP calls, etc.):

```kotlin
fun fetchUser(id: String): IO<ApiError, User> =
    IO.effect {
        api.getUser(id) // This may throw exceptions
    }.mapError { e ->
        when (e) {
            is HttpException -> ApiError.HttpError(e.code)
            else -> ApiError.UnknownError(e)
        }
    }
```

---

## 8 Domain Modeling

### 8.1 Data Classes

* Use data classes for immutable value objects:

```kotlin
data class Version(
    val candidate: String,
    val version: String,
    val url: String,
    val platform: Platform,
    val vendor: String? = null
)
```

### 8.2 Sealed Classes

* Use sealed classes for sum types (entities with different variants):

```kotlin
sealed class Platform {
    object Universal : Platform()
    data class MacOS(val arch: Architecture) : Platform()
    data class Linux(val arch: Architecture) : Platform()
    data class Windows(val arch: Architecture) : Platform()
}

sealed class Architecture {
    object X64 : Architecture()
    object Arm64 : Architecture()
}
```

### 8.3 Value Types

* Create dedicated types for primitive values to improve type safety:

```kotlin
@JvmInline
value class CandidateId(val value: String)

@JvmInline
value class VersionString(val value: String)
```

---

## 9 Persistence and Effects

### 9.1 Repository Pattern

* Define repository interfaces in the domain layer:

```kotlin
interface VersionRepository {
    fun findById(id: VersionId): IO<RepositoryError, Option<Version>>
    fun findByCandidate(candidate: String): IO<RepositoryError, List<Version>>
    fun save(version: Version): IO<RepositoryError, Version>
}
```

### 9.2 Effect Management

* Use Arrow's `IO` for managing side effects:

```kotlin
class MongoVersionRepository(private val db: MongoDatabase) : VersionRepository {
    override fun findById(id: VersionId): IO<RepositoryError, Option<Version>> =
        IO.effect {
            db.collection("versions")
                .find(Filters.eq("_id", id.value))
                .firstOrNull()
                ?.toVersion()
        }.map { it.toOption() }
         .mapError { RepositoryError.DatabaseError(it) }
}
```

### 9.3 Transaction Handling

* Keep transactions explicit and at the service layer:

```kotlin
fun processOrder(order: Order): IO<OrderError, Receipt> =
    IO.fx {
        val validatedOrder = !validateOrder(order)
                              .mapLeft { OrderError.ValidationFailed(it) }
        val savedOrder = !orderRepository.save(validatedOrder)
                         .mapLeft { OrderError.PersistenceError(it) }
        val payment = !paymentService.processPayment(savedOrder)
                      .mapLeft { OrderError.PaymentFailed(it) }
        
        Receipt(savedOrder.id, payment.id, savedOrder.total)
    }
```

---

## 10 API Design

### 10.1 Function Signatures

* Make failures explicit in function signatures:

```kotlin
// Instead of:
@Throws(NotFoundException::class)
fun getUser(id: String): User

// Prefer:
fun getUser(id: String): Either<GetUserError, User>
```

### 10.2 Consistent Return Types

* Maintain consistent return types across similar operations:

```kotlin
interface CandidateService {
    fun findById(id: String): IO<ServiceError, Option<Candidate>>
    fun findAll(): IO<ServiceError, List<Candidate>>
    fun save(candidate: Candidate): IO<ServiceError, Candidate>
    fun delete(id: String): IO<ServiceError, Unit>
}
```

### 10.3 Parameterization Over Generics

* Parameterize interfaces to support different result types:

```kotlin
interface CrudRepository<ID, E, F> {
    fun findById(id: ID): IO<F, Option<E>>
    fun findAll(): IO<F, List<E>>
    fun save(entity: E): IO<F, E>
    fun delete(id: ID): IO<F, Unit>
}
```

---

## 11 Code Formatting and Style

### 11.1 Line Length and Wrapping

* Maximum line length: 120 characters
* Wrap long parameter lists, chained function calls

### 11.2 Indentation and Spacing

* Use 4 spaces for indentation (no tabs)
* Put spaces around operators
* No trailing whitespace

### 11.3 File Organization

* Order of declarations in files:
  1. Package statement
  2. Imports (alphabetical, no wildcards)
  3. Top-level declarations (classes, functions, properties)

### 11.4 EditorConfig Settings

```
root = true

[*]
charset = utf-8
end_of_line = lf
indent_size = 4
indent_style = space
insert_final_newline = true
max_line_length = 120
tab_width = 4
trim_trailing_whitespace = true

[*.{yml,yaml}]
indent_size = 2
```

---

### TL;DR

1. **Immutability**: Use `val`, immutable collections, and data classes.
2. **Type Safety**: Make illegal states unrepresentable with sealed classes and custom types.
3. **Functional Style**: Use expressions over statements, pure functions, and function composition.
4. **Error Handling**: Represent errors with Arrow's `Either`, `Option`, `Validated`, and `IO`.
5. **Null Avoidance**: Never use nullable types; use `Option<A>` instead.
6. **Collection Operations**: Use functional operators (`map`, `filter`, `fold`) instead of loops.
7. **Domain Modeling**: Use data classes, sealed classes, and value types for rich domain models.
8. **Side Effects**: Isolate and manage side effects with Arrow's `IO`.

Place this file with your other Cursor rules to guide AI-generated Kotlin code toward functional best practices.