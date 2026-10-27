# The path of a save's descriptor.xml, the small file beside campaign.xml that the game's load list reads.
function Get-SaveDescriptorPath {

    param(
        [string] $StarsectorRoot,

        # The save's folder name under saves\.
        [string] $SaveFolder
    )

    $pathNames = Get-InstallPathNames

    return Join-Path `
        -Path $StarsectorRoot `
        -ChildPath $pathNames.SavesFolder `
        -AdditionalChildPath $SaveFolder, $pathNames.DescriptorFile
}
