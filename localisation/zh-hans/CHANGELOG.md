# 更新日志

KMLib 的所有重要变更都记录在此。格式遵循 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)，项目遵守[语义化版本](https://semver.org/)。KMLib 及其使用方 Mod 的版本号递增条件定义在 [docs/dev/versioning.md](../../docs/dev/versioning.md) 中。

可复用的发布工作流会把与所发布版本相匹配的章节提取到 GitHub 发布说明中，因此每个已发布的版本都必须在此有对应章节。

## 索引

- [未发布](#unreleased)
- [0.4.0](#040---2026-09-15)
- [0.3.1](#031---2026-09-15)
- [0.3.0](#030---2026-09-15)
- [0.2.0](#020---2026-09-14)
- [0.1.0](#010---2026-09-14)

## [Unreleased]

### 修复

- **`MapIconReseater` 在战役加速下也能抬升图标。** 开启加速后，无论星图是否打开，战役每渲染一帧都会多次推进其脚本，因此在相邻两次推进中进行的移除与放回落在同一帧内，控件从未在缺少该图标的状态下渲染过：每次打开星图，所有图层都停留在星云之下，重新读取存档或切换星景都无法改变。现在放回会等到控件已丢弃该图标后再进行，最多等待 `MAX_ADVANCES_DETACHED` 次推进，并在星图关闭时立即结束。由 **MiniRockytheOracle** 在 [**USC**](https://discord.com/channels/187635036525166592/1549091275167240272/1551829173037957170) 报告。
- **被搁置的抬升会在下次打开星图时重新尝试。** 尝试次数上限只在本次打开的剩余时间内放弃抬升，而不是在整个会话内放弃：下次打开面对的是全新的控件和全新的初始化，而一次比其起因持续更久的搁置，从外部看就像这一机制失效了。
- **`GlRuns`、`GlLines`、`GlQuads` 和 `GlTriangles` 在绘制抛出异常时会关闭其打开的图元。** 中途出错的绘制序列会让管线停留在 `glBegin` 内部，在那里之后的每个状态调用都会被拒绝，包括其外围 `GlPasses` 渲染通道的属性恢复，因此一次失败的绘制会破坏其后绘制的所有内容的状态。`glEnd` 现在在 `finally` 块中执行。
- **应用失败的设置变更会被记录到日志。** LunaLib 会拦下抛出异常的监听器，但只在 DEBUG 级别提及且不附堆栈跟踪，低于玩家日志所保留的级别，因此交给 `LunaSettingsReader.runOnSettingsChange` 的回调失败时不会留下任何一行日志。现在它会以 ERROR 级别连同堆栈跟踪记录，下一次变更照常尝试。日志级别绑定也基于同一中转机制构建。
- **对游戏自身界面的失败访问会留在其边界之内。** `CoreUiTree` 会把游戏自身的失败包装成受检异常并以未声明的方式抛出，而游戏新版本移除的成员会以 `LinkageError` 的形式出现，二者任一逃逸都会终结这一帧。通知面板和覆盖在星图之上的面板共同使用的界面检查，以 `ShownMapTab.isMapTabShowing()` 的形式捕获这两者。`CoreUiTree.readHopIfOffered` 在使其返回 null 的那个 catch 内部获取方法名。`CampaignMapView` 会捕获来自游戏战役界面数据类的 `LinkageError`（该类的读取由渲染通道发起，而渲染通道自身没有 catch），并判定星图未显示，同时发出一条警告。`VanillaIntelScreenView` 对情报信息面板的淡入淡出器（这是它通过直接调用从游戏面板上读取的唯一成员）也做同样处理，并返回无遮罩。
- **每个针对访问游戏控件的类级警告，在开启诊断时都会再次发出。** 此前只有星图探测器的警告被收集起来以便重置，因此筛选栏控件、其标签以及 `CampaignMapView` 的警告一旦发出就不会再响：在其中某个出错后才开启追踪的读者什么也得不到。

### 新增

- **简体中文。** 发布版本附带第二个压缩包 `KMLib-<version>-zh-hans.zip`，其中兼容性通知、设置标签页以及启动器的 Mod 列表条目均为简体中文。它需要先将[中文本地化](https://github.com/TruthOriginem/Starsector-Localization-CN)覆盖到 `starsector-core` 上：游戏自身的字体不含中文字符，没有它，所有中文字符都会绘制为 `?`。日志详细程度设置的选项保留英文，因为 LunaLib 保存的是选项的标签，这样该设置在英文与中文压缩包之间可以沿用。
- **`FeatureFailure` 和 `CompatibilityFailures.recordFeatureFailureOnce()`**：Mod 自身的某项功能抛出异常并被关闭时，通过兼容性通知告知玩家，而不是只写进日志。一项功能悄然停止工作，玩家只会看到某样东西不见了，却没有理由去日志里找原因。通知会写明是哪个 Mod、哪项功能、什么停止工作、什么不受影响，说明这是该 Mod 自身的故障而非与其他 Mod 的冲突，并请玩家报告此问题。它与绑定一样只记录一次，因此下次读档时再次出错的功能每次游戏期间只报告一次。它与 `CompatibilityFailure` 都是 `ReportedFailure`，即两种通知界面共同绘制的密封类型。
- **`ShownMapTab.isMapTabShowing()`**：星图是否显示在屏幕上，在无法遍历控件树时按未显示处理（失败即关闭）；这是覆盖在星图之上、必须随星图一同撤下的面板所用的读取。`CompatibilityNoticePanel` 通过它读取。
- **`VerticalRadioSpec`**：自上而下堆叠的一列选项单元，其中一个亮起；超过两三个选项的选项组应呈现为这种形式，因为同样的选项排成一行时，文字会窄到难以区分。它不包含分段尺寸设置和尾随说明文字，二者都仅适用于行。
- **`RadioSpec`**：`HorizontalRadio` 与 `VerticalRadio` 共同归属的密封接口，承载二者共同遵循的重选规则。处理任意单选组的读取器只需指明它，而不必分别指明每种排列方式。`HorizontalRadio` 其余部分不变，现有的每个调用点都可原样编译。
- **`ScrollingSectionSpec`**：正文中可滚动的一段，容纳任意控件，而不是某一个控件的属性。标题、其下的列表以及旁边的行现在会在高度受限的布局留给它们的视口内一起移动，而此前只有单个列表可以滚动。
- **`Control.isScrolled()`**：已布局的控件是否被放在该区段内；裁剪渲染器和受视口限制的命中测试读取的正是这一个值。未带此值构建的 `Control` 是固定的，因此现有的三参数构造方式不变。
- **`MemoryKeyAddress`、`AddressedMemoryFlag`、`AddressedMemoryString`**：存储值按使用方声明的轴上的每个点各保存一份，而不是每个存档一份。持有者声明其基础键，并指明每次读写所指的地址；键在一处组合，因此任何持有者都不会以不同方式拼写各段，也不会漏掉一段而悄悄与另一持有者共用一个槽位。
- **`KmlibStringKeys.get()` 和 `format()`**：绑定到 KMLib 自身类别的查找，因此调用点只需写出键，不必在旁边重复类别。
- **`KmlibStrings.requireText()`**：`Objects.requireNonNull` 的拒绝空白版本，用于作为名称或句子的组成部分。
- **`GlMatrix.FLOAT_COUNT`**：一个 GL 矩阵所占的十六个浮点数，为每个据此确定缓冲区大小或拒绝错误大小数组的读取器和写入器统一声明一次。`ModelviewMatrixReader.MATRIX_FLOAT_COUNT` 现在取自它。
- **`GlMatrix.createIdentity()`**：不做任何变换的矩阵，每次调用都返回独立的数组。它因两个互不相关的原因被读取，此前每处都各自写出：一是作为模型视图矩阵不对应任何渲染通道的标志，二是作为组合变换的基础。它以对角线形式写成，而不是十六个字面量，因为对布局的误读正藏在后者之中。
- **`CampaignCountdown`**：从起始时间戳开始的一段战役天数，对照运行中的时钟读取剩余天数以及是否已完成。它带有完成余量，因此由不均匀的帧步长驱动的倒计时可以在玩家预期的那一帧完成，而不是晚上零点几天，并且对它的每次读取都对完成时刻达成一致。时钟在每次读取时传入而不是被持有，因此倒计时只是普通数字，不指向任何星域。`BaseExpiringIntelPlugin` 通过一个无余量的倒计时读取其时间窗口。
- **`Colonies.selectColonies(test)` 和 `Colonies.hasAnyColony(test)`**：对殖民地集合的两种遍历，定义在集合本身上，而不是在其旁边手写。筛选保留集合自身的顺序，模仿原版平局规则的使用方正是按此顺序裁决争夺；判空读取在第一个通过的殖民地处停止，因为扫描时会对星域中的每个地点询问它，绘制星图时还会逐帧询问。未提供测试条件时不通过任何殖民地，宁可不予显示，也不报告一个无人要求显示的殖民地。
- **`PersistedChoice` 和 `PersistedChoices.fromKey()`**：存档以其自身的键存储的选项，以及把存储的键读回为选项的唯一查找：在没有存储任何值，或没有选项对应该键时（无论该键是旧版本留下的还是其他 Mod 写入的）进行回退。它是 `LabeledChoice` 的对应物，用于选项自己拥有的键，而不是 LunaLib 存储的标签。`SortDirection`、`ListColumns` 和 `ListSortMode` 都是持久化选项。
- **`SpreadsheetRows`**：游戏合并后的电子表格的行，读取为数据行，并将列表值单元格拆分为各个条目。合并器会把作者用于分隔表格的空 ID 行和用 `#` 注释掉的行与其他行一样返回，因此不识别它们的读取器会在一个本不是数据的行上失败，或把注释当作 ID 归档；这里在一处将它们排除。它接收行而不是打开文件，因为读取失败是否致命由调用方决定。`FactionSourceMods` 通过它读取 `factions.csv`。
- **`TooltipRow.createRow(List<LabelRun>)` 和 `LabelledRow.createRow(List<LabelRun>)`**：基于已组合成多个文本段的标签构建的裸行，供持有整句的调用方使用。它与以第一个文本段开始、再逐个接续其余文本段所得到的行相同，但只构建一份，而不是每段一份。
- **`CampaignMonth`**：战役日历中的一个月，从调用方传入的任意时钟读取；以及 `formatKey()`，即每月一次的任务记录上次运行时所用的 `<cycle>-<month>` 拼写。该键会写入存档，因此只在一处拼写：同一个月若有两种拼写，各自都会把对方的记录读成从未运行过的月份。
- **`Jitter.roll(jitterSize, random)`**：从调用方的 `Random` 抽取的同一区间，用于必须能按种子重放的抽取。两种形式共享同一个把抽取值映射到区间的方式，因此无论由哪个来源抽取，同一尺寸的含义都相同。区间的上端是开区间，一向如此；文档现在已注明这一点。
- **指明星域的 `SectorMemoryString` 和 `SectorMemoryFlag` 形式**，以及其下的 **`SectorMemoryAccess.readSectorMemory(sector)`**：每次读写都接收其作用的星域，供由选择星域的一方传入星域、而不是读取当前运行星域的代码使用。它们的行为与无星域参数的形式相同，包括空值守卫，因此区别仅在于值落入哪个存档。
- **`BaseExpiringIntelPlugin.findActive(sector, intelClass)`**、**`isExpired(clock)`** 以及接收时钟的构造函数：针对调用方持有的星域打开、查找并度量的限时情报。无参形式通过它们读取当前运行的星域，因此二者对"仍在时间窗口内"遵循同一定义。
- **`FaceLineHeightReader`**：字体的已安装图集按 1:1 绘制时的尺寸，即其描述文件的 `lineHeight`，从已加载的字体读取，而不是写死在代码中。覆盖 `starsector-core` 的核心本地化会以相同的基本文件名替换游戏的若干图集，而它的各个版本分支对行高的改动各不相同，因此没有一个常量能适用于每个安装环境。**`LazyFontLineHeightReader`** 是运行时适配器，通过 `LazyFontCache` 读取 LazyLib 对描述文件的解析结果，对无法加载的字体返回 `NO_LINE_HEIGHT`，正如文本跨度测量器对这类字体不返回宽度。`FaceLineHeightReader.isFaceLoadable()` 将这一结果与已加载字体的读数区分开来。
- **`TextFace.createNativeFace(atlas, lineHeights)`**：按已安装图集原生尺寸构建的字体，供本身没有指定尺寸的调用方使用；以及 **`InstalledFaces.createNativeFace(atlas)`**，通过 LazyLib 从当前运行游戏的安装环境读取同一尺寸。**`InstalledFaces.createFaceResolver()`** 是基于已安装图集的解析器。
- **`GlyphCoverageReader`**：字体的已安装图集能否把一段文本的每个字符都按其本身绘制，而不是绘制为后备问号。原版图集都不含 CJK 字形，而核心本地化的各个版本分支对每个被替换图集包含多少字形各不相同，因此只有已加载的字体才能回答。**`LazyFontGlyphCoverageReader`** 是运行时适配器；当 LazyLib 以字体的后备字形回应某个字符时，它将该字符计为未覆盖，因此 LazyLib 自己把排版引号重绘为直引号的情况会被视为已绘制。空白字符不在检查之列。
- **`FaceResolver`**：决定文本用哪种字体绘制。调用方指明它想用的字体以及要绘制的文本；如果该字体的已安装图集能加载且包含每个字符，解析器就保留该字体，否则继续查找第一个能绘制的字体：先沿字体家族向下，每个字号通过 **`StarsectorFont.resolveLowerResolutionFont()`** 指向下一个更小的字号，然后是游戏设置声明为默认的字体，最后是 **`FaceResolver.LAST_RESORT_FONT`**，即 `insignia15LTaa`。当回退路径上没有任何字体能绘制该文本时，返回的是最后手段字体，而绝不是声明的默认字体，因此任何设置都无法让损坏或缺失的文件成为 KM 的最终答案。它读取的是文本而不是语言区域，因此在已本地化的安装环境上运行的英文版本，在唯一一个没有任何本地化替换的图集中绘制本地化的势力名称时，会降到一个包含这些字符的字号；而在原版安装环境上则不会有任何变化。`listFallbackWalk()` 给出一段文本依次尝试的字体。
- **`GameDefaultFontReader`**：游戏自身的 `defaultFont`，按原版 `Fonts.DEFAULT_SMALL` 的方式读取。任何 Mod 都可以设置它，因此当它指向枚举中的某个字体时读取为该字体，否则读取为指向该路径的 **`DeclaredFontAtlas`**：语言包将其指向包含其文字的图集时，那正是这类文本所需要的字体。未指向任何内容的设置读取为 `insignia15LTaa`。**`StarsectorFont.findFontByPath()`** 负责这一映射。
- **`FontAtlas`**：KM 绘制所用两类图集之上的密封接口：一类是 **`StarsectorFont`**，即 KM 选择的字体；另一类是 **`DeclaredFontAtlas`**，即游戏设置声明的那一个字体。它承载加载器、测量器和绘制通道对二者所需的信息：路径和平滑方式。
- **`AtlasSmoothing.resolveFromInfoLine()`**：从图集描述文件的第一行读取其平滑方式（`aa=1` 为像素精确，其他均为抗锯齿），用于 KM 并未随附、因而没有对照表的图集。
- **`InstalledFaceCheck`**：在游戏启动时将每种字体以及游戏设置声明为默认的字体各加载一次，并为每种字体输出一行日志，说明其已安装的行高或无法加载；排查"文字以错误尺寸绘制"类报告时，正是对照这一读数。`KMLib_ModPlugin` 在应用加载时运行它。
- **针对传入的每个安装环境的字体检查**：`test` 会检查 KM 绘制所用的每种字体（在构建自身的安装环境上、在锁文件列出的每个本地化版本分支上，以及在 `-PfontInstallRoots` 指定的任何安装环境上），要求其头部能被 LazyLib 加载、只有一页、具有行高以及枚举为其声明的平滑方式，并要求每种字体针对该安装环境自身语言文本的回退路径都落在能绘制该文本的字体上；在中文版本分支上，`insignia42LTaa` 会降到 `insignia25LTaa`。不含任何字体的根目录会失败，而不是被跳过。
- **`checkFontEditions`** 和 **`font-editions.lock.json`**：每次 `test` 都在该门禁之后运行；门禁会按锁文件固定的 blob SHA 获取每个版本分支的描述文件，并覆盖到安装环境自身的描述文件之上。上游发布新的语言包会使构建失败，并指出版本分支、字体以及两个 SHA，直到 **`writeFontEditionsLock`** 重写锁文件中的字体为止。锁文件以 KM 自己的名称列出每个版本分支，以及其仓库、分支、描述文件目录和语言，并为每种语言给出一段探测文本；其中的字体就是枚举中的字体。每次构建都需要网络和游戏的字体。
- **`StarsectorFont.getBasename()`**：图集的基本文件名，日志行或失败信息以此指明字体。
- **`StarsectorFont.VANILLA_INSIGNIA_21`** 和 **`VANILLA_INSIGNIA_25`**：正文字体的中号与较大字号，即文本从 `insignia42LTaa`（核心本地化唯一没有补上其文字的字号）向下降级、在退到游戏默认字体之前经过的各级。
- **`SettledFaceMemo`**：记录每段文本最终落定的字体，在其所依据的文本保持不变期间一直保留。调用方指明某种字体要绘制的文本种类（使用自己的键）以及每种文本的读取器；每种文本只读取一次，每个字体与文本种类组合只解析一次，发生变动的字体只记录一次日志，指明所请求的字体、文本种类以及返回的字体。落定需要读取每段文本的每个字形，因此调用方应每个星域持有一个备忘，而不是每次绘制都重新解析，并在文本变化时调用 `discardFaces()`。构造函数拒绝缺失的解析器来源或读取器；读取器对某种文本返回 null 时，该种文本视为不含任何文本。`createUnsettled()` 保留每个所请求的字体，供没有文本可读的调用方使用。**`InstalledFaces.createFaceMemo(reader)`** 是基于已安装图集的备忘，首次落定时才构建其解析器。
- **用于落定字体的文本读取器**：**`SectorStarSystems.listSystemNames(sector)`**，每个星系的名称；**`SectorMarkets.listMarketNames(sector)`**，每个市场的名称。二者都读取全部内容，无论是否已被发现，因为玩家尚未见到的名称，正是之后同一星域中某个字体要绘制的名称；星域为 null 时不读取任何内容。**`StarsectorStrings.listCategoryStrings(category)`** 读取 `data/strings/strings.json` 中的某一类别，读取的是游戏在所有已启用 Mod 之间合并后的结果，因此替换这些字符串的翻译 Mod 所提供的内容就是读到的内容；文件或类别无法读取时发出警告并不读取任何内容。
- **`KmlibStrings.collectTexts(items, readText)`**：按顺序取每一项给出的文本，省略 null 和空白。上述读取器以及其他任何读取"这里可能写着什么"的代码共用这一个循环。

#### 势力

- **`FactionAlliances`**：哪些势力站在一起，表示为每个结盟势力所属的联盟，并在其上提供 `areFactionsAllied`，以及用 `buildFrom` 将一组记录反转为该结构。原版不保存这种关系，因此这就是它从维护它的任一 Mod 传来时的形态。它只说明谁与谁站在一起，仅此而已；联盟意味着什么由使用方决定，因为保守秘密、发动战争和共享市场会出于不同目的读取同一份成员关系。
- **`AllianceRecord`**：扁平化为纯数据的一个联盟：它的稳定 ID、显示名称，以及按市场规模降序排列的成员。每个字段都是读取时拍下的快照，而不是指回维护该联盟之物的实时句柄。
- **`AllianceSource`**：这些记录传入所经由的端口，使归并或权衡它们的代码无需游戏环境即可运行。之所以是端口而非快照，是因为联盟会在游戏过程中建立和解散。
- **`NexerelinAllianceSource`**：以这些记录呈现的 Nexerelin 实时联盟，位于存在性门禁之后。为此唯一提及 `exerelin.*` 的代码位于一个由门禁延迟加载的独立类中，连方法签名都不会触及它，因此未安装该 Mod 的环境永远不会查找 Nexerelin 的类；使用方归并记录时也无需知道是哪个 Mod 产生了它们。
- **`FactionNames`** 和 **`FactionNameForm`**：在一处读取势力的设定名称。`resolveName` 按设定原样读取简称或全称，势力不存在时为 null；`resolveFullestName` 读取全称，全称为空时读取简称，结果经过修剪且永不为空。`listEveryName` 读取每个势力的全部设定名称（简称和全称），同一文本只读取一次，供必须覆盖所有名称的字体使用。`FactionListingReport` 通过它读取全称。
- **`StarsectorFactionColours.findPalette()`**：势力的亮色与暗色配对，没有灰色后备，在星域或势力不存在时为 null；适用于对无法确定的势力不绘制任何内容的调用方。`resolvePalette` 经由它进行后备。

#### 几何

- **`VertexWelder`**：按容差判断哪些角点报告属于同一个角点，使分别计算出的边可以作为精确 ID 比较，而不必在每一步都比较距离。原先是 `EdgeRings` 中的私有实现，在第二个调用方需要相同功能后被提升出来。按边长为一个容差的网格分桶，因此一次查找只扫描九个方格而非整个集合；保留的是某个角点的第一次报告，因为取平均会在边已经焊接到该角点之后移动它。
- **`Disk.measureSagitta(radius, segments)`**：近似圆盘的多边形在最坏处向内偏离圆盘多远。这就是针对该圆盘绘制的任何内容的实际分辨率，因此也是使用方据以焊接或丢弃小特征的数值。它由半径和线段数直接得出，因此是读取而非重复表述；使用方 Mod 中对它的四处重复表述本应同步变动，却没有。它是静态方法，接受这两个参数：圆盘中心的位置与之无关，而且每个持有这两个值的调用方都是将其作为可调参数持有，而不是作为一个可供询问的圆盘。
- **`Segment.readStart()` 和 `readEnd()`**：以 `{x, y}` 点的形式给出线段的两端，是按角色命名两端的值与旁边接受数组的点运算之间唯一的转换处。每次读取都返回独立的数组，因此将其保存为角点的调用方不会遇到它被暗中改动。
- **`Segment.joinPoints(start, end)`**：从一个 `{x, y}` 点到另一个点的线段，是与上述两个出口相对应的入口，因此调用方无需从两个数组中逐一写出四个坐标。线段持有的是值，因此调用方之后复用其数组不会移动它。
- **`PolygonRegions.countSelfCrossings(ring)`**：一个环与自身相交的次数，这决定了它究竟能否绘制：折叠的环填充出的形状不同于其轮廓，描边时会有一条线穿过自身内部。返回计数而非标志，以便区分变差了的夹具和从未干净过的夹具。相邻的边不参与判断，因为它们按构造共享端点；相隔更远、仅在一点相接的两条边则计入，因为被挤压到与自身接触的环并不比穿过自身的环更简单。每一对边都会比较，因此开销是环长度的平方，应当用于测试或探测器中，而不是每帧绘制的代码中。
- **`PolygonOffsets.removeReversedLoops(polygon, isCounterClockwise, windowVertexCount)`**：被告知环本应绕向的折叠拼接器，供知道绕向的调用方使用。一旦折叠超过主体，净有符号面积就不再是正确的依据：一个两端被内缩超过自身宽度、中间却没有的环，会变成两个大的反向环路围绕一个绕向正确的较小主体，而按净面积判断，被移除的会是主体。按构建时的绕向询问，剩下的部分就能说明内缩吞掉了整个环还是只吞掉了两端。两种形式现在都会拼接跨越环起点的折叠，即围绕交点的环路，而非两条边之间的那个；此前，折叠落入哪个环路取决于环恰好从哪里开始。并且两者都只拼接不自交的环路，先处理最内层的折叠：一个两端都折叠的环，在任一交点处切开时，会分成该端的折叠和仍带着另一端折叠的主体，而此前被移除的正是主体。这项简单性检查对每个候选环路都要计算一次交点数，与环路角点数呈二次关系；在窗口限制下这只是常数因子，而比较每一对时则不是。
- **`Points.measurePathLength(points)`**：经过给定各点的路径从头到尾的长度，路径不闭合；这正是此前每个读取一段路径长度的代码都在自行编写的逐段求和。
- **`PolygonOffsets.hasInsetCollapsed(rawRing, insetRing)`**：判断斜接内缩是否把环折叠了，而不是对其偏移：剩余角点太少、面积趋于零或符号翻转，或者外环变大了。原本是某个使用方的私有检查，在第二个由逐边斜接塑形的主体需要同样的判定后被提升出来。须在任何边界解析之前询问，因为单独一个绕向错误的环交给细分处理后，会被调整绕向作为填充交回，连同其中所有的洞，而不是被丢弃；只有原始环还在可供比较时，才能识别出折叠。
- **`PolygonShapes.computeRegularVertices`**：根据中心、半径、边数和起始角计算正多边形的顶点，形式为 `{x, y}` 对或 `Vector2f`。这是绕中心逐个步进顶点的唯一实现；`LabelledPolygon.createRegularPolygon` 通过它从正 x 轴开始生成其裁剪形状，而以一个顶点立着的形状则传入自己的起始角。
- **三种度量的 `Vector2f` 形式**：`Points.computeMeanOfVectors`、`PolygonRegions.isPointInsideRing(ring, point)` 和 `Rectangle.computeEnclosingRectangle`，供在游戏的浮点界面坐标中布置形状的调用方使用。每个都通过与其 `{x, y}` 形式相同的遍历读取点，而不是把点复制过去，因此两者不会产生分歧。包围盒以 `Rectangle` 而非 `Bounds` 返回，因为屏幕上布置的盒子是按一个角和尺寸来放置的。均值方法另起名称而非重载，因为 `List<Vector2f>` 和 `List<double[]>` 擦除后是同一个参数类型。

#### 第三方兼容性

对第三方代码的绑定一旦失效，现在损失的是本次游戏期间基于它构建的功能，而不是渲染通道或加载过程；玩家会在游戏内收到一次通知，以一组带标签的行列出失去功能的 Mod、两个版本、损失了什么以及没有损失什么，其标题会指引玩家查看日志了解技术细节。两类绑定通过同一渠道报告：对 Fast Rendering 桥接层的调用，以及与另一个 Mod 集成的启动步骤。[`starsector/compatibility/`](../../src/main/java/kmlib/starsector/compatibility/README.md) 阐述了其工作方式以及为何如此设计；下面的条目只说明新增内容。

- **将构建所针对的版本写入 jar**：构建会把桥接适配器进行类型检查时所针对的 Fast Rendering 发布版本作为常量生成到 jar 中，这样不匹配报告就能写明分歧的双方。针对桥接存根进行的构建不写入版本。
- **`CompatibilitySubject`** 和 **`CompatibilityFailure`**：报告所涉及的第三方及其两个版本，以及对它的一个已失效的绑定。主体还会按数字段比较已安装版本与目标版本的关系（落后、领先、相同或无法比较），而这只影响措辞，因此第三方自报版本有误，代价只是一句话而不是行为。失败会以 `CompatibilityNoticeLine` 的形式提供给玩家的通知：一个标题，写明哪个 Mod 无法与哪个第三方集成；一段诊断，视版本情况建议更新、降级或等待（若两者指向同一发布版本，则请求向该 Mod 自己的开发者报告，因为没有可以切换到的版本，而集成中的故障恰恰在版本匹配时依然存在）；各行内容；以及一行指向日志的结束语。每一行都带有其文本以及其中需要突出显示的片段，按阅读顺序排列。日志中的区块以相同的标签、相同的顺序承载相同的行，取自字面量。四个组成部分属于四种不同的类型，因此任意两个都不会被调换；未读到的版本会显示为明确的未知，而不是 `null`。
- **`CompatibilityBreakage`**：哪个守卫捕获了绑定，以及什么不再成立，合为一个值。两者单独都无法诊断任何问题。
- **`CompatibilityConsumer`**：采用绑定的 Mod：其 Mod ID、该绑定服务于它的哪个功能，以及说明该功能损失了什么、没有损失什么的句子。锁存键由 ID 和功能组合而成，而不是整体提供，因此两个 Mod 不会拼出同一个键。两句话都属于采用绑定的 Mod，绝不属于程序库；ID 仅在撰写报告时才对照 Mod 管理器解析。
  - **`resolveConsumerAtPosition()`**：同一个 Mod 和同样的句子，但功能键带有编号；当一个 Mod 在同一个键下登记了两个功能时，记录交给描述器的就是它。
- **`CompatibilityFailures`**：本次会话的记录，按第三方、使用方 Mod 和功能损失句子锁存，可从任意线程写入，并由报告器逐个取出失败。失败由描述器构建，而描述器只会在被保留的记录上调用，因此反射探测或版本读取对每个绑定只付出一次代价，而不是每帧一次。
  - **被一个 Mod 重复使用的功能键会报告两个功能**：锁存由主体、使用方键和功能损失句子共同构成，因此一个 Mod 若为两个功能写了同一个功能键，两个功能都会被报告，第二个以 `<feature>-2` 报告。区分第二个功能与同一功能再次记录的是那句句子，而日志中带编号的键会在作者能看到的地方指出这种重复使用。
  - **`CompatibilityFailures.SESSION_RECORD`**：每个绑定都写入的那一份记录，按会话而非按星域持有。`KMLib_ModPlugin` 在每次加载游戏时安装取出它的通知。
- **`InstalledMods.readModName()`** 和 **`readModVersion()`**：游戏为某个 Mod ID 保存的显示名称，以及该 Mod 声明的版本；游戏无法回答时则不返回任何内容。与旁边的 `ModPresence` 一样受守卫保护，因为调用其中任一方法的调用方都是在撰写报告。
- **`WiringSteps`**：Mod 启动接线中单个步骤运行时所处的守卫，位于 `starsector/startup/`。抛出异常的步骤只损失它自己的注册，而不是其后的每个步骤或在其后加载的每个 Mod。无法链接其绑定目标的步骤也是如此，第三方契约的变更正是以这种方式出现的：以 `LinkageError` 的形式，而非异常。它以执行接线的 Mod 自己的日志记录器构造，而不是自持一个，因为通过 `LunaLogLevelBinding` 设置的级别作用于一个包子树。一个方法运行不绑定任何玩家可处理之物的步骤，并记录堆栈跟踪；另一个方法接受集成，在报告的同时以一行记录日志，把堆栈跟踪留给报告。
- **`ModIntegration`**：启动步骤所绑定的第三方 Mod，以及该步骤未能生效时执行接线的 Mod 所损失的内容；它记录根据所抛出之物构建的失败，并回答记录是否保留了它。版本对的方向与渲染器补丁的绑定相反：没有任何代码针对可选 Mod 编译，因此目标版本一行显示为未知，而已安装版本则从 Mod 管理器读取。它以提供器的形式交给守卫，因此在没有任何故障的安装环境中，措辞和版本读取都不会出现在加载路径上。
- **`KmlibMod.MOD_ID`**：程序库自身的 ID，与插件分开保存，这样类可以表明自己的归属，而无需为了一个字符串加载 `BaseModPlugin` 子类。程序库以它登记，作为自身兼容性渠道的使用方。
- **`NexerelinPresence.MOD_NAME`**、**`RandomAssortmentOfThingsPresence.MOD_NAME`**、**`KmlibLunaSettings.LUNALIB_MOD_ID`** 和 **`LUNALIB_MOD_NAME`**：每个第三方的身份都放在绑定它的代码旁边，因此按 ID 锁存的记录和点名该 Mod 的报告不会偏离成两个不同的 Mod。LunaLib 的这些常量位于 `settings/` 而非 `mods/` 下：该目录树用于使用方可以不运行的 Mod，而 LunaLib 是声明的依赖项。
- **未能集成的启动步骤会被报告，而不只是记录到日志**：`KMLib_ModPlugin` 将其 LunaLib 设置绑定、Nexerelin 例程以及 Random Assortment of Things 访问路径，以程序库自己的 Mod ID 登记到兼容性渠道。在此之前，抛出异常的注册会被吞进日志，启用了某个 Mod 的玩家只有在缺少它的情况下玩过一段游戏后，才会发现它没有集成。兼容性通知自身的安装仍然只记录日志：报告器安装失败时，没有地方可以报告。
- **首次调用时才失效的集成，与安装时失效的集成一样会被报告**：可选 Mod 的适配器只在首次被调用时才接触该 Mod 的类型，因此底层已变动的 Mod 会在建立殖民地、移交、计数器决策、可达性读取或联盟读取时被碰到，而不是在加载时。每一处都运行在一个边界之后，该边界在本次游戏期间移除出错的适配器并报告一次，与安装失败归入同一份报告。链接失败时，程序库会以自己的流程代替运行，因为适配器的工作尚未执行；而在建立、移交或计数器决策中途抛出的异常则会转交给调用方，因为此时殖民地处于两种流程都不会产生的状态。联盟读取失败时返回没有联盟，与未安装 Nexerelin 时相同，并归入一个单独的功能。
  - **`IntegrationFailureReporter`**：无论由哪个边界捕获，都将绑定的失败作为其集成的报告登记，且绝不抛出异常，因为描述器属于绑定方 Mod，和绑定本身一样可能失败。它还决定堆栈跟踪的去向：边界记录一行日志，报告的区块承载堆栈跟踪，只有在不会有区块的情况下（无法撰写的报告，或已报告绑定的再次失败），报告器才自行记录堆栈跟踪。
  - **`describeIntegration()`**（位于 `NexerelinIntegration` 和 `RandomAssortmentOfThingsIntegration` 上）以及 **`KmlibLunaSettings.describeLunaLibIntegration()`**：每个集成的报告，与绑定它的代码放在一起，因此守护安装的守卫与其注册的适配器以同一份描述报告，属于同一份报告。`describeLunaLibIntegration(CompatibilityConsumer)` 将 LunaLib 的 ID 和名称与另一个 Mod 的使用方配对，因此报告自己 LunaLib 绑定的 Mod 只需写明自身及其损失。
  - **`ExtensionPoint.offerWork()`**：在该边界内将工作交给已安装的实现，并处理其结果。被移除的实现保留其注册时的后备策略，因此禁止后备的实现会继续拒绝运行，而不会放行常规流程。
- **在星图界面上发现的失败就在那里告知**：`ScreenCompatibilityNotices` 在发现失败的那一帧，于核心界面自身的控件树中立起一个面板，凡它不接手的都交给对话框。挂在该树中的面板由持有它的界面推进，而星域上的瞬态脚本在核心界面打开期间完全不会被推进，这就是从地图渲染通道中发出的报告需要自己的显示面的原因。两者都从同一份记录中取出，因此不会互相重复，也不会在两者之间丢失任何内容。只有游戏自身线程运行的守卫才会尝试弹出；在延迟渲染器线程上失败的绑定留给对话框处理。
  - **`CompatibilityNoticePanel`**：即该面板。它由游戏自身的控件构建，并通过 `CoreUiOverlayPanels` 立起，自行绘制背景和框体，用游戏为自身对话框加框的基础颜色为框体加框，并截获其控件未处理的事件，因为以这种方式添加的面板背后，游戏不会进行任何变暗处理。它以游戏自身提示窗口相同的节奏淡入淡出，因此其下方自行变淡的内容会沿同一曲线变化，而不是被切掉再突然恢复；按下时它将界面交还，并在淡出期间继续绘制，此时不截获任何事件。有四种关闭方式：按钮、Escape、Enter 和空格键，与游戏自身的单按钮对话框一致，后者把键盘确认和键盘取消都绑定到唯一的选项上。
  - **`ModalOverlays`**（位于 `starsector/ui/coreui/`）：本程序库在核心界面上弹出、且在显示期间占据该界面的叠加层。`CoreUiDialogView` 将它与游戏自身的模态基础一同读取，因此任何为游戏对话框让步的东西（侧边栏的输入、针对模态的淡化）都会在同一次读取中为本程序库的面板让步，其自身无需任何改动。叠加层在淡出过程中与占据界面时同样计入，因此跟随者能获得完整的曲线直到结束。
  - **`OverlayPresence.isShowing()`**：叠加层是否有任何部分显示在屏幕上，范围比它是否占据界面更宽。跟随淡化的内容询问这个；路由输入的代码询问那个标志。
  - **通知中的强调按含义命名，而非用标记书写**：名称、版本和日志的文件名被突出显示；出问题的部分以警告显示，包括说明失败的短语、安装所处的状态以及修复的指示；无论如何都会继续工作的部分以平和样式显示；其余部分按普通文本显示。Mod 在任何被提及之处都会突出显示，包括警告内部，因此降级或等待的指示会被拆分成多个片段，而不是整体以警告显示；其中的版本仍属于警告的一部分，因为那是告诉玩家要切换到的版本，而不是不匹配的一方。每个突出显示的片段都是填入模板的值，因此组合时就已掌握它，无需解析措辞；需要突出显示的短语有自己的字符串键。片段按阅读顺序给出，因为引擎会从上一个片段结束的位置开始匹配每一个片段，这也使得一个名称可以先作为突出内容出现，然后再次出现在警告短语中。游戏自带的字体没有一个是等宽的（在 `victor14` 中，`i` 前进两个像素，而 `M` 前进七个像素），因此在屏幕上区分一行的值与其标签靠的是颜色。
- **`CompatibilityNotice`**：每帧运行的瞬态脚本，从该记录中取出失败，并以带单个按钮的游戏自身确认对话框显示每一个失败，在请求对话框之前先写入报告行。对话框的尺寸按通知可能出现的最长形态确定，而不是凭目测，因为它无法随内容增大：已安装版本无法读取时，其诊断会达到三行，在该宽度下以游戏默认字体换行后共十九行。尺寸过小会静默截掉结束行，而那正是指向日志的一行。每帧一个对话框。使用确认对话框而非消息对话框，因为游戏正是以此弹出自身的单按钮通知，而且两者中只有它接受尺寸参数并能回答是否已打开；被拒绝时会记录日志，因此玩家从未看到的模态窗口也会留下痕迹。
- **`UnavailableModelviewMatrixReader`**：第三个 `ModelviewMatrixReader`，其每次读取都没有读数。这不是新的调用方契约：`CampaignMapTransform` 在缺少读数时本就会原地停留。
- **`FastRenderingBridgeDiagnostic`**：桥接绑定失败后，六个镜像成员中哪些不再成立及其原因，并附上已安装 jar 报告的版本和构建时进行类型检查所针对的版本。在正常路径上从不运行。
- **`ModelviewMatrixReaders` 中受守卫的绑定**：Fast Rendering 分支在 `LinkageError` 守卫下执行，一个 catch 即可同时覆盖类移动、成员移动和签名变更。不再能链接的绑定现在会降级为 `UnavailableModelviewMatrixReader` 并记录一次失败，而此前它会从最先触及它的渲染通道中抛出，终止该通道，并在堆栈跟踪中留下 KM 的类名，玩家随后便归咎于 KM。
- **桥接适配器绑定到 Fast Rendering `v0.9.0`**：`Context.exec`（模型视图矩阵副本经由其入队的成员）的类型为该版本引入的 `bridge.context.executor.Executor` 接口，存根、诊断器和版本标记随之跟进。字段按其类型链接，因此同一个 jar 在 `v0.8.10` 及更早版本上无法链接：在那里绑定会降级并报告一次，与任何移动过的成员一样。
- **`FastRendering.COMPATIBILITY_SUBJECT_KEY`** 和 **`COMPATIBILITY_SUBJECT_NAME`**：对该渲染器的失败绑定所记录的身份，以及报告为其显示的名称，公开出来以便每个绑定方都以相同方式书写。
- **桥接层在调用处失效也会降级，而不仅限于链接处**：绑定完成链接后，若 Fast Rendering 入口点抛出异常，此前会以致命错误的形式到达玩家面前：在下一帧，从一个没有任何 KM 栈帧可归咎的调用栈中抛出；若由游戏线程调用，则直接从渲染通道本身抛出。现在两种情况都只损失地图的光标读数：本次游戏期间该读数锁存为不可用，不会以过时的矩阵代替报告，且无论地图保持打开多少帧，都只记录一次失败。这一点在 Fast Rendering `v0.8.9` 及之后最为重要，其外观层声明了 LWJGL 的全部接口，并拒绝其未实现的部分，因此这类故障能顺利链接，链接时的守卫永远察觉不到。其机制详见 [docs/dev/rendering-environment.md](../../docs/dev/rendering-environment.md)。
- **每个基于已损坏绑定的 Mod 都会被告知其损失**：`ModelviewMatrixReaders.selectForActiveRenderer()` 按使用方分别解析，因此读取地图的第二个 Mod 会收到报告，而不是被交给第一个 Mod 的绑定。此前第一个调用方的选择会在本次游戏期间一直保持，之后每个 Mod 的玩家都不会被告知任何事情。
- **对游戏自身代码的访问失效时会被报告，以游戏作为第三方**：地图视图状态背后的控件遍历和具体类读取、情报信息界面的地图、筛选栏及其按钮、立于核心界面之上的面板、界面之上的模态窗口或数据百科、地图的提示框及其重绘，以及地图绘制图标的顺序，各自都会通过因此受损的 Mod 的报告器登记失败。报告会列出该 Mod 声明的游戏版本与正在运行的版本，因此运行的游戏版本并非该 Mod 所适配的玩家，会被告知更新或降级游戏，或等待该 Mod 更新。访问结果为空时，只有在该界面上空结果绝非正常答复的情况下才会登记（打开的情报标签页中没有事件面板，地图标签页中没有图标地图），因此单纯不显示地图的界面绝不会被报告为故障。只登记而不在界面上弹出：通知面板立于这些访问所遍历的同一控件树中，因此报告会等待战役的对话框。
  - **`GameReachReporter`**：单个使用方的报告器，接受该 Mod 的使用方作为一个仅在失败时才组合的描述器，只登记一次且绝不抛出异常。`COMPATIBILITY_SUBJECT_KEY` 和 `COMPATIBILITY_SUBJECT_NAME` 是游戏作为主体的身份。`UNREPORTED` 不登记任何内容，用于诊断性读取或自带后备的显示面。
  - **`InstalledMods.readModGameVersion()`**：某个 Mod 声明其所适配的游戏版本，与旁边的读取方法一样受守卫保护。

#### 控件行

- **`InteractiveSpec.isSegmented()`** 和 **`reselectBehaviour()`**：控件对自身的回答，取代了两串分别从外部推断这些信息的类型判断链。控件的单元是否分别命中，以及重新选择已点亮的单元时会发生什么，各自都是一条规则，有两个读取方：解析单元的命中测试，以及决定按下时是否生效的收窄判断；若在两侧分别表述，控件就可能被当作一排分段来命中，却作为整行来按下，或者反过来。现在每个交互变体都会回答这两个问题，因此不会有哪个变体遗漏其中之一。
- **`RowDimensions`**：每一行的高度与宽度，合为一个值。若作为两个列表传递，它们可能来自不同的读取：两个长度不同的列表，或者高度来自宽度从未测量过的那一组行，而堆叠器无从察觉。放在一起时，它们会在声明处相互校验。
  - **`ControlStripLayout.StripMeasurement.rowDimensions()`** 以这种形态返回一次测量的各行。

#### 本地化构建与发布

此处是测试夹具一节所述按语言区域划分的语言包的构建与发布部分，该节的本地化夹具会检查这些步骤写出的内容。

- **`writeLocaleFiles`**：将一个语言区域的语言包写入游戏读取的文件，由共享的 Starsector 约定为提交了 `localisation/manifest.json` 的 Mod 注册。`-Plocale=<tag>` 用于选择，否则构建清单中的默认语言区域。每个映射文件逐字节复制；若 Mod 提交了 `mod_info.base.json`，则将该语言区域的启动器片段合并到其上，仅限文本字段，任何功能性字段都会被拒绝。位于 Mod 根目录之外的映射，以及缺少某个映射文件的语言包，都会使构建失败。清单和片段通过 `shipped-json-reader.gradle` 读取，它是 `ShippedJson` 在构建时的对应物，基于同一个 `json.jar`，因此构建与检查对何者可解析的判断一致。`jar` 依赖于它，`test` 将其输出作为输入，因此切换语言区域会让两者重新运行。`test` 还将 `localisation/` 和根目录的 `CHANGELOG.md` 作为输入，因此只编辑译文也会重新运行一致性测试套件。
- **`mod_info.base.json`**：按语言区域保存启动器文本的 Mod 提交该基础文件，并在 gitignore 中忽略由其写出的 `mod_info.json`，因为有基础文件时，后者是构建产物：在全新检出时不存在，否则则是最后一次写出的那个语言区域的版本。每个构建脚本和每个发布 Action 都会在提交了基础文件时读取它，未提交时读取 `mod_info.json`。
  - **`mod-info-reader.gradle`**：在构建配置期间读取一次元数据，将其发布为 `modInfo`，将其来源文件发布为 `modInfoFile`，并提供用于读取另一个检出的 `readModInfo(File)`，均遵循该规则。约定会应用它，`writeVersionFile` 和 KMLib 版本报告都通过它读取，而不是自行解析该文件。
- **按语言区域发布**：对于提交了 `localisation/manifest.json` 的 Mod，`mod-release.yml` 会为每个语言区域发布一个压缩包，每个都带有自己的版本文件，所有语言区域的版本文件都附在压缩包旁边。jar 只构建一次，因为它在各语言区域间完全相同。未提交清单的 Mod 发布方式与以前完全一样。
  - **`read-locales`**：从清单中读取 Mod 发布的语言区域（标签、显示名称和核心本地化，默认语言区域排在首位）及其默认语言区域。未提交清单的 Mod 得到空列表。
  - **`package-release`**：将每个语言区域写入检出目录，组装并压缩其内容，填写其版本文件，并把所有资产收集到一个目录中。之所以是一个 Action，是因为这项工作是循环，而工作流无法循环 `uses:` 步骤，因此所有版本文件都由同一个脚本填写。每个压缩包都带有本语言区域的更新日志：已翻译语言区域的 `CHANGELOG.md` 取自其语言包目录，默认语言区域的取自根目录。
  - **`compose-locale-note`**：组成发布说明的两个部分。*Builds by language* 列表列出每个语言区域的压缩包，并链接该语言区域需要覆盖安装到 `starsector-core` 上的核心本地化，启动器无法检查这一依赖。在依赖行下方，每个已翻译语言区域中该版本的说明折叠在其显示名称之下。译文缺少该版本的章节会使发布失败。
  - **`fill-version-file-template` 接受可选的 `locale`**：下载 URL 指向该语言区域的压缩包，`masterVersionFile` 指向该语言区域自己的副本，因此安装后轮询的是指回其自身语言的副本。若模板的主地址不以 `<mod-id>.version` 结尾，则它指向的并非发布版本发布语言区域副本之处，此时会失败，而不是让安装指向不存在的位置。
- **KMLib 按语言区域保存自己的文本**：其字符串和设置表位于 `localisation/<locale>/` 下，启动器元数据位于 `mod_info.base.json` 中，游戏读取的副本由构建写出。`localisation/zh-hans/CHANGELOG.md` 翻译了本更新日志，涵盖每个版本和每个章节。`LocaleParityIntegrationTests` 以默认语言区域为准校验其声明的每个语言区域，正如每个使用方 Mod 的测试套件对其自身目录所做的那样。中文语言包的 `README.md` 是基础术语参考，收录程序库自身文本涉及的原版用词及其自有术语，每个使用方 Mod 的参考都以它为基础。

### 测试夹具

- **`SaveFormatFixture`**：通过 XStream（游戏写入存档所用的序列化器）驱动 Mod 的持久化对象：将一个对象图写出的元素路径排序后与签入的列表比对，并以加载时的方式读回该对象图。类或字段改名会使每个现有存档中的某个值成为孤立值，或导致其加载失败，而从不进行序列化的测试套件对这两种情况都毫无察觉。对象图引用但不拥有的协作对象在构建夹具时指明，并写为空元素，因此它们的字段仍会出现在路径中，而它们所持有的内容留给针对它们的测试套件。其接口上不出现任何 XStream 类型，因此使用方的测试套件只在运行时需要游戏的 XStream jar，而约定已将所有核心 jar 置于运行时。
- **测试 JVM 开放游戏 JVM 所开放的内容**：共享的 Starsector 约定在每个测试任务上将 `java.util`、`java.lang.reflect`、`java.text` 和 `java.awt.font` 开放给未命名模块，与游戏自身的 `vmparams` 一致。没有这些开放，XStream 1.4.10 无法在现代 JDK 下构造，而测试套件也不会因此获得运行中的游戏所没有的能力。
- **`CoreUiReachFailures`**：通过 `CoreUiTree` 的访问在其无法识别的游戏构建上失败的两种方式：游戏自身的失败被包装为受检异常并以未声明的方式抛出，以及某个成员不再能链接。每种方式都可应答任意类型，因此能替代以 supplier 形式传入的访问。若用例抛出的是最容易写出的非受检异常，那么面对捕获范围过窄的边界它也会通过。
- **`StarsectorSettingsFake` 应答游戏版本**：`answerGameVersion()` 应答运行中游戏的版本，`answerModGameVersions()` 应答每个 Mod 声明的游戏版本，`ModStateScopes.runWithGameVersions()` 则为单个 Mod 同时设置这两者。
- **`GameReachRecordFixture`**：一个将游戏访问的报告归档到用例自己的记录中的报告器，以及读回该记录的方法，因此探测器是否归档不取决于在它之前运行了哪些测试套件。
- **`LogAppenderFake.getThrowables()`**：按顺序给出一次捕获中各条目记录日志时附带的异常，略去未附带异常的条目；用于询问某个失败是否连同其堆栈跟踪一起记录的用例。
- **`LogAppenderFake.captureLogOf(loggingClass, capturedLevel, work)`**：将记录器保持在指定级别而非所有级别的捕获，用于关注某个级别放行什么的用例：例如受守卫的堆栈跟踪，在其级别关闭时必须不产生任何开销。
- **`StaticSeams`**：一个布置中已打开的静态接缝，退出时由内向外依次关闭，用于同时为多个类设置替身的布置。`openSeam()` 在一个类上打开一个接缝；`holdSeam()` 接管某个夹具以其自身应答打开的接缝（`Global` 的接缝，来自 `StubbedGlobalLogger.openGlobalAnsweringLoggers()`），使其在应有的位置与其余接缝一同关闭。
- **为 `Global` 设置裸替身会使构建失败**：Starsector 约定在所有 KM 包根下，只允许在 `StubbedGlobalLogger` 中使用 `mockStatic(Global.class)`，并且任何地方都不允许使用 `openSeam(Global.class)`。若某个类的静态记录器首次在不应答记录器的替身内被解析，它将在 JVM 的剩余生命周期内一直为 null，导致后续某个测试套件在它从未写过的代码行上失败；该失败会指出应改写成的调用。匹配依照 Java 对该调用的写法，因此 Kotlin 测试套件不受此约束。
- **列表控件夹具**：`Anomaly` 和 `AnomalySortMode`，即在列表包之外声明的选择器行和排序词汇；以及 `ListPickerBlockReads`，它深入已构建的选择器区块，读取列数选择器、排序行、排序选择器或条目列表。区块的顺序记录在这里，而不是分散在每个测试列表的测试套件中，因此向区块插入一行只会破坏一个文件。
- **`MemoryKeyAddresses`**：两个替身地址，供在某个轴上的某一点存储值、但并不关注该轴是什么的测试套件使用。
- **`CompatibilityFailureFixture`** 和 **`CompatibilitySlotTemplates`**：一个具有代表性的兼容性失败，用例要变动的每个槽位各有一个构建器；另有测试套件记录时所用的两个主体键和两个使用方（每个使用方都会失去另一方不会失去的东西，因此关于区分两者的用例无法凭一句同时代表两者的句子通过）；以及作为替身、暴露其槽位的通知模板，使关于记录、通知或措辞的测试套件只需指明其所关注的那一个槽位。`drainSessionRecord()` 清空进程自身的记录（其生命周期长于单个用例），供任何驱动向该记录写入的守卫的测试套件使用。`createFeatureFailure()` 构建代表性使用方自身的功能失败，`takeNextBindingFailure()` 则把记录中的下一份报告作为绑定失败取出，使读取其主体或损坏信息的用例无需强制转换。
- **`RendererModelviewMatrices`**：一次星图渲染通道的 modelview 矩阵，分别以渲染器持有它的两种布局给出，并附带平移量及其所在的列主序槽位。它是一项认知而非一个数值：哪个槽位承载平移量取决于询问的是哪个渲染器，因此测试套件自行重述它，就是在重述其所测代码存在的意义所要做对的那件事。`kmlib.opengl.FastRenderingTests` 刻意不使用它：该测试套件测试的正是转置，因此其输入仍完整写在断言旁边。
- **`StarsectorSettingsFake.installSettingsWithModNames()` 和 `ModStateScopes.runWithModNamed()`**：一个既按启用状态、也按名称识别 Mod 的 Mod 管理器，用于显示 Mod 自身名称的被测对象。若每个 ID 都应答为空，报告找到的名称与其退而使用的 ID 就是同一个字符串，因此关键的分支与无关紧要的分支无从区分。
- **`TooltipMakerFake`**：一个提示框元素，记录添加到其中的段落、段落间距、每段标签上高亮的文字片段以及按钮，而不进行任何布局。它是代理而非手写的替身，因为该元素的 API 过于庞大，不值得为正文所用的寥寥几个调用去实现；正因如此，关于面板由什么组成的测试套件才能在没有游戏的情况下运行。
- **`ShippedStrings`**：读取 Mod 随附的 `data/strings/strings.json`（引擎读取的唯一路径，以 `STRINGS_JSON` 公开）以及其持有类列出的字符串 ID，供将两者保持一致的守卫使用；也可按类别读取任意字符串文件，或将其类别展平后读取。文件通过 `ShippedJson` 解析，因此游戏读取的每个键在这里都会被读取，无论其写法如何；在两个类别中声明的同一个键会使展平读取失败，而不是让守卫与最后出现的那个比对。
- **`StringTemplates.countFormatArguments()`**：`String.format` 会从随附模板中取用多少个参数，供将模板槽位与填充它们的调用点保持一致的守卫使用。多出一个槽位的模板会渲染为后备哨兵值，少了一个槽位的模板在渲染时会悄然丢掉最后一个数值，因为格式化器会忽略多余的参数；对查找打桩的测试套件对这两种情况都毫无察觉。计数正是容易出细微错误的部分：位置说明符可以重复同一个索引，因此它意味着最高索引而非说明符的个数，而 `%%` 和 `%n` 根本不取用参数。
- **`SalvageEntityMock`**：将原版的掉落抽取器固定住，掌管静态模拟的生命周期，并捕获每次抽取的倍率、掉落列表和随机源。每次捕获都会同时锁定抽取次数，因此本应抽取一次却抽取了两次的代码会使用例失败，而不是凭第一次抽取的参数通过。只固定七参数的抽取器，因为静态模拟须为每个重载分别打桩。
- **`ShippedSpreadsheet`**：Mod 随附的 CSV，按测试套件所需的四种形式之一读取：按表头名称的行、以某个 ID 列为键的行、单列的值，或按位置排列的单元格行。放在这里而不是各 Mod 的测试套件中，是因为解析正是可能出细微错误的部分：随附表格会给含逗号的字段加引号，而按逗号拆分的读取会落到它本想读取的列左边一列，恰好落在那些加了引号的行上，并且会对着错误的单元格持续通过。按位置读取不是为了方便，而是某些表格唯一允许的方式：LunaLib 的设置表有若干列名为空，要求以该表头为键的解析器会直接拒绝该文件。ID 为空的行在按键读取中作为其本来的间隔行被丢弃，在按位置读取中则被保留，由按索引读取的调用方自行判断其行。`LunaSettingsTable` 通过它读取，而不是自行拆分行。Apache Commons CSV 作为 `api` 依赖随夹具变体提供，因为使用方需要在自己的测试运行时中使用该解析器来读回一行。刻意不做 JSON 整形：若某个 Mod 的表格解析器接受引擎的行对象形式，就在其自己的测试套件中从这些行构建该形式，因此读取表格的 Mod 都无需在测试编译类路径上放置 `json.jar`。
- **`ShippedJson`**：按引擎的方式读取 Mod 随附的 JSON：在游戏自身 `json.jar` 中的 org.json 之前，先执行引擎的 `#` 注释剥离（逐字符照搬）。游戏能加载的内容在这里都能读取，包括尾随逗号和注释；游戏拒绝的内容在这里也会失败：重复的键，以及字节顺序标记（会被明确指出，而不是报告为 "must begin with '{'"）。对象以排序映射返回，因为游戏的解析器不保留成员顺序，且没有任何 org.json 类型越过边界。附带读取用来声明其字段的形状检查，其中包括未知键检查，因此拼错的字段会失败而不是被当作缺失；当由读取内容构建的值拒绝所读内容时，会指出对应的文件。
- **`IntelManagerFake`**：一个保存添加到其中的情报的情报管理器，用于先添加情报、再向管理器查询该情报的被测对象：例如对照已记录内容检查的上限，或仅在缺失时才安装的条目。它采用游戏关于保留什么的规则：已持有的情报不会重复添加，已结束的情报根本不会添加，按类读取会按添加顺序应答所有可赋值给该类的情报。游戏在保存之外所做的事（标记玩家看到情报的时间、告知情报已显示或已移除、发布消息）需要深入运行中的游戏，留给针对这些行为的测试套件。
- **`FaceLineHeightReaderFake`**：从表中取得行高，用于在没有安装游戏的情况下组合原生字体。`createVanillaLineHeights()` 应答原版描述文件所声明的值，`answeringLineHeight()` 在同一基础名下声明一个不同的图集，供测试套件锁定某个尺寸是读取所得而非写死的。
- **`LazyFontLineHeightReaderMock`**：将实时行高读取器固定住，并从 `FaceLineHeightReaderFake` 应答，用于针对每次绘制时静态组合、且未传入读取器的外观的测试套件。
- **`GlyphCoverageReaderFake`**：根据规则给出字形覆盖。`createLatinOnlyCoverage()` 代表原版安装，每种字体只包含 Latin-1；`coveringEveryCharacter()` 扩大某一种字体的覆盖，正如本地化安装替换后的图集那样。
- **`StoredMemoryFake`**：单独的、以映射为后备的记忆，背后没有其他东西。`SectorMemoryFake` 以游戏的方式（通过 `Global.getSector()`）访问记忆，并为此在其生命周期内保持一个静态替身处于打开状态；而已持有自己的 `Global` 替身的测试套件，或将记忆挂在行星而非星域上的测试套件，则完全无法使用它，因为为同一类型设置第二个替身会抛出异常。这样的测试套件改为设置本夹具并自行附加记忆，得到相同的存储值以及相同的写入和移除计数。`SectorMemoryFake` 基于它构建，并通过它应答每次读取，因此其自身接口不变。

#### 本地化

Mod 面向玩家的文件可以按语言分别保存，每个语言区域一个语言包，位于 `localisation/<locale>/` 下，另有一份清单列出各语言区域、默认语言区域以及每个语言包文件在 Mod 中的落点。每个已翻译语言区域的目录中还有 Mod 的 `CHANGELOG.md` 的译文。这些夹具读取该布局并比较其中的语言区域。每个文件都按 Mod 随附副本的读取方式读取（JSON 通过 `ShippedJson`，字符串通过 `ShippedStrings`，设置表通过 `LunaSettingsTable`），因此语言包从不会以第二种方式解析。

- **`LocalisationDirectory`**：Mod 的 `localisation/` 目录。读取其根部的清单，列出清单旁的每个语言包目录（无论是否已声明），仅为清单声明的语言区域打开语言包，并在 Mod 提交了 `mod_info.base.json` 时读取清单旁的该文件。
- **`LocaleManifest`**：存在哪些语言区域、哪个是默认语言区域、语言包包含哪些文件以及每个文件的落点；它是这三者的唯一事实来源。它可能携带的每个键都是已知的，其他任何键都会被拒绝，因此拼错的 `coreLocalization` 会失败而不是被当作缺失。无论清单如何构建，其数据路径都限定在 Mod 根目录之内，因为向某个路径复制就是对仓库的一次写入。
- **`DeclaredLocale`**：一个已声明的语言区域：小写的 BCP 47 标签、以该语言自身书写的名称，以及（当原版图集缺少其字形时）其玩家覆盖安装到 `starsector-core` 上的 https 项目。
- **`LocaleBundle`**：一个语言区域的目录。读取其字符串文件、设置表和启动器片段，并且只按裸文件名解析其他语言包文件，因此任何语言包都无法触及另一个语言包。
- **`ModInfoFragment`**：语言区域所翻译的启动器文本：`name`、`description`、`author` 和依赖项名称，每项均可选，缺省时退回基础文件。任何功能性字段都会被拒绝，因为片段若改变版本或 jar 列表，就会为每个语言区域构建出不同的 Mod。`listFallbackFieldNames()` 列出片段留给基础文件的每个字段。
- **`ModInfoBase`**：片段能触及的 `mod_info.base.json` 内容：它退回使用的文本字段，以及它可以指明的依赖项 ID。
- **`LocaleParity`**：使每个语言区域与默认语言区域保持一致，每项检查给出的发现项都以应做的修改来表述。在缺口没有后备的地方严格要求：缺失的目录、文件、字符串或行，在任一语言区域中留空的字符串，取用不同参数的槽位，改变其存储内容的行，被拆分或合并的标签页，在未指明 `coreLocalisation` 时出现的 Latin-1 以外的文本，以及指明未声明依赖项的片段；并报告语言区域留给基础文件的启动器字段。Radio 的选项须完全一致，因为 LunaLib 存储的是所选的标签。已翻译的更新日志必须与根目录的更新日志逐点一致：版本标题及其顺序相同，各节相同，每一嵌套深度的列表项数量相同，每节的代码片段也相同。其行文不参与比较。每个缺陷只报告一次，清单未映射的文件不参与比较。`findAllMismatches()` 汇集所有检查，使 Mod 的测试套件只需断言一次；不随附设置表的 Mod 打开它时无需字段 ID 前缀。
- **`LunaSettingsTable.readBehavioursByFieldId()`、`readDisplayedTexts()` 和 `readTabsByFieldId()`**：将一行拆分为它做什么与它说什么：其 `FieldBehaviour`（类型、存储的默认值、选项和取值范围，由 `describeDifferencesFrom()` 逐列比较）、界面绘制的单元格，以及其标签页。声明了两次的字段 ID 会使按键读取失败，而不是被从中丢弃。
- **`StringTemplates.readArgumentConversions()`**：模板的每个槽位取用哪个参数、以何种转换取用，无论槽位以何种顺序书写；这正是同一字符串的两种措辞为了能由同一个调用点填充而必须一致的内容。
- **`ShippedJson.requireList()` 和 `locateElement()`**：对象检查与成员定位在数组上的对应方法。
- **`ChangelogOutline`**：Keep a Changelog 文件的结构：按标题列出的各版本、每个版本的各节、每节在每一嵌套深度上的列表项数量，以及每节的代码片段。不读取行文，因此只要没有条目只在一方被增删或改用其他标识符，译文与原文的结构就相同。

### 变更

- **Mod 列表中该 Mod 的名称为 Klark Morrigan 的程序库 (KMLib)**，使其他各处使用的缩写与全名并列。更新检查器显示同一名称。依赖 KMLib 的 Mod 可以在其 `dependencies` 条目中使用同一名称；启动器只在 KMLib 缺失时才显示它。
- **压缩包按语言命名：**`KMLib-<version>-en.zip` 和 `KMLib-<version>-zh-hans.zip`，并在 `kmlib.version` 旁附带 `kmlib-<locale>.version`。KMLib 的每种译文语言都作为独立的压缩包出现在同一个发布页面上，发布说明会写明哪个压缩包对应哪种语言。更新检查器在此变更前后均可正常工作：安装了更早版本的用户仍会收到此发布版本的通知。
- **`MapIconReseater` 会在日志中说明它观察到的情况。**在一次会话中逐渐退化的图层叠放无法从画面诊断，仅凭移动操作本身也无法说明为何某次移动不再生效。星图的打开与关闭以 DEBUG 级别跟踪，每次切换都附带自上次观察到图标未被遮挡以来的抬升次数，并在关闭时附带星图显示期间是否曾观察到图标未被遮挡。有两种状态以 WARN 级别报告，每种每次会话只报告一次，因此不会自愈的失败只占用一行，而不是每次打开都占一行：一种是星图保持显示、实体位于其所在位置，却没有可放置的图标，此时星图读取与放置读取对屏幕上的内容判断不一致；另一种是停用，附带导致停用的读数，从而说明抬升是从未被观察到，还是被观察到后又被撤销。全部记录在程序库自己的记录器上，`KmlibLunaSettings` 将该记录器绑定到 KMLib 的详细程度字段；包的 README 列出了这些日志行。
- **`VoronoiCellBuilder` 统一放置其单元共享的角点。**当相邻单元的边界线与半径界限相交时，两个单元现在会把该角点放在同一位置，该位置由两个站点和延伸半径计算得出，而不是由任一单元自身的种子多边形得出，因此两者完全一致，而非仅在容差范围内一致。种子是内接于界限的多边形，其平直的边过去会使这样的角点偏移，或在线段数较少时将其完全切掉；将相邻单元的边境边串接成一条轮廓的使用方，会在每一处这样的位置发现缺口，在线段数较低时缺口宽到足以让一个封闭区域渗入下一个区域。被切掉的角点现在会被补回：打断它本应位于其上的那段跨段，并让边界穿过该角点。

  因此，线段数只决定圆弧绘制得有多平滑，别无其他作用。单元提供的角点在任何线段数下都相同，而这正是任何贴着单元布置的东西所需要的。当三个单元在界限内相交时，它们共享的角点是一个角点，而不是中间夹着一段空跨段的两个角点。

  在这些角点处，单元几何最多会移动一条弦的矢高：在随附的延伸半径和默认线段数下约为八个单位，线段数减半时则为其四倍。以比这更严格的精度断言单元顶点的使用方需要重新建立基线。
- **Starsector 约定以启动器的方式读取 Mod 的元数据**：通过 `json.jar` 中游戏自身的解析器，而此前使用的是 `JsonSlurper`，因此构建与启动器在文件能否解析上不会出现分歧。所以配置构建需要已安装的游戏；在没有安装游戏的机器上，会在配置构建时（而非编译时）告知在何处查找过 `json.jar`。

### 公共契约变更（**破坏性**）

- 每个深入游戏自身界面的读取都接受一个 `GameReachReporter`，即该读取失败时会有所损失的 Mod 的报告器：`CampaignMapView` 的三个星图读取及 `resolveSectorMapState()`、`MapFilterRows.resolveShownMapFilterRow()`、`MapFilterToggle.appendToRow()`、`VanillaButtonLabel.resolveLabelOf()`、`CoreUiOverlayPanels.attachOverlayPanel()`、`CoreUiDialogView.isModalDialogShowing()` 和 `resolveModalPresence()`、`CodexView.isCodexShowing()` 以及 `MapIconLayeringProbe.readLayeringOf()` 将其作为最后一个参数接受，而 `MapPresence`、`VanillaIntelScreenView` 和 `VanillaMapTooltipProbe` 在构造时接受它，其无参构造函数已移除。没有玩家可见损失的调用方传入 `GameReachReporter.UNREPORTED`。
- `CompatibilityFailures.takeNextUnreported()` 返回 `ReportedFailure`，`CompatibilityNoticePanel.showFailure()` 也接受它，二者原先都使用 `CompatibilityFailure`。记录现在除了失效的绑定，也会保存 Mod 自身失败的功能。只显示或记录所取内容的调用方照常编译；读取 `subject()` 或 `breakage()` 的调用方需先确认它是 `CompatibilityFailure`。
- `ReflectiveCoreUiComponentRepainter` 不再是枚举，其 `INSTANCE` 已移除：调用方用自己的报告器构造一个实例，失败的重绘将作为该调用方的损失归档。
- `MapProbeWarnings` 更名为 `RearmableWarnings`，位于 `kmlib.logging` 中，与 `SessionWarning` 并列；`createSharedWarning()` 改为公开的 `createRearmableWarning()`。它汇集的警告来自每个按类保留一个警告的所有者，而不仅是星图探测器。重新启用这些警告的调用方需更改其导入；`rearmAllWarnings()` 不变。
- `KmLogging` 已移除。Mod 通过 `LunaLogLevelBinding.bindLogLevel(modId, loggerRoot, fieldId)` 绑定其日志级别，它与 KMLib 其余的 LunaLib 读取一起位于 `kmlib.settings` 中，程序库的默认级别为 `LunaLogLevelBinding.DEFAULT_LEVEL`。接受后备级别的重载未被保留；未设置或无法识别的字段采用默认级别。
- `GlStateGuard` 已移除。它只是转发到 `GlPasses.runWithSavedState`，调用方现在直接调用后者；保存的状态以及抛出异常时的恢复保持不变。
- `ModelviewMatrixReaders.selectForActiveRenderer()` 接受使用该读数的 `CompatibilityConsumer`；无参形式已移除。调用方现在要指明其记录锁存所用的键，以及说明其损失的句子，两者都只在绑定失败时读取。该句子由调用方提供，因为绑定失败的代价需要对基于该读数构建的功能有所了解：程序库知道渲染器、两个版本以及发生变动的成员，却对用它绘制了什么一无所知。
- `FastRenderingModelviewMatrixReader` 不再是枚举，其 `INSTANCE` 已移除：`ModelviewMatrixReaders` 现在在接受绑定的地方，为提出请求的使用方构建一个实例。读取器在调用时记录它遇到的失败，而无法说明自己服务于哪个 Mod 的读取器只能把失败记在无人名下，而绑定损坏的代价应由使用该读数的 Mod 来说明。同一时间屏幕上只有一个星图，这是关于星图的事实，与有多少个 Mod 在其上绘制无关。无论哪种方式，包外都无法构造它，因为该类只能通过选择方法访问。
- `VerticalTableSpec` 失去其 `scrolls` 组件和 `asScrolling()` 细化方法；可滚动的部分由宿主用来容纳一段行的 `ScrollingSection` 声明。原先标记了其列表的宿主现在改为包装它：`new ScrollingSectionSpec(List.of(list))`。该表格的规范构造函数原先接受八个参数，现在接受七个。
- `ControlSpec` 的各变体现在是新包 `kmlib.starsector.ui.controls.specs` 中的顶层类型，每个变体一个文件，均以 `Spec` 为后缀：`ControlSpec.Checkbox` 现在是 `CheckboxSpec`，`ControlSpec.Interactive` 是 `InteractiveSpec`，十二个变体均依此类推。它们嵌套在一个 878 行的文件中时，无法分别打开、审查或追溯修改记录，而且不带外围类型就读不出变体的名称。整个类型族一起迁移，因为密封类型与其允许的变体必须位于同一个包中；`ControlAction`、`ControlHoverReport`、`RadioAlignment`、`ReselectBehaviour`、`RowGeometry` 和 `SegmentSizing` 也随之迁移，使新包不依赖旧包中的任何内容。使用方需更改导入并去掉 `ControlSpec.` 前缀；规格的其他方面均未改变。
- `RowStack.layoutRows()` 接受一个 `RowDimensions`，取代原先作为两个相邻列表传入的逐行高度和宽度，统一高度的重载已移除。这两个重载各接受五个参数，且都以三个浮点数开头，其中第三个在一个重载中是行高，在另一个中是间距，因此无论本意是哪个，调用点读起来都一样。各行高度相同的一组行现在写作 `RowDimensions.createUniform(rowHeight, rowWidths)`。
- `ControlStripLayout.layoutControls()` 接受测量各行时所得的 `StripMeasurement`，取代其 `rowHeights` 和 `rowWidths` 列表。它们是对同一控件条的同一次读数，分开后可能来自不同的读数：两个长度不同的列表，或某个控件条的高度而宽度却从未从该控件条测得，而放置逻辑对这两种情况都无从察觉。
- `ExtensionPoint.settleWorkOutcome()` 已移除：扩展点的工作通过 `offerWork(work)` 提交，由它自行调用已安装的实现。先调用实现、事后再结算结果的端口会让该调用处于任何边界之外，而同时提供两种方式的扩展点会让这条路一直敞开。`registerImplementation()` 接受第四个参数：当注册方的实现失败并被移除时告知注册方的内容。
- `ColonisationRoutines.registerRoutine()`、`OwnershipTransferRoutines.registerRoutine()`、`OwnerSubmarketRules.registerRule()` 和 `ModdedSystemAccessRoutes.registerRoute()` 接受一个 `Supplier<ModIntegration>`：实现来自哪个 Mod，以及注册的 Mod 在没有它时会失去什么，仅在实现失败时才组合。
- `mod-release.yml` 按语言区域命名已本地化 Mod 的发布资产：为其 `localisation/manifest.json` 中的每个语言区域生成 `<folder>-<version>-<locale>.zip` 和 `<mod-id>-<locale>.version`，不再有无后缀的压缩包。默认语言区域的版本文件会以 `<mod-id>.version` 再附加一次，变更之前的安装仍会轮询该文件，因此不会有更新检查失效。已提交清单的调用方，会在将其固定版本移到此版本的那次发布中改变其发布内容。
- `StubbedGlobalLogger.answerLoggersOn()` 已移除：应答记录器的 `Global` 替身通过 `openGlobalAnsweringLoggers()` 打开，测试套件在其上为星域或设置打桩；持有多个接缝的布置将其交给 `StaticSeams.holdSeam()`。
- `StarsectorFont.getNativeSize()` 已移除，该枚举不再声明任何尺寸：图集按 1:1 绘制时的尺寸通过 `FaceLineHeightReader` 从已加载的字体读取。为原版图集写死的数值，会使本地化安装在同一基础名下的图集被缩放绘制，在像素字体上就会模糊。调用方改写为 `InstalledFaces.createNativeFace(font)`。
- `TextStyle.createStyle()` 接受 `TextFace`，取代 `StarsectorFont`，因为样式不再知道用来构建字体的原生尺寸。原先只指明字体的调用方传入 `InstalledFaces.createNativeFace(...)`。
- `TextFace` 以 `atlas()` 持有一个 `FontAtlas`，取代以 `font()` 持有的 `StarsectorFont`，因此无论枚举是否列出，文本都能以游戏设置所声明的字体绘制。读取 `face.font()` 的调用方改为读取 `face.atlas()`，它应答的路径和平滑方式与之前相同；`StarsectorFont` 是一种 `FontAtlas`，因此由它构建的每个字体均可原样编译。`LazyFontCache.loadByFace()`、`FaceLineHeightReader`、`GlyphCoverageReader`、`WidgetStyle.bodyFont` 以及 `StripTextMeasurers.loadFaceMeasurers()` 接受的正文字体同样都改为 `FontAtlas`。
- 字体包一分为三。`kmlib.starsector.ui.font` 保留字体、向字体发出询问的端口以及后备规则，这些都不加载图集，因此 `StarsectorFont`、`AtlasSmoothing` 和 `TextFace` 留在原处。`LazyFontCache` 和 `DrawableStringCache` 移至 `kmlib.starsector.ui.font.installed`，与运行中游戏图集的其他读取器放在一起。`LineWidthMeasurer`、`TextSpanMeasurer`、`LazyFontMeasurer`、`LazyFontSpanMeasurer` 和 `StripTextMeasurers` 移至 `kmlib.starsector.ui.font.measure`，夹具 `LineWidthMeasurerFake` 移至 `kmlib.testfixtures.starsector.ui.font.measure`。调用方需更改导入；这些类的其他方面均未改变。
- `SortDirection.fromKeyOrDefault()` 和 `ListColumns.fromKeyOrDefault()` 已移除：存储的键通过 `PersistedChoices.fromKey(options, key, fallback)` 解析，这是所有带键选项集共用的唯一查找。读取存储的列数的调用方写作 `PersistedChoices.fromKey(ListColumns.values(), key, ListColumns.DEFAULT)`。

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
