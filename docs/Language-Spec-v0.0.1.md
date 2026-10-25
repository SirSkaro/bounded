# Bounded Language Specification

**Status:** Discussion Draft 0.1  
**Date:** 2026-08-18  
**Scope:** Semantic core and first compiler milestone  

This document captures the current design direction for Bounded. It is intentionally incomplete. Its purpose is to make the central claims precise enough to implement, test, challenge, and revise.

Normative words such as **MUST**, **MUST NOT**, **SHOULD**, and **MAY** describe intended language rules. Syntax examples are provisional unless explicitly described as normative.

## 1. Vision

Bounded is an architecture-aware, JVM-targeted programming language. It makes architectural roles, dependency direction, boundary contracts, and effects part of the program's compile-time semantics.

Bounded is named principally for bounded rationality: neither a person nor a software agent can continuously retain and reason over every global constraint in a large system. The language and compiler externalize those constraints so that developers can reason locally without silently violating the declared whole.

The name also reflects several related forms of boundedness:

- architectural boundaries;
- bounded contexts;
- bounded effects;
- bounded authority;
- bounded visibility.

Bounded's goal is not to infer a good architecture. Its goal is to make a chosen architecture visible, executable, and difficult to contradict accidentally.

## 2. Core proposition

A Bounded program is governed by three layers of permission:

1. An **ArcheClass** declares why a type exists and establishes the maximum kinds of behavior it may contain.
2. An **ArcheOperation** declares the maximum effect grade of a particular operation.
3. The **architecture** grants named authority to communicate across modules and boundaries.

A call is legal only when all three layers permit it.

Informally:

```text
legal call
    = permitted by the caller's ArcheOperation
    AND permitted by the caller's ArcheClass
    AND authorized by the declared architecture
```

Lexical visibility alone is not architectural authorization.

## 3. Terminology

### 3.1 ArcheClass

An **ArcheClass** is a declaration form whose architectural role determines its capabilities and restrictions. Unlike a conventional class, an ArcheClass tells the compiler not only what a type is, but why it exists in the system.

Examples include `value`, `entity`, `message`, `mapper`, `usecase`, and `adapter`.

### 3.2 ArcheOperation (ArcheOp)

An **ArcheOperation**, abbreviated **ArcheOp**, is an operation with a declared effect grade. The initial grades are `fn`, `query`, `command`, and `action`.

The phonetic resemblance between ArcheOps and the Pokemon Archeops is acknowledged but non-normative. Defeatist is not a supported compiler mode.

### 3.3 Module

A **module** owns implementation declarations, state, and contracts. Its internal declarations are unnameable outside the module except through explicitly exposed architectural contracts.

### 3.4 Port

A **port** is a module-owned contract through which authorized communication may occur. A port describes a dependency required or provided by a module; composition binds requirements to providers.

### 3.5 Boundary data

**Boundary data** consists of immutable data declarations whose complete transitive type graph is legal to carry through a specified boundary. Internal entities and implementation types MUST NOT cross a boundary, including when nested inside generic types or other data structures.

### 3.6 Architecture

The **architecture** is a top-level declaration of modules, ports, permitted dependencies, and bindings. It authorizes communication; it does not merely document communication discovered elsewhere.

## 4. ArcheOp effect grades

The initial effect grades form the following order:

```text
fn < query < command < action
```

An ArcheOp MAY invoke another ArcheOp at its own grade or a lower grade. It MUST NOT invoke an ArcheOp at a higher grade.

| Caller | Permitted callee grades |
| --- | --- |
| `fn` | `fn` |
| `query` | `fn`, `query` |
| `command` | `fn`, `query`, `command` |
| `action` | `fn`, `query`, `command`, `action` |

This ordering establishes an effect ceiling, not an obligation. An `action` MAY happen to perform a pure calculation, but callers must treat it as action-grade behavior because its contract permits action-grade effects.

### 4.1 `fn`

A `fn` is a composable calculation.

A `fn`:

- MUST NOT create an observable side effect;
- MUST NOT mutate existing state;
- MUST NOT read mutable or external state;
- MUST NOT invoke a port or arbitrary foreign JVM operation;
- MAY allocate and return immutable values;
- MAY call only other `fn` ArcheOps.

The intended model is referential transparency. Typed failure values MAY be returned. Exception and termination semantics remain deferred.

### 4.2 `query`

A `query` observes state without causing a logical state change.

A `query`:

- MAY read owned mutable state;
- MAY invoke architecturally authorized read capabilities;
- MUST NOT modify logical state;
- MAY call `fn` and `query` ArcheOps.

A query is not necessarily referentially transparent. Its result may change when the state it observes changes.

### 4.3 `command`

A `command` may cause a logical state transition within the authority granted to its ArcheClass and module.

A `command`:

- MAY observe state;
- MAY mutate state that its ArcheClass is authorized to own or control;
- MAY call `fn`, `query`, and `command` ArcheOps;
- MUST NOT invoke an `action`;
- MUST NOT acquire undeclared architectural authority merely because its effect grade is sufficient.

The distinction between returning acknowledgements, typed rejections, domain events, or no value is deferred.

### 4.4 `action`

An `action` coordinates effects through explicitly declared capabilities.

An `action`:

- MAY call any ArcheOp grade;
- MAY cross boundaries only through declared ports;
- MAY exercise only capabilities granted to its ArcheClass, module, and operation;
- MUST NOT use its grade as blanket access to otherwise unreachable code.

Use cases and adapters are typical homes for actions.

### 4.5 Effect widening and narrowing

An ArcheOp's grade is part of its public type and API contract.

- **Effect widening**, such as `query` to `command`, is internally monotonic but potentially breaking to callers.
- **Effect narrowing**, such as `command` to `query`, is safe for existing callers but requires the implementation to satisfy the narrower grade.
- An implementation of an abstract operation or port operation MUST NOT have a higher grade than the contract it implements.
- Function values, lambdas, callbacks, and higher-order operations MUST preserve their effect grade. They MUST NOT erase a command or action into an apparently pure function type.

## 5. ArcheClass effect ceilings

Every ArcheClass has a maximum effect grade. Every ArcheOp declared within it MUST be at or below that ceiling.

The initial proposed built-in ArcheClasses are:

| ArcheClass | Maximum grade | Initial intent |
| --- | --- | --- |
| `message` | none | Immutable boundary data with no behavior |
| `value` | `fn` | Immutable domain concept with invariants and structural equality |
| `mapper` | `fn` | Pure boundary transformation; intended to become total by default |
| `entity` | `command` | Identity-bearing domain concept with queries and controlled transitions |
| `usecase` | `action` | Stateless orchestration of domain behavior and declared ports |
| `adapter` | `action` | Technical detail that implements or consumes declared ports |
| `compose` | special | Construction and binding authority, not ordinary application behavior |

The initial checker MUST reject an ArcheOp whose grade exceeds the enclosing ArcheClass ceiling.

Examples:

```bounded
value Money {
    fn plus(other: Money) -> Money
}

entity Order {
    query total() -> Money
    command add(line: OrderLine)
}

mapper OrderMapper {
    fn toStored(order: Order) -> StoredOrder
}

usecase PlaceOrder {
    fn validate(input: PlaceOrderInput) -> Validation
    action execute(input: PlaceOrderInput) -> PlaceOrderOutput
}
```

The following is invalid:

```bounded
value Money {
    command changeCurrency(currency: Currency)
}
```

The compiler MUST report that `command` exceeds `value`'s maximum grade of `fn`.

The `service` ArcheClass is intentionally deferred because statelessness does not determine effect grade. A later design may distinguish pure domain policies from effectful application orchestration rather than introduce one ambiguous service category.

## 6. Call legality

For a call from operation `C` to operation `T`, the compiler MUST establish all of the following:

1. `grade(T) <= grade(C)`.
2. `grade(C) <= maximumGrade(archeClass(C))`.
3. `T` is visible through the appropriate module API or is internal to `C`'s module.
4. The declared architecture permits the dependency from `C`'s module to `T`'s contract-owning module.
5. Any required port capability is declared for `C`'s ArcheClass or enclosing module.
6. Boundary data used by the call is transitively boundary-safe.

A sufficient effect grade does not override rules 3 through 6.

For example, an entity command cannot call a repository merely because both operations are command-grade. The entity has no repository capability unless the language explicitly defines and the architecture grants one.

## 7. Ports and dependency inversion

A module owns both its inbound and outbound policy contracts. Runtime control direction and compile-time source dependency direction are modeled separately.

Provisional syntax:

```bounded
module orders {
    inbound port PlaceOrders {
        action place(input: PlaceOrderInput) -> PlaceOrderOutput
    }

    outbound port OrderStore {
        query load(id: OrderId) -> StoredOrder?
        command save(order: StoredOrder)
    }
}
```

An inbound adapter requires `orders.PlaceOrders`. An outbound persistence adapter provides `orders.OrderStore`. Both compile-time dependencies point toward the module that owns the policy contracts even though runtime calls travel in different directions.

Provisional composition syntax:

```bounded
architecture Commerce {
    module web
    module orders
    module jdbc

    bind web.placeOrders to orders.PlaceOrders
    bind orders.OrderStore to jdbc.orderStore
}
```

Normal application code cannot acquire composition's privileged construction authority. Composition may construct and bind implementations but MUST NOT receive blanket access to their internal behavior or state.

## 8. Module APIs and boundary data

A module API SHOULD contain only:

- ports;
- immutable boundary data;
- sum types and enums used by port signatures;
- branded primitive types approved for the boundary.

Concrete implementations, entities, use cases, framework types, and foreign library types MUST NOT appear in a module API.

Boundary safety is recursive. For every type exposed by a port signature, the compiler MUST inspect the complete reachable type graph, including:

- generic arguments;
- collection elements;
- fields and record components;
- arrays;
- nested boundary data;
- success and failure variants.

An internal entity hidden inside `List<Result<OrderEntity>>` is still a boundary violation.

## 9. Mapping

Mapping is a first-class boundary operation rather than reflection-based convention.

A mapper:

- MUST be `fn` grade;
- MUST NOT retain a reference to an internal source object in boundary data;
- MUST assign every required target field;
- MUST fail compilation when a required target field is added without a mapping;
- SHOULD be total unless explicitly declared otherwise.

Provisional syntax:

```bounded
mapper OrderMapper {
    fn toStored(source: Order) -> StoredOrder {
        id     <- source.id
        status <- source.status
    }
}
```

Automatic same-name assignment MAY be considered as syntax shorthand, but semantic transformations and information loss SHOULD remain explicit.

## 10. Foreign JVM interoperability

Foreign JVM interoperability is a core long-term requirement but is outside the first compiler milestone.

Initial policy:

- arbitrary foreign calls are considered effectful and unsafe for `fn` ArcheOps;
- foreign adapters are the intended location for Java, Kotlin, Scala, framework, database, network, and reflection details;
- a future trusted-pure foreign declaration may permit selected JVM operations inside `fn` ArcheOps;
- the compiler and linker must not claim guarantees they cannot enforce against foreign bytecode.

The first implementation SHOULD avoid solving foreign purity, Spring compatibility, proxying, reflection, JPMS emission, or direct JVM bytecode verification.

## 11. Diagnostics and tooling

Compiler diagnostics are part of Bounded's user-facing architecture model. They SHOULD name the violated concept and the legal alternatives.

Initial stable diagnostic categories:

| Code | Meaning |
| --- | --- |
| `BND100` | ArcheOp exceeds its ArcheClass maximum grade |
| `BND110` | Caller invokes a higher-grade ArcheOp |
| `BND120` | Implementation widens the grade of its contract |
| `BND200` | Undeclared or unauthorized module dependency |
| `BND210` | Internal or unsafe type crosses a boundary |
| `BND220` | Required port is unbound |
| `BND230` | ArcheOp lacks a required named capability |

Example:

```text
BND110: query `Order.total` cannot invoke command `Order.applyDiscount`
  at order.bounded:14:16
  permitted callees: fn, query
```

The compiler SHOULD eventually support:

```text
bounded check
bounded check --diagnostics=json
bounded explain PlaceOrder.execute
bounded architecture
bounded format
```

Structured and stable diagnostics are required for IDEs, automated testing, build pipelines, and software agents.

## 12. First executable milestone: effect checker

The first milestone is not JVM bytecode generation. It is a semantic checker proving Bounded's central claim.

The milestone supports only enough syntax to declare:

- modules;
- the initial ArcheClasses;
- ArcheOps and their grades;
- simple operation bodies containing calls;
- a minimal architecture declaration;
- named ports or capabilities sufficient for authorization checks.

It MUST demonstrate these cases:

1. A valid `value` containing only `fn` ArcheOps compiles.
2. A `value` containing a `command` fails with `BND100`.
3. A `query` calling a `command` fails with `BND110`.
4. A `command` calling a `query` compiles.
5. An `action` calling all lower grades compiles.
6. A legal-grade cross-module call without architectural authorization fails with `BND200` or `BND230`.
7. Promoting a depended-upon `query` to `command` causes query-grade callers to fail.

The milestone does not require expressions, generics, inheritance, exceptions, complete type checking, Java interoperability, Spring, or code generation.

## 13. Provisional implementation strategy

Use a deliberately familiar bootstrap stack:

- Java 21;
- Maven or Gradle, with preference given to whichever minimizes project friction;
- ANTLR for lexing and parsing;
- sealed interfaces and records for the AST;
- JUnit for automated tests;
- generated Java source as the first executable backend;
- ASM only after the semantics are stable enough to justify direct bytecode emission.

Generating Java before emitting bytecode directly preserves the JVM goal while keeping the early work focused on Bounded's semantics. The generated source can be compiled with the JDK compiler and inspected during debugging.

Suggested source structure:

```text
bounded/
  docs/
    specification.md
  src/main/antlr/
    BoundedLexer.g4
    BoundedParser.g4
  src/main/java/.../
    ast/
    parse/
    symbols/
    semantics/
    diagnostics/
    cli/
  src/test/java/.../
  src/test/resources/cases/
    valid/
    invalid/archeclass/
    invalid/effects/
    invalid/architecture/
```

The compiler core SHOULD expose a library API such as:

```java
CompilationResult compile(SourceFile source);
```

The command-line interface should be a thin adapter over that API. Tests should invoke the compiler API directly rather than spawn a process for every case.

## 14. Compiler testing strategy

Compilers are unusually well suited to automated testing because source text is an input and diagnostics or generated output are deterministic results.

### 14.1 Unit tests

Test semantic rules without parsing whenever possible.

Examples:

```java
assertTrue(COMMAND.canCall(QUERY));
assertFalse(QUERY.canCall(COMMAND));
assertTrue(ENTITY.permits(COMMAND));
assertFalse(VALUE.permits(QUERY));
```

AST-level tests can construct small declarations directly and verify exact diagnostic codes and source spans.

### 14.2 Source fixture tests

Place small `.bounded` programs in test resources.

```text
cases/invalid/effects/query-calls-command.bounded
cases/invalid/effects/query-calls-command.diag
```

The source fixture contains the smallest program that demonstrates the rule. The diagnostic fixture contains the expected code, location, and important symbols.

Parameterized tests scan all fixtures:

- every file under `valid` MUST compile without errors;
- every file under `invalid` MUST produce the expected diagnostics;
- an invalid fixture SHOULD test one primary rule so failures remain interpretable.

### 14.3 Parser tests

Parser tests verify accepted syntax, rejected syntax, operator precedence when introduced, and accurate source locations. Parse-tree snapshots MAY be useful early but should not replace semantic assertions.

### 14.4 Golden diagnostics

Human-readable compiler output can be snapshot-tested as golden text. Stable diagnostic codes should also be asserted independently so harmless wording changes do not obscure semantic regressions.

### 14.5 Backend tests

After Java generation exists:

1. compile Bounded source;
2. inspect the generated Java;
3. compile the Java using the JDK compiler;
4. load or run the result;
5. assert observable behavior.

Later direct-bytecode tests can use ASM to inspect class shape and an isolated class loader or subprocess to execute generated classes.

### 14.6 Regression rule

Every discovered compiler bug MUST receive a minimal failing source fixture before or alongside the fix.

## 15. AI-assisted development

Bounded SHOULD be designed so that its architectural model is legible to humans, build tools, IDEs, and software agents.

Helpful properties include:

- one canonical formatter;
- few equivalent ways to express an architectural relationship;
- explicit module and port declarations;
- stable machine-readable diagnostics;
- compiler-generated architecture manifests;
- commands that explain a symbol's role, effect grade, and authority;
- deterministic builds and tests.

These features do not make an architecture wise. They allow generated and human-written changes to be checked against the same declared model. Bounded may therefore be friendly to AI-assisted implementation while remaining hostile to "vibe architecture."

## 16. Explicit non-goals for Draft 0.1

This draft does not yet specify:

- complete grammar or concrete syntax;
- primitive types or expression semantics;
- generics;
- nullability;
- inheritance or traits;
- concurrency;
- exception behavior;
- totality checking;
- persistence;
- framework integration;
- foreign JVM purity declarations;
- direct bytecode emission;
- package management;
- incremental compilation;
- custom user-defined ArcheClasses;
- a complete effect-capability algebra.

These are deferred so the first implementation can test the language's architectural thesis rather than recreate Java before validating it.

## 17. Open design questions

1. Are the four grades a permanently linear order, or surface syntax over a more precise internal capability set?
2. Should entity `query` operations and externally backed port queries share one grade despite observing different scopes of state?
3. What may a `command` return without collapsing command-query separation?
4. Should a mapper be required to be total in the first language release?
5. May messages contain derived `fn` ArcheOps, or must boundary data remain behavior-free?
6. Which ArcheClass owns pure domain behavior that fits neither an entity nor a value?
7. Are ArcheClass ceilings fixed language rules or eventually user-definable under compiler-verified constraints?
8. Which portions of an operation's capability set must appear explicitly in source, and which may be inferred?
9. How should domain events relate to entity commands and application actions?
10. What is the smallest Java ABI that allows polyglot adapters without weakening the core guarantees?

## 18. Immediate next steps

1. Review and revise Sections 4 through 7 until the effect and authority model feels internally coherent.
2. Choose the smallest provisional syntax needed by the seven milestone cases in Section 12.
3. Create the compiler repository and a single `EffectGrade` enum with exhaustive unit tests.
4. Add ArcheClass ceiling validation against hand-built AST objects.
5. Add ANTLR only when the semantic tests establish what the parser must produce.
6. Add fixture-based compilation tests before implementing architecture authorization.
7. Defer JVM code generation until `BND100`, `BND110`, and `BND200` are demonstrated end to end.

## 19. Intellectual foundation

The design direction is informed principally by:

- Eric Evans, *Domain-Driven Design*, especially knowledge-rich models, model-driven design, services, side-effect-free functions, assertions, supple design, and declarative design.
- Robert C. Martin, *Clean Architecture*, especially dependency inversion, boundary crossing, component boundaries, Main as composition root, and screaming architecture.
- Herbert A. Simon's bounded rationality: decision-makers operate under limited information, time, and cognitive capacity, motivating the externalization of global constraints into language and compiler semantics.

Bounded does not attempt to encode either Evans's or Martin's diagrams as a mandatory topology. It attempts to provide language primitives capable of expressing and enforcing the architectural knowledge those works ask developers to preserve.
