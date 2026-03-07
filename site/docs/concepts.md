# Concepts

## Contract-Driven Development

Contract-Driven Development (CDD) starts from a simple premise: **define what the software must do before writing tests or code**. The contract is a versioned, machine-readable artifact that lives in source control alongside the code it describes.

Tests are written against the contract. Coverage is measured against the contract. The contract is the source of truth.

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

## The Coverage Quadrant

Prathya is designed to sit alongside JaCoCo. Used together, the two metrics expose a quadrant of insights:

| | Code Coverage High | Code Coverage Low |
|---|---|---|
| **Requirement Coverage High** | Well-tested and well-documented | Requirements are mapped but tests may be shallow |
| **Requirement Coverage Low** | Code is exercised but features aren't documented — possible dead code | Undertested and underdocumented |
