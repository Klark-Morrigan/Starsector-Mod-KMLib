#!/usr/bin/env bats
# Unit tests for the check-version composite action's script.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib mod root: bats .github/tests/check_version.bats
#
# git is stubbed by writing a minimal shell script into a temporary
# directory that is prepended to PATH, so no real repository is needed.

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

SCRIPT="$BATS_TEST_DIRNAME/../actions/check-version/scripts/check_version.sh"

setup() {
    STUBS_DIR="$(mktemp -d)"
    export GITHUB_OUTPUT
    GITHUB_OUTPUT="$(mktemp)"
    export PATH="$STUBS_DIR:$PATH"
}

teardown() {
    rm -rf "$STUBS_DIR"
    rm -f "$GITHUB_OUTPUT"
}

# Writes a git stub standing in for `git rev-parse --verify refs/tags/<v>^{}`:
# it exits 0 only for the tag names it is given and 1 for every other ref,
# the way rev-parse reports an absent one. Call it with no arguments for a
# repository that carries no tags at all.
stub_git_tags() {
    local tags="$*"
    cat > "$STUBS_DIR/git" <<EOF
#!/bin/sh
TAGS="${tags}"
for tag in \$TAGS; do
  case "\$*" in
    *"refs/tags/\${tag}^{}"*) exit 0 ;;
  esac
done
exit 1
EOF
    chmod +x "$STUBS_DIR/git"
}

@test "version_updated=false when a tag already names the version" {
    stub_git_tags "1.2.3"
    VERSION=1.2.3 run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=false" "$GITHUB_OUTPUT"
}

@test "version_updated=true when no tag names the version" {
    # A patch bump, the smallest release the scheme allows and so the case
    # most at risk of being compared as equal.
    stub_git_tags "1.2.3"
    VERSION=1.2.4 run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=true" "$GITHUB_OUTPUT"
}

@test "version_updated=true when no tags exist" {
    stub_git_tags
    VERSION=1.0.0 run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=true" "$GITHUB_OUTPUT"
}

# The case that made every push to master read as a bump: the released
# version is tagged, but that tag is not the newest one and need not be
# reachable from HEAD. Any tag naming the version settles it.
@test "version_updated=false when the version's tag is not the newest tag" {
    stub_git_tags "1.2.3 2.0.0"
    VERSION=1.2.3 run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=false" "$GITHUB_OUTPUT"
}

@test "logs a tagged version as released, then the output, on stderr alone" {
    stub_git_tags "1.2.3"
    VERSION=1.2.3 run --separate-stderr bash "$SCRIPT"
    [ "$status" -eq 0 ]
    [ "$stderr" = $'Tag 1.2.3 exists, so this version is already released.\nversion_updated: false' ]
    [ "$output" = "" ]
}

@test "logs an untagged version as unreleased, then the output" {
    stub_git_tags "1.2.3"
    VERSION=1.2.4 run --separate-stderr bash "$SCRIPT"
    [ "$status" -eq 0 ]
    [ "$stderr" = $'No tag names 1.2.4, so this version is unreleased.\nversion_updated: true' ]
}

@test "fails when no version is given" {
    stub_git_tags "1.2.3"
    run bash "$SCRIPT"
    [ "$status" -ne 0 ]
    [[ "$output" == *"VERSION is required"* ]]
}

# The version is the caller's to supply; reading mod_info.json is
# read-mod-info's job. A stray read here would make the gate rule on a
# different string than the one the rest of the pipeline builds.
@test "asks git about the given version without reading mod_info.json" {
    stub_git_tags "9.9.9"
    cd "$(mktemp -d)"
    VERSION=9.9.9 run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=false" "$GITHUB_OUTPUT"
}
