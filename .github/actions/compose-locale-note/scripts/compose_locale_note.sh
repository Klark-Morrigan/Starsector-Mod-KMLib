#!/usr/bin/env bash
# Composes the markdown line a release body carries naming the zip for each
# locale the mod releases in, and what a locale needs installed besides.
#
# Reads from the environment (set by action.yml):
#   LOCALES     the read-locales action's locales output; empty or [] means no
#               line is due
#   JAR_SOURCE  mod_info jars[0], which the zip names are derived from
#   VERSION     the version being released
#
# Emits to $GITHUB_OUTPUT:
#   note  the composed line, or empty
#
# A locale naming a core localisation gets a link to it: that project is what
# supplies the glyphs its text is drawn in, it is installed over the game
# rather than as a mod, and so nothing but this line and the README can tell a
# player they need it.
set -euo pipefail

SCRIPT_NAME="compose_locale_note"

# Supplies the zip-name rule, so the name a reader is told to download is the
# name the pipeline uploads.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/_lib"
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${LIB_DIR}/mod_info.sh"

LOCALES="${LOCALES:-[]}"
JAR_SOURCE="${JAR_SOURCE:?${SCRIPT_NAME}: JAR_SOURCE is required}"
VERSION="${VERSION:?${SCRIPT_NAME}: VERSION is required}"

if ! jq -e 'type == "array"' <<< "${LOCALES}" > /dev/null 2>&1; then
  echo "${SCRIPT_NAME}: LOCALES is not a JSON array: ${LOCALES}" >&2
  exit 1
fi

# One line, because $GITHUB_OUTPUT's key=value form takes no newline and the
# body joins this under the same rule as the dependency line.
NOTE=""
LOCALE_COUNT=$(jq 'length' <<< "${LOCALES}")

if (( LOCALE_COUNT > 0 )); then

  LOCALE_ENTRIES=()
  for (( index = 0; index < LOCALE_COUNT; index++ )); do
    localeTag=$(jq -r --argjson index "${index}" '.[$index].tag' <<< "${LOCALES}")
    displayName=$(jq -r --argjson index "${index}" '.[$index].displayName' <<< "${LOCALES}")
    coreLocalisation=$(jq -r --argjson index "${index}" '.[$index].coreLocalisation // ""' <<< "${LOCALES}")
    zipName=$(mod_info_derive_zip_name "${JAR_SOURCE}" "${VERSION}" "${localeTag}")

    localeEntry="${displayName} in \`${zipName}\`"
    if [[ -n "${coreLocalisation}" ]]; then
      localeEntry="${localeEntry}, which needs [its core localisation](${coreLocalisation})"
      localeEntry="${localeEntry} installed over \`starsector-core\`"
    fi
    LOCALE_ENTRIES+=("${localeEntry}")
  done

  # Joined by semicolons, since an entry naming a core localisation holds a
  # comma of its own.
  JOINED_ENTRIES=$(printf '%s; ' "${LOCALE_ENTRIES[@]}")
  NOTE="Builds by language: ${JOINED_ENTRIES%; }."
fi

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154
echo "note=${NOTE}" >> "${GITHUB_OUTPUT}"
