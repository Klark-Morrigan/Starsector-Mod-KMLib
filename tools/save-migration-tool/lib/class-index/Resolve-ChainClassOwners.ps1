# Finds the mods whose JARs hold the classes object chains name, in the install the log belongs to,
# and says on stderr what it read and why any owner stays unknown.
# Returns each class name with its owners; a class no JAR holds is left out.
function Resolve-ChainClassOwners {

    [CmdletBinding()]
    param(
        [string] $InstallRoot,
        [string[]] $ClassNames,

        # Get-ModRoster.ps1, which lists the install's enabled mods and the JARs each declares.
        [string] $RosterScriptPath,

        # The script name each diagnostic line opens with.
        [string] $DiagnosticSource
    )

    $modsFolderName = 'mods'
    $noOwners = @{}

    if ($ClassNames.Count -eq 0) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            'no listed failure names an object chain; resolving no class owners')

        return $noOwners
    }

    if (-not (Test-Path -LiteralPath (Join-Path $InstallRoot $modsFolderName) -PathType Container)) {

        Write-Warning "no mods folder in the log's install, $InstallRoot; object chain classes stay without owners"

        return $noOwners
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "resolving the owners of $(Format-ConsoleNumber $ClassNames.Count) object chain classes " +
        "from the install at $(Format-ConsoleText -Role 'Path' -Text $InstallRoot)")

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $roster = @(& $RosterScriptPath -StarsectorRoot $InstallRoot)
    $resolved = Resolve-ClassOwner -StarsectorRoot $InstallRoot -Roster $roster -ClassNames $ClassNames

    if ($resolved.CoreClasspathProblem) {
        Write-Warning "$($resolved.CoreClasspathProblem); vanilla classes stay without owners"
    }

    foreach ($skippedJar in $resolved.SkippedJars) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "skipped JAR $(Format-ConsoleText -Role 'Path' -Text $skippedJar.Path): $($skippedJar.Reason)")
    }

    # A mod whose mod_info.json gave no ID declares no JARs here, so its classes cannot resolve to it.
    $parsedStatus = (Get-ModStatusNames).Parsed
    $unknownJarModCount = @($roster | Where-Object { $_.Status -ne $parsedStatus }).Count

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleNumber $resolved.JarCount) JARs of $(Format-ConsoleNumber $roster.Count) mods " +
        "and $(Format-ConsoleNumber $resolved.CoreJarCount) core classpath entries " +
        "in $(Format-ConsoleText -Role 'Time' -Text "$($stopwatch.ElapsedMilliseconds) ms"), " +
        "the JARs of $(Format-ConsoleNumber $unknownJarModCount) unparsed mods unknown; " +
        "owners found for $(Format-ConsoleNumber $resolved.OwnersByClass.Count) classes, " +
        "more than one mod for $(Format-ConsoleNumber $resolved.SharedClassCount), " +
        "mod copies of core classes dropped for $(Format-ConsoleNumber $resolved.ShadowedClassCount)")

    return $resolved.OwnersByClass
}
