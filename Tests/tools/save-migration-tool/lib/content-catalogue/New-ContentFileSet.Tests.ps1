BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\New-ContentFileSet.ps1"
}

Describe 'New-ContentFileSet' {

    It 'describes a folder of files by how their names end, without subfolders unless asked' {

        $fileSet = New-ContentFileSet -Folder 'data/hulls' -Ending 'ship' -IdKey 'hullId'

        $fileSet.Folder | Should -Be 'data/hulls'
        $fileSet.Ending | Should -Be 'ship'
        $fileSet.IdKey | Should -Be 'hullId'
        $fileSet.HasSubfolders | Should -BeFalse
        $fileSet.ListedBy | Should -BeNullOrEmpty
        (New-ContentFileSet -Folder 'f' -Ending 'e' -IdKey 'k' -HasSubfolders).HasSubfolders | Should -BeTrue
    }

    It 'describes files a spreadsheet lists' {

        $listing = [pscustomobject]@{ Path = 'data/world/factions/factions.csv' }

        $fileSet = New-ContentFileSet -ListedBy $listing -IdKey 'id'

        [object]::ReferenceEquals($fileSet.ListedBy, $listing) | Should -BeTrue
        $fileSet.Folder | Should -BeNullOrEmpty
        $fileSet.IdKey | Should -Be 'id'
    }
}
