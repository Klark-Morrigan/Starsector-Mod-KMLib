#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_derive_zip_name_regex.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Emits the regex a whole release zip name matches for a jars[0] value,
# whatever its version and locale, so a release listing can be narrowed to the
# mod's zips and away from its version files.
#
# Built from the constants mod_info_derive_zip_name joins, so the name and its
# pattern cannot drift apart. The locale suffix is optional because a mod
# keeping no locales releases unsuffixed. It matches any text rather than the
# tags a mod ships today, so a new locale matches the day it ships.
#
# The tag sits in a named group, so a reader takes each zip's locale from its
# name rather than restating where the tag sits. An unsuffixed zip leaves the
# group empty, and the reader decides which locale that is.
#
# Written for jq's test and capture. The named group is Oniguruma syntax, which
# bash's =~ does not read.
mod_info_derive_zip_name_regex() {

    local jarSource="${1}"
    local modFolderName folderPattern versionSeparatorPattern localeSeparatorPattern extensionPattern localePattern

    modFolderName=$(mod_info_derive_mod_folder_name "${jarSource}")
    folderPattern=$(mod_info_escape_regex_text "${modFolderName}")
    versionSeparatorPattern=$(mod_info_escape_regex_text "${ZIP_NAME_VERSION_SEPARATOR}")
    localeSeparatorPattern=$(mod_info_escape_regex_text "${LOCALE_SEPARATOR}")
    extensionPattern=$(mod_info_escape_regex_text "${ZIP_EXTENSION}")
    localePattern="(${localeSeparatorPattern}(?<${ZIP_NAME_LOCALE_GROUP}>.+))?"

    printf '%s\n' \
        "^${folderPattern}${versionSeparatorPattern}${SEMVER_PATTERN}${localePattern}${extensionPattern}\$"
}
