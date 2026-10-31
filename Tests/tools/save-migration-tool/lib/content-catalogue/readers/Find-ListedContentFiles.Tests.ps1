BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\content-catalogue\readers\Find-ListedContentFiles.ps1"

    function script:New-Source {
        # One data folder under the case's own folder.
        param([string] $Owner)

        return [pscustomobject]@{ Owner = $Owner; Folder = Join-Path $TestDrive "$script:Case\$Owner" }
    }

    function script:New-CaseFile {
        # Writes an empty file under the case's own folder, since TestDrive keeps every case's files.
        param([string] $Name)

        return New-TestFile -Name "$script:Case\$Name"
    }
}

Describe 'Find-ListedContentFiles' {

    BeforeEach {

        $script:Case = [guid]::NewGuid().ToString('N')
        $script:Sources = @((New-Source -Owner 'alpha'), (New-Source -Owner 'core'))
    }

    It 'gives each listed path the copy each source holds, in search order, paths in the order given' {

        # ARRANGE
        $expectedFirstCopy = New-CaseFile -Name 'alpha\data\world\factions\b.faction'
        New-CaseFile -Name 'core\data\world\factions\b.faction' | Out-Null
        New-CaseFile -Name 'core\data\world\factions\a.faction' | Out-Null

        $relativePaths = @(
            'data/world/factions/a.faction'
            'data/world/factions/b.faction'
        )

        $expectedPaths = @('data/world/factions/a.faction', 'data/world/factions/b.faction')
        $expectedFirstOwners = @('core')
        $expectedSecondOwners = @('alpha', 'core')

        # ACT
        $actualFiles = Find-ListedContentFiles -Sources $script:Sources -RelativePaths $relativePaths

        # ASSERT
        $actualFiles.RelativePath | Should -Be $expectedPaths
        $actualFiles[0].Copies.Source.Owner | Should -Be $expectedFirstOwners
        $actualFiles[1].Copies.Source.Owner | Should -Be $expectedSecondOwners
        $actualFiles[1].Copies[0].Path | Should -Be $expectedFirstCopy
    }

    It 'leaves out a path no source holds' {

        # ARRANGE
        New-CaseFile -Name 'core\data\a.json' | Out-Null

        $relativePaths = @('data/gone.json', 'data/a.json')

        $expectedPaths = @('data/a.json')

        # ACT
        $actualFiles = Find-ListedContentFiles -Sources $script:Sources -RelativePaths $relativePaths

        # ASSERT
        $actualFiles.RelativePath | Should -Be $expectedPaths
    }

    It 'matches a path whatever its case or slash direction, keeping the path as listed' {

        # ARRANGE
        New-CaseFile -Name 'alpha\Data\Config\Planets.json' | Out-Null

        $expectedPaths = @('data\config\planets.json')
        $expectedOwners = @('alpha')

        # ACT
        $actualFiles = @(Find-ListedContentFiles -Sources $script:Sources -RelativePaths @('data\config\planets.json'))

        # ASSERT
        $actualFiles.RelativePath | Should -Be $expectedPaths
        $actualFiles[0].Copies.Source.Owner | Should -Be $expectedOwners
    }

    It 'finds a file at the top of a data folder' {

        # ARRANGE
        New-CaseFile -Name 'core\top.csv' | Out-Null

        $expectedOwners = @('core')

        # ACT
        $actualFiles = Find-ListedContentFiles -Sources $script:Sources -RelativePaths @('top.csv')

        # ASSERT
        $actualFiles.Copies.Source.Owner | Should -Be $expectedOwners
    }
}
