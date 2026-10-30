# Reads a key a parsed JSON object may lack, as null, since mod authors omit fields freely.
function Get-JsonPropertyValue {

    param(
        [object] $Object,
        [string] $Name
    )

    if (Test-JsonProperty -Object $Object -Name $Name) {
        return $Object[$Name]
    }

    return $null
}
