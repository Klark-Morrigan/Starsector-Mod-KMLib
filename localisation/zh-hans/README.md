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
A term listed here is not repeated there,
so the two can never disagree.

## Index

- [What this folder holds](#what-this-folder-holds)
- [Rules](#rules)
- [Vanilla terms](#vanilla-terms)
- [KMLib terms](#kmlib-terms)
  - [The compatibility notice](#the-compatibility-notice)
  - [What a failed binding costs](#what-a-failed-binding-costs)
  - [Settings](#settings)
  - [The changelog](#the-changelog)
- [Forum terms](#forum-terms)
- [Adding a string](#adding-a-string)
- [Translating the changelog](#translating-the-changelog)

## What this folder holds

| File | What it is |
| --- | --- |
| [strings.json](strings.json) | Every string KMLib draws in game: the compatibility notice, the sentences its own failed bindings cost, and the jump-point label. |
| [LunaSettings.csv](LunaSettings.csv) | KMLib's row on the LunaLib settings screen: the tab name, the setting's name and its description. |
| [mod_info.json](mod_info.json) | The launcher's mod list entry: `name` and `description` only, merged over the base. |
| [CHANGELOG.md](CHANGELOG.md) | A full translation of the root [CHANGELOG.md](../../CHANGELOG.md): every version, every section. The Chinese zip ships it, and each release's notes show its section for the version. |

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
  The description then ends with each option's translation,
  one `Option：译文` line per option in the Radio's own order,
  after a blank line below the default,
  so a player can read what each button means.
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
- **A highlighted run takes an ASCII space where it touches Chinese.**
  The game highlights a run only when the character on each side is whitespace or ASCII punctuation,
  or the run starts or ends the text.
  A Chinese character or full-width punctuation beside it, `。` above all, leaves it plain, with no error.
  So a highlighted run gets a space on any side that would touch one:
  `将此问题报告给 %s 的开发者 。`, `用 上移 和 下移 调整图层顺序`.
  That covers a format slot the code highlights, a phrase the code highlights, and a `[...]` run in a settings description.
  The parity suite and the suites drawing highlighted paragraphs fail a run that would draw plain.
- **Defaults read `[默认值：X]`, on a line of their own.**
  `X` is the shipped value as the English file writes it,
  except a Boolean, which reads `开启` for true and `关闭` for false.
  The default ends the description, after a line break and with no full stop, as `[Default: X]` does in English.
  A Radio's option translations are the one thing that follows it.
  A line break counts as whitespace, so the default highlights without padding.

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
| Market | 市场 | `starfarer_obf.jar` |
| Star system | 星系 | `starfarer.api.jar`; in tooltips as `Naraka 星系` |
| Faction | 势力 | `starfarer.api.jar` |
| Alliance | 联盟 | vanilla's word for the Persean League, taken for Nexerelin's alliances |
| Sector | 星域 | `descriptions.csv` `plasma`: "known to the Persean Sector" is 英仙座星域已知的 |
| Fleet | 舰队 | `strings.json` `fleetInteractionDialog` `initialWithStationVsLargeFleet`: "your fleet" is 你的舰队 |
| Blueprint | 蓝图 | `special_items.csv` `ship_bp`: "Base Ship Blueprint" is 基础舰船蓝图 |
| ModSpec, hullmod | 船体插件 | `special_items.csv` `modspec`: "Base ModSpec" is 基础船体插件; the item and the hullmod it teaches take the one word |

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
| X ran into an error and switched one of its features off | X 运行出错，已关闭其中一项功能 | the heading of a feature failure; 运行出错 is the phrase |
| a fault in X itself rather than a clash with another mod | X 自身的故障，并非与其他 Mod 的冲突 | |
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
| Feature (row label) | 功能 | one of a mod's own features |
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
| fleet | 舰队 | the [vanilla term](#vanilla-terms) |

### Settings

| English | 简体中文 | Note |
| --- | --- | --- |
| Dev (tab) | 开发 | |
| Log verbosity | 日志详细程度 | verbosity alone is 详细程度 |
| OFF / ERROR / WARN / INFO / DEBUG / ALL (log levels) | 关闭 / 错误 / 警告 / 信息 / 调试 / 全部 | the usual logging words; OFF follows a switch's 关闭 |
| probes that read the game's UI by reflection | 通过反射读取游戏界面的探测器 | reflection is 反射, a probe 探测器 |

### The changelog

The headings every KM mod's translated changelog shares,
and the words its entries lean on most.
A version heading is never translated; see [translating the changelog](#translating-the-changelog).

| English | 简体中文 | Note |
| --- | --- | --- |
| Changelog (the title) | 更新日志 | |
| Index | 索引 | |
| Unreleased (index entry) | 未发布 | the index's link text only; the heading stays `## [Unreleased]` |
| Added | 新增 | |
| Changed | 变更 | |
| Fixed | 修复 | |
| Dependency changes | 依赖变更 | |
| Public contracts changed (**breaking**) | 公共契约变更（**破坏性**） | from 公共契约 |
| Test fixtures | 测试夹具 | |
| For developers | 面向开发者 | |
| No player-facing changes. | 没有面向玩家的变更。 | opens a version whose changes are all under `For developers` |
| Reported by X at USC | 由 X 在 USC 报告 | closes the entry as a sentence of its own |
| Requested by X at USC | 由 X 在 USC 提出请求 | |
| release (a published version) | 发布版本 | |
| release notes | 发布说明 | |
| release zip | 压缩包 | |
| version file | 版本文件 | |
| update checker | 更新检查器 | |
| locale | 语言区域 | |
| bundle | 语言包 | |
| core localisation | 核心本地化 | the Chinese localisation as a project is 中文本地化 |
| library | 程序库 | |
| consumer mod | 使用方 Mod | |
| caller | 调用方 | |
| binding | 绑定 | |
| adapter | 适配器 | |
| seam | 接缝 | |
| renderer, render pass | 渲染器, 渲染通道 | |
| font atlas, face, cut | 字体图集, 字体, 字号 | a cut is one size of a face |
| line height, glyph, fallback glyph | 行高, 字形, 后备字形 | |
| test suite, fake, fixture | 测试套件, 伪实现, 夹具 | |
| data file | 数据文件 | a CSV or JSON a mod ships |
| constructor, no-arg constructor | 构造函数, 无参构造函数 | |
| method handle | 方法句柄 | |
| reflection | 反射 | as in the settings description |
| stand-in (a test double) | 替身 | |
| highlight, highlighted run | 高亮, 高亮片段 | a run is the span of text one highlight tints |
| slot (in a format string) | 槽位 | |
| body colour (of text) | 正文颜色 | |
| solid filled rectangle (a glyph drawn without its atlas) | 实心矩形 | 实心, filled, with 矩形, rectangle |
| map label (a name drawn on the map) | 星图标签 | vanilla's 星图 with 标签, label |
| failed load (of a font) | 加载失败 | the noun form of 无法加载 |
| pull request | 拉取请求 | GitHub's own Chinese interface word |
| gate (a CI check a change must pass) | 门禁 | |
| runner (the machine a CI job runs on) | 运行器 | GitHub's own Chinese documentation word |
| lint (a YAML or shell check) | 检查 | a tool's name, such as yamllint, stays in English |
| field, member (of `mod_info.json`) | 字段, 成员 | 字段 for what a player reads; 成员 for a JSON object's member, as the developer notes write it |
| object-holding array (in JSON) | 对象数组 | 对象, object, before 数组, array: an array whose elements are objects |
| root, nested record (of a JSON file) | 根层级, 嵌套的记录 | 层级, level, so 根 reads as the file's top level rather than a root directory |
| campaign speed-up | 战役加速, 加速 for short | settled in the `0.5.0` entries; vanilla's own "Speeding up time" sits in the game jar, which the cached core localisation does not hold, so it is unchecked |
| 2nd fix (another fix for a bug an earlier version claimed fixed) | 第二次修复 | 第二次, the second time, before 修复 as in the Fixed heading |
| location (what holds entities: a star system or hyperspace) | 位置 | as the `0.3.0` entries write it |
| advance (one step of a script) | 推进 | as the `0.3.0` and `0.5.0` entries write it |
| put back (an entity taken out of its location) | 放回 | the counterpart of 移出 |
| placement (of a map icon among the others), unplaceable | 叠放情况, 无法定位 | 叠放, stacking, as the `0.5.0` entry's 重新叠放; 定位, to locate, kept apart from 位置 for a location |
| supplier (a function a caller hands in) | 供应器 | |
| lift (an icon moved past the map's nebulae) | 抬升 | as the `0.5.0` entries write it |
| reseat (the removal and put-back a lift is made of) | 重新安置 | as the `0.3.0` entries write it |
| reading (what one advance found and ordered) | 读数 | as the `0.5.0` entries write it |
| log line | 日志行 | as the settings description and the `0.5.0` entries write it |
| wait (an advance spent with the entity out) | 等待 | the plain word |
| long name (of a faction) | 长名称 | 长, long, before 名称, name |
| row (one line of a list or hover box) | 行 | as 日志行 writes it |
| blank (text holding only whitespace) | 空白 | |
| walk (a read over every item of a set), sector walk counters | 遍历, 星域遍历计数 | as the `0.5.0` entries write 遍历; 计数, count |
| map (a Java `Map`), list, record | 映射, 列表, 记录 | the usual computing words, as the `0.4.0` and `0.5.0` entries write them |
| teach (what an item gives a faction to know), know | 传授, 掌握 | |

## Forum terms

The words of a KM mod's thread on [Fossic](https://www.fossic.org/), the Chinese Starsector forum,
and of its release notes.
How such a thread works is [docs/dev/fossic-thread.md](../../docs/dev/fossic-thread.md).

| English | 简体中文 | Note |
| --- | --- | --- |
| vanilla (the base game) | 原版 | the players' word; the game leaves "vanilla" in English |
| the Chinese core localisation | 远行星号中文汉化, 汉化包 for short | the name of its Fossic thread |
| Simplified Chinese edition | 简体中文版 | the usual way a Chinese edition is named |
| English edition | 英文版 | the counterpart of 简体中文版 |
| Fossic (the forum) | Fossic, with 远行星号中文论坛 beside it | the forum's own site name, "the Starsector Chinese forum", for a reader who knows it by that |
| forum thread | 论坛帖子, 帖子 for short; 本帖 for this thread | 帖子, a post or thread, the forum's own word, as its 发表帖子 button writes it |
| pre-release | 预发布 | Fossic's word for its pre-release board |
| testing (a title tag) | 测试 | how Fossic titles mark a pre-release |
| repost | 搬运 | a Fossic board, for mods posted by someone other than their author |
| translation (a translated build) | 汉化 | a Fossic board |
| dependency (a required mod) | 前置 | Fossic's word; its posting form says 依赖Mod |
| attachment | 附件 | a file uploaded to a forum post |
| download panel | 下载面板 | the posting form's word, from its 插入Mod文件下载面板到帖子正文中 button |
| zip (a release archive) | 压缩包 | the plain word for an archive file |
| mod release (a thread tag) | mod发布 | the one tag Fossic's mod threads share; tags there are free text, so a thread's others are the words a player would search for |
| mod author | Mod 作者 | Mod kept Latin as vanilla does, with 作者, author; the posting form writes Mod作者 |
| translation reference (the terminology reference, as a post names it) | 翻译参考 | 参考, reference, after 翻译, translation |
| rationale (for a term's choice) | 选词理由 | 选词, choosing a word, with 理由, reason: what each row's note records |
| translation (the act) | 翻译 | the plain word; 汉化 is a translated build, the forum's sense |
| install mid-run | 中途安装 | 中途, partway through |
| mod installer (a third-party tool) | Mod 安装工具 | 安装, install, with 工具, tool; Mod kept Latin as in Mod 作者 |
| uninstall, uninstall procedure | 卸载, 卸载步骤 | the usual software word; 步骤 for the steps |
| changelog | 更新日志 | [KMLib's](https://github.com/Klark-Morrigan/Starsector-Mod-KMLib/blob/master/localisation/zh-hans/README.md#the-changelog) |

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

## Translating the changelog

Every entry added to the root [CHANGELOG.md](../../CHANGELOG.md) is added here in the same pull request:
`LocaleParityIntegrationTests` holds this file to the root one point for point,
so a pull request adding a point the translation lacks fails.

1. **Version headings stay as written**, byte for byte:
   `## [Unreleased]`, `## [0.4.0] - 2026-09-15`.
   A release finds a version's notes by its heading,
   and the index links each version by the anchor its heading makes.
2. **The same sections in the same order**, at the same heading level,
   their headings translated by [the changelog terms](#the-changelog).
3. **One point for one point.**
   Every list item stays one item at the same depth;
   never merge, split, drop or add one.
   Wording, links and emphasis are the translation's own and are not compared.
4. **One line per point and per paragraph.**
   A version's section becomes the release notes,
   where GitHub draws every newline as a line break.
5. **Links to repository files take `../../`**,
   this file sitting two directories below the root one.
   Absolute links and `#` anchors stay as they are.
6. **Code spans stay as written.**
   Each section must hold the same code spans as the root one's,
   in any order within the section.
   A changed identifier in either file fails until the other matches.
   The [rules](#rules) hold as they do for strings.
