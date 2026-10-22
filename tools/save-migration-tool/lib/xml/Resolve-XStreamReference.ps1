# Follows an XStream back-pointer: an element with ref="N" stands for the element tagged z="N".
# An element with no ref holds its object itself.
# A ref no element answers means a damaged file, so it fails rather than read the object as empty.
function Resolve-XStreamReference {

    param(
        [System.Xml.XmlElement] $Element,
        [System.Collections.IDictionary] $IdIndex
    )

    $id = $Element.GetAttribute('ref')

    if (-not $id) {
        return $Element
    }

    if (-not $IdIndex.ContainsKey($id)) {
        throw "<$($Element.Name) ref=`"$id`"> points at no object: no element has z=`"$id`""
    }

    return $IdIndex[$id]
}
