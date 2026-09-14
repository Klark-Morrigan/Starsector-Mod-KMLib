#!/usr/bin/env bash
# Confirms that a pinned dependency version exists as a published release,
# and emits that release's canonical URL.
#
# One API call answers both questions the release pipeline has. Asking
# whether the release exists and then separately assembling a URL from the
# tag would let the two disagree: the second would produce a link for a
# release the first never saw, and the mistake would surface only to a
# player who clicked it.
#
# Reads from the environment (set by action.yml):
#   REPO      owner/name of the repository publishing the dependency
#   VERSION   release tag to look for
#   GH_TOKEN  token gh authenticates with (consumed by gh, not read here)
#
# Emits to $GITHUB_OUTPUT:
#   release-url  the release's html_url
#
# Exits non-zero when the release is absent AND when the lookup could not be
# completed, with a different message for each. Both are failures - a release
# must not proceed on an unverified pin - but they call for opposite
# responses from whoever reads the log, so they must not read alike: the
# first means the pin is wrong or the dependency has not been released yet,
# the second means nothing has been learned about the pin at all.
set -euo pipefail

SCRIPT_NAME="check_dependency_release"

# How gh reports a 404 on its stderr ("gh: Not Found (HTTP 404)"). Matching
# gh's text is what separates "no such release" from every other failure -
# gh exits 1 for all of them alike, so the exit code alone cannot tell them
# apart. The tests pin this format; a gh that reworded it would fail them
# only if the stub were updated to match, so the format is stated here as
# one named constant rather than buried in the branch that uses it.
HTTP_NOT_FOUND_PATTERN="HTTP 404"

REPO="${REPO:?${SCRIPT_NAME}: REPO is required}"
VERSION="${VERSION:?${SCRIPT_NAME}: VERSION is required}"

# gh writes its diagnostics to stderr and the response to stdout. The two
# have to be told apart to classify the failure, so stderr is captured to a
# file rather than merged into the output being parsed.
ERROR_OUTPUT="$(mktemp)"
trap 'rm -f "${ERROR_OUTPUT}"' EXIT

# The tags endpoint takes the tag directly, so no listing is paged through
# and a repository with hundreds of releases costs the same single call.
if ! RELEASE_URL="$(gh api "repos/${REPO}/releases/tags/${VERSION}" \
    --jq '.html_url' 2>"${ERROR_OUTPUT}")"; then
  if grep -q "${HTTP_NOT_FOUND_PATTERN}" "${ERROR_OUTPUT}"; then
    echo "${SCRIPT_NAME}: ${REPO} has no release tagged '${VERSION}'." >&2
    echo "${SCRIPT_NAME}: release that version of ${REPO} first, or correct the pin." >&2
    echo "${SCRIPT_NAME}: a repository this token cannot see reports the same way." >&2
  else
    echo "${SCRIPT_NAME}: could not check ${REPO} for a release tagged '${VERSION}'." >&2
    echo "${SCRIPT_NAME}: the pin has NOT been shown to be wrong - the lookup itself failed:" >&2
    cat "${ERROR_OUTPUT}" >&2
  fi
  exit 1
fi

# A 200 whose body carries no html_url. jq prints absent fields as the string
# "null", which would otherwise reach the release body as a link target.
if [[ -z "${RELEASE_URL}" ]] || [[ "${RELEASE_URL}" == "null" ]]; then
  echo "${SCRIPT_NAME}: ${REPO} release '${VERSION}' was found but states no URL." >&2
  exit 1
fi

echo "${SCRIPT_NAME}: ${REPO} release '${VERSION}' found at ${RELEASE_URL}"

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154
echo "release-url=${RELEASE_URL}" >> "${GITHUB_OUTPUT}"
