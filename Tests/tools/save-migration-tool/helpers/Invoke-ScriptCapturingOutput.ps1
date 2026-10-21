# Runs a script in-process, so coverage sees it, and returns what it wrote to each stream.
# The scripts write their diagnostics to the console's stderr, outside PowerShell's streams,
# so stderr is swapped for a buffer while the script runs. Warnings are collected rather than printed.
function Invoke-ScriptCapturingOutput {

    param(
        [string] $ScriptPath,
        [hashtable] $Arguments
    )

    $stderrBuffer = [System.IO.StringWriter]::new()
    $originalStderr = [Console]::Error

    [Console]::SetError($stderrBuffer)

    try {
        $output = & $ScriptPath @Arguments -WarningVariable warnings -WarningAction SilentlyContinue

    } finally {
        [Console]::SetError($originalStderr)
    }

    return [pscustomobject]@{
        Output      = @($output)
        Warnings    = @($warnings | ForEach-Object { "$_" })
        Diagnostics = @($stderrBuffer.ToString() -split "`r?`n" | Where-Object { $_ })
    }
}
