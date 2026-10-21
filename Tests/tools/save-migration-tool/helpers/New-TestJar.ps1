# Writes a jar at the given path holding an empty entry under each given name, and returns the path.
# Entries are named as a jar stores them, data/scripts/Foo.class, so a test controls the entries that hold no class too.
function New-TestJar {

    param(
        [string] $Path,
        [string[]] $EntryNames
    )

    New-Item -ItemType Directory -Path (Split-Path -Parent $Path) -Force | Out-Null

    $archive = [System.IO.Compression.ZipFile]::Open($Path, [System.IO.Compression.ZipArchiveMode]::Create)

    try {
        foreach ($entryName in $EntryNames) {
            $archive.CreateEntry($entryName) | Out-Null
        }

    } finally {
        $archive.Dispose()
    }

    return $Path
}
