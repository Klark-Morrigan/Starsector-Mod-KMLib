#!/usr/bin/env bats
# Unit tests for .github/actions/publish-download-badges/scripts/summarise_release_downloads.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/summarise_release_downloads.bats
#
# gh is stubbed onto PATH so the suite never reaches the network, driven by
# environment variables as in check_dependency_release.bats. The stub replays
# a releases listing built by the helpers below, in the shape gh prints with
# --paginate and no --jq: one JSON array per page, back to back. jq runs for
# real, since the filter over that listing is what is under test.

SCRIPT="$BATS_TEST_DIRNAME/../actions/publish-download-badges/scripts/summarise_release_downloads.sh"
REPO_UNDER_TEST="Klark-Morrigan/Starsector-Mod-KMU"

setup() {

    TMP="$(mktemp -d)"
    mkdir -p "$TMP/bin" "$TMP/mod"

    # Records the arguments it was called with, then replays whatever the
    # case asked for.
    cat > "$TMP/bin/gh" <<'STUB'
#!/usr/bin/env bash

printf '%s\n' "$@" > "${GH_ARGS_FILE}"

[ -n "${GH_STUB_STDOUT:-}" ] && printf '%s\n' "${GH_STUB_STDOUT}"
[ -n "${GH_STUB_STDERR:-}" ] && printf '%s\n' "${GH_STUB_STDERR}" >&2

exit "${GH_STUB_EXIT:-0}"

STUB
    chmod +x "$TMP/bin/gh"

    GH_ARGS_FILE="$TMP/gh.args"
    PATH="$TMP/bin:$PATH"
    GITHUB_REPOSITORY="$REPO_UNDER_TEST"
    
    export GH_ARGS_FILE PATH GITHUB_REPOSITORY

    # Cleared per case so one test's stub setup cannot leak into the next.
    unset GH_STUB_STDOUT GH_STUB_STDERR GH_STUB_EXIT

    write_mod_info "$TMP/mod" "jars/KMU.jar"
    cd "$TMP/mod"
}

teardown() {
    rm -rf "$TMP"
}

# Writes a mod_info.json fixture holding the one field the script reads.
write_mod_info() {
    cat > "$1/mod_info.json" <<EOF
{
  "id": "kmu",
  "version": "1.2.3",
  "jars": ["$2"]
}
EOF
}

# Emits one release object, given its tag, its publish time, and its assets as
# "<name>:<download count>" pairs. A draft is stated with "draft" in place of
# the time, because a draft carries no publish time.
release() {

    local tag="$1" publishedAt="$2"
    
    shift 2
    
    local isDraft=false
    
    if [ "$publishedAt" = "draft" ]; then
        isDraft=true
        publishedAt=""
    fi
    
    local assets="[]" pair
    
    for pair in "$@"; do
        assets=$(jq -c --arg name "${pair%:*}" --argjson count "${pair##*:}" \
            '. + [{name: $name, download_count: $count}]' <<< "$assets")
    done
    
    jq -cn --arg tag "$tag" --arg publishedAt "$publishedAt" \
        --argjson draft "$isDraft" --argjson assets "$assets" \
        '{tag_name: $tag,
          published_at: (if $publishedAt == "" then null else $publishedAt end),
          draft: $draft,
          assets: $assets}'
}

# Emits one page of the listing from the given release objects.
page() {

    local IFS=,
    printf '[%s]' "$*"
}

# Makes the stub answer with the given pages, as gh prints them.
stub_pages() {

    export GH_STUB_STDOUT="$*" GH_STUB_EXIT=0
}

# Builds one expected summary line.
summary_line() {

    printf '%s\t%s\t%s' "$1" "$2" "$3"
}

@test "sums the unlocalised and every locale's zip of a release into one line" {

    stub_pages "$(page "$(release 1.2.3 2026-09-01T00:00:00Z \
        KMU-1.2.3.zip:5 KMU-1.2.3-en.zip:40 KMU-1.2.3-zh-hans.zip:15)")"

    run bash "$SCRIPT"

    [ "$status" -eq 0 ]
    [ "$output" = "$(summary_line 1.2.3 2026-09-01T00:00:00Z 60)" ]
}

@test "leaves every version file form out of the count" {

    # The update checkers poll these on every game start, so they outnumber
    # the zips many times over - excluding them is the point of the script.
    stub_pages "$(page "$(release 1.2.3 2026-09-01T00:00:00Z \
        KMU-1.2.3-en.zip:40 kmu.version:900 kmu-en.version:800 kmu-zh-hans.version:700)")"

    run bash "$SCRIPT"

    [ "$status" -eq 0 ]
    [ "$output" = "$(summary_line 1.2.3 2026-09-01T00:00:00Z 40)" ]
}

@test "leaves zips the release pipeline does not name out of the count" {

    stub_pages "$(page "$(release 1.2.3 2026-09-01T00:00:00Z \
        KMU-1.2.3-en.zip:40 KMLib-1.2.3.zip:1 KMUX-1.2.3.zip:2 KMU-sources.zip:3 \
        KMU-1.2.zip:4 KMU-1.2.3-en.zip.sha256:5 xKMU-1.2.3.zip:6 KMU-1.2.3_zip:7)")"

    run bash "$SCRIPT"

    [ "$status" -eq 0 ]
    [ "$output" = "$(summary_line 1.2.3 2026-09-01T00:00:00Z 40)" ]
}

@test "skips draft releases" {

    stub_pages "$(page \
        "$(release 1.3.0 draft KMU-1.3.0-en.zip:7)" \
        "$(release 1.2.3 2026-09-01T00:00:00Z KMU-1.2.3-en.zip:40)")"

    run bash "$SCRIPT"

    [ "$status" -eq 0 ]
    [ "$output" = "$(summary_line 1.2.3 2026-09-01T00:00:00Z 40)" ]
}

@test "leaves out a release carrying no matching zip" {

    stub_pages "$(page \
        "$(release 1.2.4 2026-09-10T00:00:00Z kmu.version:9)" \
        "$(release 1.2.3 2026-09-01T00:00:00Z KMU-1.2.3-en.zip:40)")"

    run bash "$SCRIPT"

    [ "$status" -eq 0 ]
    [ "$output" = "$(summary_line 1.2.3 2026-09-01T00:00:00Z 40)" ]
}

@test "lists releases newest first across pages" {

    # The second page holds the newest release, so an order taken from the
    # listing, or a sort run page by page, would put it last.
    stub_pages \
        "$(page \
            "$(release 0.1.0 2026-01-01T00:00:00Z KMU-0.1.0.zip:1)" \
            "$(release 1.2.3 2026-09-01T00:00:00Z KMU-1.2.3-en.zip:40)")" \
        "$(page "$(release 1.2.4 2026-09-10T00:00:00Z KMU-1.2.4-en.zip:3)")"

    run bash "$SCRIPT"

    [ "$status" -eq 0 ]
    [ "$output" = "$(summary_line 1.2.4 2026-09-10T00:00:00Z 3)
$(summary_line 1.2.3 2026-09-01T00:00:00Z 40)
$(summary_line 0.1.0 2026-01-01T00:00:00Z 1)" ]
}

@test "reports a zero count for a matching zip nobody has downloaded" {

    # A release that ships a zip is a release, even before its first download.
    stub_pages "$(page "$(release 1.2.4 2026-09-10T00:00:00Z KMU-1.2.4-en.zip:0)")"
    run bash "$SCRIPT"

    [ "$status" -eq 0 ]
    [ "$output" = "$(summary_line 1.2.4 2026-09-10T00:00:00Z 0)" ]
}

@test "lists every page of the given repository's releases" {

    stub_pages "$(page "$(release 1.2.3 2026-09-01T00:00:00Z KMU-1.2.3-en.zip:40)")"
    run bash "$SCRIPT"

    [ "$status" -eq 0 ]

    grep -qx "api" "$GH_ARGS_FILE"
    grep -qx -- "--paginate" "$GH_ARGS_FILE"
    grep -qx "repos/$REPO_UNDER_TEST/releases?per_page=100" "$GH_ARGS_FILE"
}

@test "reads mod_info.json from the given mod root" {

    mkdir -p "$TMP/caller/mod"
    write_mod_info "$TMP/caller/mod" "jars/Other.jar"

    stub_pages "$(page "$(release 1.2.3 2026-09-01T00:00:00Z \
        KMU-1.2.3-en.zip:40 Other-1.2.3-en.zip:8)")"

    cd "$TMP/caller"
    run bash "$SCRIPT" mod

    [ "$status" -eq 0 ]
    [ "$output" = "$(summary_line 1.2.3 2026-09-01T00:00:00Z 8)" ]
}

@test "fails when no release carries a matching zip" {

    stub_pages "$(page "$(release 1.2.3 2026-09-01T00:00:00Z kmu.version:9)")"
    run bash "$SCRIPT"

    [ "$status" -ne 0 ]
    [[ "$output" == *"no release of $REPO_UNDER_TEST carries a zip named KMU-<version>.zip"* ]]
}

@test "fails when the repository has no releases" {

    stub_pages "[]"
    run bash "$SCRIPT"

    [ "$status" -ne 0 ]
    [[ "$output" == *"no release of $REPO_UNDER_TEST"* ]]
}

@test "fails with gh's own message when the lookup fails" {

    export GH_STUB_STDERR="gh: Bad credentials (HTTP 401)" GH_STUB_EXIT=1
    run bash "$SCRIPT"

    [ "$status" -ne 0 ]
    [[ "$output" == *"could not list the releases of $REPO_UNDER_TEST"* ]]
    [[ "$output" == *"HTTP 401"* ]]
}

@test "prints nothing to stdout when it fails" {

    # A caller publishing from stdout must not mistake a partial listing for
    # figures.
    export GH_STUB_STDOUT="$(page "$(release 1.2.3 2026-09-01T00:00:00Z KMU-1.2.3-en.zip:40)")"
    export GH_STUB_EXIT=1
    bash "$SCRIPT" > "$TMP/stdout" 2> /dev/null || true

    [ ! -s "$TMP/stdout" ]
}

@test "requires the repository" {

    unset GITHUB_REPOSITORY
    run bash "$SCRIPT"

    [ "$status" -ne 0 ]
    [[ "$output" == *"GITHUB_REPOSITORY is required"* ]]

    # Cheapest checks first: an incomplete invocation costs no API call.
    [ ! -f "$GH_ARGS_FILE" ]
}

@test "requires jars[0] in mod_info.json" {

    echo '{"id": "kmu", "version": "1.2.3"}' > "$TMP/mod/mod_info.json"
    run bash "$SCRIPT"

    [ "$status" -ne 0 ]
    [[ "$output" == *"missing required field 'jars[0]'"* ]]
    [ ! -f "$GH_ARGS_FILE" ]
}

@test "fails when the mod root does not exist" {

    run bash "$SCRIPT" missing

    [ "$status" -ne 0 ]
    [[ "$output" == *"mod root missing not found"* ]]
}
