package org.hp.tinker_foundry.multiblock;

import net.minecraft.core.BlockPos;

/** 描述一次已加载多方块结构检查的结果。 */
public record StructureResult(boolean valid, int capacity, BlockPos min, BlockPos max, int interiorBlocks, int fuelRate,
                              BlockPos errorPos, StructureErrorReason errorReason) {
    /** 保留旧调用方使用的六参数构造形式。 */
    public StructureResult(boolean valid, int capacity, BlockPos min, BlockPos max, int interiorBlocks, int fuelRate) {
        this(valid, capacity, min, max, interiorBlocks, fuelRate, null, StructureErrorReason.NONE);
    }

    /** 无效结构的统一结果。 */
    public static final StructureResult INVALID = new StructureResult(false, 0, BlockPos.ZERO, BlockPos.ZERO, 0, 0,
        null, StructureErrorReason.INVALID_INNER_BLOCK);

    /** 创建带错误方块位置的无效结构结果。 */
    public static StructureResult invalid(BlockPos errorPos, StructureErrorReason errorReason) {
        return new StructureResult(false, 0, BlockPos.ZERO, BlockPos.ZERO, 0, 0, errorPos, errorReason);
    }
}
