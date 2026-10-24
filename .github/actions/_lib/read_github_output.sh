#!/usr/bin/env bash
# Reads one step output back out of an output file, for a script that runs the
# read actions' scripts outside Actions and takes their outputs as the next
# step would. Sourced, not executed: defines read_github_output. Failures
# report through the sourcing script's own SCRIPT_NAME.
#
# Reads both forms github_output.sh writes: "key=value", and "key<<delimiter",
# the value's lines, then the delimiter alone. A line inside another key's
# multi-line value is never read as a key of its own.

SCRIPT_NAME="${SCRIPT_NAME:-read_github_output}"

# A "key<<delimiter" line opening a multi-line value. The key holds no "=", so
# a single-line value that happens to hold "<<" is not read as one.
GITHUB_OUTPUT_MULTILINE_OPENER_REGEX='^([^=<]+)<<(.+)$'

# Prints the value written for the key, the last one when the key was written
# more than once, as the runner reads it. Fails, naming the key and the file,
# when the key was never written.
#   read_github_output <output file> <key>
read_github_output() {

  local outputFile="${1}" outputKey="${2}"
  local line value="" isFound=false
  local openKey="" delimiter=""
  local -a valueLines=()

  while IFS= read -r line || [[ -n "${line}" ]]; do

    if [[ -n "${delimiter}" ]]; then

      if [[ "${line}" != "${delimiter}" ]]; then
        valueLines+=("${line}")
        continue
      fi

      if [[ "${openKey}" == "${outputKey}" ]]; then
        printf -v value '%s\n' "${valueLines[@]}"
        value="${value%$'\n'}"
        isFound=true
      fi

      delimiter=""
    elif [[ "${line}" =~ ${GITHUB_OUTPUT_MULTILINE_OPENER_REGEX} ]]; then

      openKey="${BASH_REMATCH[1]}"
      delimiter="${BASH_REMATCH[2]}"
      valueLines=()
    elif [[ "${line}" == "${outputKey}="* ]]; then

      value="${line#"${outputKey}="}"
      isFound=true
    fi
  done < "${outputFile}"

  if [[ "${isFound}" != true ]]; then
    echo "${SCRIPT_NAME}: output '${outputKey}' was never written to ${outputFile}" >&2
    exit 1
  fi

  printf '%s' "${value}"
}
