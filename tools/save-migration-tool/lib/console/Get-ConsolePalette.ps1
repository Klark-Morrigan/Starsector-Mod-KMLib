# The colour each kind of value takes on a console, as ANSI SGR sequences, so every script paints alike.
# The words around the values keep the console's own text colour.
function Get-ConsolePalette {

    return @{
        Reset   = "`e[0m"

        Script  = "`e[90m"
        Time    = "`e[90m"

        Error   = "`e[31m"
        Success = "`e[32m"

        # The amber the workspace menu uses for "needs a look", so a warning does not read as an error.
        Warning = "`e[38;2;255;190;70m"

        # Three yellows, from saturated to pale: the mods, the content they ship, the files on disk.
        # Content sits halfway between the other two, so the steps read as even.
        # Exact colours, since a console scheme decides what the 16 named ones look like:
        # common schemes draw plain yellow as a dull ochre, and bright white barely apart from the text.
        Mod     = "`e[38;2;255;230;0m"
        Content = "`e[38;2;249;232;81m"
        Path    = "`e[38;2;242;234;162m"

        Code    = "`e[36m"

        # Faint, so numbers stand out from the words without competing with the values they count.
        # Not named Count: a hashtable key of that name hides the hashtable's own Count.
        Number  = "`e[38;2;128;215;128m"
    }
}
