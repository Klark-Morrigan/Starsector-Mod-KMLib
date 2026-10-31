#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_derive_mod_folder_name.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Emits the shipped folder name for a jars[0] value: its basename without
# the .jar extension.
#
# The shipped folder is named after the mod's jar, not after its id.
# Starsector itself does not care what a mod folder is called, but a build
# that compiles against an installed mod locates it at
# <mods>/<folder>/jars/<Jar>.jar, and such installs are laid out with the
# jar's own name (mods/KMLib, mods/LazyLib). Deriving the folder from the
# same string keeps a zip install and a hand-deployed one on a single
# layout, including on case-sensitive filesystems where mods/kmlib and
# mods/KMLib are two different directories.
mod_info_derive_mod_folder_name() {

    local jarSource="${1}"

    basename "${jarSource}" "${JAR_EXTENSION}"
}
