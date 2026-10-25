# Tinker Foundry 1.20.1 Forge PG-1 总静态审计

核对日期：2026-09-21  
目标工作树：`E:\模组\发cf的模组\Tinker Foundry\1.20.1-forge-tinker-foundry`  
目标分支：`1201forge`  
目标版本：Minecraft 1.20.1、Forge 47.4.23、Java 17、official mappings  
执行者：Luna  
审核者：主对话  
状态：**PG-1/T6.1 静态审计完成，待主对话复核；T6.2/T6.3 仍未授权。**

本文件只记录静态文本、源码、资源、JSON、JAR 条目和 Git 状态核对。没有运行 `build`、`compile`、Gradle validation、`runClient` 或 GameTest，也没有修改两个 NeoForge 参考工作树。

Git 静态快照：目标分支 `1201forge`、HEAD `1102c56`、工作树 dirty 条目 409；1211 master 参考树 dirty 条目 1（既有未跟踪交接文件）；2612 参考树 dirty 条目 278。目标的大量迁移差异属于前序工作，本轮不清理、不回退；本轮不提交、不推送。

## 1. 审计边界与总结果

| 检查面 | 结果 | 证据 |
| --- | --- | --- |
| Forge/NeoForge 构建残留 | 目标运行时、构建配置和资源元数据中没有实际 `net.neoforged`、`neoforge:`、`neoforge.mods.toml` 或 2612 `items/` schema | `build.gradle`、`gradle.properties`、`settings.gradle`、`src/main/resources/META-INF/`、目标源码/资源扫描 |
| 版本骨架 | ForgeGradle `[6.0,6.2)`、Forge `47.4.23`、Java 17、official mappings `1.20.1`；数据运行类型为模板可用的 `serverData`；`processResources` 展开 `jei_version_range` | `build.gradle:1-75`、`gradle.properties:1-22`、`docs/1201-forge-api-evidence.md` §2.9 |
| 注册闭合 | 77 个静态方块、20 个动态流体方块、110 个物品、20/20/20 流体注册对象、1 方块实体、1 实体、1 菜单、1 创造标签页、8 配方类型和 8 序列化器 | 本文件 §2、`docs/1201-forge-contract-manifest.md` §1-§4 |
| 资源闭合 | 744 个 JSON 使用目标目录；System.Text.Json 解析失败 0；97 blockstates、110 item models、170 block models；引用审计缺失 0 | `docs/1201-forge-resource-audit.md` §2、§4 |
| 配方闭合 | 220 个配方文件：117 个自定义类型、103 个普通 Minecraft 类型；`alloying` 数据 0 条，保持无来源事实 | 本文件 §4、`docs/1201-forge-alloying-data.md` |
| 网络闭合 | 两个 Forge `SimpleChannel` 消息，协议值 6，字段顺序、边界和结构错误 ordinal 已冻结 | `network/FoundryNetworking.java:18-42`、`FoundryStatePayload.java`、`FoundryStructureErrorPayload.java`、manifest §6 |
| 语言闭合 | `en_us.json` 与 `zh_cn.json` 各 297 key，集合差异 0 | `src/main/resources/assets/tinker_foundry/lang/`、资源审计 §3 |
| 命令闭合 | Forge `RegisterCommandsEvent` 注册 `/tinker_foundry debug generate <type>`，权限为 `hasPermission(2)` | `debug/FoundryDebugCommand.java:27-47`、`TinkerFoundry.java:206` |
| book | 7 章、2 个结构 NBT 已盘点；22 个标题/正文 key 缺失，图片逐张许可仍未决，不纳入最终完成声明 | 资源审计 §4.2、manifest §8 |

PG-1 的结论是“目标已经完成 Forge 迁移的静态闭合审计”，不是“可构建、可启动、可玩或可发布”。book 的 22 个翻译 key 和图片许可仍是明确未决项；`alloying` 没有来源数据，不能补写 JSON。

## 2. NeoForge/2612 残留扫描

### 2.1 实际运行时和资源残留

扫描范围包括目标 `src/main/java/`、`src/test/java/`、`src/main/resources/`、根构建文件、`README.md` 和 `NOTICE.md`；排除只用于证据说明的 `docs/` 内容。PG-1 对残留分为“实际目标配置/API”和“解释性版本边界文字”两类：

| 项目 | 结果 | 处理/判断 |
| --- | --- | --- |
| `net.neoforged` 实际 import/类型 | 0 | 无需处理 |
| `neoforge:` 或 `neoforge.` 资源/配置标记 | 0 | 桶/罐模型已使用 Forge `forge:fluid_container` |
| `src/main/resources/META-INF/neoforge.mods.toml` | 不存在 | 目标只保留 `mods.toml` 和既有 `tinker_foundry.mixins.json` 元数据 |
| `assets/tinker_foundry/items/` | 不存在 | 未复制 26.1.2 的新版 item-model 根目录；目标使用 `models/item/` |
| `TFDataComponents` 目标实现 | 不注册数据组件 | `TFDataComponents.FLUID_TAG` 只是字符串键 `Fluid`，便携罐通过 1.20.1 Forge `ItemStack` NBT/能力实现 |
| 2612 的 `CustomData`、`StreamCodec`、payload 注册 API | 0 个目标运行时调用 | 目标网络改为 Forge `SimpleChannel`/`FriendlyByteBuf` |

本轮还把根 `README.md`、`NOTICE.md` 中过时的 1.21.1 NeoForge/协议 4 表述改成 1.20.1 Forge/协议 6，并把测试入口和能力工具的过时 loader 说明改为目标版本表述。这些是文档或注释清理，没有改变业务逻辑。

### 2.2 允许保留的版本边界说明

以下源码中的高版本名称只用于说明迁移边界，不是目标 API 残留：

- `registry/TFDataComponents.java`：说明 1.20.1 不创建数据组件注册表；实际代码只有 `FLUID_TAG = "Fluid"`。
- `recipe/FoundryFluidAmount.java`、`recipe/FoundryRecipeData.java`：说明来源版本的组件/有数量流体输入如何转换为 Forge 1.20.1 语义。
- `client/FoundryFluidRenderer.java`：说明目标使用 `vertex/color/uv/uv2/normal/endVertex` 链，而不是高版本顶点入口。
- `client/FoundryScreen.java`：说明药水流体在目标版本按独立 `potion` 流体 ID 识别。

这些说明不会生成 NeoForge 类引用；API 细签仍以 `docs/1201-forge-api-evidence.md` 和本机 Forge JAR 为准。

## 3. 注册、资源和文件闭合

### 3.1 注册数量与资源一一对应

| 注册对象 | 静态来源/规则 | 数量 | 资源或后续消费核对 |
| --- | --- | ---: | --- |
| `TFBlocks` 静态方块 | `registry/TFBlocks.java` 的 `RegistryObject<Block>` 声明 | 77 | 对应 77 个非流体 blockstate/loot 条目 |
| 流体液体方块 | `TFFluids.java` 固定 3 + `EXTRA_COLORS` 17，`name + "_fluid"` | 20 | 与 77 个静态方块合计 97 个 blockstates |
| 固定物品 | `TFItems.java` 54 个 `public static final RegistryObject` | 54 | 纳入 manifest，无重复 |
| 方块变种物品 | `registerVariantBlockItems()` 的 `items.put` | 39 | 与 item model 合计 |
| 附加流体桶 | `EXTRA_SOURCES` 排序生成 | 17 | 与 item model 合计 |
| 物品合计 | 固定 + 方块变种 + 附加桶 | **110** | 110 个 item model，ID 集合无重复 |
| 流体类型/源/流动 | 固定 3 + 附加 17；流动名为 `<name>_flowing` | 20 / 20 / 20 | 由 `TFFluids` 动态规则闭合 |
| 方块实体类型 | `TFBlockEntities.GENERIC` | 1 | `foundry_device` 覆盖目标设备清单 |
| 实体类型 | `TFEntities.FLUID_CANNON_PROJECTILE` | 1 | `fluid_spit` |
| 菜单类型 | `TFMenus.FOUNDRY` | 1 | `foundry` |
| 配方类型/序列化器 | `TFRecipes` | 8 / 8 | §4 明列，未新增 alloying 数据 |
| 创造标签页 | `TinkerFoundry.FOUNDRY_TAB` | 1 | `foundry` |

固定物品 54、方块变种 39、附加桶 17 的集合交叉核对结果为 110 个不重复 ID；固定铁/金/铜桶由 `bucket(name, ...)` 规则生成 `<name>_bucket`，不能把 helper 的 `name` 参数误当成最终注册 ID。这与 T0.5 manifest 和 1211/2612 两树的核对结果一致。`TFDataComponents` 不计入 1.20.1 注册数量。

### 3.2 资源 schema 和引用闭合

- `assets/tinker_foundry/models/item/`：110 个模型；没有新版 `items/` 根 schema。
- `assets/tinker_foundry/blockstates/`：97 个；`models/block/`：170 个；blockstate model 引用缺失 0。
- 目标命名空间的 model parent 和 texture 引用缺失 0；blockstate 顶层格式错误 0。
- 资源 JSON 总数 744；用 .NET `System.Text.Json.JsonDocument` 静态解析失败 0。
- PowerShell 旧版 `ConvertFrom-Json` 对 `crafting/scorched_bricks_fence.json` 的 `B`/`b` 大小写键报兼容性错误，但该文件由 `System.Text.Json` 验证为合法 JSON；这不是资源错误。
- `data/tinker_foundry/recipes/` 和 `data/tinker_foundry/loot_tables/` 使用复数目录；目标不存在单数 `recipe/` 或 `loot_table/` 文件。
- `pack.mcmeta` 使用 `pack_format: 15`；tags 57 个、值 171 个；loot tables 77 个；damage type 2 个。

### 3.3 文件所有权

PG-1 只接管审计证据和进度回写，不接管各业务目录。矩阵中的父子目录排除和串行交接继续有效：

| 范围 | 当前 owner | PG-1 处理 |
| --- | --- | --- |
| `registry/`、`block/`、`item/`、`common/`、`recipe/`、`network/`、`debug/` 等业务源码 | PC/PD/PE 对应 owner；阶段已串行交接 | 只读扫描；本轮仅对 `FoundryCoreTest.java` 和 `FoundryCapabilityUtil.java` 做无语义过时说明清理 |
| `assets/`、`data/`、`lang/` | PF/PD 对应 owner | 只读扫描；不新增、不删除业务资源 |
| `README.md`、`NOTICE.md` | 项目文档 | 清除与 1.20.1 Forge 目标冲突的版本/协议说明 |
| `docs/1201-forge-final-static-audit.md` | PG-1/Luna | 本文件独占新增 |
| 四份阶段文档、API evidence、contract manifest、resource audit | PG-1 串行回写 | 只在前序交付已交接后更新状态、交叉引用和审计结果 |

没有并发编辑同一业务目录；参考工作树和 1201 Forge 模板未写入。

## 4. 配方、网络、语言和命令闭合

### 4.1 配方

目标 `TFRecipes` 明确注册以下 8 个自定义类型及同名序列化器：

1. `tinker_foundry:melting`
2. `tinker_foundry:ore_melting`
3. `tinker_foundry:damageable_melting`
4. `tinker_foundry:entity_melting`
5. `tinker_foundry:alloying`
6. `tinker_foundry:casting`
7. `tinker_foundry:molding`
8. `tinker_foundry:fuel`

实际 JSON 统计为 220 个：117 个自定义类型（casting 25、damageable_melting 12、entity_melting 29、fuel 3、melting 19、molding 16、ore_melting 13），103 个普通 Minecraft 配方（crafting_shaped 76、crafting_shapeless 7、smelting 6、stonecutting 14）。其中 `alloying` 注册入口存在，但 `data/tinker_foundry/recipes/` 中 `tinker_foundry:alloying` JSON 为 **0**；指南章节不能作为配方来源，禁止臆造。

### 4.2 网络和状态边界

- 通道：`tinker_foundry:main`，协议值字符串 `"6"`；消息注册顺序为状态快照 ID 0、结构错误 ID 1。
- `FoundryStatePayload` 字段顺序：`BlockPos`、主流体、燃料流体、合金输入数量/流体列表、结构流体层数量/流体列表、进度、处理时长、浇注容量、结构容量、结构有效标志、热量数组长度/值。
- 流体存在标志、`FluidStack.writeToPacket/readFromPacket`、VarInt 非负边界和流体 NBT 16 KiB 上限均已静态核对；合金输入受 `MAX_ALLOY_INPUTS`，结构层受 `MAX_LAYERS`，热量长度受 `MAX_STRUCTURE_INPUTS * 5` 与 5 的整除约束。
- `FoundryStructureErrorPayload` 顺序：控制器位置、错误位置存在标志及位置、`StructureErrorReason.ordinal()`；`StructureErrorReason` 0..7 映射和越界回退见 manifest §6.3。
- 这些是本模组 1.20.1 Forge 内部逻辑契约，不承诺与 1.21.1/26.1.2 的旧字节流互通。

### 4.3 语言和命令

- `en_us.json` 和 `zh_cn.json` 都是 297 个 key，英文独有 0、中文独有 0；`gui.tinker_foundry.unit.%s` 是运行时格式化前缀，不是固定遗漏 key。
- `/tinker_foundry debug generate smeltery|foundry|alloyer|all` 由 Forge `RegisterCommandsEvent` 注册，根节点使用 `source.hasPermission(2)`；生成逻辑包含控制器、墙体、燃料罐、排液/浇注设备、盆/台和预装流体。

## 5. API 证据与明确未决项

### 5.1 已有证据

`docs/1201-forge-api-evidence.md` 已记录并由本轮交叉检查以下精确证据：

- Forge 47.4.23 的 `DeferredRegister`、`RegistryObject`、`ForgeCapabilities`、`IFluidHandler`、`FluidStack`、`BlockEntity`、`AbstractContainerMenu`、`SimpleChannel`、`FriendlyByteBuf`、Forge 客户端流体扩展、命令事件和 JEI 15.49.0.191 Forge API。
- 目标 `PortableTankFluidHandler` 使用 `ItemStack` NBT 键 `Fluid`；目标 `FoundryBlockEntity` 使用 Forge `getCapability(Capability, Direction)`、`load(CompoundTag)` 和 `saveAdditional(CompoundTag)`。
- 目标配方使用 1.20.1 `Recipe`、`RecipeSerializer`、`RecipeType`、`FriendlyByteBuf` 和 `recipes/` 路径；没有把 1.21/2612 的 `RecipeInput`、`Codec`、`StreamCodec`、`RecipeHolder` 作为目标 API。

### 5.2 未决项

1. 尚未运行构建、客户端、资源加载、JEI 显示、书籍展示、掉落行为或 GameTest；T6.2/T6.3 必须等用户明确授权。
2. book 的 22 个翻译 key 和图片逐张来源/许可未决，不纳入最终完成声明。
3. `alloying` 配方仍无来源数据，不新增 JSON；合金催化匹配只有源码静态证据，未做运行时验证。
4. Git 工作树保持既有大量迁移改动；本轮不清理、不回退、不提交、不推送。

## 6. PG-1 结论

PG-1/T6.1 已完成：目标 1.20.1 Forge 的 NeoForge/2612 实际残留、注册/资源/配方/网络/语言/命令闭合、API 证据和文件所有权均已静态记录。Gate G 只能表示“静态审计完成，等待用户授权验证”，不能表示构建或运行通过。book 的 22 个翻译 key、图片许可和 `alloying` 0 条来源事实继续保持未决/不新增。
