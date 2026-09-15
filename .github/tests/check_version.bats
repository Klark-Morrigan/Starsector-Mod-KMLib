#!/usr/bin/env bats
# Unit tests for the check-version composite action's script.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib mod root: bats .github/tests/check_version.bats
#
# External commands (jq, git) are stubbed by writing minimal shell scripts
# into a temporary directory that is prepended to PATH, so no real
# mod_info.json or git repository is needed.

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

# Writes a jq stub that ignores its arguments and prints VERSION.
stub_jq() {
    local version="$1"
    printf '#!/bin/sh\necho "%s"\n' "$version" > "$STUBS_DIR/jq"
    chmod +x "$STUBS_DIR/jq"
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
    stub_jq "1.2.3"
    stub_git_tags "1.2.3"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=false" "$GITHUB_OUTPUT"
}

@test "version_updated=true when no tag names the version" {
    # A patch bump, the smallest release the scheme allows and so the case
    # most at risk of being compared as equal.
    stub_jq "1.2.4"
    stub_git_tags "1.2.3"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=true" "$GITHUB_OUTPUT"
}

@test "version_updated=true when no tags exist" {
    stub_jq "1.0.0"
    stub_git_tags
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=true" "$GITHUB_OUTPUT"
}

# The case that made every push to master read as a bump: the released
# version is tagged, but that tag is not the newest one and need not be
# reachable from HEAD. Any tag naming the version settles it.
@test "version_updated=false when the version's tag is not the newest tag" {
    stub_jq "1.2.3"
    stub_git_tags "1.2.3 2.0.0"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version_updated=false" "$GITHUB_OUTPUT"
}

@test "version value is written to GITHUB_OUTPUT" {
    stub_jq "2.5.0"
    stub_git_tags "2.4.9"
    run bash "$SCRIPT"
    [ "$status" -eq 0 ]
    grep -q "version=2.5.0" "$GITHUB_OUTPUT"
}
