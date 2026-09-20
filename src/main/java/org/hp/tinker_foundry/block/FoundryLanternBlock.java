package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.item.FoundryTankItem;
import org.hp.tinker_foundry.registry.TFBlockEntities;

/** 带独立流体槽的冶炼灯，保留原版灯笼的悬挂和碰撞行为。 */
public final class FoundryLanternBlock extends LanternBlock implements EntityBlock {
    /** 灯笼内部允许保存的流体数量，和官方 1.21.1 基线一致。 */
    public static final int CAPACITY = 50;
    /** 灯笼根据流体类型保存的光照等级。 */
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    /** 使用原版方块编解码器，避免引入额外模型或配置加载器。 */
    public static final MapCodec<LanternBlock> CODEC = simpleCodec(FoundryLanternBlock::new);

    /** 创建带动态光照状态的独立冶炼灯。 */
    public FoundryLanternBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIGHT, 0));
    }

    /** 补充灯笼流体光照状态。 */
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT);
    }

    /** 为冶炼灯创建通用流体方块实体。 */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryBlockEntity(TFBlockEntities.GENERIC.get(), pos, state);
    }

    /** 放置带流体的灯笼时先把流体光照写入方块状态。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || !(context.getItemInHand().getItem() instanceof FoundryTankItem tankItem)) {
            return state;
        }
        FluidStack fluid = tankItem.getFluid(context.getItemInHand());
        return state.setValue(LIGHT, lightLevel(fluid));
    }

    /** 放置后恢复灯笼物品中的流体组件。 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof FoundryBlockEntity entity
            && stack.getItem() instanceof FoundryTankItem tankItem) {
            entity.setTankFluid(tankItem.getFluid(stack));
        }
    }

    /** 灯笼复用冶炼设备的服务端流体交互，不打开不存在的菜单。 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoundryBlockEntity entity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        return FoundryEntityBlock.handleItemOn(stack, state, level, pos, player, hand, hit, entity);
    }

    /** 灯笼破坏时掉落保留流体组件的同类方块物品。 */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!(blockEntity instanceof FoundryBlockEntity entity)) {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
            return;
        }
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (level instanceof ServerLevel serverLevel
            && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ItemStack drop = new ItemStack(this);
            FoundryTankItem.setFluid(drop, entity.getTankFluid());
            Block.popResource(level, pos, drop);
        }
    }

    /** 中键取块时保留灯笼中的流体。 */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof FoundryBlockEntity entity) {
            FoundryTankItem.setFluid(stack, entity.getTankFluid());
        }
        return stack;
    }

    /** 为客户端和服务端提供通用方块实体 tick。 */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        BlockEntityTicker<FoundryBlockEntity> ticker = level.isClientSide
            ? FoundryBlockEntity::clientTick : FoundryBlockEntity::serverTick;
        return (BlockEntityTicker<T>) (BlockEntityTicker<?>) ticker;
    }

    /** 返回当前流体为灯笼提供的光照等级。 */
    public static int lightLevel(FluidStack fluid) {
        return fluid.isEmpty() ? 0 : Math.max(0, Math.min(15, fluid.getFluid().getFluidType().getLightLevel(fluid)));
    }

    /** 冶炼灯复用匠魂储罐的比较器语义，空灯输出零，装液量按容量比例输出。 */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** 从通用方块实体读取冶炼灯当前的比较器强度。 */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FoundryBlockEntity entity ? entity.comparatorStrength() : 0;
    }

    /** 返回方块自身的编解码器。 */
    @Override
    public MapCodec<LanternBlock> codec() {
        return CODEC;
    }
}
