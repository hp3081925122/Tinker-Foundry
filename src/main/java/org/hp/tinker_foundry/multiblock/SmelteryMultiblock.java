package org.hp.tinker_foundry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 普通冶炼炉的独立结构描述器，只接受冶炼炉外壳。 */
public final class SmelteryMultiblock {
    /** 最小有效容量。 */
    public static final int MINIMUM_CAPACITY = 4000;
    /** 每个炉腔方块提供的容量。 */
    public static final int CAPACITY_PER_INTERIOR_BLOCK = 12 * 90;

    /** 检查独立冶炼炉。 */
    public static StructureResult validate(Level level, BlockPos controller) {
        return RectangularStructureDetector.validate(level, controller, new RectangularStructureDetector.Rules() {
            @Override
            public boolean isCasing(BlockState state) {
                return state.is(StructureTags.SMELTERY_WALL);
            }

            /** 底板使用独立标签，不接受玻璃或储罐替代底板中心。 */
            @Override
            public boolean isFloor(BlockState state) {
                return state.is(StructureTags.SMELTERY_FLOOR);
            }

            /** 两种炉体的角框要求不同。 */
            @Override
            public boolean hasFrame() {
                return false;
            }

            @Override
            public boolean isInterior(BlockState state) {
                return isSharedInterior(state);
            }

            @Override
            public int capacity(int shellWidth, int shellDepth, int shellHeight, int interiorBlocks) {
                // 实际储液量只由炉腔体积决定，不能给小型炉额外保底容量。
                return interiorBlocks * CAPACITY_PER_INTERIOR_BLOCK;
            }

            @Override
            public int fuelRate(int shellWidth, int shellDepth, int interiorHeight) {
                // 冶炼炉只计算与炉腔接触的四面墙和底板，不计角柱。
                int dx = shellWidth - 2;
                int dz = shellDepth - 2;
                return 1 + (2 * dx * interiorHeight + 2 * dz * interiorHeight + dx * dz) / 15;
            }
        });
    }

    /** 两种炉体共同允许的炉腔内容；1.20.1 官方规则只接受空气。 */
    static boolean isSharedInterior(BlockState state) {
        return state.isAir() || state.is(StructureTags.AIR);
    }

    /** 保留旧的高度容量单元测试，但不参与世界结构扫描。 */
    public static int capacityForHeight(int height) {
        if (height < RectangularStructureDetector.MINIMUM_HEIGHT) {
            return 0;
        }
        return Math.max(MINIMUM_CAPACITY, (height - 2) * 1000);
    }

    private SmelteryMultiblock() {
    }
}
