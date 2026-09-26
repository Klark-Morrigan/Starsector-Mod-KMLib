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
# shellcheck source=../../_lib/json.sh
source "${LIB_DIR}/json.sh"

# Separates a locale's fields on one line. A control character no display name
# or URL carries, so a field is read back exactly, where a tab-separated row
# would come back with its backslashes escaped.
FIELD_SEPARATOR=$'\x1f'

LOCALES="${LOCALES:-[]}"
JAR_SOURCE="${JAR_SOURCE:?${SCRIPT_NAME}: JAR_SOURCE is required}"
VERSION="${VERSION:?${SCRIPT_NAME}: VERSION is required}"

json_require_array "${LOCALES}" "LOCALES"
# Declared here, being filled through json_read_lines' name reference.
LOCALE_ROWS=()
# $separator is jq's own, bound by --arg, not the shell's.
# shellcheck disable=SC2016
json_read_lines LOCALE_ROWS -r --arg separator "${FIELD_SEPARATOR}" \
  '.[] | [.tag, .displayName, (.coreLocalisation // "")] | join($separator)' <<< "${LOCALES}"

# One line, because $GITHUB_OUTPUT's key=value form takes no newline and the
# body joins this under the same rule as the dependency line.
NOTE=""

if (( ${#LOCALE_ROWS[@]} > 0 )); then

  LOCALE_ENTRIES=()
  for localeRow in "${LOCALE_ROWS[@]}"; do
    IFS="${FIELD_SEPARATOR}" read -r localeTag displayName coreLocalisation <<< "${localeRow}"
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
