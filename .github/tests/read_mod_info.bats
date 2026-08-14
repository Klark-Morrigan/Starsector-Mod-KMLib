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

# Reads one emitted key back out of $GITHUB_OUTPUT, for values whose exact
# text is awkward to assert with grep -qx (JSON carrying quotes and braces).
read_output_value() {
    sed -n "s/^$1=//p" "$GITHUB_OUTPUT"
}

@test "emits all derived values for a well-formed mod_info.json" {
    write_mod_info "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "mod-id=kmu"                        "$GITHUB_OUTPUT"
    grep -qx "version=0.1.0"                     "$GITHUB_OUTPUT"
    grep -qx "runner-label=kmu-runner"           "$GITHUB_OUTPUT"
    grep -qx "mod-folder-name=KMU"               "$GITHUB_OUTPUT"
    grep -qx "dist-dir=dist/KMU/"                "$GITHUB_OUTPUT"
    grep -qx "zip-name=KMU-0.1.0.zip"            "$GITHUB_OUTPUT"
    grep -qx "jar-source=jars/KMU.jar"           "$GITHUB_OUTPUT"
    grep -qx "prerelease=false"                  "$GITHUB_OUTPUT"
}

@test "defaults prerelease to false when the key is absent" {
    write_mod_info "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    # The fixture writes no .prerelease, which is the shape every mod has
    # today - the release must stay a normal one without any opt-out.
    grep -qx "prerelease=false"                  "$GITHUB_OUTPUT"
}

@test "emits prerelease=true when the mod opts in" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "version": "0.1.0", "jars": ["jars/KMU.jar"], "prerelease": true }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "prerelease=true"                   "$GITHUB_OUTPUT"
}

@test "accepts prerelease as a quoted string, matching Starsector's style" {
    # mod_info.json states booleans as strings ("utility": "true"), so a mod
    # author following the file's own convention has to work too.
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "version": "0.1.0", "jars": ["jars/KMU.jar"], "prerelease": "true" }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "prerelease=true"                   "$GITHUB_OUTPUT"
}

@test "emits prerelease=false when the mod opts out explicitly" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "version": "0.1.0", "jars": ["jars/KMU.jar"], "prerelease": false }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "prerelease=false"                  "$GITHUB_OUTPUT"
}

@test "names the shipped folder after the jar, not after the mod id" {
    write_mod_info "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    # The id is lowercase and the folder is not: a build that looks for
    # <mods>/KMU/jars/KMU.jar has to find what the zip unpacks, on a
    # case-sensitive filesystem as much as on Windows.
    grep -qx "mod-id=kmu"                        "$GITHUB_OUTPUT"
    grep -qx "mod-folder-name=KMU"               "$GITHUB_OUTPUT"
}

@test "strips only the jar extension from a nested jar path" {
    write_mod_info "kmu" "0.1.0" "jars/internal/KMU-Core.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "mod-folder-name=KMU-Core"          "$GITHUB_OUTPUT"
    grep -qx "dist-dir=dist/KMU-Core/"           "$GITHUB_OUTPUT"
    grep -qx "zip-name=KMU-Core-0.1.0.zip"       "$GITHUB_OUTPUT"
}

@test "works for KMLib itself (no transformation of id)" {
    write_mod_info "kmlib" "1.0.0" "jars/KMLib.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "mod-id=kmlib"                      "$GITHUB_OUTPUT"
    grep -qx "runner-label=kmlib-runner"         "$GITHUB_OUTPUT"
    grep -qx "mod-folder-name=KMLib"             "$GITHUB_OUTPUT"
    grep -qx "dist-dir=dist/KMLib/"              "$GITHUB_OUTPUT"
    grep -qx "zip-name=KMLib-1.0.0.zip"          "$GITHUB_OUTPUT"
    grep -qx "jar-source=jars/KMLib.jar"         "$GITHUB_OUTPUT"
}

@test "emits Common-Java and KMLib as siblings for a consumer mod" {
    write_mod_info "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    siblings="$(read_output_value "sibling-checkouts")"
    [ "$(jq -r 'length' <<< "$siblings")" -eq 2 ]
    [ "$(jq -r '.[0].repo' <<< "$siblings")" = "Klark-Morrigan/Common-Java" ]
    [ "$(jq -r '.[0].path' <<< "$siblings")" = "Common-Java" ]
    [ "$(jq -r '.[1].repo' <<< "$siblings")" = "Klark-Morrigan/Starsector-Mod-KMLib" ]
    [ "$(jq -r '.[1].path' <<< "$siblings")" = "Starsector-Mod-KMLib" ]
}

@test "omits the KMLib sibling when the mod being built is KMLib" {
    write_mod_info "kmlib" "0.1.0" "jars/KMLib.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    siblings="$(read_output_value "sibling-checkouts")"
    [ "$(jq -r 'length' <<< "$siblings")" -eq 1 ]
    [ "$(jq -r '.[0].repo' <<< "$siblings")" = "Klark-Morrigan/Common-Java" ]
}

@test "emits the sibling set on a single line so it parses as one output key" {
    write_mod_info "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    [ "$(grep -c '^sibling-checkouts=' "$GITHUB_OUTPUT")" -eq 1 ]
    [ "$(wc -l < "$GITHUB_OUTPUT")" -eq 9 ]
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
