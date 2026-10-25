# 冶炼多方块与匠魂冶炼体系迁移对照

核对日期：2026-09-21。目标工作树是 Minecraft 1.20.1 Forge 47.4.23、Java 17、官方映射的 `1201forge` 分支。本文件记录 Tinker Foundry 将匠魂冶炼流程迁移为独立模组时的范围、静态对照结果和未覆盖项；它不是运行验证报告，也不把资源或代码静态闭合写成可构建、可启动或可玩。

## 1. 范围与审计边界

本项目只迁移独立的熔炼、合金、浇筑和流体设备体系，保留本项目命名空间下的注册 ID、配方 ID、网络逻辑语义、方块实体保存语义和数据资源路径。上游匠魂源码只作为冶炼行为和资源含义的对照依据；目标实现必须使用 1.20.1 Forge API，不把其他 Minecraft 版本或其他加载器的 API 当作目标代码。

本轮只做静态审计和文档记录，边界如下：

- 未运行 Gradle、`compileJava`、`build`、`runClient`、GameTest 或其他运行验证。
- 静态闭合不证明界面布局、液柱连续渲染、浇筑交互、容器传输、配方加载或 JEI 页面在游戏内正确。
- 未新增无来源的 `tinker_foundry:alloying` 配方 JSON。合金配方注册入口可以存在，但当前数据清单中的合金 JSON 数量为 0。
- 不纳入匠魂工具、材料词条、Modifier、工具部件、匠魂书或旧存档兼容；本项目自己的独立指南资源是否随最终模组发布仍受图片许可和翻译完整度约束。

## 2. 当前迁移范围的静态结果

### 2.1 动态输入库存与冶炼炉界面

目标源码中保留了按冶炼结构规模计算输入槽位的动态路径，而不是固定为单一槽数。静态核对到的行为契约包括：

- 控制器根据有效炉腔规模计算输入槽数量，菜单使用动态槽表；列数按槽位数量分组，界面在主面板高度内安排可见行。
- 滚轮和滑轨只改变显示窗口，真实槽号保持稳定；不存在的末行位置使用无槽背景，不应伪装成可点击的空槽。
- 单个输入槽限制一个物品堆栈；扩容保留已有前缀，缩容需要在服务端处理超出范围的物品。
- 菜单快捷移动必须直接遍历真实容器，而不是操作临时副本；状态同步需要携带动态槽数和逐槽热量。
- 结构规模变化时，当前代码采用关闭旧菜单、处理临时桶槽并重新打开的安全路径。这与上游热更新界面是否逐像素一致仍未做运行时验证。

相关静态日志标记为 `[inventory-resize]`、`[inventory-scroll]`、`[screen-layout]` 和 `[inventory-sync]`；日志存在不等于对应游戏交互已经验收。

### 2.2 燃料、容量和容器处理

静态代码审计记录了以下迁移范围：

- 燃料配方包含加热速率，结构处理周期和逐步冷却逻辑保留；熔岩的速率语义固定为 10。
- 结构控制器使用结构登记的燃料储罐，不借用旁边独立加热器；无有效热源时按处理周期降温，而不是无条件立刻清零。
- 冶炼炉不额外添加 4000 mB 的保底容量；铸造炉容量按目标结构尺寸和底板规则计算。
- 桶槽处理保留服务端重试、容量不足时保留输入和模式切换后的重试路径；无桶形态的流体不能直接按普通空桶提取。

上述内容目前只有源码和数据路径证据，尚未通过连续右键、持续液柱、容器自动化或多人同步的游戏内验证。

### 2.3 注册方块、流体和资源闭合

PG-1 静态盘点得到以下数量，均以目标工作树中的注册入口和资源清单为依据：

| 对象 | 静态数量 | 说明 |
| --- | ---: | --- |
| 普通静态注册方块 | 77 | 对应方块状态、模型、物品模型、语言和掉落表闭合检查 |
| 流体方块 | 20 | 固定 3 个加 `EXTRA_COLORS` 17 个动态颜色；与普通方块合计 97 个 block ID |
| 物品 | 110 | 固定物品 54、方块变种 39、附加流体桶 17；ID 无重复 |
| 流体 type/source/flowing | 20 / 20 / 20 | 动态颜色按同一生成规则闭合 |
| 方块实体、实体、菜单、创造标签页 | 1 / 1 / 1 / 1 | 注册入口均已静态盘点 |

普通方块的掉落表使用 `data/tinker_foundry/loot_tables/` 复数目录；配方使用 `data/tinker_foundry/recipes/` 复数目录。目标资源审计没有把单数 `recipe/` 或 `loot_table/` 作为有效文件目录，也没有使用目标版本之外的物品模型 schema。

### 2.4 冶炼、合金、浇筑和普通配方

目标注册入口明确包含以下 8 个自定义配方 type 和同名 serializer：

1. `tinker_foundry:melting`
2. `tinker_foundry:ore_melting`
3. `tinker_foundry:damageable_melting`
4. `tinker_foundry:entity_melting`
5. `tinker_foundry:alloying`
6. `tinker_foundry:casting`
7. `tinker_foundry:molding`
8. `tinker_foundry:fuel`

静态文件统计为 220 个配方：117 个自定义类型（`casting` 25、`damageable_melting` 12、`entity_melting` 29、`fuel` 3、`melting` 19、`molding` 16、`ore_melting` 13），以及 103 个普通 Minecraft 配方（`crafting_shaped` 76、`crafting_shapeless` 7、`smelting` 6、`stonecutting` 14）。普通 Minecraft 配方和 Tinker Foundry 自定义 serializer 分开统计，不能把两者混称为“七类配方以及 fuel”。

`alloying` 的 type/serializer 注册存在，但当前 `data/tinker_foundry/recipes/` 中对应 JSON 为 0。指南章节、注册名或旧版本代码都不能作为合金配方来源，因此本轮不补写合金数据。

### 2.5 多方块结构与浇筑流程

当前迁移范围包含冶炼炉、铸造炉、合金炉、熔化炉、加热器、燃料罐、排液/输送/浇注设备、浇注盆和浇注台，以及结构合法性、流体层、燃料、输出占用和菜单状态等静态逻辑。排液孔、输导孔和滑槽的方向资源与处理逻辑已经纳入资源审计；浇注口、流体炮和流体计保留其自身方向规则。

以下内容仍不能从静态闭合推出完全一致：冶炼炉和铸造炉的角框、墙体、底板及标签组合；结构尺寸边界；焦黑灌浇道与灌浇口的间隔；产物生成后的重复浇筑保护；多层液体的连续液柱形状、液面高度和视角剔除；以及不同第三方流体管道的自动化策略。

## 3. 保留项与明确排除项

### 3.1 保留项

- 熔化器、冶炼炉、铸造炉、合金炉、加热器、燃料罐、便携罐和流体设备。
- 流体输入、输出、流体容器交互、方块实体容量和状态同步。
- 熔炼、矿物熔炼、耐久物品熔炼、实体熔炼、浇筑、模具和燃料配方。
- 多方块结构检测、动态输入槽、热量、结构流体层、浇注容量和调试结构生成命令。
- 1.20.1 Forge 的物品 NBT/能力语义、方块实体保存读取、`SimpleChannel`/`FriendlyByteBuf` 网络语义、菜单同步和可选 JEI 适配。
- 英文和中文核心语言、方块状态、模型、掉落表、tags、配方和结构资源的静态闭合。

### 3.2 排除项和未决项

- 不迁移匠魂工具、工具部件、材料词条、Modifier、工具浇注或部件替换系统。
- 不把原版匠魂书作为本项目功能；`data/tinker_foundry/book/` 是本项目独立指南候选，不是匠魂书的迁移入口。
- 独立指南目前有 22 个翻译 key 尚未完成核对，图片来源和许可映射也未最终确认；因此 book 资源不纳入最终发布完成声明。章节内容不能作为合金配方来源。
- 不新增 `alloying` JSON；缺少可核查来源的合金催化组合保持未决。
- 不承诺旧存档、旧网络字节流或跨 Minecraft 版本网络互通；只保留本模组当前逻辑语义。
- 不以启动成功、资源静态解析或日志存在代替游戏内验证。液体渲染、连续排液、桶和储罐右键交互、焦黑浇注道间隔、重复浇筑、JEI 查看和第三方自动化仍待授权后的实机核验。

## 4. 静态审计发现的差异与未覆盖行为

下表把“已有静态路径”和“尚未覆盖”分开，避免把代码存在误写成行为通过：

| 领域 | 静态已核对 | 仍未覆盖或不能宣称通过 |
| --- | --- | --- |
| 结构合法性 | 控制器、结构方块、墙体/底板标签和多方块登记路径 | 各合法尺寸、角框组合、间隔规则和复杂结构的游戏内重建 |
| 物品加热 | fuel、melting、ore_melting、damageable_melting 注册及数据文件 | 不同燃料切换、点火相位、逐槽热量、冷却和容量边界的实机时序 |
| 浇筑 | casting/molding 类型、输出占用检查和产物保护代码路径 | 连续浇注液柱、浇注道间隔、重复右键、不同模具和世界液体外观 |
| 实体熔炼 | entity_melting 类型、数据文件和处理链路 | 实体受伤、吸入/输出和客户端交互 |
| 流体容器 | Forge 1.20.1 的 NBT/能力转换路径、桶/罐注册和容量字段 | 罐与罐、罐与桶、其他流体容器的所有方向右键组合 |
| 网络与菜单 | 两个状态载荷的字段顺序、边界和菜单同步入口 | 客户端/服务端实际同步、异常载荷拒绝和多人延迟场景 |
| JEI | 8 类自定义配方的静态适配入口与配方管理器查询路径 | JEI 精确运行版本下的每类配方页面、输入输出显示和搜索 |
| 资源 | `recipes/`、`loot_tables/`、tags、模型、语言和结构文件静态闭合 | 游戏资源重载、变体模型渲染、材质颜色和书籍发布许可 |

## 5. 证据位置

本文件的静态数字和契约引用以下目标工作树文档，文档之间的状态以它们的最新记录为准：

- [`docs/1201-forge-final-static-audit.md`](1201-forge-final-static-audit.md)：PG-1/T6.1 总静态审计、注册闭合、配方统计、网络边界和禁止运行验证的状态。
- [`docs/1201-forge-contract-manifest.md`](1201-forge-contract-manifest.md)：注册 ID、动态生成规则、保存/读取键、协议字段、FaucetState 和 book 资源清单。
- [`docs/1201-forge-resource-audit.md`](1201-forge-resource-audit.md)：1.20.1 Forge 资源目录、模型、语言、tags、掉落表和结构资源审计。
- [`docs/1201-forge-api-evidence.md`](1201-forge-api-evidence.md)：Forge 47.4.23、官方映射、能力、流体、物品容器、方块实体、菜单、网络、渲染、命令、配方和 JEI 的本地 API 证据。
- [`docs/1201-forge-alloying-data.md`](1201-forge-alloying-data.md)：`alloying` 来源清单为 0，明确禁止凭空添加 JSON。
- [`docs/1201-forge-requirements.md`](1201-forge-requirements.md)：产品范围、非目标、版本差异和阶段状态。

上游冶炼语义对照使用 1.20.1 Tinkers Construct 源码中的结构、菜单、库存、加热和浇注实现，例如：

- [HeatingStructureContainerMenu](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/menu/HeatingStructureContainerMenu.java)
- [MeltingModuleInventory](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/block/entity/module/MeltingModuleInventory.java)
- [SideInventoryScreen](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/tables/client/inventory/module/SideInventoryScreen.java)
- [SmelteryMultiblock](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/block/entity/multiblock/SmelteryMultiblock.java)
- [FoundryMultiblock](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/block/entity/multiblock/FoundryMultiblock.java)
- [MeltingModule](https://github.com/SlimeKnights/TinkersConstruct/blob/1.20.1/src/main/java/slimeknights/tconstruct/smeltery/block/entity/module/MeltingModule.java)

## 6. 当前结论

本文件现在只描述 1.20.1 Forge 迁移的静态范围和证据边界。注册、资源、配方、网络、语言、命令和所有权的静态结果已在 PG-1/T6.1 文档中记录；book 的 22 个翻译 key、图片许可、`alloying` 的 0 条来源数据以及所有运行时行为仍保持未决。任何构建、客户端启动、JEI 展示或玩家交互结论，必须在用户重新授权后单独验证，不能从本文件推导出来。
