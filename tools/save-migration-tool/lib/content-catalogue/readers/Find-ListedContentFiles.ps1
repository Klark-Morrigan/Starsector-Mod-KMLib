# Finds the copy each source holds of the given files, for files the game reads by path rather than by folder.
# Returns one entry per path any source holds, in the order given, shaped as Find-ContentFiles shapes its own.
# Each source's copy of every folder the paths name is listed once, and its files matched against the paths:
# asking every source for every path runs to a hundred thousand lookups on a large install.
# Paths match as Windows matches them, ignoring case and slash direction.
function Find-ListedContentFiles {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # Paths under a data folder, as the game names them.
        [string[]] $RelativePaths
    )

    $copiesByPath = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[object]]]::new(
        [System.StringComparer]::OrdinalIgnoreCase)
    $folders = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)

    foreach ($relativePath in $RelativePaths) {

        $normalisedPath = $relativePath.Replace('\', '/')

        if (-not $copiesByPath.ContainsKey($normalisedPath)) {
            $copiesByPath.Add($normalisedPath, [System.Collections.Generic.List[object]]::new())
        }

        $folders.Add($normalisedPath.Substring(0, [Math]::Max(0, $normalisedPath.LastIndexOf('/')))) | Out-Null
    }

    foreach ($source in $Sources) {
        foreach ($folder in $folders) {

            # GetFullPath turns the folder's slashes into the system's, so a path reads as one.
            $folderPath = [System.IO.Path]::GetFullPath($folder, $source.Folder)

            if (-not [System.IO.Directory]::Exists($folderPath)) {
                continue
            }

            foreach ($path in [System.IO.Directory]::EnumerateFiles($folderPath)) {

                $listedPath = "$folder/$([System.IO.Path]::GetFileName($path))".TrimStart('/')

                # The indexer answers a missing path with null, at a fraction of a TryGetValue call's cost.
                $copies = $copiesByPath[$listedPath]

                if ($null -ne $copies) {
                    $copies.Add([pscustomobject]@{ Source = $source; Path = $path })
                }
            }
        }
    }

    return @(foreach ($relativePath in $RelativePaths) {

        $copies = $copiesByPath[$relativePath.Replace('\', '/')]

        if ($copies.Count -gt 0) {
            [pscustomobject]@{ RelativePath = $relativePath; Copies = $copies }
        }
    })
}
