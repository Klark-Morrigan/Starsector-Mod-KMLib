# KMLib UI Map Controls

Part of [KMLib UI](../../README.md).

The writes into the map's own furniture, as against the [reads](../probes/README.md) below it:
one more toggle stood on the row of buttons a map screen is furnished from,
made by the row itself so it is a toggle of the game's making rather than a lookalike.
Nothing here names an obfuscated type,
which is what lets the code that runs against the game's row be the same code a row standing in for it is put through.

## Index

- [Reaching the row](#reaching-the-row)
- [Standing a button on it](#standing-a-button-on-it)
- [Fitting the control to the row](#fitting-the-control-to-the-row)
- [What a holder asks](#what-a-holder-asks)
- [The control's key](#the-controls-key)

## Reaching the row

[`MapFilterRows`](MapFilterRows.java) reaches the row of toggles a map screen is furnished from:
the `M` screen's full-width strip and the intel visor's shorter band alike,
both being the same widget reached by the same unobfuscated accessor off the map itself.
It hands the row back as a [`MapFilterRow`](MapFilterRow.java):
a handle rather than the widget,
answering only which row it is, for the reason that class states.

## Standing a button on it

[`VanillaToggleFactory`](VanillaToggleFactory.java) stands one more button at the end of the row,
built by the row's own members and laid out by them.

Those members are matched by signature and never by name:
the two it needs carry the same regenerated name in every build.
A signature fitting twice is refused as readily as one fitting nothing,
guessing being a coin toss made inside another party's widget.

A button's clicks are redirected before it is ever appended,
since the row answers a click by rewriting the whole of the player's map filters from its own toggles.
The stand-in bound in its place is built over whatever interface the button names,
through `java.lang.invoke` rather than the reflection proxy it resembles:
the game's script classloader denies mod code `java.lang.reflect` by name
and allows what a proxy is made of underneath.

## Fitting the control to the row

[`MapFilterToggle`](MapFilterToggle.java) is what a caller holds,
and what fits the control to the row before any of that is attempted.
Its height comes off the row and its width off the button already standing at the end of it,
so one path lays a control on the `M` screen's larger buttons and on the intel band's smaller ones
with no branch on which screen is up.
An append the width left will not take is declined outright.

The row does not clip what it holds,
so an unguarded control draws off the end of the strip rather than failing.
The guard is not there for the game's own rows, which both have room to spare,
but for the row another mod reached first.

## What a holder asks

The handle answers the three things a holder has to ask:

- whether the button is ticked;
- how to set it without that travelling back out as a click;
- whether it is still standing on the row currently on screen.
  This one is asked every frame, since every open of the map screen builds a new row.

It also hangs the control's hover, six of the row's own carrying one.
That is asked of the handle rather than of the button, which is never handed out,
and placed above, where the row's own sit.
A hover that cannot be hung costs the hover alone:
the box is on the row already, and the row offers no way to take one off again.

## The control's key

The handle binds the control's key through the published call the game drives its own buttons by,
and nothing has to be arranged for that key to be live in the right places.
A button matches its key while it is on screen and its row is taking input,
so a control that never reached a screen answers nothing on it,
and there is no listener to register or take away.
A cleared binding reads as a code of zero rather than as an absent one,
and is left unbound rather than passed through as a key nothing can press.

The key is also said in the button's own words through [`buttons`](../../buttons/),
because a key nothing shows is a key nobody presses.
The game will not say it here:
its own key announcement writes into the words of one kind of button,
and a filter row is furnished with another,
so a control keyed by bare code is bound and silent.
The game is not asked to spell it out as well,
that being a request it cannot meet on this kind of button,
and a second announcement beside this one on any build that could.
