package org.hp.tinker_foundry.multiblock;

import net.minecraft.network.chat.Component;

/** 多方块结构失败原因，文本沿用匠魂官方 1.20.1 的结构提示语义。 */
public enum StructureErrorReason {
    /** 没有结构错误。 */
    NONE("gui.tinker_foundry.structure_valid"),
    /** 结构所需区块尚未加载。 */
    NOT_LOADED("multiblock.tinker_foundry.generic.not_loaded"),
    /** 结构高度超过上限。 */
    TOO_HIGH("multiblock.tinker_foundry.generic.too_high"),
    /** 结构内部存在不允许的方块。 */
    INVALID_INNER_BLOCK("multiblock.tinker_foundry.generic.invalid_inner_block"),
    /** 结构侧壁存在不允许的方块。 */
    INVALID_WALL_BLOCK("multiblock.tinker_foundry.generic.invalid_wall_block"),
    /** 结构底板存在不允许的方块。 */
    INVALID_FLOOR_BLOCK("multiblock.tinker_foundry.generic.invalid_floor_block"),
    /** 结构顶面存在不允许的方块。 */
    INVALID_CEILING_BLOCK("multiblock.tinker_foundry.generic.invalid_ceiling_block"),
    /** 结构内部尺寸超过匠魂官方限制。 */
    TOO_LARGE("multiblock.tinker_foundry.generic.too_large");

    /** 对应的本模组翻译键。 */
    private final String translationKey;

    /** 保存原因对应的翻译键。 */
    StructureErrorReason(String translationKey) {
        this.translationKey = translationKey;
    }

    /** 返回客户端显示使用的翻译键。 */
    public String translationKey() {
        return translationKey;
    }

    /** 将网络中的整数安全还原为错误原因。 */
    public static StructureErrorReason fromId(int id) {
        StructureErrorReason[] values = values();
        return id < 0 || id >= values.length ? NONE : values[id];
    }

    /** 为结构提示构造本地化文本。 */
    public Component component() {
        return Component.translatable(translationKey);
    }
}
