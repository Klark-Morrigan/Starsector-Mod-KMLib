# Builds the parts the game keeps for a mod version, the fields of its VersionInfo, and compares versions by.
# A missing part binds as an empty string, as the game's fields start out, so a comparison never meets a null.
function New-VersionInfo {

    param(
        [string] $Major,
        [string] $Minor,
        [string] $Patch,
        [bool] $MinorIsRC
    )

    return [pscustomobject]@{
        Major     = $Major
        Minor     = $Minor
        Patch     = $Patch
        MinorIsRC = $MinorIsRC
    }
}
