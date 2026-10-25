# Tinker Foundry 1.20.1 Forge — Parallel Task Plan（阶段 A 修订版）

> 主要执行者：Luna（极高推理）。审核者：主对话。  
> 当前实际只有一个实现者；下表保留未来拆分任务所需的所有权，但本项目当前按串行子阶段推进。  
> 当前状态：Gate A、Gate B、Gate D、Gate E、Gate F 核心资源静态审核均已通过（主对话审核日期：2026-09-21）；PC-1→PC-2→PC-3→PC-4、PD-1→PD-2→PD-3、PE-1→PE-2→PE-3→PE-4、PF-1→PF-3→PF-2、PG-1/T6.1 已按所有权串行完成静态审计。本轮不运行构建或客户端。

## 0. 使用规则

1. 所有具有读取依赖的任务按依赖链串行执行；不能再把“读前序输出”的任务描述为同阶段可并行。
2. 同一串行子阶段内若要并行，只允许任务既无相互读取依赖，又不共享文件；当前任务表没有把存在依赖的任务放在同一子阶段。
3. 任务只能写 `Owns` 中的文件；`Reads` 始终只读。两个 NeoForge 参考工作树和 1201-forge 模板永远只读。
4. 跨阶段需要继续修改同一文件时，必须写明“串行交接”；前一 owner 完成并交接后，后一 owner 才能编辑，不能重叠。
5. 领取任务时改为 ◐ 并写 owner；完成后先更新本计划和主计划，再交主对话审核。审核者不是业务文件 owner。
6. build、compile、Gradle validation、runClient 和游戏内测试必须等主对话重新获得用户明确授权。
7. Luna 运行慢不是阻塞条件，不催促、不降级模型；只有共享契约冲突、缺失外部支持或确实无法核实才暂停并汇报。

## 1. 共享契约（T0.5/Gate A 已通过）

### 已确认的构建/范围边界

- 模组 ID/命名空间候选：`tinker_foundry`。
- Java 基包候选：`org.hp.tinker_foundry`。
- 目标：Minecraft 1.20.1 Forge 47.4.23、Java 17、official mappings、Gradle wrapper 8.8。
- 构建骨架唯一来源：`E:\模组\模组模板\1201-forge`。
- 配方目标目录候选：`data/tinker_foundry/recipes/`。
- loot 目标目录已由同版本 JAR 静态证实为 `data/tinker_foundry/loot_tables/`，禁止写成 `loot_table/`。
- 当前产品范围不含 Tinkers' Construct 书、工具、材料词条、Modifier 和工具部件系统。

### T0.5 已冻结且已审核通过的契约

- 全部方块、物品、流体、方块实体、实体、菜单和创造标签注册名 manifest 位于 [`docs/1201-forge-contract-manifest.md`](1201-forge-contract-manifest.md)，包括每个动态生成 ID 的规则、来源文件/行、1211/2612 存在性、差异和 1201 策略；物品固定声明 54、方块变种 39、附加桶 17，共 110 个不重复 ID。
- 8 个自定义配方名：`entity_melting`、`melting`、`ore_melting`、`damageable_melting`、`alloying`、`casting`、`molding`、`fuel`；它们与普通 Minecraft 配方分开。
- `foundry_state`/`foundry_structure_error` 的字段/编码顺序、协议 `6`、长度边界和 `StructureErrorReason` ordinal 0..7。
- 方块实体 NBT 键、来源类型、缺省行为、数组边界和便携罐逻辑持久化语义；`FaucetState` 显式为 `OFF=0`、`POURING=1`、`POWERED=2`，缺失/越界按 0..2 钳制；1.20.1 具体 ItemStack NBT/能力 API 已由 PB-2 证据清单核实。
- 1.21.1 来源 `TFDataComponents.FLUID` 的语义与目标 `TFDataComponents.FLUID_TAG` 字符串键/1.20.1 可实现持久化语义分开；不得移植目标不存在的 component registry。
- book 的实际清单、引用、MIT 许可证据和“独立指南可选、图片最终发布待外部选择、匠魂书明确排除”结论。

## 2. 串行依赖图

```mermaid
flowchart LR
    PA1[PA-1 盘点] --> PA2[PA-2 契约证据]
    PA2 --> PA3[PA-3 计划/所有权]
    PA3 --> PA4[PA-4 T0.5 冻结审计]
    PA4 --> PB1[PB-1 Forge 骨架]
    PB1 --> PB2[PB-2 Forge API 证据]
    PB2 --> PB3[PB-3 确定性 Forge 适配]
    PB3 --> PC1[PC-1 注册骨架]
    PC1 --> PC2[PC-2 流体/持久化]
    PC2 --> PC3[PC-3 多方块]
    PC3 --> PC4[PC-4 调试命令]
    PC4 --> PD1[PD-1 配方 Java]
    PD1 --> PD2[PD-2 配方 JSON]
    PD2 --> PD3[PD-3 处理保护]
    PD3 --> PE1[PE-1 菜单]
    PE1 --> PE2[PE-2 网络]
    PE2 --> PE3[PE-3 客户端]
    PE3 --> PE4[PE-4 JEI]
    PE4 --> PF1[PF-1 视觉资源]
    PF1 --> PF3[PF-3 语言]
    PF3 --> PF2[PF-2 数据/book]
    PF2 --> PG1[PG-1 总静态审计]
    PG1 -.主对话授权.-> PH1[PH-1 构建]
    PH1 -.主对话授权.-> PH2[PH-2 启动/玩法]
```

依赖含义：每个箭头都是前置任务完成并交接后才可领取；因此 PC-2 不与 PC-1 并行，PD-2 不与 PD-1 并行，PE-4 不与 PE-1 并行，PF-3 不与 PF-1 并行。本轮主对话追加的确定性适配统一放在 PB-3 串行节点，避免把跨阶段读取依赖伪装成并行任务。

## 3. 任务详情

### Stage A · 盘点、证据与冻结门禁

下列任务均由 Luna 承担，主对话只作为独立审核者；PA-1 至 PA-4 的文档/静态证据已交付，主对话已于 2026-09-21 通过 Gate A。

**PA-1 · 版本、文件和 Git 盘点**  ☑ owner:Luna（已交付，Gate A 已审核）  
审核者：主对话。  
Owns：`docs/1201-forge-requirements.md` 的盘点和来源部分。  
Reads：三个项目、模板 README、模板构建文件。  
验收：分支、HEAD、dirty 数量、2612-only 文件和模板版本有绝对路径证据。  
映射：T0.1。

**PA-2 · API、资源与契约证据**  ☑ owner:Luna（已交付，Gate A 已审核）  
审核者：主对话。  
Owns：`docs/1201-forge-requirements.md` 的契约部分、`docs/1201-forge-port-design.md`。  
Reads：1211/2612 注册、网络、配方、资源、book、调试命令和本地 Forge JAR。  
验收：8 个自定义配方名、payload/NBT、data component 区分、`recipes/`、`loot_tables/`、tags 和 book 判断均区分事实与待验证项。  
映射：T0.2、T0.3。

**PA-3 · 主计划和所有权矩阵**  ☑ owner:Luna（已交付，Gate A 已审核）  
审核者：主对话。  
Owns：`docs/1201-forge-task-plan.md`、`docs/1201-forge-parallel-tasks.md`。  
Reads：三个计划模板、并行化规则、PA-1/PA-2 输出。  
验收：真实串行依赖、父子目录排除、跨阶段交接、Luna owner 和单列审核者均写清。  
映射：T0.4。

**PA-4 · 首次业务代码前契约冻结审计**  ☑ owner:Luna（T0.5 已完成，Gate A 已审核）  
审核者：主对话。  
Owns：上述四份文档以及 `docs/1201-forge-contract-manifest.md` 的契约清单和证据回写，不写 Java/Gradle 业务代码。  
Reads：PA-1 至 PA-3、本地 Forge 证据。  
验收：完整注册 manifest、110 个不重复物品 ID、payload/NBT/持久化、`FaucetState` 映射、已知资源路径和 book 纳入/排除结论齐全；Forge API 细签交由 PB-2 串行完成；主对话已确认 Gate A 并解锁 Stage B。  
映射：T0.5。

> Gate A：PA-4/T0.5 已完成，主对话已于 2026-09-21 审核通过；Stage B 已解锁。

### Stage B · Forge 骨架与 API 证据

**PB-1 · 重建 Forge 构建骨架**  ☑ owner:Luna（静态完成，Gate B 已通过）  
Owns：`build.gradle`、`gradle.properties`、`settings.gradle`、`gradle/wrapper/`、`src/main/resources/META-INF/mods.toml`、`src/main/resources/pack.mcmeta`，以及可选 JEI 开发依赖的声明。  
Reads：本机 `1201-forge` 模板、requirements/design。  
验收：只有 ForgeGradle/Forge 47.4.23/Java 17/official mappings；没有 NeoForge plugin、metadata 或 run 字段。旧 `src/main/templates/META-INF/neoforge.mods.toml` 已确认为无效 Forge 残留并删除；保留 Mixin 配置并改为 Java 17。JEI 依赖配置由本任务阶段修改，PE-4 不编辑 `build.gradle`。  
映射：T1.1、T1.2。

**PB-2 · Forge 47.4.23 API 证据清单**  ☑ owner:Luna（静态完成，Gate B 已通过）  
前置：PB-1。  
Owns：`docs/1201-forge-api-evidence.md`。  
Reads：模板依赖、Forge 47.4.23 本地缓存/JAR、同版本 Forge 项目；不写 NeoForge 目录。  
验收：注册、能力、流体、菜单、网络、渲染、命令、配方、JEI 各类都有精确版本、方法签名和本地 JAR/路径证据；资源目录和数据组件缺口已标为已验证/待验证。  
映射：T1.3。

**PB-3 · 确定性 Forge 适配、配方资源和 JEI 静态改写**  ☑ owner:Luna（静态完成，Gate B 已通过）  
前置：PB-2；本任务是主对话追加的串行交接，不与 PC-1 至 PE-4 并行。  
Owns：本轮实际修改的 Forge 适配 Java、`recipe/`、`registry/TFRecipes.java`、`compat/`、`src/main/resources/data/` 资源目录和 `docs/1201-forge-alloying-data.md`；不修改只读参考树。  
Reads：PB-2 API 证据、T0.5 manifest、Forge 47.4.23/JEI 1.20.1 本地 JAR、1211/2612 只读源码。  
验收：处理 `serverData`、`replaceProperties`、Forge capability/ItemStack NBT、SimpleChannel/FriendlyByteBuf、命令权限、1.20.1 Recipe serializer、`recipes/`/`loot_tables/`、ForgeTypes/RecipeManager 和 ore/damageable JEI 分类；不运行构建或客户端。完成后将各文件交还对应 PC/PD/PE 任务做静态闭合审计。  
映射：T1.4-T1.6。

> Gate B：PB-1→PB-2→PB-3 已按串行依赖完成静态交付，主对话已于 2026-09-21 审核通过；不执行 build/compile/Gradle validation/runClient/GameTest。Gate B 不等同于完整业务验收。

### Stage C · 注册、流体、结构、调试

**PC-1 · 注册与方块/物品/流体/实体/菜单骨架**  ☑ owner:Luna（静态完成，Gate C 已通过并交接 Stage D）  
前置：PB-3。  
Owns：`src/main/java/org/hp/tinker_foundry/TinkerFoundry.java`；`registry/` **但排除 `registry/TFRecipes.java`**；`block/` **但排除 `block/entity/`**；`entity/`；`item/` **但排除 `item/PortableTank*`**。  
Reads：PB-2 证据、PA-4 冻结 manifest。  
验收：所有 1211 注册名保留，2612-only 注册逐项有理由；不得写 PD-1 的配方注册文件。静态结果为方块 97、物品 110、方块状态 97、物品模型 110；同时修正 1.20.1 的 `ForgeFlowingFluid`、`BlockBehaviour.Properties.copy`、`GlassBlock` 和统一 `Block.use` 入口。
映射：T2.1。

**PC-2 · 流体、能力、容器和持久化**  ☑ owner:Luna（静态完成，Gate C 已通过并交接 Stage D）  
前置：PC-1。  
Owns：`common/`；`item/PortableTankItem.java`、`item/PortableTankFluidHandler.java` 及同一便携罐适配子文件；`block/entity/FoundryBlockEntity.java`。  
Reads：PC-1 注册、PB-2 API、PA-4 NBT/持久化契约。  
验收：simulate/execute、桶、物品栏容量、自动化输入输出和保存/读取状态一致。静态结果确认 Forge 方块实体 capability、`FluidStack` NBT、`load(CompoundTag)`/`saveAdditional(CompoundTag)`、1.20.1 tooltip 签名和 `ChunkStatus` 包路径；不注册 1.21 数据组件。
交接：完成后将 `FoundryBlockEntity.java` 的处理逻辑交给 PD-3；两者不同时编辑。  
映射：T2.2。

**PC-3 · 多方块结构与错误状态**  ☑ owner:Luna（静态完成，Gate C 已通过并交接 Stage D）  
前置：PC-2。  
Owns：`multiblock/`，以及明确列入结构验证的独立测试/资源文件；不拥有 PC-2 的方块实体文件。  
Reads：PC-1/PC-2 已交接的注册和领域状态、只读 1211/2612 结构代码。  
验收：冶炼炉、铸造炉、合金炉的边界、方向、燃料罐、设备登记和错误原因保持；静态 API 核查通过，`StructureErrorReason.fromId` 保留越界回退。
映射：T2.3。

**PC-4 · Forge 调试生成命令**  ☑ owner:Luna（静态完成，Gate C 已通过并交接 Stage D）  
前置：PC-3。  
Owns：`debug/`。  
Reads：PC-1 至 PC-3 的交接结果、调试命令契约。  
验收：四种命令类型、`CommandSourceStack.hasPermission(2)`、方向、结构、可操作设备和初始流体均基于 Forge API；使用 `RegisterCommandsEvent` 的 1.20.1 命令入口。
映射：T2.4。

> Gate C：PC-1→PC-2→PC-3→PC-4 已串行完成静态审阅，主对话已授权进入 Stage D；不运行客户端，构建/运行验证仍待用户明确授权。

### Stage D · 配方与熔炼业务

**PD-1 · 配方 Java type/serializer/type**  ☑ owner:Luna（静态完成，待 Gate D 审核）  
前置：PC-4。  
Owns：`recipe/`、`registry/TFRecipes.java`。这是对 PC-1 的**跨阶段串行接力**，PC-1 明确不拥有该文件。  
Reads：PB-2、PA-4 配方契约、PC-1 注册交接。  
验收：8 个自定义配方名和字段语义保留，所有 1.20.1 API 有证据；普通 Minecraft 配方不混入自定义注册表；药水组件已转换为 1.20.1 传统 NBT 语义。  
映射：T3.1。

**PD-2 · 配方 JSON 与数据路径**  ☑ owner:Luna（静态完成，待 Gate D 审核）  
前置：PD-1。  
Owns：`src/main/resources/data/tinker_foundry/recipes/` 及配方静态清单。  
Reads：PD-1、原 `data/tinker_foundry/recipe/` 只读副本、1.20.1 数据包规则。  
验收：旧 ID→新路径/字段一一映射；220 个 JSON 全部解析，8 类自定义配方共 117 个，普通 Minecraft 配方共 103 个；10 个药水流体结果使用 `tag.Potion`；无 alloying 数据臆造。  
映射：T3.2。

**PD-3 · 处理、容量和重复浇筑保护**  ☑ owner:Luna（静态完成，待 Gate D 审核）  
前置：PD-2。  
Owns：`block/entity/FoundryBlockEntity.java` 的处理逻辑后续变更；这是对 PC-2 的**串行接力**，不得并行编辑。  
Reads：PC-2 交接、PD-1/PD-2、浇筑和容量契约。  
验收：输出阻塞不扣流体，产物存在时不能重复浇筑；浇注口已有缓冲批次时不会覆盖，模拟/执行输出均限制在当前批次；合金/燃料/多流体状态未改变。  
映射：T3.3。

> Gate D：已通过（主对话审核日期：2026-09-21）。PD-1→PD-2→PD-3 的配方 Java、220 个 JSON、药水 NBT、输出阻塞和重复浇注保护已完成静态审计；不执行数据生成、构建或客户端。

### Stage E · 菜单、网络、客户端、JEI

**PE-1 · 菜单和动态 GUI 数据**  ☑ owner:Luna（静态完成，Gate E 已通过）  
前置：PD-3。  
Owns：`menu/`、`client/FoundryScreen.java`、菜单同步相关文件。  
Reads：PC-2、PD-3、冻结状态字段。  
验收：动态槽、容量、热量、输入流体和结构状态的服务端/客户端职责一致；补齐 `FoundryBlockEntity` 缺失的菜单/状态 getter，作为 PC-2→PD-3 文件的串行窄接口接力。  
映射：T4.1。

**PE-2 · Forge 网络适配**  ☑ owner:Luna（静态完成，Gate E 已通过）  
前置：PE-1。  
Owns：`network/`。  
Reads：PB-2、PE-1、payload 字段和协议契约。  
验收：两个逻辑 payload ID、协议 6、字段顺序和边界检查保留，底层为 Forge 网络 API；流体标签的显式列表/资源 ID、FluidStack NBT 大小、非负状态值均有对称边界。  
映射：T4.2。

**PE-3 · 客户端渲染和模型**  ☑ owner:Luna（静态完成，Gate E 已通过）  
前置：PE-2。  
Owns：`client/` 中除 `FoundryScreen.java` 外的客户端 Java 文件。  
Reads：PC-1 至 PC-3、PE-1/PE-2、Forge 客户端 API 证据。  
验收：液柱、连续液柱、罐容量、结构和设备渲染使用 1.20.1 API；修正 `VertexConsumer` 顶点写入链，不借用未审计的 2612 schema。  
映射：T4.3。

**PE-4 · JEI 适配**  ☑ owner:Luna（静态完成，Gate E 已通过）  
前置：PE-3。  
Owns：`compat/` **仅限 JEI Java 适配**。  
Reads：PD-1、PE-1、1.20.1 Forge JEI JAR、PB-1 已交接的可选依赖配置。  
验收：8 个自定义配方类型均有静态查看入口，`casting` 拆为盆/台两实例共 9 个分类注册；使用 Forge JEI 15.49.0.191 API、`ForgeTypes` 和 RecipeManager 列表，核心不硬依赖 JEI；不修改 `build.gradle`、`gradle.properties` 或其他 PB-1 文件；不新增 alloying JSON。  
映射：T4.4。

> Gate E：PE-1→PE-2→PE-3→PE-4 已串行完成静态审阅，主对话已于 2026-09-21 通过；已记录流体标签网络边界、药水 NBT 转换和合金催化匹配，画面、启动、JEI 展示和玩法验证待授权。

### Stage F · 视觉资源、语言、数据

**PF-1 · 材质、方块状态、模型、GUI**  ☑ owner:Luna（静态完成）  
前置：PE-4。  
Owns：`src/main/resources/assets/tinker_foundry/` **但排除 `lang/`**。  
Reads：PC-1、PE-3、目标版本资源 schema。  
验收：97 个 blockstates、110 个 item model、模型/纹理引用闭合；2612 `items/` 明确排除新版根 schema，便携容器使用 Forge `fluid_container`；不得编辑 PF-3 文件。证据：`docs/1201-forge-resource-audit.md`。  
映射：T5.1。

**PF-3 · 英文/中文语言**  ☑ owner:Luna（静态完成）  
前置：PF-1。  
Owns：`assets/tinker_foundry/lang/en_us.json`、`zh_cn.json`。这是与 PF-1 的**串行接力**，PF-1 明确排除 `lang/`。  
Reads：PC-1、PE-1、PF-1 资源 key 清单。  
验收：英文和中文各 297 个 key 且集合一致，Java 固定显示 key 无缺失，调试日志仍为英文；book 章节缺失 key 作为外部内容/许可未决项记录。  
映射：T5.3。

**PF-2 · 数据包、掉落、标签、书籍与结构资源**  ☑ owner:Luna（静态完成）  
前置：PF-3 且需读取 PD-2 的配方交接。  
Owns：`src/main/resources/data/` **但排除 `recipes/`**。目标掉落目录仅使用 `loot_tables/`。  
Reads：PC-3、PD-2、PA-2 的资源/book 证据和 PF-1/PF-3 交接。  
验收：57 个 tag JSON、77 个 loot table、2 个 damage type、7 个 book 章节和 2 个指南结构 NBT 通过静态审计；目录只使用 `loot_tables/`；book 按独立指南候选保留，匠魂书 API 明确排除，22 个章节翻译 key 和图片许可不臆造；无未经决定的删除。证据：`docs/1201-forge-resource-audit.md`。  
映射：T5.2。

> Gate F：核心资源静态审核已由主对话于 2026-09-21 通过；PF-1→PF-3→PF-2 的 JSON、资源引用、语言 key、book 内容和目录 schema 静态审计已交付。`alloying` 仍为 0 条数据，book 外部许可和 22 个缺失章节 key 不被伪装为完成。

### Stage G/H · 审核与授权验证

**PG-1 · 全项目静态审计与文档回写**  ☑ owner:Luna（静态完成，待主对话复核）  
前置：PF-2、PE-4。  
Owns：全项目只读审计结果、`docs/1201-forge-final-static-audit.md` 和阶段计划/证据文档的进度回写；不接管 Luna 已交接的业务文件 owner。  
Reads：Stage B-F 全部交付。  
验收：无实际 NeoForge/26.1.2 构建/API/资源残留；注册/资源/配方/网络/语言/命令清单闭合；待验证项有结论或明确阻塞；book 未决项不写成完成。  
映射：T6.1。

**PH-1 · 用户授权后的构建验证**  ☐ owner:Luna（需主对话授权）  
Owns：验证日志/报告，不默认修改文档之外的业务代码。  
前置：PG-1 且用户明确授权。  
映射：T6.2。

**PH-2 · 用户授权后的启动/玩法验证**  ☐ owner:Luna（需主对话授权）  
前置：PH-1 且用户明确授权。  
验证：GUI、配方、流体、桶、多方块、浇筑、JEI、调试命令；不得把启动成功冒充玩法成功。  
映射：T6.3。

## 4. 文件所有权矩阵与排除范围

| 文件/目录 | 独占任务 | 阶段/交接 | 排除范围或串行说明 |
| --- | --- | --- | --- |
| `build.gradle`、`gradle.properties`、`settings.gradle`、`gradle/wrapper/` | PB-1 | B | JEI 开发依赖也由 PB-1 修改；PE-4 不编辑构建文件 |
| 本轮确定性 Forge 适配的 Java/资源文件 | PB-3（一次性串行适配） | B→C/D/E 交接 | PB-3 完成后交还 PC/PD/PE 对应 owner；不得与这些后续任务并行编辑同一文件；只读参考树不在所有权内 |
| `src/main/resources/META-INF/mods.toml`、`pack.mcmeta` | PB-1 | B | 后续只读；改变需 Change Request |
| `TinkerFoundry.java` | PC-1 | C | 后续只读 |
| `registry/` | PC-1 | C | **排除 `registry/TFRecipes.java`**；该文件由 PD-1 在 D 阶段串行接管 |
| `block/` | PC-1 | C | **排除 `block/entity/`**；方块实体由 PC-2/PD-3 串行接力 |
| `entity/`、`item/` | PC-1 | C | PC-1 **排除 `item/PortableTank*`**；便携罐适配由 PC-2 接管 |
| `common/`、`item/PortableTank*` | PC-2 | C | PC-2 完成后只读；不得与 PD-3 同时编辑方块实体处理逻辑 |
| `block/entity/FoundryBlockEntity.java` | PC-2 → PD-3 → PE-1 窄接口接力 | C→D→E 串行接力 | PC-2 先完成保存/能力基础；PD-3 后续只改处理/重复浇筑逻辑；PE-1 只补菜单/网络状态所需的公开 getter 与 `menuData()`，不得改处理、能力和持久化；禁止并行编辑 |
| `multiblock/` | PC-3 | C | PC-2 交接后编辑，命令/客户端只读消费 |
| `debug/` | PC-4 | C | 只读消费结构/注册契约 |
| `recipe/` | PD-1 | D | PD-2 只读消费 Java serializer/type |
| `registry/TFRecipes.java` | PD-1 | C→D 串行接力 | PC-1 明确排除，PD-1 独占 |
| `data/tinker_foundry/recipes/` | PD-2 | D | PD-2 独占；PF-2 排除；普通/自定义 recipe 统一由 PD-2 逐项审计 |
| `menu/`、`client/FoundryScreen.java` | PE-1 | E | PE-1 不拥有 `block/entity/FoundryBlockEntity.java`；所需窄接口由上行文件 owner 在串行交接点完成；PE-2/PE-3/PE-4 只读消费 |
| `network/` | PE-2 | E | payload 契约改变需 Change Request |
| `client/`（不含 `FoundryScreen.java`） | PE-3 | E | 不拥有菜单文件；只读消费注册和同步状态；不编辑 PE-1 的 `FoundryScreen.java` |
| `compat/` | PE-4 | E | 仅 Java JEI 适配；**排除所有 Gradle/build 文件** |
| `assets/tinker_foundry/`（不含 `lang/`） | PF-1 | F | PF-1 明确排除语言目录 |
| `assets/tinker_foundry/lang/` | PF-3 | F 串行接 PF-1 | PF-3 读取 PF-1 的 key 清单，不与 PF-1 同时编辑 |
| `data/`（不含 `recipes/`） | PF-2 | F 串行接 PF-3 | PF-2 独占 `loot_tables/`、tags、book、结构数据；PD-2 的 recipes 排除 |
| `README.md`、`NOTICE.md` 与无语义过时 loader 注释 | PG-1/Luna | G | 仅允许清除与 1.20.1 Forge 目标冲突的版本、协议和 loader 说明；不得改业务逻辑；参考树不在所有权内 |
| `docs/1201-forge-final-static-audit.md` | PG-1/Luna | G | PG-1 独占新增；只记录静态证据、所有权、未决项和阶段状态 |
| 四份阶段 A 文档 | PA-1/PA-2/PA-3/PA-4；PG-1 串行回写 | A→G | 不允许整篇并发覆盖；每次回写需保留前序契约和审核状态 |
| `docs/1201-forge-contract-manifest.md` | PA-4；PG-1 串行回写 | A→G | PA-4 独占 T0.5 清单；Gate A 已通过，后续只允许串行审计回写，不与业务代码任务重叠编辑 |

## 5. Change Request

| 编号 | 日期 | 提出者 | 变更 | 处理 |
| --- | --- | --- | --- | --- |
| — | — | — | 当前无已批准变更 | 修改 mod_id、注册 ID、配方 ID、网络字段/协议、持久化语义、book 范围或文件所有权时，先新增一行并由主对话审核 |

## 6. 任务映射

| 并行任务 | 主计划 ID | 串行前置 |
| --- | --- | --- |
| PA-1 | T0.1 | — |
| PA-2 | T0.2-T0.3 | PA-1 |
| PA-3 | T0.4 | PA-2 |
| PA-4 | T0.5 | PA-3 |
| PB-1 | T1.1-T1.2 | PA-4 |
| PB-2 | T1.3 | PB-1 |
| PB-3 | T1.4-T1.6 | PB-2 |
| PC-1 | T2.1 | PB-3 |
| PC-2 | T2.2 | PC-1 |
| PC-3 | T2.3 | PC-2 |
| PC-4 | T2.4 | PC-3 |
| PD-1 | T3.1 | PC-4 |
| PD-2 | T3.2 | PD-1 |
| PD-3 | T3.3 | PD-2 |
| PE-1 | T4.1 | PD-3 |
| PE-2 | T4.2 | PE-1 |
| PE-3 | T4.3 | PE-2 |
| PE-4 | T4.4 | PE-3 |
| PF-1 | T5.1 | PE-4 |
| PF-3 | T5.3 | PF-1 |
| PF-2 | T5.2 | PF-3、PD-2 |
| PG-1 | T6.1 | PF-2、PE-4 |
| PH-1 | T6.2 | PG-1、用户授权 |
| PH-2 | T6.3 | PH-1、用户授权 |

## Revision Log

- 2026-09-21 — 主对话通过 Gate A、Gate B，并授权越过 PC-1→PC-4 静态交接；PD-1→PD-3 串行静态审计完成（当时记录为待审核，后续已通过 Gate D）。
- 2026-09-21 — 同步 PC-1→PC-4 的 Gate C 状态为已通过并已交接 Stage D，修正旧的“待 Gate C 审核”措辞。
- 2026-09-21 — 主对话通过 Gate F 核心资源静态审核；PG-1/T6.1 由 Luna 完成目标工作树 NeoForge/2612 残留、注册/资源/配方/网络/语言/命令/API/所有权总静态审计，新增最终静态审计文档；book 22 个翻译 key 和图片许可保持未决，未运行验证。
