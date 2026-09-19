package org.hp.tinker_foundry.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 焦黑铸造炉的独立结构描述器，只接受焦黑外壳。 */
public final class FoundryMultiblock {
    /** 最小有效容量。 */
    public static final int MINIMUM_CAPACITY = 4000;
    /** 最小总高度。 */
    public static final int MINIMUM_HEIGHT = RectangularStructureDetector.MINIMUM_HEIGHT;
    /** 最大总高度。 */
    public static final int MAXIMUM_HEIGHT = RectangularStructureDetector.MAXIMUM_HEIGHT;
    /** 最大内部宽度和深度。 */
    public static final int MAXIMUM_INNER_SIZE = RectangularStructureDetector.MAXIMUM_INNER_SIZE;
    /** 铸造炉每个外壳方块提供的容量。 */
    public static final int CAPACITY_PER_SHELL_BLOCK = 8 * 90;

    /** 检查独立焦黑铸造炉。 */
    public static StructureResult validate(Level level, BlockPos controller) {
        return RectangularStructureDetector.validate(level, controller, new RectangularStructureDetector.Rules() {
            @Override
            public boolean isCasing(BlockState state) {
                return state.is(TFBlocks.SCORCHED_BRICK.get()) || state.is(TFBlocks.SCORCHED_GLASS.get())
                    || state.is(TFBlocks.SCORCHED_LANTERN.get()) || state.is(TFBlocks.SCORCHED_WALL.get())
                    || state.is(TFBlocks.SCORCHED_FANCY_BRICK.get()) || state.is(TFBlocks.SCORCHED_LADDER.get())
                    || state.is(TFBlocks.SCORCHED_TANK.get()) || state.is(TFBlocks.SCORCHED_FUEL_TANK.get())
                    || state.is(TFBlocks.SCORCHED_CASTING_TANK.get()) || state.is(TFBlocks.DRAIN.get())
                    || state.is(TFBlocks.DUCT.get()) || state.is(TFBlocks.CHUTE.get());
            }

            @Override
            public boolean isInterior(BlockState state) {
                return SmelteryMultiblock.isSharedInterior(state);
            }

            @Override
            public int capacity(int shellWidth, int shellDepth, int shellHeight, int interiorBlocks) {
                return Math.max(MINIMUM_CAPACITY, shellWidth * shellDepth * shellHeight * CAPACITY_PER_SHELL_BLOCK);
            }

            @Override
            public int fuelRate(int shellWidth, int shellDepth, int interiorHeight) {
                // 铸造炉需要完整边框，角柱计入两面墙，底板按外轮廓面积计算。
                return 1 + (2 * shellWidth * interiorHeight + 2 * (shellDepth - 2) * interiorHeight
                    + shellWidth * shellDepth) / 18;
            }
        });
    }

    /** 保留结构容量基础测试的公共入口，不参与铸造炉世界检测。 */
    public static int capacityForHeight(int height) {
        if (height < MINIMUM_HEIGHT) {
            return 0;
        }
        return Math.max(MINIMUM_CAPACITY, (height - 2) * 1000);
    }

    private FoundryMultiblock() {
    }
}
