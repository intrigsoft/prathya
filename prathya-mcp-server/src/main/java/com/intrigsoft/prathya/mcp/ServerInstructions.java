package com.intrigsoft.prathya.mcp;

/**
 * Server-level instructions sent to LLMs connecting to the Prathya MCP server.
 * Provides conceptual context about Contract-Driven Development (CDD) and
 * guidance on how to use the tools together effectively.
 */
public final class ServerInstructions {

    private ServerInstructions() {}

    public static final String INSTRUCTIONS = """
            Prathya is a Contract-Driven Development (CDD) tool for Java. It brings formal \
            requirement traceability to software testing by treating requirements as first-class, \
            versioned artifacts.

            ## Why CDD exists

            CDD complements TDD and BDD:
            - TDD drives implementation but doesn't ensure the tests are the *right* tests.
            - BDD improves communication but doesn't enforce traceability or coverage measurement.
            - CDD defines the contract first. Tests are written against the contract. Coverage is \
            measured against the contract. The contract is the source of truth.

            The core insight: code coverage tells you what code was *touched*. Requirement coverage \
            tells you whether *intent* was verified.

            ## Key concepts

            **CONTRACT.yaml** — Every module has a CONTRACT.yaml file that defines its behavioral \
            contract. This is a human-authored, version-controlled artifact that is the single \
            source of truth for what the module is supposed to do.

            **@Requirement annotation** — Test methods are annotated with @Requirement("REQ-ID") \
            to declare which requirement or corner case they verify. The relationship is many-to-many: \
            one test can cover multiple requirements, and one requirement can be covered by multiple tests. \
            The annotations *are* the traceability — no separate mapping file is needed.

            **Coverage matrix** — Prathya cross-references annotations against CONTRACT.yaml and \
            computes a three-state coverage model:
            - Covered and tests passing — contract satisfied
            - Covered but tests failing — contract broken (actively worse than uncovered)
            - Not covered — contract unverified

            ## Requirement lifecycle

            Requirements follow a strict lifecycle: DRAFT -> APPROVED -> DEPRECATED or SUPERSEDED.
            - DRAFT: Under discussion, not yet part of the active contract.
            - APPROVED: Active part of the contract. Tests should cover it.
            - DEPRECATED: No longer relevant. Kept for history, excluded from coverage.
            - SUPERSEDED: Replaced by a newer requirement. The superseded_by field links to the replacement.

            ## Corner cases

            Corner cases are first-class citizens in the contract, not afterthoughts. Each corner case \
            has its own ID (e.g. AUTH-001-CC-001) and can be independently tested and tracked. They \
            represent the edge conditions that often harbor bugs.

            ## ID conventions

            Requirement IDs are permanent and immutable. They follow the format {MODULE}-{SEQUENCE} \
            (e.g. AUTH-001) with corner cases as {MODULE}-{SEQUENCE}-CC-{N} (e.g. AUTH-001-CC-001).

            Critical rules:
            - IDs are append-only and never reused.
            - Requirements are never deleted — only deprecated or superseded.
            - When a requirement changes significantly, it gets a new ID with a supersedes back-reference.
            - When wording changes only, the version increments on the same ID.

            ## Versioning

            Requirement versions follow semver semantics:
            - Major — breaking change to the contract; mapped tests must be re-evaluated.
            - Minor — additive change (new corner case, expanded scope).
            - Patch — wording or clarification only, no behavioral change.

            ## Getting started

            If the project hasn't been configured with Prathya yet, use configure_project to get \
            step-by-step setup instructions for Maven or Gradle, including JaCoCo integration.

            ## Workflow

            The typical CDD workflow is:
            1. Define or update the contract (add_requirement, update_requirement, add_corner_case).
            2. Annotate test methods with @Requirement to map them to the contract.
            3. Measure coverage (get_coverage_matrix, list_untested, run_audit).
            4. Iterate until the contract is fully satisfied.

            Use get_contract first to understand the current state. Use run_audit to find violations \
            (orphaned annotations, uncovered requirements). Use get_coverage_matrix to see the full \
            picture. When evolving requirements, prefer supersede_requirement over delete — IDs are \
            permanent and the history matters.
            """;
}
