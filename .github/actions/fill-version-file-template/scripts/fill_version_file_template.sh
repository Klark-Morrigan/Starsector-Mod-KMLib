#!/usr/bin/env bash
# Fills a mod's committed VersionChecker template from mod_info.json and
# writes the result to the path given as an argument.
#
# A mod publishes the same version number in three places: mod_info.json,
# the .version file it ships, and the .version file served as the release
# asset an update checker polls. Generating the latter two from the first
# at release time keeps the number stated once, in the file the game itself
# reads, instead of in three that drift apart the moment one is forgotten.
#
# Usage: fill_version_file_template.sh <output-path> <zip-name>
#
# Reads from the current working directory:
#   mod_info.json     - .id, .name, .version, .gameVersion
#   <mod-id>.version  - the committed template
#
# The template is a complete VersionChecker file whose release-varying
# values are written as tokens. It is the shape the mod publishes: whatever
# keys it states are the keys that come out, so a mod adding modThreadId or
# modNexusId needs no change here. This script only substitutes:
#
#   {{modName}}            - mod_info.json .name
#   {{major}}              - first component of .version, as a JSON number
#   {{minor}}              - second component, as a JSON number
#   {{patch}}              - third component, as a JSON number
#   {{starsectorVersion}}  - mod_info.json .gameVersion
#   {{directDownloadURL}}  - the release asset URL, built below
#
# A token is replaced only when it is the whole value, never as a substring.
# That is what lets the version components come out as JSON numbers rather
# than quoted digits, which is the type VersionChecker compares on. Any
# token surviving substitution fails the run, so a typo in a template
# ships nothing rather than shipping a literal "{{modName}}" to players.
#
# A thin script (rather than inline action steps) so bats can exercise it
# in isolation - composite actions are not unit-testable directly.
set -euo pipefail

SCRIPT_NAME="fill_version_file_template"

# Resolved from this script's own location, not from $PWD: these scripts run
# against the caller's checkout, which is never where they live.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/_lib"
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${LIB_DIR}/mod_info.sh"

TEMPLATE_EXTENSION=".version"
GITHUB_BASE_URL="https://github.com"
RELEASE_DOWNLOAD_PATH="releases/download"

# Matches any {{...}} left in the filled file. Deliberately broader than the
# token list above: an unknown token is a mistake in the template, and it has
# to be caught here because nothing downstream reads this file until a
# player's update checker does.
TOKEN_REGEX='\{\{[^}]*\}\}'

OUTPUT_PATH="${1:?output path argument required}"
# Taken as an argument rather than derived, so the name in the download URL
# and the name of the zip actually uploaded come from one place: the
# read-mod-info action that already emits it.
ZIP_NAME="${2:?zip name argument required}"

# Set by the Actions runtime to <owner>/<repo>. Using it rather than a URL
# restated in each mod's template means the download link points at
# whichever repository is publishing the release, and cannot disagree with
# it.
REPOSITORY="${GITHUB_REPOSITORY:?must be set to <owner>/<repo>}"

mod_info_require_file

MOD_ID=$(jq -r '.id' "${MOD_INFO_FILE}")
MOD_NAME=$(jq -r '.name' "${MOD_INFO_FILE}")
VERSION=$(jq -r '.version' "${MOD_INFO_FILE}")
GAME_VERSION=$(jq -r '.gameVersion' "${MOD_INFO_FILE}")

# Fail loudly on missing required fields rather than emitting a file that
# reports the mod as "null" to every update checker that reads it.
mod_info_require_fields "id:${MOD_ID}" "name:${MOD_NAME}" "version:${VERSION}" \
                        "gameVersion:${GAME_VERSION}"

# The template is named after the mod id, so no caller has to state a
# path that mod_info.json already determines.
TEMPLATE_FILE="${MOD_ID}${TEMPLATE_EXTENSION}"
if [[ ! -f "${TEMPLATE_FILE}" ]]; then
  echo "fill_version_file_template: template ${TEMPLATE_FILE} not found in ${PWD}" >&2
  exit 1
fi

# The one field no release can derive, so the template has to state it:
# without an address to poll, the generated file is inert.
MASTER_VERSION_FILE=$(jq -r '.masterVersionFile' "${TEMPLATE_FILE}")
if [[ -z "${MASTER_VERSION_FILE}" ]] || [[ "${MASTER_VERSION_FILE}" == "null" ]]; then
  echo "fill_version_file_template: ${TEMPLATE_FILE} is missing required field 'masterVersionFile'" >&2
  exit 1
fi

if ! [[ "${VERSION}" =~ ${SEMVER_REGEX} ]]; then
  echo "fill_version_file_template: version '${VERSION}' does not split into three numeric parts (MAJOR.MINOR.PATCH)" >&2
  exit 1
fi
IFS='.' read -r MAJOR MINOR PATCH <<< "${VERSION}"

# Base-10 arithmetic canonicalises each part before it is emitted as a JSON
# number: "01" is a legal thing to write in a version string but not legal
# JSON, and update checkers compare these components numerically.
MAJOR=$((10#${MAJOR}))
MINOR=$((10#${MINOR}))
PATCH=$((10#${PATCH}))

# The release tag is the bare version, matching what mod-release.yml pushes.
DIRECT_DOWNLOAD_URL="${GITHUB_BASE_URL}/${REPOSITORY}/${RELEASE_DOWNLOAD_PATH}/${VERSION}/${ZIP_NAME}"

# Built as JSON rather than as sed expressions so each token carries its own
# type - the version components substitute as numbers, everything else as
# strings - and so a mod name holding quotes or backslashes is escaped
# rather than breaking the file it lands in.
REPLACEMENTS=$(jq -n \
  --arg modName "${MOD_NAME}" \
  --argjson major "${MAJOR}" \
  --argjson minor "${MINOR}" \
  --argjson patch "${PATCH}" \
  --arg starsectorVersion "${GAME_VERSION}" \
  --arg directDownloadURL "${DIRECT_DOWNLOAD_URL}" \
  '{ "{{modName}}": $modName,
     "{{major}}": $major,
     "{{minor}}": $minor,
     "{{patch}}": $patch,
     "{{starsectorVersion}}": $starsectorVersion,
     "{{directDownloadURL}}": $directDownloadURL }')

# walk() reaches nested values (modVersion's components) without this script
# knowing the template's shape, which is what keeps the shape the mod's
# to state. The visited value is bound to $value first because the "." in a
# piped expression would otherwise refer to $replacements, not to it.
GENERATED=$(jq --argjson replacements "${REPLACEMENTS}" \
  'walk(. as $value
        | if ($value | type) == "string" and ($replacements | has($value))
          then $replacements[$value]
          else $value
          end)' \
  "${TEMPLATE_FILE}")

UNREPLACED=$(jq -r --arg tokenRegex "${TOKEN_REGEX}" \
  '[.. | strings | select(test($tokenRegex))] | unique | join(", ")' \
  <<< "${GENERATED}")
if [[ -n "${UNREPLACED}" ]]; then
  echo "fill_version_file_template: ${TEMPLATE_FILE} has unreplaced token(s): ${UNREPLACED}" >&2
  exit 1
fi

# Callers write both a copy inside the assembled dist payload and a copy for
# the release asset, so the parent directory is not always one that already
# exists.
mkdir -p "$(dirname "${OUTPUT_PATH}")"
printf '%s\n' "${GENERATED}" > "${OUTPUT_PATH}"
