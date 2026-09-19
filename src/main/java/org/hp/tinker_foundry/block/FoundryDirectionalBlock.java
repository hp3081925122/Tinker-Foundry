package org.hp.tinker_foundry.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** 为排液口、浇注口和管道提供六向朝向状态。 */
public class FoundryDirectionalBlock extends FoundryEntityBlock {
    /** 设备的输入或输出方向。 */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = DirectionalBlock.FACING;

    /** 创建带方向状态的设备方块。 */
    public FoundryDirectionalBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    /** 根据玩家放置时面对的方向设置设备朝向。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    /** 将方向属性加入方块状态定义。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
