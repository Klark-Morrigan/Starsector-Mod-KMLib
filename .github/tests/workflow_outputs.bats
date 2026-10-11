#!/usr/bin/env bats
# Contract tests between the outputs a workflow reads and the ones declared
# for it: mod-release.yml's job outputs, and the outputs of every KMLib action
# a workflow step runs.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/workflow_outputs.bats
#
# The action-level equivalent of this lives in action_outputs.bats; this is
# the same failure one level up. A `needs.config.outputs.<key>` or
# `steps.<id>.outputs.<key>` naming an output nothing declares is not an error
# to GitHub Actions - the expression interpolates to an empty string and the
# job carries on, so a renamed output surfaces as a mysteriously blank zip
# name, a skipped gate or a release body missing a line, several jobs away
# from the typo that caused it.
#
# Only one direction is asserted. Every reference must be declared, because
# that is the direction that fails silently. The reverse is legitimate: an
# output can be declared before the job that consumes it is written, which is
# the normal state part-way through a change.

WORKFLOW_NAME="mod-release.yml"

# The only job in that workflow declaring outputs. The last test keeps this
# honest if another job gains an outputs: block.
JOB_WITH_OUTPUTS="config"

# How a workflow names one of this repo's actions in registry form, up to the
# action's directory name.
KMLIB_ACTION_PREFIX="Starsector-Mod-KMLib/.github/actions/"

setup() {

    WORKFLOWS_DIR="$BATS_TEST_DIRNAME/../workflows"
    ACTIONS_DIR="$BATS_TEST_DIRNAME/../actions"
    WORKFLOW="$WORKFLOWS_DIR/$WORKFLOW_NAME"
}

# Echoes "<step id> <action>" for every step of the given workflow that has an
# id and runs a KMLib action, one per line. A step starts at its `- ` line;
# its id and uses: may come in either order after it.
read_kmlib_action_steps() {

    local workflow="$1"

    awk -v prefix="$KMLIB_ACTION_PREFIX" '
        /^ *- / { id = ""; action = ""; printed = 0 }
        /^ +id: / { id = $2 }
        index($0, prefix) {
            action = substr($0, index($0, prefix) + length(prefix))
            sub(/@.*/, "", action)
        }
        id != "" && action != "" && !printed { print id, action; printed = 1 }
    ' "$workflow"
}

# Echoes the output keys an action.yml declares, sorted.
read_action_outputs() {

    local actionPath="$1"

    sed -n '/^outputs:/,/^runs:/p' "$actionPath" \
        | grep -oE '^  [a-z0-9-]+:' \
        | tr -d ' :' \
        | sort -u
}

# Echoes the output keys the given workflow reads from the given step, sorted.
read_step_references() {

    local workflow="$1" stepId="$2"

    grep -oE "steps\.${stepId}\.outputs\.[a-z0-9-]+" "$workflow" \
        | sed -E 's/.*\.outputs\.//' \
        | sort -u
}

# Echoes the output keys downstream jobs read from the given job, sorted.
read_referenced_outputs() {

    local job="$1"

    grep -oE "needs\.${job}\.outputs\.[a-z0-9-]+" "$WORKFLOW" \
        | sed -E 's/.*\.outputs\.//' \
        | sort -u
}

# Echoes the output keys the given job declares, sorted. The block runs from
# the job's `outputs:` line to its `steps:` line, which is the shape every
# job in this workflow follows.
read_declared_outputs() {

    local job="$1"

    sed -n "/^  ${job}:/,/^    steps:/p" "$WORKFLOW" \
        | sed -n '/^    outputs:/,/^    steps:/p' \
        | grep -oE '^      [a-z0-9-]+:' \
        | tr -d ' :' \
        | sort -u
}

@test "every job output a downstream job reads is declared" {

    referenced="$(read_referenced_outputs "$JOB_WITH_OUTPUTS")"
    declared="$(read_declared_outputs "$JOB_WITH_OUTPUTS")"

    # Both non-empty first, so a broken extraction cannot pass by comparing
    # nothing against nothing.
    [ -n "$referenced" ]
    [ -n "$declared" ]

    undeclared="$(comm -23 <(printf '%s\n' "$referenced") <(printf '%s\n' "$declared"))"

    if [ -n "$undeclared" ]; then

        echo "read from $JOB_WITH_OUTPUTS but never declared:" >&2
        echo "$undeclared" >&2

        return 1
    fi
}

@test "every step output a workflow reads is declared by the KMLib action the step runs" {

    checked=0

    for workflow in "$WORKFLOWS_DIR"/*.yml; do
        while read -r stepId action; do

            actionPath="$ACTIONS_DIR/$action/action.yml"

            if [ ! -f "$actionPath" ]; then

                echo "$(basename "$workflow"): step '$stepId' runs '$action', which has no action.yml" >&2
                return 1
            fi

            referenced="$(read_step_references "$workflow" "$stepId")"

            [ -n "$referenced" ] || continue

            declared="$(read_action_outputs "$actionPath")"
            undeclared="$(comm -23 <(printf '%s\n' "$referenced") <(printf '%s\n' "$declared"))"

            if [ -n "$undeclared" ]; then

                echo "$(basename "$workflow"): read from step '$stepId' but never declared by $action:" >&2
                echo "$undeclared" >&2
                return 1
            fi

            checked=$((checked + 1))

        done < <(read_kmlib_action_steps "$workflow")
    done

    # A broken extraction would find no step at all and pass by checking
    # nothing.
    [ "$checked" -gt 0 ]
}

# The version gate's correctness is split across two files: check_version.sh
# asks which tags exist, and this workflow is what puts them within reach.
# The script's own suite passes either way, so nothing but this case notices
# a checkout that fetches a shallow slice of history and no tags - which is
# what once made every push to master build and package a release.
@test "the config job checks out full history for the version gate" {

    configCheckout="$(sed -n '/^  config:/,/^      - name: Derive/p' "$WORKFLOW")"

    # Non-empty first, so a renamed job or step cannot pass by matching
    # nothing at all.
    [ -n "$configCheckout" ]

    if ! grep -qE '^ +fetch-depth: 0$' <<< "$configCheckout"; then

        echo "config's checkout must set fetch-depth: 0 - check-version reads" >&2
        echo "the tag refs, and a shallow checkout carries too few of them" >&2
        return 1
    fi
}

@test "config is still the only job declaring outputs" {

    # Without this the test above silently stops covering the workflow the
    # moment a second job starts publishing values.
    jobsWithOutputs="$(grep -oE '^  [a-z-]+:|^    outputs:' "$WORKFLOW" \
        | grep -B1 '^    outputs:' \
        | grep -oE '^  [a-z-]+:' \
        | tr -d ' :' \
        | sort -u)"

    [ "$jobsWithOutputs" = "$JOB_WITH_OUTPUTS" ]
}
