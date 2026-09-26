#!/usr/bin/env bash
# Packages the release assets of a mod whose jar is already built: one zip per
# locale it releases in, each carrying its own VersionChecker file, plus that
# file again as a release asset per locale for update checkers to poll.
#
# Inputs are read from the environment (set by action.yml):
#   MOD_ROOT        Directory holding the checkout.         Default: .
#   OUTPUT_DIR      Where the assets land, relative to where the caller
#                   stands. Required.
#   LOCALES         JSON array of { tag, ... } from read-locales. Default: []
#   DEFAULT_LOCALE  The manifest's default locale. Required with LOCALES.
#
# Per locale, in LOCALES order:
#   1. ./gradlew writeLocaleFiles -Plocale=<tag>, which writes that locale's
#      data files and merged mod_info.json into the checkout
#   2. the runtime payload assembled into dist/<folder>/
#   3. <mod-id>.version filled into the payload for that locale
#   4. <folder>-<version>-<tag>.zip and <mod-id>-<tag>.version into OUTPUT_DIR
# then <mod-id>.version into OUTPUT_DIR as a copy of the default locale's.
#
# That last copy is what an install released before locales existed still
# polls: its masterVersionFile names <mod-id>.version, and without it every such
# install's update check would 404, silently, for good. It is a version file,
# not a zip, so every zip still carries its locale.
#
# With no locales - a mod committing no manifest - this is the single pass the
# release has always made: no locale is written, and the zip and the version
# file carry no suffix.
#
# The jar is built once, before this runs: it is identical across locales, so
# only the files a locale changes are rewritten between passes.
#
# A thin script (rather than inline action steps) so bats can exercise it
# in isolation - composite actions are not unit-testable directly.
set -euo pipefail

SCRIPT_NAME="package_release"

# Resolved from this script's own location, not from $PWD: these scripts run
# against the caller's checkout, which is never where they live.
ACTIONS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${ACTIONS_DIR}/_lib/mod_info.sh"

# Called rather than restated, so a zip's version file and the one a mod without
# locales has always shipped come out of one implementation.
FILL_VERSION_FILE_SCRIPT="${ACTIONS_DIR}/fill-version-file-template/scripts/fill_version_file_template.sh"

DIST_ROOT="dist"

# What a player's install needs and nothing else - no source, tests, docs/dev,
# build caches or Gradle wrapper. Each is copied when present, so a jar-only
# library and an asset-bearing content mod share one list.
PAYLOAD_FILES=(README.md CHANGELOG.md LICENSE)
PAYLOAD_DIRECTORIES=(data graphics sounds)

MOD_ROOT="${MOD_ROOT:-.}"
OUTPUT_DIR="${OUTPUT_DIR:?${SCRIPT_NAME}: OUTPUT_DIR is required}"
LOCALES="${LOCALES:-[]}"
DEFAULT_LOCALE="${DEFAULT_LOCALE:-}"

if [[ ! -d "${MOD_ROOT}" ]]; then
  echo "${SCRIPT_NAME}: mod root ${MOD_ROOT} not found in ${PWD}" >&2
  exit 1
fi

# Made absolute before the move below, so it keeps naming the directory the
# caller stated it from.
mkdir -p "${OUTPUT_DIR}"
OUTPUT_DIR="$(cd "${OUTPUT_DIR}" && pwd)"

cd "${MOD_ROOT}"

mod_info_locate_file

MOD_ID=$(jq -r '.id' "${MOD_INFO_FILE}")
VERSION=$(jq -r '.version' "${MOD_INFO_FILE}")
JAR_SOURCE=$(jq -r '.jars[0]' "${MOD_INFO_FILE}")

mod_info_require_fields "id:${MOD_ID}" "version:${VERSION}" "jars[0]:${JAR_SOURCE}"

MOD_FOLDER_NAME=$(mod_info_derive_mod_folder_name "${JAR_SOURCE}")
DIST_DIR="${DIST_ROOT}/${MOD_FOLDER_NAME}"

# The name the payload carries its version file under, whatever the locale:
# VersionChecker finds it through version_files.csv, which is one file for
# every locale.
PAYLOAD_VERSION_FILE_NAME=$(mod_info_derive_version_file_name "${MOD_ID}")

if ! jq -e 'type == "array"' <<< "${LOCALES}" > /dev/null 2>&1; then
  echo "${SCRIPT_NAME}: LOCALES is not a JSON array: ${LOCALES}" >&2
  exit 1
fi

# Carriage returns dropped because jq on Windows ends its lines with CRLF,
# which a local run there would otherwise carry into every file name. Split
# only when non-empty, since a here-string of nothing still reads as one line.
LOCALE_TAG_LINES=$(jq -r '.[].tag' <<< "${LOCALES}")
LOCALE_TAG_LINES="${LOCALE_TAG_LINES//$'\r'/}"
LOCALE_TAGS=()
if [[ -n "${LOCALE_TAG_LINES}" ]]; then
  mapfile -t LOCALE_TAGS <<< "${LOCALE_TAG_LINES}"
fi

IS_LOCALISED=false
if (( ${#LOCALE_TAGS[@]} > 0 )); then

  IS_LOCALISED=true
  IS_DEFAULT_DECLARED=false
  for localeTag in "${LOCALE_TAGS[@]}"; do
    if [[ "${localeTag}" == "${DEFAULT_LOCALE}" ]]; then
      IS_DEFAULT_DECLARED=true
    fi
  done
  if [[ "${IS_DEFAULT_DECLARED}" != "true" ]]; then
    echo "${SCRIPT_NAME}: default locale '${DEFAULT_LOCALE}' is not among the locales ${LOCALES}" >&2
    exit 1
  fi
else
  # One pass under no tag, which every name below reads as "no suffix".
  LOCALE_TAGS=("")
fi

# Copies the runtime payload into a fresh dist folder. Fresh, because a file a
# previous locale shipped and this one does not would otherwise ride along.
assemble_payload() {
  rm -rf "${DIST_DIR}"
  mkdir -p "${DIST_DIR}/jars"

  # The launcher file, never the base: it is what the game reads, and for a
  # mod keeping locales it is the one writeLocaleFiles just merged.
  if [[ ! -f "${MOD_INFO_LAUNCHER_FILE}" ]]; then
    echo "${SCRIPT_NAME}: ${MOD_INFO_LAUNCHER_FILE} not found in ${PWD}; the build writes it from" \
         "${MOD_INFO_BASE_FILE}" >&2
    exit 1
  fi
  cp "${MOD_INFO_LAUNCHER_FILE}" "${DIST_DIR}/"

  local payloadFile payloadDirectory
  for payloadFile in "${PAYLOAD_FILES[@]}"; do
    if [[ -f "${payloadFile}" ]]; then
      cp "${payloadFile}" "${DIST_DIR}/"
    fi
  done
  for payloadDirectory in "${PAYLOAD_DIRECTORIES[@]}"; do
    if [[ -d "${payloadDirectory}" ]]; then
      cp -r "${payloadDirectory}" "${DIST_DIR}/"
    fi
  done
  cp "${JAR_SOURCE}" "${DIST_DIR}/jars/"
}

# Packages one locale, or the unlocalised release for an empty tag.
package_locale() {
  local localeTag="${1}"
  local zipName assetVersionFileName
  zipName=$(mod_info_derive_zip_name "${JAR_SOURCE}" "${VERSION}" "${localeTag}")
  assetVersionFileName=$(mod_info_derive_version_file_name "${MOD_ID}" "${localeTag}")

  if [[ -n "${localeTag}" ]]; then
    ./gradlew writeLocaleFiles "-Plocale=${localeTag}" --no-daemon --console=plain
  fi

  assemble_payload

  # The zip name is passed rather than left to the script to derive, so the
  # download URL and the asset written below are one string.
  bash "${FILL_VERSION_FILE_SCRIPT}" "${DIST_DIR}/${PAYLOAD_VERSION_FILE_NAME}" "${zipName}" "." "${localeTag}"
  cp "${DIST_DIR}/${PAYLOAD_VERSION_FILE_NAME}" "${OUTPUT_DIR}/${assetVersionFileName}"

  # From inside dist, so the zip holds the mod folder at its root - the shape a
  # player unzips into mods/.
  (cd "${DIST_ROOT}" && zip -r -q "${OUTPUT_DIR}/${zipName}" "${MOD_FOLDER_NAME}/")

  echo "${SCRIPT_NAME}: packaged ${zipName} and ${assetVersionFileName}"
}

for localeTag in "${LOCALE_TAGS[@]}"; do
  package_locale "${localeTag}"
done

if [[ "${IS_LOCALISED}" == "true" ]]; then
  DEFAULT_VERSION_FILE_NAME=$(mod_info_derive_version_file_name "${MOD_ID}" "${DEFAULT_LOCALE}")
  cp "${OUTPUT_DIR}/${DEFAULT_VERSION_FILE_NAME}" "${OUTPUT_DIR}/${PAYLOAD_VERSION_FILE_NAME}"
  echo "${SCRIPT_NAME}: copied locale ${DEFAULT_LOCALE}'s version file to ${PAYLOAD_VERSION_FILE_NAME}" \
       "for installs polling the unlocalised name"
fi
