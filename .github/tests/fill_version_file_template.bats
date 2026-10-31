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

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

SCRIPT="$BATS_TEST_DIRNAME/../actions/fill-version-file-template/scripts/fill_version_file_template.sh"

OUTPUT_FILE="generated.version"
ZIP_NAME="KMU-1.2.3.zip"

setup() {

    WORK_DIR="$(mktemp -d)"

    # The script builds the download URL from the publishing repository,
    # which the Actions runtime exports; the tests supply it the same way.
    export GITHUB_REPOSITORY="Klark-Morrigan/Starsector-Mod-KMU"

    # The well-formed pair, so each case below states only what it varies.
    write_mod_info "kmu" "Fixture Mod" "1.2.3" "0.98a-RC8"
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

# Writes a localisation manifest naming the given default locale, beside the
# root changelog every localised mod keeps.
write_localisation() {

    mkdir -p "$WORK_DIR/localisation"
    cat > "$WORK_DIR/localisation/manifest.json" <<EOF
{ "defaultLocale": "$1", "files": {}, "locales": { "$1": {}, "zh-hans": {} } }
EOF
    printf '# Changelog\n' > "$WORK_DIR/CHANGELOG.md"
}

# Writes a translated locale's changelog into its bundle directory.
write_translated_changelog() {

    mkdir -p "$WORK_DIR/localisation/$1"
    printf '# Changelog\n' > "$WORK_DIR/localisation/$1/CHANGELOG.md"
}

# Reads one field out of the generated file with jq.
read_generated() {
    jq -r "$1" "$WORK_DIR/$OUTPUT_FILE"
}

@test "substitutes every token from mod_info.json" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedModName="Fixture Mod"
    expectedStarsectorVersion="0.98a-RC8"
    expectedMajor="1"
    expectedMinor="2"
    expectedPatch="3"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.modName')" = "$expectedModName" ]
    [ "$(read_generated '.starsectorVersion')" = "$expectedStarsectorVersion" ]
    [ "$(read_generated '.modVersion.major')" = "$expectedMajor" ]
    [ "$(read_generated '.modVersion.minor')" = "$expectedMinor" ]
    [ "$(read_generated '.modVersion.patch')" = "$expectedPatch" ]
}

# The template writes these as quoted tokens because JSON has no
# other way to write one. VersionChecker compares them numerically, so
# coming out as numbers rather than quoted digits is the contract.
@test "substitutes the version components as JSON numbers, not strings" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedType="number"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.modVersion.major | type')" = "$expectedType" ]
    [ "$(read_generated '.modVersion.minor | type')" = "$expectedType" ]
    [ "$(read_generated '.modVersion.patch | type')" = "$expectedType" ]
}

@test "leaves values that are not tokens untouched" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedMasterVersionFile="https://example.invalid/master/kmu.version"
    expectedChangelogUrl="https://example.invalid/CHANGELOG.md"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.masterVersionFile')" = "$expectedMasterVersionFile" ]
    [ "$(read_generated '.changelogURL')" = "$expectedChangelogUrl" ]
}

# The template states the published shape; a mod adding a forum
# thread id or any other VersionChecker field needs no change here.
@test "carries keys this script knows nothing about" {

    # ARRANGE
    cat > "$WORK_DIR/kmu.version.template" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/kmu.version",
  "modName": "{{modName}}",
  "modThreadId": 12345
}
EOF
    cd "$WORK_DIR"

    expectedThreadId="12345"
    expectedThreadIdType="number"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.modThreadId')" = "$expectedThreadId" ]
    [ "$(read_generated '.modThreadId | type')" = "$expectedThreadIdType" ]
}

@test "builds the download URL from the publishing repo, version, and zip name" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedUrl="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/KMU-1.2.3.zip"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.directDownloadURL')" = "$expectedUrl" ]
}

@test "logs the file it read and the file it wrote, with the addresses it filled in" {

    # ARRANGE
    cd "$WORK_DIR"

    downloadUrl="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/$ZIP_NAME"
    expectedLines=(
        "Reading mod_info.json."
        "Wrote $OUTPUT_FILE from kmu.version.template, version 1.2.3:"
        "  masterVersionFile: https://example.invalid/master/kmu.version"
        "  directDownloadURL: $downloadUrl"
        "  changelogURL: https://example.invalid/CHANGELOG.md"
    )

    # ACT
    run --separate-stderr env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$stderr" = "$(printf '%s\n' "${expectedLines[@]}")" ]
    [ "$output" = "" ]
}

@test "finds the template by mod id, not by a fixed name" {

    # ARRANGE
    write_mod_info "kmlib" "Fixture Library" "0.1.0" "0.98a-RC8"
    write_template "kmlib"
    cd "$WORK_DIR"

    expectedMasterVersionFile="https://example.invalid/master/kmlib.version"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="KMLib-0.1.0.zip" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.masterVersionFile')" = "$expectedMasterVersionFile" ]
}

# The release pipeline's shape: the mod checked out one level down, the
# caller standing above it. Both paths below are stated the way that
# caller states them - the template found under the mod root, the output
# written relative to the caller, not to the root it was read from.
@test "reads the mod root given to it, and writes from where the caller stands" {

    # ARRANGE
    mkdir -p "$WORK_DIR/checkout"
    mv "$WORK_DIR/mod_info.json" "$WORK_DIR/kmu.version.template" "$WORK_DIR/checkout/"
    cd "$WORK_DIR"

    expectedModName="Fixture Mod"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" MOD_ROOT="checkout" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ -f "$WORK_DIR/$OUTPUT_FILE" ]
    [ ! -f "$WORK_DIR/checkout/$OUTPUT_FILE" ]
    [ "$(read_generated '.modName')" = "$expectedModName" ]
}

# Named as the wrong directory rather than reported as a missing
# mod_info.json, which is the same symptom from a different cause.
@test "fails when the mod root does not exist" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedError="mod root absent not found"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" MOD_ROOT="absent" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "creates the output directory when it does not exist" {

    # ARRANGE
    cd "$WORK_DIR"

    # ACT
    run env OUTPUT_PATH="dist/KMU/$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ -f "$WORK_DIR/dist/KMU/$OUTPUT_FILE" ]
}

# A mistyped token would otherwise ship the literal braces to players,
# since nothing reads this file until an update checker does.
@test "fails on a token this script does not substitute" {

    # ARRANGE
    cat > "$WORK_DIR/kmu.version.template" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/kmu.version",
  "modName": "{{modname}}"
}
EOF
    cd "$WORK_DIR"

    expectedError="unreplaced token(s): {{modname}}"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
    [ ! -f "$WORK_DIR/$OUTPUT_FILE" ]
}

@test "fails when the template is absent" {

    # ARRANGE
    rm "$WORK_DIR/kmu.version.template"
    cd "$WORK_DIR"

    expectedError="template kmu.version.template not found"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "fails when the template is missing masterVersionFile" {

    # ARRANGE
    cat > "$WORK_DIR/kmu.version.template" <<EOF
{ "modName": "{{modName}}" }
EOF
    cd "$WORK_DIR"

    expectedError="missing required field 'masterVersionFile'"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "fails when the version does not split into three numeric parts" {

    # ARRANGE
    write_mod_info "kmu" "Fixture Mod" "1.2.3a" "0.98a-RC8"
    cd "$WORK_DIR"

    expectedError="does not split into three numeric parts"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "fails when mod_info.json is missing .name" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "version": "1.2.3", "gameVersion": "0.98a-RC8" }
EOF
    cd "$WORK_DIR"

    expectedError="missing required field 'name'"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "fails when mod_info.json is missing .gameVersion" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "name": "Fixture Mod", "version": "1.2.3" }
EOF
    cd "$WORK_DIR"

    expectedError="missing required field 'gameVersion'"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

# The id is what names the template, so an absent one would otherwise
# be reported as a missing ".version" file rather than as the real fault.
@test "fails when mod_info.json is missing .id" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "name": "Fixture Mod", "version": "1.2.3", "gameVersion": "0.98a-RC8" }
EOF
    cd "$WORK_DIR"

    expectedError="missing required field 'id'"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "fails when no metadata file is present" {

    # ARRANGE
    rm "$WORK_DIR/mod_info.json"
    cd "$WORK_DIR"

    expectedError="neither mod_info.base.json nor mod_info.json found"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "reads the version from the base over a generated mod_info.json" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.base.json" <<EOF
{ "id": "kmu", "name": "Fixture Mod", "version": "2.0.0", "gameVersion": "0.98a-RC8" }
EOF
    cd "$WORK_DIR"

    expectedMajor="2"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.modVersion.major')" = "$expectedMajor" ]
}

@test "names a locale's zip in the download URL when the name is derived" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedUrl="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/KMU-1.2.3-zh-hans.zip"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" LOCALE="zh-hans" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.directDownloadURL')" = "$expectedUrl" ]
}

# An install of this locale polls the copy whose download link leads back
# to its own language, not the default locale's.
@test "names a locale's own copy as the master version file" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedMasterVersionFile="https://example.invalid/master/kmu-zh-hans.version"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="KMU-1.2.3-zh-hans.zip" LOCALE="zh-hans" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.masterVersionFile')" = "$expectedMasterVersionFile" ]
}

@test "leaves the master version file as the template states it with no locale" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedMasterVersionFile="https://example.invalid/master/kmu.version"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" LOCALE="" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.masterVersionFile')" = "$expectedMasterVersionFile" ]
}

# There is no locale copy to point at beside a file this release does not
# publish, and an install polling a guessed address would never update.
@test "fails for a locale when the master version file names another file" {

    # ARRANGE
    cat > "$WORK_DIR/kmu.version.template" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/elsewhere.json",
  "modName": "{{modName}}"
}
EOF
    cd "$WORK_DIR"

    expectedError="does not end in kmu.version, so it names no copy for locale zh-hans"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" LOCALE="zh-hans" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
    [ ! -f "$WORK_DIR/$OUTPUT_FILE" ]
}

# The zip carries the translation in place of the root changelog, so the
# address an update checker shows leads to the same file.
@test "names a translated locale's own changelog" {

    # ARRANGE
    write_localisation "en"
    write_translated_changelog "zh-hans"
    cd "$WORK_DIR"

    expectedChangelogUrl="https://example.invalid/localisation/zh-hans/CHANGELOG.md"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="KMU-1.2.3-zh-hans.zip" LOCALE="zh-hans" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.changelogURL')" = "$expectedChangelogUrl" ]
}

@test "keeps the root changelog for the default locale" {

    # ARRANGE
    write_localisation "en"
    write_translated_changelog "zh-hans"
    cd "$WORK_DIR"

    expectedChangelogUrl="https://example.invalid/CHANGELOG.md"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="KMU-1.2.3-en.zip" LOCALE="en" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.changelogURL')" = "$expectedChangelogUrl" ]
}

# The zip then carries no changelog to translate, so the template's address stands.
@test "keeps the root changelog for a mod keeping none" {

    # ARRANGE
    write_localisation "en"
    write_translated_changelog "zh-hans"

    rm "$WORK_DIR/CHANGELOG.md"
    cd "$WORK_DIR"

    expectedChangelogUrl="https://example.invalid/CHANGELOG.md"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="KMU-1.2.3-zh-hans.zip" LOCALE="zh-hans" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.changelogURL')" = "$expectedChangelogUrl" ]
}

@test "adds no changelog address the template does not state" {

    # ARRANGE
    write_localisation "en"
    write_translated_changelog "zh-hans"

    cat > "$WORK_DIR/kmu.version.template" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/kmu.version",
  "modName": "{{modName}}"
}
EOF
    cd "$WORK_DIR"

    expectedHasChangelogUrl="false"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="KMU-1.2.3-zh-hans.zip" LOCALE="zh-hans" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated 'has("changelogURL")')" = "$expectedHasChangelogUrl" ]
}

# The root changelog is still a true answer, so the release goes ahead on it.
@test "warns and keeps a changelog address naming another file" {

    # ARRANGE
    write_localisation "en"
    write_translated_changelog "zh-hans"

    jq '.changelogURL = "https://example.invalid/NEWS.md"' "$WORK_DIR/kmu.version.template" \
        > "$WORK_DIR/template.json"
    mv "$WORK_DIR/template.json" "$WORK_DIR/kmu.version.template"
    cd "$WORK_DIR"

    expectedWarning="changelogURL 'https://example.invalid/NEWS.md' does not end in CHANGELOG.md"
    expectedChangelogUrl="https://example.invalid/NEWS.md"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="KMU-1.2.3-zh-hans.zip" LOCALE="zh-hans" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [[ "$output" == *"$expectedWarning"* ]]
    [ "$(read_generated '.changelogURL')" = "$expectedChangelogUrl" ]
}

# Its zip fails to package for the same reason, and the parity suite would
# have failed the checkout first.
@test "fails for a translated locale holding no changelog" {

    # ARRANGE
    write_localisation "en"
    cd "$WORK_DIR"

    expectedError="locale zh-hans has no localisation/zh-hans/CHANGELOG.md"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="KMU-1.2.3-zh-hans.zip" LOCALE="zh-hans" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
    [ ! -f "$WORK_DIR/$OUTPUT_FILE" ]
}

@test "fails naming OUTPUT_PATH when no output path is given" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedError="fill_version_file_template: OUTPUT_PATH is required"

    # ACT
    run env -u OUTPUT_PATH bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

# The local build has no read-mod-info output to pass, so the name comes
# from the jar the mod ships, by the rule the release reads it by.
@test "derives the zip name from mod_info.json when none is given" {

    # ARRANGE
    write_mod_info "kmu" "Fixture Mod" "1.2.3" "0.98a-RC8" "jars/DerivedName.jar"
    cd "$WORK_DIR"

    expectedUrl="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/DerivedName-1.2.3.zip"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.directDownloadURL')" = "$expectedUrl" ]
}

# The release states the name of the asset it is about to upload, and that
# name wins: the URL has to point at what is published, not at what the
# jar implies.
@test "prefers the zip name it is given over the one it would derive" {

    # ARRANGE
    write_mod_info "kmu" "Fixture Mod" "1.2.3" "0.98a-RC8" "jars/DerivedName.jar"
    cd "$WORK_DIR"

    expectedUrlEnd="/KMU-1.2.3.zip"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [[ "$(read_generated '.directDownloadURL')" == *"$expectedUrlEnd" ]]
}

@test "fails when no zip name is given and mod_info.json lists no jar" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "name": "Fixture Mod", "version": "1.2.3", "gameVersion": "0.98a-RC8" }
EOF
    cd "$WORK_DIR"

    expectedError="missing required field 'jars[0]'"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

# A local build has no Actions runtime to export the variable, and the
# checkout's own remote is the only answer that cannot name a repository
# this clone does not push to.
@test "reads the publishing repository from the origin remote when GITHUB_REPOSITORY is unset" {

    # ARRANGE
    init_git_origin "https://github.com/Klark-Morrigan/Starsector-Mod-KMU.git"
    cd "$WORK_DIR"

    expectedUrl="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/KMU-1.2.3.zip"

    # ACT
    run env -u GITHUB_REPOSITORY OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.directDownloadURL')" = "$expectedUrl" ]
}

@test "reads an ssh origin remote the same way as an https one" {

    # ARRANGE
    init_git_origin "git@github.com:Klark-Morrigan/Starsector-Mod-KMU.git"
    cd "$WORK_DIR"

    expectedUrl="https://github.com/Klark-Morrigan/Starsector-Mod-KMU/releases/download/1.2.3/KMU-1.2.3.zip"

    # ACT
    run env -u GITHUB_REPOSITORY OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.directDownloadURL')" = "$expectedUrl" ]
}

# A release runs against a checkout whose remote is whatever the runner
# cloned; the variable is what states which repository is publishing.
@test "prefers GITHUB_REPOSITORY over the origin remote" {

    # ARRANGE
    init_git_origin "https://github.com/someone-else/a-fork.git"
    cd "$WORK_DIR"

    expectedRepositoryPath="/Klark-Morrigan/Starsector-Mod-KMU/"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [[ "$(read_generated '.directDownloadURL')" == *"$expectedRepositoryPath"* ]]
}

# The download URL has no third source, and a run outside both Actions and
# a checkout is exactly where it would otherwise be built against an empty
# owner/repo.
@test "fails when GITHUB_REPOSITORY is unset and there is no origin remote" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedError="<owner>/<repo>"

    # ACT
    run env -u GITHUB_REPOSITORY OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
    [ ! -f "$WORK_DIR/$OUTPUT_FILE" ]
}

@test "fails when the origin remote names no owner" {

    # ARRANGE
    init_git_origin "a-local-clone"
    cd "$WORK_DIR"

    expectedError="cannot read <owner>/<repo> out of the origin remote"

    # ACT
    run env -u GITHUB_REPOSITORY OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

# "02" is a legal thing to write in a version string but not legal JSON,
# so the component is canonicalised rather than emitted verbatim.
@test "drops leading zeros from a version component" {

    # ARRANGE
    write_mod_info "kmu" "Fixture Mod" "1.02.3" "0.98a-RC8"
    cd "$WORK_DIR"

    expectedMinor="2"
    expectedMinorType="number"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.modVersion.minor')" = "$expectedMinor" ]
    [ "$(read_generated '.modVersion.minor | type')" = "$expectedMinorType" ]
}

# Substitution is by whole value, which is what lets a token carry a
# non-string type. A partial one cannot be honoured, so it is refused
# rather than left in place for a player to read.
@test "fails on a token embedded in a longer value" {

    # ARRANGE
    cat > "$WORK_DIR/kmu.version.template" <<EOF
{
  "masterVersionFile": "https://example.invalid/master/kmu.version",
  "modName": "KMU v{{major}}"
}
EOF
    cd "$WORK_DIR"

    expectedError="unreplaced token(s): KMU v{{major}}"

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

# Substituting through jq rather than sed is what keeps a name like this
# from breaking the JSON it lands in.
@test "escapes a mod name holding quotes and backslashes" {

    # ARRANGE
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

    expectedModName='He said "hi" C:\x'

    # ACT
    run env OUTPUT_PATH="$OUTPUT_FILE" ZIP_NAME="$ZIP_NAME" bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_generated '.modName')" = "$expectedModName" ]
}
