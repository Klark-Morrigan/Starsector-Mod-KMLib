#!/usr/bin/env bash
# Writes a script's step outputs, in one place: every key written is also
# logged, so a run's log shows each value one step handed the next.
#
# Sourced, not executed: loads write_github_output and write_multiline_output,
# one function per file under github_output/, so a script sources one file.
# Failures report through the sourcing script's own SCRIPT_NAME.
#
# Every value is logged as written. A secret has to be masked with
# ::add-mask:: before it is written, since Actions masks only the secrets it
# was handed.
#
# The log goes to stderr, where the runner shows it, and the output file stays
# as the runner reads it.
#
# Each shellcheck source path is written from the repository root, for the
# reason mod_info.sh gives.

SCRIPT_NAME="${SCRIPT_NAME:-github_output}"

# shellcheck source=.github/actions/_lib/log_output_value.sh
source "${BASH_SOURCE[0]%/*}/log_output_value.sh"

# shellcheck source=.github/actions/_lib/github_output/draw_output_delimiter.sh
source "${BASH_SOURCE[0]%/*}/github_output/draw_output_delimiter.sh"

# shellcheck source=.github/actions/_lib/github_output/write_github_output.sh
source "${BASH_SOURCE[0]%/*}/github_output/write_github_output.sh"

# shellcheck source=.github/actions/_lib/github_output/write_multiline_output.sh
source "${BASH_SOURCE[0]%/*}/github_output/write_multiline_output.sh"
