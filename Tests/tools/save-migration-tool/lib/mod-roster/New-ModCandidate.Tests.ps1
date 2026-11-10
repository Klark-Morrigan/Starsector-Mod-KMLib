BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-roster\New-ModCandidate.ps1"
}

Describe 'New-ModCandidate' {

    It 'carries every field it is given' {

        # ARRANGE
        $folder = [System.IO.DirectoryInfo]::new(
            'C:\Games\Starsector\mods\Alpha-1.2')

        $expectedId = 'alpha'
        $expectedName = 'Alpha'
        $expectedVersion = '1.2'
        $expectedJars = @('jars/alpha.jar')
        $expectedModPlugin = 'data.scripts.AlphaModPlugin'
        $expectedSortString = 'aa alpha'
        $expectedReplacedPaths = @('data/hulls/ship_data.csv')
        $expectedFolder = 'C:\Games\Starsector\mods\Alpha-1.2'
        $expectedStatus = 'parsed'

        # ACT
        $actualCandidate = New-ModCandidate `
            -Id 'alpha' `
            -Name 'Alpha' `
            -Version '1.2' `
            -Jars @('jars/alpha.jar') `
            -ModPlugin 'data.scripts.AlphaModPlugin' `
            -SortString 'aa alpha' `
            -ReplacedPaths @('data/hulls/ship_data.csv') `
            -Folder $folder `
            -Status 'parsed'

        # ASSERT
        $actualCandidate.Id | Should -Be $expectedId
        $actualCandidate.Name | Should -Be $expectedName
        $actualCandidate.Version | Should -Be $expectedVersion
        $actualCandidate.Jars | Should -Be $expectedJars
        $actualCandidate.ModPlugin | Should -Be $expectedModPlugin
        $actualCandidate.SortString | Should -Be $expectedSortString
        $actualCandidate.ReplacedPaths | Should -Be $expectedReplacedPaths
        $actualCandidate.Folder.FullName | Should -Be $expectedFolder
        $actualCandidate.Status | Should -Be $expectedStatus
    }

    # A copy the game skips is built from its ID, folder and status alone.
    It 'leaves the fields it is not given null, and the lists empty' {

        # ARRANGE
        $folder = [System.IO.DirectoryInfo]::new(
            'C:\Games\Starsector\mods\Broken')

        $expectedJarCount = 0
        $expectedReplacedPathCount = 0

        # ACT
        $actualCandidate = New-ModCandidate `
            -Id 'Broken' `
            -Folder $folder `
            -Status 'unparsed'

        # ASSERT
        $actualCandidate.Name | Should -BeNullOrEmpty
        $actualCandidate.Version | Should -BeNullOrEmpty
        $actualCandidate.ModPlugin | Should -BeNullOrEmpty
        $actualCandidate.SortString | Should -BeNullOrEmpty
        $actualCandidate.Jars.Count | Should -Be $expectedJarCount
        $actualCandidate.ReplacedPaths.Count | Should -Be $expectedReplacedPathCount
    }
}
