BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue"

    . "$PSScriptRoot\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Add-GeneratedContent.ps1"
    . "$libRoot\Get-ContentDefinitions.ps1"
    . "$libRoot\New-ContentCatalogue.ps1"
    . "$libRoot\Read-KindContentOwners.ps1"
    . "$libRoot\Set-ContentOwnerOrder.ps1"
}

Describe 'New-ContentCatalogue' {

    BeforeEach {

        $script:Generated = [System.Collections.Generic.List[string]]::new()

        # Base is built from files alone. Built takes a second ID from each Base ID, and one from its own.
        Mock Get-ContentDefinitions {
            [pscustomobject]@{ Kind = 'Base'; Generated = @() }
            [pscustomobject]@{
                Kind      = 'Built'
                Generated = @(
                    [pscustomobject]@{ FromKind = 'Base'; Suffix = '_from_base' }
                    [pscustomobject]@{ FromKind = 'Built'; Suffix = '_from_self' }
                )
            }
            [pscustomobject]@{ Kind = 'Other'; Generated = @() }
        }

        # Each kind defines one ID named for it, reads 10 files and 2 spreadsheets, and skips and replaces one each.
        Mock Read-KindContentOwners {
            [pscustomobject]@{
                OwnersById       = New-OwnerIndex @{ "$($Definition.Kind)-id" = @('m') }
                ContentFileCount = 10
                SpreadsheetCount = 2
                SkippedFiles     = @([pscustomobject]@{ Path = "$($Definition.Kind)\bad"; Reason = 'broken' })
                Replacements     = @([pscustomobject]@{ Spreadsheet = "$($Definition.Kind).csv"; Owner = 'm' })
            }
        }

        # Records which IDs each rule was handed and whose owners it read, and adds one ID per rule.
        Mock Add-GeneratedContent {

            $script:Generated.Add("$($Rule.Suffix): $($BaseIds -join '+') owned as $(@($BaseOwners.Keys) -join '+')")
            $OwnersById.Add("added$($Rule.Suffix)", [System.Collections.Generic.List[string]]::new([string[]] @('m')))

            1
        }

        Mock Set-ContentOwnerOrder {}
    }

    It 'reads each kind asked for, in the definitions'' order' {

        $catalogue = New-ContentCatalogue -Sources @('sources') -Kinds @('Other', 'Base')

        @($catalogue.OwnersByKind.Keys) | Should -Be @('Base', 'Other')
        Format-OwnerIndex -OwnersById $catalogue.OwnersByKind['Other'] | Should -Be 'Other-id=m'

        Should -Invoke Read-KindContentOwners -Times 2 -Exactly
        Should -Invoke Read-KindContentOwners -Times 1 -Exactly -ParameterFilter {
            "$Sources" -eq 'sources' -and $Definition.Kind -eq 'Base'
        }
    }

    # The game builds only from the IDs files define, so no rule sees another rule's IDs.
    It 'adds each rule''s IDs from the base kind''s file-defined IDs, read though not asked for' {

        $catalogue = New-ContentCatalogue -Sources @() -Kinds @('Built')

        @($catalogue.OwnersByKind.Keys) | Should -Be @('Built')
        Format-OwnerIndex -OwnersById $catalogue.OwnersByKind['Built'] |
            Should -Be 'Built-id=m added_from_base=m added_from_self=m'
        $script:Generated | Should -Be @(
            '_from_base: Base-id owned as Base-id'
            '_from_self: Built-id owned as Built-id+added_from_base'
        )
        $catalogue.GeneratedCount | Should -Be 2
    }

    It 'orders each asked kind''s owners by the sources' {

        New-ContentCatalogue -Sources @('sources') -Kinds @('Base', 'Built') | Out-Null

        Should -Invoke Set-ContentOwnerOrder -ParameterFilter { "$Sources" -eq 'sources' } -Times 2 -Exactly
    }

    It 'sums what every kind read, the base kinds too, and returns what each skipped and replaced' {

        $catalogue = New-ContentCatalogue -Sources @() -Kinds @('Built', 'Other')

        $catalogue.ContentFileCount | Should -Be 30
        $catalogue.SpreadsheetCount | Should -Be 6
        $catalogue.SkippedFiles.Path | Should -Be @('Base\bad', 'Built\bad', 'Other\bad')
        $catalogue.Replacements.Spreadsheet | Should -Be @('Base.csv', 'Built.csv', 'Other.csv')
    }
}
