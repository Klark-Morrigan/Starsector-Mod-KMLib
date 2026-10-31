#!/usr/bin/env bash
# Sourced through ../json.sh, not on its own: defines json_read_lines.

# Runs jq with the given arguments and splits what it prints into the named
# array, one element per line. Standard input passes through to jq, so a
# caller reads a string with a here-string on the call.
#
# Three guards every such read needs, which is why this exists:
#   - jq runs in a command substitution, where set -e sees its failure, rather
#     than in a process substitution, where it would not;
#   - carriage returns are dropped, because jq on Windows ends its lines with
#     CRLF and a local run there would carry them into every value;
#   - no output leaves the array empty, where a here-string of nothing would
#     still read as one empty line.
#
# The array is written through a name reference, which shellcheck reads as a
# variable set and never used.
# shellcheck disable=SC2034
json_read_lines() {

    # The caller's array, by name. Prefixed so it cannot shadow the name a
    # caller passes in.
    local -n jsonReadLinesTarget="${1}"
    shift

    local jqOutput
    jqOutput=$(jq "$@")
    jqOutput="${jqOutput//$'\r'/}"

    jsonReadLinesTarget=()

    if [[ -n "${jqOutput}" ]]; then
        mapfile -t jsonReadLinesTarget <<< "${jqOutput}"
    fi
}
