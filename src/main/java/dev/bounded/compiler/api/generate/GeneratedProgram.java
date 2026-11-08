package dev.bounded.compiler.api.generate;

import java.util.List;

public record GeneratedProgram(List<GeneratedClass> classes) {
    public GeneratedProgram {
        classes = List.copyOf(classes);
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("A generated program requires at least one class");
        }
        if (classes.stream().map(GeneratedClass::className).distinct().count() != classes.size()) {
            throw new IllegalArgumentException("Generated class names must be unique");
        }
    }
}
