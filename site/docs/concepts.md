# Concepts

## Contract-Driven Development

Contract-Driven Development (CDD) starts from a simple premise: **define what the software must do before writing tests or code**. The contract is a versioned, machine-readable artifact that lives in source control alongside the code it describes.

Tests are written against the contract. Coverage is measured against the contract. The contract is the source of truth.

!!! note "Disambiguation"
    The term *Contract-Driven Development* is traditionally associated with defining API or service interaction contracts prior to implementation. In Prathya, the contract refers to **behavioral requirements** — not API schemas — and coverage is measured against those requirements.

## Key Components

### The Contract (`CONTRACT.yaml`)

Every module has a `CONTRACT.yaml` file that defines its behavioral contract. This is a human-authored, version-controlled artifact that defines what the module is supposed to do — including corner cases as first-class citizens, not afterthoughts.

### The Annotation (`@Requirement`)

Test methods are annotated with `@Requirement("REQ-ID")` to declare which requirement or corner case they verify. This is the only coupling between test code and requirements.

```java
@Test
@Requirement("AUTH-001")
void login_validCredentials_returnsJwt() { /* ... */ }

@Test
@Requirement({"AUTH-001-CC-001", "AUTH-001-CC-002"})
void login_invalidCredentials_returns401() { /* ... */ }
```

One test can cover multiple requirements or corner cases. One requirement can be covered by multiple tests. The relationship is many-to-many.

### Requirement Coverage

Prathya scans annotations at build time, cross-references them against `CONTRACT.yaml`, and computes a coverage matrix. No manually maintained mapping file is needed — the annotations _are_ the trace.

## Three-State Coverage Model

When using the `prathya:run` goal, coverage goes beyond a simple covered/uncovered binary:

| State | Meaning |
|---|---|
| **Covered + Passing** | Contract satisfied — the requirement is tested and the test passes |
| **Covered + Failing** | Contract broken — a test exists but the implementation is wrong |
| **Not Covered** | Contract unverified — no test maps to this requirement |

## Requirement Lifecycle

Requirements follow a defined lifecycle:

| Status | Meaning |
|---|---|
| `draft` | Not yet approved — excluded from coverage calculations |
| `approved` | Active requirement — must have tests |
| `deprecated` | No longer relevant — excluded from coverage |
| `superseded` | Replaced by a newer requirement (linked via `superseded_by`) |

Requirements are **never deleted** — only deprecated or superseded. IDs are append-only and never reused.

## Corner Cases as First-Class Citizens

Corner cases are defined directly inside each requirement, with their own IDs:

```yaml
corner_cases:
  - id: AUTH-001-CC-001
    description: Invalid password — must return 401, not 404
  - id: AUTH-001-CC-002
    description: Email does not exist — response must be identical to wrong password
```

Corner cases can be annotated and tracked independently, ensuring edge cases are tested deliberately rather than discovered accidentally.

## ID Conventions

Requirement IDs are opaque strings — Prathya does not enforce a specific format. The following convention is recommended:

### Recommended Format

```
{MODULE}-{SEQUENCE}         → AUTH-001        (requirement)
{MODULE}-{SEQUENCE}-CC-{N}  → AUTH-001-CC-002 (corner case)
```

### Rules

- IDs are **append-only and never reused**
- When a requirement changes significantly, it gets a **new ID** with a `supersedes` back-reference
- When a requirement changes in wording only, the **version increments** on the same ID
- When a requirement is split, the original is deprecated and new IDs are created

### Recommended Versioning Semantics

Requirement versions are free-form strings. We recommend following semver semantics:

| Bump | Meaning |
|---|---|
| **Major** | Breaking change to the contract — mapped tests must be re-evaluated |
| **Minor** | Additive change (new corner case, expanded scope) |
| **Patch** | Wording or clarification, no behavioral change |

## Contract Code Coverage

When JaCoCo is present, Prathya computes **contract code coverage** — the percentage of code covered exclusively by `@Requirement`-annotated tests. This is distinct from total code coverage, which includes all tests regardless of whether they trace to a requirement.

The gap between the two numbers is meaningful. If total code coverage is 87% but contract code coverage is 60%, then 27% of your code coverage comes from tests that aren't linked to any requirement. Those tests exercise code, but don't prove intent.

## Interpreting the Metrics

Prathya measures requirement coverage — whether each documented requirement has a test mapped to it. It does not verify correctness; it trusts that a test annotated with `@Requirement("AUTH-001")` actually verifies that requirement. This is an indirect measurement.

Used alongside JaCoCo's code coverage, the relationship between the two metrics helps identify specific issues:

!!! failure "Requirement coverage below 100%"
    Some requirements have no tests mapped to them. These are unverified parts of the contract.

!!! warning "Requirement coverage :material-arrow-up-bold: Code coverage :material-arrow-down-bold:"
    Most requirements have mapped tests, but large portions of the code are not exercised. This may indicate:

    - :material-close: **Tests are shallow** — annotated tests exist but rely heavily on mocking or do not exercise real code paths.
    - :material-close: **Requirements are incomplete** — the contract does not capture all functionality in the module, leaving undocumented code untested.
    - :material-close: **Dead code** — code exists that is not needed by any requirement and is never executed.

!!! info "Requirement coverage :material-arrow-down-bold: Code coverage :material-arrow-up-bold:"
    The codebase has substantial test coverage, but tests are not mapped to requirements. This may indicate:

    - :material-progress-clock: **CDD adoption in progress** — tests exist and the team is incrementally annotating them with `@Requirement`. This is a transitional state.
    - :material-close: **Tests verify implementation, not intent** — tests exercise code but are not driven by documented business requirements. The tests may pass, but they do not prove that the contract is satisfied.

!!! success "Requirement coverage :material-arrow-up-bold: Code coverage :material-arrow-up-bold:"
    Both metrics are high. Requirements are documented, tests are mapped, and code is well exercised. This is the target state.
