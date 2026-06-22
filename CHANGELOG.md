# Changelog

All notable changes to KMLib are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project
adheres to [Semantic Versioning](https://semver.org/). Versioning triggers
for KMLib and its consumer mods are defined in
[docs/dev/versioning.md](docs/dev/versioning.md).

The reusable release workflow extracts the section matching the released
version into the GitHub release body, so every released version must have a
section here.

## [Unreleased]

## [0.1.0] - 2026-06-18
First tagged release. Establishes the shared Java helper jar consumed by the
KM mod family and the reusable CI / release pipeline that ships it.

### Added

#### Library helpers (`kmlib.*`)
- `text` - Starsector-agnostic utilities: `KmlibStrings` string predicates
  (`hasText`) and `KmlibNumbers` numeric formatting (`formatDelta`, signed
  delta with an explicit leading `+`).
- `starsector.strings` - `StarsectorStrings`, a defensive wrapper around
  `settings.json` localisation lookups that emits a loud `REDACTED` on
  missing / malformed entries; and `StarsectorFormat.formatPercent`, which
  renders a `[0, 1]` fraction as a truncated whole-percent string.
- `starsector.factions` - `StarsectorPlayerFactionResolver`, centralising
  player-faction display-name normalisation across vanilla and Nexerelin
  defaults (`isPlayerFactionEstablished`, `resolveDisplayName`).
- `starsector.relation` - `StarsectorPlayerRelationshipFormatter` for
  player-relationship display copy.
- `starsector.ui.color` - `StarsectorUiColor`, a palette enum that routes
  vanilla shades through `Misc` suppliers and resolves to a live `Color`.
- `starsector.ui.highlight` - `Highlight`, `HighlightedParagraph`, and
  `HighlightedMessage`, binding substrings to colours and rendering to
  `TextPanelAPI`, `TooltipMakerAPI`, `LabelAPI`, and `MessageIntel`.
- `starsector.ui.tooltip` - `Tooltips`, a wrapper over the tooltip-make API.
- `starsector.intel` - `BaseTaggedIntelPlugin` (tab-tag mix-in over vanilla's
  `BaseIntelPlugin`) and `BaseExpiringIntelPlugin` (timestamped auto-removal
  with a default one-month window and a static `findActive` lookup).
- `starsector.scripts` - `SectorScripts`, for safely registering scripts on
  the `SectorAPI`.
- `starsector.time` - `StarsectorClock`, exposing engine calendar constants
  vanilla keeps private (`DAYS_PER_MONTH`).
- `math` - `Jitter`, a uniform random multiplier centred on `1.0` whose
  single parameter is the band half-width (`roll(0.15)` yields
  `[0.85, 1.15]`); one shared definition for consumers that scale a value by
  random monthly variation.
- `starsector.testing` - `StarsectorSettingsFake`, a test seam for the
  settings-backed helpers.

#### CI / release pipeline
- Reusable `mod-release.yml` workflow that any KM-family mod consumes with a
  one-line `release.yml`, deriving all mod-specific values from the caller's
  `mod_info.json`.
- Composite actions backing the pipeline: `read-mod-info`, `check-version`,
  and `validate-versioning`, each unit-tested with bats. The GitHub release
  body and asset attachment reuse Common-Automation's stack-agnostic
  `create-github-release` action rather than a KMLib-specific extractor.
- `release.yml` so KMLib releases itself through the same pipeline via a
  local workflow ref.
- Versioning policy ([docs/dev/versioning.md](docs/dev/versioning.md)) shared
  by KMLib and every consumer mod.
