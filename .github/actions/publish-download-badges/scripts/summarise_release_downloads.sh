#!/usr/bin/env bash
# Prints how many times each release's mod zips were downloaded, one release
# per line, newest first.
#
# Shields' GitHub downloads badge sums every asset of a release, and the
# .version files an update checker polls on every game start outnumber the
# zips many times over. Counting only the zips needs the asset list, so this
# is the one place that talks to the API; every badge renderer reads its
# output rather than asking again.
#
# Usage: summarise_release_downloads.sh [mod-root]
#
# Reads from the environment:
#   GITHUB_REPOSITORY  owner/name of the repository whose releases are summed
#   GH_TOKEN           token gh authenticates with (consumed by gh, not read here)
#
# Reads from the mod root, which defaults to the working directory:
#   mod_info.base.json, else mod_info.json  - .jars[0], which names the zips
#
# Prints to stdout, tab-separated, one line per release:
#   <tag>  <published_at>  <download count>
#
# A zip counts when its name is the one the release pipeline uploads -
# <mod-folder-name>-<version>.zip, or that name with a locale suffix - so the
# folder name and the version shape come from the shared lib that names the
# upload. The locale is matched as any suffix rather than a list of tags, so a
# locale added later counts from its first release. Drafts are skipped: the
# run's token can see them, and a draft has no downloads to report.
#
# Exits non-zero, printing nothing to stdout, when the lookup fails or no
# release carries a matching zip. A caller publishing from this output then
# keeps its last good figures rather than publishing zeroes.
set -euo pipefail

SCRIPT_NAME="summarise_release_downloads"

# Resolved from this script's own location, not from $PWD: these scripts run
# against the caller's checkout, which is never where they live.
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/_lib"

# shellcheck source-path=SCRIPTDIR
# shellcheck source=../../_lib/mod_info.sh
source "${LIB_DIR}/mod_info.sh"

# The API's page ceiling. --paginate fetches every page either way, so this
# only cuts the number of calls a long release history costs.
RELEASES_PER_PAGE=100
REPOSITORY="${GITHUB_REPOSITORY:?${SCRIPT_NAME}: GITHUB_REPOSITORY is required}"

# Directory holding mod_info.json. Optional because a caller standing in the
# mod root has nothing to say; the release pipeline checks the mod out one
# level down, and Actions permits no working-directory on a `uses:` step.
MOD_ROOT="${1:-.}"

mod_info_enter_mod_root "${MOD_ROOT}"
mod_info_locate_file

JAR_SOURCE=$(jq -r '.jars[0]' "${MOD_INFO_FILE}")

mod_info_require_fields "jars[0]:${JAR_SOURCE}"

MOD_FOLDER_NAME=$(mod_info_derive_mod_folder_name "${JAR_SOURCE}")

# The zip name splits into a literal prefix and a pattern for the rest. The
# folder name is compared as a plain string, so a name holding regex
# metacharacters needs no escaping. The rest is the shared version pattern
# with its anchors moved outward, then an optional locale, then the extension.
ZIP_NAME_PREFIX="${MOD_FOLDER_NAME}-"
VERSION_PATTERN="${SEMVER_REGEX#^}"
VERSION_PATTERN="${VERSION_PATTERN%\$}"
ZIP_NAME_REST_REGEX="^${VERSION_PATTERN}(${LOCALE_SEPARATOR}.+)?${ZIP_EXTENSION//./\\.}\$"

# Without --jq, gh prints each page as its own JSON array, one after another.
# The filter below gathers every page through `inputs` before sorting; run
# once per page instead, it would sort each page on its own. A failure is gh's
# own message on stderr; the line here says which lookup it belongs to.
if ! RELEASE_PAGES=$(gh api --paginate "repos/${REPOSITORY}/releases?per_page=${RELEASES_PER_PAGE}"); then
  echo "${SCRIPT_NAME}: could not list the releases of ${REPOSITORY}" >&2
  exit 1
fi

# Sorted here rather than trusted to the API's order, which GitHub documents
# for no listing field. published_at is ISO 8601 in UTC, so it sorts as text.
SUMMARY=$(jq -nr \
  --arg prefix "${ZIP_NAME_PREFIX}" \
  --arg restRegex "${ZIP_NAME_REST_REGEX}" \
  '[ inputs
     | .[]
     | select(.draft | not)
     | { tag: .tag_name,
         publishedAt: .published_at,
         zips: [ .assets[]
                 | select(.name | startswith($prefix)
                                  and (.[($prefix | length):] | test($restRegex))) ] }
     | select(.zips | length > 0) ]
   | sort_by(.publishedAt) | reverse
   | .[]
   | [.tag, .publishedAt, (.zips | map(.download_count) | add)]
   | @tsv' \
  <<< "${RELEASE_PAGES}")

if [[ -z "${SUMMARY}" ]]; then
  echo "${SCRIPT_NAME}: no release of ${REPOSITORY} carries a zip named ${ZIP_NAME_PREFIX}<version>${ZIP_EXTENSION}" >&2
  exit 1
fi

printf '%s\n' "${SUMMARY}"
