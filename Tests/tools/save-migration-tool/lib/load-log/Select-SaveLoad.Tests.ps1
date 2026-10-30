BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Select-SaveLoad.ps1"
}

Describe 'Select-SaveLoad' {

    BeforeEach {

        # Newest first, as Get-SaveLoadAttempt hands them over.
        $script:Loads = @(
            [pscustomobject]@{ Name = 'newest'; SaveFolder = 'save_A'; Outcome = 'Loaded' }
            [pscustomobject]@{ Name = 'middle'; SaveFolder = 'save_B'; Outcome = 'Failed' }
            [pscustomobject]@{ Name = 'oldest'; SaveFolder = 'save_A'; Outcome = 'Failed' }
        )
    }

    It 'keeps every load, in order, with no filter' {

        # ARRANGE
        $expectedNames = @('newest', 'middle', 'oldest')

        # ACT
        $actualLoads = Select-SaveLoad -Loads $script:Loads

        # ASSERT
        $actualLoads.Name | Should -Be $expectedNames
    }

    It 'keeps one save''s loads' {

        # ARRANGE
        $expectedNames = @('newest', 'oldest')

        # ACT
        $actualLoads = Select-SaveLoad -Loads $script:Loads -SaveFolder 'save_A'

        # ASSERT
        $actualLoads.Name | Should -Be $expectedNames
    }

    It 'keeps one outcome''s loads' {

        # ARRANGE
        $expectedNames = @('middle', 'oldest')

        # ACT
        $actualLoads = Select-SaveLoad -Loads $script:Loads -Outcome 'Failed'

        # ASSERT
        $actualLoads.Name | Should -Be $expectedNames
    }

    # Outcome names are case-sensitive, as the script checks them before any log is read.
    It 'keeps no load for an outcome in another case' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualLoads = Select-SaveLoad -Loads $script:Loads -Outcome 'failed'

        # ASSERT
        $actualLoads.Count | Should -Be $expectedCount
    }

    It 'keeps the newest load the other filters leave' {

        # ARRANGE
        $expectedNames = @('oldest')

        # ACT
        $actualLoads = Select-SaveLoad -Loads $script:Loads -SaveFolder 'save_A' -Outcome 'Failed' -IsNewestOnly

        # ASSERT
        $actualLoads.Name | Should -Be $expectedNames
    }

    It 'returns an empty list when nothing matches' {

        # ARRANGE
        $expectedCount = 0

        # ACT
        $actualLoads = Select-SaveLoad -Loads $script:Loads -SaveFolder 'save_C' -IsNewestOnly

        # ASSERT
        $null -eq $actualLoads | Should -BeFalse
        $actualLoads.Count | Should -Be $expectedCount
    }
}
