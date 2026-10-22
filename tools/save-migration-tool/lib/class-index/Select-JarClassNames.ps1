# Keeps the given classes a JAR holds, under their Java names: data.scripts.Foo$Bar sits at data/scripts/Foo$Bar.class.
# XStream names a class the same way in its debugging information, so the two compare as plain strings.
# Each wanted class is looked up by its entry path, since a mod JAR holds thousands of entries
# and a failure names a few dozen classes.
function Select-JarClassNames {

    param(
        [string] $Path,
        [string[]] $ClassNames
    )

    $classFileSuffix = '.class'

    $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)

    try {

        foreach ($className in $ClassNames) {

            if ($null -ne $archive.GetEntry($className.Replace('.', '/') + $classFileSuffix)) {
                $className
            }
        }

    } finally {
        $archive.Dispose()
    }
}
