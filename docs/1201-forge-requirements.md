# Tinker Foundry 1.20.1 Forge 迁移需求与冻结契约（阶段 A 修订版）

核对日期：2026-09-21  
阶段：A、B、D、E、F 核心资源静态审核已通过；PC-1→PC-4、PD-1→PD-3、PE-1→PE-4、PF-1→PF-3→PF-2 已完成静态审计；PG-1/T6.1 已完成，Gate G 的构建/运行验证仍未授权  
目标分支：1201forge  
目标工作树：`E:\模组\发cf的模组\Tinker Foundry\1.20.1-forge-tinker-foundry`

本版记录 Gate A、Gate B、Gate D、Gate E 以及 Gate F 的核心资源静态审核均已通过（主对话审核日期：2026-09-21）。依据是 1211/2612 的完整注册清单、110 个不重复物品 ID、两个网络载荷契约、NBT/便携罐语义、FaucetState 显式映射、资源路径证据以及 PB-1/PB-2/PB-3、PD-1→PD-3、PE-1→PE-4、PF-1→PF-3→PF-2 静态交付已独立核对；PG-1/T6.1 总静态审计已完成。book 图片许可和 22 个章节翻译 key 仍待外部内容决策，不纳入最终完成声明；运行验证未执行。

## 1. 范围与不可违反的边界

本阶段只做静态盘点、版本差异核对、公开契约核对和后续任务拆分。唯一允许写入的工作树是目标工作树；以下项目均为只读参考：

| 角色 | 绝对路径 | 静态状态与用途 |
| --- | --- | --- |
| 1.21.1 NeoForge 主项目 | `E:\模组\发cf的模组\Tinker Foundry\Tinker Foundry` | master，HEAD `1102c56`；行为与完整功能基线 |
| 26.1.2 NeoForge 工作树 | `E:\模组\发cf的模组\Tinker Foundry\26.1.2-neoforge-tinker-foundry` | `2612neoforge`，HEAD 同为 `1102c56`，工作树脏；仅作修复/新增候选来源 |
| 1.20.1 Forge 官方模板 | `E:\模组\模组模板\1201-forge` | ForgeGradle、Forge `47.4.23`、Java 17、official mappings；唯一构建骨架来源 |
| 模板说明 | `E:\模组\模组模板\README.md` | 工作树与模板约定 |
| 1.20.1 目标 | `E:\模组\发cf的模组\Tinker Foundry\1.20.1-forge-tinker-foundry` | `1201forge`；构建、元数据、源码适配和资源 schema 已按 Forge 静态闭合，尚未构建或运行验证 |

不得修改、清理、切换、提交、回退或覆盖两个只读参考目录。不得把 2612 的未提交修改视为稳定 API；每个 2612-only 文件都必须记录纳入、改写或排除理由。

## 2. 功能保留清单

| 功能面 | 1211 master | 2612 dirty worktree | 1.20.1 要求 |
| --- | --- | --- | --- |
| 主模组与注册 | `TinkerFoundry` 及方块、物品、流体、方块实体、实体、菜单、配方、创造标签注册 | 保留并改用 2612 候选写法 | 全部保留；公开 ID 已逐项写入 T0.5 manifest，Gate A 已审核 |
| 方块与方块实体 | `block/` 及 `block/entity/FoundryBlockEntity.java` | 同名实现有大量修改 | 保留冶炼炉、铸造炉、合金炉、熔化炉、加热器、罐、排液/输送/浇注设备及状态 |
| 物品与便携容器 | `PortableTankItem`、流体处理器、桶/铸件/容器类；固定声明 54、方块变种 39、附加流体桶 17，合计 110 个物品 ID | 新增资源处理和能力兼容候选 | 保留全部 110 个 ID、容量、桶交互、物品栏容量显示、损坏后流体保留；1.20.1 能力 API 由 PB-2 核验 |
| 多方块结构 | `FoundryMultiblock`、`SmelteryMultiblock`、矩形检测、错误原因/结果/标签 | 结构刷新和调试生成候选 | 保留冶炼炉、铸造炉、合金炉合法性、尺寸、燃料罐、控制器和设备登记 |
| 流体与容器交互 | 结构罐、通道/端口处理器、物品处理器、容器转移 | 能力兼容、液柱客户端逻辑候选 | 保留 Forge 流体能力、桶和其他容器双向交互、自动化输入输出、连续液柱和排液 |
| 熔炼与实体处理 | `melting`、`ore_melting`、`damageable_melting`、`entity_melting` | 配方序列化/同步候选 | 保留四类自定义配方及 JSON 数据 |
| 合金 | `alloying` | 同名实现修改 | 保留多输入流体、温度、输出和 JEI 展示 |
| 浇筑与模具 | `casting`、`molding`、浇注盆/台、模具 | 同名实现修改 | 保留浇筑、砂模、产物占用检查；产物存在后禁止重复浇筑 |
| 燃料 | `fuel` | 温度、速率和消耗链路修改 | 保留燃料温度、加热速率、容量和冷却行为 |
| 菜单、GUI、网络 | `FoundryMenu`、`FoundryScreen`、两个逻辑载荷 | 配方同步和 GUI/流体渲染候选 | 保留槽位、容量、热量、配方查看和同步；底层改用 Forge |
| JEI | `FoundryJeiPlugin` | 插件与同步候选 | 保留每种自定义配方的查看；精确 Forge JEI 版本待核实 |
| 材质、模型、方块状态 | `assets/tinker_foundry/` | `items/` 等资源 schema 修改候选；便携桶/罐模型已改为 Forge `fluid_container` loader | 视觉资源全部盘点；不能直接复制 2612 item model schema |
| 数据包资源 | 来源含单数 `recipe/`、`loot_table/`、tags、damage_type、book | 目标已迁移到 `recipes/`、`loot_tables/`；旧目录仅可能保留空目录，不计资源文件 | 继续按 1.20.1 schema 审计配方、tags、damage_type、book |
| 汉化 | `en_us.json`、`zh_cn.json` | 两份语言文件有修改 | 英文默认、中文覆盖；显示文本不得直接使用内部 ID |
| 调试指令 | 1211 未发现 | `FoundryDebugCommand` 提供 `/tinker_foundry debug generate <smeltery|foundry|alloyer|all>` | 必须纳入 Forge 迁移，生成可操作结构、设备、燃料和流体 |
| GameTest | 1211 有 `FoundryGameTests.java` | 2612 删除 | 不因迁移方便而删除测试意图；Forge 形式待 API 核对，当前不运行 |

## 3. 来源矩阵

| 项目 | 静态证据 | 结论 |
| --- | --- | --- |
| 1211 master | 两个参考目录的 `git status --short --branch`、`git log -1` | `1102c56` 是完整功能行为基线；未跟踪交接文档不算 Java/资源基线 |
| 2612 dirty | `git status --short` 统计为 245 modified、23 deleted、10 untracked | 最新修复/新增候选；不能整体复制 |
| 1201forge | `git status --short --branch`，基线同为 `1102c56` | 隔离目标，但当前内容仍为 NeoForge 1.21.1 |
| 1201 Forge 模板 | `1201-forge/build.gradle`、`gradle.properties`、`settings.gradle`、wrapper | 后续构建骨架唯一来源：Forge `47.4.23`、Java 17、official mappings、Gradle 8.8 |

### 3.1 2612-only 文件与行为

已从 2612 清单确认的候选文件包括：`client/FoundryFluidClient.java`、`client/FoundryRecipeSyncClient.java`、`client/model/FoundryTankItemModel.java`、`common/FoundryCapabilityCompat.java`、`debug/FoundryDebugCommand.java`、`item/PortableTankResourceHandler.java`、`recipe/FoundryRecipe.java`。同时删除了 `gametest/FoundryGameTests.java`、`recipe/FoundryRecipeSerializer.java`，并修改/删除若干物品模型。它们都不是“直接拷贝项”；纳入判断放在对应迁移任务和 T0.5 契约审计中。

## 4. 公开契约冻结状态（T0.5/Gate A 已通过）

### 4.1 命名空间、入口和已核实注册名

完整逐项清单位于 [`docs/1201-forge-contract-manifest.md`](1201-forge-contract-manifest.md)。该清单从 1211/2612 的全部 `TF*.java` 注册调用和动态生成规则生成，逐项记录来源文件/行、两版本存在性、变化和 1201 纳入策略；本节只保留审核摘要。

| 类别 | 当前核实注册名/入口 | 来源 | 状态 |
| --- | --- | --- | --- |
| 模组/数据命名空间 | `tinker_foundry` | `TinkerFoundry.java` | 已写入 manifest，Gate A 已审核 |
| Java 基包/主类 | `org.hp.tinker_foundry` / `org.hp.tinker_foundry.TinkerFoundry` | 1211 master | 已核实；API 迁移仍由 PB-2 负责 |
| 创造标签 | `foundry` | 注册入口 | 已写入 manifest，Gate A 已审核 |
| 方块实体 | `foundry_device` | `registry/TFBlockEntities.java` | 已写入 manifest，Gate A 已审核 |
| 实体 | `fluid_spit` | `registry/TFEntities.java` | 已写入 manifest，Gate A 已审核 |
| 物品 | `copper_canister` | `registry/TFItems.java` | 已写入 manifest，Gate A 已审核 |
| 自定义配方 type/serializer | `entity_melting`、`melting`、`ore_melting`、`damageable_melting`、`alloying`、`casting`、`molding`、`fuel` | `registry/TFRecipes.java` | 8 个名称和来源行已写入 manifest；Forge API 由 PB-2 核验 |
| 数据组件来源入口 | `TFDataComponents.FLUID`，注册名 `fluid` | 1211/2612 `registry/TFDataComponents.java` | 仅为来源语义证据；1.20.1 目标不注册该表，目标内部只保留 `TFDataComponents.FLUID_TAG = "Fluid"` 字符串键 |
| 其他方块/物品/流体/菜单 | 全部 `TF*.java` 的 `.register(...)` 名称及动态生成规则 | 1211 master、2612 dirty 对照 | 已逐项写入 manifest，Gate A 已审核 |

### 4.2 配方、路径和 8 个明确的自定义注册名

八个名称是自定义配方 type/serializer，不是“七类再加 fuel”的模糊表述：

1. `entity_melting`
2. `melting`
3. `ore_melting`
4. `damageable_melting`
5. `alloying`
6. `casting`
7. `molding`
8. `fuel`

它们与普通 Minecraft 配方分开统计。普通配方包括目标版本原版 `crafting`、`smelting`、`blasting`、`smoking`、`campfire_cooking`、`stonecutting` 等已有 type；若本项目数据使用它们，只迁移到 1.20.1 的普通 `recipes/` JSON schema，不新增或冒充 Tinker Foundry 自定义 serializer。逻辑配方 ID（例如 `casting/copper_ingot`、`melting/iron_ingot`）不因目录从 `recipe/` 复数化为 `recipes/` 而改变。

### 4.3 网络载荷顺序与协议契约

当前来源的两个逻辑载荷 ID 是 `tinker_foundry:foundry_state`、`tinker_foundry:foundry_structure_error`，协议字符串为 `6`。字段/编码顺序、边界和错误枚举已在 manifest 冻结为本模组逻辑契约，1.20.1 的承载 API 仍由 PB-2 逐项核验：

- `FoundryStatePayload` 字段顺序：`BlockPos pos`、`FluidStack fluid`、`FluidStack fuelFluid`、`List<FluidStack> alloyInputs`、`List<FluidStack> structureFluids`、`int progress`、`int processTime`、`int castingFluidCapacity`、`int structureCapacity`、`boolean structureValid`、`int[] heat`。
- 当前写入顺序：位置；主流体存在标记及流体；燃料流体存在标记及流体；合金列表长度及元素；结构流体列表长度及元素；进度、总时间、浇注容量、结构容量；合法性；热量数组长度及元素。
- Forge 1.20.1 网络边界：显式流体列表最多 128 项，资源位置字符串最多 256；存在的 `FluidStack` 数量必须为正，附加 NBT 不超过 16 KiB；进度、时间、容量和热量值不得为负。该边界只保护当前协议，不承诺与其他 Minecraft 版本字节互通。
- `FoundryStructureErrorPayload` 字段顺序：`BlockPos controllerPos`、可选 `BlockPos errorPos`、`StructureErrorReason reason`；当前写入为控制器位置、错误位置存在标记/位置、`reason.ordinal()`。
- `reason.ordinal()` 是当前实现的编码顺序；manifest 已列出 0..7 映射。1.20.1 保留逻辑映射即可，不承诺跨 Minecraft 版本旧字节流互通。

### 4.4 方块实体 NBT 契约摘要

来源：1211 master `block/entity/FoundryBlockEntity.java` 的 `saveAdditional/loadAdditional`。这些键名和字段含义须保留；1.20.1 的 `ItemStack`、`FluidStack` 保存/读取调用必须按目标 API 重写，不能把 1.21.1 `HolderLookup.Provider` 签名照搬。

| 分组 | 键名 |
| --- | --- |
| 物品/输入 | `Inputs`（条目含 `Slot`）、`InputSize`、`Output`、`Remainder`、`Fuel`、`ProxyItem`、`CannonItem` |
| 主流体/燃料 | `Fluid`、`CastingFluidCapacity`、`StructureFluids`、`FuelFluid` |
| 处理进度 | `Progress`、`ProcessTime`、`InputProgress`、`BurnTime`、`FuelTemperature`、`FuelHeatingRate`、`FuelFluidConsumption`、`FuelFluidConsumed`、`FuelBurnDuration` |
| 结构/容量 | `StructureFuelRate`、`StructureCapacity`、`StructureValid`、`StructureInteriorBlocks`、`StructureMin`、`StructureMax`、`Controller` |
| 同步/浇注 | `TransferCooldown`、`ChannelFlowing`、`FaucetState`、`FaucetStop`、`FaucetRedstone`、`FaucetRenderFluid` |
| 合金 | `AlloyInputs`（条目含 `Slot`、`Fluid`） |

manifest 已逐项列出 `StructureFluids` 的有序层语义、空值/缺省键行为、数组边界、`FaucetState` 枚举顺序和迁移含义。1.20.1 选择 Forge ItemStack NBT/能力实现时只保留这些逻辑语义，不承诺读取 1.21.1 component 或 2612 ValueInput/ValueOutput 存档格式。

`FaucetState` 已核实为两版本一致的显式映射：`OFF=0`、`POURING=1`、`POWERED=2`。1211 来源为 `FoundryBlockEntity.java:293-300`，2612 来源为 `FoundryBlockEntity.java:295-302`；读取时缺失值默认为 0，负值钳制为 0，超过 2 钳制为 2。完整表和读取行号见 [`docs/1201-forge-contract-manifest.md`](1201-forge-contract-manifest.md) §7.1。

### 4.5 来源 `TFDataComponents.FLUID` 与目标 `FLUID_TAG` 持久化语义必须分开

1211 当前源码 `registry/TFDataComponents.java` 使用 `Registries.DATA_COMPONENT_TYPE` 注册 `fluid`，类型为 `DataComponentType<CustomData>`，并以 `CustomData.CODEC` 持久化。`item/PortableTankFluidHandler.java` 通过该组件读取/删除/设置 `FluidStack.CODEC` 编码后的流体。

本地 1.20.1 Forge `47.4.23` 映射 JAR `C:\Users\30819\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1-recomp.jar` 的类路径静态检查未发现 1.21.1 使用的 `net/minecraft/core/component/` 与 `net/minecraft/world/item/component/` 数据组件包。因此：

- **不能**为了保留类名而移植目标不存在的 `Registries.DATA_COMPONENT_TYPE` 注册表或来源 `TFDataComponents.FLUID` 的 1.21.1 实现。
- 目标树的 `TFDataComponents` 不是注册表入口，只提供 `FLUID_TAG = "Fluid"` 字符串键；这与保留便携罐持久化语义是两件事。
- 必须保留的是便携罐物品的持久化语义：流体类型、数量、容量限制、填充/排出模拟与实际操作、空罐清理、损坏后流体保留和物品栏容量显示。目标实现应使用 1.20.1 Forge 能力/物品 NBT 机制；具体方法名属于 PB-2，不在 T0.5 预写目标 API。

## 5. 数据目录与资源路径证据

本地 1.20.1 Forge JAR 静态清单：`C:\Users\30819\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1-recomp.jar`。`jar tf` 结果包含 `data/minecraft/loot_tables/`、`data/minecraft/recipes/` 和 `data/forge/tags/`，没有 `data/minecraft/loot_table/`。因此目标路径候选为：

- `src/main/resources/data/tinker_foundry/recipes/`
- `src/main/resources/data/tinker_foundry/loot_tables/`（复数，已由同版本 JAR 证实；不得写成 `loot_table/`）
- `src/main/resources/data/tinker_foundry/tags/`，其下再按 `blocks/`、`items/`、`fluids/` 等实际类型核对
- `src/main/resources/data/tinker_foundry/damage_type/`：名称和 1.20.1 支持情况仍需单独核对，不能仅由目录名推断
- `src/main/resources/assets/tinker_foundry/`

当前 1211/2612 的 `data/tinker_foundry/recipe/` 单数目录只说明来源布局，不能作为目标路径证据；目标 220 个配方文件已位于 `recipes/`，77 个掉落表已位于 `loot_tables/`。每份 JSON 仍须按 1.20.1 schema 静态映射。

## 6. book 目录的范围判断

不能因为目录名 `book` 就删除，也不能把它当作必须纳入的匠魂书。静态核对 1211 master 得到：

- `assets/tinker_foundry/book/` 含 `alloyer.png`、`melter.png`、`foundrygui.png`、`smeltergui.png`、`faucet_pour.png` 等本项目熔炼/浇筑图片，并含 `images/covers/puny_smelting_pages.png`、中文图片、`structures/foundry.nbt`、`structures/smeltery.nbt` 和 `UPSTREAM-LICENSE.txt`。
- `data/tinker_foundry/book/chapters.json` 引用 `alloying.json`、`casting.json`、`foundry.json`、`fuel.json`、`intro.json`、`melter.json`、`smeltery.json` 七个章节。
- `assets/tinker_foundry/lang/en_us.json` 与 `zh_cn.json` 有 `book.tinker_foundry.*` 键，英文索引标题为 `Standalone Foundry Guide`，中文为“独立冶炼指南”；静态引用搜索没有发现 Java 端 Tinkers' Construct 书注册。

阶段性决定：

1. **核心骨架不依赖 book，且不阻塞 Stage B**。本项目独立指南的 JSON、章节、语言和结构资源保留为 Stage F 可选候选。
2. **最终发布范围待外部选择**：`UPSTREAM-LICENSE.txt` 只有 MIT 条款和版权信息，没有逐张图片的来源映射、版本号或外部 URL；在外部选择前不把 book 图片宣布为已批准发布内容。
3. **明确排除**：不迁移 Tinkers' Construct 书 API、书注册、工具、材料词条、Modifier、工具部件或相关章节；这与产品范围“不含匠魂书”一致。

## 7. 明确非目标

- 阶段 A 的冻结门禁不迁移 Java/Gradle 业务代码；本版后续变更记录的是 Stage B 之后明确授权的 Forge 适配批次，不修改只读参考目录。
- 不运行 build、compile、任何 Gradle validation、runClient 或 GameTest；验证须等主对话重新明确授权。
- 不提交、不推送、不上传 CurseForge，不修改 GitHub Wiki。
- 不混用 1.21.1 NeoForge、26.1.2 NeoForge API 或构建配置。
- 不通过删除 GUI、JEI、流体交互、多方块、资源或调试命令来换取编译通过。
- 本模组仍是独立熔炼与浇筑系统，不新增匠魂工具、材料词条、Modifier 或工具部件系统。

## 8. 阶段状态与门禁

当前状态：**Gate A、Gate B、Gate D、Gate E、Gate F 核心资源静态审核已通过（2026-09-21）；PC-1→PC-4、PD-1→PD-3、PE-1→PE-4、PF-1→PF-3→PF-2 已完成静态审计；PG-1/T6.1 已完成**。本轮及前序批次补齐 Forge 构建骨架、Forge 元数据、可选 JEI 精确依赖、API 证据、注册/能力/持久化/结构/调试命令、8 类配方、220 个配方 JSON、浇筑输出保护、菜单状态同步、网络边界、1.20.1 客户端顶点写入、JEI API 和资源 schema 的静态闭合；没有运行验证。

T0.5 交付范围已经完成：

1. 1211 master 与 2612 dirty 的完整注册 manifest 已逐项记录来源文件/行、两版本存在性、实现变化和 1201 纳入策略；物品已核实为 54 个固定声明 + 39 个方块变种 + 17 个附加桶，共 110 个不重复 ID。
2. 两个网络载荷的协议 `6`、ID、字段/编码顺序、长度边界和 `StructureErrorReason` ordinal 0..7 已冻结为逻辑契约；跨 Minecraft 版本网络不保证互通。
3. 方块实体 NBT 键、类型、缺省行为、容器差异和便携罐“组件来源语义/1.20.1 Forge ItemStack NBT 或能力实现”已分开记录；`FaucetState` 的 `OFF=0`、`POURING=1`、`POWERED=2` 及越界钳制已显式记录；不伪造 1.20.1 数据组件注册表。
4. 1.20.1 `recipes/`、`loot_tables/` 和 tags 的已知路径证据已记录；book 已明确为不阻塞骨架的独立指南候选，图片最终发布待外部许可选择，匠魂书明确排除。
5. Forge 方法、事件、构造器、配方和 JEI API 的细签已写入 [`1201-forge-api-evidence.md`](1201-forge-api-evidence.md)；目标代码已按已验证入口处理 `SimpleChannel/FriendlyByteBuf`、Forge capability、1.20.1 Recipe serializer、`recipes/`/`loot_tables/`、ForgeTypes 和 RecipeManager 列表，但其适用范围、资源闭合性和运行结果仍分开标注。

6. 本轮确定性改写的边界：`build.gradle` 使用 Forge 运行类型 `serverData`，并在 `processResources` 中展开 `jei_version_range`；PortableTank 使用 ItemStack NBT 而不是不存在的 1.21 数据组件注册表；方块能力通过方块实体查询，不能调用 1.20.1 Forge 不存在的 `Level.getCapability(Capability, BlockPos, Direction)`；命令使用 `CommandSourceStack.hasPermission`；配方使用 `Recipe`/`RecipeSerializer`/`FriendlyByteBuf`；JEI 使用 `ForgeTypes` 和 `RecipeManager.getAllRecipesFor`；`alloying` 无 1211/2612/目标配方 JSON 时只记录清单，不臆造数据。

Stage B 已通过 Gate B，PC-1→PC-4 已完成静态交接；PD-1→PD-3 已完成配方 Java、JSON 和处理保护静态审计并通过 Gate D；PE-1→PE-4 已串行完成 Stage E 静态审计并通过 Gate E；PF-1→PF-3→PF-2 已串行完成 Stage F 核心资源静态审计并通过 Gate F；PG-1/T6.1 已完成总静态审计。构建、客户端、JEI 展示、资源加载和 GameTest 验证仍待用户重新授权；book 22 个翻译 key 和图片许可不纳入最终完成声明。

## Revision Log

- 2026-09-21 — 主对话审核通过 Gate A、Gate B，并授权越过 PC-1→PC-4 静态交接；完成 PD-1→PD-3 串行配方与处理保护静态审计（当时记录为待审核，后续已通过 Gate D），未运行验证。
- 2026-09-21 — 主对话通过 Gate D、Gate E 和 Gate F 核心资源静态审核；完成 PE-1→PE-2→PE-3→PE-4 串行静态适配，补齐菜单状态 getter、Forge 网络流体边界、1.20.1 顶点写入链和 JEI 15.49.0.191 方法调用，记录药水 NBT 转换与合金催化匹配；完成 PF-1→PF-3→PF-2 资源 schema、语言、tags、loot、book 和结构静态审计，未运行验证。
- 2026-09-21 — 主对话通过 Gate F 核心资源静态审核；PG-1/T6.1 完成目标 Forge/NeoForge 残留、注册/资源/配方/网络/语言/命令/API/所有权总静态审计。book 的 22 个翻译 key 和图片许可仍未决，`alloying` 仍为 0 条来源数据；未运行验证。
