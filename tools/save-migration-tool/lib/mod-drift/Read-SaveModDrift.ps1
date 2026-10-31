# Compares the mods a save's descriptor was last written with against the installed roster.
# Rows holds one drift row per mod on either side. When nothing could be compared, Rows is null
# and Problem says why: the descriptor is gone, or it has no enabledMods list.
# No list says nothing of the save's mods, where an empty list says it had none.
# The caller decides whether a problem warns or fails.
# Says on stderr what it read and decided, under the caller's script name.
function Read-SaveModDrift {

    param(
        [string] $StarsectorRoot,

        # The save's folder name under saves\.
        [string] $SaveFolder,

        # The install's enabled mods, as Read-ModRoster lists them.
        [object[]] $Roster,

        # The script name each diagnostic line opens with.
        [string] $DiagnosticSource
    )

    $descriptorPath = Get-SaveDescriptorPath -StarsectorRoot $StarsectorRoot -SaveFolder $SaveFolder

    if (-not (Test-Path -LiteralPath $descriptorPath -PathType Leaf)) {
        return [pscustomobject]@{ Rows = $null; Problem = "descriptor not found: $descriptorPath" }
    }

    $descriptor = Read-XmlDocument -Path $descriptorPath
    $idIndex = New-XStreamIdIndex -Document $descriptor
    $saveData = $descriptor.DocumentElement

    $savedAt = Format-ConsoleText -Role 'Time' -Text (Get-XmlChildText -Element $saveData -Name 'saveDate')
    $gameVersion = Format-ConsoleText -Role 'Mod' -Text (Get-XmlChildText -Element $saveData -Name 'gameVersion')

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleText -Role 'Path' -Text $descriptorPath), " +
        "saved $savedAt under Starsector $gameVersion; " +
        "$(Format-ConsoleNumber $idIndex.Count) objects carry an ID for back-pointers")

    $savedMods = Read-SaveModList -SaveData $saveData -ListName 'enabledMods' -IdIndex $idIndex

    if ($null -eq $savedMods) {
        return [pscustomobject]@{ Rows = $null; Problem = "the descriptor lists no enabled mods: $descriptorPath" }
    }

    $everEnabledMods = Read-SaveModList -SaveData $saveData -ListName 'allModsEverEnabled' -IdIndex $idIndex

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleNumber $savedMods.Count) enabled mods as the save last wrote them; " +
        (Format-DroppedModSummary -SavedMods $savedMods -EverEnabledMods $everEnabledMods))

    $rows = @(Compare-SaveModRoster -SavedMods $savedMods -Roster $Roster)

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "compared them with $(Format-ConsoleNumber @($Roster).Count) installed mods: " +
        (Format-ModDriftCounts -Rows $rows))

    return [pscustomobject]@{ Rows = $rows; Problem = $null }
}
