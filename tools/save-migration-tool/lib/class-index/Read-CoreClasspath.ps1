# Reads the jars the vanilla launcher puts on the classpath, as full paths.
# The launcher passes vmparams to java from starsector-core, so the jars it names are relative to that folder.
# Returns no paths when vmparams is missing or names no classpath.
function Read-CoreClasspath {

    param([string] $StarsectorRoot)

    $vmparamsFileName = 'vmparams'
    $coreFolderName = 'starsector-core'
    $classpathPattern = '(?:^|\s)-classpath\s+(?<classpath>\S+)'

    # The Windows launcher's separator. The scripts read a Windows install.
    $classpathSeparator = ';'

    $vmparamsPath = Join-Path $StarsectorRoot $vmparamsFileName

    if (-not (Test-Path -LiteralPath $vmparamsPath -PathType Leaf)) {
        return
    }

    $match = [regex]::Match([System.IO.File]::ReadAllText($vmparamsPath), $classpathPattern)

    if (-not $match.Success) {
        return
    }

    $coreFolder = Join-Path $StarsectorRoot $coreFolderName

    foreach ($jar in $match.Groups['classpath'].Value.Split($classpathSeparator)) {

        if ($jar) {
            [System.IO.Path]::GetFullPath($jar, $coreFolder)
        }
    }
}
