# The names of one kind's cache entry files, under the install's cache folder.
# A kind's owners and its key go in two files, the key written last,
# so a run stopped part-way leaves no key beside owners it never finished writing.
function Get-CatalogueCacheFileNames {

    param(
        [string] $Kind
    )

    return [pscustomobject]@{
        Folder = 'catalogue'
        Key = "$Kind.key.json"
        Owners = "$Kind.owners.json"
    }
}
