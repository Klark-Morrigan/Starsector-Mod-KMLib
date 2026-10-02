# A KM mod's thread on Fossic

[Fossic](https://www.fossic.org/) is the Chinese Starsector forum,
and a mod's thread there is the Chinese player's front door:
the download is a forum attachment by the board's rules,
and GitHub is not a link a player in mainland China can rely on.
This records what the board asks of any KM mod's thread,
read off the board on 2026-10-01.
Each mod's own values, links and checklist live beside its post source under `promo/`.

## Index

- [The board](#the-board)
- [Review and the mod index](#review-and-the-mod-index)
- [The rules a thread keeps](#the-rules-a-thread-keeps)
- [The posting form](#the-posting-form)
- [Images and attachments](#images-and-attachments)
- [Markup](#markup)
- [Links](#links)

## The board

An original mod starts in Mod制作发布 (`forum-71-1.html`),
whose rule box is headed 原创Mod预发布规则摘要:
it is the pre-release board,
and every mod stays there until an admin's review moves it.
Its one thread type is 【原创Mod发布】.
There is no pre-release prefix;
threads mark testing in the title, as `[测试]` or `[早期测试]`,
after the game version in brackets,
which is how the board's titles open.

The board's rules are thread 13 (Mod区管理规定与细则),
the review rules thread 6107 (Mod审核规定),
and the category guide thread 5932.

## Review and the mod index

A thread reaches 原创Mod区 (`forum-46-1.html`) and the Mod索引 only once an admin has reviewed it.
A lightweight mod, 美化, 功能 or 杂项,
may take 快速审核 straight after posting,
by contacting 欧瑞捷门, Scythe, 肥龙太刀足 or jn_xyp.
Anything larger needs 15 days posted, 200 downloads and 10 replies from different users first.
The newest version in the thread is the one reviewed.
It fails on instability (crashes, broken saves),
plagiarism or unlicensed use of others' code and assets,
or too little content;
balance is not judged.
A fail waits 15 days before a retry.

## The rules a thread keeps

- The author posts in person; one thread per mod, and every later release updates it.
- The download is a forum attachment and free:
  no price on the thread and no reply-to-download.
  A download permission level is allowed.
- The form must match the post, and a thread supporting several game versions carries a download for each.
- A known incompatible mod is named in the form's 冲突Mod and in the post body both.
- A mod that, without telling the player, modifies the game itself (its config aside),
  goes online, or reads or writes outside the game folder is banned;
  one that does any of these with notice says so in the post.
- Feeding another author's work to a large language model counts as using that work,
  and must follow its licence or have the author's permission.
- Not recommended, not banned:
  a mod with no Chinese, one that conflicts with mainstream mods,
  or one whose style or mechanics cut against the base game.

## The posting form

The form carries the mod's metadata beside the body;
the board list shows a line built from it under each title.
Every input, in the form's order (`*` is required):

| Input | What it takes |
| --- | --- |
| 标题 `*` | 80 characters at most; the game version in brackets first, then the status tag, then the name. The mod version can stay out: the board list and the download panel show the form's Mod版本, and a title without it needs no edit per release |
| Mod标识符 `*` | the mod ID from `mod_info.json` |
| Mod中文名 `*`, Mod英文名 | the mod's names |
| Mod作者 `*` | the forum user name first; several authors comma-separated |
| Mod类别 `*` | 功能 is a mod with `utility: true` that can be removed mid-save; 杂项 a function mod that cannot; the rest are 前置, 内容, 独立, 美化, 战役, 大型, 势力, Mod扩展 |
| Mod适用版本 `*` | the game version without its patch suffix, as 0.98 |
| Mod更新时间 `*` | the current version's date |
| Mod版本 `*` | the mod's version, which must match `mod_info.json` |
| 可安全移除 `*` | whether the mod can be removed without restarting the save |
| 依赖Mod `*` | tick boxes for the common libraries; 其它 with the rest named in 其它依赖Mod, one per line |
| 冲突Mod | known conflicts, one per line |
| Mod简短介绍 `*` | a short description |
| Mod下载地址, 网盘提取码/解压密码 | empty when the download is the attachment, which the form itself recommends |
| Mod语言 `*` | 中文, 英文, 其它 or 无文本 |
| Mod发布文件 | one row per release zip, added after the upload: file, game version, mod version and display name. The 插入Mod文件下载面板到帖子正文中 button then puts the download panel into the body. Only the mod's own zips go here, since each row's version must match `mod_info.json` |
| Mod允许直链下载 `*` | 是 lets a mod manager fetch the zip without opening the thread |
| Mod索引隐藏, Mod索引备注 | whether the index lists the mod, and a short note shown after its version there |
| 滑块验证 `*` | the slider captcha |
| 附加选项 | 禁用编辑器代码 off, or the BBCode prints as text; 禁用表情 on, so no command syntax reads as a smiley; 禁用链接识别 off; 接收回复通知 as wanted; HTML is not offered and `[img]` is fixed on |
| 发贴本地化图片 | copies remote images onto the forum's storage; harmless, and not relied on, since it fetches from Fossic's side where GitHub is the unreliable host |
| 回帖奖励 | left empty. Mod threads on the board run without one, and replies come from bug reports and questions anyway. A reward draws bump replies (顶) that bury those, and its whole pool comes out of the author's 积分 up front |
| 主题标签 | up to five tags. They are free text posters make up, and sparsely used: mod发布 is the one mod threads share. Players browse by category, game version and the mod index, so a tag earns its place only as a word a player would type into search |
| 定时发布 | scheduled posting |

## Images and attachments

Every image is a forum attachment served from `cdn.fossic.cn`,
never a GitHub link:
`raw.githubusercontent.com` is blocked in mainland China,
and the forum rules advise against sources players there cannot reach.
An uploaded image stays across edits,
so a release re-uploads only an image that changed.
Fossic publishes no size limit,
but a per-file and a daily one exist per user group;
the FAQ (thread 3316) says to ask an admin to raise them.

An attachment the post text does not reference is listed under the post.
A zip referenced by its tag is drawn as a download link where the text names it,
which is how a dependency's zip with no release-file row of its own,
such as KMLib's in a consumer's thread, gets a link of its own.
The editor inserts a tag with its numeric ID when an uploaded file is clicked;
the attachment URL a browser shows is encoded and is not linked by hand.

## Markup

Fossic is Discuz! X3.5, and its BBCode differs from the Fractal Softworks forum's SMF:
`[collapse=title]` for `[spoiler=]`,
`[size=1..7]` for point sizes,
`[*]` list items with no `[li]`,
`[attachimg]id[/attachimg]` for an uploaded image,
`[attach]id[/attach]` for an uploaded file, drawn as an inline download link with its name, size and download count,
and `[hr]` checked in the preview before it is relied on.
Markdown is also accepted as `[md]...[/md]`,
but a post translated from the Fractal Softworks one stays BBCode.

## Links

Fossic's canonical link form is `https://www.fossic.org/thread-<tid>-1-1.html`,
with boards as `forum-<fid>-1.html`.
Dependencies and compatible mods link their Fossic threads,
in the 搬运 (repost) or 汉化 (translation) boards,
as the Fractal Softworks post links their threads there.
Guest search is blocked;
the public Mod索引 at `topic-mod_index.html` is the way to find a thread.
