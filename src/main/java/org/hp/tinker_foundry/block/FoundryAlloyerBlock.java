package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.multiblock.StructureTags;

/** 合金炉的微型控制器方块，提供匠魂原版的燃料结构状态和邻接刷新入口。 */
public final class FoundryAlloyerBlock extends FoundryMachineBlock {
    /** 下方存在合法燃料来源时的形成状态。 */
    public static final BooleanProperty IN_STRUCTURE = BooleanProperty.create("in_structure");
    /** 使用当前版本方块状态的编解码器。 */
    public static final MapCodec<FoundryAlloyerBlock> CODEC = simpleCodec(FoundryAlloyerBlock::new);

    /** 创建带微型结构状态的合金炉。 */
    public FoundryAlloyerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false).setValue(IN_STRUCTURE, false));
    }

    /** 将合金炉的形成状态加入继承的设备方块状态。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(IN_STRUCTURE);
    }

    /** 放置时立即根据下方方块确定是否形成。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
            .setValue(IN_STRUCTURE, isFuelSource(context.getLevel().getBlockState(context.getClickedPos().below())));
    }

    /** 下方燃料来源变化时同步形成状态，拆除燃料来源还要立即关闭 active。 */
    @Override
    @Deprecated
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            boolean formed = isFuelSource(neighbor);
            state = state.setValue(IN_STRUCTURE, formed);
            if (!formed) {
                state = state.setValue(ACTIVE, false);
            }
        }
        return state;
    }

    /** 任意非下方邻居变化都会使输入储罐缓存失效，下方变化还会触发结构重检。 */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FoundryBlockEntity entity) {
            entity.markAlloyerNeighborDirty();
        }
    }

    /** 燃料标签允许数据包扩展加热器和燃料罐，不把 Mantle 或具体方块 ID 写死在逻辑里。 */
    private static boolean isFuelSource(BlockState state) {
        return state.is(StructureTags.FUEL_TANKS);
    }

    /** 返回专用方块状态编解码器。 */
    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
}
