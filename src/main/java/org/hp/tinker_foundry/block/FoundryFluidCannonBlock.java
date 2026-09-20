package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.Level;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 可储存流体并受红石触发的流体炮。 */
public final class FoundryFluidCannonBlock extends FoundryDirectionalBlock {
    /** 红石边沿触发状态。 */
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    /** 流体炮编解码器。 */
    public static final MapCodec<FoundryFluidCannonBlock> CODEC = simpleCodec(FoundryFluidCannonBlock::new);

    /** 创建流体炮。 */
    public FoundryFluidCannonBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
    }

    /** 增加红石触发状态。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TRIGGERED);
    }

    /** 按玩家实际观察的六向方向放置流体炮，让上下炮口也能正确对准目标。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    /** 红石信号上升沿安排一次服务端开火，避免保持信号时每 tick 重复消耗流体。 */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean triggered = state.getValue(TRIGGERED);
        if (powered && !triggered) {
            level.scheduleTick(pos, this, 4);
            level.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_INVISIBLE);
        } else if (!powered && triggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), Block.UPDATE_INVISIBLE);
        }
    }

    /** 到时执行一次完整的流体炮动作。 */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FoundryBlockEntity entity) {
            entity.shootCannon(state, level, random);
        }
    }
}
