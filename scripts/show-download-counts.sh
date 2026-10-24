#!/usr/bin/env bash
# Shows the download counts and badge figures a mod's scheduled badge run would
# publish, publishing nothing.
#
# Usage: scripts/show-download-counts.sh [mod-root]
#
# mod-root defaults to this repository's root, so KMLib checks its own badges.
# A mod's own shim passes its root.
#
# Derives the action's inputs as publish-download-badges.yml does: it runs
# read_mod_info.sh and read_locales.sh in the mod root, so a figure shown here
# is the figure a scheduled run would publish. The repository comes from the
# mod root's origin remote, since outside Actions no run names it.
#
# Hands the inputs to Common-Automation's show-download-counts.sh, which runs
# the action's chain dry. Common-Automation is expected as a sibling checkout
# of this repository.
#
# Needs gh on PATH, logged in with gh auth login.
set -euo pipefail

# The origin URL forms that name a GitHub repository, once a trailing .git is
# cut: https, scp-like ssh, and ssh:// URLs.
GITHUB_REMOTE_REGEX='^(https://github\.com/|git@github\.com:|ssh://git@github\.com/)([^/]+/[^/]+)$'

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
kmlib_root="$(cd "${script_dir}/.." && pwd)"
actions_dir="${kmlib_root}/.github/actions"
common_automation_root="$(cd "${kmlib_root}/../Common-Automation" && pwd)"
mod_root="$(cd "${1:-${kmlib_root}}" && pwd)"

outputs_file="$(mktemp)"
trap 'rm -f "${outputs_file}"' EXIT

# Runs one of the workflow's read scripts in the mod root, as the workflow's
# checkout does, writing its outputs where read_output_value finds them.
#   run_read_script <script path>
run_read_script() {

    (cd "${mod_root}" && GITHUB_OUTPUT="${outputs_file}" "$1")
}

# Prints the value a read script wrote for the given output key.
#   read_output_value <key>
read_output_value() {

    local key value

    while IFS='=' read -r key value; do

        if [[ "${key}" == "$1" ]]; then

            printf '%s' "${value}"

            return
        fi
    done < "${outputs_file}"
}

# Prints the owner/name of the GitHub repository the mod root's origin names.
read_origin_repository() {

    local origin_url

    if ! origin_url="$(git -C "${mod_root}" remote get-url origin)"; then

        echo "show-download-counts: ${mod_root} has no origin remote to read the repository from." >&2
        exit 1
    fi

    if ! [[ "${origin_url%.git}" =~ ${GITHUB_REMOTE_REGEX} ]]; then

        echo "show-download-counts: origin '${origin_url}' of ${mod_root} is not a GitHub repository." >&2
        exit 1
    fi

    printf '%s' "${BASH_REMATCH[2]}"
}

echo "Deriving the inputs from ${mod_root}, as publish-download-badges.yml does." >&2

run_read_script "${actions_dir}/read-mod-info/scripts/read_mod_info.sh"
run_read_script "${actions_dir}/read-locales/scripts/read_locales.sh"

repository="$(read_origin_repository)"
asset_name_regex="$(read_output_value "zip-name-regex")"
default_locale="$(read_output_value "default-locale")"
label="$(read_output_value "mod-folder-name")"

echo "repository: ${repository}" >&2
echo "asset-name-regex: ${asset_name_regex}" >&2
echo "default-locale:${default_locale:+ ${default_locale}}" >&2
echo "label: ${label}" >&2

"${common_automation_root}/scripts/show-download-counts.sh" \
    --repository "${repository}" \
    --asset-name-regex "${asset_name_regex}" \
    --default-locale "${default_locale}" \
    --label "${label}"
