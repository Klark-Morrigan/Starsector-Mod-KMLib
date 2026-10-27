# Reads a key a parsed JSON object may lack, as null, since mod authors omit fields freely.
# A parsed object is a hashtable matching keys case-sensitively, as org.json's do.
# Anything else, such as a string where the file should hold an object, holds no keys.
function Get-JsonPropertyValue {

    param(
        [object] $Object,
        [string] $Name
    )

    if ($Object -isnot [System.Collections.IDictionary]) {
        return $null
    }

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
