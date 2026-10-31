#!/usr/bin/env bash
# Sourced through ../github_output.sh, not on its own: defines draw_output_delimiter.

# Echoes a heredoc delimiter no one could have written by chance.
draw_output_delimiter() {

    local randomHex
    randomHex="$(od -An -N8 -tx1 /dev/urandom)"
    randomHex="${randomHex//[[:space:]]/}"

    printf 'EOF_%s' "${randomHex}"
}
