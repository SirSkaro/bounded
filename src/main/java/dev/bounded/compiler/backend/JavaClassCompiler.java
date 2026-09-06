package dev.bounded.compiler.backend;

import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.diagnostics.DiagnosticCode;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JavaClassCompiler {
    private final int targetRelease;

    public JavaClassCompiler() {
        this(Runtime.version().feature());
    }

    JavaClassCompiler(int targetRelease) {
        this.targetRelease = targetRelease;
    }

    public JavaClassCompilation compile(
            GeneratedJavaSource generatedSource,
            Path outputRoot
    ) throws IOException {
        var sourceRoot = outputRoot.resolve("generated-sources");
        var classesRoot = outputRoot.resolve("classes");
        var sourcePath = sourcePath(sourceRoot, generatedSource.qualifiedClassName());
        var classPath = classPath(classesRoot, generatedSource.qualifiedClassName());

        Files.createDirectories(sourcePath.getParent());
        Files.createDirectories(classesRoot);
        Files.writeString(sourcePath, generatedSource.content(), StandardCharsets.UTF_8);

        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return failure(sourcePath, "No system Java compiler is available; run Bounded with a JDK.");
        }

        var compilerDiagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                compilerDiagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            var compilationUnits = fileManager.getJavaFileObjects(sourcePath.toFile());
            var options = List.of(
                    "--release", Integer.toString(targetRelease),
                    "-d", classesRoot.toString());
            var task = compiler.getTask(
                    null, fileManager, compilerDiagnostics, options, null, compilationUnits);

            if (Boolean.TRUE.equals(task.call()) && Files.exists(classPath)) {
                return new JavaClassCompilation(
                        List.of(sourcePath), List.of(classPath), List.of());
            }
        }

        var diagnostics = new ArrayList<Diagnostic>();
        for (var javaDiagnostic : compilerDiagnostics.getDiagnostics()) {
            diagnostics.add(new Diagnostic(
                    DiagnosticCode.JAVA_BACKEND_FAILURE,
                    new SourceSpan(
                            positiveOrOne(javaDiagnostic.getLineNumber()),
                            positiveOrOne(javaDiagnostic.getColumnNumber())),
                    javaDiagnostic.getMessage(Locale.ROOT)));
        }
        if (diagnostics.isEmpty()) {
            diagnostics.add(new Diagnostic(
                    DiagnosticCode.JAVA_BACKEND_FAILURE,
                    new SourceSpan(1, 1),
                    "javac failed without providing a diagnostic."));
        }

        return new JavaClassCompilation(List.of(sourcePath), List.of(), diagnostics);
    }

    private JavaClassCompilation failure(Path sourcePath, String message) {
        return new JavaClassCompilation(
                List.of(sourcePath),
                List.of(),
                List.of(new Diagnostic(
                        DiagnosticCode.JAVA_BACKEND_FAILURE,
                        new SourceSpan(1, 1),
                        message)));
    }

    private Path sourcePath(Path sourceRoot, String qualifiedClassName) {
        return sourceRoot.resolve(qualifiedClassName.replace('.', '/') + ".java");
    }

    private Path classPath(Path classesRoot, String qualifiedClassName) {
        return classesRoot.resolve(qualifiedClassName.replace('.', '/') + ".class");
    }

    private int positiveOrOne(long value) {
        return value > 0 && value <= Integer.MAX_VALUE ? (int) value : 1;
    }
}
