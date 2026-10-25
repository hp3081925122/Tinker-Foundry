# Tinker Foundry 1.20.1 Forge — Master Task Plan（阶段 A 修订版）

> 来源：`1201-forge-requirements.md`、`1201-forge-port-design.md`。  
> 主要执行者：Luna（极高推理）；审核者：主对话。  
> 当前状态：Gate A、Gate B、Gate D、Gate E、Gate F 核心资源静态审核已通过（主对话审核日期：2026-09-21）；PC-1→PC-2→PC-3→PC-4、PD-1→PD-2→PD-3、PE-1→PE-2→PE-3→PE-4、PF-1→PF-3→PF-2、PG-1/T6.1 已完成静态审计。本计划按真实串行子阶段推进。未执行 build、compile、Gradle validation、runClient 或 GameTest。  
> 状态：☐ 待开始 · ◐ 进行中/待审核 · ☑ 已核实 · ⛔ 阻塞；不写时间估计。

## 里程碑

| 里程碑 | 可验证交付物 | 阶段状态 |
| --- | --- | --- |
| M0 | 来源、功能、公开 ID、payload/NBT、数据组件语义和资源证据完成冻结 | Gate A 已通过（2026-09-21） |
| M1 | 目标工作树完全采用 1201-forge ForgeGradle 骨架，元数据/API 证据静态一致 | PB-1/PB-2/PB-3 已完成，Gate B 已通过（2026-09-21） |
| M2 | 注册、领域状态、流体能力、容器、多方块、调试命令、配方和处理完成 | PC-1→PC-4、PD-1→PD-3 已完成静态审计，Gate D 已通过 |
| M3 | 菜单、网络、JEI、客户端、资源、材质、模型和汉化完成 | PE-1→PE-4 已完成菜单、网络边界、客户端 1.20.1 API 和 JEI 静态适配；资源/运行验证仍待后续阶段或授权 |
| M4 | 静态审计完成；用户授权后分别执行构建、启动和玩法验收 | PG-1/T6.1 静态完成；T6.2/T6.3 待授权 |

## Stage A0 · 盘点证据

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T0.1 | 核对三项目、模板、Git 状态和文件清单 | M | — | 记录绝对路径、分支、HEAD、dirty 数量、2612-only 清单和模板版本 | `docs/1201-forge-requirements.md` |

## Stage A1 · 契约与版本证据（串行接在 A0 后）

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T0.2 | 盘点功能、来源矩阵、候选注册名和公开数据契约 | L | T0.1 | 八个自定义配方名、payload 字段顺序、NBT 键和数据组件/持久化差异均逐项记录；完整逐项清单交由 T0.5 manifest 汇总 | `docs/1201-forge-requirements.md`、`docs/1201-forge-contract-manifest.md` |
| ☑ T0.3 | 核对版本 API 边界、依赖、包/资源布局、风险和静态证据 | L | T0.2 | 已知的 1.20.1 Forge 与 NeoForge 差异、`recipes/`、`loot_tables/`、tags、book、Forge JAR 资源路径证据可追溯；方法级 API 细签留给 PB-2 | `docs/1201-forge-port-design.md` |

## Stage A2 · 计划与契约冻结门禁（串行接在 A1 后）

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T0.4 | 建立真实串行任务、依赖图和无冲突所有权 | M | T0.2、T0.3 | 不再声称同阶段任务可并行；父子目录有排除范围或串行交接；PA owner 为 Luna，审核者单列 | `docs/1201-forge-task-plan.md`、`docs/1201-forge-parallel-tasks.md` |
| ☑ T0.5 | 首次业务代码前的公开契约冻结审计（Gate A 已通过） | L | T0.4 | `docs/1201-forge-contract-manifest.md` 逐项记录全部注册 ID/动态规则、1211/2612 差异和 1201 策略；物品核实为 54+39+17=110 个不重复 ID；冻结 payload/NBT/`FaucetState`（`OFF=0`、`POURING=1`、`POWERED=2`，缺失/越界钳制）顺序、便携罐逻辑持久化、已知资源路径和 book 范围；主对话于 2026-09-21 通过 Gate A | `docs/1201-forge-contract-manifest.md`、四份阶段 A 文档 |

**Gate A：已通过（2026-09-21）。** 主对话独立核对了 1211 TFBlocks 静态 77、TFFluids `EXTRA_COLORS` 17+固定 3、TFItems 固定 54+动态 39+附加桶 17=110 且无重复，并复查了 `FaucetState` `OFF=0`、`POURING=1`、`POWERED=2` 与读取钳制源码。Stage B 已解锁；后续用户明确要求继续处理确定的 Forge 版本缺口，当前验证门仍只允许静态文件、文本、JSON、JAR 清单和 Git 状态检查。

## Stage B-1 · Forge 构建骨架

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T1.1 | 按精确模板重建 Forge 构建骨架和 metadata | M | T0.5 | 已按 `E:\模组\模组模板\1201-forge` 对应复制 ForgeGradle、Forge 47.4.23、Java 17、official mappings、Gradle 8.8、`mods.toml`、`pack.mcmeta`；无 NeoForge 构建插件/元数据残留；旧 `src/main/templates/META-INF/neoforge.mods.toml` 已删除并记录原因 | 根构建文件、`gradle/`、`META-INF/mods.toml`、`pack.mcmeta` |

## Stage B-2 · Forge 源码/资源骨架

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T1.2 | 建立 1.20.1 Forge 源码与资源目录骨架 | S | T1.1 | `mod_id`、包名、source set、`pack.mcmeta`、Forge `mods.toml` 和 Mixin Java 17 元数据已静态对齐；业务 NeoForge Java/资源未冒险改写 | `src/main/java/`、`src/main/resources/` |

## Stage B-3 · API 证据

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T1.3 | 建立 Forge 47.4.23 API 证据清单 | L | T1.2 | 注册、能力、流体、菜单、网络、渲染、命令、配方、JEI 每类均有精确本地 JAR/路径/方法签名证据，并标出待验证边界 | `docs/1201-forge-api-evidence.md` |

## Stage B-4 · 确定性 Forge 迁移适配批次（串行接在 B-3 后）

本批次由主对话明确要求继续执行，属于跨原 Stage C/D/E 的确定性 API/资源适配；它不把未核实的运行结果标记为通过，也不跳过后续分阶段静态审计。

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T1.4 | Forge 启动、能力、网络、命令适配 | L | T1.3 | `serverData` 对齐本地日志可用 run type；`processResources` 展开 `jei_version_range`；物品使用 ItemStack NBT、方块实体使用 Forge capability 与 1.20.1 `load/saveAdditional`；网络使用 `SimpleChannel/FriendlyByteBuf`；命令使用 `hasPermission`；所有位置能力查询通过方块实体而非不存在的 `Level` 三参入口 | `build.gradle`、`network/`、`common/`、`item/`、`block/`、`block/entity/`、`menu/`、`debug/` |
| ☑ T1.5 | 配方、资源目录和 JEI 适配 | L | T1.4 | 8 个自定义配方迁移到 `Recipe`/`RecipeSerializer`/`FriendlyByteBuf`；资源目录为 `recipes/`、`loot_tables/`；普通 Minecraft 配方结果字段符合 1.20.1；合金清单明确无源数据时不臆造；JEI 使用 `ForgeTypes`、RecipeManager 列表并补齐 `ore_melting`/`damageable_melting` | `recipe/`、`registry/TFRecipes.java`、`src/main/resources/data/`、`compat/`、`docs/1201-forge-alloying-data.md` |
| ☑ T1.6 | 客户端 Forge API 静态适配 | L | T1.5 | 菜单注册、流体类型客户端扩展、模型 bake 签名、颜色/渲染事件和 Forge ModList 使用目标 JAR 已验证入口；不宣称已运行或已完成所有资源 schema | `client/`、`registry/TFFluids.java`、`docs/1201-forge-api-evidence.md` |

**T1.4-T1.6 静态结果：** 已写入目标工作树并由主对话纳入 Gate B 审核。未执行构建、Gradle validation、客户端或 GameTest；T2.x-T5.x 的完整验收仍按原串行链执行。

**Gate B：已通过（主对话审核日期：2026-09-21）。** 主对话确认 PB-1/PB-2/PB-3 的 Forge 骨架、API 证据和确定性适配静态一致；构建验证仍待用户重新授权，Gate B 不等同于业务运行通过。

## Stage C-1 · 注册与骨架

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T2.1 | 迁移主类、注册表、方块、物品、流体、方块实体、实体、菜单、标签 | L | T1.6 | 静态闭合：方块 77+动态流体方块 20=97，物品固定 54+方块变种 39+附加桶 17=110 且无重复；方块状态 97、物品模型 110 一一对应；注册入口和 Forge 构造器均按本地 JAR 核实 | `registry/`、`block/`、`item/`、主类 |

## Stage C-2 · 流体、容器与持久化

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T2.2 | 迁移领域流体罐、物品能力、容器转移和持久化 | L | T2.1 | 静态核实 Forge capability、`FluidStack` NBT、`load(CompoundTag)`/`saveAdditional(CompoundTag)`、simulate/execute、桶、自动化和 tooltip 签名；便携罐仍用 ItemStack NBT，未注册 1.21 数据组件 | `common/`、`item/`、`block/entity/` |

## Stage C-3 · 多方块结构

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T2.3 | 迁移多方块结构和结构错误状态 | L | T2.2 | 静态核实冶炼炉、铸造炉、合金炉的边界、尺寸、方向、燃料、设备登记和 `StructureErrorReason` 越界回退；目标使用 1.20.1 `Level`/`BlockPos`/`BlockState` API | `multiblock/`、相关方块实体 |

## Stage C-4 · 调试生成

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T2.4 | 迁移 Forge 调试生成命令 | M | T2.3 | 静态核实 `/tinker_foundry debug generate` 的四种类型、`CommandSourceStack.hasPermission(2)`、方向、结构、可操作设备和初始流体；命令树使用 1.20.1 Forge `RegisterCommandsEvent` | `debug/` |

**Gate C：PC-1→PC-2→PC-3→PC-4 已串行完成静态审计，主对话已授权继续 Stage D。** 已审阅注册全集、能力调用链、结构方向和命令树；不运行客户端，构建/运行验证待用户明确授权。

## Stage D-1 · 配方 Java

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T3.1 | 迁移 8 个自定义配方 type、serializer 和匹配逻辑 | L | T2.4 | 8 个自定义注册名、JSON/FriendlyByteBuf 顺序、匹配边界已逐类静态核对；`alloying` 只保留 Java type/serializer，无来源 JSON；普通 Minecraft 配方未混入自定义注册表 | `recipe/`、`registry/TFRecipes.java` |

## Stage D-2 · 配方 JSON

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T3.2 | 转换全部配方 JSON 到 `recipes/` 和 1.20.1 字段 | L | T3.1 | 220 个 JSON 全部可解析；1211/2612 各 220 个相对 ID 一一映射；自定义 117 个、普通 Minecraft 103 个；10 个药水流体结果由 1.21 `components` 改为 1.20.1 `tag.Potion`；无单数目标路径、无 alloying JSON | `src/main/resources/data/tinker_foundry/recipes/`、`docs/1201-forge-alloying-data.md` |

## Stage D-3 · 处理与重复操作保护

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T3.3 | 迁移熔炼、合金、浇筑产物占用和重复浇筑保护 | L | T3.2 | `tickCasting`、`finishCasting`、`finishMolding` 和容器处理均在输出非空时阻断；流体仅在产物可接收后扣除；浇注口批次不会被覆盖，输出模拟/执行量均受当前批次上限约束 | `block/entity/FoundryBlockEntity.java` |

**Gate D：已通过（主对话审核日期：2026-09-21）。** 主对话确认 PD-1→PD-2→PD-3 的 JSON、ID、serializer、药水 NBT、输出阻塞和连续浇注保护静态结果；不运行数据生成或 Gradle。

## Stage E-1 · 菜单

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T4.1 | 迁移 Forge 菜单、动态槽和容量/热量同步 | L | T3.3 | `FoundryMenu` 的动态槽、`ContainerData` 数据编号、输入/输出与流体/热量/结构状态职责静态闭合；补齐 Forge 菜单所需方块实体状态 getter | `menu/`、`client/FoundryScreen.java`、串行接力的 `block/entity/FoundryBlockEntity.java` |

## Stage E-2 · 网络

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T4.2 | 迁移 Forge 网络通道与两个逻辑载荷 | L | T4.1 | payload ID、协议 6、字段顺序、`FriendlyByteBuf` 边界、流体堆栈 NBT 大小、流体标签资源 ID、非负数值和枚举回退均已静态核对；不保证跨 MC 版本字节互通 | `network/` |

## Stage E-3 · 客户端

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T4.3 | 迁移流体、结构、设备、模型和 GUI 渲染 | L | T4.2 | Forge 47.4.23 `VertexConsumer` 的 1.20.1 写入链、模型 loader、流体颜色、设备/结构 renderer 和动态 GUI 调用已静态核对；不宣称画面运行验证 | `client/`、客户端资源 |

## Stage E-4 · JEI

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T4.4 | 迁移 JEI 插件 | M | T4.3 | 本地 JEI 15.49.0.191 Forge API 已静态核对；8 个自定义配方类型均有查看入口，`casting` 拆成盆/台两实例共 9 个分类注册，使用 `ForgeTypes` 和 RecipeManager 列表并含 ore/damageable；无 alloying JSON；真实客户端可查看仍待用户授权验证；不编辑 PB-1 构建文件 | `compat/` |

**Gate E：PE-1→PE-2→PE-3→PE-4 已串行完成静态审计，主对话已于 2026-09-21 通过。** 已覆盖动态菜单、Forge 网络边界、流体标签网络边界、药水 NBT 转换、合金催化匹配、1.20.1 渲染 API 和 JEI API；真实画面、启动、JEI 展示和玩法证据仍待用户授权。

## Stage F-1 · 视觉资源

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T5.1 | 迁移 blockstates、方块/物品模型、纹理和 GUI 资源 | L | T4.4 | 97 个 blockstates、110 个 item model、170 个 block model、206 个方块纹理和 8 个 GUI 纹理已静态闭合；2612 `items/` 未直接复制，便携容器改用 Forge `fluid_container` schema；JSON 解析和模型/纹理引用无错误 | `assets/tinker_foundry/`（不含 `lang/`）；证据见 `docs/1201-forge-resource-audit.md` |

## Stage F-2 · 语言

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T5.3 | 迁移英文默认与中文翻译 | M | T5.1 | `en_us.json` 与 `zh_cn.json` 各 297 个 key 且集合完全一致；Java 静态显示 key 无固定缺失；调试日志仍为英文；不与 PF-1 同时编辑 | `assets/tinker_foundry/lang/`；book 外部许可/章节正文未决项见资源审计 |

## Stage F-3 · 数据包与 book

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T5.2 | 迁移 `loot_tables/`、tags、damage type、book/章节及结构资源 | M | T5.3、T3.2 | 57 个 tag JSON、77 个 loot table、2 个 damage type、7 个 book 章节和 2 个指南结构 NBT 已静态审计；目标只使用 `loot_tables/`；book 保留为独立指南候选，22 个章节翻译 key 和图片许可不凭空补写 | `src/main/resources/data/`（不含 `recipes/`）；`assets/tinker_foundry/book/`；证据见 `docs/1201-forge-resource-audit.md` |

**Gate F：核心资源静态审核已由主对话于 2026-09-21 通过。** JSON、模型/纹理引用、语言 key、注册 ID、tags、loot、damage type、book 和结构目录均已核对；`alloying` 仍为 0 条数据。book 章节没有 Java 消费者，且有 22 个未提供的正文/标题 key；这与图片许可一起继续列为外部选择项，不纳入最终完成声明，也不阻塞核心 Forge 资源骨架。

## Stage G · 总静态审计

| ID | 任务 | 复杂度 | 依赖 | 验收标准 | 文件 |
| --- | --- | --- | --- | --- | --- |
| ☑ T6.1 | 全项目静态审计和文档回写 | L | T5.2、T4.4 | 已静态核对无 NeoForge/26.1.2-only 构建/API 残留；ID、资源、配方、网络、语言、能力、命令清单闭合；book 未决项未伪装为完成 | 全项目、docs、`docs/1201-forge-final-static-audit.md` |
| ☐ T6.2 | 用户授权后执行最小构建验证 | M | T6.1 且用户明确授权 | 单独记录构建证据；不把静态通过冒充构建通过 | 根构建文件 |
| ☐ T6.3 | 用户授权后执行客户端/玩法验证 | L | T6.2 且用户明确授权 | 分别记录启动、GUI、配方、流体、桶、多方块、浇筑、JEI、调试命令证据 | 日志与截图 |

**Gate G：** PG-1/T6.1 静态审计完成，等待主对话复核；T6.2/T6.3 在用户明确授权前保持 ☐，不得将静态完成写成构建或运行完成。

## 串行依赖图

```mermaid
flowchart LR
    A0[T0.1 盘点] --> A1[T0.2 契约]
    A1 --> A2[T0.3 版本证据]
    A2 --> A3[T0.4 所有权/计划]
    A3 --> A4[T0.5 冻结门禁]
    A4 --> B1[T1.1 Forge 骨架]
    B1 --> B2[T1.2 目录骨架]
    B2 --> B3[T1.3 API 证据]
    B3 --> C1[T2.1 注册]
    C1 --> C2[T2.2 流体/持久化]
    C2 --> C3[T2.3 多方块]
    C3 --> C4[T2.4 调试命令]
    C4 --> D1[T3.1 配方 Java]
    D1 --> D2[T3.2 配方 JSON]
    D2 --> D3[T3.3 处理保护]
    D3 --> E1[T4.1 菜单]
    E1 --> E2[T4.2 网络]
    E2 --> E3[T4.3 客户端]
    E3 --> E4[T4.4 JEI]
    E4 --> F1[T5.1 视觉资源]
    F1 --> F2[T5.3 语言]
    F2 --> F3[T5.2 数据/book]
    F3 --> G[T6.1 静态总审计]
    G -.重新授权.-> H1[T6.2 构建]
    H1 -.重新授权.-> H2[T6.3 运行/玩法]
```

这里不再把有读取依赖的任务放在同一可并行阶段。若未来拆分代理，仍必须遵循该链；只有不改变上述交付物且不形成共享文件依赖的只读研究，才可作为阶段内附属工作。

## 阻塞研究与待验证

| ID | 项目 | 阻塞任务 | 解决方式 |
| --- | --- | --- | --- |
| R1 | Forge 47.4.23 注册、能力、流体、菜单、网络、渲染、命令 API | T1.3、T2.x、T4.x | 从本地 Forge 依赖源码/JAR 建立版本证据 |
| R2 | 1.20.1 配方 serializer/schema | T3.1-T3.2 | 读取目标 Recipe/RecipeSerializer 和同版本数据，逐 JSON 映射 |
| R3 | 1.20.1 Forge JEI 精确版本/API | T4.4 | 核查本地 Forge JEI JAR 与目标环境；不使用 NeoForge 坐标 |
| R4 | 2612 `FoundryRecipe` 与删除 `FoundryRecipeSerializer` 的关系 | T3.1 | 对照 dirty diff、依赖源码和目标 Forge API 决定重写/排除 |
| R5 | book 图片/许可和产品范围 | T5.2 | 审核章节、图片、`UPSTREAM-LICENSE.txt`，排除匠魂工具/材料系统内容 |

## Definition of Done

- ☑ M0-M3 的功能、注册、配方、数据、资源、网络和兼容清单已完成静态闭合；book 22 个翻译 key 和图片许可仍未决，`alloying` 无来源数据。
- ☑ 已静态检查新增 Java/Gradle 注释为中文、日志消息为英文、文件为 UTF-8；未运行格式化或构建工具。
- ☑ PG-1 静态审计确认目标无实际 NeoForge/26.1.2 构建、API 或资源残留。
- ☐ 用户授权后，构建、启动和玩法证据分别记录；授权前不执行。
- ☐ 未经用户另行要求，不提交、不推送、不上传。

## Revision Log

- 2026-09-21 — 主对话通过 Gate A、Gate B，并授权越过 PC-1→PC-4 静态交接；完成 PD-1→PD-3 配方 Java、220 个 JSON、药水 NBT 转换、输出阻塞和连续浇注保护静态交付（当时记录为待审核，后续已通过 Gate D），未运行验证。
- 2026-09-21 — 主对话通过 Gate D、Gate E 和 Gate F 核心资源静态审核；Luna 按 PE-1→PE-2→PE-3→PE-4 串行完成菜单数据适配、Forge 网络边界、1.20.1 客户端顶点写入和 JEI 15.49.0.191 API 适配，记录流体标签边界、药水 NBT 转换和合金催化匹配；PF-1→PF-3→PF-2 资源静态审计完成，未运行验证。
- 2026-09-21 — 主对话通过 Gate F 核心资源静态审核；PG-1/T6.1 完成 NeoForge/2612 残留、注册/资源/配方/网络/语言/命令/API/所有权总静态审计，新增 `docs/1201-forge-final-static-audit.md`；book 22 个翻译 key 和图片许可继续未决，未运行验证。
