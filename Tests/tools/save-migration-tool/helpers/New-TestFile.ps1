# Writes a file under the suite's TestDrive and returns its full path, creating the folders on the way.
# The content is given as text, as lines, or as bytes, so a case can also write what no text encoding produces.
function New-TestFile {

    param(
        # The path under TestDrive, such as mods\a\mod_info.json.
        [string] $Name,

        [string] $Text = '',
        [string[]] $Lines,
        [byte[]] $Bytes
    )

    $path = Join-Path $TestDrive $Name

    New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force | Out-Null

    if ($null -ne $Bytes) {
        [System.IO.File]::WriteAllBytes($path, $Bytes)

    } elseif ($null -ne $Lines) {
        [System.IO.File]::WriteAllLines($path, $Lines)

    } else {
        [System.IO.File]::WriteAllText($path, $Text)
    }

    return $path
}
