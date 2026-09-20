package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Map;

/** 浇注口按被点击的连接面朝向，出液端固定位于方块下方。 */
public final class FoundryFaucetBlock extends FoundryDirectionalBlock {
    /** 浇注口方块编解码器。 */
    public static final MapCodec<FoundryFaucetBlock> CODEC = simpleCodec(FoundryFaucetBlock::new);
    /** 浇注口六向碰撞体积，尺寸与匠魂 faucet 模板模型保持一致。 */
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
        Direction.DOWN, Shapes.join(Block.box(4, 10, 4, 12, 16, 12), Block.box(6, 10, 6, 10, 16, 10), BooleanOp.ONLY_FIRST),
        Direction.NORTH, Shapes.join(Block.box(4, 4, 10, 12, 10, 16), Block.box(6, 6, 10, 10, 10, 16), BooleanOp.ONLY_FIRST),
        Direction.SOUTH, Shapes.join(Block.box(4, 4, 0, 12, 10, 6), Block.box(6, 6, 0, 10, 10, 6), BooleanOp.ONLY_FIRST),
        Direction.WEST, Shapes.join(Block.box(10, 4, 4, 16, 10, 12), Block.box(10, 6, 6, 16, 10, 10), BooleanOp.ONLY_FIRST),
        Direction.EAST, Shapes.join(Block.box(0, 4, 4, 6, 10, 12), Block.box(0, 6, 6, 6, 10, 10), BooleanOp.ONLY_FIRST),
        Direction.UP, Shapes.empty()
    );

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

    /** 返回与浇注口模型一致的非完整碰撞体积，避免玩家被空气区域挡住。 */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        return direction == Direction.UP ? SHAPES.get(Direction.DOWN) : SHAPES.get(direction);
    }

    /** 返回浇注口自身编解码器。 */
    @Override
    protected MapCodec<? extends FoundryEntityBlock> codec() {
        return CODEC;
    }
}
