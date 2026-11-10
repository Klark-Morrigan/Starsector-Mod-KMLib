# The regex for a string as org.json reads one: in double or single quotes, a backslash escaping the character after it.
# A scan for structure matches strings first and keeps them whole, so a quote, bracket or slash inside one is text.
function Get-JsonStringPattern {

    return '"(?:[^"\\]|\\.)*"|''(?:[^''\\]|\\.)*'''
}
