# How a roster entry's mod_info.json read: parsed, or kept though nothing could parse it.
# Reading a candidate sets them and the roster filter and summary read them, so they are named in one place.
function Get-ModStatusNames {

    return [pscustomobject]@{
        Parsed   = 'parsed'
        Unparsed = 'unparsed'
    }
}
