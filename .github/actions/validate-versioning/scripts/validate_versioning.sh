#!/usr/bin/env bash
# Validates that the caller checkout is consistent with the version about
# to be released. Enforces four rules from docs/dev/versioning.md:
#
#   1. CHANGELOG.md has a "## [<version>]" section (date suffix optional).
#   2. mod_info.json .version equals the supplied version, so a stale or
#      hand-edited tag cannot release a version different from what the
#      file declares.
#   3. The released version is well-formed SemVer (MAJOR.MINOR.PATCH). A
#      malformed version does not fail loudly downstream - the game and
#      the update checkers each mangle it in their own quiet way - so it
#      is caught here instead.
#   4. If mod_info.json lists a kmlib dependency, that entry's .version
#      field is present and well-formed SemVer too. Gated on the
#      dependency being present so KMLib's own release uses this same
#      action without needing a self-dependency.
#
# Usage: validate_versioning.sh <version>
#
# Reads CHANGELOG.md and mod_info.json from the current directory. Exits
# non-zero with a clear message on the first failed rule.
set -euo pipefail

VERSION="${1:?version argument required}"

CHANGELOG_FILE="CHANGELOG.md"
MOD_INFO_FILE="mod_info.json"
KMLIB_DEP_ID="kmlib"

# Plain SemVer, digits only. The digits-only part is not decoration: the
# game's own parser splits a version on the letter "a" as well as ".", and
# TriOS strips letters out of a mod_info.json version entirely, so a
# suffixed version like 1.2.3a is silently mangled by both rather than
# rejected by either.
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+$'

# Rule 1: changelog section must exist for this version. A literal-string
# match on "## [<version>]" is sufficient because the date suffix is
# optional and any text after the closing bracket is permitted.
if [[ ! -f "${CHANGELOG_FILE}" ]]; then
  echo "ERROR: ${CHANGELOG_FILE} not found in ${PWD}" >&2
  exit 1
fi
if ! grep -qF "## [${VERSION}]" "${CHANGELOG_FILE}"; then
  echo "ERROR: no '## [${VERSION}]' section found in ${CHANGELOG_FILE}" >&2
  exit 1
fi

# Rule 2: mod_info.json .version must equal the input. Without this a
# tag-only bump could publish a release that disagrees with the file the
# game actually reads.
if [[ ! -f "${MOD_INFO_FILE}" ]]; then
  echo "ERROR: ${MOD_INFO_FILE} not found in ${PWD}" >&2
  exit 1
fi
MOD_INFO_VERSION="$(jq -r '.version' "${MOD_INFO_FILE}")"
if [[ "${MOD_INFO_VERSION}" != "${VERSION}" ]]; then
  echo "ERROR: ${MOD_INFO_FILE} .version (${MOD_INFO_VERSION}) does not match released version (${VERSION})" >&2
  exit 1
fi

# Rule 3: the released version itself must be well-formed. Every consumer
# of a malformed version degrades quietly rather than erroring - the game's
# loader drops the dependency check entirely when the version does not
# split into 2-4 components - so this gate is the only place the mistake
# surfaces.
if ! [[ "${VERSION}" =~ ${SEMVER_REGEX} ]]; then
  echo "ERROR: version '${VERSION}' is not well-formed SemVer (MAJOR.MINOR.PATCH, digits only)" >&2
  exit 1
fi

# Rule 4: if a kmlib dependency entry exists, its .version must be in that
# same shape. The game compares dependency versions component-by-component
# as strings, so a pin written in a different shape than the KMLib it names
# never matches it. jq returns "null" (the string) when the field is
# missing, which we treat as malformed.
HAS_KMLIB_DEP="$(jq --arg id "${KMLIB_DEP_ID}" \
  '[.dependencies // [] | .[] | select(.id == $id)] | length > 0' \
  "${MOD_INFO_FILE}")"
if [[ "${HAS_KMLIB_DEP}" == "true" ]]; then
  KMLIB_DEP_VERSION="$(jq -r --arg id "${KMLIB_DEP_ID}" \
    '[.dependencies[] | select(.id == $id)][0].version // ""' \
    "${MOD_INFO_FILE}")"
  if [[ -z "${KMLIB_DEP_VERSION}" ]]; then
    echo "ERROR: ${MOD_INFO_FILE} kmlib dependency is missing a 'version' field" >&2
    exit 1
  fi
  if ! [[ "${KMLIB_DEP_VERSION}" =~ ${SEMVER_REGEX} ]]; then
    echo "ERROR: ${MOD_INFO_FILE} kmlib dependency version '${KMLIB_DEP_VERSION}' is not well-formed SemVer (MAJOR.MINOR.PATCH)" >&2
    exit 1
  fi
fi
