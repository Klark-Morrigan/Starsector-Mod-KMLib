#!/usr/bin/env bash
# Sourced by ../fill_version_file_template.sh, not on its own: defines resolve_changelog_url.

# Echoes the changelog address the filled file states: the template's, or for
# a translated locale the translation beside it. Empty when the template
# states none, so the template keeps owning its shape.
#   resolve_changelog_url <template file> <locale>
#
# A translated locale's zip carries that locale's changelog in place of the
# root one, so its address swaps the last segment for the translation's path.
# The default locale, a mod keeping no root changelog and a template stating
# no address keep the template's - the same cases in which the zip keeps the
# root changelog. Called inside the mod root, as the bundle paths are relative
# to it.
resolve_changelog_url() {

    local templateFile="${1}" locale="${2}"
    local changelogUrl defaultLocale translatedChangelogFile

    changelogUrl="$(jq -r '.changelogURL // ""' "${templateFile}")" || return 1

    if [[ -z "${locale}" || -z "${changelogUrl}" || ! -f "${LOCALISATION_CHANGELOG_FILE}" \
            || ! -f "${LOCALISATION_MANIFEST_FILE}" ]]; then

        printf '%s\n' "${changelogUrl}"
        return
    fi

    defaultLocale="$(jq -r '.defaultLocale // ""' "${LOCALISATION_MANIFEST_FILE}")" || return 1

    if [[ "${locale}" == "${defaultLocale}" ]]; then
        printf '%s\n' "${changelogUrl}"
        return
    fi

    # Required here as the packaging requires it: a translated locale without
    # its changelog was never tested, and its zip fails to package for the same
    # reason.
    translatedChangelogFile="$(localisation_require_translated_changelog "${locale}")" || return 1

    # An address ending in another name is left as written rather than failed:
    # it still leads to the root changelog, which is degraded rather than broken.
    if [[ "${changelogUrl##*/}" != "${LOCALISATION_CHANGELOG_FILE}" ]]; then

        echo "${SCRIPT_NAME}: ${templateFile} changelogURL '${changelogUrl}' does not end in" \
             "${LOCALISATION_CHANGELOG_FILE}, so locale ${locale} keeps it rather than naming" \
             "${translatedChangelogFile}" >&2

        printf '%s\n' "${changelogUrl}"
        return
    fi

    printf '%s\n' "${changelogUrl%/*}/${translatedChangelogFile}"
}
