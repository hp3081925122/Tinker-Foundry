package org.hp.tinker_foundry.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.hp.tinker_foundry.TinkerFoundry;

/** 本模组的可扩展物品标签，替代 Mantle 中由代理储罐使用的黑名单标签。 */
public final class FoundryTags {
    /** 不能放入代理储罐的物品及其配方容器。 */
    public static final TagKey<Item> PROXY_TANK_BLACKLIST = item("proxy_tank_blacklist");

    /** 创建当前模组命名空间下的物品标签。 */
    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, path));
    }

    private FoundryTags() { }
}
