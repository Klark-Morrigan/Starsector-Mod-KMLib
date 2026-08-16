#!/usr/bin/env bash
# Composes the markdown line a release body carries naming the dependency
# release the mod requires.
#
# Reads from the environment (set by action.yml):
#   DEPENDENCY_NAME  display name, e.g. KMLib
#   VERSION          version the mod pins; empty means no line is due
#   RELEASE_URL      URL of that release; empty means no line is due
#
# Emits to $GITHUB_OUTPUT:
#   note  the composed line, or empty
#
# Both emptiness cases are ordinary rather than errors, and they are not the
# same case. An empty VERSION is a mod that declares no such dependency -
# KMLib releasing itself - and there is simply no line to write. An empty
# RELEASE_URL alongside a version should not happen, because the pipeline
# only reaches this point once a release has been confirmed to exist; if it
# ever does, dropping the line beats publishing a version with a link that
# goes nowhere, since the link is the half a reader acts on.
set -euo pipefail

SCRIPT_NAME="compose_dependency_note"

DEPENDENCY_NAME="${DEPENDENCY_NAME:?${SCRIPT_NAME}: DEPENDENCY_NAME is required}"
VERSION="${VERSION:-}"
RELEASE_URL="${RELEASE_URL:-}"

# One line, because $GITHUB_OUTPUT's key=value form takes no newline and a
# release body wants a single sentence under the rule that precedes it.
NOTE=""
if [[ -n "${VERSION}" ]] && [[ -n "${RELEASE_URL}" ]]; then
  NOTE="Requires [${DEPENDENCY_NAME} ${VERSION}](${RELEASE_URL})."
fi

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154
echo "note=${NOTE}" >> "${GITHUB_OUTPUT}"
