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
# of this repository; its section headings and checkout description are used
# here too, so the whole dry run reads as one log.
#
# Everything runs from working trees, which may sit on another branch or hold
# edits a scheduled run, reading the default branch and KMLib at @master,
# would not see. So each checkout's state is logged first.
#
# Needs gh on PATH, logged in with gh auth login.
set -euo pipefail

# shellcheck disable=SC2034 # the sourced _lib functions name it in their failures
SCRIPT_NAME="show-download-counts"

# The workflow's read scripts, in the order it runs them, under .github/actions.
READ_SCRIPTS=(
    read-mod-info/scripts/read_mod_info.sh
    read-locales/scripts/read_locales.sh
)

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
kmlib_root="$(cd "${script_dir}/.." && pwd)"
actions_dir="${kmlib_root}/.github/actions"
common_automation_root="$(cd "${kmlib_root}/../Common-Automation" && pwd)"
mod_root="$(cd "${1:-${kmlib_root}}" && pwd)"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=../.github/actions/_lib/log_output_value.sh
source "${actions_dir}/_lib/log_output_value.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=../.github/actions/_lib/read_github_output.sh
source "${actions_dir}/_lib/read_github_output.sh"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=../.github/actions/_lib/read_origin_repository.sh
source "${actions_dir}/_lib/read_origin_repository.sh"

# shellcheck source=/dev/null # a sibling checkout's file, absent in CI's
source "${common_automation_root}/.github/lib/describe-git-checkout.sh"

# shellcheck source=/dev/null # a sibling checkout's file, absent in CI's
source "${common_automation_root}/.github/lib/print-section-heading.sh"

outputs_file="$(mktemp)"
trap 'rm -f "${outputs_file}"' EXIT

print_section_heading "Checkouts" >&2
describe_git_checkout "${mod_root}" >&2

if [[ "${kmlib_root}" != "${mod_root}" ]]; then
    describe_git_checkout "${kmlib_root}" >&2
fi

# Each runs in the mod root, as the workflow's checkout does, and logs every
# output it writes.
for read_script in "${READ_SCRIPTS[@]}"; do

    read_script_file_name="${read_script##*/}"

    print_section_heading "${read_script_file_name%.sh}" >&2
    (cd "${mod_root}" && GITHUB_OUTPUT="${outputs_file}" "${actions_dir}/${read_script}")
done

repository="$(read_origin_repository "${mod_root}")"
asset_name_regex="$(read_github_output "${outputs_file}" "zip-name-regex")"
default_locale="$(read_github_output "${outputs_file}" "default-locale")"
label="$(read_github_output "${outputs_file}" "mod-folder-name")"

print_section_heading "Inputs" >&2
log_output_value "repository" "${repository}"
log_output_value "asset-name-regex" "${asset_name_regex}"
log_output_value "default-locale" "${default_locale}"
log_output_value "label" "${label}"

"${common_automation_root}/scripts/show-download-counts.sh" \
    --repository "${repository}" \
    --asset-name-regex "${asset_name_regex}" \
    --default-locale "${default_locale}" \
    --label "${label}"
