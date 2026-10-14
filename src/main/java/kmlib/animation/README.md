# KMLib Animation

Part of [KMLib](../../../../../README.md).

Arithmetic over time and nothing else -
no colour,
no shape,
no element,
no pointer.
Whatever holds one of these decides for itself what its number drives,
which is what lets one fraction serve every animation on a surface
where a shared effect would have to know what each of them is made of.
Nothing here names the Starsector API,
and nothing here is persisted:
every type is only where an animation currently sits,
so none of them needs migrating.

## Index

- [Events and cycles](#events-and-cycles)
- [Stepped and read](#stepped-and-read)
- [The register](#the-register)
- [The two envelopes](#the-two-envelopes)

## Events and cycles

The one distinction that decides which type to reach for,
and which package it lives in:
whether something *happened* or something *is going on*.

- **An event** is a moment the caller can name -
  a key struck,
  a value confirmed,
  a toggle flipped.
  Its animation is timed from that moment,
  which nothing but the caller knows.
  These types live in this package.
- **A cycle** is ambient repetition nobody triggered -
  a beacon's glow,
  a distress pulse.
  It was already turning before anyone looked.
  These types live in [`cycles`](cycles/README.md).

## Stepped and read

How a type keeps its time is a separate question from what it answers.

**Stepped** types hold where they currently stand
and must be advanced by each frame's elapsed time.
Forgetting the advance freezes them part-way.

**Read** types derive the value from a clock at the instant it is asked.
There is no per-frame call to place and none to forget,
and two consumers drawing in different passes of one frame agree
because they evaluate the same function of the same clock.

Every cycle type is read.
Most event types are stepped;
`ClockedEasedFraction` is the read one.
It holds only the instant of its last turn and where the ramp stood then,
and derives the rest off the clock at each ask.
That suits a holder whose value is read in several passes,
or on frames nothing steps.
Its time comes off a monotonic source
for the reason [a phase does](cycles/README.md#why-a-phase-comes-off-real-time).

## The register

| Type | Keeps time | Answers |
| --- | --- | --- |
| `EasedFraction` | stepped | how far between two ends something is, advanced toward a target and eased only on read |
| `TraverseFraction` | stepped | the same, aimed at the far end while a flag holds and back at rest while it does not |
| `ClockedEasedFraction` | read | how far between two ends a ramp stands at the instant it is asked, off the moment it last turned |
| `TraverseDurations` | - | how long a motion takes each way, as a pair |
| `PulseEnvelope` | stepped | a lift a trigger sends out and back, or holds at its peak until released |
| `PulseEnvelopes<K>` | stepped | the same, one per element of a keyed set, advanced in one pass |

`TraverseDurations` holds nothing and so keeps no time;
it paces the stepped types.
A rise and a fall are separate values
because a motion answering input does not read right when they are equal -
a rise confirms what the player just did and wants to arrive,
while a fall is the element letting go and can take its time.

Easing is not this package's.
`EasedFraction`, `ClockedEasedFraction` and the cycles' `PhaseEnvelope`
ease on read through `kmlib.math.easing.Easing`,
so the curve is one shape to reason about rather than arithmetic threaded through each of them.

## The two envelopes

`PulseEnvelope` and `cycles.PhaseEnvelope` are not alternatives,
and the shared word is the whole reason this section exists.

- **`PulseEnvelope`** is a lift something *triggers*.
  It times its own rise and fall from that moment,
  holds where it stands,
  and has to be stepped every frame.
  Retriggering aims it back at the peak from wherever it is rather than replaying from zero,
  so a repeated trigger neither stacks past the peak nor dips first.
  A held trigger waits at the top until released,
  for an act the player is still making.
- **[`PhaseEnvelope`](cycles/README.md#the-envelope)** is a *shape read off a phase*:
  climbing over a leading share of the turn and falling over the rest of it.
  It holds nothing and steps nothing.
