BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\data-file\Test-JsonProperty.ps1"
}

Describe 'Test-JsonProperty' {

    It 'finds a key of a <Description>' -ForEach @(
        @{ Description = 'hashtable'; Object = @{ id = 'a' } }
        @{ Description = 'ordered dictionary, which has no ContainsKey'; Object = [ordered]@{ id = 'a' } }
    ) {

        # ARRANGE
        $expectedIsPresent = $true

        # ACT
        $actualIsPresent = Test-JsonProperty -Object $Object -Name 'id'

        # ASSERT
        $actualIsPresent | Should -Be $expectedIsPresent
    }

    # org.json's has() holds a key written as null, and getString reads it without throwing.
    It 'finds a key whose value is null' {

        # ARRANGE
        $object = [ordered]@{ description = $null }
        $expectedIsPresent = $true

        # ACT
        $actualIsPresent = Test-JsonProperty -Object $object -Name 'description'

        # ASSERT
        $actualIsPresent | Should -Be $expectedIsPresent
    }

    # A string's Length is no key of the file, though PowerShell would read it as a property.
    It 'finds no key for <Description>' -ForEach @(
        @{ Description = 'a key the hashtable lacks'; Object = @{ id = 'a' }; Name = 'name' }
        @{ Description = 'no object'; Object = $null; Name = 'name' }
        @{ Description = 'a string where an object was expected'; Object = 'text'; Name = 'Length' }
        @{ Description = 'a list where an object was expected'; Object = @('a', 'b'); Name = 'Count' }
    ) {

        # ARRANGE
        $expectedIsPresent = $false

        # ACT
        $actualIsPresent = Test-JsonProperty -Object $Object -Name $Name

        # ASSERT
        $actualIsPresent | Should -Be $expectedIsPresent
    }

    # A hashtable parsed as org.json keys its objects matches as they do, case-sensitively.
    It 'answers <Expected> for <Name> in a case-sensitive hashtable holding <Description>' -ForEach @(
        @{ Description = 'that exact spelling'; Name = 'A'; Expected = $true }
        @{ Description = 'only another case'; Name = 'B'; Expected = $false }
    ) {

        # ARRANGE
        $object = [System.Collections.Generic.Dictionary[string, object]]::new()
        $object.Add('a', 'lower')
        $object.Add('A', 'upper')
        $object.Add('b', 'other')
        $expectedIsPresent = $Expected

        # ACT
        $actualIsPresent = Test-JsonProperty -Object $object -Name $Name

        # ASSERT
        $actualIsPresent | Should -Be $expectedIsPresent
    }
}
