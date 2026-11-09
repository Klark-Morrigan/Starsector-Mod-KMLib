# The folder the scripts keep what they cache for one install: under %LOCALAPPDATA%, outside the install,
# so nothing is added to the game's folders. One machine can hold several installs,
# so each takes a folder named by a hash of its full path, which matches as Windows matches paths, ignoring case.
function Get-InstallCacheFolder {

    param(
        [string] $StarsectorRoot
    )

    # Enough of the hash to keep a machine's installs apart, short enough to keep paths short.
    $folderNameLength = 16
    $localAppData = $env:LOCALAPPDATA

    if (-not $localAppData) {
        $localAppData = [Environment]::GetFolderPath(
            [Environment+SpecialFolder]::LocalApplicationData)
    }

    $installPath = [System.IO.Path]::GetFullPath($StarsectorRoot).
        TrimEnd('\', '/').
        ToLowerInvariant()

    $installHash = Get-TextHash -Text $installPath

    return Join-Path $localAppData 'KMLib\save-migration-tool\cache' `
        -AdditionalChildPath ($installHash.Substring(0, $folderNameLength))
}
