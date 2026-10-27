BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\install\Get-InstallPathNames.ps1"
    . "$libRoot\mod-roster\Read-ModCandidate.ps1"
    . "$libRoot\mod-roster\Read-ModFolderCandidates.ps1"
}

Describe 'Read-ModFolderCandidates' {

    BeforeEach {

        $script:ModsRoot = Join-Path $TestDrive "mods-$([guid]::NewGuid())"
        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()

        foreach ($folder in 'alpha', 'beta', 'loose-assets') {
            New-Item -ItemType Directory -Path (Join-Path $script:ModsRoot $folder) -Force | Out-Null
        }

        Mock Write-DiagnosticLine { $script:Diagnostics.Add("${Source}: $Message") }
        Mock Format-ConsoleText { $Text }
        Mock Format-ConsoleNumber { "$Number" }
        Mock Get-InstallPathNames { [pscustomobject]@{ ModInfoFile = 'mod_info.json' } }

        # A folder named loose-assets holds no mod_info.json; every other folder is a mod named after it.
        Mock Read-ModCandidate {
            if ($Folder.Name -ne 'loose-assets') { [pscustomobject]@{ Id = $Folder.Name } }
        }
    }

    It 'reads each folder holding a mod as a candidate' {

        $candidates = Read-ModFolderCandidates -ModsRoot $script:ModsRoot -DiagnosticSource 'Test'

        $candidates.Id | Sort-Object | Should -Be @('alpha', 'beta')
    }

    It 'names the folders holding no mod_info.json on stderr' {

        Read-ModFolderCandidates -ModsRoot $script:ModsRoot -DiagnosticSource 'Test' | Out-Null

        $script:Diagnostics | Should -Be @('Test: skipped 1 folders with no mod_info.json: loose-assets')
    }

    It 'says nothing when every folder holds a mod' {

        Remove-Item -LiteralPath (Join-Path $script:ModsRoot 'loose-assets')

        Read-ModFolderCandidates -ModsRoot $script:ModsRoot -DiagnosticSource 'Test' | Out-Null

        $script:Diagnostics.Count | Should -Be 0
    }

    # A caller wraps nothing round the result, so an empty folder must still come back as a list.
    It 'returns an empty list for a mods folder with no folders' {

        $emptyRoot = Join-Path $TestDrive 'empty-mods'

        New-Item -ItemType Directory -Path $emptyRoot | Out-Null

        $candidates = Read-ModFolderCandidates -ModsRoot $emptyRoot -DiagnosticSource 'Test'

        $null -eq $candidates | Should -BeFalse
        $candidates.Count | Should -Be 0
    }
}
