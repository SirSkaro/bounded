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

For this slice, the rule is: a value has maximum effect grade `FUNCTION`, so a `QUERY`
exceeds its ceiling. An entity has maximum effect grade `QUERY`, so it may declare either
a function or a query.

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

`ArcheClassKind` owns its maximum effect:

```java
VALUE(EffectGrade.FUNCTION),
ENTITY(EffectGrade.QUERY);
```

`ArcheOperationKind` owns its required effect:

```java
FUNCTION(EffectGrade.FUNCTION),
QUERY(EffectGrade.QUERY);
```

That gives the checker a general comparison instead of a special-case conditional such as
`if value and query`. When `COMMAND` and `ACTION` arrive, the same relation can grow with them.

## 3. Test the semantic checker before parsing

`EffectChecker` compares each operation's required grade with its ArcheClass ceiling:

```java
if (!maximumEffect.allows(operation.kind().effectGrade())) {
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

The valid fixture continues into the backend. The invalid fixture asserts that no generated
source or class output exists. A semantically invalid Bounded program must never reach `javac`.

## 7. Cross the backend boundary into Java source

The Java AST objects still exist only inside the running compiler. `JavaSourceBackend` performs
the missing translation. For this slice it converts the validated `Money` declaration into:

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

## 8. Invoke `javac` through the Java Compiler API

`JavaClassCompiler` obtains the compiler bundled with the running JDK:

```java
var compiler = ToolProvider.getSystemJavaCompiler();
```

It writes the generated source under `generated-sources`, supplies that file to `javac`, and
sets the class output directory with `-d`:

```java
var task = compiler.getTask(
        null,
        fileManager,
        compilerDiagnostics,
        options,
        null,
        compilationUnits);

task.call();
```

The resulting files are:

```text
generated-sources/bounded/generated/Bounded_Money.java
classes/bounded/generated/Bounded_Money.class
```

The `.class` file begins with the four-byte JVM class-file magic number:

```text
CA FE BA BE
```

The end-to-end test then creates a `URLClassLoader`, loads
`bounded.generated.Bounded_Money`, and invokes its metadata methods with reflection. That proves
the result is not merely a file bearing a `.class` extension; it is a JVM-loadable class.

## 9. `CompilationResult` reports both meanings and artifacts

`BoundedCompiler` coordinates the stages and stops at the first failed boundary:

```text
syntax diagnostics?   -> stop; no artifacts
semantic diagnostics? -> stop; no artifacts
backend diagnostics?  -> report generated-Java/javac failure
otherwise              -> return generated .java and .class paths
```

The public result is deliberately modest:

```java
public record CompilationResult(
        List<Diagnostic> diagnostics,
        List<Path> generatedSourceFiles,
        List<Path> generatedClassFiles
) {}
```

A later compiler will likely carry more structured phase results internally. This is enough for
the CLI, tests, and eventually a Maven plugin without committing to a large public API now.

## What the slice teaches without deciding

This project proves that the architecture works:

- language concepts can be explored as Java types with fast unit tests;
- the grammar can remain a replaceable input adapter;
- stable diagnostics can be tested as compiler output;
- invalid programs stop before code generation;
- a validated model can be lowered to generated Java;
- the JDK can turn that Java into interoperable bytecode.

It intentionally leaves the difficult next questions visible rather than answering them by
accident. The next meaningful slice is likely operation bodies plus operation calls, because that
would let Bounded enforce the ArcheOperation call hierarchy and generate actual executable method
bodies.
