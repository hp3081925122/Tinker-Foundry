package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** 浇注口按被点击的连接面朝向，出液端固定位于方块下方。 */
public final class FoundryFaucetBlock extends FoundryDirectionalBlock {
    /** 浇注口方块编解码器。 */
    public static final MapCodec<FoundryFaucetBlock> CODEC = simpleCodec(FoundryFaucetBlock::new);

    /** 创建浇注口。 */
    public FoundryFaucetBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** 复现 1.20.1 浇注口的连接面放置规则，顶面点击时自动向下出液。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace();
        if (direction == Direction.UP) {
            direction = Direction.DOWN;
        }
        return defaultBlockState().setValue(FACING, direction);
    }

    /** 返回浇注口自身编解码器。 */
    @Override
    protected MapCodec<? extends FoundryEntityBlock> codec() {
        return CODEC;
    }
}
