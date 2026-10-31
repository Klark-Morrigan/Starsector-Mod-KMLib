#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_require_fields.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Fails the run on the first absent field, given "<label>:<value>" pairs.
#
# Takes values already read rather than reading them itself: the callers want
# the values anyway, and jq is cheaper called once per field than twice. The
# label is what the caller sees, so it is written as it appears in the file
# ("jars[0]", not "jarSource"). Splitting on the first colon leaves values
# holding colons - URLs, mostly - intact.
mod_info_require_fields() {

    local pair field value

    for pair in "$@"; do

        field="${pair%%:*}"
        value="${pair#*:}"

        if [[ -z "${value}" ]] || [[ "${value}" == "null" ]]; then
            echo "${SCRIPT_NAME}: ${MOD_INFO_FILE} is missing required field '${field}'" >&2
            exit 1
        fi

    done
}
