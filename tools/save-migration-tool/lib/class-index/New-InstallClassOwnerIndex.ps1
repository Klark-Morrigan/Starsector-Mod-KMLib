# Builds an install's index of the owners of the given classes,
# from the JARs its roster's mods declare and from the game's own classpath.
# CoreClasspathEntryCount says how many classpath entries vmparams named, and CoreClasspathProblem why there are none.
function New-InstallClassOwnerIndex {

    param(
        [string] $StarsectorRoot,

        # The install's enabled mods, as Read-ModRoster lists them.
        [object[]] $Roster,

        [string[]] $ClassNames
    )

    $coreClasspath = Read-CoreClasspath -StarsectorRoot $StarsectorRoot

    $sources = @([pscustomobject]@{ Owner = (Get-CoreOwnerName); Jars = $coreClasspath.Jars }) +
        @($Roster | ForEach-Object { [pscustomobject]@{ Owner = $_.Id; Jars = $_.Jars } })

    $index = New-ClassOwnerIndex -Sources $sources -ClassNames $ClassNames
    $index | Add-Member -NotePropertyName 'CoreClasspathEntryCount' -NotePropertyValue @($coreClasspath.Jars).Count
    $index | Add-Member -NotePropertyName 'CoreClasspathProblem' -NotePropertyValue $coreClasspath.Problem

    return $index
}
