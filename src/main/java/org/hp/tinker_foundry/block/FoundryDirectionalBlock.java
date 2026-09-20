package org.hp.tinker_foundry.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** 为浇注口、流体炮、流体计和兼容附件提供六向朝向状态。 */
public class FoundryDirectionalBlock extends FoundryEntityBlock {
    /** 设备的水平输入或输出方向。 */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = DirectionalBlock.FACING;

    /** 创建带方向状态的设备方块。 */
    public FoundryDirectionalBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    /** 按匠魂原版规则让水平端口朝向放置玩家。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** 将方向属性加入方块状态定义。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
