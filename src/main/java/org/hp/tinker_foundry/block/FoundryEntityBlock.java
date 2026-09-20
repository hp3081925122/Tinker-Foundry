package org.hp.tinker_foundry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.item.PortableTankFluidHandler;
import org.hp.tinker_foundry.item.PortableTankItem;
import org.hp.tinker_foundry.item.FoundryTankItem;
import org.hp.tinker_foundry.menu.FoundryMenu;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.network.FoundryNetworking;

/** 冶炼设备共用的方块实体宿主；具体行为由方块注册名决定。 */
public class FoundryEntityBlock extends BaseEntityBlock {
    /** 设备是否正在处理配方或燃烧燃料，供 active 模型状态使用。 */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** 使用原版方块属性编解码器，避免引入额外配置格式。 */
    public static final MapCodec<FoundryEntityBlock> CODEC = simpleCodec(FoundryEntityBlock::new);
    /** 浇注盆与模型一致的底部支脚、外壁和内部空腔碰撞体积。 */
    private static final VoxelShape CASTING_BASIN_SHAPE = Shapes.join(
        Shapes.block(),
        Shapes.or(
            Block.box(0, 0, 5, 16, 2, 11),
            Block.box(5, 0, 0, 11, 2, 16),
            Block.box(2, 4, 2, 14, 16, 14)
        ),
        BooleanOp.ONLY_FIRST
    );
    /** 浇注台与模型一致的四角支脚、台面和顶边碰撞体积。 */
    private static final VoxelShape CASTING_TABLE_SHAPE = Shapes.join(
        Shapes.block(),
        Shapes.or(
            Block.box(4, 0, 0, 12, 10, 16),
            Block.box(0, 0, 4, 16, 10, 12),
            Block.box(1, 15, 1, 15, 16, 15)
        ),
        BooleanOp.ONLY_FIRST
    );

    /** 创建冶炼设备方块。 */
    public FoundryEntityBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** 返回与匠魂模型相同的浇注设备选择箱和碰撞箱，其他实体设备保持完整方块。 */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.is(TFBlocks.SEARED_BASIN.get()) || state.is(TFBlocks.SCORCHED_BASIN.get())) {
            return CASTING_BASIN_SHAPE;
        }
        if (state.is(TFBlocks.SEARED_TABLE.get()) || state.is(TFBlocks.SCORCHED_TABLE.get())) {
            return CASTING_TABLE_SHAPE;
        }
        return super.getShape(state, level, pos, context);
    }

    /** 空手取出设备已经完成的物品。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoundryBlockEntity entity)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            // 匠魂官方控制器在结构无效时只显示具体错误，不允许进入控制器界面。
            if (entity.isStructureController()) {
                entity.refreshStructureIfDirty();
                if (!entity.isStructureValid()) {
                    Component message = entity.structureErrorMessage();
                    player.displayClientMessage(message, true);
                    FoundryNetworking.syncStructureError(entity);
                    TinkerFoundry.LOGGER.debug("[interaction] blocked invalid controller={} reason={} errorPos={}",
                        pos, entity.structureErrorReason(), entity.structureErrorPos());
                    return InteractionResult.SUCCESS;
                }
            }
            // 浇注口空手右键始终视为一次启动或停止操作，即使当前没有可转移流体也要消费点击。
            if (entity.isFaucetBlock()) {
                entity.activateFaucet();
                return InteractionResult.SUCCESS;
            }
            // 代理储罐和流体炮的空手区域用于取出内部物品，不能落入普通菜单逻辑。
            if (entity.isProxyTankBlock()) {
                // 代理储罐四角是液体区域；空手点击四角时保留容器，不把它误当成物品槽取出。
                if (!isProxyTankFluidArea(hit, pos) && entity.swapSpecialItem(player, InteractionHand.MAIN_HAND)) {
                    return InteractionResult.SUCCESS;
                }
            } else if (entity.swapSpecialItem(player, InteractionHand.MAIN_HAND)) {
                return InteractionResult.SUCCESS;
            }
            // 浇注台和浇注盆才允许空手取出铸造结果，普通冶炼设备的产物必须从菜单槽取出。
            if (entity.isCastingBlock()) {
                ItemStack output = entity.takeOutput();
                if (!output.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(output);
                    return InteractionResult.SUCCESS;
                }
            }
            // 只有上游对应的熔炼设备拥有菜单，浇注台、储液罐和流体附件保持直接交互。
            if (entity.hasMenuScreen() && player instanceof ServerPlayer serverPlayer) {
                return openMenu(state, pos, entity, serverPlayer);
            }
            // 非菜单附件不消费空手右键，交给原版继续处理，避免制造“右键成功但没有效果”的假交互。
            return InteractionResult.PASS;
        }
        // 客户端只为真正会打开菜单或执行专用交互的方块确认点击。
        return entity.hasMenuScreen() || entity.isCastingBlock() || entity.isProxyTankBlock() || entity.isFluidCannonBlock()
            || entity.isFaucetBlock()
            || state.is(org.hp.tinker_foundry.registry.TFBlocks.SEARED_FAUCET.get())
            || state.is(org.hp.tinker_foundry.registry.TFBlocks.SCORCHED_FAUCET.get())
            ? InteractionResult.sidedSuccess(true) : InteractionResult.PASS;
    }

    /** 处理桶装流体、燃料和物品输入，所有修改只在服务端执行。 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoundryBlockEntity entity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 浇注口的非空手交互只切换浇注状态，不把手持物误传入自身缓存。
        if (entity.isFaucetBlock()) {
            if (player.isShiftKeyDown()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            entity.activateFaucet();
            return ItemInteractionResult.SUCCESS;
        }
        // 菜单设备的普通手持物只负责打开菜单，不再触发基类的隐式物品插入。
        boolean fluidContainer = net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent();
        if (entity.hasMenuScreen() && !fluidContainer) {
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true);
            }
            return player instanceof ServerPlayer serverPlayer
                ? itemInteractionResult(openMenu(state, pos, entity, serverPlayer))
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 代理储罐按 Mantle 的中心物品区和四角液体区分别处理交互，失败的流体交互可继续交换容器。
        if (entity.isProxyTankBlock()) {
            if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
            return handleProxyTankItem(stack, pos, hit, level, player, hand, entity, fluidContainer);
        }
        // 流体炮只接受非流体物品作为内部弹药或展示物。
        if (!fluidContainer && entity.isFluidCannonBlock()) {
            if (level.isClientSide) return ItemInteractionResult.sidedSuccess(true);
            return entity.swapSpecialItem(player, hand)
                ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 浇注台和浇注盆保留自身的物品交互，流体容器则进入同一套受限流体传输逻辑。
        if (!fluidContainer && !entity.isCastingBlock() && !entity.isFuelTankBlock()) {
            TinkerFoundry.LOGGER.debug("[interaction] ignored item={} on block={} at {}", stack.getItem(), state.getBlock(), pos);
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        return handleItemOn(stack, state, level, pos, player, hand, entity);
    }

    /** 把空手交互结果转换为 NeoForge 1.21.1 的物品交互结果。 */
    private static ItemInteractionResult itemInteractionResult(InteractionResult result) {
        return result == InteractionResult.SUCCESS
            ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 只为真正拥有界面的冶炼设备打开菜单，并在打开前重新确认控制器结构。 */
    private static InteractionResult openMenu(BlockState state, BlockPos pos, FoundryBlockEntity entity, ServerPlayer player) {
        if (entity.isStructureController()) {
            entity.refreshStructureIfDirty();
            if (!entity.isStructureValid()) {
                Component message = entity.structureErrorMessage();
                player.displayClientMessage(message, true);
                FoundryNetworking.syncStructureError(entity);
                TinkerFoundry.LOGGER.debug("[interaction] blocked invalid controller={} reason={} errorPos={}",
                    pos, entity.structureErrorReason(), entity.structureErrorPos());
                return InteractionResult.SUCCESS;
            }
        }
        TinkerFoundry.LOGGER.debug("[interaction] opening menu block={} pos={} screenKind={} structureValid={} dataCount={}",
            state.getBlock(), pos, entity.screenKind(), entity.isStructureValid(), FoundryBlockEntity.MENU_DATA_COUNT);
        // 控制器菜单标题沿用匠魂的设备名称，不把内部方块注册名“控制器”显示给玩家。
        Component menuTitle = entity.isSmelteryController()
            ? Component.translatable("gui.tinker_foundry.smeltery")
            : entity.isFoundryController()
                ? Component.translatable("gui.tinker_foundry.foundry")
                : Component.translatable(state.getBlock().getDescriptionId());
        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, ignored) -> new FoundryMenu(containerId, inventory, entity),
            menuTitle
        ), buffer -> buffer.writeBlockPos(pos).writeVarInt(entity.screenKind()).writeVarInt(entity.inputSlotCount()));
        // 打开菜单后立即补发一次流体和结构快照，避免等待下一次进度变化。
        FoundryNetworking.sync(entity);
        return InteractionResult.SUCCESS;
    }

    /** 统一处理设备和冶炼灯的容器交互，避免两个方块类出现不同的流体规则。 */
    static ItemInteractionResult handleItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, FoundryBlockEntity entity) {
        if (net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent()) {
            // 普通空桶也先模拟完整传输，禁止不足一桶时先扣液再返回失败。
            var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, null);
            if (handler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            return net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, handler)
                ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        }
        // 非流体物品只能进入代理储罐或流体炮的专用内部槽。
        if (entity.isProxyTankBlock() || entity.isFluidCannonBlock()) {
            return entity.swapSpecialItem(player, hand)
                ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        }
        // 只有浇注台和浇注盆允许普通手持物走专用铸造输入逻辑。
        if ((entity.isCastingBlock() || entity.isFuelTankBlock()) && entity.insertItem(stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 处理代理储罐的中心物品槽和四角流体槽，替代 Mantle InventoryBlock 的点击区域判断。 */
    private static ItemInteractionResult handleProxyTankItem(ItemStack stack, BlockPos pos, BlockHitResult hit,
                                                              Level level, Player player, InteractionHand hand,
                                                              FoundryBlockEntity entity, boolean fluidContainer) {
        boolean clickedFluid = isProxyTankFluidArea(hit, pos);
        if (fluidContainer) {
            var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, null);
            if (handler != null && net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, handler)) {
                return ItemInteractionResult.SUCCESS;
            }
            // 已有内部容器时，点击四角只表示液体槽交互失败，不应意外替换容器。
            if (clickedFluid && !entity.getSpecialItem().isEmpty()) {
                return ItemInteractionResult.SUCCESS;
            }
        }
        // 空内部槽可以从任意点击面放入有效流体容器；已有容器只有中心区域允许交换。
        if (!clickedFluid || entity.getSpecialItem().isEmpty()) {
            return entity.swapSpecialItem(player, hand)
                ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** 判断代理储罐点击是否落在四角液体区域，中心十字区域属于内部物品槽。 */
    private static boolean isProxyTankFluidArea(BlockHitResult hit, BlockPos pos) {
        if (hit.getDirection() == net.minecraft.core.Direction.DOWN) {
            return false;
        }
        double x = hit.getLocation().x - pos.getX();
        double z = hit.getLocation().z - pos.getZ();
        boolean corner = x < 5.0 / 16.0 || x > 11.0 / 16.0 || z < 5.0 / 16.0 || z > 11.0 / 16.0;
        if (!corner || hit.getDirection() == net.minecraft.core.Direction.UP) {
            return corner;
        }
        return hit.getLocation().y - pos.getY() > 0.25;
    }

    /** 邻居变化时同步浇注口的红石边沿，其他统一设备不参与该状态机。 */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FoundryBlockEntity entity && entity.isFaucetBlock()) {
            entity.handleFaucetRedstone(level.hasNeighborSignal(pos));
        }
    }

    /** 红石延迟 tick 到达后启动浇注口，等价于原版方块层调度。 */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FoundryBlockEntity entity && entity.isFaucetBlock()) {
            entity.activateFaucet();
        }
    }


    /** 返回方块自身的编解码器。 */
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** 为所有设备创建独立的持久化方块实体。 */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryBlockEntity(TFBlockEntities.GENERIC.get(), pos, state);
    }

    /** 放置带流体的专用储液罐时恢复物品中的流体组件。 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.getItem() instanceof FoundryTankItem tankItem
            && level.getBlockEntity(pos) instanceof FoundryBlockEntity entity) {
            net.neoforged.neoforge.fluids.FluidStack stored = tankItem.getFluid(stack);
            TinkerFoundry.LOGGER.debug("[tank] placement callback at {}, item fluid={} mB, state={}", pos, stored.getAmount(), state.getBlock());
            entity.setTankFluid(stored);
        }
    }

    /** 破坏专用储液罐时掉落带有当前流体组件的同类物品。 */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (blockEntity instanceof FoundryBlockEntity entity && entity.isProxyTankBlock()) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            if (level instanceof ServerLevel serverLevel && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                Block.popResource(level, pos, new ItemStack(this));
                if (!entity.getSpecialItem().isEmpty()) Block.popResource(level, pos, entity.getSpecialItem());
            }
            return;
        }
        if (!(blockEntity instanceof FoundryBlockEntity entity) || !entity.isTankBlock()) {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
            return;
        }
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        if (level instanceof ServerLevel serverLevel && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            ItemStack drop = new ItemStack(this);
            FoundryTankItem.setFluid(drop, entity.getTankFluid());
            Block.popResource(level, pos, drop);
            // 流体炮的内部展示物不是方块流体组件，破坏时必须像匠魂原版一样单独掉落。
            if (entity.isFluidCannonBlock() && !entity.getSpecialItem().isEmpty()) {
                Block.popResource(level, pos, entity.getSpecialItem());
            }
        }
    }

    /** 使用精准采集或中键取块时保留专用储液罐的流体。 */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof FoundryBlockEntity entity && entity.isTankBlock()) {
            FoundryTankItem.setFluid(stack, entity.getTankFluid());
        }
        return stack;
    }

    /** 服务端只在方块实体所在区块加载时执行设备 tick。 */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        BlockEntityTicker<FoundryBlockEntity> ticker = level.isClientSide
            ? FoundryBlockEntity::clientTick : FoundryBlockEntity::serverTick;
        return (BlockEntityTicker<T>) (BlockEntityTicker<?>) ticker;
    }

    /** 设备需要方块实体渲染器时再由客户端注册具体模型。 */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** 仅让匠魂原版有比较器语义的设备响应红石比较器。 */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return state.is(TFBlocks.SMELTERY_CONTROLLER.get()) || state.is(TFBlocks.FOUNDRY_CONTROLLER.get())
            || state.is(TFBlocks.SEARED_MELTER.get()) || state.is(TFBlocks.SCORCHED_ALLOYER.get()) || state.is(TFBlocks.SEARED_HEATER.get())
            || state.is(TFBlocks.SEARED_TABLE.get()) || state.is(TFBlocks.SCORCHED_TABLE.get())
            || state.is(TFBlocks.SEARED_BASIN.get()) || state.is(TFBlocks.SCORCHED_BASIN.get())
            || state.is(TFBlocks.SEARED_INGOT_TANK.get()) || state.is(TFBlocks.SCORCHED_INGOT_TANK.get())
            || state.is(TFBlocks.SEARED_FUEL_TANK.get()) || state.is(TFBlocks.SCORCHED_FUEL_TANK.get())
            || state.is(TFBlocks.SEARED_CASTING_TANK.get())
            || state.is(TFBlocks.SEARED_LANTERN.get()) || state.is(TFBlocks.SCORCHED_LANTERN.get())
            || state.is(TFBlocks.SCORCHED_PROXY_TANK.get())
            || state.is(TFBlocks.SEARED_FLUID_CANNON.get()) || state.is(TFBlocks.SCORCHED_FLUID_CANNON.get());
    }

    /** 把统一方块实体的实际比较器强度接入原版比较器查询。 */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FoundryBlockEntity entity ? entity.comparatorStrength() : 0;
    }
}
