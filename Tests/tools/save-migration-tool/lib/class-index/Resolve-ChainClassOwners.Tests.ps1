BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\class-index\Resolve-ClassOwner.ps1"
    . "$libRoot\class-index\Resolve-ChainClassOwners.ps1"
    . "$libRoot\install\Get-InstallPathNames.ps1"
    . "$libRoot\mod-roster\Get-UnparsedModCount.ps1"
    . "$libRoot\mod-roster\Read-ModRoster.ps1"
}

Describe 'Resolve-ChainClassOwners' {

    BeforeEach {

        $script:InstallRoot = Join-Path $TestDrive 'install'

        New-Item -ItemType Directory -Path (Join-Path $script:InstallRoot 'mods') -Force | Out-Null

        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()
        $script:Roster = @(
            [pscustomobject]@{ Id = 'alpha' }
            [pscustomobject]@{ Id = 'Broken Mod' }
        )

        Mock Write-DiagnosticLine { $script:Diagnostics.Add($Message) }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Get-InstallPathNames { [pscustomobject]@{ ModsFolder = 'mods' } }
        Mock Get-UnparsedModCount { 1 }
        Mock Read-ModRoster { $script:Roster }
        Mock Resolve-ClassOwner {
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

        $owners = Resolve-ChainClassOwners -InstallRoot $script:InstallRoot -ClassNames @('x.A', 'x.B') `
            -DiagnosticSource 'Test'

        $owners['x.A'] | Should -Be @('alpha')

        Should -Invoke Read-ModRoster -Times 1 -Exactly -ParameterFilter {
            $StarsectorRoot -eq $script:InstallRoot -and $DiagnosticSource -eq 'Test'
        }
        Should -Invoke Resolve-ClassOwner -Times 1 -Exactly -ParameterFilter {
            "$($Roster.Id)" -eq 'alpha Broken Mod' -and "$ClassNames" -eq 'x.A x.B'
        }

        $script:Diagnostics[0] |
            Should -Be "resolving the owners of 2 object chain classes from the install at $script:InstallRoot"
        $script:Diagnostics[1] | Should -Be 'skipped JAR C:\mods\b.jar: not on disk'
        $script:Diagnostics[2] | Should -BeLike 'read 3 JARs of 2 mods and 1 of 2 core classpath entries in * ms, *'
        $script:Diagnostics[2] | Should -BeLike '*the JARs of 1 unparsed mods unknown; owners found for 1 classes, *'
    }

    # A caller that has read the roster for its own use hands it over, so the install's mods are read once.
    It 'resolves against a given roster without reading one' {

        $given = @([pscustomobject]@{ Id = 'given' })

        Resolve-ChainClassOwners -InstallRoot $script:InstallRoot -ClassNames @('x.A') -Roster $given `
            -DiagnosticSource 'Test' | Out-Null

        Should -Invoke Read-ModRoster -Times 0
        Should -Invoke Resolve-ClassOwner -Times 1 -Exactly -ParameterFilter { "$($Roster.Id)" -eq 'given' }
    }

    It 'warns why vanilla classes stay without owners when the core classpath could not be read' {

        Mock Resolve-ClassOwner {
            [pscustomobject]@{
                OwnersByClass = @{}; ModJarCount = 0; CoreJarCount = 0; CoreClasspathEntryCount = 0
                CoreClasspathProblem = 'C:\vmparams not found'; SharedClassCount = 0; ShadowedClassCount = 0
                SkippedJars = @()
            }
        }

        Resolve-ChainClassOwners -InstallRoot $script:InstallRoot -ClassNames @('x.A') -DiagnosticSource 'Test' `
            -WarningVariable warnings -WarningAction SilentlyContinue | Out-Null

        "$warnings" | Should -Be 'C:\vmparams not found; vanilla classes stay without owners'
    }

    It 'resolves nothing, and says so, when no class is asked for' {

        $owners = Resolve-ChainClassOwners -InstallRoot $script:InstallRoot -ClassNames @() -DiagnosticSource 'Test'

        $owners.Count | Should -Be 0
        $script:Diagnostics | Should -Be @('no listed failure names an object chain; resolving no class owners')

        Should -Invoke Read-ModRoster -Times 0
        Should -Invoke Resolve-ClassOwner -Times 0
    }

    # The game's own classes need no mods folder, so they still find their owner.
    It 'resolves against no mods, and warns, when the install has no mods folder' {

        $bareRoot = Join-Path $TestDrive 'bare'

        Resolve-ChainClassOwners -InstallRoot $bareRoot -ClassNames @('x.A') -DiagnosticSource 'Test' `
            -WarningVariable warnings -WarningAction SilentlyContinue | Out-Null

        "$warnings" | Should -Be "no mods folder in the log's install, $bareRoot; mod classes stay without owners"

        Should -Invoke Read-ModRoster -Times 0
        Should -Invoke Resolve-ClassOwner -Times 1 -Exactly -ParameterFilter { @($Roster).Count -eq 0 }
    }
}
