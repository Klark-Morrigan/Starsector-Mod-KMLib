#!/usr/bin/env bats
# Checks how far publish-download-badges.yml lets its token reach.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/download_badge_token_scope.bats
#
# The workflow runs on a schedule with a token that can write every branch,
# tag and release asset of the caller's repository, since no token can be
# scoped to one branch. What holds it in is a handful of lines that a later
# edit could widen without anything failing: the workflow-level grant, the
# publishing job's grant, and the checkout leaving no credentials behind.

WORKFLOW_NAME="publish-download-badges.yml"
PUBLISHING_JOB="publish"

setup() {

    WORKFLOW="$BATS_TEST_DIRNAME/../workflows/$WORKFLOW_NAME"
}

# Echoes the grants in the workflow-level permissions: block, one per line.
read_workflow_permissions() {

    sed -n '/^permissions:/,/^[a-z]/p' "$WORKFLOW" \
        | grep -E '^  [a-z-]+: ' \
        | sed -E 's/^ +//'
}

# Echoes the grants in the publishing job's permissions: block, one per line.
read_job_permissions() {

    sed -n "/^  ${PUBLISHING_JOB}:/,/^    steps:/p" "$WORKFLOW" \
        | sed -n '/^    permissions:/,/^    [a-z]/p' \
        | grep -E '^      [a-z-]+: ' \
        | sed -E 's/^ +//'
}

# Echoes the checkout step, from its uses: line to the next step.
read_checkout_step() {

    sed -n '/uses: actions\/checkout@/,/^      - /p' "$WORKFLOW"
}

@test "the workflow grants read access alone" {

    granted="$(read_workflow_permissions)"

    if [ "$granted" != "contents: read" ]; then

        echo "workflow-level permissions must be 'contents: read' alone, found:" >&2
        echo "${granted:-<none>}" >&2
        return 1
    fi
}

@test "the publishing job raises contents to write and grants nothing else" {

    granted="$(read_job_permissions)"

    if [ "$granted" != "contents: write" ]; then

        echo "$PUBLISHING_JOB's permissions must be 'contents: write' alone, found:" >&2
        echo "${granted:-<none>}" >&2
        return 1
    fi
}

@test "the checkout leaves no credentials in the repository" {

    checkout="$(read_checkout_step)"

    # Non-empty first, so a renamed or repinned step cannot pass by matching
    # nothing at all.
    [ -n "$checkout" ]

    if ! grep -qE '^ +persist-credentials: false$' <<< "$checkout"; then

        echo "the checkout must set persist-credentials: false" >&2
        return 1
    fi
}
