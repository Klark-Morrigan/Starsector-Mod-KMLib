#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_has_dependency.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Echoes "true" when mod_info.json declares a dependency with the given id,
# "false" otherwise.
#
# Separate from reading the version because the two answers are not
# interchangeable: a mod that declares no KMLib dependency is a normal case
# (KMLib releasing itself), while one that declares the dependency without a
# version is a mistake worth failing on. A single "empty means absent" read
# would flatten those into one state and lose the error.
mod_info_has_dependency() {

    local dependencyId="${1}"

    # `.dependencies // []` covers a mod_info.json with no dependencies key at
    # all, which is otherwise a jq error rather than an empty result.
    jq --arg id "${dependencyId}" \
        '[.dependencies // [] | .[] | select(.id == $id)] | length > 0' \
        "${MOD_INFO_FILE}"
}
