#!/usr/bin/env bash
# Sourced by ../fill_version_file_template.sh, not on its own: defines build_token_replacements.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/../../../_lib/mod_info/mod_info_constants.sh"

# Echoes the JSON object mapping each template token to the value it stands
# for. Fails when the version does not split into three numeric parts.
#   build_token_replacements <mod name> <version> <game version> <direct download url>
#
# Built as JSON rather than as sed expressions so each token carries its own
# type - the version components substitute as numbers, everything else as
# strings - and so a mod name holding quotes or backslashes is escaped rather
# than breaking the file it lands in.
build_token_replacements() {

    local modName="${1}" version="${2}" gameVersion="${3}" directDownloadUrl="${4}"
    local major minor patch

    if ! [[ "${version}" =~ ${SEMVER_REGEX} ]]; then
        echo "${SCRIPT_NAME}: version '${version}' does not split into three numeric parts (MAJOR.MINOR.PATCH)" >&2
        return 1
    fi

    IFS='.' read -r major minor patch <<< "${version}"

    # Base-10 arithmetic canonicalises each part before it is emitted as a JSON
    # number: "01" is a legal thing to write in a version string but not legal
    # JSON, and update checkers compare these components numerically.
    jq -n \
        --arg modName "${modName}" \
        --argjson major "$((10#${major}))" \
        --argjson minor "$((10#${minor}))" \
        --argjson patch "$((10#${patch}))" \
        --arg starsectorVersion "${gameVersion}" \
        --arg directDownloadURL "${directDownloadUrl}" \
        '{ "{{modName}}": $modName,
           "{{major}}": $major,
           "{{minor}}": $minor,
           "{{patch}}": $patch,
           "{{starsectorVersion}}": $starsectorVersion,
           "{{directDownloadURL}}": $directDownloadURL }'
}
