# Walkthrough: from `Money.bounded` to `Bounded_Money.class`

This walkthrough follows the same order in which the first vertical slice should be developed.
The class-file backend is shown last because it consumes the result of all earlier decisions.

## 1. Start with a source example and one language rule

The valid program is:

```bounded
value Money {
  function format;
}
```

The invalid counterpart is:

```bounded
value Money {
  function format;
  query exchangeRate;
}
```

For this slice, a value may declare only functions. An entity may declare functions, queries,
and commands. A use case may also declare actions, the highest supported effect grade.
Messages permit no operations, including functions:

```bounded
message TransferRequest {}
```

This message declaration exposes metadata only. Message fields, immutable data construction,
and transitive boundary-safety checks remain unimplemented.

Nothing else is being decided yet: no parameters, return types, bodies, calls, or runtime
meaning for an ArcheOperation.

## 2. Represent the idea with a hand-built AST

The relevant records and enums live under `dev.bounded.compiler.ast`:

```java
var program = new BoundedProgram(new ArcheClassDeclaration(
        ArcheClassKind.VALUE,
        "Money",
        List.of(new ArcheOperationDeclaration(
                ArcheOperationKind.QUERY,
                "exchangeRate",
                new SourceSpan("money.bounded", 3, 3))),
        new SourceSpan("money.bounded", 1, 1)));
```

At this point, no parser is involved. The Java expression says exactly what the compiler
should eventually learn from the source text.

`ArcheClassKind` owns an `OperationPermissions` policy:

```java
MESSAGE(OperationPermissions.noOperations(), "message", "MESSAGE", "Message"),
VALUE(OperationPermissions.through(ArcheOperationKind.FUNCTION), "value", "VALUE", "Value"),
ENTITY(OperationPermissions.through(ArcheOperationKind.COMMAND), "entity", "ENTITY", "Entity"),
USECASE(OperationPermissions.through(ArcheOperationKind.ACTION), "usecase", "USECASE", "Use case");
```

`ArcheOperationKind` exposes its numeric effect grade through `effectGrade()`:

```java
FUNCTION(0, "function", "FUNCTION", "Function"),
QUERY(1, "query", "QUERY", "Query"),
COMMAND(2, "command", "COMMAND", "Command"),
ACTION(3, "action", "ACTION", "Action");
```

`OperationPermissions.through(...)` builds an immutable set containing all operation kinds at or
below the specified numeric effect grade. `noOperations()` represents an empty allowed set.
`ArcheClassKind.allows(operationKind)` delegates to that policy's membership check. Its
`maximumOperationKind()` returns an `Optional`: empty for messages, or the ceiling for a policy
created through an operation kind. The absence of a ceiling has a domain meaning rather than a
numeric sentinel value.

The checker asks whether an operation is allowed without comparing grades itself. Explicit
keywords, metadata names, and human-readable labels do not depend on Java enum constant names.

## 3. Test the semantic checker before parsing

`EffectChecker` asks whether the ArcheClass kind permits each operation kind:

```java
if (!archeClass.kind().allows(operation.kind())) {
    diagnostics.add(...);
}
```

`EffectCheckerTest` constructs the AST by hand and proves these permissions:

- a message may not declare any operation, including a function;
- a value may declare a function;
- a value may not declare a query, command, or action;
- an entity may declare a function, query, or command, but not an action;
- a use case may declare any of the four operation kinds.

`ProgramChecker` implements `api.check.SemanticChecker` and composes `DeclarationChecker`
and `EffectChecker`, each returning diagnostics for its own rule. Duplicate names are rejected
with `BND1002`, regardless of operation kind. When all rules pass, the API-owned `CheckResult`
retains an `AnalyzedProgram` containing the original program and an immutable declaration index.
The failure is data rather than an exception:

```text
money.bounded:BND1001:3:3 Value 'Money' permits operations through FUNCTION; 'exchangeRate' is QUERY.
```

When the permission policy has no ceiling, the diagnostic says so explicitly:

```text
message-function.bounded:BND1001:2:5 Message 'TransferRequested' permits no operations; 'describe' is FUNCTION.
```

`BND1001` is part of the compiler's user-facing contract. The test deliberately checks the
complete formatted diagnostic so accidental changes are visible.

## 4. Add only enough grammar to construct that AST

`Bounded.g4` defines the small surface language:

```antlr
program
    : archeClass EOF
    ;

archeClass
    : archeClassKind IDENTIFIER LBRACE archeOperation* RBRACE
    ;

archeOperation
    : archeOperationKind IDENTIFIER SEMICOLON
    ;
```

ANTLR generates `BoundedLexer` and `BoundedParser` Java classes during Maven's
`generate-sources` phase. The generated parser produces an ANTLR parse tree. It does not
produce the Bounded AST and it does not enforce the effect rule.

## 5. Map the parse tree into compiler-owned objects

`BoundedAstMapper` is the boundary between ANTLR and the rest of the compiler:

```text
ANTLR ProgramContext
        |
        v
BoundedAstMapper
        |
        v
BoundedProgram
```

Keeping that translation explicit prevents ANTLR-generated context classes from leaking into
the semantic checker. If the grammar changes, the mapper absorbs much of the disruption.

The mapper also converts ANTLR's zero-based character position to a one-based `SourceSpan`:

```java
new SourceSpan(sourceName, token.getLine(), token.getCharPositionInLine() + 1)
```

That source span is why the semantic checker can report `3:3` for the invalid query even though
the checker knows nothing about ANTLR or source text.

## 6. Prove the real-source path with fixtures

The fixtures under `src/test/resources/cases` exercise more plumbing than the hand-built test:

```text
value-query.bounded
        |
        v
lexer -> parser -> mapper -> ProgramChecker
        |
        v
actual formatted diagnostics
        |
        v
compare with value-query.diag
```

Valid inline acceptance examples continue into the backend, covering operations at and below
each applicable ArcheClass ceiling. An empty message exposes `MESSAGE` metadata and no operations;
a message function is rejected with `BND1001`. The use case example exposes `USECASE` class metadata
and all four operation kinds, including `ACTION`. An entity action is rejected with `BND1001` because
it exceeds the entity's command ceiling. Invalid examples assert that no output exists. A semantically
invalid Bounded program must stop before bytecode generation.

## 7. Cross the backend boundary into JVM bytecode

`JvmBackend` translates the analyzed program into class bytes using the JDK 25 Class-File API.
For this slice, the emitted `bounded.generated.Bounded_Money` class has behavior equivalent to
the following Java. This is an illustration; the compiler does not generate this source text.

```java
package bounded.generated;

public final class Bounded_Money {
    private Bounded_Money() {}

    public static String boundedArcheClassKind() {
        return "VALUE";
    }

    public static java.util.Map<String, String> boundedOperations() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("format", "FUNCTION")
        );
    }
}
```

These methods expose metadata because the slice has no operation bodies. Generating pretend
runtime behavior here would silently make language decisions that the specification has not made.

The class builder fixes the version at Java 25 and declares a public final class extending
`Object`. The constructor invokes `Object`'s constructor. The class-kind method loads the
explicit metadata name as a string constant and returns it.

The operations method creates an array of `Map.Entry` objects, fills it with operation names
and kinds, and invokes `Map.ofEntries`. An empty archeclass uses `Map.of`. The backend also
emits the generic signature so reflection sees `Map<String, String>` rather than a raw map.

`GeneratedClass` pairs the qualified class name with the resulting byte array. Both its
constructor and bytecode accessor copy the array so callers cannot change the stored result.
It lives in `api.generate`, alongside the `CodeGenerator` port and `GenerationResult`.
`GeneratedProgram` groups one or more uniquely named classes.
`JvmBackend` returns a valid generation result containing that collection, or an invalid result with
`BND9001` diagnostics when the Class-File API rejects the generated class.

## 8. Write and verify the class file

`ClassFileWriter` implements `api.write.ArtifactWriter`. It uses the requested `OutputTarget`
to create package directories under `classes` and write the bytecode. The returned `WriteResult`
contains each artifact's validated binary name and class path root, from which its file path is derived:

```text
classes/bounded/generated/Bounded_Money.class
```

The `.class` file begins with the four-byte JVM class-file magic number:

```text
CA FE BA BE
```

Backend unit tests construct ASTs directly and check `ClassFile.of().verify(bytecode)`.
They inspect the class version and load the class to test its modifiers, constructor,
generic signature, and metadata methods. Writer tests separately check paths, exact bytes,
replacement of existing files, and filesystem failures.

The acceptance DSL owns an isolated `URLClassLoader`, loads artifacts using the API
descriptors, and invokes metadata methods behind a `CompiledProgram` facade. That proves
the result is not merely a file bearing a `.class` extension; it is a JVM-loadable class.

## 9. `CompilationResult` reports both meanings and artifacts

`BoundedCompiler` coordinates the stages and stops at the first failed boundary:

```text
syntax diagnostics?   -> stop; no artifacts
semantic diagnostics? -> stop; no artifacts
backend diagnostics?  -> report BND9001; no artifacts
otherwise              -> write and return loadable artifact descriptors
```

The result's private constructor prevents contradictory outcomes. `CompilationResult.success`
requires a nonempty list of `CompiledClass` artifacts and has no diagnostics;
`CompilationResult.failure` requires nonempty diagnostics and has no artifacts. Both lists
are immutable. Generation and writing also require at least one artifact on success.

A Class-File API generation failure is reported at the original archeclass's span, including
its source identity. Diagnostic data has no text-formatting responsibility;
`TextDiagnosticFormatter` renders it for the CLI and golden fixtures. Filesystem failures
propagate as `IOException`, retaining files completed before the failure. Validated binary
names and rejection of symbolic links beneath the output root keep generated paths within
the `classes` tree.

`BoundedCompiler` implements the inbound `CompileProgram` port and accepts a `CompilationRequest`.
Its constructor receives `Parser`, `SemanticChecker`, `CodeGenerator`, and `ArtifactWriter`, all
defined in `api` with their result contracts. It knows the stage order and failure policy, while
the concrete implementations own parsing, semantic checks, bytecode generation, and file output.

`cli.Main` constructs and connects these implementations. `BoundedCli` invokes `CompileProgram`,
so a new driving adapter can use the same application policy with its own composition root.
`FixtureCompilationTest` uses the production assembly, while `BoundedCompilerTest` supplies small
port implementations to test orchestration independently. `CompilerArchitectureTest` prevents
outward dependencies or component cycles from being reintroduced.

## What the slice teaches without deciding

This project proves that the architecture works:

- language concepts can be explored as Java types with fast unit tests;
- the grammar can remain a replaceable input adapter;
- stable diagnostics can be tested as compiler output;
- invalid programs stop before code generation;
- a validated model can be lowered directly to JVM bytecode;
- the emitted class can be verified, loaded, and called using the JDK.

It intentionally leaves the difficult next questions visible rather than answering them by
accident. The next meaningful slice is likely operation bodies plus operation calls, because that
would let Bounded enforce the ArcheOperation call hierarchy and generate actual executable method
bodies.
