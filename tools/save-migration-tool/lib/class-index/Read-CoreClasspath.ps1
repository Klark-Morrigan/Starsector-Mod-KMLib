# Reads the JARs the vanilla launcher puts on the classpath, as full paths.
# The launcher passes vmparams to java from starsector-core, so the JARs it names are relative to that folder.
# Problem says why no JAR was read: vmparams is missing, or names no classpath. It is null when JARs were read.
function Read-CoreClasspath {

    param([string] $StarsectorRoot)

    $vmparamsFileName = 'vmparams'
    $coreFolderName = 'starsector-core'
    $classpathPattern = '(?:^|\s)-classpath\s+(?<classpath>\S+)'

    # The Windows launcher's separator. The scripts read a Windows install.
    $classpathSeparator = ';'

    $vmparamsPath = Join-Path $StarsectorRoot $vmparamsFileName

    if (-not (Test-Path -LiteralPath $vmparamsPath -PathType Leaf)) {
        return [pscustomobject]@{ Jars = @(); Problem = "$vmparamsPath not found" }
    }

    $match = [regex]::Match([System.IO.File]::ReadAllText($vmparamsPath), $classpathPattern)

    if (-not $match.Success) {
        return [pscustomobject]@{ Jars = @(); Problem = "$vmparamsPath names no classpath" }
    }

    $coreFolder = Join-Path $StarsectorRoot $coreFolderName
    $jars = @(foreach ($jar in $match.Groups['classpath'].Value.Split($classpathSeparator)) {

        if ($jar) {
            [System.IO.Path]::GetFullPath($jar, $coreFolder)
        }
    })

    return [pscustomobject]@{ Jars = $jars; Problem = $null }
}
