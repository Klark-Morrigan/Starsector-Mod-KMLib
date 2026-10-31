BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Get-LogFileChain.ps1"

    function script:New-LogFolder {
        # Writes empty files with the given names into a folder under TestDrive and returns the folder.
        param(
            [string] $Name,
            [string[]] $FileNames
        )

        foreach ($fileName in $FileNames) {
            New-TestFile -Name "$Name\$fileName" | Out-Null
        }

        return Join-Path $TestDrive $Name
    }
}

Describe 'Get-LogFileChain' {

    It 'returns the rotated logs highest number first, then the live log, comparing numbers numerically' {

        # ARRANGE
        $folder = New-LogFolder -Name 'rotated' `
            -FileNames @('starsector.log', 'starsector.log.1', 'starsector.log.2', 'starsector.log.10')

        $expectedNames = @('starsector.log.10', 'starsector.log.2', 'starsector.log.1', 'starsector.log')

        # ACT
        $actualChain = Get-LogFileChain -Path (Join-Path $folder 'starsector.log')

        # ASSERT
        $actualChain | ForEach-Object { Split-Path -Leaf $_ } | Should -Be $expectedNames
    }

    It 'skips files that are not a numbered rotation of the log' {

        # ARRANGE
        $folder = New-LogFolder -Name 'unrelated' -FileNames @('starsector.log', 'starsector.log.bak', 'other.log.1')

        $expectedNames = @('starsector.log')

        # ACT
        $actualChain = Get-LogFileChain -Path (Join-Path $folder 'starsector.log')

        # ASSERT
        $actualChain | ForEach-Object { Split-Path -Leaf $_ } | Should -Be $expectedNames
    }

    It 'returns a lone log as a one-element array' {

        # ARRANGE
        $folder = New-LogFolder -Name 'alone' -FileNames @('starsector.log')

        $expectedCount = 1

        # ACT
        $actualChain = Get-LogFileChain -Path (Join-Path $folder 'starsector.log')

        # ASSERT
        ($actualChain -is [array]) | Should -BeTrue
        $actualChain.Count | Should -Be $expectedCount
    }
}
