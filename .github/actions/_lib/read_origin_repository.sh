#!/usr/bin/env bash
# Reads which GitHub repository a checkout's origin remote names, for a script
# running outside Actions, where no run names it. Sourced, not executed:
# defines read_origin_repository. Failures report through the sourcing
# script's own SCRIPT_NAME.

SCRIPT_NAME="${SCRIPT_NAME:-read_origin_repository}"

# The origin URL forms that name a GitHub repository, once a trailing .git is
# cut: https, scp-like ssh, and ssh:// URLs.
GITHUB_REMOTE_REGEX='^(https://github\.com/|git@github\.com:|ssh://git@github\.com/)([^/]+/[^/]+)$'

# Prints the owner/name of the repository the checkout's origin names. Fails,
# naming the checkout, when it has no origin or its origin is not on GitHub.
#   read_origin_repository <checkout dir>
read_origin_repository() {

  local checkoutDir="${1}" originUrl

  if ! originUrl="$(git -C "${checkoutDir}" remote get-url origin)"; then
    echo "${SCRIPT_NAME}: ${checkoutDir} has no origin remote to read the repository from." >&2
    exit 1
  fi

  if ! [[ "${originUrl%.git}" =~ ${GITHUB_REMOTE_REGEX} ]]; then
    echo "${SCRIPT_NAME}: origin '${originUrl}' of ${checkoutDir} is not a GitHub repository." >&2
    exit 1
  fi

  printf '%s' "${BASH_REMATCH[2]}"
}
