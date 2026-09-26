#!/usr/bin/env bats
# Unit tests for .github/actions/read-locales/scripts/read_locales.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/read_locales.bats
#
# Uses a real (small) manifest fixture and a real jq, since jq is required in
# CI anyway. The script is run from a temp working directory so it cannot
# accidentally read the repository's own files.

SCRIPT="$BATS_TEST_DIRNAME/../actions/read-locales/scripts/read_locales.sh"

CORE_LOCALISATION_URL="https://github.com/TruthOriginem/Starsector-Localization-CN"

# "Simplified Chinese" (U+7B80 U+4F53 U+4E2D U+6587)
SIMPLIFIED_CHINESE_DISPLAY_NAME="简体中文"

setup() {
    WORK_DIR="$(mktemp -d)"
    mkdir -p "$WORK_DIR/localisation"
    export GITHUB_OUTPUT
    GITHUB_OUTPUT="$(mktemp)"
}

teardown() {
    rm -rf "$WORK_DIR"
    rm -f "$GITHUB_OUTPUT"
}

# Writes the manifest fixture with the given raw JSON body.
write_manifest() {
    printf '%s\n' "$1" > "$WORK_DIR/localisation/manifest.json"
}

# Writes the two-locale manifest most cases read: English by default, and a
# locale naming a core localisation. Listed out of tag order, so a case can
# see the default put first.
write_two_locale_manifest() {
    write_manifest "{
      \"defaultLocale\": \"en\",
      \"files\": { \"strings.json\": \"data/strings/strings.json\" },
      \"locales\": {
        \"zh-hans\": {
          \"displayName\": \"$SIMPLIFIED_CHINESE_DISPLAY_NAME\",
          \"coreLocalisation\": \"$CORE_LOCALISATION_URL\"
        },
        \"en\": { \"displayName\": \"English\" }
      }
    }"
}

read_output_value() {
    sed -n "s/^$1=//p" "$GITHUB_OUTPUT"
}

@test "emits an empty list and no default for a mod committing no manifest" {
    rm -rf "$WORK_DIR/localisation"
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    # The pipeline reads this as "release the one unsuffixed zip", so a mod
    # with no locales releases exactly as before.
    [ "$status" -eq 0 ]
    grep -qx "locales=\[\]"                      "$GITHUB_OUTPUT"
    grep -qx "default-locale="                   "$GITHUB_OUTPUT"
}

@test "emits the default locale" {
    write_two_locale_manifest
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -qx "default-locale=en"                 "$GITHUB_OUTPUT"
}

@test "lists the default locale first and the rest by tag" {
    write_two_locale_manifest
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    locales="$(read_output_value "locales")"
    [ "$(jq -r '[.[].tag] | join(",")' <<< "$locales")" = "en,zh-hans" ]
}

@test "carries each locale's display name and core localisation" {
    write_two_locale_manifest
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    locales="$(read_output_value "locales")"
    [ "$(jq -r '.[1].displayName' <<< "$locales")" = "$SIMPLIFIED_CHINESE_DISPLAY_NAME" ]
    [ "$(jq -r '.[1].coreLocalisation' <<< "$locales")" = "$CORE_LOCALISATION_URL" ]
}

@test "emits an empty core localisation for a locale naming none" {
    write_two_locale_manifest
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    locales="$(read_output_value "locales")"
    # Empty rather than null, so a consumer tests one absent value.
    [ "$(jq -r '.[0].coreLocalisation' <<< "$locales")" = "" ]
}

@test "emits the locale list on a single line so it parses as one output key" {
    write_two_locale_manifest
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    [ "$(wc -l < "$GITHUB_OUTPUT")" -eq 2 ]
}

@test "fails when the manifest names no default locale" {
    write_manifest '{ "locales": { "en": { "displayName": "English" } } }'
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"names no defaultLocale"* ]]
}

@test "fails when the default locale is not among the locales" {
    write_manifest '{ "defaultLocale": "fr", "locales": { "en": { "displayName": "English" } } }'
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    # Its version file is what an older install polls, so it has to exist.
    [ "$status" -ne 0 ]
    [[ "$output" == *"default locale 'fr' is not among its locales"* ]]
}

@test "fails on a tag that could step out of a file name" {
    write_manifest '{ "defaultLocale": "en", "locales": { "en": {}, "../x": {} } }'
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    # A tag is spliced into the release's file names on the mod's own runner.
    [ "$status" -ne 0 ]
    [[ "$output" == *"locale '../x' is not a lowercased BCP 47 tag"* ]]
}

@test "fails on a tag that is not lowercased" {
    write_manifest '{ "defaultLocale": "en", "locales": { "en": {}, "zh-Hans": {} } }'
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"locale 'zh-Hans' is not a lowercased BCP 47 tag"* ]]
}

@test "writes no output when the manifest fails a check" {
    write_manifest '{ "defaultLocale": "fr", "locales": { "en": {} } }'
    cd "$WORK_DIR"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [ ! -s "$GITHUB_OUTPUT" ]
}
