BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\content-catalogue\readers\Get-ContentSources.ps1"
    . "$libRoot\content-catalogue\New-ContentCatalogue.ps1"
    . "$libRoot\content-catalogue\Read-ContentCatalogue.ps1"
}

Describe 'Read-ContentCatalogue' {

    BeforeEach {

        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()
        $script:OwnersByKind = [ordered]@{
            Hull    = New-OwnerIndex @{ 'a' = @('alpha'); 'b' = @('core') }
            Hullmod = New-OwnerIndex @{ 'c' = @('core') }
        }

        Mock Write-DiagnosticLine { $script:Diagnostics.Add("${Source}: $Message") }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Get-ContentSources { 'alpha source'; 'core source' }
        Mock New-ContentCatalogue {
            [pscustomobject]@{
                OwnersByKind     = $script:OwnersByKind
                GeneratedCount   = 1
                ContentFileCount = 120
                SpreadsheetCount = 4
                SkippedFiles     = @([pscustomobject]@{ Path = 'C:\mods\alpha\data\hulls\bad.ship'; Reason = 'broken' })
                Replacements     = @([pscustomobject]@{ Spreadsheet = 'data/hulls/ship_data.csv'; Owner = 'alpha' })
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

    It 'says what it catalogues, each file it skipped, each replaced spreadsheet and what each kind holds' {

        # ARRANGE
        $expectedStart = (
            "Test: cataloguing Hull, Hullmod from 1 mods and the game's own data, in the game's search order")
        $expectedSkip = 'Test: skipped C:\mods\alpha\data\hulls\bad.ship: broken'
        $expectedReplacement = 'Test: alpha replaces data/hulls/ship_data.csv, so no other copy of it is read'
        $expectedSummary = (
            'Test: read 120 content files and 4 spreadsheets in * ms, skipping 1; ' +
            'defined: 2 Hull, 1 Hullmod, 1 of them built by the game at startup')
        $expectedCount = 4

        # ACT
        Read-ContentCatalogue -StarsectorRoot 'C:\Games' -Roster @('one mod') -Kinds @('Hull', 'Hullmod') `
            -DiagnosticSource 'Test' | Out-Null

        # ASSERT
        $script:Diagnostics[0] | Should -Be $expectedStart
        $script:Diagnostics[1] | Should -Be $expectedSkip
        $script:Diagnostics[2] | Should -Be $expectedReplacement
        $script:Diagnostics[3] | Should -BeLike $expectedSummary
        $script:Diagnostics.Count | Should -Be $expectedCount
    }
}
