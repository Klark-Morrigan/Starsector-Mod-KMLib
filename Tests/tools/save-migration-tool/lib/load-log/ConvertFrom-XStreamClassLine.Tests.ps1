BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\ConvertFrom-XStreamClassLine.ps1"
}

Describe 'ConvertFrom-XStreamClassLine' {

    It 'reads the failed object''s class at depth 0' {

        # ARRANGE
        $line = 'class               : com.fs.starfarer.loading.specs.Spec'

        $expectedDepth = 0
        $expectedClass = 'com.fs.starfarer.loading.specs.Spec'

        # ACT
        $actualLink = ConvertFrom-XStreamClassLine -Line $line

        # ASSERT
        $actualLink.Depth | Should -Be $expectedDepth
        $actualLink.Class | Should -Be $expectedClass
    }

    It 'reads a container''s class at its depth, nested class names included' {

        # ARRANGE
        $line = 'class[18]           : com.fs.RouteManager$RouteData'

        $expectedDepth = 18
        $expectedClass = 'com.fs.RouteManager$RouteData'

        # ACT
        $actualLink = ConvertFrom-XStreamClassLine -Line $line

        # ASSERT
        $actualLink.Depth | Should -Be $expectedDepth
        $actualLink.Class | Should -Be $expectedClass
    }

    It 'returns null for <Description>' -ForEach @(
        @{ Description = 'another field'; Line = 'required-type       : com.fs.starfarer.loading.specs.Spec' }
        @{ Description = 'a converter'; Line = 'converter-type[1]   : com.thoughtworks.xstream.MapConverter' }
        @{ Description = 'a stack frame'; Line = "`tat com.fs.starfarer.campaign.save.Manager.load(Unknown Source)" }
        @{ Description = 'the block''s end'; Line = '-------------------------------' }
    ) {

        # ACT
        $actualLink = ConvertFrom-XStreamClassLine -Line $Line

        # ASSERT
        $actualLink | Should -BeNullOrEmpty
    }
}
