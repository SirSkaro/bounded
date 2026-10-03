package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.diagnostics.DiagnosticCode;

import java.util.ArrayList;
import java.util.Locale;

public final class EffectChecker {
    public EffectCheckResult check(BoundedProgram program) {
        var archeClass = program.archeClass();
        var maximumEffect = archeClass.kind().maximumEffect();
        var diagnostics = new ArrayList<Diagnostic>();

        for (var operation : archeClass.operations()) {
            if (!maximumEffect.allows(operation.kind().effectGrade())) {
                diagnostics.add(new Diagnostic(
                        DiagnosticCode.EFFECT_NOT_ALLOWED,
                        operation.span(),
                        "%s '%s' permits operations through %s; '%s' is %s."
                                .formatted(
                                        titleCase(archeClass.kind().name()),
                                        archeClass.name(),
                                        maximumEffect,
                                        operation.name(),
                                        operation.kind().effectGrade())));
            }
        }

        return new EffectCheckResult(diagnostics);
    }

    private String titleCase(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase(Locale.ROOT);
    }
}
