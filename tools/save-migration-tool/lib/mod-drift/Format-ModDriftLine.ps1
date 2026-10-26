# Says in one line how one mod differs between a save and the install.
# The verdict is red for a major change or a missing mod, and amber for a minor change or an added mod.
# A patch change stays unpainted, so the changes that need a look stand out from the many small ones.
function Format-ModDriftLine {

    param([object] $Row)

    $changes = Get-ModDriftChangeNames
    $levels = Get-VersionChangeLevelNames

    $levelPhrases = @{
        $levels.Major = Format-ConsoleText -Role 'Error' -Text 'major change'
        $levels.Minor = Format-ConsoleText -Role 'Warning' -Text 'minor change'
        $levels.Patch = 'patch change'
    }

    $modId = Format-ConsoleText -Role 'Mod' -Text $Row.Id
    $name = "$($Row.Name)".Trim()
    $mod = if ($name) { "$(Format-ConsoleText -Role 'Mod' -Text $name) ($modId)" } else { $modId }
    $savedVersion = Format-ModDriftVersion -Version $Row.SavedVersion
    $installedVersion = Format-ModDriftVersion -Version $Row.InstalledVersion

    switch ($Row.Change) {

        $changes.Changed {
            return "${mod}: $savedVersion -> $installedVersion, $($levelPhrases[$Row.Level])"
        }

        $changes.Missing {
            return "${mod}: $(Format-ConsoleText -Role 'Error' -Text 'missing'), saved at $savedVersion"
        }

        $changes.Added {
            return "${mod}: $(Format-ConsoleText -Role 'Warning' -Text 'added'), installed at $installedVersion"
        }
    }

    return "${mod}: unchanged at $savedVersion"
}
