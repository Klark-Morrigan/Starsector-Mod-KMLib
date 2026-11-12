# Describes one place a campaign.xml names content of one kind by ID: an element path, and where on its last element
# the ID sits. Its name writes the path as the save does, so a reader finds the site in the file by it:
# wpn/e/st[2] is a slot's weapon, CIStack[t=WEAPONS]/d a weapon stack, savedVariant@hId a variant's hull,
# stats/s{keys} the keys of a JSON object in the text.
function New-SaveReferenceSite {

    param(
        [string] $Kind,

        # Element names from the outermost to the one holding the ID. Each matches an element's own name or the class
        # its cl attribute names; * matches any element.
        [string[]] $Path,

        # The attribute holding the ID; the element's text holds it when left out.
        [string] $Attribute,

        # The first element of the path must carry this attribute with one of these values.
        [string] $FilterAttribute,
        [string[]] $FilterValues = @(),

        # The last element's place among its parent's children of the same name, from 1.
        [int] $Position,

        # The text is a JSON object whose keys are the IDs.
        [switch] $AreJsonKeysIds,

        # The text is a JSON object, and the value under this key is the ID.
        [string] $JsonKey
    )

    $elementNames = [string[]] $Path.Clone()

    if ($FilterAttribute) {
        $elementNames[0] += "[$FilterAttribute=$($FilterValues -join ',')]"
    }

    if ($Position -gt 0) {
        $elementNames[-1] += "[$Position]"
    }

    $name = $elementNames -join '/'

    if ($Attribute) {
        $name += "@$Attribute"
    }

    if ($AreJsonKeysIds) {
        $name += '{keys}'

    } elseif ($JsonKey) {
        $name += "{$JsonKey}"
    }

    return [pscustomobject]@{
        Kind = $Kind
        Name = $name
        Path = $Path
        Attribute = $Attribute
        FilterAttribute = $FilterAttribute
        FilterValues = $FilterValues
        Position = $Position
        AreJsonKeysIds = [bool] $AreJsonKeysIds
        JsonKey = $JsonKey
    }
}
