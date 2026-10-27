BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\install\Get-InstallPathNames.ps1"
    . "$libRoot\mod-roster\ConvertTo-RosterEntry.ps1"
    . "$libRoot\mod-roster\Format-ModCopy.ps1"
    . "$libRoot\mod-roster\Get-ModIdComparer.ps1"
    . "$libRoot\mod-roster\Get-ModStatusNames.ps1"
    . "$libRoot\mod-roster\Get-UnparsedModCount.ps1"
    . "$libRoot\mod-roster\Group-ModCandidate.ps1"
    . "$libRoot\mod-roster\Read-EnabledModIds.ps1"
    . "$libRoot\mod-roster\Read-ModFolderCandidates.ps1"
    . "$libRoot\mod-roster\Read-ModRoster.ps1"

    function script:New-Group {
        # One mod ID's copies as Group-ModCandidate groups them: the winner and the copies it beat.
        param(
            [string] $Id,
            [string] $Status = 'P',
            [object[]] $Superseded = @()
        )

        $winner = [pscustomobject]@{ Id = $Id; Status = $Status }

        return [pscustomobject]@{ Id = $Id; Winner = $winner; Superseded = $Superseded }
    }
}

Describe 'Read-ModRoster' {

    BeforeEach {

        $script:InstallRoot = Join-Path $TestDrive 'install'
        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()

        New-Item -ItemType Directory -Path (Join-Path $script:InstallRoot 'mods') -Force | Out-Null

        Mock Write-DiagnosticLine { $script:Diagnostics.Add("${Source}: $Message") }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Get-InstallPathNames { [pscustomobject]@{ ModsFolder = 'mods' } }
        Mock Get-ModIdComparer { [System.StringComparer]::Ordinal }
        Mock Get-ModStatusNames { [pscustomobject]@{ Parsed = 'P'; Unparsed = 'U' } }
        Mock Get-UnparsedModCount { @($Roster | Where-Object { $_.Id -eq 'Broken Mod' }).Count }
        Mock Read-EnabledModIds { [pscustomobject]@{ Ids = @('alpha'); IsListRead = $true } }
        Mock Read-ModFolderCandidates { ,@('candidate one', 'candidate two', 'candidate three') }
        Mock Format-ModCopy { "copy of $($Candidate.Id)" }
        Mock ConvertTo-RosterEntry { [pscustomobject]@{ Id = $ModGroup.Id } }
        Mock Group-ModCandidate {
            New-Group -Id 'alpha' -Superseded @([pscustomobject]@{ Id = 'alpha-old' })
            New-Group -Id 'beta'
            New-Group -Id 'Broken Mod' -Status 'U'
        }
    }

    It 'lists the enabled mods, and keeps an unparsed one whatever the enabled list says' {

        # ARRANGE
        $expectedIds = @('alpha', 'Broken Mod')

        # ACT
        $actualRoster = Read-ModRoster -StarsectorRoot $script:InstallRoot -DiagnosticSource 'Test'

        # ASSERT
        $actualRoster.Id | Should -Be $expectedIds
    }

    # The game enables only the mod whose ID enabled_mods.json spells exactly.
    It 'matches enabled IDs by the mod ID comparer' {

        # ARRANGE
        Mock Read-EnabledModIds { [pscustomobject]@{ Ids = @('Alpha'); IsListRead = $true } }

        $expectedIds = @('Broken Mod')

        # ACT
        $actualRoster = Read-ModRoster -StarsectorRoot $script:InstallRoot -DiagnosticSource 'Test'

        # ASSERT
        $actualRoster.Id | Should -Be $expectedIds
    }

    It 'lists every mod with -IncludeDisabled' {

        # ARRANGE
        $expectedIds = @('alpha', 'beta', 'Broken Mod')

        # ACT
        $actualRoster = Read-ModRoster -StarsectorRoot $script:InstallRoot -IncludeDisabled -DiagnosticSource 'Test'

        # ASSERT
        $actualRoster.Id | Should -Be $expectedIds
    }

    It 'lists every mod when enabled_mods.json gave no list' {

        # ARRANGE
        Mock Read-EnabledModIds { [pscustomobject]@{ Ids = @(); IsListRead = $false } }

        $expectedIds = @('alpha', 'beta', 'Broken Mod')

        # ACT
        $actualRoster = Read-ModRoster -StarsectorRoot $script:InstallRoot -DiagnosticSource 'Test'

        # ASSERT
        $actualRoster.Id | Should -Be $expectedIds
    }

    It 'names each copy a winner beat, then what it read and decided, under the caller''s name' {

        # ARRANGE
        $expectedDiagnostics = @(
            'Test: alpha: copy of alpha wins over copy of alpha-old, coming first in folder order'
            ("Test: read 3 mod folders under $script:InstallRoot\mods and 1 enabled IDs; " +
                'listing 2 mods (1 unparsed), skipping 1 disabled and 1 superseded copies')
        )

        # ACT
        Read-ModRoster -StarsectorRoot $script:InstallRoot -DiagnosticSource 'Test' | Out-Null

        # ASSERT
        $script:Diagnostics | Should -Be $expectedDiagnostics
    }

    It 'fails naming the path when the install has no mods folder' {

        # ARRANGE
        $bareRoot = Join-Path $TestDrive 'bare'
        $expectedMessage = "mods directory not found: $bareRoot\mods"

        # ACT
        $actualError = { Read-ModRoster -StarsectorRoot $bareRoot -DiagnosticSource 'Test' } | Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message | Should -Be $expectedMessage
    }
}
