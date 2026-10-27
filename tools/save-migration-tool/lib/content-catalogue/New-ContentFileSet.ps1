# Describes a set of content files, one spec each, whose JSON key IdKey holds the ID.
# The game finds them either in one folder of a data tree, by how their names end,
# or as the paths a spreadsheet lists, as factions.csv lists every .faction file.
function New-ContentFileSet {

    [CmdletBinding(DefaultParameterSetName = 'Folder')]
    param(
        [Parameter(Mandatory, ParameterSetName = 'Folder')]
        [string] $Folder,

        # How a file's name ends, without the dot, as the game matches it.
        [Parameter(Mandatory, ParameterSetName = 'Folder')]
        [string] $Ending,

        # The game also lists the files one subfolder down.
        [Parameter(ParameterSetName = 'Folder')]
        [switch] $HasSubfolders,

        # A spreadsheet source whose IDs are the files' paths under a data folder.
        [Parameter(Mandatory, ParameterSetName = 'Listing')]
        [object] $ListedBy,

        [Parameter(Mandatory)]
        [string] $IdKey
    )

    return [pscustomobject]@{
        Folder        = $Folder
        Ending        = $Ending
        HasSubfolders = [bool] $HasSubfolders
        ListedBy      = $ListedBy
        IdKey         = $IdKey
    }
}
