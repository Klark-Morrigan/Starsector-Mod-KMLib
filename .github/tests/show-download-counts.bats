#!/usr/bin/env bats
# Unit tests for scripts/show-download-counts.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/show-download-counts.bats
#
# The script finds the read scripts and Common-Automation through its own
# location, so each case runs a copy of it in a stand-in workspace: KMLib's
# real actions beside it, and a Common-Automation whose dry run is a stub
# printing the flags it was handed. The mod root is a fixture with its own
# metadata, manifest and origin remote.

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

REPOSITORY="Klark-Morrigan/Starsector-Mod-Fixture"
EXPECTED_ASSET_NAME_REGEX='^Fixture-[0-9]+\.[0-9]+\.[0-9]+(-(?<locale>.+))?\.zip$'

setup() {

    local workspace="${BATS_TEST_TMPDIR}/workspace"
    local stand_in_kmlib="${workspace}/Starsector-Mod-KMLib"
    local dry_run_stub="${workspace}/Common-Automation/scripts/show-download-counts.sh"

    STAND_IN_SCRIPT="${stand_in_kmlib}/scripts/show-download-counts.sh"
    MOD_ROOT="${workspace}/mod"

    mkdir -p "${stand_in_kmlib}/scripts" "${stand_in_kmlib}/.github" "${dry_run_stub%/*}"

    cp "${BATS_TEST_DIRNAME}/../../scripts/show-download-counts.sh" "${STAND_IN_SCRIPT}"
    cp -r "${BATS_TEST_DIRNAME}/../actions" "${stand_in_kmlib}/.github/"

    cat > "${dry_run_stub}" <<'STUB'
#!/usr/bin/env bash
printf '%s\n' "$@"
STUB
    chmod +x "${dry_run_stub}"

    write_mod_info "${MOD_ROOT}"
    write_manifest "${MOD_ROOT}"

    init_mod_repo "${MOD_ROOT}" "https://github.com/${REPOSITORY}.git"
}

# Writes a mod_info.json whose jar names the folder "Fixture".
#   write_mod_info <mod root>
write_mod_info() {

    mkdir -p "$1"

    cat > "$1/mod_info.json" <<'EOF'
{
  "id": "fixture",
  "version": "1.2.3",
  "jars": ["jars/Fixture.jar"]
}
EOF
}

# Writes a manifest defaulting to English.
#   write_manifest <mod root>
write_manifest() {

    mkdir -p "$1/localisation"

    cat > "$1/localisation/manifest.json" <<'EOF'
{
  "defaultLocale": "en",
  "locales": { "en": { "displayName": "English" } }
}
EOF
}

# Makes the mod root a git repository with the given origin.
#   init_mod_repo <mod root> <origin url>
init_mod_repo() {

    git -C "$1" init --quiet
    git -C "$1" remote add origin "$2"
}

show_download_counts() {

    run --separate-stderr "${STAND_IN_SCRIPT}" "$@"
}

# Prints the flags the dry run should be handed, given the default locale.
build_expected_flags() {

    printf '%s\n' \
        --repository "${REPOSITORY}" \
        --asset-name-regex "${EXPECTED_ASSET_NAME_REGEX}" \
        --default-locale "$1" \
        --label "Fixture"
}

@test "derives the inputs from the given mod root" {

    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [ "${output}" = "$(build_expected_flags en)" ]
}

# KMLib checks its own badges with no argument.
@test "defaults to its own repository's root" {

    local stand_in_kmlib="${BATS_TEST_TMPDIR}/workspace/Starsector-Mod-KMLib"

    write_mod_info "${stand_in_kmlib}"
    write_manifest "${stand_in_kmlib}"

    init_mod_repo "${stand_in_kmlib}" "https://github.com/${REPOSITORY}.git"
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

@test "reads the repository from an ssh origin" {

    git -C "${MOD_ROOT}" remote set-url origin "git@github.com:${REPOSITORY}.git"
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [ "${output}" = "$(build_expected_flags en)" ]
}

@test "reads the repository from an origin without .git" {

    git -C "${MOD_ROOT}" remote set-url origin "https://github.com/${REPOSITORY}"
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [ "${output}" = "$(build_expected_flags en)" ]
}

@test "fails on an origin that is not GitHub, naming it, before the dry run" {

    git -C "${MOD_ROOT}" remote set-url origin "https://example.com/${REPOSITORY}.git"
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -ne 0 ]
    [[ "${stderr}" == *"origin 'https://example.com/${REPOSITORY}.git'"*"is not a GitHub repository."* ]]
    [ -z "${output}" ]
}

@test "fails on a mod root with no origin, before the dry run" {

    git -C "${MOD_ROOT}" remote remove origin
    show_download_counts "${MOD_ROOT}"

    [ "${status}" -ne 0 ]
    [[ "${stderr}" == *"has no origin remote"* ]]
    [ -z "${output}" ]
}

@test "logs each input it derived" {

    show_download_counts "${MOD_ROOT}"

    [ "${status}" -eq 0 ]
    [[ "${stderr}" == *"repository: ${REPOSITORY}"* ]]
    [[ "${stderr}" == *"asset-name-regex: ${EXPECTED_ASSET_NAME_REGEX}"* ]]
    [[ "${stderr}" == *"default-locale: en"* ]]
    [[ "${stderr}" == *"label: Fixture"* ]]
}
