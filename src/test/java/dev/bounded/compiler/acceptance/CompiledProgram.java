package dev.bounded.compiler.acceptance;

import dev.bounded.compiler.api.write.CompiledClass;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompiledProgram implements AutoCloseable {
    private final URLClassLoader loader;
    private final List<Class<?>> classes;

    CompiledProgram(List<CompiledClass> artifacts) throws IOException, ClassNotFoundException {
        loader = new URLClassLoader(classPath(artifacts), ClassLoader.getPlatformClassLoader());
        try {
            classes = loadClasses(artifacts);
        } catch (ClassNotFoundException | SecurityException | LinkageError failure) {
            try (loader) {
                throw failure;
            }
        }
    }

    public ArcheClassMetadata archeClass() throws ReflectiveOperationException {
        var primary = classes.getFirst();
        var kind = (String) primary.getMethod("boundedArcheClassKind").invoke(null);
        var rawOperations = (Map<?, ?>) primary.getMethod("boundedOperations").invoke(null);
        Map<String, String> operations = new LinkedHashMap<>();
        rawOperations.forEach((name, operationKind) -> operations.put((String) name, (String) operationKind));
        return new ArcheClassMetadata(kind, operations);
    }

    private URL[] classPath(List<CompiledClass> artifacts) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (var root : artifacts.stream().map(CompiledClass::classPathRoot).distinct().toList()) {
            urls.add(root.toUri().toURL());
        }
        return urls.toArray(URL[]::new);
    }

    private List<Class<?>> loadClasses(List<CompiledClass> artifacts) throws ClassNotFoundException {
        List<Class<?>> loaded = new ArrayList<>();
        for (var artifact : artifacts) {
            loaded.add(loader.loadClass(artifact.className().binaryName()));
        }
        return List.copyOf(loaded);
    }

    @Override
    public void close() throws IOException {
        loader.close();
    }
}
