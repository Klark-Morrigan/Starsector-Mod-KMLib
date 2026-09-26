#!/usr/bin/env bash
# Reads of JSON handed between the release actions, in one place: the locale
# list travels between jobs as a JSON array, and three scripts read lines out
# of jq, each needing the same two guards.
#
# Sourced, not executed: defines the json_* functions below. Failures report
# through the sourcing script's own SCRIPT_NAME.

SCRIPT_NAME="${SCRIPT_NAME:-json}"

# Fails the run unless the given text is a JSON array, naming it by the label.
#
# An input arriving from another job is a string until parsed, and a reading
# of one that is not an array fails partway through, in whatever jq says.
json_require_array() {
  local jsonText="${1}" label="${2}"
  if ! jq -e 'type == "array"' <<< "${jsonText}" > /dev/null 2>&1; then
    echo "${SCRIPT_NAME}: ${label} is not a JSON array: ${jsonText}" >&2
    exit 1
  fi
}

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
