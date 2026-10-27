# Builds the catalogue of content IDs the install defines, per kind, each with the owners that define it:
# the mod IDs, and the core owner for the game's own content, in the order the game searches them.
# It holds the IDs the files define and those the game builds from them at startup.
# Files and spreadsheets that do not parse are skipped and returned with the reason,
# and each spreadsheet a mod replaces is returned with that mod.
function New-ContentCatalogue {

    param(
        # The data folders, in the order the game searches them, as Get-ContentSources lists them.
        [object[]] $Sources,

        # The kinds to catalogue, as Get-ContentKindNames names them. Only their files, and those of the kinds
        # they are built from, are read.
        [string[]] $Kinds
    )

    $definitions = @(Get-ContentDefinitions)
    $readKinds = [System.Collections.Generic.HashSet[string]]::new([string[]] $Kinds, [System.StringComparer]::Ordinal)

    foreach ($definition in $definitions) {

        if ($definition.Kind -cnotin $Kinds) {
            continue
        }

        foreach ($rule in $definition.Generated) {
            $readKinds.Add($rule.FromKind) | Out-Null
        }
    }

    $ownersByReadKind = @{}
    $skippedFiles = [System.Collections.Generic.List[object]]::new()
    $replacements = [System.Collections.Generic.List[object]]::new()
    $contentFileCount = 0
    $spreadsheetCount = 0

    foreach ($definition in $definitions) {

        if (-not $readKinds.Contains($definition.Kind)) {
            continue
        }

        $read = Read-KindContentOwners -Sources $Sources -Definition $definition

        $ownersByReadKind[$definition.Kind] = $read.OwnersById
        $contentFileCount += $read.ContentFileCount
        $spreadsheetCount += $read.SpreadsheetCount
        $skippedFiles.AddRange([object[]] $read.SkippedFiles)
        $replacements.AddRange([object[]] $read.Replacements)
    }

    # The game builds only from the IDs files define, so every rule reads those before any rule adds its own.
    $definedIdsByKind = @{}

    foreach ($kind in $ownersByReadKind.Keys) {
        $definedIdsByKind[$kind] = [string[]] $ownersByReadKind[$kind].Keys
    }

    $ownersByKind = [ordered]@{}
    $generatedCount = 0

    foreach ($definition in $definitions) {

        if ($definition.Kind -cnotin $Kinds) {
            continue
        }

        $kindOwners = $ownersByReadKind[$definition.Kind]

        foreach ($rule in $definition.Generated) {

            $generatedCount += Add-GeneratedContent -OwnersById $kindOwners -Rule $rule `
                -BaseIds $definedIdsByKind[$rule.FromKind] -BaseOwners $ownersByReadKind[$rule.FromKind]
        }

        Set-ContentOwnerOrder -OwnersById $kindOwners -Sources $Sources
        $ownersByKind[$definition.Kind] = $kindOwners
    }

    return [pscustomobject]@{
        OwnersByKind     = $ownersByKind
        GeneratedCount   = $generatedCount
        ContentFileCount = $contentFileCount
        SpreadsheetCount = $spreadsheetCount
        SkippedFiles     = $skippedFiles.ToArray()
        Replacements     = $replacements.ToArray()
    }
}
