# Resolves the owners of the given classes in an install,
# from the JARs its roster's mods declare and from the game's own classpath.
# CoreJarCount says how many classpath entries vmparams named, and CoreClasspathProblem why there are none.
function Resolve-ClassOwner {

    param(
        [string] $StarsectorRoot,

        # The install's enabled mods, as Get-ModRoster.ps1 lists them.
        [object[]] $Roster,

        [string[]] $ClassNames
    )

    $coreClasspath = Read-CoreClasspath -StarsectorRoot $StarsectorRoot

    $sources = @([pscustomobject]@{ Owner = (Get-CoreOwnerName); Jars = $coreClasspath.Jars }) +
        @($Roster | ForEach-Object { [pscustomobject]@{ Owner = $_.Id; Jars = $_.Jars } })

    $index = New-ClassOwnerIndex -Sources $sources -ClassNames $ClassNames
    $index | Add-Member -NotePropertyName 'CoreJarCount' -NotePropertyValue @($coreClasspath.Jars).Count
    $index | Add-Member -NotePropertyName 'CoreClasspathProblem' -NotePropertyValue $coreClasspath.Problem

    return $index
}
