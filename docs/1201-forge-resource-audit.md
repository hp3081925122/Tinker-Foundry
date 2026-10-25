# Tinker Foundry 1.20.1 Forge 资源静态审计

核对日期：2026-09-21  
目标：`1.20.1-forge-tinker-foundry` / `1201forge`  
阶段：Stage F，`PF-1 → PF-3 → PF-2` 串行审计；Gate F 核心资源静态审核已由主对话通过  
限制：本记录只包含静态文件、路径、JSON、JAR 条目和源码引用核对；没有运行 Gradle、客户端或 GameTest。

## 1. 证据范围

| 对象 | 路径/版本 | 用途 |
| --- | --- | --- |
| 目标资源 | `src/main/resources/` | 1.20.1 Forge 最终资源目录 |
| 1.21.1 master | `E:\模组\发cf的模组\Tinker Foundry\Tinker Foundry` | 资源内容与公开 ID 对照 |
| 26.1.2 dirty | `E:\模组\发cf的模组\Tinker Foundry\26.1.2-neoforge-tinker-foundry` | 新版 `items/` schema 和资源差异对照 |
| Forge 证据 | `C:\Users\30819\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1-recomp.jar` | 1.20.1 Forge 资源路径、模型 loader 和 `pack_format` 证据 |

本轮没有修改两个参考工作树，也没有把 26.1.2 的 `assets/tinker_foundry/items/` 直接复制到 Forge 目标。

## 2. PF-1：材质、方块状态、模型和 GUI 资源

### 2.1 目录与数量

目标资源根只有 `assets/`、`data/`、`META-INF/` 和 `pack.mcmeta`。`assets/tinker_foundry/` 下使用 1.20.1 目录：

```text
blockstates/
models/block/
models/item/
textures/
lang/
book/
```

静态计数如下：

| 资源 | 数量 | 结论 |
| --- | ---: | --- |
| blockstates JSON | 97 | 对应 77 个静态方块和 20 个动态流体方块 |
| item model JSON | 110 | 与冻结的 110 个物品 ID 一一对应 |
| block model JSON | 170 | 本地 parent/引用闭合 |
| block texture PNG | 206 | 所有目标命名空间引用均可定位 |
| item texture PNG | 16 | 其余物品复用方块或原版材质 |
| GUI texture PNG | 8 | 菜单/JEI 背景所需资源存在 |
| 资源 JSON 总数 | 744 | JSON 解析失败 0 |

### 2.2 1.20.1 schema 结论

1.20.1 Forge 目标使用 `assets/tinker_foundry/models/item/*.json`，物品模型根为 `parent`/`textures`，必要时使用 Forge loader。目标不存在新版的 `assets/tinker_foundry/items/` 目录，也没有 `"model": { ... }` 的 26.1.2 item-model 根结构。

便携桶和罐的目标模型使用已由 Forge JAR 核实的形式：

```json
{
  "parent": "forge:item/default",
  "loader": "forge:fluid_container",
  "fluid": "minecraft:empty",
  "textures": {
    "base": "minecraft:item/bucket",
    "fluid": "forge:item/mask/bucket_fluid"
  }
}
```

2612 的 `neoforge:fluid_container`、`neoforge:item/default` 和 `neoforge:item/mask/bucket_fluid` 没有复制到目标；目标对应项改为 Forge 命名空间。静态扫描没有发现 `neoforge:` loader。

所有 blockstate 的 `model` 引用、模型 `parent` 和目标命名空间纹理引用均已解析：

- blockstate 引用的目标模型缺失：0；
- 目标命名空间 parent 缺失：0；
- 目标命名空间纹理缺失：0；
- blockstate 顶层既不是 `variants` 也不是 `multipart`：0；
- 单数 `recipe/`、单数 `loot_table/` 资源文件：0。

目标 `pack.mcmeta` 使用 `pack_format: 15`，与本机 1.20.1 Forge 模板证据一致。

## 3. PF-3：英文和中文语言

目标语言文件的 JSON 及 key 集合核对结果：

| 文件 | key 数量 | 结论 |
| --- | ---: | --- |
| `assets/tinker_foundry/lang/en_us.json` | 297 | 英文默认完整 |
| `assets/tinker_foundry/lang/zh_cn.json` | 297 | 中文 key 集合与英文完全一致 |

英文独有 key：0；中文独有 key：0。Java 中静态可提取的本地化调用只留下一个动态前缀 `gui.tinker_foundry.unit.%s`，不是缺失的固定 key；对应单位后缀由代码根据流体单位生成。

调试日志仍保持英文；玩家可见的菜单、结构错误、设备、JEI 和流体提示使用语言 key，没有把内部注册 ID直接作为显示文本。

## 4. PF-2：data、tags、loot、book 和结构资源

### 4.1 data 目录

目标数据目录使用 `data/<namespace>/` 结构，配方由 PD-2 独占的 `recipes/` 排除在 PF-2 文件所有权之外。PF-2 范围内静态计数：

| 数据项 | 数量 | 结论 |
| --- | ---: | --- |
| tag JSON | 57 | 全部可解析 |
| tag value 条目 | 171 | ResourceLocation 格式检查通过 |
| `tinker_foundry` loot table | 77 | 全部为 `minecraft:block` 表 |
| damage type JSON | 2 | `smeltery_heat`、`smeltery_magic` |
| book 章节 JSON | 7 | 与 `chapters.json` 的七个章节名一致 |
| book 结构 NBT | 2 | `foundry.nbt`、`smeltery.nbt` |
| `data/**/structures/` 文件 | 0 | 结构 NBT 只存在于独立指南资源目录 |

tag 检查未发现非法 ResourceLocation、缺少 `values` 的 tag 或未知目录类型。77 个 loot table 文件与非流体 blockstate ID 一一对应；loot 中的 77 个物品引用均有目标 item model。流体方块没有被错误地补写 block loot table，因为其注册使用无战利品属性。

### 4.2 book 与结构资源边界

`assets/tinker_foundry/book/` 和 `data/tinker_foundry/book/` 是本项目命名空间下的独立指南候选，不是 Tinkers' Construct 的书注册/API。Java 源码静态搜索没有发现书注册或章节读取调用，因此当前只能确认资源存在，不能宣称运行时已经接入。

章节引用了 22 个当前语言文件没有提供的 `book.tinker_foundry.*.text/title` key。它们在英文和中文中都缺失；本轮没有凭章节名称臆造正文，也没有把原版匠魂书 API 混入。`UPSTREAM-LICENSE.txt` 记录为 SlimeKnights 2022 的 MIT 条款，但没有逐张图片的来源映射、版本和 URL，因此图片是否随最终模组发布仍需外部许可决定。该未决项不阻塞核心 Forge 资源骨架，但阻止将 book 资源宣称为最终发布内容。

`alloying` 章节只是指南资源引用，不能转化为配方来源；目标 `data/tinker_foundry/recipes/` 中仍然没有 `alloying` JSON。

## 5. 末尾空行清理

本轮仅按 `git diff --check` 报告清理 Java 文件末尾多余空行，共处理 25 个 Java 文件；没有改动其业务内容、换 loader API 或删除资源。清理后：

- `new blank line at EOF`：0；
- 剩余 `LF will be replaced by CRLF`：Git 工作树换行提示，不是内容错误；
- 没有执行构建或格式化工具。

## 6. Stage F 结论

PF-1、PF-3、PF-2 已按串行顺序完成静态审计。资源 schema、模型/纹理引用、语言 key、tags、loot、damage type、book 和结构资源均已记录；book 的 22 个未提供翻译 key 及图片许可属于外部选择项，未被伪装成已完成内容。`alloying` 仍为 0 条数据。

Gate F：核心资源静态审核已由主对话于 2026-09-21 通过。book 的 22 个翻译 key 和图片许可仍未决，不纳入最终完成声明；构建、资源加载、书籍展示、模型画面和实际掉落行为仍未验证。
