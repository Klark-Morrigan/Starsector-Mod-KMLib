# Reads a property a parsed JSON object may lack, as null.
# Strict mode throws on a missing property, and mod authors omit fields freely.
# An object parsed as a hashtable matches the name as its keys do, case-sensitively.
function Get-JsonPropertyValue {

    param(
        [object] $Object,
        [string] $Name
    )

    if ($null -eq $Object) {
        return $null
    }

    if ($Object -is [System.Collections.IDictionary]) {

        # A generic dictionary hides IDictionary.Contains from PowerShell, and an ordered one has no ContainsKey.
        $hasKey = if ($Object.PSObject.Methods['ContainsKey']) {
            $Object.ContainsKey($Name)
        } else {
            $Object.Contains($Name)
        }

        if ($hasKey) {
            return $Object[$Name]
        }

        return $null
    }

    $property = $Object.PSObject.Properties[$Name]

    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}
