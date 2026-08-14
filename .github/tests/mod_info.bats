#!/usr/bin/env bats
# Unit tests for .github/actions/_lib/mod_info.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/mod_info.bats
#
# The lib is sourced by four action scripts, so its checks are tested here
# once rather than through each of them. The action suites still cover their
# own use of it - what matters there is which fields a given script requires,
# not that the check works.
#
# Each case runs the lib in a subshell (bash -c) because both helpers exit on
# failure, which would otherwise take the test runner down with them.

LIB="$BATS_TEST_DIRNAME/../actions/_lib/mod_info.sh"

setup() {
    WORK_DIR="$(mktemp -d)"
}

teardown() {
    rm -rf "$WORK_DIR"
}

# Sources the lib with the given SCRIPT_NAME and runs one call in it.
run_in_lib() {
    local scriptName="$1"
    shift
    run bash -c "set -euo pipefail
                 SCRIPT_NAME='${scriptName}'
                 source '$LIB'
                 $*"
}

@test "mod_info_require_file passes when mod_info.json is present" {
    echo '{}' > "$WORK_DIR/mod_info.json"
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_require_file"
    [ "$status" -eq 0 ]
}

@test "mod_info_require_file fails when mod_info.json is absent" {
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_require_file"
    [ "$status" -ne 0 ]
    [[ "$output" == *"mod_info.json not found"* ]]
}

@test "mod_info_require_file names the sourcing script in its message" {
    cd "$WORK_DIR"
    run_in_lib "fill_version_file_template" "mod_info_require_file"
    # A shared lib reporting under its own name would tell a reader nothing
    # about which release step actually stopped.
    [[ "$output" == "fill_version_file_template: "* ]]
}

@test "mod_info_require_file falls back to a default name when unset" {
    cd "$WORK_DIR"
    run bash -c "set -euo pipefail; source '$LIB'; mod_info_require_file"
    # Under set -u an unset SCRIPT_NAME would crash the reporting itself,
    # losing the real failure behind an unbound-variable error.
    [ "$status" -ne 0 ]
    [[ "$output" == "mod_info: "* ]]
}

@test "mod_info_require_fields passes when every field has a value" {
    run_in_lib "read_mod_info" \
        "mod_info_require_fields 'id:kmu' 'version:1.2.3' 'jars[0]:jars/KMU.jar'"
    [ "$status" -eq 0 ]
}

@test "mod_info_require_fields fails on an empty value" {
    run_in_lib "read_mod_info" "mod_info_require_fields 'id:kmu' 'version:'"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'version'"* ]]
}

@test "mod_info_require_fields fails on jq's null" {
    run_in_lib "read_mod_info" "mod_info_require_fields 'name:null'"
    # jq -r prints absent fields as the string "null", so this is what a
    # missing field actually looks like by the time it reaches the check.
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'name'"* ]]
}

@test "mod_info_require_fields reports the first absent field only" {
    run_in_lib "read_mod_info" \
        "mod_info_require_fields 'id:' 'version:' 'gameVersion:'"
    [ "$status" -ne 0 ]
    [[ "$output" == *"'id'"* ]]
    [[ "$output" != *"'version'"* ]]
}

@test "mod_info_require_fields keeps values holding colons intact" {
    # URLs are the common case; splitting on the last colon would truncate
    # them and report a present field as missing.
    run_in_lib "read_mod_info" \
        "mod_info_require_fields 'masterVersionFile:https://example.invalid/x.version'"
    [ "$status" -eq 0 ]
}

@test "mod_info_require_fields uses the field label verbatim in its message" {
    run_in_lib "read_mod_info" "mod_info_require_fields 'jars[0]:null'"
    # The label is written as it appears in mod_info.json so a reader can
    # go straight to it, rather than as the shell variable holding it.
    [[ "$output" == *"missing required field 'jars[0]'"* ]]
}
