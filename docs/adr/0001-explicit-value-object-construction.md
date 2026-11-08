# ADR 0001: Explicit construction of value objects

Status: Accepted

Date: 2026-11-07

## Context

Value objects express compiler concepts and their invariants. Their construction should
make the information needed to represent those concepts explicit.

`SourceSpan` had an overload taking only a line and column, which supplied `"<unknown>"`
as the source name. Production parsing already supplied the source name; the overload
served hand-built AST tests. It introduced a default without a corresponding language
requirement and allowed callers to omit source identity accidentally.

`GeneratedClass` also accepted a string in place of its `ClassName` component. That overload
preserved validation, but provided another construction path and hid the explicit use of
the validated value type from callers.

## Decision

Use one constructor per value object, taking its complete required state in the appropriate
domain types. For records, use the canonical constructor, including its compact form where
appropriate, to enforce invariants and defensive copying. Do not add convenience overloads
that omit required information, invent defaults, or accept a weaker representation of a
component solely to shorten call sites.

Named factories remain appropriate when they express meaningful domain choices.
`CompilationResult.success(...)` and `failure(...)`, for example, enforce different valid
outcomes through one private constructor. Factories must preserve the type's invariants;
they should not simply recreate removed convenience overloads under different names.

Callers perform explicit conversions to value types. Tests supply explicit fixture data,
such as `"money.bounded"` for a source name. Reusable test fixtures may provide defaults
within test sources when that improves readability without changing production semantics.

Applied here:

```java
new SourceSpan("money.bounded", 3, 7);
new GeneratedClass(new ClassName("example.Money"), bytecode);
```

This is the default for value objects. Service construction and dependency injection have
different responsibilities and are outside this decision's scope. Revisit the decision
when a concrete domain requirement warrants an alternative, and document that requirement.

## Consequences

- Required information and validated types are visible at construction sites.
- Invariants and defensive copies remain owned by the value object.
- Call sites can be longer; test conveniences stay in test fixtures.
- Existing callers of the removed constructors must supply a source name or construct a
  `ClassName` explicitly. There is no compatibility overload supplying `"<unknown>"`.
- Unknown or synthetic source locations are not introduced speculatively. If they become
  necessary, their meaning and representation must be defined explicitly.

Constructor changes must retain invariant tests and pass the existing verification gates
described in [AGENTS.md](../../AGENTS.md).
