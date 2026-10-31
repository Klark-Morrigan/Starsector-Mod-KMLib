# Describes one part of a kind's definition: an ID any of its sources defines is defined.
# A part with both content files and spreadsheets needs both: an ID counts only when a file defines it
# and a row of any of its spreadsheets does, as the game drops a hull, weapon, ship system or skill
# that has a file and no row.
function New-ContentPart {

    param(
        # File sets, as New-ContentFileSet describes them.
        [object[]] $FileSets = @(),

        # Spreadsheet sources, as New-SpreadsheetSource describes them.
        [object[]] $Spreadsheets = @(),

        # JSON files keyed by ID, as New-KeyedFileSource describes them.
        [object[]] $KeyedFiles = @(),

        # IDs the game builds in its own code, owned by the game.
        [string[]] $BuiltInIds = @()
    )

    return [pscustomobject]@{
        FileSets     = $FileSets
        Spreadsheets = $Spreadsheets
        KeyedFiles   = $KeyedFiles
        BuiltInIds   = $BuiltInIds
    }
}
