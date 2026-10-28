# Finds the data folder whose copy of a file the game reads alone: the first, in search order,
# whose mod_info.json lists the file's path under "replace". The game drops every other copy.
# It compares the path case-sensitively. Which slashes match depends on the file:
# a JSON or content file takes the path written with either slash, a spreadsheet only as written.
function Find-ReplacingSource {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # The file's path under a data folder, written with forward slashes.
        [string] $Path,

        # A replaced path written with backslashes matches too.
        [switch] $AcceptsEitherSlash
    )

    $acceptedPaths = if ($AcceptsEitherSlash) {
        @($Path, $Path.Replace('/', '\'))
    } else {
        @($Path)
    }

    foreach ($source in $Sources) {
        foreach ($acceptedPath in $acceptedPaths) {

            if ($source.ReplacedPaths -ccontains $acceptedPath) {
                return $source
            }
        }
    }

    return $null
}
