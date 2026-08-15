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
ACTIONS_WITH_OUTPUTS=(check-dependency-release check-version read-mod-info)

ACTIONS_DIR_NAME="actions"

# Echoes the keys a script writes to $GITHUB_OUTPUT, one per line, sorted.
# Both writers use the same `{ echo "key=value" ... } >> "$GITHUB_OUTPUT"`
# block, so matching the echo lines is enough and needs no YAML or shell parse.
read_emitted_keys() {
    local scriptPath="$1"
    grep -oE '^[[:space:]]*echo "[a-z][a-z0-9_-]*=' "$scriptPath" \
        | sed -E 's/.*echo "([a-z0-9_-]+)=/\1/' \
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

@test "every action declares an output for each key its script emits" {
    for action in "${ACTIONS_WITH_OUTPUTS[@]}"; do
        actionPath="$ACTIONS_DIR/$action/action.yml"
        scriptPath="$(derive_script_path "$ACTIONS_DIR" "$action")"

        if [ ! -f "$actionPath" ]; then
            report "no action.yml for '$action'"
            return 1
        fi
        if [ ! -f "$scriptPath" ]; then
            report "no script for '$action' at $scriptPath"
            return 1
        fi

        emitted="$(read_emitted_keys "$scriptPath")"
        referenced="$(read_referenced_keys "$actionPath")"

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

@test "the covered action list names every action declaring outputs" {
    # Without this the test above passes by omission: a new action arrives
    # with its own outputs and nothing checks its two halves agree.
    declared="$(cd "$ACTIONS_DIR" && grep -l '^outputs:' -- */action.yml \
        | sed -E 's#/action\.yml$##' | sort)"
    covered="$(printf '%s\n' "${ACTIONS_WITH_OUTPUTS[@]}" | sort)"
    [ "$declared" = "$covered" ]
}
