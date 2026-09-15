#!/usr/bin/env bats
# Contract test between the job outputs mod-release.yml declares and the ones
# its downstream jobs read.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/workflow_outputs.bats
#
# The action-level equivalent of this lives in action_outputs.bats; this is
# the same failure one level up. A `needs.config.outputs.<key>` naming an
# output the config job does not declare is not an error to GitHub Actions -
# the expression interpolates to an empty string and the job carries on, so a
# renamed output surfaces as a mysteriously blank zip name or a release body
# missing a line, several jobs away from the typo that caused it.
#
# Only one direction is asserted. Every reference must be declared, because
# that is the direction that fails silently. The reverse is legitimate: an
# output can be declared before the job that consumes it is written, which is
# the normal state part-way through a change.

WORKFLOW_NAME="mod-release.yml"

# The only job in that workflow declaring outputs. The second test keeps this
# honest if another job gains an outputs: block.
JOB_WITH_OUTPUTS="config"

setup() {
    WORKFLOW="$BATS_TEST_DIRNAME/../workflows/$WORKFLOW_NAME"
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
