package dev.bounded.compiler.api.parse;

import dev.bounded.compiler.api.SourceFile;

@FunctionalInterface
public interface Parser {
    ParseResult parse(SourceFile sourceFile);
}
