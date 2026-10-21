# Lists the classes a jar holds, under their Java names: entry data/scripts/Foo$Bar.class reads as data.scripts.Foo$Bar.
# XStream names a class the same way in its debugging information, so the two compare as plain strings.
# The entries are filtered and renamed as whole arrays: a large mod jar holds thousands of entries.
function Read-JarClassNames {

    param([string] $Path)

    # Holds no class the game loads by this name: multi-release copies and module descriptors.
    $metadataFolder = 'META-INF/'

    $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)

    try {
        $classEntryNames = @($archive.Entries.FullName) -clike '*.class' -cnotlike "$metadataFolder*"

        $classEntryNames -creplace '\.class$', '' -creplace '/', '.'

    } finally {
        $archive.Dispose()
    }
}
