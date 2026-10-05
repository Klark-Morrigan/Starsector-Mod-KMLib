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

- **写在按钮文字之后的快捷键会留在按钮上。** 在文字中不含该按键字母的译文中，或在英文中改绑按键之后，形如 `[M]` 的按键会换行到按钮之外的第二行。
- **开启战役加速时，KM 系列 Mod 绘制的星图图层会留在星云之上。** 每次打开星图，所有图层都停留在星云之下，重新读取存档或切换星景都无法恢复。由 **MiniRockytheOracle** 在 [**USC**](https://discord.com/channels/187635036525166592/1549091275167240272/1551829173037957170) 报告。
  - **已放弃的图层修正会在下次打开星图时重新尝试**，而不是在本次会话的剩余时间内一直停用。

### 新增

- **简体中文。** 第二个压缩包 `KMLib-<version>-zh-hans.zip` 中的兼容性通知、设置标签页以及 Mod 列表条目均为简体中文。请先将[中文本地化](https://github.com/TruthOriginem/Starsector-Localization-CN)覆盖到 `starsector-core` 上：游戏自身的字体不含中文字符，没有它，所有中文字符都会绘制为 `?`。日志详细程度设置的选项保留英文，因此该设置在两个压缩包之间可以沿用。
- **当 KM 系列 Mod 无法与另一个 Mod、Fast Rendering 或游戏本身集成时，游戏内会弹出通知。** 通知会写明是哪个 Mod、两个版本、什么停止工作、什么不受影响，以及应当更新、降级还是等待。这类失败只会损失基于该集成构建的功能，并且每个失败在本次游戏期间只显示一次。
  - **KM 系列 Mod 自身的某项功能出错并被关闭时，也会以同样方式报告**，并请玩家向该 Mod 的开发者报告此问题。
  - **星图打开时发现的失败可以直接显示在星图之上**，无需等到回到战役界面。

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
- **`LabelHighlightRule`**：按游戏的规则判断标签的高亮调用会让哪些片段保持原样。只有当片段两侧的字符是空白或 ASCII 标点，或片段位于文本开头或结尾时，片段才会高亮；每个片段都从上一个片段的匹配位置之后开始查找。因此紧挨中文文字或中文句号等全角标点的片段会以原样绘制，且不报错。接受一段文本及其片段，或接受一个 `HighlightedParagraph`，按它交出片段的方式判断。从渲染器复制而来，因为渲染器的类在运行中的游戏之外无法加载。`UnhighlightedRun.describeIn()` 将以原样绘制的片段表述为一条发现项。
- **`HighlightedTooltipMock`**：替身提示框，为添加到其中的每个段落分配各自的标签，使对象绘制到其中的每个高亮之后都能按该规则检查。
- **`ButtonLabelFake`**：以游戏交出按钮文字时所用的标签形式呈现按钮文字，包括文字内容、其中被高亮的片段及高亮颜色。它按每个字符 `CHARACTER_WIDTH` 的宽度测量文字，并记录最近一次被调整到的宽度，使测试套件能够确认加长后的文字会重新调整宽度而不是换行。
- **`LunaSettingsHighlights`** 和 **`LunaSettingsTable.readHighlightedTextsByFieldId()`**：按 LunaLib 绘制方式读取的设置单元格（去掉方括号，并列出其高亮的片段），以及 LunaLib 以这种方式绘制的单元格：数值行的描述与文字行的内容。

#### 本地化

Mod 面向玩家的文件可以按语言分别保存，每个语言区域一个语言包，位于 `localisation/<locale>/` 下，另有一份清单列出各语言区域、默认语言区域以及每个语言包文件在 Mod 中的落点。每个已翻译语言区域的目录中还有 Mod 的 `CHANGELOG.md` 的译文。这些夹具读取该布局并比较其中的语言区域。每个文件都按 Mod 随附副本的读取方式读取（JSON 通过 `ShippedJson`，字符串通过 `ShippedStrings`，设置表通过 `LunaSettingsTable`），因此语言包从不会以第二种方式解析。

- **`LocalisationDirectory`**：Mod 的 `localisation/` 目录。读取其根部的清单，列出清单旁的每个语言包目录（无论是否已声明），仅为清单声明的语言区域打开语言包，并在 Mod 提交了 `mod_info.base.json` 时读取清单旁的该文件。
- **`LocaleManifest`**：存在哪些语言区域、哪个是默认语言区域、语言包包含哪些文件以及每个文件的落点；它是这三者的唯一事实来源。它可能携带的每个键都是已知的，其他任何键都会被拒绝，因此拼错的 `coreLocalization` 会失败而不是被当作缺失。无论清单如何构建，其数据路径都限定在 Mod 根目录之内，因为向某个路径复制就是对仓库的一次写入。
- **`DeclaredLocale`**：一个已声明的语言区域：小写的 BCP 47 标签、以该语言自身书写的名称，以及（当原版图集缺少其字形时）其玩家覆盖安装到 `starsector-core` 上的 https 项目。
- **`LocaleBundle`**：一个语言区域的目录。读取其字符串文件、设置表和启动器片段，并且只按裸文件名解析其他语言包文件，因此任何语言包都无法触及另一个语言包。
- **`ModInfoFragment`**：语言区域所翻译的启动器文本：`name`、`description`、`author` 和依赖项名称，每项均可选，缺省时退回基础文件。任何功能性字段都会被拒绝，因为片段若改变版本或 jar 列表，就会为每个语言区域构建出不同的 Mod。`listFallbackFieldNames()` 列出片段留给基础文件的每个字段。
- **`ModInfoBase`**：片段能触及的 `mod_info.base.json` 内容：它退回使用的文本字段，以及它可以指明的依赖项 ID。
- **`LocaleParity`**：使每个语言区域与默认语言区域保持一致，每项检查给出的发现项都以应做的修改来表述。在缺口没有后备的地方严格要求：缺失的目录、文件、字符串或行，在任一语言区域中留空的字符串，取用不同参数的槽位，改变其存储内容的行，被拆分或合并的标签页，在未指明 `coreLocalisation` 时出现的 Latin-1 以外的文本，以及指明未声明依赖项的片段；并报告语言区域留给基础文件的启动器字段。Radio 的选项须完全一致，因为 LunaLib 存储的是所选的标签。已翻译的更新日志必须与根目录的更新日志逐点一致：版本标题及其顺序相同，各节相同，每一嵌套深度的列表项数量相同，每节的代码片段也相同。其行文不参与比较。每个语言区域（包括默认语言区域）的设置表中，每个方括号片段都必须是游戏在其所在位置能够高亮的。每个缺陷只报告一次，清单未映射的文件不参与比较。`findAllMismatches()` 汇集所有检查，使 Mod 的测试套件只需断言一次；不随附设置表的 Mod 打开它时无需字段 ID 前缀。
- **`LunaSettingsTable.readBehavioursByFieldId()`、`readDisplayedTexts()` 和 `readTabsByFieldId()`**：将一行拆分为它做什么与它说什么：其 `FieldBehaviour`（类型、存储的默认值、选项和取值范围，由 `describeDifferencesFrom()` 逐列比较）、界面绘制的单元格，以及其标签页。声明了两次的字段 ID 会使按键读取失败，而不是被从中丢弃。
- **`StringTemplates.readArgumentConversions()`**：模板的每个槽位取用哪个参数、以何种转换取用，无论槽位以何种顺序书写；这正是同一字符串的两种措辞为了能由同一个调用点填充而必须一致的内容。
- **`ShippedJson.requireList()` 和 `locateElement()`**：对象检查与成员定位在数组上的对应方法。
- **`ChangelogOutline`**：Keep a Changelog 文件的结构：按标题列出的各版本、每个版本的各节、每节在每一嵌套深度上的列表项数量，以及每节的代码片段。不读取行文，因此只要没有条目只在一方被增删或改用其他标识符，译文与原文的结构就相同。
- **`LocaleBundle.readStringSource()`**：以设置替身作答的形式提供某个语言区域的字符串，使在其下组成的文本以该语言区域呈现。
- **`ShippedLocales`**：Mod 随附的语言区域，从其自身的 `localisation/` 目录读取，供按语言区域逐一组成文本的用例使用：`listLocaleTags()` 作为参数化来源，`installLocaleStrings()` 将某个语言区域的字符串设为游戏的字符串。

### 变更

- **在 Fast Rendering 下，星图只在 `v0.9.1rc1` 及更高版本中跟随光标。** 光标读数与当前帧一致，而不会在星图平移时落后一到两帧。在更早的版本上，星图不会响应光标，并会有通知说明应更新到哪个版本。
- **日志详细程度设置的描述以单独一行的默认值结尾**，因此默认值在中文中也能高亮。
- **Mod 列表中该 Mod 的名称为 Klark Morrigan 的程序库 (KMLib)。** 更新检查器显示同一名称。
- **每种语言作为独立的压缩包发布**：`KMLib-<version>-en.zip` 和 `KMLib-<version>-zh-hans.zip` 并列在发布页面上。发布说明会写明哪个压缩包对应哪种语言，更新检查器在此变更前后均可正常工作。

### 公共契约变更（**破坏性**）

- 每个深入游戏自身界面的读取都接受一个 `GameReachReporter`，即该读取失败时会有所损失的 Mod 的报告器：`CampaignMapView` 的三个星图读取及 `resolveSectorMapState()`、`MapFilterRows.resolveShownMapFilterRow()`、`MapFilterToggle.appendToRow()`、`VanillaButtonLabel.resolveLabelOf()`、`CoreUiOverlayPanels.attachOverlayPanel()`、`CoreUiDialogView.isModalDialogShowing()` 和 `resolveModalPresence()`、`CodexView.isCodexShowing()` 以及 `MapIconLayeringProbe.readLayeringOf()` 将其作为最后一个参数接受，而 `MapPresence`、`VanillaIntelScreenView` 和 `VanillaMapTooltipProbe` 在构造时接受它，其无参构造函数已移除。没有玩家可见损失的调用方传入 `GameReachReporter.UNREPORTED`。
- `CompatibilityFailures.takeNextUnreported()` 返回 `ReportedFailure`，`CompatibilityNoticePanel.showFailure()` 也接受它，二者原先都使用 `CompatibilityFailure`。记录现在除了失效的绑定，也会保存 Mod 自身失败的功能。只显示或记录所取内容的调用方照常编译；读取 `subject()` 或 `breakage()` 的调用方需先确认它是 `CompatibilityFailure`。
- `ReflectiveCoreUiComponentRepainter` 不再是枚举，其 `INSTANCE` 已移除：调用方用自己的报告器构造一个实例，失败的重绘将作为该调用方的损失归档。
- `MapProbeWarnings` 更名为 `RearmableWarnings`，位于 `kmlib.logging` 中，与 `SessionWarning` 并列；`createSharedWarning()` 改为公开的 `createRearmableWarning()`。它汇集的警告来自每个按类保留一个警告的所有者，而不仅是星图探测器。重新启用这些警告的调用方需更改其导入；`rearmAllWarnings()` 不变。
- `KmLogging` 已移除。Mod 通过 `LunaLogLevelBinding.bindLogLevel(modId, loggerRoot, fieldId)` 绑定其日志级别，它与 KMLib 其余的 LunaLib 读取一起位于 `kmlib.settings` 中，程序库的默认级别为 `LunaLogLevelBinding.DEFAULT_LEVEL`。接受后备级别的重载未被保留；未设置或无法识别的字段采用默认级别。
- `GlStateGuard` 已移除。它只是转发到 `GlPasses.runWithSavedState`，调用方现在直接调用后者；保存的状态以及抛出异常时的恢复保持不变。
- `ModelviewMatrixReaders.selectForActiveRenderer()` 接受使用该读数的 `CompatibilityConsumer`；无参形式已移除。调用方现在要指明其记录锁存所用的键，以及说明其损失的句子，两者都只在绑定失败时读取。该句子由调用方提供，因为绑定失败的代价需要对基于该读数构建的功能有所了解：程序库知道渲染器、两个版本以及发生变动的成员，却对用它绘制了什么一无所知。
- `FastRenderingModelviewMatrixReader` 不再是枚举，其 `INSTANCE` 已移除：`ModelviewMatrixReaders` 现在为提出请求的使用方构建一个实例。读取器记录它遇到的拒绝，而无法说明自己服务于哪个 Mod 的读取器只能把拒绝记在无人名下，而读取被拒绝的代价应由使用该读数的 Mod 来说明。同一时间屏幕上只有一个星图，这是关于星图的事实，与有多少个 Mod 在其上绘制无关。无论哪种方式，包外都无法构造它，因为该类只能通过选择方法访问。
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

### 面向开发者

<details>
<summary>API、构建工具与测试夹具</summary>

#### 修复

- **按钮上的快捷键：** `VanillaButtonLabel.announceShortcut()` 会把标签加宽到其所承载的文字。游戏只按创建时的文字设定一次标签宽度，更长的内容会换行。
- **加速下的星图图层：** `MapIconReseater` 只在控件已丢弃图标后才将其放回，最多等待 `MAX_ADVANCES_DETACHED` 次推进，并在星图关闭时结束。加速会让脚本每帧推进多次，因此移除与放回落在同一帧内，控件从未重新叠放该图标。
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

#### 变更

- **Fast Rendering 下的星图光标：** 模型视图矩阵通过 `glGetFloat(GL_MODELVIEW_MATRIX)` 读取，`v0.9.1rc1` 会就地应答这一调用。KMLib 不针对 `fr.jar` 的任何部分编译，因此 Fast Rendering 的内部重构不会破坏这次读取。
- **Mod 列表名称：** 依赖 KMLib 的 Mod 可以在其 `dependencies` 条目中使用同一名称。启动器只在 KMLib 缺失时才显示它。
- **`MapIconReseater` 会在日志中说明它观察到的情况**，记录在 KMLib 自己的记录器上。包的 README 列出了这些日志行。
  - 星图的打开与关闭以 DEBUG 级别跟踪，每次都附带自上次观察到图标未被遮挡以来的抬升次数。
  - 两种状态每次会话警告一次：星图显示但没有可放置的图标；以及搁置，附带导致搁置的读数。
- **`VoronoiCellBuilder` 把相邻单元共享的角点放在两者中的同一位置**，该位置由两个站点和延伸半径计算得出。串接的边境边之间没有缺口，线段数只决定圆弧绘制得有多平滑。
  - 在这些角点处，单元顶点最多会移动一条弦的矢高：在随附的延伸半径和默认线段数下约为八个单位。以比这更严格的精度断言的使用方需要重新建立基线。
- **Starsector 约定通过游戏的 `json.jar` 读取 Mod 的元数据**，与启动器的方式相同，因此两者在文件能否解析上不会出现分歧。所以配置构建需要已安装的游戏。

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
