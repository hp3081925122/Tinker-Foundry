# Tinker Foundry 1.20.1 Forge 迁移设计（阶段 A 修订版）

> 目标：在 Minecraft 1.20.1 Forge 47.4.23、Java 17、official mappings 上保留 Tinker Foundry 的熔炼、合金、浇筑、多方块、流体容器、GUI、网络、JEI、资源和调试能力。  
> 来源：1.21.1 NeoForge master `1102c56`；26.1.2 NeoForge dirty worktree 只作为候选修复/新增来源。  
> 当前状态：Gate A、Gate B、Gate D、Gate E、Gate F 核心资源静态审核已通过（主对话审核日期：2026-09-21）；PC-1→PC-4、PD-1→PD-3、PE-1→PE-4、PF-1→PF-3→PF-2 已完成静态交接；PG-1/T6.1 已完成，待主对话复核；构建/运行未执行。

## 1. 背景与迁移原则

迁移起点是 1.21.1 NeoForge：原始 `gradle.properties` 使用 1.21.1、NeoForge 和 Java 21，源码使用 `net.neoforged.*`；目标工作树的构建骨架现已按本机 1201 Forge 模板重建，剩余版本差异必须逐项适配，不能把 NeoForge API 当作 Forge API。

后续必须先以本机模板 `E:\模组\模组模板\1201-forge` 重建构建骨架，再逐项迁移领域代码。不得从 NeoForge 构建文件反向裁剪，也不得为了通过编译删除功能。

## 2. 版本 API 差异与边界

| 能力 | 1.21.1/2612 当前来源 | 1.20.1 Forge 设计 |
| --- | --- | --- |
| 构建插件/元数据 | NeoForge ModDev、`neoforge.mods.toml` | ForgeGradle `[6.0,6.2)`、`mods.toml`；只取本机模板 |
| Minecraft/Java | 1.21.1/26.1.2，Java 21/25 | 1.20.1，Java 17 |
| 映射 | NeoForge 对应映射 | official 1.20.1 |
| 注册对象 | `DeferredRegister`/`DeferredHolder` 等 NeoForge 组合 | Forge `DeferredRegister`、`RegistryObject`、`ForgeRegistries`；每个签名须由 Forge 47.4.23 证据确认 |
| 流体能力 | NeoForge 能力包与事件 | Forge 流体能力和能力提供方式；模拟/实际操作须共享同一领域状态 |
| 网络 | NeoForge payload、`StreamCodec` 形态 | Forge `SimpleChannel`/目标缓冲区 API；保留逻辑 ID、字段顺序、协议语义，不复制 payload API |
| 数据组件 | 1211 使用 `Registries.DATA_COMPONENT_TYPE`、`CustomData.CODEC` | 1.20.1 目标 JAR 未见对应 `core/component` 和 `world/item/component` 包；不得移植不存在的注册表，改用目标可验证的物品 NBT/能力持久化 |
| 配方 | 1.21/26 的 codec、资源单数 `recipe/` | 1.20.1 `Recipe`/`RecipeSerializer`/`RecipeType` 和 `recipes/`；字段逐 JSON 核对 |
| 资源 | 2612 有 `assets/.../items/` 等新 schema | 1.20.1 模型/方块状态按目标加载器规则；便携桶/罐使用 Forge `forge:fluid_container`，不能直接复制 2612 item model |
| 命令/事件 | NeoForge 事件注册 | Forge `RegisterCommandsEvent` 等具体类、方法和权限签名须由本地依赖证据确认 |

## 3. 架构和迁移顺序

包边界保持 `org.hp.tinker_foundry`，版本差异限制在适配边界：

```text
注册与启动 -> blocks/items/fluids/block entity/entities/menus/recipes
领域状态 -> common、容器转移、流体与多方块
Forge 适配 -> 能力、SimpleChannel、事件、命令、客户端注册
客户端 -> GUI、模型、流体和结构渲染
可选集成 -> JEI compat
```

真实顺序是串行的：契约/资源证据 → Forge 骨架/API 证据 → 注册 → 流体与持久化 → 多方块 → 调试命令 → 配方 Java → 配方 JSON/处理 → 菜单 → 网络 → 客户端 → JEI → 资源 → 语言 → 全项目静态审计。每个阶段内部只有文件完全不重叠且无数据依赖的工作才允许并行；本项目当前由 Luna 单一实现者承接，计划文件另列审核者。

## 4. 核心设计决策

### 4.1 构建与元数据

唯一来源：`E:\模组\模组模板\1201-forge`。

- ForgeGradle `[6.0,6.2)`。
- Forge `47.4.23`；IDEA 数据运行类型使用本地可用的 `serverData`，不使用用户日志中报错的 `data`。
- Minecraft `1.20.1`、Java 17、official mappings `1.20.1`。
- Gradle wrapper `8.8`。
- `META-INF/mods.toml` 和 Forge run 配置。
- JEI 只作可选集成；当前本地已核实 `15.49.0.191` 的 Forge JAR，依赖由构建骨架 owner 写入，`mods.toml` 的 `${jei_version_range}` 由 `processResources` 的 `replaceProperties` 展开；JEI Java 适配不得修改构建文件。

### 4.2 注册

保持 `tinker_foundry`、Java 包、逻辑配方 ID 和来源已核实注册名。完整方块/物品/流体/菜单注册名及动态生成规则已经写入 [`1201-forge-contract-manifest.md`](1201-forge-contract-manifest.md)，物品数量核实为固定声明 54、方块变种 39、附加流体桶 17，合计 110 个不重复 ID；Gate A 已依据该 manifest 通过，任何“全注册全集”声明都必须引用该 manifest。

来源 `TFDataComponents.FLUID` 单独处理：1211 的 `DataComponentType<CustomData>` 是来源语义，不是可直接移植的 1.20.1 注册表。目标 `TFDataComponents.FLUID_TAG` 只是 `ItemStack` NBT 的字符串键。目标先按 manifest 冻结便携罐的逻辑持久化语义，再由 PB-2 核验 Forge 流体能力实现；不得为保留同名组织类而创建不存在的数据组件注册表。

### 4.3 流体、容器和持久化

`StructureFluidTank`、合金输入、燃料罐、浇注输出和便携罐容量是领域状态。Forge 适配层必须保证以下入口共享同一状态：右键桶交互、`IFluidHandler` 模拟/执行、自动化输入输出、菜单容量显示、物品栏 tooltip 和损坏后物品保存。

1211 `PortableTankFluidHandler` 的实际语义是：从来源 `TFDataComponents.FLUID` 读取 `FluidStack.CODEC` 数据，空流体删除数据，填充/排出执行后写回，容量限制由 handler 控制。1.20.1 不保留不存在的 component registry，而是使用目标 `TFDataComponents.FLUID_TAG` 指向的 `ItemStack` NBT 键 `Fluid` 保存 `FluidStack.writeToNBT/loadFluidStackFromNBT` 语义，并通过 Forge 物品能力暴露；目标键名、空值、损坏保留和兼容边界已由 T0.5 manifest 冻结。方块实体使用 Forge `getCapability(Capability, Direction)`，位置查询先取方块实体；不能照搬 NeoForge 的 Level 三参能力调用。`FaucetState` 两版本源码顺序也已冻结为 `OFF=0`、`POURING=1`、`POWERED=2`，缺失/越界值按 0..2 钳制。

### 4.4 多方块与调试命令

保留控制器、边界/墙/底板、内部空间、燃料罐、设备、方向和结构错误位置。2612 的 `/tinker_foundry debug generate <smeltery|foundry|alloyer|all>` 是候选必须纳入功能；Forge 实现必须静态确认命令树、权限、方块放置方向、方块实体刷新和流体预装调用。

生成结构必须能操作：包括对应控制器、浇注口、浇注盆/台、排液设备、燃料和初始熔融流体，不能只生成外框。

### 4.5 配方与 JEI

本项目有 **8 个自定义配方 type/serializer 注册名**：

`entity_melting`、`melting`、`ore_melting`、`damageable_melting`、`alloying`、`casting`、`molding`、`fuel`。

这 8 个自定义注册名与普通 Minecraft 配方分开。原版 `crafting`、`smelting`、`blasting`、`smoking`、`campfire_cooking`、`stonecutting` 等若出现在数据中，只使用 1.20.1 普通 `recipes/` schema，不计入 Tinker Foundry 自定义 serializer 数量。

1.20.1 实现使用目标 `Recipe`、`RecipeSerializer`、`RecipeType`、`Ingredient`、自定义 Forge 流体输入和 `FriendlyByteBuf`；不得从 1.21/26 的 `RecipeInput`、`MapCodec`、`StreamCodec`、`RecipeHolder` 形态猜测。JEI 使用 `mezz.jei.api.forge.ForgeTypes` 与 `RecipeManager.getAllRecipesFor`，已补 `ore_melting`/`damageable_melting` 分类；真实客户端查看仍待授权验证。

### 4.6 网络与菜单

逻辑载荷仍为：

- `tinker_foundry:foundry_state`
- `tinker_foundry:foundry_structure_error`

协议语义版本为 `6`。`FoundryStatePayload` 的字段顺序、列表长度、流体存在标记和热量数组顺序见 requirements；`FoundryStructureErrorPayload` 的可选错误位置和枚举顺序也已冻结。Forge 网络适配层使用 `SimpleChannel/FriendlyByteBuf` 负责编码/解码和客户端应用，服务端领域状态仍为权威来源；不保证跨 Minecraft 版本旧字节流互通。

网络边界按目标协议增加防御检查：流体标签显式列表最多 128 项、资源位置字符串最多 256；存在的 `FluidStack` 必须为正数量，附加 NBT 不超过 16 KiB；状态 VarInt 和热量值不得为负。`StructureErrorReason` 仍通过 ordinal 编码，读取端越界回退 `NONE`。这些限制保护 Forge 1.20.1 当前协议，不构成跨 Minecraft 版本互通承诺。

### 4.7 Stage E 串行实现结果

- PE-1：`FoundryMenu` 的动态槽、`ContainerData` 字段和 `FoundryScreen` 的容量/热量/流体显示静态闭合；方块实体通过串行窄接口补齐菜单需要的状态 getter 和 `menuData()`，不改处理、能力或持久化语义。
- PE-2：两个逻辑载荷继续使用 Forge `SimpleChannel`/`FriendlyByteBuf`；流体标签网络边界和 FluidStack 附加 NBT 边界已写入目标代码。药水结果沿用 PD-1 的 `Potion`、`CustomPotionEffects`、`CustomPotionColor` 传统 NBT 转换。
- PE-3：本地 Forge 1.20.1 `VertexConsumer` 证据要求 `vertex`、`color`、`uv`、`uv2`、`normal`、`endVertex` 链，目标流体 renderer 已移除 1.21 `addVertex` 链；模型 loader、颜色事件和 FluidType 客户端扩展保持 Forge 入口。
- PE-4：目标 JEI 插件使用本地 `15.49.0.191` Forge API 的 `ForgeTypes.FLUID_STACK`、`RecipeManager.getAllRecipesFor`、`addAnimatedRecipeArrow` 和 `addDrawable`；8 个自定义配方类型均有入口，`casting` 拆成盆/台两个分类实例，共 9 个分类注册；`alloying` 仍无来源 JSON。合金匹配中 `catalyst=true` 必须占用匹配槽位但执行时不扣料。

## 5. 依赖策略

### 必需依赖

- Minecraft 1.20.1。
- Forge 47.4.23。
- Java 17。
- official mappings 1.20.1。
- 本机 1201-forge 模板的 ForgeGradle/wrapper。

### 可选依赖

- 1.20.1 Forge JEI：本机缓存已核实 `jei-1.20.1-common-api`、`jei-1.20.1-forge-api`、`jei-1.20.1-common`、`jei-1.20.1-forge` 的 `15.49.0.191` JAR；依赖保持可选，目标 `compat/` 只使用 Forge API，运行时解析和客户端展示仍待授权；禁止使用 NeoForge 坐标。
- 其他流体容器：只走 Forge 标准能力，不绑定未核实的模组 API。

每个类、事件、方法、构造器和配方字段都必须有 1.20.1 Forge 模板依赖、缓存源码/JAR、反编译结果或同版本项目证据；不能凭记忆补 API。

## 6. 包与资源路径

```text
src/main/java/org/hp/tinker_foundry/
  TinkerFoundry.java
  block/                 方块；block/entity/ 为方块实体
  client/                GUI、模型、流体和结构渲染
  common/                流体、物品能力、容器传输、共享状态
  compat/                JEI
  debug/                 调试命令
  entity/                流体炮实体
  item/                  便携罐和物品
  menu/                  菜单
  multiblock/            多方块检测
  network/               Forge 网络适配
  recipe/                熔炼/合金/浇筑/燃料配方
  registry/              注册入口
```

目标资源路径候选：

- `src/main/resources/META-INF/mods.toml`
- `src/main/resources/assets/tinker_foundry/`
- `src/main/resources/data/tinker_foundry/recipes/`
- `src/main/resources/data/tinker_foundry/loot_tables/`（复数，已证实）
- `src/main/resources/data/tinker_foundry/tags/`
- `src/main/resources/data/tinker_foundry/damage_type/`（待目标 schema 证据）
- `src/main/resources/assets/tinker_foundry/lang/en_us.json`
- `src/main/resources/assets/tinker_foundry/lang/zh_cn.json`

不得把未证实的 `loot_table/` 写成目标目录。现有单数 `data/tinker_foundry/recipe/` 是来源目录，迁移目标是 `recipes/`，但每个 JSON 的 type/字段仍需独立核对。

## 7. book 范围决策

当前 book 不是可直接认定的“匠魂书”：`data/tinker_foundry/book/chapters.json` 和七个章节 JSON 使用本项目命名空间，英文索引为 `Standalone Foundry Guide`，资源内容主要是熔炼、合金、浇筑、燃料、冶炼炉和铸造炉说明。静态搜索未发现 Java 端 Tinkers' Construct 书注册。

设计结论：

- 核心 Forge 骨架不依赖 book，book 不阻塞 Stage B；本项目独立指南的 JSON、语言和结构资源保留为 Stage F 可选候选。
- 最终是否发布 `assets/tinker_foundry/book/images/` 待外部选择。`UPSTREAM-LICENSE.txt` 只有 MIT 条款和版权信息，没有逐张图片的来源映射、版本号或外部 URL；在选择前不得宣称图片发布许可已经完成。
- **明确排除** Tinkers' Construct 书 API/注册以及工具、材料词条、Modifier、工具部件内容；不能把本项目指南目录称为或混入“原版匠魂书”。

## 8. 风险与缓解

| 风险 | 等级 | 缓解 |
| --- | --- | --- |
| NeoForge DeferredHolder/payload/能力 API 混入 Forge | 高 | 先完成 Forge 47.4.23 API 证据表，逐文件迁移 |
| 自定义配方/普通配方统计或 schema 混淆 | 高 | 固定 8 个自定义注册名，逐 JSON 建立 ID→路径/字段映射 |
| 流体能力与 NBT 状态分叉 | 高 | 同一领域状态覆盖模拟/执行、桶、自动化、tooltip 和持久化 |
| 1.20.1 没有 1.21 数据组件注册表 | 高 | 不迁移 `Registries.DATA_COMPONENT_TYPE`；先冻结等价 NBT/能力语义 |
| 资源路径错误 | 高 | 使用本地 1.20.1 JAR 证据，目标为 `recipes/`、`loot_tables/` |
| 2612 dirty 未完成改动混入 | 高 | 每个 dirty-only 文件记录纳入/重写/排除理由 |
| book 越界或许可问题 | 中 | 独立指南候选不阻塞骨架；发布图片前补齐逐项来源/许可选择，明确排除匠魂书 |
| 无运行证据却宣称完成 | 高 | 静态、构建、启动、玩法分层记录；当前后三级均未执行 |

## 9. 已验证证据路径

| 结论 | 证据 |
| --- | --- |
| Forge 47.4.23、Minecraft 1.20.1、Java 17、official mappings | `E:\模组\模组模板\1201-forge\gradle.properties`、`build.gradle` |
| Gradle wrapper 8.8 | `E:\模组\模组模板\1201-forge\gradle\wrapper\gradle-wrapper.properties` |
| Forge 1.20.1 类/资源清单 | `C:\Users\30819\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1-sources.jar` 与同目录 recomp JAR |
| `loot_tables/`、`recipes/`、tags 路径 | 上述 1.20.1 recomp JAR 的 `jar tf`：存在 `data/minecraft/loot_tables/`、`data/minecraft/recipes/`、`data/forge/tags/`，不存在单数 `loot_table/` |
| 1.21.1 数据组件语义 | 只读 master `src/main/java/org/hp/tinker_foundry/registry/TFDataComponents.java`、`item/PortableTankFluidHandler.java` |
| 1211 NBT 键/顺序 | 只读 master `src/main/java/org/hp/tinker_foundry/block/entity/FoundryBlockEntity.java` 的 `saveAdditional/loadAdditional` |
| book 内容和引用 | 只读 master `src/main/resources/assets/tinker_foundry/book/`、`data/tinker_foundry/book/`、两份 lang 文件及静态引用搜索 |
| T0.5 完整注册、网络、NBT、便携罐和 book 冻结清单 | 目标工作树 `docs/1201-forge-contract-manifest.md`；逐项引用 1211/2612 来源文件和行 |
| Forge JEI 坐标候选 | `E:\模组\发cf的模组\JEI Tag Groups\1201 Forge JEI Tag Groups\gradle.properties`、`build.gradle` |

## 10. 阶段状态、待验证项与门禁

T0.5 已由主对话于 2026-09-21 审核通过，Gate A 已通过。完整注册 manifest、动态规则、payload 字段/编码顺序和边界、`StructureErrorReason` ordinal、方块实体 NBT/缺省行为、便携罐逻辑持久化语义、1.20.1 已知资源路径和 book 范围均见 [`1201-forge-contract-manifest.md`](1201-forge-contract-manifest.md)。

仍待后续阶段或外部选择的事项：

1. Forge 47.4.23 注册、能力、流体、菜单、网络、渲染、命令、配方和 JEI 的方法级证据已由 PB-2 完成，详见 [`1201-forge-api-evidence.md`](1201-forge-api-evidence.md)；目标代码已按其中已验证入口做确定性适配，但这不等于已经构建或运行通过。
2. 目标树已把单数 `recipe/` 静态资源迁到 `recipes/`；220 个配方 JSON 已按 1.20.1 字段逐文件核对，10 个 1.21 药水组件结果改为传统 `tag.Potion`，tags 和其他资源引用仍留后续阶段。
3. `alloying` 没有 1211/2612/目标 JSON 数据，当前只保留 type/serializer/JEI 入口和清单记录；若产品要新增配方，必须先冻结数据字段和来源，不从数量反推。
4. `damage_type/`、tags 子目录和 book 图片最终发布范围仍需目标 schema/外部许可选择；这些不改变 T0.5 已冻结的公开 ID 和逻辑持久化契约。
5. Gate B 已由主对话于 2026-09-21 审核通过；PC-1→PC-4 已完成静态交接，PD-1→PD-3 静态审计完成并通过 Gate D，PE-1→PE-4 已完成 Stage E 静态适配并通过 Gate E，PF-1→PF-3→PF-2 的核心资源静态审计已通过 Gate F，PG-1/T6.1 总静态审计已完成并待主对话复核；构建、compile、Gradle validation、runClient 和 GameTest 均未执行。

## Revision Log

- 2026-09-21 — Gate A、Gate B 通过；PC-1→PC-4 静态交接获授权后，完成 PD-1→PD-3 串行配方、JSON 和处理保护审计（当时记录为待审核，后续已通过 Gate D）。
- 2026-09-21 — 主对话通过 Gate D、Gate E 和 Gate F 核心资源静态审核；按 PE-1→PE-2→PE-3→PE-4 串行完成菜单、网络边界、1.20.1 客户端渲染和 JEI 静态适配；按 PF-1→PF-3→PF-2 串行完成资源 schema、语言、tags、loot、book 和结构资源静态审计；未运行验证。
- 2026-09-21 — 主对话通过 Gate F 核心资源静态审核；PG-1/T6.1 完成 Forge/NeoForge 残留、注册/资源/配方/网络/语言/命令/API/所有权总审计，book 22 个翻译 key 和图片许可仍未决，未运行验证。
