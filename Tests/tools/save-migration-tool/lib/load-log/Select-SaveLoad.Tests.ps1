BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Assert-LoadOutcomeName.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\load-log\Select-SaveLoad.ps1"
}

Describe 'Select-SaveLoad' {

    BeforeEach {

        Mock Assert-LoadOutcomeName { }

        # Newest first, as Get-SaveLoadAttempt hands them over.
        $script:Loads = @(
            [pscustomobject]@{ Name = 'newest'; SaveFolder = 'save_A'; Outcome = 'Loaded' }
            [pscustomobject]@{ Name = 'middle'; SaveFolder = 'save_B'; Outcome = 'Failed' }
            [pscustomobject]@{ Name = 'oldest'; SaveFolder = 'save_A'; Outcome = 'Failed' }
        )
    }

    It 'keeps every load, in order, with no filter, checking no outcome' {

        (Select-SaveLoad -Loads $script:Loads).Name | Should -Be @('newest', 'middle', 'oldest')

        Should -Invoke Assert-LoadOutcomeName -Times 0
    }

    It 'keeps one save''s loads' {
        (Select-SaveLoad -Loads $script:Loads -SaveFolder 'save_A').Name | Should -Be @('newest', 'oldest')
    }

    It 'keeps one outcome''s loads, once the outcome is checked' {

        (Select-SaveLoad -Loads $script:Loads -Outcome 'Failed').Name | Should -Be @('middle', 'oldest')

        Should -Invoke Assert-LoadOutcomeName -Times 1 -Exactly -ParameterFilter { $Outcome -eq 'Failed' }
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
