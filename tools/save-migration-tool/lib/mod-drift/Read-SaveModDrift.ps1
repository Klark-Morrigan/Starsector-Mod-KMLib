# Compares the mods a save's descriptor was last written with against the installed roster.
# Rows holds one drift row per mod on either side. When nothing could be compared, Rows is null
# and Problem says why: a descriptor with no enabledMods list says nothing of the save's mods,
# where an empty list says it had none.
# Says on stderr what it read and decided, under the caller's script name.
function Read-SaveModDrift {

    param(
        # An existing descriptor.xml.
        [string] $DescriptorPath,

        # The install's enabled mods, as Read-ModRoster lists them.
        [object[]] $Roster,

        # The script name each diagnostic line opens with.
        [string] $DiagnosticSource
    )

    $descriptor = Read-XmlDocument -Path $DescriptorPath
    $idIndex = New-XStreamIdIndex -Document $descriptor
    $saveData = $descriptor.DocumentElement

    $savedAt = Format-ConsoleText -Role 'Time' -Text (Get-XmlChildText -Element $saveData -Name 'saveDate')
    $gameVersion = Format-ConsoleText -Role 'Mod' -Text (Get-XmlChildText -Element $saveData -Name 'gameVersion')

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleText -Role 'Path' -Text $DescriptorPath), " +
        "saved $savedAt under Starsector $gameVersion; " +
        "$(Format-ConsoleNumber $idIndex.Count) objects carry an ID for back-pointers")

    $savedMods = Read-SaveModList -SaveData $saveData -ListName 'enabledMods' -IdIndex $idIndex

    if ($null -eq $savedMods) {
        return [pscustomobject]@{ Rows = $null; Problem = "the descriptor lists no enabled mods: $DescriptorPath" }
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
