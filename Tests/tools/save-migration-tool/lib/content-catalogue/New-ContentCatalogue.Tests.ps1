BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue"

    . "$PSScriptRoot\..\..\helpers\Format-OwnerIndex.ps1"
    . "$PSScriptRoot\..\..\helpers\New-OwnerIndex.ps1"
    . "$libRoot\Add-GeneratedContent.ps1"
    . "$libRoot\definitions\Get-ContentDefinitions.ps1"
    . "$libRoot\New-ContentCatalogue.ps1"
    . "$libRoot\owner-index\Set-ContentOwnerOrder.ps1"
    . "$libRoot\readers\New-ParsedFileCache.ps1"
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

        $script:Cache = [pscustomobject]@{
            JsonByPath        = [System.Collections.Generic.Dictionary[string, object]]::new()
            SpreadsheetsByKey = [System.Collections.Generic.Dictionary[string, object]]::new()
        }

        Mock New-ParsedFileCache { $script:Cache }

        # Each kind defines one ID named for it, reads 10 content files, and skips and replaces one each.
        # Through the cache it parses a spreadsheet of its own and the one JSON file every kind shares.
        Mock Read-KindContentOwners {

            $Cache.SpreadsheetsByKey["$($Definition.Kind).csv"] = 'parsed'
            $Cache.JsonByPath['shared.json'] = 'parsed'

            [pscustomobject]@{
                OwnersById       = New-OwnerIndex @{ "$($Definition.Kind)-id" = @('m') }
                ContentFileCount = 10
                SkippedFiles     = @([pscustomobject]@{ Path = "$($Definition.Kind)\bad"; Reason = 'broken' })
                Replacements     = @([pscustomobject]@{ Path = "$($Definition.Kind).csv"; Owner = 'm' })
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

    It 'reads every kind with one cache for the run' {

        # ARRANGE
        $expectedNewCacheCount = 1
        $expectedReadCount = 2
        $expectedCache = $script:Cache

        # ACT
        New-ContentCatalogue -Sources @() -Kinds @('Base', 'Other') | Out-Null

        # ASSERT
        Should -Invoke New-ParsedFileCache -Times $expectedNewCacheCount -Exactly
        Should -Invoke Read-KindContentOwners -Times $expectedReadCount -Exactly -ParameterFilter {
            [object]::ReferenceEquals($Cache, $expectedCache)
        }
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

    # A file several kinds read is parsed once, so the cache, not the kinds, says how many were read.
    It 'counts every kind''s content files and each file the cache parsed, returning what each skipped and replaced' {

        # ARRANGE
        $expectedContentFileCount = 31
        $expectedSpreadsheetCount = 3
        $expectedSkippedPaths = @('Base\bad', 'Built\bad', 'Other\bad')
        $expectedReplacedPaths = @('Base.csv', 'Built.csv', 'Other.csv')

        # ACT
        $actualCatalogue = New-ContentCatalogue -Sources @() -Kinds @('Built', 'Other')

        # ASSERT
        $actualCatalogue.ContentFileCount | Should -Be $expectedContentFileCount
        $actualCatalogue.SpreadsheetCount | Should -Be $expectedSpreadsheetCount
        $actualCatalogue.SkippedFiles.Path | Should -Be $expectedSkippedPaths
        $actualCatalogue.Replacements.Path | Should -Be $expectedReplacedPaths
    }

    # sounds.json holds both sounds and music sets, so two kinds read it, and it would be named twice.
    It 'returns a file several kinds replaced once, and one several kinds skipped once per reason' {

        # ARRANGE
        Mock Read-KindContentOwners {

            $reason = if ($Definition.Kind -eq 'Other') {
                'missing a column'
            } else {
                'broken'
            }

            [pscustomobject]@{
                OwnersById       = New-OwnerIndex @{}
                ContentFileCount = 0
                SkippedFiles     = @([pscustomobject]@{ Path = 'shared.csv'; Reason = $reason })
                Replacements     = @([pscustomobject]@{ Path = 'shared.json'; Owner = 'm' })
            }
        }

        $expectedSkippedReasons = @('broken', 'missing a column')
        $expectedReplacedPaths = @('shared.json')

        # ACT
        $actualCatalogue = New-ContentCatalogue -Sources @() -Kinds @('Base', 'Built', 'Other')

        # ASSERT
        $actualCatalogue.SkippedFiles.Reason | Should -Be $expectedSkippedReasons
        $actualCatalogue.Replacements.Path | Should -Be $expectedReplacedPaths
    }

    It 'returns each kind read, the base kinds too, with how many IDs its files define and how long it took' {

        # ARRANGE
        $expectedKinds = @('Base', 'Built')
        $expectedIdCounts = @(1, 1)

        # A stopwatch's elapsed milliseconds, whose value no run repeats.
        $expectedTimeType = [long]

        # ACT
        $actualCatalogue = New-ContentCatalogue -Sources @() -Kinds @('Built')

        # ASSERT
        $actualCatalogue.KindReads.Kind | Should -Be $expectedKinds
        $actualCatalogue.KindReads.IdCount | Should -Be $expectedIdCounts
        $actualCatalogue.KindReads[0].Milliseconds | Should -BeOfType $expectedTimeType
        $actualCatalogue.KindReads[1].Milliseconds | Should -BeOfType $expectedTimeType
    }
}
