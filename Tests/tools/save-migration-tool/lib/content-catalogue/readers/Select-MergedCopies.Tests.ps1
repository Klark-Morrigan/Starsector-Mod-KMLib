BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\readers\Find-ReplacingSource.ps1"
    . "$libRoot\content-catalogue\readers\Select-MergedCopies.ps1"

    function script:New-Copy {
        # One copy of a file, held by a source that is a mod unless told otherwise.
        param(
            [string] $Owner,
            [switch] $IsCore
        )

        $source = [pscustomobject]@{ Owner = $Owner; IsMod = -not $IsCore }

        return [pscustomobject]@{ Source = $source; Path = "C:\$Owner\a.ship" }
    }
}

Describe 'Select-MergedCopies' {

    BeforeEach {
        Mock Find-ReplacingSource { $null }
    }

    # The game lays each mod's copy over its own, so the mods' keys win over the game's.
    It 'puts the game''s own copy first, then the mods'' copies in search order, with no replacing mod' {

        # ARRANGE
        $file = [pscustomobject]@{
            RelativePath = 'data/hulls/a.ship'
            Copies       = @((New-Copy -Owner 'alpha'), (New-Copy -Owner 'beta'), (New-Copy -Owner 'core' -IsCore))
        }

        $expectedOwners = @('core', 'alpha', 'beta')

        # ACT
        $actualCopies = Select-MergedCopies -File $file

        # ASSERT
        $actualCopies.Source.Owner | Should -Be $expectedOwners
    }

    # The game takes a replaced JSON or content file's path written with either slash.
    It 'asks for the replacing source among the copies'' sources, in search order, accepting either slash' {

        # ARRANGE
        $file = [pscustomobject]@{
            RelativePath = 'data/hulls/a.ship'
            Copies       = @((New-Copy -Owner 'alpha'), (New-Copy -Owner 'core' -IsCore))
        }

        $expectedPath = 'data/hulls/a.ship'
        $expectedOwners = 'alpha core'

        # ACT
        Select-MergedCopies -File $file | Out-Null

        # ASSERT
        Should -Invoke Find-ReplacingSource -Times 1 -Exactly -ParameterFilter {
            $Path -ceq $expectedPath -and "$($Sources.Owner)" -ceq $expectedOwners -and $AcceptsEitherSlash
        }
    }

    It 'keeps only the replacing source''s copy' {

        # ARRANGE
        $file = [pscustomobject]@{
            RelativePath = 'data/hulls/a.ship'
            Copies       = @(
                (New-Copy -Owner 'alpha')
                (New-Copy -Owner 'beta')
                (New-Copy -Owner 'core' -IsCore)
            )
        }
        $script:ReplacingSource = $file.Copies[1].Source

        Mock Find-ReplacingSource { $script:ReplacingSource }

        $expectedOwners = @('beta')

        # ACT
        $actualCopies = Select-MergedCopies -File $file

        # ASSERT
        $actualCopies.Source.Owner | Should -Be $expectedOwners
    }
}
