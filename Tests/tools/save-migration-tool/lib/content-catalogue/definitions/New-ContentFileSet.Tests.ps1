BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\content-catalogue\definitions\New-ContentFileSet.ps1"
}

Describe 'New-ContentFileSet' {

    It 'describes a folder of files by how their names end, without subfolders unless asked' {

        # ARRANGE
        $expectedFolder = 'data/hulls'
        $expectedEnding = 'ship'
        $expectedIdKey = 'hullId'

        # ACT
        $actualFileSet = New-ContentFileSet -Folder 'data/hulls' -Ending 'ship' -IdKey 'hullId'

        # ASSERT
        $actualFileSet.Folder | Should -Be $expectedFolder
        $actualFileSet.Ending | Should -Be $expectedEnding
        $actualFileSet.IdKey | Should -Be $expectedIdKey
        $actualFileSet.HasSubfolders | Should -BeFalse
        $actualFileSet.ListedBy | Should -BeNullOrEmpty
    }

    It 'reads subfolders when asked' {

        # ACT
        $actualFileSet = New-ContentFileSet -Folder 'f' -Ending 'e' -IdKey 'k' -HasSubfolders

        # ASSERT
        $actualFileSet.HasSubfolders | Should -BeTrue
    }

    It 'describes files a spreadsheet lists' {

        # ARRANGE
        $listing = [pscustomobject]@{ Path = 'data/world/factions/factions.csv' }

        $expectedIdKey = 'id'

        # ACT
        $actualFileSet = New-ContentFileSet -ListedBy $listing -IdKey 'id'

        # ASSERT
        [object]::ReferenceEquals($actualFileSet.ListedBy, $listing) | Should -BeTrue
        $actualFileSet.Folder | Should -BeNullOrEmpty
        $actualFileSet.IdKey | Should -Be $expectedIdKey
    }
}
