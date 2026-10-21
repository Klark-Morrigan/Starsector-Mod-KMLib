BeforeAll {

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\mod-info\ConvertFrom-StarsectorJson.ps1"
    . "$libRoot\mod-info\Get-JsonPropertyValue.ps1"
    . "$libRoot\mod-info\Get-VersionSortKey.ps1"
    . "$libRoot\mod-roster\Get-ModStatusNames.ps1"
    . "$libRoot\mod-roster\Read-ModCandidate.ps1"

    function script:New-ModFolder {
        # Writes a mod folder under TestDrive, holding an empty mod_info.json unless told not to, and returns it.
        param(
            [string] $Name,
            [switch] $HasNoModInfo
        )

        $folder = New-Item -ItemType Directory -Path (Join-Path $TestDrive $Name)

        if (-not $HasNoModInfo) {
            New-Item -ItemType File -Path (Join-Path $folder.FullName 'mod_info.json') | Out-Null
        }

        return $folder
    }
}

Describe 'Read-ModCandidate' {

    BeforeEach {

        Mock Get-ModStatusNames { [pscustomobject]@{ Parsed = 'P'; Unparsed = 'U' } }
        # Strict mode throws on a missing property, as CI runs the suites, so the fake looks the name up first.
        Mock Get-JsonPropertyValue {
            $property = if ($null -ne $Object) { $Object.PSObject.Properties[$Name] }
            if ($null -ne $property) { $property.Value }
        }
        Mock Get-VersionSortKey { "key-$Version" }
    }

    It 'reads the id, name, version and sort key of a parsed mod_info.json' {

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
    }

    It 'keeps the jars mod_info.json declares, as it writes them' {

        Mock ConvertFrom-StarsectorJson {
            [pscustomobject]@{ id = 'alpha'; jars = @('jars/alpha.jar', '', 'jars/lib.jar') }
        }

        $candidate = Read-ModCandidate -Folder (New-ModFolder -Name 'Alpha-jars')

        $candidate.Jars | Should -Be @('jars/alpha.jar', 'jars/lib.jar')
    }

    It 'keeps <Description> under its folder name, warning why' -ForEach @(
        @{ Description = 'a file nothing parses'; Reader = { throw 'no repair produced valid JSON' }
            Reason = 'no repair produced valid JSON' }
        @{ Description = 'a file with no id'; Reader = { [pscustomobject]@{ name = 'Nameless' } }
            Reason = 'no "id" field' }
    ) {

        Mock ConvertFrom-StarsectorJson $Reader

        $candidate = Read-ModCandidate -Folder (New-ModFolder -Name "Broken $($Reason.Length)") `
            -WarningVariable warnings -WarningAction SilentlyContinue

        $candidate.Id | Should -Be "Broken $($Reason.Length)"
        $candidate.Name | Should -BeNullOrEmpty
        $candidate.Status | Should -Be 'U'
        $candidate.Jars.Count | Should -Be 0
        $warnings | ForEach-Object { "$_" } |
            Should -Be @("Unparseable mod_info.json, keeping in roster: Broken $($Reason.Length) ($Reason)")
    }

    It 'returns null for a folder with no mod_info.json' {
        Read-ModCandidate -Folder (New-ModFolder -Name 'loose-assets' -HasNoModInfo) | Should -BeNullOrEmpty
    }
}
