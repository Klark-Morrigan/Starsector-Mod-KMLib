#!/usr/bin/env bash
# Reads localisation/manifest.json from the caller checkout's working directory
# and emits the locales the mod releases in, so the release pipeline packages
# one zip per locale and names each in the release body.
#
# Emits the following keys to $GITHUB_OUTPUT (one "key=value" per line):
#   locales         - JSON array of { tag, displayName, coreLocalisation }, the
#                     default locale first and the rest by tag; coreLocalisation
#                     is empty for a locale naming none
#   default-locale  - the manifest's defaultLocale
#
# A mod committing no manifest keeps no locales, and gets an empty array and an
# empty default: the pipeline then packages the single unsuffixed zip it always
# has, so a mod adopts locales by committing the manifest and nothing else.
#
# Read with jq, as every other file this pipeline reads, where the build reads
# the manifest with the game's own parser. So the manifest has to be plain JSON
# here even though the build would accept a '#' comment in it; a file breaking
# that fails this step loudly rather than reading as something else.
#
# Only what packaging acts on is checked: the tags, since each becomes part of
# a file name, and the default being among them, since its copy is what an
# existing install polls. The rest of the schema is the build's and the
# localisation fixtures' to hold, and a release runs both before it gets here.
#
# A thin script (rather than inline action steps) so bats can exercise it
# in isolation - composite actions are not unit-testable directly.
set -euo pipefail

SCRIPT_NAME="read_locales"

# Resolved from this script's own location, not from $PWD: these scripts run
# against the caller's checkout, which is never where they live.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/_lib"
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/json.sh
source "${LIB_DIR}/json.sh"

MANIFEST_FILE="localisation/manifest.json"

# Lowercased BCP 47, the rule the build holds a tag to. Checked again here
# because a tag is spliced into the release's file names, so nothing matching
# it can carry a path separator or a shell-significant character into them.
LOCALE_TAG_REGEX='^[a-z]{2,3}(-[a-z0-9]{1,8})*$'

LOCALES="[]"
DEFAULT_LOCALE=""

if [[ -f "${MANIFEST_FILE}" ]]; then

  DEFAULT_LOCALE=$(jq -r '.defaultLocale // ""' "${MANIFEST_FILE}")
  if [[ -z "${DEFAULT_LOCALE}" ]]; then
    echo "${SCRIPT_NAME}: ${MANIFEST_FILE} names no defaultLocale" >&2
    exit 1
  fi

  # Declared here, being filled through json_read_lines' name reference.
  LOCALE_TAGS=()
  json_read_lines LOCALE_TAGS -r '.locales // {} | keys[]' "${MANIFEST_FILE}"
  for localeTag in "${LOCALE_TAGS[@]}"; do
    if ! [[ "${localeTag}" =~ ${LOCALE_TAG_REGEX} ]]; then
      echo "${SCRIPT_NAME}: ${MANIFEST_FILE} locale '${localeTag}' is not a lowercased BCP 47 tag such as en or zh-hans" >&2
      exit 1
    fi
  done

  if ! jq -e --arg tag "${DEFAULT_LOCALE}" '.locales // {} | has($tag)' "${MANIFEST_FILE}" > /dev/null; then
    echo "${SCRIPT_NAME}: ${MANIFEST_FILE} default locale '${DEFAULT_LOCALE}' is not among its locales" >&2
    exit 1
  fi

  # Compact, so the array is one line and parses as one output key. The default
  # leads, being the locale every other one is translated from; the rest follow
  # by tag so the release body lists them in the same order every release.
  LOCALES=$(jq -c --arg defaultTag "${DEFAULT_LOCALE}" \
    '.locales
     | to_entries
     | map({ tag: .key,
             displayName: (.value.displayName // .key),
             coreLocalisation: (.value.coreLocalisation // "") })
     | sort_by(.tag != $defaultTag, .tag)' \
    "${MANIFEST_FILE}")
fi

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154
{
  echo "locales=${LOCALES}"
  echo "default-locale=${DEFAULT_LOCALE}"
} >> "${GITHUB_OUTPUT}"
