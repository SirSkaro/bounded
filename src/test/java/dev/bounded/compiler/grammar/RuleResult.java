package dev.bounded.compiler.grammar;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.Optional;

record RuleResult<T extends ParserRuleContext>(
        String source,
        T tree, List<String> errors,
        Optional<TokenFacade> unconsumedToken
) implements GrammarParseResult {}
