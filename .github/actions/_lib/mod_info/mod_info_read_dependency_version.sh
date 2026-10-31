#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_read_dependency_version.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Echoes the version a dependency entry pins, or nothing when that entry is
# absent or states no version.
#
# Emits the first match should a file list the same dependency twice. That
# is malformed input either way, and picking a match keeps this a pure read -
# the callers decide what an unusable answer means for them.
mod_info_read_dependency_version() {

    local dependencyId="${1}"

    jq -r --arg id "${dependencyId}" \
        '[.dependencies // [] | .[] | select(.id == $id)][0].version // ""' \
        "${MOD_INFO_FILE}"
}
