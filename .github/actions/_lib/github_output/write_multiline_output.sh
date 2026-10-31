#!/usr/bin/env bash
# Sourced through ../github_output.sh, not on its own: defines write_multiline_output.

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154

# Appends one output in the multi-line form - "key<<delimiter", the value,
# then the delimiter alone - and logs it as "key:" with the value's lines
# indented beneath, a blank line left blank.
#   write_multiline_output <key> <value>
#
# The delimiter is random and drawn again while the value holds it, so no line
# of the value can end it early.
write_multiline_output() {

    local outputKey="${1}" outputValue="${2}" delimiter valueLine
    delimiter="$(draw_output_delimiter)"

    while [[ "${outputValue}" == *"${delimiter}"* ]]; do
        delimiter="$(draw_output_delimiter)"
    done

    {
        echo "${outputKey}<<${delimiter}"
        printf '%s\n' "${outputValue}"
        echo "${delimiter}"
    } >> "${GITHUB_OUTPUT}"

    echo "${outputKey}:" >&2

    if [[ -n "${outputValue}" ]]; then
        while IFS= read -r valueLine; do
            echo "${valueLine:+  ${valueLine}}"
        done <<< "${outputValue}" >&2
    fi
}
