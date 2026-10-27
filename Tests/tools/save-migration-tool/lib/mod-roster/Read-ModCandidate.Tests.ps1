BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\mod-info\ConvertFrom-StarsectorJson.ps1"
    . "$libRoot\mod-info\Get-JsonPropertyValue.ps1"
    . "$libRoot\mod-info\Get-VersionSortKey.ps1"
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
        Mock Get-VersionSortKey { "key-$Version" }

        # Strict mode throws on a missing property, so the fake looks the name up first.
        Mock Get-JsonPropertyValue {

            $property = $null

            if ($null -ne $Object) {
                $property = $Object.PSObject.Properties[$Name]
            }

            if ($null -ne $property) {
                $property.Value
            }
        }
    }

    It 'reads the ID, name, version and sort key of a parsed mod_info.json' {

        Mock ConvertFrom-StarsectorJson { [pscustomobject]@{ id = 'alpha'; name = ' Alpha'; version = '1.2' } }

        $folder = New-ModFolder -Name 'Alpha-1.2'
        $candidate = Read-ModCandidate -Folder $folder

        $candidate.Id | Should -Be 'alpha'
        $candidate.Name | Should -Be ' Alpha'
        $candidate.Version | Should -Be '1.2'
        $candidate.SortKey | Should -Be 'key-1.2'
        $candidate.Status | Should -Be 'P'
        $candidate.Folder.FullName | Should -Be $folder.FullName
        $candidate.Jars.Count | Should -Be 0
        $candidate.SortString | Should -BeNullOrEmpty
        $candidate.ReplacedPaths.Count | Should -Be 0
    }

    It 'reads the text the game sorts the mod by' {

        Mock ConvertFrom-StarsectorJson { [pscustomobject]@{ id = 'alpha'; sortString = 'zz last' } }

        (Read-ModCandidate -Folder (New-ModFolder -Name 'Alpha-sort')).SortString | Should -Be 'zz last'
    }

    # The game ignores a replaced settings.json, whichever folder it names.
    It 'keeps the paths the mod replaces, leaving out empty ones and settings.json' {

        Mock ConvertFrom-StarsectorJson {
            [pscustomobject]@{
                id      = 'alpha'
                replace = @('data/hulls/ship_data.csv', '', 'data/config/settings.json', 'data\weapons\a.wpn')
            }
        }

        $candidate = Read-ModCandidate -Folder (New-ModFolder -Name 'Alpha-replace')

        $candidate.ReplacedPaths | Should -Be @('data/hulls/ship_data.csv', 'data\weapons\a.wpn')
    }

    It 'keeps the JARs mod_info.json declares, as it writes them' {

        Mock ConvertFrom-StarsectorJson {
            [pscustomobject]@{ id = 'alpha'; jars = @('jars/alpha.jar', '', 'jars/lib.jar') }
        }

        $candidate = Read-ModCandidate -Folder (New-ModFolder -Name 'Alpha-jars')

        $candidate.Jars | Should -Be @('jars/alpha.jar', 'jars/lib.jar')
    }

    It 'keeps <Description> under its folder name, warning why' -ForEach @(
        @{ Description = 'a file nothing parses'; Reader = { throw 'no repair produced valid JSON' }
            Reason = 'no repair produced valid JSON' }
        @{ Description = 'a file with no ID'; Reader = { [pscustomobject]@{ name = 'Nameless' } }
            Reason = 'no "id" field' }
    ) {

        Mock ConvertFrom-StarsectorJson $Reader

        $candidate = Read-ModCandidate -Folder (New-ModFolder -Name "Broken $($Reason.Length)") `
            -WarningVariable warnings -WarningAction SilentlyContinue

        $candidate.Id | Should -Be "Broken $($Reason.Length)"
        $candidate.Name | Should -BeNullOrEmpty
        $candidate.Status | Should -Be 'U'
        $candidate.Jars.Count | Should -Be 0
        $candidate.SortString | Should -BeNullOrEmpty
        $candidate.ReplacedPaths.Count | Should -Be 0
        $warnings | ForEach-Object { "$_" } |
            Should -Be @("No mod ID read from mod_info.json, keeping in roster: Broken $($Reason.Length) ($Reason)")
    }

    It 'returns null for a folder with no mod_info.json' {
        Read-ModCandidate -Folder (New-ModFolder -Name 'loose-assets' -HasNoModInfo) | Should -BeNullOrEmpty
    }
}
