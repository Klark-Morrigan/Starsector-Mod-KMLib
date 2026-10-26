# KMLib in Simplified Chinese (简体中文)

The terminology reference for KMLib's `zh-hans` bundle:
the words the Chinese core localisation already uses for the vanilla concepts KMLib's own text touches,
and the translations settled for KMLib's own.
Check a term here before translating a new string,
and add a row when a new term is settled.
Every table keeps its rows in alphabetical order of the English column, ignoring case.
A long section splits its table by topic;
a term that fits no topic opens a new one, with its own heading and index entry.
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
    - [Names and terms](#names-and-terms)
    - [Headings](#headings)
    - [Phrases](#phrases)
    - [Row labels](#row-labels)
  - [What a failed binding costs](#what-a-failed-binding-costs)
  - [Settings](#settings)
  - [Console commands](#console-commands)
  - [The changelog](#the-changelog)
    - [Headings and entry wording](#headings-and-entry-wording)
    - [Code and tests](#code-and-tests)
    - [CI and git](#ci-and-git)
    - [Save Migration Tool](#save-migration-tool)
    - [Releases and badges](#releases-and-badges)
    - [Data files](#data-files)
    - [Localisation](#localisation)
    - [Text and rendering](#text-and-rendering)
    - [Campaign scripts](#campaign-scripts)
    - [Factions](#factions)
    - [System claims](#system-claims)
    - [Markets](#markets)
    - [Monthly report](#monthly-report)
    - [Salvage drops](#salvage-drops)
    - [Geometry](#geometry)
    - [Lists and sorting](#lists-and-sorting)
- [Forum terms](#forum-terms)
  - [The forum](#the-forum)
  - [Editions and translation](#editions-and-translation)
  - [Mods](#mods)
  - [Support replies](#support-replies)
- [Adding a string](#adding-a-string)
- [Translating the changelog](#translating-the-changelog)

## What this folder holds

| File | What it is |
| --- | --- |
| [CHANGELOG.md](CHANGELOG.md) | A full translation of the root [CHANGELOG.md](../../CHANGELOG.md): every version, every section. The Chinese zip ships it, and each release's notes show its section for the version. |
| [commands.csv](commands.csv) | The console commands' table: the help Console Commands prints for each `kmlib_` command. Only the `help` column is translated; the rest is what a player types and what the console runs. |
| [LunaSettings.csv](LunaSettings.csv) | KMLib's row on the LunaLib settings screen: the tab name, the setting's name and its description. |
| [mod_info.json](mod_info.json) | The launcher's mod list entry: `name` and `description` only, merged over the base. |
| [strings.json](strings.json) | Every string KMLib draws in game: the compatibility notice, the sentences its own failed bindings cost, the jump-point label, and what its console commands print. |

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
- **A reason gives the word's meaning.**
  A row's note says what the word means, or names its upstream source:
  vanilla's own text, a forum, another row of a reference.
  Text the mods ship - the settings, the strings, the changelog - follows the reference,
  so it is never the reason for a word:
  a wrong word that had shipped would otherwise keep itself.
  The build's `enforceNoForbiddenPhrases` fails a reference giving where a text has the word as its reason,
  over the phrases [forbid-cited-wording.gradle](../../gradle/tasks/checks/forbid-cited-wording.gradle) declares.
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
  following the core localisation's names (`通讯中继站 (隐藏)`):
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
- **Console text carries the English after it.**
  Console Commands draws in fonts of its own, which hold no Chinese, so a Chinese line prints as a row of `?`.
  Each console text is the Chinese,
  then a `-- English --` line,
  then the English text word for word:
  every help cell of `commands.csv`,
  and every `strings.json` string whose key starts with `console_`.
  The console prints the English until its fonts draw Chinese.
  The parity suite holds the English to the English bundle's,
  and the Chinese to its slots.
- **Defaults read `[默认值：X]`, on a line of their own.**
  `X` is the shipped value, spelled exactly as in the English file,
  except a Boolean, which reads `开启` for true and `关闭` for false.
  The default ends the description, after a line break and with no full stop, as `[Default: X]` does in English.
  A Radio's option translations are the one thing that follows it.
  A line break counts as whitespace, so the default highlights without padding.

## Vanilla terms

What the core localisation writes for each vanilla English term KMLib's text touches.
Each row was read off the core localisation itself,
by pairing its text with vanilla's at the same location:
the same `strings.json` key, the same CSV row, or the same string constant in the same class of the game JARs.
The core localisation at `startup-optimization` 2026.09.04 was the edition read;
the typeface editions share its text.
The Dialog, Dialog option, Entity, Fighter wing, Hull, Hullmod, Interaction, Screen, Target and Weapon rows were read off the installed `startup-optimization` edition,
which records no date of its own.

| English | 简体中文 | Source |
| --- | --- | --- |
| Administrator (of a colony) | 管理员 | `tooltips.json` `fleetScreen` `tooltipAdmin`: "Assign Administrator" is 分配管理员 |
| Alliance | 联盟 | vanilla's word for the Persean League, taken for Nexerelin's alliances |
| Attitude (the relation number) | 关系 | `starfarer_obf.jar` faction screen, "Attitude: " is 关系： |
| Blueprint | 蓝图 | `special_items.csv` `ship_bp`: "Base Ship Blueprint" is 基础舰船蓝图 |
| Cancel | 取消 | `starfarer.api.jar`, `SetFlagship`: the pair with Ok |
| Colony | 殖民地 | `starfarer_obf.jar` |
| Commodity | 商品 | `starfarer_obf.jar`, `CommodityPanel` |
| Credits | 星币 | `rules.csv` `oyaTanaicaAskSampleBack2`: "It's your credits." is 反正是你的星币 |
| Dialog (an interaction dialog) | 对话框 | `starfarer.api.jar`, `RuleBasedInteractionDialogPluginImpl`: the failsafe's "Exit dialog" is 退出对话框 |
| Dialog option | 选项 | `starfarer_obf.jar`, the story-point help: "dialog options" is 对话选项; the failsafe's "failsafe option" is 故障保护选项 |
| Entity | 实体 | `descriptions.csv` `test_station`: "a custom campaign entity" is 自定义实体 |
| Faction | 势力 | `starfarer.api.jar` |
| Fighter wing | 战机联队 | `strings.json` `chmWingPurchase_text`: "you just bought a new fighter wing" is 你购买了一个战机联队 |
| Fleet | 舰队 | `strings.json` `fleetInteractionDialog` `initialWithStationVsLargeFleet`: "your fleet" is 你的舰队 |
| Free Port (condition) | 自由港 | `market_conditions.csv` `free_market` |
| Gate | 星门 | `descriptions.csv`, Active Gate is 激活的星门 |
| Hidden | 隐藏 | `starfarer_obf.jar`; "Comm Relay (hidden)" is 通讯中继站 (隐藏) |
| Hull, (D) hull (a d-hull) | 船体, (D) 船体 | `tips.json`: "Ships with a (D) designation are substandard versions of the normal hulls." is 带有 (D) 标识的船体是性能低于正常状态的低质量船体 |
| Hullmod | 舰船插件 | `tooltips.json`: "Change the weapons and hullmods equipped on this ship." is 改变这艘船的武器与舰船插件; the codex's "Hull Modifications" is 舰船插件. A bare 插件 reads as a hullmod too: S-插件, D-插件, 本插件 |
| Hyperspace | 超空间 | `descriptions.csv` `jump_point_normal` |
| Interaction | 交互 | `starfarer.api.jar`, `RuleBasedInteractionDialogPluginImpl`: "any mission interaction in the current dialog" is 当前对话框中的某些任务交互 |
| Jump-point | 跳跃点 | `descriptions.csv` `jump_point_normal` is 跳跃点，前往超空间; `strings.json` `approach` is 驶向了跳跃点 |
| Market | 市场 | `starfarer_obf.jar` |
| Mod | Mod | `starfarer_obf.jar`, "Found mod: %s" is 发现 Mod：%s |
| ModSpec (the item) | 船体插件 | `special_items.csv` `modspec`: "Base ModSpec" is 基础船体插件; the hullmod it teaches is 舰船插件 |
| Ok (a dialog's confirm button) | 确认 | `starfarer.api.jar`, `SetFlagship`: the picker's `"Ok"` / `"Cancel"` pair is 确认 / 取消 |
| Outpost | 前哨站 | `market_conditions.csv` |
| Planet | 行星 | `rules.csv` `surveyPrintFullData`: "You have full survey data for this planet." is 你已获得该行星的完整调查数据。 |
| Population & Infrastructure (industry) | 居住区 & 基础设施 | `industries.csv` `population` |
| Screen, tab (of the core UI) | 面板 | `starfarer.api.jar`, the campaign tutorial: "open the intel screen" is 打开情报信息面板, "the character tab" is 角色面板. Vanilla shows no UI "panel" by that name; its English panels are physical ones |
| Save (a saved game) | 存档 | "Load last save" is 读取最近的存档 |
| Sector | 星域 | `descriptions.csv` `plasma`: "known to the Persean Sector" is 英仙座星域已知的 |
| Spaceport | 太空港 | `industries.csv` `spaceport` |
| Star system | 星系 | `starfarer.api.jar`; in tooltips as `Naraka 星系` |
| Survey data | 调查数据 | `descriptions.csv` `survey_data`: "a recent survey database" is 一个最新的调查数据库 |
| Target | 目标 | `descriptions.csv` `plasma`: "toward the target" is 射向目标 |
| Tariff | 关税 | `submarkets.csv` `open_market`: "subject to tariffs" is 可能有关税 |
| Weapon | 武器 | `tooltips.json`, as the Hullmod row: "the weapons and hullmods" is 武器与舰船插件 |

## KMLib terms

KMLib's own concepts, settled when the bundle was first written.

### The compatibility notice

The notice is composed from phrases, each a highlighted run the sentence around it must carry verbatim,
so each phrase is one row here and the sentences number their slots to put it where Chinese wants it.

#### Names and terms

| English | 简体中文 | Note |
| --- | --- | --- |
| integration | 集成 | |
| Klark Morrigan's Library (KMLib) (the mod's name) | Klark Morrigan 的程序库 (KMLib) | `KMLib` is the brand: kept in every locale, never translated |
| KM mods | KM 系列 Mod | every mod built on KMLib |
| public contracts | 公共契约 | |
| the shared library | 共享库 | KMLib as the notice and the settings row name it |

#### Headings

| English | 简体中文 | Note |
| --- | --- | --- |
| Error integrating X with Y | X 与 Y 集成出错 | the phrase 集成出错 closes the sentence rather than opening it |
| X ran into an error and switched one of its features off | X 运行出错，已关闭其中一项功能 | a feature failure's; 运行出错 is the phrase |

#### Phrases

| English | 简体中文 | Note |
| --- | --- | --- |
| a fault in X itself rather than a clash with another mod | X 自身的故障，并非与其他 Mod 的冲突 | |
| carries changes to public contracts | 更改了公共契约 | |
| depends on | 依赖于 | the sentence completes it with 这些契约 |
| either downgrade X to V or wait for a Y update | 将 X 降级到 V，或等待 Y 更新 | |
| it's new and carries changes to public contracts | 版本较新且更改了公共契约 | |
| needs to be updated to at least X | 需要更新到至少 X | |
| report this issue to the X developer | 将此问题报告给 X 的开发者 | |
| See X for more details | 详情请见 X | |
| too old | 版本过旧 | |

#### Row labels

| English | 简体中文 | Note |
| --- | --- | --- |
| Broken | 已损坏 | |
| Detected | 检测版本 | the version installed |
| Effect | 影响 | |
| Failed while | 失败于 | |
| Feature | 功能 | one of a mod's own features |
| Integration | 集成 | |
| Mod | Mod | |
| No effect | 不受影响 | |
| Targeted | 目标版本 | the version the build was type-checked against |
| unknown (a version nothing could read) | 未知 | |

The row labels are padded with ASCII spaces after the full-width colon,
so the values line up as they do in English.

### What a failed binding costs

| English | 简体中文 | Note |
| --- | --- | --- |
| and on your save | 以及你的存档 | closes every sentence stating what is unaffected |
| fleet | 舰队 | the [vanilla term](#vanilla-terms) |
| founded or handed over (colonies) | 建立或移交 | |
| read as cut off | 视为无法到达 | |
| routes (Random Assortment of Things' own) | 路径 | |
| the game's own rules | 游戏自身的规则 | |
| this session | 本次游戏期间 | opens every sentence stating a loss |

### Settings

| English | 简体中文 | Note |
| --- | --- | --- |
| Dev (tab) | 开发 | |
| Log verbosity | 日志详细程度 | verbosity alone is 详细程度 |
| OFF / ERROR / WARN / INFO / DEBUG / ALL (log levels) | 关闭 / 错误 / 警告 / 信息 / 调试 / 全部 | the usual logging words; OFF follows a switch's 关闭 |
| probes that read the game's UI by reflection | 通过反射读取游戏界面的探测器 | reflection is 反射, a probe 探测器 |
| settings slider | 设置滑块 | 设置, settings, with 滑块, a slider: the block slid along a track |

### Console commands

What KMLib's console commands print and their help says.
A term the console text shares with the changelog, such as a parameter, is under [Code and tests](#code-and-tests).

| English | 简体中文 | Note |
| --- | --- | --- |
| cut off (a system no jump point leads into), cut-off tag | 被截断, 被截断标签 | 截断, to cut off |
| discoverable (a place the player has still to find) | 可发现 | `SectorEntityToken.isDiscoverable()`: kept off the system map until the player's sensors find it |
| drift rate (an orbit's speed) | 漂移速率 | 漂移, drift, with 速率, rate |
| flag (an argument given as a bare keyword) | 开关参数 | 开关, a switch, with 参数: a parameter that is on by being given |
| game log | 游戏日志 | 游戏, the game, with 日志, a log (see the [log](#support-replies) row) |
| holdings (the places a faction holds) | 持有地 | 持有, to hold, with 地, a place |
| hyperspace entrance (of a jump point) | 超空间入口 | the vanilla Hyperspace row with 入口, entrance |
| intel entry | 情报条目 | 情报, vanilla's intel item, with 条目, an entry |
| map spoilers | 星图剧透 | vanilla's 星图 with 剧透, the usual word for a spoiler |
| named parameter (one given as `name=value`) | 具名参数 | 具名, named, the usual computing word |
| orbit tree, focus (what an orbit goes round) | 轨道树, 焦点 | 轨道, orbit, with 树, a tree; 焦点, a focus (see the [orbit-focus chain](#geometry) row) |
| stockpiling, accrued unrest (what a colony's old owner arranged) | 囤积, 累积的动荡 | plain words, unchecked against the game JAR |
| system center (a system's middle where no star sits) | 星系中心 | vanilla's 星系 with 中心, center; vanilla never names it. Not 质心, barycentre, which is true only where stars orbit it, and false for a starless system such as a nebula |
| territorial, non-territorial (a faction) | 领土, 非领土 | `territorial` in a faction's `punitiveExpeditionData`, read by `Misc.getClaimingFaction`: only a territorial faction claims a system dynamically. 非领土势力 in prose; 领土 is also the word for territory |
| timings (what a profiler measured) | 性能计时 | 性能, performance, with 计时, timing |
| unattributed (a faction no data file declares) | 来源不明 | 来源, source, with 不明, unknown |
| undiscovered | 未发现 | not yet found |
| unnamed | 未命名 | the usual word |

### The changelog

The headings every KM mod's translated changelog shares,
and the words its entries lean on most.
A version heading is never translated; see [translating the changelog](#translating-the-changelog).

#### Headings and entry wording

| English | 简体中文 | Note |
| --- | --- | --- |
| 2nd fix (another fix for a bug an earlier version claimed fixed) | 第二次修复 | 第二次, the second time, before 修复 as in the Fixed heading |
| Added | 新增 | |
| Changed | 变更 | |
| Changelog (the title) | 更新日志 | |
| Dependency changes | 依赖变更 | |
| Fixed | 修复 | |
| For developers | 面向开发者 | |
| Index | 索引 | |
| Minor optimisations | 小幅优化 | 小幅, slight, with 优化, the usual word for optimisation |
| No player-facing changes. | 没有面向玩家的变更。 | opens a version whose changes are all under `For developers` |
| Public contracts changed (**breaking**) | 公共契约变更（**破坏性**） | from 公共契约 |
| Reported by X at USC | 由 X 在 USC 报告 | closes the entry as a sentence of its own |
| Requested by X at USC | 由 X 在 USC 提出请求 | |
| Test fixtures | 测试夹具 | |
| Unreleased (index entry) | 未发布 | the index's link text only; the heading stays `## [Unreleased]` |

#### Code and tests

| English | 简体中文 | Note |
| --- | --- | --- |
| adapter | 适配器 | |
| animation, cycle (repetition nobody triggered) | 动画, 循环 | the usual computing words |
| binding | 绑定 | |
| blank (text holding only whitespace) | 空白 | |
| build (an instance of a class a data file names) | 构建 | the usual computing word |
| cache, memo (a value kept to save working it out again) | 缓存 | the usual computing word |
| caller | 调用方 | |
| class, syntax, tags, help (the columns of a console command table) | 类, 语法, 标签, 帮助文字 | the usual computing words; 帮助文字, help text, so the column reads as words rather than as the help command |
| class name | 类名 | 类, a class, with 名, name |
| command word (what a console command is typed as) | 指令名 | 指令, as for a console command, with 名, name; Console Commands' thread uses it in `help <指令名>` |
| compile | 编译 | the usual computing word |
| console command, console command table (a mod's `commands.csv`) | 控制台指令, 控制台指令表 | 指令, an instruction, the word [Console Commands' Fossic thread](https://www.fossic.org/thread-14602-1-1.html) uses throughout, never 命令: that thread is where a Chinese player installs the mod from, and it names the mod itself 控制台. 表, a table |
| constructor, no-arg constructor | 构造函数, 无参构造函数 | |
| consumer mod | 使用方 Mod | |
| control character | 控制字符 | the usual computing word |
| conventions (rules a family of builds share, such as the Starsector conventions) | 约定 | the plain word for what is agreed and held to |
| conversion (between units, such as fraction and percent) | 换算 | not 转换, which reads as a change of type or format |
| envelope (a value's rise and fall over time) | 包络 | the signal-processing word |
| event, unconsumed (an input event no panel has taken) | 事件, 未被消费的 | the usual UI words |
| exception, exception message, field (of a Java type) | 异常, 异常消息, 字段 | the usual computing words; 字段, a field (see the `mod_info.json` row) |
| extend (an interface extending another), implementer | 继承, 实现方 | the usual computing words; 方, a party to something, as in 调用方, the caller |
| final class (one nothing may extend) | 不可继承的类 | said plainly rather than left as the keyword |
| float (a Java `float`) | 浮点数 | the usual computing word |
| ideographic space (U+3000) | 全角空格 | what a Chinese typist calls it; not the literal 表意空格 |
| import, compose | 导入, 组合 | the usual computing words for a package naming another and for putting parts together |
| keyword (a word a console command accepts as an argument) | 关键词 | the usual computing word |
| kind (of a place or a record a reader branches on) | 种类 | the plain word for a sort of thing, kept apart from 类别, a category |
| library | 程序库 | |
| listener manager (the game's registry of event listeners) | 监听器管理器 | 监听器, the usual computing word for a listener, with 管理器, a manager |
| listener, saved or transient (registered on the sector) | 监听器, 存档的或临时的 | 临时, temporary, for one kept out of the save; 存档的 for one written into it |
| loader (code reading a data file in) | 加载器 | 加载, to load in, with 器, a thing that does a job |
| log line | 日志行 | 日志, a log, with 行, a line |
| map (a Java `Map`), list, record | 映射, 列表, 记录 | the usual computing words: 映射, a mapping; 列表, a list; 记录, a record |
| method handle | 方法句柄 | |
| named group (of a regex) | 命名分组 | the usual computing word |
| overload (one of several same-named methods) | 重载 | the usual computing word |
| parameter, positional parameter, parameter spec (of a console command) | 参数, 位置参数, 参数规范 | the usual computing words; 位置, position, for one filled by where it stands |
| parser, value parser | 解析器, 值解析器 | the usual computing words |
| per-frame (run once every frame) | 逐帧 | 逐, one after another, with 帧, a frame |
| phrase (a run of words a rule looks for) | 短语 | the plain word for a run of words |
| profiler, capture (what a profiler has timed) | 性能分析器, 采集结果 | the usual computing words |
| reader, source (a port opening readers) | 读取器, 来源 | 读取, to read, with 器, a thing that does a job; 来源, where something comes from |
| record (a Java value type) | 记录类型 | 记录 with 类型, a type: the type itself, where 记录 is one value of it |
| reflection | 反射 | 反射, to reflect back: the usual computing word for code reading its own types |
| regex, match (a name a regex accepts) | 正则表达式, 匹配 | the usual computing words; 匹配, to match |
| register (a store of records kept under one key) | 登记表 | from 登记, to record, with 表, a table |
| relay (a listener passing events on) | 转发器 | 转发, to pass on |
| revision (a number that moves when content changes) | 修订号 | 修订, a revision, with 号, its number |
| sampling (one read of a live value) | 采样 | the usual word for taking one reading of something that changes |
| script (a PowerShell file run from a terminal) | 脚本 | the usual computing word |
| script (a class the game builds from a name a data file gives, or one run every frame on a sector) | 脚本 | the usual computing word; one word for both, as the game has one for both |
| seam (an interface one part answers another through) | 接缝 | where two parts are joined |
| stand-in (a test double) | 替身 | |
| static initialiser (code a class runs once, as it is first loaded) | 静态初始化器 | 静态, static, with 初始化器, an initialiser: the Java Language Specification's Chinese name for it |
| standalone (a parameter that is a whole invocation by itself) | 本身即构成一次完整调用 | written out as a clause; no single word says it |
| string ID, key class | 字符串 ID, 键类 | 字符串, the computing word for a string; a key class is the class holding one reader's string IDs |
| strings category (the namespace in strings.json) | 字符串类别 | 字符串 before 类别, a category |
| supplier (a function a caller hands in) | 供应器 | |
| test suite, fake, fixture | 测试套件, 伪实现, 夹具 | |
| told (a listener hearing of an event) | 得到通知 | 得到, to receive, with 通知, a notice |
| trigger (a moment that sets an animation off) | 触发 | the usual word for setting something off |
| type (the class or interface code holds a value as) | 类型 | the usual computing word |
| usage line (the grammar a console command prints when mistyped) | 用法行 | 用法, usage, with 行, a line |
| word (a run of text between whitespace), part into words | 词, 划分词 | 词, a word, as in 关键词, a keyword; 划分, to divide, before 词 |
| wording (the words a text puts something in) | 措辞 | the plain word for how something is put into words |

#### CI and git

| English | 简体中文 | Note |
| --- | --- | --- |
| action (what a workflow step runs) | 操作 | GitHub's own Chinese documentation word |
| branch (of a git repository) | 分支 | the usual git word |
| checkout (a repository's working copy) | 检出副本 | 检出, git's own word for checking out, with 副本, a copy |
| gate (a CI check a change must pass) | 门禁 | 门, a door, with 禁, to bar: what a change has to pass to get in |
| lint (a YAML, shell or PowerShell check) | 检查 | a tool's name, such as yamllint or Pester, stays in English |
| output (a value one step hands the next) | 输出 | 输出, to put out: GitHub's Chinese documentation word for it |
| pull request | 拉取请求 | GitHub's own Chinese interface word |
| reusable workflow | 可复用工作流 | GitHub's own Chinese documentation words |
| runner (the machine a CI job runs on) | 运行器 | GitHub's own Chinese documentation word |
| step (of a workflow job) | 步骤 | GitHub's own Chinese documentation word |

#### Save Migration Tool

| English | 简体中文 | Note |
| --- | --- | --- |
| Save Migration Tool (SMT) | 存档迁移工具（SMT） | 迁移, to migrate, for a save carried across mod versions; the abbreviation stays in English |
| load (of a save), failed load, retry (vanilla loading the save again from another file) | 读取, 读取失败, 重试 | 读取 as the vanilla Save row's 读取最近的存档; 重试, the usual computing word |
| newest first (the order a list runs in) | 最新的在前 | 最新, newest, with 在前, in front |
| missing content (an ID a save holds and no installed mod defines) | 缺失内容 | 缺失, missing, with 内容, content |
| base hull (the hull a (D) hull is built from) | 基础船体 | 基础 as the vanilla Blueprint row's 基础舰船蓝图, with the vanilla Hull row's 船体 |
| variant (a ship's saved loadout), spec (one entry of a spec store) | 变体, 规格 | the usual words, unchecked against the game JAR, which shows a player neither; 规格 kept apart from 规范, a parameter spec's rules |
| object chain (the objects a failed object sat in, innermost first) | 对象链 | 对象, object, as the JSON row's 对象数组, with 链, chain |
| JAR, declared JAR (one a mod's `mod_info.json` lists) | JAR, 声明的 JAR | JAR stays in English, as the game JARs' names do in this file; 声明, the usual computing word for declaring |
| load (a mod or file the game reads in), enabled (a mod) | 加载, 已启用 | 加载, to load in (see the failed load row, of a font), kept apart from a save's 读取; 已, already, with 启用, to put into use |
| cause (the exception a failed load hit) | 原因 | the plain word |
| diagnose (a save, end to end) | 诊断 | the usual word for finding what is wrong |
| descriptor (a save's `descriptor.xml`) | 描述文件 | 描述, to describe, with 文件, file: the small file that describes the save |
| mod class (a class a mod's JAR holds) | Mod 类 | Mod stays in Latin (see the [mod](#mods) row); 类, a Java class |
| narrow (a list, by a filter) | 缩小 | to make smaller, as a filter makes a list |
| text for a person (rather than data for a caller) | 供人阅读的文本 | 供...阅读, for ... to read; kept apart from the JSON a caller parses |
| console (the terminal a script runs in) | 终端 | the terminal itself; never 控制台, which the [console command](#console-commands) rows keep for the Console Commands mod |
| colour (text, by the kind of each value), plain (with no colour) | 着色, 不带颜色 | 着色, to apply colour; 不带, without, before 颜色, colour (see the colour rows of [Text and rendering](#text-and-rendering)) |

#### Releases and badges

| English | 简体中文 | Note |
| --- | --- | --- |
| download count | 下载量 | |
| endpoint file (the data a shields badge reads) | 端点文件 | 端点, endpoint, as shields names the service; shields itself stays in English |
| package (build the release zip) | 打包 | the usual word |
| poll (an update checker asking for a version file) | 轮询 | the usual computing word |
| release (a published version) | 发布版本 | |
| release notes | 发布说明 | |
| release zip | 压缩包 | |
| update checker | 更新检查器 | |
| version file | 版本文件 | |

#### Data files

| English | 简体中文 | Note |
| --- | --- | --- |
| data file | 数据文件 | a CSV or JSON a mod ships |
| field, member (of `mod_info.json`) | 字段, 成员 | 字段 for what a player reads; 成员 for a JSON object's member, the usual computing word |
| object-holding array (in JSON) | 对象数组 | 对象, object, before 数组, array: an array whose elements are objects |
| root, nested record (of a JSON file) | 根层级, 嵌套的记录 | 层级, level, so 根 reads as the file's top level rather than a root directory |

#### Localisation

| English | 简体中文 | Note |
| --- | --- | --- |
| bundle | 语言包 | |
| core localisation | 核心本地化 | the Chinese localisation as a project is 中文本地化 |
| English copy (the English a console text carries after its translation) | 英文副本 | 英文, English, with 副本, a copy, as in 检出副本, a checkout |
| locale | 语言区域 | |
| locale tag (such as `zh-hans`) | 语言区域标签 | 标签, tag |
| translation (the translated text of a string) | 译文 | 译, translated, with 文, text; kept apart from 汉化, a translated build |

#### Text and rendering

| English | 简体中文 | Note |
| --- | --- | --- |
| allocate (memory, per call) | 分配内存 | the usual computing words |
| body colour (of text) | 正文颜色 | |
| colour channel (one of red, green, blue and alpha) | 颜色通道 | the usual graphics word; 通道 alone in 渲染通道 is a pass, so the colour stays named |
| eased ramp, turn (of a ramp heading the other way) | 缓动过渡, 转向 | 缓动, easing, with 过渡, transition |
| embedded map, map render hook | 嵌入星图, 星图渲染钩子 | vanilla's 星图 with 嵌入, embedded, and 钩子, hook |
| digits (of a figure) | 位数 | how many digits a figure carries, as in 保留全部位数, every digit kept; one digit on its own is 数字 |
| failed load (of a font) | 加载失败 | the noun form of 无法加载 |
| font atlas, face, cut | 字体图集, 字体, 字号 | a cut is one size of a face |
| hatch, stroke, spacing, pattern | 斜线, 描边, 间距, 图样 | 斜线, a slanted line; 描边, to stroke an edge; 间距, the distance between; 图样, a pattern |
| fraction (a share in `[0, 1]`, such as `0.20` for 20%) | 比例 | not 分数, which reads as a ratio of two integers such as 1/5 |
| highlight, highlighted run | 高亮, 高亮片段 | a run is the span of text one highlight tints |
| highlight colour | 高亮颜色 | |
| line height, glyph, fallback glyph | 行高, 字形, 后备字形 | |
| pad (the gap a tooltip row is laid with), tooltip | 间距, 提示框 | 间距, the distance between (see the hatch row); 提示框, a prompt box |
| map label (a name drawn on the map) | 星图标签 | vanilla's 星图 with 标签, label |
| modal panel, host screen, claim (input) | 面板, 宿主屏幕, 占用 | 面板, a panel; 宿主, host; 占用, to take up |
| dialog (one the game raises), prompt (a confirmation a core screen raises) | 对话框, 提示窗口 | 提示, a prompt, with 窗口, a window; 对话框, vanilla's word (see the vanilla Dialog row) |
| interaction dialog, option (in one), visual panel | 交互对话框, 选项, 视觉面板 | the vanilla Interaction and Dialog rows joined; 选项 as the vanilla Dialog option row; 视觉, visual, with 面板 as vanilla names its screens, the visual panel having no name a player sees |
| text panel (the column of paragraphs in an interaction dialog) | 文本面板 | 文本, text, with 面板, a panel (see the visual panel row), the text panel having no name a player sees |
| balance (the credits a player holds) | 余额 | the usual word for what an account holds |
| interaction target (the entity a dialog is open on) | 交互目标 | the vanilla Interaction and Target rows joined; kept apart from 目标版本, a targeted version |
| plugin (the class the game runs a feature through), hand off (a dialog to a plugin), start (a plugin) | 插件, 移交, 启动 | 插件, something plugged in. Vanilla shows players no plugin in this sense, and reads a bare 插件 as a hullmod (see the Hullmod row), so an entry names the kind where it is not plain: 对话框插件, 插件类. 移交, to transfer; 启动 as vanilla starts the game, a system or an ability |
| fade in and out, pace (of a fade) | 淡入淡出, 节奏 | 淡入淡出, to fade in and fade out; 节奏, rhythm |
| close key (a key that closes a panel) | 关闭键 | 关闭, close, with 键, key |
| public API | 公共 API | 公共, public, as in 公共契约, a public contract; API stays in Latin letters |
| paragraph | 段落 | |
| renderer, render pass | 渲染器, 渲染通道 | |
| round, truncate (a figure) | 四舍五入, 截断 | 四舍五入, four down and five up, is rounding half up, the rule `Math.round` follows; 截断, cut off, drops the fraction outright |
| row (one line of a list or hover box) | 行 | 行, a line, as in 日志行, a log line |
| scientific notation, trailing zero (of a figure) | 科学计数法, 末尾零 | |
| slot (in a format string) | 槽位 | |
| solid filled rectangle (a glyph drawn without its atlas) | 实心矩形 | 实心, filled, with 矩形, rectangle |
| stack (of a call) | 调用栈 | the usual computing word |
| whole percent | 整数百分比 | 整数, integer, before 百分比, percent, which alone says nothing about decimals |

#### Campaign scripts

| English | 简体中文 | Note |
| --- | --- | --- |
| advance (one step of a script) | 推进 | 推进, to push forward |
| campaign message (a `MessageIntel` notification) | 战役消息 | 战役, campaign, as in 战役加速, campaign speed-up, with 消息, message |
| campaign speed-up | 战役加速, 加速 for short | settled in the `0.5.0` entries; vanilla's own "Speeding up time" sits in the game JAR, which the cached core localisation does not hold, so it is unchecked |
| lift (an icon moved past the map's nebulae) | 抬升 | 抬升, to raise up |
| location (what holds entities: a star system or hyperspace) | 位置 | 位置, a position, the place something is at |
| placement (of a map icon among the others), unplaceable | 叠放情况, 无法定位 | 叠放, stacking, as the `0.5.0` entry's 重新叠放; 定位, to locate, kept apart from 位置 for a location |
| put back (an entity taken out of its location) | 放回 | the counterpart of 移出 |
| reading (what one advance found and ordered) | 读数 | 读数, a reading, what a measurement shows |
| reseat (the removal and put-back a lift is made of) | 重新安置 | 重新, again, with 安置, to settle in place |
| wait (an advance spent with the entity out) | 等待 | the plain word |
| walk (a read over every item of a set), sector walk counters | 遍历, 星域遍历计数 | 遍历, to go over every item; 星域, the sector; 计数, count |

#### Factions

| English | 简体中文 | Note |
| --- | --- | --- |
| alliance lead | 盟主 | the alliance's leading member |
| change hands (a colony passing to another owner) | 易手 | the plain word, kept apart from 移交, a hand-over a caller makes |
| friendly, neutral (a disposition between two factions), set (of factions) | 友好, 中立, 组 | vanilla's words for the reputation levels; 一组势力 for a set of factions |
| long name (of a faction) | 长名称 | 长, long, before 名称, name |
| teach (what an item gives a faction to know), know | 传授, 掌握 | |

#### System claims

| English | 简体中文 | Note |
| --- | --- | --- |
| claim, claimant (the faction claiming a system) | 宣称, 宣称方 | KMU's words, from vanilla's 无宣称领土; 方 for the party holding it |
| decree (a claim the system's memory flag imposes) | 无条件宣称 | KMU's word for the `$claimingFaction` flag, which wins over any scored claim |
| contest, standing (one faction's place in it), score | 争夺, 排名, 得分 | 争夺, to contend for; 得分, a score; 排名, ranking, for a place the scores order |
| breakdown (the whole scored contest behind a claim) | 明细 | the itemised account, as a bill's 明细 lists its lines |
| fill (a map cell's paint), hover box | 填充, 悬停框 | 填充, a fill; 悬停框, a box raised on hover, kept apart from 提示框, a tooltip |
| index (of one pass's walk), memory read (of a flag) | 索引, 记忆读取 | the usual computing word; 记忆, memory, the API's `MemoryAPI` read literally, since no player ever sees the term |

#### Markets

| English | 简体中文 | Note |
| --- | --- | --- |
| submarket (one of a market's trading counters), cargo | 子市场, 货物 | the game never names the kind to a player, only each counter (`submarkets.csv` names 开放市场 and 仓库), so 子市场 follows the API's `SubmarketAPI`; 货物, goods, vanilla's word in that file's 仓库 description |
| storage, one-time access fee, paid to unlock | 仓库, 一次性使用费, 已付费解锁 | 仓库 and 一次性使用费, vanilla's words in its `StoragePlugin` dialog (使用此地仓库需要缴纳一次性使用费); 已付费解锁 is the plain reading of `setPlayerPaidToUnlock`, which the game never shows |
| buy, sell (the player's side of a trade), unit price | 买入, 卖出, 每单位价格 | vanilla's words in the trade screen's cargo tooltips (每单位买入的价格, 每单位价格是基于一次性买入或卖出) |
| average price, price modifier | 平均价格, 价格修正 | plain words; 修正 for a modifier is unchecked against the game JAR |

#### Monthly report

| English | 简体中文 | Note |
| --- | --- | --- |
| monthly report (the income and expenses the game totals and pays each month), income, expenses | 月度收支报告, 收益, 支出 | 收益 and 支出, vanilla's words in its month-end message (每月收益, 每月支出, `CoreScript`); the report's own title sits in the game JAR and is unchecked |
| row (one line of the report), fleet (its parent line) | 条目, 舰队 | 舰队 as `CoreScript` names the fleet line; 条目, an entry, for a line under it |
| economy step (one of the month's economy iterations), month end | 经济步进, 月末 | 步进, a step, as a script's 推进 is an advance; the game never names the step to a player |

#### Salvage drops

| English | 简体中文 | Note |
| --- | --- | --- |
| drop roller, drop list | 掉落抽取器, 掉落列表 | 掉落, drop, the plain word, unchecked against the game JAR; 抽取器, a thing that draws; 列表, a list |
| random drop, value drop, drop group | 随机掉落, 价值掉落, 掉落组 | from 掉落 |
| roll (one draw from the roller) | 抽取 | 抽取, to draw one out |
| weight (of a random drop), multiplier, neutral (a multiplier of 1) | 权重, 倍率, 中性 | the usual words |

#### Geometry

| English | 简体中文 | Note |
| --- | --- | --- |
| arc length, path (a ring walked from one start), stretch (a piece of a path), span (one piece of a layout laid along a path) | 弧长, 路径, 路段, 跨度 | 弧长, the usual geometry word; 路段, a stretch of a road, so it reads as part of the 路径; 跨度, the extent a piece reaches, kept apart from 区段, a scrolling section |
| band (a strip either side of a centreline), centreline, girth, lane, divider | 带状区域, 中心线, 宽度, 分道, 分隔线 | 带状, strip-shaped, with 区域; 分道, a lane as a road is split into them; 分隔线, a dividing line |
| body (an entity in orbit), orbit-focus chain | 天体, 轨道焦点链 | 焦点, focus, with 链, chain |
| body (the main part of a shape), arm (a thin part reaching out of it) | 主体, 伸出部分 | 主体, the main part, kept apart from 天体, a body in orbit; 伸出部分, a part that reaches out |
| corner (where a polyline turns), polyline | 拐角, 折线 | 拐角, a turn of a road; 折线, the usual geometry word for a line of joined segments |
| directed line, even-odd count | 有向直线, 奇偶计数 | the usual geometry words |
| extent (one painted shape), outline | 范围, 轮廓 | |
| hash grid, cell, bounds (a bounding box) | 哈希网格, 单元, 边界框 | 哈希, hash, with 网格, a grid; 单元, a unit, for a cell; 边界框, a bounding box |
| inset (a ring moved inward), miter (a corner joined by extending its two edges), fold (a loop where an inset crosses itself) | 内缩, 斜接, 折叠 | 内缩, drawing inward; 斜接, the usual word for a miter joint; 折叠, folding over, since the loop runs back against the rest of the ring |
| normal (the unit vector perpendicular to a segment), direction of travel, bearing, radian (the unit a bearing is given in) | 单位法向量, 行进方向, 方位, 弧度 | 法向量, the usual geometry word; 行进, moving forward, for the way a segment runs from start to end; 弧度, the usual word for the unit |
| offset (a segment moved sideways, parallel to itself) | 侧向平移 | 平移, moving a shape without turning it, the usual geometry word; 侧向, sideways |
| region (the area a set of rings bounds) | 区域 | the usual geometry word; 语言区域 keeps it for a locale |
| ring, enclosing loop | 环, 外围环 | 外围, surrounding, before 环 |
| segment (a line between two ends), centre (its middle), endpoint (one of its ends) | 线段, 中心, 端点 | the usual geometry words; 端点 is the same word as the endpoint file's, the two never sharing a sentence |
| stride, triangle list | 步长, 三角形列表 | the usual words |
| winding (which way round a ring runs), counter-clockwise, net area (the area left once loops wound opposite ways cancel) | 绕行方向, 逆时针, 净面积 | 绕行, going round, with 方向, direction; 净, net, as in 净值 |
| window (how many edges apart two edges may be and still be compared), edge | 窗口, 边 | the usual words |

#### Lists and sorting

| English | 简体中文 | Note |
| --- | --- | --- |
| sort mode, metric, ranking | 排序模式, 指标, 排名 | the usual words |
| tie, break a tie | 平局, 打破平局 | |

## Forum terms

The words of a KM mod's thread on [Fossic](https://www.fossic.org/), the Chinese Starsector forum,
and of its release notes.
How such a thread works is [docs/dev/fossic-thread.md](../../docs/dev/fossic-thread.md).

### The forum

| English | 简体中文 | Note |
| --- | --- | --- |
| attachment | 附件 | a file uploaded to a forum post |
| download panel | 下载面板 | the posting form's word, from its 插入Mod文件下载面板到帖子正文中 button |
| fast review (an admin review asked for straight after posting) | 快速审核 | [the review rules'](https://www.fossic.org/thread-6107-1-1.html) word, for a lightweight mod |
| forum thread | 论坛帖子, 帖子 for short; 本帖 for this thread | 帖子, a post or thread, the forum's own word, on its 发表帖子 button |
| Fossic (the forum) | Fossic, with 远行星号中文论坛 beside it | the forum's own site name, "the Starsector Chinese forum", for a reader who knows it by that |
| mod index (Fossic's list of reviewed mods) | Mod索引 | the forum's own name, spelled with no space |
| mod release (a thread tag) | mod发布 | the one tag Fossic's mod threads share; tags there are free text, so a thread's others are the words a player would search for |
| OP (a thread's first post) | 主楼 | the players' word on Chinese forums, where replies are numbered floors |
| original mod board (where a reviewed thread moves) | 原创Mod区 | the board's own name on the forum, spelled with no space |
| pre-release | 预发布 | Fossic's word for its pre-release board |
| private message | 私信 | the usual forum word, for messaging one user directly |
| Q&A (a post's section of player questions and answers) | 常见问题; 问 and 答 open each question and answer | the usual heading for that section of a mod post; 问答 reads as a quiz or an interview |
| repost | 搬运 | a Fossic board, for mods posted by someone other than their author |
| review (an admin's check before a thread is listed) | 审核 | the review rules' word, in [Mod审核规定](https://www.fossic.org/thread-6107-1-1.html) |
| testing (a title tag) | 测试 | how Fossic titles mark a pre-release |
| translation (a translated build) | 汉化 | a Fossic board |

### Editions and translation

| English | 简体中文 | Note |
| --- | --- | --- |
| English edition | 英文版 | the counterpart of 简体中文版 |
| rationale (for a term's choice) | 选词理由 | 选词, choosing a word, with 理由, reason: what each row's note records |
| Simplified Chinese edition | 简体中文版 | the usual way a Chinese edition is named |
| terminology reference (a bundle's `README.md`) | 术语参考 | 术语, terminology, with 参考, a reference |
| the Chinese core localisation | 远行星号中文汉化, 汉化包 for short | the name of its Fossic thread |
| translation (the act) | 翻译 | the plain word; 汉化 is a translated build, the forum's sense |
| translation reference (the terminology reference, as a post names it) | 翻译参考 | 参考, reference, after 翻译, translation |

### Mods

| English | 简体中文 | Note |
| --- | --- | --- |
| changelog | 更新日志 | [KMLib's](https://github.com/Klark-Morrigan/Starsector-Mod-KMLib/blob/master/localisation/zh-hans/README.md#the-changelog) |
| dependency (a required mod) | 前置 | Fossic's word; its posting form says 依赖Mod |
| install mid-run | 中途安装 | 中途, partway through |
| mod author | Mod 作者 | Mod kept Latin as vanilla does, with 作者, author; the posting form spells it Mod作者 |
| mod installer (a third-party tool) | Mod 安装工具 | 安装, install, with 工具, tool; Mod kept Latin as in Mod 作者 |
| uninstall, uninstall procedure | 卸载, 卸载步骤 | the usual software word; 步骤 for the steps |
| vanilla (the base game) | 原版 | the players' word; the game leaves "vanilla" in English |
| zip (a release archive) | 压缩包 | the plain word for an archive file |

### Support replies

The words of a reply to a player's crash or bug report in the thread.

| English | 简体中文 | Note |
| --- | --- | --- |
| code (source code) | 代码 | the usual computing word |
| crash (the game closing on an error) | 崩溃 | the usual software word |
| fallback value (what a read uses when its key is missing) | 回退值 | 回退, fall back, with 值, value |
| log (a log file such as `starsector.log`) | 日志 | the usual word; 日志行 and 日志详细程度 build on it |
| memory (RAM), out of memory, ask for memory | 内存, 内存不足, 申请内存 | 不足, insufficient; 申请, request, the usual word for an allocation |
| mod list (the mods a player has enabled) | Mod 列表 | Mod kept Latin as in Mod 作者, with 列表, list |
| pop-up (an error dialog) | 弹窗 | the players' word for a dialog that pops up |
| setting (one entry on a settings screen) | 设置项 | 项, item, after 设置; 设置 alone is the settings as a whole |

## Adding a string

1. Look the term up here.
   A vanilla concept missing here is looked up in the core localisation before it is translated:
   find the English in vanilla's `data/` or game JARs, and read what the core localisation has at the same place.
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
