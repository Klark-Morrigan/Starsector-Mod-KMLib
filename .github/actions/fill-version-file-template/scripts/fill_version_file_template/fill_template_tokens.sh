#!/usr/bin/env bash
# Sourced by ../fill_version_file_template.sh, not on its own: defines fill_template_tokens.

# Set by the script sourcing this file; defaulted so a failure is still named.
SCRIPT_NAME="${SCRIPT_NAME:-fill_version_file_template}"

# Matches any {{...}} left in the filled file. Deliberately broader than the
# token list: an unknown token is a mistake in the template, and it has to be
# caught here because nothing downstream reads this file until a player's
# update checker does.
TEMPLATE_TOKEN_REGEX='\{\{[^}]*\}\}'

# Echoes the template with every token replaced and the two resolved
# addresses written in. Fails, naming the template, on any token left over.
#   fill_template_tokens <template file> <replacements json> <master version file> <changelog url>
#
# walk() reaches nested values (modVersion's components) without this script
# knowing the template's shape, which is what keeps the shape the mod's to
# state. The visited value is bound to $value first because the "." in a
# piped expression would otherwise refer to $replacements, not to it. The
# changelog address is written only where the template states one.
fill_template_tokens() {

    local templateFile="${1}" replacements="${2}" masterVersionFile="${3}" changelogUrl="${4}"
    local filledText unreplacedTokens

    filledText="$(jq --argjson replacements "${replacements}" \
        --arg masterVersionFile "${masterVersionFile}" \
        --arg changelogUrl "${changelogUrl}" \
        'walk(. as $value
              | if ($value | type) == "string" and ($replacements | has($value))
                then $replacements[$value]
                else $value
                end)
         | .masterVersionFile = $masterVersionFile
         | if $changelogUrl != "" then .changelogURL = $changelogUrl else . end' \
        "${templateFile}")" || return 1

    unreplacedTokens="$(jq -r --arg tokenRegex "${TEMPLATE_TOKEN_REGEX}" \
        '[.. | strings | select(test($tokenRegex))] | unique | join(", ")' \
        <<< "${filledText}")" || return 1

    if [[ -n "${unreplacedTokens}" ]]; then
        echo "${SCRIPT_NAME}: ${templateFile} has unreplaced token(s): ${unreplacedTokens}" >&2
        return 1
    fi

    printf '%s\n' "${filledText}"
}
