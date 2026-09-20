package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 1.20.1 对应的贴壁流体计，只占据贴附面上的薄片碰撞箱。 */
public final class FoundryGaugeBlock extends FoundryEntityBlock {
    /** 使用流体计构造器的独立方块编解码器。 */
    public static final MapCodec<FoundryGaugeBlock> CODEC = simpleCodec(FoundryGaugeBlock::new);

    /** 流体计六个朝向对应的薄片碰撞箱。 */
    private static final VoxelShape[] BOUNDS = {
        Block.box(4, 15, 4, 12, 16, 12),
        Block.box(4, 0, 4, 12, 1, 12),
        Block.box(4, 4, 15, 12, 12, 16),
        Block.box(4, 4, 0, 12, 12, 1),
        Block.box(15, 4, 4, 16, 12, 12),
        Block.box(0, 4, 4, 1, 12, 12)
    };

    /** 使用当前 NeoForge 方块编解码器注册贴壁流体计。 */
    public FoundryGaugeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FoundryDirectionalBlock.FACING, Direction.NORTH));
    }

    /** 返回流体计自身的方块编解码器。 */
    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }

    /** 返回与 1.20.1 流体计一致的贴壁碰撞和选择箱。 */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BOUNDS[state.getValue(FoundryDirectionalBlock.FACING).get3DDataValue()];
    }

    /** 只有相邻的非流体计冶炼设备存在时，流体计才可以贴附。 */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FoundryDirectionalBlock.FACING);
        BlockEntity adjacent = level.getBlockEntity(pos.relative(facing.getOpposite()));
        return adjacent instanceof FoundryBlockEntity source
            && !source.isGaugeBlock();
    }

    /** 按玩家观察方向选择能贴在相邻设备上的朝向。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : context.getNearestLookingDirections()) {
            state = state.setValue(FoundryDirectionalBlock.FACING, direction.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    /** 相邻设备被移除时，自动移除失去支撑的流体计。 */
    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return facing.getOpposite() == state.getValue(FoundryDirectionalBlock.FACING)
            && !state.canSurvive(level, currentPos)
            ? Blocks.AIR.defaultBlockState() : state;
    }

    /** 将流体计的六向朝向加入方块状态定义。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FoundryDirectionalBlock.FACING);
    }
}
