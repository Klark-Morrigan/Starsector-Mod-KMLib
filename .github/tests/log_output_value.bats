#!/usr/bin/env bats
# Unit tests for .github/actions/_lib/log_output_value.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/log_output_value.bats

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

setup() {

    # shellcheck source=../actions/_lib/log_output_value.sh
    source "$BATS_TEST_DIRNAME/../actions/_lib/log_output_value.sh"
}

@test "logs the key and its value on stderr alone" {

    run --separate-stderr log_output_value "default-locale" "en"

    [ "$status" -eq 0 ]
    [ "$stderr" = "default-locale: en" ]
    [ "$output" = "" ]
}

# A trailing space after the colon could not be seen in the log.
@test "logs the key alone for an empty value" {

    run --separate-stderr log_output_value "default-locale" ""

    [ "$stderr" = "default-locale:" ]
}
