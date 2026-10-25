# 1.20.1 Forge 合金配方数据清单

核对日期：2026-09-21  
目标：Minecraft 1.20.1、Forge 47.4.23。  
状态：Stage D、Stage E 已完成且 Gate D、Gate E、Gate F 核心资源静态审核已通过（主对话审核日期：2026-09-21）；PE-1→PE-4、PG-1/T6.1 已完成相关静态语义/总静态审计；没有凭空新增合金配方；`alloying` JSON 继续为 0。

## 来源核对

| 来源 | 合金配方 JSON | 证据 |
| --- | ---: | --- |
| 1.21.1 NeoForge master | 0 | `E:\模组\发cf的模组\Tinker Foundry\Tinker Foundry\src\main\resources\data\tinker_foundry\recipe\` 下没有 `alloying/` 目录或合金配方 JSON；Java 注册入口仍有 `TFRecipes.ALLOYING` 和 `AlloyingRecipe` |
| 26.1.2 NeoForge dirty worktree | 0 | `E:\模组\发cf的模组\Tinker Foundry\26.1.2-neoforge-tinker-foundry\src\main\resources\data\tinker_foundry\recipe\` 下没有 `alloying/` 目录或合金配方 JSON；dirty Java 仍有 `TFRecipes.ALLOYING` 和 `AlloyingRecipe` |
| 1.20.1 Forge 目标 | 0 | `src/main/resources/data/tinker_foundry/recipes/` 下的子目录统计没有 `alloying/`；目标 Java 在 `src/main/java/org/hp/tinker_foundry/registry/TFRecipes.java:32-47` 保留类型和 serializer |

master 的 `book/chapters/alloying.json` 是指南章节，不是配方数据；它不能被当作合金配方清单，也不能据此捏造配方输入和输出。产品范围仍不包含原版匠魂书。

## 目标注册契约

目标保留 8 个自定义配方注册名，并与普通 Minecraft 配方分开：

1. `entity_melting`
2. `melting`
3. `ore_melting`
4. `damageable_melting`
5. `alloying`
6. `casting`
7. `molding`
8. `fuel`

其中 `alloying` 的 Java serializer 使用 Forge 1.20.1 的 `RecipeSerializer#fromJson(ResourceLocation, JsonObject)`、`fromNetwork(ResourceLocation, FriendlyByteBuf)` 和 `toNetwork(FriendlyByteBuf, T)`；输入使用 `FoundryFluidAmount`，保留 `ingredients`、`catalyst`、`result` 和 `temperature` 字段语义。普通 Minecraft 的 `crafting_shaped`、`crafting_shapeless`、`smelting`、`stonecutting` 等仍然只是原版配方，不注册为本模组的 8 类自定义 type。

## 后续决策

- 如果产品需要可玩的合金配方，必须由产品/主对话提供明确的输入流体、数量、催化标记、产物流体和温度，再新增到 `data/tinker_foundry/recipes/alloying/`。
- 新增前先保持本清单与 `TFRecipes`、JEI `ALLOYING_TYPE` 同步；不得把 1.21.1 的 `Codec`、`StreamCodec` 或 `RecipeInput` 格式复制进 1.20.1 Forge。
- 当前 0 条合金 JSON 不阻塞 Forge 骨架和 serializer 迁移，但会阻塞“合金配方实际玩法完整”的验收。

## Stage D 交接证据

- PD-1 已核对 `TFRecipes.java` 的 `alloying` type/serializer 仍存在，`AlloyingRecipe` 的 `Recipe`、JSON、FriendlyByteBuf 顺序和催化输入语义保留。
- PD-2 已递归核对目标 `recipes/` 共 220 个 JSON，`type=tinker_foundry:alloying` 计数为 0；因此没有新增任何推测性合金输入、输出或温度。
- PD-3 的合金处理调用链保持原有多流体消耗与输出模拟逻辑，本轮只在浇注输出/浇注口批次边界增加保护，没有改写合金数据。
- Stage E 静态复核确认：`AlloyingRecipe` 和 `FoundryBlockEntity` 使用回溯一对一分配，标签重叠时不会重复占用同一输入槽；`catalyst=true` 的输入必须匹配并占用实际输入槽，但成功执行时跳过扣料，非催化输入才按配方数量模拟/执行排出。该结论来自目标 Java 代码及 1211/2612 对应处理逻辑，不代表存在任何 alloying JSON。
