package org.hp.tinker_foundry.common;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;

/** 通用配置：控制自动生成配方的行为。 */
public final class FoundryConfig {
    private static final List<String> DEFAULT_CRAFTING_IGNORED = List.of("minecraft:stick");
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CRAFTING_IGNORED_INGREDIENTS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("automatic_melting");
        CRAFTING_IGNORED_INGREDIENTS = builder
            .comment("合成配方折算熔炼时可忽略的材料，填写物品 ID 或 #物品标签；被忽略的材料不计入熔炼量，配方仍需至少一个可熔炼材料。",
                "Ingredients ignored when deriving melting recipes from crafting recipes. Use item IDs or #item tags; ignored ingredients add no fluid, and the recipe still needs at least one meltable ingredient.")
            .defineListAllowEmpty(List.of("crafting_ignored_ingredients"), () -> DEFAULT_CRAFTING_IGNORED, FoundryConfig::validEntry);
        builder.pop();
        SPEC = builder.build();
    }

    /** 条目必须是合法的物品 ID 或以 # 开头的标签 ID。 */
    private static boolean validEntry(Object value) {
        return value instanceof String entry && ResourceLocation.tryParse(entry.startsWith("#") ? entry.substring(1) : entry) != null;
    }

    /** 解析可忽略材料为物品集合；配置未加载时使用默认值。 */
    public static Set<Item> craftingIgnoredItems() {
        Set<Item> items = Collections.newSetFromMap(new IdentityHashMap<>());
        List<? extends String> entries = SPEC.isLoaded() ? CRAFTING_IGNORED_INGREDIENTS.get() : DEFAULT_CRAFTING_IGNORED;
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(entry.substring(1));
                if (id != null) BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, id)).forEach(holder -> items.add(holder.value()));
            } else {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id != null) BuiltInRegistries.ITEM.getOptional(id).ifPresent(items::add);
            }
        }
        return items;
    }

    private FoundryConfig() { }
}
