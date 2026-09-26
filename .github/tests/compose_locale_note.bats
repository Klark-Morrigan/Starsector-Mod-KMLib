#!/usr/bin/env bats
# Unit tests for the compose-locale-note composite action's script.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/compose_locale_note.bats
#
# This is player-facing copy: the line these cases pin is what someone reads
# on a release page to pick a zip. The exact markdown is asserted rather than
# matched loosely, because a zip name that differs from the uploaded asset
# sends a reader looking for a file that is not there.

SCRIPT="$BATS_TEST_DIRNAME/../actions/compose-locale-note/scripts/compose_locale_note.sh"

CORE_LOCALISATION_URL="https://github.com/TruthOriginem/Starsector-Localization-CN"

# "Simplified Chinese" (U+7B80 U+4F53 U+4E2D U+6587)
SIMPLIFIED_CHINESE_DISPLAY_NAME="简体中文"

ENGLISH_LOCALE='{"tag":"en","displayName":"English","coreLocalisation":""}'
SIMPLIFIED_CHINESE_LOCALE="{\"tag\":\"zh-hans\",\"displayName\":\"$SIMPLIFIED_CHINESE_DISPLAY_NAME\",\"coreLocalisation\":\"$CORE_LOCALISATION_URL\"}"

setup() {
    TMP="$(mktemp -d)"
    GITHUB_OUTPUT="$TMP/github_output"
    : > "$GITHUB_OUTPUT"
    export GITHUB_OUTPUT
    export JAR_SOURCE="jars/KMU.jar"
    export VERSION="0.2.0"
    unset LOCALES
}

teardown() {
    rm -rf "$TMP"
}

run_compose() {
    export LOCALES="$1"
    run bash "$SCRIPT"
}

read_note() {
    sed -n 's/^note=//p' "$GITHUB_OUTPUT"
}

@test "names each locale's zip after its display name" {
    run_compose "[$ENGLISH_LOCALE]"
    [ "$status" -eq 0 ]
    [ "$(read_note)" = 'Builds by language: English in `KMU-0.2.0-en.zip`.' ]
}

@test "links the core localisation a locale needs" {
    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"
    [ "$status" -eq 0 ]
    expected="Builds by language: English in \`KMU-0.2.0-en.zip\`;"
    expected+=" $SIMPLIFIED_CHINESE_DISPLAY_NAME in \`KMU-0.2.0-zh-hans.zip\`,"
    expected+=" which needs [its core localisation]($CORE_LOCALISATION_URL) installed over \`starsector-core\`."
    [ "$(read_note)" = "$expected" ]
}

@test "keeps the order the locales arrive in" {
    run_compose "[$SIMPLIFIED_CHINESE_LOCALE,$ENGLISH_LOCALE]"
    [ "$status" -eq 0 ]
    # read-locales owns the order; composing again here would state it twice.
    [[ "$(read_note)" == "Builds by language: $SIMPLIFIED_CHINESE_DISPLAY_NAME in"* ]]
}

@test "emits an empty note for a mod keeping no locales" {
    run_compose "[]"
    [ "$status" -eq 0 ]
    grep -qx "note=" "$GITHUB_OUTPUT"
}

@test "emits an empty note when no locales are given at all" {
    run_compose ""
    [ "$status" -eq 0 ]
    grep -qx "note=" "$GITHUB_OUTPUT"
}

@test "emits the note on a single line" {
    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"
    [ "$status" -eq 0 ]
    [ "$(wc -l < "$GITHUB_OUTPUT")" -eq 1 ]
}

@test "fails on locales that are not a JSON array" {
    run_compose "{}"
    [ "$status" -ne 0 ]
    [[ "$output" == *"LOCALES is not a JSON array"* ]]
    [ ! -s "$GITHUB_OUTPUT" ]
}

@test "requires the jar source the zip names are derived from" {
    unset JAR_SOURCE
    run_compose "[$ENGLISH_LOCALE]"
    [ "$status" -ne 0 ]
    [[ "$output" == *"JAR_SOURCE is required"* ]]
}
