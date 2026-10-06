#!/usr/bin/env bats
# Checks that mod-release.yml re-runs every pull request gate before it
# packages.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/release_gates.bats
#
# A release re-gates the commit it ships, and each gate is a job the workflow
# has to name. A gate added on the pull request side but never to the release
# fails nothing - the release simply ships without it. KMLib's own ci-*.yml
# files stand for the gate list, since every mod on this pipeline keeps the
# same set.

PACKAGING_JOB="prepare-artifact"

setup() {
    WORKFLOWS_DIR="$BATS_TEST_DIRNAME/../workflows"
    RELEASE_WORKFLOW="$WORKFLOWS_DIR/mod-release.yml"
}

# Echoes the name of every pull request gate, sorted: the stem of each
# ci-*.yml that triggers on pull_request.
read_pull_request_gates() {

    local workflow

    for workflow in "$WORKFLOWS_DIR"/ci-*.yml; do

        if grep -qE '^  pull_request:' "$workflow"; then

            basename "$workflow" .yml
        fi

    done | sort -u
}

# Echoes the jobs the release workflow declares, sorted.
read_release_jobs() {

    sed -n '/^jobs:/,$p' "$RELEASE_WORKFLOW" \
        | grep -oE '^  [a-z-]+:' \
        | tr -d ' :' \
        | sort -u
}

# Echoes the jobs the packaging job waits for, sorted. Reads the one-line
# `needs: [a, b]` form the workflow's multi-job needs are written in.
read_packaging_needs() {

    sed -n "/^  ${PACKAGING_JOB}:/,/^    needs:/p" "$RELEASE_WORKFLOW" \
        | grep -E '^    needs:' \
        | sed -E 's/^    needs: *\[(.*)\]$/\1/' \
        | tr ',' '\n' \
        | tr -d ' ' \
        | sort -u
}

@test "the release runs every pull request gate as a job" {

    gates="$(read_pull_request_gates)"
    jobs="$(read_release_jobs)"

    # Both non-empty first, so a broken extraction cannot pass by comparing
    # nothing against nothing.
    [ -n "$gates" ]
    [ -n "$jobs" ]

    missing="$(comm -23 <(printf '%s\n' "$gates") <(printf '%s\n' "$jobs"))"

    if [ -n "$missing" ]; then

        echo "pull request gates the release never runs:" >&2
        echo "$missing" >&2
        return 1
    fi
}

@test "packaging waits for every pull request gate" {

    gates="$(read_pull_request_gates)"
    needs="$(read_packaging_needs)"

    [ -n "$gates" ]
    [ -n "$needs" ]

    missing="$(comm -23 <(printf '%s\n' "$gates") <(printf '%s\n' "$needs"))"

    if [ -n "$missing" ]; then

        echo "$PACKAGING_JOB packages without waiting for:" >&2
        echo "$missing" >&2
        return 1
    fi
}
