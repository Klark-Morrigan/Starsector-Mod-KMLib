BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\mod-drift\Compare-SaveModRoster.ps1"
    . "$libRoot\mod-drift\Format-ModDriftCounts.ps1"
    . "$libRoot\mod-drift\Read-SaveModDrift.ps1"
    . "$libRoot\mod-drift\Format-DroppedModSummary.ps1"
    . "$libRoot\save-descriptor\Get-SaveDescriptorPath.ps1"
    . "$libRoot\save-descriptor\Read-SaveModList.ps1"
    . "$libRoot\xml\Get-XmlChildText.ps1"
    . "$libRoot\xml\New-XStreamIdIndex.ps1"
    . "$libRoot\xml\Read-XmlDocument.ps1"
}

Describe 'Read-SaveModDrift' {

    BeforeEach {

        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()
        $script:Arguments = @{
            StarsectorRoot   = 'C:\Games'
            SaveFolder       = 's'
            Roster           = @([pscustomobject]@{ Id = 'installed' })
            DiagnosticSource = 'Test'
        }

        $script:Document = [System.Xml.XmlDocument]::new()
        $script:Document.LoadXml('<SaveGameData/>')

        Mock Get-SaveDescriptorPath { 'C:\saves\s\descriptor.xml' }
        Mock Test-Path { $true }
        Mock Read-XmlDocument { $script:Document }
        Mock New-XStreamIdIndex { @{ '1' = 'one'; '2' = 'two' } }
        Mock Get-XmlChildText { "$Name-text" }
        Mock Write-DiagnosticLine { $script:Diagnostics.Add("${Source}: $Message") }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Format-DroppedModSummary { "dropped from $(@($EverEnabledMods).Count) ever enabled" }
        Mock Compare-SaveModRoster { 'row one'; 'row two' }
        Mock Format-ModDriftCounts { "counts of $(@($Rows).Count)" }
        Mock Read-SaveModList {

            if ($ListName -eq 'enabledMods') {
                ,@([pscustomobject]@{ Id = 'saved' })
            } else {
                ,@([pscustomobject]@{ Id = 'saved' }, [pscustomobject]@{ Id = 'retired' })
            }
        }
    }

    It 'compares the save''s enabled mods with the roster, and has no problem' {

        # ARRANGE
        $arguments = $script:Arguments
        $expectedRows = @('row one', 'row two')

        # ACT
        $actualDrift = Read-SaveModDrift @arguments

        # ASSERT
        $actualDrift.Rows | Should -Be $expectedRows
        $actualDrift.Problem | Should -BeNullOrEmpty

        Should -Invoke Compare-SaveModRoster -Times 1 -Exactly -ParameterFilter {
            "$($SavedMods.Id)" -eq 'saved' -and "$($Roster.Id)" -eq 'installed'
        }
    }

    It 'reads the descriptor of the named save under the install' {

        # ARRANGE
        $arguments = $script:Arguments

        # ACT
        Read-SaveModDrift @arguments | Out-Null

        # ASSERT
        Should -Invoke Get-SaveDescriptorPath -Times 1 -Exactly -ParameterFilter {
            $StarsectorRoot -eq 'C:\Games' -and $SaveFolder -eq 's'
        }
        Should -Invoke Read-XmlDocument -Times 1 -Exactly -ParameterFilter {
            $Path -eq 'C:\saves\s\descriptor.xml'
        }
    }

    It 'says what it read and compared, under the caller''s name' {

        # ARRANGE
        $arguments = $script:Arguments
        $expectedDiagnostics = @(
            ('Test: read C:\saves\s\descriptor.xml, saved saveDate-text under Starsector gameVersion-text; ' +
                '2 objects carry an ID for back-pointers')
            'Test: read 1 enabled mods as the save last wrote them; dropped from 2 ever enabled'
            'Test: compared them with 1 installed mods: counts of 2'
        )

        # ACT
        Read-SaveModDrift @arguments | Out-Null

        # ASSERT
        $script:Diagnostics | Should -Be $expectedDiagnostics
    }

    # An empty list says the save had no mods, so every installed one is added; it must not read as no list.
    It 'compares an empty enabledMods list' {

        # ARRANGE
        Mock Read-SaveModList { ,@() }
        Mock Compare-SaveModRoster { 'added row' }

        $arguments = $script:Arguments
        $expectedRows = @('added row')

        # ACT
        $actualDrift = Read-SaveModDrift @arguments

        # ASSERT
        $actualDrift.Rows | Should -Be $expectedRows
        $actualDrift.Problem | Should -BeNullOrEmpty
    }

    It 'compares nothing, saying why, when the descriptor has no enabledMods list' {

        # ARRANGE
        Mock Read-SaveModList { $null }

        $arguments = $script:Arguments
        $expectedProblem = 'the descriptor lists no enabled mods: C:\saves\s\descriptor.xml'

        # ACT
        $actualDrift = Read-SaveModDrift @arguments

        # ASSERT
        $actualDrift.Rows | Should -BeNullOrEmpty
        $actualDrift.Problem | Should -Be $expectedProblem

        Should -Invoke Compare-SaveModRoster -Times 0
    }

    # A save deleted or renamed after the caller found it has no descriptor; the caller decides what that means.
    It 'reads nothing, naming the path, when the save has no descriptor' {

        # ARRANGE
        Mock Test-Path { $false }

        $arguments = $script:Arguments
        $expectedProblem = 'descriptor not found: C:\saves\s\descriptor.xml'

        # ACT
        $actualDrift = Read-SaveModDrift @arguments

        # ASSERT
        $actualDrift.Rows | Should -BeNullOrEmpty
        $actualDrift.Problem | Should -Be $expectedProblem

        Should -Invoke Read-XmlDocument -Times 0
        Should -Invoke Write-DiagnosticLine -Times 0
    }
}
