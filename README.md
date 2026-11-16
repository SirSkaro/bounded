# Bounded

Bounded is an experimental JVM language that puts architectural roles, effects, and module
boundaries into source declarations so the compiler can check them.

This overview describes the [intended language](docs/Language-Spec-v0.0.2.md). The compiler
is at an [early stage](#current-status).

## ArcheClasses: architectural roles

An **ArcheClass** is a type declaration whose architectural role determines the behavior
it may contain.

| ArcheClass | Role | Operation ceiling |
| --- | --- | --- |
| `message` | Immutable boundary data | No operations |
| `value` | Immutable domain concept with invariants | `function` |
| `mapper` | Pure boundary transformation | `function` |
| `entity` | Identity and controlled state transitions | `command` |
| `usecase` | Orchestration of domain behavior and ports | `action` |
| `adapter` | Technical implementation or consumption of ports | `action` |

## ArcheOperations: effect ceilings

An **ArcheOperation** is an operation with a declared maximum effect grade:

| ArcheOperation | Permitted behavior |
| --- | --- |
| `function` | Pure calculation, independent of mutable or external state |
| `query` | Observe immutable or immutable state without a logical state change |
| `command` | Change state within granted authority |
| `action` | Coordinate effects through declared capabilities |

The grades form `function < query < command < action`. An operation may call its own grade
or lower. Its enclosing ArcheClass caps which grades it may declare: an entity can contain
a `command` that calls a `query`, but cannot declare an `action`. Effect grades never grant
undeclared boundary access.

## Boundaries: modules, architecture, and composition

- **`module`** owns inbound and outbound **ports** and their boundary data, declares required
  and provided capabilities, and privately maps contracts to implementations.
- **`architecture`** lists every participating module and binds consumers through port
  contracts to providing modules. It does not name private implementation classes or members.
- **`compose`** constructs and configures instances within those declarations. The compiler
  derives port wiring; each contract identifies one provider instance per executable assembly.
  Composition authority is separate from effect grades.

Here, orders owns the inbound `PlaceOrders` and outbound `OrderStore` contracts. This is
provisional syntax; port and implementation declarations are omitted:

```bounded
architecture Commerce {
    module web
    module orders
    module jdbc

    bind web via orders.PlaceOrders to orders
    bind orders via orders.OrderStore to jdbc
}
```

Port signatures expose immutable, transitively safe boundary data. Internal entities and
implementation types stay inside their modules.

## Current status

The compiler accepts one `message`, `value`, `entity`, `usecase`, or `adapter` per source
file, with bodyless operation declarations. It checks declaration ceilings and unique,
case-sensitive operation names, then emits JVM class files containing declaration metadata.

Parameters, return types, executable bodies and calls, message fields, `mapper`, modules,
ports, architecture, and composition remain unimplemented. The emitted classes do not yet
run application behavior.

## Build and test

Requires **JDK 25** and **Maven 3.9+**. Check `mvn --version` to confirm Maven uses JDK 25;
the IDE's project SDK and Maven's runtime can differ.

```bash
mvn clean verify
```

Verification includes unit and acceptance tests, architecture checks, static analysis,
and mutation testing. The compiler emits class files directly through the JDK Class-File
API, without preview flags or an external bytecode library.

## Try the compiler

After building, compile the included example:

```bash
java -jar target/bounded-0.0.1.jar examples/money.bounded build/money
```

This produces `build/money/classes/bounded/generated/Bounded_Money.class`. Inspect it with:

```bash
javap -c -p -classpath build/money/classes bounded.generated.Bounded_Money
```

Try a value declaration that exceeds its function ceiling:

```bash
java -jar target/bounded-0.0.1.jar examples/invalid-money.bounded build/invalid-money
```

It reports `BND1001` and produces no output artifacts.

## Read more

- [Language specification](docs/Language-Spec-v0.0.2.md): intended semantics and open design questions.
- [Compiler walkthrough](docs/WALKTHROUGH.md): source text, semantic checking, and JVM output.
- [Architecture diagrams](docs/architecture.drawio): the compiler's ports and adapters, plus draft language-design views.
- [Acceptance testing roadmap](docs/Acceptance-Testing-Roadmap.md): implemented test support and planned execution tests.
- [Working on Bounded](AGENTS.md): contributor conventions and verification commands.
