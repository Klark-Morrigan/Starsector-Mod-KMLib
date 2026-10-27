# Lists the installed mods the game loads: for each enabled mod ID, the one folder that wins, at its version.
# Folder names are not a usable key: the same mod ships as both "bmo" and "Building Menu Overhaul-2.1.0".
# mod_info.json's "id" is the identity and "version" the recency; enabled_mods.json says which IDs the game loads.
# Disabled mods and superseded copies stay on disk; the roster only leaves them out.
# An install with no mods folder loads no mods, so its roster is empty, with a warning.
# Says on stderr what it read and decided, under the caller's script name.
function Read-ModRoster {

    [CmdletBinding()]
    param(
        [string] $StarsectorRoot,

        # Every installed mod rather than only those enabled_mods.json lists.
        [switch] $IncludeDisabled,

        # The script name each diagnostic line opens with.
        [string] $DiagnosticSource
    )

    $modsRoot = Join-Path $StarsectorRoot (Get-InstallPathNames).ModsFolder

    if (-not (Test-Path -LiteralPath $modsRoot -PathType Container)) {

        Write-Warning "mods directory not found, listing no mods: $modsRoot"

        return
    }

    # Every path the roster reports is relative to this one, so it is resolved once, whatever form it was given in.
    $installRoot = (Resolve-Path -LiteralPath $StarsectorRoot).Path

    $enabled = Read-EnabledModIds -ModsRoot $modsRoot
    $isEveryModListed = $IncludeDisabled -or -not $enabled.IsListRead
    $enabledSet = [System.Collections.Generic.HashSet[string]]::new(
        [string[]] $enabled.Ids,
        (Get-ModIdComparer))

    $candidates = Read-ModFolderCandidates -ModsRoot $modsRoot -DiagnosticSource $DiagnosticSource
    $parsedStatus = (Get-ModStatusNames).Parsed
    $roster = [System.Collections.Generic.List[object]]::new()
    $supersededCount = 0
    $disabledCount = 0

    foreach ($modGroup in (Group-ModCandidate -Candidates $candidates)) {

        $winner = $modGroup.Winner

        foreach ($loser in $modGroup.Superseded) {

            $supersededCount++
            $winnerCopy = Format-ModCopy -Candidate $winner
            $loserCopy = Format-ModCopy -Candidate $loser
            $modId = Format-ConsoleText -Role 'Mod' -Text $modGroup.Id

            Write-DiagnosticLine -Source $DiagnosticSource -Message (
                "${modId}: $winnerCopy wins over $loserCopy, coming first in folder order")
        }

        # Unparsed entries bypass the enabled filter: their ID is unknown, so the
        # only safe assumption is that they might matter.
        $isListed = $isEveryModListed -or
            $winner.Status -ne $parsedStatus -or
            $enabledSet.Contains($modGroup.Id)

        if (-not $isListed) {

            $disabledCount++
            continue
        }

        $roster.Add((ConvertTo-RosterEntry -ModGroup $modGroup -StarsectorRoot $installRoot))
    }

    $unparsedCount = Get-UnparsedModCount -Roster $roster.ToArray()

    Write-DiagnosticLine -Source $DiagnosticSource -Message (
        "read $(Format-ConsoleNumber $candidates.Count) mod folders " +
        "under $(Format-ConsoleText -Role 'Path' -Text $modsRoot) " +
        "and $(Format-ConsoleNumber $enabled.Ids.Count) enabled IDs; " +
        "listing $(Format-ConsoleNumber $roster.Count) mods ($(Format-ConsoleNumber $unparsedCount) unparsed), " +
        "skipping $(Format-ConsoleNumber $disabledCount) disabled " +
        "and $(Format-ConsoleNumber $supersededCount) superseded copies")

    return $roster.ToArray()
}
