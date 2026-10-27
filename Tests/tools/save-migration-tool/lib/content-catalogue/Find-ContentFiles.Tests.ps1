BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\content-catalogue\Find-ContentFiles.ps1"

    function script:New-Source {
        # One data folder under the case's own folder, as Get-ContentSources lists it.
        param([string] $Owner)

        return [pscustomobject]@{ Owner = $Owner; Folder = Join-Path $TestDrive "$script:Case\$Owner" }
    }

    function script:New-CaseFile {
        # Writes an empty file under the case's own folder, since TestDrive keeps every case's files.
        param([string] $Name)

        return New-TestFile -Name "$script:Case\$Name"
    }

    function script:Format-FoundFiles {
        # Writes each found file as "<relative path>: <owner of each copy>", one per file, in the order found.
        param([object[]] $Files)

        return @($Files | ForEach-Object { "$($_.RelativePath): $($_.Copies.Source.Owner -join ', ')" })
    }
}

Describe 'Find-ContentFiles' {

    BeforeEach {

        $script:Case = [guid]::NewGuid().ToString('N')
        $script:FileSet = [pscustomobject]@{ Folder = 'data/variants'; Ending = 'variant'; HasSubfolders = $true }
        $script:Sources = @((New-Source -Owner 'alpha'), (New-Source -Owner 'beta'), (New-Source -Owner 'core'))
    }

    It 'lists each path once, with the copy each source holds, in search order' {

        $betaCopy = New-CaseFile -Name 'beta\data\variants\shared.variant'
        New-CaseFile -Name 'core\data\variants\shared.variant' | Out-Null
        New-CaseFile -Name 'core\data\variants\own.variant' | Out-Null

        $files = Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet

        Format-FoundFiles -Files $files | Should -Be @(
            'data/variants/shared.variant: beta, core'
            'data/variants/own.variant: core'
        )
        $files[0].Copies[0].Path | Should -Be $betaCopy
    }

    It 'lists the folder''s own files before each subfolder, and subfolders in the order first found' {

        New-CaseFile -Name 'core\data\variants\top.variant' | Out-Null
        New-CaseFile -Name 'beta\data\variants\fighters\wing.variant' | Out-Null
        New-CaseFile -Name 'core\data\variants\drones\drone.variant' | Out-Null
        New-CaseFile -Name 'core\data\variants\fighters\wing.variant' | Out-Null

        Format-FoundFiles -Files (Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet) | Should -Be @(
            'data/variants/top.variant: core'
            'data/variants/fighters/wing.variant: beta, core'
            'data/variants/drones/drone.variant: core'
        )
    }

    It 'reads no subfolder for a set without them' {

        New-CaseFile -Name 'alpha\data\variants\sub\kept.variant' | Out-Null

        $script:FileSet.HasSubfolders = $false

        @(Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet).Count | Should -Be 0
    }

    It 'reads one subfolder level down, and no further' {

        New-CaseFile -Name 'alpha\data\variants\sub\deeper\deep.variant' | Out-Null
        New-CaseFile -Name 'alpha\data\variants\sub\kept.variant' | Out-Null

        Format-FoundFiles -Files (Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet) |
            Should -Be @('data/variants/sub/kept.variant: alpha')
    }

    It 'skips a subfolder whose name opens with a dot, and a hidden one' {

        New-CaseFile -Name 'alpha\data\variants\.backup\old.variant' | Out-Null

        $hiddenFile = New-CaseFile -Name 'alpha\data\variants\hidden\old.variant'
        $hiddenFolder = Get-Item -LiteralPath (Split-Path -Parent $hiddenFile)

        $hiddenFolder.Attributes = $hiddenFolder.Attributes -bor [System.IO.FileAttributes]::Hidden

        @(Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet).Count | Should -Be 0
    }

    # The game matches by how the name ends, with no dot: a backup named d.variant.bak is not read.
    It 'lists a file whose name ends with the set''s ending, case-sensitively and with or without a dot' {

        New-CaseFile -Name 'alpha\data\variants\a.variant' | Out-Null
        New-CaseFile -Name 'alpha\data\variants\bvariant' | Out-Null
        New-CaseFile -Name 'alpha\data\variants\c.VARIANT' | Out-Null
        New-CaseFile -Name 'alpha\data\variants\d.variant.bak' | Out-Null

        (Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet).RelativePath | Sort-Object | Should -Be @(
            'data/variants/a.variant'
            'data/variants/bvariant'
        )
    }

    It 'lists nothing when no source holds the folder' {
        @(Find-ContentFiles -Sources $script:Sources -FileSet $script:FileSet).Count | Should -Be 0
    }
}
