# Bounded compiler: first vertical slice

This is a deliberately tiny but end-to-end compiler for the first Bounded slice:

- one source file;
- one ArcheClass declaration;
- `value` and `entity` ArcheClasses;
- `function` and `query` ArcheOperations without bodies;
- values may declare functions but not queries;
- stable syntax (`BND0001`), effect (`BND1001`), and backend (`BND9001`) diagnostics;
- valid input is lowered directly to a JVM `.class` file using the JDK Class-File API.

The generated class is only an **interoperable metadata shell**. Because this slice has no
operation bodies or `main`, it cannot yet perform useful work. That is intentional: the example
shows the complete compiler plumbing without prematurely deciding how Bounded bodies lower to JVM instructions.

## Requirements

- JDK 25
- Maven 3.9+

## Build and test

```bash
mvn clean verify
```

Check `mvn --version` to confirm Maven uses JDK 25; the IDE's project SDK and Maven's
runtime can differ. The compiler targets Java 25, and emitted classes explicitly target
class-file version 69.0. No preview flags or external bytecode libraries are needed.

For incremental grammar development, add an accepted or rejected source example to
`BoundedGrammarTest`, then update `Bounded.g4` and run:

```bash
mvn -Dtest=BoundedGrammarTest test
```

These tests use the generated lexer and parser directly, checking syntax acceptance
and parse-tree structure without running AST mapping or the compiler pipeline.

The tests exercise several boundaries:

1. `BoundedGrammarTest` tests the language defined by `Bounded.g4` using source examples.
2. `BoundedParserFacadeTest` proves that ANTLR source becomes the intended AST with source spans.
3. `EffectCheckerTest` constructs the AST by hand and tests semantic rules directly.
4. `JvmBackendTest` constructs ASTs directly, verifies emitted bytecode with the Class-File API,
   and checks class structure and behavior through an isolated class loader.
5. `BoundedCompilerTest` supplies port implementations to test ordering, data flow, and early exits
   without invoking the parser, backend, or filesystem.
6. `FixtureCompilationTest` compiles real `.bounded` fixtures, compares a stable `.diag` file,
   loads the resulting class, and calls it reflectively across the configured locales.

`BoundedCliTest` checks source loading, output, and exit statuses through a supplied `CompileProgram`.
`CompilerArchitectureTest` uses ArchUnit to enforce the production dependency boundaries.

## Complexity checks

PMD runs during `mvn verify` using the root-level [`pmd-ruleset.xml`](pmd-ruleset.xml).
It analyzes handwritten production and test Java sources, excluding ANTLR-generated sources.
The rules use PMD's default reporting thresholds:

| Rule | Reports at or above |
| --- | --- |
| [Cyclomatic complexity](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#cyclomaticcomplexity) | 10 per method or 80 per class |
| [Cognitive complexity](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#cognitivecomplexity) | 15 per method |

Run the complexity check without running tests:

```bash
mvn test-compile pmd:check
```

Violations fail the check and are printed in the Maven output. PMD also writes `target/pmd.xml`
and an HTML report at `target/reports/pmd.html`. The initial setup adds the checks without
refactoring existing code to satisfy them.

## Locale-sensitive tests

Use `@LocaleSensitiveTest` from `dev.bounded.compiler.locale` instead of `@Test`
for behavior that must stay consistent across default locales. The containing
class must have `@Isolated`; nested tests can inherit isolation from an enclosing
test container. The extension rejects missing isolation.

```java
@Isolated
class KeywordTest {
    @LocaleSensitiveTest
    @DisplayName("Recognizes entity independently of the default locale")
    void recognizesEntity() {
        assertThat(ArcheClassKind.fromKeyword("entity"), is(ArcheClassKind.ENTITY));
    }
}
```

The global list lives in `src/test/resources/locale-sensitive-test.properties`:

```properties
locales=ROOT,tr-TR,ar-EG,de-DE
```

`ROOT` denotes `Locale.ROOT`; other entries are BCP 47 language tags.
`@LocaleSensitiveTest({"ROOT", "tr-TR"})` replaces the global list for that method.
Each locale appears as a separate invocation in the test runner.

The locale is set before `@BeforeEach` and restored after `@AfterEach`, including
when setup, assertions, or teardown fail. Default, display, and format locales
are restored independently. Constructors and `@BeforeAll` run outside this
per-invocation locale scope. Invocations run sequentially.

Do not combine `@LocaleSensitiveTest` with `@Test` or `@ParameterizedTest` on the
same method. Ordinary and parameterized tests can coexist elsewhere in the class.

## Run the compiler

Package the executable compiler JAR:

```bash
mvn clean package
```

Compile the valid example:

```bash
java -jar target/bounded-0.0.1.jar \
  examples/money.bounded \
  build/money
```

The generated output is:

```text
build/money/classes/bounded/generated/Bounded_Money.class
```

Inspect the generated JVM class:

```bash
javap -c -p -classpath build/money/classes bounded.generated.Bounded_Money
```

Try the semantic failure:

```bash
java -jar target/bounded-0.0.1.jar \
  examples/invalid-money.bounded \
  build/invalid-money
```

It reports `BND1001` and produces no output artifacts.

## Architecture

The fourth tab of [`docs/architecture.drawio`](docs/architecture.drawio) shows the implemented
ports and adapters architecture. The first three tabs preserve the structure before this refactor.

`api` owns the application policy, port interfaces, and boundary data. `BoundedCompiler`
implements the inbound `CompileProgram` port and receives four outbound ports in its constructor:

| Port | API-owned result | Implementation |
| --- | --- | --- |
| `api.CompileProgram` | `api.CompilationResult` | `api.BoundedCompiler` |
| `api.parse.Parser` | `api.parse.ParseResult` | `parse.BoundedParserFacade` |
| `api.check.SemanticChecker` | `api.check.CheckResult` | `semantics.EffectChecker` |
| `api.generate.CodeGenerator` | `api.generate.GenerationResult` | `backend.JvmBackend` |
| `api.write.ArtifactWriter` | `api.write.WriteResult` | `backend.ClassFileWriter` |

`CompilationRequest` holds the `SourceFile` and an `api.write.OutputTarget`. Generated class names
and bytes belong to `api.generate.GeneratedClass`. These contracts expose no ANTLR or Class-File
API types. `ast` and `diagnostics` form the inner model and depend on neither the API nor adapters.
Implementations use that model where needed; the writer only consumes API artifact contracts.

`cli.Main` is the executable entry point and composition root. It constructs the implementations
and injects them into `BoundedCompiler`. `BoundedCli` receives only the inbound port and output
streams. The fixture tests live in the `cli` test package so they can use the same assembly as `Main`.

With implementations of the four ports, a caller can assemble a compiler explicitly:

```java
CompileProgram compiler = new BoundedCompiler(parser, checker, generator, writer);
var request = new CompilationRequest(sourceFile, new OutputTarget(outputDirectory));
CompilationResult result = compiler.compile(request);
```

The [ArchUnit](https://www.archunit.org/userguide/html/000_Index.html) checks run as ordinary JUnit
tests in Maven and the IDE. They check production classes, including generated grammar, while
excluding test code. They enforce inward dependencies, independent adapter components, API-owned
port results, composition in `Main`, and absence of cycles between compiler components. They also
confine ANTLR, the Class-File API, and `Files` access to the appropriate implementations. New production
packages must be included explicitly in the architecture rules.

Run the architecture and compiler fixtures together:

```bash
mvn -Dtest=CompilerArchitectureTest,FixtureCompilationTest test
```

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
JvmBackend -> GenerationResult containing GeneratedClass
        |
        v
ClassFileWriter -> generated .class and WriteResult
```

`JvmBackend` translates the validated Bounded AST into class bytes in memory. `GeneratedClass`
defensively copies those bytes, and `ClassFileWriter` writes them under the output directory's
`classes` tree. Compilation produces no intermediate Java source and does not invoke `javac`.

`CompilationResult` contains diagnostics and generated class paths, both as unmodifiable lists.
The backend translates bytecode generation failures into `BND9001` diagnostics at the original
archeclass's source span. `BoundedCompiler` stops on any stage's diagnostics without knowing the
implementation that produced them. Filesystem failures propagate as `IOException`.

## Deliberate simplifications

- No operation parameters, return types, bodies, calls, imports, ports, or architecture declarations.
- No name/type resolution is needed because the slice contains no references.
- The backend emits metadata methods rather than pretending bodyless ArcheOperations have runtime behavior.
- The CLI lives in this module. It can be split out after the compiler API earns a stable shape.
- The backend consumes the validated model independently of parsing and effect checking.
