package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 可储存流体并受红石触发的流体炮。 */
public final class FoundryFluidCannonBlock extends FoundryDirectionalBlock {
    /** 红石边沿触发状态。 */
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    /** 流体炮编解码器。 */
    public static final MapCodec<FoundryFluidCannonBlock> CODEC = simpleCodec(FoundryFluidCannonBlock::new);
    /** 流体炮强度，决定一次发射消耗量和效果强度。 */
    private final float power;
    /** 流体炮弹射速度。 */
    private final float velocity;
    /** 流体炮弹射散布。 */
    private final float inaccuracy;

    /** 创建流体炮。 */
    public FoundryFluidCannonBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        this(properties, 1.0F, 1.1F, 6.0F);
    }

    /** 创建带有匠魂原版弹射参数的流体炮。 */
    public FoundryFluidCannonBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties,
                                   float power, float velocity, float inaccuracy) {
        super(properties);
        this.power = power;
        this.velocity = velocity;
        this.inaccuracy = inaccuracy;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
    }

    /** 返回流体炮强度。 */
    public float power() {
        return power;
    }

    /** 返回流体炮弹射速度。 */
    public float velocity() {
        return velocity;
    }

    /** 返回流体炮弹射散布。 */
    public float inaccuracy() {
        return inaccuracy;
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

    /** 复刻匠魂流体炮的上半部流体区和下半部物品区点击分区。 */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                                   BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                                   InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoundryBlockEntity entity)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean clickedTank = clickedTankArea(state, pos, hit);
        if (level.isClientSide) {
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(true);
        }
        entity.interactFluidCannon(player, hand, clickedTank);
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    /** 空手点击时沿用流体炮分区，避免基类把上半部误当成内部物品槽。 */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                    net.minecraft.world.entity.player.Player player,
                                                                    BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoundryBlockEntity entity)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        boolean clickedTank = clickedTankArea(state, pos, hit);
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.sidedSuccess(true);
        }
        entity.interactFluidCannon(player, InteractionHand.MAIN_HAND, clickedTank);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    /** 判断当前点击是否命中流体区；朝上放置时顶面边缘保留喷口周围物品区。 */
    private static boolean clickedTankArea(BlockState state, BlockPos pos, BlockHitResult hit) {
        Vec3 location = hit.getLocation();
        boolean clickedTank = location.y - pos.getY() > 0.5D;
        if (clickedTank && hit.getDirection() == Direction.UP && state.getValue(FACING) == Direction.UP) {
            double x = location.x - pos.getX();
            double z = location.z - pos.getZ();
            clickedTank = 0.25D > x || x > 0.75D || 0.25D > z || z > 0.75D;
        }
        return clickedTank;
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
