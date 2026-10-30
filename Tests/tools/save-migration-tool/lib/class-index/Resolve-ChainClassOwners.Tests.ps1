BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\class-index\New-InstallClassOwnerIndex.ps1"
    . "$libRoot\class-index\Resolve-ChainClassOwners.ps1"
    . "$libRoot\mod-roster\Get-UnparsedModCount.ps1"
    . "$libRoot\mod-roster\Read-ModRoster.ps1"
}

Describe 'Resolve-ChainClassOwners' {

    BeforeEach {

        $script:InstallRoot = Join-Path $TestDrive 'install'
        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()
        $script:Roster = @(
            [pscustomobject]@{ Id = 'alpha' }
            [pscustomobject]@{ Id = 'Broken Mod' }
        )

        Mock Write-DiagnosticLine { $script:Diagnostics.Add($Message) }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Get-UnparsedModCount { 1 }
        Mock Read-ModRoster { $script:Roster }
        Mock New-InstallClassOwnerIndex {
            [pscustomobject]@{
                OwnersByClass           = @{ 'x.A' = @('alpha') }
                ModJarCount             = 3
                CoreJarCount            = 1
                CoreClasspathEntryCount = 2
                CoreClasspathProblem    = $null
                SharedClassCount        = 0
                ShadowedClassCount      = 1
                SkippedJars             = @([pscustomobject]@{ Path = 'C:\mods\b.jar'; Reason = 'not on disk' })
            }
        }
    }

    It 'reads the install''s roster when none is given, resolves the classes against it, and says what it read' {

        # ARRANGE
        $expectedOwners = @('alpha')
        $expectedResolvingLine = 'resolving the owners of 2 object chain classes ' +
            "from the install at $script:InstallRoot"
        $expectedSkipLine = 'skipped JAR C:\mods\b.jar: not on disk'
        $expectedReadPattern = 'read 3 JARs of 2 mods and 1 of 2 core classpath entries in * ms, *'
        $expectedOwnersPattern = '*the JARs of 1 unparsed mods unknown; owners found for 1 classes, *'

        # ACT
        $actualOwners = Resolve-ChainClassOwners -InstallRoot $script:InstallRoot -ClassNames @('x.A', 'x.B') `
            -DiagnosticSource 'Test'

        # ASSERT
        $actualOwners['x.A'] | Should -Be $expectedOwners

        Should -Invoke Read-ModRoster -Times 1 -Exactly -ParameterFilter {
            $StarsectorRoot -eq $script:InstallRoot -and $DiagnosticSource -eq 'Test'
        }
        Should -Invoke New-InstallClassOwnerIndex -Times 1 -Exactly -ParameterFilter {
            "$($Roster.Id)" -eq 'alpha Broken Mod' -and "$ClassNames" -eq 'x.A x.B'
        }

        $script:Diagnostics[0] | Should -Be $expectedResolvingLine
        $script:Diagnostics[1] | Should -Be $expectedSkipLine
        $script:Diagnostics[2] | Should -BeLike $expectedReadPattern
        $script:Diagnostics[2] | Should -BeLike $expectedOwnersPattern
    }

    # A caller that has read the roster for its own use hands it over, so the install's mods are read once.
    It 'resolves against a given roster without reading one' {

        # ARRANGE
        $given = @([pscustomobject]@{ Id = 'given' })

        # ACT
        Resolve-ChainClassOwners -InstallRoot $script:InstallRoot -ClassNames @('x.A') -Roster $given `
            -DiagnosticSource 'Test' | Out-Null

        # ASSERT
        Should -Invoke Read-ModRoster -Times 0
        Should -Invoke New-InstallClassOwnerIndex -Times 1 -Exactly -ParameterFilter { "$($Roster.Id)" -eq 'given' }
    }

    It 'warns why vanilla classes stay without owners when the core classpath could not be read' {

        # ARRANGE
        Mock New-InstallClassOwnerIndex {
            [pscustomobject]@{
                OwnersByClass = @{}; ModJarCount = 0; CoreJarCount = 0; CoreClasspathEntryCount = 0
                CoreClasspathProblem = 'C:\vmparams not found'; SharedClassCount = 0; ShadowedClassCount = 0
                SkippedJars = @()
            }
        }

        $expectedWarning = 'C:\vmparams not found; vanilla classes stay without owners'

        # ACT
        Resolve-ChainClassOwners -InstallRoot $script:InstallRoot -ClassNames @('x.A') -DiagnosticSource 'Test' `
            -WarningVariable actualWarnings -WarningAction SilentlyContinue | Out-Null

        # ASSERT
        "$actualWarnings" | Should -Be $expectedWarning
    }

    It 'resolves nothing, and says so, when no class is asked for' {

        # ARRANGE
        $expectedCount = 0
        $expectedDiagnostics = @('no listed failure names an object chain; resolving no class owners')

        # ACT
        $actualOwners = Resolve-ChainClassOwners -InstallRoot $script:InstallRoot -ClassNames @() `
            -DiagnosticSource 'Test'

        # ASSERT
        $actualOwners.Count | Should -Be $expectedCount
        $script:Diagnostics | Should -Be $expectedDiagnostics

        Should -Invoke Read-ModRoster -Times 0
        Should -Invoke New-InstallClassOwnerIndex -Times 0
    }
}
