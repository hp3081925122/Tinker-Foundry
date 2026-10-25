# 1.3 · 2026-10-25

## 中文

- 按 `forge:` / `c:` 材料标签自动补全缺失的熔融流体，并注册静止/流动流体、流体方块、桶和所需资源；已有明确流体标签的外部熔融流体优先复用。
- 自动补全粒、锭、块、粗矿、粗矿块、矿石、粉、板和宝石的熔炼配方，保留已有配方；矿石与粗矿沿用冶炼炉/铸造炉的增产规则，支持黑名单排除。
- 新增 `config/tinker_foundry/fluids/` JSON 流体配置，递归读取子目录。每个文件只需 `material`、`color`、`temperature` 三项，名称通过 lang 提供。
- 自动识别只使用标签，不按注册名猜材料。新增流体需要双端相同的启动资源与配置并完全重启；晚加载的数据包不能通过 `/reload` 注册新流体。
- 外部流体的颜色和温度保持原模组定义。多个不同源流体有歧义时跳过自动配方；装备拆分仍需明确成分配方。

## English

- Automatically generate missing molten fluids from common `forge:` / `c:` material tags, including source/flowing fluids, fluid blocks, buckets and resources. Existing externally tagged molten fluids take priority.
- Fill missing melting recipes for nuggets, ingots, storage blocks, raw materials, raw blocks, ores, dusts, plates and gems. Existing recipes remain preferred; ores and raw materials follow the smeltery/foundry yield rules, with an automatic-melting blacklist.
- Add recursive JSON fluid definitions under `config/tinker_foundry/fluids/`. Each file requires only `material`, `color` and `temperature`; names come from lang resources.
- Detection uses tags only, with no material guessing from registry names. New fluids require matching startup resources/configuration on both sides and a full restart; late world datapacks cannot register fluids through `/reload`.
- Reused external fluids retain their own color and temperature. Ambiguous multiple-source mappings skip automatic recipes; equipment recycling still needs explicit composition recipes.

Wiki: https://github.com/hp3081925122/Tinker-Foundry/wiki
