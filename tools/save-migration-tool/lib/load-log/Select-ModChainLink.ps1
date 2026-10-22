# Keeps the links of an object chain whose class a mod owns, innermost first as given.
# A class only the game owns, or one no JAR holds, says nothing about which mod's object the failure sat in.
function Select-ModChainLink {

    param([object[]] $ObjectChain)

    $coreOwner = Get-CoreOwnerName

    return ,@($ObjectChain | Where-Object { @($_.Owners).Count -gt 0 -and $_.Owners -notcontains $coreOwner })
}
