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
# Usage: fill_version_file_template.sh <output-path> [zip-name] [mod-root] [locale]
#
# Reads from the mod root, which defaults to the working directory:
#   mod_info.base.json, else mod_info.json
#                              - .id, .name, .version, .gameVersion, and
#                                .jars[0] when no zip name is given
#   <mod-id>.version.template  - the committed template
#
# The output path is always resolved from where the caller stands, never
# from the mod root, so a caller writing into the checkout and one writing
# beside it each state the path they would state anyway.
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

# The suffix keeps the committed template distinct from the generated
# <mod-id>.version: a dev install symlinks the repo into mods/, and a
# committed file under the generated name would have version_files.csv
# point VersionChecker at quoted tokens instead of numbers.
TEMPLATE_EXTENSION=".version.template"
GITHUB_BASE_URL="https://github.com"
RELEASE_DOWNLOAD_PATH="releases/download"

# Matches any {{...}} left in the filled file. Deliberately broader than the
# token list above: an unknown token is a mistake in the template, and it has
# to be caught here because nothing downstream reads this file until a
# player's update checker does.
TOKEN_REGEX='\{\{[^}]*\}\}'

OUTPUT_PATH="${1:?output path argument required}"
# Optional because only a release has a zip name to state. The pipeline
# passes read-mod-info's output, so the name in the download URL and the
# name of the asset actually uploaded are one string rather than two
# derivations that agree until one changes. A caller with no such name -
# the local build - leaves it empty and gets the same derivation, taken
# from the shared lib both sides read it from.
ZIP_NAME="${2:-}"
# Directory holding mod_info.json and the template. Optional because a
# caller already standing in the mod root - which is every caller running
# against a plain checkout - has nothing to say. The release pipeline is the
# exception: it checks the mod out one level down so the sibling repos can
# sit beside it, and Actions permits no working-directory on a `uses:` step.
MOD_ROOT="${3:-.}"
# The locale tag of the build this file ships in, for a mod releasing one zip
# per locale. Optional because a mod keeping no locales releases one zip under
# no tag, and a local build fills the file for whichever locale it holds. Set,
# it names that locale's zip in the download URL and that locale's copy of this
# file as the master, so an install polls the copy whose download link leads
# back to its own language.
LOCALE_TAG="${4:-}"

# Kept before the move below, so the output path resolves against the
# directory the caller stated it from.
CALLER_DIR="${PWD}"

if [[ ! -d "${MOD_ROOT}" ]]; then
  echo "${SCRIPT_NAME}: mod root ${MOD_ROOT} not found in ${PWD}" >&2
  exit 1
fi
cd "${MOD_ROOT}"

mod_info_locate_file

MOD_ID=$(jq -r '.id' "${MOD_INFO_FILE}")
MOD_NAME=$(jq -r '.name' "${MOD_INFO_FILE}")
VERSION=$(jq -r '.version' "${MOD_INFO_FILE}")
GAME_VERSION=$(jq -r '.gameVersion' "${MOD_INFO_FILE}")

# Fail loudly on missing required fields rather than emitting a file that
# reports the mod as "null" to every update checker that reads it.
mod_info_require_fields "id:${MOD_ID}" "name:${MOD_NAME}" "version:${VERSION}" \
                        "gameVersion:${GAME_VERSION}"

if [[ -z "${ZIP_NAME}" ]]; then
  JAR_SOURCE=$(jq -r '.jars[0]' "${MOD_INFO_FILE}")
  # Required only on this path: a caller that stated the zip name needs no
  # jar to work it out from, and refusing one for a field it never reads
  # would fail mods that publish no jar at all.
  mod_info_require_fields "jars[0]:${JAR_SOURCE}"
  ZIP_NAME=$(mod_info_derive_zip_name "${JAR_SOURCE}" "${VERSION}" "${LOCALE_TAG}")
fi

# The template is named after the mod id, so no caller has to state a
# path that mod_info.json already determines.
TEMPLATE_FILE="${MOD_ID}${TEMPLATE_EXTENSION}"
if [[ ! -f "${TEMPLATE_FILE}" ]]; then
  echo "${SCRIPT_NAME}: template ${TEMPLATE_FILE} not found in ${PWD}" >&2
  exit 1
fi

# Half of the download URL is the publishing repository. The Actions runtime
# exports it, so a release states nothing; a local build has no such variable
# and the checkout's own origin remote is the honest answer, because a URL
# built from anything else would point at a repository this clone does not
# push to. Read here, inside the mod root, so the remote consulted is the
# mod's own rather than that of whatever checkout the caller stood in.
REPOSITORY="${GITHUB_REPOSITORY:-}"
if [[ -z "${REPOSITORY}" ]]; then

  if ! ORIGIN_REMOTE_URL=$(git remote get-url origin 2>/dev/null); then
    echo "${SCRIPT_NAME}: GITHUB_REPOSITORY is unset and ${PWD} has no origin" \
         "remote to read <owner>/<repo> from" >&2
    exit 1
  fi

  # https://host/owner/repo(.git) and git@host:owner/repo(.git) differ only in
  # what separates the host from the owner, so both are read by taking the last
  # two segments and treating ":" as another separator.
  ORIGIN_REMOTE_URL="${ORIGIN_REMOTE_URL%.git}"
  if [[ "${ORIGIN_REMOTE_URL}" != */* ]]; then
    echo "${SCRIPT_NAME}: cannot read <owner>/<repo> out of the origin remote" \
         "'${ORIGIN_REMOTE_URL}'" >&2
    exit 1
  fi
  REPOSITORY_NAME="${ORIGIN_REMOTE_URL##*/}"
  REPOSITORY_OWNER="${ORIGIN_REMOTE_URL%/*}"
  REPOSITORY_OWNER="${REPOSITORY_OWNER##*[:/]}"
  REPOSITORY="${REPOSITORY_OWNER}/${REPOSITORY_NAME}"
fi

# Where a mod serves its master copy is a policy choice rather than
# something a release can work out - a release asset, a raw branch path, or
# a host that is not GitHub at all are all valid answers - so the template
# states it. Checked because without an address to poll, the generated file
# is inert, and nothing downstream reads it until a player's update checker
# does.
MASTER_VERSION_FILE=$(jq -r '.masterVersionFile' "${TEMPLATE_FILE}")
if [[ -z "${MASTER_VERSION_FILE}" ]] || [[ "${MASTER_VERSION_FILE}" == "null" ]]; then
  echo "${SCRIPT_NAME}: ${TEMPLATE_FILE} is missing required field 'masterVersionFile'" >&2
  exit 1
fi

# A localised release serves each locale's copy of this file as a release
# asset beside the unlocalised one, so the template's address - written for the
# unlocalised name - leads to a locale's copy by swapping its last segment. An
# address ending in another name is a place this release does not publish
# locale copies to, so the run fails rather than point an install at nothing.
if [[ -n "${LOCALE_TAG}" ]]; then
  UNLOCALISED_VERSION_FILE_NAME=$(mod_info_derive_version_file_name "${MOD_ID}")
  if [[ "${MASTER_VERSION_FILE##*/}" != "${UNLOCALISED_VERSION_FILE_NAME}" ]]; then
    echo "${SCRIPT_NAME}: ${TEMPLATE_FILE} masterVersionFile '${MASTER_VERSION_FILE}' does not end in" \
         "${UNLOCALISED_VERSION_FILE_NAME}, so it names no copy for locale ${LOCALE_TAG}" >&2
    exit 1
  fi
  MASTER_VERSION_FILE="${MASTER_VERSION_FILE%/*}/$(mod_info_derive_version_file_name "${MOD_ID}" "${LOCALE_TAG}")"
fi

if ! [[ "${VERSION}" =~ ${SEMVER_REGEX} ]]; then
  echo "${SCRIPT_NAME}: version '${VERSION}' does not split into three numeric parts (MAJOR.MINOR.PATCH)" >&2
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
# piped expression would otherwise refer to $replacements, not to it. The
# master address is written back as resolved above, which for an unlocalised
# file is the template's own.
GENERATED=$(jq --argjson replacements "${REPLACEMENTS}" \
  --arg masterVersionFile "${MASTER_VERSION_FILE}" \
  'walk(. as $value
        | if ($value | type) == "string" and ($replacements | has($value))
          then $replacements[$value]
          else $value
          end)
   | .masterVersionFile = $masterVersionFile' \
  "${TEMPLATE_FILE}")

UNREPLACED=$(jq -r --arg tokenRegex "${TOKEN_REGEX}" \
  '[.. | strings | select(test($tokenRegex))] | unique | join(", ")' \
  <<< "${GENERATED}")
if [[ -n "${UNREPLACED}" ]]; then
  echo "${SCRIPT_NAME}: ${TEMPLATE_FILE} has unreplaced token(s): ${UNREPLACED}" >&2
  exit 1
fi

# Back to where the caller stood, because that is what the output path was
# written against. Everything above needed the mod root; nothing below does.
cd "${CALLER_DIR}"

# Callers write both a copy inside the assembled dist payload and a copy for
# the release asset, so the parent directory is not always one that already
# exists.
mkdir -p "$(dirname "${OUTPUT_PATH}")"
printf '%s\n' "${GENERATED}" > "${OUTPUT_PATH}"
