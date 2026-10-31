#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_derive_version_template_name.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Emits the name of the VersionChecker template a mod commits, for a mod id.
#
# Here rather than in the script filling it because more than one script reads
# the template: the fill substitutes its tokens, and read_mod_info reads the
# Nexus mod id it states. Named after the unlocalised version file, since one
# template fills every locale's copy. write-version-file.gradle restates the
# name, since a Gradle build cannot call this.
mod_info_derive_version_template_name() {

    local modId="${1}"
    local versionFileName

    versionFileName="$(mod_info_derive_version_file_name "${modId}")"

    printf '%s\n' "${versionFileName}${VERSION_TEMPLATE_SUFFIX}"
}
