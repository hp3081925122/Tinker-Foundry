# 2.2 · 2026-10-30

## 中文

- 自动生成的流体和桶缺少 lang 时不再显示原始键：按材料对应锭、粒、块在当前语言下的译名自动拼出名称（如“锡锭”→“熔融锡”“熔融锡桶”）。拼接规则由三个 lang 键控制：`tinker_foundry.automatic.fluid`（`熔融%s`）、`tinker_foundry.automatic.bucket`（`%s桶`）、`tinker_foundry.automatic.item_forms`（`%s锭|%s粒|%s块|粗%s` 等形态词），其他语言补这三个键即可。
- 新增客户端指令 `/tinker_foundry_client export_lang`，按同样规则把 `en_us`、`zh_cn` 和当前语言中缺失的流体与桶翻译导出到 `.minecraft/tinker_foundry/lang_export/<语言>.json.export`，去掉 `.export` 后缀即可放进资源包使用。
- 内置约 200 种金属、合金和宝石的中英文流体名称，覆盖常见矿物合金及通用机械、热力、末影接口、现代工业化、AE2 附属、ATM 金属、暮色森林、植物魔法、灾变、地下花园、永恒星光、冰与火之歌、Silent Gear/Gems 等模组材料（译名取自各模组官方语言文件）。

## English

- Generated fluids and buckets without lang entries no longer show raw keys: names are built from the current-language name of the material's ingot/nugget/block (e.g. `Tin Ingot` → `Molten Tin` / `Molten Tin Bucket`). Three lang keys control this: `tinker_foundry.automatic.fluid` (`Molten %s`), `tinker_foundry.automatic.bucket` (`%s Bucket`) and `tinker_foundry.automatic.item_forms` (form words such as `%s Ingot|%s Nugget|Raw %s`); add them to support any language.
- New client command `/tinker_foundry_client export_lang` exports missing fluid and bucket names for `en_us`, `zh_cn` and the selected language to `.minecraft/tinker_foundry/lang_export/<lang>.json.export` using the same rules; remove the `.export` suffix to use it in a resource pack.
- Built-in Chinese and English fluid names for about 200 metals, alloys and gems, covering common ores and alloys plus materials from Mekanism, Thermal, Ender IO, Modern Industrialization, AE2 add-ons, Allthemodium, Twilight Forest, Botania, Cataclysm, Undergarden, Eternal Starlight, Ice and Fire and Silent Gear/Gems (names taken from each mod's official lang files).

Wiki: https://github.com/hp3081925122/Tinker-Foundry/wiki
