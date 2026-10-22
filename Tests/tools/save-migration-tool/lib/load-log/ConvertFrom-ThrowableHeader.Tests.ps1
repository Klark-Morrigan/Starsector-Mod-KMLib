BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\ConvertFrom-ThrowableHeader.ps1"
}

Describe 'ConvertFrom-ThrowableHeader' {

    It 'reads <Line> as <Type>' -ForEach @(
        @{ Line = 'Caused by: java.lang.RuntimeException: Weapon spec [x] not found!'
            Type = 'java.lang.RuntimeException'; Message = 'Weapon spec [x] not found!' }
        @{ Line = 'com.thoughtworks.xstream.converters.ConversionException: Failed calling method'
            Type = 'com.thoughtworks.xstream.converters.ConversionException'; Message = 'Failed calling method' }
        @{ Line = 'java.lang.NullPointerException'; Type = 'java.lang.NullPointerException'; Message = '' }
        @{ Line = 'java.lang.OutOfMemoryError: Java heap space'
            Type = 'java.lang.OutOfMemoryError'; Message = 'Java heap space' }
    ) {

        $header = ConvertFrom-ThrowableHeader -Line $Line

        $header.Type | Should -Be $Type
        $header.Message | Should -Be $Message
    }

    It 'returns null for <Description>' -ForEach @(
        @{ Description = 'a stack frame'; Line = '	at com.fs.starfarer.loading.Q.String(Unknown Source)' }
        @{ Description = 'a suppressed block'; Line = '	Suppressed: java.io.IOException: closed' }
        @{ Description = 'a debugging line'; Line = 'cause-exception     : java.lang.RuntimeException' }
        @{ Description = 'a class with no throwable suffix'; Line = 'com.fs.starfarer.Something: text' }
    ) {
        ConvertFrom-ThrowableHeader -Line $Line | Should -BeNullOrEmpty
    }
}
