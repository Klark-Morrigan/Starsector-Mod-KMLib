#!/usr/bin/env bash
# Compares the version in mod_info.json to the latest git tag and writes
# two outputs to $GITHUB_OUTPUT:
#   version         - the value from mod_info.json
#   version_updated - "true" if version differs from the latest tag,
#                     "false" otherwise (including when no tags exist and
#                     the version happens to equal the "none" sentinel -
#                     an impossible case in practice)
#
# Runs against the caller's checkout: mod_info.json is read from the
# current working directory, which GitHub sets to the caller's repo root
# when this script is invoked from a composite action.
set -euo pipefail

# Supplies MOD_INFO_FILE. Resolved from this script's own location, not from
# $PWD: these scripts run against the caller's checkout, which is never where
# they live.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/_lib"
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${LIB_DIR}/mod_info.sh"

VERSION=$(jq -r .version "${MOD_INFO_FILE}")
LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "none")

if [[ "${VERSION}" == "${LATEST_TAG}" ]]; then
  version_updated="false"
else
  version_updated="true"
fi

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154
{
  echo "version=${VERSION}"
  echo "version_updated=${version_updated}"
} >> "${GITHUB_OUTPUT}"
