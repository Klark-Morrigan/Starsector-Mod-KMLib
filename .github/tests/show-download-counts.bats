#!/usr/bin/env bats
# Unit tests for scripts/show-download-counts.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/show-download-counts.bats
#
# The script finds the read scripts and Common-Automation through its own
# location, so each case runs a copy of it in a stand-in workspace: KMLib's
# real actions beside it, and a Common-Automation of stubs. Its dry run prints
# the flags it was handed, its section heading prints the title alone, and its
# checkout description names the directory. CI checks KMLib out without that
# sibling, so the suite cannot copy the real ones. The mod root is a fixture
# with its own metadata, manifest and origin remote.

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

# shellcheck source=mod_root_fixtures.bash
source "$BATS_TEST_DIRNAME/mod_root_fixtures.bash"

REPOSITORY="Klark-Morrigan/Starsector-Mod-Fixture"
EXPECTED_ASSET_NAME_REGEX='^Fixture-[0-9]+\.[0-9]+\.[0-9]+(-(?<locale>.+))?\.zip$'
ENGLISH_MANIFEST='{ "defaultLocale": "en", "locales": { "en": { "displayName": "English" } } }'

setup() {

    local workspace="${BATS_TEST_TMPDIR}/workspace"
    local stand_in_common_automation="${workspace}/Common-Automation"

    STAND_IN_KMLIB="${workspace}/Starsector-Mod-KMLib"
    STAND_IN_SCRIPT="${STAND_IN_KMLIB}/scripts/show-download-counts.sh"
    MOD_ROOT="${workspace}/mod"

    mkdir -p "${STAND_IN_KMLIB}/scripts" \
        "${STAND_IN_KMLIB}/.github" \
        "${stand_in_common_automation}/scripts" \
        "${stand_in_common_automation}/.github/lib"

    cp "${BATS_TEST_DIRNAME}/../../scripts/show-download-counts.sh" "${STAND_IN_SCRIPT}"
    cp -r "${BATS_TEST_DIRNAME}/../actions" "${STAND_IN_KMLIB}/.github/"

    write_common_automation_stubs "${stand_in_common_automation}"

    write_mod_info_file "${MOD_ROOT}" "fixture" "1.2.3" "jars/Fixture.jar"
    write_manifest_file "${MOD_ROOT}" "${ENGLISH_MANIFEST}"

    init_mod_repo "${MOD_ROOT}" "https://github.com/${REPOSITORY}.git"
}

# Writes the stand-in Common-Automation: a dry run printing its flags, and
# stubs of the two lib functions the script sources.
#   write_common_automation_stubs <stand-in root>
write_common_automation_stubs() {

    cat > "$1/scripts/show-download-counts.sh" <<'STUB'
#!/usr/bin/env bash
printf '%s\n' "$@"
STUB
    chmod +x "$1/scripts/show-download-counts.sh"

    cat > "$1/.github/lib/print-section-heading.sh" <<'STUB'
print_section_heading() {
    printf '## %s\n' "$*"
}
STUB

    cat > "$1/.github/lib/describe-git-checkout.sh" <<'STUB'
describe_git_checkout() {
    printf 'checkout of %s\n' "$1"
}
STUB
}

# Makes the directory a git repository with the given origin.
#   init_mod_repo <dir> <origin url>
init_mod_repo() {

    git -C "$1" init --quiet
    git -C "$1" remote add origin "$2"
}

show_download_counts() {

    run --separate-stderr "${STAND_IN_SCRIPT}" "$@"
}

# Prints the flags the dry run should be handed, given the default locale and
# the Nexus mod id.
build_expected_flags() {

    printf '%s\n' \
        --repository "${REPOSITORY}" \
        --asset-name-regex "${EXPECTED_ASSET_NAME_REGEX}" \
        --default-locale "$1" \
        --label "Fixture" \
        --nexus-game-id "2672" \
        --nexus-mod-id "${2:-}" \
        --nexus-file-name "Fixture" \
        --nexus-locale "$1"
}

# Writes a version template stating the given Nexus mod id into the mod root.
#   write_version_template <dir> <nexus mod id>
write_version_template() {

    printf '{ "masterVersionFile": "https://example.invalid/fixture.version", "modNexusId": %s }\n' "$2" \
        > "$1/fixture.version.template"
}

# Prints the stderr lines between the given heading and the next one.
read_section() {

    awk -v heading="## $1" '
        $0 == heading { inside = 1; next }
        /^## /        { inside = 0 }
        inside' <<< "${stderr}"
}

@test "derives the inputs from the given mod root" {

    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [ "${output}" = "$(build_expected_flags en)" ]
}

# KMLib checks its own badges with no argument.
@test "defaults to its own repository's root" {

    write_mod_info_file "${STAND_IN_KMLIB}" "fixture" "1.2.3" "jars/Fixture.jar"
    write_manifest_file "${STAND_IN_KMLIB}" "${ENGLISH_MANIFEST}"

    init_mod_repo "${STAND_IN_KMLIB}" "https://github.com/${REPOSITORY}.git"
    show_download_counts

    [ "${status}" -eq 0 ]
    [ "${output}" = "$(build_expected_flags en)" ]
}

# The workflow passes read-locales' empty default through unchanged, and so
# does this script: the action counts such a mod under no locale.
@test "hands on an empty default locale for a mod without a manifest" {

    rm -rf "${MOD_ROOT}/localisation"
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [ "${output}" = "$(build_expected_flags "")" ]
}

# The dry run reads Nexus exactly when a scheduled run would.
@test "hands on the Nexus mod id the version template states" {

    write_version_template "${MOD_ROOT}" 160
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [ "${output}" = "$(build_expected_flags en 160)" ]
}

@test "fails on an origin that is not GitHub, before the dry run" {

    git -C "${MOD_ROOT}" remote set-url origin "https://example.com/${REPOSITORY}.git"
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -ne 0 ]
    [[ "${stderr}" == *"show-download-counts: origin 'https://example.com/${REPOSITORY}.git'"* ]]
    [ -z "${output}" ]
}

# The subshell each read script runs in must not swallow its failure.
@test "stops before the dry run when a read script fails" {

    rm "${MOD_ROOT}/mod_info.json"
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -ne 0 ]
    [[ "${stderr}" == *"neither mod_info.base.json nor mod_info.json found"* ]]
    [[ "${stderr}" != *"## Inputs"* ]]
    [ -z "${output}" ]
}

@test "logs the mod's and KMLib's checkouts under a heading" {

    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [ "$(read_section "Checkouts")" = "checkout of ${MOD_ROOT}
checkout of ${STAND_IN_KMLIB}" ]
}

@test "logs one checkout when the mod is KMLib itself" {

    write_mod_info_file "${STAND_IN_KMLIB}" "fixture" "1.2.3" "jars/Fixture.jar"

    init_mod_repo "${STAND_IN_KMLIB}" "https://github.com/${REPOSITORY}.git"
    show_download_counts

    [ "${status}" -eq 0 ]
    [ "$(read_section "Checkouts")" = "checkout of ${STAND_IN_KMLIB}" ]
}

@test "heads each read script's log with its name" {

    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [[ "$(read_section "read_mod_info")" == "Reading mod_info.json."* ]]
    [[ "$(read_section "read_locales")" == "Reading localisation/manifest.json."* ]]
}

@test "logs each input it derived under a heading" {

    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [ "$(read_section "Inputs")" = "repository: ${REPOSITORY}
asset-name-regex: ${EXPECTED_ASSET_NAME_REGEX}
default-locale: en
label: Fixture
nexus-game-id: 2672
nexus-mod-id:
nexus-file-name: Fixture
nexus-locale: en" ]
}

@test "logs an empty input as its name alone" {

    rm -rf "${MOD_ROOT}/localisation"
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [[ "$(read_section "Inputs")" == *$'\ndefault-locale:\n'* ]]
}
