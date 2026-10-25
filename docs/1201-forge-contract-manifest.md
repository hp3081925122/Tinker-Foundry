# Tinker Foundry 1.20.1 Forge 公开契约清单（T0.5）

核对日期：2026-09-21  
来源：

- 1211 master：E:\模组\发cf的模组\Tinker Foundry\Tinker Foundry，HEAD 1102c56
- 2612 dirty：E:\模组\发cf的模组\Tinker Foundry\26.1.2-neoforge-tinker-foundry，HEAD 1102c56
- 目标：E:\模组\发cf的模组\Tinker Foundry\1.20.1-forge-tinker-foundry

本清单完成 T0.5 的逐项注册、协议、持久化和 book 静态冻结工作，主对话已于 **2026-09-21 审核通过 Gate A、Gate B**。PB-2 已另行完成 1.20.1 Forge API 静态证据，见 `1201-forge-api-evidence.md`；本清单不把 API 证据和业务迁移混为一谈。下文表格中的 ID 均是本项目本地 ID，完整命名空间形式为 `tinker_foundry:<ID>`；1211 与 2612 的注册入口均使用命名空间 `tinker_foundry`。

逐项差异判定采用以下口径：`ID 集合无变化` 表示 2612 没有对该注册表公开新增或删除 ID；`实现改动` 表示 dirty worktree 修改了注册 helper、对象构造或相关版本 API，不能直接复制到 Forge；`来源保持` 表示本轮对照未发现该注册入口的公开语义变化。下文每个注册行均按所属小节的差异判定继承，不把实现改动误写成 ID 新增。

## 1. 注册数量总表

“数量”按实际注册对象计数；流体类型、源流体、流动流体和液体方块分别计入各自注册表。来源 `TFDataComponents.FLUID` 是 1.21.1 数据组件注册，不计入 1.20.1 目标实际注册；目标 `TFDataComponents.FLUID_TAG` 只是 `ItemStack` NBT 键字符串。

| 注册表 | 1211 | 2612 | 1201 策略 | 来源入口 |
| --- | ---: | ---: | --- | --- |
| 方块：TFBlocks 静态 | 77 | 77 | 全部纳入，ID 不变 | registry/TFBlocks.java |
| 方块：流体方块动态 | 20 | 20 | 全部纳入，按生成规则重建 | registry/TFFluids.java |
| 方块合计 | 97 | 97 | 纳入 | TFBlocks.java、TFFluids.java |
| 物品：固定声明 | 54 | 54 | 全部纳入，ID 不变 | registry/TFItems.java |
| 物品：方块变种动态 | 39 | 39 | 全部纳入，按 items.put 顺序重建 | registry/TFItems.java |
| 物品：附加流体桶动态 | 17 | 17 | 全部纳入，按 EXTRA_SOURCES 规则重建 | TFItems.java、TFFluids.java |
| 物品合计 | 110 | 110 | 纳入 | TFItems.java |
| 流体类型 | 20 | 20 | 全部纳入，ID 不变 | registry/TFFluids.java |
| 源流体 | 20 | 20 | 全部纳入，ID 不变 | registry/TFFluids.java |
| 流动流体 | 20 | 20 | 全部纳入，ID 不变 | registry/TFFluids.java |
| 方块实体类型 | 1 | 1 | 纳入 foundry_device | registry/TFBlockEntities.java |
| 实体类型 | 1 | 1 | 纳入 fluid_spit | registry/TFEntities.java |
| 菜单类型 | 1 | 1 | 纳入 foundry | registry/TFMenus.java |
| 配方类型 | 8 | 8 | 8 个全部纳入 | registry/TFRecipes.java |
| 配方序列化器 | 8 | 8 | 8 个全部纳入 | registry/TFRecipes.java |
| 创造模式标签页 | 1 | 1 | 纳入 foundry | TinkerFoundry.java |
| 数据组件类型 | 1（来源） | 1（来源） | 不直接注册；保留便携罐流体持久化语义 | TFDataComponents.java |

静态入口共有 11 类：BLOCKS、ITEMS、FLUID_TYPES、FLUIDS、BLOCK_ENTITY_TYPES、ENTITY_TYPES、RECIPE_TYPES、RECIPE_SERIALIZERS、MENUS、CREATIVE_MODE_TABS 和来源专用 TFDataComponents.REGISTER。1211 与 2612 没有发现公开注册 ID 的新增或删除；2612 的差异是注册 API、动态能力调用、调试命令和配方同步行为。

数量表的 2612 差异逐类冻结为：TFBlocks、TFItems、TFFluids 的注册 helper/API 有实现改动但 ID 集合无变化；TFBlockEntities、TFEntities、TFMenus、创造标签页的注册实现/API 有版本改动但 ID 集合无变化；TFRecipes 的 serializer/type 实现有改动但 8 个 type 和 8 个 serializer ID 均无变化；TFDataComponents 是来源项，1201 不创建对应注册表。该判定适用于下文各表的每一个 ID 行。

## 2. 方块注册清单

下表每行均为一个 BLOCKS 注册 ID。来源列格式为 1211 行 / 2612 行；两个版本此处行号相同，注册 helper 实现不同但 ID 相同。

本表 77 行的 2612 差异统一为：两版本均存在，公开 ID 无新增/删除；TFBlocks 注册 helper/API 有实现改动；1201 全部纳入并保持 `tinker_foundry:<ID>`。

| ID | 来源行 | 1211/2612 | 1201 策略 |
| --- | --- | --- | --- |
| grout | TFBlocks.java:33 / 33 | 有 / 有 | 纳入，ID 不变 |
| nether_grout | TFBlocks.java:35 / 35 | 有 / 有 | 纳入，ID 不变 |
| seared_stone | TFBlocks.java:38 / 38 | 有 / 有 | 纳入，ID 不变 |
| seared_stone_stairs | TFBlocks.java:39 / 39 | 有 / 有 | 纳入，ID 不变 |
| seared_stone_slab | TFBlocks.java:40 / 40 | 有 / 有 | 纳入，ID 不变 |
| seared_cobble | TFBlocks.java:42 / 42 | 有 / 有 | 纳入，ID 不变 |
| seared_cobble_stairs | TFBlocks.java:43 / 43 | 有 / 有 | 纳入，ID 不变 |
| seared_cobble_slab | TFBlocks.java:44 / 44 | 有 / 有 | 纳入，ID 不变 |
| seared_cobble_wall | TFBlocks.java:45 / 45 | 有 / 有 | 纳入，ID 不变 |
| seared_paver | TFBlocks.java:47 / 47 | 有 / 有 | 纳入，ID 不变 |
| seared_paver_stairs | TFBlocks.java:48 / 48 | 有 / 有 | 纳入，ID 不变 |
| seared_paver_slab | TFBlocks.java:49 / 49 | 有 / 有 | 纳入，ID 不变 |
| seared_bricks | TFBlocks.java:51 / 51 | 有 / 有 | 纳入，ID 不变 |
| seared_bricks_stairs | TFBlocks.java:52 / 52 | 有 / 有 | 纳入，ID 不变 |
| seared_bricks_slab | TFBlocks.java:53 / 53 | 有 / 有 | 纳入，ID 不变 |
| seared_bricks_wall | TFBlocks.java:54 / 54 | 有 / 有 | 纳入，ID 不变 |
| seared_cracked_bricks | TFBlocks.java:56 / 56 | 有 / 有 | 纳入，ID 不变 |
| seared_fancy_bricks | TFBlocks.java:57 / 57 | 有 / 有 | 纳入，ID 不变 |
| seared_triangle_bricks | TFBlocks.java:58 / 58 | 有 / 有 | 纳入，ID 不变 |
| seared_glass | TFBlocks.java:60 / 60 | 有 / 有 | 纳入，ID 不变 |
| seared_lantern | TFBlocks.java:62 / 62 | 有 / 有 | 纳入，ID 不变 |
| seared_ladder | TFBlocks.java:64 / 64 | 有 / 有 | 纳入，ID 不变 |
| seared_lamp | TFBlocks.java:66 / 66 | 有 / 有 | 纳入，ID 不变 |
| scorched_stone | TFBlocks.java:68 / 68 | 有 / 有 | 纳入，ID 不变 |
| polished_scorched_stone | TFBlocks.java:69 / 69 | 有 / 有 | 纳入，ID 不变 |
| scorched_bricks | TFBlocks.java:70 / 70 | 有 / 有 | 纳入，ID 不变 |
| scorched_bricks_stairs | TFBlocks.java:71 / 71 | 有 / 有 | 纳入，ID 不变 |
| scorched_bricks_slab | TFBlocks.java:72 / 72 | 有 / 有 | 纳入，ID 不变 |
| scorched_bricks_fence | TFBlocks.java:73 / 73 | 有 / 有 | 纳入，ID 不变 |
| scorched_road | TFBlocks.java:74 / 74 | 有 / 有 | 纳入，ID 不变 |
| scorched_road_stairs | TFBlocks.java:75 / 75 | 有 / 有 | 纳入，ID 不变 |
| scorched_road_slab | TFBlocks.java:76 / 76 | 有 / 有 | 纳入，ID 不变 |
| chiseled_scorched_bricks | TFBlocks.java:77 / 77 | 有 / 有 | 纳入，ID 不变 |
| scorched_glass | TFBlocks.java:79 / 79 | 有 / 有 | 纳入，ID 不变 |
| scorched_lantern | TFBlocks.java:81 / 81 | 有 / 有 | 纳入，ID 不变 |
| scorched_ladder | TFBlocks.java:83 / 83 | 有 / 有 | 纳入，ID 不变 |
| scorched_lamp | TFBlocks.java:85 / 85 | 有 / 有 | 纳入，ID 不变 |
| seared_glass_pane | TFBlocks.java:88 / 88 | 有 / 有 | 纳入，ID 不变 |
| seared_tinted_glass | TFBlocks.java:89 / 89 | 有 / 有 | 纳入，ID 不变 |
| seared_soul_glass | TFBlocks.java:90 / 90 | 有 / 有 | 纳入，ID 不变 |
| seared_soul_glass_pane | TFBlocks.java:91 / 91 | 有 / 有 | 纳入，ID 不变 |
| scorched_glass_pane | TFBlocks.java:92 / 92 | 有 / 有 | 纳入，ID 不变 |
| scorched_tinted_glass | TFBlocks.java:93 / 93 | 有 / 有 | 纳入，ID 不变 |
| scorched_soul_glass | TFBlocks.java:94 / 94 | 有 / 有 | 纳入，ID 不变 |
| scorched_soul_glass_pane | TFBlocks.java:95 / 95 | 有 / 有 | 纳入，ID 不变 |
| smeltery_controller | TFBlocks.java:98 / 98 | 有 / 有 | 纳入，ID 不变 |
| foundry_controller | TFBlocks.java:100 / 100 | 有 / 有 | 纳入，ID 不变 |
| seared_melter | TFBlocks.java:102 / 102 | 有 / 有 | 纳入，ID 不变 |
| seared_heater | TFBlocks.java:104 / 104 | 有 / 有 | 纳入，ID 不变 |
| scorched_alloyer | TFBlocks.java:106 / 106 | 有 / 有 | 纳入，ID 不变 |
| seared_fuel_tank | TFBlocks.java:108 / 108 | 有 / 有 | 纳入，ID 不变 |
| scorched_fuel_tank | TFBlocks.java:110 / 110 | 有 / 有 | 纳入，ID 不变 |
| seared_casting_tank | TFBlocks.java:112 / 112 | 有 / 有 | 纳入，ID 不变 |
| seared_channel | TFBlocks.java:114 / 114 | 有 / 有 | 纳入，ID 不变 |
| scorched_channel | TFBlocks.java:115 / 115 | 有 / 有 | 纳入，ID 不变 |
| seared_drain | TFBlocks.java:117 / 117 | 有 / 有 | 纳入，ID 不变 |
| scorched_drain | TFBlocks.java:118 / 118 | 有 / 有 | 纳入，ID 不变 |
| seared_duct | TFBlocks.java:119 / 119 | 有 / 有 | 纳入，ID 不变 |
| scorched_duct | TFBlocks.java:120 / 120 | 有 / 有 | 纳入，ID 不变 |
| seared_chute | TFBlocks.java:121 / 121 | 有 / 有 | 纳入，ID 不变 |
| scorched_chute | TFBlocks.java:122 / 122 | 有 / 有 | 纳入，ID 不变 |
| seared_faucet | TFBlocks.java:123 / 123 | 有 / 有 | 纳入，ID 不变 |
| scorched_faucet | TFBlocks.java:124 / 124 | 有 / 有 | 纳入，ID 不变 |
| seared_table | TFBlocks.java:126 / 126 | 有 / 有 | 纳入，ID 不变 |
| scorched_table | TFBlocks.java:127 / 127 | 有 / 有 | 纳入，ID 不变 |
| seared_basin | TFBlocks.java:128 / 128 | 有 / 有 | 纳入，ID 不变 |
| scorched_basin | TFBlocks.java:129 / 129 | 有 / 有 | 纳入，ID 不变 |
| scorched_proxy_tank | TFBlocks.java:131 / 131 | 有 / 有 | 纳入，ID 不变 |
| seared_fluid_cannon | TFBlocks.java:133 / 133 | 有 / 有 | 纳入，ID 不变 |
| copper_gauge | TFBlocks.java:135 / 135 | 有 / 有 | 纳入，ID 不变 |
| obsidian_gauge | TFBlocks.java:136 / 136 | 有 / 有 | 纳入，ID 不变 |
| seared_ingot_gauge | TFBlocks.java:138 / 138 | 有 / 有 | 纳入，ID 不变 |
| scorched_ingot_gauge | TFBlocks.java:139 / 139 | 有 / 有 | 纳入，ID 不变 |
| seared_fuel_gauge | TFBlocks.java:141 / 141 | 有 / 有 | 纳入，ID 不变 |
| scorched_fuel_gauge | TFBlocks.java:142 / 142 | 有 / 有 | 纳入，ID 不变 |
| seared_ingot_tank | TFBlocks.java:144 / 144 | 有 / 有 | 纳入，ID 不变 |
| scorched_ingot_tank | TFBlocks.java:145 / 145 | 有 / 有 | 纳入，ID 不变 |

### 2.1 流体方块动态清单

固定三种流体在 TFFluids.java:47-49 调用 registerFluidBlock(name, source)；附加流体由 TFFluids.java:65-83 的 EXTRA_COLORS 键，经 TFFluids.java:97-99 的排序循环调用同一 helper 生成。每行四个 ID 都是独立注册对象。

本表 20 行的 2612 差异统一为：两版本均存在，公开 ID 无新增/删除；TFFluids 的 fluid type/source/flowing/fluid-block helper/API 有实现改动；1201 按同一动态规则全部纳入。

| 名称键 | 流体类型 ID | 源/流动 ID | 液体方块 ID | 1211/2612 | 1201 策略 |
| --- | --- | --- | --- | --- | --- |
| iron | iron | iron / iron_flowing | iron_fluid | 有 / 有 | 纳入 |
| gold | gold | gold / gold_flowing | gold_fluid | 有 / 有 | 纳入 |
| copper | copper | copper / copper_flowing | copper_fluid | 有 / 有 | 纳入 |
| cobalt | cobalt | cobalt / cobalt_flowing | cobalt_fluid | 有 / 有 | 纳入 |
| liquid_soul | liquid_soul | liquid_soul / liquid_soul_flowing | liquid_soul_fluid | 有 / 有 | 纳入 |
| honey | honey | honey / honey_flowing | honey_fluid | 有 / 有 | 纳入 |
| blazing_blood | blazing_blood | blazing_blood / blazing_blood_flowing | blazing_blood_fluid | 有 / 有 | 纳入 |
| molten_glass | molten_glass | molten_glass / molten_glass_flowing | molten_glass_fluid | 有 / 有 | 纳入 |
| ender | ender | ender / ender_flowing | ender_fluid | 有 / 有 | 纳入 |
| seared_stone | seared_stone | seared_stone / seared_stone_flowing | seared_stone_fluid | 有 / 有 | 纳入 |
| scorched_stone | scorched_stone | scorched_stone / scorched_stone_flowing | scorched_stone_fluid | 有 / 有 | 纳入 |
| molten_clay | molten_clay | molten_clay / molten_clay_flowing | molten_clay_fluid | 有 / 有 | 纳入 |
| molten_emerald | molten_emerald | molten_emerald / molten_emerald_flowing | molten_emerald_fluid | 有 / 有 | 纳入 |
| molten_diamond | molten_diamond | molten_diamond / molten_diamond_flowing | molten_diamond_fluid | 有 / 有 | 纳入 |
| magma | magma | magma / magma_flowing | magma_fluid | 有 / 有 | 纳入 |
| meat_soup | meat_soup | meat_soup / meat_soup_flowing | meat_soup_fluid | 有 / 有 | 纳入 |
| potion | potion | potion / potion_flowing | potion_fluid | 有 / 有 | 纳入 |
| milk | milk | milk / milk_flowing | milk_fluid | 有 / 有 | 纳入 |
| slime | slime | slime / slime_flowing | slime_fluid | 有 / 有 | 纳入 |
| venom | venom | venom / venom_flowing | venom_fluid | 有 / 有 | 纳入 |

## 3. 物品注册清单

### 3.1 固定声明物品

来源均为 registry/TFItems.java；固定声明 1211/2612 的 ID 集合一致。1211 和 2612 各有 54 条 `public static final DeferredItem` 声明；iron、gold、copper 通过 bucket(name, ...) 实际注册为带 _bucket 后缀的 ID。表格与源码名单静态比对无遗漏、无重复。

本表 54 行的 2612 差异统一为：两版本均存在，公开 ID 无新增/删除；TFItems 固定注册 helper/API 有实现改动；1201 全部纳入。

| ID | 来源行（1211 / 2612） | 1211/2612 | 1201 策略 |
| --- | --- | --- | --- |
| grout | 17 / 17 | 有 / 有 | 纳入 |
| nether_grout | 19 / 19 | 有 / 有 | 纳入 |
| seared_brick | 21 / 21 | 有 / 有 | 纳入 |
| seared_glass | 23 / 23 | 有 / 有 | 纳入 |
| seared_lantern | 25 / 25 | 有 / 有 | 纳入 |
| seared_ladder | 27 / 27 | 有 / 有 | 纳入 |
| scorched_brick | 29 / 29 | 有 / 有 | 纳入 |
| scorched_glass | 31 / 31 | 有 / 有 | 纳入 |
| scorched_lantern | 33 / 33 | 有 / 有 | 纳入 |
| scorched_ladder | 35 / 35 | 有 / 有 | 纳入 |
| smeltery_controller | 37 / 37 | 有 / 有 | 纳入 |
| foundry_controller | 39 / 39 | 有 / 有 | 纳入 |
| seared_melter | 41 / 41 | 有 / 有 | 纳入 |
| seared_heater | 43 / 43 | 有 / 有 | 纳入 |
| scorched_alloyer | 45 / 45 | 有 / 有 | 纳入 |
| seared_ingot_tank | 47 / 47 | 有 / 有 | 纳入 |
| scorched_ingot_tank | 48 / 48 | 有 / 有 | 纳入 |
| seared_fuel_tank | 50 / 50 | 有 / 有 | 纳入 |
| scorched_fuel_tank | 52 / 52 | 有 / 有 | 纳入 |
| seared_casting_tank | 54 / 54 | 有 / 有 | 纳入 |
| copper_nugget | 56 / 56 | 有 / 有 | 纳入 |
| seared_table | 58 / 58 | 有 / 有 | 纳入 |
| scorched_table | 60 / 60 | 有 / 有 | 纳入 |
| seared_basin | 62 / 62 | 有 / 有 | 纳入 |
| scorched_basin | 64 / 64 | 有 / 有 | 纳入 |
| seared_drain | 66 / 66 | 有 / 有 | 纳入 |
| scorched_drain | 68 / 68 | 有 / 有 | 纳入 |
| seared_faucet | 70 / 70 | 有 / 有 | 纳入 |
| scorched_faucet | 72 / 72 | 有 / 有 | 纳入 |
| seared_duct | 74 / 74 | 有 / 有 | 纳入 |
| scorched_duct | 76 / 76 | 有 / 有 | 纳入 |
| seared_chute | 78 / 78 | 有 / 有 | 纳入 |
| scorched_chute | 80 / 80 | 有 / 有 | 纳入 |
| copper_gauge | 82 / 82 | 有 / 有 | 纳入 |
| obsidian_gauge | 84 / 84 | 有 / 有 | 纳入 |
| seared_ingot_gauge | 86 / 86 | 有 / 有 | 纳入 |
| scorched_ingot_gauge | 88 / 88 | 有 / 有 | 纳入 |
| seared_fuel_gauge | 90 / 90 | 有 / 有 | 纳入 |
| scorched_fuel_gauge | 92 / 92 | 有 / 有 | 纳入 |
| seared_fluid_cannon | 94 / 94 | 有 / 有 | 纳入 |
| scorched_proxy_tank | 96 / 96 | 有 / 有 | 纳入 |
| ingot_cast | 99 / 99 | 有 / 有 | 纳入 |
| nugget_cast | 101 / 101 | 有 / 有 | 纳入 |
| ingot_sand_cast | 103 / 103 | 有 / 有 | 纳入 |
| nugget_sand_cast | 105 / 105 | 有 / 有 | 纳入 |
| ingot_red_sand_cast | 107 / 107 | 有 / 有 | 纳入 |
| nugget_red_sand_cast | 109 / 109 | 有 / 有 | 纳入 |
| gem_cast | 111 / 111 | 有 / 有 | 纳入 |
| gem_sand_cast | 113 / 113 | 有 / 有 | 纳入 |
| gem_red_sand_cast | 115 / 115 | 有 / 有 | 纳入 |
| iron_bucket | 118 / 118 | 有 / 有 | 纳入 |
| gold_bucket | 120 / 120 | 有 / 有 | 纳入 |
| copper_bucket | 122 / 122 | 有 / 有 | 纳入 |
| copper_canister | 125 / 125-126 | 有 / 有 | 纳入；保留流体持久化语义 |

### 3.2 方块变种动态物品

每行是一个 items.put(id, block(id, TFBlocks...)) 注册；master 行号 / dirty 行号如下。动态规则本身位于 master TFItems.java:155-196、dirty TFItems.java:159-200。

本表 39 行的 2612 差异统一为：两版本均存在，公开 ID 无新增/删除；`items.put` 的注册 helper/API 有实现改动；1201 按逐项 ID 全部纳入。

物品 ID 交叉核对：固定声明 54 + 方块变种 39 + 附加流体桶 17 = 110；三组集合交集为空，1211/2612 两树的集合完全一致。

| ID | 来源行（1211 / 2612） | 1211/2612 | 1201 策略 |
| --- | --- | --- | --- |
| seared_lamp | 157 / 161 | 有 / 有 | 纳入 |
| seared_stone | 158 / 162 | 有 / 有 | 纳入 |
| seared_stone_stairs | 159 / 163 | 有 / 有 | 纳入 |
| seared_stone_slab | 160 / 164 | 有 / 有 | 纳入 |
| seared_cobble | 161 / 165 | 有 / 有 | 纳入 |
| seared_cobble_stairs | 162 / 166 | 有 / 有 | 纳入 |
| seared_cobble_slab | 163 / 167 | 有 / 有 | 纳入 |
| seared_cobble_wall | 164 / 168 | 有 / 有 | 纳入 |
| seared_paver | 165 / 169 | 有 / 有 | 纳入 |
| seared_paver_stairs | 166 / 170 | 有 / 有 | 纳入 |
| seared_paver_slab | 167 / 171 | 有 / 有 | 纳入 |
| seared_bricks | 168 / 172 | 有 / 有 | 纳入 |
| seared_bricks_stairs | 169 / 173 | 有 / 有 | 纳入 |
| seared_bricks_slab | 170 / 174 | 有 / 有 | 纳入 |
| seared_bricks_wall | 171 / 175 | 有 / 有 | 纳入 |
| seared_cracked_bricks | 172 / 176 | 有 / 有 | 纳入 |
| seared_fancy_bricks | 173 / 177 | 有 / 有 | 纳入 |
| seared_triangle_bricks | 174 / 178 | 有 / 有 | 纳入 |
| seared_glass_pane | 175 / 179 | 有 / 有 | 纳入 |
| seared_tinted_glass | 176 / 180 | 有 / 有 | 纳入 |
| seared_soul_glass | 177 / 181 | 有 / 有 | 纳入 |
| seared_soul_glass_pane | 178 / 182 | 有 / 有 | 纳入 |
| scorched_lamp | 179 / 183 | 有 / 有 | 纳入 |
| scorched_stone | 180 / 184 | 有 / 有 | 纳入 |
| polished_scorched_stone | 181 / 185 | 有 / 有 | 纳入 |
| scorched_bricks | 182 / 186 | 有 / 有 | 纳入 |
| scorched_bricks_stairs | 183 / 187 | 有 / 有 | 纳入 |
| scorched_bricks_slab | 184 / 188 | 有 / 有 | 纳入 |
| scorched_bricks_fence | 185 / 189 | 有 / 有 | 纳入 |
| scorched_road | 186 / 190 | 有 / 有 | 纳入 |
| scorched_road_stairs | 187 / 191 | 有 / 有 | 纳入 |
| scorched_road_slab | 188 / 192 | 有 / 有 | 纳入 |
| chiseled_scorched_bricks | 189 / 193 | 有 / 有 | 纳入 |
| scorched_glass_pane | 190 / 194 | 有 / 有 | 纳入 |
| scorched_tinted_glass | 191 / 195 | 有 / 有 | 纳入 |
| scorched_soul_glass | 192 / 196 | 有 / 有 | 纳入 |
| scorched_soul_glass_pane | 193 / 197 | 有 / 有 | 纳入 |
| seared_channel | 194 / 198 | 有 / 有 | 纳入 |
| scorched_channel | 195 / 199 | 有 / 有 | 纳入 |

### 3.3 附加流体桶动态物品

生成规则：TFFluids.EXTRA_COLORS 的 17 个键（1211/2612 TFFluids.java:65-83）在静态初始化中按排序顺序生成 EXTRA_SOURCES（TFFluids.java:85-98），然后 TFItems.registerExtraBuckets（1211 TFItems.java:200-205；2612 204-209）对每个键调用 bucket(name, fluid)，实际 ID 为 name_bucket。

本节 17 行的 2612 差异统一为：两版本均存在，公开 ID 无新增/删除；EXTRA_SOURCES 和 bucket 注册 helper/API 有实现改动；1201 按规则全部纳入。

完整 17 个 ID：cobalt_bucket、liquid_soul_bucket、honey_bucket、blazing_blood_bucket、molten_glass_bucket、ender_bucket、seared_stone_bucket、scorched_stone_bucket、molten_clay_bucket、molten_emerald_bucket、molten_diamond_bucket、magma_bucket、meat_soup_bucket、potion_bucket、milk_bucket、slime_bucket、venom_bucket。

1211/2612 均存在且 ID 集合一致；1201 按同一逻辑纳入，实际 Forge 注册 API 留给 PB-2 验证。

| ID | 来源行（1211 / 2612） | 1211/2612 | 1201 策略 |
| --- | --- | --- | --- |
| cobalt_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| liquid_soul_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| honey_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| blazing_blood_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| molten_glass_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| ender_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| seared_stone_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| scorched_stone_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| molten_clay_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| molten_emerald_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| molten_diamond_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| magma_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| meat_soup_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| potion_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| milk_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| slime_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |
| venom_bucket | TFItems.java:200-205 / 204-209 | 有 / 有 | 纳入 |

## 4. 其他注册表逐项清单

### 4.1 流体类型、源流体和流动流体

固定三组来源：TFFluids.java:32-44 / 32-44，ID 分别为：

- 流体类型：iron、gold、copper。
- 源流体：iron、gold、copper。
- 流动流体：iron_flowing、gold_flowing、copper_flowing。

附加 17 组由 TFFluids.java:65-83 的每个键按照 moltenType(name)、source(name)、flowing(name) 和 registerFluidBlock(name + "_fluid") 生成；完整 ID 已在第 2.1 节逐项列出。1211/2612 均为 17 组，1201 全部纳入。

### 4.2 方块实体、实体、菜单和创造标签页

下表逐项的 2612 差异为：`foundry_device`、`fluid_spit`、`foundry` 和创造标签页的公开 ID 均无新增/删除；dirty worktree 只改变了相应注册实现/API 或来源行位置；1201 全部纳入，具体 Forge 构造器交给 PB-2。

| 注册表 | ID | 1211 来源 | 2612 来源 | 1201 策略 |
| --- | --- | --- | --- | --- |
| 方块实体类型 | foundry_device | TFBlockEntities.java:11 | TFBlockEntities.java:11 | 纳入；关联方块列表按来源保留 |
| 实体类型 | fluid_spit | TFEntities.java:13 | TFEntities.java:16 | 纳入；构造器/API 在 PB-2 重核 |
| 菜单类型 | foundry | TFMenus.java:12 | TFMenus.java:12 | 纳入；Forge 菜单构造器在 PB-2 重核 |
| 创造模式标签页 | foundry | TinkerFoundry.java:58 | TinkerFoundry.java:60 | 纳入；显示顺序保留 |

### 4.3 配方类型与序列化器

下表每行同时列出同名 type 和 serializer；两者是两个注册表对象。

下表每行的 2612 差异为：同名 type 和 serializer 均存在，公开 ID 无新增/删除；`FoundryRecipeSerializer` 到 `RecipeSerializer` 的实现/API 有改动；1201 保留全部 8+8 个对象，具体 Forge API 交给 PB-2。

| ID | RecipeType 来源（1211 / 2612） | Serializer 来源（1211 / 2612） | 1211/2612 | 1201 策略 |
| --- | --- | --- | --- | --- |
| melting | TFRecipes.java:20 / 19 | 40 / 39 | 有 / 有 | 纳入 |
| ore_melting | 22 / 21 | 42 / 41 | 有 / 有 | 纳入 |
| damageable_melting | 24 / 23 | 44 / 43 | 有 / 有 | 纳入 |
| entity_melting | 26 / 25 | 28 / 27 | 有 / 有 | 纳入 |
| alloying | 31 / 30 | 46 / 45 | 有 / 有 | 纳入 |
| casting | 33 / 32 | 48 / 47 | 有 / 有 | 纳入 |
| molding | 35 / 34 | 50 / 49 | 有 / 有 | 纳入 |
| fuel | 37 / 36 | 52 / 51 | 有 / 有 | 纳入 |

普通 Minecraft crafting、smelting、blasting、smoking、campfire_cooking、stonecutting 等不是本表的自定义注册。

### 4.4 1.21.1 数据组件来源项

| 来源项 | 1211/2612 来源 | 1201 策略 |
| --- | --- | --- |
| tinker_foundry:fluid 数据组件类型 | TFDataComponents.java:15 / 15；DataComponentType<CustomData>，persistent(CustomData.CODEC) | 不创建目标 1.20.1 的同名数据组件注册；保留便携罐流体字段的逻辑语义，使用 Forge 1.20.1 可验证的 ItemStack NBT/能力方案 |

## 5. 动态注册调用和非 Registry 契约

这些调用不是新的游戏 Registry ID，但会影响迁移行为，不能漏盘：

| 调用/契约 | 1211 | 2612 | 1201 策略 |
| --- | --- | --- | --- |
| DeferredRegister 绑定 | TinkerFoundry.java:199-209 | 205-215 | 用模板/Forge 注册总线重建；字段集合保持 |
| 方块实体流体能力 | TinkerFoundry.java:214-236，按 faucet/channel/chute/drain/duct/alloyer 分支 | 235-259，多一层 FoundryCapabilityCompat | 保留分支语义，Forge API 由 PB-2 核实 |
| 方块实体物品能力 | 237-245 | 260-269，经 compat wrapper | 保留过滤条件和 FoundryItemHandler 语义 |
| 便携罐能力 | 246 注册 copper_canister | 270-271，使用 dirty-only PortableTankResourceHandler | 目标使用 Forge ItemStack 能力；不得直接复制 NeoForge transfer API |
| 储罐/灯/炮能力 | 247-252，8 个 FoundryTankItem | 272-279，额外加入 seared/scorched_ingot_gauge、seared/scorched_fuel_gauge | 2612 新增的是能力覆盖，不是新物品 ID；1201 纳入能力语义 |
| 状态网络注册 | FoundryNetworking.java:18-22 | 同行 | 只保留本模组逻辑语义，跨 MC 版本不保证旧字节流互通 |
| 2612 配方同步 | 无 requestRecipeSync | TinkerFoundry.java:203-231，发送 8 个自定义 RecipeType | 作为 dirty-only 行为候选，Forge 同步 API 留 PB-2 验证 |
| 调试命令 | 1211 无 | FoundryDebugCommand.java:31-46，命令为 /tinker_foundry debug generate <smeltery|foundry|alloyer|all> | 纳入；命令树语义不变，Forge 命令事件由 PB-2 验证 |
| 客户端菜单/方块实体/实体渲染 | FoundryClientEvents.java:29-36 | 33-40 | 保留渲染对象绑定，改用 Forge 客户端事件 |
| 自定义模型 loader ID | tank、connected，FoundryClientEvents.java:42-43 | 46-47，另有 item model tank 53 | ID 作为资源/渲染契约保留；1.20.1 schema 在 PB-2/Stage E 核实 |
| 流体类型与物品染色 | FoundryClientEvents.java:49-61 | 49-61，API 事件类型改变 | 保留颜色和绑定集合，API 不在 T0.5 猜测 |

动态生成规则汇总：

- TFItems.registerVariantBlockItems：固定 39 个 items.put，逐项见第 3.2 节。
- TFItems.registerExtraBuckets：遍历 TFFluids.EXTRA_SOURCES，每个键生成 name_bucket，逐项见第 3.3 节。
- TFFluids：EXTRA_COLORS 17 个键排序后各生成 fluid type、source、flowing、liquid block，逐项见第 2.1 节。
- FoundryDebugCommand：字符串参数只接受 smeltery、foundry、alloyer、all；all 生成三类结构，不是 Registry ID。

## 6. 网络载荷契约

### 6.1 共同约束

- 协议值：字符串 "6"，来源 FoundryNetworking.java:15，两版本相同。
- 载荷方向：服务端向客户端；两个逻辑 ID 不是原版容器包。
- **跨 Minecraft 版本不保证网络互通。** 1.20.1 Forge 与 1.21.1/26.1.2 NeoForge 只保留本模组逻辑字段语义，不要求旧字节流、旧 StreamCodec 或旧 loader 网络实现兼容。
- 1.20.1 迁移不得为了旧字节流兼容而复制 NeoForge payload API；PB-2 只需验证目标 Forge 网络承载方式。

### 6.2 tinker_foundry:foundry_state

来源：network/FoundryStatePayload.java:15-88；1211/2612 的逻辑字段顺序一致，但目标 1.20.1 使用 Forge `FriendlyByteBuf`，不是来源版本的 payload/codec 编码入口。

记录顺序和编码顺序固定如下：

1. BlockPos pos：Forge 1.20.1 `FriendlyByteBuf.writeBlockPos/readBlockPos`。
2. FluidStack fluid：一个 boolean present，为真才写流体。
3. FluidStack fuelFluid：一个 boolean present，为真才写流体。
4. List<FluidStack> alloyInputs：VarInt inputCount，随后按列表顺序写每个流体的 presence boolean 和流体。
5. List<FluidStack> structureFluids：VarInt layers，随后按层顺序写每个流体。
6. int progress、int processTime、int castingFluidCapacity、int structureCapacity：各一个 VarInt，顺序不变。
7. boolean structureValid：一个 boolean。
8. int[] heat：VarInt size，随后按索引写 size 个 VarInt。

长度边界：

- inputCount 必须为 0..MAX_ALLOY_INPUTS，MAX_ALLOY_INPUTS = 5；超过边界读取抛出错误。
- layers 必须为 0..StructureFluidTank.MAX_LAYERS，MAX_LAYERS = 128。
- heat.size 必须为 0..MAX_STRUCTURE_INPUTS * 5，且 size % 5 == 0；MAX_STRUCTURE_INPUTS = 14 * 14 * 63 = 12348，上限为 61740。
- 数量、进度、容量和热量使用 VarInt；不把负值兼容或跨版本旧字节流兼容作为目标。
- `FoundryFluidIngredient` 的显式流体列表最多 128 项，资源位置字符串读写上限为 256；未知显式流体不会被静默构造成有效输入，标签资源位置非法时拒绝载荷。
- `FoundryStatePayload` 中存在的 `FluidStack` 必须为正数量，附加 `CompoundTag.sizeInBytes()` 不得超过 16 KiB；进度、时间、容量和热量值不得为负。

### 6.3 tinker_foundry:foundry_structure_error

来源：network/FoundryStructureErrorPayload.java:13-40，1211/2612 完全一致。

记录顺序和编码顺序固定如下：

1. BlockPos controllerPos。
2. boolean hasErrorPos；若为真写 BlockPos errorPos，否则为 null。
3. StructureErrorReason reason 写为 reason.ordinal() 的 VarInt。

当前 ordinal 映射（来源 multiblock/StructureErrorReason.java:6-22，两版本一致）：

| ordinal | 枚举 | 翻译键 |
| ---: | --- | --- |
| 0 | NONE | gui.tinker_foundry.structure_valid |
| 1 | NOT_LOADED | multiblock.tinker_foundry.generic.not_loaded |
| 2 | TOO_HIGH | multiblock.tinker_foundry.generic.too_high |
| 3 | INVALID_INNER_BLOCK | multiblock.tinker_foundry.generic.invalid_inner_block |
| 4 | INVALID_WALL_BLOCK | multiblock.tinker_foundry.generic.invalid_wall_block |
| 5 | INVALID_FLOOR_BLOCK | multiblock.tinker_foundry.generic.invalid_floor_block |
| 6 | INVALID_CEILING_BLOCK | multiblock.tinker_foundry.generic.invalid_ceiling_block |
| 7 | TOO_LARGE | multiblock.tinker_foundry.generic.too_large |

fromId 对 <0 或 >=8 的值回退为 NONE。1201 保留逻辑映射，但目标实现不要求与其他 Minecraft 版本的旧网络字节流互通。

## 7. 方块实体持久化契约

来源：

- 1211：block/entity/FoundryBlockEntity.java:2156-2320，CompoundTag + HolderLookup.Provider。
- 2612：block/entity/FoundryBlockEntity.java:2170-2323，ValueOutput/ValueInput + ItemStack.CODEC/FluidStack.CODEC。

两版本的逻辑键集合相同，但 Inputs、StructureFluids、AlloyInputs 的容器形态和物品/流体 codec 包装不同；不能把 2612 的 ValueOutput API 或 1211 的 HolderLookup.Provider 签名直接放进 1.20.1。以下表列出逻辑键、来源类型、缺省和迁移意义。

| 键 | 1211 来源类型 | 2612 来源类型 | 缺省/读取行为 | 1201 冻结语义 |
| --- | --- | --- | --- | --- |
| Inputs | ListTag；每项 CompoundTag 含 Slot int，物品字段直接包含 id 等 | ValueOutput children list；每项 Slot int + Stack 的 ItemStack.CODEC | 缺失清空全部输入；非法 slot 忽略；损坏物品回退空栈 | 保留按槽位的输入物品和空栈回退；目标使用 Forge 1.20.1 ItemStack NBT API |
| InputSize | int | int | 缺失为 0；控制器按值调整输入数组 | 保留结构输入槽规模语义 |
| Output | 可选 ItemStack compound | 可选 codec 字段 | 缺失/损坏为空栈 | 保留 |
| Remainder | 可选 ItemStack compound | 可选 codec 字段 | 缺失/损坏为空栈 | 保留 |
| Fuel | 可选 ItemStack compound | 可选 codec 字段 | 缺失/损坏为空栈 | 保留 |
| ProxyItem | 可选 ItemStack compound | 可选 codec 字段 | 缺失/损坏为空栈 | 保留 |
| CannonItem | 可选 ItemStack compound | 可选 codec 字段 | 缺失/损坏为空栈 | 保留 |
| Fluid | 可选 FluidStack compound | 可选 FluidStack.CODEC 字段 | 缺失为空流体；控制器读取结构层后清空自身 fluid | 保留主流体语义 |
| CastingFluidCapacity | int | int | 缺失 0；非浇注方块或无流体时强制 0 | 保留 |
| StructureFluids | ListTag，每项直接为 FluidStack compound | children list，每项含 Fluid codec 字段 | 非控制器不读取；控制器恢复有序流体层，缺失为空列表 | 保留层顺序；目标使用普通 NBT 列表 |
| FuelFluid | 可选 FluidStack compound | 可选 codec 字段 | 缺失为空流体 | 保留 |
| Progress、ProcessTime | int | int | 缺失 0 | 保留 |
| InputProgress | int array | int array | 缺失空数组；按输入数组长度补 0，逐项取非负值 | 保留数组语义 |
| BurnTime、FuelTemperature、FuelHeatingRate | int | int | 缺失 0 | 保留 |
| FuelFluidConsumption、FuelFluidConsumed、FuelBurnDuration | int | int | 缺失 0 | 保留 |
| StructureFuelRate | int | int | 缺失值最终至少为 1；2612 默认值为 1 | 保留至少 1 的有效速率规则 |
| StructureCapacity、StructureInteriorBlocks | int | int | 缺失 0 | 保留 |
| StructureValid | boolean | boolean | 缺失 false | 保留 |
| StructureMin、StructureMax、Controller | long（BlockPos.asLong） | optional long | 缺失 null；存在时 BlockPos.of | 保留边界/归属位置语义 |
| TransferCooldown | int | int | 缺失 0 | 保留 |
| ChannelFlowing | byte array，写入当前流动状态 | int array，再转入 byte 状态 | 缺失清零；元素 clamp 到 0..2 | 目标规范化为逻辑值 0..2；不承诺 2612 int-array 存档兼容 |
| FaucetState | byte，写入 enum ordinal | byte，写入 enum ordinal | 缺失/越界按读取代码钳制到有效 ordinal；缺失默认为 0 | 保留 `OFF=0`、`POURING=1`、`POWERED=2` |
| FaucetStop、FaucetRedstone | boolean | boolean | 缺失 false | 保留 |
| FaucetRenderFluid | 可选 FluidStack compound；缺失使用 fluid.copy() | 可选 codec 字段；读取空时使用 fluid.copy() | 同左 | 保留连续浇注显示语义 |
| AlloyInputs | ListTag；每项 Slot int + Fluid compound | children list；每项 Slot int + Fluid codec 字段 | 缺失初始化为 5 个空槽；非法 slot 忽略 | 保留五输入槽和按 slot 恢复 |

### 7.1 FaucetState 显式映射

两版本源码顺序一致：

| ordinal | 1211 `FoundryBlockEntity.java:293-300` | 2612 `FoundryBlockEntity.java:295-302` | 1201 迁移语义 |
| ---: | --- | --- | --- |
| 0 | `OFF` | `OFF` | 空闲，不启动浇注 |
| 1 | `POURING` | `POURING` | 已取得液体，正在输出 |
| 2 | `POWERED` | `POWERED` | 红石供电但暂时没有可输出液体 |

两版本读取代码都将存档中的 `FaucetState` byte 先钳制到 `0..2`：负值回退为 `0/OFF`，大于 `2` 回退为 `2/POWERED`；缺失值也按 `0/OFF` 处理。1211 对应 `FoundryBlockEntity.java:2309-2310`，2612 对应 `FoundryBlockEntity.java:2291-2292`。1201 保留这组三值逻辑和缺失/越界回退，不承诺跨版本旧存档字节格式兼容。

### 7.2 便携罐持久化

来源：1211/2612 `item/PortableTankFluidHandler.java`。来源实现使用组件注册项 `tinker_foundry:fluid`、`CustomData` 和 `FluidStack.CODEC`；这些是来源语义，不是 1.20.1 Forge 目标 API。

- 来源读取：组件缺失或 `CustomData.EMPTY` → 空流体；codec 解码失败 → 空流体并英文 warning 日志。
- 来源写入非空流体：`FluidStack.CODEC` 编码成功后写入组件；编码结果不是复合标签则不写入。
- 来源写入空流体：删除 `tinker_foundry:fluid` 组件。
- fill：只接受非空流体；已有流体必须同流体/同组件；数量不超过 capacity；只有 EXECUTE 才修改物品。
- drain：类型不匹配、空流体或非正数量返回空；只有 EXECUTE 才扣减并回写。
- 2612 dirty 额外用 PortableTankResourceHandler 适配 NeoForge transfer ItemAccess；这是实现候选，不是 1.20.1 API 证据。

1201 策略：不注册 1.21.1 `Registries.DATA_COMPONENT_TYPE`，也不伪造不存在的 `DataComponentType` API；目标已选择 Forge 1.20.1 可核实的 ItemStack NBT/流体能力实现：目标键为 `Fluid`，非空值使用 `FluidStack.writeToNBT` 写入，读取使用 `FluidStack.loadFluidStackFromNBT`，空值删除该键，能力通过 Forge `IFluidHandlerItem` 暴露。实现保持上述“流体、数量、容量、simulate/execute、空罐删除、损坏后物品仍保留流体”的逻辑语义，但不承诺读取 1.21.1 component 或 2612 ValueOutput 存档格式。

## 8. book 资源和许可结论

### 8.1 实际资源清单

assets/tinker_foundry/book/ 实际包含：

- UPSTREAM-LICENSE.txt。
- images/ 根目录 25 个 PNG：alloyer、alloyergui、bartering、blaze_smeltery、carry_blaze、cast_automation、channels、compositecast、entitymelting、faucet_pour、fcontroller、foundrygui、gold_pick、hottub、manyullyn_alloy、melter、meltergui、movingfluids、sand_pick、sandcast_partbuilder、sandcast、scontroller、scorched_casting、smeltergui、thruul。
- images/covers/puny_smelting_pages.png。
- images/zh_cn/ 目录 PNG：alloyergui、compositecast、fcontroller、foundrygui、gold_pick；实际清单以目录枚举为准。
- structures/foundry.nbt、structures/smeltery.nbt。

data/tinker_foundry/book/ 实际包含 chapters.json 和七个章节：intro、melter、smeltery、foundry、casting、alloying、fuel。章节文件实际引用 book.tinker_foundry.* 翻译键；en_us.json:280 的索引标题为 Standalone Foundry Guide，zh_cn.json:280 为“独立冶炼指南”。Java 源码静态搜索没有发现书注册或 chapters.json 读取调用，因此当前只能确认它是本项目命名空间下的资源集合，不能确认运行时一定会加载。

### 8.2 第三方许可证据和范围决定

UPSTREAM-LICENSE.txt 是 MIT License，版权人为 SlimeKnights，版权年份 2022。该文件证明这些上游资源受 MIT 条款覆盖，但没有逐张图片的来源映射、版本号或外部仓库 URL；因此不能仅凭文件名证明每张图片都可独立再分发，也不能将其称为“原版匠魂书”。

T0.5 的明确决定：

1. **核心 1.20.1 Forge 骨架不依赖 book；book 不阻塞 Stage B。**
2. **项目指南资源保留为可选迁移候选**：chapters.json、七个章节 JSON、本项目命名空间语言键和结构 NBT 可以继续盘点。
3. **最终资源包是否随模组发布待外部选择**：在没有逐项图片来源/许可映射前，不把 assets/tinker_foundry/book/images 宣布为已批准发布内容。若不批准，则只排除/替换图片资源，不改变核心熔炼功能。
4. **明确排除** Tinkers' Construct 书 API、书注册、工具、材料词条、Modifier、工具部件和相关内容；本清单没有把它们注册或纳入产品范围。

## 9. T0.5 结论与后续边界

T0.5 已完成，主对话已于 **2026-09-21 通过 Gate A**。依据是独立核对 1211 TFBlocks 静态 77、TFFluids `EXTRA_COLORS` 17+固定 3、TFItems 固定 54+动态 39+附加桶 17=110 且无重复，并复查 `FaucetState` `OFF=0`、`POURING=1`、`POWERED=2` 及读取钳制源码。

- 1211/2612 全部 Registry ID 已逐项列出，动态生成规则和证据行已列明。
- 两个网络载荷的 ID、协议 6、字段/编码顺序、长度边界和 ordinal 0..7 已冻结为本模组逻辑契约；不要求跨 Minecraft 版本旧字节流互通。
- 方块实体 NBT 键、类型、缺省行为、1211/2612 容器差异和 1201 迁移含义已列明。
- 便携罐组件来源语义与 1.20.1 Forge ItemStack NBT/能力实现边界已分开记录。
- book 的实际文件、引用、MIT 上游许可证据、独立指南判断和外部选择项已记录；不阻塞骨架。

仍需后续阶段或外部选择确认的事项：

1. 是否接受“book 图片最终发布与否由外部许可选择决定”的资源策略。
2. 是否接受 1.20.1 只保留便携罐流体逻辑语义，不承诺读取 1.21.1 component 或 2612 ValueOutput 存档格式。
3. Gate B 已于 2026-09-21 审核通过；该审核只确认 PB-1/PB-2/PB-3 的静态交付，不能等同于构建、启动或业务玩法验证完成。PC-1→PC-4 的静态审计另见 `1201-forge-api-evidence.md` §2.10，PD-1→PD-3 已完成且 Gate D 已由主对话通过；PE-1→PE-4 的菜单、网络、渲染和 JEI 静态审计另见 `1201-forge-api-evidence.md` §2.11，Gate E 已由主对话通过；PF-1→PF-3→PF-2 的资源审计见 `docs/1201-forge-resource-audit.md`，核心资源静态审核已通过 Gate F；PG-1/T6.1 总静态审计见 `docs/1201-forge-final-static-audit.md`，待主对话复核。

## 10. 静态证据位置

- 注册入口：两个参考树 src/main/java/org/hp/tinker_foundry/TinkerFoundry.java、registry/TFBlocks.java、TFItems.java、TFFluids.java、TFBlockEntities.java、TFEntities.java、TFMenus.java、TFRecipes.java、TFDataComponents.java。
- 协议：两个参考树 network/FoundryNetworking.java、FoundryStatePayload.java、FoundryStructureErrorPayload.java、multiblock/StructureErrorReason.java。
- NBT：两个参考树 block/entity/FoundryBlockEntity.java；便携罐 item/PortableTankFluidHandler.java、FoundryTankItem.java、2612 item/PortableTankResourceHandler.java。
- book：1211 master src/main/resources/assets/tinker_foundry/book/、data/tinker_foundry/book/、assets/tinker_foundry/lang/en_us.json、zh_cn.json。
- 1.20.1 资源路径基础证据：C:\Users\30819\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1-recomp.jar 的 jar tf 结果包含 data/minecraft/loot_tables/、data/minecraft/recipes/、data/forge/tags/，没有单数 loot_table/。

## Revision Log

- 2026-09-21 — 完成 T0.5；主对话依据注册数量、重复性和 FaucetState 映射通过 Gate A，并审核通过 PB-1/PB-2/PB-3 的 Gate B。PC-1→PC-4 静态审计完成后获授权继续，PD-1→PD-3 配方与处理保护审计完成并通过 Gate D；Stage E 的 PE-1→PE-4 静态证据已回写并通过 Gate E；Stage F 的 PF-1→PF-3→PF-2 核心资源静态证据已回写并通过 Gate F；PG-1/T6.1 总静态审计已完成，book 未决项和 alloying 0 条来源事实保持不变。
