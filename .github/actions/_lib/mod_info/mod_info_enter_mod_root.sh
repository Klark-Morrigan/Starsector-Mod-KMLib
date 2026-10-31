#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_enter_mod_root.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Moves into the given mod root, the directory holding the mod's metadata.
# Fails the run naming that directory when it is not there.
#
# Checked rather than left to cd: a wrong root would otherwise surface as a
# missing mod_info.json, the same symptom from a different cause.
mod_info_enter_mod_root() {

    local modRoot="${1}"

    if [[ ! -d "${modRoot}" ]]; then
        echo "${SCRIPT_NAME}: mod root ${modRoot} not found in ${PWD}" >&2
        exit 1
    fi

    cd "${modRoot}" || exit 1
}
