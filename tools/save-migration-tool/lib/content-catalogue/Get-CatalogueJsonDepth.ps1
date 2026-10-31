# How deep the catalogue nests, so its JSON keeps every ID: an owner entry, its IDs by kind, each kind's list.
# A row nests less: the row and its owners.
function Get-CatalogueJsonDepth {

    return 3
}
