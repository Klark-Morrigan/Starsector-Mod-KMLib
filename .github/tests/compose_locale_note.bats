#!/usr/bin/env bats
# Unit tests for the compose-locale-note composite action's script.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/compose_locale_note.bats
#
# This is player-facing copy: the list these cases pin is what someone reads
# on a release page to pick a zip, and the collapsed sections are the notes
# they read in their own language. The exact markdown is asserted rather than
# matched loosely, because a zip name that differs from the uploaded asset
# sends a reader looking for a file that is not there.
#
# The changelog helpers are Common-Automation's, which this repository does not
# check out, so a stand-in on the fixture's own terms cuts a version's section
# the way they do: the lines under its heading up to the next version, blank
# lines trimmed from both ends.

SCRIPT="$BATS_TEST_DIRNAME/../actions/compose-locale-note/scripts/compose_locale_note.sh"
CORE_LOCALISATION_URL="https://github.com/TruthOriginem/Starsector-Localization-CN"

# "Simplified Chinese" (U+7B80 U+4F53 U+4E2D U+6587)
SIMPLIFIED_CHINESE_DISPLAY_NAME="简体中文"

# "Added" (U+65B0 U+589E), a translated section heading.
TRANSLATED_ADDED_HEADING="新增"

ENGLISH_LOCALE='{"tag":"en","displayName":"English","coreLocalisation":""}'
SIMPLIFIED_CHINESE_LOCALE="{\"tag\":\"zh-hans\",\"displayName\":\"$SIMPLIFIED_CHINESE_DISPLAY_NAME\",\"coreLocalisation\":\"$CORE_LOCALISATION_URL\"}"

setup() {

    TMP="$(mktemp -d)"
    GITHUB_OUTPUT="$TMP/github_output"

    : > "$GITHUB_OUTPUT"

    export GITHUB_OUTPUT
    export JAR_SOURCE="jars/KMU.jar"
    export VERSION="0.2.0"

    unset LOCALES DEFAULT_LOCALE CHANGELOG_LIB
    write_changelog_lib

    cd "$TMP"

}

teardown() {
    rm -rf "$TMP"
}

write_changelog_lib() {
    cat > "$TMP/changelog.sh" <<'EOF'

changelog_section() {

    awk -v ver="$2" '
        $0 ~ "^## \\[" ver "\\]" { capture = 1; next }
        capture && /^## \[/      { exit }
        capture                  { body = body $0 "\n" }
        END {
            gsub(/^[ \t\r\n]+/, "", body)
            gsub(/[ \t\r\n]+$/, "", body)
            printf "%s", body
        }

    ' "$1"
}
EOF
}

# A translated changelog holding the released version and the one before it.
write_translated_changelog() {

    mkdir -p "$TMP/localisation/zh-hans"
    cat > "$TMP/localisation/zh-hans/CHANGELOG.md" <<EOF
# Changelog

## [0.2.0] - 2026-10-01

### $TRANSLATED_ADDED_HEADING

- Point.

## [0.1.0] - 2026-09-14

- Older point.
EOF
}

run_compose() {

    export LOCALES="$1"
    run bash "$SCRIPT"
}

# Echoes one output's value, read out of the multi-line form it was written in.
read_output() {

    awk -v key="$1" '
        delimiter != "" && $0 == delimiter { exit }
        delimiter != ""                    { print; next }
        index($0, key "<<") == 1           { delimiter = substr($0, length(key) + 3) }

    ' "$GITHUB_OUTPUT"
}

@test "lists each locale's zip after its display name under a heading" {

    run_compose "[$ENGLISH_LOCALE]"

    [ "$status" -eq 0 ]
    [ "$(read_output note)" = '### Builds by language

- English: `KMU-0.2.0-en.zip`.' ]
}

@test "links the core localisation a locale needs" {

    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"

    [ "$status" -eq 0 ]

    expected="### Builds by language"$'\n\n'
    expected+="- English: \`KMU-0.2.0-en.zip\`."$'\n'
    expected+="- $SIMPLIFIED_CHINESE_DISPLAY_NAME: \`KMU-0.2.0-zh-hans.zip\`,"
    expected+=" which needs [its core localisation]($CORE_LOCALISATION_URL) installed over \`starsector-core\`."

    [ "$(read_output note)" = "$expected" ]
}

@test "keeps the order the locales arrive in" {

    run_compose "[$SIMPLIFIED_CHINESE_LOCALE,$ENGLISH_LOCALE]"

    [ "$status" -eq 0 ]

    # read-locales owns the order; composing again here would state it twice.
    [ "$(read_output note | sed -n 3p)" = "- $SIMPLIFIED_CHINESE_DISPLAY_NAME: \`KMU-0.2.0-zh-hans.zip\`, which needs [its core localisation]($CORE_LOCALISATION_URL) installed over \`starsector-core\`." ]
}

@test "collapses each translated locale's section for the version under its display name" {

    write_translated_changelog
    export DEFAULT_LOCALE="en" CHANGELOG_LIB="$TMP/changelog.sh"
    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"

    [ "$status" -eq 0 ]

    expected="<details>"$'\n'"<summary>$SIMPLIFIED_CHINESE_DISPLAY_NAME</summary>"$'\n\n'
    expected+="### $TRANSLATED_ADDED_HEADING"$'\n\n'"- Point."$'\n\n'"</details>"

    [ "$(read_output translations)" = "$expected" ]
}

@test "gives the default locale no collapsed section, its notes being the body's own" {

    export DEFAULT_LOCALE="en" CHANGELOG_LIB="$TMP/changelog.sh"
    run_compose "[$ENGLISH_LOCALE]"

    [ "$status" -eq 0 ]
    [ -z "$(read_output translations)" ]
}

@test "composes the list alone when no changelog helpers are given" {

    write_translated_changelog
    export DEFAULT_LOCALE="en"
    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"

    [ "$status" -eq 0 ]
    [ -n "$(read_output note)" ]
    [ -z "$(read_output translations)" ]
}

@test "fails when a translated changelog has no section for the version" {

    write_translated_changelog
    export DEFAULT_LOCALE="en" CHANGELOG_LIB="$TMP/changelog.sh" VERSION="0.3.0"
    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"

    [ "$status" -ne 0 ]
    [[ "$output" == *"localisation/zh-hans/CHANGELOG.md has no '## [0.3.0]' section"* ]]
}

@test "fails when a translated locale has no changelog" {

    export DEFAULT_LOCALE="en" CHANGELOG_LIB="$TMP/changelog.sh"
    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"

    [ "$status" -ne 0 ]
    [[ "$output" == *"locale zh-hans has no localisation/zh-hans/CHANGELOG.md"* ]]
}

@test "fails when the changelog helpers are not where they were said to be" {

    write_translated_changelog
    export DEFAULT_LOCALE="en" CHANGELOG_LIB="$TMP/absent/changelog.sh"
    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"

    [ "$status" -ne 0 ]
    [[ "$output" == *"changelog helpers not found at $TMP/absent/changelog.sh"* ]]
    [ ! -s "$GITHUB_OUTPUT" ]
}

@test "requires the default locale to tell the translated locales apart" {

    export CHANGELOG_LIB="$TMP/changelog.sh"
    run_compose "[$ENGLISH_LOCALE,$SIMPLIFIED_CHINESE_LOCALE]"

    [ "$status" -ne 0 ]
    [[ "$output" == *"DEFAULT_LOCALE is required"* ]]
}

@test "emits empty outputs for a mod keeping no locales" {

    run_compose "[]"

    [ "$status" -eq 0 ]
    [ -z "$(read_output note)" ]
    [ -z "$(read_output translations)" ]
}

@test "emits empty outputs when no locales are given at all" {

    run_compose ""

    [ "$status" -eq 0 ]
    [ -z "$(read_output note)" ]
    [ -z "$(read_output translations)" ]
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
