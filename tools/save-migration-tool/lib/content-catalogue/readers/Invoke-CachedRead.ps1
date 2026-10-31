# Runs a read the first time its key is asked for, and hands every later caller the same result.
# A read that fails fails every caller with the same message, so each skips the file for the same reason.
function Invoke-CachedRead {

    param(
        # The results read so far, by key, as New-ParsedFileCache holds them.
        [System.Collections.Generic.Dictionary[string, object]] $Results,

        [string] $Key,

        # The command that reads the file, and its parameters.
        [string] $Command,
        [hashtable] $Parameters
    )

    $result = $Results[$Key]

    if ($null -eq $result) {

        $result = try {
            [pscustomobject]@{ Value = & $Command @Parameters; Failure = $null }

        } catch {
            [pscustomobject]@{ Value = $null; Failure = $_.Exception.Message }
        }

        $Results[$Key] = $result
    }

    if ($null -ne $result.Failure) {
        throw $result.Failure
    }

    return $result.Value
}
