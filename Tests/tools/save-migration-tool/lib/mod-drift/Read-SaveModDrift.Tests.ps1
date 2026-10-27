BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\mod-drift\Compare-SaveModRoster.ps1"
    . "$libRoot\mod-drift\Format-ModDriftCounts.ps1"
    . "$libRoot\mod-drift\Read-SaveModDrift.ps1"
    . "$libRoot\save-descriptor\Format-DroppedModSummary.ps1"
    . "$libRoot\save-descriptor\Read-SaveModList.ps1"
    . "$libRoot\xml\Get-XmlChildText.ps1"
    . "$libRoot\xml\New-XStreamIdIndex.ps1"
    . "$libRoot\xml\Read-XmlDocument.ps1"
}

Describe 'Read-SaveModDrift' {

    BeforeEach {

        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()
        $script:Roster = @([pscustomobject]@{ Id = 'installed' })

        $script:Document = [System.Xml.XmlDocument]::new()
        $script:Document.LoadXml('<SaveGameData/>')

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

        $drift = Read-SaveModDrift -DescriptorPath 'C:\saves\s\descriptor.xml' -Roster $script:Roster `
            -DiagnosticSource 'Test'

        $drift.Rows | Should -Be @('row one', 'row two')
        $drift.Problem | Should -BeNullOrEmpty

        Should -Invoke Compare-SaveModRoster -Times 1 -Exactly -ParameterFilter {
            "$($SavedMods.Id)" -eq 'saved' -and "$($Roster.Id)" -eq 'installed'
        }
    }

    It 'says what it read and compared, under the caller''s name' {

        Read-SaveModDrift -DescriptorPath 'C:\saves\s\descriptor.xml' -Roster $script:Roster `
            -DiagnosticSource 'Test' | Out-Null

        $script:Diagnostics | Should -Be @(
            ('Test: read C:\saves\s\descriptor.xml, saved saveDate-text under Starsector gameVersion-text; ' +
                '2 objects carry an ID for back-pointers')
            'Test: read 1 enabled mods as the save last wrote them; dropped from 2 ever enabled'
            'Test: compared them with 1 installed mods: counts of 2'
        )
    }

    # An empty list says the save had no mods, so every installed one is added; it must not read as no list.
    It 'compares an empty enabledMods list' {

        Mock Read-SaveModList { ,@() }
        Mock Compare-SaveModRoster { 'added row' }

        $drift = Read-SaveModDrift -DescriptorPath 'C:\saves\s\descriptor.xml' -Roster $script:Roster `
            -DiagnosticSource 'Test'

        $drift.Rows | Should -Be @('added row')
        $drift.Problem | Should -BeNullOrEmpty
    }

    It 'compares nothing, saying why, when the descriptor has no enabledMods list' {

        Mock Read-SaveModList { $null }

        $drift = Read-SaveModDrift -DescriptorPath 'C:\saves\s\descriptor.xml' -Roster $script:Roster `
            -DiagnosticSource 'Test'

        $drift.Rows | Should -BeNullOrEmpty
        $drift.Problem | Should -Be 'the descriptor lists no enabled mods: C:\saves\s\descriptor.xml'

        Should -Invoke Compare-SaveModRoster -Times 0
    }
}
