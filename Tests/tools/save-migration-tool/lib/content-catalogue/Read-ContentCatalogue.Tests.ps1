BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\content-catalogue\readers\Get-ContentSources.ps1"
    . "$libRoot\content-catalogue\Format-SkippedFileLines.ps1"
    . "$libRoot\content-catalogue\New-ContentCatalogue.ps1"
    . "$libRoot\content-catalogue\Read-ContentCatalogue.ps1"

    function script:New-KindRead {
        # One kind's read, as New-ContentCatalogue returns it.
        param(
            [string] $Kind,
            [long] $Milliseconds,
            [int] $IdCount
        )

        return [pscustomobject]@{ Kind = $Kind; Milliseconds = $Milliseconds; IdCount = $IdCount }
    }
}

Describe 'Read-ContentCatalogue' {

    BeforeEach {

        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()
        $script:OwnersByKind = [ordered]@{
            Hull    = New-OwnerIndex @{ 'a' = @('alpha'); 'b' = @('core') }
            Hullmod = New-OwnerIndex @{ 'c' = @('core') }
        }
        $script:KindReads = @((New-KindRead 'Hull' 30 2), (New-KindRead 'Hullmod' 50 1))

        Mock Write-DiagnosticLine { $script:Diagnostics.Add("${Source}: $Message") }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Get-ContentSources { 'alpha source'; 'core source' }

        # One line per skipped path, so a case reads back what it was handed.
        Mock Format-SkippedFileLines {
            foreach ($skippedFile in $SkippedFiles) { "skipped line for $($skippedFile.Path)" }
        }
        Mock New-ContentCatalogue {
            [pscustomobject]@{
                OwnersByKind     = $script:OwnersByKind
                GeneratedCount   = 1
                ContentFileCount = 120
                SpreadsheetCount = 4
                SkippedFiles     = @([pscustomobject]@{ Path = 'C:\mods\alpha\data\hulls\bad.ship'; Reason = 'broken' })
                Replacements     = @([pscustomobject]@{ Path = 'data/hulls/ship_data.csv'; Owner = 'alpha' })
                KindReads        = $script:KindReads
            }
        }
    }

    It 'catalogues the asked kinds from the roster''s sources, and returns each kind''s owners and the sources' {

        # ARRANGE
        $expectedSources = @('alpha source', 'core source')

        # ACT
        $actualCatalogue = Read-ContentCatalogue -StarsectorRoot 'C:\Games' -Roster @('one mod') `
            -Kinds @('Hull', 'Hullmod') -DiagnosticSource 'Test'

        # ASSERT
        [object]::ReferenceEquals($actualCatalogue.OwnersByKind, $script:OwnersByKind) | Should -BeTrue
        $actualCatalogue.Sources | Should -Be $expectedSources

        Should -Invoke Get-ContentSources -ParameterFilter {
            $StarsectorRoot -eq 'C:\Games' -and "$Roster" -eq 'one mod'
        }
        Should -Invoke New-ContentCatalogue -ParameterFilter {
            "$Sources" -eq 'alpha source core source' -and "$Kinds" -eq 'Hull Hullmod'
        } -Times 1 -Exactly
    }

    It 'says what it catalogues, what it skipped, each replaced file, what each kind holds and the slowest kinds' {

        # ARRANGE
        $expectedStart = (
            "Test: cataloguing Hull, Hullmod from 1 mods and the game's own data, in the game's search order")
        $expectedSkip = 'Test: skipped line for C:\mods\alpha\data\hulls\bad.ship'
        $expectedReplacement = 'Test: alpha replaces data/hulls/ship_data.csv, so no other copy of it is read'
        $expectedSummary = (
            'Test: read 120 content files and 4 spreadsheets in * ms, skipping 1; ' +
            'defined: 2 Hull, 1 Hullmod, 1 of them built by the game at startup')
        $expectedSlowest = 'Test: slowest to read: Hullmod in 50 ms (1 IDs), Hull in 30 ms (2 IDs)'
        $expectedCount = 5

        # ACT
        Read-ContentCatalogue -StarsectorRoot 'C:\Games' -Roster @('one mod') -Kinds @('Hull', 'Hullmod') `
            -DiagnosticSource 'Test' | Out-Null

        # ASSERT
        $script:Diagnostics[0] | Should -Be $expectedStart
        $script:Diagnostics[1] | Should -Be $expectedSkip
        $script:Diagnostics[2] | Should -Be $expectedReplacement
        $script:Diagnostics[3] | Should -BeLike $expectedSummary
        $script:Diagnostics[4] | Should -Be $expectedSlowest
        $script:Diagnostics.Count | Should -Be $expectedCount
    }

    # 45 kinds would make a long line, and the few slowest are where a slow run spent its time.
    It 'names only the five slowest kinds, slowest first, a tie in the order read' {

        # ARRANGE
        $script:KindReads = @(
            (New-KindRead 'A' 10 1)
            (New-KindRead 'B' 60 1)
            (New-KindRead 'C' 30 1)
            (New-KindRead 'D' 40 1)
            (New-KindRead 'E' 60 1)
            (New-KindRead 'F' 20 1)
        )

        $expectedSlowest = (
            'Test: slowest to read: B in 60 ms (1 IDs), E in 60 ms (1 IDs), D in 40 ms (1 IDs), ' +
            'C in 30 ms (1 IDs), F in 20 ms (1 IDs)')

        # ACT
        Read-ContentCatalogue -StarsectorRoot 'C:\Games' -Roster @() -Kinds @('A') -DiagnosticSource 'Test' | Out-Null

        # ASSERT
        $script:Diagnostics[-1] | Should -Be $expectedSlowest
    }
}
