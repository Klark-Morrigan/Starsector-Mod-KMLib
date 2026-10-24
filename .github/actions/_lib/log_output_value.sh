#!/usr/bin/env bash
# Logs one value a script hands on, in the one form every such log line takes,
# so a run's log reads alike whichever script wrote it. Sourced, not executed:
# defines log_output_value.

# Logs "key: value" to stderr, or "key:" alone for an empty value, which a
# trailing space would leave unseen.
#   log_output_value <key> <value>
log_output_value() {

  echo "${1}:${2:+ ${2}}" >&2
}
