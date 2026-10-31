#!/usr/bin/env bash
# Sourced through ../json.sh, not on its own: defines json_require_array.

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
