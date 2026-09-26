package dev.bounded.compiler.parse;

import dev.bounded.compiler.grammar.BoundedParser;
import dev.bounded.compiler.grammar.BoundedParser.ArcheClassContext;
import dev.bounded.compiler.grammar.BoundedParser.ArcheClassKindContext;
import dev.bounded.compiler.grammar.BoundedParser.ArcheOperationContext;
import dev.bounded.compiler.grammar.BoundedParser.ArcheOperationKindContext;
import dev.bounded.compiler.grammar.BoundedParser.ProgramContext;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

final class MapperContextFixtures {
    private MapperContextFixtures() {}

    static ProgramContext program(ArcheClassContext declaration) {
        var context = new ProgramContext(null, -1);
        context.addChild(declaration);
        declaration.setParent(context);
        return context;
    }

    static ArcheClassContext archeClass(int kind, String name, ArcheOperationContext... operations) {
        var context = new ArcheClassContext(null, -1);
        var kindContext = new ArcheClassKindContext(context, -1);
        var keyword = keyword(kind);
        kindContext.addChild(new TerminalNodeImpl(keyword));
        context.addChild(kindContext);
        context.addChild(identifier(name));
        context.start = keyword;
        for (var operation : operations) {
            context.addChild(operation);
            operation.setParent(context);
        }
        return context;
    }

    static ArcheOperationContext operation(int kind, String name) {
        var context = new ArcheOperationContext(null, -1);
        var kindContext = new ArcheOperationKindContext(context, -1);
        var keyword = keyword(kind);
        kindContext.addChild(new TerminalNodeImpl(keyword));
        context.addChild(kindContext);
        context.addChild(identifier(name));
        context.start = keyword;
        return context;
    }

    static <T extends ParserRuleContext> T at(T context, int line, int zeroBasedColumn) {
        var start = (CommonToken) context.getStart();
        start.setLine(line);
        start.setCharPositionInLine(zeroBasedColumn);
        return context;
    }

    private static CommonToken keyword(int kind) {
        var literal = BoundedParser.VOCABULARY.getLiteralName(kind);
        var token = new CommonToken(kind, literal.substring(1, literal.length() - 1));
        token.setLine(1);
        token.setCharPositionInLine(0);
        return token;
    }

    private static TerminalNodeImpl identifier(String name) {
        return new TerminalNodeImpl(new CommonToken(BoundedParser.IDENTIFIER, name));
    }
}
