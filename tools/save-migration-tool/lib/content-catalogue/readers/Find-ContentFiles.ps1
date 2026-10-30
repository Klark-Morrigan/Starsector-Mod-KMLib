# Lists the content files of one file set as the game reads them: one entry per path under a data folder,
# with the copy of it each source holds, in search order.
# The game lists the set's folder, then each subfolder any source holds, in the order it first finds them.
# It matches a file by how its name ends, case-sensitively and without the dot,
# and skips hidden subfolders and those whose name opens with a dot.
function Find-ContentFiles {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # One file set of Get-ContentDefinitions.
        [object] $FileSet
    )

    $hiddenFolderPrefix = '.'

    # Each folder of the set, with the sources that hold it, so no source is asked for a folder it lacks.
    $folders = [System.Collections.Generic.List[string]]::new()
    $sourcesByFolder = [System.Collections.Generic.Dictionary[string, System.Collections.Generic.List[object]]]::new()

    $folders.Add($FileSet.Folder)
    $sourcesByFolder.Add($FileSet.Folder, [System.Collections.Generic.List[object]]::new())

    foreach ($source in $Sources) {

        $folder = [System.IO.DirectoryInfo]::new([System.IO.Path]::Combine($source.Folder, $FileSet.Folder))

        if (-not $folder.Exists) {
            continue
        }

        $sourcesByFolder[$FileSet.Folder].Add($source)

        if (-not $FileSet.HasSubfolders) {
            continue
        }

        foreach ($subfolder in $folder.EnumerateDirectories()) {

            $isHidden = $subfolder.Attributes.HasFlag([System.IO.FileAttributes]::Hidden) -or
                $subfolder.Name.StartsWith($hiddenFolderPrefix)

            if ($isHidden) {
                continue
            }

            $subfolderPath = "$($FileSet.Folder)/$($subfolder.Name)"

            if (-not $sourcesByFolder.ContainsKey($subfolderPath)) {

                $folders.Add($subfolderPath)
                $sourcesByFolder.Add($subfolderPath, [System.Collections.Generic.List[object]]::new())
            }

            $sourcesByFolder[$subfolderPath].Add($source)
        }
    }

    $files = [System.Collections.Generic.List[object]]::new()
    $filesByPath = [System.Collections.Generic.Dictionary[string, object]]::new()

    foreach ($dataFolder in $folders) {
        foreach ($source in $sourcesByFolder[$dataFolder]) {

            # GetFullPath turns the data folder's slashes into the system's, so a path reads as one.
            $folderPath = [System.IO.Path]::GetFullPath($dataFolder, $source.Folder)

            foreach ($path in [System.IO.Directory]::EnumerateFiles($folderPath)) {

                $fileName = [System.IO.Path]::GetFileName($path)

                if (-not $fileName.EndsWith($FileSet.Ending, [System.StringComparison]::Ordinal)) {
                    continue
                }

                $relativePath = "$dataFolder/$fileName"

                # The indexer answers a missing path with null, at a fraction of a TryGetValue call's cost.
                $file = $filesByPath[$relativePath]

                if ($null -eq $file) {

                    $file = [pscustomobject]@{
                        RelativePath = $relativePath
                        Copies       = [System.Collections.Generic.List[object]]::new()
                    }

                    $files.Add($file)
                    $filesByPath.Add($relativePath, $file)
                }

                $file.Copies.Add([pscustomobject]@{ Source = $source; Path = $path })
            }
        }
    }

    return $files.ToArray()
}
