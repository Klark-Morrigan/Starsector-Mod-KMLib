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
# failure, which would otherwise take the test runner down with them. Its
# stderr is kept apart, so a value a helper echoes is read without its log.

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

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

    run --separate-stderr bash -c "set -euo pipefail
                                   SCRIPT_NAME='${scriptName}'
                                   source '$LIB'
                                   $*"
}

@test "mod_info_enter_mod_root moves into the given directory" {

    # ARRANGE
    mkdir -p "$WORK_DIR/checkout"
    cd "$WORK_DIR"

    expectedDirectory="$(cd "$WORK_DIR/checkout" && pwd)"

    # ACT
    run_in_lib "package_release" 'mod_info_enter_mod_root checkout; pwd'

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedDirectory" ]
}

@test "mod_info_enter_mod_root fails naming the directory when it is absent" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedErrorStart="package_release: mod root absent not found in "

    # ACT
    run_in_lib "package_release" "mod_info_enter_mod_root absent"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$stderr" == "$expectedErrorStart"* ]]
}

@test "mod_info_locate_file points at mod_info.json when it is the only file" {

    # ARRANGE
    echo '{}' > "$WORK_DIR/mod_info.json"
    cd "$WORK_DIR"

    expectedFile="mod_info.json"

    # ACT
    run_in_lib "read_mod_info" 'mod_info_locate_file; echo "${MOD_INFO_FILE}"'

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFile" ]
}

# Beside a base, mod_info.json is a build output on whichever locale was
# last written, so it is never the one read.
@test "mod_info_locate_file points at the base when one is committed" {

    # ARRANGE
    echo '{}' > "$WORK_DIR/mod_info.json"
    echo '{}' > "$WORK_DIR/mod_info.base.json"
    cd "$WORK_DIR"

    expectedFile="mod_info.base.json"

    # ACT
    run_in_lib "read_mod_info" 'mod_info_locate_file; echo "${MOD_INFO_FILE}"'

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFile" ]
}

# The release reads a fresh checkout, where the generated file does not
# exist yet.
@test "mod_info_locate_file points at the base on a checkout that has built nothing" {

    # ARRANGE
    echo '{}' > "$WORK_DIR/mod_info.base.json"
    cd "$WORK_DIR"

    expectedFile="mod_info.base.json"

    # ACT
    run_in_lib "read_mod_info" 'mod_info_locate_file; echo "${MOD_INFO_FILE}"'

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFile" ]
}

@test "mod_info_locate_file logs the file it chose on stderr alone" {

    # ARRANGE
    echo '{}' > "$WORK_DIR/mod_info.json"
    echo '{}' > "$WORK_DIR/mod_info.base.json"
    cd "$WORK_DIR"

    expectedLog="Reading mod_info.base.json."

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$stderr" = "$expectedLog" ]
    [ "$output" = "" ]
}

@test "mod_info_locate_file fails naming both files when neither is present" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedError="neither mod_info.base.json nor mod_info.json found"

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$stderr" == *"$expectedError"* ]]
}

# A shared lib reporting under its own name would tell a reader nothing
# about which release step actually stopped.
@test "mod_info_locate_file names the sourcing script in its message" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedErrorStart="fill_version_file_template: "

    # ACT
    run_in_lib "fill_version_file_template" "mod_info_locate_file"

    # ASSERT
    [[ "$stderr" == "$expectedErrorStart"* ]]
}

# Under set -u an unset SCRIPT_NAME would crash the reporting itself,
# losing the real failure behind an unbound-variable error.
@test "mod_info_locate_file falls back to a default name when unset" {

    # ARRANGE
    cd "$WORK_DIR"

    expectedErrorStart="mod_info: "

    # ACT
    run --separate-stderr bash -c "set -euo pipefail; source '$LIB'; mod_info_locate_file"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$stderr" == "$expectedErrorStart"* ]]
}

@test "mod_info_read_release_fields reads the id, the version and the first jar" {

    # ARRANGE
    echo '{ "id": "kmu", "version": "0.2.0", "jars": ["jars/KMU.jar", "jars/Other.jar"] }' \
        > "$WORK_DIR/mod_info.json"
    cd "$WORK_DIR"

    expectedFields="kmu|0.2.0|jars/KMU.jar"
    expectedLog="Reading mod_info.json."

    # ACT
    run_in_lib "read_mod_info" \
        'mod_info_read_release_fields; echo "${MOD_ID}|${VERSION}|${JAR_SOURCE}"'

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFields" ]
    [ "$stderr" = "$expectedLog" ]
}

@test "mod_info_read_release_fields reads the base over a generated mod_info.json" {

    # ARRANGE
    echo '{ "id": "kmu", "version": "0.1.0", "jars": ["jars/KMU.jar"] }' > "$WORK_DIR/mod_info.json"
    echo '{ "id": "kmu", "version": "0.2.0", "jars": ["jars/KMU.jar"] }' > "$WORK_DIR/mod_info.base.json"
    cd "$WORK_DIR"

    expectedVersion="0.2.0"

    # ACT
    run_in_lib "read_mod_info" 'mod_info_read_release_fields; echo "${VERSION}"'

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedVersion" ]
}

@test "mod_info_read_release_fields fails naming the first field missing" {

    # ARRANGE
    echo '{ "id": "kmu", "jars": [] }' > "$WORK_DIR/mod_info.json"
    cd "$WORK_DIR"

    expectedError="read_mod_info: mod_info.json is missing required field 'version'"

    # ACT
    run_in_lib "read_mod_info" "mod_info_read_release_fields"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$stderr" == *"$expectedError"* ]]
}

@test "mod_info_require_fields passes when every field has a value" {

    # ARRANGE

    # ACT
    run_in_lib "read_mod_info" \
        "mod_info_require_fields 'id:kmu' 'version:1.2.3' 'jars[0]:jars/KMU.jar'"

    # ASSERT
    [ "$status" -eq 0 ]
}

@test "mod_info_require_fields fails on an empty value" {

    # ARRANGE
    expectedError="missing required field 'version'"

    # ACT
    run_in_lib "read_mod_info" "mod_info_require_fields 'id:kmu' 'version:'"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$stderr" == *"$expectedError"* ]]
}

# jq -r prints absent fields as the string "null", so this is what a
# missing field actually looks like by the time it reaches the check.
@test "mod_info_require_fields fails on jq's null" {

    # ARRANGE
    expectedError="missing required field 'name'"

    # ACT
    run_in_lib "read_mod_info" "mod_info_require_fields 'name:null'"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$stderr" == *"$expectedError"* ]]
}

@test "mod_info_require_fields reports the first absent field only" {

    # ARRANGE
    expectedField="'id'"
    unexpectedField="'version'"

    # ACT
    run_in_lib "read_mod_info" \
        "mod_info_require_fields 'id:' 'version:' 'gameVersion:'"

    # ASSERT
    [ "$status" -ne 0 ]
    [[ "$stderr" == *"$expectedField"* ]]
    [[ "$stderr" != *"$unexpectedField"* ]]
}

# URLs are the common case; splitting on the last colon would truncate
# them and report a present field as missing.
@test "mod_info_require_fields keeps values holding colons intact" {

    # ARRANGE

    # ACT
    run_in_lib "read_mod_info" \
        "mod_info_require_fields 'masterVersionFile:https://example.invalid/x.version'"

    # ASSERT
    [ "$status" -eq 0 ]
}

# The label is written as it appears in mod_info.json so a reader can
# go straight to it, rather than as the shell variable holding it.
@test "mod_info_require_fields uses the field label verbatim in its message" {

    # ARRANGE
    expectedError="missing required field 'jars[0]'"

    # ACT
    run_in_lib "read_mod_info" "mod_info_require_fields 'jars[0]:null'"

    # ASSERT
    [[ "$stderr" == *"$expectedError"* ]]
}

@test "mod_info_derive_mod_folder_name strips the jar extension from a nested path" {

    # ARRANGE
    expectedFolderName="KMLib"

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_mod_folder_name 'jars/KMLib.jar'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFolderName" ]
}

@test "mod_info_derive_mod_folder_name keeps a name that carries no extension" {

    # ARRANGE
    expectedFolderName="KMLib"

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_mod_folder_name 'jars/KMLib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFolderName" ]
}

# The release uploads an asset under this name and the generated version
# file points a download URL at it, so both read the rule from here.
@test "mod_info_derive_zip_name joins the folder name and the version" {

    # ARRANGE
    expectedZipName="KMLib-0.1.0.zip"

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_zip_name 'jars/KMLib.jar' '0.1.0'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedZipName" ]
}

@test "mod_info_derive_zip_name suffixes a locale's tag after the version" {

    # ARRANGE
    expectedZipName="KMU-0.2.0-zh-hans.zip"

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_zip_name 'jars/KMU.jar' '0.2.0' 'zh-hans'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedZipName" ]
}

# Derives the zip-name regex for a jars[0] value and runs the given jq filter
# over one asset name, with the regex bound to $regex. jq is the reader the
# regex is written for.
apply_zip_name_regex() {

    local jarSource="$1" assetName="$2" filter="$3"
    local regex

    regex="$(bash -c "source '$LIB'; mod_info_derive_zip_name_regex '$jarSource'")"

    jq -nr --arg regex "$regex" --arg name "$assetName" "\$name | ${filter}"
}

# Prints true or false.
match_zip_name_regex() {

    apply_zip_name_regex "$1" "$2" 'test($regex)'
}

# Prints the captured locale, or null when the group took no part in the
# match.
capture_zip_name_locale() {

    apply_zip_name_regex "$1" "$2" 'capture($regex) | .locale'
}

@test "mod_info_derive_zip_name_regex anchors the folder name, any version and an optional locale" {

    # ARRANGE
    expectedRegex='^KMU-[0-9]+\.[0-9]+\.[0-9]+(-(?<locale>.+))?\.zip$'

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_zip_name_regex 'jars/KMU.jar'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedRegex" ]
}

@test "mod_info_derive_zip_name_regex escapes metacharacters in the folder name" {

    # ARRANGE
    expectedRegex='^K\.M\+U-[0-9]+\.[0-9]+\.[0-9]+(-(?<locale>.+))?\.zip$'

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_zip_name_regex 'jars/K.M+U.jar'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedRegex" ]
}

@test "mod_info_derive_zip_name_regex captures a localised zip's locale" {

    # ARRANGE
    expectedLocale="en"

    # ACT
    run capture_zip_name_locale 'jars/KMU.jar' 'KMU-0.2.0-en.zip'

    # ASSERT
    [ "$output" = "$expectedLocale" ]
}

@test "mod_info_derive_zip_name_regex captures the whole of a locale tag holding a hyphen" {

    # ARRANGE
    expectedLocale="zh-hans"

    # ACT
    run capture_zip_name_locale 'jars/KMU.jar' 'KMU-0.2.0-zh-hans.zip'

    # ASSERT
    [ "$output" = "$expectedLocale" ]
}

@test "mod_info_derive_zip_name_regex captures no locale from the unlocalised zip" {

    # ARRANGE
    expectedLocale="null"

    # ACT
    run capture_zip_name_locale 'jars/KMU.jar' 'KMU-0.1.0.zip'

    # ASSERT
    [ "$output" = "$expectedLocale" ]
}

@test "mod_info_derive_zip_name_regex matches a folder name holding metacharacters as itself" {

    # ARRANGE
    expectedMatch="true"

    # ACT
    run match_zip_name_regex 'jars/K.M+U.jar' 'K.M+U-1.0.0.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

# Unescaped, the dot would match any character and the plus repeat the M.
@test "mod_info_derive_zip_name_regex rejects a name its folder's metacharacters would match unescaped" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/K.M+U.jar' 'KxMMU-1.0.0.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

@test "mod_info_derive_zip_name_regex matches the unlocalised zip" {

    # ARRANGE
    expectedMatch="true"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMU-0.1.0.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

@test "mod_info_derive_zip_name_regex matches a localised zip" {

    # ARRANGE
    expectedMatch="true"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMU-0.2.0-en.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

# The tag carries a hyphen of its own, past the one joining it on.
@test "mod_info_derive_zip_name_regex matches a localised zip whose tag holds a hyphen" {

    # ARRANGE
    expectedMatch="true"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMU-0.2.0-zh-hans.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

# A jar named like the mod id, so the version files share the zip's
# prefix and only the pattern's shape keeps them out.
@test "mod_info_derive_zip_name_regex rejects the unlocalised version file" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/kmu.jar' 'kmu.version'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

# A jar named like the mod id, as above.
@test "mod_info_derive_zip_name_regex rejects a localised version file" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/kmu.jar' 'kmu-zh-hans.version'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

@test "mod_info_derive_zip_name_regex rejects another mod's zip" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMLib-0.5.2.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

# A folder name that starts with this one is still another mod.
@test "mod_info_derive_zip_name_regex rejects the zip of a mod whose folder name starts with this one" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMU-Core-0.2.0.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

@test "mod_info_derive_zip_name_regex rejects a version of two parts" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMU-1.2.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

@test "mod_info_derive_zip_name_regex rejects a version with a letter suffix" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMU-1.2.3a.zip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

@test "mod_info_derive_zip_name_regex rejects .zip spelled with another character in place of the dot" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMU-0.2.0xzip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

@test "mod_info_derive_zip_name_regex rejects .zip spelled with another character in place of the dot after a locale" {

    # ARRANGE
    expectedMatch="false"

    # ACT
    run match_zip_name_regex 'jars/KMU.jar' 'KMU-0.2.0-enxzip'

    # ASSERT
    [ "$output" = "$expectedMatch" ]
}

@test "mod_info_escape_regex_text escapes every metacharacter" {

    # ARRANGE
    expectedText='\.\^\$\|\?\*\+\(\)\[\]\{\}\\'

    # ACT
    run_in_lib "read_mod_info" "mod_info_escape_regex_text '.^\$|?*+()[]{}\\'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedText" ]
}

@test "mod_info_escape_regex_text leaves letters, digits and hyphens alone" {

    # ARRANGE
    expectedText="KMU-Core2"

    # ACT
    run_in_lib "read_mod_info" "mod_info_escape_regex_text 'KMU-Core2'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedText" ]
}

@test "mod_info_derive_version_file_name names the file after the mod id" {

    # ARRANGE
    expectedFileName="kmu.version"

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_version_file_name 'kmu'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFileName" ]
}

@test "mod_info_derive_version_file_name suffixes a locale's tag" {

    # ARRANGE
    expectedFileName="kmu-zh-hans.version"

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_version_file_name 'kmu' 'zh-hans'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFileName" ]
}

@test "mod_info_derive_version_template_name names the template after the unlocalised version file" {

    # ARRANGE
    expectedFileName="kmu.version.template"

    # ACT
    run_in_lib "read_mod_info" "mod_info_derive_version_template_name 'kmu'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedFileName" ]
}

# A mod keeping no locales releases under the names it always has.
@test "mod_info_format_locale_suffix emits nothing for no tag" {

    # ARRANGE
    expectedSuffix=""

    # ACT
    run_in_lib "read_mod_info" "mod_info_format_locale_suffix ''"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedSuffix" ]
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

    # ARRANGE
    write_mod_info_with_dependencies '{ "id": "kmlib", "version": "1.0.0" }'
    cd "$WORK_DIR"

    expectedAnswer="true"

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_has_dependency 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedAnswer" ]
}

@test "mod_info_has_dependency reports false for an undeclared id" {

    # ARRANGE
    write_mod_info_with_dependencies '{ "id": "lw_lazylib", "name": "LazyLib" }'
    cd "$WORK_DIR"

    expectedAnswer="false"

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_has_dependency 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedAnswer" ]
}

# KMLib's own mod_info.json is this shape. Without jq's `// []` guard the
# missing key is an error rather than an empty result, which would fail
# the one release that legitimately declares no dependency.
@test "mod_info_has_dependency reports false when there is no dependencies key" {

    # ARRANGE
    write_mod_info_with_dependencies
    cd "$WORK_DIR"

    expectedAnswer="false"

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_has_dependency 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedAnswer" ]
}

# The state validate-versioning fails on: declared but unpinned. It has
# to read as present here, or that rule never fires.
@test "mod_info_has_dependency finds an entry that states no version" {

    # ARRANGE
    write_mod_info_with_dependencies '{ "id": "kmlib", "name": "KMLib" }'
    cd "$WORK_DIR"

    expectedAnswer="true"

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_has_dependency 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedAnswer" ]
}

@test "mod_info_read_dependency_version echoes the pinned version" {

    # ARRANGE
    write_mod_info_with_dependencies '{ "id": "kmlib", "version": "1.0.0" }'
    cd "$WORK_DIR"

    expectedVersion="1.0.0"

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedVersion" ]
}

@test "mod_info_read_dependency_version picks the entry matching the id" {

    # ARRANGE
    write_mod_info_with_dependencies \
        '{ "id": "lw_lazylib", "version": "2.9.0" },
         { "id": "kmlib", "version": "1.0.0" }'
    cd "$WORK_DIR"

    expectedVersion="1.0.0"

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedVersion" ]
}

@test "mod_info_read_dependency_version echoes nothing for an undeclared id" {

    # ARRANGE
    write_mod_info_with_dependencies '{ "id": "lw_lazylib", "name": "LazyLib" }'
    cd "$WORK_DIR"

    expectedVersion=""

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedVersion" ]
}

@test "mod_info_read_dependency_version echoes nothing when there is no dependencies key" {

    # ARRANGE
    write_mod_info_with_dependencies
    cd "$WORK_DIR"

    expectedVersion=""

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedVersion" ]
}

# "null" would sail through an emptiness check and reach the release body
# as a version string, so the `// ""` fallback is the tested behaviour.
@test "mod_info_read_dependency_version echoes empty, not jq's null, for a versionless entry" {

    # ARRANGE
    write_mod_info_with_dependencies '{ "id": "kmlib", "name": "KMLib" }'
    cd "$WORK_DIR"

    expectedVersion=""

    # ACT
    run_in_lib "read_mod_info" "mod_info_locate_file; mod_info_read_dependency_version 'kmlib'"

    # ASSERT
    [ "$status" -eq 0 ]
    [ "$output" = "$expectedVersion" ]
}
