#!/usr/bin/env bash
# Composes the two parts a release body carries for a mod releasing in more
# than one language: the list naming the zip for each locale and what a locale
# needs installed besides, and each translated locale's notes for the version.
#
# Reads from the environment (set by action.yml):
#   LOCALES         the read-locales action's locales output; empty or [] means
#                   nothing is due
#   DEFAULT_LOCALE  the manifest's default locale, whose notes are the body's
#                   own and are not repeated
#   JAR_SOURCE      mod_info jars[0], which the zip names are derived from
#   VERSION         the version being released
#   CHANGELOG_LIB   Common-Automation's changelog.sh, which reads a version's
#                   section; empty composes no translated notes
#
# Emits to $GITHUB_OUTPUT, each in the multi-line form:
#   note          the "Builds by language" heading and list, or empty
#   translations  per translated locale, its CHANGELOG.md section for the
#                 version, collapsed under the locale's display name; or empty
#
# A locale that needs a core localisation gets a link to it. That project
# supplies the glyphs the locale's text is drawn in. It is installed over the
# game, not as a mod, so the launcher cannot tell a player it is missing.
#
# A translated locale's notes come from its translated changelog. A missing
# section for the version fails, as it does for the root changelog.
set -euo pipefail

SCRIPT_NAME="compose_locale_note"

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/_lib"

# The zip-name rule, so the name a reader is told to download is the name the
# pipeline uploads.
# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${LIB_DIR}/mod_info.sh"

# shellcheck source=../../_lib/json.sh
source "${LIB_DIR}/json.sh"

# shellcheck source=../../_lib/localisation.sh
source "${LIB_DIR}/localisation.sh"

# Separates a locale's fields on one line. A control character no display name
# or URL carries, so a field is read back exactly, where a tab-separated row
# would come back with its backslashes escaped.
FIELD_SEPARATOR=$'\x1f'

LOCALES="${LOCALES:-[]}"
DEFAULT_LOCALE="${DEFAULT_LOCALE:-}"

JAR_SOURCE="${JAR_SOURCE:?${SCRIPT_NAME}: JAR_SOURCE is required}"
VERSION="${VERSION:?${SCRIPT_NAME}: VERSION is required}"

CHANGELOG_LIB="${CHANGELOG_LIB:-}"

draw_output_delimiter() {
  local randomHex
  randomHex="$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')"
  printf 'EOF_%s' "${randomHex}"
}

# Writes one output in the multi-line form: KEY<<DELIMITER, the value, then the
# delimiter alone. The delimiter is random, so no changelog line can end the
# value early.
write_multiline_output() {

  local outputKey="${1}" outputValue="${2}" delimiter
  delimiter="$(draw_output_delimiter)"

  while [[ "${outputValue}" == *"${delimiter}"* ]]; do
    delimiter="$(draw_output_delimiter)"
  done

  # GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
  # shellcheck disable=SC2154
  {
    echo "${outputKey}<<${delimiter}"
    printf '%s\n' "${outputValue}"
    echo "${delimiter}"
  } >> "${GITHUB_OUTPUT}"
}

# Echoes one translated locale's notes for the version, collapsed under its
# display name. The blank lines inside the block are what make GitHub render
# the section as Markdown rather than print it.
compose_translated_notes() {

  local localeTag="${1}" displayName="${2}"
  local changelogFile section

  # This function runs inside a command substitution, which does not inherit
  # set -e, so a failure returns explicitly.
  changelogFile="$(localisation_require_translated_changelog "${localeTag}")" || return 1
  section="$(changelog_section "${changelogFile}" "${VERSION}")"

  if [[ -z "${section//[[:space:]]/}" ]]; then
    echo "${SCRIPT_NAME}: ${changelogFile} has no '## [${VERSION}]' section" >&2
    return 1
  fi

  printf '<details>\n<summary>%s</summary>\n\n%s\n\n</details>' "${displayName}" "${section}"
}

json_require_array "${LOCALES}" "LOCALES"

# Declared here, being filled through json_read_lines' name reference.
LOCALE_ROWS=()

# $separator is jq's own, bound by --arg, not the shell's.
# shellcheck disable=SC2016
json_read_lines LOCALE_ROWS -r --arg separator "${FIELD_SEPARATOR}" \
  '.[] | [.tag, .displayName, (.coreLocalisation // "")] | join($separator)' <<< "${LOCALES}"

if (( ${#LOCALE_ROWS[@]} > 0 )) && [[ -n "${CHANGELOG_LIB}" ]]; then

  if [[ -z "${DEFAULT_LOCALE}" ]]; then
    echo "${SCRIPT_NAME}: DEFAULT_LOCALE is required to tell the translated locales apart" >&2
    exit 1
  fi

  if [[ ! -f "${CHANGELOG_LIB}" ]]; then
    echo "${SCRIPT_NAME}: changelog helpers not found at ${CHANGELOG_LIB}" >&2
    exit 1
  fi

  # shellcheck source=/dev/null
  source "${CHANGELOG_LIB}"

fi

NOTE=""
TRANSLATIONS=""

if (( ${#LOCALE_ROWS[@]} > 0 )); then

  LOCALE_ENTRIES=()
  TRANSLATED_NOTES=()

  for localeRow in "${LOCALE_ROWS[@]}"; do

    IFS="${FIELD_SEPARATOR}" read -r localeTag displayName coreLocalisation <<< "${localeRow}"
    zipName=$(mod_info_derive_zip_name "${JAR_SOURCE}" "${VERSION}" "${localeTag}")
    localeEntry="- ${displayName}: \`${zipName}\`"

    if [[ -n "${coreLocalisation}" ]]; then
      localeEntry="${localeEntry}, which needs [its core localisation](${coreLocalisation})"
      localeEntry="${localeEntry} installed over \`starsector-core\`"
    fi

    LOCALE_ENTRIES+=("${localeEntry}.")

    if [[ -n "${CHANGELOG_LIB}" && "${localeTag}" != "${DEFAULT_LOCALE}" ]]; then

      # A plain assignment, so set -e stops the script when the notes are
      # missing instead of appending an empty entry.
      translatedNotes="$(compose_translated_notes "${localeTag}" "${displayName}")"
      TRANSLATED_NOTES+=("${translatedNotes}")

    fi
  done

  NOTE="### Builds by language"$'\n\n'"$(printf '%s\n' "${LOCALE_ENTRIES[@]}")"

  # Each block apart from the next by a blank line, so one's closing tag and
  # the next one's opening tag never share a paragraph.
  if (( ${#TRANSLATED_NOTES[@]} > 0 )); then
    TRANSLATIONS="$(printf '%s\n\n' "${TRANSLATED_NOTES[@]}")"
  fi
fi

write_multiline_output "note" "${NOTE}"
write_multiline_output "translations" "${TRANSLATIONS}"
