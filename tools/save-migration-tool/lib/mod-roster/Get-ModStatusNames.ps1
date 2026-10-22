# How a roster entry's mod_info.json read: parsed, or kept though no mod ID could be read from it.
# Named once, so the code that sets a status and the code that reads it agree.
function Get-ModStatusNames {

    return [pscustomobject]@{
        Parsed   = 'parsed'
        Unparsed = 'unparsed'
    }
}
