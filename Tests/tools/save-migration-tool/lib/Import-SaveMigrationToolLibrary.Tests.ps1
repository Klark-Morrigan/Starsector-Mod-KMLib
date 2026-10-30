BeforeAll {

    Set-StrictMode -Version Latest

    $script:LibraryRoot = [System.IO.Path]::GetFullPath("$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib")
    $script:LoaderPath = Join-Path $script:LibraryRoot 'Import-SaveMigrationToolLibrary.ps1'
}

Describe 'Import-SaveMigrationToolLibrary' {

    It 'defines every lib function in the scope that dot-sources it' {

        # ARRANGE
        $expectedCommandNames = @('Write-DiagnosticLine', 'Read-ModRoster')

        # ACT
        $actualCommandNames = & {
            . $script:LoaderPath

            @(
                Get-Command -Name 'Write-DiagnosticLine' -CommandType Function -ErrorAction Ignore
                Get-Command -Name 'Read-ModRoster' -CommandType Function -ErrorAction Ignore
            ).Name
        }

        # ASSERT
        $actualCommandNames | Should -Be $expectedCommandNames
    }

    It 'leaves the marker a script it runs reads' {

        # ARRANGE
        $expectedMarker = $script:LibraryRoot

        # ACT
        $actualMarker = & {
            . $script:LoaderPath

            Get-Variable -Name 'SaveMigrationToolLibraryRoot' -ValueOnly
        }

        # ASSERT
        $actualMarker | Should -Be $expectedMarker
    }

    # A script one script runs sees its caller's functions already; loading them again costs half a second.
    It 'loads nothing when its caller has loaded the library already' {

        # ARRANGE
        Set-Variable -Name 'SaveMigrationToolLibraryRoot' -Value $script:LibraryRoot

        # ACT
        $actualCommand = & {
            . $script:LoaderPath

            Get-Command -Name 'Read-ModRoster' -CommandType Function -ErrorAction Ignore
        }

        # ASSERT
        $actualCommand | Should -BeNullOrEmpty
    }
}
