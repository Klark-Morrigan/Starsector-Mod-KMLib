# Writes text lines meant for a person to stdout as the stream takes them: painted on a console, plain elsewhere.
function Resolve-OutputLines {

    param([string[]] $Lines)

    foreach ($line in $Lines) {
        Resolve-ConsoleText -Text $line -Stream 'Output'
    }
}
