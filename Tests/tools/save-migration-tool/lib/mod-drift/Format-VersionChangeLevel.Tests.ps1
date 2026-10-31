BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\console\Format-ConsoleText.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-VersionChangeLevelNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Format-VersionChangeLevel.ps1"
}

Describe 'Format-VersionChangeLevel' {

    BeforeEach {

        Mock Get-VersionChangeLevelNames { [pscustomobject]@{ Major = 'MAJ'; Minor = 'MIN'; Patch = 'PAT' } }
        Mock Format-ConsoleText { "<$Role>$Text" }
    }

    # Red and amber follow the game's load list; a patch stays plain so the changes that need a look stand out.
    It 'says a <Level> level as <Phrase>' -ForEach @(
        @{ Level = 'MAJ'; Phrase = '<Error>major change' }
        @{ Level = 'MIN'; Phrase = '<Warning>minor change' }
        @{ Level = 'PAT'; Phrase = 'patch change' }
    ) {

        # ARRANGE
        $expectedPhrase = $Phrase

        # ACT
        $actualPhrase = Format-VersionChangeLevel -Level $Level

        # ASSERT
        $actualPhrase | Should -Be $expectedPhrase
    }
}
