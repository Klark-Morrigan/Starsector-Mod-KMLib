#!/usr/bin/env bats
# Unit tests for the validate-versioning composite action's script.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Requires jq on PATH (used by the script under test).
# Run from the KMLib mod root: bats .github/tests/validate_versioning.bats
#
# Each test stages a fresh working directory with the fixture files the
# script reads (CHANGELOG.md, mod_info.json) so cases are isolated.

SCRIPT="$BATS_TEST_DIRNAME/../actions/validate-versioning/scripts/validate_versioning.sh"

CHANGELOG_WITH_SECTION='## [Unreleased]

## [1.2.0] - 2026-05-18
### Added
- New thing.

## [1.1.0]
Older entry.
'

CHANGELOG_WITHOUT_SECTION='## [Unreleased]

## [1.1.0]
Older entry.
'

setup() {
    WORK_DIR="$(mktemp -d)"
    cd "$WORK_DIR"
}

teardown() {
    rm -rf "$WORK_DIR"
}

# Writes a minimal mod_info.json. Optional second argument is a raw JSON
# fragment inserted as the "dependencies" array body, so callers can
# exercise the kmlib-dep branches without templating jq from inside bats.
write_mod_info() {
    local version="$1"
    local deps_body="${2:-}"
    if [ -n "$deps_body" ]; then
        cat > mod_info.json <<EOF
{
  "id": "consumer",
  "version": "$version",
  "dependencies": [ $deps_body ]
}
EOF
    else
        cat > mod_info.json <<EOF
{
  "id": "consumer",
  "version": "$version"
}
EOF
    fi
}

# Writes a changelog whose only section is the given version, so a test can
# exercise a version format without the shared fixture's headings deciding
# whether rule 1 or rule 3 fires first.
write_changelog_for() {
    printf '## [Unreleased]\n\n## [%s]\nEntry.\n' "$1" > CHANGELOG.md
}

# Writes a changelog carrying a navigation index, so the index rule has
# something to rule on. The indexed versions are given separately from the
# sectioned one, which is how a missed entry is posed.
write_changelog_with_index() {
    local sectionVersion="$1"
    shift
    {
        printf '# Changelog\n\n## Index\n\n'
        for indexed in "$@"; do
            printf -- '- [%s](#%s)\n' "$indexed" "${indexed//./}"
        done
        printf '\n## [%s] - 2026-09-15\nEntry.\n' "$sectionVersion"
    } > CHANGELOG.md
}

@test "passes when the changelog index lists the released version" {
    write_changelog_with_index "1.2.0" "1.2.0" "1.1.0"
    write_mod_info "1.2.0"
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -eq 0 ]
}

@test "fails when the changelog has an index but no entry for the version" {
    write_changelog_with_index "1.2.0" "1.1.0"
    write_mod_info "1.2.0"
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "no entry linking to 1.2.0"
}

# The rule checks a convention, so a changelog that never adopted it is not
# failed for lacking one.
@test "passes when the changelog carries no index at all" {
    printf '%s' "$CHANGELOG_WITH_SECTION" > CHANGELOG.md
    write_mod_info "1.2.0"
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -eq 0 ]
}

# Only the version being released has to be indexed; a gap in an older entry
# is not a reason to stop today's release.
@test "passes when an older version is missing from the index" {
    write_changelog_with_index "1.2.0" "1.2.0"
    write_mod_info "1.2.0"
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -eq 0 ]
}

@test "passes when changelog section, version, and no kmlib dep" {
    printf '%s' "$CHANGELOG_WITH_SECTION" > CHANGELOG.md
    write_mod_info "1.2.0"
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -eq 0 ]
}

@test "fails when CHANGELOG.md is missing" {
    write_mod_info "1.2.0"
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "CHANGELOG.md not found"
}

@test "fails when CHANGELOG.md has no section for the released version" {
    printf '%s' "$CHANGELOG_WITHOUT_SECTION" > CHANGELOG.md
    write_mod_info "1.2.0"
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "no '## \[1.2.0\]' section"
}

@test "fails when mod_info.json .version does not match input" {
    printf '%s' "$CHANGELOG_WITH_SECTION" > CHANGELOG.md
    write_mod_info "1.1.0"
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "does not match released version"
}

@test "fails when the released version has only two components" {
    write_changelog_for "1.2"
    write_mod_info "1.2"
    run bash "$SCRIPT" "1.2"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "version '1.2' is not well-formed"
}

@test "fails when the released version has a fourth component" {
    write_changelog_for "1.2.0.4"
    write_mod_info "1.2.0.4"
    run bash "$SCRIPT" "1.2.0.4"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "version '1.2.0.4' is not well-formed"
}

@test "fails when the released version carries a letter suffix" {
    write_changelog_for "1.2.0a"
    write_mod_info "1.2.0a"
    run bash "$SCRIPT" "1.2.0a"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "version '1.2.0a' is not well-formed"
}

@test "passes when kmlib dep version is well-formed SemVer" {
    printf '%s' "$CHANGELOG_WITH_SECTION" > CHANGELOG.md
    write_mod_info "1.2.0" '{ "id": "kmlib", "name": "KMLib", "version": "1.0.0" }'
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -eq 0 ]
}

@test "fails when kmlib dep version is missing" {
    printf '%s' "$CHANGELOG_WITH_SECTION" > CHANGELOG.md
    write_mod_info "1.2.0" '{ "id": "kmlib", "name": "KMLib" }'
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "missing a 'version' field"
}

@test "fails when kmlib dep version is malformed" {
    printf '%s' "$CHANGELOG_WITH_SECTION" > CHANGELOG.md
    write_mod_info "1.2.0" '{ "id": "kmlib", "name": "KMLib", "version": "1.0" }'
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -ne 0 ]
    echo "$output" | grep -q "kmlib dependency version '1.0' is not well-formed"
}

@test "passes when only a non-kmlib dependency is declared" {
    printf '%s' "$CHANGELOG_WITH_SECTION" > CHANGELOG.md
    write_mod_info "1.2.0" '{ "id": "lw_lazylib", "name": "LazyLib" }'
    run bash "$SCRIPT" "1.2.0"
    [ "$status" -eq 0 ]
}

@test "version argument is required" {
    printf '%s' "$CHANGELOG_WITH_SECTION" > CHANGELOG.md
    write_mod_info "1.2.0"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
}
