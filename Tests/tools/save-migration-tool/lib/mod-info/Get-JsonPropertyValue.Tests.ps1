BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Get-JsonPropertyValue.ps1"
}

Describe 'Get-JsonPropertyValue' {

    It 'reads a key of a <Description>' -ForEach @(
        @{ Description = 'hashtable'; Object = @{ id = 'a' } }
        @{ Description = 'ordered dictionary, which has no ContainsKey'; Object = [ordered]@{ id = 'a' } }
    ) {

        # ARRANGE
        $expectedValue = 'a'

        # ACT
        $actualValue = Get-JsonPropertyValue -Object $Object -Name 'id'

        # ASSERT
        $actualValue | Should -Be $expectedValue
    }

    # A string's Length is no key of the file, though PowerShell would read it as a property.
    It 'reads null for <Description>' -ForEach @(
        @{ Description = 'a key the hashtable lacks'; Object = @{ id = 'a' }; Name = 'name' }
        @{ Description = 'no object'; Object = $null; Name = 'name' }
        @{ Description = 'a string where an object was expected'; Object = 'text'; Name = 'Length' }
        @{ Description = 'a list where an object was expected'; Object = @('a', 'b'); Name = 'Count' }
    ) {

        # ARRANGE
        $expectedValue = $null

        # ACT
        $actualValue = Get-JsonPropertyValue -Object $Object -Name $Name

        # ASSERT
        $actualValue | Should -Be $expectedValue
    }

    # A hashtable parsed as org.json keys its objects matches as they do, case-sensitively.
    It 'reads <Name> from a case-sensitive hashtable as <Description>' -ForEach @(
        @{ Description = 'the value under that exact spelling'; Name = 'A'; Value = 'upper' }
        @{ Description = 'null when only another case is held'; Name = 'B'; Value = $null }
    ) {

        # ARRANGE
        $object = [System.Collections.Generic.Dictionary[string, object]]::new()
        $object.Add('a', 'lower')
        $object.Add('A', 'upper')
        $object.Add('b', 'other')
        $expectedValue = $Value

        # ACT
        $actualValue = Get-JsonPropertyValue -Object $object -Name $Name

        # ASSERT
        $actualValue | Should -Be $expectedValue
    }
}
