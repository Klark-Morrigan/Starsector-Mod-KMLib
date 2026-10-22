BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\class-index\Resolve-ClassOwner.ps1"
    . "$libRoot\class-index\Resolve-ChainClassOwners.ps1"
    . "$libRoot\mod-roster\Get-ModStatusNames.ps1"

    # A roster script of the suite's own, standing in for Get-ModRoster.ps1: two mods, one of them unparsed.
    $script:RosterScript = Join-Path $TestDrive 'Get-FakeRoster.ps1'

    [System.IO.File]::WriteAllText($script:RosterScript, (
        "param([string] `$StarsectorRoot)`n" +
        "[pscustomobject]@{ Id = 'alpha'; Status = 'P' }`n" +
        "[pscustomobject]@{ Id = 'Broken Mod'; Status = 'U' }"))

    function script:Invoke-Resolve {
        # Resolves the given classes in an install, with this suite's roster script and source name.
        [CmdletBinding()]
        param(
            [string] $InstallRoot,
            [string[]] $ClassNames
        )

        return Resolve-ChainClassOwners -InstallRoot $InstallRoot -ClassNames $ClassNames `
            -RosterScriptPath $script:RosterScript -DiagnosticSource 'Test'
    }
}

Describe 'Resolve-ChainClassOwners' {

    BeforeEach {

        $script:InstallRoot = Join-Path $TestDrive 'install'

        New-Item -ItemType Directory -Path (Join-Path $script:InstallRoot 'mods') -Force | Out-Null

        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()

        Mock Write-DiagnosticLine { $script:Diagnostics.Add($Message) }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Get-ModStatusNames { [pscustomobject]@{ Parsed = 'P'; Unparsed = 'U' } }
        Mock Resolve-ClassOwner {
            [pscustomobject]@{
                OwnersByClass        = @{ 'x.A' = @('alpha') }
                JarCount             = 3
                CoreJarCount         = 2
                CoreClasspathProblem = $null
                SharedClassCount     = 0
                ShadowedClassCount   = 1
                SkippedJars          = @([pscustomobject]@{ Path = 'C:\mods\b.jar'; Reason = 'not on disk' })
            }
        }
    }

    It 'resolves the classes against the install''s roster, and says what it read on stderr' {

        $owners = Invoke-Resolve -InstallRoot $script:InstallRoot -ClassNames @('x.A', 'x.B')

        $owners['x.A'] | Should -Be @('alpha')

        Should -Invoke Resolve-ClassOwner -Times 1 -Exactly -ParameterFilter {
            "$($Roster.Id)" -eq 'alpha Broken Mod' -and "$ClassNames" -eq 'x.A x.B'
        }

        $script:Diagnostics[0] |
            Should -Be "resolving the owners of 2 object chain classes from the install at $script:InstallRoot"
        $script:Diagnostics[1] | Should -Be 'skipped JAR C:\mods\b.jar: not on disk'
        $script:Diagnostics[2] | Should -BeLike 'read 3 JARs of 2 mods and 2 core classpath entries in * ms, *'
        $script:Diagnostics[2] | Should -BeLike '*the JARs of 1 unparsed mods unknown; owners found for 1 classes, *'
    }

    It 'warns why vanilla classes stay without owners when the core classpath could not be read' {

        Mock Resolve-ClassOwner {
            [pscustomobject]@{
                OwnersByClass = @{}; JarCount = 0; CoreJarCount = 0; CoreClasspathProblem = 'C:\vmparams not found'
                SharedClassCount = 0; ShadowedClassCount = 0; SkippedJars = @()
            }
        }

        Invoke-Resolve -InstallRoot $script:InstallRoot -ClassNames @('x.A') -WarningVariable warnings `
            -WarningAction SilentlyContinue | Out-Null

        "$warnings" | Should -Be 'C:\vmparams not found; vanilla classes stay without owners'
    }

    It 'resolves nothing, and says so, when no class is asked for' {

        $owners = Invoke-Resolve -InstallRoot $script:InstallRoot -ClassNames @()

        $owners.Count | Should -Be 0
        $script:Diagnostics | Should -Be @('no listed failure names an object chain; resolving no class owners')

        Should -Invoke Resolve-ClassOwner -Times 0
    }

    It 'resolves nothing, and warns, when the install has no mods folder' {

        $owners = Invoke-Resolve -InstallRoot (Join-Path $TestDrive 'bare') -ClassNames @('x.A') `
            -WarningVariable warnings -WarningAction SilentlyContinue

        $owners.Count | Should -Be 0
        "$warnings" |
            Should -BeLike "no mods folder in the log's install, *bare; object chain classes stay without owners"

        Should -Invoke Resolve-ClassOwner -Times 0
    }
}
