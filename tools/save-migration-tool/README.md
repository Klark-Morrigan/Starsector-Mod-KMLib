# Save Migration Tool (SMT) scripts

Scripts that diagnose a Starsector save that stopped loading after a mod update.
They read the game's files from a terminal and print structured output,
so a caller can act on the result without reading the log itself.
They are repository tooling: the release zip does not carry them.

- [Scripts](#scripts)
- [Get-SaveLoadFailure.ps1](#get-saveloadfailureps1)
  - [How vanilla logs a load](#how-vanilla-logs-a-load)
  - [Missing content](#missing-content)
  - [Output](#output)
- [Get-ModRoster.ps1](#get-modrosterps1)
- [Tests](#tests)

## Scripts

| Script | Answers |
| --- | --- |
| [`Get-SaveLoadFailure.ps1`](Get-SaveLoadFailure.ps1) | Which saves the game's logs show loaded, and why a load failed |
| [`Get-ModRoster.ps1`](Get-ModRoster.ps1) | Which installed folder the game loads for each enabled mod ID, at which version |

Every script writes data to stdout and what it read and decided to stderr.
`Get-SaveLoadFailure.ps1 -AsJson` prints its data as JSON;
`Get-ModRoster.ps1` returns objects for another script to consume.

## Get-SaveLoadFailure.ps1

Lists every save load in `starsector-core\starsector.log` and its rotated siblings, newest first.
An older load is likelier to describe a save or a mod set that has changed since.

```powershell
pwsh -NoProfile -File Get-SaveLoadFailure.ps1 -AsJson          # every load, as JSON
pwsh -NoProfile -File Get-SaveLoadFailure.ps1 -LogPath <log>   # another log
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
| `Missing` | The [missing content](#missing-content) the first attempt's cause names: `Kind`, `Id`, `ReportedId` and `SpecClass`. Empty when the cause names none |
| `Attempts` | Each attempt in order, with its `Source` (`campaign`, `campaign.bak`, `campaign.inprogress`), `Outcome`, `CauseType`, `Cause`, `Missing`, and the `LogFile` and `LogLine` it starts on |

Stderr gains one line per load with a cause, saying what the cause was read as.

`campaign` stands for `campaign.xml`, or `campaign.zip` for a compressed save.
The log does not say which.

## Get-ModRoster.ps1

Resolves the installed side of a save's mod list: for each mod ID the game will load, the one folder that wins.

```powershell
& .\Get-ModRoster.ps1                     # enabled mods
& .\Get-ModRoster.ps1 -IncludeDisabled    # every installed mod
```

- `mod_info.json`'s `id` is the identity, not the folder name: one mod can sit in several folders.
- When it does, the highest `version` wins, compared number by number (`1.10.0` above `1.9.0`).
  `SupersededCount` says how many copies lost.
- `enabled_mods.json` decides what loads.
  A missing or unreadable one warns and lists every mod.
- The JSON is read as leniently as the game reads it:
  comments, trailing commas, a byte order mark, single-quoted values and bare words.
  Each repair is tried only after the stricter ones fail, since a repair can corrupt a file that needed none.
- A `mod_info.json` nothing can parse stays in the roster as `unparsed`, under its folder name, with a warning.
  Dropping it would hide a real mod with nothing to say so.

Each entry carries `Id`, `Version`, `Folder`, `RelativePath` (under the install root), `Status` and `SupersededCount`.

## Tests

[`Get-SaveLoadFailure.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-SaveLoadFailure.Tests.ps1)
runs the script against hand-written install roots under
[`fixtures/`](../../Tests/tools/save-migration-tool/fixtures/).
[`Get-ModRoster.Tests.ps1`](../../Tests/tools/save-migration-tool/Get-ModRoster.Tests.ps1)
builds a small install per case,
so each odd `mod_info.json` sits beside the assertion about it.
No test reads a real install, log or save.

The tests live under the repository's top-level `Tests\`, mirroring this folder,
because the shared PowerShell CI only discovers `*.Tests.ps1` files there.
The PowerShell CI workflow runs them on every pull request.

```powershell
Invoke-Pester -Path Tests\tools\save-migration-tool
```
