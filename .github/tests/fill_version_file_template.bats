#!/usr/bin/env bats
# Unit tests for .github/actions/fill-version-file-template/scripts/fill_version_file_template.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/fill_version_file_template.bats
#
# Uses real (small) fixtures and a real jq, since jq is required in CI
# anyway and stubbing it would weaken the test - the emitted JSON's types
# are part of what is being asserted. The script is run from a temp working
# directory so it cannot accidentally read the repository's own files.

SCRIPT="$BATS_TEST_DIRNAME/../actions/fill-version-file-template/scripts/fill_version_file_template.sh"

OUTPUT_FILE="generated.version"
ZIP_NAME="KMU-1.2.3.zip"

setup() {
    WORK_DIR="$(mktemp -d)"
    # The script builds the download URL from the publishing repository,
    # which the Actions runtime exports; the tests supply it the same way.
    export GITHUB_REPOSITORY="Klark-Morrigan/Starsector-Mod-KMU"
}

teardown() {
    rm -rf "$WORK_DIR"
}

# Writes a mod_info.json fixture into the temp working dir.
write_mod_info() {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{
  "id": "$1",
  "name": "$2",
  "version": "$3",
  "gameVersion": "$4"
}
EOF
}

# Writes a template in the shape a mod commits: every release-varying
# value a token, every fixed value stated outright.
write_template() {
    cat > "$WORK_DIR/$1.version" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/$1.version",
  "modName": "{{modName}}",
  "modVersion":
  {
    "major": "{{major}}",
    "minor": "{{minor}}",
    "patch": "{{patch}}"
  },
  "starsectorVersion": "{{starsectorVersion}}",
  "directDownloadURL": "{{directDownloadURL}}",
  "changelogURL": "https://example.invalid/CHANGELOG.md"
}
EOF
}

# Reads one field out of the generated file with jq.
read_generated() {
    jq -r "$1" "$WORK_DIR/$OUTPUT_FILE"
}

@test "substitutes every token from mod_info.json" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    write_template "kmu"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    [ "$(read_generated '.modName')" = "Klark Morrigan's Universe" ]
    [ "$(read_generated '.starsectorVersion')" = "0.98a-RC8" ]
    [ "$(read_generated '.modVersion.major')" = "1" ]
    [ "$(read_generated '.modVersion.minor')" = "2" ]
    [ "$(read_generated '.modVersion.patch')" = "3" ]
}

@test "substitutes the version components as JSON numbers, not strings" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    write_template "kmu"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    # The template writes these as quoted tokens because JSON has no
    # other way to write one. VersionChecker compares them numerically, so
    # coming out as numbers rather than quoted digits is the contract.
    [ "$(read_generated '.modVersion.major | type')" = "number" ]
    [ "$(read_generated '.modVersion.minor | type')" = "number" ]
    [ "$(read_generated '.modVersion.patch | type')" = "number" ]
}

@test "leaves values that are not tokens untouched" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    write_template "kmu"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    [ "$(read_generated '.masterVersionFile')" = "https://example.invalid/master/kmu.version" ]
    [ "$(read_generated '.changelogURL')" = "https://example.invalid/CHANGELOG.md" ]
}

@test "carries keys this script knows nothing about" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    cat > "$WORK_DIR/kmu.version" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/kmu.version",
  "modName": "{{modName}}",
  "modThreadId": 12345
}
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    # The template states the published shape; a mod adding a forum
    # thread id or any other VersionChecker field needs no change here.
    [ "$(read_generated '.modThreadId')" = "12345" ]
    [ "$(read_generated '.modThreadId | type')" = "number" ]
}

@test "builds the download URL from the publishing repo, version, and zip name" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    write_template "kmu"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    expected="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/KMU-1.2.3.zip"
    [ "$(read_generated '.directDownloadURL')" = "$expected" ]
}

@test "finds the template by mod id, not by a fixed name" {
    write_mod_info "kmlib" "Klark Morrigan's Library" "0.1.0" "0.98a-RC8"
    write_template "kmlib"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "KMLib-0.1.0.zip"
    [ "$status" -eq 0 ]
    [ "$(read_generated '.masterVersionFile')" = "https://example.invalid/master/kmlib.version" ]
}

@test "creates the output directory when it does not exist" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    write_template "kmu"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "dist/KMU/$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    [ -f "$WORK_DIR/dist/KMU/$OUTPUT_FILE" ]
}

@test "fails on a token this script does not substitute" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    cat > "$WORK_DIR/kmu.version" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/kmu.version",
  "modName": "{{modname}}"
}
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    # A mistyped token would otherwise ship the literal braces to players,
    # since nothing reads this file until an update checker does.
    [[ "$output" == *"unreplaced token(s): {{modname}}"* ]]
    [ ! -f "$WORK_DIR/$OUTPUT_FILE" ]
}

@test "fails when the template is absent" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"template kmu.version not found"* ]]
}

@test "fails when the template is missing masterVersionFile" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    cat > "$WORK_DIR/kmu.version" <<EOF
{ "modName": "{{modName}}" }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'masterVersionFile'"* ]]
}

@test "fails when the version does not split into three numeric parts" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3a" "0.98a-RC8"
    write_template "kmu"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"does not split into three numeric parts"* ]]
}

@test "fails when mod_info.json is missing .name" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "version": "1.2.3", "gameVersion": "0.98a-RC8" }
EOF
    write_template "kmu"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'name'"* ]]
}

@test "fails when mod_info.json is missing .gameVersion" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "name": "Klark Morrigan's Universe", "version": "1.2.3" }
EOF
    write_template "kmu"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'gameVersion'"* ]]
}

@test "fails when mod_info.json is absent" {
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"mod_info.json not found"* ]]
}
