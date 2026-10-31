#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_derive_version_file_name.

# Emits the VersionChecker file name for a mod id and, for a mod releasing
# per locale, the locale's tag.
#
# Named after the mod id rather than the jar, unlike the zip: VersionChecker
# locates the file through data/config/version/version_files.csv, which a
# mod writes by hand, and the mod id is the string a mod author has in front
# of them. With no tag this is that file's own name, which is also what a
# localised zip carries it under, since the CSV is one file for every locale;
# with a tag it names that locale's copy served as a release asset.
mod_info_derive_version_file_name() {

    local modId="${1}" localeTag="${2:-}"
    local localeSuffix

    localeSuffix=$(mod_info_format_locale_suffix "${localeTag}")

    printf '%s\n' "${modId}${localeSuffix}${VERSION_FILE_EXTENSION}"
}
