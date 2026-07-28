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
