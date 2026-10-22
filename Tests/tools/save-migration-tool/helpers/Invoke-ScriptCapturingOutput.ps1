. "$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib\Get-DiagnosticSourceSlotName.ps1"

# Runs a script in-process, so coverage sees it, and returns what it wrote to each stream.
# The scripts write their diagnostics to the console's stderr, outside PowerShell's streams,
# so stderr is swapped for a buffer while the script runs. Warnings are collected rather than printed.
# A buffer does not count as redirected to the scripts, so NO_COLOR keeps colour codes out of what the suites compare.
#
# Diagnostics keep their blank lines, which part blocks. The slot naming the last diagnostic's script is cleared
# first, as a run in a process of its own would start, so no earlier run's last line opens a block in this one.
function Invoke-ScriptCapturingOutput {

    param(
        [string] $ScriptPath,
        [hashtable] $Arguments
    )

    $stderrBuffer = [System.IO.StringWriter]::new()
    $originalStderr = [Console]::Error
    $originalNoColor = $env:NO_COLOR

    [AppDomain]::CurrentDomain.SetData((Get-DiagnosticSourceSlotName), $null)
    [Console]::SetError($stderrBuffer)
    $env:NO_COLOR = '1'

    try {
        $output = & $ScriptPath @Arguments -WarningVariable warnings -WarningAction SilentlyContinue

    } finally {
        [Console]::SetError($originalStderr)
        $env:NO_COLOR = $originalNoColor
    }

    $stderrText = $stderrBuffer.ToString().TrimEnd("`r", "`n")

    $diagnostics = if ($stderrText) {
        @($stderrText -split "`r?`n")
    } else {
        @()
    }

    return [pscustomobject]@{
        Output      = @($output)
        Warnings    = @($warnings | ForEach-Object { "$_" })
        Diagnostics = $diagnostics
    }
}
