#!/usr/bin/env bash
# Puts LWJGL's Linux natives into a folder, from the official artefact,
# checked against the build Starsector bundles.
#
# Usage: scripts/install-lwjgl-linux-natives.sh <destination-dir>
#
# FastRenderingBridgeIntegrationTests loads LWJGL's native library to set Fast
# Rendering's bridge up, from the install's starsector-core/native/<os>. A
# Windows install ships native/windows only, so a Linux machine running the
# suite needs these. Where they go is the caller's choice: a Linux install's
# starsector-core/native/linux, or a folder a CI runner's files are copied
# from. See docs/dev/rendering-environment.md.
#
# Nothing reaches the destination until the jar and every native in it match
# their pins. A destination already holding the pinned natives is left alone,
# so a rerun costs nothing and repairs a damaged copy.
#
# Needs curl, unzip, sha1sum and sha256sum on PATH.

# Every function reports failure by its return code and every call checks it,
# because the tests source this file without set -e. So a call in an || or a
# command substitution losing set -e is the design, not a trap.
# shellcheck disable=SC2310,SC2311

SCRIPT_NAME="install-lwjgl-linux-natives"

USAGE_EXIT_CODE=2

# The LWJGL build Starsector bundles, as Maven Central publishes its Linux
# natives.
LWJGL_VERSION="2.9.3"
NATIVES_JAR_NAME="lwjgl-platform-${LWJGL_VERSION}-natives-linux.jar"
NATIVES_JAR_URL="https://repo1.maven.org/maven2/org/lwjgl/lwjgl/lwjgl-platform/${LWJGL_VERSION}/${NATIVES_JAR_NAME}"

# Maven Central's published SHA-1 of that jar.
NATIVES_JAR_SHA1="b1eafe80093381c56415731e1d64279e6140bcd0"

# Every native the jar carries, by its SHA-256: the 32- and 64-bit builds of
# LWJGL and OpenAL, the set the Linux game ships in native/linux. -g keeps the
# table global when a test sources this file from inside a function.
declare -gA NATIVE_SHA256S=(
    [liblwjgl.so]="7739ce295bfb88bceb462b3a130ec4c1668689ec3acea4b006422b43e2386b5a"
    [liblwjgl64.so]="4622966cceee1c13df9e293fbae8fa402d1be84b3e1ade7256eca57892392f08"
    [libopenal.so]="1b8d26bdf799a14005e4c3e20b67c4449c48d4bf6bb51e871522669b351d20ca"
    [libopenal64.so]="265310b84e3fbc292354ad9901425a4f6532e8c3f730f36be96edb790174091c"
)

REQUIRED_COMMANDS=(curl unzip sha1sum sha256sum)

log_info() {

    printf '%s: %s\n' "${SCRIPT_NAME}" "$*" >&2
}

log_error() {

    printf '%s: ERROR: %s\n' "${SCRIPT_NAME}" "$*" >&2
}

print_usage() {

    printf 'Usage: %s.sh <destination-dir>\n' "${SCRIPT_NAME}" >&2
}

# Fails naming every command that is not on PATH, so one run lists them all.
#   require_commands <command>...
require_commands() {

    local missing=()
    local command_name

    for command_name in "$@"; do

        if ! command -v "${command_name}" >/dev/null 2>&1; then
            missing+=("${command_name}")
        fi
    done

    if [[ "${#missing[@]}" -gt 0 ]]; then
        log_error "required commands not on PATH: ${missing[*]}"
        return 1
    fi
}

# Prints a file's hash alone, without the file name the hash tools append.
#   read_file_hash <sha1sum|sha256sum> <file>
read_file_hash() {

    local line

    line="$("$1" "$2")" || return 1
    printf '%s' "${line%% *}"
}

# Prints the pinned natives the folder lacks or holds with another hash, space
# separated, and nothing when it holds all of them as pinned.
#   find_native_pin_mismatches <dir>
find_native_pin_mismatches() {

    local dir="$1"
    local mismatches=()
    local name
    local actual

    for name in "${!NATIVE_SHA256S[@]}"; do

        actual=""
        if [[ -f "${dir}/${name}" ]]; then
            actual="$(read_file_hash sha256sum "${dir}/${name}")" || return 1
        fi

        if [[ "${actual}" != "${NATIVE_SHA256S[${name}]}" ]]; then
            mismatches+=("${name}")
        fi
    done

    printf '%s' "${mismatches[*]}"
}

# Downloads the jar into the work folder and extracts its natives into
# <work dir>/natives, failing unless the jar and every native match their pins.
#   stage_verified_natives <work dir>
stage_verified_natives() {

    local work_dir="$1"
    local jar="${work_dir}/${NATIVES_JAR_NAME}"
    local actual_sha1
    local mismatches

    log_info "downloading ${NATIVES_JAR_URL}"
    if ! curl -fsSL -o "${jar}" "${NATIVES_JAR_URL}"; then
        log_error "could not download ${NATIVES_JAR_URL}; nothing was installed"
        return 1
    fi

    actual_sha1="$(read_file_hash sha1sum "${jar}")" || return 1
    if [[ "${actual_sha1}" != "${NATIVES_JAR_SHA1}" ]]; then
        log_error "${NATIVES_JAR_NAME} has SHA-1 ${actual_sha1}, pinned ${NATIVES_JAR_SHA1}; nothing was installed"
        return 1
    fi

    # -j drops the jar's folders: the natives sit at its root, and
    # native/linux holds them flat.
    if ! unzip -q -j "${jar}" '*.so' -d "${work_dir}/natives"; then
        log_error "could not extract the natives from ${NATIVES_JAR_NAME}; nothing was installed"
        return 1
    fi

    mismatches="$(find_native_pin_mismatches "${work_dir}/natives")" || return 1
    if [[ -n "${mismatches}" ]]; then
        log_error "missing from ${NATIVES_JAR_NAME} or differing from their pinned SHA-256: ${mismatches};" \
            "nothing was installed"
        return 1
    fi
}

# Copies the pinned natives from the staging folder into the destination.
#   copy_staged_natives <staging dir> <destination>
copy_staged_natives() {

    local staging_dir="$1"
    local destination="$2"
    local name

    mkdir -p "${destination}" || return 1

    for name in "${!NATIVE_SHA256S[@]}"; do
        cp -f "${staging_dir}/${name}" "${destination}/${name}" || return 1
    done

    log_info "installed ${#NATIVE_SHA256S[@]} natives of LWJGL ${LWJGL_VERSION} into ${destination}"
}

#   install_lwjgl_linux_natives <destination>
install_lwjgl_linux_natives() {

    local destination="$1"
    local mismatches
    local work_dir
    local status=0

    require_commands "${REQUIRED_COMMANDS[@]}" || return 1

    if [[ -d "${destination}" ]]; then

        mismatches="$(find_native_pin_mismatches "${destination}")" || return 1
        if [[ -z "${mismatches}" ]]; then
            log_info "LWJGL ${LWJGL_VERSION} Linux natives already in ${destination}"
            return 0
        fi

        log_info "replacing natives in ${destination} that are missing or differ from their pins: ${mismatches}"
    fi

    work_dir="$(mktemp -d)" || return 1

    # Removed on every path out rather than by an EXIT trap, which would
    # replace one a caller sourcing this file has set.
    stage_verified_natives "${work_dir}" || status=$?

    if [[ "${status}" -eq 0 ]]; then
        copy_staged_natives "${work_dir}/natives" "${destination}" || status=$?
    fi

    rm -rf "${work_dir}"
    return "${status}"
}

main() {

    if [[ "$#" -ne 1 ]]; then
        print_usage
        exit "${USAGE_EXIT_CODE}"
    fi

    install_lwjgl_linux_natives "$1"
}

# Sourced by its tests for the functions alone.
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    set -euo pipefail
    main "$@"
fi
