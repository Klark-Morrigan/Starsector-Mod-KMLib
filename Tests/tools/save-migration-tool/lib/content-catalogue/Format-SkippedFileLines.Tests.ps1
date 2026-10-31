BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$libRoot\console\Format-ConsoleList.ps1"
    . "$libRoot\console\Format-ConsoleNumber.ps1"
    . "$libRoot\console\Format-ConsoleText.ps1"
    . "$libRoot\content-catalogue\Format-SkippedFileLines.ps1"

    function script:New-SkippedFile {
        # One skipped copy, as New-ContentCatalogue returns it.
        param(
            [string] $Path,
            [string] $Reason
        )

        return [pscustomobject]@{ Path = $Path; Reason = $Reason }
    }
}

Describe 'Format-SkippedFileLines' {

    BeforeEach {

        # Each value names its role, so a case reads back how it was painted.
        Mock Format-ConsoleText { "<$Role>$Text" }
        Mock Format-ConsoleNumber { "<Number>$Number" }
        Mock Format-ConsoleList { "<$Role list>$($Values -join '+')" }
    }

    # One copy's reason carries what is particular to it, such as the parser's message, which a group would lose.
    It 'writes a reason only one copy gives whole, with its path' {

        # ARRANGE
        $skippedFiles = @(New-SkippedFile -Path 'alpha\a.ship' -Reason 'not valid JSON: alpha\a.ship (line 3)')

        $expectedLines = @('skipped <Path>alpha\a.ship: not valid JSON: alpha\a.ship (line 3)')

        # ACT
        $actualLines = Format-SkippedFileLines -SkippedFiles $skippedFiles

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    # A broken run can skip every copy of a kind for one reason, and a line per copy buries the rest of the log.
    It 'groups copies by the reason before its first ": ", in the order found, counting each group''s copies' {

        # ARRANGE
        $skippedFiles = @(
            New-SkippedFile -Path 'a.json' -Reason 'not valid JSON: a.json (line 1)'
            New-SkippedFile -Path 'b.csv' -Reason 'has no id column in every row'
            New-SkippedFile -Path 'c.json' -Reason 'not valid JSON: c.json (line 9)'
            New-SkippedFile -Path 'd.csv' -Reason 'has no id column in every row'
        )

        $expectedLines = @(
            'skipped <Number>2 files (not valid JSON): <Path list>a.json+c.json'
            'skipped <Number>2 files (has no id column in every row): <Path list>b.csv+d.csv'
        )

        # ACT
        $actualLines = Format-SkippedFileLines -SkippedFiles $skippedFiles

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'names a group''s first three paths, and counts the rest' {

        # ARRANGE
        $skippedFiles = @(
            foreach ($name in 'a', 'b', 'c', 'd', 'e') {
                New-SkippedFile -Path "$name.json" -Reason "not valid JSON: $name.json"
            }
        )

        $expectedLines = @('skipped <Number>5 files (not valid JSON): <Path list>a.json+b.json+c.json and <Number>2 more')

        # ACT
        $actualLines = Format-SkippedFileLines -SkippedFiles $skippedFiles

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    # A reason names a column or key as the file writes it, and the game matches those case-sensitively.
    It 'keeps reasons apart that differ only in case' {

        # ARRANGE
        $skippedFiles = @(
            New-SkippedFile -Path 'a.csv' -Reason 'has no id column in every row'
            New-SkippedFile -Path 'b.csv' -Reason 'has no ID column in every row'
        )

        $expectedLines = @(
            'skipped <Path>a.csv: has no id column in every row'
            'skipped <Path>b.csv: has no ID column in every row'
        )

        # ACT
        $actualLines = Format-SkippedFileLines -SkippedFiles $skippedFiles

        # ASSERT
        $actualLines | Should -Be $expectedLines
    }

    It 'writes no line when nothing was skipped' {

        # ARRANGE
        $expectedLineCount = 0

        # ACT
        $actualLines = @(Format-SkippedFileLines -SkippedFiles @())

        # ASSERT
        $actualLines.Count | Should -Be $expectedLineCount
    }
}
