#!/usr/bin/env bash
# Where a mod keeps its locale bundles, for the release scripts that read
# them. Each path is relative to the mod root, where those scripts stand.
#
# Sourced, not executed: defines the LOCALISATION_* names and the
# localisation_* functions below. Failures report through the sourcing
# script's own SCRIPT_NAME.

# Read by the scripts that source this file, which shellcheck cannot see when
# it checks this one on its own.
# shellcheck disable=SC2034

SCRIPT_NAME="${SCRIPT_NAME:-localisation}"

LOCALISATION_DIRECTORY="localisation"
LOCALISATION_MANIFEST_FILE="${LOCALISATION_DIRECTORY}/manifest.json"

# The root changelog, and the name of each translated locale's copy.
LOCALISATION_CHANGELOG_FILE="CHANGELOG.md"

# Echoes the path of a translated locale's changelog. Fails, naming the path,
# when the bundle has none: the parity suite requires one, so a missing file
# means the checkout was never tested.
localisation_require_translated_changelog() {

  local localeTag="${1}"
  local changelogFile="${LOCALISATION_DIRECTORY}/${localeTag}/${LOCALISATION_CHANGELOG_FILE}"

  if [[ ! -f "${changelogFile}" ]]; then
    echo "${SCRIPT_NAME}: locale ${localeTag} has no ${changelogFile}" >&2
    return 1
  fi

  printf '%s' "${changelogFile}"
}
