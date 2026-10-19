# Reads a property a parsed JSON object may lack, as null.
# Strict mode throws on a missing property, and mod authors omit fields freely.
function Get-JsonPropertyValue {

    param(
        [object] $Object,
        [string] $Name
    )

    if ($null -eq $Object) {
        return $null
    }

    $property = $Object.PSObject.Properties[$Name]

    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}
