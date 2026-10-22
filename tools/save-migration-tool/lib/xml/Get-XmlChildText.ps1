# The text of an element's first child of the given name, or null when there is none or it is empty.
# XStream writes an empty string as an empty element and leaves a null field out, and both mean "no value".
function Get-XmlChildText {

    param(
        [System.Xml.XmlElement] $Element,
        [string] $Name
    )

    $child = $Element.SelectSingleNode($Name)

    if ($null -eq $child -or -not $child.InnerText) {
        return $null
    }

    return $child.InnerText
}
