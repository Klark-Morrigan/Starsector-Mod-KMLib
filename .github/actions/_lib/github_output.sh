#!/usr/bin/env bash
# Writes a script's step outputs, in one place: every key written is also
# logged, so a run's log shows each value one step handed the next.
#
# Sourced, not executed: defines write_github_output. Failures report through
# the sourcing script's own SCRIPT_NAME.

SCRIPT_NAME="${SCRIPT_NAME:-github_output}"

# Appends "key=value" to $GITHUB_OUTPUT and logs it as "key: value", or as
# "key:" alone for an empty value, which a trailing space would leave unseen.
#   write_github_output <key> <value>
#
# The log goes to stderr, where the runner shows it, and the output file stays
# as the runner reads it. A value spanning lines fails the run, naming the key:
# in this form its second line would be read as a key of its own.
write_github_output() {

  local outputKey="${1}" outputValue="${2}"

  if [[ "${outputValue}" == *$'\n'* ]]; then
    echo "${SCRIPT_NAME}: output '${outputKey}' spans more than one line" >&2
    exit 1
  fi

  # GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
  # shellcheck disable=SC2154
  echo "${outputKey}=${outputValue}" >> "${GITHUB_OUTPUT}"
  echo "${outputKey}:${outputValue:+ ${outputValue}}" >&2
}
