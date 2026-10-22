# Writes one diagnostic of what a script read or decided to stderr, opening with the script's name.
# Stderr keeps stdout for the script's data, which a caller parses or consumes as objects.
#
# A blank line parts blocks: before a block a caller opens, and wherever the writing script changes,
# as when one script runs another. A message of several lines keeps its later lines under its first.
function Write-DiagnosticLine {

    param(
        [string] $Source,
        [string] $Message,
        [switch] $IsBlockStart
    )

    $domain = [AppDomain]::CurrentDomain
    $slotName = Get-DiagnosticSourceSlotName
    $lastSource = $domain.GetData($slotName)

    if ($null -ne $lastSource -and ($IsBlockStart -or $lastSource -ne $Source)) {
        [Console]::Error.WriteLine()
    }

    $domain.SetData($slotName, $Source)

    $prefix = "${Source}: "
    $body = ($Message -split "`n") -join "`n$(' ' * $prefix.Length)"
    $text = "$(Format-ConsoleText -Role 'Script' -Text "${Source}:") $body"

    [Console]::Error.WriteLine((Resolve-ConsoleText -Text $text -Stream 'Error'))
}
