# How a mod differs between a save and the install.
# Named once, so the code that sets a change and the code that reads it agree.
function Get-ModDriftChangeNames {

    return [pscustomobject]@{
        Unchanged = 'Unchanged'
        Changed   = 'Changed'

        # The save was last written with the mod, and the install does not load it.
        Missing   = 'Missing'

        # The install loads the mod, and the save was not last written with it.
        Added     = 'Added'
    }
}
