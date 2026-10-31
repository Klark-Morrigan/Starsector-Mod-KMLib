#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_read_release_fields.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Locates the metadata file and reads the three fields every release file name
# follows from into MOD_ID, VERSION and JAR_SOURCE. Fails the run on the first
# one missing, so no caller emits a malformed name like "dist/null/".
#
# The three are not initialised when the lib is sourced: a sourcing script
# may read VERSION from its environment.
mod_info_read_release_fields() {

    mod_info_locate_file

    MOD_ID=$(jq -r '.id' "${MOD_INFO_FILE}")
    VERSION=$(jq -r '.version' "${MOD_INFO_FILE}")
    JAR_SOURCE=$(jq -r '.jars[0]' "${MOD_INFO_FILE}")

    mod_info_require_fields "id:${MOD_ID}" "version:${VERSION}" "jars[0]:${JAR_SOURCE}"
}
