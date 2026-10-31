# Builds the catalogue of content IDs the install defines, per kind, each with the owners that define it:
# the mod IDs, and the core owner for the game's own content, in the order the game searches them.
# It holds the IDs the files define and those the game builds from them at startup.
# Files and spreadsheets that do not parse are skipped and returned with the reason,
# and each file or spreadsheet a mod replaces is returned with that mod.
# A file several kinds read is returned once however many kinds replaced it, and once per reason they skipped it.
# Each kind read is returned with how long it took and how many IDs its files define, so a slow run can be traced.
# Each spreadsheet and keyed JSON file is parsed once, and counted once, however many kinds read it.
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

    $cache = New-ParsedFileCache
    $ownersByReadKind = @{}
    $skippedFiles = [System.Collections.Generic.List[object]]::new()
    $skipKeys = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $replacements = [System.Collections.Generic.List[object]]::new()
    $replacedPaths = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    $kindReads = [System.Collections.Generic.List[object]]::new()
    $contentFileCount = 0

    foreach ($definition in $definitions) {

        if (-not $readKinds.Contains($definition.Kind)) {
            continue
        }

        $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
        $read = Read-KindContentOwners -Sources $Sources -Definition $definition -Cache $cache

        $kindReads.Add([pscustomobject]@{
            Kind         = $definition.Kind
            Milliseconds = $stopwatch.ElapsedMilliseconds
            IdCount      = $read.OwnersById.Count
        })

        $ownersByReadKind[$definition.Kind] = $read.OwnersById
        $contentFileCount += $read.ContentFileCount

        foreach ($skippedFile in $read.SkippedFiles) {

            # A path holds no line break, so the key keeps path and reason apart.
            if ($skipKeys.Add("$($skippedFile.Path)`n$($skippedFile.Reason)")) {
                $skippedFiles.Add($skippedFile)
            }
        }

        foreach ($replacement in $read.Replacements) {

            if ($replacedPaths.Add($replacement.Path)) {
                $replacements.Add($replacement)
            }
        }
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
        ContentFileCount = $contentFileCount + $cache.JsonByPath.Count
        SpreadsheetCount = $cache.SpreadsheetsByKey.Count
        SkippedFiles     = $skippedFiles.ToArray()
        Replacements     = $replacements.ToArray()
        KindReads        = $kindReads.ToArray()
    }
}
