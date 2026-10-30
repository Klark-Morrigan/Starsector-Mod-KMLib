# Returns an empty cache of the data files one catalogue run parses, for Invoke-CachedRead to fill.
# Several kinds read the same file: sounds.json holds both sounds and music sets,
# and planet_gen_data.csv three kinds of row. The cache parses each copy once for all of them.
# Paths match as Windows matches them, ignoring case.
function New-ParsedFileCache {

    return [pscustomobject]@{
        # Each JSON copy's parse, by its path.
        JsonByPath        = [System.Collections.Generic.Dictionary[string, object]]::new(
            [System.StringComparer]::OrdinalIgnoreCase)

        # Each spreadsheet copy's parse, by its path and whether its curly quotes were straightened.
        SpreadsheetsByKey = [System.Collections.Generic.Dictionary[string, object]]::new(
            [System.StringComparer]::OrdinalIgnoreCase)
    }
}
