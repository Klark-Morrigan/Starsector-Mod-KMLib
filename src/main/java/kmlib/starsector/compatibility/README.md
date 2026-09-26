# KMLib Compatibility

Part of [KMLib](../../../../../../README.md).

What a binding to third-party code that has stopped holding is reported as,
and how that report reaches the player:
recorded where the binding broke,
shown from a frame that can open a dialog.
The package holds no binding of its own to any third party;
what it holds is the channel a binding reports through.

## Index

- [Record, then report](#record-then-report)
- [Two surfaces, one record](#two-surfaces-one-record)
- [A binding is a third party and a consumer](#a-binding-is-a-third-party-and-a-consumer)
- [A feature key one mod reused](#a-feature-key-one-mod-reused)
- [A step that integrates with another mod](#a-step-that-integrates-with-another-mod)
- [One record per session](#one-record-per-session)
- [What a failure says](#what-a-failure-says)
- [One dialog per frame](#one-dialog-per-frame)
- [Threads](#threads)
- [What is not here](#what-is-not-here)

## Record, then report

A binding breaks where it is used - a render pass, a load step -
and neither is a place a player can be told from:
no dialog can be opened inside a render pass,
and at load there may be no campaign UI to open one on.
So a record and a report are two steps.

[`CompatibilityFailures`](CompatibilityFailures.java) is the record:
`recordOnce` latches a binding on its first record for the session and ignores every record afterwards,
`hasUnreported` is the empty check a per-frame caller gates on,
and `takeNextUnreported` hands the oldest one over without unlatching.
One at a time, because a reporter can only show one at a time -
handing over the lot would leave it holding a second queue of what it could not yet show.
Once per binding is what stops one that fails on every frame filing a report per frame;
never unlatched is what stops the frame after a report filing it again.
The failure is built through a describer invoked only on the record that is kept,
so a description that costs something - a reflective probe, a version read -
is paid once per binding.
The describer is handed the consumer to compose against rather than reading the one the caller holds:
the two differ only where the record had to tell a reused key apart,
and the report is filed under whichever key the record settled on.

[`CompatibilityNotice`](CompatibilityNotice.java) is the report:
a transient per-frame script that drains the record
and shows each failure as the game's own confirm dialog, sized and carrying a single button.
A confirm dialog rather than the message dialog beside it,
which is how the game puts up its own one-button notices:
the message dialog takes no size, so its fixed panel cut the last rows of this block off
below its own edge, and it answers nothing about whether it opened.
Transient because what it reports is a fact about the jars installed this session,
not about the save.
It writes the log block before it asks for the dialog,
so what a report to the third party's author is written from
exists whatever the dialog call does -
which is also what the modal's own pointer at the log promises is there.

```mermaid
sequenceDiagram
  participant B as a binding
  participant Reg as CompatibilityFailures
  participant N as CompatibilityNotice
  participant UI as CampaignUIAPI
  B->>Reg: recordOnce(subject, consumer, describe)
  Reg->>Reg: latch; describe(consumer) once
  B->>Reg: recordOnce(subject, consumer, describe)
  Reg-->>B: ignored
  N->>Reg: hasUnreported()
  N->>Reg: takeNextUnreported()
  N->>N: log describeForLog()
  N->>UI: showConfirmDialog(describeForPlayer(), size)
```

## Two surfaces, one record

A binding to a renderer breaks inside a map render pass,
and the notice above is a transient script on the sector,
which the campaign engine does not advance while a core screen is up.
So a failure found on the map reaches that reporter only once the player has left the screen it was about.

[`ui/compatibility/`](../ui/compatibility/) is the second surface:
a panel stood in the core UI's own widget tree,
which the screen holding it advances,
so it can be raised on the frame the failure is found.
It is drawn from the game's own text widgets rather than handed over as one string,
which is what lets each row's value be tinted while its label is left alone.

Both drain the one record and neither repeats the other.
Whichever shows a failure takes it,
so the dialog never re-opens a notice already dismissed on the map,
and a failure the panel declines to show is still waiting when the player returns to the campaign.
Declining is the ordinary case rather than an error:
there is no screen to stand on at load, and none on the campaign view.

What the notice says is settled here and not on either surface.
[`CompatibilityFailure`](CompatibilityFailure.java) answers it as
[`CompatibilityNoticeLine`](CompatibilityNoticeLine.java)s -
a heading, a diagnosis, the rows, a closing line -
each carrying its wording and the runs of it that stand out.
A surface of widgets tints those runs;
the dialog reads the same lines plain.
Which lines there are and what order they come in is decided once,
so a line added to the notice arrives on both surfaces rather than on whichever was edited.

Emphasis is named, never marked up.
Every run that stands out is a value the composition filled into a template -
a name, a version, or a phrase with a key of its own -
so the composition already holds each one and simply says which they were.
Nothing parses the wording,
and nothing inside a value can be mistaken for a directive.
The runs are given in reading order because that is how the engine matches them,
each searched from where the last one ended:
a name brought forward early and appearing again inside a later phrase
is tinted once for each rather than twice for the first.

The diagnosis is the part of the notice that varies by what was found.
[`CompatibilitySubject`](CompatibilitySubject.java) reads how the installed version stands to the targeted one -
behind, ahead, the same, or not comparable -
and the notice advises an update where it is behind and a downgrade or a wait where it is ahead.
Where the version could not be read it runs to three lines instead,
a lead and the two cases under it,
because both directions are live at once
and a sentence carrying both reads as one tangled claim.
Where the two name one release there is no version to move to,
so the line asks for a report instead:
the rows either side of it show one version twice,
which on its own reads as though nothing is wrong,
and a version match is exactly what a fault in the integration survives.
Nothing at all where the build stamped no target, since every sentence names the release to move to.
Wording only: nothing gates on the comparison, so a self-report that lies costs a sentence.

What each kind of emphasis means is fixed, and what colour it takes is the surface's.
A name, a version or the log's own file is brought forward;
what is wrong warns, both the state and the instruction for fixing it,
so a player scanning for the trouble finds it without reading the sentence;
and what goes on working regardless is the one run set at ease,
so the good news and the bad do not read as one list the eye has to parse.

A mod is brought forward wherever it is named, including inside a warning.
Two of the phrases name a mod that way - the instruction to downgrade or wait, which names both,
and the one asking for a report - so each is split into runs rather than warned as one:
the wording around the names warns, the names themselves are brought forward,
and the version stays inside the warning, being what the player is told to move to
rather than a party to the mismatch.
A value marked the same as the wording around it is folded into that run instead of kept apart,
which is what keeps a two-letter join out of the run list -
a short run being the one thing that could match inside a longer word.
[`CompatibilityNoticeLines`](CompatibilityNoticeLines.java) is where those rules live,
apart from the failure they compose, since none of them knows what a compatibility failure is.

## A binding is a third party and a consumer

What is latched is the pair,
not the third party alone.
[`CompatibilityConsumer`](CompatibilityConsumer.java) is the mod that took the binding:
which mod it is, which of its features the binding serves,
and the sentence naming what that feature loses.

The key is composed from those first two rather than supplied whole.
A key a caller spelled outright is a key two mods can spell the same,
and a collision puts both behind one latch -
so the second mod's player is told what the first one lost, or told nothing.
Led by a mod's own ID that cannot happen between mods:
the ID namespaces the key,
and a mod naming another's ID would be claiming to be it.
The feature half stays the caller's because it is the half nothing else knows:
one mod can take two bindings to one third party and lose two different things by them,
and those have to latch apart.

The ID is not checked against the game where the value is built.
That would put a mod-manager read on the healthy path,
where this value is made and then never looked at again,
and there is no honest answer for the moments before the game is up.
The report resolves it instead,
naming the mod beside its ID where the game holds a name for it
and showing the ID alone where it does not -
which is where an invented or misspelled one surfaces.

One broken third party costs every mod bound to it something of its own.
A latch on the third party alone would keep whichever mod recorded first
and drop the rest,
so those players would be told what another mod lost, or told nothing -
and which mod reported would come down to which happened to bind first.
Latched on the pair, each mod's report is shown,
and a mod whose binding fails at link time and again at call time still reports once.

The sentence is the consumer's for the same reason it holds the key.
What a failed binding costs is knowledge of the feature built over it:
the library knows the third party, both versions, the member that moved and what was thrown,
and nothing about what was drawn over the reading it can no longer serve.
So the consumer writes that sentence,
out of its own strings,
and the library puts it in the failure's lost-feature slot.

## A feature key one mod reused

The mod half of a key cannot collide between mods.
What is left is one mod spelling one feature key for two features -
its own bug, in its own code -
and latched on the pair alone that cost the second feature's report entirely:
the record read a pair it already held and dropped it,
exactly as it drops the same feature recording twice.

So the latch is the triple of subject, consumer key and the consumer's sentence,
and what tells a collision from the same feature again is the sentence.
The same feature recording again carries the same sentence and is dropped as before;
a second feature under one key carries a different one and is kept,
with the describer handed the consumer under its feature key numbered - `<feature>-2`, then `-3` -
so both reports arrive and the log's consumer row names the reuse where its author will see it.
Read off the consumer the caller already passed,
so the dropped path still builds nothing.

At the record and never where a consumer is built.
That value is built freely - the map's is built afresh every time its publisher is -
and a registry numbering each construction would mint a new key per map open,
break the latch outright,
and report one failure once a frame forever.

Which feature keeps the bare key depends on which failed first,
so two sessions can name one feature differently.
That is acceptable for a session-scoped latch and is not the row's job anyway:
a numbered key in a log means "this mod reused a key", which is the finding.

## A step that integrates with another mod

A binding does not have to be a call into somebody else's internals.
A mod's start-up wiring registers its adapters for whichever optional mods the install has,
and a registration that throws is the same class of silent degradation:
the player enabled a mod, the mod did not integrate,
and nothing said so but a line in the log.

[`ModIntegration`](ModIntegration.java) is that case as the channel states one -
the third party the step is with, and the mod that loses something by it -
and it composes the failure from what was thrown and files it itself.
Filing rather than handing a composed failure back to the guard,
because the record hands a describer the consumer it filed under,
and that hand-off is the channel's business:
a guard is about the failure boundary a step runs behind, not about the shape of a report.
Two things run the other way round from a binding to a renderer patch.
The versions: nothing was compiled against an optional mod,
the binding going through the game's own API,
so the built-for row stands at its unknown wording
while the installed one is read off the mod manager.
And what broke: there is no probe to run and no member to point at,
the step having called into the mod and the mod having refused,
so what was thrown is the whole of the finding.

The guard is [`starsector/startup/`](../startup/),
which is also where the phrase a log's "failed while" row takes for these is spelled.
KMLib's own plugin is the first consumer to run through it,
losing library features to third parties under the library's own mod ID -
which is how a channel built for one client shows it is one.

An adapter reaches its mod's types only when first called,
so the same failure more often arrives at call time than at start-up.
The places that hold a registered adapter -
the extension points and the access routes, set out in [`extensions/`](../../extensions/README.md) -
take a describer of the registering integration's `ModIntegration` at registration
and file through an [`IntegrationFailureReporter`](IntegrationFailureReporter.java) built from it,
each spelling its own "failed while" phrase.
One describer for both is what makes a failure at install and one at call a single report,
which is why each integration holds its own describer beside the code that binds it.

The reporter is also where a failure's trace is logged.
The report's block carries the trace of the failure it was composed from,
so a boundary logs one line and the reporter logs the trace only where no block will:
a report that could not be composed,
and a failure the record dropped because the binding was already reported,
whose block carries the first failure's trace rather than this one's.

## One record per session

`CompatibilityFailures.SESSION_RECORD` is the record every binding records into
and the one the notice drains.
It is one per session rather than per sector,
because what it holds is a fact about the jars loaded into the process:
a binding is resolved once and held for as long as the game runs,
and its first record can come from a load step before any sector exists.
A sector loaded later finds what was recorded before it waiting.

The notice is installed on every game load,
by the library's own plugin,
as a transient script on the loaded sector.
Per load because a transient script does not survive one,
and on the sector because the dialog it opens is on that sector's campaign UI.

## What a failure says

[`CompatibilitySubject`](CompatibilitySubject.java) names the third party
with the two versions a mismatch is stated as:
the release KMLib was compiled against and the one installed now.
Either may be absent -
both are read from the third party rather than from a convention -
and an absent one renders as an explicit unknown rather than as `null`.
A self-reported `v` prefix is stripped before either is shown:
the label a version sits under already says it is one,
and the two come from two places that need not agree about carrying it.

[`CompatibilityFailure`](CompatibilityFailure.java) is one binding that stopped holding,
as four values of four different types:
the [subject](CompatibilitySubject.java), the [consumer](CompatibilityConsumer.java) that took it,
the [breakage](CompatibilityBreakage.java) - which guard caught it and what no longer holds -
and the cause where there was one.
Four types rather than a subject and a run of strings,
because a run of same-typed components is a run a caller can transpose with nothing to catch it,
and a report naming the broken member where the site belongs reads as plausibly as the right one.

It composes two readings of that, for two audiences.

The player's is the modal:
the same block of labelled rows, in the player's half of the readings -
which mod lost something, the two versions, what it costs and what it does not -
under a heading naming the third party and pointing at the log.
That pointer is generic, shown for every subject,
and it is kept honest by the notice writing the log block before it asks for the dialog
and whatever the dialog then does:
a player sent to the log always finds something there.
The last row is left out entirely where the consuming mod said nothing about what still works,
a row claiming nothing being worse than no row.
No mechanics rows.
Which guard caught it and which member moved mean nothing to a player
and everything to whoever fixes it,
and that reader has the log.
Every row is a whole template in KMLib's own strings category, label and padding together,
so the wording is editable without a rebuild
and a translation whose labels run longer keeps its own alignment
rather than inheriting a column width measured in English.

The heading names the third party alone.
A heading carrying the consuming mod's name as well
would put the player's eye on the mod that is working correctly;
the mod belongs in a row of the body, which is where it is.

The log's is a block of labelled rows:
both versions, which guard caught the binding, what no longer holds,
which consumer filed it and what that consumer lost,
with the throwable handed to the logger beside it.
A block rather than a line because those are separate readings
and the one that matters varies by report -
a mismatch is diagnosed off the site and the broken member,
an "is this even mine" question off the versions -
and because the values a reader compares across two logs have to line up under each other.
Which guard caught it is the row that earns its place:
a member that moved and an entry point declared and then refused
both arrive as a broken binding,
and where it was caught is the only reading that says which.
Built from literals,
so it reads the same whatever the install's localisation
and holds on a path where the game's settings may not be up yet.

The wording rationale lives with the strings,
in [`KmlibStringKeys`](../strings/KmlibStringKeys.java).

## One dialog per frame

The game refuses a dialog asked for while any dialog is up,
and its dialog check reads the same flag.
So the notice gates on that check,
and shows one failure per frame rather than every failure it took:
a second dialog asked for on the same frame as the first would be the one refused.
A frame later it waits for the first to be dismissed.
The call answers whether it opened,
so a refusal that slips past the gate is logged rather than lost -
the failure's own block having already been written, a player who never saw the modal
still leaves a report behind.

## Threads

Writers are not on one thread.
A deferred renderer runs a binding's command on its own render thread
while the game thread resolves and calls the same binding,
and the two can fail on the same subject in the same frame.
The record is safe from any thread,
and the latch is taken under one lock, held for a lookup and an append:
whether a sentence is new under its key and which position it takes there are one decision,
so one of the two wins it, and the other's description is never built.
The notice runs on the campaign thread alone.

## What is not here

- The bindings that record.
  Detecting that a binding no longer holds is the binder's job,
  and which member broke is what it records.
- The guard a start-up step runs behind.
  [`starsector/startup/`](../startup/) owns the failure boundary, the log line
  and the phrase a failed installation files under;
  this package owns only what a failure it caught is reported as.
- The panel the notice is drawn on where a screen can hold one.
  [`ui/compatibility/`](../ui/compatibility/) owns that surface and the widget tree it stands in;
  this package owns what a failure says and which rows say it.
- The transient install.
  [`starsector/scripts/`](../scripts/) is what registers the notice on a sector,
  and [`KMLib_ModPlugin`](../../KMLib_ModPlugin.java) is what asks it to on each load.
- The wording.
  [`starsector/strings/`](../strings/) holds the category and the string IDs the modal is drawn from.
