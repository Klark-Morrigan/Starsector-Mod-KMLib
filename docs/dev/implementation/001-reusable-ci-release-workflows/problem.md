# Problem: KMLib should own CI and release workflows for Klark Morrigan's mods

## Index

- [Context](#context)
- [Goal](#goal)
- [Versioning](#versioning)
- [Non-goals](#non-goals)

## Context

The KMU mod implements its own CI and release automation under
[mods/KMU/.github/](../../../../../KMU/.github/):

- `workflows/ci.yml` - PR build gate plus `workflow_call` for the release pipeline;
- `workflows/release.yml` - version-check, ci, prepare-artifact, create-tag, create-release;
- `scripts/check_version.sh` - compares `mod_info.json` version to the latest git tag;
- `scripts/extract_changelog.sh` - extracts the current version's section from `CHANGELOG.md`;
- `tests/*.bats` - bats-core tests for the two scripts.

KMO, KMP, and future mods need the same pipeline. Copy-pasting the files into each
repository duplicates logic (and the bug-fix surface) across every mod.

KMLib is the natural home: it is already the shared library every KMU-family mod
depends on, and there is precedent in
[Infrastructure-Common](../../../../../../../a_Code/Infrastructure-Common/)
for using one repository to host reusable workflows that downstream repositories
consume via `uses: <owner>/<repo>/.github/workflows/<file>.yml@<tag>`.

## Goal

Move the CI and release automation into KMLib as **reusable workflows** and
**composite actions**, with **zero inputs** required from callers. Caller mods
shrink to thin wrapper workflows that delegate to KMLib pinned by git tag.

Mod-specific values (mod short name, runner label, version) are derived inside
KMLib from each caller's `mod_info.json` - see
[Convention](#convention-derived-from-mod_infojson).

### Convention (derived from `mod_info.json`)

All values derive from `mod_info.json`. The mod `id` field is used verbatim
wherever a mod identifier is needed - no transformation, no casing change.
KMU's id is `kmu` (renamed from the earlier `klark_morrigans_utilities` as a
prerequisite to this plan). KMLib's id is `kmlib`. Future mods follow the same
short-lowercase convention.

| Value           | Derivation                                           | Example         |
| --------------- | ---------------------------------------------------- | --------------- |
| Mod id          | `.id`                                                | `kmu`           |
| Version         | `.version`                                           | `0.1.0`         |
| Runner label    | `<mod-id> + "-runner"`                               | `kmu-runner`    |
| Dist directory  | `dist/<mod-id>/`                                     | `dist/kmu/`     |
| Release zip     | `<mod-id>-<version>.zip`                             | `kmu-0.1.0.zip` |
| Jar source path | `jars[0]` from `mod_info.json`                       | `jars/KMU.jar`  |
| Asset dirs      | Fixed set: `data`, `graphics`, `sounds` (if present) | -               |

A short bootstrap job runs on `runs-on: self-hosted` (implicit label on every
self-hosted runner), reads `mod_info.json`, and emits the derived values as job
outputs that downstream jobs consume.

## Versioning

This work introduces KMLib as the authoritative version anchor for the mod
family. The versioning rules for KMLib and every consumer mod are defined in
[../versioning.md](../versioning.md). The plan's
[Step 7](plan.md#step-7---tag-kmlib-v100) and
[Step 8](plan.md#step-8---migrate-kmu-to-consume-kmlib-reusable-workflows)
both follow that policy (tag prefix, dependency pin shape, workflow pin shape).

## Non-goals

- Changing how releases are *triggered* (still: push to master with a bumped version).
- Changing the runtime payload shape (jar, mod_info.json, assets) inside the zip.
- Adding new release channels (no Forum post automation, no auto-update server).
- Migrating mods other than KMU in this plan. KMO/KMP adoption is separate work,
  but they will consume the same KMLib workflows once tagged.
