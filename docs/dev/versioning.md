# Versioning Policy

Authoritative versioning rules for KMLib and every mod that consumes it
(KMU, KMO, KMP, ...). Consumer repositories should link here from their own
release docs rather than restate the rules.

## Index

- [Scheme](#scheme)
- [KMLib version](#kmlib-version)
- [Consumer mod versions](#consumer-mod-versions)
- [Pinning KMLib from a consumer](#pinning-kmlib-from-a-consumer)
- [Pre-1.0 phase](#pre-10-phase)
- [Tag format](#tag-format)
- [Changelog](#changelog)

## Scheme

Every repository in this family follows [Semantic Versioning 2.0.0](https://semver.org/):
`MAJOR.MINOR.PATCH`. KMLib's MAJOR/MINOR/PATCH triggers and consumer mod
triggers differ because they have different consumers (other mods vs.
players), but the structure is identical.

## KMLib version

One version covers both the Java library API and the reusable workflows -
they ship from the same repository and the same git tag pins both. Splitting
into two version axes forces consumers to track two numbers for one upstream.

| Bump  | Trigger                                                                                                                |
| ----- | ---------------------------------------------------------------------------------------------------------------------- |
| MAJOR | Public Java class/method removed or renamed; workflow input added or its behavior changed in a way callers must adapt to; required `mod_info.json` shape changed (e.g., new mandatory field). |
| MINOR | New public Java API; new optional workflow input; new reusable workflow file.                                          |
| PATCH | Bug fix in script logic, internal refactor, documentation change, dependency bump that does not change the contract.   |

## Consumer mod versions

Use save-game compatibility as the breaking-change axis - this is what
matters to players, not just code shape.

| Bump  | Trigger                                                                                                                |
| ----- | ---------------------------------------------------------------------------------------------------------------------- |
| MAJOR | Existing saves break or behave incorrectly (removed/renamed conditions, removed factions, removed market types referenced in save state). |
| MINOR | New features that loaded saves cope with cleanly (new conditions, new UI panels, new commands).                        |
| PATCH | Bug fix, balance tweak, text correction.                                                                               |

## Pinning KMLib from a consumer

Two places pin KMLib and they must agree:

1. **Starsector runtime dependency** in `mod_info.json`:

    ```json
    "dependencies": [
      { "id": "kmlib", "name": "Klark Morrigan's Library", "version": "1.0.0" }
    ]
    ```

    Starsector treats `version` as a *minimum* - the mod loads if the installed
    KMLib reports the listed version or newer.

2. **Workflow pin** in `.github/workflows/*.yml`:

    ```yaml
    uses: <owner>/KMLib/.github/workflows/mod-release.yml@1.0.0
    ```

    GitHub Actions resolves this to the exact tag at the moment the workflow
    runs, so this is a strict pin and gives reproducible builds.

**Rule:** when a consumer bumps its KMLib pin in either place, bump it in the
other in the same commit. The consumer's own version does **not** need to
bump just because KMLib did - only if the consumer's own behavior changed.

**Do not pin to `@master`.** A pin to `master` lets an unrelated KMLib commit
break a consumer release retroactively. Always pin a tag.

## Pre-1.0 phase

While a repository is on `0.x.y`, MINOR is the breaking-change line:
`0.1.0 -> 0.2.0` may break consumers; `0.1.0 -> 0.1.1` must not. This is the
conventional reading of pre-1.0 SemVer and avoids spending MAJOR before APIs
have stabilized.

KMU starts at `0.1.0`. KMLib's first stable tag is `1.0.0`, cut once it has
at least one external consumer pinning it - which KMU does, through the
`kmlib` dependency in its `mod_info.json` that
[validate-versioning](../../.github/actions/validate-versioning/action.yml)
holds to a well-formed SemVer pin at release time.

## Tag format

- Git tags: bare SemVer, no `v` prefix (e.g., `1.0.0`). Matches
  `mod_info.json` `.version` exactly so the same string is used end-to-end.
- `mod_info.json` `version` field: bare (`1.0.0`). Starsector does not accept
  the `v` prefix.

This one-character mismatch is a known papercut and intentional - aligning
either side would conflict with the other system's conventions.

## Changelog

Each repository keeps a top-level `CHANGELOG.md` in
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format. The
reusable release workflow extracts the section matching the new version into
the GitHub release body, so the format must be consistent across repositories:

```markdown
## [Unreleased]

## [1.0.0] - 2026-05-18
### Added
- ...
### Changed
- ...
### Fixed
- ...
```

A missing section for the released version fails the release pipeline, so
release notes are never silently dropped.
