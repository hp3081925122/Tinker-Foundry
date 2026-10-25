package org.hp.tinker_foundry.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** 为小型熔炼器、加热器和合金炉提供与官方模型匹配的水平朝向。 */
public class FoundryMachineBlock extends FoundryEntityBlock {
    /** 三种独立设备共用的水平朝向属性。 */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** 使用 Forge 1.20.1 的方块构造器创建设备。 */
    /** 创建带正面朝向的独立设备方块。 */
    public FoundryMachineBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    /** 按玩家水平观察方向确定设备正面。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** 将水平朝向加入三种设备的方块状态。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ACTIVE);
    }

    /** 返回设备自身的方块编解码器。 */}
