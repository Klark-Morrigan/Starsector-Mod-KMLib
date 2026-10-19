BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-MissingContent.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Add-MissingContent.ps1"
}

Describe 'Add-MissingContent' {

    BeforeEach {
        Mock Get-MissingContent { if ($Cause) { [pscustomobject]@{ Id = "missing-$Cause" } } }
    }

    It 'adds each attempt its own missing content, and the load its first attempt''s' {

        $load = [pscustomobject]@{
            Attempts = @(
                [pscustomobject]@{ CauseType = 'T'; Cause = 'first' }
                [pscustomobject]@{ CauseType = 'T'; Cause = 'second' }
                [pscustomobject]@{ CauseType = $null; Cause = $null }
            )
        }

        $result = Add-MissingContent -Load $load

        $result.Missing.Id | Should -Be 'missing-first'
        $result.Attempts[1].Missing.Id | Should -Be 'missing-second'
        $result.Attempts[2].Missing | Should -BeNullOrEmpty
    }

    It 'adds none when the first attempt loaded' {

        $load = [pscustomobject]@{ Attempts = @([pscustomobject]@{ CauseType = $null; Cause = $null }) }

        (Add-MissingContent -Load $load).Missing | Should -BeNullOrEmpty
    }
}
