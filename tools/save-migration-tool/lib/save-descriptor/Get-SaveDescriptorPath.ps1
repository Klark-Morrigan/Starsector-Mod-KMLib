# The path of a save's descriptor.xml, the small file beside campaign.xml that the game's load list reads.
function Get-SaveDescriptorPath {

    param(
        [string] $StarsectorRoot,

        # The save's folder name under saves\.
        [string] $SaveFolder
    )

    return Get-SaveFilePath `
        -StarsectorRoot $StarsectorRoot `
        -SaveFolder $SaveFolder `
        -FileName (Get-InstallPathNames).DescriptorFile
}
