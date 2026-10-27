BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Select-MergedCopies.ps1"

    function script:New-Copy {
        # One copy of a file, held by a source that is a mod unless told otherwise.
        param(
            [string] $Owner,
            [string[]] $ReplacedPaths = @(),
            [switch] $IsCore
        )

        $source = [pscustomobject]@{ Owner = $Owner; IsMod = -not $IsCore; ReplacedPaths = $ReplacedPaths }

        return [pscustomobject]@{ Source = $source; Path = "C:\$Owner\a.ship" }
    }
}

Describe 'Select-MergedCopies' {

    It 'puts the game''s own copy first, then the mods'' copies in search order' {

        $file = [pscustomobject]@{
            RelativePath = 'data/hulls/a.ship'
            Copies       = @((New-Copy -Owner 'alpha'), (New-Copy -Owner 'beta'), (New-Copy -Owner 'core' -IsCore))
        }

        (Select-MergedCopies -File $file).Source.Owner | Should -Be @('core', 'alpha', 'beta')
    }

    It 'keeps only the copy of the first mod that replaces the path, written with <Description>' -ForEach @(
        @{ Description = 'forward slashes'; Replaced = 'data/hulls/a.ship' }
        @{ Description = 'backslashes'; Replaced = 'data\hulls\a.ship' }
    ) {

        $file = [pscustomobject]@{
            RelativePath = 'data/hulls/a.ship'
            Copies       = @(
                (New-Copy -Owner 'alpha')
                (New-Copy -Owner 'beta' -ReplacedPaths @($Replaced))
                (New-Copy -Owner 'gamma' -ReplacedPaths @($Replaced))
                (New-Copy -Owner 'core' -IsCore)
            )
        }

        (Select-MergedCopies -File $file).Source.Owner | Should -Be @('beta')
    }

    It 'matches a replaced path case-sensitively' {

        $file = [pscustomobject]@{
            RelativePath = 'data/hulls/a.ship'
            Copies       = @(
                (New-Copy -Owner 'alpha' -ReplacedPaths @('data/hulls/A.ship'))
                (New-Copy -Owner 'core' -IsCore)
            )
        }

        (Select-MergedCopies -File $file).Source.Owner | Should -Be @('core', 'alpha')
    }
}
