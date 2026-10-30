# Says whether a parsed JSON object holds a key, whatever its value, null included, as org.json's has() does.
# A parsed object is a hashtable matching keys case-sensitively, as org.json's do.
# Anything else, such as a string where the file should hold an object, holds no keys.
function Test-JsonProperty {

    param(
        [object] $Object,
        [string] $Name
    )

    if ($Object -isnot [System.Collections.IDictionary]) {
        return $false
    }

    # A generic dictionary hides IDictionary.Contains from PowerShell, and an ordered one has no ContainsKey.
    if ($Object.PSObject.Methods['ContainsKey']) {
        return $Object.ContainsKey($Name)
    }

    return $Object.Contains($Name)
}
