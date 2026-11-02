# Working on Bounded

These instructions apply throughout this repository. Bounded is a Java 25 compiler with
an ANTLR frontend and a backend that emits JVM class files directly through the JDK
Class-File API. Read the relevant code and tests before making changes.

## Architecture

Preserve the ports and adapters architecture and inward dependency direction:

| Package under `dev.bounded.compiler` | Responsibility |
| --- | --- |
| `ast` | Language model, declaration invariants, and operation permissions. |
| `diagnostics` | Diagnostic codes, messages, and source spans; may depend on `ast`. |
| `api` | Application policy in `BoundedCompiler`, inbound `CompileProgram`, outbound ports, and their boundary types. Depends on the inner model and JDK. |
| `parse` | Implements `api.parse.Parser` using generated ANTLR classes and AST mapping. |
| `semantics` | Implements `api.check.SemanticChecker` through `EffectChecker`. |
| `backend` | Implements `api.generate.CodeGenerator` through `JvmBackend` and `api.write.ArtifactWriter` through `ClassFileWriter`. |
| `cli` | Driving adapter and composition root. `Main` selects implementations and wires the compiler; `BoundedCli` uses `CompileProgram`. |
| `grammar` | Generated ANTLR lexer and parser in production; handwritten grammar testing DSL in test sources. |

- Keep port interfaces and their result types in `api`. Keep `ast` and `diagnostics`
  independent of the API and adapters.
- Keep adapter components independent and package dependencies acyclic. Only `cli.Main`
  selects adapter implementations from outside their component.
- Confine ANTLR dependencies to parsing and generated grammar, the Class-File API to
  `backend`, and filesystem access to `cli` and `backend`.
- Preserve constructor injection and the pipeline's early exits on invalid results.
- Treat `CompilerArchitectureTest` as executable architecture policy. New production
  packages must be explicitly incorporated into its rules while preserving these boundaries.

The implemented language currently supports one `value` or `entity` with bodyless
`function` and `query` declarations. The language specification is a discussion draft
covering a broader design. Read it before language changes, and distinguish proposed
features from behavior implemented in the grammar and tests.

## Important files

| File or directory | Use |
| --- | --- |
| [README.md](README.md) and [docs/WALKTHROUGH.md](docs/WALKTHROUGH.md) | Build instructions, implemented scope, and a compiler walkthrough. |
| [docs/Language-Spec-v0.0.1.md](docs/Language-Spec-v0.0.1.md) | Language design and intended semantics. |
| [docs/architecture.drawio](docs/architecture.drawio) | Current architecture is tab `4. Ports and adapters`; earlier tabs are historical. |
| [pom.xml](pom.xml) | Java release, dependency versions, ANTLR generation, tests, PMD, PIT, and packaging. |
| [Bounded.g4](src/main/antlr4/dev/bounded/compiler/grammar/Bounded.g4) | Source of truth for the implemented lexer and grammar. |
| [BoundedCompiler.java](src/main/java/dev/bounded/compiler/api/BoundedCompiler.java) | Application orchestration through ports. |
| [Main.java](src/main/java/dev/bounded/compiler/cli/Main.java) | Production composition root and executable entry point. |
| [CompilerArchitectureTest.java](src/test/java/dev/bounded/compiler/architecture/CompilerArchitectureTest.java) | ArchUnit dependency and implementation-boundary checks. |
| [FixtureCompilationTest.java](src/test/java/dev/bounded/compiler/cli/FixtureCompilationTest.java) and [cases/](src/test/resources/cases/) | Functional compiler tests, source examples, and expected diagnostics. |
| [grammar test package](src/test/java/dev/bounded/compiler/grammar/) | Lexer, complete-program, and grammar-rule tests with facades, result records, and Hamcrest matchers. |
| [pmd/pmd-ruleset.xml](pmd/pmd-ruleset.xml) | Maintainability rules for handwritten production and test sources. |
| [pmd/pmd-production-ruleset.xml](pmd/pmd-production-ruleset.xml) | Additional coupling check for production sources. |
| [locale test package](src/test/java/dev/bounded/compiler/locale/) and [locale-sensitive-test.properties](src/test/resources/locale-sensitive-test.properties) | Locale test extension and configured locale coverage. |

ANTLR generates Java sources under `target/generated-sources/antlr4`. Change `Bounded.g4`
and regenerate; never hand-edit or commit generated files under `target/`.

## Verification

Run commands from the repository root using JDK 25 and Maven 3.9 or newer. Check
`mvn --version`; the IDE SDK and Maven runtime can differ. Set `JAVA_HOME` to JDK 25.

| Purpose | Command |
| --- | --- |
| Full clean verification: all tests, ArchUnit, compiler fixtures, PMD, and PIT | `mvn clean verify` |
| All Maven-discovered JUnit tests, including ArchUnit and compiler fixtures | `mvn test` |
| Architecture and functional compiler tests | `mvn -Dtest=CompilerArchitectureTest,FixtureCompilationTest test` |
| Lexer and grammar tests | `mvn -Dtest=BoundedLexerTest,BoundedGrammarTest test` |
| One test class | `mvn -Dtest=BoundedAstMapperTest test` |
| PMD checks | `mvn test-compile pmd:check` |
| Mutation testing | `mvn test-compile org.pitest:pitest-maven:mutationCoverage` |
| Regenerate ANTLR sources for the IDE | `mvn generate-sources` |

On macOS with IntelliJ IDEA installed at its standard location, this fallback works when
`mvn` is not on `PATH`:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
export PATH="$JAVA_HOME/bin:/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin:$PATH"
mvn --version
mvn clean verify
```

Use focused checks while iterating. Before declaring compiler, test, or build changes
complete, run `mvn clean verify`. A passing `mvn test` or IDE run does not establish that
PMD and PIT pass. For documentation-only changes, check paths and command accuracy;
do not claim a test run that did not happen.

Inspect `target/surefire-reports/` for test results, `target/pmd.xml` and
`target/reports/pmd.html` for PMD findings, and `target/pit-reports/index.html` or
`target/pit-reports/mutations.xml` for mutation results. Report commands actually run,
their outcomes, and any remaining failures or verification limitations.

## Automated checks must not be weakened

**Never relax automated checks to make a build pass. Fix the underlying problem.**

- Do not lower the PIT mutation threshold, currently **95%**, reduce its **ALL** mutator
  set, narrow mutation or test targets, or add exclusions to improve the reported score.
- Do not raise PMD thresholds, remove rules, broaden allowed types or exclusions, add
  suppressions to silence findings, or ignore analysis/configuration errors.
- Do not weaken ArchUnit rules, omit production packages from analysis, or introduce
  dependency exceptions to accommodate an architectural violation.
- Do not disable, delete, skip, or filter out failing tests; reduce locale coverage;
  weaken assertions; or rewrite expected diagnostics merely to match incorrect behavior.
  Update expectations only when implementing an intentional change to the contract.
- Do not bypass checks with skip flags, command-line threshold overrides, profiles,
  IDE-specific test selection, or changes to Maven lifecycle bindings. Focused runs
  are diagnostic steps, not substitutes for full verification.
- Existing exceptions are narrowly scoped: generated ANTLR sources are excluded from
  PMD and PIT, the coupling-count rule excludes tests, two ANTLR `syntaxError` overrides
  suppress the parameter-count rule, and `LooseCoupling` permits ArchUnit's `JavaClasses`
  alongside PMD's default `Properties` allowance. Do not expand these exceptions.

If a check cannot be made to pass, report the exact failure and its cause. Do not hide
the failure or describe partial verification as a successful build.

## Test and implementation conventions

- Develop behavior in small increments. Use JUnit `@DisplayName`, expressive parameterized
  invocation names, and Hamcrest assertions. Give `@Nested` test groups display names.
- Keep each test focused on one aspect of behavior. Several assertions may be needed
  to express that aspect. Reuse the existing grammar DSL and extract cohesive fixtures
  and matchers when they make tests easier to read.
- Preserve test boundaries: lexer and grammar tests exercise generated ANTLR behavior;
  mapper unit tests use constructed contexts; semantic and backend unit tests use
  hand-built ASTs; orchestration tests supply ports; functional fixtures run the real
  compiler assembled by `Main`.
- Preserve construction invariants and defensive copies of collections and arrays.
  Add meaningful regression coverage when changing those contracts.
- Prefer locale-independent operations and explicit character sets. Keep the compiler
  fixtures' locale coverage. When using `@LocaleSensitiveTest`, provide an `@Isolated`
  containing test class and do not combine it with `@Test` or `@ParameterizedTest`.
