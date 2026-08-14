#!/usr/bin/env bash
# What mod_info.json contains and what shape its fields take, in one place.
#
# Four composite-action scripts read that file, and before this they each
# carried their own copy of the filename, the "field is present" check, and
# the version shape. A field added to mod_info.json, or a decision about what
# counts as a well-formed version, would have had to land in four scripts to
# take effect.
#
# Lives under .github/actions/ rather than beside Common-Automation's
# .github/lib/ helpers because mod-release.yml sparse-checkouts KMLib with
# `sparse-checkout: .github/actions`. A lib outside that path would be absent
# at run time, and worse, only for releases running an older pinned workflow -
# the action scripts track master while the workflow contract is pinned by the
# caller's tag. Sitting inside the already-checked-out path removes the skew
# entirely. The leading underscore marks it as not-an-action, matching the
# _ci-gradle.yml convention.
#
# Sourced, not executed: defines mod_info_require_file and
# mod_info_require_fields. Both report through the sourcing script's own
# SCRIPT_NAME, so a message still names the step a reader saw fail.

# Read by the scripts that source this file, which shellcheck cannot see when
# it checks this one on its own.
# shellcheck disable=SC2034

# Prefixes every message below with the sourcing script's name, so a reader
# sees which step failed. Defaulted rather than required: a caller that
# forgets to set it gets a slightly vaguer message, not an unbound-variable
# crash under `set -u` at the moment it is trying to report a real failure.
SCRIPT_NAME="${SCRIPT_NAME:-mod_info}"

MOD_INFO_FILE="mod_info.json"

# Plain SemVer, digits only. The digits-only part is not decoration: the
# game's own parser splits a version on the letter "a" as well as ".", and
# TriOS strips letters out of a mod_info.json version entirely, so a
# suffixed version like 1.2.3a is silently mangled by both rather than
# rejected by either.
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+$'

# Fails the run unless mod_info.json is in the working directory, which is
# the caller's checkout root when a composite action invokes these scripts.
mod_info_require_file() {
  if [[ ! -f "${MOD_INFO_FILE}" ]]; then
    echo "${SCRIPT_NAME}: ${MOD_INFO_FILE} not found in ${PWD}" >&2
    exit 1
  fi
}

# Fails the run on the first absent field, given "<label>:<value>" pairs.
#
# Takes values already read rather than reading them itself: the callers want
# the values anyway, and jq is cheaper called once per field than twice. The
# label is what the caller sees, so it is written as it appears in the file
# ("jars[0]", not "jarSource"). Splitting on the first colon leaves values
# holding colons - URLs, mostly - intact.
mod_info_require_fields() {
  local pair field value
  for pair in "$@"; do
    field="${pair%%:*}"
    value="${pair#*:}"
    if [[ -z "${value}" ]] || [[ "${value}" == "null" ]]; then
      echo "${SCRIPT_NAME}: ${MOD_INFO_FILE} is missing required field '${field}'" >&2
      exit 1
    fi
  done
}
