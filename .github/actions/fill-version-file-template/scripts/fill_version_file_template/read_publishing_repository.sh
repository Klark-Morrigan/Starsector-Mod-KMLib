#!/usr/bin/env bash
# Sourced by ../fill_version_file_template.sh, not on its own: defines read_publishing_repository.

# Echoes the <owner>/<repo> the release is published in, and logs where it
# came from.
#
# The Actions runtime exports it, so a release states nothing. A local build
# has no such variable, and the checkout's own origin remote is the honest
# answer: a URL built from anything else would point at a repository this
# clone does not push to. Called inside the mod root, so the remote consulted
# is the mod's own rather than that of whatever checkout the caller stood in.
read_publishing_repository() {

    local repository

    if [[ -n "${GITHUB_REPOSITORY:-}" ]]; then

        echo "Using repository ${GITHUB_REPOSITORY}, from GITHUB_REPOSITORY." >&2
        printf '%s\n' "${GITHUB_REPOSITORY}"

        return
    fi

    repository="$(read_origin_repository "${PWD}")" || return 1

    echo "Using repository ${repository}, from the origin remote." >&2
    printf '%s\n' "${repository}"
}
