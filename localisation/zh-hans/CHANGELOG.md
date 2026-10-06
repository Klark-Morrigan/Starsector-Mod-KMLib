# 更新日志

KMLib 的所有重要变更都记录在此。格式遵循 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)，项目遵守[语义化版本](https://semver.org/)。KMLib 及其使用方 Mod 的版本号递增条件定义在 [docs/dev/versioning.md](../../docs/dev/versioning.md) 中。

可复用的发布工作流会把与所发布版本相匹配的章节提取到 GitHub 发布说明中，因此每个已发布的版本都必须在此有对应章节。

## 索引

- [未发布](#unreleased)
- [0.5.0](#050---2026-10-05)
- [0.4.0](#040---2026-09-15)
- [0.3.1](#031---2026-09-15)
- [0.3.0](#030---2026-09-15)
- [0.2.0](#020---2026-09-14)
- [0.1.0](#010---2026-09-14)

## [Unreleased]

### 修复

- **第三方 Mod 安装工具会将 KMLib 识别为 KMLib 本身，而不是 LazyLib。** `mod_info.json` 先列出 KMLib 自身的字段，再列出其前置，因此读取文件中第一个 `id` 的安装工具会找到 KMLib 自己的那个。
### 面向开发者

<details>
<summary>API、构建工具与测试夹具</summary>

#### 修复

- **`writeLocaleFiles` 将 `mod_info.json` 中含有对象的成员写在最后**，排在其余已排序的成员之后。

#### 变更

- **`mod-release.yml` 在打包前会重新运行每个拉取请求门禁**：`ci-yaml` 与 `ci-bash`、`ci-gradle` 一同运行，因此会让拉取请求失败的 YAML 检查问题同样会让发布失败。
- **`mod-release.yml` 使用 Mod 运行器上的 JDK 打包**，即 `ci-gradle` 运行测试所用的 JDK，而不是在每次发布时下载一个 Temurin JDK。

</details>

## [0.5.0] - 2026-10-05

### 修复

- **Fast Rendering `0.9.0` 及更高版本下的崩溃。** 感谢 **Genir**，[Fast Rendering 已实现缺失的 OpenGL 方法](https://github.com/Halke1986/starsector-render/issues/11)，即未安装 Fast Rendering 时由游戏自身应答的那个方法。在 Fast Rendering 下，星图只在 `v0.9.1rc1` 及更高版本中跟随光标。
- **开启战役加速时，通过 KMLib 绘制的星图图层会留在星云之上。** 每次打开星图，所有图层都停留在星云之下，重新读取存档或切换星景都无法恢复。由 **MiniRockytheOracle** 在 [**USC**](https://discord.com/channels/187635036525166592/1549091275167240272/1551829173037957170) 报告。
  - **已放弃的图层修正会在下次打开星图时重新尝试**，而不是在本次会话的剩余时间内一直停用。
- **写在按钮文字之后的快捷键会留在按钮上。** 在文字中不含该按键字母的译文中，或在英文中改绑按键之后，形如 `[M]` 的按键会换行到按钮之外的第二行。

### 新增

- **简体中文。** 第二个压缩包 `KMLib-<version>-zh-hans.zip` 中的兼容性通知、设置标签页以及 Mod 列表条目均为简体中文。请先将[中文本地化](https://github.com/TruthOriginem/Starsector-Localization-CN)覆盖到 `starsector-core` 上：游戏自身的字体不含中文字符，没有它，所有中文字符都会绘制为 `?`。日志详细程度设置的选项保留英文，因此该设置在两个压缩包之间可以沿用。
- **当使用 KMLib 的 Mod 无法与另一个 Mod、Fast Rendering 或游戏本身集成时，游戏内会弹出通知。** 通知会写明是哪个 Mod、两个版本、什么停止工作、什么不受影响，以及应当更新、降级还是等待。这类失败只会损失基于该集成构建的功能，并且每个失败在本次游戏期间只显示一次。
  - **该 Mod 自身的某项功能出错并被关闭时，也会以同样方式报告**，并请玩家向该 Mod 的开发者报告此问题。
  - **星图打开时发现的失败可以直接显示在星图之上**，无需等到回到战役界面。

### 面向开发者

<details>
<summary>API、构建工具与测试夹具</summary>

#### 修复

- **Fast Rendering 崩溃：** 星图的模型视图矩阵通过 `glGetFloat(GL_MODELVIEW_MATRIX)` 读取，`v0.9.1rc1` 会就地应答这一调用。KMLib 不针对 `fr.jar` 的任何部分编译，因此 Fast Rendering 的内部重构不会破坏这次读取。
- **加速下的星图图层：** `MapIconReseater` 只在控件已丢弃图标后才将其放回，最多等待 `MAX_ADVANCES_DETACHED` 次推进，并在星图关闭时结束。加速会让脚本每帧推进多次，因此移除与放回落在同一帧内，控件从未重新叠放该图标。
- **按钮上的快捷键：** `VanillaButtonLabel.announceShortcut()` 会把标签加宽到其所承载的文字。游戏只按创建时的文字设定一次标签宽度，更长的内容会换行。
- **`GlRuns`、`GlLines`、`GlQuads` 和 `GlTriangles` 在 `finally` 块中调用 `glEnd`。** 抛出异常的绘制会让管线停留在 `glBegin` 内部，破坏其后绘制的所有内容的 GL 状态。
- **抛出异常的 `LunaSettingsReader.runOnSettingsChange` 回调会以 ERROR 级别连同堆栈跟踪记录。** LunaLib 只在 DEBUG 级别提及它，且不附堆栈跟踪。下一次变更照常尝试，日志级别绑定也使用同一中转机制。
- **对游戏界面的失败访问会在其边界处被捕获**，而不会终结这一帧。两种失败方式都会被捕获：以未声明方式抛出的游戏自身失败，以及游戏新版本移除的成员引发的 `LinkageError`。
  - `ShownMapTab.isMapTabShowing()`：通知面板和覆盖在星图之上的任何面板共同使用的界面检查。
  - `CoreUiTree.readHopIfOffered`：在返回 null 的那个 catch 内部获取方法名。
  - `CampaignMapView`：判定星图未显示，并发出一条警告。
  - `VanillaIntelScreenView`：情报面板的淡入淡出器无法链接时，返回无遮罩。
- **开启诊断会重置每个针对访问游戏控件的类级警告**：除星图探测器的警告外，还包括筛选栏控件、其标签以及 `CampaignMapView` 的警告。
- **`HighlightedParagraph` 按片段在文本中出现的顺序将其交给游戏**，因此译文可以按任意顺序为槽位编号。被提到两次的片段每次取其下一次出现，文本中没有的片段排在最后。

#### 新增

##### 兼容性

游戏内通知背后的 API。[`starsector/compatibility/`](../../src/main/java/kmlib/starsector/compatibility/README.md) 阐述了其工作方式以及为何如此设计。

- **`CompatibilityFailures`**：本次会话的失败记录，按第三方、使用方 Mod 和功能损失句子锁存。可从任意线程写入，并由报告器逐个取出失败。
  - `CompatibilityFailures.SESSION_RECORD`：每个绑定都写入的那一份记录。`KMLib_ModPlugin` 安装取出它的通知。
  - `recordFeatureFailureOnce()`：将 Mod 自身出错的功能记录为 `FeatureFailure`。
  - 一个 Mod 若为两个功能使用同一个功能键，两个功能都会被报告，第二个以 `<feature>-2` 报告。
- **`ReportedFailure`**：通知所绘制的密封类型。
  - `CompatibilityFailure`：对第三方的一个已失效的绑定。它以 `CompatibilityNoticeLine` 的形式组合通知的标题、诊断、各行内容和结束语。
  - `FeatureFailure`：Mod 自身抛出异常并被关闭的功能。
- **报告的组成部分**：
  - `CompatibilitySubject`：第三方，及其目标版本、已安装版本和两者的比较结果。
  - `CompatibilityBreakage`：哪个守卫捕获了绑定，以及什么不再成立。
  - `CompatibilityConsumer`：采用绑定的 Mod、该绑定服务的功能，以及说明它损失了什么、没有损失什么的句子。`resolveConsumerAtPosition()` 为重复使用的功能键编号。
- **启动接线**：
  - `WiringSteps`：在守卫之后运行单个启动步骤，位于 `starsector/startup/`。抛出异常或无法链接的步骤只损失它自己的注册。
  - `ModIntegration`：步骤所绑定的第三方 Mod，以及缺少它时执行接线的 Mod 所损失的内容。
  - `KMLib_ModPlugin` 以 KMLib 自己的 Mod ID，通过该渠道报告其 LunaLib、Nexerelin 和 Random Assortment of Things 接线。
- **首次调用时才失效的集成**：可选 Mod 的适配器若在建立殖民地、移交、计数器决策、可达性读取或联盟读取时失败，会在本次游戏期间被移除并报告一次。
  - `IntegrationFailureReporter`：无论由哪个边界捕获，都登记该失败，且绝不抛出异常。
  - `ExtensionPoint.offerWork()`：在该边界内将工作交给已安装的实现。
  - `describeIntegration()`（位于 `NexerelinIntegration` 和 `RandomAssortmentOfThingsIntegration` 上）以及 `KmlibLunaSettings.describeLunaLibIntegration()`：每个集成的报告，与绑定它的代码放在一起。
- **对游戏自身代码的访问**：星图、情报界面、筛选栏、叠加层、数据百科、提示框和图标顺序背后的控件遍历与类读取，会以游戏作为第三方，通过因此受损的 Mod 报告。
  - `GameReachReporter`：单个 Mod 的报告器。`UNREPORTED` 不登记任何内容，用于诊断性读取。
  - `InstalledMods.readModGameVersion()`：某个 Mod 声明的游戏版本。
- **被 Fast Rendering 拒绝的读取**：`v0.9.1rc1` 之前的发布版本在读取模型视图矩阵时会抛出异常。`FastRenderingModelviewMatrixReader` 会捕获它并记录一次失败；其机制详见 [docs/dev/rendering-environment.md](../../docs/dev/rendering-environment.md)。
  - `FastRendering.FIRST_MODELVIEW_READ_RELEASE` 和 `readInstalledVersion()`：报告被拒绝的读取时对比的两个版本。
  - `FastRendering.COMPATIBILITY_SUBJECT_KEY` 和 `COMPATIBILITY_SUBJECT_NAME`：该渲染器在报告中的身份。
  - `UnavailableModelviewMatrixReader`：每次读取都没有读数的读取器。
  - `ModelviewMatrixReaders.selectForActiveRenderer()` 按使用方分别解析，因此每个遇到被拒绝读取的 Mod 都会被告知其损失。
- **通知的显示面**：
  - `CompatibilityNotice`：以游戏自身的单按钮确认对话框显示每个失败。
  - `ScreenCompatibilityNotices`：在星图界面上发现的失败，就在那里以 `CompatibilityNoticePanel` 显示。
  - `CompatibilityNoticePanel`：立于核心界面的控件树中，与游戏自身的提示窗口一样淡入淡出，可通过按钮、Escape、Enter 或空格键关闭。
  - `ShownMapTab.isMapTabShowing()`：星图是否显示在屏幕上，失败时按未显示处理。面板借此随星图一同撤下。
  - `ModalOverlays`：KMLib 用来占据核心界面的叠加层，位于 `starsector/ui/coreui/`。`CoreUiDialogView` 将它与游戏自身的模态窗口一同读取。
  - `OverlayPresence.isShowing()`：叠加层是否有任何部分显示在屏幕上，淡出过程也算在内。
  - 通知中的强调来自填入其模板的值，绝不通过解析措辞得出。
- **Mod 身份**，各自放在绑定它的代码旁边：
  - `KmlibMod.MOD_ID`：KMLib 自身的 ID，无需加载其插件类即可读取。
  - `NexerelinPresence.MOD_NAME` 和 `RandomAssortmentOfThingsPresence.MOD_NAME`。
  - `KmlibLunaSettings.LUNALIB_MOD_ID` 和 `LUNALIB_MOD_NAME`。
  - `InstalledMods.readModName()` 和 `readModVersion()`：游戏所保存的 Mod 显示名称和其声明的版本。

##### 字体

文本会回退到包含其全部字符的字体，因此已本地化的安装环境会绘制其自身的文字，而不是 `?`。

- **`FaceResolver`**：选择文本所用的字体。所请求字体的图集包含每个字符时保留该字体，否则沿回退路径向下，取第一个能绘制的字体。
  - `StarsectorFont.resolveLowerResolutionFont()`：字体家族中下一个更小的字号，即回退路径上的一步。
  - `FaceResolver.LAST_RESORT_FONT`：`insignia15LTaa`，在回退路径上没有任何字体能绘制该文本时返回。
  - `listFallbackWalk()`：一段文本依次尝试的字体。
  - `InstalledFaces.createFaceResolver()`：基于当前运行游戏图集的解析器。
- **`SettledFaceMemo`**：每种文本最终落定的字体，只解析一次，并保留到 `discardFaces()` 为止。每个星域持有一个，而不是每次绘制都重新解析。
  - `createUnsettled()`：保留每个所请求的字体，供没有文本可读的调用方使用。
  - `InstalledFaces.createFaceMemo(reader)`：基于当前运行游戏图集的备忘。
- **用于落定字体的文本读取器**。
  - `SectorStarSystems.listSystemNames(sector)` 和 `SectorMarkets.listMarketNames(sector)`：每个星系和每个市场的名称，无论是否已被发现。
  - `StarsectorStrings.listCategoryStrings(category)`：`data/strings/strings.json` 中的某一类别，即游戏在所有已启用 Mod 之间合并后的结果。
  - `KmlibStrings.collectTexts(items, readText)`：按顺序取每一项的文本，省略 null 和空白。
- **`FontAtlas`**：文本绘制所用图集之上的密封类型：KM 指名的 `StarsectorFont`，或 `DeclaredFontAtlas`。
  - `DeclaredFontAtlas`：游戏 `defaultFont` 设置所指、且没有任何 `StarsectorFont` 对应的图集。
  - `AtlasSmoothing.resolveFromInfoLine()`：从图集描述文件第一行读取的平滑方式。
- **`StarsectorFont`**：
  - `getBasename()`：图集的基本文件名，日志行以此指明字体。
  - `findFontByPath()`：图集路径对应的字体（若枚举中有）。
  - `VANILLA_INSIGNIA_21` 和 `VANILLA_INSIGNIA_25`：正文字体的中号与较大字号，即从 `insignia42LTaa` 向下降级的各级。
- **`GameDefaultFontReader`**：游戏自身的 `defaultFont`，按原版 `Fonts.DEFAULT_SMALL` 的方式读取，结果为 `StarsectorFont` 或 `DeclaredFontAtlas`。
- **已安装图集读取器**：向已加载图集询问的端口，因为本地化会以原版的文件名替换图集。
  - `FaceLineHeightReader`：图集按 1:1 绘制时的行高；`isFaceLoadable()` 说明它是否已加载。
  - `LazyFontLineHeightReader`：其运行时适配器，对无法加载的字体返回 `NO_LINE_HEIGHT`。
  - `GlyphCoverageReader`：图集能否把一段文本的每个字符都按其本身绘制，而不是绘制为 `?`。
  - `LazyFontGlyphCoverageReader`：其运行时适配器。
- **原生字体**：按图集自身 1:1 尺寸构建的字体，供本身没有指定尺寸的调用方使用。
  - `TextFace.createNativeFace(atlas, lineHeights)`：使用调用方持有的行高。
  - `InstalledFaces.createNativeFace(atlas)`：尺寸从当前运行游戏的安装环境读取。
- **`InstalledFaceCheck`**：在启动时将每种字体加载一次，并记录每种字体的行高或其无法加载。`KMLib_ModPlugin` 运行它。
- **字体构建门禁**：每次构建都需要网络和游戏的字体。
  - `test` 会检查每种字体：在构建自身的安装环境上、在锁文件固定的每个本地化版本分支上，以及在 `-PfontInstallRoots` 指定的任何安装环境上。
  - 上游版本分支发布新的描述文件时，`checkFontEditions` 会对照 `font-editions.lock.json` 中的 SHA 使构建失败；`writeFontEditionsLock` 负责更新锁文件。

##### 势力

- **联盟**：哪些势力站在一起，来自维护这种关系的任一 Mod。原版不保存这种关系。
  - `FactionAlliances`：每个结盟势力所属的联盟，提供 `areFactionsAllied` 和 `buildFrom`。
  - `AllianceRecord`：以纯数据表示的一个联盟：ID、显示名称，以及按市场规模降序排列的成员。
  - `AllianceSource`：这些记录传入所经由的端口。
  - `NexerelinAllianceSource`：Nexerelin 的实时联盟，位于存在性门禁之后。
- **`FactionNames`** 和 **`FactionNameForm`**：在一处读取势力的设定名称。
  - `resolveName`：按设定原样读取简称或全称。
  - `resolveFullestName`：全称，全称为空时为简称。
  - `listEveryName`：每个势力的全部名称，同一文本只读取一次。
- **`StarsectorFactionColours.findPalette()`**：势力的亮色与暗色配对，没有灰色后备，势力不存在时为 null。`resolvePalette` 经由它进行后备。

##### 几何

- **`VertexWelder`**：按容差合并同一角点的多次报告，使分别计算出的边可以按精确 ID 比较。从 `EdgeRings` 中移出。
- **`Disk.measureSagitta(radius, segments)`**：近似圆盘的多边形在最坏处向内偏离圆盘多远，即针对该圆盘绘制的任何内容的实际分辨率。
- **`Segment`** 与 `{x, y}` 点：
  - `readStart()` 和 `readEnd()`：线段的两端，每次都是独立的数组。
  - `joinPoints(start, end)`：两点之间的线段。
- **多边形检查**：
  - `PolygonRegions.countSelfCrossings(ring)`：一个环与自身相交的次数。开销为二次方，因此用于测试和探测器。
  - `PolygonOffsets.hasInsetCollapsed(rawRing, insetRing)`：斜接内缩是否把环折叠了，而不是对其偏移。
  - `PolygonOffsets.removeReversedLoops(polygon, isCounterClockwise, windowVertexCount)`：被告知预期绕向的折叠拼接器，用于折叠超过主体的环。两种形式都会拼接跨越环起点的折叠，并且只拼接不自交的环路，先处理最内层。
- **`Points.measurePathLength(points)`**：不闭合路径从头到尾的长度。
- **`PolygonShapes.computeRegularVertices`**：根据中心、半径、边数和起始角计算正多边形的顶点，形式为 `{x, y}` 对或 `Vector2f`。`LabelledPolygon.createRegularPolygon` 通过它生成其裁剪形状。
- **`Vector2f` 形式**，用于在游戏的浮点界面坐标中布置形状：
  - `Points.computeMeanOfVectors`
  - `PolygonRegions.isPointInsideRing(ring, point)`
  - `Rectangle.computeEnclosingRectangle`：返回按一个角和尺寸放置的 `Rectangle`。

##### 控件

- **单选组**：
  - `RadioSpec`：横向与纵向单选组之上的密封接口，承载重选规则。现有的 `HorizontalRadio` 调用点可原样编译。
  - `VerticalRadioSpec`：一列选项单元，其中一个亮起，用于一行放不下的选项组。
- **滚动**：
  - `ScrollingSectionSpec`：一起滚动的一段控件，例如标题、其下的列表以及旁边的行。
  - `Control.isScrolled()`：已布局的控件是否位于该区段内。未带此值构建的 `Control` 是固定的。
- **`InteractiveSpec.isSegmented()`** 和 **`reselectBehaviour()`**：控件自行说明其单元是否分别命中，以及重新选择已点亮的单元时会发生什么，使命中测试与按下行为保持一致。
- **`RowDimensions`**：每一行的高度与宽度，合为一个经过校验的值。
  - `ControlStripLayout.StripMeasurement.rowDimensions()`：以这种形态返回一次测量的各行。
- **`TooltipRow.createRow(List<LabelRun>)`** 和 **`LabelledRow.createRow(List<LabelRun>)`**：基于已组合成多个文本段的标签构建的行。

##### 战役与存档

- **按地址保存的内存值**：存储值按 Mod 声明的轴上的每个点各保存一份，而不是每个存档一份。
  - `MemoryKeyAddress`：在一处组合键，因此任何两个持有者都不会意外共用一个槽位。
  - `AddressedMemoryFlag` 和 `AddressedMemoryString`：持有者。
- **指明星域的形式**，供被传入星域、而不是读取当前运行星域的代码使用：
  - `SectorMemoryString` 和 `SectorMemoryFlag`：每次读写，基于 `SectorMemoryAccess.readSectorMemory(sector)`。
  - `BaseExpiringIntelPlugin.findActive(sector, intelClass)`、`isExpired(clock)` 以及接收时钟的构造函数。
- **日历**：
  - `CampaignCountdown`：从起点开始的一段战役天数，可带完成余量，使其在玩家预期的那一帧完成。`BaseExpiringIntelPlugin` 通过它读取其时间窗口。
  - `CampaignMonth`：战役日历中的一个月。`formatKey()` 将其拼写为 `<cycle>-<month>`，即每月任务保存上次运行时所用的键。
- **`Colonies.selectColonies(test)`** 和 **`Colonies.hasAnyColony(test)`**：按集合自身顺序筛选殖民地，或在第一个匹配处停止。未提供测试条件时不通过任何殖民地。
- **`PersistedChoice`** 和 **`PersistedChoices.fromKey()`**：存档以其自身的键存储的选项，以及带回退的反向查找。`SortDirection`、`ListColumns` 和 `ListSortMode` 都是持久化选项。

##### 核心工具

- **字符串**：
  - `KmlibStringKeys.get()` 和 `format()`：在 KMLib 自身类别中的查找。
  - `KmlibStrings.requireText()`：拒绝为 null 或空白的名称或句子。
- **`GlMatrix`**：
  - `FLOAT_COUNT`：一个 GL 矩阵所占的十六个浮点数。`ModelviewMatrixReader.MATRIX_FLOAT_COUNT` 取自它。
  - `createIdentity()`：每次调用都返回一个新的单位矩阵。
- **数据文件**：
  - `SpreadsheetRows`：合并后电子表格的数据行，排除空 ID 分隔行和 `#` 注释行，并拆分列表值单元格。`FactionSourceMods` 通过它读取 `factions.csv`。
  - `ScriptClasses.instantiateScript(className, scriptType)`：不借助 `java.lang.reflect`（游戏不允许 Mod 代码使用它）构建数据文件中指名的类。错误的类名会被拒绝并指出类名。
- **`Jitter.roll(jitterSize, random)`**：从调用方的 `Random` 抽取的抖动，可按种子重放。区间的上端是开区间。

##### 构建与发布

按语言划分的压缩包在构建与发布方面的部分。测试夹具一节中的本地化夹具会检查这些步骤写出的内容。

- **`writeLocaleFiles`**：为提交了 `localisation/manifest.json` 的 Mod，将一个语言区域的语言包写入游戏读取的文件。`-Plocale=<tag>` 用于选择语言区域，否则构建清单中的默认语言区域。
  - 若 Mod 提交了 `mod_info.base.json`，则将该语言区域的启动器片段合并到其上，仅限文本字段。
  - JSON 通过 `shipped-json-reader.gradle` 读取，与 `ShippedJson` 基于同一个 `json.jar`。
  - 切换语言区域时 `jar` 和 `test` 会重新运行。`localisation/` 或根目录的 `CHANGELOG.md` 变化时，`test` 也会重新运行。
- **`mod_info.base.json`**：按语言区域保存启动器文本的 Mod 提交该基础文件，并在 gitignore 中忽略由其写出的 `mod_info.json`。每个构建脚本和发布 Action 都会在基础文件存在时读取它。
  - `mod-info-reader.gradle`：读取一次该元数据，发布为 `modInfo` 和 `modInfoFile`，并提供用于读取另一个检出的 `readModInfo(File)`。
- **按语言区域发布**：对于提交了 `localisation/manifest.json` 的 Mod，`mod-release.yml` 用同一个 jar 为每个语言区域发布一个压缩包和一个版本文件。未提交清单的 Mod 发布方式不变。
  - `read-locales`：Mod 发布的语言区域，默认语言区域排在首位。
  - `package-release`：为每个语言区域写出、压缩并填写版本文件。每个压缩包都带有本语言区域的 `CHANGELOG.md`。
  - `compose-locale-note`：发布说明中的 *Builds by language* 列表（链接每个语言区域所需的核心本地化），以及折叠在各自名称之下的每份译文说明。译文缺少该版本的章节会使发布失败。
  - `fill-version-file-template` 接受可选的 `locale`，使安装轮询其自身语言的版本文件。
- **KMLib 按语言区域保存自己的文本**：字符串和设置位于 `localisation/<locale>/` 下，启动器文本位于 `mod_info.base.json` 中。
  - `localisation/zh-hans/CHANGELOG.md` 完整翻译了本更新日志。
  - `LocaleParityIntegrationTests` 以默认语言区域为准校验每个语言区域。
  - 中文语言包的 `README.md` 是基础术语参考，每个使用方 Mod 的参考都以它为基础。

##### 测试夹具

- **`SaveFormatFixture`**：通过 XStream（游戏写入存档所用的序列化器）驱动 Mod 的持久化对象，使会让已存值成为孤立值的类或字段改名在测试套件中失败。它列出对象图写出的元素路径，并以加载时的方式读回该对象图。
- **测试约定**，位于共享的 Starsector 约定中：
  - 每个测试任务都将 `java.util`、`java.lang.reflect`、`java.text` 和 `java.awt.font` 开放给未命名模块，与游戏自身的 `vmparams` 一致。XStream 1.4.10 需要这些开放。
  - 在 `StubbedGlobalLogger` 之外使用 `mockStatic(Global.class)`，或在任何地方使用 `openSeam(Global.class)`，都会使构建失败。不应答记录器的替身会让静态记录器在 JVM 的剩余生命周期内一直为 null。
- **静态接缝**：
  - `StaticSeams`：一个布置中已打开的接缝，由内向外依次关闭。`openSeam()` 打开一个接缝，`holdSeam()` 接管某个夹具打开的接缝，例如 `StubbedGlobalLogger.openGlobalAnsweringLoggers()` 的接缝。
  - `SalvageEntityMock`：将原版的掉落抽取器固定住，捕获每次抽取并锁定抽取次数。只固定七参数的抽取器。
- **游戏与 Mod 状态**：
  - `StarsectorSettingsFake`：`answerGameVersion()` 和 `answerModGameVersions()` 应答游戏版本，`installSettingsWithModNames()` 按名称识别 Mod。
  - `ModStateScopes.runWithGameVersions()` 和 `runWithModNamed()`：为单个 Mod 设置上述内容。
  - `IntelManagerFake`：按游戏关于保留什么的规则，保存添加到其中的情报。
  - `StoredMemoryFake`：以映射为后备、背后没有其他东西的记忆，供已持有 `Global` 替身或将记忆挂在行星上的测试套件使用。`SectorMemoryFake` 基于它构建。
  - `MemoryKeyAddresses`：两个替身地址。
- **兼容性**：
  - `CompatibilityFailureFixture`：一个具有代表性的失败，每个槽位各有一个构建器，另有两个主体键和两个损失不同内容的使用方。`drainSessionRecord()`、`createFeatureFailure()` 和 `takeNextBindingFailure()` 用于重置和读取记录。
  - `CompatibilitySlotTemplates`：作为替身、暴露其槽位的通知模板。
  - `CoreUiReachFailures`：通过 `CoreUiTree` 的访问在未知游戏构建上失败的两种方式，用于必须同时捕获两者的边界。
  - `GameReachRecordFixture`：将游戏访问归档到用例自己的记录中的报告器。
- **日志**：
  - `LogAppenderFake.getThrowables()`：一次捕获中各条目记录日志时附带的异常。
  - `LogAppenderFake.captureLogOf(loggingClass, capturedLevel, work)`：在指定级别进行的捕获，用于关注某个级别放行什么。
- **界面**：
  - `TooltipMakerFake`：记录段落、间距、高亮片段和按钮而不进行布局的提示框元素。
  - `HighlightedTooltipMock`：为每个段落分配各自标签的提示框，使其高亮可以被检查。
  - `LabelHighlightRule`：游戏的高亮调用会让哪些片段保持原样，即两侧不是空白、ASCII 标点或文本首尾的片段，例如紧挨中文文字的片段。`UnhighlightedRun.describeIn()` 将这样的片段表述为一条发现项。
  - `ButtonLabelFake`：按钮的标签，按每个字符 `CHARACTER_WIDTH` 的宽度测量，并记录其被调整到的宽度。
  - `Anomaly`、`AnomalySortMode` 和 `ListPickerBlockReads`：选择器行、排序词汇，以及对已构建选择器区块的读取。
- **字体**：
  - `FaceLineHeightReaderFake`：从表中取得行高。`createVanillaLineHeights()` 应答原版的行高，`answeringLineHeight()` 在同一基础名下声明另一个图集。
  - `LazyFontLineHeightReaderMock`：从 `FaceLineHeightReaderFake` 应答的实时行高读取器。
  - `GlyphCoverageReaderFake`：根据规则给出字形覆盖。`createLatinOnlyCoverage()` 代表原版安装，`coveringEveryCharacter()` 扩大某一种字体的覆盖。
- **随附文件**，每种都按游戏的方式读取：
  - `ShippedJson`：在游戏自身 `json.jar` 之前执行引擎的 `#` 注释剥离。游戏拒绝的内容它也会拒绝，包括重复的键和字节顺序标记；其形状检查会让拼错的字段失败。
  - `ShippedStrings`：Mod 的 `data/strings/strings.json`（以 `STRINGS_JSON` 公开），以及其持有类列出的字符串 ID。
  - `ShippedSpreadsheet`：Mod 的 CSV，可按表头、按 ID 列、按单列或按位置读取，并正确解析加了引号的逗号。它以 `api` 依赖引入 Apache Commons CSV，`LunaSettingsTable` 通过它读取。
  - `StringTemplates.countFormatArguments()`：`String.format` 会从模板中取用多少个参数；重复的位置只计一次，`%%` 和 `%n` 不计入。
  - `LunaSettingsHighlights` 和 `LunaSettingsTable.readHighlightedTextsByFieldId()`：按 LunaLib 绘制方式读取的设置单元格，去掉方括号并列出其高亮的片段。

##### 本地化夹具

这些夹具读取 Mod 的 `localisation/` 布局（每个语言区域一个语言包，位于 `localisation/<locale>/` 下，由清单列出），并比较其中的语言区域。每个文件都按其随附副本的读取方式读取。

- **`LocaleParity`**：使每个语言区域与默认语言区域保持一致，每项发现都以应做的修改来表述。`findAllMismatches()` 汇集所有检查，使 Mod 的测试套件只需断言一次。
  - 在缺口没有后备的地方严格要求：缺失的文件、字符串或行，留空的字符串，取用不同参数的槽位，存储内容不同的行，被移动的标签页，未指明 `coreLocalisation` 时出现的 Latin-1 以外的文本，以及未声明的依赖项。
  - Radio 的选项须一致，因为 LunaLib 存储的是所选的标签。
  - 已翻译的更新日志必须与根目录的更新日志逐点一致。
  - 每个设置表中的每个方括号片段都必须是游戏能够高亮的。
- **布局读取器**：
  - `LocalisationDirectory`：Mod 的 `localisation/` 目录、其清单及其语言包。
  - `LocaleManifest`：各语言区域、默认语言区域，以及每个语言包文件的落点。未知的键会被拒绝，因此拼错的 `coreLocalization` 会失败。
  - `DeclaredLocale`：小写的 BCP 47 标签、以该语言自身书写的名称，以及其玩家覆盖安装到 `starsector-core` 上的核心本地化。
  - `LocaleBundle`：一个语言区域的目录。`readStringSource()` 为设置替身提供其字符串。
  - `ModInfoFragment`：语言区域所翻译的启动器文本：`name`、`description`、`author` 和依赖项名称。`listFallbackFieldNames()` 列出留给基础文件的字段。
  - `ModInfoBase`：片段能触及的 `mod_info.base.json` 内容。
- **比较读取器**：
  - `ChangelogOutline`：Keep a Changelog 文件的版本、各节、每一深度的列表项数量和代码片段，不读取行文。
  - `LunaSettingsTable.readBehavioursByFieldId()`、`readDisplayedTexts()` 和 `readTabsByFieldId()`：将设置行拆分为它做什么（由 `describeDifferencesFrom()` 比较的 `FieldBehaviour`）、它说什么，以及其标签页。
  - `StringTemplates.readArgumentConversions()`：每个槽位取用哪个参数、以何种转换取用。
  - `ShippedJson.requireList()` 和 `locateElement()`：对象检查与成员定位在数组上的对应方法。
- **`ShippedLocales`**：Mod 随附的语言区域，供按语言区域逐一运行的用例使用。`listLocaleTags()` 作为参数化来源，`installLocaleStrings()` 将某个语言区域的字符串设为游戏的字符串。

#### 变更

- **`MapIconReseater` 会在日志中说明它观察到的情况**，记录在 KMLib 自己的记录器上。包的 README 列出了这些日志行。
  - 星图的打开与关闭以 DEBUG 级别跟踪，每次都附带自上次观察到图标未被遮挡以来的抬升次数。
  - 两种状态每次会话警告一次：星图显示但没有可放置的图标；以及搁置，附带导致搁置的读数。
- **`VoronoiCellBuilder` 把相邻单元共享的角点放在两者中的同一位置**，该位置由两个站点和延伸半径计算得出。串接的边境边之间没有缺口，线段数只决定圆弧绘制得有多平滑。
  - 在这些角点处，单元顶点最多会移动一条弦的矢高：在随附的延伸半径和默认线段数下约为八个单位。以比这更严格的精度断言的使用方需要重新建立基线。
- **Starsector 约定通过游戏的 `json.jar` 读取 Mod 的元数据**，与启动器的方式相同，因此两者在文件能否解析上不会出现分歧。所以配置构建需要已安装的游戏。

#### 公共契约变更（**破坏性**）

| 之前 | 之后 | 原因 |
| --- | --- | --- |
| `CampaignMapView` 的三个星图读取及 `resolveSectorMapState()`、`MapFilterRows.resolveShownMapFilterRow()`、`MapFilterToggle.appendToRow()`、`VanillaButtonLabel.resolveLabelOf()`、`CoreUiOverlayPanels.attachOverlayPanel()`、`CoreUiDialogView.isModalDialogShowing()` 和 `resolveModalPresence()`、`CodexView.isCodexShowing()`、`MapIconLayeringProbe.readLayeringOf()` | 均以 `GameReachReporter` 作为最后一个参数。没有玩家可见损失时传入 `GameReachReporter.UNREPORTED`。 | 对游戏的失败访问会作为调用方 Mod 的损失报告。 |
| `MapPresence`、`VanillaIntelScreenView` 和 `VanillaMapTooltipProbe` 的无参构造函数 | 构造时传入 `GameReachReporter`。 | 同上。 |
| `ReflectiveCoreUiComponentRepainter.INSTANCE` | 用调用方的报告器构造一个实例。 | 失败的重绘作为调用方的损失归档。 |
| `CompatibilityFailures.takeNextUnreported()` 和 `CompatibilityNoticePanel.showFailure()` 使用 `CompatibilityFailure` | 二者都使用 `ReportedFailure`。读取 `subject()` 或 `breakage()` 之前，先确认它是 `CompatibilityFailure`。 | 记录也会保存 Mod 自身失败的功能。 |
| 无参的 `ModelviewMatrixReaders.selectForActiveRenderer()` | 传入使用该读数的 `CompatibilityConsumer`。 | 只有调用方知道读取被拒绝会让它损失什么。 |
| `FastRenderingModelviewMatrixReader.INSTANCE` | `ModelviewMatrixReaders` 为每个使用方构建一个实例。 | 读取器把拒绝记在它所服务的 Mod 名下。 |
| `ExtensionPoint.settleWorkOutcome()` | `offerWork(work)`。`registerImplementation()` 接受第四个参数：注册方的实现被移除时告知注册方的内容。 | 对实现的调用在失败边界之内运行。 |
| `ColonisationRoutines.registerRoutine()`、`OwnershipTransferRoutines.registerRoutine()`、`OwnerSubmarketRules.registerRule()`、`ModdedSystemAccessRoutes.registerRoute()` | 均接受一个 `Supplier<ModIntegration>`。 | 出错的实现会作为其集成的失败报告。 |
| `MapProbeWarnings` 和 `createSharedWarning()` | 位于 `kmlib.logging` 中的 `RearmableWarnings`，以及 `createRearmableWarning()`。`rearmAllWarnings()` 不变。 | 它汇集每个按类保留的警告，而不仅是星图探测器的警告。 |
| `KmLogging` | `LunaLogLevelBinding.bindLogLevel(modId, loggerRoot, fieldId)`，默认级别为 `LunaLogLevelBinding.DEFAULT_LEVEL`。接受后备级别的重载已移除。 | 它与 KMLib 其余的 LunaLib 读取一起位于 `kmlib.settings` 中。 |
| `GlStateGuard` | `GlPasses.runWithSavedState` | 它只是转发到后者。 |
| `ControlSpec.Checkbox`、`ControlSpec.Interactive` 以及其余十个变体 | `CheckboxSpec`、`InteractiveSpec` 等，位于 `kmlib.starsector.ui.controls.specs` 中，`ControlAction`、`ControlHoverReport`、`RadioAlignment`、`ReselectBehaviour`、`RowGeometry` 和 `SegmentSizing` 也随之迁移。 | 每个变体一个文件，而不是一个 878 行的文件。 |
| `VerticalTableSpec` 的 `scrolls` 和 `asScrolling()` | 包装列表：`new ScrollingSectionSpec(List.of(list))`。规范构造函数接受七个参数。 | 可滚动区段容纳任意控件，而不只是一个表格。 |
| 接受高度与宽度列表的 `RowStack.layoutRows()`，及其统一高度的重载 | 传入 `RowDimensions`；各行高度相同时使用 `RowDimensions.createUniform(rowHeight, rowWidths)`。 | 两个重载的第三个浮点数，在一个中是行高，在另一个中是间距。 |
| 接受 `rowHeights` 和 `rowWidths` 的 `ControlStripLayout.layoutControls()` | 传入 `StripMeasurement`。 | 两个列表来自同一次测量，因此不会不一致。 |
| `SortDirection.fromKeyOrDefault()` 和 `ListColumns.fromKeyOrDefault()` | `PersistedChoices.fromKey(options, key, fallback)`，例如 `PersistedChoices.fromKey(ListColumns.values(), key, ListColumns.DEFAULT)`。 | 所有带键选项集共用一个查找。 |
| `StarsectorFont.getNativeSize()` | `InstalledFaces.createNativeFace(font)`，尺寸通过 `FaceLineHeightReader` 读取。 | 本地化安装会以相同的名称、不同的尺寸替换图集。 |
| 接受 `StarsectorFont` 的 `TextStyle.createStyle()` | 传入 `TextFace`，例如 `InstalledFaces.createNativeFace(...)`。 | 样式不再知道原生尺寸。 |
| `TextFace.font()`，返回 `StarsectorFont` | `TextFace.atlas()`，返回 `FontAtlas`。`LazyFontCache.loadByFace()`、`FaceLineHeightReader`、`GlyphCoverageReader`、`WidgetStyle.bodyFont` 和 `StripTextMeasurers.loadFaceMeasurers()` 也改为接受它。 | 文本可以用游戏声明的默认字体绘制。由 `StarsectorFont` 构建的字体可原样编译。 |
| 位于 `kmlib.starsector.ui.font` 中的 `LazyFontCache` 和 `DrawableStringCache` | `kmlib.starsector.ui.font.installed` | 运行中游戏图集的读取器与字体分开存放。 |
| 位于 `kmlib.starsector.ui.font` 中的 `LineWidthMeasurer`、`TextSpanMeasurer`、`LazyFontMeasurer`、`LazyFontSpanMeasurer` 和 `StripTextMeasurers`，以及夹具 `LineWidthMeasurerFake` | `kmlib.starsector.ui.font.measure`；夹具移至 `kmlib.testfixtures.starsector.ui.font.measure`。 | 测量与字体分开存放。 |
| `StubbedGlobalLogger.answerLoggersOn()` | `openGlobalAnsweringLoggers()`，与其他接缝一起交给 `StaticSeams.holdSeam()`。 | `Global` 只有一个替身，并与其他接缝一同关闭。 |
| `mod-release.yml` 将带有 `localisation/manifest.json` 的 Mod 发布为一个无后缀的压缩包 | 每个语言区域发布 `<folder>-<version>-<locale>.zip` 和 `<mod-id>-<locale>.version`。仍会附加 `<mod-id>.version`，因此旧的安装会继续轮询它。 | 每种语言一个压缩包。 |

</details>

## [0.4.0] - 2026-09-15

### 修复

- **发布流水线**此前会在推送到 master 但未提升版本号时运行，且未能检测到版本号降低。

### 新增

- **发布门禁**：带有 `## Index` 的 `CHANGELOG.md` 必须列出正在发布的版本，以免索引链接指向不存在的内容。没有索引的更新日志不受影响。

### 公共契约变更（**破坏性**）

- `check-version` 现在需要必填的 `version` 输入，且不再输出 `version`。它向 git 查询传入的版本，而不是自行读取 `mod_info.json`，因此门禁与流水线的其余部分不会针对不同的字符串作出判断。可复用的 `mod-release.yml` 的调用方无需更改；直接调用该 action 的工作流现在必须传入 `version`。
- `Hatching.computeHatchRun()` 现在接受一个 `HatchPattern`（新的记录类型，携带一次切割所依据的间距、角度和拼接容差），取代原先三个零散的 double 参数。调用方现在还可以将决定其斜线几何形状的参数与结果的描边方式分开持有，并以此作为缓存依据。

## [0.3.1] - 2026-09-15

### 依赖变更

- 按照 **Numan** [在 USC 上](https://discord.com/channels/187635036525166592/1549091275167240272/1549267098415403011)的建议，反射工具类从 **MagicLib** 移植而来（他正在参与 **MagicLib** 开发版本的工作）。其范围仅限于 **coreui** 包。
- 移除了对 **MagicLib** 的依赖。
- 本项目改为以 **LGPL-3.0-only** 许可证发布，以符合所移植代码的许可要求。

## [0.3.0] - 2026-09-15

### 修复

- **Linux 上的崩溃**。EventsPanel.getMap() 返回的混淆类型在不同平台上并不相同。由 [**Elia Rowan (zinzrinz)**](https://discord.com/channels/187635036525166592/1549091275167240272/1549127910084972614) 和 [**MattTheMatt2**](https://discord.com/channels/187635036525166592/1549091275167240272/1549149360644948121) 在 **USC** 报告，由 [**WolframSegler**](https://discord.com/channels/187635036525166592/1549091275167240272/1549130150312935506) 定位问题并提出修复建议。
- **崩溃**。**星景**星图的地形重新安置因控件签名不匹配而失败时，现在会被处理并记录到日志，随后地形重新安置会在本次游戏的剩余时间内停用。
- **地图渲染状态被改变**。地形重新安置失败时，现在会先将已重新安置的地形恢复到原位，再停用。
- **地图实体重复**。若某个位置拒绝交出某个实体，重新安置不再因此为一个从未离开的实体欠下一次放回操作；此前这会在下一次推进时为该实体添加第二个副本。

### 新增

- `IntelScreenView.readMapVisorState()` 和 `MapVisorState`：将视窗是否存在及其星景筛选状态作为一次读取返回。依次调用 `getMapVisorRect()` 和 `isMapStarscapeModeOn()` 回答的是同一个问题，却要为此遍历实时控件树两次，而 `MapPresence` 此前在战役的每一帧都要付出这一代价。
- `MapIconOrderWidgetFake`：代表地图控件图标顺序的测试夹具，无需运行游戏即可驱动关于图标所处位置的规则。

### 公共契约变更（**破坏性**）

- `MapIconReseater` 的第三个构造函数参数现为 `Function<SectorEntityToken, MapIconLayering>`（针对某个实体发起的读取），原先为 `Supplier<MapIconLayering>`：一个调用方必须自行保证与第二个参数指向同一实体的读数，却没有任何检查确保这一点。调用方直接传入位置读取本身（`MapIconLayeringProbe::readLayeringOf`），而不是包裹自身实体查找的闭包。

## [0.2.0] - 2026-09-14

### 新增

- `Ranges.clampInto(int, int, int)`：现有 clamp 的整数形式，与 double 形式条件相同，包括其空区间规则。值和边界均为整数的调用方（像素宽度、计数、以秒为单位的周期）此前必须在仅支持 double 的形式两端进行类型转换，因此这类调用方改为内联一对 `min`/`max`，使这唯一的 clamp 无人调用。新形式由 double 形式实现，而不是复制一份规则，因此两者在空区间情况下的行为不会出现偏差。

## [0.1.0] - 2026-09-14

首个打标签的发布版本，因此没有可供比较的先前版本：以下即为全部公共接口，包括玩家输入的命令、使用方导入的包，以及作为第二个构件发布、供使用方在自己的测试套件中使用的测试夹具。

### 新增

#### 控制台命令

七条仅限战役中使用的命令，通过 `data/console/commands.csv` 注册，仅在安装了 Console Commands 时可用。启用 Nexerelin 时，两条殖民地命令都会交由该 Mod 自身的殖民和移交流程处理。

- `kmlib_activate_gate`
- `kmlib_colonise`
- `kmlib_list_factions`
- `kmlib_list_map_spoilers`
- `kmlib_list_system_entities`
- `kmlib_spawn`
- `kmlib_transfer_market`

#### 与游戏无关的辅助工具

签名中不涉及 Starsector API。

- `kmlib.animation`
- `kmlib.collections`
- `kmlib.colour`
- `kmlib.extensions`
- `kmlib.input`
- `kmlib.logging`
- `kmlib.math.easing`
- `kmlib.math.geometry`
- `kmlib.math.hashing`
- `kmlib.math.motion`
- `kmlib.math.random`
- `kmlib.math.ranges`
- `kmlib.math.solving`
- `kmlib.opengl`
- `kmlib.opengl.hatch`
- `kmlib.profiling`
- `kmlib.profiling.budget`
- `kmlib.profiling.recording`
- `kmlib.profiling.report`
- `kmlib.profiling.snapshot`
- `kmlib.settings`
- `kmlib.text`
- `kmlib.time`

#### 面向 Starsector 的封装与接缝

- `kmlib`：启动器加载的 Mod 插件，而非供使用方导入的包
- `kmlib.mods.console`
- `kmlib.mods.console.commands`
- `kmlib.mods.console.commands.input`
- `kmlib.mods.console.commands.output`
- `kmlib.mods.console.commands.parsing`
- `kmlib.mods.console.commands.targets`
- `kmlib.mods.console.commands.validation`
- `kmlib.mods.nexerelin`
- `kmlib.mods.rat`
- `kmlib.starsector`
- `kmlib.starsector.entities`
- `kmlib.starsector.factions`
- `kmlib.starsector.factions.relation`
- `kmlib.starsector.fleet`
- `kmlib.starsector.geometry`
- `kmlib.starsector.graphics`
- `kmlib.starsector.intel`
- `kmlib.starsector.listeners`
- `kmlib.starsector.map`
- `kmlib.starsector.markets`
- `kmlib.starsector.markets.colonies`
- `kmlib.starsector.markets.colonisation`
- `kmlib.starsector.markets.ownership`
- `kmlib.starsector.memory`
- `kmlib.starsector.scripts`
- `kmlib.starsector.settings`
- `kmlib.starsector.settings.modmanager`
- `kmlib.starsector.strings`
- `kmlib.starsector.systems`
- `kmlib.starsector.systems.claims`
- `kmlib.starsector.time`
- `kmlib.starsector.ui.buttons`
- `kmlib.starsector.ui.colour`
- `kmlib.starsector.ui.controls`
- `kmlib.starsector.ui.coreui`
- `kmlib.starsector.ui.debug`
- `kmlib.starsector.ui.font`
- `kmlib.starsector.ui.highlight`
- `kmlib.starsector.ui.input`
- `kmlib.starsector.ui.intel`
- `kmlib.starsector.ui.label`
- `kmlib.starsector.ui.layout`
- `kmlib.starsector.ui.map`
- `kmlib.starsector.ui.map.controls`
- `kmlib.starsector.ui.map.icons`
- `kmlib.starsector.ui.map.presence`
- `kmlib.starsector.ui.map.probes`
- `kmlib.starsector.ui.map.transform`
- `kmlib.starsector.ui.render.gl`
- `kmlib.starsector.ui.render.gl.controls`
- `kmlib.starsector.ui.render.gl.panel`
- `kmlib.starsector.ui.render.gl.style`
- `kmlib.starsector.ui.render.gl.tabs`
- `kmlib.starsector.ui.render.gl.tooltip`
- `kmlib.starsector.ui.screen`
- `kmlib.starsector.ui.sound`
- `kmlib.starsector.ui.suppression`
- `kmlib.starsector.ui.text`
- `kmlib.starsector.ui.tooltip`
- `kmlib.starsector.ui.widgets`
- `kmlib.starsector.ui.widgets.lists`
- `kmlib.starsector.ui.widgets.scroll`
- `kmlib.starsector.ui.widgets.segments`
- `kmlib.starsector.ui.widgets.tabs`
- `kmlib.starsector.ui.widgets.tabs.style`
- `kmlib.starsector.ui.widgets.tooltip`

#### 测试夹具

作为 jar 旁的第二个构件发布，供使用方自己的测试套件使用。伪实现充当程序库所反转的接缝的替身；夹具则构建测试用例所依托的世界。

- `kmlib.testfixtures.logging`
- `kmlib.testfixtures.mods.console`
- `kmlib.testfixtures.mods.console.commands.output`
- `kmlib.testfixtures.profiling`
- `kmlib.testfixtures.starsector`
- `kmlib.testfixtures.starsector.listeners`
- `kmlib.testfixtures.starsector.markets`
- `kmlib.testfixtures.starsector.markets.colonies`
- `kmlib.testfixtures.starsector.memory`
- `kmlib.testfixtures.starsector.settings`
- `kmlib.testfixtures.starsector.systems`
- `kmlib.testfixtures.starsector.systems.claims`
- `kmlib.testfixtures.starsector.ui.coreui`
- `kmlib.testfixtures.starsector.ui.font`
- `kmlib.testfixtures.starsector.ui.input`
- `kmlib.testfixtures.starsector.ui.intel`
- `kmlib.testfixtures.starsector.ui.label`
- `kmlib.testfixtures.starsector.ui.layout`
- `kmlib.testfixtures.starsector.ui.map`
- `kmlib.testfixtures.starsector.ui.map.controls`
- `kmlib.testfixtures.starsector.ui.map.presence`
- `kmlib.testfixtures.starsector.ui.map.probes`
- `kmlib.testfixtures.starsector.ui.map.transform`
- `kmlib.testfixtures.starsector.ui.sound`
