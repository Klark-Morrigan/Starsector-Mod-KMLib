#!/usr/bin/env bash
# Reads the mod's metadata from the caller checkout's working directory -
# mod_info.base.json when committed, else mod_info.json, by the shared lib's
# rule - and the Nexus Mods page its <mod-id>.version.template states, then
# emits the values a mod's CI, release and badge pipelines need. Callers pass
# nothing: whatever varies between mods is either stated in those two files or
# follows from them by a convention this script defines.
#
# Writes one "key=value" line per output to $GITHUB_OUTPUT, and logs each to
# stderr with the files it read. The outputs block of ../action.yml states what
# each key holds.
set -euo pipefail

SCRIPT_NAME="read_mod_info"

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/_lib"
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${LIB_DIR}/mod_info.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/github_output.sh
source "${LIB_DIR}/github_output.sh"

RUNNER_SUFFIX="-runner"
DIST_ROOT="dist"

# The repos hosting the shared Gradle scripts a mod's build applies. Both are
# reached by a path relative to the checkout's parent, so they have to be
# cloned as its neighbours before the build runs. KMLIB_MOD_ID comes from the
# shared lib, which the dependency read below uses it against too.
COMMON_JAVA_REPO="Klark-Morrigan/Common-Java"
COMMON_JAVA_PATH="Common-Java"
KMLIB_REPO="Klark-Morrigan/Starsector-Mod-KMLib"
KMLIB_PATH="Starsector-Mod-KMLib"

# Nexus Mods' id for Starsector, the game every mod's page sits under. Nexus
# addresses a page by the game and the mod id together.
NEXUS_GAME_ID="2672"

# A Nexus Mods id is a page number.
NEXUS_MOD_ID_REGEX='^[0-9]+$'

mod_info_read_release_fields

RUNNER_LABEL="${MOD_ID}${RUNNER_SUFFIX}"

# Named after the mod id rather than the jar, unlike the folder and zip
# below; the shared lib states why, since the release packaging names the
# same file.
VERSION_FILE_NAME=$(mod_info_derive_version_file_name "${MOD_ID}")

# These follow from jars[0], and the rules for deriving them live in the
# shared lib: the version file the release generates carries the zip name in
# its download URL, so that name cannot be spelled once here and once there.
MOD_FOLDER_NAME=$(mod_info_derive_mod_folder_name "${JAR_SOURCE}")
DIST_DIR="${DIST_ROOT}/${MOD_FOLDER_NAME}/"
ZIP_NAME=$(mod_info_derive_zip_name "${JAR_SOURCE}" "${VERSION}")
ZIP_NAME_REGEX=$(mod_info_derive_zip_name_regex "${JAR_SOURCE}")

# Which neighbours a build needs follows from the mod id alone, so no caller
# has to restate the list: a mod applies KMLib's gradle/starsector-mod.gradle,
# and that script applies Common-Java's java-conventions.gradle, so both repos
# have to be present. KMLib itself reaches only Common-Java, being the repo the
# other half of that chain already lives in.
IS_KMLIB_SIBLING_NEEDED=true
if [[ "${MOD_ID}" == "${KMLIB_MOD_ID}" ]]; then
  IS_KMLIB_SIBLING_NEEDED=false
fi

# Which KMLib a mod pins, emitted so the release pipeline can check that such
# a release exists before building anything and link to it from the release
# body. Empty for a mod declaring no KMLib dependency - KMLib's own release -
# which is what the pipeline gates both of those on. Whether a stated pin is
# well-formed is not asked here; validate-versioning owns that rule.
KMLIB_DEPENDENCY_VERSION=$(mod_info_read_dependency_version "${KMLIB_MOD_ID}")

# The Nexus page hosting the mod's files, as the VersionChecker template states
# it for update links. Read from there rather than stated again, so the page a
# player is sent to and the page the download badges count are one id. Empty
# when the template states none, or when the mod commits no template: either
# way the mod is not on Nexus.
VERSION_TEMPLATE_FILE=$(mod_info_derive_version_template_name "${MOD_ID}")
NEXUS_MOD_ID=""

if [[ -f "${VERSION_TEMPLATE_FILE}" ]]; then
  echo "Reading ${VERSION_TEMPLATE_FILE}." >&2
  NEXUS_MOD_ID=$(jq -r '.modNexusId // ""' "${VERSION_TEMPLATE_FILE}")

  # Checked here, where the file can be named: otherwise a typo fails only at
  # the Nexus fetch, a step later and in another repository's action.
  if [[ -n "${NEXUS_MOD_ID}" ]] && ! [[ "${NEXUS_MOD_ID}" =~ ${NEXUS_MOD_ID_REGEX} ]]; then
    echo "${SCRIPT_NAME}: ${VERSION_TEMPLATE_FILE} modNexusId '${NEXUS_MOD_ID}' is not a whole number" >&2
    exit 1
  fi
else
  echo "No ${VERSION_TEMPLATE_FILE}, so no Nexus mod id." >&2
fi

# Built with jq rather than by string concatenation so the emitted array is
# valid JSON whatever the constants above hold - the consuming workflows decode
# it with fromJSON / jq.
SIBLING_CHECKOUTS=$(jq -cn \
  --arg commonJavaRepo "${COMMON_JAVA_REPO}" \
  --arg commonJavaPath "${COMMON_JAVA_PATH}" \
  --arg kmlibRepo "${KMLIB_REPO}" \
  --arg kmlibPath "${KMLIB_PATH}" \
  --argjson isKmlibSiblingNeeded "${IS_KMLIB_SIBLING_NEEDED}" \
  '[{repo: $commonJavaRepo, path: $commonJavaPath}]
     + (if $isKmlibSiblingNeeded
        then [{repo: $kmlibRepo, path: $kmlibPath}]
        else []
        end)')

write_github_output "mod-id" "${MOD_ID}"
write_github_output "version" "${VERSION}"
write_github_output "runner-label" "${RUNNER_LABEL}"
write_github_output "mod-folder-name" "${MOD_FOLDER_NAME}"
write_github_output "dist-dir" "${DIST_DIR}"
write_github_output "zip-name" "${ZIP_NAME}"
write_github_output "zip-name-regex" "${ZIP_NAME_REGEX}"
write_github_output "version-file-name" "${VERSION_FILE_NAME}"
write_github_output "jar-source" "${JAR_SOURCE}"
write_github_output "sibling-checkouts" "${SIBLING_CHECKOUTS}"
write_github_output "kmlib-dependency-version" "${KMLIB_DEPENDENCY_VERSION}"
write_github_output "nexus-game-id" "${NEXUS_GAME_ID}"
write_github_output "nexus-mod-id" "${NEXUS_MOD_ID}"
# Emitted from the same constant the sibling clone above is built from, so
# the repository a release is checked against cannot drift from the one the
# build actually compiles the mod against.
write_github_output "kmlib-repo" "${KMLIB_REPO}"
