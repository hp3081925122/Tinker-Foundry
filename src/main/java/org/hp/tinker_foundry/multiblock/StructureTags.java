package org.hp.tinker_foundry.multiblock;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.hp.tinker_foundry.TinkerFoundry;

/** 各结构部位独立使用标签，数据包可扩展而不必修改检测器。 */
public final class StructureTags {
    /** 冶炼炉墙、底板和储罐。 */
    public static final TagKey<Block> SMELTERY_WALL = block("smeltery/wall");
    public static final TagKey<Block> SMELTERY_FLOOR = block("smeltery/floor");
    public static final TagKey<Block> SMELTERY_TANKS = block("smeltery/tanks");
    /** 铸造炉墙、底板和储罐。 */
    public static final TagKey<Block> FOUNDRY_WALL = block("foundry/wall");
    public static final TagKey<Block> FOUNDRY_FLOOR = block("foundry/floor");
    public static final TagKey<Block> FOUNDRY_TANKS = block("foundry/tanks");
    /** 合金炉侧面允许读取流体能力的邻接储罐。 */
    public static final TagKey<Block> ALLOYER_TANKS = block("alloyer_tanks");
    /** 合金炉下方允许提供燃料的加热器或燃料罐。 */
    public static final TagKey<Block> FUEL_TANKS = block("fuel_tanks");
    /** 允许放在炉腔中的空气等价方块。 */
    public static final TagKey<Block> AIR = block("structure_air");

    /** 使用当前版本的方块标签注册表。 */
    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, path));
    }

    private StructureTags() { }
}
