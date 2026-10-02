#!/usr/bin/env bats
# Unit tests for .github/actions/_lib/localisation.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/localisation.bats

LIB="$BATS_TEST_DIRNAME/../actions/_lib/localisation.sh"

setup() {
    TMP="$(mktemp -d)"
    cd "$TMP"
}

teardown() {
    rm -rf "$TMP"
}

# Sources the lib in a mod root and runs the given statements there.
run_in_lib() {
    run bash -c "set -euo pipefail
                 SCRIPT_NAME='package_release'
                 source '$LIB'
                 $*"
}

@test "localisation_require_translated_changelog echoes the bundle's changelog path" {

    mkdir -p localisation/zh-hans
    : > localisation/zh-hans/CHANGELOG.md
    run_in_lib "localisation_require_translated_changelog zh-hans"

    [ "$status" -eq 0 ]
    [ "$output" = "localisation/zh-hans/CHANGELOG.md" ]
}

@test "localisation_require_translated_changelog fails naming the missing path" {

    run_in_lib "localisation_require_translated_changelog zh-hans"

    [ "$status" -ne 0 ]
    [ "$output" = "package_release: locale zh-hans has no localisation/zh-hans/CHANGELOG.md" ]
}

@test "the manifest sits at the root of the localisation directory" {

    run_in_lib 'printf "%s" "$LOCALISATION_MANIFEST_FILE"'

    [ "$output" = "localisation/manifest.json" ]
}
