package dev.bounded.compiler.backend;

import dev.bounded.compiler.api.generate.CodeGenerator;
import dev.bounded.compiler.api.generate.GeneratedClass;
import dev.bounded.compiler.api.generate.GenerationResult;
import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.diagnostics.Diagnostic;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.MethodSignature;
import java.lang.classfile.Signature;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.JVM_BACKEND_FAILURE;
import static java.lang.classfile.ClassFile.ACC_FINAL;
import static java.lang.classfile.ClassFile.ACC_PRIVATE;
import static java.lang.classfile.ClassFile.ACC_PUBLIC;
import static java.lang.classfile.ClassFile.ACC_STATIC;
import static java.lang.classfile.ClassFile.ACC_SUPER;
import static java.lang.classfile.ClassFile.JAVA_25_VERSION;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_void;

public final class JvmBackend implements CodeGenerator {
    private static final String GENERATED_PACKAGE = "bounded.generated";
    private static final ClassDesc MAP = ClassDesc.of("java.util.Map");
    private static final ClassDesc MAP_ENTRY = ClassDesc.of("java.util.Map$Entry");

    @Override
    public GenerationResult generate(BoundedProgram program) {
        try {
            return GenerationResult.validResult(generateClass(program));
        } catch (IllegalArgumentException exception) {
            var diagnostic = new Diagnostic(
                    JVM_BACKEND_FAILURE,
                    program.archeClass().span(),
                    "Could not generate JVM bytecode: " + exception.getMessage());
            return GenerationResult.invalidResult(List.of(diagnostic));
        }
    }

    private GeneratedClass generateClass(BoundedProgram program) {
        var archeClass = program.archeClass();
        var className = GENERATED_PACKAGE + ".Bounded_" + archeClass.name();
        var bytecode = ClassFile.of().build(ClassDesc.of(className), builder -> {
            builder.withVersion(JAVA_25_VERSION, 0)
                    .withFlags(ACC_PUBLIC | ACC_FINAL | ACC_SUPER)
                    .withSuperclass(CD_Object);
            declarePrivateConstructor(builder);
            declareArcheClassKind(builder, archeClass);
            declareOperations(builder, archeClass.operations());
        });
        return new GeneratedClass(className, bytecode);
    }

    private void declarePrivateConstructor(ClassBuilder builder) {
        builder.withMethodBody("<init>", MethodTypeDesc.of(CD_void), ACC_PRIVATE,
                code -> code.aload(0)
                        .invokespecial(CD_Object, "<init>", MethodTypeDesc.of(CD_void))
                        .return_());
    }

    private void declareArcheClassKind(ClassBuilder builder, ArcheClassDeclaration archeClass) {
        builder.withMethodBody("boundedArcheClassKind", MethodTypeDesc.of(CD_String), ACC_PUBLIC | ACC_STATIC,
                code -> code.loadConstant(archeClass.kind().name()).areturn());
    }

    private void declareOperations(ClassBuilder builder, List<ArcheOperationDeclaration> operations) {
        builder.withMethod("boundedOperations", MethodTypeDesc.of(MAP), ACC_PUBLIC | ACC_STATIC,
                method -> method.with(operationsSignature())
                        .withCode(code -> returnOperations(code, operations)));
    }

    private SignatureAttribute operationsSignature() {
        var stringType = Signature.TypeArg.of(Signature.ClassTypeSig.of(CD_String));
        var mapType = Signature.ClassTypeSig.of(MAP, stringType, stringType);
        return SignatureAttribute.of(MethodSignature.of(mapType));
    }

    private void returnOperations(CodeBuilder code, List<ArcheOperationDeclaration> operations) {
        if (operations.isEmpty()) {
            code.invokestatic(MAP, "of", MethodTypeDesc.of(MAP), true).areturn();
            return;
        }

        code.loadConstant(operations.size()).anewarray(MAP_ENTRY);
        for (int index = 0; index < operations.size(); index++) {
            var operation = operations.get(index);
            code.dup()
                    .loadConstant(index)
                    .loadConstant(operation.name())
                    .loadConstant(operation.kind().name())
                    .invokestatic(MAP, "entry", MethodTypeDesc.of(MAP_ENTRY, CD_Object, CD_Object), true)
                    .aastore();
        }
        code.invokestatic(MAP, "ofEntries", MethodTypeDesc.of(MAP, MAP_ENTRY.arrayType()), true)
                .areturn();
    }
}
