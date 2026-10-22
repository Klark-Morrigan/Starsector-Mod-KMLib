# Writes a JAR at the given path holding an empty entry for each entry name and each class, and returns the path.
# EntryNames are named as a JAR stores them, data/config/x.json, so a test controls the entries that hold no class.
# ClassNames are Java names, a.b.C, stored as a/b/C.class.
function New-TestJar {

    param(
        [string] $Path,
        [string[]] $EntryNames = @(),
        [string[]] $ClassNames = @()
    )

    New-Item -ItemType Directory -Path (Split-Path -Parent $Path) -Force | Out-Null

    $archive = [System.IO.Compression.ZipFile]::Open($Path, [System.IO.Compression.ZipArchiveMode]::Create)

    try {
        foreach ($entryName in $EntryNames + @($ClassNames | ForEach-Object { "$($_.Replace('.', '/')).class" })) {
            $archive.CreateEntry($entryName) | Out-Null
        }

    } finally {
        $archive.Dispose()
    }

    return $Path
}
