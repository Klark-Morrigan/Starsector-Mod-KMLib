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
        (Select-SaveLoad -Loads $script:Loads).Name | Should -Be @('newest', 'middle', 'oldest')
    }

    It 'keeps one save''s loads' {
        (Select-SaveLoad -Loads $script:Loads -SaveFolder 'save_A').Name | Should -Be @('newest', 'oldest')
    }

    It 'keeps one outcome''s loads' {
        (Select-SaveLoad -Loads $script:Loads -Outcome 'Failed').Name | Should -Be @('middle', 'oldest')
    }

    # Outcome names are case-sensitive, as the script checks them before any log is read.
    It 'keeps no load for an outcome in another case' {
        (Select-SaveLoad -Loads $script:Loads -Outcome 'failed').Count | Should -Be 0
    }

    It 'keeps the newest load the other filters leave' {

        (Select-SaveLoad -Loads $script:Loads -SaveFolder 'save_A' -Outcome 'Failed' -IsNewestOnly).Name |
            Should -Be @('oldest')
    }

    It 'returns an empty list when nothing matches' {

        $selected = Select-SaveLoad -Loads $script:Loads -SaveFolder 'save_C' -IsNewestOnly

        $null -eq $selected | Should -BeFalse
        $selected.Count | Should -Be 0
    }
}
