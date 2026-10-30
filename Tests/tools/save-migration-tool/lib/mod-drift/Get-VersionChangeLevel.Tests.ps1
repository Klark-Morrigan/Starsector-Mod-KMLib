BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-VersionChangeLevelNames.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\mod-drift\Get-VersionChangeLevel.ps1"

    function script:New-Parts {
        # The parts of one version, as New-VersionInfo builds them.
        param(
            [string] $Major,
            [string] $Minor,
            [string] $Patch,
            [bool] $MinorIsRC
        )

        return [pscustomobject]@{ Major = $Major; Minor = $Minor; Patch = $Patch; MinorIsRC = $MinorIsRC }
    }
}

Describe 'Get-VersionChangeLevel' {

    BeforeEach {

        Mock Get-VersionChangeLevelNames {
            [pscustomobject]@{ Major = 'MAJ'; Minor = 'MIN'; Patch = 'PAT' }
        }
    }

    # Major and minor follow what the game's VersionInfo.getCompatLevel returns for the same parts.
    # Parts the game reads as equal, or cannot rank, still belong to versions whose text differs,
    # so they rank as patch.
    # Parts are written major|minor|patch.
    It 'ranks <Installed> against a saved <Saved>, <Case>, as <Expected>' -ForEach @(
        @{ Case = 'a different major'; Installed = '2|0|0'; Saved = '1|9|9'; Expected = 'MAJ' }
        @{ Case = 'a different minor'; Installed = '1|3|7'; Saved = '1|2|1'; Expected = 'MIN' }
        @{ Case = 'a different patch'; Installed = '0.1|98|8'; Saved = '0.1|98|7'; Expected = 'PAT' }
        @{ Case = 'equal parts'; Installed = '1|0|2'; Saved = '1|0|2'; Expected = 'PAT' }
        @{ Case = 'equal only as numbers'; Installed = '1|02|0'; Saved = '1|2|0'; Expected = 'MIN' }
        @{ Case = 'equal only ignoring case'; Installed = '1|0|b'; Saved = '1|0|B'; Expected = 'PAT' }
        @{ Case = 'no installed major'; Installed = '|1|1'; Saved = '1|1|1'; Expected = 'PAT' }
        @{ Case = 'no saved major'; Installed = '1|1|1'; Saved = '|1|1'; Expected = 'PAT' }
    ) {

        # ARRANGE
        $installed = $Installed -split '\|'
        $saved = $Saved -split '\|'
        $installedParts = New-Parts -Major $installed[0] -Minor $installed[1] -Patch $installed[2]
        $savedParts = New-Parts -Major $saved[0] -Minor $saved[1] -Patch $saved[2]

        $expectedLevel = $Expected

        # ACT
        $actualLevel = Get-VersionChangeLevel -Installed $installedParts -Saved $savedParts

        # ASSERT
        $actualLevel | Should -Be $expectedLevel
    }

    It 'skips the minor part when both sides mark it a release candidate' {

        # ARRANGE
        $installedParts = New-Parts -Major '0.98' -Minor '8' -Patch '' -MinorIsRC $true
        $savedParts = New-Parts -Major '0.98' -Minor '7' -Patch '' -MinorIsRC $true

        $expectedLevel = 'PAT'

        # ACT
        $actualLevel = Get-VersionChangeLevel -Installed $installedParts -Saved $savedParts

        # ASSERT
        $actualLevel | Should -Be $expectedLevel
    }

    It 'compares the minor part when only one side marks it a release candidate' {

        # ARRANGE
        $installedParts = New-Parts -Major '0.98' -Minor '8' -Patch '' -MinorIsRC $true
        $savedParts = New-Parts -Major '0.98' -Minor '7' -Patch ''

        $expectedLevel = 'MIN'

        # ACT
        $actualLevel = Get-VersionChangeLevel -Installed $installedParts -Saved $savedParts

        # ASSERT
        $actualLevel | Should -Be $expectedLevel
    }
}
