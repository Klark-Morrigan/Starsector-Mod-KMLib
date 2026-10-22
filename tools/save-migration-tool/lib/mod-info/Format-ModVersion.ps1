# Collapses both version shapes to one displayable string: the object form's parts joined by dots, a missing one
# skipped, and anything else, a string or a bare number, as its text.
function Format-ModVersion {

    param([object] $Version)

    if ($null -eq $Version) {
        return ''
    }

    if (Test-VersionObjectForm -Version $Version) {

        $parts = Get-VersionObjectParts -Version $Version

        return @($parts | Where-Object { $null -ne $_ }) -join '.'
    }

    return "$Version"
}
