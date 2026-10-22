BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\ConvertFrom-XStreamClassLine.ps1"
}

Describe 'ConvertFrom-XStreamClassLine' {

    It 'reads the failed object''s class at depth 0' {

        $link = ConvertFrom-XStreamClassLine -Line 'class               : com.fs.starfarer.loading.specs.Spec'

        $link.Depth | Should -Be 0
        $link.Class | Should -Be 'com.fs.starfarer.loading.specs.Spec'
    }

    It 'reads a container''s class at its depth, nested class names included' {

        $link = ConvertFrom-XStreamClassLine -Line 'class[18]           : com.fs.RouteManager$RouteData'

        $link.Depth | Should -Be 18
        $link.Class | Should -Be 'com.fs.RouteManager$RouteData'
    }

    It 'returns null for <Description>' -ForEach @(
        @{ Description = 'another field'; Line = 'required-type       : com.fs.starfarer.loading.specs.Spec' }
        @{ Description = 'a converter'; Line = 'converter-type[1]   : com.thoughtworks.xstream.MapConverter' }
        @{ Description = 'a stack frame'; Line = "`tat com.fs.starfarer.campaign.save.Manager.load(Unknown Source)" }
        @{ Description = 'the block''s end'; Line = '-------------------------------' }
    ) {
        ConvertFrom-XStreamClassLine -Line $Line | Should -BeNullOrEmpty
    }
}
