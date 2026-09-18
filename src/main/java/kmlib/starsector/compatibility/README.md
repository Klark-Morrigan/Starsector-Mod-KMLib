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
- [A binding is a third party and a consumer](#a-binding-is-a-third-party-and-a-consumer)
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
and `takeUnreported` hands over what was recorded without unlatching.
Once per binding is what stops one that fails on every frame filing a report per frame;
never unlatched is what stops the frame after a report filing it again.
The failure is built through a supplier invoked only on the record that is kept,
so a description that costs something - a reflective probe, a version read -
is paid once per subject.

[`CompatibilityNotice`](CompatibilityNotice.java) is the report:
a transient per-frame script that drains the record
and shows each failure as the game's own message dialog.
Transient because what it reports is a fact about the jars installed this session,
not about the save.
It writes the log line before it asks for the dialog,
so the line a report to the third party's author is written from
exists whatever the dialog call does.

```mermaid
sequenceDiagram
  participant B as a binding
  participant Reg as CompatibilityFailures
  participant N as CompatibilityNotice
  participant UI as CampaignUIAPI
  B->>Reg: recordOnce(subject, consumer, describe)
  Reg->>Reg: latch; build the failure once
  B->>Reg: recordOnce(subject, consumer, describe)
  Reg-->>B: ignored
  N->>Reg: hasUnreported()
  N->>Reg: takeUnreported()
  N->>N: log describeForLog()
  N->>UI: showMessageDialog(describeForPlayer())
```

## A binding is a third party and a consumer

What is latched is the pair,
not the third party alone.
[`CompatibilityConsumer`](CompatibilityConsumer.java) is the mod that took the binding:
the key its records latch under,
and the sentence naming what it loses.

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

[`CompatibilityFailure`](CompatibilityFailure.java) is one binding that stopped holding:
the subject, the sentence naming what the session loses,
the member or detail that broke, and the cause where there was one.
It composes two sentences.
The player's modal is drawn from KMLib's own strings category,
so the wording is editable without a rebuild;
the log line is built from literals,
so it reads the same whatever the install's localisation
and holds on a path where the game's settings may not be up yet.
The wording rationale lives with the strings,
in [`KmlibStringKeys`](../strings/KmlibStringKeys.java).

## One dialog per frame

The game's message dialog is dropped, silently,
when asked for while any dialog is up -
and its dialog check reads the same flag.
So the notice gates on that check,
and shows one failure per frame rather than every failure it took:
a second dialog asked for on the same frame as the first would be the one dropped.
A frame later it waits for the first to be dismissed.

## Threads

Writers are not on one thread.
A deferred renderer runs a binding's command on its own render thread
while the game thread resolves and calls the same binding,
and the two can fail on the same subject in the same frame.
The record is safe from any thread,
and the latch is one atomic add:
one of the two wins it, and the other's description is never built.
The notice runs on the campaign thread alone.

## What is not here

- The bindings that record.
  Detecting that a binding no longer holds is the binder's job,
  and which member broke is what it records.
- The transient install.
  [`starsector/scripts/`](../scripts/) is what registers the notice on a sector,
  and [`KMLib_ModPlugin`](../../KMLib_ModPlugin.java) is what asks it to on each load.
- The wording.
  [`starsector/strings/`](../strings/) holds the category and the string IDs the modal is drawn from.
