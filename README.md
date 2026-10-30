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
gradlew.bat build
```

## 许可与署名

本项目保留 MIT 许可和上游贡献者署名。部分冶炼链方块、界面、容器和像素材质来自上游项目的相应资源，并已放入独立的 `tinker_foundry` 命名空间。项目不使用上游 Mod ID、包名、Logo 或旧存档兼容层。

## 按标签生成熔融流体与熔炼配方

游戏启动时扫描已加载模组自带的 `data` 标签资源，为粒、锭、块、粗矿、粗矿块、矿石、粉和板补充熔融流体及熔炼配方。物品和已有流体都只按 `forge:` / `c:` 标签识别，不根据注册名、文件名中的材料字样或物品外观猜测。分类标签（例如矿石产量和基岩类别）不作为材料。只出现在 `storage_blocks` 标签里的材料（干草块、骨块、海带块、史莱姆块等）和带锭标签的砖类（`brick`、`nether_brick`）不算金属材料，不生成熔融流体；确实需要时可用流体标签或 JSON 配置显式声明。

物品标签支持 `nuggets/<材料>`、`ingots/<材料>`、`storage_blocks/<材料>`、`raw_materials/<材料>`、`raw_blocks/<材料>`、`storage_blocks/raw_<材料>`、`ores/<材料>`、`dusts/<材料>`、`plates/<材料>` 和 `gems/<材料>`。流体标签支持 `c:molten_<材料>`、`forge:molten_<材料>`、`molten_metals/<材料>` 和 `molten/<材料>`。存在明确外部流体标签时优先复用；多个不同源流体存在歧义时跳过自动配方；本体基础工艺的明确材料引用按注册 ID 的稳定顺序选择一个已标记的外部源流体，避免丢失已有配方。同一流体的 source/flowing 会归一，不会因此额外注册备用流体。

缺失的流体使用 `tinker_foundry:molten/<材料>`，同时注册 `_flowing` 流动流体、`_fluid` 方块和 `_bucket` 桶。资源由内存包提供，只包含流体标签、桶模型和方块状态，使用共用熔融纹理；流体和桶的名称一律来自 lang 文件（`fluid_type.tinker_foundry.molten.<材料>`、`item.tinker_foundry.molten.<材料>_bucket`），缺少 lang 时不回退英文。未知材料默认采用统一熔融色和 1100 K 流体温度。铁、金、铜、钴通过自身的流体标签声明基础工艺所需材料；原固定金属注册已移除，非金属工艺流体保留。不兼容原固定金属流体 ID 的旧存档。

本模组配方中的 `tinker_foundry:molten/<材料>` 是明确的材料引用，解析时会转换为启动标签选定的外部流体或生成流体。没有高优先级整配方覆盖包，用户手写配方的覆盖顺序保持有效。已有熔炼配方优先；已有配方的流体属于该材料标签时，会校准每锭用量和温度。默认一锭 90 mB、一粒 10 mB、一块 810 mB，矿石和粗矿沿用本体已有 `metal` 增产规则。宝石以 `gems` 标签识别，默认每个 100 mB、每块 900 mB，并通过已有宝石配方校准；矿石沿用 `gem` 增产规则。本体保留的钻石、绿宝石、玻璃和黏土熔融流体已补标准标签，避免额外生成重复流体。装备仍需明确的成分配方。

将物品加入 `tinker_foundry:melting/automatic_blacklist` 标签并 `/reload` 可排除自动熔炼配方；此标签不删除流体注册或手写配方。

注册发生在数据包正式加载之前，因此新增材料需要客户端和服务端安装相同的启动标签资源并重启游戏。世界数据包、运行脚本在之后才添加的新材料，不能靠 `/reload` 新增流体。启动扫描跳过条件标签；空标签、没有终端值的循环标签、仅含无法提前确认的可选物品引用的标签不触发生成。可选外部流体引用也无法在注册前证明存在，因此可能准备一个未使用的备用流体；数据加载后自动配方仍按实际绑定的流体标签优先复用外部流体。需要启动时明确避免备用注册时，流体提供方应在标准流体标签中使用必需引用。

基础流体标签中的可选 `tinker_foundry` 字段可以设置 `color`（ARGB 十六进制） 和 `temperature`；名称请写入 lang 文件；普通材料标签无需这些字段。

Automatic molten fluids and melting recipes use common material and fluid tags only. Item/fluid registry-name guessing is disabled. Existing tagged fluids take priority; missing materials receive complete source/flowing fluid, block, bucket and generated resources. Explicit material references in Foundry recipe JSON resolve to the selected fluid without overriding user recipes. New registries require matching startup resources and a full client/server restart. Late world datapacks cannot register new fluids on reload. Empty, cyclic and optional-only item declarations do not establish startup material existence. Optional-only fluid references can require an unused fallback reservation because their targets cannot be verified before registration.

## 用 JSON 注册自定义熔融流体

启动时递归读取 `config/tinker_foundry/fluids/` 中所有 `.json` 文件，每个文件定义一种材料，例如 `tin.json`：

```json
{
  "material": "tin",
  "color": "#FFD5DDE0",
  "temperature": 1100
}
```

三个字段均必填。`material` 支持 `tin` 或 `tinker_foundry:molten/tin`，材料部分只允许小写字母、数字、下划线、点和连字符；`color` 支持 `#RRGGBB`（不透明）或 `#AARRGGBB`；`temperature` 是非负的开尔文整数。没有对应物品标签也可显式注册流体；要自动生成熔炼配方，输入物品仍需相应材料标签。

不填写名称字段。配置材料的流体和桶通过资源包 `assets/tinker_foundry/lang/zh_cn.json` 提供名称，例如：

```json
{
  "fluid_type.tinker_foundry.molten.tin": "熔融锡",
  "item.tinker_foundry.molten.tin_bucket": "熔融锡桶"
}
```

其他语言放入相应 lang 文件。配置材料不生成默认翻译，不会覆盖资源包提供的名称。已有带标准标签的外部流体继续优先复用，使用原模组的语言键，其颜色和温度不会被此配置改写。为本模组生成的材料填写 JSON 时，颜色和温度优先于标签内的可选元数据。

文件按相对路径排序，同材料第一份有效配置生效；坏文件跳过并输出日志，其他文件继续读取。第一次创建目录时会写入 `example.json.disabled` 与 `说明.txt`，示例默认不生效。客户端和服务端需要相同配置，修改后完全重启；`/reload` 不能新增流体。

发布与使用文档：[CurseForge](https://www.curseforge.com/minecraft/mc-mods/tinker-foundry) · [自动流体、限制与配置](https://github.com/hp3081925122/Tinker-Foundry/wiki/Automatic-Fluids.zh-CN) · [本次更新日志](CHANGELOG.md)。
