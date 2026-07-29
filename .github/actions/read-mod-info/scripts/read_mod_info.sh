#!/usr/bin/env bash
# Reads mod_info.json from the caller checkout's working directory and emits
# the derived values used by KMLib's reusable workflows. The derivation table
# is defined in:
#   docs/dev/implementation/001-reusable-ci-release-workflows/problem.md
#   (section: Convention - derived from mod_info.json).
#
# Emits the following keys to $GITHUB_OUTPUT (one "key=value" per line):
#   mod-id        - .id verbatim
#   version       - .version verbatim
#   runner-label  - <mod-id>-runner
#   dist-dir      - dist/<mod-id>/
#   zip-name      - <mod-id>-<version>.zip
#   jar-source    - jars[0]
#
# A thin script (rather than inline action steps) so bats can exercise it
# in isolation - composite actions are not unit-testable directly.
set -euo pipefail

MOD_INFO_FILE="mod_info.json"
RUNNER_SUFFIX="-runner"
DIST_ROOT="dist"
ZIP_EXTENSION=".zip"

if [ ! -f "$MOD_INFO_FILE" ]; then
  echo "read_mod_info: $MOD_INFO_FILE not found in working directory" >&2
  exit 1
fi

MOD_ID=$(jq -r '.id' "$MOD_INFO_FILE")
VERSION=$(jq -r '.version' "$MOD_INFO_FILE")
JAR_SOURCE=$(jq -r '.jars[0]' "$MOD_INFO_FILE")

# Fail loudly on missing required fields so callers do not silently emit
# malformed downstream values like "dist/null/".
for pair in "id:$MOD_ID" "version:$VERSION" "jars[0]:$JAR_SOURCE"; do
  field="${pair%%:*}"
  value="${pair#*:}"
  if [ -z "$value" ] || [ "$value" = "null" ]; then
    echo "read_mod_info: $MOD_INFO_FILE is missing required field '$field'" >&2
    exit 1
  fi
done

RUNNER_LABEL="${MOD_ID}${RUNNER_SUFFIX}"
DIST_DIR="${DIST_ROOT}/${MOD_ID}/"
ZIP_NAME="${MOD_ID}-${VERSION}${ZIP_EXTENSION}"

{
  echo "mod-id=$MOD_ID"
  echo "version=$VERSION"
  echo "runner-label=$RUNNER_LABEL"
  echo "dist-dir=$DIST_DIR"
  echo "zip-name=$ZIP_NAME"
  echo "jar-source=$JAR_SOURCE"
} >> "$GITHUB_OUTPUT"
