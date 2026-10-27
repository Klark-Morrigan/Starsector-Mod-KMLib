# Says in one line how one mod that differs between a save and the install differs:
# changed, with both versions and the level of the change, missing, or added.
# A missing mod is red and an added one amber, as the game's load list colours them.
function Format-ModDriftLine {

    param([object] $Row)

    $changes = Get-ModDriftChangeNames

    $modId = Format-ConsoleText -Role 'Mod' -Text $Row.Id
    $name = "$($Row.Name)".Trim()
    $mod = if ($name) { "$(Format-ConsoleText -Role 'Mod' -Text $name) ($modId)" } else { $modId }
    $savedVersion = Format-ModDriftVersion -Version $Row.SavedVersion
    $installedVersion = Format-ModDriftVersion -Version $Row.InstalledVersion

    if ($Row.Change -eq $changes.Missing) {
        return "${mod}: $(Format-ConsoleText -Role 'Error' -Text 'missing'), saved at $savedVersion"
    }

    if ($Row.Change -eq $changes.Added) {
        return "${mod}: $(Format-ConsoleText -Role 'Warning' -Text 'added'), installed at $installedVersion"
    }

    return "${mod}: $savedVersion -> $installedVersion, $(Format-VersionChangeLevel -Level $Row.Level)"
}
