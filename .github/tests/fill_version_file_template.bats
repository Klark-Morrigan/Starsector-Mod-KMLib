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
    # The well-formed pair, so each case below states only what it varies.
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8"
    write_template "kmu"
}

teardown() {
    rm -rf "$WORK_DIR"
}

# Writes a mod_info.json fixture into the temp working dir. The jar path is
# what the zip name is derived from when no zip name is passed, so it is
# stated separately by the cases that exercise that derivation.
write_mod_info() {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{
  "id": "$1",
  "name": "$2",
  "version": "$3",
  "gameVersion": "$4",
  "jars": ["${5:-jars/KMU.jar}"]
}
EOF
}

# Makes the temp working dir a git checkout with the given origin remote,
# which is where the script reads the publishing repository from when
# GITHUB_REPOSITORY is unset - a local build rather than an Actions run.
init_git_origin() {
    git -C "$WORK_DIR" init --quiet
    git -C "$WORK_DIR" remote add origin "$1"
}

# Writes a template in the shape a mod commits: every release-varying
# value a token, every fixed value stated outright. The .template suffix
# is the committed name; the bare .version name is generated output only.
write_template() {
    cat > "$WORK_DIR/$1.version.template" <<EOF
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
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    [ "$(read_generated '.masterVersionFile')" = "https://example.invalid/master/kmu.version" ]
    [ "$(read_generated '.changelogURL')" = "https://example.invalid/CHANGELOG.md" ]
}

@test "carries keys this script knows nothing about" {
    cat > "$WORK_DIR/kmu.version.template" <<EOF
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

@test "reads the mod root given to it, and writes from where the caller stands" {
    # The release pipeline's shape: the mod checked out one level down, the
    # caller standing above it. Both paths below are stated the way that
    # caller states them - the template found under the mod root, the output
    # written relative to the caller, not to the root it was read from.
    mkdir -p "$WORK_DIR/checkout"
    mv "$WORK_DIR/mod_info.json" "$WORK_DIR/kmu.version.template" "$WORK_DIR/checkout/"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME" "checkout"
    [ "$status" -eq 0 ]
    [ -f "$WORK_DIR/$OUTPUT_FILE" ]
    [ ! -f "$WORK_DIR/checkout/$OUTPUT_FILE" ]
    [ "$(read_generated '.modName')" = "Klark Morrigan's Universe" ]
}

@test "fails when the mod root does not exist" {
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME" "absent"
    # Named as the wrong directory rather than reported as a missing
    # mod_info.json, which is the same symptom from a different cause.
    [ "$status" -ne 0 ]
    [[ "$output" == *"mod root absent not found"* ]]
}

@test "creates the output directory when it does not exist" {
    cd "$WORK_DIR"
    run bash "$SCRIPT" "dist/KMU/$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    [ -f "$WORK_DIR/dist/KMU/$OUTPUT_FILE" ]
}

@test "fails on a token this script does not substitute" {
    cat > "$WORK_DIR/kmu.version.template" <<EOF
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
    rm "$WORK_DIR/kmu.version.template"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"template kmu.version.template not found"* ]]
}

@test "fails when the template is missing masterVersionFile" {
    cat > "$WORK_DIR/kmu.version.template" <<EOF
{ "modName": "{{modName}}" }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'masterVersionFile'"* ]]
}

@test "fails when the version does not split into three numeric parts" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3a" "0.98a-RC8"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"does not split into three numeric parts"* ]]
}

@test "fails when mod_info.json is missing .name" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "version": "1.2.3", "gameVersion": "0.98a-RC8" }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'name'"* ]]
}

@test "fails when mod_info.json is missing .gameVersion" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "name": "Klark Morrigan's Universe", "version": "1.2.3" }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'gameVersion'"* ]]
}

@test "fails when mod_info.json is missing .id" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "name": "Klark Morrigan's Universe", "version": "1.2.3", "gameVersion": "0.98a-RC8" }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    # The id is what names the template, so an absent one would otherwise
    # be reported as a missing ".version" file rather than as the real fault.
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'id'"* ]]
}

@test "fails when mod_info.json is absent" {
    rm "$WORK_DIR/mod_info.json"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"mod_info.json not found"* ]]
}

@test "fails when no output path is given" {
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"output path argument required"* ]]
}

@test "derives the zip name from mod_info.json when none is given" {
    # The local build has no read-mod-info output to pass, so the name comes
    # from the jar the mod ships, by the rule the release reads it by.
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8" "jars/DerivedName.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE"
    [ "$status" -eq 0 ]
    expected="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/DerivedName-1.2.3.zip"
    [ "$(read_generated '.directDownloadURL')" = "$expected" ]
}

@test "prefers the zip name it is given over the one it would derive" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.2.3" "0.98a-RC8" "jars/DerivedName.jar"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    # The release states the name of the asset it is about to upload, and that
    # name wins: the URL has to point at what is published, not at what the
    # jar implies.
    [[ "$(read_generated '.directDownloadURL')" == *"/KMU-1.2.3.zip" ]]
}

@test "fails when no zip name is given and mod_info.json lists no jar" {
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "name": "Klark Morrigan's Universe", "version": "1.2.3", "gameVersion": "0.98a-RC8" }
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE"
    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'jars[0]'"* ]]
}

@test "reads the publishing repository from the origin remote when GITHUB_REPOSITORY is unset" {
    init_git_origin "https://github.com/Klark-Morrigan/Starsector-Mod-KMU.git"
    cd "$WORK_DIR"
    # A local build has no Actions runtime to export the variable, and the
    # checkout's own remote is the only answer that cannot name a repository
    # this clone does not push to.
    run env -u GITHUB_REPOSITORY bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    expected="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/KMU-1.2.3.zip"
    [ "$(read_generated '.directDownloadURL')" = "$expected" ]
}

@test "reads an ssh origin remote the same way as an https one" {
    init_git_origin "git@github.com:Klark-Morrigan/Starsector-Mod-KMU.git"
    cd "$WORK_DIR"
    run env -u GITHUB_REPOSITORY bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    expected="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/KMU-1.2.3.zip"
    [ "$(read_generated '.directDownloadURL')" = "$expected" ]
}

@test "prefers GITHUB_REPOSITORY over the origin remote" {
    init_git_origin "https://github.com/someone-else/a-fork.git"
    cd "$WORK_DIR"
    # A release runs against a checkout whose remote is whatever the runner
    # cloned; the variable is what states which repository is publishing.
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    [[ "$(read_generated '.directDownloadURL')" == *"/Klark-Morrigan/Starsector-Mod-KMU/"* ]]
}

@test "fails when GITHUB_REPOSITORY is unset and there is no origin remote" {
    cd "$WORK_DIR"
    # The download URL has no third source, and a run outside both Actions and
    # a checkout is exactly where it would otherwise be built against an empty
    # owner/repo.
    run env -u GITHUB_REPOSITORY bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"<owner>/<repo>"* ]]
    [ ! -f "$WORK_DIR/$OUTPUT_FILE" ]
}

@test "fails when the origin remote names no owner" {
    init_git_origin "a-local-clone"
    cd "$WORK_DIR"
    run env -u GITHUB_REPOSITORY bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -ne 0 ]
    [[ "$output" == *"cannot read <owner>/<repo> out of the origin remote"* ]]
}

@test "drops leading zeros from a version component" {
    write_mod_info "kmu" "Klark Morrigan's Universe" "1.02.3" "0.98a-RC8"
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    [ "$status" -eq 0 ]
    # "02" is a legal thing to write in a version string but not legal JSON,
    # so the component is canonicalised rather than emitted verbatim.
    [ "$(read_generated '.modVersion.minor')" = "2" ]
    [ "$(read_generated '.modVersion.minor | type')" = "number" ]
}

@test "fails on a token embedded in a longer value" {
    cat > "$WORK_DIR/kmu.version.template" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/kmu.version",
  "modName": "KMU v{{major}}"
}
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    # Substitution is by whole value, which is what lets a token carry a
    # non-string type. A partial one cannot be honoured, so it is refused
    # rather than left in place for a player to read.
    [ "$status" -ne 0 ]
    [[ "$output" == *"unreplaced token(s): KMU v{{major}}"* ]]
}

@test "escapes a mod name holding quotes and backslashes" {
    # Written literally rather than through write_mod_info, whose heredoc
    # expands its arguments - the point here is the exact bytes in the file.
    cat > "$WORK_DIR/mod_info.json" <<'EOF'
{
  "id": "kmu",
  "name": "He said \"hi\" C:\\x",
  "version": "1.2.3",
  "gameVersion": "0.98a-RC8"
}
EOF
    cd "$WORK_DIR"
    run bash "$SCRIPT" "$OUTPUT_FILE" "$ZIP_NAME"
    # Substituting through jq rather than sed is what keeps a name like this
    # from breaking the JSON it lands in.
    [ "$status" -eq 0 ]
    [ "$(read_generated '.modName')" = 'He said "hi" C:\x' ]
}
