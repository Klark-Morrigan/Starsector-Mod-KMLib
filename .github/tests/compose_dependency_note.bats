#!/usr/bin/env bats
# Unit tests for the compose-dependency-note composite action's script.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/compose_dependency_note.bats
#
# This is player-facing copy: the line these cases pin is what someone reads
# on a release page before installing the mod. The exact markdown is asserted
# rather than matched loosely, because a link that renders as literal text is
# a broken release note, not a formatting preference.

SCRIPT="$BATS_TEST_DIRNAME/../actions/compose-dependency-note/scripts/compose_dependency_note.sh"

DEPENDENCY_NAME_UNDER_TEST="KMLib"
VERSION_UNDER_TEST="1.0.0"
RELEASE_URL_UNDER_TEST="https://github.com/Klark-Morrigan/Starsector-Mod-KMLib/releases/tag/1.0.0"

setup() {
    TMP="$(mktemp -d)"
    GITHUB_OUTPUT="$TMP/github_output"
    : > "$GITHUB_OUTPUT"
    export GITHUB_OUTPUT
    unset DEPENDENCY_NAME VERSION RELEASE_URL
}

teardown() {
    rm -rf "$TMP"
}

# Runs the script with the given version and URL, defaulting both to the
# values under test so a case states only what it varies.
run_compose() {
    export DEPENDENCY_NAME="${DEPENDENCY_NAME_UNDER_TEST}"
    export VERSION="${1-$VERSION_UNDER_TEST}"
    export RELEASE_URL="${2-$RELEASE_URL_UNDER_TEST}"
    run bash "$SCRIPT"
}

# Echoes the composed note back out of $GITHUB_OUTPUT.
read_note() {
    sed -n 's/^note=//p' "$GITHUB_OUTPUT"
}

@test "composes a markdown link naming the dependency and version" {
    run_compose
    [ "$status" -eq 0 ]
    [ "$(read_note)" = "Requires [KMLib 1.0.0]($RELEASE_URL_UNDER_TEST)." ]
}

@test "puts the version inside the link text, not the URL" {
    run_compose
    [ "$status" -eq 0 ]
    # A reader scanning the body sees "KMLib 1.0.0" as the link; the version
    # belongs in what they read, not only in where it points.
    [[ "$(read_note)" == *"[KMLib 1.0.0]"* ]]
}

@test "emits an empty note when the mod pins no version" {
    # KMLib releasing itself takes this path: no dependency, so no line.
    run_compose "" "$RELEASE_URL_UNDER_TEST"
    [ "$status" -eq 0 ]
    [ "$(read_note)" = "" ]
}

@test "emits an empty note when no release URL is given" {
    # Dropping the line beats publishing "[KMLib 1.0.0]()", whose link goes
    # nowhere while reading as though it works.
    run_compose "$VERSION_UNDER_TEST" ""
    [ "$status" -eq 0 ]
    [ "$(read_note)" = "" ]
}

@test "emits an empty note when neither is given" {
    run_compose "" ""
    [ "$status" -eq 0 ]
    [ "$(read_note)" = "" ]
}

@test "emits the note key even when the note is empty" {
    run_compose "" ""
    [ "$status" -eq 0 ]
    # The caller reads one key that is sometimes empty, rather than having to
    # handle the key being absent for one mod in the series.
    grep -qx "note=" "$GITHUB_OUTPUT"
}

@test "emits the note on a single line" {
    run_compose
    [ "$status" -eq 0 ]
    # $GITHUB_OUTPUT's key=value form carries no newline; a multi-line value
    # would be parsed as further keys rather than as the note.
    [ "$(wc -l < "$GITHUB_OUTPUT")" -eq 1 ]
}

@test "carries the dependency name it is given rather than a fixed one" {
    export DEPENDENCY_NAME="SomeOtherLib"
    export VERSION="$VERSION_UNDER_TEST" RELEASE_URL="$RELEASE_URL_UNDER_TEST"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    [[ "$(read_note)" == "Requires [SomeOtherLib 1.0.0]"* ]]
}

@test "requires a dependency name" {
    export VERSION="$VERSION_UNDER_TEST" RELEASE_URL="$RELEASE_URL_UNDER_TEST"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"DEPENDENCY_NAME is required"* ]]
}

@test "writes no output when the dependency name is missing" {
    export VERSION="$VERSION_UNDER_TEST" RELEASE_URL="$RELEASE_URL_UNDER_TEST"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [ ! -s "$GITHUB_OUTPUT" ]
}
