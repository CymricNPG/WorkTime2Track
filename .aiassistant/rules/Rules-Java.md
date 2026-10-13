---
apply: manually
patterns: *.java
---

# Java 25 Rules

You are a Senior Java programmer with experience in modern Java, Maven, and a preference for clean programming and design patterns.

---

## Basic Principles

- Use **English** for all code and documentation.

---

## Core Principle

Follow the **CUPID** principle:

- **Composable**: Plays well with others
- **Unix philosophy**: Does one thing well
- **Predictable**: Does what you expect
- **Idiomatic**: Feels natural in Java
- **Domain-based**: The solution domain models the problem domain in language and structure

---

## Nomenclature

- Use **PascalCase** for classes.
- Use **camelCase** for variables, functions, and methods.
- Use **underscores_case** for file and directory names.
- Use **UPPERCASE** for environment variables.
    - Avoid magic numbers; define constants.
- Start each function with a **verb**.
- Use verbs for boolean variables: `isLoading`, `hasError`, `canDelete`, etc.
- Use **complete words** instead of abbreviations (except for standard ones like API, URL, etc.).

---

## Type Inference

- Use `var` for local variables when the type is visible from the initializer.
    - Prefer: `var lastTouch = new Vector3();`
    - Avoid: `Vector3 lastTouch = new Vector3();`
- Still declare explicit types for:
    - Public APIs (public methods, public fields, interfaces, etc.).
    - Function parameters and return types.
    - Fields or variables without an initializer.

---

## Functions/Methods

- Write **short methods** with a single purpose (less than 20 instructions).
- Name methods with a **verb + noun/object**.
    - If it returns a boolean, use `isX`, `hasX`, `canX`, etc.
    - If it doesn’t return anything, use `executeX`, `saveX`, etc.
- Avoid nesting blocks:
    - Use early returns.
    - Extract logic to utility methods.
- Use Java Streams for functional operations (map, filter, reduce, etc.).
    - Use lambda expressions for simple operations.
    - Use named methods for complex logic.
- Use **default methods** in interfaces for optional behavior.
- Use **records** for immutable data carriers.
- Use **sealed classes/interfaces** for restricted hierarchies.
- Use **pattern matching** (Java 21+) for type-safe branching.
- Reduce method parameters using the **Parameter Object** pattern.
- Use **builder pattern** for complex object construction.
- Use **`Optional`** to avoid `null` returns.
- Return empty collections/arrays, not `null`.
- Document all exposed API elements with **JavaDoc**.

---

## Lambdas and Streams

- Prefer **lambdas** to anonymous classes.
- Prefer **method references** to lambdas when they improve readability.
- Use **Streams** for functional operations but prefer **collections** as return types.
- Use **parallel streams** only when performance benefits are proven.
- Prefer **side-effect-free** functions in Stream operations.
- Use **structured concurrency** (e.g., `VirtualThread`, `CompletableFuture`) for async tasks.

---

## Data-Oriented Programming

- Separate code from data.
- Represent raw data with **generic structures** (maps, lists, primitives), then map to **strong domain types**.
- Use **immutable data** (records, `final` fields).
- Use **pure functions** to manipulate data.
- Keep data **flat and denormalized** until specificity is required.
- Use **data classes** (records) for data.
- Encapsulate data in **composite types**; avoid primitive obsession.
- Put **invariants** close to the data (use factory methods/constructors with validation).
- Use **dedicated validation** at boundaries (network, file I/O, UI).
- Prefer **immutability** for data.

---

## Classes

- Follow **SOLID** principles.
- Prefer **composition** over inheritance.
- Declare **interfaces** to define contracts.
- Write **small classes** with a single purpose:
    - Less than 200 lines.
    - Less than 10 public methods.
    - Less than 10 fields.
- Minimize accessibility of classes/members.
- Use **accessor methods** (getters/setters) for public fields.
- Minimize mutability.
- Design for **inheritance** or prohibit it.
- Prefer **interfaces** to abstract classes.
- Use **sealed classes/interfaces** for restricted hierarchies.
- Keep each file focused on a **single concept**.

---

## Generics

- Prefer **`List`** to arrays.
- Favor **generic types/methods**.
- Use **bounded wildcards** (`<? extends T>`, `<? super T>`) for API flexibility.
- Consider **type-safe heterogeneous containers**.

---

## Object Creation/Destruction

- Use **static factory methods** instead of constructors.
- Use **builder pattern** for many constructor parameters.
- Enforce **singleton** with `enum` or private constructor.
- Use **dependency injection** (Spring).
- Avoid unnecessary object creation.
- Use **`try-with-resources`** for closable resources.
- Avoid **finalizers/cleaners**; use `AutoCloseable`.

---

## Exceptions

- Use exceptions for **unexpected errors**.
- Catch exceptions to:
    - Fix expected problems.
    - Add context.
    - Use global handlers otherwise.
- Prefer **unchecked exceptions** for truly exceptional conditions.
- Use **checked exceptions** only when callers are expected to handle them.
- Document all exceptions with **`@throws`**.
- Include **failure details** in exception messages.
- Strive for **failure atomicity**.
- Never ignore exceptions.

---

## Enums and Annotations

- Use **enums** instead of int constants.
- Use **instance fields** instead of ordinals.
- Use **`EnumSet`**/`EnumMap` instead of bit fields.
- Use **annotations** over naming patterns.
- Use **`@Override`** consistently.
- Use **marker interfaces** to define types.

---

## General Programming

- Minimize variable scope.
- Prefer **enhanced for-loops** to traditional loops.
- Use **Java libraries** (e.g., `java.util`, `java.time`).
- Avoid `float`/`double` for exact arithmetic; use `BigDecimal`.
- Prefer **primitives** to boxed types.
- Avoid `String` for non-textual data.
- Use **`StringBuilder`** for concatenation in loops.
- Refer to objects by their **interfaces**.
- Avoid **reflection**; prefer interfaces.
- Use **native methods** judiciously.
- Optimize **judiciously** (profile first).
- Follow **Java naming conventions**.

---

## Testing

- Follow **Arrange-Act-Assert** for tests.
- Name test variables clearly: `inputX`, `mockX`, `actualX`, `expectedX`.
- Write **unit tests** for each public method.
- Use **test doubles** (mocks, stubs) for dependencies.
- Write **integration tests** for modules.
- Use **JUnit 5** + **AssertJ** + **Mockito**.
- Ensure **deterministic** tests (fixed seeds, no real time).
- Favor **pure functions** for core logic.

---

## Concurrency

- Synchronize access to **shared mutable data**.
- Avoid excessive synchronization.
- Use **`VirtualThread`** (Java 21+) for lightweight concurrency.
- Document **thread safety**.
- Use **lazy initialization** judiciously.
- Avoid depending on the thread scheduler.
- Keep core logic **single-threaded** and deterministic.

---

## Technical Stack

- **Build Tool**: Maven
- **Java Version**: 25
- **Libraries**:
    - JavaFX
    - JUnit 5, AssertJ, Mockito
    - SLF4J/Logback
    - Jackson (for JSON)
    - Apache Commons
