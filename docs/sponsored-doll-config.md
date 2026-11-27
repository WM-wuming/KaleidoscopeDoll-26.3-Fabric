# 赞助者自定义玩偶

1. 服务端和客户端都将支持同步的 26.3 模组 JAR 放进 `mods`，替换旧版模组。
2. 将 `KaleidoscopeDoll-Sponsored-Config-26.3.zip` 原样放进服务端的 `config/kaleidoscope_doll/custom/`，不要解压。重启服务器，或由管理员执行 `/kaleidoscope_doll reload`。
3. 客户端不用安装 ZIP。进服后模组通过游戏连接同步模型、贴图和介绍；首次同步并加载完后，在创造栏「自定义玩偶」或模组电脑里查看。

客户端缓存位于 `config/kaleidoscope_doll/servers/<服务器地址的 SHA-256>/`。同一服务器的文件未变时会直接复用缓存，离服后不再加载该服缓存。服务器移除 ZIP 并重载后，客户端会停用这些模型。同步不写入 `resourcepacks/`，不出现在资源包列表，也不需要设置服务器资源包下载地址。若在多人游戏服务器设置中禁用了服务器资源包，模组也不会接收该服务器的自定义玩偶文件。

单人游戏仍将 ZIP 放进自己的 `config/kaleidoscope_doll/custom/`，然后重启游戏。联网同步仅支持带 `custom_dolls.json` 的新版 Java 自定义玩偶配置 ZIP，不同步旧版基岩格式，每次上限 8 个 ZIP、合计 16 MiB。

赞助者署名按原项目 `1.20-forge` 的提交 `62303aa5` 从旧玩偶编号匹配，在鼠标悬停介绍里显示。中文和英文各有 634 条，日文保留原有的 555 条，其余在游戏中回退到英文。配置 ZIP 不随源码仓库附送。若已取得配置 ZIP，可以用 PowerShell 7 更新其中的署名：

```powershell
./scripts/export-sponsored-config.ps1 -Output '<现有配置 ZIP 的路径>' -RestoreCredits
```

参考提交已包含在完整 Git 历史中。若使用浅克隆且缺少该提交，先执行 `git fetch --unshallow origin`。

赞助者玩偶按原配置顺序连续编号为 `java:kaleidoscope_doll_sponsors:doll_1` 到 `java:kaleidoscope_doll_sponsors:doll_634`。升级时不要同时启用新旧 ZIP。旧存档里的赞助者玩偶不会自动迁移，旧 ID 可能显示成另一个玩偶，或不再找到模型。请先备份存档；可用导出脚本的 `-CreditsCsv` 参数生成新旧 ID 对照表。

不需要在资源包页面手动启用或在世界里安装数据包。若已安装旧版赞助者资源包或数据包，先停用并移除，避免旧贴图覆盖原版玩偶。
