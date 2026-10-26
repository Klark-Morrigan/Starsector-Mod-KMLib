# Reads mod_info.json's version into its parts, the way the game's VersionInfo reads it from that file:
# the { major, minor, patch } object form part by part, and anything else, a string or a bare number, as text.
# No version reads as text too, as the empty text the game reads for a missing key.
function ConvertTo-VersionInfo {

    param([object] $Version)

    if (Test-VersionObjectForm -Version $Version) {

        $parts = Get-VersionObjectParts -Version $Version

        return New-VersionInfo -Major $parts[0] -Minor $parts[1] -Patch $parts[2] -MinorIsRC $false
    }

    return ConvertFrom-VersionText -Text "$Version"
}
