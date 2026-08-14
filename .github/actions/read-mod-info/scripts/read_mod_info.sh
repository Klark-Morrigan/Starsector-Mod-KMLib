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
#   jar-source         - jars[0]
#   sibling-checkouts  - JSON array of repos to clone beside the checkout
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
ZIP_EXTENSION=".zip"
JAR_EXTENSION=".jar"

# The repos hosting the shared Gradle scripts a mod's build applies. Both are
# reached by a path relative to the checkout's parent, so they have to be
# cloned as its neighbours before the build runs.
KMLIB_MOD_ID="kmlib"
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

# The shipped folder is named after the mod's jar, not after its id. Starsector
# itself does not care what a mod folder is called, but a build that compiles
# against an installed mod locates it at <mods>/<folder>/jars/<Jar>.jar, and
# such installs are laid out with the jar's own name (mods/KMLib, mods/LazyLib).
# Deriving the folder from the same string keeps a zip install and a hand-
# deployed one on a single layout, including on case-sensitive filesystems
# where mods/kmlib and mods/KMLib are two different directories.
MOD_FOLDER_NAME=$(basename "${JAR_SOURCE}" "${JAR_EXTENSION}")
DIST_DIR="${DIST_ROOT}/${MOD_FOLDER_NAME}/"
ZIP_NAME="${MOD_FOLDER_NAME}-${VERSION}${ZIP_EXTENSION}"

# Which neighbours a build needs follows from the mod id alone, so no caller
# has to restate the list: a mod applies KMLib's gradle/starsector-mod.gradle,
# and that script applies Common-Java's java-conventions.gradle, so both repos
# have to be present. KMLib itself reaches only Common-Java, being the repo the
# other half of that chain already lives in.
IS_KMLIB_SIBLING_NEEDED=true
if [[ "${MOD_ID}" == "${KMLIB_MOD_ID}" ]]; then
  IS_KMLIB_SIBLING_NEEDED=false
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

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154
{
  echo "mod-id=${MOD_ID}"
  echo "version=${VERSION}"
  echo "runner-label=${RUNNER_LABEL}"
  echo "mod-folder-name=${MOD_FOLDER_NAME}"
  echo "dist-dir=${DIST_DIR}"
  echo "zip-name=${ZIP_NAME}"
  echo "jar-source=${JAR_SOURCE}"
  echo "sibling-checkouts=${SIBLING_CHECKOUTS}"
} >> "${GITHUB_OUTPUT}"
