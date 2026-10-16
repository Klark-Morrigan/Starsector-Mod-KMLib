#!/usr/bin/env bats
# Unit tests for .github/actions/_lib/github_output.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/github_output.bats
#
# Each case runs the lib in a subshell (bash -c) because the helper exits on
# failure, which would otherwise take the test runner down with it.

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

LIB="$BATS_TEST_DIRNAME/../actions/_lib/github_output.sh"

setup() {

    export GITHUB_OUTPUT
    GITHUB_OUTPUT="$(mktemp)"
}

teardown() {

    rm -f "$GITHUB_OUTPUT"
}

# Sources the lib and runs the given statements in it, keeping stderr apart.
run_in_lib() {

    run --separate-stderr bash -c "set -euo pipefail
                                   SCRIPT_NAME='read_locales'
                                   source '$LIB'
                                   $*"
}

@test "write_github_output appends the key and its value" {

    echo "earlier=kept" > "$GITHUB_OUTPUT"

    run_in_lib "write_github_output 'default-locale' 'en'"

    [ "$status" -eq 0 ]
    [ "$(cat "$GITHUB_OUTPUT")" = $'earlier=kept\ndefault-locale=en' ]
}

@test "write_github_output logs the key and its value on stderr alone" {

    run_in_lib "write_github_output 'default-locale' 'en'"

    [ "$status" -eq 0 ]
    [ "$stderr" = "default-locale: en" ]
    [ "$output" = "" ]
}

@test "write_github_output writes an empty value and logs the key alone" {

    run_in_lib "write_github_output 'default-locale' ''"

    [ "$status" -eq 0 ]
    [ "$(cat "$GITHUB_OUTPUT")" = "default-locale=" ]
    [ "$stderr" = "default-locale:" ]
}

@test "write_github_output fails a value spanning lines, naming the key" {

    run_in_lib "write_github_output 'locales' \$'[\n]'; echo reached"

    # Its second line would be read back as a key of its own.
    [ "$status" -ne 0 ]
    [ "$stderr" = "read_locales: output 'locales' spans more than one line" ]
    [ "$output" = "" ]
    [ ! -s "$GITHUB_OUTPUT" ]
}
