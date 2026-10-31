#!/usr/bin/env bash
# Which file holds a mod's metadata, what shape its fields take, and the
# release file names that follow from it, in one place - so a field added, a
# decision about what counts as a well-formed version, or a change to how a
# release names its files lands once rather than in every script reading it.
#
# Lives under .github/actions/ rather than beside Common-Automation's
# .github/lib/ helpers so it sits with the scripts that source it, each of
# which reaches it by a path relative to its own location. The leading
# underscore marks it as not-an-action, matching the _ci-gradle.yml
# convention.
#
# Sourced, not executed: loads the shared names and the mod_info_* functions,
# one per file under mod_info/, so a script sources one file. The checks
# report through the sourcing script's own SCRIPT_NAME, so a message still
# names the step a reader saw fail.
#
# Each source path given to the linter is written from the repository root,
# where the linter runs. A relative one would be read against the directory of
# the script being checked, not this one, and the names these files set would
# go unseen.

# shellcheck source=.github/actions/_lib/mod_info/mod_info_constants.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_constants.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_derive_mod_folder_name.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_derive_mod_folder_name.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_derive_version_file_name.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_derive_version_file_name.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_derive_version_template_name.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_derive_version_template_name.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_derive_zip_name.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_derive_zip_name.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_derive_zip_name_regex.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_derive_zip_name_regex.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_enter_mod_root.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_enter_mod_root.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_escape_regex_text.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_escape_regex_text.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_format_locale_suffix.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_format_locale_suffix.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_has_dependency.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_has_dependency.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_locate_file.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_locate_file.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_read_dependency_version.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_read_dependency_version.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_read_release_fields.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_read_release_fields.sh"

# shellcheck source=.github/actions/_lib/mod_info/mod_info_require_fields.sh
source "${BASH_SOURCE[0]%/*}/mod_info/mod_info_require_fields.sh"
