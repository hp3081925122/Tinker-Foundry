# Tinker Foundry

Tinker Foundry ports the smelting system from Tinkers' Construct into a standalone mod.

It provides melters, smelteries, foundries, alloyers, heaters, fuel tanks, fluid devices, recipes, and container interfaces for processing molten metals.

**Note:** It intentionally does not include tools, material traits, or modifiers. This mod focuses purely on the fluid and smelting mechanics.

**Source Code:** [https://github.com/hp3081925122/Tinker-Foundry.git](https://github.com/hp3081925122/Tinker-Foundry.git)

## Credits & License

*   **Original Inspiration / Source Material:** [Tinkers' Construct](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct) by **mDiyo** and the Tinkers' Construct team.
*   **License:** The original Tinkers' Construct project is licensed under the MIT License. This standalone port is distributed in compliance with the original open-source license. All credit for the original design, mechanics, and concepts belongs to the original authors.

***

# 中文说明

Tinker Foundry 将 [Tinkers' Construct](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct)（匠魂）中的冶炼系统移植为一个独立的模组。

它提供了用于处理熔融金属的熔化炉、冶炼炉、铸造厂、合金炉、加热器、燃料罐、流体设备、配方以及容器交互界面。

**注意：** 本模组有意排除了工具、材料特性或修饰符，仅专注于流体与冶炼机制。

**源码地址：** [https://github.com/hp3081925122/Tinker-Foundry.git](https://github.com/hp3081925122/Tinker-Foundry.git)

## 致谢与开源协议

*   **原项目/灵感来源:** 由 **mDiyo** 及 Tinkers' Construct 团队开发的 [Tinkers' Construct](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct)。
*   **协议:** 原 Tinkers' Construct 项目采用 MIT License 开源协议。本独立移植项目遵循原开源协议进行分发。原模组的设计、机制及相关概念的版权均归原作者所有。

***

## Automatic molten fluids and melting recipes

Common `forge:` / `c:` material tags can automatically provide missing molten fluids and melting recipes for nuggets, ingots, blocks, raw materials, raw blocks, ores, dusts, plates and gems. Existing tagged molten fluids and existing recipes take priority. Generated fluids include source/flowing variants, a placeable fluid block and a bucket. Ore and raw-material yields follow the smeltery/foundry recipe rules.

### Limits of automatic generation

*   Tags are required: registry names and item appearances are never used to guess materials or existing fluids. Incorrect or missing material tags need a pack-side fix or explicit recipes.
*   Registration scans startup mod resources. Late world datapacks and server scripts cannot add new fluids through `/reload`; both client and server need matching startup resources/configuration and a full restart.
*   Conditional, empty, cyclic and optional-only item declarations cannot reliably establish a material at startup. Optional-only external-fluid references can reserve an unused fallback because their existence cannot be proved before registration; recipes still prefer the actual tagged external fluid after data loading.
*   Multiple distinct external source fluids for one material are ambiguous and skip automatic recipes. Source/flowing variants of one fluid are normalized together. Explicit base-process material references use a stable choice among tagged sources.
*   Unknown materials use a generic molten texture and a default temperature; the color is sampled on the client from the material's ingot, gem or block texture (explicit colors in config or tags win). Use `tinker_foundry:melting/automatic_blacklist` to exclude items from automatic recipes.

### Automatic casting

Materials with a resolved molten fluid also get casting recipes: ingot cast → ingot, gem cast → gem on the casting table, and block in the casting basin. Forms already produced by an existing casting recipe are skipped.

### Melting derived from crafting

When every ingredient of a shaped, shapeless or stonecutting recipe can be melted, the result gets a melting recipe worth the total ingredient fluid divided by the output count (rounded down). The cheapest recipe is used when several make the same item, so the stonecutter cannot duplicate fluid. Multiple fluids become a main product plus byproducts (output by both the smeltery and the foundry); damageable items scale with remaining durability, so vanilla and modded tools and armor are recyclable without hand-written recipes. `automatic_melting.crafting_ignored_ingredients` in `config/tinker_foundry-common.toml` (default `minecraft:stick`) lists items or `#tags` ignored when deriving.

### Fluid names

Chinese and English names are built in for about 200 metals, alloys and gems: common ores and alloys (tin, lead, silver, nickel, zinc, aluminum, uranium, osmium, bronze, brass, electrum, invar, steel and more) plus materials from popular tech and adventure mods such as Mekanism, Thermal, Ender IO, Modern Industrialization, AE2 add-ons, Allthemodium, Twilight Forest, Botania, Cataclysm, Undergarden, Eternal Starlight, Ice and Fire and Silent Gear/Gems. Missing names fall back to the templates above. To ship fixed names, run the client command `/tinker_foundry_client export_lang`: it derives names with the same rules and writes the missing `en_us`, `zh_cn` and currently selected language entries to `.minecraft/tinker_foundry/lang_export/<lang>.json.export`. Remove the `.export` suffix and drop the file into a resource pack's `assets/tinker_foundry/lang/`.

### Register fluids with JSON

Place one JSON object per file anywhere under `config/tinker_foundry/fluids/` (subfolders are read recursively):

```
{
  "material": "tin",
  "color": "#FFD5DDE0",
  "temperature": 1100
}
```

`material` is a lowercase material ID, or the full `tinker_foundry:molten/tin` ID. `color` accepts `#RRGGBB` or `#AARRGGBB`; `temperature` is a non-negative integer in kelvin. A definition can register a fluid even without item tags; automatic melting recipes still require tagged input items. Existing tagged external fluids are reused without changing their original color or temperature. JSON overrides tag metadata for fluids generated by this mod.

Do not put names in the definition. Add translations to a resource pack's `assets/tinker_foundry/lang/en_us.json` (or the corresponding language file):

```
{
  "fluid_type.tinker_foundry.molten.tin": "Molten Tin",
  "item.tinker_foundry.molten.tin_bucket": "Molten Tin Bucket"
}
```

Lang entries always win. When a fluid or bucket has no entry, its name is built from the current-language name of the material's ingot/nugget/block using the templates `tinker_foundry.automatic.fluid` (`Molten %s`), `tinker_foundry.automatic.bucket` (`%s Bucket`, %s is the fluid name) and the form list `tinker_foundry.automatic.item_forms` (`%s Ingot|%s Nugget|...|Raw %s`); add these three keys to support any other language. Files are sorted by relative path; the first valid definition for a material wins. Invalid files are logged and skipped. The first startup creates an inactive `example.json.disabled` and instructions. Match the configuration on both sides and restart after editing.

## 自动熔融流体与自动熔炼

按 `forge:` / `c:` 材料标签，为粒、锭、块、粗矿、粗矿块、矿石、粉、板和宝石补全缺失的熔融流体与熔炼配方，已有带标签的外部流体和已有配方优先。新流体包含静止/流动变体、可放置方块和桶；矿石与粗矿沿用冶炼炉/铸造炉的增产规则。

### 自动生成的限制

*   只根据标签识别，不根据注册名或外观猜测。缺失或错误标签需要整合包修正或手写配方。
*   流体在启动时注册；晚加载的世界数据包和服务端脚本不能通过 `/reload` 新增流体。双端启动资源及配置必须一致，修改后完全重启。
*   条件、空、循环和仅含可选引用的物品标签不能可靠确认启动材料。仅含可选引用的外部流体标签可能预留一个未使用的备用流体，正式加载后的配方仍优先复用实际绑定的外部流体。
*   同一材料映射到多个不同源流体时，自动配方会因歧义而跳过；同一流体的静止/流动变体会归一。本体基础工艺的明确材料引用按稳定顺序选择已标记的源流体。
*   未知材料使用通用熔融材质和默认温度，颜色由客户端读取材料锭、宝石、块的贴图自动取色（配置或标签显式写了颜色的优先）。可用 `tinker_foundry:melting/automatic_blacklist` 标签排除自动熔炼物品。

### 自动浇注

已确定熔融流体的材料会自动获得浇注配方：浇注台放锭模产出锭、放宝石模产出宝石，浇注盆产出块；已有浇注配方的形态不重复生成。

### 合成配方折算熔炼

有序、无序合成和切石机配方的材料全部可熔炼时，按材料总量除以产物数量（向下取整）为产物生成熔炼配方；同一产物有多条配方时取熔出最少的一条，防止借切石机刷流体。多种流体时量最多的为主产物，其余为副产物（冶炼炉和铸造炉都会产出）；有耐久的物品按剩余耐久折算，原版和模组的工具、盔甲无需手写配方即可回收。`config/tinker_foundry-common.toml` 中的 `automatic_melting.crafting_ignored_ingredients`（默认 `minecraft:stick`）可列出折算时忽略的物品或 `#标签`。

### 流体名称

内置约 200 种金属、合金和宝石的中英文流体名称：常见矿物与合金（锡、铅、银、镍、锌、铝、铀、锇、青铜、黄铜、琥珀金、殷钢、钢等），以及通用机械、热力、末影接口、现代工业化、AE2 附属、ATM 金属、暮色森林、植物魔法、灾变、地下花园、永恒星光、冰与火之歌、Silent Gear/Gems 等热门科技与冒险模组的材料。其余材料缺少翻译时按上述模板自动显示；需要固定译名时，可在游戏内执行客户端指令 `/tinker_foundry_client export_lang`，按相同规则推导名称，把 `en_us`、`zh_cn` 和当前所选语言中缺失的条目写入 `.minecraft/tinker_foundry/lang_export/<语言>.json.export`，去掉 `.export` 后缀放进资源包 `assets/tinker_foundry/lang/` 即可使用。

### JSON 注册流体

在 `config/tinker_foundry/fluids/` 及子目录中，每个 `.json` 文件填写上面的 `material`、`color`、`temperature` 三项即可；温度为非负的开尔文整数。即使没有物品标签也能显式注册流体，但自动熔炼配方仍需输入物品标签。已有带标签外部流体优先复用，其固有颜色和温度不被改写；本模组生成流体的 JSON 色温优先于标签内元数据。

名称不写入配置，使用资源包 `assets/tinker_foundry/lang/zh_cn.json`：

```
{
  "fluid_type.tinker_foundry.molten.tin": "熔融锡",
  "item.tinker_foundry.molten.tin_bucket": "熔融锡桶"
}
```

lang 条目始终优先；流体或桶缺少 lang 时，按材料对应锭、粒、块在当前语言下的译名，套用 `tinker_foundry.automatic.fluid`（`熔融%s`）、`tinker_foundry.automatic.bucket`（`%s桶`，%s 为流体名）和形态词列表 `tinker_foundry.automatic.item_forms`（`%s锭|%s粒|%s块|粗%s`）自动显示，其他语言补这三个键即可。文件按相对路径排序，同材料第一份有效配置生效；坏文件跳过并记录日志。首次创建目录会生成默认不生效的 `example.json.disabled` 和说明。双端配置需一致，修改后完全重启。

[Wiki / 使用文档](https://github.com/hp3081925122/Tinker-Foundry/wiki) · [Source / 源码](https://github.com/hp3081925122/Tinker-Foundry)
