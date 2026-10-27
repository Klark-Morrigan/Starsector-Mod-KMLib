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
  - [How versions compare](#how-versions-compare)
  - [Drift output](#drift-output)
- [Get-ContentCatalogue.ps1](#get-contentcatalogueps1)
  - [Where the game defines each kind](#where-the-game-defines-each-kind)
  - [How the game combines copies](#how-the-game-combines-copies)
  - [How the game reads a spreadsheet](#how-the-game-reads-a-spreadsheet)
  - [Catalogue output](#catalogue-output)
- [Get-ModRoster.ps1](#get-modrosterps1)
  - [How the game reads its JSON](#how-the-game-reads-its-json)
- [Tests](#tests)

## Scripts

| Script | Answers |
| --- | --- |
| [`Invoke-SaveMigrationTool.ps1`](Invoke-SaveMigrationTool.ps1) | Everything the scripts below can say about one save, as one report |
| [`Get-SaveLoadAttempt.ps1`](Get-SaveLoadAttempt.ps1) | Which saves the game's logs show loaded, why a load failed, and which mods' objects the failure sat in |
| [`Get-SaveModDrift.ps1`](Get-SaveModDrift.ps1) | How the mods a save was last written with differ from the installed ones: changed, missing, added |
| [`Get-ContentCatalogue.ps1`](Get-ContentCatalogue.ps1) | Which content IDs the install defines, of every kind the game keeps by ID, and which mods define each |
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
A library function that reads or decides writes under the name of the script that called it.
`Invoke-SaveMigrationTool.ps1`, `Get-SaveLoadAttempt.ps1`, `Get-SaveModDrift.ps1` and `Get-ContentCatalogue.ps1`
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
| Errors, a failed load, a major version change, a missing mod | Red |
| Warnings, an unfinished load, a minor version change, an added mod | Amber |
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
A script one script runs sees its caller's functions already,
so the loader leaves a marker and loads nothing for every script that caller runs.
Loading the library takes about half a second.

| Folder | Holds |
| --- | --- |
| [`formats/`](formats/) | Console table views for rows with too many columns for PowerShell's default table |
| [`lib/`](lib/) | The library loader, `Write-DiagnosticLine`, which every script logs through, and the name of the process-wide slot it remembers the last writer in |
| [`lib/class-index/`](lib/class-index/) | Finding which mods' JARs, or the game's own, hold a class |
| [`lib/console/`](lib/console/) | The [colour](#colour) of each kind of value, stripping it where a stream is not a console, and the columns an object prints as a table, every mod list's included |
| [`lib/content-catalogue/`](lib/content-catalogue/) | The kinds of content a save names by ID, where the game defines each, and reading the IDs the install defines with the mods behind them |
| [`lib/diagnosis/`](lib/diagnosis/) | Writing `Invoke-SaveMigrationTool.ps1`'s report as text |
| [`lib/install/`](lib/install/) | The names of the install's folders and files the scripts read |
| [`lib/load-log/`](lib/load-log/) | Reading save load attempts out of `starsector.log`, keeping the loads asked for, reading their causes, and writing them as text |
| [`lib/mod-drift/`](lib/mod-drift/) | Comparing a save's mods with the installed ones, ranking each version change, and writing the drift as text |
| [`lib/mod-info/`](lib/mod-info/) | Reading a data file's text and JSON as the game does, and mod versions as text and as the parts the game compares |
| [`lib/mod-roster/`](lib/mod-roster/) | Reading a `mods\` folder into the roster the game loads |
| [`lib/save-descriptor/`](lib/save-descriptor/) | Finding a save's `descriptor.xml` and reading its mod lists |
| [`lib/xml/`](lib/xml/) | Loading XML safely, and following the back-pointers XStream writes |

## Invoke-SaveMigrationTool.ps1

Diagnoses one save end to end, in order:

1. It reads the installed mods once, as `Get-ModRoster.ps1` does.
   Both parts below need them, and each read takes seconds on a large install.
2. `Get-SaveLoadAttempt.ps1` finds the one load the report shows:
   the newest of the named save,
   or without `-SaveFolder` the newest failed load, whose save is the one diagnosed.
   It reads the JARs for that load's class owners alone.
3. It compares the mods that save was last written with against the installed ones,
   as `Get-SaveModDrift.ps1` does.
   A save whose `descriptor.xml` is gone, deleted or renamed since its load,
   still gets its load reported.

```powershell
pwsh -NoProfile -File Invoke-SaveMigrationTool.ps1 -StarsectorRoot <install>                            # newest failed load
pwsh -NoProfile -File Invoke-SaveMigrationTool.ps1 -StarsectorRoot <install> -SaveFolder <save folder>  # a named save
```

The text report names the save,
then the newest load of it as `Get-SaveLoadAttempt.ps1 -AsText` writes a load,
then the mod drift.
The drift opens with a line counting the mods changed, missing, added and unchanged,
then gives one line to each mod that differs:
changed first, with both versions and the [level](#how-versions-compare) of the change,
then missing, then added.
An unchanged mod gives a person nothing to act on,
so the text only counts it.
When nothing could be compared,
because the descriptor is gone or lists no enabled mods,
the drift is one line saying so and why,
since counting nothing would read as a save that matches the install.
A named save may have no load in the logs;
the report says so.
With no failed load and no `-SaveFolder`,
the report is one line saying no save is diagnosed.

With `-AsJson` the report is one object:
`SaveFolder`,
`Load` (one load as [`Get-SaveLoadAttempt.ps1`](#output) prints it, or null),
`ModDrift` (as [`Get-SaveModDrift.ps1`](#drift-output) prints it, or null when nothing was compared),
and `ModDriftProblem` (why nothing was compared, or null).

Stderr holds `Get-SaveLoadAttempt.ps1`'s lines under its own name,
what the roster read and the comparison found under the runner's,
and a last line saying whether the report was written as text or JSON, and how many drift rows it holds.

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

`-Roster` hands over the installed mods from a caller that has read them already,
as `Invoke-SaveMigrationTool.ps1` does,
so the install's mods are read once.
Without it, they are read only when a kept load names an object chain.

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
the script warns and leaves the mod owners empty;
the game's own classes still resolve to `vanilla`.
With `vmparams` missing or naming no classpath,
it warns and leaves the `vanilla` owners empty.

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
what reading the installed mods found, unless `-Roster` handed them over,
each JAR skipped,
how many of the mods' JARs and of the core classpath's entries were read, and how long that took,
how many unparsed mods left their JARs unknown,
and how many classes found owners,
more than one mod,
or dropped mod copies of a core class.

`campaign` stands for `campaign.xml`,
or `campaign.zip` for a compressed save.
The log does not say which.

## Get-SaveModDrift.ps1

Compares the mods a save was last written with against the installed ones.
The save's side comes from its `descriptor.xml`.
That small file sits beside `campaign.xml`,
and the game's load list reads only it.
The installed side is the roster [`Get-ModRoster.ps1`](#get-modrosterps1) gives for the install's enabled mods.
The load list's hover tooltip compares the same two lists.

```powershell
pwsh -NoProfile -File Get-SaveModDrift.ps1 -StarsectorRoot <install> -SaveFolder <save folder> -AsJson
```

`-SaveFolder` is the folder name under `saves\`,
as `Get-SaveLoadAttempt.ps1` reports it in `SaveFolder`.
`-Roster` hands over the installed mods from a caller that has read them already.

A mod is:

- **unchanged** when both sides hold it at the same version text;
- **changed** when the version text differs,
  with the [level](#how-versions-compare) the game ranks the change at;
- **missing** when the save was last written with it and the install does not load it;
- **added** when the install loads it and the save was not last written with it.

Mod IDs match case-sensitively, as the game matches them.
A change counts by the text a person sees,
so a rebuild the game ranks as the same version still shows.

### How a save stores its mod list

The game writes the descriptor through XStream,
which writes each object in full once, tagged `z="N"`.
Every later use of that object is an empty element with `ref="N"`.

The descriptor holds two mod lists:

- `allModsEverEnabled` holds every mod this save has been written with, each spec in full;
- `enabledMods` holds the mods enabled at the last save.
  It comes second,
  so its entries are usually back-pointers into the first list.

The script compares `enabledMods`,
following each entry, its spec and the spec's version wherever they point.
A back-pointer that no `z` answers fails the run, naming it:
the file is damaged, and reading the entry as empty would hide that.

A mod enabled once and dropped since stays in `allModsEverEnabled`,
and the save can still hold its content.
Stderr names each such mod;
stdout does not list it.
The game's load list does not compare it either.

The descriptor is loaded with DTDs refused.
A save can come from another player,
and a DTD can expand a small file past memory.
XStream never writes one.

A `mod_info.json` that gives its version as a string,
such as `"1.14.1aa"`,
leaves that string in the save.
One that gives `{ major, minor, patch }` leaves only the parts,
and the script joins them with dots,
skipping an empty one,
as `Get-ModRoster.ps1` does.
So an unchanged mod reads the same on both sides.

### How versions compare

The level of a change follows the game's own rule,
the one behind the load list's version warnings (`VersionInfo.getCompatLevel`).
The game keeps each version as three parts, major, minor and patch,
and the first of major and minor that differs names the level:

| Level | Meaning | The load list warns |
| --- | --- | --- |
| `Major` | The major parts differ | In red: unlikely to load or work |
| `Minor` | The minor parts differ | In yellow: may fail to load or work |
| `Patch` | Any other change of the version text | In yellow when the patch parts differ, else nothing |

The game sees no change when the text differs and the parts do not,
and cannot rank one when either side has no major part.
The text still changed, so the mod's content may have too,
and both count as a `Patch`.

The parts compare as exact text, not as numbers, so `02` and `2` differ.
The game marks a minor part from a version holding `-RC` as a release candidate;
when both sides are marked, the minor parts are not compared.

The game splits a version given as text the way `VersionInfo.setFromString` does:

- on each dot, and on each letter `a`, so `1.14.1aa` reads as `1`, `14`, `1`.
  This is why the game reads `1.0.2` and `1.0.2a` as the same version;
- two parts are major and minor;
  three are major, minor and patch;
  four join the first two as the major, as in `0.1.98.7`: major `0.1`, minor `98`, patch `7`;
- a first part of `0` joins the second as the major for two or three parts too,
  so `0.8.0` to `0.8.1` is a minor change;
- one part, or five or more, gives no parts at all, so the game cannot rank the change.

A version given as `{ major, minor, patch }` keeps its parts as written.
The save keeps the parts the game read when it wrote the save,
so the save's side takes them from the descriptor, never from its version text.
The installed side reads them from `mod_info.json` as the game does.

### Drift output

One row per mod on either side:
the save's mods in the descriptor's order,
then the added mods in the roster's order.

| Field | Meaning |
| --- | --- |
| `Id` | The mod's ID |
| `Name` | The mod's name, as the save wrote it, leading spaces included, else as installed |
| `SavedVersion` | The version the save was last written with, in the form `Get-ModRoster.ps1` prints for the same `mod_info.json`. Null for an added mod |
| `InstalledVersion` | The installed version, as `Get-ModRoster.ps1` prints it. Null for a missing mod |
| `Change` | `Unchanged`, `Changed`, `Missing` or `Added` |
| `Level` | For a changed mod, how the change [ranks](#how-versions-compare): `Major`, `Minor` or `Patch`. Null otherwise |

On a console the rows print as a table of these columns,
through the view in [`formats/ModDriftRow.format.ps1xml`](formats/ModDriftRow.format.ps1xml).
PowerShell would print an object of six columns as a list.

Stderr names what reading the installed mods found, unless `-Roster` handed them over,
the descriptor read,
when and under which game version the save was written,
how many mods it lists,
the mods enabled once and dropped since,
and how many mods changed, went missing, were added and stayed unchanged.
A descriptor with no `enabledMods` list warns and reports no drift,
since it says nothing of the save's mods.
A save folder with no descriptor fails the run, naming the path.
An empty list reports every installed mod as added.
A `mod_info.json` the roster keeps as `unparsed` takes its folder name as its ID,
so its mod reads as missing on one side and added on the other.

## Get-ContentCatalogue.ps1

Lists the content IDs the install defines,
each with the mods that define it.
An ID a save names and the catalogue lacks is content no installed mod provides any more.
It reads the game's own data under `starsector-core\data` and every enabled mod's `data` folder,
as the game reads them at startup,
and adds the IDs the game builds from them.

```powershell
pwsh -NoProfile -File Get-ContentCatalogue.ps1 -StarsectorRoot <install> -Id 'ssp_*'           # IDs by pattern
pwsh -NoProfile -File Get-ContentCatalogue.ps1 -StarsectorRoot <install> -Kind FighterWing -AsJson
pwsh -NoProfile -File Get-ContentCatalogue.ps1 -StarsectorRoot <install> -GroupByOwner -OutFile <file>
```

A full list runs to tens of thousands of lines, more than a console's scrollback holds.
`-OutFile` writes the JSON to a file instead, creating its folder,
and `-GroupByOwner` gives one entry per mod,
so the file folds and searches by mod in an editor.

`-Kind` reads only the kinds it names,
so a run for one kind skips the other kinds' files,
except those of the kinds it is [built from](#where-the-game-defines-each-kind).
A mistyped kind fails before any file is read.
`-Id` keeps the IDs matching any of its wildcard patterns, ignoring case;
every file of the asked kinds is still read,
since any file could define a matching ID.
`-Roster` hands over the installed mods from a caller that has read them already.

A full run on an install of 400 mods reads about 47,000 content files and 2,300 spreadsheets,
and takes about a minute.
Parsing each content file in full would take minutes more,
so an ID is taken by pattern when its key appears once with a plain string value,
and read as absent when the key never appears.
The pattern takes the key quoted or bare, as the game's own `.faction` files write `id:"hegemony"`.
Any other file gets the full parse the game makes.

### Where the game defines each kind

The catalogue covers every kind of content the game keeps by ID from its data files, as of 0.98a.
It reads each from one or more of four sources:

- **content files**, one spec each, found in a folder by how their names end, or listed by a spreadsheet;
- **spreadsheets**, one row per spec;
- **JSON files keyed by ID**, whose top-level keys are the IDs;
- **IDs the game builds in its own code**.

| Kind | Read from |
| --- | --- |
| `Hull` | `data/hulls/*.ship` by `hullId`, each with its `data/hulls/ship_data.csv` row; and `data/hulls/skins/*.skin` by `skinHullId` |
| `Weapon` | `data/weapons/*.wpn` and `data/shipsystems/wpn/*.wpn` by `id`, each with its `data/weapons/weapon_data.csv` row |
| `FighterWing` | `data/hulls/wing_data.csv` |
| `Hullmod` | `data/hullmods/hull_mods.csv` |
| `Variant` | `data/variants/*.variant` by `variantId`, and the 51 variants the game builds in its own code, such as `omen1` |
| `ShipSystem` | `data/shipsystems/*.system` by `id`, each with its `data/shipsystems/ship_systems.csv` row |
| `Projectile` | `data/weapons/proj/*.proj` and `data/shipsystems/proj/*.proj` by `id` |
| `MarketCondition` | `data/campaign/market_conditions.csv` |
| `Industry` | `data/campaign/industries.csv` |
| `SpecialItem` | `data/campaign/special_items.csv` |
| `Commodity` | `data/campaign/commodities.csv` |
| `Ability` | `data/campaign/abilities.csv` |
| `Submarket` | `data/campaign/submarkets.csv` |
| `Faction` | Each `.faction` file `data/world/factions/factions.csv` lists in its `faction` column, by `id` |
| `Skill` | `data/characters/skills/*.skill` by `id`, each listed in `skill_data.csv` or named as an aptitude's `effect_skill_id` in `aptitude_data.csv` |
| `Aptitude` | `data/characters/skills/aptitude_data.csv` |
| `Personality` | `data/characters/personalities.csv` |
| `PersonMission` | `data/campaign/person_missions.csv`, keyed by `mission id` |
| `BarEvent` | `data/campaign/bar_events.csv`, keyed by `bar event id` |
| `CustomEntity` | `data/config/custom_entities.json` |
| `Planet` | `data/config/planets.json`, object entries only |
| `Terrain` | `data/campaign/terrain.json`, object entries only |
| `BattleObjective` | `data/config/battle_objectives.json` |
| `EngineStyle` | `data/config/engine_styles.json`, object entries only |
| `HullStyle` | `data/config/hull_styles.json`, object entries only |
| `CampaignEvent` | `data/campaign/events.json` |
| `Ping` | `data/campaign/pings.json`, object entries only |
| `IntelTag` | `data/config/tag_data.json` |
| `ContactTag` | `data/config/contact_tag_data.json` |
| `Sound` | `data/config/sounds.json`, every key but `music` |
| `MusicSet` | `data/config/sounds.json`, the keys under `music` |
| `Description` | `data/strings/descriptions.csv`, keyed by `id` and `type` together, as `<id>_<type>` |
| `StarGenData`, `AgeGenData`, `ObjectiveGenData`, `LocationGenData`, `ConditionGenData`, `SalvageEntityGenData` | The matching `data/campaign/procgen/*_gen_data.csv` |
| `NameGenData` | `data/campaign/procgen/name_gen_data.csv`, keyed by `name` and `tags` together, as `<name>_\|_<tags>` |
| `PlanetGenData`, `CategoryGenData`, `TerrainGenData` | The `PLANET`, `CATEGORY` and `TERRAIN` rows of `data/campaign/procgen/planet_gen_data.csv`; a category by its `category` column |
| `DropGroup` | `data/campaign/procgen/drop_groups.csv`, keyed by `commodity` and `group` together, as `<commodity>_\|_<group>` |
| `Mission` | `data/missions/mission_list.csv`, keyed by `mission` |
| `Rule` | `data/campaign/rules.csv` |

A spreadsheet keys its rows by its `id` column unless the table says otherwise.
A key joined from several columns that opens with `#` is a comment, as it is for a mission or a rule.

Kinds read from content files and spreadsheets together need both:
the game drops a hull, weapon, ship system or skill whose file has no row,
and ignores a row whose file is missing.
A skin copies its base hull's row, so it needs none.

The game lists a folder's own files.
For skins and variants it lists one level of subfolders too,
skipping hidden ones and those whose name opens with a dot.
It matches a file by how its name ends,
case-sensitively and without the dot,
so `a.variant.bak` is not read.

The game also builds content at startup that no file names,
from every hull and skin the files define:

- a d-hull under the hull ID plus `_default_D`, unless the ID already ends so;
- an empty variant under the hull ID plus `_Hull`.

A d-hull gets neither.
The catalogue lists these IDs too, each owned as its hull is,
so a save's d-hulls and empty variants read as defined while their hull is.

Two of the game's rules are not followed:

- A custom entity whose `baseId` names no other entity is dropped by the game.
  The catalogue keeps it, so its ID never reads as missing when it is not.
- A total conversion gets none of the game's built-in variants,
  and drops core skins and variants of hulls it lacks.
  The catalogue reads every install as one that is not a total conversion.

[`lib/content-catalogue/Get-ContentDefinitions.ps1`](lib/content-catalogue/Get-ContentDefinitions.ps1)
holds this table and these rules.

### How the game combines copies

The game searches the enabled mods first, then its own data.
It orders the mods by each `mod_info.json`'s `sortString`, or by its name when it gives none,
comparing the text code unit by code unit,
so `Zeta` sorts before `alpha`.
A mod the roster keeps as unparsed sorts by its folder name.
Its content still counts:
calling it undefined would mark it for removal.

The same file path can sit in several sources.

- **A content file** is merged key by key, as the game merges any data JSON.
  The game's own copy is the base,
  and each mod's copy is laid over it in search order.
  So the last copy that sets the ID decides it,
  and a mod's copy that sets no ID only patches the others:
  a mod can rename a hull without restating its `hullId`.
- **A JSON file keyed by ID** is merged the same way,
  so its IDs are the keys any copy holds.
  Where only object entries count, the copy the merge takes last decides whether an entry is one.
- **A spreadsheet's rows** are merged by their key:
  each source's copy adds its rows,
  and a key already read keeps its first row.
  That row's filter and ID column decide whether the key defines an ID.
- **A mod that lists a path under `replace`** in its `mod_info.json` stands alone.
  The game reads only that mod's copy, the first such mod's in search order,
  and drops every other copy.
  A content file's path may be written with either slash;
  a spreadsheet's must be written with forward slashes, as the game compares it as written.

### How the game reads a spreadsheet

The game reads its spreadsheets with a reader of its own,
neither stricter nor looser:

- The first line is the header.
  It splits on every comma, quoted or not,
  and each name is trimmed and loses one enclosing quote.
- A name the header repeats takes the value of its last column.
  `Import-Csv` refuses such a header,
  and LLI's `wing_data.csv` repeats a column name eleven times.
- A row that opens with `#` is a comment,
  and an empty line is no row.
- A double quote turns quoting on or off;
  a comma or line break inside quotes is text.
  Two double quotes in a row are one literal quote, inside quotes or out.
- Values are not trimmed.
  Carriage returns are dropped.
- Before it reads `rules.csv`, the game turns curly quotes straight,
  a run of curly double quotes into one double quote,
  which can move where a value ends.
- A quote left open, or a row too short to reach a column the game reads,
  stops the game at startup.
  The catalogue skips that copy instead and names it on stderr.

### Catalogue output

One row per defined ID,
by kind in the order of the table above,
then by ID, code unit by code unit:

| Field | Meaning |
| --- | --- |
| `Kind` | One of the kinds in the [table above](#where-the-game-defines-each-kind) |
| `Id` | The ID, as the game looks it up, case-sensitively |
| `Owners` | Each source whose file or row defines the ID, in search order: mod IDs, then `vanilla` for the game's own |

Owners name every source that would keep the ID defined without the others,
so a mod that restates the game's own file is listed beside `vanilla`:

- A content file's owners are every copy that sets the ID the merge ends with.
- A spreadsheet's owners are every source whose own row defines the ID,
  though the game keeps only the first.
- A JSON file keyed by ID is owned by every source whose copy holds the key.
- A kind read from files and rows together lists the owners of both,
  so a mod that adds a row for a vanilla hull is listed beside `vanilla`.

With `-GroupByOwner`, one entry per owner instead,
in search order, leaving out owners that define nothing:

| Field | Meaning |
| --- | --- |
| `Owner` | The mod ID, or `vanilla` |
| `Name` | The mod's name, or `Starsector` for the game's own data |
| `Version` | The installed version; null for the game's own data |
| `IdCount` | How many IDs the entry holds |
| `Ids` | The IDs by kind, kinds and IDs in the order of the rows above. An ID several owners define sits under each |

On a console an entry prints as a table row of its owner, version, name and ID count.

Stderr names what reading the installed mods found, unless `-Roster` handed them over,
the kinds catalogued and from how many mods,
each copy skipped with the reason it does not parse,
each spreadsheet a mod replaces,
how many content files and spreadsheets were read and how long that took,
how many IDs each kind holds and how many of them the game builds,
how many of them the filters kept, and under how many owners when grouped,
and the file written, when `-OutFile` names one.

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

Each entry carries `Id`, `Version`,
`VersionInfo` (the `Major`, `Minor`, `Patch` and `MinorIsRC` the game [compares versions by](#how-versions-compare)),
`Name`, `Folder`, `RelativePath` (under the install root),
`Jars` (the JARs `mod_info.json` declares, as full paths),
`SortString` and `ReplacedPaths` (what decides [how the game reads the mod's data](#how-the-game-combines-copies),
as `mod_info.json` writes them, less a replaced `settings.json`, which the game ignores),
`Status` (`parsed` or `unparsed`) and `SupersededCount`.
On a console the entries print as a table of `Id`, `Version` and `Name`;
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
- org.json accepts trailing commas and single-quoted strings, and ConvertFrom-Json accepts them as they are.
- org.json takes more that ConvertFrom-Json refuses, and the reader repairs it,
  only when the file fails without the repair, since a repair can corrupt a file that needed none:
  - an unquoted value, kept as text unless it is `true`, `false` or `null` in any case, or a number Java parses,
    so `0.5f`, `049` and `True` all load;
  - `;` between entries, and `=` or `=>` between a key and its value;
  - text after the root value, which org.json never reads.
- org.json keeps keys that differ only in case apart.
  ConvertFrom-Json refuses them unless it builds hashtables,
  so a reader that needs every key, such as the content catalogue, asks for hashtables.

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
beside a `mods` folder that keeps one of the save's mods, changes one, lacks one and adds one,
and writes damaged descriptors under a temporary folder.
[`Invoke-SaveMigrationTool.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Invoke-SaveMigrationTool.IntegrationTests.Tests.ps1)
copies a log fixture to a temporary folder,
and gives its failed save that descriptor and its install those mods;
a case removes the descriptor, or empties its mod lists.
[`Get-ModRoster.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-ModRoster.IntegrationTests.Tests.ps1)
builds a small install per case.
[`Get-ContentCatalogue.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-ContentCatalogue.IntegrationTests.Tests.ps1)
builds one install of the game's own data and three mods:
one patching a game hull file and a game faction, and adding a hull, a faction and a planet,
one whose wing header repeats a column name,
and one replacing a spreadsheet.
The game's own data holds a case of each other kind of source:
skills from both spreadsheets, a description keyed by two columns, and a planets.json entry that is not an object.

[`helpers/`](../../Tests/tools/save-migration-tool/helpers/) holds what the suites share:
running a script with its stderr and warnings captured, blank lines kept,
writing a test file, mod or JAR under the suite's temporary folder,
and building and writing the owner index the content catalogue passes around.

Every suite runs under `Set-StrictMode -Version Latest`, as the scripts do,
so a read the scripts would fail on fails in the suites too.

No test reads a real install, log or save.

The tests live under the repository's top-level `Tests\`, mirroring this folder,
because the shared PowerShell CI only discovers `*.Tests.ps1` files there.
The PowerShell CI workflow runs them on every pull request.

```powershell
Invoke-Pester -Path Tests\tools\save-migration-tool
```
