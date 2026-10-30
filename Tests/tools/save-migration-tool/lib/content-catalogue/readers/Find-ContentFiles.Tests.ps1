BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\content-catalogue\readers\Find-ContentFiles.ps1"

    function script:New-Source {
        # One data folder under the case's own folder, as Get-ContentSources lists it.
        param([string] $Owner)

        return [pscustomobject]@{ Owner = $Owner; Folder = Join-Path $TestDrive "$script:Case\$Owner" }
    }

    function script:New-CaseFile {
        # Writes an empty file under the case's own folder, since TestDrive keeps every case's files.
        param([string] $Name)

        return New-TestFile -Name "$script:Case\$Name"
    }

    function script:Format-FoundFiles {
        # Writes each found file as "<relative path>: <owner of each copy>", one per file, in the order found.
        param([object[]] $Files)

        return @($Files | ForEach-Object { "$($_.RelativePath): $($_.Copies.Source.Owner -join ', ')" })
    }
}

Describe 'Find-ContentFiles' {

    BeforeEach {

        $script:Case = [guid]::NewGuid().ToString('N')
        $script:FileSet = [pscustomobject]@{ Folder = 'data/variants'; Ending = 'variant'; HasSubfolders = $true }
        $script:Sources = @((New-Source -Owner 'alpha'), (New-Source -Owner 'beta'), (New-Source -Owner 'core'))
    }

    It 'lists each path once, with the copy each source holds, in search order' {

        # ARRANGE
        $expectedFirstCopy = New-CaseFile -Name 'beta\data\variants\shared.variant'
        New-CaseFile -Name 'core\data\variants\shared.variant' | Out-Null
        New-CaseFile -Name 'core\data\variants\own.variant' | Out-Null

        $expectedFiles = @(
            'data/variants/shared.variant: beta, core'
            'data/variants/own.variant: core'
        )

        # ACT
        $actualFiles = Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet

        # ASSERT
        Format-FoundFiles -Files $actualFiles | Should -Be $expectedFiles
        $actualFiles[0].Copies[0].Path | Should -Be $expectedFirstCopy
    }

    It 'lists the folder''s own files before each subfolder, and subfolders in the order first found' {

        # ARRANGE
        New-CaseFile -Name 'core\data\variants\top.variant' | Out-Null
        New-CaseFile -Name 'beta\data\variants\fighters\wing.variant' | Out-Null
        New-CaseFile -Name 'core\data\variants\drones\drone.variant' | Out-Null
        New-CaseFile -Name 'core\data\variants\fighters\wing.variant' | Out-Null

        $expectedFiles = @(
            'data/variants/top.variant: core'
            'data/variants/fighters/wing.variant: beta, core'
            'data/variants/drones/drone.variant: core'
        )

        # ACT
        $actualFiles = Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet

        # ASSERT
        Format-FoundFiles -Files $actualFiles | Should -Be $expectedFiles
    }

    It 'reads no subfolder for a set without them' {

        # ARRANGE
        New-CaseFile -Name 'alpha\data\variants\sub\kept.variant' | Out-Null

        $script:FileSet.HasSubfolders = $false

        $expectedCount = 0

        # ACT
        $actualFiles = @(Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet)

        # ASSERT
        $actualFiles.Count | Should -Be $expectedCount
    }

    It 'reads one subfolder level down, and no further' {

        # ARRANGE
        New-CaseFile -Name 'alpha\data\variants\sub\deeper\deep.variant' | Out-Null
        New-CaseFile -Name 'alpha\data\variants\sub\kept.variant' | Out-Null

        $expectedFiles = @('data/variants/sub/kept.variant: alpha')

        # ACT
        $actualFiles = Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet

        # ASSERT
        Format-FoundFiles -Files $actualFiles | Should -Be $expectedFiles
    }

    It 'skips a subfolder whose name opens with a dot, and a hidden one' {

        # ARRANGE
        New-CaseFile -Name 'alpha\data\variants\.backup\old.variant' | Out-Null

        $hiddenFile = New-CaseFile -Name 'alpha\data\variants\hidden\old.variant'
        $hiddenFolder = Get-Item -LiteralPath (Split-Path -Parent $hiddenFile)

        $hiddenFolder.Attributes = $hiddenFolder.Attributes -bor [System.IO.FileAttributes]::Hidden

        $expectedCount = 0

        # ACT
        $actualFiles = @(Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet)

        # ASSERT
        $actualFiles.Count | Should -Be $expectedCount
    }

    # The game matches by how the name ends, with no dot: a backup named d.variant.bak is not read.
    It 'lists a file whose name ends with the set''s ending, case-sensitively and with or without a dot' {

        # ARRANGE
        New-CaseFile -Name 'alpha\data\variants\a.variant' | Out-Null
        New-CaseFile -Name 'alpha\data\variants\bvariant' | Out-Null
        New-CaseFile -Name 'alpha\data\variants\c.VARIANT' | Out-Null
        New-CaseFile -Name 'alpha\data\variants\d.variant.bak' | Out-Null

        $expectedPaths = @(
            'data/variants/a.variant'
            'data/variants/bvariant'
        )

        # ACT
        $actualFiles = Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet

        # ASSERT
        $actualFiles.RelativePath | Sort-Object | Should -Be $expectedPaths
    }

    It 'lists nothing when no source holds the folder' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualFiles = @(Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet)

        # ASSERT
        $actualFiles.Count | Should -Be $expectedCount
    }
}
