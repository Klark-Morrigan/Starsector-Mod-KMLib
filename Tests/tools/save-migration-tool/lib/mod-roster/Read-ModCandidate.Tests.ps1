BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\mod-info\ConvertFrom-StarsectorJson.ps1"
    . "$libRoot\mod-info\Get-JsonPropertyValue.ps1"
    . "$libRoot\mod-roster\Get-ModStatusNames.ps1"
    . "$libRoot\mod-roster\Read-ModCandidate.ps1"
    . "$libRoot\install\Get-InstallPathNames.ps1"

    function script:New-ModFolder {
        # Writes a mod folder under TestDrive, holding an empty mod_info.json unless told not to, and returns it.
        param(
            [string] $Name,
            [switch] $HasNoModInfo
        )

        $folder = New-Item -ItemType Directory -Path (Join-Path $TestDrive $Name) -Force

        if (-not $HasNoModInfo) {
            New-TestFile -Name "$Name\mod_info.json" | Out-Null
        }

        return $folder
    }
}

Describe 'Read-ModCandidate' {

    BeforeEach {

        Mock Get-ModStatusNames { [pscustomobject]@{ Parsed = 'P'; Unparsed = 'U' } }
        Mock Get-InstallPathNames { [pscustomobject]@{ ModInfoFile = 'mod_info.json' } }

        # The parser hands back ordered hashtables, so the fake looks the key up in one.
        Mock Get-JsonPropertyValue {

            if ($null -ne $Object -and $Object.Contains($Name)) {
                $Object[$Name]
            }
        }
    }

    # The game's parser keeps keys that differ only in case apart, and only hashtables can hold both.
    It 'parses mod_info.json into hashtables' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson { [ordered]@{ id = 'alpha' } }

        $folder = New-ModFolder -Name 'Alpha-hashtable'

        # ACT
        Read-ModCandidate -Folder $folder | Out-Null

        # ASSERT
        Should -Invoke ConvertFrom-StarsectorJson -Times 1 -Exactly -ParameterFilter { $AsHashtable }
    }

    It 'reads the ID, name and version of a parsed mod_info.json' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson { [ordered]@{ id = 'alpha'; name = ' Alpha'; version = '1.2' } }

        $folder = New-ModFolder -Name 'Alpha-1.2'
        $expectedId = 'alpha'
        $expectedName = ' Alpha'
        $expectedVersion = '1.2'
        $expectedStatus = 'P'
        $expectedFolder = $folder.FullName
        $expectedJarCount = 0
        $expectedReplacedPathCount = 0

        # ACT
        $actualCandidate = Read-ModCandidate -Folder $folder

        # ASSERT
        $actualCandidate.Id | Should -Be $expectedId
        $actualCandidate.Name | Should -Be $expectedName
        $actualCandidate.Version | Should -Be $expectedVersion
        $actualCandidate.Status | Should -Be $expectedStatus
        $actualCandidate.Folder.FullName | Should -Be $expectedFolder
        $actualCandidate.Jars.Count | Should -Be $expectedJarCount
        $actualCandidate.SortString | Should -BeNullOrEmpty
        $actualCandidate.ReplacedPaths.Count | Should -Be $expectedReplacedPathCount
    }

    It 'reads the text the game sorts the mod by' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson { [ordered]@{ id = 'alpha'; sortString = 'zz last' } }

        $folder = New-ModFolder -Name 'Alpha-sort'
        $expectedSortString = 'zz last'

        # ACT
        $actualCandidate = Read-ModCandidate -Folder $folder

        # ASSERT
        $actualCandidate.SortString | Should -Be $expectedSortString
    }

    # The game ignores a replaced settings.json, whichever folder it names.
    It 'keeps the paths the mod replaces, leaving out empty ones and settings.json' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson {
            [ordered]@{
                id      = 'alpha'
                replace = @('data/hulls/ship_data.csv', '', 'data/config/settings.json', 'data\weapons\a.wpn')
            }
        }

        $folder = New-ModFolder -Name 'Alpha-replace'
        $expectedReplacedPaths = @('data/hulls/ship_data.csv', 'data\weapons\a.wpn')

        # ACT
        $actualCandidate = Read-ModCandidate -Folder $folder

        # ASSERT
        $actualCandidate.ReplacedPaths | Should -Be $expectedReplacedPaths
    }

    It 'keeps the JARs mod_info.json declares, as it writes them' {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson {
            [ordered]@{ id = 'alpha'; jars = @('jars/alpha.jar', '', 'jars/lib.jar') }
        }

        $folder = New-ModFolder -Name 'Alpha-jars'
        $expectedJars = @('jars/alpha.jar', 'jars/lib.jar')

        # ACT
        $actualCandidate = Read-ModCandidate -Folder $folder

        # ASSERT
        $actualCandidate.Jars | Should -Be $expectedJars
    }

    # A filter that hides a real mod is worse than no filter, because nothing signals that it happened.
    It 'keeps <Description> under its folder name, warning why' -ForEach @(
        @{ Description = 'a file nothing parses'; Reader = { throw 'no repair produced valid JSON' }
            Reason = 'no repair produced valid JSON' }
        @{ Description = 'a file with no ID'; Reader = { [ordered]@{ name = 'Nameless' } }
            Reason = 'no "id" field' }
    ) {

        # ARRANGE
        Mock ConvertFrom-StarsectorJson $Reader

        $folderName = "Broken $($Reason.Length)"
        $folder = New-ModFolder -Name $folderName
        $expectedId = $folderName
        $expectedStatus = 'U'
        $expectedJarCount = 0
        $expectedReplacedPathCount = 0
        $expectedWarnings = @("No mod ID read from mod_info.json, keeping in roster: $folderName ($Reason)")

        # ACT
        $actualCandidate = Read-ModCandidate -Folder $folder `
            -WarningVariable actualWarnings -WarningAction SilentlyContinue

        # ASSERT
        $actualCandidate.Id | Should -Be $expectedId
        $actualCandidate.Name | Should -BeNullOrEmpty
        $actualCandidate.Status | Should -Be $expectedStatus
        $actualCandidate.Jars.Count | Should -Be $expectedJarCount
        $actualCandidate.SortString | Should -BeNullOrEmpty
        $actualCandidate.ReplacedPaths.Count | Should -Be $expectedReplacedPathCount
        $actualWarnings | ForEach-Object { "$_" } | Should -Be $expectedWarnings
    }

    It 'returns null for a folder with no mod_info.json' {

        # ARRANGE
        $folder = New-ModFolder -Name 'loose-assets' -HasNoModInfo

        # ACT
        $actualCandidate = Read-ModCandidate -Folder $folder

        # ASSERT
        $actualCandidate | Should -BeNullOrEmpty
    }
}
