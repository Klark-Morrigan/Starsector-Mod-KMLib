#!/usr/bin/env bash
# Sourced by ../fill_version_file_template.sh, not on its own: defines resolve_zip_name.

# Echoes the name of the zip the download URL points at, and logs whether it
# was given or derived.
#   resolve_zip_name <given zip name> <version> <locale>
#
# Only a release has a zip name to state: it passes read-mod-info's, so the
# name in the URL and the name of the asset uploaded are one string. A caller
# with no such name - the local build - gets the same derivation from the
# shared lib, out of the metadata's jars[0].
resolve_zip_name() {

    local givenZipName="${1}" version="${2}" locale="${3}"
    local jarSource zipName

    if [[ -n "${givenZipName}" ]]; then

        echo "Using zip name ${givenZipName}, as given." >&2
        printf '%s\n' "${givenZipName}"

        return
    fi

    jarSource="$(jq -r '.jars[0]' "${MOD_INFO_FILE}")" || return 1

    # Required only on this path: a caller that stated the zip name needs no
    # jar to work it out from, and refusing one for a field it never reads
    # would fail mods that publish no jar at all.
    mod_info_require_fields "jars[0]:${jarSource}"

    zipName="$(mod_info_derive_zip_name "${jarSource}" "${version}" "${locale}")"

    echo "Using zip name ${zipName}, derived from ${jarSource}." >&2
    printf '%s\n' "${zipName}"
}
