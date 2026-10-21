BeforeAll {

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index\Read-CoreClasspath.ps1"

    function script:New-Install {
        # Writes an install root under TestDrive, holding vmparams with the given text unless none is given.
        param(
            [string] $Name,
            [string] $Vmparams
        )

        $root = Join-Path $TestDrive $Name

        New-Item -ItemType Directory -Path $root | Out-Null

        if ($Vmparams) {
            [System.IO.File]::WriteAllText((Join-Path $root 'vmparams'), $Vmparams)
        }

        return $root
    }
}

Describe 'Read-CoreClasspath' {

    It 'reads each classpath entry as a full path under starsector-core' {

        $root = New-Install -Name 'vanilla' -Vmparams (
            'java.exe -Xms1536m -classpath janino.jar;starfarer.api.jar;starfarer_obf.jar -Dsetting=x')

        Read-CoreClasspath -StarsectorRoot $root | Should -Be @(
            "$root\starsector-core\janino.jar"
            "$root\starsector-core\starfarer.api.jar"
            "$root\starsector-core\starfarer_obf.jar"
        )
    }

    It 'skips the empty entry a trailing separator leaves' {

        $root = New-Install -Name 'trailing' -Vmparams '-classpath json.jar;'

        Read-CoreClasspath -StarsectorRoot $root | Should -Be @("$root\starsector-core\json.jar")
    }

    It 'returns no paths when <Description>' -ForEach @(
        @{ Description = 'vmparams is missing'; Name = 'none'; Vmparams = $null }
        @{ Description = 'vmparams names no classpath'; Name = 'noClasspath'; Vmparams = 'java.exe -Xms1536m' }
    ) {
        @(Read-CoreClasspath -StarsectorRoot (New-Install -Name $Name -Vmparams $Vmparams)).Count | Should -Be 0
    }
}
