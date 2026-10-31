#!/usr/bin/env bash
# The names the mod_info_* functions share. Sourced, not executed: by
# ../mod_info.sh, and by each function file reading one of them, so every file
# states where its names come from.
#
# Loaded once however often it is sourced: a second load would empty
# MOD_INFO_FILE after mod_info_locate_file had set it.

# Read by the files that source this one, which shellcheck cannot see when it
# checks this one on its own.
# shellcheck disable=SC2034

if [[ -n "${MOD_INFO_CONSTANTS_LOADED:-}" ]]; then
    return
fi

MOD_INFO_CONSTANTS_LOADED=true

# Prefixes every message with the sourcing script's name, so a reader sees
# which step failed. Defaulted rather than required: a caller that forgets to
# set it gets a slightly vaguer message, not an unbound-variable crash under
# `set -u` at the moment it is trying to report a real failure.
SCRIPT_NAME="${SCRIPT_NAME:-mod_info}"

# The file the launcher reads, and the committed file it is generated from by
# a mod keeping its launcher text per locale. That mod's mod_info.json is a
# build output on the default locale, so the base is what states the id, the
# version and the jars - and the pipeline reads it before anything is built.
MOD_INFO_LAUNCHER_FILE="mod_info.json"
MOD_INFO_BASE_FILE="mod_info.base.json"

# Set by mod_info_locate_file to whichever of the two above holds the mod's
# metadata, and read by every function after it. Empty until then, so a
# function called before it fails on the missing file rather than reading one
# guessed.
MOD_INFO_FILE=""

# Plain SemVer, digits only. The digits-only part is not decoration: the
# game's own parser splits a version on the letter "a" as well as ".", and
# TriOS strips letters out of a mod_info.json version entirely, so a
# suffixed version like 1.2.3a is silently mangled by both rather than
# rejected by either.
#
# The pattern is kept apart from its anchors so a regex over a whole file name
# can embed it.
SEMVER_PATTERN='[0-9]+\.[0-9]+\.[0-9]+'
SEMVER_REGEX="^${SEMVER_PATTERN}\$"

JAR_EXTENSION=".jar"
ZIP_EXTENSION=".zip"
VERSION_FILE_EXTENSION=".version"

# The suffix keeps the committed template distinct from the generated
# <mod-id>.version: a dev install symlinks the repo into mods/, and a
# committed file under the generated name would have version_files.csv
# point VersionChecker at quoted tokens instead of numbers.
VERSION_TEMPLATE_SUFFIX=".template"

# Joins the folder name and the version in a release zip name.
ZIP_NAME_VERSION_SEPARATOR="-"

# Joins a locale tag onto a release file name. A tag is lowercased BCP 47
# (en, zh-hans), which carries hyphens of its own, so a reader splits a name on
# the version rather than on this.
LOCALE_SEPARATOR="-"

# Names the group of the zip-name regex holding the locale tag. Common-
# Automation's publish-download-badges action reads the group by this name.
ZIP_NAME_LOCALE_GROUP="locale"

# KMLib's mod id, as it appears both as .id in KMLib's own mod_info.json and
# as a dependency entry's .id in every consumer's. One string here because
# the scripts sourcing the lib test it for opposite reasons - one asks "am
# I KMLib?", the others ask "do I depend on KMLib?" - and a rename that
# reached only one of them would leave the pipeline quietly half-right.
KMLIB_MOD_ID="kmlib"
