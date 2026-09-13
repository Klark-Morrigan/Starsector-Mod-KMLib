# Versioning Policy

## Index

- [Scheme](#scheme)
- [KMLib version](#kmlib-version)
- [Consumer mod versions](#consumer-mod-versions)
- [What the game does with a version](#what-the-game-does-with-a-version)
- [Pinning KMLib from a consumer](#pinning-kmlib-from-a-consumer)
- [Pre-1.0 phase](#pre-10-phase)
- [Tag format](#tag-format)
- [Changelog](#changelog)

## Scheme

[Semantic Versioning 2.0.0](https://semver.org/):
`MAJOR.MINOR.PATCH`,
digits only.

```
0.1.0
```

KMLib's MAJOR/MINOR/PATCH triggers and consumer mod triggers differ
because they have different consumers (other mods vs. players),
but the structure is identical.

MAJOR is meant to be expensive.
Holding it still is the discipline that resists changing a public contract on a whim -
if a change would force MAJOR,
that is the signal to find a compatible way to make it instead.

**No letters,
ever.** A suffixed version like `1.2.3a` is not rejected by anything downstream,
it is silently mangled:
the game's own parser splits a version on the letter `a` as well as on `.`,
and TriOS strips letters out of a `mod_info.json` version entirely,
so two releases differing only by a letter look identical to it.
The release gate enforces digits-only.

## KMLib version

One version covers both the Java library API and the reusable workflows -
they ship from the same repository and the same git tag pins both.
Splitting into two version axes forces consumers to track two numbers for one upstream.

| Bump  | Trigger                                                                                                                |
| ----- | ---------------------------------------------------------------------------------------------------------------------- |
| MAJOR | Public Java class/method removed or renamed; workflow input added or its behaviour changed in a way callers must adapt to; required `mod_info.json` shape changed (e.g., new mandatory field). |
| MINOR | New public Java API; new optional workflow input; new reusable workflow file.                                          |
| PATCH | Bug fix in script logic, internal refactor, documentation change, dependency bump that does not change the contract.   |

## Consumer mod versions

Use save-game compatibility as the breaking-change axis -
this is what matters to players,
not just code shape.

| Bump  | Trigger                                                                                                                |
| ----- | ---------------------------------------------------------------------------------------------------------------------- |
| MAJOR | Existing saves break or behave incorrectly (removed/renamed conditions, removed factions, removed market types referenced in save state). |
| MINOR | New features that loaded saves cope with cleanly (new conditions, new UI panels, new commands).                        |
| PATCH | Bug fix, balance tweak, text correction.                                                                               |

## What the game does with a version

Starsector parses the version in `mod_info.json` with `com.fs.starfarer.launcher.ModManager$VersionInfo`,
and its behaviour is narrower than it looks:

- **Comparison is string equality,
  component by component -
  not ordering.** There is no numeric parse and no "or newer" anywhere.
  The result is a compatibility level chosen by which component first differs,
  which the launcher displays;
  a *newer* dependency than the pin is flagged exactly like an older one.
- **The parser splits on `.`,
  `a-RC` and `a`.** The letter cases exist for the game's own `0.98a-RC8` format
  and apply to mod versions too -
  the reason the scheme is digits-only.
- **Only 2,
  3 or 4 components are handled.** Anything else leaves the parsed major empty,
  and an empty major makes the dependency check skip itself without a word.
- **A leading `0` fuses the first two components into the parsed major**,
  so `0.1.0` reaches the game as major `0.1`,
  minor `0`,
  empty patch,
  while `1.2.3` arrives as the three components you would expect.
  This only shifts which severity the launcher renders,
  since every comparison is equality regardless.

## Pinning KMLib from a consumer

Two places pin KMLib and they must agree:

1. **Starsector runtime dependency** in `mod_info.json`:

    ```json
    "dependencies": [
      { "id": "kmlib", "name": "Klark Morrigan's Library", "version": "1.0.0" }
    ]
    ```

    This is an exact-match check,
    not a minimum -
    see above.
    The pin names the precise KMLib the consumer was built against,
    and any other installed KMLib is reported as a mismatch.

2. **Workflow pin** in `.github/workflows/*.yml`:

    ```yaml
    uses: <owner>/Starsector-Mod-KMLib/.github/workflows/mod-release.yml@1.0.0
    ```

    GitHub Actions resolves this to the exact tag at the moment the workflow runs,
    so this is a strict pin and gives reproducible builds.

**Rule:** when a consumer bumps its KMLib pin in either place,
bump it in the other in the same commit.
The consumer's own version does **not** need to bump just because KMLib did -
only if the consumer's own behaviour changed.

Note the cost the equality check imposes:
because the runtime pin matches exactly,
any KMLib bump leaves every consumer reporting a mismatch until its pin is updated too.

**The runtime pin must name a KMLib that has already been released.** A player installs KMLib from its release page,
so a pin naming a version that was never published leaves the game refusing to load a mod nobody can obtain the dependency for.
The release pipeline checks this before it builds anything,
in the same cheap gate that enforces the rules above,
via [check-dependency-release](../../.github/actions/check-dependency-release/action.yml).

This makes the two releases sequential where they used to be independent:
bumping a consumer's `kmlib` pin fails that consumer's release
until the matching KMLib release exists.
Release KMLib first,
then the consumers that pin the new version.
The check reports an absent release separately from a lookup it could not complete -
the first is a verdict on the pin,
the second explicitly is not,
and only the first means the pin needs changing.

**Do not pin to `@master`.** A pin to `master` lets an unrelated KMLib commit break a consumer release retroactively.
Always pin a tag.

## Pre-1.0 phase

While a repository is on `0.x.y`,
MINOR is the breaking-change line:
`0.1.0 -> 0.2.0` may break consumers;
`0.1.0 -> 0.1.1` must not.
This is the conventional reading of pre-1.0 SemVer
and avoids spending MAJOR before APIs have stabilised.

KMU starts at `0.1.0`.
KMLib's first stable tag is `1.0.0`,
cut once it has at least one external consumer pinning it -
which KMU does,
through the `kmlib` dependency in its `mod_info.json` that [validate-versioning](../../.github/actions/validate-versioning/action.yml)
holds to a well-formed SemVer pin at release time.

## Tag format

- Git tags:
  bare SemVer,
  no `v` prefix (e.g., `1.0.0`).
  Matches `mod_info.json` `.version` exactly so the same string is used end-to-end.
- `mod_info.json` `version` field:
  bare (`1.0.0`).
  Starsector does not accept the `v` prefix.

This one-character mismatch is a known papercut and intentional -
aligning either side would conflict with the other system's conventions.

## Changelog

Each repository keeps a top-level `CHANGELOG.md` in [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.
The reusable release workflow extracts the section matching the new version into the GitHub release body,
so the format must be consistent across repositories:

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

A missing section for the released version fails the release pipeline,
so release notes are never silently dropped.
