#!/usr/bin/env bats
# Unit tests for .github/actions/package-release/scripts/package_release.sh.
#
# Requires bats-core: https://github.com/bats-core/bats-core
# Run from the KMLib repo root: bats .github/tests/package_release.bats
#
# The script drives two tools that are not its own: the mod's Gradle build and
# zip. Both are replaced by fakes on the fixture's own terms - a gradlew that
# writes a locale the way writeLocaleFiles does, and a zip that copies what it
# was asked to pack somewhere a case can read it - so a case asserts what each
# zip would hold without either tool installed. The version file is filled by
# the real fill script, since which file a zip carries is the point.

SCRIPT="$BATS_TEST_DIRNAME/../actions/package-release/scripts/package_release.sh"

OUTPUT_DIR_NAME="release-assets"
CHECKOUT_DIR_NAME="checkout"

TWO_LOCALES='[{"tag":"en"},{"tag":"zh-hans"}]'

setup() {
    WORK_DIR="$(mktemp -d)"
    CHECKOUT="$WORK_DIR/$CHECKOUT_DIR_NAME"
    OUTPUT="$WORK_DIR/$OUTPUT_DIR_NAME"
    FAKE_BIN="$WORK_DIR/bin"
    export ZIP_CAPTURE_DIR="$WORK_DIR/zip-captures"
    export GRADLE_CALL_LOG="$WORK_DIR/gradle-calls.log"
    export GITHUB_REPOSITORY="Klark-Morrigan/Starsector-Mod-KMU"
    unset LOCALES DEFAULT_LOCALE

    mkdir -p "$CHECKOUT/jars" "$CHECKOUT/data/strings" "$FAKE_BIN" "$ZIP_CAPTURE_DIR"
    : > "$GRADLE_CALL_LOG"
    write_checkout
    write_fake_zip
    export PATH="$FAKE_BIN:$PATH"
    export MOD_ROOT="$CHECKOUT_DIR_NAME"
    export OUTPUT_DIR="$OUTPUT_DIR_NAME"
}

teardown() {
    rm -rf "$WORK_DIR"
}

# A checkout keeping two locales, its jar built and the default locale written,
# which is the state the release leaves it in before packaging.
write_checkout() {
    cat > "$CHECKOUT/mod_info.base.json" <<EOF
{ "id": "kmu", "name": "KMU", "version": "1.2.3", "gameVersion": "0.98a-RC8", "jars": ["jars/KMU.jar"] }
EOF
    cp "$CHECKOUT/mod_info.base.json" "$CHECKOUT/mod_info.json"
    cat > "$CHECKOUT/kmu.version.template" <<EOF
{
  "masterVersionFile": "https://example.invalid/latest/download/kmu.version",
  "modName": "{{modName}}",
  "modVersion": { "major": "{{major}}", "minor": "{{minor}}", "patch": "{{patch}}" },
  "starsectorVersion": "{{starsectorVersion}}",
  "directDownloadURL": "{{directDownloadURL}}"
}
EOF
    for localeTag in en zh-hans; do
        mkdir -p "$CHECKOUT/localisation/$localeTag"
        echo "{ \"text\": \"$localeTag\" }" > "$CHECKOUT/localisation/$localeTag/strings.json"
    done
    cp "$CHECKOUT/localisation/en/strings.json" "$CHECKOUT/data/strings/strings.json"
    echo "jar" > "$CHECKOUT/jars/KMU.jar"
    echo "readme" > "$CHECKOUT/README.md"
    mkdir -p "$CHECKOUT/src"
    echo "source" > "$CHECKOUT/src/Source.java"

    # Writes the requested locale's strings and launcher name, the two kinds of
    # file writeLocaleFiles writes, and logs how it was called.
    cat > "$CHECKOUT/gradlew" <<'EOF'
#!/usr/bin/env bash
echo "$*" >> "$GRADLE_CALL_LOG"
for argument in "$@"; do
    case "$argument" in -Plocale=*) localeTag="${argument#-Plocale=}" ;; esac
done
cp "localisation/$localeTag/strings.json" data/strings/strings.json
jq --arg name "KMU $localeTag" '.name = $name' mod_info.base.json > mod_info.json
EOF
    chmod +x "$CHECKOUT/gradlew"
}

# Stands in for zip: copies the folder it was asked to pack into a directory
# named after the zip, and writes the zip itself as an empty file so the asset
# exists where the release would look for it.
write_fake_zip() {
    cat > "$FAKE_BIN/zip" <<'EOF'
#!/usr/bin/env bash
zipPath="${@: -2:1}"
folder="${@: -1}"
mkdir -p "$ZIP_CAPTURE_DIR/$(basename "$zipPath")"
cp -r "$folder" "$ZIP_CAPTURE_DIR/$(basename "$zipPath")/"
: > "$zipPath"
EOF
    chmod +x "$FAKE_BIN/zip"
}

# Echoes the output directory's file names, sorted and space-joined.
list_assets() {
    (cd "$OUTPUT" && find . -type f | sed 's#^\./##' | sort | tr '\n' ' ')
}

# Echoes a file out of what the named zip would hold.
read_zipped_file() {
    cat "$ZIP_CAPTURE_DIR/$1/KMU/$2"
}

run_package() {
    cd "$WORK_DIR"
    run bash "$SCRIPT"
}

@test "packages one unsuffixed zip and version file for a mod keeping no locales" {
    run_package
    [ "$status" -eq 0 ]
    [ "$(list_assets)" = "KMU-1.2.3.zip kmu.version " ]
}

@test "writes no locale for a mod keeping none" {
    run_package
    [ "$status" -eq 0 ]
    # The jar build already wrote the only files there are.
    [ ! -s "$GRADLE_CALL_LOG" ]
}

@test "packages a zip and a version file per locale, and the unsuffixed version file" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="en"
    run_package
    [ "$status" -eq 0 ]
    [ "$(list_assets)" = "KMU-1.2.3-en.zip KMU-1.2.3-zh-hans.zip kmu-en.version kmu-zh-hans.version kmu.version " ]
}

@test "writes each locale through the build, in the order given" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="en"
    run_package
    [ "$status" -eq 0 ]
    [ "$(cat "$GRADLE_CALL_LOG")" = "writeLocaleFiles -Plocale=en --no-daemon --console=plain
writeLocaleFiles -Plocale=zh-hans --no-daemon --console=plain" ]
}

@test "zips each locale with that locale's data files and launcher file" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="en"
    run_package
    [ "$status" -eq 0 ]
    [ "$(jq -r '.text' <<< "$(read_zipped_file KMU-1.2.3-zh-hans.zip data/strings/strings.json)")" = "zh-hans" ]
    [ "$(jq -r '.name' <<< "$(read_zipped_file KMU-1.2.3-zh-hans.zip mod_info.json)")" = "KMU zh-hans" ]
    [ "$(jq -r '.text' <<< "$(read_zipped_file KMU-1.2.3-en.zip data/strings/strings.json)")" = "en" ]
}

@test "zips each locale's version file under the name version_files.csv gives" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="en"
    run_package
    [ "$status" -eq 0 ]
    versionFile="$(read_zipped_file KMU-1.2.3-zh-hans.zip kmu.version)"
    # The CSV naming the file is one for every locale; what differs is where
    # the copy inside points an install.
    [ "$(jq -r '.masterVersionFile' <<< "$versionFile")" = "https://example.invalid/latest/download/kmu-zh-hans.version" ]
    [[ "$(jq -r '.directDownloadURL' <<< "$versionFile")" == *"/1.2.3/KMU-1.2.3-zh-hans.zip" ]]
}

@test "publishes each locale's version file as the one its zip carries" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="en"
    run_package
    [ "$status" -eq 0 ]
    [ "$(cat "$OUTPUT/kmu-zh-hans.version")" = "$(read_zipped_file KMU-1.2.3-zh-hans.zip kmu.version)" ]
}

@test "publishes the default locale's version file under the unsuffixed name" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="en"
    run_package
    [ "$status" -eq 0 ]
    # What an install released before locales existed still polls.
    [ "$(cat "$OUTPUT/kmu.version")" = "$(cat "$OUTPUT/kmu-en.version")" ]
}

@test "ships the runtime payload and nothing the build keeps beside it" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="en"
    run_package
    [ "$status" -eq 0 ]
    zipped="$ZIP_CAPTURE_DIR/KMU-1.2.3-en.zip/KMU"
    [ -f "$zipped/jars/KMU.jar" ]
    [ -f "$zipped/README.md" ]
    [ ! -e "$zipped/mod_info.base.json" ]
    [ ! -e "$zipped/localisation" ]
    [ ! -e "$zipped/src" ]
}

@test "leaves out of a locale's zip a file only an earlier locale's payload held" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="en"
    # The first pass's payload is where a stale file would come from.
    mkdir -p "$CHECKOUT/dist/KMU"
    echo "stale" > "$CHECKOUT/dist/KMU/stale.txt"
    run_package
    [ "$status" -eq 0 ]
    [ ! -e "$ZIP_CAPTURE_DIR/KMU-1.2.3-en.zip/KMU/stale.txt" ]
}

@test "fails when the default locale is not among the locales" {
    export LOCALES="$TWO_LOCALES" DEFAULT_LOCALE="fr"
    run_package
    [ "$status" -ne 0 ]
    [[ "$output" == *"default locale 'fr' is not among the locales"* ]]
}

@test "fails on locales that are not a JSON array" {
    export LOCALES="{}" DEFAULT_LOCALE="en"
    run_package
    [ "$status" -ne 0 ]
    [[ "$output" == *"LOCALES is not a JSON array"* ]]
}

@test "fails when the launcher file was never written" {
    rm "$CHECKOUT/mod_info.json"
    run_package
    # A base with no build behind it ships no file the game can load.
    [ "$status" -ne 0 ]
    [[ "$output" == *"mod_info.json not found"* ]]
}

@test "fails when the mod root does not exist" {
    export MOD_ROOT="absent"
    run_package
    [ "$status" -ne 0 ]
    [[ "$output" == *"mod root absent not found"* ]]
}
