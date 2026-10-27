BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-InstallPathNames.ps1"
}

Describe 'Get-InstallPathNames' {

    It 'names the install''s folders and files as the game lays them out' {

        $names = Get-InstallPathNames

        $names.CoreFolder | Should -Be 'starsector-core'
        $names.LogFile | Should -Be 'starsector.log'
        $names.VmparamsFile | Should -Be 'vmparams'
        $names.ModsFolder | Should -Be 'mods'
        $names.EnabledModsFile | Should -Be 'enabled_mods.json'
        $names.ModInfoFile | Should -Be 'mod_info.json'
        $names.SavesFolder | Should -Be 'saves'
        $names.DescriptorFile | Should -Be 'descriptor.xml'
    }
}
