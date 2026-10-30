# 2.0 · 2026-10-30

## 中文

- 自动材料新增浇注配方：已确定熔融流体的材料，浇注台放锭模产出锭、放宝石模产出宝石，浇注盆产出块；已有浇注配方的形态不重复生成，黑名单物品不作为产物。
- 自动生成的熔融流体颜色改为在客户端读取材料代表物品（锭、宝石、块优先）的材质，取占比最多的颜色；配置或流体标签显式指定颜色的材料保持原色，资源包重载后重新取色。
- 只出现在 `storage_blocks` 标签里的材料（干草块、骨块、海带块、史莱姆块等）和砖类不再生成熔融流体。
- 流体和桶名称改为固定 lang 键，不再运行时生成英文名；药水流体在界面和 JEI 中显示药水名称，JEI 展开显示各药水的浇注配方。
- 浇注配方新增可选 `basin` 字段，浇注盆可放入单个模具物品；焦黑石块类配方标记为浇注盆配方。
- 新增蜂蜜、史莱姆、肉汤、灵魂液和毒液的浇注用途，新增肉汤食物，冶炼炉的牛奶可合成为牛奶桶。
- 移除粘液球的宝石模浇注配方。
- 新增仅限管理员（权限等级 2）的 `/tinker_foundry debug` 测试命令，用于快速搭建和填充测试设备。
- 26.1.2：恢复结构预览、设备流体与物品、流体炮弹等渲染，以及储罐连接玻璃模型。

## English

- Automatic materials now receive casting recipes: ingot cast to ingot, gem cast to gem, and basin to block. Forms already produced by an existing casting recipe are skipped; blacklisted items are never chosen as results.
- Generated molten fluid colors are sampled on the client from the material's representative item texture (ingot, gem and block first), using the most common color. Materials with an explicit color in config or fluid tags keep it; colors refresh on resource reload.
- Materials that only appear in `storage_blocks` tags (hay, bone, dried kelp, slime blocks, etc.) and bricks no longer get molten fluids.
- Fluid and bucket names use static lang keys instead of runtime English names. Potion fluids show the potion name in screens and JEI, and JEI lists potion casting per potion.
- Casting recipes accept an optional `basin` flag; basins can hold a single cast item. Seared block recipes are marked as basin recipes.
- Added casting uses for honey, slime, meat soup, liquid soul and venom, a meat soup food item, and a milk bucket recipe from foundry milk.
- Removed the slime ball gem-cast recipe.
- Added operator-only (permission level 2) `/tinker_foundry debug` commands for setting up and filling test devices.
- 26.1.2: restored structure preview, device fluid/item, fluid cannon projectile rendering and connected tank glass models.

Wiki: https://github.com/hp3081925122/Tinker-Foundry/wiki
