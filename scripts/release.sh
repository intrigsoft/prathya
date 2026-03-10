#!/usr/bin/env bash
#
# release.sh — Automate the Prathya release process.
#
# Usage:
#   ./scripts/release.sh <release-version> [next-snapshot-version]
#
# Examples:
#   ./scripts/release.sh 1.0.0              # next defaults to 1.0.1-SNAPSHOT
#   ./scripts/release.sh 1.0.0 1.1.0-SNAPSHOT
#
# What it does:
#   1. Validates preconditions (clean tree, on main, correct branch)
#   2. Runs the full build (mvn clean verify)
#   3. Sets the release version in all POMs, docs, samples
#   4. Commits and tags
#   5. Bumps to next SNAPSHOT version
#   6. Commits
#   7. Pushes commits + tag (with confirmation)
#
set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# --- Colors -----------------------------------------------------------
red()   { printf '\033[1;31m%s\033[0m\n' "$*"; }
green() { printf '\033[1;32m%s\033[0m\n' "$*"; }
yellow(){ printf '\033[1;33m%s\033[0m\n' "$*"; }
bold()  { printf '\033[1m%s\033[0m\n' "$*"; }

# --- Helpers -----------------------------------------------------------
die() { red "ERROR: $*" >&2; exit 1; }

confirm() {
    local prompt="$1"
    read -r -p "$(yellow "$prompt [y/N] ")" answer
    [[ "$answer" =~ ^[Yy]$ ]] || die "Aborted."
}

# Derive next snapshot: 1.0.0 -> 1.0.1-SNAPSHOT
next_snapshot() {
    local version="$1"
    local major minor patch
    IFS='.' read -r major minor patch <<< "$version"
    echo "${major}.${minor}.$((patch + 1))-SNAPSHOT"
}

# --- Parse arguments ---------------------------------------------------
if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <release-version> [next-snapshot-version]"
    echo "  e.g. $0 1.0.0"
    exit 1
fi

RELEASE_VERSION="$1"
NEXT_VERSION="${2:-$(next_snapshot "$RELEASE_VERSION")}"

# Validate version format
[[ "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] \
    || die "Release version must be in X.Y.Z format (got: $RELEASE_VERSION)"
[[ "$NEXT_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT$ ]] \
    || die "Next version must be in X.Y.Z-SNAPSHOT format (got: $NEXT_VERSION)"

cd "$PROJECT_DIR"

# --- Precondition checks ----------------------------------------------
bold "--- Checking preconditions ---"

# Clean working tree
if [[ -n "$(git status --porcelain)" ]]; then
    die "Working tree is not clean. Commit or stash changes first."
fi

# On main branch
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$BRANCH" != "main" ]]; then
    die "Not on main branch (current: $BRANCH). Switch to main first."
fi

# Up to date with remote
git fetch origin main --quiet
LOCAL="$(git rev-parse HEAD)"
REMOTE="$(git rev-parse origin/main)"
if [[ "$LOCAL" != "$REMOTE" ]]; then
    die "Local main is not up to date with origin/main. Pull or push first."
fi

# Detect current version from POM
CURRENT_VERSION="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)"
if [[ ! "$CURRENT_VERSION" =~ -SNAPSHOT$ ]]; then
    die "Current POM version ($CURRENT_VERSION) is not a SNAPSHOT. Already released?"
fi

# Tag doesn't already exist
if git rev-parse "v${RELEASE_VERSION}" >/dev/null 2>&1; then
    die "Tag v${RELEASE_VERSION} already exists."
fi

green "All preconditions met."
echo ""

bold "=== Prathya Release ==="
echo "  Current version : $CURRENT_VERSION"
echo "  Release version : $RELEASE_VERSION"
echo "  Next dev version: $NEXT_VERSION"
echo ""

# --- Build and test ----------------------------------------------------
bold "--- Running full build ---"
mvn clean verify -q || die "Build failed. Fix issues before releasing."
green "Build passed."
echo ""

# --- Set release version -----------------------------------------------
bold "--- Setting release version $RELEASE_VERSION ---"

# POMs (reactor modules)
mvn versions:set -DnewVersion="$RELEASE_VERSION" -DgenerateBackupPoms=false -q

# Docs, README, and sample files
DOC_FILES=(
    README.md
    docs/integration-guide.md
    site/docs/index.md
    site/docs/maven-plugin.md
    site/docs/gradle-plugin.md
    site/docs/mcp-server.md
    samples/sample-maven/pom.xml
    samples/sample-gradle/build.gradle.kts
)

# Replace both the SNAPSHOT version and any previously pinned stable version
# (docs are pinned to the latest stable release, not SNAPSHOT)
CURRENT_STABLE="${CURRENT_VERSION%-SNAPSHOT}"
for f in "${DOC_FILES[@]}"; do
    if [[ -f "$f" ]]; then
        sed -i "s/${CURRENT_VERSION}/${RELEASE_VERSION}/g" "$f"
        sed -i "s/${CURRENT_STABLE}/${RELEASE_VERSION}/g" "$f"
    fi
done

# Also replace any older prathya version strings (e.g. docs pinned to a prior release)
OLDER_VERSIONS=$(grep -rhoP '\d+\.\d+\.\d+' "${DOC_FILES[@]}" 2>/dev/null \
    | grep -v "^${RELEASE_VERSION}$" \
    | sort -uV)
for old in $OLDER_VERSIONS; do
    # Only replace versions that look like prathya versions (in prathya-specific contexts)
    for f in "${DOC_FILES[@]}"; do
        if [[ -f "$f" ]]; then
            sed -i "s/com\.intrigsoft\.prathya\(.*\)${old}/com.intrigsoft.prathya\1${RELEASE_VERSION}/g" "$f"
            sed -i "s/prathya\.version>${old}/prathya.version>${RELEASE_VERSION}/g" "$f"
            sed -i "s/prathya-annotations:${old}/prathya-annotations:${RELEASE_VERSION}/g" "$f"
            sed -i "s/prathya-mcp-server:${old}/prathya-mcp-server:${RELEASE_VERSION}/g" "$f"
            sed -i "s/prathya\.gradle\") version \"${old}/prathya.gradle\") version \"${RELEASE_VERSION}/g" "$f"
            sed -i "s/prathya\") version \"${old}/prathya\") version \"${RELEASE_VERSION}/g" "$f"
        fi
    done
done

green "Version set to $RELEASE_VERSION"
echo ""

# --- Commit and tag ----------------------------------------------------
bold "--- Committing release ---"
git add -A
git commit -m "Release v${RELEASE_VERSION}"
git tag -a "v${RELEASE_VERSION}" -m "Release v${RELEASE_VERSION}"
green "Created commit and tag v${RELEASE_VERSION}"
echo ""

# --- Bump to next snapshot ---------------------------------------------
bold "--- Setting next development version $NEXT_VERSION ---"

# POMs
mvn versions:set -DnewVersion="$NEXT_VERSION" -DgenerateBackupPoms=false -q

# Docs stay pinned to the stable release version — do NOT replace with SNAPSHOT

git add -A
git commit -m "Prepare next development iteration ${NEXT_VERSION}"
green "Bumped to $NEXT_VERSION"
echo ""

# --- Push --------------------------------------------------------------
bold "--- Ready to push ---"
echo "This will push:"
echo "  - 2 commits (release + snapshot bump)"
echo "  - Tag v${RELEASE_VERSION} (triggers CI release job)"
echo ""
confirm "Push to origin?"

git push origin main
git push origin "v${RELEASE_VERSION}"

echo ""
green "=== Release v${RELEASE_VERSION} complete! ==="
echo ""
echo "Next steps:"
echo "  1. Monitor CI: https://github.com/intrigsoft/prathya/actions"
echo "  2. Approve publishing in Sonatype Central portal"
echo "  3. Verify artifacts on Maven Central (~10 min after approval)"
