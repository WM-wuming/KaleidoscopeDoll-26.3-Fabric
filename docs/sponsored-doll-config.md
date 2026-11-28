# 自定义玩偶

服务端和客户端都需要安装同一新版模组。把配置包文件夹放到服务端的 `config/kaleidoscope_doll/custom/`，重启或执行 `/kaleidoscope_doll reload`。单人游戏把文件夹放到本机同名目录即可。

```text
config/kaleidoscope_doll/
├── kaleidoscope_doll-common.toml
└── custom/
    └── KaleidoscopeDoll-Sponsored-Config-26.3/
        ├── pack.mcmeta
        └── custom_dolls/
            ├── models/doll_99.json
            ├── textures/doll_99.png
            ├── lang/zh_cn.json
            └── loot_table/doll_99.json
```

直接扫描 `models/`，不需要也不生成 `items/` 或 `custom_dolls.json`。`textures/`、`lang/`、`loot_table/` 都是可选目录。模型引用包内贴图时，使用例如 `kaleidoscope_doll_sponsors:block/doll/doll_99` 的路径；模组从包内引用推导命名空间，没有这类引用时使用规范化后的包目录名。`pack.mcmeta` 必须适配目标游戏版本，26.3 的资源包格式是 `[97,1]`。

## 掉落表

`loot_table/` 与 `models/` 中相同的相对文件名对应。同名有效掉落表会替换原玩偶掉落；没有表则掉落原玩偶。空表不掉落原玩偶，也不产生替代物品。损坏的表会记录日志并保留原掉落，玩偶实体手持的其他物品仍按原规则掉落。

例如，`loot_table/doll_99.json`：

```json
{
  "pools": [
    {
      "rolls": 1,
      "entries": [{"type": "minecraft:item", "name": "minecraft:diamond"}]
    }
  ]
}
```

## 服务端同步设置

只读取 `config/kaleidoscope_doll/kaleidoscope_doll-common.toml`，不会读取或迁移旧模组的配置文件。以下是默认值：

```toml
[serverDollSync]
EncryptServerDollPacks = true
MaxPackCount = 8
MaxTotalMiB = 16
```

两个上限的可配置范围都是 `0` 到 `64`，任一为 `0` 时关闭同步。`MaxTotalMiB` 按全部包压缩后、加密前的合计大小计算。修改设置后重启服务器。

客户端连接后通过模组网络通道同步模型、贴图和语言；掉落表只在服务端使用。同步不走原版服务器资源包下载、不写入 `resourcepacks/`，也不受游戏内「服务器资源包」开关影响。

默认开启加密时，客户端磁盘只保留按服务器地址隔离的 `.zip.enc`，进服后使用该服密钥在内存解密，离服后卸载。同一服务器的内容没变时复用缓存。服务端密钥位于 `config/kaleidoscope_doll/server-doll-sync.key`，不要在不同服务器之间共用。此功能保护的是离线缓存；已经进服的客户端仍能访问解密后的资源，第三方登录插件的 `/login` 不参与同步。
