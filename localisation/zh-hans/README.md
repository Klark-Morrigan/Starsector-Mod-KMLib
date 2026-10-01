# KMLib in Simplified Chinese (简体中文)

The terminology reference for KMLib's `zh-hans` bundle:
the words the Chinese core localisation already uses for the vanilla concepts KMLib's own text touches,
and the translations settled for KMLib's own.
Check a term here before translating a new string,
and add a row when a new term is settled.
How bundles are built and checked is in the root README's
[Build & Test](../../README.md#build--test) section.

KMLib is the library under every KM mod,
so this is the base reference:
its rules hold for every KM mod's bundle,
and a mod built on KMLib keeps a reference of its own beside its bundle,
building on this one with the terms of its features.

## Index

- [What this folder holds](#what-this-folder-holds)
- [Rules](#rules)
- [Vanilla terms](#vanilla-terms)
- [KMLib terms](#kmlib-terms)
  - [The compatibility notice](#the-compatibility-notice)
  - [What a failed binding costs](#what-a-failed-binding-costs)
  - [Settings](#settings)
- [Adding a string](#adding-a-string)

## What this folder holds

| File | What it is |
| --- | --- |
| [strings.json](strings.json) | Every string KMLib draws in game: the compatibility notice, the sentences its own failed bindings cost, and the jump-point label. |
| [LunaSettings.csv](LunaSettings.csv) | KMLib's row on the LunaLib settings screen: the tab name, the setting's name and its description. |
| [mod_info.json](mod_info.json) | The launcher's mod list entry: `name` and `description` only, merged over the base. |

Players of this bundle are expected to have the
[Chinese core localisation](https://github.com/TruthOriginem/Starsector-Localization-CN)
laid over `starsector-core`, as [manifest.json](../manifest.json) records under `coreLocalisation`.
The game's own fonts hold no Chinese characters;
the core localisation replaces them, and without it every Chinese character draws as `?`.

## Rules

These hold for every KM mod's Chinese bundle.
A mod's own reference may add rules for its own features.

- **Vanilla's word wins.**
  Where the game already names a concept, a KM mod uses the core localisation's word for it,
  so a player reads the same word in a KM screen as in the game's own.
  The [vanilla terms](#vanilla-terms) below are that evidence.
- **Radio options stay English.**
  LunaLib saves the label of the option picked, not its position,
  so a translated option would lose the player's setting and break the code reading it.
  Descriptions quote the English option as it appears:
  the log levels `OFF, ERROR, WARN, INFO, DEBUG, ALL`.
- **Proper nouns stay Latin.**
  Names of places, people, mods and files keep their English spelling, as the core localisation keeps them:
  `KMLib`, `LunaLib`, `Nexerelin`, `Fast Rendering`, `Random Assortment of Things`, `starsector.log`.
  A Latin word inside Chinese text takes a space on each side (`加载 Mod 错误`),
  except against full-width punctuation.
- **Full-width punctuation**, the core localisation's own:
  `，` `。` `：` `；` `（）` `、`.
  ASCII stays where the text is markup or a number:
  LunaLib's `[` `]` highlight marks, `%%` and format slots, and numeric runs.
  A name's parenthesised suffix keeps ASCII parentheses and a leading space,
  as the core localisation writes names (`通讯中继站 (隐藏)`):
  `Klark Morrigan 的程序库 (KMLib)`.
- **No em dash (`——`).**
  An aside becomes a comma, a colon or parentheses.
- **Word order may move; arguments keep their slots.**
  A sentence whose order differs from English numbers its slots (`%2$s`).
  Each argument keeps its conversion,
  and the parity suite fails a dropped or retyped slot.
  Where the code highlights part of a sentence, the highlighted run must still appear in it word for word.
  The compatibility notice is built from such runs, so its phrases are translated whole
  and placed where Chinese puts them; see [the compatibility notice](#the-compatibility-notice).
- **Defaults read `[默认值：X]`.**
  `X` is the shipped value as the English file writes it,
  except a Boolean, which reads `开启` for true and `关闭` for false.

## Vanilla terms

What the core localisation writes for each vanilla English term KMLib's text touches.
Each row was read off the core localisation itself,
by pairing its text with vanilla's at the same location:
the same `strings.json` key, the same CSV row, or the same string constant in the same class of the game jars.
The core localisation at `startup-optimization` 2026.09.04 was the edition read;
the typeface editions share its text.

| English | 简体中文 | Source |
| --- | --- | --- |
| Jump-point | 跳跃点 | `descriptions.csv` `jump_point_normal` is 跳跃点，前往超空间; `strings.json` `approach` is 驶向了跳跃点 |
| Hyperspace | 超空间 | `descriptions.csv` `jump_point_normal` |
| Ok (a dialog's confirm button) | 确认 | `starfarer.api.jar`, `SetFlagship`: the picker's `"Ok"` / `"Cancel"` pair is 确认 / 取消 |
| Cancel | 取消 | the same pair |
| Mod | Mod | `starfarer_obf.jar`, "Found mod: %s" is 发现 Mod：%s |
| Save (a saved game) | 存档 | "Load last save" is 读取最近的存档 |
| Colony | 殖民地 | `starfarer_obf.jar` |
| Star system | 星系 | `starfarer.api.jar`; in tooltips as `Naraka 星系` |
| Faction | 势力 | `starfarer.api.jar` |
| Alliance | 联盟 | vanilla's word for the Persean League, taken for Nexerelin's alliances |

## KMLib terms

KMLib's own concepts, settled when the bundle was first written.

### The compatibility notice

The notice is composed from phrases, each a highlighted run the sentence around it must carry verbatim,
so each phrase is one row here and the sentences number their slots to put it where Chinese wants it.

| English | 简体中文 | Note |
| --- | --- | --- |
| Klark Morrigan's Library (KMLib) (the mod's name) | Klark Morrigan 的程序库 (KMLib) | `KMLib` is the brand: kept in every locale, never translated |
| the shared library | 共享库 | KMLib as the notice and the settings row name it |
| KM mods | KM 系列 Mod | every mod built on KMLib |
| integration | 集成 | |
| Error integrating X with Y | X 与 Y 集成出错 | the heading; the phrase 集成出错 closes the sentence rather than opening it |
| public contracts | 公共契约 | |
| carries changes to public contracts | 更改了公共契约 | |
| it's new and carries changes to public contracts | 版本较新且更改了公共契约 | |
| depends on | 依赖于 | the sentence completes it with 这些契约 |
| too old | 版本过旧 | |
| needs to be updated to at least X | 需要更新到至少 X | |
| either downgrade X to V or wait for a Y update | 将 X 降级到 V，或等待 Y 更新 | |
| report this issue to the X developer | 将此问题报告给 X 的开发者 | |
| Mod (row label) | Mod | |
| Integration (row label) | 集成 | |
| Targeted (row label) | 目标版本 | the version the build was type-checked against |
| Detected (row label) | 检测版本 | the version installed |
| Broken (row label) | 已损坏 | |
| Failed while (row label) | 失败于 | |
| Effect (row label) | 影响 | |
| No effect (row label) | 不受影响 | |
| unknown (a version nothing could read) | 未知 | |
| See X for more details | 详情请见 X | |

The row labels are padded with ASCII spaces after the full-width colon,
so the values line up as they do in English.

### What a failed binding costs

| English | 简体中文 | Note |
| --- | --- | --- |
| this session | 本次游戏期间 | opens every sentence stating a loss |
| and on your save | 以及你的存档 | closes every sentence stating what is unaffected |
| founded or handed over (colonies) | 建立或移交 | |
| the game's own rules | 游戏自身的规则 | |
| routes (Random Assortment of Things' own) | 路径 | |
| read as cut off | 视为无法到达 | |
| fleet | 舰队 | |

### Settings

| English | 简体中文 | Note |
| --- | --- | --- |
| Dev (tab) | 开发 | |
| Log verbosity | 日志详细程度 | verbosity alone is 详细程度 |
| probes that read the game's UI by reflection | 通过反射读取游戏界面的探测器 | reflection is 反射, a probe 探测器 |

## Adding a string

1. Look the term up here.
   A vanilla concept missing here is looked up in the core localisation before it is translated:
   find the English in vanilla's `data/` or game jars, and read what the core localisation has at the same place.
2. Translate by the [rules](#rules), and add a row for any new term to the [KMLib terms](#kmlib-terms).
3. Check the characters render.
   Every character must be in the core localisation's font atlases,
   `starsector-core/graphics/fonts/<face>.fnt` on each installed edition,
   or it draws as `?`.
4. Run the tests.
   `LocaleParityIntegrationTests` holds this bundle to the English one,
   and `test` takes every bundle as an input,
   so a change made here alone re-runs it.
