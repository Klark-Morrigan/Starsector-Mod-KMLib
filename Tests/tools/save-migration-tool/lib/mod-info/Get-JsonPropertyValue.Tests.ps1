BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-JsonPropertyValue.ps1"
}

Describe 'Get-JsonPropertyValue' {

    It 'reads a property an object holds' {
        Get-JsonPropertyValue -Object ([pscustomobject]@{ id = 'a' }) -Name 'id' | Should -Be 'a'
    }

    It 'reads null for <Description>' -ForEach @(
        @{ Description = 'a property the object lacks'; Object = [pscustomobject]@{ id = 'a' } }
        @{ Description = 'no object'; Object = $null }
        @{ Description = 'a key the hashtable lacks'; Object = @{ id = 'a' } }
    ) {
        Get-JsonPropertyValue -Object $Object -Name 'name' | Should -BeNullOrEmpty
    }

    It 'reads a key of a <Description>' -ForEach @(
        @{ Description = 'hashtable'; Object = @{ id = 'a' } }
        @{ Description = 'ordered dictionary, which has no ContainsKey'; Object = [ordered]@{ id = 'a' } }
    ) {
        Get-JsonPropertyValue -Object $Object -Name 'id' | Should -Be 'a'
    }

    # A hashtable parsed as org.json keys its objects matches as they do, case-sensitively.
    It 'matches a key of a case-sensitive hashtable case-sensitively' {

        $object = [System.Collections.Generic.Dictionary[string, object]]::new()
        $object.Add('a', 'lower')
        $object.Add('A', 'upper')

        Get-JsonPropertyValue -Object $object -Name 'A' | Should -Be 'upper'
        Get-JsonPropertyValue -Object $object -Name 'b' | Should -BeNullOrEmpty
    }
}
