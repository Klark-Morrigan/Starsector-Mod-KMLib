# The folders and files of a Starsector install the scripts read, relative to the install root or to their folder.
# Named once, so every script finds the same file under the same name.
function Get-InstallPathNames {

    return [pscustomobject]@{
        CoreFolder      = 'starsector-core'
        LogFile         = 'starsector.log'
        VmparamsFile    = 'vmparams'
        ModsFolder      = 'mods'
        EnabledModsFile = 'enabled_mods.json'
        ModInfoFile     = 'mod_info.json'
        SavesFolder     = 'saves'
        DescriptorFile  = 'descriptor.xml'
    }
}
