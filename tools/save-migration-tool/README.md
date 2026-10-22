# Save Migration Tool (SMT) scripts

Scripts that diagnose a Starsector save that stopped loading after a mod update.
They read the game's files from a terminal and print structured output,
so a caller can act on the result without reading the log itself.
They are repository tooling:
the release zip does not carry them.

- [Scripts](#scripts)
  - [Colour](#colour)
- [Layout](#layout)
- [Invoke-SaveMigrationTool.ps1](#invoke-savemigrationtoolps1)
- [Get-SaveLoadAttempt.ps1](#get-saveloadattemptps1)
  - [How vanilla logs a load](#how-vanilla-logs-a-load)
  - [Missing content](#missing-content)
  - [Object chain](#object-chain)
  - [Class owners](#class-owners)
  - [Output](#output)
- [Get-SaveModDrift.ps1](#get-savemoddriftps1)
  - [How a save stores its mod list](#how-a-save-stores-its-mod-list)
  - [Mod list output](#mod-list-output)
- [Get-ModRoster.ps1](#get-modrosterps1)
  - [How the game reads its JSON](#how-the-game-reads-its-json)
- [Tests](#tests)

## Scripts

| Script | Answers |
| --- | --- |
| [`Invoke-SaveMigrationTool.ps1`](Invoke-SaveMigrationTool.ps1) | Everything the scripts below can say about one save, as one report |
| [`Get-SaveLoadAttempt.ps1`](Get-SaveLoadAttempt.ps1) | Which saves the game's logs show loaded, why a load failed, and which mods' objects the failure sat in |
| [`Get-SaveModDrift.ps1`](Get-SaveModDrift.ps1) | Which mods, at which versions, a save was last written with |
| [`Get-ModRoster.ps1`](Get-ModRoster.ps1) | Which installed folder the game loads for each enabled mod ID, at which version |

`Invoke-SaveMigrationTool.ps1` is the one to run.
The others are its parts, each runnable alone.

Every script takes the install it reads as `-StarsectorRoot`;
none assumes where the game is installed.
Every script writes data to stdout and what it read and decided to stderr.
A blank line on stderr parts blocks:
wherever the writing script changes, as when one script runs another,
and before each load's readings.
A reading too long for one line continues on the next, indented under its first.
`Invoke-SaveMigrationTool.ps1`, `Get-SaveLoadAttempt.ps1` and `Get-SaveModDrift.ps1`
print their data as JSON with `-AsJson`;
`Get-ModRoster.ps1` returns objects for another script to consume.
`Get-SaveLoadAttempt.ps1 -AsText` writes its loads as text for a person,
since PowerShell lays out its nested objects as unreadable lists.

### Colour

On a console, the text a person reads paints each kind of value in one colour:
the runner's report, and the lines on stderr.
The words around the values keep the console's own text colour.
[`lib/console/Get-ConsolePalette.ps1`](lib/console/Get-ConsolePalette.ps1) holds the one table.
Three yellows run from saturated to pale:
the mods, the content they ship, the files on disk.
They are exact colours,
since a console scheme decides what the named ones look like.

| Value | Colour |
| --- | --- |
| Script names, dates, times, durations | Grey |
| Errors, a failed load | Red |
| Warnings, an unfinished load | Amber |
| A load that worked | Green |
| Numbers and counts | Faint green |
| Mod IDs, names, versions | Yellow |
| Content IDs and names: hulls, weapons, items | Light yellow |
| File names and paths, save folders | Palest yellow |
| Code: class names, exception types, parameters | Cyan |

Colour reaches only a console.
When a stream goes to a file or into another program, or `NO_COLOR` is set,
its text is written without colour codes,
so a caller parsing the output never reads them as data.
Objects and JSON are never painted.
The mod tables stay plain:
every column there is the same kind of value.
Warnings take PowerShell's own warning colour.

## Layout

Each script holds its parameters and its flow.
The functions it calls live one per file under [`lib/`](lib/), named after the function.
A script dot-sources [`lib/Import-SaveMigrationToolLibrary.ps1`](lib/Import-SaveMigrationToolLibrary.ps1),
which dot-sources every function under `lib/`,
so no script lists the functions it calls or what those call in turn.

| Folder | Holds |
| --- | --- |
| [`lib/`](lib/) | The library loader, and `Write-DiagnosticLine`, which every script logs through |
| [`lib/class-index/`](lib/class-index/) | Finding which mods' JARs, or the game's own, hold a class |
| [`lib/console/`](lib/console/) | The [colour](#colour) of each kind of value, stripping it where a stream is not a console, and the columns every mod list prints |
| [`lib/diagnosis/`](lib/diagnosis/) | Writing `Invoke-SaveMigrationTool.ps1`'s report as text |
| [`lib/load-log/`](lib/load-log/) | Reading save load attempts out of `starsector.log`, keeping the loads asked for, reading their causes, and writing them as text |
| [`lib/mod-info/`](lib/mod-info/) | Reading JSON as the game does, and mod versions |
| [`lib/mod-roster/`](lib/mod-roster/) | Turning a `mods\` folder into the roster the game loads |
| [`lib/save-descriptor/`](lib/save-descriptor/) | Reading the mod list out of a save's `descriptor.xml` |
| [`lib/xml/`](lib/xml/) | Loading XML safely, and following the back-pointers XStream writes |

## Invoke-SaveMigrationTool.ps1

Diagnoses one save end to end,
running the other scripts in order:

1. `Get-SaveLoadAttempt.ps1` finds the one load the report shows:
   the newest of the named save,
   or without `-SaveFolder` the newest failed load, whose save is the one diagnosed.
   It reads the JARs for that load's class owners alone.
2. `Get-SaveModDrift.ps1` lists the mods that save was last written with.

```powershell
pwsh -NoProfile -File Invoke-SaveMigrationTool.ps1 -StarsectorRoot <install>                            # newest failed load
pwsh -NoProfile -File Invoke-SaveMigrationTool.ps1 -StarsectorRoot <install> -SaveFolder <save folder>  # a named save
```

The text report names the save,
then the newest load of it as `Get-SaveLoadAttempt.ps1 -AsText` writes a load,
then how many mods the save was last written with.
The list itself gives a person nothing to act on,
so the text leaves it to `Get-SaveModDrift.ps1`.
A named save may have no load in the logs;
the report says so.
With no failed load and no `-SaveFolder`,
the report is one line saying no save is diagnosed.

With `-AsJson` the report is one object:
`SaveFolder`,
`Load` (one load as [`Get-SaveLoadAttempt.ps1`](#output) prints it, or null),
and `Mods` (as [`Get-SaveModDrift.ps1`](#mod-list-output) prints them).

## Get-SaveLoadAttempt.ps1

Lists every save load in `starsector-core\starsector.log` and its rotated siblings, newest first.
An older load is likelier to describe a save or a mod set that has changed since.

```powershell
pwsh -NoProfile -File Get-SaveLoadAttempt.ps1 -StarsectorRoot <install> -AsJson                  # every load, as JSON
pwsh -NoProfile -File Get-SaveLoadAttempt.ps1 -LogPath <log>                                     # another log
pwsh -NoProfile -File Get-SaveLoadAttempt.ps1 -StarsectorRoot <install> -Outcome Failed -Newest  # newest failure
pwsh -NoProfile -File Get-SaveLoadAttempt.ps1 -StarsectorRoot <install> -AsText                  # for a person
```

`-AsText` writes each load under a line naming its save and how it ended:
the cause and what it was read as,
each retry vanilla made, in order, with where in the log it starts,
and each class in the object chain that a mod owns, with its depth and owners.
Each value of a cause or a retry takes a line of its own, nested under what it belongs to.
A blank line opens each load, the first too, so the text stands apart from the stderr lines above it.
A load tried once lists no retries,
and the classes only the game owns, or no JAR holds, are left out.
`-AsJson` keeps the whole chain.

Three filters narrow the list:
`-SaveFolder` keeps one save's loads,
`-Outcome` keeps the loads that ended `Loaded`, `Failed` or `Unfinished`,
and `-Newest` keeps only the newest load the others leave.
A mistyped `-Outcome` fails before any log is read.
They apply before any failure's trace is read and before [class owners](#class-owners) are resolved,
so a filtered run reads only the traces and JARs its loads need,
and no JAR when they name no object chain.
Reading every mod's JARs takes seconds.

### How vanilla logs a load

All load lines come from `CampaignGameManager`.
An attempt opens with `Loading <save folder>...`
and ends with `Finished loading` or `Error loading`.
After `Error loading`, an ERROR line and the stack trace follow.
The last throwable header in that trace (the deepest `Caused by:`) is the root cause.
The loader's own stack frames name the same class,
so only timestamped lines count as loader lines.

A failed attempt makes vanilla retry from `campaign.xml.bak`,
and a failed retry tries `campaign.xml.inprogress`.
That file rarely exists,
so a broken save usually leaves three failures,
the last a `FileNotFoundException`.
The script folds these retries back into one load:
an attempt is a retry
when the one before it failed on the same save and the load has used fewer than three sources.
Loading the same broken save twice so still reads as two loads.

Rotated logs (`starsector.log.1`, `.2`, ...) are read first,
highest number first,
as one stream.
An attempt can straddle a rotation.
Rotation renumbers the files,
so a reported log position holds only until the game rotates again.

Each file is read whole,
and the lines naming the loader are found as one array operation.
A loop over every line would take seconds on a log of a million lines.
A failure's trace is set aside as raw lines,
and read for its cause and chain only once the filters have kept its load.

### Missing content

A failed load usually names one ID that the save holds and no installed mod defines.
Each spec store throws its own wording,
and the script reads each into a kind and an ID:

| Cause | Kind |
| --- | --- |
| `Weapon spec [<id>] not found!` | `Weapon` |
| `Ship hull spec [<id>] not found!` | `Hull` |
| `Ship hull variant [<id>] not found!` | `Variant` |
| `Fighter wing spec [<id>] not found` | `FighterWing` |
| `Hullmod with id [<id>] not found` | `Hullmod` |
| `Spec of class [<class>] with id [<id>] not found` | `Spec`, with the class kept as `SpecClass` |
| `CannotResolveClassException: <class>` | `Class`, the class name as the ID |

`Spec` covers every spec type without a store of its own:
special items,
market conditions,
industries and more.
Its class name is often obfuscated,
so it says which store missed,
not always which kind.

Vanilla builds a d-hull for every hull at startup,
under the hull ID plus `_default_D`.
A missing d-hull so means a missing base hull,
and the base hull is what a mod defines.
`Id` holds the base hull;
`ReportedId` keeps the ID as the message names it.

A cause matching none of these,
a crash in a mod's script for one,
gives no missing content.

### Object chain

When XStream fails on an object,
it prints a block of debugging information beneath the ERROR line.
`class` names the object that failed;
`class[1]`, `class[2]`, ... name the objects that contain it,
counting outwards to `CampaignEngine`.
The script keeps these as the object chain,
`class` at depth 0.
The ERROR line and the exception header print the same block,
so only the first block of an attempt is read.

The nearest class a mod owns is the likeliest holder of the failed object:
a black market's cargo,
a fleet,
a script.
Classes further out mostly show how XStream reached it.
XStream writes an object where it first meets it,
so a market can sit under a mod's script that happens to hold a reference to it.

### Class owners

The classes the chains name are looked up in the install's JARs:
the JARs each enabled mod's `mod_info.json` declares,
and the game's own classpath from `vmparams`.
The install is the folder above the log's,
since the log sits in `starsector-core`.

- The game's own classes have the owner `vanilla`.
- A mod can bundle copies of another mod's classes,
  so `Owners` lists every mod whose JARs hold the class.
- A mod can bundle copies of the game's classes too,
  and those never load.
  The game loads every mod JAR through one class loader whose parent is its own,
  and that loader asks its parent first.
  So a class the game's own JARs hold keeps `vanilla` as its only owner.
- A class no JAR holds has no owners:
  one of the Java runtime's,
  such as `java.util.ArrayList`,
  or one no installed mod ships any more.
- A declared JAR that is not on disk,
  or that no zip reader opens,
  is skipped,
  and stderr names it with the reason.
- A mod whose `mod_info.json` gives no ID declares no JARs here,
  so its classes resolve to no owner;
  stderr counts such mods.

Owners are resolved only when a failure names a chain,
and only for the classes the chains name.
Each class is looked up in a JAR by its entry path, `a/b/C.class`,
so a JAR of thousands of entries costs one lookup per class.
With no `mods` folder in the install,
or `vmparams` missing or naming no classpath,
the script warns, saying which, and leaves those owners empty.

### Output

One object per load, newest first:

| Field | Meaning |
| --- | --- |
| `SaveFolder` | The save's folder name under `saves\` |
| `SavePath` | The full save path, with `..` resolved against the log's folder (the game's working directory) |
| `Outcome` | `Loaded`, `Failed`, or `Unfinished` when the log ends first: a crash, or a load still running |
| `CauseType`, `Cause` | The root throwable of the first attempt, read from `campaign.xml`, the newest file. Empty when that attempt loaded |
| `Attempts` | Each attempt in order, with its `Source` (`campaign`, `campaign.bak`, `campaign.inprogress`), `SavePath`, `Outcome`, `CauseType`, `Cause`, the `LogFile` and `LogLine` it starts on, its `Missing` and its `ObjectChain` |
| `Missing` | The [missing content](#missing-content) the first attempt's cause names: `Kind`, `Id`, `ReportedId` and `SpecClass`. Empty when the cause names none |
| `ObjectChain` | The first attempt's [object chain](#object-chain), innermost first: each class's `Depth`, `Class` and [`Owners`](#class-owners). Empty when the log names none |

Stderr gains a block for each listed load with a cause:
what the cause was read as,
and, when it names one,
the chain's length and, on a second line, its nearest mod class.
When owners are resolved,
stderr names the install they come from,
each JAR skipped,
how many JARs were read and how long that took,
how many unparsed mods left their JARs unknown,
and how many classes found owners,
more than one mod,
or dropped mod copies of a core class.

`campaign` stands for `campaign.xml`,
or `campaign.zip` for a compressed save.
The log does not say which.

## Get-SaveModDrift.ps1

Lists the mods a save was last written with,
from its `descriptor.xml`.
That small file sits beside `campaign.xml`,
and the game's load list reads only it.
Its mod list is the one the load list's hover tooltip compares against the installed mods.

```powershell
pwsh -NoProfile -File Get-SaveModDrift.ps1 -StarsectorRoot <install> -SaveFolder <save folder> -AsJson
```

`-SaveFolder` is the folder name under `saves\`,
as `Get-SaveLoadAttempt.ps1` reports it in `SaveFolder`.

### How a save stores its mod list

The game writes the descriptor through XStream,
which writes each object in full once, tagged `z="N"`.
Every later use of that object is an empty element with `ref="N"`.

The descriptor holds two mod lists:

- `allModsEverEnabled` holds every mod this save has been written with, each spec in full;
- `enabledMods` holds the mods enabled at the last save.
  It comes second,
  so its entries are usually back-pointers into the first list.

The script lists `enabledMods`,
following each entry, its spec and the spec's version wherever they point.
A back-pointer that no `z` answers fails the run, naming it:
the file is damaged, and reading the entry as empty would hide that.

A mod enabled once and dropped since stays in `allModsEverEnabled`,
and the save can still hold its content.
Stderr names each such mod;
stdout does not list it.

The descriptor is loaded with DTDs refused.
A save can come from another player,
and a DTD can expand a small file past memory.
XStream never writes one.

### Mod list output

One object per enabled mod, in the descriptor's order:

| Field | Meaning |
| --- | --- |
| `Id` | The mod's ID |
| `Version` | The version the save was last written with, in the form `Get-ModRoster.ps1` prints for the same `mod_info.json` |
| `Name` | The mod's name, as the save wrote it, leading spaces included |

On a console the mods print as a table of these three columns,
the same table `Get-ModRoster.ps1` prints for the installed mods.

A `mod_info.json` that gives its version as a string,
such as `"1.14.1aa"`,
leaves that string in the save,
and `Version` is that string.
One that gives `{ major, minor, patch }` leaves only the parts,
and `Version` joins them with dots,
skipping an empty one.
So an unchanged mod reads the same on both sides.

Stderr names the descriptor read,
when and under which game version the save was written,
how many mods it lists,
and the mods enabled once and dropped since.
A descriptor with no `enabledMods` list warns and lists no mods.

## Get-ModRoster.ps1

Resolves the installed side of a save's mod list:
for each mod ID the game will load,
the one folder that wins.

```powershell
pwsh -NoProfile -File Get-ModRoster.ps1 -StarsectorRoot <install>                    # enabled mods
pwsh -NoProfile -File Get-ModRoster.ps1 -StarsectorRoot <install> -IncludeDisabled   # every installed mod
```

- `mod_info.json`'s `id` is the identity, not the folder name: one mod can sit in several folders.
- When it does, the highest `version` wins, compared number by number (`1.10.0` above `1.9.0`),
  however many digits a part holds.
  Copies whose numbers tie are ordered by the version as written, so `1.5b` beats `1.5a`,
  and copies at the very same version by folder name, so the same install always picks the same one.
  `SupersededCount` says how many copies lost, and stderr names each winner and loser, and each tie.
- `enabled_mods.json` decides what loads.
  A missing or unreadable one warns and lists every mod.
- A folder with no `mod_info.json` is not a mod; stderr names each one skipped.
- A `mod_info.json` nothing can parse, or one naming no `id`,
  stays in the roster as `unparsed`, under its folder name, with a warning.
  Dropping it would hide a real mod with nothing to say so.

Each entry carries `Id`, `Version`, `Name`, `Folder`, `RelativePath` (under the install root),
`Jars` (the JARs `mod_info.json` declares, as full paths), `Status` (`parsed` or `unparsed`) and `SupersededCount`.
On a console the entries print as a table of `Id`, `Version` and `Name`,
the same table `Get-SaveModDrift.ps1` prints for a save's mods;
every field stays on the object.

### How the game reads its JSON

Every file is read as the game reads its data files, neither stricter nor looser.
`LoadingUtils` strips `#` comments, then org.json parses what is left.

- A `#` outside a string starts a comment that runs to the end of its line.
  The game tracks strings by flipping on every `"`, escaped or not, and forgets an open string at each line break;
  the reader does the same, so a `#` after an odd number of `\"` is a comment here too.
  Only a line holding a `#` is read character by character, so a file with none costs a single pass.
- `//` and `/* */` are not comments to the game, and a file holding one outside a string fails to parse.
  ConvertFrom-Json would skip them, so the reader looks for them itself.
- A file opening with a byte order mark fails, as it does in the game.
- org.json accepts trailing commas, single-quoted strings and unquoted values.
  ConvertFrom-Json accepts the first two as they are.
  An unquoted value is quoted only when the file fails without the repair,
  since a repair can corrupt a file that needed none.

## Tests

Each `lib/` function has a unit suite of the same name under
[`Tests/tools/save-migration-tool/lib/`](../../Tests/tools/save-migration-tool/lib/),
in the same subfolder, with the functions it calls mocked.

Each script has an integration suite, which runs it with its `lib/` functions unmocked:
[`Get-SaveLoadAttempt.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-SaveLoadAttempt.IntegrationTests.Tests.ps1)
runs the script against hand-written install roots under
[`fixtures/`](../../Tests/tools/save-migration-tool/fixtures/),
whose logs are shaped as 0.98a writes them.
A case that resolves class owners copies its fixture to a temporary folder and writes the JARs there,
so the repository holds no binary fixture.
[`Get-SaveModDrift.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-SaveModDrift.IntegrationTests.Tests.ps1)
runs the script against a hand-written descriptor shaped as 0.98a writes it,
and writes damaged descriptors under a temporary folder.
[`Invoke-SaveMigrationTool.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Invoke-SaveMigrationTool.IntegrationTests.Tests.ps1)
copies a log fixture to a temporary folder and gives its failed save that descriptor.
[`Get-ModRoster.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-ModRoster.IntegrationTests.Tests.ps1)
builds a small install per case.

[`helpers/`](../../Tests/tools/save-migration-tool/helpers/) holds what the suites share:
running a script with its stderr and warnings captured, blank lines kept,
and writing a test file, mod or JAR under the suite's temporary folder.

Every suite runs under `Set-StrictMode -Version Latest`, as the scripts do,
so a read the scripts would fail on fails in the suites too.

No test reads a real install, log or save.

The tests live under the repository's top-level `Tests\`, mirroring this folder,
because the shared PowerShell CI only discovers `*.Tests.ps1` files there.
The PowerShell CI workflow runs them on every pull request.

```powershell
Invoke-Pester -Path Tests\tools\save-migration-tool
```
