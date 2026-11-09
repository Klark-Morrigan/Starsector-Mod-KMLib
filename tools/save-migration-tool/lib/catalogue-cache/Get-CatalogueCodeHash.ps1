# A hash of the code that builds the catalogue, for the cache key:
# a change to how the catalogue reads the files can then never be served a result read the old way.
# It covers every file under the folders the catalogue's reading and its cache live in,
# with each file's path under lib\.
function Get-CatalogueCodeHash {

    $libRoot = Split-Path -Parent $PSScriptRoot

    $codeFolders = @(
        'catalogue-cache',
        'content',
        'content-catalogue',
        'data-file',
        'install')

    $codeFiles = foreach ($codeFolder in $codeFolders) {

        Get-ChildItem `
            -LiteralPath (Join-Path $libRoot $codeFolder) `
            -File `
            -Recurse
    }

    $fileLines = [string[]] @(foreach ($codeFile in $codeFiles) {

        $relativePath = [System.IO.Path]::GetRelativePath($libRoot, $codeFile.FullName).
            Replace('\', '/')

        $contentHash = [Convert]::ToHexString(
            [System.Security.Cryptography.SHA256]::HashData(
                [System.IO.File]::ReadAllBytes($codeFile.FullName)))

        "$relativePath $contentHash"
    })

    # The file system lists files in its own order, which another machine need not share.
    [System.Array]::Sort(
        $fileLines,
        [System.StringComparer]::Ordinal)

    return Get-TextHash -Text ($fileLines -join "`n")
}
