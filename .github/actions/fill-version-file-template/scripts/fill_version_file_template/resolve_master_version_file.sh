#!/usr/bin/env bash
# Sourced by ../fill_version_file_template.sh, not on its own: defines resolve_master_version_file.

# Echoes the address an install polls for this file: the template's
# masterVersionFile, or for a locale that locale's copy beside it.
#   resolve_master_version_file <template file> <mod id> <locale>
#
# Where a mod serves its master copy is a policy choice rather than something
# a release can work out - a release asset, a raw branch path, or a host that
# is not GitHub at all are all valid answers - so the template states it.
# Required because without an address to poll, the generated file is inert,
# and nothing downstream reads it until a player's update checker does.
#
# A localised release serves each locale's copy as a release asset beside the
# unlocalised one, so the template's address - written for the unlocalised
# name - leads to a locale's copy by swapping its last segment. An address
# ending in another name is a place this release does not publish locale
# copies to, so it fails rather than point an install at nothing.
resolve_master_version_file() {

    local templateFile="${1}" modId="${2}" locale="${3}"
    local masterVersionFile unlocalisedFileName

    masterVersionFile="$(jq -r '.masterVersionFile' "${templateFile}")" || return 1

    if [[ -z "${masterVersionFile}" ]] || [[ "${masterVersionFile}" == "null" ]]; then
        echo "${SCRIPT_NAME}: ${templateFile} is missing required field 'masterVersionFile'" >&2
        return 1
    fi

    if [[ -z "${locale}" ]]; then
        printf '%s\n' "${masterVersionFile}"
        return
    fi

    unlocalisedFileName="$(mod_info_derive_version_file_name "${modId}")"

    if [[ "${masterVersionFile##*/}" != "${unlocalisedFileName}" ]]; then

        echo "${SCRIPT_NAME}: ${templateFile} masterVersionFile '${masterVersionFile}' does not end in" \
             "${unlocalisedFileName}, so it names no copy for locale ${locale}" >&2

        return 1
    fi

    printf '%s\n' "${masterVersionFile%/*}/$(mod_info_derive_version_file_name "${modId}" "${locale}")"
}
