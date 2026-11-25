#!/usr/bin/env bats
# Unit tests for scripts/install-lwjgl-linux-natives.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/install-lwjgl-linux-natives.bats
#
# The script is sourced, so its functions run with the pins pointed at
# fixtures: no case reaches the network or needs the real jar. curl and unzip
# are stubs - neither is in CI's bats image - that hand over the fixture jar
# and the fixture natives, and record how they were called.

# run --separate-stderr arrived in 1.5.0.
bats_require_minimum_version 1.5.0

SCRIPT="${BATS_TEST_DIRNAME}/../../scripts/install-lwjgl-linux-natives.sh"

# shellcheck source=../../scripts/install-lwjgl-linux-natives.sh
source "${SCRIPT}"

# Any 40 hex digits but the fixture's own hash.
WRONG_SHA1="0000000000000000000000000000000000000000"
WRONG_SHA256="0000000000000000000000000000000000000000000000000000000000000000"

setup() {

    DESTINATION="${BATS_TEST_TMPDIR}/destination"

    export CALLS_DIR="${BATS_TEST_TMPDIR}/calls"
    export FIXTURE_JAR="${BATS_TEST_TMPDIR}/fixture.jar"
    export FIXTURE_NATIVES_DIR="${BATS_TEST_TMPDIR}/fixture-natives"

    # The script's work folder lands here, so a case can see it was removed.
    export TMPDIR="${BATS_TEST_TMPDIR}/tmp"

    mkdir -p "${CALLS_DIR}" "${FIXTURE_NATIVES_DIR}" "${TMPDIR}"

    printf 'fixture jar bytes' > "${FIXTURE_JAR}"
    printf 'lwjgl bytes' > "${FIXTURE_NATIVES_DIR}/liblwjgl.so"
    printf 'openal bytes' > "${FIXTURE_NATIVES_DIR}/libopenal.so"

    NATIVES_JAR_SHA1="$(read_file_hash sha1sum "${FIXTURE_JAR}")"
    NATIVE_SHA256S=(
        [liblwjgl.so]="$(read_file_hash sha256sum "${FIXTURE_NATIVES_DIR}/liblwjgl.so")"
        [libopenal.so]="$(read_file_hash sha256sum "${FIXTURE_NATIVES_DIR}/libopenal.so")"
    )

    write_stubs "${BATS_TEST_TMPDIR}/stubs"
    PATH="${BATS_TEST_TMPDIR}/stubs:${PATH}"
}

# Writes curl and unzip stubs. Each records its arguments, one call per line,
# and fails when the case asks it to.
#   write_stubs <stubs dir>
write_stubs() {

    mkdir -p "$1"

    cat > "$1/curl" <<'STUB'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${CALLS_DIR}/curl"
[[ -z "${CURL_FAILS:-}" ]] || exit 22
while [[ "$#" -gt 0 ]]; do
    if [[ "$1" == "-o" ]]; then out="$2"; shift; fi
    shift
done
cp "${FIXTURE_JAR}" "${out}"
STUB

    cat > "$1/unzip" <<'STUB'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "${CALLS_DIR}/unzip"
[[ -z "${UNZIP_FAILS:-}" ]] || exit 9
while [[ "$#" -gt 0 ]]; do
    if [[ "$1" == "-d" ]]; then out="$2"; shift; fi
    shift
done
mkdir -p "${out}"
cp "${FIXTURE_NATIVES_DIR}"/* "${out}/"
STUB

    chmod +x "$1/curl" "$1/unzip"
}

# Puts the fixture natives into the destination, as an earlier run would have.
populate_destination() {

    mkdir -p "${DESTINATION}"
    cp "${FIXTURE_NATIVES_DIR}"/* "${DESTINATION}/"
}

@test "installs every pinned native into a destination that does not exist yet" {

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 0 ]
    cmp "${FIXTURE_NATIVES_DIR}/liblwjgl.so" "${DESTINATION}/liblwjgl.so"
    cmp "${FIXTURE_NATIVES_DIR}/libopenal.so" "${DESTINATION}/libopenal.so"
    [[ "${stderr}" == *"installed 2 natives"* ]]
}

@test "downloads the pinned jar" {

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 0 ]
    [[ "$(cat "${CALLS_DIR}/curl")" == *"${NATIVES_JAR_URL}"* ]]
}

@test "extracts only the natives, without the jar's folders" {

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 0 ]
    [[ "$(cat "${CALLS_DIR}/unzip")" == *"-j"* ]]
    [[ "$(cat "${CALLS_DIR}/unzip")" == *"*.so"* ]]
}

@test "rejects a jar whose SHA-1 differs from the pin and installs nothing" {

    NATIVES_JAR_SHA1="${WRONG_SHA1}"

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 1 ]
    [[ "${stderr}" == *"SHA-1"* ]]
    [[ "${stderr}" == *"${WRONG_SHA1}"* ]]
    [ ! -e "${DESTINATION}" ]
    [ ! -e "${CALLS_DIR}/unzip" ]
}

@test "rejects a native whose SHA-256 differs from its pin and installs nothing" {

    NATIVE_SHA256S[liblwjgl.so]="${WRONG_SHA256}"

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 1 ]
    [[ "${stderr}" == *"liblwjgl.so"* ]]
    [[ "${stderr}" != *"libopenal.so"* ]]
    [ ! -e "${DESTINATION}" ]
}

@test "rejects a jar that lacks a pinned native and installs nothing" {

    NATIVE_SHA256S[libabsent.so]="${WRONG_SHA256}"

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 1 ]
    [[ "${stderr}" == *"libabsent.so"* ]]
    [ ! -e "${DESTINATION}" ]
}

@test "fails when the download fails and installs nothing" {

    export CURL_FAILS=1

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 1 ]
    [[ "${stderr}" == *"could not download"* ]]
    [ ! -e "${DESTINATION}" ]
}

@test "fails when the natives cannot be extracted and installs nothing" {

    export UNZIP_FAILS=1

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 1 ]
    [[ "${stderr}" == *"could not extract"* ]]
    [ ! -e "${DESTINATION}" ]
}

@test "leaves a destination that already holds the pinned natives alone" {

    populate_destination

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 0 ]
    [[ "${stderr}" == *"already in"* ]]
    [ ! -e "${CALLS_DIR}/curl" ]
}

@test "replaces a native in the destination that differs from its pin, and names it" {

    populate_destination
    printf 'tampered' > "${DESTINATION}/libopenal.so"

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 0 ]
    cmp "${FIXTURE_NATIVES_DIR}/libopenal.so" "${DESTINATION}/libopenal.so"
    [[ "${stderr}" == *"libopenal.so"* ]]
    [[ "${stderr}" != *"liblwjgl.so"* ]]
}

@test "removes its work folder after an install" {

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 0 ]
    [ -z "$(ls -A "${TMPDIR}")" ]
}

@test "removes its work folder after a rejected jar" {

    NATIVES_JAR_SHA1="${WRONG_SHA1}"

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 1 ]
    [ -z "$(ls -A "${TMPDIR}")" ]
}

@test "fails before downloading when a required command is missing" {

    REQUIRED_COMMANDS=(curl no-such-command-for-this-test)

    run --separate-stderr install_lwjgl_linux_natives "${DESTINATION}"

    [ "${status}" -eq 1 ]
    [[ "${stderr}" == *"no-such-command-for-this-test"* ]]
    [ ! -e "${CALLS_DIR}/curl" ]
}

@test "names every missing command at once" {

    run --separate-stderr require_commands no-such-command-a curl no-such-command-b

    [ "${status}" -eq 1 ]
    [[ "${stderr}" == *"no-such-command-a no-such-command-b"* ]]
}

@test "prints its usage and exits 2 without a destination" {

    run --separate-stderr bash "${SCRIPT}"

    [ "${status}" -eq 2 ]
    [[ "${stderr}" == *"Usage:"* ]]
}

@test "prints its usage and exits 2 given more than a destination" {

    run --separate-stderr bash "${SCRIPT}" "${DESTINATION}" extra

    [ "${status}" -eq 2 ]
    [[ "${stderr}" == *"Usage:"* ]]
    [ ! -e "${DESTINATION}" ]
}
