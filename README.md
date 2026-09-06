# Bounded compiler: first vertical slice

This is a deliberately tiny but end-to-end compiler for the first Bounded slice:

- one source file;
- one ArcheClass declaration;
- `value` and `entity` ArcheClasses;
- `function` and `query` ArcheOperations without bodies;
- values may declare functions but not queries;
- stable syntax (`BND0001`), effect (`BND1001`), and backend (`BND9001`) diagnostics;
- valid input is lowered to Java source and then compiled to a real JVM `.class` file.

The generated class is only an **interoperable metadata shell**. Because this slice has no
operation bodies or `main`, it cannot yet perform useful work. That is intentional: the example
shows the complete compiler plumbing without prematurely deciding how Bounded bodies lower to Java.

## Requirements

- JDK 21 (a JDK, not only a JRE, because the compiler uses the Java Compiler API)
- Maven 3.9+

## Build and test

```bash
mvn clean verify
```

The tests demonstrate three levels:

1. `EffectCheckerTest` constructs the AST by hand and tests semantic rules directly.
2. `BoundedParserFacadeTest` proves that ANTLR source becomes the intended AST with source spans.
3. `FixtureCompilationTest` compiles real `.bounded` fixtures, compares a stable `.diag` file,
   generates Java, invokes `javac`, loads the resulting class, and calls it reflectively.

## Run the compiler

Package the executable compiler JAR:

```bash
mvn clean package
```

Compile the valid example:

```bash
java -jar target/bounded-first-slice-0.1.0-SNAPSHOT.jar \
  examples/money.bounded \
  build/money
```

The important outputs are:

```text
build/money/generated-sources/bounded/generated/Bounded_Money.java
build/money/classes/bounded/generated/Bounded_Money.class
```

Inspect the generated JVM class:

```bash
javap -classpath build/money/classes bounded.generated.Bounded_Money
```

Try the semantic failure:

```bash
java -jar target/bounded-first-slice-0.1.0-SNAPSHOT.jar \
  examples/invalid-money.bounded \
  build/invalid-money
```

It reports `BND1001` and deliberately produces no Java or class file.

## Compiler pipeline

```text
.bounded characters
        |
        v
ANTLR lexer/parser -> parse tree
        |
        v
BoundedAstMapper -> syntax AST
        |
        v
EffectChecker -> diagnostics or valid AST
        |
        v
JavaSourceBackend -> generated .java
        |
        v
JavaClassCompiler -> javac -> generated .class
```

The AST objects are ordinary in-memory Java data used by the compiler. They are **not** the
objects that `javac` compiles. `JavaSourceBackend` is the explicit bridge from the validated
Bounded model to Java source, and `JavaClassCompiler` is the bridge from that source to bytecode.

## Deliberate simplifications

- No operation parameters, return types, bodies, calls, imports, ports, or architecture declarations.
- No name/type resolution is needed because the slice contains no references.
- The backend emits metadata methods rather than pretending bodyless ArcheOperations have runtime behavior.
- The CLI lives in this module. It can be split out after the compiler API earns a stable shape.
- The generated-Java backend is replaceable; a later bytecode backend can consume the same validated model.
