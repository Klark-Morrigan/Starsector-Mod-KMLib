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

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

# shellcheck source=mod_root_fixtures.bash
source "$BATS_TEST_DIRNAME/mod_root_fixtures.bash"

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

# Writes a fixture that also pins KMLib, for the cases exercising the
# dependency read. Separate from write_mod_info_file so the cases above keep
# stating the no-dependency shape they are about.
write_mod_info_with_kmlib() {

    cat > "$WORK_DIR/mod_info.json" <<EOF
{
  "id": "$1",
  "version": "$2",
  "jars": ["$3"],
  "dependencies": [ { "id": "kmlib", "name": "KMLib", "version": "$4" } ]
}
EOF
}

# Reads one emitted key back out of $GITHUB_OUTPUT, for values whose exact
# text is awkward to assert with grep -qx (JSON carrying quotes and braces).
read_output_value() {

    sed -n "s/^$1=//p" "$GITHUB_OUTPUT"
}

# Fails unless every given line is a whole line of $GITHUB_OUTPUT.
assert_github_output_lines() {

    local expectedLine

    for expectedLine in "$@"; do
        grep -qx -- "$expectedLine" "$GITHUB_OUTPUT"
    done
}

@test "emits all derived values for a well-formed mod_info.json" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"

    expectedLines=(
        "mod-id=kmu"
        "version=0.1.0"
        "runner-label=kmu-runner"
        "mod-folder-name=KMU"
        "dist-dir=dist/KMU/"
        "zip-name=KMU-0.1.0.zip"
        "version-file-name=kmu.version"
        "jar-source=jars/KMU.jar"
    )
    expectedZipNameRegex='^KMU-[0-9]+\.[0-9]+\.[0-9]+(-(?<locale>.+))?\.zip$'

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
    [ "$(read_output_value "zip-name-regex")" = "$expectedZipNameRegex" ]
}

# The id is lowercase and the folder is not: a build that looks for
# <mods>/KMU/jars/KMU.jar has to find what the zip unpacks, on a
# case-sensitive filesystem as much as on Windows.
@test "names the shipped folder after the jar, not after the mod id" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"

    expectedLines=("mod-id=kmu" "mod-folder-name=KMU")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

# The opposite convention to the folder and zip on either side of it.
# VersionChecker finds this file through version_files.csv, which a mod
# writes by hand, so it is named with the string a mod author states.
@test "names the version file after the mod id, not after the jar" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"

    expectedLines=("mod-folder-name=KMU" "version-file-name=kmu.version")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

@test "strips only the jar extension from a nested jar path" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/internal/KMU-Core.jar"
    cd "$WORK_DIR"

    expectedLines=(
        "mod-folder-name=KMU-Core"
        "dist-dir=dist/KMU-Core/"
        "zip-name=KMU-Core-0.1.0.zip"
    )

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

@test "works for KMLib itself (no transformation of id)" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmlib" "1.0.0" "jars/KMLib.jar"
    cd "$WORK_DIR"

    expectedLines=(
        "mod-id=kmlib"
        "runner-label=kmlib-runner"
        "mod-folder-name=KMLib"
        "dist-dir=dist/KMLib/"
        "zip-name=KMLib-1.0.0.zip"
        "version-file-name=kmlib.version"
        "jar-source=jars/KMLib.jar"
    )

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

@test "emits Common-Java and KMLib as siblings for a consumer mod" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"

    expectedSiblings='[{"repo":"Klark-Morrigan/Common-Java","path":"Common-Java"},'
    expectedSiblings+='{"repo":"Klark-Morrigan/Starsector-Mod-KMLib","path":"Starsector-Mod-KMLib"}]'

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_output_value "sibling-checkouts")" = "$expectedSiblings" ]
}

@test "omits the KMLib sibling when the mod being built is KMLib" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmlib" "0.1.0" "jars/KMLib.jar"
    cd "$WORK_DIR"

    expectedSiblings='[{"repo":"Klark-Morrigan/Common-Java","path":"Common-Java"}]'

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_output_value "sibling-checkouts")" = "$expectedSiblings" ]
}

@test "emits the sibling set on a single line so it parses as one output key" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"

    expectedSiblingLineCount=1
    expectedLineCount=14

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(grep -c '^sibling-checkouts=' "$GITHUB_OUTPUT")" -eq "$expectedSiblingLineCount" ]
    [ "$(wc -l < "$GITHUB_OUTPUT")" -eq "$expectedLineCount" ]
}

# One constant behind both, so the release a consumer is checked against
# is the same repository its build compiles the mod against.
@test "emits the KMLib repository the sibling checkout set names" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"

    expectedKmlibRepo="Klark-Morrigan/Starsector-Mod-KMLib"

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$(read_output_value "kmlib-repo")" = "$expectedKmlibRepo" ]
    [ "$(jq -r '.[1].repo' <<< "$(read_output_value "sibling-checkouts")")" = "$expectedKmlibRepo" ]
}

# KMLib drops the sibling checkout but the key is still emitted, so no
# consumer of this output has to handle it being absent for one mod.
@test "emits the KMLib repository even for KMLib itself" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmlib" "0.1.0" "jars/KMLib.jar"
    cd "$WORK_DIR"

    expectedLines=("kmlib-repo=Klark-Morrigan/Starsector-Mod-KMLib")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

@test "emits the kmlib dependency version a consumer pins" {

    # ARRANGE
    write_mod_info_with_kmlib "kmu" "0.1.0" "jars/KMU.jar" "1.0.0"
    cd "$WORK_DIR"

    expectedLines=("kmlib-dependency-version=1.0.0")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

# KMLib's own release takes this path. The key is still emitted so the
# pipeline reads one absent-or-present value rather than an absent key.
@test "emits an empty kmlib dependency version when none is declared" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmlib" "0.1.0" "jars/KMLib.jar"
    cd "$WORK_DIR"

    expectedLines=("kmlib-dependency-version=")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

@test "reads the kmlib pin past an unrelated dependency" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.json" <<EOF
{
  "id": "kmu",
  "version": "0.1.0",
  "jars": ["jars/KMU.jar"],
  "dependencies": [
    { "id": "lw_lazylib", "name": "LazyLib" },
    { "id": "kmlib", "name": "KMLib", "version": "1.0.0" }
  ]
}
EOF
    cd "$WORK_DIR"

    expectedLines=("kmlib-dependency-version=1.0.0")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

@test "emits the Nexus mod id the version template states" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    printf '{ "masterVersionFile": "https://example.invalid/kmu.version", "modNexusId": 160 }\n' \
        > "$WORK_DIR/kmu.version.template"
    cd "$WORK_DIR"

    expectedLines=("nexus-mod-id=160" "nexus-game-id=2672")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

# A template without the key is a mod not on Nexus, and the action skips it.
@test "emits an empty Nexus mod id when the version template states none" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    printf '{ "masterVersionFile": "https://example.invalid/kmu.version" }\n' > "$WORK_DIR/kmu.version.template"
    cd "$WORK_DIR"

    expectedLines=("nexus-mod-id=")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

@test "emits an empty Nexus mod id when no version template is committed" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"
    cd "$WORK_DIR"

    expectedLines=("nexus-mod-id=")
    expectedLog="No kmu.version.template, so no Nexus mod id."

    # ACT
    run --separate-stderr bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
    [[ "$stderr" == *"$expectedLog"* ]]
}

# The generated copy lags whatever the base now says until the next
# build, and the release reads before it builds.
@test "reads the base over a generated mod_info.json" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"

    cat > "$WORK_DIR/mod_info.base.json" <<EOF
{ "id": "kmu", "version": "0.2.0", "jars": ["jars/KMU.jar"] }
EOF
    cd "$WORK_DIR"

    expectedLines=("version=0.2.0" "zip-name=KMU-0.2.0.zip")

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    assert_github_output_lines "${expectedLines[@]}"
}

@test "logs the file it read and every key it emits, with its value" {

    # ARRANGE
    write_mod_info_with_kmlib "kmu" "0.1.0" "jars/KMU.jar" "1.0.0"
    printf '{ "modNexusId": 160 }\n' > "$WORK_DIR/kmu.version.template"
    cd "$WORK_DIR"

    siblings='[{"repo":"Klark-Morrigan/Common-Java","path":"Common-Java"},'
    siblings+='{"repo":"Klark-Morrigan/Starsector-Mod-KMLib","path":"Starsector-Mod-KMLib"}]'

    expectedLines=(
        "Reading mod_info.json."
        "Reading kmu.version.template."
        "mod-id: kmu"
        "version: 0.1.0"
        "runner-label: kmu-runner"
        "mod-folder-name: KMU"
        "dist-dir: dist/KMU/"
        "zip-name: KMU-0.1.0.zip"
        'zip-name-regex: ^KMU-[0-9]+\.[0-9]+\.[0-9]+(-(?<locale>.+))?\.zip$'
        "version-file-name: kmu.version"
        "jar-source: jars/KMU.jar"
        "sibling-checkouts: $siblings"
        "kmlib-dependency-version: 1.0.0"
        "nexus-game-id: 2672"
        "nexus-mod-id: 160"
        "kmlib-repo: Klark-Morrigan/Starsector-Mod-KMLib"
    )

    # ACT
    run --separate-stderr bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "" ]
    [ "$stderr" = "$(printf '%s\n' "${expectedLines[@]}")" ]
}

@test "logs the base as the file it read over a generated mod_info.json" {

    # ARRANGE
    write_mod_info_file "$WORK_DIR" "kmu" "0.1.0" "jars/KMU.jar"

    cat > "$WORK_DIR/mod_info.base.json" <<EOF
{ "id": "kmu", "version": "0.2.0", "jars": ["jars/KMU.jar"] }
EOF
    cd "$WORK_DIR"

    expectedFirstLine="Reading mod_info.base.json."

    # ACT
    run --separate-stderr bash "$SCRIPT"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "${stderr%%$'\n'*}" = "$expectedFirstLine" ]
}

@test "fails when no metadata file is present" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedError="neither mod_info.base.json nor mod_info.json found"

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "fails when .id is missing" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "version": "0.1.0", "jars": ["jars/X.jar"] }
EOF
    cd "$WORK_DIR"

    expectedError="missing required field 'id'"

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "fails when .version is missing" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "jars": ["jars/X.jar"] }
EOF
    cd "$WORK_DIR"

    expectedError="missing required field 'version'"

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}

@test "fails when .jars is missing or empty" {

    # ARRANGE
    cat > "$WORK_DIR/mod_info.json" <<EOF
{ "id": "kmu", "version": "0.1.0", "jars": [] }
EOF
    cd "$WORK_DIR"

    expectedError="missing required field 'jars[0]'"

    # ACT
    run bash "$SCRIPT"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$output" == *"$expectedError"* ]]
}
