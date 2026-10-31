# Reads a data file's text the way the game does: as UTF-8, whatever its encoding.
# A byte order mark stays as a character, where ReadAllText would drop it unseen. The game keeps it too,
# so a reader can refuse or keep it as the game does.
function Read-StarsectorText {

    param([string] $Path)

    return [System.Text.Encoding]::UTF8.GetString([System.IO.File]::ReadAllBytes($Path))
}
