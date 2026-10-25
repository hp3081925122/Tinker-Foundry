# Tinker Foundry 1.19.2 Forge 迁移文档

核对日期：2026-09-23  
目标分支：`1192forge`  
目标工作树：`E:\模组\发cf的模组\Tinker Foundry\1.19.2-forge-tinker-foundry`  
状态：独立工作树与 1.19.2 模板配置已落地；源码 API 迁移进行中，尚未完整。

## 1. 目标与来源

将 Tinker Foundry 的独立熔炼、合金和浇筑体系迁移到 Minecraft 1.19.2 Forge。目标不是新增匠魂工具、材料词条、Modifier 或匠魂书依赖；模组 ID、Java 包、内容 ID 和现有功能语义应保持稳定。不同 Minecraft 版本之间不承诺网络字节或存档格式兼容。

| 用途 | 路径/版本 | 处理方式 |
| --- | --- | --- |
| 迁移源 | `1.20.1-forge-tinker-foundry`，分支 `1201forge`，HEAD `1102c561` 加其当前工作树改动 | 只读源快照；保留所有未提交内容，不在源目录上修改 |
| 行为参考 | `Tinker Foundry`，分支 `master` | 只读；用于区分原始 1.21.1 NeoForge 行为和 Forge 迁移增补 |
| 辅助参考 | `26.1.2-neoforge-tinker-foundry`，分支 `2612neoforge` | 只读；逐项采用，不把 NeoForge API 直接复制到 Forge |
| 目标模板 | `E:\模组\模组模板\1192-forge` | 目标构建插件、版本、Java、映射和元数据的唯一模板依据 |

1.20.1 源工作树在迁移启动时有 446 个跟踪文件差异和 337 个未跟踪文件。必须把这份实际内容作为迁移功能基线，不得只从源分支 HEAD 复制；未跟踪的 1.20.1 阶段文档不属于目标代码，目标分支维护本文件及 1.19.2 自己的审计资料。

## 2. 目标平台冻结项

| 项目 | 目标值 |
| --- | --- |
| Minecraft | `1.19.2` |
| Forge | `43.5.2`，取自匹配的 `1192-forge` 模板 |
| Java | 17 |
| 映射 | Official，Minecraft `1.19.2` |
| Gradle Wrapper | `8.8`，取自匹配模板 |
| 模组 ID | `tinker_foundry` |
| Java 包 | `org.hp.tinker_foundry` |
| 许可证 | MIT |
| 数据命名空间 | `tinker_foundry` |
| 可选配方查看器 | JEI 11.6.0.1019，CurseForge 文件 ID `5106178`；本机缓存中已核对该精确 1.19.2 Forge API |

构建版本属性、ForgeGradle `data` run、JEI CurseMaven 依赖、Wrapper 和资源包格式已按目标模板调整；仍需继续静态核对剩余源代码与资源。ForgeGradle 配方、run 配置或依赖不能从 1.20.1/NeoForge 文件中凭印象推导。

## 3. 必须保留的功能与逻辑契约

迁移采用 1.20.1 Forge 源树为功能起点，并保留以下已盘点范围；如源树内实现与本节冲突，以玩家可见功能和稳定内容 ID 为目标，先在目标版本证据中解释差异，再适配。

| 功能面 | 必须保留的行为 |
| --- | --- |
| 注册内容 | 方块、方块实体、物品、流体、菜单、实体、创造标签及动态生成条目；110 个已盘点物品 ID，不随迁移静默删减或改名 |
| 熔炼设备 | 单方块熔化炉、冶炼炉、多方块铸造炉和合金炉；温度、燃料、进度、容量、输入/输出状态 |
| 多方块 | 结构边界、墙/底板/内部判定、控制器、燃料罐、设备登记、错误位置和修复提示 |
| 调试功能 | `/tinker_foundry debug generate <smeltery|foundry|alloyer|all>`；生成的结构必须含可操作控制器、燃料、流体、浇注口及浇注设备 |
| 流体 | Forge 流体能力、流体罐层、燃料罐、桶和其他能力容器双向交互、自动化输入/输出、浇注及连续液柱 |
| 便携容器 | 容量、填充/排出模拟和实际操作、流体持久化、空罐清理、损坏后流体保留、物品栏容量提示 |
| 配方 | 保留八种自定义 type/serializer：`entity_melting`、`melting`、`ore_melting`、`damageable_melting`、`alloying`、`casting`、`molding`、`fuel`；普通原版配方仍使用原版 serializer |
| 原矿增产 | 按配方 `rate` 缩放主产物和副产物；计算后的产物必须进入实际熔炼队列，而不能只在 JEI 中显示增量 |
| 浇筑 | 浇注盆/台、模具、砂模和产物占用检查；产物已经生成后不允许重复浇筑 |
| 网络 | 保留逻辑载荷 ID `foundry_state`、`foundry_structure_error`、协议版本语义 `6`、字段顺序和范围检查；重新实现为 1.19.2 Forge 通道，不要求跨版本旧字节互通 |
| 界面与集成 | 菜单槽位、进度、温度、燃料/流体容量和结构状态显示；JEI 分类继续覆盖所有自定义配方 |
| 资源 | 模型、方块状态、贴图、声音、语言、独立指南、掉落表、tag 和配方 JSON；英文默认并保留中文 |

方块实体需要保留既有领域状态含义，包括输入/输出、燃料、熔融流体、配方进度、容量、结构范围和流体层、冷却/浇注状态、合金输入等。实现时根据目标源码选择 1.19.2 Forge 的 NBT 与能力 API；不要求读取其他版本保存的数据。

### 原矿增产规则及跨版本核对

原矿熔炼结果由 `OreMeltingRecipe.output(foundry)` 按基础流体量缩放；实际配方匹配路径须把该返回值作为 `MeltingMatch` 主产物写入熔池。`rate` 为 `default` 时保持基础量，副产物 `default` 继承主配方的 `rate`。

| `rate` | 非铸造炉主产物 | 铸造炉主产物 | 副产物倍率 |
| --- | --- | --- | --- |
| `metal` | 基础量 × 12/9 | 基础量 × 9/9 | 基础量 × 3/9 |
| `gem` | 基础量 × 8/4 | 基础量 × 4/4 | 基础量 × 4/4 |

跨版本静态核对了 1.19.2、1.20.1、1.21.1 和 26.1.2 四个工作树：`copper_ore`、`deepslate_copper_ore`、`deepslate_gold_ore`、`deepslate_iron_ore`、`gold_ore`、`iron_ore`、`nether_gold_ore`、`raw_copper`、`raw_copper_block`、`raw_gold`、`raw_gold_block`、`raw_iron`、`raw_iron_block` 共 13 个配方 ID 集合完全一致，且每个配方均声明 `rate: metal`。普通矿石和原矿基础产物均为 90 mB，原矿块为 810 mB。四版 `OreMeltingRecipe` 均保留普通熔炉 `12/9`、铸造炉 `9/9` 的金属倍率；副产物倍率及 `default` 继承逻辑也保留。四版 `FoundryBlockEntity` 的实际匹配路径都把倍率计算结果写入熔炼结果，而非只改 JEI 显示。目标 1.19.2 JSON 使用 `recipes/melting/`；1.21.1/26.1.2 使用各自版本的 `recipe/melting/` 路径并保留各自 ingredient schema。1.19.2 静态 GameTest 另覆盖普通炉 120 mB、铸造炉 90 mB 和继承倍率副产物 30 mB，但没有运行测试。以上仅为源码、配方和调用链静态核对，不代表构建或游戏内验证。

## 4. 版本差异与适配要求

所有目标类、方法、事件和序列化签名必须先由 Forge 43.5.2 模板依赖、映射源码、反编译产物或同版本项目核实。1.20.1 和 NeoForge API 只作为行为线索。

| 区域 | 迁移要求 |
| --- | --- |
| 注册 | 已核实 43.5.2 的 `Registry` key；1.19.2 创造标签页改为静态 `CreativeModeTab`，配方 type/serializer 改用 1.19 注册 key；其余注册对象继续核对 |
| 物品数据 | 1.19.2 不使用 1.20.5+ Item Data Components；以已证实的 `ItemStack` NBT 与 Forge fluid capability 保存便携罐语义 |
| 方块实体能力 | 按 Forge 43.5.2 的 capability provider、方向查询和生命周期实现；确保 GUI、桶、自动化和存档访问同一状态 |
| 网络 | 按 Forge `SimpleChannel` 与 1.19.2 buffer API 重建注册、编解码和客户端分发；保持逻辑字段顺序、边界和服务端权威语义 |
| 配方 | 以目标 `Recipe`、`RecipeType`、`RecipeSerializer`、`Ingredient` 和 `FriendlyByteBuf` 为准；不带入新版 `MapCodec` / `StreamCodec` / `SingleRecipeInput` 形态 |
| 流体配方 | 按 1.19.2 Forge 流体 Ingredient/FluidStack 实际实现输入匹配、序列化和 JEI 表达；不能为通过编译删去流体输入或副产物 |
| 菜单 | 以 1.19.2 `AbstractContainerMenu`、`ContainerData`、MenuType 注册和 Screen 注册签名实现；保留槽位和同步边界 |
| 渲染 | 已按 1.19.2 目标 API 将 Screen 绘制迁回 `PoseStack`/`GuiComponent`、模型烘焙改为 `ModelBakery`，颜色类型改为 `MaterialColor`；剩余渲染及资源 loader 仍需核对 |
| 事件/命令 | 核实 Forge 43.5.2 的事件总线、客户端注册、命令事件和权限方法；调试命令须服务端执行并校验权限 |
| GameTest | Forge `43.5.2` 精确 JAR 已核实 `GameTestHolder`、`PrefixGameTestTemplate`、`@GameTest`、当前测试调用的 `GameTestHelper` 方法与 `fail(String)`；测试类注解和 `gameTestServer` 命名空间配置也已静态核对。发现目标 API 没有 `GameTestHelper.assertTrue/assertFalse`，已改为本地断言适配器调用 `fail(String)`，保留原测试断言；尚未执行 GameTest，完整运行通过仍待验证 |
| JEI | 已用本机精确 11.6.0.1019 JAR 核实插件使用 `PoseStack`、`IRecipeCategory.setRecipe/draw`、`IRecipeLayoutBuilder.addSlot`，并检查 `ForgePluginFinder` 从 Forge 扫描注解后才反射构造 `@JeiPlugin`；JEI 依赖在 `mods.toml` 为客户端可选，项目内 JEI 类型引用仅位于 `FoundryJeiPlugin`，没有主入口直接调用。静态结构支持缺少 JEI 时不加载插件类；有/无 JEI 的真实启动和最终展示仍待验证。熔炼时间箭头已按配方时间恢复动画 |

## 5. 配方和资源迁移

1.19.2 配方、掉落表和 tags 使用目标 Minecraft 版本资源目录与 schema。目录名称及字段必须从 1.19.2 模板资源、Forge/Minecraft 源码或 1.19.2 数据中核实；不能把 1.20.1 的 `pack.mcmeta`、模型 loader、配方字段或新版物品组件 JSON 原样视为兼容。

当前 1.19.2 `src/main/resources/data` 下 369 个 JSON 均通过大小写敏感的严格 JSON 解析：220 个配方、77 个掉落表、64 个 tags 和 8 个指南数据文件。77 个掉落表的根类型均为 `minecraft:block`，使用的唯一掉落条件 `minecraft:survives_explosion` 已在 1.19.2 `LootItemConditions` 中注册；其 77 个表名均对应已注册方块，掉落物引用均对应已注册模组物品或 1.19.2 原版物品。64 个 tag 文件共有 184 个条目，`values` 均为合法数组、条目均为字符串；12 个嵌套 tag 引用均能在目标树或精确 1.19.2 原版服务器数据中解析，所有模组、原版方块/物品/实体条目均对应有效注册 ID。`replace` 按 1.19.2 `TagFile.CODEC` 可选且默认 `false`，两个空 tag 也是合法结构。280 个模型文件和 97 个 blockstate 中的本模组父模型、blockstate 模型与纹理引用均能解析到现存资源；21 个 Forge 流体容器模型、26 个连接玻璃模型、10 个储罐模型均满足对应 loader 的静态字段形状，并已检查两个本模组几何 loader 的客户端注册入口。以上属于静态结构核对，不代替数据包加载或实际渲染验证。与 1.20.1 资源树逐路径比较，仅缺 `data/tinker_foundry/damage_type/{smeltery_heat,smeltery_magic}.json` 与 `data/minecraft/tags/damage_type/is_fire.json`。这是目标版本适配而非漏迁：Minecraft 1.19.2 使用 `DamageSource` 实例，不使用后续版本的数据驱动伤害类型；熔炉热伤害通过 `setIsFire()` 保留火焰标记，针对抗火实体则使用不带火焰标记的来源，并以 `msgId` 保留死亡提示键。1.19.2 `DamageSource` 默认耗损为 `0.1` 且不随难度缩放，与源资源字段一致。指南章节/图片等资源虽然保留，但源项目各分支都没有发现章节加载器或对应指南界面，因此不能将其计作已加载的游戏内指南功能。

另对 220 个配方中的 546 个物品、tag、实体 tag、流体及主/副产物 ID 引用执行闭包检查，目标模组注册、1.19.2 原版注册及可用 tag 中均可解析，未发现无效引用。此项仍属于静态检查，不代表数据包在游戏中成功加载。

迁移源和目标的 `src/main/java` 文件相对路径均为 75 个且集合相同；资源文件相对路径为源 1019 个、目标 1016 个，除上述三个版本专属伤害类型数据资源外没有缺项或目标独有文件。通过注册声明及动态流体注册名称静态抽取，1.19.2 与 1.20.1 的 97 个方块 ID、110 个物品 ID 集合完全一致。逐配方检查原版物品 ID/tag 时发现三个 1.20.1 专属引用，已改为 1.19.2 可用写法：煤燃料显式接受煤炭与木炭；删去 1.19.2 不存在的装饰陶罐和陶片输入。其他原版 `minecraft:` 物品引用均能在 1.19.2 `Items` 常量中对应。220 个配方 JSON 已完成严格语法解析及字段结构静态核对；对照目标 `SingleItemRecipe.Serializer#fromJson` 后，发现 14 个石切配方必须使用字符串 `result` 和根级整数 `count`，已将它们调整为目标格式并保留数量。该结果只证明数据形状与已核对的 serializer 字段相符，不等于游戏启动时成功加载全部配方。

每个源配方要登记原 ID、type、输入、输出/副产物、温度/时间、目标路径及转换结果。八个自定义配方注册 ID 不变；原版 `crafting`、`smelting`、`blasting`、`smoking`、`campfire_cooking`、`stonecutting` 等数据继续使用原版配方格式。药水等组件结果改用 1.19.2 可读的数据表示，但须保留药水效果、颜色与流体产量语义。

资源盘点必须覆盖：assets 的 blockstates/models/textures/lang/sounds、数据包 recipes/loot tables/tags、`pack.mcmeta`、书籍指南 JSON 与其图片/NBT 结构。升级/降级目录只按目标版本规则；不得仅因名称或目录为空而删除资源。

## 6. 实施顺序和完成门槛

1. 建立并核实 `1192forge` 分支与独立工作树；冻结源目录只读。
2. 从 1.20.1 实际工作树导入已跟踪变更及必需的未跟踪源码/资源，排除 `build/`、`.gradle/`、`run/`、本地日志和仅 1.20.1 阶段文档；逐项记录来源差异。
3. 按 1.19.2 Forge 模板落定构建、Wrapper、版本属性和 `mods.toml`。
4. 迁移注册和启动入口，再迁移方块/物品/流体/方块实体与持久化、能力和容器。
5. 迁移多方块检测、设备交互、调试生成命令、服务端事件及网络。
6. 迁移八种配方 Java 实现和所有 JSON 数据，再迁移菜单/GUI、渲染、JEI、模型和其他资源。
7. 静态核对注册 ID 数量、配方数量/目录/schema、NBT 语义、网络字段顺序、无跨版本 API/包残留、metadata 展开键、翻译 key 和模板示例清理。

迁移完成必须能逐项证明第 3 节功能未被静默删减，并记录每项证据。静态检查只能证明源码/资源结构，不能表述为编译、启动或游戏内行为通过。Gradle build、compile 和 runClient 需遵守用户针对 Tinker Foundry 的单独授权要求。

## 7. 当前未决项

- JEI 11.6.0.1019 有/无安装两种客户端启动，以及配方分类最终展示；可选依赖、插件发现路径和方法签名已静态核实。
- 模型父级、blockstate 模型、纹理闭包及 loader 字段已静态核验，但游戏内资源加载、渲染和数据包加载仍未验证。
- Forge 43.5.2 精确 JAR/源码已核对项目所用的流体能力、方块实体生命周期、事件与网络关键方法；GameTest 注解/调用已静态核对，模板 `minecraft:bastion/blocks/air` 存在于精确 1.19.2 服务端 JAR。GameTest、编译、启动及游戏内行为仍未运行验证。
- 便携罐模型已静态核实：Forge `43.5.2` 精确映射 JAR 含 `DynamicFluidContainerModel.Loader`，其读取器要求 `fluid` 字段并支持 `base`/`fluid` 材质键；目标物品模型 JSON 使用 `forge:fluid_container` 并提供这些字段。游戏内渲染仍未验证。
- 书籍/指南资源在 1.19.2 的格式和本项目图片授权范围；不阻塞主体功能迁移，但不能静默删除。

## 8. 变更记录

- 2026-09-23：依据实际 1.20.1 Forge 工作树、1.20.1 迁移需求清单及本机 1.19.2 Forge 模板建立本计划。本文落盘后才开始迁移代码和资源。
- 2026-09-23：完成目标版本构建元数据初迁，并静态适配配方签名、注册表访问、创造标签页、客户端 GUI、模型 API 和 JEI 11；原矿倍率公式及熔炼调用保留，并增加覆盖普通炉、铸造炉及继承倍率副产物的 GameTest。JEI 熔炼箭头动画已按原配方时间恢复。未运行 build、compile、GameTest 或 runClient。
- 2026-09-23：通过 Forge `43.5.2` 精确映射 JAR 的类与字节码核实 `forge:fluid_container` loader 及便携罐 JSON 字段；只确认静态 API/资源匹配，未作客户端渲染验证。
- 2026-09-23：通过 JEI `11.6.0.1019` 精确 JAR 的插件发现字节码及目标依赖引用范围，静态确认可选 JEI 插件隔离路径；有/无 JEI 的客户端启动仍未验证。
- 2026-09-23：通过 Forge `43.5.2` 精确映射 JAR 逐项核对现有 GameTestHelper 调用，修复不存在的 `assertTrue/assertFalse` 调用，使用目标 API 的 `fail(String)` 保留失败语义；未执行编译或 GameTest。
- 2026-09-23：严格解析目标数据目录全部 369 个 JSON，并与 1.20.1 资源树逐路径比较；确认仅三个后续版本的数据驱动伤害类型资源未复制，并由 1.19.2 `DamageSource` 源码及调用点确认已改为等价实例语义。确认现存指南数据/图像在源分支也没有 Java 加载入口，不把资源存在误记为运行时指南功能。未运行构建或游戏测试。
- 2026-09-23：比较目标与 1.20.1 的 Java/资源相对路径及注册 ID 集合：75 个 Java 文件路径一致；仅版本专属伤害数据文件未迁；97 个方块 ID 与 110 个物品 ID 集合一致。
- 2026-09-23：逐项将配方引用的 Minecraft 物品/tag 与精确 1.19.2 `Items`、`ItemTags` 源码对照，修正 `minecraft:coals`、`minecraft:decorated_pot_sherds` 和 `minecraft:decorated_pot` 三个不存在的 ID/tag 引用；煤配方保留煤炭与木炭两种 1.19.2 原版燃料。未运行构建或游戏测试。
- 2026-09-23：对 220 个 1.19.2 配方 JSON 完成严格解析和字段结构静态检查；依 1.19.2 `SingleItemRecipe.Serializer#fromJson` 要求，将 14 个石切配方从对象型 `result` 改为字符串，并保留根级 `count`。运行时数据包加载仍未验证。
- 2026-09-23：静态核对 77 个掉落表、64 个 tag 的根字段和条目形状，对照 1.19.2 `LootItemConditions`、`TagFile.CODEC` 与 `TagEntry.CODEC`；确认掉落条件已注册，空 tag 与省略 `replace` 均符合目标版本规则。引用目标闭包已于 2026-09-23 追加完成；游戏内数据包加载仍未验证。
- 2026-09-23：核对 280 个模型、97 个 blockstate 的本模组 parent/model/texture 引用闭包，均有对应资源；确认 `forge:fluid_container`、`tinker_foundry:connected`、`tinker_foundry:tank` 三类 loader 字段均符合静态读取结构，两个自定义几何 loader 有客户端事件注册。未进行客户端资源加载或渲染验证。
- 2026-09-23：跨版本核对 13 个矿石/原矿配方 ID、`metal` 标记、基础产物数量及四版实际熔炼匹配调用，确认原矿增产逻辑在 1.21.1 与 26.1.2 保留；补记各版本资源目录差异。未运行 build、GameTest 或 runClient。
- 2026-09-23：将 64 个目标 tag 的 184 个条目与模组注册集、精确 1.19.2 原版注册源码及服务端内置 tag 交叉核对；将 77 个掉落表名称与方块注册集、掉落物与物品注册集核对，未发现未解析引用。该检查不替代游戏内数据包加载。
