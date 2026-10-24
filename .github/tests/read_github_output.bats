#!/usr/bin/env bats
# Unit tests for .github/actions/_lib/read_github_output.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/read_github_output.bats
#
# Each case runs the lib in a subshell (bash -c) because the helper exits on
# failure, which would otherwise take the test runner down with it. The
# output files are written as github_output.sh writes them, the multi-line
# form included.

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

LIB="$BATS_TEST_DIRNAME/../actions/_lib/read_github_output.sh"

setup() {

    OUTPUT_FILE="$BATS_TEST_TMPDIR/github_output"
}

# Sources the lib and reads the given key from the output file.
read_key() {

    run --separate-stderr bash -c "set -euo pipefail
                                   SCRIPT_NAME='show-download-counts'
                                   source '$LIB'
                                   read_github_output '$OUTPUT_FILE' '$1'"
}

@test "reads a single-line value" {

    printf '%s\n' "mod-id=kmu" "zip-name-regex=^KMU-(?<locale>.+)\.zip$" > "$OUTPUT_FILE"

    read_key "zip-name-regex"

    [ "$status" -eq 0 ]
    [ "$output" = '^KMU-(?<locale>.+)\.zip$' ]
}

@test "reads an empty value as empty" {

    printf '%s\n' "default-locale=" > "$OUTPUT_FILE"

    read_key "default-locale"

    [ "$status" -eq 0 ]
    [ "$output" = "" ]
}

@test "reads a value holding = whole" {

    printf '%s\n' "label=a=b" > "$OUTPUT_FILE"

    read_key "label"

    [ "$output" = "a=b" ]
}

@test "reads a multi-line value, a blank line kept" {

    printf '%s\n' "note<<EOF_1" "first" "" "third" "EOF_1" > "$OUTPUT_FILE"

    read_key "note"

    [ "$status" -eq 0 ]
    [ "$output" = $'first\n\nthird' ]
}

@test "reads an empty multi-line value as empty" {

    printf '%s\n' "translations<<EOF_1" "" "EOF_1" > "$OUTPUT_FILE"

    read_key "translations"

    [ "$status" -eq 0 ]
    [ "$output" = "" ]
}

# A line inside another key's value is that value's text, whatever it says.
@test "never reads a line inside another key's multi-line value as a key" {

    printf '%s\n' "note<<EOF_1" "label=wrong" "EOF_1" "label=right" > "$OUTPUT_FILE"

    read_key "label"

    [ "$output" = "right" ]
}

@test "reads the last value of a key written twice, as the runner does" {

    printf '%s\n' "label=first" "label=second" > "$OUTPUT_FILE"

    read_key "label"

    [ "$output" = "second" ]
}

@test "does not read a key that only starts with the asked one" {

    printf '%s\n' "zip-name=KMU-0.2.1.zip" > "$OUTPUT_FILE"

    read_key "zip-name-regex"

    [ "$status" -ne 0 ]
}

@test "fails a key never written, naming it and the file" {

    printf '%s\n' "mod-id=kmu" > "$OUTPUT_FILE"

    read_key "zip-name-regex"

    [ "$status" -ne 0 ]
    [ "$stderr" = "show-download-counts: output 'zip-name-regex' was never written to $OUTPUT_FILE" ]
    [ "$output" = "" ]
}
