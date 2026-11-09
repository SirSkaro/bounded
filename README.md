# Bounded compiler: first vertical slice

This is a deliberately tiny but end-to-end compiler for the first Bounded slice:

- one source file;
- one ArcheClass declaration;
- `value` and `entity` ArcheClasses;
- `function`, `query`, and `command` ArcheOperations without bodies;
- values may declare functions; entities may declare functions, queries, and commands;
- unique, case-sensitive operation names within each archeclass;
- stable syntax (`BND0001`), effect (`BND1001`), duplicate-name (`BND1002`), and backend (`BND9001`) diagnostics;
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
3. `DeclarationCheckerTest`, `EffectCheckerTest`, and `ProgramCheckerTest` use hand-built ASTs
   to test individual semantic rules and their composition.
4. `JvmBackendTest` constructs ASTs directly, verifies emitted bytecode with the Class-File API,
   and checks class structure and behavior through an isolated class loader.
5. `BoundedCompilerTest` supplies port implementations to test ordering, data flow, and early exits
   without invoking the parser, backend, or filesystem.
6. `FixtureCompilationTest` compiles real `.bounded` fixtures, compares a stable `.diag` file,
   loads the resulting class, and calls it reflectively across the configured locales.

`BoundedCliTest` checks source loading, output, and exit statuses through a supplied `CompileProgram`.
`CompilerArchitectureTest` uses ArchUnit to enforce the production dependency boundaries.

The [acceptance testing roadmap](docs/Acceptance-Testing-Roadmap.md) describes the planned
DSL for compilation, diagnostics, and execution of Bounded programs. Stage 1 is implemented
in the test `acceptance` package; fixture tests use facades, result records, and Hamcrest
matchers to compile, reject, and load declaration metadata. Operation execution is deferred.

## Maintainability checks

PMD runs during `mvn verify` using two rulesets in `pmd/`:

- [`pmd-ruleset.xml`](pmd/pmd-ruleset.xml) checks handwritten production and test Java sources.
- [`pmd-production-ruleset.xml`](pmd/pmd-production-ruleset.xml) adds a coupling limit for production
  classes. Tests are excluded from this rule because fixtures often bring several components together.

ANTLR-generated sources are excluded from both rulesets. The design checks use these thresholds:

| Rule | Reporting threshold |
| --- | --- |
| [Cyclomatic complexity](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#cyclomaticcomplexity) | At least 10 per method or 80 per class |
| [Cognitive complexity](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#cognitivecomplexity) | At least 15 per method |
| [NCSS count](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#ncsscount) | At least 30 statements per method or 300 per class |
| [NPath complexity](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#npathcomplexity) | At least 200 execution paths |
| [Excessive parameter list](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#excessiveparameterlist) | More than 5 parameters |
| [Coupling between objects](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#couplingbetweenobjects) | More than 20 distinct types; production only |
| [God class](https://docs.pmd-code.org/pmd-doc-7.17.0/pmd_rules_java_design.html#godclass) | PMD's combined complexity, cohesion, and foreign-data heuristic |

The rules also detect unused imports, variables, private fields and methods, private parameters,
and assignments; protect array encapsulation; encourage final fields, collection interfaces,
and explicit overrides; and check exception handling, resource cleanup, string comparisons,
ignored pure-method results, locales, and character sets.

The parameter limit is suppressed only on the two ANTLR `syntaxError` overrides, whose six-argument
signature is required by ANTLR. The collection-interface rule permits ArchUnit's `JavaClasses`,
which its rule-checking API requires, alongside PMD's default allowance for `Properties`.

Run the maintainability checks without running tests:

```bash
mvn test-compile pmd:check
```

Violations fail the check and are printed in the Maven output. PMD also writes `target/pmd.xml`
and an HTML report at `target/reports/pmd.html`.

## Mutation testing

[PIT](https://pitest.org/quickstart/maven/) runs during `mvn verify` with its `ALL`
mutators. It mutates handwritten production classes, excluding the ANTLR-generated grammar
package, and uses the existing JUnit tests, including the compiler fixtures.

The `pitest.mutationThreshold` property in [`pom.xml`](pom.xml) sets **95%** as the minimum
mutation score. A lower score fails the build. PIT compares scores to two decimal places,
and mutations without test coverage count against the score.

Run mutation testing on its own:

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Open `target/pit-reports/index.html` to see surviving mutations and the tests that killed
others. PIT also writes `target/pit-reports/mutations.xml` for automated analysis.

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

The `Ports and adapters` tab of [`docs/architecture.drawio`](docs/architecture.drawio) shows
the current production architecture, including port ownership and dependency direction.

`api` owns the application policy, port interfaces, and boundary data. `BoundedCompiler`
implements the inbound `CompileProgram` port and receives four outbound ports in its constructor:

| Port | API-owned result | Implementation |
| --- | --- | --- |
| `api.CompileProgram` | `api.CompilationResult` | `api.BoundedCompiler` |
| `api.parse.Parser` | `api.parse.ParseResult` | `parse.BoundedParserFacade` |
| `api.check.SemanticChecker` | `api.check.CheckResult` | `semantics.ProgramChecker` |
| `api.generate.CodeGenerator` | `api.generate.GenerationResult` | `backend.JvmBackend` |
| `api.write.ArtifactWriter` | `api.write.WriteResult` | `backend.ClassFileWriter` |

`CompilationRequest` holds the `SourceFile` and an `api.write.OutputTarget`. Generated class names
and bytes belong to `api.generate.GeneratedClass`, with a validated `ClassName` and immutable
`GeneratedProgram` collection. Successful checking retains an `ast.AnalyzedProgram` with the
original AST and declaration index. Writing returns `api.write.CompiledClass` descriptors with
binary names and class path roots. These contracts expose no ANTLR or Class-File
API types. `ast` and `diagnostics` form the inner model and depend on neither the API nor adapters.
Implementations use that model where needed; the writer only consumes API artifact contracts.

`cli.Main` is the executable entry point and composition root. It constructs the implementations
and injects them into `BoundedCompiler`. `BoundedCli` receives only the inbound port and output
streams. The fixture tests live in the `cli` test package and use a test-side bridge so the
DSL shares the same assembly as `Main`.

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
ProgramChecker -> diagnostics or AnalyzedProgram
        |
        v
JvmBackend -> GenerationResult containing GeneratedProgram
        |
        v
ClassFileWriter -> .class files and loadable descriptors
```

`JvmBackend` translates the analyzed Bounded program into class bytes in memory. `GeneratedClass`
defensively copies those bytes, and `ClassFileWriter` writes them under the output directory's
`classes` tree. Compilation produces no intermediate Java source and does not invoke `javac`.

`CompilationResult.success` requires nonempty artifact descriptors and carries no diagnostics;
`CompilationResult.failure` requires diagnostics and carries no artifacts. Both expose immutable
lists. Callers load output using each descriptor's class name and class path root.
`SourceSpan` carries the source identity, line, and column; `TextDiagnosticFormatter` renders
`source:code:line:column message` independently of diagnostic data.
The backend translates bytecode generation failures into `BND9001` diagnostics at the original
archeclass's source span. `BoundedCompiler` stops on any stage's diagnostics without knowing the
implementation that produced them. Generated names cannot contain path traversal, and the writer
rejects symbolic links beneath the requested output root. Filesystem failures propagate as
`IOException`; files written before a later failure remain in place. An unsuccessful source
compilation does not invoke the writer.

## Deliberate simplifications

- No operation parameters, return types, bodies, calls, imports, ports, or architecture declarations.
- Operation declarations are indexed and duplicate names rejected; reference and type resolution await language support.
- The backend emits metadata methods rather than pretending bodyless ArcheOperations have runtime behavior.
- The CLI lives in this module. It can be split out after the compiler API earns a stable shape.
- The backend consumes the validated model independently of parsing and effect checking.
