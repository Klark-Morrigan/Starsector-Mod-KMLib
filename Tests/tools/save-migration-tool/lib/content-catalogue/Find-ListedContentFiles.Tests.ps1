BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Find-ListedContentFiles.ps1"

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

        $alphaCopy = New-CaseFile -Name 'alpha\data\world\factions\b.faction'
        New-CaseFile -Name 'core\data\world\factions\b.faction' | Out-Null
        New-CaseFile -Name 'core\data\world\factions\a.faction' | Out-Null

        $files = Find-ListedContentFiles -Sources $script:Sources -RelativePaths @(
            'data/world/factions/a.faction'
            'data/world/factions/b.faction'
        )

        $files.RelativePath | Should -Be @('data/world/factions/a.faction', 'data/world/factions/b.faction')
        $files[0].Copies.Source.Owner | Should -Be @('core')
        $files[1].Copies.Source.Owner | Should -Be @('alpha', 'core')
        $files[1].Copies[0].Path | Should -Be $alphaCopy
    }

    It 'leaves out a path no source holds' {

        New-CaseFile -Name 'core\data\a.json' | Out-Null

        $files = Find-ListedContentFiles -Sources $script:Sources -RelativePaths @('data/gone.json', 'data/a.json')

        $files.RelativePath | Should -Be @('data/a.json')
    }

    It 'matches a path whatever its case or slash direction, keeping the path as listed' {

        New-CaseFile -Name 'alpha\Data\Config\Planets.json' | Out-Null

        $files = @(Find-ListedContentFiles -Sources $script:Sources -RelativePaths @('data\config\planets.json'))

        $files.RelativePath | Should -Be @('data\config\planets.json')
        $files[0].Copies.Source.Owner | Should -Be @('alpha')
    }

    It 'finds a file at the top of a data folder' {

        New-CaseFile -Name 'core\top.csv' | Out-Null

        (Find-ListedContentFiles -Sources $script:Sources -RelativePaths @('top.csv')).Copies.Source.Owner |
            Should -Be @('core')
    }
}
