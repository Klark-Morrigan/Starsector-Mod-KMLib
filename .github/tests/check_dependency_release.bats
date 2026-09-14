#!/usr/bin/env bats
# Unit tests for the check-dependency-release composite action's script.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/check_dependency_release.bats
#
# gh is stubbed onto PATH so the suite never reaches the network: a test that
# called the real API would depend on a repository's live release list and
# would fail offline. The stub is driven by environment variables, which is
# what lets one stub serve the success, not-found, and lookup-failed cases.
#
# What stubbing cannot cover: the script classifies a 404 by matching gh's
# error text, and a real gh that reworded that text would still pass this
# suite. The format is a named constant on both sides so the assumption is at
# least visible in one place rather than implied by a bare grep.

SCRIPT="$BATS_TEST_DIRNAME/../actions/check-dependency-release/scripts/check_dependency_release.sh"

REPO_UNDER_TEST="Klark-Morrigan/Starsector-Mod-KMLib"
VERSION_UNDER_TEST="1.0.0"
RELEASE_URL_UNDER_TEST="https://github.com/Klark-Morrigan/Starsector-Mod-KMLib/releases/tag/1.0.0"

# gh's own wording for a 404, which the script keys its classification off.
GH_NOT_FOUND_MESSAGE="gh: Not Found (HTTP 404)"

setup() {
    TMP="$(mktemp -d)"
    mkdir -p "$TMP/bin"

    # Records the arguments it was called with, then replays whatever the
    # case asked for. Reading its behaviour from the environment at call
    # time (rather than baking it in per test) keeps one stub for every case.
    cat > "$TMP/bin/gh" <<'STUB'
#!/usr/bin/env bash
printf '%s\n' "$@" > "${GH_ARGS_FILE}"
[ -n "${GH_STUB_STDOUT:-}" ] && printf '%s\n' "${GH_STUB_STDOUT}"
[ -n "${GH_STUB_STDERR:-}" ] && printf '%s\n' "${GH_STUB_STDERR}" >&2
exit "${GH_STUB_EXIT:-0}"
STUB
    chmod +x "$TMP/bin/gh"

    GH_ARGS_FILE="$TMP/gh.args"
    GITHUB_OUTPUT="$TMP/github_output"
    : > "$GITHUB_OUTPUT"
    PATH="$TMP/bin:$PATH"
    export GH_ARGS_FILE GITHUB_OUTPUT PATH

    # Cleared per case so one test's stub setup cannot leak into the next.
    unset GH_STUB_STDOUT GH_STUB_STDERR GH_STUB_EXIT REPO VERSION
}

teardown() {
    rm -rf "$TMP"
}

# Runs the script against the repo/version under test, after the case has
# set up how the stub should answer.
run_check() {
    export REPO="${1:-$REPO_UNDER_TEST}" VERSION="${2:-$VERSION_UNDER_TEST}"
    run bash "$SCRIPT"
}

# Makes the stub answer as gh does for an existing release.
stub_release_found() {
    export GH_STUB_STDOUT="$RELEASE_URL_UNDER_TEST" GH_STUB_EXIT=0
}

# Makes the stub answer as gh does for a tag with no release.
stub_release_missing() {
    export GH_STUB_STDERR="$GH_NOT_FOUND_MESSAGE" GH_STUB_EXIT=1
}

@test "emits the release URL when the release exists" {
    stub_release_found
    run_check
    [ "$status" -eq 0 ]
    grep -qx "release-url=$RELEASE_URL_UNDER_TEST" "$GITHUB_OUTPUT"
}

@test "asks the tags endpoint for the given repo and version" {
    stub_release_found
    run_check
    [ "$status" -eq 0 ]
    # A listing endpoint would page through every release of a long-lived
    # repository to answer the same question.
    grep -qx "repos/$REPO_UNDER_TEST/releases/tags/$VERSION_UNDER_TEST" "$GH_ARGS_FILE"
    grep -qx "api" "$GH_ARGS_FILE"
}

@test "asks for the browser URL, not another of the response's URL fields" {
    stub_release_found
    run_check
    [ "$status" -eq 0 ]
    # A release response carries url (the API endpoint), html_url (the page
    # a player can open) and tarball_url. The stub cannot tell them apart -
    # it replays one string whatever was asked for - so without this the
    # suite would pass just as happily on the wrong field.
    grep -qx -- "--jq" "$GH_ARGS_FILE"
    grep -qx -- ".html_url" "$GH_ARGS_FILE"
}

@test "takes the URL from the response rather than assembling it from the tag" {
    # The API is the authority on where a release lives; a URL built from the
    # tag would be a second guess at the same answer, and would still look
    # right for a release that does not exist.
    export GH_STUB_STDOUT="https://github.example.invalid/elsewhere/releases/tag/9.9.9"
    export GH_STUB_EXIT=0
    run_check
    [ "$status" -eq 0 ]
    grep -qx "release-url=https://github.example.invalid/elsewhere/releases/tag/9.9.9" \
        "$GITHUB_OUTPUT"
}

@test "fails when the repository has no release for the version" {
    stub_release_missing
    run_check
    [ "$status" -ne 0 ]
    [[ "$output" == *"has no release tagged '$VERSION_UNDER_TEST'"* ]]
}

@test "names the two things a missing release can mean" {
    stub_release_missing
    run_check
    [ "$status" -ne 0 ]
    # Whoever reads this has either bumped a pin too early or not released
    # the dependency yet; the message has to leave both open.
    [[ "$output" == *"release that version"* ]]
    [[ "$output" == *"correct the pin"* ]]
}

@test "distinguishes a lookup that failed from a release that is absent" {
    export GH_STUB_STDERR="gh: Internal Server Error (HTTP 500)" GH_STUB_EXIT=1
    run_check
    [ "$status" -ne 0 ]
    # The opposite response to a 404: nothing has been learned about the pin,
    # so the message must not read as a verdict on it.
    [[ "$output" == *"could not check"* ]]
    [[ "$output" != *"has no release tagged"* ]]
}

@test "surfaces the underlying error when the lookup fails" {
    export GH_STUB_STDERR="gh: Bad credentials (HTTP 401)" GH_STUB_EXIT=1
    run_check
    [ "$status" -ne 0 ]
    # Without gh's own message the log says only that something went wrong.
    [[ "$output" == *"HTTP 401"* ]]
}

@test "fails when a found release states no URL" {
    # jq prints an absent field as the string "null", which would otherwise
    # be written out as the link the release body points players at.
    export GH_STUB_STDOUT="null" GH_STUB_EXIT=0
    run_check
    [ "$status" -ne 0 ]
    [[ "$output" == *"states no URL"* ]]
}

@test "fails when the response carries an empty URL" {
    export GH_STUB_STDOUT="" GH_STUB_EXIT=0
    run_check
    [ "$status" -ne 0 ]
    [[ "$output" == *"states no URL"* ]]
}

@test "writes no output when the release is absent" {
    stub_release_missing
    run_check
    [ "$status" -ne 0 ]
    # A half-written output would leave the release body linking nowhere if
    # a caller ever read it past the failure.
    [ ! -s "$GITHUB_OUTPUT" ]
}

@test "writes no output when the lookup fails" {
    export GH_STUB_STDERR="gh: Internal Server Error (HTTP 500)" GH_STUB_EXIT=1
    run_check
    [ "$status" -ne 0 ]
    [ ! -s "$GITHUB_OUTPUT" ]
}

@test "requires a repo" {
    stub_release_found
    export VERSION="$VERSION_UNDER_TEST"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"REPO is required"* ]]
}

@test "requires a version" {
    stub_release_found
    export REPO="$REPO_UNDER_TEST"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"VERSION is required"* ]]
}

@test "does not call gh when an input is missing" {
    stub_release_found
    export REPO="$REPO_UNDER_TEST"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    # Cheapest checks first: an incomplete invocation should cost no API call.
    [ ! -f "$GH_ARGS_FILE" ]
}
