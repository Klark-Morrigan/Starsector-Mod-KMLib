# Lists the classes a jar holds, under their Java names: entry data/scripts/Foo$Bar.class reads as data.scripts.Foo$Bar.
# XStream names a class the same way in its debugging information, so the two compare as plain strings.
function Read-JarClassNames {

    param([string] $Path)

    $classSuffix = '.class'

    # Holds no class the game loads by this name: multi-release copies and module descriptors.
    $metadataFolder = 'META-INF/'

    $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)

    try {
        foreach ($entry in $archive.Entries) {

            $entryName = $entry.FullName

            if ($entryName.EndsWith($classSuffix) -and -not $entryName.StartsWith($metadataFolder)) {
                $entryName.Substring(0, $entryName.Length - $classSuffix.Length).Replace('/', '.')
            }
        }

    } finally {
        $archive.Dispose()
    }
}
