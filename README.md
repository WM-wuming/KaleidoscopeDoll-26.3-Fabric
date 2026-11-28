# 森罗物语：玩偶 · Minecraft Fabric 移植版

这是 Kaleidoscope Doll（森罗物语：玩偶）面向 Minecraft Java Edition 的**非官方 Fabric 移植版**，默认目标为 26.3，其他支持版本见下表。[原版代码](https://github.com/KaleidoscopeMods/KaleidoscopeDoll/commit/62303aa58bbd5ee52498f3ad0fbf4a0c7d04c183)由 KaleidoscopeMods 维护，原作者为 ysbb 和 tartaric_acid；本仓库不代表上游官方发布。

## 移植出处

移植提交直接接在 [KaleidoscopeMods/KaleidoscopeDoll `1.20-forge` 的 `62303aa5` 提交](https://github.com/KaleidoscopeMods/KaleidoscopeDoll/commit/62303aa58bbd5ee52498f3ad0fbf4a0c7d04c183)之后，保留该提交及其上游 Git 历史。赞助者玩偶署名也来自该版本。

## 本移植版新增与适配

- **服务端自定义玩偶同步**：将新版 Java 自定义玩偶 ZIP 放入服务端 `config/kaleidoscope_doll/custom/`，重启或执行 `/kaleidoscope_doll reload`。缓存按服务器隔离，离服后停用。
- **自定义玩偶配置包**：安装方式与迁移注意事项见[配置文档](docs/sponsored-doll-config.md)；赞助者玩偶配置 ZIP 不随仓库附送。
- **玩偶的新功能**：玩偶可与末影珍珠、风弹或不死图腾合成，在铁砧中可以把头盔放在前面玩偶放在后面像附魔书一样给他们加在一起然后就可以在带着头盔的情况下带着玩偶

自定义玩偶、模组电脑等玩法不全是本仓库原创；上面列出的是本移植的新增或适配内容。

## 环境与构建

### 前置依赖与下载

请选择下表中的 Minecraft Java Edition 版本，安装对应的 Java 和 Fabric：

- [Fabric Loader 安装器](https://fabricmc.net/use/installer/)
- [Fabric API（Modrinth）](https://modrinth.com/mod/fabric-api)
- [GeckoLib（Modrinth）](https://modrinth.com/mod/geckolib)
- [Forge Config API Port（Modrinth）](https://modrinth.com/mod/forge-config-api-port)

配方查看器为可选项：[Just Enough Items / JEI（Modrinth）](https://modrinth.com/mod/jei)。

使用 Fabric Loader `0.20.1` 或更高版本。下载其他依赖时，请在对应页面选择相同的 Minecraft 版本和 **Fabric**；客户端和服务端都应安装本模组及其必需依赖。不同 Minecraft 版本的模组 JAR 不可混用。

| Minecraft | Java | Fabric API（构建版本） | GeckoLib（构建版本） | Forge Config API Port（构建版本） |
| --------- | ---- | ---------------------- | -------------------- | --------------------------------- |
| 1.20.1    | 17+  | `0.92.12+1.20.1`       | `4.8.6`              | `8.0.3`                           |
| 1.21.1    | 21+  | `0.116.18+1.21.1`      | `4.9.5`              | `21.1.6`                          |
| 1.21.4    | 21+  | `0.119.4+1.21.4`       | `4.8.5`              | `21.4.3`                          |
| 1.21.10   | 21+  | `0.138.4+1.21.10`      | `5.3-alpha-3`        | `21.10.1`                         |
| 26.1      | 25+  | `0.145.1+26.1`         | `5.5`                | `26.1.0.2`                        |
| 26.1.2    | 25+  | `0.155.3+26.1.2`       | `5.5.3`              | `26.1.5`                          |
| 26.2      | 25+  | `0.161.0+26.2`         | `5.5.7`              | `26.2.1`                          |
| 26.3      | 25+  | `0.166.2+26.3`         | `5.5.7`              | `26.3.1`                          |

1.20.1 没有原版风弹，因此不包含风弹玩偶功能。1.21.4 不包含本模组的 JEI 配方集成。自定义玩偶 ZIP 需使用目标版本支持的资源包格式；1.20.1 和 1.21.1 的 Java 自定义玩偶只支持引用单个模型的 `minecraft:model` 定义。

使用 JDK 25 启动项目自带的 Gradle Wrapper（当前 Fabric Loom 插件需要 Java 25）。Gradle 会按目标版本选用实际的编译和运行 JDK：`1.20.1` 用 JDK 17，`1.21.x` 用 JDK 21，`26.x` 用 JDK 25。构建旧版时请额外安装对应的 JDK；若 Gradle 未自动找到，可传入 `-Porg.gradle.java.installations.paths=<目标 JDK 安装目录>`。

```sh
./gradlew build
./gradlew '-PminecraftTarget=26.1.2' build
```

Windows 将 `./gradlew` 换成 `.\gradlew.bat`。默认构建 26.3，产物位于 `build/libs/`；其他目标可传 `-PminecraftTarget`，产物位于对应的 `build/minecraft-<版本>/libs/`。

使用 `./gradlew '-PminecraftTarget=26.1.2' runGameTest` 可用目标 JDK 运行无界面世界测试，覆盖电脑扣费、抽奖机、玩偶物品数据与自定义 ZIP 登记。仅在需要用其他 JDK 测试游戏时，才传入 `-PminecraftRuntimeJava=<版本>`；这不会改变编译 JDK。

推送到 `fabric` 或向该分支提交拉取请求时，GitHub Actions 会自动构建并运行世界测试，也可在 **Actions → Build** 手动运行。成功后可从该次运行的 **Artifacts** 下载对应版本的模组 JAR。

默认分支构建成功后，工作流会按 `gradle.properties` 的 `mod_version` 自动创建 `v<版本号>` 标签和 GitHub Release，并附上 26.3 模组 JAR；其他版本从 Actions 构建产物下载。相同版本不会重复发布或替换；发布下一版时请先更新 `mod_version`。PR 构建不会发布。

## 许可证与致谢

代码沿用原项目的 [BSD 3-Clause 许可证](LICENSE)。模型、贴图及其他美术资源不适用代码许可证，仍为 **All Rights Reserved**，相关权利归原作者及权利人。仓库上传许可不构成对第三方的再分发授权。
