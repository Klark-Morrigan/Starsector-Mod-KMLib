BeforeAll {
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\ConvertFrom-LogLine.ps1"
}

Describe 'ConvertFrom-LogLine' {

    BeforeAll {
        $script:Logger = 'com.fs.starfarer.campaign.save.CampaignGameManager'
    }

    It 'reads the level and message of a line from the logger' {

        $line = "1200 [Thread-3] ERROR $script:Logger  - Failed calling method"

        $logLine = ConvertFrom-LogLine -Line $line -LoggerName $script:Logger

        $logLine.Level | Should -Be 'ERROR'
        $logLine.Message | Should -Be 'Failed calling method'
    }

    It 'returns null for <Description>' -ForEach @(
        @{ Description = 'a stack frame naming the logger class'
            Line = '	at com.fs.starfarer.campaign.save.CampaignGameManager.o00000(Unknown Source)' }
        @{ Description = 'a line from another logger'
            Line = '1200 [Thread-3] INFO  com.fs.starfarer.Other  - Loading' }
        @{ Description = 'a line with no timestamp'
            Line = '[Thread-3] INFO  com.fs.starfarer.campaign.save.CampaignGameManager  - x' }
    ) {
        ConvertFrom-LogLine -Line $Line -LoggerName $script:Logger | Should -BeNullOrEmpty
    }
}
