# Writes one line of what a script read or decided to stderr, opening with the script's name.
# Stderr keeps stdout for the script's data, which a caller parses or consumes as objects.
function Write-DiagnosticLine {

    param(
        [string] $Source,
        [string] $Message
    )

    [Console]::Error.WriteLine("${Source}: $Message")
}
