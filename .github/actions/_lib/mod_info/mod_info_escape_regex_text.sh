#!/usr/bin/env bash
# Sourced through ../mod_info.sh, not on its own: defines mod_info_escape_regex_text.

# Emits the given text with every regex metacharacter backslash-escaped, so
# the result matches only that text. A folder name is a jar's basename, which
# may carry a dot or a plus as freely as a letter.
mod_info_escape_regex_text() {

    local text="${1}"

    printf '%s\n' "${text}" | sed 's/[][\.^$|?*+(){}]/\\&/g'
}
