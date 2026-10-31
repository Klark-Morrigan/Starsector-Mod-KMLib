#!/usr/bin/env bash
# Reads of JSON handed between the release actions, in one place: the locale
# list travels between jobs as a JSON array, and three scripts read lines out
# of jq, each needing the same two guards.
#
# Sourced, not executed: loads the json_* functions, one per file under json/,
# so a script sources one file. Failures report through the sourcing script's
# own SCRIPT_NAME.

SCRIPT_NAME="${SCRIPT_NAME:-json}"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=json/json_read_lines.sh
source "${BASH_SOURCE[0]%/*}/json/json_read_lines.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=json/json_require_array.sh
source "${BASH_SOURCE[0]%/*}/json/json_require_array.sh"
