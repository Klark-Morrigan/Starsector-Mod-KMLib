#!/usr/bin/env bash
# What mod_info.json contains and what shape its fields take, in one place.
#
# Four composite-action scripts read that file, and before this they each
# carried their own copy of the filename, the "field is present" check, and
# the version shape. A field added to mod_info.json, or a decision about what
# counts as a well-formed version, would have had to land in four scripts to
# take effect.
#
# Lives under .github/actions/ rather than beside Common-Automation's
# .github/lib/ helpers so it sits with the four scripts that source it, each
# of which reaches it by a path relative to its own location. The leading
# underscore marks it as not-an-action, matching the _ci-gradle.yml
# convention.
#
# Sourced, not executed: defines mod_info_require_file,
# mod_info_require_fields, mod_info_derive_mod_folder_name and
# mod_info_derive_zip_name. The two checks report through the sourcing
# script's own SCRIPT_NAME, so a message still names the step a reader saw
# fail.

# Read by the scripts that source this file, which shellcheck cannot see when
# it checks this one on its own.
# shellcheck disable=SC2034

# Prefixes every message below with the sourcing script's name, so a reader
# sees which step failed. Defaulted rather than required: a caller that
# forgets to set it gets a slightly vaguer message, not an unbound-variable
# crash under `set -u` at the moment it is trying to report a real failure.
SCRIPT_NAME="${SCRIPT_NAME:-mod_info}"

MOD_INFO_FILE="mod_info.json"

# Plain SemVer, digits only. The digits-only part is not decoration: the
# game's own parser splits a version on the letter "a" as well as ".", and
# TriOS strips letters out of a mod_info.json version entirely, so a
# suffixed version like 1.2.3a is silently mangled by both rather than
# rejected by either.
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+$'

JAR_EXTENSION=".jar"
ZIP_EXTENSION=".zip"

# Fails the run unless mod_info.json is in the working directory, which is
# the caller's checkout root when a composite action invokes these scripts.
mod_info_require_file() {
  if [[ ! -f "${MOD_INFO_FILE}" ]]; then
    echo "${SCRIPT_NAME}: ${MOD_INFO_FILE} not found in ${PWD}" >&2
    exit 1
  fi
}

# Fails the run on the first absent field, given "<label>:<value>" pairs.
#
# Takes values already read rather than reading them itself: the callers want
# the values anyway, and jq is cheaper called once per field than twice. The
# label is what the caller sees, so it is written as it appears in the file
# ("jars[0]", not "jarSource"). Splitting on the first colon leaves values
# holding colons - URLs, mostly - intact.
mod_info_require_fields() {
  local pair field value
  for pair in "$@"; do
    field="${pair%%:*}"
    value="${pair#*:}"
    if [[ -z "${value}" ]] || [[ "${value}" == "null" ]]; then
      echo "${SCRIPT_NAME}: ${MOD_INFO_FILE} is missing required field '${field}'" >&2
      exit 1
    fi
  done
}

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

# Emits the release zip name for a jars[0] value and a version.
#
# Here rather than in one script because two of them need this string and
# neither may guess it: the release pipeline names the asset it uploads,
# and the version file's directDownloadURL points at that asset. A rule
# stated twice would give a working download link and a 404 the same
# spelling, and only a player following the link would find out.
mod_info_derive_zip_name() {
  local jarSource="${1}" version="${2}"
  local modFolderName
  modFolderName=$(mod_info_derive_mod_folder_name "${jarSource}")
  printf '%s\n' "${modFolderName}-${version}${ZIP_EXTENSION}"
}
