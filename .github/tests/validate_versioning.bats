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
    echo "$output" | grep -q "not well-formed SemVer"
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
