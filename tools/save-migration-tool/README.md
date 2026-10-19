# Save Migration Tool (SMT) scripts

Scripts that diagnose a Starsector save that stopped loading after a mod update.
They read the game's files from a terminal and print structured output,
so a caller can act on the result without reading the log itself.
They are repository tooling: the release zip does not carry them.

- [Scripts](#scripts)
- [Layout](#layout)
- [Get-SaveLoadAttempt.ps1](#get-saveloadattemptps1)
  - [How vanilla logs a load](#how-vanilla-logs-a-load)
  - [Missing content](#missing-content)
  - [Output](#output)
- [Get-ModRoster.ps1](#get-modrosterps1)
  - [How the game reads its JSON](#how-the-game-reads-its-json)
- [Tests](#tests)

## Scripts

| Script | Answers |
| --- | --- |
| [`Get-SaveLoadAttempt.ps1`](Get-SaveLoadAttempt.ps1) | Which saves the game's logs show loaded, and why a load failed |
| [`Get-ModRoster.ps1`](Get-ModRoster.ps1) | Which installed folder the game loads for each enabled mod ID, at which version |

Every script takes the install it reads as `-StarsectorRoot`; none assumes where the game is installed.
Every script writes data to stdout and what it read and decided to stderr.
`Get-SaveLoadAttempt.ps1 -AsJson` prints its data as JSON;
`Get-ModRoster.ps1` returns objects for another script to consume.

## Layout

Each script holds its parameters and its flow.
The functions it calls live one per file under [`lib/`](lib/), named after the function, and the scripts dot-source the ones they need.

| Folder | Holds |
| --- | --- |
| [`lib/`](lib/) | `Write-DiagnosticLine`, which every script logs through |
| [`lib/load-log/`](lib/load-log/) | Reading save load attempts out of `starsector.log` and what their causes name |
| [`lib/mod-info/`](lib/mod-info/) | Reading JSON as the game does, and mod versions |
| [`lib/mod-roster/`](lib/mod-roster/) | Turning a `mods\` folder into the roster the game loads |

## Get-SaveLoadAttempt.ps1

Lists every save load in `starsector-core\starsector.log` and its rotated siblings, newest first.
An older load is likelier to describe a save or a mod set that has changed since.

```powershell
pwsh -NoProfile -File Get-SaveLoadAttempt.ps1 -StarsectorRoot <install> -AsJson   # every load, as JSON
pwsh -NoProfile -File Get-SaveLoadAttempt.ps1 -LogPath <log>                      # another log
```

### How vanilla logs a load

All load lines come from `CampaignGameManager`.
An attempt opens with `Loading <save folder>...` and ends with `Finished loading` or `Error loading`.
After `Error loading`, an ERROR line and the stack trace follow.
The last throwable header in that trace (the deepest `Caused by:`) is the root cause.
The loader's own stack frames name the same class, so only timestamped lines count as loader lines.

A failed attempt makes vanilla retry from `campaign.xml.bak`, and a failed retry tries `campaign.xml.inprogress`.
That file rarely exists, so a broken save usually leaves three failures, the last a `FileNotFoundException`.
The script folds these retries back into one load:
an attempt is a retry when the one before it failed on the same save and the load has used fewer than three sources.
Loading the same broken save twice so still reads as two loads.

Rotated logs (`starsector.log.1`, `.2`, ...) are read first, highest number first, as one stream.
An attempt can straddle a rotation.
Rotation renumbers the files, so a reported log position holds only until the game rotates again.

### Missing content

A failed load usually names one id that the save holds and no installed mod defines.
Each spec store throws its own wording, and the script reads each into a kind and an id:

| Cause | Kind |
| --- | --- |
| `Weapon spec [<id>] not found!` | `Weapon` |
| `Ship hull spec [<id>] not found!` | `Hull` |
| `Ship hull variant [<id>] not found!` | `Variant` |
| `Fighter wing spec [<id>] not found` | `FighterWing` |
| `Hullmod with id [<id>] not found` | `Hullmod` |
| `Spec of class [<class>] with id [<id>] not found` | `Spec`, with the class kept as `SpecClass` |
| `CannotResolveClassException: <class>` | `Class`, the class name as the id |

`Spec` covers every spec type without a store of its own: special items, market conditions, industries and more.
Its class name is often obfuscated,
so it says which store missed, not always which kind.

Vanilla builds a d-hull for every hull at startup, under the hull id plus `_default_D`.
A missing d-hull so means a missing base hull, and the base hull is what a mod defines.
`Id` holds the base hull;
`ReportedId` keeps the id as the message names it.

A cause matching none of these, a crash in a mod's script for one, gives no missing content.

### Output

One object per load, newest first:

| Field | Meaning |
| --- | --- |
| `SaveFolder` | The save's folder name under `saves\` |
| `SavePath` | The full save path, with `..` resolved against the log's folder (the game's working directory) |
| `Outcome` | `Loaded`, `Failed`, or `Unfinished` when the log ends first: a crash, or a load still running |
| `CauseType`, `Cause` | The root throwable of the first attempt, read from `campaign.xml`, the newest file. Empty when that attempt loaded |
| `Attempts` | Each attempt in order, with its `Source` (`campaign`, `campaign.bak`, `campaign.inprogress`), `Outcome`, `CauseType`, `Cause`, the `LogFile` and `LogLine` it starts on, and its `Missing` |
| `Missing` | The [missing content](#missing-content) the first attempt's cause names: `Kind`, `Id`, `ReportedId` and `SpecClass`. Empty when the cause names none |

Stderr gains one line per load with a cause, saying what the cause was read as.

`campaign` stands for `campaign.xml`, or `campaign.zip` for a compressed save.
The log does not say which.

## Get-ModRoster.ps1

Resolves the installed side of a save's mod list: for each mod ID the game will load, the one folder that wins.

```powershell
pwsh -NoProfile -File Get-ModRoster.ps1 -StarsectorRoot <install>                    # enabled mods
pwsh -NoProfile -File Get-ModRoster.ps1 -StarsectorRoot <install> -IncludeDisabled   # every installed mod
```

- `mod_info.json`'s `id` is the identity, not the folder name: one mod can sit in several folders.
- When it does, the highest `version` wins, compared number by number (`1.10.0` above `1.9.0`).
  `SupersededCount` says how many copies lost, and stderr names each winner and loser.
- `enabled_mods.json` decides what loads.
  A missing or unreadable one warns and lists every mod.
- A folder with no `mod_info.json` is not a mod; stderr names each one skipped.
- A `mod_info.json` nothing can parse stays in the roster as `unparsed`, under its folder name, with a warning.
  Dropping it would hide a real mod with nothing to say so.

Each entry carries `Id`, `Version`, `Folder`, `RelativePath` (under the install root), `Status` and `SupersededCount`.

### How the game reads its JSON

Every file is read as the game reads its data files, neither stricter nor looser.
`LoadingUtils` strips `#` comments, then org.json parses what is left.

- A `#` outside a string starts a comment that runs to the end of its line.
  The game tracks strings by flipping on every `"`, escaped or not, and forgets an open string at each line break;
  the reader does the same, so a `#` after an odd number of `\"` is a comment here too.
- `//` and `/* */` are not comments to the game, and a file holding them fails to parse.
- A file opening with a byte order mark fails, as it does in the game.
- org.json accepts trailing commas, single-quoted strings and unquoted values.
  ConvertFrom-Json rejects the last two, so the reader repairs them,
  each repair tried only after the ones before it fail, since a repair can corrupt a file that needed none.

## Tests

Each `lib/` function has a unit suite of the same name under
[`Tests/tools/save-migration-tool/lib/`](../../Tests/tools/save-migration-tool/lib/),
in the same subfolder, with the functions it calls mocked.

Each script has an integration suite, which runs it with its `lib/` functions unmocked:
[`Get-SaveLoadAttempt.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-SaveLoadAttempt.IntegrationTests.Tests.ps1)
runs the script against hand-written install roots under
[`fixtures/`](../../Tests/tools/save-migration-tool/fixtures/),
whose logs are shaped as 0.98a writes them.
[`Get-ModRoster.IntegrationTests.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-ModRoster.IntegrationTests.Tests.ps1)
builds a small install per case.

No test reads a real install, log or save.

The tests live under the repository's top-level `Tests\`, mirroring this folder,
because the shared PowerShell CI only discovers `*.Tests.ps1` files there.
The PowerShell CI workflow runs them on every pull request.

```powershell
Invoke-Pester -Path Tests\tools\save-migration-tool
```
