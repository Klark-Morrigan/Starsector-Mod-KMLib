# Finds the mods whose JARs hold the classes object chains name, in the install the log belongs to,
# and says on stderr what it read and why any owner stays unknown.
# Returns each class name with its owners; a class no JAR holds is left out.
function Resolve-ChainClassOwners {

    [CmdletBinding()]
    param(
        [string] $InstallRoot,
        [string[]] $ClassNames,

        # The install's enabled mods, as Read-ModRoster lists them. Read here, and only when a class is asked for,
        # when the caller has not read them already.
        [object[]] $Roster,

        # The script name each diagnostic line opens with.
        [string] $DiagnosticSource
    )

    if ($ClassNames.Count -eq 0) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            'no listed failure names an object chain; resolving no class owners')

        return @{}
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "resolving the owners of $(Format-ConsoleNumber $ClassNames.Count) object chain classes " +
        "from the install at $(Format-ConsoleText -Role 'Path' -Text $InstallRoot)")

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

    if (-not $PSBoundParameters.ContainsKey('Roster')) {

        $modsFolder = Join-Path $InstallRoot (Get-InstallPathNames).ModsFolder

        # The game's own classes still resolve with no mods installed.
        if (Test-Path -LiteralPath $modsFolder -PathType Container) {
            $Roster = @(Read-ModRoster -StarsectorRoot $InstallRoot -DiagnosticSource $DiagnosticSource)

        } else {
            Write-Warning "no mods folder in the log's install, $InstallRoot; mod classes stay without owners"
            $Roster = @()
        }
    }

    $resolved = Resolve-ClassOwner -StarsectorRoot $InstallRoot -Roster $Roster -ClassNames $ClassNames

    if ($resolved.CoreClasspathProblem) {
        Write-Warning "$($resolved.CoreClasspathProblem); vanilla classes stay without owners"
    }

    foreach ($skippedJar in $resolved.SkippedJars) {

        Write-DiagnosticLine -Source $DiagnosticSource -Message (
            "skipped JAR $(Format-ConsoleText -Role 'Path' -Text $skippedJar.Path): $($skippedJar.Reason)")
    }

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleNumber $resolved.ModJarCount) JARs of $(Format-ConsoleNumber $Roster.Count) mods " +
        "and $(Format-ConsoleNumber $resolved.CoreJarCount) " +
        "of $(Format-ConsoleNumber $resolved.CoreClasspathEntryCount) core classpath entries " +
        "in $(Format-ConsoleText -Role 'Time' -Text "$($stopwatch.ElapsedMilliseconds) ms"), " +
        "the JARs of $(Format-ConsoleNumber (Get-UnparsedModCount -Roster $Roster)) unparsed mods unknown; " +
        "owners found for $(Format-ConsoleNumber $resolved.OwnersByClass.Count) classes, " +
        "more than one mod for $(Format-ConsoleNumber $resolved.SharedClassCount), " +
        "mod copies of core classes dropped for $(Format-ConsoleNumber $resolved.ShadowedClassCount)")

    return $resolved.OwnersByClass
}
