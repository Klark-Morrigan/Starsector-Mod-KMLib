#!/usr/bin/env bats
# Unit tests for .github/actions/_lib/json.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/json.bats
#
# Each case runs the lib in a subshell (bash -c) because both helpers exit on
# failure, which would otherwise take the test runner down with them.

LIB="$BATS_TEST_DIRNAME/../actions/_lib/json.sh"

# Sources the lib and runs the given statements in it.
run_in_lib() {
    run bash -c "set -euo pipefail
                 SCRIPT_NAME='package_release'
                 source '$LIB'
                 $*"
}

@test "json_require_array passes an array" {
    run_in_lib "json_require_array '[1]' 'LOCALES'"
    [ "$status" -eq 0 ]
}

@test "json_require_array fails an object, naming it by its label" {
    run_in_lib "json_require_array '{}' 'LOCALES'"
    [ "$status" -ne 0 ]
    [[ "$output" == "package_release: LOCALES is not a JSON array: {}" ]]
}

@test "json_require_array fails text that is not JSON" {
    run_in_lib "json_require_array 'en' 'LOCALES'"
    [ "$status" -ne 0 ]
}

@test "json_read_lines reads one element per line" {
    run_in_lib "json_read_lines tags -r '.[]' <<< '[\"en\",\"zh-hans\"]'
                printf '%s|' \"\${tags[@]}\"; echo \"\${#tags[@]}\""
    [ "$status" -eq 0 ]
    [ "$output" = "en|zh-hans|2" ]
}

@test "json_read_lines leaves the array empty when jq prints nothing" {
    run_in_lib "json_read_lines tags -r '.[]' <<< '[]'
                echo \"\${#tags[@]}\""
    # A here-string of nothing reads as one empty line, which a loop over the
    # array would take for a locale with no tag.
    [ "$status" -eq 0 ]
    [ "$output" = "0" ]
}

@test "json_read_lines drops carriage returns" {
    run_in_lib "json_read_lines tags -r '.[]' <<< '[\"en\\r\"]'
                [[ \"\${tags[0]}\" == 'en' ]]"
    # jq on Windows ends its lines with CRLF; a value carrying one fails every
    # comparison and lands in every file name built from it.
    [ "$status" -eq 0 ]
}

@test "json_read_lines fails the run when jq fails" {
    run_in_lib "json_read_lines tags -r '.[]' <<< 'not json'; echo reached"
    [ "$status" -ne 0 ]
    [[ "$output" != *"reached"* ]]
}
