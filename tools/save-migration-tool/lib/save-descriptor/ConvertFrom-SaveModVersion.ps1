# Reads a mod's versionInfo, as a save holds it, into the text Get-ModRoster prints for the same mod_info.json.
# A version that mod_info.json writes as a string keeps that string as <string>.
# A version it writes as { major, minor, patch } has no <string>,
# so the parts are joined as Format-ModVersion joins them.
function ConvertFrom-SaveModVersion {

    param([System.Xml.XmlElement] $VersionInfo)

    if ($null -eq $VersionInfo) {
        return ''
    }

    $text = Get-XmlChildText -Element $VersionInfo -Name 'string'

    if ($text) {
        return $text
    }

    $parts = [pscustomobject]@{
        major = Get-XmlChildText -Element $VersionInfo -Name 'major'
        minor = Get-XmlChildText -Element $VersionInfo -Name 'minor'
        patch = Get-XmlChildText -Element $VersionInfo -Name 'patch'
    }

    return Format-ModVersion -Version $parts
}
