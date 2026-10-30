BeforeAll {

    Set-StrictMode -Version Latest

    . "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib\install\Get-InstallPathNames.ps1"
}

Describe 'Get-InstallPathNames' {

    It 'names the install''s folders and files as the game lays them out' {

        # ARRANGE
        $expectedCoreFolder = 'starsector-core'
        $expectedLogFile = 'starsector.log'
        $expectedVmparamsFile = 'vmparams'
        $expectedModsFolder = 'mods'
        $expectedEnabledModsFile = 'enabled_mods.json'
        $expectedModInfoFile = 'mod_info.json'
        $expectedSavesFolder = 'saves'
        $expectedDescriptorFile = 'descriptor.xml'

        # ACT
        $actualNames = Get-InstallPathNames

        # ASSERT
        $actualNames.CoreFolder | Should -Be $expectedCoreFolder
        $actualNames.LogFile | Should -Be $expectedLogFile
        $actualNames.VmparamsFile | Should -Be $expectedVmparamsFile
        $actualNames.ModsFolder | Should -Be $expectedModsFolder
        $actualNames.EnabledModsFile | Should -Be $expectedEnabledModsFile
        $actualNames.ModInfoFile | Should -Be $expectedModInfoFile
        $actualNames.SavesFolder | Should -Be $expectedSavesFolder
        $actualNames.DescriptorFile | Should -Be $expectedDescriptorFile
    }
}
