# KMLib UI Core UI

Part of [KMLib UI](../README.md).

The reach into the game's live core-UI widget tree,
and the coarse questions about it that no one screen owns:
which core screen is up,
what stands over it,
and which tabs are still on screen after the core UI stopped naming them.
This package is the only one in the library that reflects at all,
so what the game's reflection ban exists to contain stays contained here.

## Index

- [The reach](#the-reach)
- [Reflection, contained](#reflection-contained)
- [Writing to the tree](#writing-to-the-tree)
- [Which screen is up](#which-screen-is-up)
- [What stands over a screen](#what-stands-over-a-screen)
- [Tabs still fading out](#tabs-still-fading-out)
- [What is not here](#what-is-not-here)

## The reach

[`CoreUiTree`](CoreUiTree.java) is the by-name reach that every screen's probes walk:
the hops from the campaign UI down to the tab that is up,
one component's children once there,
and whether a shape carries a given hop at all.
The hops it takes are ones the core UI publishes and keeps.

[`CoreUiMethods`](CoreUiMethods.java) is the by-shape reach, for the members a name cannot find.
An obfuscated member is renamed with every game build,
so a caller after one is handed the whole set a class declares
and recognises what it wants by the signature,
as one [`CoreUiMethod`](CoreUiMethod.java) it can call without ever naming it.
Declared and inherited are two reads rather than one, because the difference decides matches:
a signature matched over what a class merely inherits meets every base class's members
and finds candidates that were never candidates.

## Reflection, contained

The facility both reaches go through is split
so that the part which can be read is not the part which can be misused:

- [`ReflectionBypass`](ReflectionBypass.java) holds the classloader route and the method handles over it,
  and no logic whatever.
- [`ReflectedMembers`](ReflectedMembers.java) holds every search and every by-name read, write and call,
  and no way into reflection of its own.
- [`ParameterCompatibility`](ParameterCompatibility.java) holds the one rule deciding which member a caller's arguments reach.
  It touches no reflection at all,
  so it is testable on its own.

A member any of them finds comes back as one
[`ReflectedField`](ReflectedField.java), [`ReflectedMethod`](ReflectedMethod.java) or [`ReflectedConstructor`](ReflectedConstructor.java),
described once and callable without its own type ever being named -
those types being exactly what the game's classloader denies mod code.
What a search matches on is stated as a
[`FieldQuery`](FieldQuery.java), [`MethodQuery`](MethodQuery.java) or [`ConstructorQuery`](ConstructorQuery.java),
an unstated criterion meaning "any".
That is what lets one call site ask by name where the game publishes a member
and by signature where it renames one every build.

All of it is package-private,
and carried rather than depended on,
because it is another author's code under a licence of its own -
see [THIRD-PARTY-NOTICES.md](../../../../../../../THIRD-PARTY-NOTICES.md).

## Writing to the tree

The [repaint port](CoreUiComponentRepainter.java) and its [reflective implementation](ReflectiveCoreUiComponentRepainter.java)
drive a component's own draw through the same reach,
clipped to a region a caller hands it.
It is the one thing here that writes to the tree rather than reading it.

## Which screen is up

[`CampaignScreenView`](CampaignScreenView.java) answers which core tab is up,
and whether none of it is and the player is looking at the campaign world itself -
the second being the first plus "no interaction dialog".

The tab is published API with one correction,
and the correction is why the read exists rather than each caller asking the campaign UI.
A dialog goes on handing out the core UI of a screen the player has closed,
still naming the tab it was showing,
so a tab it reported stands only while that core is still on screen.
That is the one thing the view reaches the tree for,
and only far enough to ask whether a panel is still drawn.

## What stands over a screen

[`CoreUiDialogView`](CoreUiDialogView.java) answers what stands over a screen rather than which screen is up:
whether a core screen has raised a modal in front of itself,
and how far through its own fade that modal stands -
the same curve it darkens the screen by.
Both come off one walk, as an [`OverlayPresence`](OverlayPresence.java).

Presence and fade travel together because they are owed at different moments and must not disagree.
A modal claims input from the frame it is raised, while its fade is still at nothing,
so anything standing aside stands aside at once,
and only what it paints follows the fade.

That shape is not the modal's alone.
Anything raised over a screen answers in it,
a panel a mod stands in the core UI included,
which no walk here can find, descending from nothing of the game's.
So a caller stands aside for every such thing on one reading rather than one per kind.
Which frames the flag covers is each answerer's to state, and they differ:
the game holds a modal raised until its fade has run out,
that being how long it keeps it in the tree intercepting,
while something a mod raises may hand the screen back on the press
and go on painting for the length of its fall.

It is a read of its own rather than a corner of the tab one,
because the published dialog state answers neither question:
that reports the campaign's conversations,
and a modal stood up inside the core UI is not one.
It is also asked before anything drawn after the core UI draws at all.

[`CodexView`](CodexView.java) answers the same question for the codex.
It is a separate reading rather than a case of the modal one, because the codex is not core UI at all:
the campaign state holds a second screen panel of its own for it,
so no walk of the core UI's children can reach it,
and it carries none of the modal marker either.
It is read off the app state instead,
which reports the codex up both ways it can be raised - over a screen, and from the campaign itself -
where a widget-shaped rule would have to find two different things in two different trees.

## Tabs still fading out

[`ClosingCoreTabs`](ClosingCoreTabs.java) reads the tabs the core UI is still drawing after it stopped naming them.
Closing a tab starts the fade-out of the wrapper the tab sits in
and clears the current tab in the same call,
so for the length of that fade every read through the current tab says the screen is gone.
A wrapper is recognised by the `getPanel` hop it offers;
its class name is obfuscator output.
What a closing tab means for the maps is set out in the hub's
[Two hosts, one map widget](../README.md#two-hosts-one-map-widget).

## What is not here

- A surface of one's own over a screen.
  That is [`modal`](../modal/),
  which reads this package and is never read by it.
- Any one screen's probes.
  The map's are in [`map/probes`](../map/probes/README.md),
  and the intel screen's in [`intel`](../intel/).
