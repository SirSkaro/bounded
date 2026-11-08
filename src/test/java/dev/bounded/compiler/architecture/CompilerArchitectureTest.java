package dev.bounded.compiler.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.bounded.compiler.api.BoundedCompiler;
import dev.bounded.compiler.cli.Main;
import dev.bounded.compiler.diagnostics.TextDiagnosticFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@DisplayName("Compiler architecture")
class CompilerArchitectureTest {
    private static final String ROOT = "dev.bounded.compiler";
    private static final String API = ROOT + ".api..";
    private static final String AST = ROOT + ".ast..";
    private static final String DIAGNOSTICS = ROOT + ".diagnostics..";
    private static final String PARSE = ROOT + ".parse..";
    private static final String GRAMMAR = ROOT + ".grammar..";
    private static final String SEMANTICS = ROOT + ".semantics..";
    private static final String BACKEND = ROOT + ".backend..";
    private static final String CLI = ROOT + ".cli..";

    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    @DisplayName("Production dependencies point toward the API and inner model")
    void enforcesDependencyDirection() {
        layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage(ROOT + "..")
                .layer("AST").definedBy(AST)
                .layer("Diagnostics").definedBy(DIAGNOSTICS)
                .layer("API").definedBy(API)
                .layer("Parser").definedBy(PARSE)
                .layer("Grammar").definedBy(GRAMMAR)
                .layer("Semantics").definedBy(SEMANTICS)
                .layer("Backend").definedBy(BACKEND)
                .layer("CLI").definedBy(CLI)
                .whereLayer("AST").mayNotAccessAnyLayer()
                .whereLayer("Diagnostics").mayOnlyAccessLayers("AST")
                .whereLayer("API").mayOnlyAccessLayers("AST", "Diagnostics")
                .whereLayer("Grammar").mayNotAccessAnyLayer()
                .whereLayer("Parser").mayOnlyAccessLayers("API", "AST", "Diagnostics", "Grammar")
                .whereLayer("Semantics").mayOnlyAccessLayers("API", "AST", "Diagnostics")
                .whereLayer("Backend").mayOnlyAccessLayers("API", "AST", "Diagnostics")
                .whereLayer("CLI").mayOnlyAccessLayers("API", "Diagnostics", "Parser", "Semantics", "Backend")
                .ensureAllClassesAreContainedInArchitecture()
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("Compiler components have no dependency cycles")
    void preventsComponentCycles() {
        slices().matching(ROOT + ".(*)..")
                .should().beFreeOfCycles()
                .check(PRODUCTION);
    }

    @DisplayName("Only the composition root selects adapter implementations")
    @ParameterizedTest(name = "Only Main may depend on {0} from outside that component")
    @ValueSource(strings = {PARSE, SEMANTICS, BACKEND})
    void confinesAdapterSelectionToMain(String component) {
        noClasses().that().resideOutsideOfPackages(component)
                .and().doNotHaveFullyQualifiedName(Main.class.getName())
                .should().dependOnClassesThat().resideInAPackage(component)
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("The driving CLI uses the inbound port instead of BoundedCompiler")
    void keepsCompilerSelectionInMain() {
        noClasses().that().resideInAPackage(CLI)
                .and().doNotHaveFullyQualifiedName(Main.class.getName())
                .should().dependOnClassesThat().haveFullyQualifiedName(BoundedCompiler.class.getName())
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("Port return contracts belong to the API")
    void keepsPortResultsInApi() {
        methods().that().areDeclaredInClassesThat().resideInAPackage(API)
                .and().areDeclaredInClassesThat().areInterfaces()
                .should().haveRawReturnType(resideInAPackage(API))
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("The API and inner model require only their own types and the JDK")
    void keepsCoreIndependentOfFrameworks() {
        classes().that().resideInAnyPackage(API, AST, DIAGNOSTICS, SEMANTICS)
                .should().onlyDependOnClassesThat().resideInAnyPackage(API, AST, DIAGNOSTICS, SEMANTICS, "java..")
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("ANTLR is confined to parsing and generated grammar")
    void confinesAntlrToParsing() {
        noClasses().that().resideOutsideOfPackages(PARSE, GRAMMAR)
                .should().dependOnClassesThat().resideInAPackage("org.antlr..")
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("The Class-File API is confined to the backend")
    void confinesClassFileApiToBackend() {
        noClasses().that().resideOutsideOfPackages(BACKEND)
                .should().dependOnClassesThat().resideInAnyPackage("java.lang.classfile..", "java.lang.constant..")
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("Filesystem operations stay in the CLI and backend")
    void confinesFilesystemAccessToAdapters() {
        noClasses().that().resideOutsideOfPackages(CLI, BACKEND)
                .should().dependOnClassesThat().haveFullyQualifiedName(Files.class.getName())
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("Diagnostic text presentation stays outside compiler policy and model")
    void confinesDiagnosticFormattingToPresentation() {
        noClasses().that().resideOutsideOfPackages(CLI)
                .and().doNotHaveFullyQualifiedName(TextDiagnosticFormatter.class.getName())
                .should().dependOnClassesThat().haveFullyQualifiedName(TextDiagnosticFormatter.class.getName())
                .check(PRODUCTION);
    }
}
