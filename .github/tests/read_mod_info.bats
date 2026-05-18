#!/usr/bin/env bats
# Unit tests for .github/actions/read-mod-info/scripts/read_mod_info.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/read_mod_info.bats
#
# Uses a real (small) mod_info.json fixture and a real jq, since jq is
# required in CI anyway and stubbing it would weaken the test. The script
# is run from a temp working directory so it cannot accidentally read the
# repository's own mod_info.json.

SCRIPT="$BATS_TEST_DIRNAME/../actions/read-mod-info/scripts/read_mod_info.sh"

setup() {
    WORK_DIR="$(mktemp -d)"
    export GITHUB_OUTPUT
    GITHUB_OUTPUT="$(mktemp)"
}

teardown() {
    rm -rf "$WORK_DIR"
    rm -f "$GITHUB_OUTPUT"
}

# Writes a mod_info.json fixture into the temp working dir.
write_mod_info() {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{
  "id": "$1",
  "version": "$2",
  "jars": ["$3"]
}
EOF
}

@test "emits all derived values for a well-formed mod_info.json" {
    write_mod_info "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "mod-id=kmu"                        "$GITHUB_OUTPUT"
    grep -qx "version=0.1.0"                     "$GITHUB_OUTPUT"
    grep -qx "runner-label=kmu-runner"           "$GITHUB_OUTPUT"
    grep -qx "dist-dir=dist/kmu/"                "$GITHUB_OUTPUT"
    grep -qx "zip-name=kmu-0.1.0.zip"            "$GITHUB_OUTPUT"
    grep -qx "jar-source=jars/KMU.jar"           "$GITHUB_OUTPUT"
}

@test "works for KMLib itself (no transformation of id)" {
    write_mod_info "kmlib" "1.0.0" "jars/KMLib.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "mod-id=kmlib"                      "$GITHUB_OUTPUT"
    grep -qx "runner-label=kmlib-runner"         "$GITHUB_OUTPUT"
    grep -qx "dist-dir=dist/kmlib/"              "$GITHUB_OUTPUT"
    grep -qx "zip-name=kmlib-1.0.0.zip"          "$GITHUB_OUTPUT"
    grep -qx "jar-source=jars/KMLib.jar"         "$GITHUB_OUTPUT"
}

@test "fails when mod_info.json is absent" {
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"mod_info.json not found"* ]]
}

@test "fails when .id is missing" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "version": "0.1.0", "jars": ["jars/X.jar"] }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'id'"* ]]
}

@test "fails when .version is missing" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "jars": ["jars/X.jar"] }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'version'"* ]]
}

@test "fails when .jars is missing or empty" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "version": "0.1.0", "jars": [] }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'jars[0]'"* ]]
}
