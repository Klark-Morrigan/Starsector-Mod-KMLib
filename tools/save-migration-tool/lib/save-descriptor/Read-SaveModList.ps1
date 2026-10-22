# Reads one mod list of a save's descriptor, such as enabledMods, into each mod's ID, name and version.
# Returns null when the descriptor has no such list, so a caller can tell a missing list from an empty one.
function Read-SaveModList {

    param(
        [System.Xml.XmlElement] $SaveData,
        [string] $ListName,
        [System.Collections.IDictionary] $IdIndex
    )

    $list = $SaveData.SelectSingleNode($ListName)

    if ($null -eq $list) {
        return $null
    }

    $entries = (Resolve-XStreamReference -Element $list -IdIndex $IdIndex).SelectNodes('*')

    return ,@(foreach ($entry in $entries) { ConvertFrom-EnabledModData -EnabledModData $entry -IdIndex $IdIndex })
}
