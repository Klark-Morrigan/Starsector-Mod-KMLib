# KMLib Profiling

Part of [KMLib](../../../../../README.md).

Where a frame went, and what it went on. A caller brackets work in a named
[section](ProfileSection.java) and adds to [counters](ProfileCounter.java) as it goes; whichever
[profiler is bound](ActiveProfiler.java) keeps a tree of those calls, says the alarming ones in the
log as they happen, and hands the whole capture over on request. Nothing here draws anything or
knows what a section measures - a section is a name, and what it is worth is stated beside it.

The library is [silent](SilentProfiler.java) until a mod binds otherwise, because a capture holds a
tree in memory for as long as it is bound and a player who never opens a readout must pay nothing
for one.

## Index

- [The three levels](#the-three-levels)
- [What a section states about itself](#what-a-section-states-about-itself)
- [Counters, and what earns one](#counters-and-what-earns-one)
- [Tags: a detail of one call](#tags-a-detail-of-one-call)
- [Budgets: numbers into findings](#budgets-numbers-into-findings)
- [Telling a cold call from a slow one](#telling-a-cold-call-from-a-slow-one)
- [Origins: which game a row came from](#origins-which-game-a-row-came-from)
- [Reading a capture](#reading-a-capture)
- [What a row's columns say](#what-a-rows-columns-say)
- [Wiring it into a mod](#wiring-it-into-a-mod)

## The three levels

[`ProfileLevel`](ProfileLevel.java) answers two questions with one value: how much detail a capture
keeps, and how much detail a section is worth keeping at. A section the bound profiler does not
reach opens on the silent profiler instead - a comparison rather than a clock read, which is what
lets a per-item section exist at all without a reader of whole frames paying for it.

| | What is bound | What is timed | What it costs |
| --- | --- | --- | --- |
| `OFF` | the silent profiler | nothing | nothing: no allocation, no clock read, one virtual call per bracket |
| `COARSE` | a [recording profiler](recording/RecordingProfiler.java) | every section | a few clock reads a frame |
| `FINE` | a recording profiler | every section, plus the turns inside a loop | a clock read per step per item |

`OFF` admits nothing, which is what makes it the state a reader asks for rather than a level
anything is registered at. The other two are ordered coarsest first, and the order is the contract:
each admits itself and everything above it.

**In practice `COARSE` times everything.** [`SectionTerms.DEFAULT`](SectionTerms.java) states
`COARSE`, and no section in this library registers at anything else - so the whole of the difference
between coarse and fine is whether a [phased section](PhasedSection.java) gets its turns measured.
A capture taken at `COARSE` still holds every row, every close line, every counter, every budget
breach and every duration band; a bake that runs a loop appears with its total span and no
breakdown. `FINE` adds one line under such a row, saying what one turn cost in each of the steps
the section declared:

```
  iterations=1954  plan=8.7us/ea  trace=10.9us/ea  carve=6.3us/ea  stroke=4.9us/ea  slowest=12.445ms
```

So `COARSE` is the answer for "where did the frame go", and `FINE` for "which part of this one slow
step is slow" - asked deliberately, since a clock read per step per item distorts the thing it
measures more than the coarse reads do.

## What a section states about itself

[`SectionTerms`](SectionTerms.java) is one value rather than one registration overload per term,
because the terms combine. A section starts from `DEFAULT` and replaces what differs, so it names
only what it changes:

- **[level](ProfileLevel.java)** - the detail a capture must be keeping to time it at all.
- **[budget](budget/ProfileBudget.java)** - what one of its calls is allowed. See
  [Budgets](#budgets-numbers-into-findings).
- **[call-log threshold](CallLogThreshold.java)** - how slow one call must be before it writes a
  line as it closes. `NO_LOGGING` is nearly every section; `LOGGING_EVERY_CALL` suits a step that
  runs when something changed rather than on a clock, where the fast calls are as much of the trace
  as the slow ones.

Stating *any* threshold also marks the section's calls as events rather than per-frame cost, and
buys them a reading of the conditions they ran under - see
[cold calls](#telling-a-cold-call-from-a-slow-one). That reading is a native clock call, which is
why it follows the threshold rather than being taken for every section.

A section's terms are the ones its name was first registered with. Register it once, in a
`static final` beside the constant that holds it.

## Counters, and what earns one

A duration on its own cannot be judged: `40ms` says nothing until it is `40ms over 2100 entities`.
A [`ProfileCounter`](ProfileCounter.java) is that second number, accumulated beside the span rather
than printed into a log line the profiler never sees.

What earns a counter is **a volume of work a duration is divided by** - systems visited, markets
read, cells built, sector walks. Each one a shown row touches adds two columns to the reading it
appears in, so they are few on purpose.

Counts roll up: what a call counted is inclusive of everything opened inside it, so a bound stated
at the top of a pass is checkable there whoever underneath it did the counting. A row also keeps
what it counted *itself*, which is the only honest divisor for its own self time.

A count added with no scope open is not dropped - it lands on a reserved row of a reserved
[origin](#origins-which-game-a-row-came-from), so work from an unprofiled path is seen.

## Tags: a detail of one call

Anything that is a fact about *one call* rather than a volume its duration divides by rides on the
call's tag: the knobs a search ran under, how many of the cells handed to a bake came back with a
band, whether an update rebuilt anything at all. A tag is free text, quoted where it is written, and
kept for the one call a row holds a record of.

The rule of thumb: if you would divide the duration by it, it is a counter; if you would read it to
understand the duration, it is a tag.

## Budgets: numbers into findings

A table of durations tells a reader what happened and leaves them to work out what of it was wrong.
A [budget](budget/ProfileBudget.java) states the answer in advance, so a capture produces findings -
"your layer's build walked the sector three times" rather than a row somebody has to already know is
one walk too many.

Two bounds, combinable with `combineWith`:

- **an amount of a counter per call**, a fixed number, because it states a rule about how the work
  is done - one walk per rebuild is true of the algorithm, not of the machine.
- **a duration per call**, read as the call closes, because what a frame can afford is a knob and a
  bound fixed when a constant was first touched would ignore every later move of it. A bound of zero
  or less states no bound, so the knob carries its own off position.

Checked against one call alone: a rebuild that walks the sector once per call is correct however
many rebuilds a session ran. A breaking call is [warned about once per section per
capture](recording/CaptureLog.java) - the tenth line says nothing the first did not - and takes the
row's kept-call record whatever it took, since a fault can be over faster than the calls that
behaved. The report then names that record the row's *latest breach* rather than its *worst call*,
which is what stops a breached row's number reading as a fault in the report when it moves down.

## Telling a cold call from a slow one

The first call of a pass in a session runs on code the JIT has not compiled yet, and can cost half
again what every warm call after it does. It lands in the same row as the warm ones, inflating the
maximum, the kept call and the cost per item - and nothing in a duration says which it was.

[`CallWarmth`](snapshot/CallWarmth.java) is the answer: whether the call was its row's first, and
how far the JVM's own [compilation clock](recording/JitCompilationClock.java) moved while it ran.
Both are needed. A first-call bit alone lies after a capture is cleared mid-session, which makes
every call a first again on a JVM that is by then warm; a compilation reading alone does not say why
the clock moved. Together they read as `first jitMs=412`, `first`, or - for nearly every call -
nothing at all.

Read only for sections that state a call-log threshold, and written straight after the duration it
qualifies, on both the close line and the report's kept-call line.

## Origins: which game a row came from

Diagnostics isolate nothing - one profiler, one tree - but they do have to attribute. A root opened
under a [`ProfileOrigin`](ProfileOrigin.java) groups with the other roots of that game, because a
row averaging work done in one with work done in another describes neither, and a reader who cannot
say which game a maximum came from cannot go back and reproduce it. A root opened under none lands
on the reserved `unscoped` origin.

## Reading a capture

[`ProfileReportRequest`](report/ProfileReportRequest.java) is one value rather than four arguments,
because "the twenty worst rows of my own layer, per frame" is one question. It is immutable and
composed by narrowing.

Three readings, named by how the capture is being read rather than by naming a view type:

- **the tree** - every row under the row it ran inside, as it was measured.
- **a listing** - worst self time first, each row named by its whole path.
- **what one counter says** - ranked by the most any single call reached, with the rows that never
  counted it dropped.

Any of them narrows by `limitToNamespace` (a dotted prefix) and `limitToTopRows`. Both keep the rows
a kept row ran inside, so a finding is still readable against the frame it cost. `divideByFramesOf`
names a section that runs once a frame and divides the totals by its calls, so a figure means the
same whether the map was open for four seconds or four minutes.

[`TimingReport`](report/TimingReport.java) renders any of them as one aligned table. The counter
columns are raised from the rows actually shown, so a narrowed reading carries no column of blanks.

## What a row's columns say

Per row: the call count, average, minimum, maximum, self and total milliseconds, then the duration
bands, then two columns per counter any shown row touched - the total, and what one item cost
(`us/ea`, the row's self time over what it counted itself).

The band column is 22 characters, one per [band](snapshot/DurationBuckets.java), each twice as long
as the one before: `<1us`, then a doubling apiece up to `>=1s`. A `.` is a band no call landed in;
a digit is **how many digits that band's tally has** - `1` is 1-9 calls, `4` is 1,000-9,999. Both
axes are logarithmic, which is what fits the range in a fixed-width column. It is the one thing that
tells a section that is always this slow from one that ran fast until it stalled once, and those two
want different fixes.

Under a row goes a second line where its kept call has something the columns do not carry - its
[tag](#tags-a-detail-of-one-call), its counters, the conditions it ran under, what it broke. Under
that goes a loop line where the row's calls ran one.

## Wiring it into a mod

1. Bind a [recording profiler](recording/RecordingProfiler.java) through
   [`ActiveProfiler`](ActiveProfiler.java) at the level wanted, and rebind when that level changes -
   a rebind starts a fresh capture, which is exactly right, since a level change reinterprets what
   the numbers mean.
2. Open sections with [`Profiler#open`](Profiler.java) in a try-with-resources, or
   `openIterations` for a loop whose turns are worth timing.
3. Read it back through a [request](report/ProfileReportRequest.java) and
   [`TimingReport`](report/TimingReport.java), and `reset` to start a fresh capture.

Binding and measuring both happen on the game thread, and neither the holder nor the profilers are
synchronised.
