# A SHA-256 of text, as lowercase hex, for the parts of a cache key.
function Get-TextHash {

    param(
        [string] $Text
    )

    $hash = [System.Security.Cryptography.SHA256]::HashData(
        [System.Text.Encoding]::UTF8.GetBytes($Text))

    return [Convert]::ToHexString($hash).
        ToLowerInvariant()
}
