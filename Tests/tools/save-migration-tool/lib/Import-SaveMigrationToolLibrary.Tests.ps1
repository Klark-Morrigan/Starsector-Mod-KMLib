BeforeAll {

    Set-StrictMode -Version Latest

    $script:LibraryRoot = [System.IO.Path]::GetFullPath("$PSScriptRoot\..\..\..\..\tools\save-migration-tool\lib")
    $script:LoaderPath = Join-Path $script:LibraryRoot 'Import-SaveMigrationToolLibrary.ps1'
}

Describe 'Import-SaveMigrationToolLibrary' {

    It 'defines every lib function in the scope that dot-sources it' {

        & {
            . $script:LoaderPath

            Get-Command -Name 'Write-DiagnosticLine' -CommandType Function | Should -Not -BeNullOrEmpty
            Get-Command -Name 'Read-ModRoster' -CommandType Function | Should -Not -BeNullOrEmpty
        }
    }

    It 'leaves the marker a script it runs reads' {

        & {
            . $script:LoaderPath

            Get-Variable -Name 'SaveMigrationToolLibraryRoot' -ValueOnly | Should -Be $script:LibraryRoot
        }
    }

    # A script one script runs sees its caller's functions already; loading them again costs half a second.
    It 'loads nothing when its caller has loaded the library already' {

        & {
            Set-Variable -Name 'SaveMigrationToolLibraryRoot' -Value $script:LibraryRoot

            & {
                . $script:LoaderPath

                Get-Command -Name 'Read-ModRoster' -CommandType Function -ErrorAction Ignore | Should -BeNullOrEmpty
            }
        }
    }
}
