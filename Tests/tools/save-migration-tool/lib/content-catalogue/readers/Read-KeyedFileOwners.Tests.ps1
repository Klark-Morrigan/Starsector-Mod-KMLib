BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\Format-OwnerIndex.ps1"
    . "$libRoot\content-catalogue\owner-index\Add-ContentOwner.ps1"
    . "$libRoot\content-catalogue\readers\Find-ListedContentFiles.ps1"
    . "$libRoot\content-catalogue\owner-index\New-ContentOwnerIndex.ps1"
    . "$libRoot\content-catalogue\definitions\New-KeyedFileSource.ps1"
    . "$libRoot\content-catalogue\readers\Read-KeyedFileOwners.ps1"
    . "$libRoot\content-catalogue\readers\Select-MergedCopies.ps1"
    . "$libRoot\data-file\ConvertFrom-StarsectorJson.ps1"
    . "$libRoot\data-file\Get-JsonPropertyValue.ps1"

    function script:New-Copy {
        # One copy of the keyed file, held by the named owner; its path is the owner's name.
        param([string] $Owner)

        return [pscustomobject]@{ Source = [pscustomobject]@{ Owner = $Owner }; Path = $Owner }
    }
}

Describe 'Read-KeyedFileOwners' {

    BeforeEach {

        # Each copy's JSON, by owner, as the reader parses it: an ordered hashtable keyed case-sensitively.
        $script:JsonByOwner = @{}

        Mock Find-ListedContentFiles {
            [pscustomobject]@{ RelativePath = $RelativePaths[0]; Copies = @((New-Copy 'core'), (New-Copy 'alpha')) }
        }

        # Merge order is the copies' order, so the fake reads core first.
        Mock Select-MergedCopies { [pscustomobject]@{ Copies = $File.Copies; Replacement = $null } }
        Mock ConvertFrom-StarsectorJson { $script:JsonByOwner[$Path] }

        # A plain key lookup, as every object the fake parser returns is a dictionary.
        Mock Get-JsonPropertyValue { $Object[$Name] }
    }

    It 'defines each key any copy holds, with every owner whose copy holds it, keys matched case-sensitively' {

        # ARRANGE
        $script:JsonByOwner['core'] = [ordered]@{ planet = @{}; shared = @{} }

        $alphaJson = [System.Collections.Specialized.OrderedDictionary]::new([System.StringComparer]::Ordinal)
        $alphaJson['shared'] = @{}
        $alphaJson['Shared'] = @{}
        $script:JsonByOwner['alpha'] = $alphaJson

        $keyedFile = New-KeyedFileSource -Path 'data/config/planets.json'
        $expectedOwners = 'Shared=alpha planet=core shared=core+alpha'
        $expectedCopyCount = 2

        # ACT
        $actualRead = Read-KeyedFileOwners -Sources @('sources') -KeyedFile $keyedFile

        # ASSERT
        Format-OwnerIndex -OwnersById $actualRead.OwnersById | Should -Be $expectedOwners
        $actualRead.CopyCount | Should -Be $expectedCopyCount
        $actualRead.Replacement | Should -BeNullOrEmpty
        Should -Invoke Find-ListedContentFiles -ParameterFilter {
            "$Sources" -eq 'sources' -and "$RelativePaths" -eq 'data/config/planets.json'
        } -Times 1 -Exactly
    }

    It 'reads the keys under <Description>' -ForEach @(
        @{ Description = 'a nested key'; Options = @{ NestedKey = 'music' }; Owners = 'set_a=core' }
        @{ Description = 'the root, leaving out the excluded ones'; Options = @{ ExcludedKeys = @('music') }
            Owners = 'sound_a=core sound_b=alpha' }
    ) {

        # ARRANGE
        $script:JsonByOwner['core'] = [ordered]@{ music = [ordered]@{ set_a = @() }; sound_a = @{} }
        $script:JsonByOwner['alpha'] = [ordered]@{ sound_b = @{} }

        $keyedFile = New-KeyedFileSource -Path 'sounds.json' @Options
        $expectedOwners = $Owners

        # ACT
        $actualRead = Read-KeyedFileOwners -Sources @() -KeyedFile $keyedFile

        # ASSERT
        Format-OwnerIndex -OwnersById $actualRead.OwnersById | Should -Be $expectedOwners
    }

    # The merge keeps the last copy's value, so that copy decides whether the entry is an object.
    It 'keeps only object entries when asked, judged by the last copy in merge order' {

        # ARRANGE
        $script:JsonByOwner['core'] = [ordered]@{ kept = @{}; dropped = 'x'; turned = @{}; restored = 'y' }
        $script:JsonByOwner['alpha'] = [ordered]@{ turned = 'z'; restored = @{} }

        $keyedFile = New-KeyedFileSource -Path 'planets.json' -IsObjectEntryOnly
        $expectedOwners = 'kept=core restored=core+alpha'

        # ACT
        $actualRead = Read-KeyedFileOwners -Sources @() -KeyedFile $keyedFile

        # ASSERT
        Format-OwnerIndex -OwnersById $actualRead.OwnersById | Should -Be $expectedOwners
    }

    # A mod replacing the file leaves only its copy, so the merge hands back that copy alone.
    It 'reads only the copies the merge keeps, and returns the mod replacing the file' {

        # ARRANGE
        $script:JsonByOwner['core'] = [ordered]@{ planet = @{} }
        $script:JsonByOwner['alpha'] = [ordered]@{ other = @{} }

        Mock Select-MergedCopies {
            [pscustomobject]@{
                Copies      = @($File.Copies[1])
                Replacement = [pscustomobject]@{ Path = $File.RelativePath; Owner = 'alpha' }
            }
        }

        $keyedFile = New-KeyedFileSource -Path 'data/config/planets.json'
        $expectedOwners = 'other=alpha'
        $expectedReplacedPath = 'data/config/planets.json'
        $expectedReplacingOwner = 'alpha'

        # ACT
        $actualRead = Read-KeyedFileOwners -Sources @() -KeyedFile $keyedFile

        # ASSERT
        Format-OwnerIndex -OwnersById $actualRead.OwnersById | Should -Be $expectedOwners
        $actualRead.Replacement.Path | Should -Be $expectedReplacedPath
        $actualRead.Replacement.Owner | Should -Be $expectedReplacingOwner
    }

    It 'reads nothing when no source holds the file' {

        # ARRANGE
        Mock Find-ListedContentFiles { }

        $keyedFile = New-KeyedFileSource -Path 'planets.json'
        $expectedOwnerCount = 0
        $expectedCopyCount = 0

        # ACT
        $actualRead = Read-KeyedFileOwners -Sources @() -KeyedFile $keyedFile

        # ASSERT
        $actualRead.OwnersById.Count | Should -Be $expectedOwnerCount
        $actualRead.CopyCount | Should -Be $expectedCopyCount
        $actualRead.Replacement | Should -BeNullOrEmpty
        Should -Invoke Select-MergedCopies -Times 0 -Exactly
    }

    It 'skips a copy that does not parse, returning it with the reason, and reads the others' {

        # ARRANGE
        $script:JsonByOwner['alpha'] = [ordered]@{ planet = @{} }

        Mock ConvertFrom-StarsectorJson {

            if ($Path -eq 'core') {
                throw 'not valid JSON'
            }

            $script:JsonByOwner[$Path]
        }

        $keyedFile = New-KeyedFileSource -Path 'planets.json'
        $expectedOwners = 'planet=alpha'
        $expectedSkippedPaths = @('core')
        $expectedSkippedReasons = @('not valid JSON')

        # ACT
        $actualRead = Read-KeyedFileOwners -Sources @() -KeyedFile $keyedFile

        # ASSERT
        Format-OwnerIndex -OwnersById $actualRead.OwnersById | Should -Be $expectedOwners
        $actualRead.SkippedFiles.Path | Should -Be $expectedSkippedPaths
        $actualRead.SkippedFiles.Reason | Should -Be $expectedSkippedReasons
    }
}
