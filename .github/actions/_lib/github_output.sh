#!/usr/bin/env bash
# Writes a script's step outputs, in one place: every key written is also
# logged, so a run's log shows each value one step handed the next.
#
# Sourced, not executed: defines write_github_output and
# write_multiline_output. Failures report through the sourcing script's own
# SCRIPT_NAME.
#
# Every value is logged as written. A secret has to be masked with
# ::add-mask:: before it is written, since Actions masks only the secrets it
# was handed.
#
# The log goes to stderr, where the runner shows it, and the output file stays
# as the runner reads it.

# GITHUB_OUTPUT is exported by the Actions runtime, not assigned here.
# shellcheck disable=SC2154

SCRIPT_NAME="${SCRIPT_NAME:-github_output}"

# Appends "key=value" to $GITHUB_OUTPUT and logs it as "key: value", or as
# "key:" alone for an empty value, which a trailing space would leave unseen.
#   write_github_output <key> <value>
#
# A value spanning lines fails the run, naming the key: in this form its
# second line would be read as a key of its own. write_multiline_output takes
# such a value.
write_github_output() {

  local outputKey="${1}" outputValue="${2}"

  if [[ "${outputValue}" == *$'\n'* ]]; then
    echo "${SCRIPT_NAME}: output '${outputKey}' spans more than one line; write it with write_multiline_output" >&2
    exit 1
  fi

  echo "${outputKey}=${outputValue}" >> "${GITHUB_OUTPUT}"
  echo "${outputKey}:${outputValue:+ ${outputValue}}" >&2
}

# Appends one output in the multi-line form - "key<<delimiter", the value,
# then the delimiter alone - and logs it as "key:" with the value's lines
# indented beneath, a blank line left blank.
#   write_multiline_output <key> <value>
#
# The delimiter is random and drawn again while the value holds it, so no line
# of the value can end it early.
write_multiline_output() {

  local outputKey="${1}" outputValue="${2}" delimiter valueLine
  delimiter="$(draw_output_delimiter)"

  while [[ "${outputValue}" == *"${delimiter}"* ]]; do
    delimiter="$(draw_output_delimiter)"
  done

  {
    echo "${outputKey}<<${delimiter}"
    printf '%s\n' "${outputValue}"
    echo "${delimiter}"
  } >> "${GITHUB_OUTPUT}"

  echo "${outputKey}:" >&2

  if [[ -n "${outputValue}" ]]; then
    while IFS= read -r valueLine; do
      echo "${valueLine:+  ${valueLine}}"
    done <<< "${outputValue}" >&2
  fi
}

# Echoes a heredoc delimiter no one could have written by chance.
draw_output_delimiter() {

  local randomHex
  randomHex="$(od -An -N8 -tx1 /dev/urandom)"
  randomHex="${randomHex//[[:space:]]/}"

  printf 'EOF_%s' "${randomHex}"
}
