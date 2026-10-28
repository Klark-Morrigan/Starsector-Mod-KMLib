# Writes a script's output to a file, for output that outruns a console's scrollback.
# Creates the file's folder, and writes UTF-8 without a byte order mark, as a JSON reader expects.
# Returns the full path written, for the caller to name on stderr.
function Write-TextFile {

    param(
        # The file to write, absolute or relative to the shell's current folder.
        [string] $Path,

        [string] $Text
    )

    # .NET resolves a relative path against the process's folder, not the shell's.
    $fullPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Path)

    New-Item -ItemType Directory -Path (Split-Path -Parent $fullPath) -Force | Out-Null
    [System.IO.File]::WriteAllText($fullPath, $Text, [System.Text.UTF8Encoding]::new($false))

    return $fullPath
}
