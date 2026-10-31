#!/usr/bin/env bash
# Sourced through ../github_output.sh, not on its own: defines write_github_output.

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154

# Appends "key=value" to $GITHUB_OUTPUT and logs it through log_output_value.
#   write_github_output <key> <value>
#
# A value spanning lines fails the run, naming the key: in this form its
# second line would be read as a key of its own. write_multiline_output takes
# such a value.
write_github_output() {

    local outputKey="${1}" outputValue="${2}"

    if [[ "${outputValue}" == *$'\n'* ]]; then
        echo "${SCRIPT_NAME}: output '${outputKey}' spans more than one line; write it with write_multiline_output" >&2
        exit 1
    fi

    echo "${outputKey}=${outputValue}" >> "${GITHUB_OUTPUT}"
    log_output_value "${outputKey}" "${outputValue}"
}
