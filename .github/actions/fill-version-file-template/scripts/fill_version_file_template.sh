#!/usr/bin/env bash
# Fills a mod's committed VersionChecker template from mod_info.json and
# writes the result to the path OUTPUT_PATH names.
#
# A mod publishes the same version number in three places: mod_info.json,
# the .version file it ships, and the .version file served as the release
# asset an update checker polls. Generating the latter two from the first
# at release time keeps the number stated once, in the file the game itself
# reads, instead of in three that drift apart the moment one is forgotten.
#
# Inputs are read from the environment, each described where it is read below:
#   OUTPUT_PATH  Required.
#   ZIP_NAME     Default: derived from mod_info.json .jars[0]
#   MOD_ROOT     Default: .
#   LOCALE       Default: none
#
# Reads from the mod root:
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
# Each step is a function of its own under fill_version_file_template/, so this
# file reads as their order. Logs the zip name and the repository it chose and
# where each came from, then the file it writes with the addresses it filled in.
set -euo pipefail

SCRIPT_NAME="fill_version_file_template"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STEPS_DIR="${SCRIPT_DIR}/fill_version_file_template"
LIB_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)/_lib"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${LIB_DIR}/mod_info.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/localisation.sh
source "${LIB_DIR}/localisation.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/read_origin_repository.sh
source "${LIB_DIR}/read_origin_repository.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=fill_version_file_template/build_token_replacements.sh
source "${STEPS_DIR}/build_token_replacements.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=fill_version_file_template/fill_template_tokens.sh
source "${STEPS_DIR}/fill_template_tokens.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=fill_version_file_template/read_publishing_repository.sh
source "${STEPS_DIR}/read_publishing_repository.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=fill_version_file_template/resolve_changelog_url.sh
source "${STEPS_DIR}/resolve_changelog_url.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=fill_version_file_template/resolve_master_version_file.sh
source "${STEPS_DIR}/resolve_master_version_file.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=fill_version_file_template/resolve_zip_name.sh
source "${STEPS_DIR}/resolve_zip_name.sh"

GITHUB_BASE_URL="https://github.com"
RELEASE_DOWNLOAD_PATH="releases/download"

OUTPUT_PATH="${OUTPUT_PATH:?${SCRIPT_NAME}: OUTPUT_PATH is required}"
# Optional because only a release has a zip name to state; resolve_zip_name
# says what an empty one gets.
ZIP_NAME="${ZIP_NAME:-}"
# Directory holding mod_info.json and the template. Optional because a
# caller already standing in the mod root - which is every caller running
# against a plain checkout - has nothing to say. The release pipeline is the
# exception: it checks the mod out one level down so the sibling repos can
# sit beside it, and Actions permits no working-directory on a `uses:` step.
MOD_ROOT="${MOD_ROOT:-.}"
# The locale tag of the build this file ships in, for a mod releasing one zip
# per locale. Optional because a mod keeping no locales releases one zip under
# no tag, and a local build fills the file for whichever locale it holds. Set,
# it names that locale's zip in the download URL, that locale's copy of this
# file as the master, and that locale's changelog, so an install polls the copy
# whose download link leads back to its own language and reads its changes in it.
LOCALE="${LOCALE:-}"

# Kept before the move below, so the output path resolves against the
# directory the caller stated it from.
CALLER_DIR="${PWD}"

mod_info_enter_mod_root "${MOD_ROOT}"
mod_info_locate_file

MOD_ID=$(jq -r '.id' "${MOD_INFO_FILE}")
MOD_NAME=$(jq -r '.name' "${MOD_INFO_FILE}")
VERSION=$(jq -r '.version' "${MOD_INFO_FILE}")
GAME_VERSION=$(jq -r '.gameVersion' "${MOD_INFO_FILE}")

# Fail loudly on missing required fields rather than emitting a file that
# reports the mod as "null" to every update checker that reads it.
mod_info_require_fields "id:${MOD_ID}" "name:${MOD_NAME}" "version:${VERSION}" \
                        "gameVersion:${GAME_VERSION}"

ZIP_NAME=$(resolve_zip_name "${ZIP_NAME}" "${VERSION}" "${LOCALE}")

# The template is named after the mod id, so no caller has to state a
# path that mod_info.json already determines.
TEMPLATE_FILE=$(mod_info_derive_version_template_name "${MOD_ID}")

if [[ ! -f "${TEMPLATE_FILE}" ]]; then
    echo "${SCRIPT_NAME}: template ${TEMPLATE_FILE} not found in ${PWD}" >&2
    exit 1
fi

REPOSITORY=$(read_publishing_repository)
MASTER_VERSION_FILE=$(resolve_master_version_file "${TEMPLATE_FILE}" "${MOD_ID}" "${LOCALE}")
CHANGELOG_URL=$(resolve_changelog_url "${TEMPLATE_FILE}" "${LOCALE}")

# The release tag is the bare version, matching what mod-release.yml pushes.
DIRECT_DOWNLOAD_URL="${GITHUB_BASE_URL}/${REPOSITORY}/${RELEASE_DOWNLOAD_PATH}/${VERSION}/${ZIP_NAME}"

REPLACEMENTS=$(build_token_replacements "${MOD_NAME}" "${VERSION}" "${GAME_VERSION}" "${DIRECT_DOWNLOAD_URL}")
GENERATED=$(fill_template_tokens "${TEMPLATE_FILE}" "${REPLACEMENTS}" "${MASTER_VERSION_FILE}" "${CHANGELOG_URL}")

# Back to where the caller stood, because that is what the output path was
# written against. Everything above needed the mod root; nothing below does.
cd "${CALLER_DIR}"

# Callers write both a copy inside the assembled dist payload and a copy for
# the release asset, so the parent directory is not always one that already
# exists.
mkdir -p "$(dirname "${OUTPUT_PATH}")"

printf '%s\n' "${GENERATED}" > "${OUTPUT_PATH}"

echo "Wrote ${OUTPUT_PATH} from ${TEMPLATE_FILE}, version ${VERSION}:" >&2
echo "  masterVersionFile: ${MASTER_VERSION_FILE}" >&2
echo "  directDownloadURL: ${DIRECT_DOWNLOAD_URL}" >&2

if [[ -n "${CHANGELOG_URL}" ]]; then
    echo "  changelogURL: ${CHANGELOG_URL}" >&2
fi
