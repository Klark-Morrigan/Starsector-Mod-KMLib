# KMLib UI Input

Part of [KMLib UI](../README.md).

The pointer's side of a panel: where it is, what it is on, what it just did there, and what the
panel holds in answer across frames - the fades and lifts a cell is showing, the moment the pointer
reached it, the list moving under the wheel or a dragged thumb. Everything here is timed and nothing
here is coloured: a controller answers with fractions, offsets and moments, and what a fraction lifts
a widget toward is the widget's own paint, resolved where its style is.

## Index

- [The cursor](#the-cursor)
- [Two controllers, one frame pass](#two-controllers-one-frame-pass)
- [Where a point lands](#where-a-point-lands)
- [What a body cell is doing](#what-a-body-cell-is-doing)
- [Moving the list](#moving-the-list)
- [Arrivals](#arrivals)
- [The hover report](#the-hover-report)
- [Sounds](#sounds)
- [Claiming an event](#claiming-an-event)
- [What is not here](#what-is-not-here)

## The cursor

`UiCursor` is the pointer in UI units: the LWJGL mouse rescaled along the
[screen axis](../screen/ScreenAxis.java) it was read from, so an x cannot be rescaled by the screen's
height. `CursorPosition` is the port over it, which is what a rule comparing the pointer against a box
takes when that rule has to be exercisable without a display.

## Two controllers, one frame pass

`PanelController` drives a headerless panel: it routes each pointer event to the end that answers it
and holds what the body's cells are doing. `TabPanelController` stands a header on top - the tab row's
own hits, the panel's own band button, the collapse handle, a bound key's blink - and delegates
everything else to a
`PanelController` for the body, so a tab panel's scroll and press behaviour is the plain panel's,
unchanged. What the header is doing is held as one value on `TabHeaderMotions`; what the body is doing
is held on the body's own controller, the end a body press actually lands on.

Both are charged by one per-frame pass off one reading of where the pointer is, resolved against the
placement being drawn rather than latched from the last event. That is what keeps a fade honest when
the panel moves under a still cursor, and what keeps the panel answering at a single rhythm: every
travel runs on [`EasedFraction`](../../../animation/EasedFraction.java) at one pair of
[`TraverseDurations`](../../../animation/TraverseDurations.java), arriving at twice the speed it
leaves at, since a motion answering the player has to land under the gesture that asked for it while
letting go answers nothing. The one exception is the hotkey blink, a strike rather than a travel,
which keeps a pace of its own on the tab panel.

Stepping the motions is the holder's obligation and cannot be checked from below: a lift is spent
only by being advanced, so a surface that never runs the pass shows none of its presses rather than
showing them stuck.

## Where a point lands

`ControlHitResolver` is the one hit-test a hover and a press both read: which control of a laid-out
panel a point is on and which cell of that control, answered subject to what is actually on screen -
a folded header presents no tabs, a folded body is behind its rail, a scrolled row is behind its
viewport - and to nothing else. Whether pressing that cell would *act* is `ControlActivation`'s, a
class of its own downstream of the resolver, because the two are asked by different readers and
folding them together is what would leave a lit control dark: a tabs row lights the tab it is already
showing while firing nothing there.

A hit is reduced to a `BodyCellSlot` - the control's place in the strip and the cell within it - and,
while the walk still holds the control, to what kind of thing was reached and where that control wants
the reading told (`HoveredBodyCell`), so nothing downstream asks the control a second time.

A tab panel's own band button is a hit-test beside the tabs rather than inside them
(`isBandButtonHoveredAt`, read by the fade that lights it and by the press that fires it). It has to
be: every index the row's selection, its lit tab and any bound keys resolve by is a position in the
tabs control, so a cell in that control which is not a tab would move all three one along. It is
gated exactly as the tabs are, standing in their row and being wiped with them by the fold - unlike
the handle, which draws past the fold because it is what brings a docked panel back. It carries a
lone fade rather than an entry in the row's keyed set, and no lift at all: it acts on the way down,
so there is nothing for a held lift to report by the release.

## What a body cell is doing

Every motion a body cell carries is keyed by its slot rather than by the widget standing in it,
because a host rebuilds its strip every frame: a hover belongs to the place under the pointer, so a
strip that changes under a still cursor keeps one continuous lift instead of dipping dark and starting
again. Three channels, held apart because they answer different questions about one cell:

- **Hover fades** (`HoverFades`, keyed; `HoverFade` for a lone element) - how far the cell has
  travelled onto its hovered look. Where the pointer is standing.
- **Press lifts** ([`PulseEnvelopes`](../../../animation/PulseEnvelopes.java)) - how far through its
  lift a pressed cell is. What the pointer just did there. A tab's lift is held at its peak while the
  button is down and released wherever the pointer has got to by then; a body cell's times its own
  fall, that control having acted on the way down with nothing left to let go of. Painted as
  [the style's press light](../render/gl/style/ControlPressLight.java), added over the wash rather than
  blended toward it.
- **The blink** - a bound key's confirmation on a tab, an envelope like a press but read with the fades:
  it carries a tab onto the hovered look rather than past it, so the two compose by the greater of them
  and a blink under the pointer shows nothing.

A body control's press is answered off the resolver rather than off the firing below it - sounded and
lifted from the one cell it resolved, so the two answers cannot part - and a press landing on an inert
cell therefore still answers like the press it was, while chrome resolving no cell stays silent and
unlit by the same rule rather than by a second one.

## Moving the list

`PanelScrollController` is the list's own end: the wheel over it, the thumb drag (grab, follow,
release), the `ScrollState` both write and the layout reads, and whether either moved the list since
a frame last asked. It is held apart from the cells' state above because the two share nothing - one
answers to where the pointer is, the other to where the list is - and a `PanelController` owns one
rather than a host wiring it.

The two acts are gated on different readings of the placement, deliberately. A drag needs a bar to
grab, so it asks `PanelPlacement.isScrollbarDrawn`; the wheel needs only somewhere to scroll to, so it
asks `isScrollbarNeeded`. The wheel is how a list whose host drew no bar is moved at all, and gating
it on the bar would strand that list. A held drag owns the event wherever the pointer is - even past
the panel edge - so it is asked before the panel asks whether the event is over it at all, and a bar
taken away under a held thumb carries the drag without moving anything, so the release still ends it
where the player let go.

The wheel sounds on the list having *moved* rather than on the notch having turned, which is why the
offset settles against the drawn overflow at the event rather than at the next layout: a request that
will be pulled back is not a list that went anywhere. A drag is silent by the same rule - one held act
rather than a moment - and both report through one latch, since what the latch answers is that content
moved and not what moved it.

## Arrivals

`HoverArrival` and its keyed form `KeyedHoverArrival` answer the *moment* the pointer reaches an
element, where a fade is the position it holds afterwards. Anything owed on reaching an element rather
than while on it reads one of these - a fraction parked at 1 says "on it" and never "just got here".

The body's latch adopts, without announcing, whatever a scroll carried under the cursor. An arrival is
the player reaching something, and rows sliding past a parked pointer were reached by nobody - so one
wheel turn is one sound rather than one per row it swept, a thumb drag cannot announce the rows it
carries either, and the frame after a scroll is an ordinary frame again.

## The hover report

`BodyHoverReporter` is the one reading that leaves the panel: it tells the host that built the control
under the pointer which of its cells that is, once per change, so a host answers a hover as it answers
a click without reading a cursor of its own. It keeps the channel each reading went out on, the leave
having to reach the host that heard the arrival after that host's per-frame spec is gone. Keyed by the
slot like the fades, so a scroll under a still pointer reports the row now under it where the arrival
latch stays silent - the rows moved, and nobody reached anything.

## Sounds

The controllers detect the moments a panel answers audibly - a press landing, an arrival, the wheel
moving the list - and ask the look's [`UiSoundScheme`](../sound/UiSoundScheme.java) what each sounds
like through `PanelSounds`, one held pair of player and scheme handed down from a tab panel to the
body beneath so the two halves of one panel cannot answer by different looks. A controller names
neither role nor volume; for an arrival it names only what kind of thing was reached
([`PointerArrivalTarget`](../sound/PointerArrivalTarget.java)), that being the one part of the question
a detector is what knows.

## Claiming an event

`PointerParking` is how a claim is made, which differs by the kind of event and matters to the screen
the panel is drawn over. A press or a wheel is consumed, the panel acting on it being the reason the
screen must not. A move is not: a consumed event is invisible to the screen, and a vanilla control only
lets go of its hover on hearing a move that is not on it, so consuming one leaves whatever was lit when
the pointer crossed onto the panel lit for as long as the pointer stays there. A move is claimed by
parking the pointer instead - the event is left unconsumed and moved far off every widget - and each
widget beneath reaches the true conclusion that the pointer is not on it. One rule for every control
on the screen, including ones a later game version adds.

The real event is moved rather than a substitute passed, because the engine hands input listeners a
copy of the frame's list and gives the original to the screen: only a change to the event itself
reaches both. The setters that move it are on the game's own event type rather than the modding
interface, so they are reached by name off the instance in hand, and a move that cannot be made is
consumed instead - the stale hover, never a live pointer at its real position.

## What is not here

- What a control looks like at a fraction - [`render/gl/controls`](../render/gl/controls/) binds a
  motion's progress into paint per cell.
- The geometry a hit-test walks - [`widgets`](../widgets/) lays the placements, and
  [`widgets/scroll`](../widgets/scroll/) computes the track, thumb and grab column a drag reads.
- The arithmetic over time - [`animation`](../../../animation/README.md) holds the envelopes and the
  eased fraction every motion here runs on.
- Which sound a role plays as - [`sound`](../sound/) holds the roles, cues and schemes.
