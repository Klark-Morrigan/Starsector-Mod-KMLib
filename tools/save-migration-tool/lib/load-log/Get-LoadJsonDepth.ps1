# How deep a load nests, so its JSON keeps every class owner: a load, its attempts, an attempt's object chain,
# a chain link's owners. A report holding loads one level down adds one.
function Get-LoadJsonDepth {

    return 6
}
