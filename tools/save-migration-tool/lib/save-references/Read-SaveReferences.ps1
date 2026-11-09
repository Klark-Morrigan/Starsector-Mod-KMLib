# Reads every content ID a campaign.xml names at the given sites, in one streaming pass of the file.
# Returns one reference per kind and ID, with how many times the save names it, in all and per site,
# how many elements the pass read, and how many values each site held that it could not read.
#
# The pass runs in compiled code beside this file, which the first call of a session loads.
function Read-SaveReferences {

    param(
        [string] $Path,

        # The sites to read, as Get-SaveReferenceSites lists them.
        [object[]] $Sites
    )

    $scannerTypeName = 'KMLib.SaveMigrationTool.SaveReferenceScanner'

    if (-not ($scannerTypeName -as [type])) {
        Add-Type -Path (Join-Path $PSScriptRoot 'SaveReferenceScanner.cs')
    }

    $scannerSites = [KMLib.SaveMigrationTool.SaveReferenceSite[]] @(foreach ($site in $Sites) {

        $scannerSite = [KMLib.SaveMigrationTool.SaveReferenceSite]::new()
        $scannerSite.Kind = $site.Kind
        $scannerSite.Name = $site.Name
        $scannerSite.Path = $site.Path
        $scannerSite.Position = $site.Position
        $scannerSite.AreJsonKeysIds = $site.AreJsonKeysIds

        # Empty text reads as no attribute, no filter and no key, as the scanner reads null.
        if ($site.Attribute) {
            $scannerSite.Attribute = $site.Attribute
        }

        if ($site.FilterAttribute) {
            $scannerSite.FilterAttribute = $site.FilterAttribute
            $scannerSite.FilterValues = $site.FilterValues
        }

        if ($site.JsonKey) {
            $scannerSite.JsonKey = $site.JsonKey
        }

        $scannerSite
    })

    $fullPath = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Path)
    $scan = [KMLib.SaveMigrationTool.SaveReferenceScanner]::Scan(
        $fullPath,
        $scannerSites)

    $references = @(foreach ($kindUses in $scan.UsesByKind.GetEnumerator()) {

        foreach ($idUses in $kindUses.Value.GetEnumerator()) {

            $useCount = 0

            foreach ($siteCount in $idUses.Value.Values) {
                $useCount += $siteCount
            }

            [pscustomobject]@{
                Kind = $kindUses.Key
                Id = $idUses.Key
                UseCount = $useCount
                UseCountBySite = $idUses.Value
            }
        }
    })

    return [pscustomobject]@{
        References = $references
        ElementCount = $scan.ElementCount
        UnreadableCountBySite = $scan.UnreadableCountBySite
    }
}
