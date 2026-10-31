#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_derive_zip_name.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Emits the release zip name for a jars[0] value, a version and, for a mod
# releasing per locale, the locale's tag.
#
# Here rather than in one script because several need this string and none
# may guess it: the release pipeline names the asset it uploads, the version
# file's directDownloadURL points at that asset, and the release body names
# it to a reader. A rule stated twice would give a working download link and
# a 404 the same spelling, and only a player following the link would find
# out.
mod_info_derive_zip_name() {

    local jarSource="${1}" version="${2}" localeTag="${3:-}"
    local modFolderName localeSuffix

    modFolderName=$(mod_info_derive_mod_folder_name "${jarSource}")
    localeSuffix=$(mod_info_format_locale_suffix "${localeTag}")

    printf '%s\n' "${modFolderName}${ZIP_NAME_VERSION_SEPARATOR}${version}${localeSuffix}${ZIP_EXTENSION}"
}
