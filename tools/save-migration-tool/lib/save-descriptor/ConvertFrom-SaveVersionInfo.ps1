# Reads a mod's versionInfo, as a save holds it, into the parts the game compares versions by.
# The save keeps the parts the game read when it wrote the save, so they are taken as written, never re-read
# from the version text: a version given as { major: 0, minor: 1, patch: 2 } would read back differently.
function ConvertFrom-SaveVersionInfo {

    param([System.Xml.XmlElement] $VersionInfo)

    # How XStream writes a true boolean.
    $trueText = 'true'

    if ($null -eq $VersionInfo) {
        return New-VersionInfo -MinorIsRC $false
    }

    $minorIsRCText = Get-XmlChildText -Element $VersionInfo -Name 'minorIsRC'

    return New-VersionInfo `
        -Major (Get-XmlChildText -Element $VersionInfo -Name 'major') `
        -Minor (Get-XmlChildText -Element $VersionInfo -Name 'minor') `
        -Patch (Get-XmlChildText -Element $VersionInfo -Name 'patch') `
        -MinorIsRC ($minorIsRCText -ceq $trueText)
}
