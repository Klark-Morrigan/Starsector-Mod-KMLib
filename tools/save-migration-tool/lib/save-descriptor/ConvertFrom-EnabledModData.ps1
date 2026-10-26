# Reads one EnabledModData entry of a save's descriptor into the mod's ID, name and version,
# as the save last wrote them, and the parts the game compares that version by.
# The descriptor writes each mod's spec in full under allModsEverEnabled first,
# so an entry under enabledMods is usually a back-pointer to it.
# The entry, its spec and the spec's version are each followed wherever they point.
function ConvertFrom-EnabledModData {

    param(
        [System.Xml.XmlElement] $EnabledModData,
        [System.Collections.IDictionary] $IdIndex
    )

    $entry = Resolve-XStreamReference -Element $EnabledModData -IdIndex $IdIndex
    $specElement = $entry.SelectSingleNode('spec')

    if ($null -eq $specElement) {
        throw "<$($entry.Name) z=`"$($entry.GetAttribute('z'))`"> holds no mod spec"
    }

    $spec = Resolve-XStreamReference -Element $specElement -IdIndex $IdIndex
    $versionInfo = $spec.SelectSingleNode('versionInfo')

    if ($null -ne $versionInfo) {
        $versionInfo = Resolve-XStreamReference -Element $versionInfo -IdIndex $IdIndex
    }

    $mod = [pscustomobject]@{
        Id          = Get-XmlChildText -Element $spec -Name 'id'
        Version     = ConvertFrom-SaveModVersion -VersionInfo $versionInfo
        VersionInfo = ConvertFrom-SaveVersionInfo -VersionInfo $versionInfo
        Name        = Get-XmlChildText -Element $spec -Name 'name'
    }

    return Add-ModDefaultDisplay -Mod $mod
}
