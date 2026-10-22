BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-info\Test-JsonSlashComment.ps1"
}

Describe 'Test-JsonSlashComment' {

    It 'finds <Description>' -ForEach @(
        @{ Description = 'a // comment'; Text = "{ `"id`": `"a`" // note`n}" }
        @{ Description = 'a /* */ comment'; Text = '{ /* note */ "id": "a" }' }
        @{ Description = 'a comment after a string holding an escaped quote'; Text = '{ "a": "say \"x\"" // note }' }
    ) {
        Test-JsonSlashComment -Text $Text | Should -BeTrue
    }

    It 'finds none in <Description>' -ForEach @(
        @{ Description = 'a double-quoted URL'; Text = '{ "url": "https://example.com" }' }
        @{ Description = 'a single-quoted URL'; Text = "{ 'url': 'https://example.com/*x' }" }
        @{ Description = 'a lone slash'; Text = '{ "path": "a/b" }' }
        @{ Description = 'text with no slash pair'; Text = '{ "id": "a" }' }
    ) {
        Test-JsonSlashComment -Text $Text | Should -BeFalse
    }
}
