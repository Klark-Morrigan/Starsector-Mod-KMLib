# The major, minor and patch of the { major, minor, patch } version form, a missing one as null.
function Get-VersionObjectParts {

    param([object] $Version)

    return ,@(
        (Get-JsonPropertyValue -Object $Version -Name 'major'),
        (Get-JsonPropertyValue -Object $Version -Name 'minor'),
        (Get-JsonPropertyValue -Object $Version -Name 'patch')
    )
}
