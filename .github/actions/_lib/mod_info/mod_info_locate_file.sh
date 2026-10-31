#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_locate_file.

# Points MOD_INFO_FILE at the file holding the mod's metadata in the working
# directory, which is the caller's checkout root when a composite action
# invokes these scripts: the committed base when there is one, else
# mod_info.json. Logs the file chosen, and fails the run when neither is
# there.
#
# The base wins whenever it is present, because beside it mod_info.json is
# generated - absent on a fresh checkout, and on whichever locale was last
# built otherwise. The two differ only in text a locale translates, and the
# base's is the text every locale falls back to.
mod_info_locate_file() {

    if [[ -f "${MOD_INFO_BASE_FILE}" ]]; then
        MOD_INFO_FILE="${MOD_INFO_BASE_FILE}"
    elif [[ -f "${MOD_INFO_LAUNCHER_FILE}" ]]; then
        MOD_INFO_FILE="${MOD_INFO_LAUNCHER_FILE}"
    else
        echo "${SCRIPT_NAME}: neither ${MOD_INFO_BASE_FILE} nor ${MOD_INFO_LAUNCHER_FILE} found in ${PWD}" >&2
        exit 1
    fi

    echo "Reading ${MOD_INFO_FILE}." >&2
}
