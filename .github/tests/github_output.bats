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
    [ "$stderr" = "read_locales: output 'locales' spans more than one line; write it with write_multiline_output" ]
    [ "$output" = "" ]
    [ ! -s "$GITHUB_OUTPUT" ]
}

@test "write_multiline_output writes the value between a delimiter line and its closing" {

    run_in_lib "write_multiline_output 'note' \$'first\n\nthird'"

    [ "$status" -eq 0 ]

    mapfile -t writtenLines < "$GITHUB_OUTPUT"
    delimiter="${writtenLines[0]#note<<}"

    [[ "${writtenLines[0]}" == "note<<EOF_"* ]]
    [ "${writtenLines[1]}" = "first" ]
    [ "${writtenLines[2]}" = "" ]
    [ "${writtenLines[3]}" = "third" ]
    [ "${writtenLines[4]}" = "$delimiter" ]
    [ "${#writtenLines[@]}" -eq 5 ]
}

@test "write_multiline_output logs the key with the value's lines indented beneath, on stderr alone" {

    run_in_lib "write_multiline_output 'note' \$'first\n\nthird'"

    # The blank line stays blank rather than carrying the indent's spaces.
    [ "$status" -eq 0 ]
    [ "$stderr" = $'note:\n  first\n\n  third' ]
    [ "$output" = "" ]
}

@test "write_multiline_output writes an empty value and logs the key alone" {

    run_in_lib "write_multiline_output 'translations' ''"

    [ "$status" -eq 0 ]
    [ "$(sed -n '2p' "$GITHUB_OUTPUT")" = "" ]
    [ "$(wc -l < "$GITHUB_OUTPUT")" -eq 3 ]
    [ "$stderr" = "translations:" ]
}
