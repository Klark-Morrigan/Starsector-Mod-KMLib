# Resolves the owners of the given classes in an install,
# from the jars its roster's mods declare and from the game's own classpath.
# CoreJarCount says how many classpath entries vmparams named: none leaves every core class without an owner.
function Resolve-ClassOwner {

    param(
        [string] $StarsectorRoot,

        # The install's enabled mods, as Get-ModRoster.ps1 lists them.
        [object[]] $Roster,

        [string[]] $ClassNames
    )

    $coreJars = @(Read-CoreClasspath -StarsectorRoot $StarsectorRoot)

    $sources = @([pscustomobject]@{ Owner = (Get-CoreOwnerName); Jars = $coreJars }) +
        @($Roster | ForEach-Object { [pscustomobject]@{ Owner = $_.Id; Jars = $_.Jars } })

    $index = New-ClassOwnerIndex -Sources $sources -ClassNames $ClassNames
    $index | Add-Member -NotePropertyName 'CoreJarCount' -NotePropertyValue $coreJars.Count

    return $index
}
