# Tinker Foundry

Tinker Foundry 是面向 Minecraft 1.21.1 NeoForge 的独立冶炼与浇注模组。它只实现冶炼炉、铸造炉、熔炼器、合金炉、加热器、流体设备、配方和容器，不提供工具、材料词条、Modifier、匠魂书或旧存档兼容。

## 当前环境

- Minecraft 1.21.1
- NeoForge 21.1.250
- Java 21
- Mod ID：`tinker_foundry`
- JEI：可选，仅在编译时使用 API，运行时不安装 JEI 也可以启动

## 使用方式

普通设备右键打开界面。桶和便携储液罐可以在服务端完成装液、取液；排液口、浇注口、流体管道和导流槽只在相邻方块实体之间传输，不会扫描全世界。

冶炼炉和铸造炉控制器必须位于矩形外壳侧壁，炉腔保持空气；储罐和排液口放在外壳上，不能放入炉腔。容量和物品输入槽数量按实际结构重新计算。

控制器使用共享总容量的多流体槽（最多 128 种流体及组件组合）。同类流体合并，不同流体按层显示；空手左键点击流体层可将其移到底部，排液口优先抽取底层。手持空桶左键某层取液，手持满桶右键主槽倒入。中央容器槽按自动、倒空、装满模式处理。冶炼炉会按配方进行炉内合金，铸造炉保留分离产物、不自动合金。

结构中的多个燃料罐共同供料，同类燃料可跨罐满足一次配方消耗，来源耗尽后搜索其他罐。右侧燃料栏汇总当前流体同类罐的数量，并把同类罐和空罐容量相加；不同流体不混加。燃料温度由 `fuel` 配方决定，没有配方的流体显示无效燃料。右键燃料栏倒入、左键取出均支持跨罐搬运。

主槽提示显示容量、空余和已用，默认按锭／粒／毫桶计量，按 Shift 改为桶／毫桶。当前项目一锭为 90 mB、一桶为 1000 mB。

熔岩燃料为 1000°C，每次点火扣除 50 mB、提供 100 份燃烧量。控制器每四游戏刻按炉壁和底板规模扣除燃烧量；火焰显示当前燃烧段剩余比例，液面显示多罐储液总量。空闲时不再点燃新燃料，已点燃的热量继续耗尽。

多方块输入数量随炉腔体积变化，不再封顶 28 槽。列数按 `min(4, ceil(槽数 / 7))`，每槽一个物品；侧栏最多显示十一行，更多输入可滚轮或拖动滚动条访问。末行不存在的槽绘制为空白。扩容保留物品和热量，缩容把超出槽位的物品掉落在控制器附近；结构规模变化会关闭旧菜单并返还桶槽物品，请重新打开。数据槽协议仍有短整数编号上界，当前结构检测器允许的全部炉腔规模均在范围内。

本次存档使用 `InputSize` 保存动态槽数，使用有序 `StructureFluids` 保存多流体；不迁移旧格式。请用新炉体或新测试世界验收；客户端和服务端须使用相同版本（网络协议版本 4）。输入物品保存使用 1.21.1 编码器的返回标签，旧版已丢失物品标识的条目无法恢复，不做推测性补偿。

当前不是匠魂 1.20.1 冶炼系统的完整等价实现。已核对的差异与本轮修改边界见 `docs/smeltery-parity.md`。

## 数据配方接口

所有配方均使用 `tinker_foundry:*` 类型，不兼容原项目配方 JSON。

- `melting`：`ingredient`、`result`、`temperature`、`time`
- `alloying`：`ingredients`、`result`、`temperature`；每个输入可设置 `catalyst: true`
- `casting`：`fluid`、可选 `mold`、`result`、`time`
- `molding`：`mold`、`fluid`、`result`、`time`，可选 `remainder` 返还容器或模具
- `fuel`：可选 `item` 或 `fluid`、`duration`、`temperature`、`consumption`

物品输入支持精确注册名和物品标签；流体输入支持精确注册名和流体标签。冶炼配方已经使用 `c:ingots/*`、`c:nuggets/*` 和 `c:storage_blocks/*` 标签，因此安装其他金属模组后可直接熔炼其标准标签物品。

外部金属的浇注输出需要由外部数据包或兼容模组添加精确结果物品 ID，例如：

```json
{
  "type": "tinker_foundry:casting",
  "fluid": {"ingredient": {"fluid": "tinker_foundry:iron"}, "amount": 90},
  "mold": {"item": "tinker_foundry:ingot_cast"},
  "result": {"id": "other_mod:iron_ingot", "count": 1},
  "time": 60
}
```

## 构建与验证

```text
gradlew.bat compileJava
gradlew.bat processResources
gradlew.bat foundryUnitTest runGameTestServer
gradlew.bat build
```

游戏测试覆盖流体容量、配方 Codec、熔炼、并行输入、可变尺寸结构、浇注链、流体燃料、专用燃料罐、容器模具返还、存档重载、输出保护、合金消耗和三输入合金接口。

## 许可与署名

本项目保留 MIT 许可和上游贡献者署名。部分冶炼链方块、界面、容器和像素材质来自上游项目的相应资源，并已放入独立的 `tinker_foundry` 命名空间。项目不使用上游 Mod ID、包名、Logo 或旧存档兼容层。公开发布前仍需确认上游对未来 Java Forge/NeoForge 移植边界的许可。
