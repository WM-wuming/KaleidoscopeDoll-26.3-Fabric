# 森罗物语：玩偶 · Minecraft 26.3 Fabric 移植版

这是 Kaleidoscope Doll（森罗物语：玩偶）面向 Minecraft Java Edition 26.3 的**非官方 Fabric 移植版**。[原版代码](https://github.com/KaleidoscopeMods/KaleidoscopeDoll/commit/62303aa58bbd5ee52498f3ad0fbf4a0c7d04c183)由 KaleidoscopeMods 维护，原作者为 ysbb 和 tartaric_acid；本仓库不代表上游官方发布。

## 移植出处

移植提交直接接在 [KaleidoscopeMods/KaleidoscopeDoll `1.20-forge` 的 `62303aa5` 提交](https://github.com/KaleidoscopeMods/KaleidoscopeDoll/commit/62303aa58bbd5ee52498f3ad0fbf4a0c7d04c183)之后，保留该提交及其上游 Git 历史。赞助者玩偶署名也来自该版本。

## 本移植版新增与适配

- **26.3 Fabric 适配**：更新玩偶方块、实体、物品和抽奖机的渲染与动画，迁移至 26.3 的物品模型和网络接口。
- **服务端自定义玩偶同步**：将新版 Java 自定义玩偶 ZIP 放入服务端 `config/kaleidoscope_doll/custom/`，重启或执行 `/kaleidoscope_doll reload`。开启服务器资源包接收的客户端进服后可直接同步模型、贴图和介绍，不必手动安装 ZIP 或配置下载地址。缓存按服务器隔离，离服后停用。每次最多同步 8 个 ZIP、合计 16 MiB。
- **赞助者玩偶配置包与署名**：可用导出脚本整理已有赞助者资源、恢复悬停署名并导出新旧 ID 对照表。安装方式与迁移注意事项见[赞助者配置文档](docs/sponsored-doll-config.md)；赞助者配置 ZIP 不随仓库附送。
- **模组电脑与功能玩偶**：电脑界面支持搜索和批量换取玩偶；功能玩偶可与末影珍珠、风弹或不死图腾合成。

自定义玩偶、模组电脑等玩法不全是本仓库原创；上面列出的是本移植的新增或适配内容。

## 环境与构建

### 前置依赖与下载

请先安装 Minecraft Java Edition **26.3**、Java **25** 或更新版本，以及 Fabric：

- [Fabric Loader 安装器](https://fabricmc.net/use/installer/)
- [Fabric API（Modrinth）](https://modrinth.com/mod/fabric-api)
- [GeckoLib（Modrinth）](https://modrinth.com/mod/geckolib)，需要 **5.5.7** 或更新版本

本模组还必须安装 [Forge Config API Port（Modrinth）](https://modrinth.com/mod/forge-config-api-port)，它不会内嵌在本项目发布的 JAR 中。配方查看器为可选项：[Just Enough Items / JEI（Modrinth）](https://modrinth.com/mod/jei)。

下载依赖时，请在对应页面选择 **Minecraft 26.3** 和 **Fabric** 的版本；客户端和服务端都应安装本模组及其必需依赖。

使用 JDK 25 和项目自带的 Gradle Wrapper：

```sh
./gradlew build
```

Windows 使用 `.\gradlew.bat build`，产物位于 `build/libs/`。多人游戏请在服务端和客户端安装相匹配的模组版本。

推送到 `26.3-fabric` 或向该分支提交拉取请求时，GitHub Actions 会自动构建，也可在 **Actions → Build** 手动运行。构建成功后可从该次运行的 **Artifacts** 下载模组 JAR。

默认分支构建成功后，工作流会按 `gradle.properties` 的 `mod_version` 自动创建 `v<版本号>` 标签和 GitHub Release，并附上模组 JAR。相同版本不会重复发布或替换；发布下一版时请先更新 `mod_version`。PR 构建不会发布。

部分玩偶 ID 与旧版不同，升级前请备份存档。普通玩偶的部分映射见 [26.3 ID 对照表](docs/doll-id-mapping-26.3.json)，赞助者玩偶另见配置文档；旧 ID 不保证自动迁移。

## 许可证与致谢

代码沿用原项目的 [BSD 3-Clause 许可证](LICENSE)。模型、贴图及其他美术资源不适用代码许可证，仍为 **All Rights Reserved**，相关权利归原作者及权利人。仓库上传许可不构成对第三方的再分发授权。
