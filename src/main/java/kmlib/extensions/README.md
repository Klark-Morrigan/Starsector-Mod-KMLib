# KMLib Optional Mod Seams

Part of [KMLib](../../../../../README.md).

How this library reaches a mod it does not require.
The machinery in this package -
`ExtensionPoint`,
`FallbackToDefaults`,
`WorkOutcome` -
backs one of the three shapes;
the other two need no machinery and are described here
so the choice between them is made in one place.
Nothing in this package names the Starsector API or any mod.

## Index

- [What every shape shares](#what-every-shape-shares)
- [Shape 1: a routine taken over](#shape-1-a-routine-taken-over)
- [Shape 2: a fact the mod publishes](#shape-2-a-fact-the-mod-publishes)
- [Shape 3: an answer that composes](#shape-3-an-answer-that-composes)
- [Choosing between them](#choosing-between-them)

## What every shape shares

- **A presence gate**,
  one per mod,
  holding that mod's ID in one place -
  `NexerelinPresence`,
  `RandomAssortmentOfThingsPresence`,
  `ConsoleCommandsPresence`.
  Gate and ID are both public,
  so a consuming mod integrating with the same mod asks here rather than writing the ID again.
- **An adapter** in that mod's own package under `kmlib.mods`,
  which is where every one of these lives and the only place in the library allowed to name a mod -
  `kmlib.starsector` is closed to that subtree by the layering gate,
  so a new adapter is covered by the rule the moment it is added.
  The sole reference to a type of the mod's lives in a nested holder class the classloader does not resolve until the gate has passed,
  so an install without the mod never seeks a class it does not have.
- **An integration facade** in that same package -
  `NexerelinIntegration`,
  `RandomAssortmentOfThingsIntegration` -
  registering the adapters at load,
  and only where the mod is enabled.
  The arrow runs from the mod's package to the operation,
  never back:
  an operation that names a mod has taken a decision it cannot see the inputs to.
  Each facade keeps a package-private overload taking the mod-enabled answer,
  so both installs are posed on a machine that has whichever mods it happens to have.
- **Soft dependency status**:
  every mod reached this way is absent from `mod_info.json`.
- **A boundary where the adapter is called**.
  The holder defers the mod's types past install,
  so a mod that changed underneath an adapter is met when the adapter is first called,
  not when it is registered.
  Each shape answers that at the call, as set out under it,
  and shapes 1 and 3 report it through the compatibility channel
  under the integration the registration named.

Registration happens in `KMLib_ModPlugin.installOptionalModIntegrations`,
one guarded step per facade,
so a mod whose registration throws costs only its own adapters.
The describer each step reports under is handed to the facade as well,
so an adapter failing at install and one failing when called are one report.

## Shape 1: a routine taken over

The mod has its own sequence for something this library also does,
and takes the whole operation while ours stands down -
founding a colony,
handing one to another owner,
deciding which counters a colony trades over.

`ExtensionPoint` holds it:
**one implementation,
the last registered**,
because work is taken over whole
and a second implementation behind the first would be one nothing ever reaches.
A displacement is logged rather than left to be discovered.
The registrant states through `FallbackToDefaults`
whether a decline may be done the ordinary way instead;
`FORBIDDEN` fails the run where it happened rather than producing something wrong nothing can trace.

The seam interface performs the work *and* answers whether it did,
in one method returning `WorkOutcome`.
Split into a question and a command,
a caller could ask and then not call.

The port hands the work over through `offerWork`,
never by calling the implementation itself,
so the call runs inside the point's boundary.
An implementation that throws is taken out for the session and reported once,
and what that means for the call in hand turns on what was thrown.
A `LinkageError` comes before any of the implementation's own work,
so the call settles as a decline and the fallback policy answers it.
A `RuntimeException` can come with the work half done,
so it is passed on to the caller rather than settled -
the ordinary sequence run over half a founding would build on a state neither sequence produces.
The policy outlives the implementation:
one that forbade the fallback goes on refusing runs after it is out.

Held by `ColonisationRoutines`,
`OwnershipTransferRoutines`,
`OwnerSubmarketRules`.

## Shape 2: a fact the mod publishes

Something only the mod can answer,
with no sequence to stand down -
whether a console is taking text entry,
whether a mini-map has replaced the campaign radar.

No register:
a gate class per mod,
behind which the mod's own read is a seam.
Where more than one mod could answer,
the gate is stated as a role a caller holds and the mod's adapter implements it (`CampaignMinimap`).
Where only one mod ever will,
the gate class **is** what callers hold (`ConsoleCommandsOverlay`),
because an interface with one permanent implementation says nothing a reader can act on -
what varies there is the mod's own read underneath,
so that is the seam a suite stands in (`ConsoleOverlayPresence`).

Either way the seam is what makes the answer settleable without a game running,
which matters because it decides whether a caller draws at all.

Such a read **fails open**:
mod absent,
class gone,
accessor moved by a release,
read throwing -
each reports the answer that leaves a caller behaving as it did before the question existed,
and warns once naming the hop that broke rather than per frame.
Where the answer is a feature the player would miss -
the alliances a map groups factions by -
the read is also taken out for the session and reported through the compatibility channel,
since a warning in the log reaches no player.

## Shape 3: an answer that composes

The mod adds to an answer the library already has,
rather than replacing it -
a means of reaching a star system that the engine does not model.

A **register of many**,
`ModdedSystemAccessRoutes`,
keyed by the name each was registered under and consulted in insertion order.
Not an `ExtensionPoint`:
access is not taken over,
two mods can each add a way in,
both are true at once,
and keeping only the last registered would silently drop one mod's routes
because another loaded after it.
One route answering false leaves the question where it found it.

A route that throws is taken out for the session, reported once,
and answers as not granting access - the answer on an install without its mod.
Whatever was thrown is treated alike,
a read having no half-done work to protect,
and the routes after it are still asked.

## Choosing between them

| The mod... | Shape | Held by |
| --- | --- | --- |
| does the whole job instead of us | 1 | `ExtensionPoint` |
| knows something we cannot ask anyone else | 2 | a role the caller holds |
| adds to an answer we already have | 3 | a register of many |

The test is what two installed mods would mean.
If the second would have to displace the first,
it is shape 1.
If their answers would both stand,
it is shape 3.
