#!/usr/bin/env bash
# Reports whether the version about to be released has already been tagged
# in the caller's checkout, which is what decides whether the release
# pipeline does any work.
#
# Reads from the environment (set by action.yml):
#   VERSION  the version to look for
#
# Emits to $GITHUB_OUTPUT:
#   version_updated  "true" when no tag names that version, "false" when one
#                    already does
#
# The version is taken from the caller rather than read from mod_info.json
# here. read-mod-info has already read that file for the rest of the
# pipeline, and a second read of the same field is a second chance for the
# two to disagree; it also leaves this script asking git one question and
# touching no files at all.
#
# Requires a checkout carrying the repository's tags. The question is which
# tags exist, not which one is nearest to HEAD, so a shallow checkout that
# fetched none of them reports every version as unreleased - see the
# fetch-depth the calling workflow sets.
#
# A thin script (rather than inline action steps) so bats can exercise it
# in isolation - composite actions are not unit-testable directly.
set -euo pipefail

SCRIPT_NAME="check_version"

VERSION="${VERSION:?${SCRIPT_NAME}: VERSION is required}"

# ^{} peels an annotated tag to the commit it points at, so both tag
# flavours are treated alike; only whether the ref resolves matters here.
if git rev-parse -q --verify "refs/tags/${VERSION}^{}" >/dev/null 2>&1; then
  version_updated="false"
else
  version_updated="true"
fi

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154
echo "version_updated=${version_updated}" >> "${GITHUB_OUTPUT}"
