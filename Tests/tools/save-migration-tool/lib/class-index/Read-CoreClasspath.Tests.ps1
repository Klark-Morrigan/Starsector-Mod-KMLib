BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\class-index\Read-CoreClasspath.ps1"

    function script:New-Install {
        # Writes an install root under TestDrive, holding vmparams with the given text unless none is given.
        param(
            [string] $Name,
            [string] $Vmparams
        )

        $root = Join-Path $TestDrive $Name

        New-Item -ItemType Directory -Path $root -Force | Out-Null

        if ($Vmparams) {
            New-TestFile -Name "$Name\vmparams" -Text $Vmparams | Out-Null
        }

        return $root
    }
}

Describe 'Read-CoreClasspath' {

    It 'reads each classpath entry as a full path under starsector-core, with no problem' {

        $root = New-Install -Name 'vanilla' -Vmparams (
            'java.exe -Xms1536m -classpath janino.jar;starfarer.api.jar;starfarer_obf.jar -Dsetting=x')

        $classpath = Read-CoreClasspath -StarsectorRoot $root

        $classpath.Jars | Should -Be @(
            "$root\starsector-core\janino.jar"
            "$root\starsector-core\starfarer.api.jar"
            "$root\starsector-core\starfarer_obf.jar"
        )

        $classpath.Problem | Should -BeNullOrEmpty
    }

    It 'skips the empty entry a trailing separator leaves' {

        $root = New-Install -Name 'trailing' -Vmparams '-classpath json.jar;'

        (Read-CoreClasspath -StarsectorRoot $root).Jars | Should -Be @("$root\starsector-core\json.jar")
    }

    It 'reads no JARs, and says why, when <Description>' -ForEach @(
        @{ Description = 'vmparams is missing'; Name = 'none'; Vmparams = $null; Problem = 'not found' }
        @{ Description = 'vmparams names no classpath'; Name = 'noClasspath'; Vmparams = 'java.exe -Xms1536m'
            Problem = 'names no classpath' }
    ) {
        $root = New-Install -Name $Name -Vmparams $Vmparams

        $classpath = Read-CoreClasspath -StarsectorRoot $root

        $classpath.Jars.Count | Should -Be 0
        $classpath.Problem | Should -Be "$root\vmparams $Problem"
    }
}
