#!/usr/bin/env bats
# Contract tests between each composite action's outputs: block and the keys
# its script writes to $GITHUB_OUTPUT.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/action_outputs.bats
#
# The two halves live in separate files that have to be edited together, and
# nothing else notices when they are not: an action declaring an output whose
# value names a key no script writes interpolates empty rather than failing,
# and a script emitting a key the action does not declare is simply invisible
# to consumers. Either way the release keeps running and a workflow reads a
# blank string where it expected a value.
#
# The asserted contract is not that the two sets of NAMES match - check-version
# deliberately publishes version-updated for a script writing version_updated,
# because the hyphenated form is the convention consumers expect and the
# underscored one keeps its bats fixtures stable. What must match is the set of
# keys the script writes and the set of keys the action's value: expressions
# read back out of it.

# Every action declaring an outputs: block, stated once. The second test below
# is what keeps this list honest as actions are added.
ACTIONS_WITH_OUTPUTS=(
    check-dependency-release
    check-version
    compose-dependency-note
    compose-locale-note
    read-locales
    read-mod-info
)

ACTIONS_DIR_NAME="actions"

# The shared lib holding the only writers of $GITHUB_OUTPUT, and the files under
# it that write, each holding one writer.
GITHUB_OUTPUT_LIB_NAME="_lib/github_output.sh"
GITHUB_OUTPUT_WRITER_FILES=(
    "_lib/github_output/write_github_output.sh"
    "_lib/github_output/write_multiline_output.sh"
)

# Echoes the keys a script writes to $GITHUB_OUTPUT, one per line, sorted.
# Every key goes through a `write_github_output "key" ...` or
# `write_multiline_output "key" ...` call - the last test below holds that -
# so matching those two shapes is enough and needs no YAML or shell parse.
read_emitted_keys() {

    local scriptPath="$1"
    grep -oE '^[[:space:]]*(write_github_output|write_multiline_output) "[a-z][a-z0-9_-]*"' "$scriptPath" \
        | sed -E 's/.*(write_github_output|write_multiline_output) "([a-z0-9_-]+)"$/\2/' \
        | sort -u
}

# Echoes the keys an action.yml reads back out of its step, one per line,
# sorted. Every output's value is a steps.<id>.outputs.<key> expression, and
# the trailing <key> is the half that has to exist on the script side.
read_referenced_keys() {

    local actionPath="$1"
    grep -oE 'steps\.[a-z0-9_-]+\.outputs\.[a-z0-9_-]+' "$actionPath" \
        | sed -E 's/.*\.outputs\.//' \
        | sort -u
}

# Maps an action directory to the script backing it: hyphens become
# underscores (read-mod-info -> read_mod_info.sh), which is the convention
# every action here follows. A future action breaking it fails this suite
# rather than going unchecked.
derive_script_path() {

    local actionsDir="$1" action="$2"
    printf '%s/%s/scripts/%s.sh' "$actionsDir" "$action" "${action//-/_}"
}

setup() {
    ACTIONS_DIR="$BATS_TEST_DIRNAME/../$ACTIONS_DIR_NAME"
}

# These suites load no bats assertion library, so a case reports its own
# reason rather than through bats-support's fail(). Returning non-zero from
# the test body is what marks it failed.
report() {
    echo "$1" >&2
}

# Each action is its own arrange, act and assert, in turn.
@test "every action declares an output for each key its script emits" {

    for action in "${ACTIONS_WITH_OUTPUTS[@]}"; do

        # ARRANGE
        actionPath="$ACTIONS_DIR/$action/action.yml"
        scriptPath="$(derive_script_path "$ACTIONS_DIR" "$action")"

        # ACT
        emitted=""
        referenced=""

        if [ -f "$scriptPath" ]; then
            emitted="$(read_emitted_keys "$scriptPath")"
        fi

        if [ -f "$actionPath" ]; then
            referenced="$(read_referenced_keys "$actionPath")"
        fi

        # ASSERT
        if [ ! -f "$actionPath" ]; then
            report "no action.yml for '$action'"
            return 1
        fi

        if [ ! -f "$scriptPath" ]; then
            report "no script for '$action' at $scriptPath"
            return 1
        fi

        # Non-empty on both sides first: a silently-broken extraction above
        # would otherwise compare two empty sets and pass every case.
        if [ -z "$emitted" ]; then
            report "no emitted keys found in $scriptPath"
            return 1
        fi

        if [ -z "$referenced" ]; then
            report "no referenced keys found in $actionPath"
            return 1
        fi

        if [ "$emitted" != "$referenced" ]; then
            report "$action: script emits [$(echo "$emitted" | tr '\n' ' ')]"
            report "$action: action.yml references [$(echo "$referenced" | tr '\n' ' ')]"
            return 1
        fi
    done
}

# Without this the test above passes by omission: a new action arrives
# with its own outputs and nothing checks its two halves agree.
@test "the covered action list names every action declaring outputs" {

    # ARRANGE
    expectedActions="$(printf '%s\n' "${ACTIONS_WITH_OUTPUTS[@]}" | sort)"

    # ACT
    actualActions="$(cd "$ACTIONS_DIR" && grep -l '^outputs:' -- */action.yml \
        | sed -E 's#/action\.yml$##' | sort)"

    # ASSERT
    [ "$actualActions" = "$expectedActions" ]
}

# A key written by hand is neither logged nor seen by the first test.
@test "no script writes \$GITHUB_OUTPUT but the shared lib" {

    # ARRANGE
    expectedWriters="$(printf '%s\n' "${GITHUB_OUTPUT_WRITER_FILES[@]}" | sort)"

    # ACT
    actualWriters="$(cd "$ACTIONS_DIR" && grep -lE '>>[[:space:]]*"?\$\{?GITHUB_OUTPUT' -r --include='*.sh' . \
        | sed 's#^\./##' | sort)"

    # ASSERT
    if [ "$actualWriters" != "$expectedWriters" ]; then
        report "scripts writing \$GITHUB_OUTPUT: [$(echo "$actualWriters" | tr '\n' ' ')]"
        report "write each output through $GITHUB_OUTPUT_LIB_NAME instead"
        return 1
    fi
}
