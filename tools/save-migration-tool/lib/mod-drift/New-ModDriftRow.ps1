# Says how one mod differs between a save and the install, given its saved entry, its roster entry, or both.
# A mod changed when its version text differs, so a rebuild the game reads as the same version still shows;
# Level then says how the game ranks the change. Level is null for a mod that did not change.
# The name is the one the save wrote, else the installed one.
function New-ModDriftRow {

    param(
        [object] $Saved,
        [object] $Installed
    )

    # The type name formats\ModDriftRow.format.ps1xml selects, so a console prints the rows as a table.
    $rowTypeName = 'SaveMigrationTool.ModDriftRow'

    $changes = Get-ModDriftChangeNames

    $row = [pscustomobject]@{
        PSTypeName       = $rowTypeName
        Id               = $null
        Name             = $null
        SavedVersion     = $null
        InstalledVersion = $null
        Change           = $null
        Level            = $null
    }

    if ($null -ne $Installed) {

        $row.Id = $Installed.Id
        $row.Name = $Installed.Name
        $row.InstalledVersion = $Installed.Version
    }

    if ($null -ne $Saved) {

        $row.Id = $Saved.Id
        $row.SavedVersion = $Saved.Version

        if ($Saved.Name) {
            $row.Name = $Saved.Name
        }
    }

    if ($null -eq $Saved) {
        $row.Change = $changes.Added

    } elseif ($null -eq $Installed) {
        $row.Change = $changes.Missing

    } elseif ($Saved.Version -ceq $Installed.Version) {
        $row.Change = $changes.Unchanged

    } else {
        $row.Change = $changes.Changed
        $row.Level = Get-VersionChangeLevel -Installed $Installed.VersionInfo -Saved $Saved.VersionInfo
    }

    return $row
}
