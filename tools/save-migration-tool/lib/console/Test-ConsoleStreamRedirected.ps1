# Says whether the process's stdout or stderr goes somewhere other than a console: a file, or a pipe into another program.
function Test-ConsoleStreamRedirected {

    param(
        [ValidateSet('Output', 'Error')]
        [string] $Stream
    )

    if ($Stream -eq 'Error') {
        return [Console]::IsErrorRedirected
    }

    return [Console]::IsOutputRedirected
}
