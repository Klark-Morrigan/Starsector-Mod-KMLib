# Maps each XStream object ID in a document to the element that holds the object.
# XStream writes an object in full once, tagged z="N", and writes each later use as an empty element with ref="N".
function New-XStreamIdIndex {

    param([System.Xml.XmlDocument] $Document)

    $index = [System.Collections.Generic.Dictionary[string, System.Xml.XmlElement]]::new()

    foreach ($element in $Document.SelectNodes('//*[@z]')) {
        $index[$element.GetAttribute('z')] = $element
    }

    return $index
}
