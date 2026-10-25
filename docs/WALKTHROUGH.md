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

For this slice, a value may declare functions but not queries. An entity may declare
either a function or a query.

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
                new SourceSpan(3, 3))),
        new SourceSpan(1, 1)));
```

At this point, no parser is involved. The Java expression says exactly what the compiler
should eventually learn from the source text.

`ArcheClassKind` owns the maximum operation kind it permits:

```java
VALUE(ArcheOperationKind.FUNCTION, "Value"),
ENTITY(ArcheOperationKind.QUERY, "Entity");
```

`ArcheOperationKind` exposes its numeric effect grade through `effectGrade()`:

```java
FUNCTION(0, "Function"),
QUERY(1, "Query");
```

`ArcheClassKind.allows(operationKind)` compares the requested operation's grade with
its ceiling. The checker asks that domain question without comparing grades itself.

## 3. Test the semantic checker before parsing

`EffectChecker` asks whether the ArcheClass kind permits each operation kind:

```java
if (!archeClass.kind().allows(operation.kind())) {
    diagnostics.add(...);
}
```

`EffectCheckerTest` constructs the AST by hand and proves three facts:

- a value may declare a function;
- a value may not declare a query;
- an entity may declare a query.

The failure is data rather than an exception:

```text
BND1001:3:3 Value 'Money' permits operations through FUNCTION; 'exchangeRate' is QUERY.
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
new SourceSpan(token.getLine(), token.getCharPositionInLine() + 1)
```

That source span is why the semantic checker can report `3:3` for the invalid query even though
the checker knows nothing about ANTLR or source text.

## 6. Prove the real-source path with fixtures

The fixtures under `src/test/resources/cases` exercise more plumbing than the hand-built test:

```text
value-query.bounded
        |
        v
lexer -> parser -> mapper -> EffectChecker
        |
        v
actual formatted diagnostics
        |
        v
compare with value-query.diag
```

The valid fixture continues into the backend. The invalid fixture asserts that no output
exists. A semantically invalid Bounded program must stop before bytecode generation.

## 7. Cross the backend boundary into JVM bytecode

`JvmBackend` translates the validated AST into class bytes using the JDK 25 Class-File API.
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
enum name as a string constant and returns it.

The operations method creates an array of `Map.Entry` objects, fills it with operation names
and kinds, and invokes `Map.ofEntries`. An empty archeclass uses `Map.of`. The backend also
emits the generic signature so reflection sees `Map<String, String>` rather than a raw map.

`GeneratedClass` pairs the qualified class name with the resulting byte array. Both its
constructor and bytecode accessor copy the array so callers cannot change the stored result.

## 8. Write and verify the class file

`ClassFileWriter` creates package directories under `classes` and writes the bytecode.
It returns the path of the generated artifact:

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

The end-to-end test creates an isolated `URLClassLoader`, loads
`bounded.generated.Bounded_Money`, and invokes its metadata methods with reflection. That proves
the result is not merely a file bearing a `.class` extension; it is a JVM-loadable class.

## 9. `CompilationResult` reports both meanings and artifacts

`BoundedCompiler` coordinates the stages and stops at the first failed boundary:

```text
syntax diagnostics?   -> stop; no artifacts
semantic diagnostics? -> stop; no artifacts
backend diagnostics?  -> report BND9001; no artifacts
otherwise              -> write and return generated .class paths
```

The public result is deliberately modest:

```java
public record CompilationResult(
        List<Diagnostic> diagnostics,
        List<Path> generatedClassFiles
) {}
```

The result defensively copies both lists. A Class-File API generation failure is reported at
the original archeclass's span; it has no generated-Java location. Filesystem failures propagate
as `IOException`. The CLI prints diagnostics or the paths of generated classes.

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
