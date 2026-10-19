# Collapses both version shapes to one displayable string.
#   Depends on Get-JsonPropertyValue and Get-VersionObjectParts being dot-sourced into the same scope.
function Format-ModVersion {

    param([object] $Version)

    if ($null -eq $Version) {
        return ''
    }

    if ($Version -is [string]) {
        return $Version
    }

    if ($null -ne (Get-JsonPropertyValue -Object $Version -Name 'major')) {

        $parts = Get-VersionObjectParts -Version $Version
        return (@($parts | Where-Object { $null -ne $_ }) -join '.')
    }

    return "$Version"
}
