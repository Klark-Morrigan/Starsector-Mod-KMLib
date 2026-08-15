#!/usr/bin/env bash
# Reads mod_info.json from the caller checkout's working directory and emits
# the values a mod's CI and release pipeline needs. Callers pass nothing:
# whatever varies between mods is either stated in mod_info.json or follows
# from it by a convention this script defines.
#
# Emits the following keys to $GITHUB_OUTPUT (one "key=value" per line):
#   mod-id             - .id verbatim
#   version            - .version verbatim
#   runner-label       - <mod-id>-runner
#   mod-folder-name    - jars[0] basename without .jar
#   dist-dir           - dist/<mod-folder-name>/
#   zip-name           - <mod-folder-name>-<version>.zip
#   version-file-name  - <mod-id>.version
#   jar-source         - jars[0]
#   sibling-checkouts  - JSON array of repos to clone beside the checkout
#   kmlib-dependency-version - the kmlib dependency's pinned version, or
#                        empty when the mod declares no such dependency
#
# A thin script (rather than inline action steps) so bats can exercise it
# in isolation - composite actions are not unit-testable directly.
set -euo pipefail

SCRIPT_NAME="read_mod_info"

# Resolved from this script's own location, not from $PWD: these scripts run
# against the caller's checkout, which is never where they live.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/_lib"
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${LIB_DIR}/mod_info.sh"

RUNNER_SUFFIX="-runner"
DIST_ROOT="dist"
VERSION_FILE_EXTENSION=".version"

# The repos hosting the shared Gradle scripts a mod's build applies. Both are
# reached by a path relative to the checkout's parent, so they have to be
# cloned as its neighbours before the build runs. KMLIB_MOD_ID comes from the
# shared lib, which the dependency read below uses it against too.
COMMON_JAVA_REPO="Klark-Morrigan/Common-Java"
COMMON_JAVA_PATH="Common-Java"
KMLIB_REPO="Klark-Morrigan/Starsector-Mod-KMLib"
KMLIB_PATH="Starsector-Mod-KMLib"

mod_info_require_file

MOD_ID=$(jq -r '.id' "${MOD_INFO_FILE}")
VERSION=$(jq -r '.version' "${MOD_INFO_FILE}")
JAR_SOURCE=$(jq -r '.jars[0]' "${MOD_INFO_FILE}")

# Fail loudly on missing required fields so callers do not silently emit
# malformed downstream values like "dist/null/".
mod_info_require_fields "id:${MOD_ID}" "version:${VERSION}" "jars[0]:${JAR_SOURCE}"

RUNNER_LABEL="${MOD_ID}${RUNNER_SUFFIX}"

# Named after the mod id rather than the jar, unlike the folder and zip
# below: VersionChecker locates it through data/config/version/
# version_files.csv, which a mod writes by hand, and the mod id is the string
# a mod author has in front of them. The release pipeline names four things
# with it - the file it generates, the artifact it passes between jobs, and
# the release asset the masterVersionFile URL resolves to - so it is derived
# once here rather than reassembled at each of them.
VERSION_FILE_NAME="${MOD_ID}${VERSION_FILE_EXTENSION}"

# Both names follow from jars[0], and the rules for deriving them live in the
# shared lib: the version file the release generates carries the zip name in
# its download URL, so that name cannot be spelled once here and once there.
MOD_FOLDER_NAME=$(mod_info_derive_mod_folder_name "${JAR_SOURCE}")
DIST_DIR="${DIST_ROOT}/${MOD_FOLDER_NAME}/"
ZIP_NAME=$(mod_info_derive_zip_name "${JAR_SOURCE}" "${VERSION}")

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

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154
{
  echo "mod-id=${MOD_ID}"
  echo "version=${VERSION}"
  echo "runner-label=${RUNNER_LABEL}"
  echo "mod-folder-name=${MOD_FOLDER_NAME}"
  echo "dist-dir=${DIST_DIR}"
  echo "zip-name=${ZIP_NAME}"
  echo "version-file-name=${VERSION_FILE_NAME}"
  echo "jar-source=${JAR_SOURCE}"
  echo "sibling-checkouts=${SIBLING_CHECKOUTS}"
  echo "kmlib-dependency-version=${KMLIB_DEPENDENCY_VERSION}"
} >> "${GITHUB_OUTPUT}"
