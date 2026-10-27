BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\Write-DiagnosticLine.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\install\Get-InstallPathNames.ps1"
    . "$libRoot\mod-roster\Read-ModCandidate.ps1"
    . "$libRoot\mod-roster\Read-ModFolderCandidates.ps1"

    function script:New-ModsRoot {
        # Writes a mods folder under TestDrive holding an empty folder of each given name, and returns its path.
        param([string[]] $Folders)

        $root = Join-Path $TestDrive "mods-$([guid]::NewGuid())"

        New-Item -ItemType Directory -Path $root | Out-Null

        foreach ($folder in $Folders) {
            New-Item -ItemType Directory -Path (Join-Path $root $folder) | Out-Null
        }

        return $root
    }
}

Describe 'Read-ModFolderCandidates' {

    BeforeEach {

        $script:Diagnostics = [System.Collections.Generic.List[string]]::new()

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

        # ARRANGE
        $modsRoot = New-ModsRoot -Folders 'alpha', 'beta', 'loose-assets'
        $expectedIds = @('alpha', 'beta')

        # ACT
        $actualCandidates = Read-ModFolderCandidates -ModsRoot $modsRoot -DiagnosticSource 'Test'

        # ASSERT
        $actualCandidates.Id | Should -Be $expectedIds
    }

    # The game keeps the first copy of a mod in the file system's order.
    # NTFS lists "[" after letters, where a sort by name puts it first.
    It 'reads the folders in the order the file system lists them' {

        # ARRANGE
        $modsRoot = New-ModsRoot -Folders '[Repack] Alpha', 'Alpha'
        $expectedIds = @('Alpha', '[Repack] Alpha')

        # ACT
        $actualCandidates = Read-ModFolderCandidates -ModsRoot $modsRoot -DiagnosticSource 'Test'

        # ASSERT
        $actualCandidates.Id | Should -BeExactly $expectedIds
    }

    # The game reads a hidden folder as it reads any other.
    It 'reads a hidden folder' {

        # ARRANGE
        $modsRoot = New-ModsRoot -Folders 'hidden'
        $hiddenFolder = Get-Item -LiteralPath (Join-Path $modsRoot 'hidden')

        $hiddenFolder.Attributes = $hiddenFolder.Attributes -bor [System.IO.FileAttributes]::Hidden

        $expectedIds = @('hidden')

        # ACT
        $actualCandidates = Read-ModFolderCandidates -ModsRoot $modsRoot -DiagnosticSource 'Test'

        # ASSERT
        $actualCandidates.Id | Should -Be $expectedIds
    }

    Context 'a mods folder given relative to the current location' {

        BeforeEach {

            $script:ModsRoot = New-ModsRoot -Folders 'alpha'

            Push-Location -LiteralPath (Split-Path -Parent $script:ModsRoot)
        }

        AfterEach {
            Pop-Location
        }

        # PowerShell's location is not the process's working folder, which .NET resolves a relative path against.
        It 'reads it from PowerShell''s location' {

            # ARRANGE
            $relativeRoot = Split-Path -Leaf $script:ModsRoot
            $expectedIds = @('alpha')

            # ACT
            $actualCandidates = Read-ModFolderCandidates -ModsRoot $relativeRoot -DiagnosticSource 'Test'

            # ASSERT
            $actualCandidates.Id | Should -Be $expectedIds
        }
    }

    It 'names the folders holding no mod_info.json on stderr' {

        # ARRANGE
        $modsRoot = New-ModsRoot -Folders 'alpha', 'loose-assets'
        $expectedDiagnostics = @('Test: skipped 1 folders with no mod_info.json: loose-assets')

        # ACT
        Read-ModFolderCandidates -ModsRoot $modsRoot -DiagnosticSource 'Test' | Out-Null

        # ASSERT
        $script:Diagnostics | Should -Be $expectedDiagnostics
    }

    It 'says nothing when every folder holds a mod' {

        # ARRANGE
        $modsRoot = New-ModsRoot -Folders 'alpha', 'beta'
        $expectedDiagnosticCount = 0

        # ACT
        Read-ModFolderCandidates -ModsRoot $modsRoot -DiagnosticSource 'Test' | Out-Null

        # ASSERT
        $script:Diagnostics.Count | Should -Be $expectedDiagnosticCount
    }

    # A caller wraps nothing round the result, so an empty folder must still come back as a list.
    It 'returns an empty list for a mods folder with no folders' {

        # ARRANGE
        $modsRoot = New-ModsRoot -Folders @()
        $expectedCount = 0

        # ACT
        $actualCandidates = Read-ModFolderCandidates -ModsRoot $modsRoot -DiagnosticSource 'Test'

        # ASSERT
        $null -eq $actualCandidates | Should -BeFalse
        $actualCandidates.Count | Should -Be $expectedCount
    }
}
