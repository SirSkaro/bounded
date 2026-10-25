package dev.bounded.compiler.backend;

final class GeneratedClassLoader extends ClassLoader {
    GeneratedClassLoader() {
        super(ClassLoader.getPlatformClassLoader());
    }

    Class<?> load(GeneratedClass generatedClass) {
        var bytecode = generatedClass.bytecode();
        return defineClass(generatedClass.qualifiedClassName(), bytecode, 0, bytecode.length);
    }
}
