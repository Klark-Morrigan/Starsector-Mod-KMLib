BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\readers\New-ContentSource.ps1"
}

Describe 'New-ContentSource' {

    It 'writes each field as given' {

        # ARRANGE
        $expectedOwner = 'alpha'
        $expectedName = 'Alpha'
        $expectedVersion = '1.2'
        $expectedFolder = 'C:\Games\mods\alpha'
        $expectedIsMod = $true
        $expectedReplacedPaths = @('data/hulls/ship_data.csv')

        # ACT
        $actualSource = New-ContentSource `
            -Owner $expectedOwner `
            -Name $expectedName `
            -Version $expectedVersion `
            -Folder $expectedFolder `
            -IsMod `
            -ReplacedPaths $expectedReplacedPaths

        # ASSERT
        $actualSource.Owner | Should -Be $expectedOwner
        $actualSource.Name | Should -Be $expectedName
        $actualSource.Version | Should -Be $expectedVersion
        $actualSource.Folder | Should -Be $expectedFolder
        $actualSource.IsMod | Should -Be $expectedIsMod
        $actualSource.ReplacedPaths | Should -Be $expectedReplacedPaths
    }

    # The catalogue's JSON writes a missing name or version as null, which an empty string would change.
    It 'keeps a missing name and version null, and defaults to the game''s own data replacing nothing' {

        # ARRANGE
        $expectedIsMod = $false
        $expectedReplacedPathCount = 0

        # ACT
        $actualSource = New-ContentSource -Owner 'core' -Folder 'C:\Games\starsector-core'

        # ASSERT
        $null -eq $actualSource.Name | Should -BeTrue
        $null -eq $actualSource.Version | Should -BeTrue
        $actualSource.IsMod | Should -Be $expectedIsMod
        $actualSource.ReplacedPaths.Count | Should -Be $expectedReplacedPathCount
    }
}
