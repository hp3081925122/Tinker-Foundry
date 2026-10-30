# 2.1 · 2026-10-30

## 中文

- 新增合成配方自动熔炼：有序、无序合成和切石机配方的材料全部可熔炼时，按材料总量除以产物数量（向下取整）为产物生成熔炼配方；同一产物有多条配方时取熔出最少的一条，防止借切石机等高产路线刷流体。多种流体时量最多的为主产物，其余为副产物；有耐久的物品按剩余耐久折算。
- 新增通用配置 `automatic_melting.crafting_ignored_ingredients`（默认 `minecraft:stick`），列出的物品或 `#标签` 在折算时忽略，使木棍做柄的工具也能熔炼。
- 冶炼炉现在也会产出熔炼副产物。
- 移除铁、金工具与盔甲的手写耐久熔炼配方及 `*_tools_costing_*` 标签，改由合成配方反推；修复铁锹、金锹熔出 270 mB（多送 2 锭）的问题，现为 90 mB。
- JEI 熔炼页产物区拆为两个细容器：左侧主产物，右侧轮换显示副产物，无副产物时为空。
- JEI 各分类关联对应设备（冶炼炉、铸造炉、熔炉、合金炉、浇注台/盆、燃料储罐/计量器、加热器），可在设备上按 U 查看用途。
- 重绘项目图标，模组列表显示新 logo。

## English

- Crafting recipes now melt automatically: when every ingredient of a shaped, shapeless or stonecutting recipe can be melted, the result gets a melting recipe worth the total ingredient fluid divided by the output count (rounded down). When several recipes make the same item, the cheapest one is used so high-yield routes such as the stonecutter cannot duplicate fluid. With multiple fluids the largest becomes the main product and the rest byproducts; damageable items scale with remaining durability.
- New common config `automatic_melting.crafting_ignored_ingredients` (default `minecraft:stick`): listed items or `#tags` are ignored when deriving, so stick-handled tools can be melted.
- The smeltery now outputs melting byproducts too.
- Removed the hand-written iron/gold tool and armor damageable melting recipes and the `*_tools_costing_*` tags in favour of crafting-derived recipes. This fixes iron and gold shovels melting into 270 mB (2 free ingots); they now give 90 mB.
- The JEI melting page shows two thin tanks: the main product on the left and byproducts cycling on the right (empty when there are none).
- JEI categories are linked to their devices (smeltery, foundry, melter, alloyer, casting table/basin, fuel tanks/gauges, heater), so pressing U on a device lists its uses.
- Redrawn project icon; the mod list now shows the new logo.

Wiki: https://github.com/hp3081925122/Tinker-Foundry/wiki
