# Writes a failure's cause as two lines, its exception type and then its message, so a long message never
# runs on from the type. The caller indents them under what they belong to.
function Format-CauseLines {

    param(
        [string] $CauseType,
        [string] $Cause
    )

    return @(
        (Format-ConsoleText -Role 'Code' -Text $CauseType)
        (Format-ConsoleText -Role 'Error' -Text $Cause)
    )
}
