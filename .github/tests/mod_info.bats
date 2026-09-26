#!/usr/bin/env bats
# Unit tests for .github/actions/_lib/mod_info.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/mod_info.bats
#
# The lib is sourced by several action scripts, so its checks are tested here
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

@test "mod_info_locate_file points at mod_info.json when it is the only file" {
    echo '{}' > "$WORK_DIR/mod_info.json"
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" 'mod_info_locate_file; echo "${MOD_INFO_FILE}"'
    [ "$status" -eq 0 ]
    [ "$output" = "mod_info.json" ]
}

@test "mod_info_locate_file points at the base when one is committed" {
    echo '{}' > "$WORK_DIR/mod_info.json"
    echo '{}' > "$WORK_DIR/mod_info.base.json"
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" 'mod_info_locate_file; echo "${MOD_INFO_FILE}"'
    # Beside a base, mod_info.json is a build output on whichever locale was
    # last written, so it is never the one read.
    [ "$status" -eq 0 ]
    [ "$output" = "mod_info.base.json" ]
}

@test "mod_info_locate_file points at the base on a checkout that has built nothing" {
    echo '{}' > "$WORK_DIR/mod_info.base.json"
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" 'mod_info_locate_file; echo "${MOD_INFO_FILE}"'
    # The release reads a fresh checkout, where the generated file does not
    # exist yet.
    [ "$status" -eq 0 ]
    [ "$output" = "mod_info.base.json" ]
}

@test "mod_info_locate_file fails naming both files when neither is present" {
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file"
    [ "$status" -ne 0 ]
    [[ "$output" == *"neither mod_info.base.json nor mod_info.json found"* ]]
}

@test "mod_info_locate_file names the sourcing script in its message" {
    cd "$WORK_DIR"
    run_in_lib "fill_version_file_template" "mod_info_locate_file"
    # A shared lib reporting under its own name would tell a reader nothing
    # about which release step actually stopped.
    [[ "$output" == "fill_version_file_template: "* ]]
}

@test "mod_info_locate_file falls back to a default name when unset" {
    cd "$WORK_DIR"
    run bash -c "set -euo pipefail; source '$LIB'; mod_info_locate_file"
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

@test "mod_info_derive_mod_folder_name strips the jar extension from a nested path" {
    run_in_lib "read_mod_info" "mod_info_derive_mod_folder_name 'jars/KMLib.jar'"
    [ "$status" -eq 0 ]
    [ "$output" = "KMLib" ]
}

@test "mod_info_derive_mod_folder_name keeps a name that carries no extension" {
    run_in_lib "read_mod_info" "mod_info_derive_mod_folder_name 'jars/KMLib'"
    [ "$status" -eq 0 ]
    [ "$output" = "KMLib" ]
}

@test "mod_info_derive_zip_name joins the folder name and the version" {
    run_in_lib "read_mod_info" "mod_info_derive_zip_name 'jars/KMLib.jar' '0.1.0'"
    [ "$status" -eq 0 ]
    # The release uploads an asset under this name and the generated version
    # file points a download URL at it, so both read the rule from here.
    [ "$output" = "KMLib-0.1.0.zip" ]
}

@test "mod_info_derive_zip_name suffixes a locale's tag after the version" {
    run_in_lib "read_mod_info" "mod_info_derive_zip_name 'jars/KMU.jar' '0.2.0' 'zh-hans'"
    [ "$status" -eq 0 ]
    [ "$output" = "KMU-0.2.0-zh-hans.zip" ]
}

@test "mod_info_derive_version_file_name names the file after the mod id" {
    run_in_lib "read_mod_info" "mod_info_derive_version_file_name 'kmu'"
    [ "$status" -eq 0 ]
    [ "$output" = "kmu.version" ]
}

@test "mod_info_derive_version_file_name suffixes a locale's tag" {
    run_in_lib "read_mod_info" "mod_info_derive_version_file_name 'kmu' 'zh-hans'"
    [ "$status" -eq 0 ]
    [ "$output" = "kmu-zh-hans.version" ]
}

@test "mod_info_format_locale_suffix emits nothing for no tag" {
    run_in_lib "read_mod_info" "mod_info_format_locale_suffix ''"
    # A mod keeping no locales releases under the names it always has.
    [ "$status" -eq 0 ]
    [ "$output" = "" ]
}

# Writes a mod_info.json whose dependencies array is the given raw JSON
# fragment, so a case can state the entries it needs without templating jq
# from inside bats. No argument writes a file carrying no dependencies key.
write_mod_info_with_dependencies() {
    local dependenciesBody="${1:-}"
    if [ -n "$dependenciesBody" ]; then
        cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "consumer", "version": "1.2.0", "dependencies": [ $dependenciesBody ] }
EOF
    else
        cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "consumer", "version": "1.2.0" }
EOF
    fi
}

@test "mod_info_has_dependency finds a declared dependency by id" {
    write_mod_info_with_dependencies '{ "id": "kmlib", "version": "1.0.0" }'
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_has_dependency 'kmlib'"
    [ "$status" -eq 0 ]
    [ "$output" = "true" ]
}

@test "mod_info_has_dependency reports false for an undeclared id" {
    write_mod_info_with_dependencies '{ "id": "lw_lazylib", "name": "LazyLib" }'
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_has_dependency 'kmlib'"
    [ "$status" -eq 0 ]
    [ "$output" = "false" ]
}

@test "mod_info_has_dependency reports false when there is no dependencies key" {
    # KMLib's own mod_info.json is this shape. Without jq's `// []` guard the
    # missing key is an error rather than an empty result, which would fail
    # the one release that legitimately declares no dependency.
    write_mod_info_with_dependencies
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_has_dependency 'kmlib'"
    [ "$status" -eq 0 ]
    [ "$output" = "false" ]
}

@test "mod_info_has_dependency finds an entry that states no version" {
    # The state validate-versioning fails on: declared but unpinned. It has
    # to read as present here, or that rule never fires.
    write_mod_info_with_dependencies '{ "id": "kmlib", "name": "KMLib" }'
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_has_dependency 'kmlib'"
    [ "$status" -eq 0 ]
    [ "$output" = "true" ]
}

@test "mod_info_read_dependency_version echoes the pinned version" {
    write_mod_info_with_dependencies '{ "id": "kmlib", "version": "1.0.0" }'
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"
    [ "$status" -eq 0 ]
    [ "$output" = "1.0.0" ]
}

@test "mod_info_read_dependency_version picks the entry matching the id" {
    write_mod_info_with_dependencies \
        '{ "id": "lw_lazylib", "version": "2.9.0" },
         { "id": "kmlib", "version": "1.0.0" }'
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"
    [ "$status" -eq 0 ]
    [ "$output" = "1.0.0" ]
}

@test "mod_info_read_dependency_version echoes nothing for an undeclared id" {
    write_mod_info_with_dependencies '{ "id": "lw_lazylib", "name": "LazyLib" }'
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"
    [ "$status" -eq 0 ]
    [ "$output" = "" ]
}

@test "mod_info_read_dependency_version echoes nothing when there is no dependencies key" {
    write_mod_info_with_dependencies
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"
    [ "$status" -eq 0 ]
    [ "$output" = "" ]
}

@test "mod_info_read_dependency_version echoes empty, not jq's null, for a versionless entry" {
    write_mod_info_with_dependencies '{ "id": "kmlib", "name": "KMLib" }'
    cd "$WORK_DIR"
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"
    [ "$status" -eq 0 ]
    # "null" would sail through an emptiness check and reach the release body
    # as a version string, so the `// ""` fallback is the tested behaviour.
    [ "$output" = "" ]
}
