package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** 为排液口、输导孔和导流槽提供与匠魂一致的四向水平朝向。 */
public class FoundryHorizontalBlock extends FoundryEntityBlock {
    /** 水平端口朝向。 */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** 创建默认朝北的水平设备。 */
    public FoundryHorizontalBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    /** 按玩家放置方向让端口正面朝向玩家。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** 将水平朝向加入方块状态。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /** 返回方块编解码器，保持 NeoForge 1.21.1 方块序列化契约。 */
    @Override
    protected MapCodec<? extends FoundryEntityBlock> codec() {
        return simpleCodec(FoundryHorizontalBlock::new);
    }
}
