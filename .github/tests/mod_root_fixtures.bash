#!/usr/bin/env bash
# Shared mod-root fixtures for the suites of scripts that read a mod's
# metadata or its localisation manifest. Sourced - never run - so it carries
# no tests itself and is skipped by the recursive *.bats runner.

# Writes a mod_info.json naming the given id, version and jar into the
# directory, creating it.
#   write_mod_info_file <dir> <id> <version> <jar>
write_mod_info_file() {

    mkdir -p "$1"

    cat > "$1/mod_info.json" <<EOF
{
  "id": "$2",
  "version": "$3",
  "jars": ["$4"]
}
EOF
}

# Writes the given raw JSON as the directory's localisation/manifest.json,
# creating the folder.
#   write_manifest_file <dir> <json>
write_manifest_file() {

    mkdir -p "$1/localisation"
    printf '%s\n' "$2" > "$1/localisation/manifest.json"
}
