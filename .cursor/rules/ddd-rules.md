---
description: 
globs: 
alwaysApply: false
---
 # Domain Driven Design Principles

*Cursor rules file – language-agnostic DDD guidelines for complex software systems.*

> **Intent**  
> Establish a **common language** between developers and domain experts.  
> Create a **rich, expressive domain model** that encapsulates business logic.  
> Apply **strategic and tactical design patterns** to manage complexity in large systems.

---

## 1 Core Concepts

| Concept | Description |
|---------|-------------|
| **Ubiquitous Language** | Shared vocabulary between domain experts and developers that is reflected in the code, documentation, and conversations. |
| **Bounded Context** | Explicit boundary within which a model applies, isolating it from other parts of the system with its own ubiquitous language. |
| **Context Map** | Documentation of how bounded contexts relate to each other and integrate at their boundaries. |
| **Domain Model** | Object model expressing domain concepts, relationships, and business rules, focusing on behavior over data. |

---

## 2 Strategic Design

### 2.1 Bounded Contexts

* Define explicit boundaries around models where terms and concepts have consistent meaning
* Each context maintains its own ubiquitous language and model
* A large system will typically have multiple bounded contexts
* Organize code structure to reflect these boundaries (e.g., separate modules or packages)

### 2.2 Context Mapping

Document relationships between bounded contexts with specific patterns:

| Pattern | Description |
|---------|-------------|
| **Anticorruption Layer** | Translation layer that isolates a system from another context/legacy system |
| **Shared Kernel** | Common code shared between two contexts with explicit agreement on shared elements |
| **Customer/Supplier** | Upstream/downstream relationship where downstream depends on upstream |
| **Conformist** | Downstream team adopts the model of upstream team without translation |
| **Partnership** | Two contexts with mutual dependency and collaborative integration |
| **Open Host Service** | Well-defined protocol or interface for integration with multiple contexts |
| **Published Language** | Shared, well-documented format for transferring data between contexts |

### 2.3 Core Domain

* Identify the **core domain** — the part with highest business value and differentiation
* Distinguish from **supporting domains** (important but not unique) and **generic domains** (commodities)
* Invest effort proportional to business value — focus most energy on core domain
* Consider using generic solutions for generic domains (off-the-shelf, open source)

---

## 3 Tactical Design

### 3.1 Building Blocks

#### Value Objects

* Immutable objects defined by their attributes, without identity
* Examples: Money, Color, Address, DateRange
* Two value objects with the same properties are considered equal
* Designed to be passed by value, not reference
* Encapsulate domain rules related to their composition

#### Entities

* Objects defined by a thread of continuity and identity
* Identity is maintained regardless of attribute changes
* Examples: User, Order, Account
* Two entities with the same ID represent the same object
* Focus on behavior rather than attributes
* Encapsulate business rules specific to the entity

#### Aggregates

* Cluster of associated objects treated as a unit for data changes
* One entity serves as the **aggregate root** — the only access point from outside
* All access to internal objects must go through the root
* Transactional consistency boundary — one transaction per aggregate
* References between aggregates use identity (not direct object references)
* Keep aggregates small to avoid consistency conflicts

#### Domain Events

* Record of something significant that happened in the domain
* Named in past tense (e.g., OrderPlaced, PaymentReceived)
* Immutable and typically include a timestamp
* Used for communication between bounded contexts
* Enable eventual consistency and event sourcing
* Can trigger workflows or be used for audit trails

#### Repositories

* Provide collection-like interface for accessing aggregates
* Abstract underlying persistence mechanisms
* Retrieve complete aggregates, not partial objects
* Usually one repository per aggregate type
* Allow querying by ID and limited criteria
* Enforce persistence ignorance in the domain model

#### Domain Services

* Operations that don't conceptually belong to any entity or value object
* Represent domain concepts that are processes or transformations
* Stateless operations involving multiple domain objects
* Named after activities or actions in the domain
* Contain significant business logic, not just CRUD

#### Specifications

* Encapsulate selection criteria or business rules
* Can be composed using boolean logic (and, or, not)
* Used for validation, selection, and creation of objects
* Express business rules in a clear, reusable way
* Can be used in-memory or translated to database queries

---

## 4 Layered Architecture

Organize code into layers with dependencies flowing inward:

### 4.1 Core Layers

| Layer | Responsibility | Components |
|-------|----------------|------------|
| **Domain Layer** | Core business concepts, rules, and logic | Entities, Value Objects, Domain Events, Domain Services |
| **Application Layer** | Directs workflows, orchestrates domain objects | Application Services, Use Cases, Command/Query Handlers |
| **Infrastructure Layer** | Technical capabilities, external concerns | Repositories (impl), Messaging, Persistence, Security |
| **Interface Layer** | Interaction with external systems/users | Controllers, Views, API endpoints, CLI, Event listeners |

### 4.2 Layer Interaction Principles

* **Dependency Rule**: Outer layers depend on inner layers, never the reverse
* **Domain Isolation**: Domain layer has no dependencies on infrastructure or interface layers
* **Ports & Adapters**: Define interfaces in inner layers, implementations in outer layers
* **DTO Boundaries**: Use Data Transfer Objects at layer boundaries to avoid leaking domain objects

---

## A Layered Implementation Approach

A common architecture pattern implements DDD with these specific components:

1. **Presentation Layer**
   * **Controllers**: Handle HTTP requests and user input
   * **Views/Templates**: Format data for presentation
   * **DTOs**: Simplify data exchange with clients
   * **Validation**: Input sanitization and validation

2. **Application Layer**
   * **Application Services**: Orchestrate domain operations
   * **Commands/Queries**: Represent user intents
   * **Assemblers/Mappers**: Transform between DTOs and domain objects
   * **Security**: Authentication and authorization

3. **Domain Layer**
   * **Entities**: Business objects with identity and lifecycle
   * **Value Objects**: Immutable objects defined by attributes
   * **Domain Services**: Operations across multiple entities
   * **Domain Events**: Record of significant occurrences
   * **Repositories** (interfaces): Collection-like storage abstraction

4. **Infrastructure Layer**
   * **Repository Implementations**: Database access
   * **ORM/Data Access**: Database mapping technology
   * **Messaging**: Event publishing and subscription
   * **External Services**: Integration with third-party systems
   * **Persistence**: Database connections and transactions

---

### TL;DR

1. Start with **strategic design**: identify bounded contexts and their relationships.
2. Develop a **ubiquitous language** with domain experts and reflect it in your code.
3. Implement **tactical patterns** to express the model: entities, value objects, aggregates.
4. Organize code in **layers**: domain, application, infrastructure, and interface.
5. Use **repositories** for persistence and **domain services** for cross-entity operations.
6. Capture state changes with **domain events** for traceability and integration.
