package dev.bounded.compiler.backend;

import dev.bounded.compiler.api.generate.GeneratedClass;
import dev.bounded.compiler.ast.AnalyzedProgram;
import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.classfile.ClassFile;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static dev.bounded.compiler.ast.ArcheClassKind.ENTITY;
import static dev.bounded.compiler.ast.ArcheClassKind.MESSAGE;
import static dev.bounded.compiler.ast.ArcheClassKind.USECASE;
import static dev.bounded.compiler.ast.ArcheClassKind.VALUE;
import static dev.bounded.compiler.ast.ArcheOperationKind.ACTION;
import static dev.bounded.compiler.ast.ArcheOperationKind.COMMAND;
import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static dev.bounded.compiler.ast.ArcheOperationKind.QUERY;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.JVM_BACKEND_FAILURE;
import static java.lang.reflect.AccessFlag.FINAL;
import static java.lang.reflect.AccessFlag.PRIVATE;
import static java.lang.reflect.AccessFlag.PUBLIC;
import static java.lang.reflect.AccessFlag.STATIC;
import static java.lang.reflect.AccessFlag.SUPER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JvmBackendTest {
    private final JvmBackend backend = new JvmBackend();
    private final SourceSpan span = new SourceSpan("Amount.bounded", 1, 1);

    @Test
    @DisplayName("Names the generated class in the Bounded package")
    void namesGeneratedClass() {
        var generatedClass = generate(program(VALUE, List.of()));

        assertThat(generatedClass.qualifiedClassName(), is("bounded.generated.Bounded_Amount"));
    }

    @Test
    @DisplayName("Targets Java 25 regardless of the running JDK")
    void targetsJava25() {
        var generatedClass = generate(program(VALUE, List.of()));
        var model = ClassFile.of().parse(generatedClass.bytecode());

        assertThat(model.majorVersion(), is(ClassFile.JAVA_25_VERSION));
        assertThat(model.minorVersion(), is(0));
    }

    @Test
    @DisplayName("Generates a public final class")
    void declaresPublicFinalClass() {
        var generatedClass = load(program(VALUE, List.of()));

        assertThat(generatedClass.accessFlags(), containsInAnyOrder(PUBLIC, FINAL, SUPER));
    }

    @Test
    @DisplayName("Generates a working private no-argument constructor")
    void declaresPrivateConstructor() throws Exception {
        var generatedClass = load(program(VALUE, List.of()));
        var constructor = generatedClass.getDeclaredConstructor();

        assertThat(constructor.accessFlags(), containsInAnyOrder(PRIVATE));
        constructor.setAccessible(true);
        assertThat(constructor.newInstance().getClass(), is(generatedClass));
    }

    @DisplayName("Exposes public static metadata methods")
    @ParameterizedTest(name = "Exposes {0} as public static")
    @ValueSource(strings = {"boundedArcheClassKind", "boundedOperations"})
    void exposesMetadataMethods(String methodName) throws Exception {
        var generatedClass = load(program(VALUE, List.of()));

        assertThat(generatedClass.getDeclaredMethod(methodName).accessFlags(), containsInAnyOrder(PUBLIC, STATIC));
    }

    @DisplayName("Reports the declared archeclass kind")
    @ParameterizedTest(name = "Reports {1} for an archeclass of kind {0}")
    @CsvSource({"VALUE, VALUE", "ENTITY, ENTITY", "USECASE, USECASE", "MESSAGE, MESSAGE"})
    void reportsArcheClassKind(ArcheClassKind kind, String expectedKind) throws Exception {
        var generatedClass = load(program(kind, List.of()));

        assertThat(generatedClass.getMethod("boundedArcheClassKind").invoke(null), is(expectedKind));
    }

    private static Stream<Arguments> operationDeclarations() {
        var span = new SourceSpan("Amount.bounded", 2, 1);
        var function = new ArcheOperationDeclaration(FUNCTION, "calculate", span);
        var query = new ArcheOperationDeclaration(QUERY, "total", span);
        var command = new ArcheOperationDeclaration(COMMAND, "deposit", span);
        var action = new ArcheOperationDeclaration(ACTION, "commit", span);
        return Stream.of(
                Arguments.of("no operations", ENTITY, List.of(), Map.of()),
                Arguments.of("a message without operations", MESSAGE, List.of(), Map.of()),
                Arguments.of("a function", ENTITY, List.of(function), Map.of("calculate", "FUNCTION")),
                Arguments.of("a query", ENTITY, List.of(query), Map.of("total", "QUERY")),
                Arguments.of("a command", ENTITY, List.of(command), Map.of("deposit", "COMMAND")),
                Arguments.of("multiple operations", ENTITY, List.of(function, query, command),
                        Map.of("calculate", "FUNCTION", "total", "QUERY", "deposit", "COMMAND")),
                Arguments.of("an action", USECASE, List.of(action), Map.of("commit", "ACTION")),
                Arguments.of("all operation kinds", USECASE, List.of(function, query, command, action),
                        Map.of("calculate", "FUNCTION", "total", "QUERY", "deposit", "COMMAND", "commit", "ACTION"))
        );
    }

    @DisplayName("Reports the names and kinds of declared operations")
    @ParameterizedTest(name = "Reports {0}")
    @MethodSource("operationDeclarations")
    void reportsOperations(String description, ArcheClassKind kind, List<ArcheOperationDeclaration> operations,
                           Map<String, String> expectedOperations) throws Exception {
        var generatedClass = load(program(kind, operations));

        assertThat(generatedClass.getMethod("boundedOperations").invoke(null), is(expectedOperations));
    }

    @DisplayName("Generates verifiable bytecode for varying operation counts")
    @ParameterizedTest(name = "Verifies a class with {0} operations")
    @ValueSource(ints = {0, 1, 12})
    void generatesVerifiableBytecode(int operationCount) {
        var generatedClass = generate(program(VALUE, operations(operationCount)));

        assertThat(ClassFile.of().verify(generatedClass.bytecode()), is(empty()));
    }

    @Test
    @DisplayName("Supports more than ten operations")
    void supportsManyOperations() throws Exception {
        var generatedClass = load(program(VALUE, operations(12)));
        var operations = (Map<?, ?>) generatedClass.getMethod("boundedOperations").invoke(null);

        assertThat(operations.size(), is(12));
        assertThat(operations.get("operation11"), is("FUNCTION"));
    }

    @Test
    @DisplayName("Preserves the generic operation map signature")
    void preservesOperationMapSignature() throws Exception {
        var generatedClass = load(program(VALUE, List.of()));
        var returnType = (ParameterizedType) generatedClass.getMethod("boundedOperations").getGenericReturnType();

        assertThat(returnType.getRawType(), is(Map.class));
        assertThat(returnType.getActualTypeArguments(), arrayContaining(String.class, String.class));
    }

    @Test
    @DisplayName("Returned operation metadata cannot be modified")
    void exposesUnmodifiableOperations() throws Exception {
        var generatedClass = load(program(VALUE, operations(1)));
        var operations = (Map<?, ?>) generatedClass.getMethod("boundedOperations").invoke(null);

        assertThrows(UnsupportedOperationException.class, () -> operations.remove("operation0"));
    }

    private BoundedProgram program(ArcheClassKind kind, List<ArcheOperationDeclaration> operations) {
        return new BoundedProgram(new ArcheClassDeclaration(kind, "Amount", operations, span));
    }

    private List<ArcheOperationDeclaration> operations(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new ArcheOperationDeclaration(FUNCTION, "operation" + index, span))
                .toList();
    }

    private Class<?> load(BoundedProgram program) {
        return new GeneratedClassLoader().load(generate(program));
    }

    private GeneratedClass generate(BoundedProgram program) {
        var result = backend.generate(new AnalyzedProgram(program));
        assertThat(result.diagnostics(), is(empty()));
        assertThat(result.generatedProgram().classes(), hasSize(1));
        return result.generatedProgram().classes().getFirst();
    }

    @Test
    @DisplayName("Reports a Class-File API failure at the original archeclass span")
    void reportsBytecodeGenerationFailure() {
        var sourceSpan = new SourceSpan("Amount.bounded", 2, 3);
        var program = new BoundedProgram(new ArcheClassDeclaration(
                VALUE, "A".repeat(65_536), List.of(), sourceSpan));

        var result = backend.generate(new AnalyzedProgram(program));

        assertThat(result.isValid(), is(false));
        assertThat(result.diagnostics(), hasSize(1));
        var diagnostic = result.diagnostics().getFirst();
        assertThat(diagnostic.code(), is(JVM_BACKEND_FAILURE));
        assertThat(diagnostic.span(), is(sourceSpan));
        assertThat(diagnostic.message(), startsWith("Could not generate JVM bytecode: "));
    }
}
