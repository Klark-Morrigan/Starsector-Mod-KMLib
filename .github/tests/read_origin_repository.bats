#!/usr/bin/env bats
# Unit tests for .github/actions/_lib/read_origin_repository.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/read_origin_repository.bats
#
# Runs real git against a throwaway repository, since reading its remote is
# the function's whole job. Each case runs the lib in a subshell (bash -c)
# because the helper exits on failure.

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

LIB="$BATS_TEST_DIRNAME/../actions/_lib/read_origin_repository.sh"
REPOSITORY="Klark-Morrigan/Starsector-Mod-Fixture"

setup() {

    CHECKOUT_DIR="$BATS_TEST_TMPDIR/checkout"

    mkdir -p "$CHECKOUT_DIR"
    git -C "$CHECKOUT_DIR" init --quiet
}

# Sets the checkout's origin to the given URL.
set_origin() {

    git -C "$CHECKOUT_DIR" remote add origin "$1"
}

read_repository() {

    run --separate-stderr bash -c "set -euo pipefail
                                   SCRIPT_NAME='show-download-counts'
                                   source '$LIB'
                                   read_origin_repository '$CHECKOUT_DIR'"
}

@test "reads an https origin" {

    set_origin "https://github.com/$REPOSITORY.git"

    read_repository

    [ "$status" -eq 0 ]
    [ "$output" = "$REPOSITORY" ]
}

@test "reads an https origin without .git" {

    set_origin "https://github.com/$REPOSITORY"

    read_repository

    [ "$output" = "$REPOSITORY" ]
}

@test "reads an scp-like ssh origin" {

    set_origin "git@github.com:$REPOSITORY.git"

    read_repository

    [ "$output" = "$REPOSITORY" ]
}

@test "reads an ssh:// origin" {

    set_origin "ssh://git@github.com/$REPOSITORY.git"

    read_repository

    [ "$output" = "$REPOSITORY" ]
}

@test "fails an origin that is not on GitHub, naming it" {

    set_origin "https://example.com/$REPOSITORY.git"

    read_repository

    [ "$status" -ne 0 ]
    [ "$stderr" = "show-download-counts: origin 'https://example.com/$REPOSITORY.git' of $CHECKOUT_DIR is not a GitHub repository." ]
    [ "$output" = "" ]
}

@test "fails a checkout with no origin, naming it" {

    read_repository

    [ "$status" -ne 0 ]
    [[ "$stderr" == *"show-download-counts: $CHECKOUT_DIR has no origin remote to read the repository from." ]]
    [ "$output" = "" ]
}
