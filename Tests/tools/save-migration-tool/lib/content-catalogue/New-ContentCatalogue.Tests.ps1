BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue"

    . "$PSScriptRoot\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Add-GeneratedContent.ps1"
    . "$libRoot\definitions\Get-ContentDefinitions.ps1"
    . "$libRoot\New-ContentCatalogue.ps1"
    . "$libRoot\owner-index\Set-ContentOwnerOrder.ps1"
    . "$libRoot\readers\Read-KindContentOwners.ps1"
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

        # ARRANGE
        $expectedKinds = @('Base', 'Other')
        $expectedOtherOwners = 'Other-id=m'
        $expectedReadCount = 2
        $expectedBaseReadCount = 1

        # ACT
        $actualCatalogue = New-ContentCatalogue -Sources @('sources') -Kinds @('Other', 'Base')

        # ASSERT
        @($actualCatalogue.OwnersByKind.Keys) | Should -Be $expectedKinds
        Format-OwnerIndex -OwnersById $actualCatalogue.OwnersByKind['Other'] | Should -Be $expectedOtherOwners

        Should -Invoke Read-KindContentOwners -Times $expectedReadCount -Exactly
        Should -Invoke Read-KindContentOwners -Times $expectedBaseReadCount -Exactly -ParameterFilter {
            "$Sources" -eq 'sources' -and $Definition.Kind -eq 'Base'
        }
    }

    # The game builds only from the IDs files define, so no rule sees another rule's IDs.
    It 'adds each rule''s IDs from the base kind''s file-defined IDs, read though not asked for' {

        # ARRANGE
        $expectedKinds = @('Built')
        $expectedBuiltOwners = 'Built-id=m added_from_base=m added_from_self=m'
        $expectedGenerated = @(
            '_from_base: Base-id owned as Base-id'
            '_from_self: Built-id owned as Built-id+added_from_base'
        )
        $expectedGeneratedCount = 2

        # ACT
        $actualCatalogue = New-ContentCatalogue -Sources @() -Kinds @('Built')

        # ASSERT
        @($actualCatalogue.OwnersByKind.Keys) | Should -Be $expectedKinds
        Format-OwnerIndex -OwnersById $actualCatalogue.OwnersByKind['Built'] | Should -Be $expectedBuiltOwners
        $script:Generated | Should -Be $expectedGenerated
        $actualCatalogue.GeneratedCount | Should -Be $expectedGeneratedCount
    }

    It 'orders each asked kind''s owners by the sources' {

        # ARRANGE
        $expectedOrderCount = 2

        # ACT
        New-ContentCatalogue -Sources @('sources') -Kinds @('Base', 'Built') | Out-Null

        # ASSERT
        Should -Invoke Set-ContentOwnerOrder -ParameterFilter { "$Sources" -eq 'sources' } `
            -Times $expectedOrderCount -Exactly
    }

    It 'sums what every kind read, the base kinds too, and returns what each skipped and replaced' {

        # ARRANGE
        $expectedContentFileCount = 30
        $expectedSpreadsheetCount = 6
        $expectedSkippedPaths = @('Base\bad', 'Built\bad', 'Other\bad')
        $expectedReplacedSpreadsheets = @('Base.csv', 'Built.csv', 'Other.csv')

        # ACT
        $actualCatalogue = New-ContentCatalogue -Sources @() -Kinds @('Built', 'Other')

        # ASSERT
        $actualCatalogue.ContentFileCount | Should -Be $expectedContentFileCount
        $actualCatalogue.SpreadsheetCount | Should -Be $expectedSpreadsheetCount
        $actualCatalogue.SkippedFiles.Path | Should -Be $expectedSkippedPaths
        $actualCatalogue.Replacements.Spreadsheet | Should -Be $expectedReplacedSpreadsheets
    }
}
