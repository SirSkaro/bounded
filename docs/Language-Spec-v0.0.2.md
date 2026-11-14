# Bounded Language Specification

**Status:** Discussion Draft 0.0.2\
**Date:** 2026-11-14\
**Scope:** Semantic core, module boundaries, composition, and first compiler milestone

This document captures the current design direction for Bounded. It is intentionally incomplete. Its purpose is to make the central claims precise enough to implement, test, challenge, and revise.

Normative words such as **MUST**, **MUST NOT**, **SHOULD**, and **MAY** describe intended language rules. Syntax examples are provisional unless explicitly described as normative.

Revision 0.0.2 separates architecture, module, and composition authority; keeps SPI implementation mappings private to their providing module; and adopts one provider instance per port contract in each executable assembly. These are intended semantics, not newly implemented compiler features. Section 13 describes the implemented slice.

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

An **ArcheOperation**, abbreviated **ArcheOp**, is an operation with a declared effect grade. The initial grades are `function`, `query`, `command`, and `action`.

The phonetic resemblance between ArcheOps and the Pokemon Archeops is acknowledged but non-normative. Defeatist is not a supported compiler mode.

### 3.3 Module

A **module** owns implementation declarations, state, and contracts. Its declaration describes its public contracts and boundary data, required capabilities, provided capabilities, and private mappings from provided contracts to implementations. Internal declarations are unnameable outside the module; authorized callers interact with their instances through exposed contracts.

### 3.4 Port

A **port** is a module-owned contract through which authorized communication may occur. A port describes a dependency required or provided by a module. The architecture selects the providing module, that module maps the contract to a private implementation, and composition constructs the instance used by the compiler's generated wiring.

An outbound port is also a service provider interface (**SPI**): its owning module defines the policy contract that an external provider must satisfy. Providing that SPI does not transfer ownership of the contract to the provider.

### 3.5 Boundary data

**Boundary data** consists of immutable data declarations whose complete transitive type graph is legal to carry through a specified boundary. Internal entities and implementation types MUST NOT cross a boundary, including when nested inside generic types or other data structures.

### 3.6 Architecture

The **architecture** is the authoritative top-level declaration of all participating modules, permitted dependencies, and logical port bindings. It refers to module-owned contracts and selects providing modules. It does not name the private classes or members that satisfy those contracts. It authorizes communication; it does not merely document communication discovered elsewhere.

### 3.7 Composition

A **compose** declaration constructs and configures instances within the authority granted by a module and architecture. An **executable assembly** is the instance graph constructed for one executable target under a selected architecture and root compose. Composition realizes the declared graph; it does not independently choose port bindings or redefine module provisions.

## 4. ArcheOp effect grades

The initial effect grades form the following order:

```text
function < query < command < action
```

An ArcheOp MAY invoke another ArcheOp at its own grade or a lower grade. It MUST NOT invoke an ArcheOp at a higher grade.

| Caller | Permitted callee grades |
| --- | --- |
| `function` | `function` |
| `query` | `function`, `query` |
| `command` | `function`, `query`, `command` |
| `action` | `function`, `query`, `command`, `action` |

This ordering establishes an effect ceiling, not an obligation. An `action` MAY happen to perform a pure calculation, but callers must treat it as action-grade behavior because its contract permits action-grade effects.

### 4.1 `function`

A `function` is a composable calculation.

A `function`:

- MUST NOT create an observable side effect;
- MUST NOT mutate existing state;
- MUST NOT read mutable or external state;
- MUST NOT invoke a port or arbitrary foreign JVM operation;
- MAY allocate and return immutable values;
- MAY call only other `function` ArcheOps.

The intended model is referential transparency. Typed failure values MAY be returned. Exception and termination semantics remain deferred.

### 4.2 `query`

A `query` observes state without causing a logical state change.

A `query`:

- MAY read owned mutable state;
- MAY invoke architecturally authorized read capabilities;
- MUST NOT modify logical state;
- MAY call `function` and `query` ArcheOps.

A query is not necessarily referentially transparent. Its result may change when the state it observes changes.

### 4.3 `command`

A `command` may cause a logical state transition within the authority granted to its ArcheClass and module.

A `command`:

- MAY observe state;
- MAY mutate state that its ArcheClass is authorized to own or control;
- MAY call `function`, `query`, and `command` ArcheOps;
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

An ArcheClass either forbids operations or permits them through a maximum effect grade. Every
ArcheOp declared within it MUST be permitted by that policy.

The initial proposed built-in ArcheClasses are:

| ArcheClass | Maximum grade | Initial intent |
| --- | --- | --- |
| `message` | none | Immutable boundary data with no behavior |
| `value` | `function` | Immutable domain concept with invariants and structural equality |
| `mapper` | `function` | Pure boundary transformation; intended to become total by default |
| `entity` | `command` | Identity-bearing domain concept with queries and controlled transitions |
| `usecase` | `action` | Stateless orchestration of domain behavior and declared ports |
| `adapter` | `action` | Technical detail that implements or consumes declared ports |
| `compose` | special | Scoped construction, configuration, and lifetime authority for a declared assembly |

Composition authority is separate from the ordinary effect-grade ordering. It is not a grade above `action`. This discussion provisionally calls `module` and `architecture` ArcheClasses as well; whether they are ultimately classified as ArcheClasses or dedicated structural declarations remains open. Neither classification grants ordinary application behavior or unrestricted construction rights.

The initial checker MUST reject an ArcheOp whose grade exceeds the enclosing ArcheClass ceiling,
or any ArcheOp when the enclosing ArcheClass permits no operations.

Examples:

```bounded
value Money {
    function plus(other: Money) -> Money
}

entity Order {
    query total() -> Money
    command add(line: OrderLine)
}

mapper OrderMapper {
    function toStored(order: Order) -> StoredOrder
}

usecase PlaceOrder {
    function validate(input: PlaceOrderInput) -> Validation
    action execute(input: PlaceOrderInput) -> PlaceOrderOutput
}
```

The following is invalid:

```bounded
value Money {
    command changeCurrency(currency: Currency)
}
```

The compiler MUST report that `command` exceeds `value`'s maximum grade of `function`.

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

### 7.1 Distributed authority

The architecture, modules, and composition have distinct responsibilities:

| Declaration | Authority |
| --- | --- |
| `architecture` | Enumerates all participating modules, authorizes their relationships, and selects logical bindings from required contracts to providing modules. |
| `module` | Owns its contracts and boundary data, declares requirements and provisions, and privately maps each provision to its implementing class or members. |
| `compose` | Constructs and configures instances and manages their lifetime within the declared module and architecture constraints. |

Module declarations MUST satisfy the architecture. Composition MUST satisfy both. No declaration may use its own authority to bypass the constraints owned by another declaration.

### 7.2 Module contracts and private provisions

A module owns both its inbound and outbound policy contracts. Runtime control direction and compile-time source dependency direction are modeled separately.

Provisional declaration syntax follows. Implementation bodies, detailed member mappings, and boundary-data declarations are omitted; these examples are not complete programs or implemented grammar.

```bounded
module orders {
    inbound port PlaceOrders {
        action place(input: PlaceOrderInput) -> PlaceOrderOutput
    }

    outbound port OrderStore {
        query load(id: OrderId) -> StoredOrder?
        command save(order: StoredOrder)
    }

    provides PlaceOrders using PlaceOrder
}

module web {
    requires orders.PlaceOrders
}

module jdbc {
    provides orders.OrderStore using SqlOrderStore
}
```

Here `PlaceOrder` is a private use case and `SqlOrderStore` is a private adapter. The `using` clause sketches a module-owned association; final syntax for class and member mappings remains open. The compiler MUST verify that a provision satisfies the complete port contract, including operation signatures and effect grades.

The web adapter requires `orders.PlaceOrders`. The JDBC adapter provides `orders.OrderStore`. Both compile-time dependencies point toward `orders`, which owns the policy contracts, even though runtime calls travel from web to orders and from orders to the JDBC provider.

The fact that `jdbc` provides `orders.OrderStore` is architecture-visible assembly metadata. `SqlOrderStore` and its implementing members remain private to `jdbc`; they do not become members of its public application API. An authorized consumer invokes the provider through `orders.OrderStore`.

### 7.3 Authoritative architecture

Provisional architecture syntax:

```bounded
architecture Commerce {
    module web
    module orders
    module jdbc

    bind web via orders.PlaceOrders to orders
    bind orders via orders.OrderStore to jdbc
}
```

Each illustrative `bind` names a consuming module, a port contract, and a providing module. It authorizes that relationship and selects a provision declared by the provider. The module list MUST be exhaustive for the executable assembly. A module's requirement or provision does not itself authorize an undeclared architectural relationship.

The architecture MUST NOT refer to `SqlOrderStore`, `PlaceOrder`, their constructors, or their implementing members. Any named provision syntax introduced later must identify a contract-level provision rather than expose a private implementation member.

The binding through `orders.OrderStore` does not make the JDBC implementation nameable from orders. The compiler preserves the source dependency from the provider to the contract-owning module while generating the runtime connection to the provider instance.

### 7.4 One provider instance per contract

Within one executable assembly, port contract identity MUST identify one provider instance. Identity is the port declaration in its owning module, not an unqualified name or a structurally equivalent set of operations.

- Each required port MUST have exactly one selected providing module and one implementing instance.
- All authorized uses of the same port contract MUST resolve to that same instance. Composition MUST NOT create a separate provider instance for each consumer or injection site.
- An adapter-provided SPI therefore has one adapter instance in the assembly. A use-case-provided inbound port follows the same provider-identity rule.
- Multiple selected provisions or provider instances for one contract MUST be rejected rather than resolved by declaration order, type scanning, or runtime selection.
- Alternative implementations may exist in the codebase; an assembly selects only one provision for a given contract. Qualified parallel instances and runtime provider replacement are outside this draft.

The rule is scoped to an executable assembly, not to an entire JVM or all executions of a program. Separate assemblies may select different implementations and construct different instances.

This constrains provider count, not caller count. Multiple architecturally authorized consumers may share the instance. Provider uniqueness alone does not guarantee that a contract has only one client or one reason to change; the contract must still express its owning module's policy needs.

### 7.5 Composition and generated wiring

The compiler MUST derive port wiring from the architecture's bindings and the modules' verified provisions. Application-level compose source does not need to name ports or know which private class or member provides them. It constructs and configures module instances and coordinates their assembly. It MUST NOT independently select or override a binding or implementation mapping.

Module-local composition may name its own private implementation types and the dependencies needed to construct them. It assembles those implementations according to the module's declared provisions. Knowledge of private constructors and implementation details remains inside the owning module.

A program MAY contain multiple compose declarations to divide construction work. Each executable target MUST select exactly one root compose; a library may supply composition fragments without selecting a root. Only composition contexts may invoke or delegate to another compose, and delegation MUST remain within the architecture and module authority granted to those contexts. The selected root realizes the declared assembly; it does not acquire unrestricted access to every participating module's private declarations.

Normal application code MUST NOT invoke compose or acquire its privileged construction authority. Composition MUST NOT receive blanket access to an implementation's internal behavior or state. Construction of the graph and invocation of application entry behavior are distinct responsibilities.

Detailed construction syntax, delegation syntax, lifecycle and shutdown rules, and cycle handling remain open. These open details do not permit composition to introduce undeclared modules, communication paths, or additional instances of a port provider.

## 8. Module APIs and boundary data

A module's public application API SHOULD contain only:

- its owned inbound and outbound ports;
- immutable boundary data;
- sum types and enums used by port signatures;
- branded primitive types approved for the boundary.

Concrete implementations, entities, use cases, framework types, and foreign library types MUST NOT appear in a module API.

Reading a module declaration SHOULD reveal its public port signatures, parameter and return data, required capabilities, and provided capabilities. Requirements and provisions form an assembly contract for architecture checking; they do not expose callable implementation classes or members to ordinary application code. A provider references another module's port contract without re-exporting its implementation or taking ownership of that contract.

Private implementation mappings belong to the module declaration. Replacing an implementation that satisfies the same provision, configuration, and lifetime requirements SHOULD leave the architecture and application-level composition unchanged.

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

- MUST be `function` grade;
- MUST NOT retain a reference to an internal source object in boundary data;
- MUST assign every required target field;
- MUST fail compilation when a required target field is added without a mapping;
- SHOULD be total unless explicitly declared otherwise.

Provisional syntax:

```bounded
mapper OrderMapper {
    function toStored(source: Order) -> StoredOrder {
        id     <- source.id
        status <- source.status
    }
}
```

Automatic same-name assignment MAY be considered as syntax shorthand, but semantic transformations and information loss SHOULD remain explicit.

## 10. Foreign JVM interoperability

Foreign JVM interoperability is a core long-term requirement but is outside the first compiler milestone.

Initial policy:

- arbitrary foreign calls are considered effectful and unsafe for `function` ArcheOps;
- foreign adapters are the intended location for Java, Kotlin, Scala, framework, database, network, and reflection details;
- a future trusted-pure foreign declaration may permit selected JVM operations inside `function` ArcheOps;
- the compiler and linker must not claim guarantees they cannot enforce against foreign bytecode.

The first implementation SHOULD avoid solving foreign purity, Spring compatibility, proxying, reflection, JPMS emission, or implementing a custom JVM bytecode verifier.

## 11. Diagnostics and tooling

Compiler diagnostics are part of Bounded's user-facing architecture model. They SHOULD name the violated concept and the legal alternatives.

Proposed diagnostic categories for the broader semantic milestone are listed below.
The current vertical slice uses the implemented codes documented in Section 13; these
proposals do not rename those existing codes.

| Code | Meaning |
| --- | --- |
| `BND100` | ArcheOp exceeds its ArcheClass maximum grade |
| `BND110` | Caller invokes a higher-grade ArcheOp |
| `BND120` | Implementation widens the grade of its contract |
| `BND200` | Undeclared or unauthorized module dependency |
| `BND210` | Internal or unsafe type crosses a boundary |
| `BND220` | Required port is unbound |
| `BND230` | ArcheOp lacks a required named capability |

Architecture and assembly diagnostics MUST also identify multiple selected providers for one contract, a selected module that lacks the promised provision, an implementation that fails its contract, and an unauthorized use of composition authority. Codes for these additional categories remain undecided.

Example:

```text
BND110: query `Order.total` cannot invoke command `Order.applyDiscount`
  at order.bounded:14:16
  permitted callees: function, query
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

1. A valid `value` containing only `function` ArcheOps compiles.
2. A `value` containing a `command` fails with `BND100`.
3. A `query` calling a `command` fails with `BND110`.
4. A `command` calling a `query` compiles.
5. An `action` calling all lower grades compiles.
6. A legal-grade cross-module call without architectural authorization fails with `BND200` or `BND230`.
7. Promoting a depended-upon `query` to `command` causes query-grade callers to fail.

The milestone does not require expressions, generics, inheritance, exceptions, complete type checking, Java interoperability, Spring, or code generation.

## 13. Provisional implementation strategy

Use a deliberately familiar bootstrap stack:

- Java 25;
- Maven;
- ANTLR for lexing and parsing;
- sealed interfaces and records for the AST;
- JUnit and Hamcrest for automated tests;
- direct JVM bytecode emission using the standard JDK Class-File API.

The backend consumes the analyzed program and produces class bytes in memory; a separate writer persists the class files. Generated classes explicitly target Java 25. This replaces the initial generated-Java experiment and requires no external bytecode library or preview flags. Emitted classes can be inspected with the Class-File API or `javap`.

The current vertical slice supports one `message`, `value`, `entity`, `usecase`, or `adapter` with bodyless
`function`, `query`, `command`, and `action` declarations. Messages permit no operations, including
derived functions; values permit only functions; entities permit operations through command grade;
use cases and adapters permit all four operation kinds. `function` is the keyword for a pure operation.
Operation names are case-sensitive and MUST be unique within their archeclass,
regardless of operation kind;
overloading is not supported in this slice. Each redeclaration produces `BND1002` at the
later declaration. Syntax errors use `BND0001`, forbidden effects use `BND1001`, and backend
failures use `BND9001`. These implemented codes are distinct from the proposed diagnostic
categories in Section 11.

The implemented `message` is an empty declaration with `MESSAGE` metadata and an empty operation
map. A message containing even a `function` is rejected with `BND1001`. Message fields, immutable
data construction, and transitive boundary-safety checks remain unimplemented.

The implemented `adapter` supports bodyless declarations and emits `ADAPTER` metadata with all
four operation kinds permitted through its action ceiling. Foreign JVM calls, port implementation
and binding, capabilities, and executable operation bodies remain unimplemented.

`module`, `architecture`, `compose`, requirements and provisions, provider-instance uniqueness,
and generated port wiring are also unimplemented. Sections 3, 7, and 8 define the design direction
for those features rather than describe current compiler behavior.

Semantic analysis retains the parsed program and a declaration index. The backend consumes
that analysis and returns a nonempty collection of uniquely named generated classes. The
writer returns descriptors containing each binary class name and class path root, so callers
can locate and load output without inferring the backend's naming convention.

Successful compilation contains artifacts and no diagnostics; rejected compilation contains
diagnostics and no artifacts. A source span carries its source name as well as a one-based
line and column. Text formatting is separate from diagnostic data and uses
`source:code:line:column message`. Filesystem failures propagate as I/O exceptions; completed
writes are retained if a later write fails. Output below the requested root must not traverse
symbolic links.

The current vertical slice emits metadata for archeclass and operation declarations. Runtime lowering for operation bodies remains deferred until their semantics are defined. The semantic milestone in Section 12 remains the language's central objective.

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

The [acceptance testing roadmap](Acceptance-Testing-Roadmap.md) describes how the existing
source fixtures can grow into a DSL for compilation, diagnostics, and execution of emitted
code. Stage 1 supports compilation, rejection, and metadata loading. Later stages, including
executable operation bodies and Java interoperability, remain subject to the language
semantics defined here.

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

Backend unit tests construct ASTs directly:

1. generate class bytes with the JDK Class-File API;
2. inspect the emitted class version and structure;
3. verify the bytes with `ClassFile.verify`;
4. load the class with an isolated class loader;
5. assert observable behavior and public signatures.

Test filesystem output separately, including package paths, exact byte preservation, and I/O failures. Test result invariants and defensive copying, including mutable byte arrays. End-to-end fixture tests compile Bounded source, load generated classes, and ensure invalid programs emit no artifacts.

### 14.6 Regression rule

Every discovered compiler bug MUST receive a minimal failing source fixture before or alongside the fix.

### 14.7 Planned architecture and composition coverage

When the corresponding features are implemented, acceptance examples MUST cover:

- an authorized binding to a module whose SPI implementation stays private, including rejection of attempts to name its concrete class or members across the boundary;
- replacement of a private implementation without changing the architecture or application-level composition when its assembly requirements are unchanged;
- rejection of unbound requirements, incompatible provisions, undeclared modules or connections, and multiple selected providers for one contract;
- one shared provider instance for all authorized uses of a contract within an assembly;
- composition fragments assembled under one selected executable root, with ordinary application calls to compose rejected.

These are planned contracts, not acceptance tests already present in the compiler suite.

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

## 16. Explicit non-goals for Draft 0.0.2

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
- complete JVM lowering for operation bodies;
- package management;
- incremental compilation;
- qualified parallel provider instances or runtime provider replacement for one port contract;
- complete construction, lifecycle, shutdown, or composition-delegation syntax;
- custom user-defined ArcheClasses;
- a complete effect-capability algebra.

These are deferred so the first implementation can test the language's architectural thesis rather than recreate Java before validating it.

## 17. Open design questions

1. Are the four grades a permanently linear order, or surface syntax over a more precise internal capability set?
2. Should entity `query` operations and externally backed port queries share one grade despite observing different scopes of state?
3. What may a `command` return without collapsing command-query separation?
4. Should a mapper be required to be total in the first language release?
5. Messages are behavior-free in the current slice, including rejection of derived `function`
   ArcheOps. Would allowing derived functions be justified in a later design?
6. Which ArcheClass owns pure domain behavior that fits neither an entity nor a value?
7. Are ArcheClass ceilings fixed language rules or eventually user-definable under compiler-verified constraints?
8. Which portions of an operation's capability set must appear explicitly in source, and which may be inferred?
9. How should domain events relate to entity commands and application actions?
10. What is the smallest Java ABI that allows polyglot adapters without weakening the core guarantees?
11. Should `module` and `architecture` remain structural ArcheClasses or become dedicated declaration forms?
12. How should modules expose scoped construction authority to composition while keeping private implementations unnameable outside the module?
13. What syntax should express private class/member mappings, module configuration, compose delegation, and executable-root selection?
14. What are the construction order, cycle, failure, and shutdown rules for an executable assembly with one provider instance per contract?

## 18. Immediate next steps

1. Review and revise Sections 4 through 8 until the effect, authority, and boundary model feels internally coherent.
2. Choose the smallest provisional syntax needed by the seven milestone cases in Section 12.
3. Create the compiler repository and a single `EffectGrade` enum with exhaustive unit tests.
4. Add ArcheClass ceiling validation against hand-built AST objects.
5. Add ANTLR only when the semantic tests establish what the parser must produce.
6. Add fixture-based compilation tests before implementing architecture authorization.
7. Keep JVM emission limited to the metadata shell while developing the semantic checks; add operation-body lowering as the corresponding semantics are established.

## 19. Intellectual foundation

The design direction is informed principally by:

- Eric Evans, *Domain-Driven Design*, especially knowledge-rich models, model-driven design, services, side-effect-free functions, assertions, supple design, and declarative design.
- Robert C. Martin, *Clean Architecture*, especially dependency inversion, boundary crossing, component boundaries, Main as composition root, and screaming architecture.
- Herbert A. Simon's bounded rationality: decision-makers operate under limited information, time, and cognitive capacity, motivating the externalization of global constraints into language and compiler semantics.

Bounded does not attempt to encode either Evans's or Martin's diagrams as a mandatory topology. It attempts to provide language primitives capable of expressing and enforcing the architectural knowledge those works ask developers to preserve.
