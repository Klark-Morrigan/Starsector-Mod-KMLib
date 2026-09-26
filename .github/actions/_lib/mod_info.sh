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
# Sourced, not executed: defines mod_info_locate_file,
# mod_info_require_fields, mod_info_derive_mod_folder_name,
# mod_info_derive_zip_name, mod_info_derive_version_file_name,
# mod_info_format_locale_suffix, mod_info_has_dependency and
# mod_info_read_dependency_version. The two
# checks report through the sourcing script's own SCRIPT_NAME, so a message
# still names the step a reader saw fail.

# Read by the scripts that source this file, which shellcheck cannot see when
# it checks this one on its own.
# shellcheck disable=SC2034

# Prefixes every message below with the sourcing script's name, so a reader
# sees which step failed. Defaulted rather than required: a caller that
# forgets to set it gets a slightly vaguer message, not an unbound-variable
# crash under `set -u` at the moment it is trying to report a real failure.
SCRIPT_NAME="${SCRIPT_NAME:-mod_info}"

# The file the launcher reads, and the committed file it is generated from by
# a mod keeping its launcher text per locale. That mod's mod_info.json is a
# build output on the default locale, so the base is what states the id, the
# version and the jars - and the pipeline reads it before anything is built.
MOD_INFO_LAUNCHER_FILE="mod_info.json"
MOD_INFO_BASE_FILE="mod_info.base.json"

# Set by mod_info_locate_file to whichever of the two above holds the mod's
# metadata, and read by every helper below. Empty until then, so a helper
# called before it fails on the missing file rather than reading one guessed.
MOD_INFO_FILE=""

# Plain SemVer, digits only. The digits-only part is not decoration: the
# game's own parser splits a version on the letter "a" as well as ".", and
# TriOS strips letters out of a mod_info.json version entirely, so a
# suffixed version like 1.2.3a is silently mangled by both rather than
# rejected by either.
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+$'

JAR_EXTENSION=".jar"
ZIP_EXTENSION=".zip"
VERSION_FILE_EXTENSION=".version"

# Joins a locale tag onto a release file name. A tag is lowercased BCP 47
# (en, zh-hans), which carries hyphens of its own, so a reader splits a name on
# the version rather than on this.
LOCALE_SEPARATOR="-"

# KMLib's mod id, as it appears both as .id in KMLib's own mod_info.json and
# as a dependency entry's .id in every consumer's. One string here because
# the scripts sourcing this file test it for opposite reasons - one asks "am
# I KMLib?", the others ask "do I depend on KMLib?" - and a rename that
# reached only one of them would leave the pipeline quietly half-right.
KMLIB_MOD_ID="kmlib"

# Points MOD_INFO_FILE at the file holding the mod's metadata in the working
# directory, which is the caller's checkout root when a composite action
# invokes these scripts: the committed base when there is one, else
# mod_info.json. Fails the run when neither is there.
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
  printf '%s\n' "${modFolderName}-${version}${localeSuffix}${ZIP_EXTENSION}"
}

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

# Emits the suffix a locale's release files carry, or nothing for no tag -
# which is what a mod keeping no locales releases under.
mod_info_format_locale_suffix() {
  local localeTag="${1:-}"
  if [[ -n "${localeTag}" ]]; then
    printf '%s' "${LOCALE_SEPARATOR}${localeTag}"
  fi
}

# Echoes "true" when mod_info.json declares a dependency with the given id,
# "false" otherwise.
#
# Separate from reading the version below because the two answers are not
# interchangeable: a mod that declares no KMLib dependency is a normal case
# (KMLib releasing itself), while one that declares the dependency without a
# version is a mistake worth failing on. A single "empty means absent" read
# would flatten those into one state and lose the error.
mod_info_has_dependency() {
  local dependencyId="${1}"
  # `.dependencies // []` covers a mod_info.json with no dependencies key at
  # all, which is otherwise a jq error rather than an empty result.
  jq --arg id "${dependencyId}" \
    '[.dependencies // [] | .[] | select(.id == $id)] | length > 0' \
    "${MOD_INFO_FILE}"
}

# Echoes the version a dependency entry pins, or nothing when that entry is
# absent or states no version.
#
# Emits the first match should a file list the same dependency twice. That
# is malformed input either way, and picking a match keeps this a pure read -
# the callers decide what an unusable answer means for them.
mod_info_read_dependency_version() {
  local dependencyId="${1}"
  jq -r --arg id "${dependencyId}" \
    '[.dependencies // [] | .[] | select(.id == $id)][0].version // ""' \
    "${MOD_INFO_FILE}"
}
