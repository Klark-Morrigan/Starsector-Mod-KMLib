# The scan runs in the compiled scanner beside Read-SaveReferences, so these cases read small campaign files
# shaped as the game writes them, each naming its sites by hand.

BeforeAll {

    Set-StrictMode -Version Latest

    $libRoot = "$PSScriptRoot\..\..\..\..\..\tools\save-migration-tool\lib"

    . "$PSScriptRoot\..\..\helpers\New-TestFile.ps1"
    . "$libRoot\save-references\New-SaveReferenceSite.ps1"
    . "$libRoot\save-references\Read-SaveReferences.ps1"

    function script:Format-References {
        # Writes each reference as "<kind> <ID> <uses>: <site>=<uses> ...", sorted, so a case compares literals.
        param([object[]] $References)

        $lines = [string[]] @(foreach ($reference in $References) {

            $sites = foreach ($siteUses in $reference.
                    UseCountBySite.
                    GetEnumerator() |
                    Sort-Object Key) {

                "$($siteUses.Key)=$($siteUses.Value)"
            }

            "$($reference.Kind) $($reference.Id) $($reference.UseCount): $($sites -join ' ')"
        })

        [System.Array]::Sort(
            $lines,
            [System.StringComparer]::Ordinal)

        return $lines
    }

    function script:Read-TestSave {
        # Writes the campaign text to a file and scans it at the given sites.
        param(
            [string] $Text,
            [object[]] $Sites)

        $path = New-TestFile `
            -Name "campaign-$([guid]::NewGuid()).xml" `
            -Text $Text

        return Read-SaveReferences `
            -Path $path `
            -Sites $Sites
    }
}

Describe 'Read-SaveReferences' {

    It 'reads an attribute of an element matched by its name or its cl class, and counts each use' {

        # ARRANGE
        $text = @'
<CampaignEngine z="1">
<FMmbr z="2" sid="a_Hull"><savedVariant z="3" hId="a_hull"></savedVariant></FMmbr>
<FMmbr z="4" sid="a_Hull"><savedVariant z="5" hId="a_hull"/></FMmbr>
<v cl="savedVariant" z="6" hId="b_hull"></v>
<savedVariant ref="3"></savedVariant>
</CampaignEngine>
'@
        $sites = @(New-SaveReferenceSite `
            -Kind 'Hull' `
            -Path @('savedVariant') `
            -Attribute 'hId')

        $expectedReferences = @(
            'Hull a_hull 2: savedVariant@hId=2',
            'Hull b_hull 1: savedVariant@hId=1')

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        Format-References `
            -References $actualScan.References |
            Should -Be $expectedReferences
    }

    It 'reads the text of a list''s items, only under the list the path names' {

        # ARRANGE
        $text = @'
<Faction z="1">
<knownShips z="2">
<st>a_hull</st>
<st>b_hull</st>
<st></st>
</knownShips>
<other z="3"><st>not_a_hull</st></other>
</Faction>
'@
        $sites = @(New-SaveReferenceSite `
            -Kind 'Hull' `
            -Path @('knownShips', 'st'))

        $expectedReferences = @(
            'Hull a_hull 1: knownShips/st=1',
            'Hull b_hull 1: knownShips/st=1')

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        Format-References `
            -References $actualScan.References |
            Should -Be $expectedReferences
    }

    It 'reads a stack''s first child only when the stack carries the filter''s value' {

        # ARRANGE
        $text = @'
<s z="1">
<CIStack z="2" t="WEAPONS"><d cl="st">a_gun</d><c ref="9"></c></CIStack>
<CIStack z="3" t="RESOURCES"><d cl="st">supplies</d></CIStack>
<CIStack z="4" t="FIGHTER_CHIP"><d cl="st">a_wing</d></CIStack>
</s>
'@
        $sites = @(

            New-SaveReferenceSite `
                -Kind 'Weapon' `
                -Path @('CIStack', 'd') `
                -FilterAttribute 't' `
                -FilterValues @('WEAPONS')

            New-SaveReferenceSite `
                -Kind 'FighterWing' `
                -Path @('CIStack', 'd') `
                -FilterAttribute 't' `
                -FilterValues @('FIGHTER_CHIP')
        )

        $expectedReferences = @(
            'FighterWing a_wing 1: CIStack[t=FIGHTER_CHIP]/d=1'
            'Weapon a_gun 1: CIStack[t=WEAPONS]/d=1'
        )

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        Format-References `
            -References $actualScan.References |
            Should -Be $expectedReferences
    }

    It 'reads only the element at the position the site names among its same-named siblings' {

        # ARRANGE
        $text = @'
<wpn z="1">
<e><st>WS 001</st><st>a_gun</st></e>
<e><st>WS 002</st><st>b_gun</st></e>
</wpn>
'@
        $sites = @(New-SaveReferenceSite `
            -Kind 'Weapon' `
            -Path @('wpn', 'e', 'st') `
            -Position 2)

        $expectedReferences = @(
            'Weapon a_gun 1: wpn/e/st[2]=1',
            'Weapon b_gun 1: wpn/e/st[2]=1')

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        Format-References `
            -References $actualScan.References |
            Should -Be $expectedReferences
    }

    It 'reads an attribute on any element when the path is a wildcard, and under any child with one' {

        # ARRANGE
        $text = @'
<root z="1">
<c z="2" fid="hegemony" pid="steady"></c>
<person z="3" fid="pirates"></person>
<industries z="4"><Farming z="5" id="farming"></Farming><Spaceport z="6" id="spaceport"></Spaceport></industries>
</root>
'@
        $sites = @(

            New-SaveReferenceSite `
                -Kind 'Faction' `
                -Path @('*') `
                -Attribute 'fid'

            New-SaveReferenceSite `
                -Kind 'Industry' `
                -Path @('industries', '*') `
                -Attribute 'id'
        )

        $expectedReferences = @(
            'Faction hegemony 1: *@fid=1'
            'Faction pirates 1: *@fid=1'
            'Industry farming 1: industries/*@id=1'
            'Industry spaceport 1: industries/*@id=1'
        )

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        Format-References `
            -References $actualScan.References |
            Should -Be $expectedReferences
    }

    It 'reads the keys of a JSON object, or the value under one key' {

        # ARRANGE
        $text = @'
<root z="1">
<stats z="2" xp="0"><s>{"helmsmanship":2,"gunnery_implants":1}</s></stats>
<CCEnt z="3"><j1>{"f0":10,"f5":"comm_relay"}</j1></CCEnt>
<CCEnt z="4"><j1>{"f0":10}</j1></CCEnt>
</root>
'@
        $sites = @(

            New-SaveReferenceSite `
                -Kind 'Skill' `
                -Path @('stats', 's') `
                -AreJsonKeysIds

            New-SaveReferenceSite `
                -Kind 'CustomEntity' `
                -Path @('CCEnt', 'j1') `
                -JsonKey 'f5'
        )

        $expectedReferences = @(
            'CustomEntity comm_relay 1: CCEnt/j1{f5}=1'
            'Skill gunnery_implants 1: stats/s{keys}=1'
            'Skill helmsmanship 1: stats/s{keys}=1'
        )

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        Format-References `
            -References $actualScan.References |
            Should -Be $expectedReferences
    }

    # One bad value must not stop the scan of a file of millions, so it is counted and the scan goes on.
    It 'counts the values a JSON site cannot read, per site' {

        # ARRANGE
        $text = '<root><stats><s>not JSON</s></stats><stats><s>[1, 2]</s></stats></root>'
        $sites = @(New-SaveReferenceSite `
            -Kind 'Skill' `
            -Path @('stats', 's') `
            -AreJsonKeysIds)

        $expectedUnreadableCount = 2

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        $actualScan.References |
            Should -BeNullOrEmpty

        $actualScan.UnreadableCountBySite['stats/s{keys}'] |
            Should -Be $expectedUnreadableCount
    }

    It 'adds an ID''s uses across sites of one kind, and keeps kinds apart' {

        # ARRANGE
        $text = '<root><knownShips><st>a</st></knownShips><priorityShips><st>a</st></priorityShips>' +
            '<knownWeapons><st>a</st></knownWeapons></root>'
        $sites = @(
            New-SaveReferenceSite `
                -Kind 'Hull' `
                -Path @('knownShips', 'st')
            New-SaveReferenceSite `
                -Kind 'Hull' `
                -Path @('priorityShips', 'st')
            New-SaveReferenceSite `
                -Kind 'Weapon' `
                -Path @('knownWeapons', 'st')
        )

        $expectedReferences = @(
            'Hull a 2: knownShips/st=1 priorityShips/st=1',
            'Weapon a 1: knownWeapons/st=1')

        $expectedElementCount = 7

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        Format-References `
            -References $actualScan.References |
            Should -Be $expectedReferences

        $actualScan.
            ElementCount |
            Should -Be $expectedElementCount
    }

    # XStream writes characters XML 1.0 forbids, such as a control character in a ship's name.
    It 'reads a save holding a character XML 1.0 forbids, and decodes entities' {

        # ARRANGE
        $text = "<root><sN>a&#x1;b</sN><knownShips><st>a&amp;b</st></knownShips></root>"
        $sites = @(New-SaveReferenceSite `
            -Kind 'Hull' `
            -Path @('knownShips', 'st'))

        $expectedReferences = @('Hull a&b 1: knownShips/st=1')

        # ACT
        $actualScan = Read-TestSave `
            -Text $text `
            -Sites $sites

        # ASSERT
        Format-References `
            -References $actualScan.References |
            Should -Be $expectedReferences
    }

    It 'refuses a document type declaration' {

        # ARRANGE
        $text = '<!DOCTYPE root [<!ENTITY x "y">]><root>&x;</root>'
        $sites = @(New-SaveReferenceSite `
            -Kind 'Hull' `
            -Path @('root'))

        # ACT
        $actualError = { Read-TestSave `
            -Text $text `
            -Sites $sites } |
            Should -Throw -PassThru

        # ASSERT
        $actualError.Exception.Message |
            Should -BeLike '*DTD*'
    }
}
