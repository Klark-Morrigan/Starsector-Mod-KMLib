#!/usr/bin/env bash
# Decides whether the version in mod_info.json still needs releasing and
# writes two outputs to $GITHUB_OUTPUT:
#   version         - the value from mod_info.json
#   version_updated - "true" if no tag names that version yet, "false" if
#                     one already does
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

# Ask whether a tag already names this exact version, rather than comparing
# against the nearest tag reachable from HEAD. Reachability answers a
# different question and gets two cases wrong: a shallow checkout reports
# "no tags" and releases an already-released version, and a version moved
# backwards counts as a bump because it merely differs from the newest tag.
#
# ^{} peels an annotated tag to the commit it points at, so the check treats
# both tag flavours alike; only whether the ref resolves at all matters here.
if git rev-parse -q --verify "refs/tags/${VERSION}^{}" >/dev/null 2>&1; then
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
