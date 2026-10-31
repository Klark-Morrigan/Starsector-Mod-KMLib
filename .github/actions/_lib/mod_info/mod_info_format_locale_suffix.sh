#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_format_locale_suffix.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info_constants.sh"

# Emits the suffix a locale's release files carry, or nothing for no tag -
# which is what a mod keeping no locales releases under.
mod_info_format_locale_suffix() {

    local localeTag="${1:-}"

    if [[ -n "${localeTag}" ]]; then
        printf '%s' "${LOCALE_SEPARATOR}${localeTag}"
    fi
}
