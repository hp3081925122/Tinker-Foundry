package org.hp.tinker_foundry.block.entity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.common.StructureFluidTank;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.block.FoundryLanternBlock;
import org.hp.tinker_foundry.multiblock.FoundryMultiblock;
import org.hp.tinker_foundry.multiblock.SmelteryMultiblock;
import org.hp.tinker_foundry.multiblock.StructureErrorReason;
import org.hp.tinker_foundry.multiblock.StructureResult;
import org.hp.tinker_foundry.network.FoundryNetworking;
import org.hp.tinker_foundry.network.FoundryStructureErrorPayload;
import org.hp.tinker_foundry.network.FoundryStatePayload;
import org.hp.tinker_foundry.recipe.AlloyingRecipe;
import org.hp.tinker_foundry.recipe.CastingRecipe;
import org.hp.tinker_foundry.recipe.FluidRecipeInput;
import org.hp.tinker_foundry.recipe.FuelRecipe;
import org.hp.tinker_foundry.recipe.MeltingRecipe;
import org.hp.tinker_foundry.recipe.MoldingRecipe;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 冶炼设备的服务端权威状态、配方进度、流体槽和物品槽。 */
public final class FoundryBlockEntity extends BlockEntity implements IFluidHandler, Container {
    /** 基础设备容器预留的输入编号段；不是多方块输入上限。 */
    public static final int BASE_INPUT_SLOTS = 28;
    /** 菜单基础状态字段数量，保留旧字段顺序以兼容现有客户端逻辑。 */
    public static final int MENU_BASE_DATA_COUNT = 18;
    /** 菜单同步的燃料温度字段编号。 */
    public static final int MENU_FUEL_TEMPERATURE_INDEX = MENU_BASE_DATA_COUNT;
    /** 菜单同步的逐槽进度字段起始编号。 */
    public static final int MENU_INPUT_PROGRESS_START = MENU_FUEL_TEMPERATURE_INDEX + 1;
    /** 菜单同步的逐槽配方时间字段起始编号。 */
    public static final int MENU_INPUT_TIME_START = MENU_INPUT_PROGRESS_START + BASE_INPUT_SLOTS;
    /** 菜单同步的逐槽所需温度字段起始编号。 */
    public static final int MENU_INPUT_TEMPERATURE_START = MENU_INPUT_TIME_START + BASE_INPUT_SLOTS;
    /** 菜单同步的逐槽处理状态字段起始编号。 */
    public static final int MENU_INPUT_STATUS_START = MENU_INPUT_TEMPERATURE_START + BASE_INPUT_SLOTS;
    /** 输入槽状态字段结束位置，后面追加燃料栏显示字段。 */
    public static final int MENU_INPUT_DATA_COUNT = MENU_INPUT_STATUS_START + BASE_INPUT_SLOTS;
    /** 菜单同步的燃料流体数量字段编号。 */
    public static final int MENU_FUEL_AMOUNT_INDEX = MENU_INPUT_DATA_COUNT;
    /** 菜单同步的燃料流体容量字段编号。 */
    public static final int MENU_FUEL_CAPACITY_INDEX = MENU_FUEL_AMOUNT_INDEX + 1;
    /** 菜单同步的燃料流体注册表编号字段。 */
    public static final int MENU_FUEL_FLUID_INDEX = MENU_FUEL_CAPACITY_INDEX + 1;
    /** 菜单同步的燃料来源存在字段编号。 */
    public static final int MENU_FUEL_SOURCE_INDEX = MENU_FUEL_FLUID_INDEX + 1;
    /** 菜单同步的当前燃烧段总量，用于火焰比例而不是燃料罐液面。 */
    public static final int MENU_FUEL_DURATION_INDEX = MENU_FUEL_SOURCE_INDEX + 1;
    /** 菜单状态字段总数。 */
    public static final int MENU_DATA_COUNT = MENU_FUEL_DURATION_INDEX + 1;
    /** 四个整数同步一个输入，保证数据槽编号不超出原版短整数边界。 */
    public static final int MAX_STRUCTURE_INPUTS = 14 * 14 * 63;
    /** 逐槽状态：没有输入或没有可用配方。 */
    public static final int INPUT_STATUS_EMPTY = 0;
    /** 逐槽状态：正在处理。 */
    public static final int INPUT_STATUS_PROCESSING = 1;
    /** 逐槽状态：燃料温度不足。 */
    public static final int INPUT_STATUS_NO_HEAT = 2;
    /** 逐槽状态：输出流体空间不足。 */
    public static final int INPUT_STATUS_NO_SPACE = 3;
    /** 逐槽状态：输入物品没有熔炼配方。 */
    public static final int INPUT_STATUS_UNMELTABLE = 4;
    /** 合金炉最多同时接受的流体输入数量，满足二元及三元、四元合金。 */
    public static final int MAX_ALLOY_INPUTS = 4;
    /** 合金炉输出流体槽编号。 */
    public static final int ALLOY_OUTPUT_TANK = MAX_ALLOY_INPUTS;
    /** 统一菜单中的固体燃料槽编号。 */
    public static final int FUEL_SLOT = BASE_INPUT_SLOTS;
    /** 统一菜单中的产物槽编号。 */
    public static final int OUTPUT_SLOT = FUEL_SLOT + 1;
    /** 模具配方容器返还物品槽编号。 */
    public static final int REMAINDER_SLOT = OUTPUT_SLOT + 1;
    /** 统一菜单中的设备槽数量。 */
    public static final int CONTAINER_SIZE = REMAINDER_SLOT + 1;
    /** 普通设备的默认容量。 */
    public static final int DEFAULT_CAPACITY = 4000;
    /** 物品输入槽，多方块炉按结构内部空间启用多个槽位。 */
    private ItemStack[] inputs = emptyInputs();
    /** 已分配的真实多方块槽数，与基础设备编号预留区分开。 */
    private int structureInventorySize;
    /** 物品输出槽。 */
    private ItemStack output = ItemStack.EMPTY;
    /** 模具或容器配方完成后返还的物品槽。 */
    private ItemStack remainder = ItemStack.EMPTY;
    /** 多输入槽各自的熔炼进度，保证冶炼炉可以并行处理同一结构中的物品。 */
    private int[] inputProgress = new int[BASE_INPUT_SLOTS];
    /** 每个输入槽当前匹配配方的处理时长，用于客户端绘制逐槽进度条。 */
    private int[] inputRecipeTimes = new int[BASE_INPUT_SLOTS];
    /** 每个输入槽当前匹配配方的所需温度，用于客户端绘制热量状态。 */
    private int[] inputRequiredTemperatures = new int[BASE_INPUT_SLOTS];
    /** 每个输入槽当前处理状态，用于客户端区分无热量、无空间和无配方。 */
    private int[] inputStatuses = new int[BASE_INPUT_SLOTS];
    /** 记录进度变化，避免大炉体每刻复制整份进度数组用于比较。 */
    private long inputProgressVersion;
    /** 固体燃料槽。 */
    private ItemStack fuel = ItemStack.EMPTY;
    /** 普通设备的流体槽或合金设备的输出槽。 */
    private FluidStack fluid = FluidStack.EMPTY;
    /** 控制器共享容量的多流体存储，普通设备仍使用独立单槽。 */
    private final StructureFluidTank structureFluids = new StructureFluidTank();
    /** 加热器专用的流体燃料槽，和冶炼产物槽完全隔离。 */
    private FluidStack fuelFluid = FluidStack.EMPTY;
    /** 合金炉的四个独立输入槽。 */
    private FluidStack[] alloyInputs = emptyFluidInputs();
    /** 当前处理进度。 */
    private int progress;
    /** 当前处理配方的总时间。 */
    private int processTime;
    /** 当前燃料剩余 tick。 */
    private int burnTime;
    /** 当前燃料提供的温度。 */
    private int fuelTemperature;
    /** 每四刻施加一次的结构熔炼热量，由燃料配方决定。 */
    private int fuelHeatingRate;
    /** 同一周期的流体和物品变化合并为一次世界同步。 */
    private boolean worldSyncDirty;
    /** 当前流体燃料段剩余的总消耗量。 */
    private int fuelFluidConsumption;
    /** 当前流体燃料段已经消耗的总量。 */
    private int fuelFluidConsumed;
    /** 当前流体燃料段的总持续时间。 */
    private int fuelBurnDuration;
    /** 控制器每四刻扣除的燃烧量，由炉壁与底板面积决定。 */
    private int structureFuelRate = 1;
    /** 非法输入存档只报告一次，避免同一更新包持续刷屏。 */
    private boolean invalidInputReported;
    /** 最近一次结构容量。 */
    private int structureCapacity;
    /** 最近一次结构状态。 */
    private boolean structureValid;
    /** 最近一次结构的内部方块数。 */
    private int structureInteriorBlocks;
    /** 最近一次结构验证得到的边界，用于定位结构内燃料罐。 */
    private BlockPos structureMin;
    private BlockPos structureMax;
    /** 最近一次结构内发现的燃料罐位置。 */
    private BlockPos structureFuelTankPos;
    /** 验证结构时收集全部燃料罐，运行期间只访问缓存位置。 */
    private List<BlockPos> structureFuelTanks = List.of();
    /** 排液口由最近一次结构验证绑定控制器，不在每次抽取时搜索世界。 */
    private BlockPos drainControllerPos;
    /** 最近一次结构检查定位到的错误方块。 */
    private BlockPos structureErrorPos;
    /** 最近一次结构检查的错误类型。 */
    private StructureErrorReason structureErrorReason = StructureErrorReason.NONE;
    /** 客户端错误高亮剩余 tick，和匠魂官方的十秒显示时长一致。 */
    private int structureErrorVisibleFor;
    /** 结构变化事件触发的延迟校验标记，避免固定周期扫描世界。 */
    private boolean structureDirty = true;
    /** 导流设备距离下一次传输的服务端 tick 数。 */
    private int transferCooldown;
    /** 限制自定义状态载荷发送频率，进度仍会按短间隔同步。 */
    private int networkSyncCooldown;

    /** 通过注册器构造设备。 */
    public FoundryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // 固定数组让菜单在客户端和服务端拥有一致的槽位数量，实际启用数量由结构数据控制。
        for (int index = 0; index < inputs.length; index++) {
            inputs[index] = ItemStack.EMPTY;
        }
    }

    /** 方块实体注册器使用的标准构造器。 */
    public FoundryBlockEntity(BlockPos pos, BlockState state) {
        this(TFBlockEntities.GENERIC.get(), pos, state);
    }

    /** 服务端逐 tick 处理已加载设备，结构只在事件或区块加载后重新校验。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FoundryBlockEntity entity) {
        if (level.isClientSide) {
            return;
        }
        // 记录会随 tick 改变的持久化字段，空闲设备不再每 tick 触发区块保存。
        int oldProgress = entity.progress;
        int oldProcessTime = entity.processTime;
        int oldBurnTime = entity.burnTime;
        int oldFuelTemperature = entity.fuelTemperature;
        int oldStructureCapacity = entity.structureCapacity;
        boolean oldStructureValid = entity.structureValid;
        long oldInputProgressVersion = entity.inputProgressVersion;
        FluidStack oldFluid = entity.fluid.copy();
        FluidStack oldFuelFluid = entity.fuelFluid.copy();
        FluidStack[] oldAlloyInputs = entity.alloyInputs.clone();
        // 结构只在脏标记出现时检查；右键控制器也会调用同一入口，避免打开界面前读取旧状态。
        entity.refreshStructureIfDirty();
        entity.tickProcess(level);
        entity.updateActiveBlockState(level);
        if (entity.worldSyncDirty && level.getGameTime() % 4 == 3) {
            entity.worldSyncDirty = false;
            level.sendBlockUpdated(pos, state, entity.getBlockState(), Block.UPDATE_CLIENTS);
        }
        // 只有服务端状态发生变化时才写入区块脏标记，避免空设备产生持续存档开销。
        boolean stateChanged = oldProgress != entity.progress || oldProcessTime != entity.processTime || oldBurnTime != entity.burnTime
            || oldFuelTemperature != entity.fuelTemperature || oldStructureCapacity != entity.structureCapacity
            || oldStructureValid != entity.structureValid || oldInputProgressVersion != entity.inputProgressVersion
            || !FluidStack.matches(oldFluid, entity.fluid) || !FluidStack.matches(oldFuelFluid, entity.fuelFluid)
            || !alloyInputsMatch(oldAlloyInputs, entity.alloyInputs);
        if (stateChanged) {
            entity.setChanged();
        }
        if (entity.networkSyncCooldown > 0) {
            entity.networkSyncCooldown--;
        } else if (stateChanged) {
            // 自定义载荷补足多槽流体和结构状态，发送目标仅限当前打开该设备菜单的玩家。
            FoundryNetworking.sync(entity);
            entity.networkSyncCooldown = 2;
        }
    }

    /**
     * 在服务端立即处理待检查的结构，并在错误位置或原因变化时同步客户端。
     * 这个入口同时用于方块实体 tick 和控制器右键，确保无效结构不能绕过检查打开界面。
     */
    public void refreshStructureIfDirty() {
        if (level == null || level.isClientSide || !structureDirty) {
            return;
        }
        BlockPos oldErrorPos = structureErrorPos;
        StructureErrorReason oldErrorReason = structureErrorReason;
        refreshStructure(level);
        structureDirty = false;
        worldSyncDirty = true;
        if (!Objects.equals(oldErrorPos, structureErrorPos) || oldErrorReason != structureErrorReason) {
            // 结构错误只在变化时广播，避免每次控制器 tick 都向附近玩家发送载荷。
            FoundryNetworking.syncStructureError(this);
        }
    }

    /** 创建统一菜单所需的空输入数组。 */
    private static ItemStack[] emptyInputs() {
        ItemStack[] result = new ItemStack[BASE_INPUT_SLOTS];
        for (int index = 0; index < result.length; index++) {
            result[index] = ItemStack.EMPTY;
        }
        return result;
    }

    /** 创建合金炉使用的独立流体输入槽数组。 */
    private static FluidStack[] emptyFluidInputs() {
        FluidStack[] result = new FluidStack[MAX_ALLOY_INPUTS];
        for (int index = 0; index < result.length; index++) {
            result[index] = FluidStack.EMPTY;
        }
        return result;
    }

    /** 比较合金炉的全部输入槽，避免数组引用变化导致误判。 */
    private static boolean alloyInputsMatch(FluidStack[] first, FluidStack[] second) {
        if (first.length != second.length) {
            return false;
        }
        for (int index = 0; index < first.length; index++) {
            if (!FluidStack.matches(first[index], second[index])) {
                return false;
            }
        }
        return true;
    }

    /** 结构变化只重新检查控制器，普通单方块设备不扫描世界。 */
    private void refreshStructure(Level level) {
        if (isSmelteryController() || isFoundryController()) {
            StructureResult result = isFoundryController()
                ? FoundryMultiblock.validate(level, worldPosition)
                : SmelteryMultiblock.validate(level, worldPosition);
            structureValid = result.valid();
            structureCapacity = result.capacity();
            structureInteriorBlocks = result.interiorBlocks();
            // 有效炉体按真实炉腔体积扩缩容；临时损坏不销毁输入，等待修复或拆除。
            if (result.valid()) resizeStructureInputs(Math.min(MAX_STRUCTURE_INPUTS, result.interiorBlocks()), true);
            structureMin = result.valid() ? result.min() : null;
            structureMax = result.valid() ? result.max() : null;
            structureFuelTanks = result.valid() ? findStructureFuelTanks(level, result.min(), result.max()) : List.of();
            if (result.valid()) {
                // 直接使用结构检测器计算的燃料倍率，避免界面和燃烧各维护一套公式。
                structureFuelRate = result.fuelRate();
                for (BlockPos pos : BlockPos.betweenClosed(result.min(), result.max())) {
                    if (isStructurePart(pos) && level.getBlockEntity(pos) instanceof FoundryBlockEntity port
                        && (port.isDrain() || port.isTransferBlock())) {
                        port.drainControllerPos = worldPosition;
                        port.setChanged();
                    }
                }
            }
            if (structureFuelTankPos == null || !structureFuelTanks.contains(structureFuelTankPos)) {
                structureFuelTankPos = structureFuelTanks.isEmpty() ? null : structureFuelTanks.getFirst();
            }
            structureErrorPos = result.errorPos();
            structureErrorReason = result.errorReason();
            String errorBlock = structureErrorPos == null ? "none"
                : BuiltInRegistries.BLOCK.getKey(level.getBlockState(structureErrorPos).getBlock()).toString();
            TinkerFoundry.LOGGER.debug("[structure] controller={} type={} valid={} reason={} errorPos={} capacity={} interiorBlocks={} boundsMin={} boundsMax={} fuelTankPos={}",
                worldPosition, isFoundryController() ? "foundry" : "smeltery", structureValid, structureErrorReason,
                structureErrorPos, structureCapacity, structureInteriorBlocks, structureMin, structureMax, structureFuelTankPos);
            TinkerFoundry.LOGGER.debug("[structure] controller={} errorBlock={}", worldPosition, errorBlock);
        } else {
            structureValid = true;
            structureCapacity = DEFAULT_CAPACITY;
            structureInteriorBlocks = 1;
            structureErrorPos = null;
            structureErrorReason = StructureErrorReason.NONE;
            structureMin = null;
            structureMax = null;
            structureFuelTankPos = null;
        }
    }

    /** 在已通过验证的结构边界内收集全部燃料罐。 */
    private List<BlockPos> findStructureFuelTanks(Level level, BlockPos min, BlockPos max) {
        List<BlockPos> tanks = new java.util.ArrayList<>();
        if (min == null || max == null) {
            return tanks;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean foundCandidate = false;
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!isStructurePart(cursor) || !state.is(isFoundryController()
                        ? org.hp.tinker_foundry.multiblock.StructureTags.FOUNDRY_TANKS
                        : org.hp.tinker_foundry.multiblock.StructureTags.SMELTERY_TANKS)) {
                        continue;
                    }
                    foundCandidate = true;
                    boolean hasBlockEntity = level.getBlockEntity(cursor) instanceof FoundryBlockEntity;
                    TinkerFoundry.LOGGER.debug("[structure] fuel tank candidate pos={} block={} hasBlockEntity={}",
                        cursor, BuiltInRegistries.BLOCK.getKey(state.getBlock()), hasBlockEntity);
                    if (level.getBlockEntity(cursor) instanceof FoundryBlockEntity source && source.isTankBlock()) {
                        TinkerFoundry.LOGGER.debug("[structure] fuel tank accepted pos={} fluid={} amount={}",
                            cursor, source.getFluidInTank(0).getFluid(), source.getFluidInTank(0).getAmount());
                        tanks.add(cursor.immutable());
                    }
                }
            }
        }
        if (!foundCandidate) {
            TinkerFoundry.LOGGER.debug("[structure] no fuel tank candidate inside bounds min={} max={}", min, max);
        } else if (tanks.isEmpty()) {
            TinkerFoundry.LOGGER.debug("[structure] fuel tank candidates existed but none were accepted inside bounds min={} max={}", min, max);
        }
        TinkerFoundry.LOGGER.debug("[structure] collected fuel tanks count={} positions={}", tanks.size(), tanks);
        return List.copyOf(tanks);
    }

    /** 客户端逐 tick 递减结构错误高亮计时。 */
    public static void clientTick(Level level, BlockPos pos, BlockState state, FoundryBlockEntity entity) {
        if (entity.structureErrorVisibleFor > 0) {
            entity.structureErrorVisibleFor--;
        }
    }

    /** 由方块变化和区块加载事件标记结构，下一次设备 tick 在本区块内完成校验。 */
    public void markStructureDirty() {
        if (isSmelteryController() || isFoundryController()) {
            structureDirty = true;
        }
    }

    /** 方块实体重新加载后必须重新确认周围结构，不能沿用旧的扫描缓存。 */
    @Override
    public void onLoad() {
        super.onLoad();
        structureDirty = true;
        updateLanternLight();
    }

    /** 根据宿主方块选择熔炼、合金或浇注处理器。 */
    private void tickProcess(Level level) {
        if (!structureValid) {
            progress = 0;
            processTime = 0;
            // 结构损坏时保留尚未耗尽燃料的热量，燃料耗尽后每四刻冷却一次。
            if (isStructureController()) {
                if (level.getGameTime() % 4 == 1 && burnTime <= 0) {
                    for (int index = 0; index < inputs.length; index++) {
                        if (inputStatuses[index] != INPUT_STATUS_NO_SPACE) setInputProgress(index, Math.max(0, inputProgress[index] - 5));
                    }
                }
                consumeBurningFuel();
                return;
            }
            // 无效结构只在进度实际清空时标记改变，避免空闲时持续同步。
            if (java.util.Arrays.stream(inputProgress).anyMatch(value -> value != 0)) {
                java.util.Arrays.fill(inputProgress, 0);
                inputProgressVersion++;
            }
            java.util.Arrays.fill(inputRecipeTimes, 0);
            java.util.Arrays.fill(inputRequiredTemperatures, 0);
            java.util.Arrays.fill(inputStatuses, INPUT_STATUS_EMPTY);
            // 结构损坏不再获取新燃料，但已经燃烧的热量仍按原速耗尽。
            consumeBurningFuel();
            return;
        }
        if (isAlloyer()) {
            tickAlloying(level);
        } else if (isCastingBlock()) {
            tickCasting(level);
        } else if (isCastingTankBlock()) {
            tickCastingTank(level);
        } else if (isMeltingBlock()) {
            // 每秒检查一次炉腔实体；无燃料时仍吸入物品，生物燃料由tickEntities单独判断。
            if (isStructureController() && level.getGameTime() % 20 == 12) tickEntities();
            // 第零相位仅在确实有可加热物品或可合金配方时取得新燃料。
            if (level.getGameTime() % 4 == 0 && burnTime <= 0) {
                int possible = findHeatingFuel(false);
                boolean needed = false;
                if (possible > 0) {
                    for (int index = 0; index < inputSlotCount(); index++) {
                        if (inputs[index].isEmpty() || inputStatuses[index] == INPUT_STATUS_NO_SPACE) continue;
                        Optional<RecipeHolder<MeltingRecipe>> candidate = level.getRecipeManager().getRecipeFor(
                            TFRecipes.MELTING.get(), new SingleRecipeInput(inputs[index]), level);
                        if (candidate.isPresent() && candidate.get().value().temperature() <= possible) {
                            needed = true;
                            break;
                        }
                    }
                    if (!needed && isSmelteryController()) needed = runStructureAlloying(level, true, possible);
                }
                if (needed) findHeatingFuel(true);
            }
            // 多方块按四刻周期加热，独立设备暂时保留各自原有处理周期。
            if (level.getGameTime() % 4 == (isStructureController() ? 1 : 2)) tickMelting(level);
            // 冶炼炉允许原地合金；铸造炉按原版设计保留分离产物，不自动合金。
            if (isSmelteryController() && burnTime > 0 && level.getGameTime() % 4 == 2) runStructureAlloying(level, false, fuelTemperature);
        } else if (isFaucet()) {
            tickFaucet(level);
        }
        consumeBurningFuel();
    }

    /** 根据服务端实际处理状态切换 active 方块模型，避免客户端自行猜测机器状态。 */
    private void updateActiveBlockState(Level level) {
        BlockState state = getBlockState();
        if (!state.hasProperty(org.hp.tinker_foundry.block.FoundryEntityBlock.ACTIVE)) {
            return;
        }
        boolean active = ((isHeater() || isStructureController()) && burnTime > 0)
            || (!isStructureController() && (isMeltingBlock() || isAlloyer()) && structureValid && progress > 0 && processTime > 0);
        if (state.getValue(org.hp.tinker_foundry.block.FoundryEntityBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(org.hp.tinker_foundry.block.FoundryEntityBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    /** 控制器每四刻按炉体规模消耗热量；独立加热器保持逐刻流体燃烧。 */
    private void consumeBurningFuel() {
        if (burnTime <= 0) {
            return;
        }
        // 控制器的 duration 是燃烧量，不是游戏刻数；液体已在点火时一次扣除。
        if (isMeltingBlock()) {
            if (level != null && level.getGameTime() % 4 == (isStructureController() ? 3 : 2)) {
                burnTime = Math.max(0, burnTime - (isStructureController() ? structureFuelRate : 1));
                if (burnTime == 0) fuelTemperature = 0;
            }
            return;
        }
        burnTime--;
        if (!isFluidFuelBurning()) {
            if (burnTime == 0) {
                fuelTemperature = 0;
            }
            return;
        }
        int elapsed = Math.max(0, fuelBurnDuration - burnTime);
        int targetConsumed = (int) ((long) fuelFluidConsumption * elapsed / Math.max(1, fuelBurnDuration));
        int amount = Math.max(0, targetConsumed - fuelFluidConsumed);
        if (burnTime == 0) {
            amount = Math.max(amount, fuelFluidConsumption - fuelFluidConsumed);
        }
        if (amount > 0) {
            if (fuelFluid.getAmount() < amount) {
                burnTime = 0;
                fuelTemperature = 0;
                fuelFluidConsumption = 0;
                fuelFluidConsumed = 0;
                fuelBurnDuration = 0;
                return;
            }
            fuelFluid.shrink(amount);
            fuelFluidConsumed += amount;
            if (fuelFluid.isEmpty()) {
                fuelFluid = FluidStack.EMPTY;
            }
            markFluidChanged();
        }
        if (burnTime == 0) {
            fuelTemperature = 0;
            fuelFluidConsumption = 0;
            fuelFluidConsumed = 0;
            fuelBurnDuration = 0;
        }
    }

    /** 判断当前是否处于流体燃料燃烧段。 */
    private boolean isFluidFuelBurning() {
        return fuelFluidConsumption > 0 && fuelBurnDuration > 0;
    }

    /** 处理物品到流体的熔炼；每个已启用输入槽都拥有独立进度。 */
    private void tickMelting(Level level) {
        int activeSlots = inputSlotCount();
        int displaySlot = -1;
        for (int index = 0; index < inputs.length; index++) {
            if (index >= activeSlots) {
                setInputProgress(index, 0);
                inputRecipeTimes[index] = 0;
                inputRequiredTemperatures[index] = 0;
                inputStatuses[index] = INPUT_STATUS_EMPTY;
                continue;
            }
            if (inputs[index].isEmpty()) {
                setInputProgress(index, 0);
                inputRecipeTimes[index] = 0;
                inputRequiredTemperatures[index] = 0;
                inputStatuses[index] = INPUT_STATUS_EMPTY;
                continue;
            }
            Optional<RecipeHolder<MeltingRecipe>> found = level.getRecipeManager().getRecipeFor(
                TFRecipes.MELTING.get(), new SingleRecipeInput(inputs[index]), level
            );
            if (found.isEmpty()) {
                setInputProgress(index, 0);
                inputRecipeTimes[index] = 0;
                inputRequiredTemperatures[index] = 0;
                inputStatuses[index] = INPUT_STATUS_UNMELTABLE;
                continue;
            }
            MeltingRecipe recipe = found.get().value();
            FluidStack meltingResult = recipe.output(isFoundryController());
            inputRecipeTimes[index] = Math.max(0, recipe.time());
            inputRequiredTemperatures[index] = Math.max(0, recipe.temperature());
            if (displaySlot < 0) {
                displaySlot = index;
            }
            // 已经熔化但空间不足的物品不重复点火，腾出空间后直接交付产物。
            boolean heated = inputProgress[index] >= recipe.time();
            if (!heated && !hasHeat(level, recipe.temperature())) {
                // 仍有低温燃料时保持原热量，完全无燃料才冷却，与上游逐槽语义一致。
                if (burnTime <= 0) setInputProgress(index, Math.max(0, inputProgress[index] - 5));
                inputStatuses[index] = INPUT_STATUS_NO_HEAT;
                continue;
            }
            // 结构熔炼允许先完成加热再等待储液空间，不能因满罐停止升温。
            if (heated && fill(meltingResult, FluidAction.SIMULATE) != meltingResult.getAmount()) {
                inputStatuses[index] = INPUT_STATUS_NO_SPACE;
                continue;
            }
            inputStatuses[index] = INPUT_STATUS_PROCESSING;
            if (!heated) {
                int rate = Math.max(1, fuelHeatingRate);
                setInputProgress(index, (int) Math.min(recipe.time(), (long) inputProgress[index] + rate));
            }
            // 上游在下一次加热周期提交已完成的物品，避免本周期提前完成。
            if (heated) {
                inputs[index].shrink(1);
                if (inputs[index].isEmpty()) {
                    inputs[index] = ItemStack.EMPTY;
                }
                fill(meltingResult, FluidAction.EXECUTE);
                // 只有铸造炉提取副产物，注入顺序和不足容量时的损失遵循上游。
                if (isFoundryController()) {
                    for (FluidStack byproduct : recipe.byproductOutputs()) fill(byproduct, FluidAction.EXECUTE);
                }
                setInputProgress(index, 0);
            }
        }
        if (displaySlot < 0) {
            progress = 0;
            processTime = 0;
        } else {
            Optional<RecipeHolder<MeltingRecipe>> display = level.getRecipeManager().getRecipeFor(
                TFRecipes.MELTING.get(), new SingleRecipeInput(inputs[displaySlot]), level
            );
            processTime = display.map(holder -> holder.value().time()).orElse(0);
            progress = Math.min(processTime, inputProgress[displaySlot]);
        }
    }

    /** 返回控制器有序流体快照，供网络、界面和自动化测试使用。 */
    public List<FluidStack> structureFluidLayers() {
        return structureFluids.snapshot();
    }

    /** 玩家选择某层为底层，排液口与容器取液随后优先使用它。 */
    public boolean selectStructureFluid(int index) {
        if (!isStructureController() || !structureFluids.select(index)) return false;
        TinkerFoundry.LOGGER.debug("[fluid-order] controller={} selected={} bottom={}", worldPosition, index, structureFluids.get(0));
        markFluidChanged();
        return true;
    }

    /** 原地合金先在副本上完整扣料和验容，确认成功后再原子替换真实储量。 */
    private boolean runStructureAlloying(Level level, boolean simulate, int temperature) {
        List<FluidStack> available = structureFluids.snapshot();
        if (available.size() < 2) return false;
        for (RecipeHolder<AlloyingRecipe> holder : level.getRecipeManager().getAllRecipesFor(TFRecipes.ALLOYING.get())) {
            AlloyingRecipe recipe = holder.value();
            int[] selected = new int[recipe.ingredients().size()];
            if (selected.length < 2 || !matchStructureAlloy(recipe, available, 0, new boolean[available.size()], selected)) continue;
            StructureFluidTank result = new StructureFluidTank();
            result.restore(available);
            for (int index = 0; index < selected.length; index++) {
                AlloyingRecipe.AlloyIngredient ingredient = recipe.ingredients().get(index);
                if (!ingredient.catalyst()) {
                    result.drain(result.indexOf(available.get(selected[index])), ingredient.ingredient().amount(), FluidAction.EXECUTE);
                }
            }
            if (result.fill(recipe.result(), capacity(), FluidAction.SIMULATE) != recipe.result().getAmount()) continue;
            if (temperature < recipe.temperature()) continue;
            if (simulate) return true;
            result.fill(recipe.result(), capacity(), FluidAction.EXECUTE);
            structureFluids.restore(result.snapshot());
            markFluidChanged();
            TinkerFoundry.LOGGER.debug("[alloy] controller={} recipe={} layers={} amount={}", worldPosition, holder.id(), structureFluids.size(), structureFluids.amount());
            return true;
        }
        return false;
    }

    /** 回溯分配合金原料，标签重叠时仍保证同一流体层不会被重复扣除。 */
    private static boolean matchStructureAlloy(AlloyingRecipe recipe, List<FluidStack> available, int index, boolean[] used, int[] selected) {
        if (index == selected.length) return true;
        AlloyingRecipe.AlloyIngredient ingredient = recipe.ingredients().get(index);
        for (int tank = 0; tank < available.size(); tank++) {
            if (!used[tank] && ingredient.ingredient().test(available.get(tank))
                && available.get(tank).getAmount() >= ingredient.ingredient().amount()) {
                used[tank] = true;
                selected[index] = tank;
                if (matchStructureAlloy(recipe, available, index + 1, used, selected)) return true;
                used[tank] = false;
            }
        }
        return false;
    }

    /** 处理二元、三元或四元流体合金。 */
    private void tickAlloying(Level level) {
        List<FluidStack> availableFluids = java.util.Arrays.stream(alloyInputs).filter(stack -> !stack.isEmpty()).toList();
        if (availableFluids.size() < 2) {
            progress = 0;
            processTime = 0;
            return;
        }
        FluidRecipeInput input = new FluidRecipeInput(availableFluids);
        Optional<RecipeHolder<AlloyingRecipe>> found = level.getRecipeManager().getRecipeFor(TFRecipes.ALLOYING.get(), input, level);
        if (found.isEmpty() || !hasHeat(level, found.get().value().temperature())) {
            progress = 0;
            processTime = 0;
            return;
        }
        AlloyingRecipe recipe = found.get().value();
        int[] selected = selectAlloyInputs(recipe);
        if (selected == null) {
            progress = 0;
            processTime = 0;
            return;
        }
        processTime = 100;
        progress++;
        if (progress >= processTime && fillOutput(recipe.result(), FluidAction.SIMULATE) == recipe.result().getAmount()
            && consumeAlloyInputs(recipe, selected)) {
            fillOutput(recipe.result(), FluidAction.EXECUTE);
            progress = 0;
        }
    }

    /** 使用回溯为合金配方分配实际流体槽，确保标签输入不会发生贪心误配。 */
    private int[] selectAlloyInputs(AlloyingRecipe recipe) {
        if (recipe.ingredients().size() > alloyInputs.length) {
            return null;
        }
        int[] selected = new int[recipe.ingredients().size()];
        for (int index = 0; index < selected.length; index++) {
            selected[index] = -1;
        }
        return selectAlloyInputs(recipe, 0, new boolean[alloyInputs.length], selected) ? selected : null;
    }

    /** 递归尝试每个合金输入的槽位，催化流体也占用独立槽位。 */
    private boolean selectAlloyInputs(AlloyingRecipe recipe, int ingredientIndex, boolean[] used, int[] selected) {
        if (ingredientIndex == recipe.ingredients().size()) {
            return true;
        }
        AlloyingRecipe.AlloyIngredient ingredient = recipe.ingredients().get(ingredientIndex);
        for (int tank = 0; tank < alloyInputs.length; tank++) {
            if (!used[tank] && alloyInputs[tank].getAmount() >= ingredient.ingredient().amount()
                && ingredient.ingredient().test(alloyInputs[tank])) {
                used[tank] = true;
                selected[ingredientIndex] = tank;
                if (selectAlloyInputs(recipe, ingredientIndex + 1, used, selected)) {
                    return true;
                }
                used[tank] = false;
                selected[ingredientIndex] = -1;
            }
        }
        return false;
    }

    /** 按配方所需数量消耗非催化合金输入，保留催化流体和每槽剩余流体。 */
    private boolean consumeAlloyInputs(AlloyingRecipe recipe, int[] selected) {
        if (selected.length != recipe.ingredients().size()) {
            return false;
        }
        for (int ingredientIndex = 0; ingredientIndex < selected.length; ingredientIndex++) {
            AlloyingRecipe.AlloyIngredient ingredient = recipe.ingredients().get(ingredientIndex);
            int tank = selected[ingredientIndex];
            if (tank < 0 || (!ingredient.catalyst() && alloyInputs[tank].getAmount() < ingredient.ingredient().amount())) {
                return false;
            }
        }
        for (int ingredientIndex = 0; ingredientIndex < selected.length; ingredientIndex++) {
            AlloyingRecipe.AlloyIngredient ingredient = recipe.ingredients().get(ingredientIndex);
            if (ingredient.catalyst()) {
                continue;
            }
            int tank = selected[ingredientIndex];
            alloyInputs[tank].shrink(ingredient.ingredient().amount());
            if (alloyInputs[tank].isEmpty()) {
                alloyInputs[tank] = FluidStack.EMPTY;
            }
        }
        markFluidChanged();
        return true;
    }

    /** 处理浇注台和浇注盆的物品输出。 */
    private void tickCasting(Level level) {
        if (fluid.isEmpty()) {
            return;
        }
        FluidRecipeInput recipeInput = new FluidRecipeInput(List.of(fluid), inputs[0]);
        Optional<RecipeHolder<CastingRecipe>> casting = level.getRecipeManager().getRecipeFor(TFRecipes.CASTING.get(), recipeInput, level);
        if (casting.isPresent()) {
            finishCasting(casting.get().value());
            return;
        }
        Optional<RecipeHolder<MoldingRecipe>> molding = level.getRecipeManager().getRecipeFor(TFRecipes.MOLDING.get(), recipeInput, level);
        molding.ifPresent(holder -> finishMolding(holder.value()));
    }

    /** 浇注储液罐自动处理桶、便携罐和专用储液罐，结果进入输出槽等待取出或自动化抽取。 */
    private void tickCastingTank(Level level) {
        if (inputs[0].isEmpty() || !output.isEmpty()) {
            return;
        }
        ItemStack input = inputs[0].copyWithCount(1);
        ItemStack result = processCastingContainer(input);
        if (result.isEmpty()) {
            return;
        }
        inputs[0].shrink(1);
        if (inputs[0].isEmpty()) {
            inputs[0] = ItemStack.EMPTY;
        }
        output = result;
        progress = 0;
        setChanged();
        markFluidChanged();
    }

    /** 处理单个流体容器，所有模拟检查完成后才执行两侧变更。 */
    private ItemStack processCastingContainer(ItemStack input) {
        if (input.getItem() instanceof net.minecraft.world.item.BucketItem bucket) {
            if (bucket.content == net.minecraft.world.level.material.Fluids.EMPTY) {
                FluidStack stored = getFluidInTank(0);
                if (stored.getAmount() < FluidValues.BUCKET) {
                    return ItemStack.EMPTY;
                }
                FluidStack drained = drainTank(0, FluidValues.BUCKET, FluidAction.EXECUTE);
                return drained.getAmount() == FluidValues.BUCKET ? new ItemStack(drained.getFluid().getBucket()) : ItemStack.EMPTY;
            }
            FluidStack resource = new FluidStack(bucket.content, FluidValues.BUCKET);
            if (fillTank(0, resource, FluidAction.SIMULATE) != FluidValues.BUCKET) {
                return ItemStack.EMPTY;
            }
            fillTank(0, resource, FluidAction.EXECUTE);
            return new ItemStack(net.minecraft.world.item.Items.BUCKET);
        }
        if (input.getItem() instanceof org.hp.tinker_foundry.item.PortableTankItem portable) {
            return processPortableContainer(input, portable.capacity(), false);
        }
        if (input.getItem() instanceof org.hp.tinker_foundry.item.FoundryTankItem tank) {
            return processPortableContainer(input, tank.capacity(), tank.allowsFuel());
        }
        return ItemStack.EMPTY;
    }

    /** 在专用储液罐与可携带容器之间进行一次双向流体传输。 */
    private ItemStack processPortableContainer(ItemStack input, int containerCapacity, boolean allowFuel) {
        org.hp.tinker_foundry.item.PortableTankFluidHandler container =
            new org.hp.tinker_foundry.item.PortableTankFluidHandler(input, containerCapacity, allowFuel);
        FluidStack contained = container.getFluidInTank(0);
        if (!contained.isEmpty()) {
            int moved = fillTank(0, contained, FluidAction.SIMULATE);
            if (moved <= 0) {
                return ItemStack.EMPTY;
            }
            fillTank(0, contained.copyWithAmount(moved), FluidAction.EXECUTE);
            container.drain(moved, FluidAction.EXECUTE);
            return input;
        }
        FluidStack stored = getFluidInTank(0);
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int requested = Math.min(stored.getAmount(), containerCapacity);
        FluidStack moved = stored.copyWithAmount(requested);
        if (container.fill(moved, FluidAction.SIMULATE) != requested) {
            return ItemStack.EMPTY;
        }
        FluidStack drained = drainTank(0, requested, FluidAction.EXECUTE);
        if (drained.getAmount() != requested) {
            return ItemStack.EMPTY;
        }
        container.fill(drained, FluidAction.EXECUTE);
        return input;
    }

    /** 冷却并完成可重复使用铸模浇注，铸模本身不会被消耗。 */
    private void finishCasting(CastingRecipe recipe) {
        processTime = recipe.time();
        progress++;
        if (progress >= processTime && canAcceptOutput(recipe.result())) {
            addOutput(recipe.result());
            fluid.shrink(recipe.fluid().amount());
            if (fluid.isEmpty()) fluid = FluidStack.EMPTY;
            markFluidChanged();
            progress = 0;
        }
    }

    /** 冷却并完成必须使用模具的浇注。 */
    private void finishMolding(MoldingRecipe recipe) {
        processTime = recipe.time();
        progress++;
        if (progress >= processTime && canAcceptOutput(recipe.result()) && canAcceptRemainder(recipe.remainder())) {
            addOutput(recipe.result());
            fluid.shrink(recipe.fluid().amount());
            if (fluid.isEmpty()) fluid = FluidStack.EMPTY;
            markFluidChanged();
            inputs[0].shrink(1);
            if (inputs[0].isEmpty()) {
                inputs[0] = ItemStack.EMPTY;
            }
            addRemainder(recipe.remainder());
            progress = 0;
        }
    }

    /** 检查产物槽是否能接收完整结果，允许连续浇注堆叠同类物品。 */
    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty() || output.isEmpty()) {
            return result.getCount() <= result.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    /** 将浇注结果合并到产物槽。 */
    private void addOutput(ItemStack result) {
        if (output.isEmpty()) {
            output = result.copy();
        } else {
            output.grow(result.getCount());
        }
    }

    /** 检查容器返还槽能否接收完整残留物，防止完成配方后丢失容器。 */
    private boolean canAcceptRemainder(ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }
        if (remainder.isEmpty()) {
            return result.getCount() <= result.getMaxStackSize();
        }
        return ItemStack.isSameItemSameComponents(remainder, result)
            && remainder.getCount() + result.getCount() <= remainder.getMaxStackSize();
    }

    /** 将模具配方的容器返还物品合并到独立槽位。 */
    private void addRemainder(ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        if (remainder.isEmpty()) {
            remainder = result.copy();
        } else {
            remainder.grow(result.getCount());
        }
    }

    /** 先使用自定义燃料配方，再回退到 NeoForge 原版燃料值。 */
    private boolean hasHeat(Level level, int requiredTemperature) {
        // 熔炼设备由第零相位统一点火，逐槽检查不得抽取燃料。
        if (isMeltingBlock()) return burnTime > 0 && fuelTemperature >= requiredTemperature;
        if (consumeOwnFuel(level, requiredTemperature)) {
            return true;
        }
        // 多方块只允许使用已登记的结构储罐，不能绕过结构通过外部加热器供热。
        if (isStructureController()) return false;
        if (level != null) {
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(direction)) instanceof FoundryBlockEntity heater && heater.isHeater()
                    && heater.consumeOwnFuel(level, requiredTemperature)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 炉腔掉落物进入真实库存；可熔炼生物仅在受到伤害后产生配方流体。 */
    private void tickEntities() {
        net.minecraft.world.phys.AABB bounds = interiorBounds();
        if (bounds == null || level == null) return;
        // 物品实体吸入不依赖燃料；燃料只在首个符合条件的生物实体出现时检查一次。
        Boolean canMelt = null;
        boolean melted = false;
        var inventory = new org.hp.tinker_foundry.common.FoundryItemHandler(this);
        var blacklist = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "melting_blacklist"));
        for (net.minecraft.world.entity.Entity entity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, bounds)) {
            if (!entity.isAlive()) continue;
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
                ItemStack remaining = item.getItem().copy();
                for (int slot = 0; slot < inventory.getSlots() && !remaining.isEmpty(); slot++) remaining = inventory.insertItem(slot, remaining, false);
                if (remaining.isEmpty()) item.discard();
                else item.setItem(remaining);
                continue;
            }
            // 创造玩家、抗火效果、实体黑名单及对应伤害免疫均禁止产液。
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)
                || entity.getType().is(blacklist) || living.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)
                || living instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().invulnerable) continue;
            if (canMelt == null) canMelt = burnTime > 0 || findHeatingFuel(false) > 0;
            if (!canMelt) continue;
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID,
                    entity.fireImmune() ? "smeltery_magic" : "smeltery_heat"));
            var damageSource = level.damageSources().source(key);
            if (living.isInvulnerableTo(damageSource)) continue;
            FluidStack result = new FluidStack(TFFluids.EXTRA_SOURCES.get("liquid_soul").get(), 10);
            int damage = 2;
            for (var holder : level.getRecipeManager().getAllRecipesFor(TFRecipes.ENTITY_MELTING.get())) {
                if (holder.value().matchesEntity(entity.getType())) {
                    result = holder.value().result().copy();
                    damage = holder.value().damage();
                    break;
                }
            }
            // 只有实际伤害成功才输出流体，免疫或无敌实体不会消耗产液机会。
            if (living.hurt(damageSource, damage)) {
                fill(result, FluidAction.EXECUTE);
                melted = true;
                TinkerFoundry.LOGGER.debug("[entity-melting] controller={} entity={} damage={} result={}",
                    worldPosition, entity.getType(), damage, result);
            }
        }
        // 模拟找到燃料后真正发生伤害才点火，单次实体扫描最多消耗一份燃料。
        if (melted && burnTime <= 0) findHeatingFuel(true);
    }

    /** 模拟或取得燃料，结构使用登记储罐，熔化器只访问正下方设备的标准能力。 */
    private int findHeatingFuel(boolean consume) {
        if (level == null || !structureValid) return 0;
        java.util.List<IFluidHandler> handlers = new java.util.ArrayList<>();
        if (isStructureController()) handlers.addAll(structureFuelSources());
        else {
            IFluidHandler below = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                worldPosition.below(), Direction.UP);
            if (below != null) handlers.add(below);
        }
        // 流体优先于固体，模拟时绝不修改燃料储罐。
        for (IFluidHandler handler : handlers) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stored = handler.getFluidInTank(tank);
                for (RecipeHolder<FuelRecipe> holder : level.getRecipeManager().getAllRecipesFor(TFRecipes.FUEL.get())) {
                    FuelRecipe recipe = holder.value();
                    if (!recipe.matchesFluid(stored) || recipe.temperature() <= 0) continue;
                    FluidStack cost = stored.copyWithAmount(Math.max(1, recipe.consumption()));
                    FluidStack available = isStructureController() ? drainStructureFuel(cost, FluidAction.SIMULATE)
                        : handler.drain(cost, FluidAction.SIMULATE);
                    if (available.getAmount() != cost.getAmount()) continue;
                    if (consume) {
                        FluidStack drained = isStructureController() ? drainStructureFuel(cost, FluidAction.EXECUTE)
                            : handler.drain(cost, FluidAction.EXECUTE);
                        if (drained.getAmount() != cost.getAmount()) {
                            TinkerFoundry.LOGGER.warn("[fuel] handler violated simulation at {} expected={} actual={}",
                                worldPosition, cost.getAmount(), drained.getAmount());
                            return 0;
                        }
                        burnTime = fuelBurnDuration = Math.max(1, recipe.duration());
                        fuelTemperature = recipe.temperature();
                        fuelHeatingRate = recipe.rate();
                        if (handler instanceof FoundryBlockEntity source) structureFuelTankPos = source.worldPosition;
                        setChanged();
                        TinkerFoundry.LOGGER.debug("[fuel] pos={} recipe={} duration={} temperature={} rate={} consumedMb={}",
                            worldPosition, holder.id(), burnTime, fuelTemperature, fuelHeatingRate, cost.getAmount());
                    }
                    return recipe.temperature();
                }
            }
        }
        // 多方块不接受固体燃料；熔化器从下方物品能力抽取并返还燃料容器。
        if (isStructureController()) return 0;
        net.neoforged.neoforge.items.IItemHandler items = level.getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, worldPosition.below(), Direction.UP);
        if (items == null) return 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack candidate = items.extractItem(slot, 1, true);
            int duration = candidate.getBurnTime(null) / 4;
            if (duration <= 0) continue;
            if (consume) {
                ItemStack taken = items.extractItem(slot, 1, false);
                if (taken.isEmpty() || !ItemStack.isSameItemSameComponents(candidate, taken)) return 0;
                burnTime = fuelBurnDuration = duration;
                fuelTemperature = 800;
                fuelHeatingRate = 8;
                ItemStack remains = taken.getCraftingRemainingItem();
                for (int destination = 0; destination < items.getSlots() && !remains.isEmpty(); destination++) {
                    remains = items.insertItem(destination, remains, false);
                }
                if (!remains.isEmpty()) net.minecraft.world.Containers.dropItemStack(level,
                    worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, remains);
                setChanged();
                TinkerFoundry.LOGGER.debug("[fuel] solid pos={} duration={} temperature=800 rate=8", worldPosition, duration);
            }
            return 800;
        }
        return 0;
    }

    /** 独立合金设备的旧有热源处理，不参与熔炼设备的分相点火。 */
    private boolean consumeOwnFuel(Level level, int requiredTemperature) {
        // 控制器按配方一次抽取燃料并保留燃烧时间，避免把整桶燃料藏在产物槽之外。
        if (isStructureController()) {
            if (burnTime <= 0) {
                for (FoundryBlockEntity source : structureFuelSources()) {
                    FluidStack available = source.getFluidInTank(0);
                    Optional<RecipeHolder<FuelRecipe>> found = level.getRecipeManager().getAllRecipesFor(TFRecipes.FUEL.get()).stream()
                        .filter(holder -> holder.value().matchesFluid(available))
                        .filter(holder -> holder.value().temperature() >= requiredTemperature)
                        .filter(holder -> drainStructureFuel(available.copyWithAmount(Math.max(1, holder.value().consumption())), FluidAction.SIMULATE).getAmount() >= Math.max(1, holder.value().consumption()))
                        .findFirst();
                    if (found.isEmpty()) continue;
                    FuelRecipe recipe = found.get().value();
                    structureFuelTankPos = source.getBlockPos();
                    drainStructureFuel(available.copyWithAmount(Math.max(1, recipe.consumption())), FluidAction.EXECUTE);
                    burnTime = Math.max(1, recipe.duration());
                    fuelBurnDuration = burnTime;
                    fuelTemperature = recipe.temperature();
                    fuelHeatingRate = recipe.rate();
                    fuelFluidConsumption = 0;
                    TinkerFoundry.LOGGER.debug("[fuel] controller={} source={} recipe={} temperature={} consumedMb={} quality={} costPerFourTicks={} heatingRate={}",
                        worldPosition, structureFuelTankPos, found.get().id(), fuelTemperature, recipe.consumption(), fuelBurnDuration, structureFuelRate, fuelHeatingRate);
                    break;
                }
            }
            return burnTime > 0 && fuelTemperature >= requiredTemperature;
        }
        if (burnTime <= 0 && isHeater()) {
            // 加热器燃料槽不足时从相邻专用燃料罐补充，专用罐只作为存储，不直接参与熔炼。
            pullFuelFromAdjacent(level);
        }
        if (burnTime <= 0 && (isHeater() || isStructureController()) && !fuelFluid.isEmpty()) {
            // 流体燃料按配方声明的总消耗量启动一段燃烧，实际扣除在每个服务端 tick 平滑完成。
            Optional<RecipeHolder<FuelRecipe>> found = level.getRecipeManager().getAllRecipesFor(TFRecipes.FUEL.get()).stream()
                .filter(holder -> holder.value().matchesFluid(fuelFluid))
                .filter(holder -> fuelFluid.getAmount() >= Math.max(1, holder.value().consumption()))
                .findFirst();
            if (found.isPresent()) {
                FuelRecipe recipe = found.get().value();
                burnTime = Math.max(1, recipe.duration());
                fuelTemperature = Math.max(0, recipe.temperature());
                fuelFluidConsumption = Math.max(1, recipe.consumption());
                fuelFluidConsumed = 0;
                fuelBurnDuration = burnTime;
                markFluidChanged();
            }
        }
        if (burnTime <= 0 && !fuel.isEmpty()) {
            Optional<RecipeHolder<FuelRecipe>> found = level.getRecipeManager().getRecipeFor(TFRecipes.FUEL.get(), new SingleRecipeInput(fuel), level);
            if (found.isPresent()) {
                FuelRecipe recipe = found.get().value();
                burnTime = recipe.duration();
                fuelTemperature = recipe.temperature();
                fuelFluidConsumption = 0;
                fuelFluidConsumed = 0;
                fuelBurnDuration = 0;
                fuel.shrink(1);
            } else {
                burnTime = fuel.getBurnTime(null);
                fuelTemperature = burnTime > 0 ? 2000 : 0;
                fuelFluidConsumption = 0;
                fuelFluidConsumed = 0;
                fuelBurnDuration = 0;
                if (burnTime > 0) fuel.shrink(1);
            }
        }
        return burnTime > 0 && fuelTemperature >= requiredTemperature;
    }

    /** 从相邻专用燃料罐补充最多一桶流体燃料，执行前先模拟两侧容量。 */
    private void pullFuelFromAdjacent(Level level) {
        int availableCapacity = getTankCapacity(0) - fuelFluid.getAmount();
        if (availableCapacity <= 0) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (!(level.getBlockEntity(worldPosition.relative(direction)) instanceof FoundryBlockEntity source)
                || !source.isFuelTankBlock() || source == this) {
                continue;
            }
            FluidStack available = source.transferFluid();
            if (available.isEmpty()) {
                continue;
            }
            int requested = Math.min(Math.min(available.getAmount(), availableCapacity), FluidValues.BUCKET);
            int accepted = fillTank(0, available.copyWithAmount(requested), FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            FluidStack drained = source.drain(accepted, FluidAction.EXECUTE);
            if (drained.getAmount() == accepted) {
                fillTank(0, drained, FluidAction.EXECUTE);
                return;
            }
        }
    }

    /** 统一记录各槽进度变化，不复制与当前处理无关的大数组。 */
    private void setInputProgress(int index, int progress) {
        if (inputProgress[index] != progress) {
            inputProgress[index] = progress;
            inputProgressVersion++;
        }
    }

    /** 读取设备的物品输入槽。 */
    public ItemStack getInput() {
        return inputs[0].copy();
    }

    /** 读取指定物品输入槽。 */
    public ItemStack getInput(int slot) {
        return slot >= 0 && slot < inputs.length ? inputs[slot].copy() : ItemStack.EMPTY;
    }

    /** 返回当前结构实际启用的输入槽数量。 */
    public int inputSlotCount() {
        if (isSmelteryController() || isFoundryController()) {
            return Math.min(MAX_STRUCTURE_INPUTS, Math.max(0, structureInteriorBlocks));
        }
        // 合金炉只接收流体，加热器只接收燃料，二者都没有固体输入槽。
        if (isAlloyer() || isHeater()) {
            return 0;
        }
        // 熔炼器、浇注台、浇注盆和浇注储液罐保留一个物品输入槽供直接交互或自动化使用。
        if (isMeltingBlock()) return 3;
        if (isCastingBlock() || isCastingTankBlock()) {
            return 1;
        }
        return 0;
    }

    /** 把动态输入编号映射到容器编号，避开三个固定功能槽。 */
    public static int inputContainerSlot(int input) {
        return input < BASE_INPUT_SLOTS ? input : input + 3;
    }

    /** 从容器编号反查输入编号，燃料及输出槽返回负值。 */
    private int inputIndex(int slot) {
        int index = slot < BASE_INPUT_SLOTS ? slot : slot >= CONTAINER_SIZE ? slot - 3 : -1;
        return index >= 0 && index < inputs.length ? index : -1;
    }

    /** 计算随真实槽数增长的菜单数据长度，额外槽按进度、总时长、温度、状态排列。 */
    public static int menuDataCount(int slots) {
        return MENU_DATA_COUNT;
    }

    /** 超出基础槽数的数据不使用原版短整数编号通道。 */
    public int inputHeatValue(int slot, int field) {
        if (slot < 0 || slot >= inputs.length) return 0;
        return switch (field) {
            case 0 -> inputProgress[slot];
            case 1 -> inputRecipeTimes[slot];
            case 2 -> inputRequiredTemperatures[slot];
            default -> inputStatuses[slot];
        };
    }

    /** 仅传输非空输入的槽号及四项热量状态，避免大型空炉发送巨量零值。 */
    public int[] heatSnapshot() {
        java.util.stream.IntStream.Builder values = java.util.stream.IntStream.builder();
        for (int slot = 0; slot < inputSlotCount(); slot++) {
            if (inputs[slot].isEmpty()) continue;
            values.add(slot);
            for (int field = 0; field < 4; field++) values.add(inputHeatValue(slot, field));
        }
        return values.build().toArray();
    }

    /** 扩容保留输入和热量；缩容先返还桶槽，再把超出部分掉落在控制器旁。 */
    public void resizeStructureInputs(int requested, boolean dropOverflow) {
        int size = Math.max(0, Math.min(MAX_STRUCTURE_INPUTS, requested));
        if (structureInventorySize == size && inputs.length == Math.max(BASE_INPUT_SLOTS, size)) return;
        int oldSize = structureInventorySize;
        // 菜单编号变化前关闭旧菜单，避免多人仍用旧槽号操作新增槽或玩家背包。
        if (dropOverflow && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.containerMenu instanceof org.hp.tinker_foundry.menu.FoundryMenu menu && menu.blockEntity() == this) {
                    player.closeContainer();
                }
            }
            for (int index = size; index < inputs.length; index++) {
                if (!inputs[index].isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, inputs[index]);
                    inputs[index] = ItemStack.EMPTY;
                }
            }
        }
        // 数组只按当前规模分配，普通设备仍保留基础编号，不能预分配整个最大炉体。
        int length = Math.max(BASE_INPUT_SLOTS, size);
        int oldLength = inputs.length;
        inputs = java.util.Arrays.copyOf(inputs, length);
        for (int index = oldLength; index < length; index++) inputs[index] = ItemStack.EMPTY;
        inputProgress = java.util.Arrays.copyOf(inputProgress, length);
        inputRecipeTimes = java.util.Arrays.copyOf(inputRecipeTimes, length);
        inputRequiredTemperatures = java.util.Arrays.copyOf(inputRequiredTemperatures, length);
        inputStatuses = java.util.Arrays.copyOf(inputStatuses, length);
        // 基础编号预留区可能大于真实槽数，缩容后不能保留已移除槽位的残余热量。
        java.util.Arrays.fill(inputProgress, size, length, 0);
        java.util.Arrays.fill(inputRecipeTimes, size, length, 0);
        java.util.Arrays.fill(inputRequiredTemperatures, size, length, 0);
        java.util.Arrays.fill(inputStatuses, size, length, INPUT_STATUS_EMPTY);
        inputProgressVersion++;
        structureInventorySize = size;
        if (dropOverflow) {
            setChanged();
            TinkerFoundry.LOGGER.debug("[inventory-resize] pos={} oldSlots={} newSlots={} dataCount={} overflowDropped=true",
                worldPosition, oldSize, size, menuDataCount(size));
        }
    }

    /** 读取设备的物品输出槽。 */
    public ItemStack getOutput() {
        return output.copy();
    }

    /** 服务端插入一个物品或燃料。 */
    public boolean insertItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 合金炉和加热器没有固体输入槽，只允许把可燃物放入燃料槽。
        if (isAlloyer() || isHeater()) {
            return insertFuel(stack);
        }
        // 浇注台、浇注盆只接受模具或容器输入，不把流体容器误当作普通模具。
        if (isCastingBlock()) {
            if (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                || stack.getItem() instanceof org.hp.tinker_foundry.item.PortableTankItem
                || stack.getItem() instanceof org.hp.tinker_foundry.item.FoundryTankItem) {
                return false;
            }
            return insertCastingItem(stack);
        }
        // 浇注储液罐只接受自身的容器处理输入，其他设备不应吞掉玩家物品。
        if (isCastingTankBlock()) {
            if (!(stack.getItem() instanceof net.minecraft.world.item.BucketItem
                || stack.getItem() instanceof org.hp.tinker_foundry.item.PortableTankItem
                || stack.getItem() instanceof org.hp.tinker_foundry.item.FoundryTankItem)) {
                return false;
            }
            return insertInput(stack);
        }
        // 只有熔炼器和多方块控制器拥有固体熔炼输入。
        if (!isMeltingBlock()) {
            return false;
        }
        return insertFuelOrInput(stack);
    }

    /** 插入固体燃料，供加热器、合金炉和其他带燃料槽的设备使用。 */
    private boolean insertFuel(ItemStack stack) {
        if (stack.getBurnTime(null) <= 0) {
            return false;
        }
        if (fuel.isEmpty()) {
            fuel = stack.split(1);
            setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(fuel, stack) && fuel.getCount() < fuel.getMaxStackSize()) {
            fuel.grow(1);
            stack.shrink(1);
            setChanged();
            return true;
        }
        return false;
    }

    /** 插入浇注台或浇注盆的模具输入。 */
    private boolean insertCastingItem(ItemStack stack) {
        if (inputs[0].isEmpty()) {
            inputs[0] = stack.split(1);
            setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(inputs[0], stack) && inputs[0].getCount() < inputs[0].getMaxStackSize()) {
            inputs[0].grow(1);
            stack.shrink(1);
            setChanged();
            return true;
        }
        return false;
    }

    /** 插入浇注储液罐的桶或便携容器。 */
    private boolean insertInput(ItemStack stack) {
        if (inputs[0].isEmpty()) {
            inputs[0] = stack.split(1);
            setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(inputs[0], stack) && inputs[0].getCount() < inputs[0].getMaxStackSize()) {
            inputs[0].grow(1);
            stack.shrink(1);
            setChanged();
            return true;
        }
        return false;
    }

    /** 插入熔炼器或多方块控制器的燃料和物品输入。 */
    private boolean insertFuelOrInput(ItemStack stack) {
        if (!isStructureController() && stack.getBurnTime(null) > 0) {
            if (fuel.isEmpty()) {
                fuel = stack.split(1);
                setChanged();
                return true;
            }
            if (ItemStack.isSameItemSameComponents(fuel, stack) && fuel.getCount() < fuel.getMaxStackSize()) {
                fuel.grow(1);
                stack.shrink(1);
                setChanged();
                return true;
            }
        }
        for (int index = 0; index < inputSlotCount(); index++) {
            if (inputs[index].isEmpty()) {
                inputs[index] = stack.split(1);
                setChanged();
                return true;
            }
            if (!isStructureController() && ItemStack.isSameItemSameComponents(inputs[index], stack) && inputs[index].getCount() < inputs[index].getMaxStackSize()) {
                inputs[index].grow(1);
                stack.shrink(1);
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** 取出一个已完成的物品。 */
    public ItemStack takeOutput() {
        ItemStack result = output;
        output = ItemStack.EMPTY;
        if (!result.isEmpty()) setChanged();
        return result;
    }

    /** 返回当前结构是否有效，供菜单、调试和服务端状态同步使用。 */
    public boolean isStructureValid() {
        return structureValid;
    }

    /** 返回当前结构容量，结构无效时为零。 */
    public int structureCapacity() {
        return structureValid ? structureCapacity : 0;
    }

    /** 返回当前处理进度。 */
    public int progress() {
        return progress;
    }

    /** 返回当前处理总时长。 */
    public int processTime() {
        return processTime;
    }

    /** 提供菜单使用的服务端权威进度、燃料和容量同步数据。 */
    public ContainerData menuData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                // 超过基础编号段的每个输入同样同步四项状态，不受显示页影响。
                if (index >= MENU_DATA_COUNT) {
                    int input = BASE_INPUT_SLOTS + (index - MENU_DATA_COUNT) / 4;
                    if (input >= inputs.length) return 0;
                    return switch ((index - MENU_DATA_COUNT) % 4) {
                        case 0 -> inputProgress[input];
                        case 1 -> inputRecipeTimes[input];
                        case 2 -> inputRequiredTemperatures[input];
                        default -> inputStatuses[input];
                    };
                }
                if (index >= MENU_INPUT_PROGRESS_START && index < MENU_INPUT_TIME_START) {
                    return inputProgress[index - MENU_INPUT_PROGRESS_START];
                }
                if (index >= MENU_INPUT_TIME_START && index < MENU_INPUT_TEMPERATURE_START) {
                    return inputRecipeTimes[index - MENU_INPUT_TIME_START];
                }
                if (index >= MENU_INPUT_TEMPERATURE_START && index < MENU_INPUT_STATUS_START) {
                    return inputRequiredTemperatures[index - MENU_INPUT_TEMPERATURE_START];
                }
                if (index >= MENU_INPUT_STATUS_START && index < MENU_INPUT_DATA_COUNT) {
                    return inputStatuses[index - MENU_INPUT_STATUS_START];
                }
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> processTime;
                    case 2 -> burnTime;
                    case 3 -> isStructureController() ? structureCapacity() : getTankCapacity(0);
                    case 4 -> structureValid ? 1 : 0;
                    case 5 -> isStructureController() ? structureFluids.amount() : displayFluid().getAmount();
                    case 6 -> isAlloyer() ? alloyInputs[0].getAmount() : 0;
                    case 7 -> isAlloyer() ? alloyInputs[1].getAmount() : 0;
                    case 8 -> fluidRegistryId(displayFluid());
                    case 9 -> isAlloyer() ? fluidRegistryId(alloyInputs[0]) : -1;
                    case 10 -> isAlloyer() ? fluidRegistryId(alloyInputs[1]) : -1;
                    case 13 -> isAlloyer() ? alloyInputs[2].getAmount() : 0;
                    case 14 -> isAlloyer() ? fluidRegistryId(alloyInputs[2]) : -1;
                    case 15 -> isAlloyer() ? alloyInputs[3].getAmount() : 0;
                    case 16 -> isAlloyer() ? fluidRegistryId(alloyInputs[3]) : -1;
                    case 17 -> structureErrorReason.ordinal();
                    case MENU_FUEL_TEMPERATURE_INDEX -> fuelDisplayTemperature();
                    case 11 -> screenKind();
                    case 12 -> inputSlotCount();
                    case MENU_FUEL_AMOUNT_INDEX -> fuelDisplayFluid().getAmount();
                    case MENU_FUEL_CAPACITY_INDEX -> fuelDisplayCapacity();
                    case MENU_FUEL_FLUID_INDEX -> fluidRegistryId(fuelDisplayFluid());
                    case MENU_FUEL_SOURCE_INDEX -> hasFuelSource() ? 1 : 0;
                    case MENU_FUEL_DURATION_INDEX -> fuelBurnDuration;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // 服务端数据由方块实体自身维护，客户端只接收菜单同步值。
            }

            @Override
            public int getCount() {
                return menuDataCount(inputSlotCount());
            }
        };
    }

    /** 容器包含动态输入和三个固定编号功能槽。 */
    @Override
    public int getContainerSize() {
        return inputs.length + 3;
    }

    /** 原版多方块熔炼每槽最多一个物品，漏斗也遵守这一限制。 */
    @Override
    public int getMaxStackSize() {
        return isMeltingBlock() ? 1 : 64;
    }

    /** 判断设备物品槽是否全部为空。 */
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) return false;
        }
        return fuel.isEmpty() && output.isEmpty() && remainder.isEmpty();
    }

    /** 容器协议要求返回实际堆栈，使快速移动和漏斗扣量能写回设备。 */
    @Override
    public ItemStack getItem(int slot) {
        int input = inputIndex(slot);
        if (input >= 0) return inputs[input];
        return switch (slot) {
            case FUEL_SLOT -> fuel;
            case OUTPUT_SLOT -> output;
            case REMAINDER_SLOT -> remainder;
            default -> ItemStack.EMPTY;
        };
    }

    /** 从菜单物品槽移出指定数量。 */
    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = current.split(amount);
        setItem(slot, current);
        return result;
    }

    /** 无动画地移出整个菜单物品槽。 */
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        int input = inputIndex(slot);
        ItemStack result = input >= 0 ? inputs[input] : switch (slot) {
            case FUEL_SLOT -> fuel;
            case OUTPUT_SLOT -> output;
            case REMAINDER_SLOT -> remainder;
            default -> ItemStack.EMPTY;
        };
        if (!result.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        }
        return result;
    }

    /** 写入菜单物品槽并限制到原版最大堆叠数量。 */
    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(Math.min(copy.getCount(), getMaxStackSize(copy)));
        int input = inputIndex(slot);
        if (input >= 0) {
            // 更换或取出物品必须清除旧配方热量，不能把上一件物品的进度移给新物品。
            if (!ItemStack.isSameItemSameComponents(inputs[input], copy) || inputs[input].getCount() != copy.getCount()) {
                setInputProgress(input, 0);
                inputStatuses[input] = INPUT_STATUS_EMPTY;
                inputRecipeTimes[input] = 0;
                inputRequiredTemperatures[input] = 0;
            }
            inputs[input] = copy;
            worldSyncDirty = true;
        } else if (slot == FUEL_SLOT) {
            fuel = copy;
        } else if (slot == OUTPUT_SLOT) {
            output = copy;
        } else if (slot == REMAINDER_SLOT) {
            remainder = copy;
        } else {
            return;
        }
        setChanged();
    }

    /** 只允许输入非燃料物品、燃料槽放入可燃物，输出槽禁止手动放入。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int input = inputIndex(slot);
        if (input >= 0) {
            return input < inputSlotCount() && (isStructureController() || stack.getBurnTime(null) <= 0);
        }
        return isHeater() && slot == FUEL_SLOT && stack.getBurnTime(null) > 0;
    }

    /** 菜单打开期间只允许玩家在设备有效距离内操作。 */
    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    /** 清空三个物品槽，供容器生命周期和异常关闭保护使用。 */
    @Override
    public void clearContent() {
        for (int index = 0; index < inputs.length; index++) {
            inputs[index] = ItemStack.EMPTY;
        }
        fuel = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
        remainder = ItemStack.EMPTY;
        setChanged();
    }

    /** 写入设备物品、流体和进度。 */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag inputTags = new ListTag();
        for (int index = 0; index < inputs.length; index++) {
            if (!inputs[index].isEmpty()) {
                // 1.21.1 编码器返回包含组件的新标签，不会原地填充传入的空标签。
                CompoundTag inputTag = (CompoundTag) inputs[index].save(registries);
                inputTag.putInt("Slot", index);
                inputTags.add(inputTag);
            }
        }
        tag.put("Inputs", inputTags);
        tag.putInt("InputSize", structureInventorySize);
        if (!output.isEmpty()) tag.put("Output", output.save(registries));
        if (!remainder.isEmpty()) tag.put("Remainder", remainder.save(registries));
        if (!fuel.isEmpty()) tag.put("Fuel", fuel.save(registries));
        if (!fluid.isEmpty()) tag.put("Fluid", fluid.save(registries));
        // 多流体单独保存有序列表，不读取或迁移旧控制器的单槽流体。
        if (isStructureController()) {
            ListTag layers = new ListTag();
            for (FluidStack layer : structureFluids.snapshot()) layers.add(layer.save(registries));
            tag.put("StructureFluids", layers);
        }
        if (!fuelFluid.isEmpty()) tag.put("FuelFluid", fuelFluid.save(registries));
        tag.putInt("Progress", progress);
        tag.putInt("ProcessTime", processTime);
        tag.putIntArray("InputProgress", inputProgress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("FuelTemperature", fuelTemperature);
        tag.putInt("FuelHeatingRate", fuelHeatingRate);
        tag.putInt("FuelFluidConsumption", fuelFluidConsumption);
        tag.putInt("FuelFluidConsumed", fuelFluidConsumed);
        tag.putInt("FuelBurnDuration", fuelBurnDuration);
        tag.putInt("StructureFuelRate", structureFuelRate);
        tag.putInt("StructureCapacity", structureCapacity);
        tag.putBoolean("StructureValid", structureValid);
        tag.putInt("StructureInteriorBlocks", structureInteriorBlocks);
        // 客户端世界渲染和重载后的附件归属使用相同结构边界。
        if (structureMin != null) tag.putLong("StructureMin", structureMin.asLong());
        if (structureMax != null) tag.putLong("StructureMax", structureMax.asLong());
        if (drainControllerPos != null) tag.putLong("Controller", drainControllerPos.asLong());
        tag.putInt("TransferCooldown", transferCooldown);
        ListTag alloyInputTags = new ListTag();
        for (int index = 0; index < alloyInputs.length; index++) {
            if (!alloyInputs[index].isEmpty()) {
                CompoundTag alloyInputTag = new CompoundTag();
                alloyInputTag.putInt("Slot", index);
                alloyInputTag.put("Fluid", alloyInputs[index].save(registries));
                alloyInputTags.add(alloyInputTag);
            }
        }
        tag.put("AlloyInputs", alloyInputTags);
    }

    /** 读取设备物品、流体和进度。 */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 当前格式明确保存槽数；不从旧条目猜测炉体规模或迁移损坏输入。
        if (isStructureController()) resizeStructureInputs(tag.getInt("InputSize"), false);
        // 对比更新前后的输入总数，只在实际变化时输出客户端同步诊断。
        int previousInputCount = java.util.Arrays.stream(inputs).mapToInt(ItemStack::getCount).sum();
        // 只解析带槽位编号的物品列表，避免无效条目污染其他输入槽。
        for (int index = 0; index < inputs.length; index++) {
            inputs[index] = ItemStack.EMPTY;
        }
        if (tag.contains("Inputs", Tag.TAG_LIST)) {
            ListTag inputTags = tag.getList("Inputs", Tag.TAG_COMPOUND);
            for (int entry = 0; entry < inputTags.size(); entry++) {
                CompoundTag inputTag = inputTags.getCompound(entry);
                int slot = inputTag.getInt("Slot");
                if (slot >= 0 && slot < inputs.length) {
                    if (inputTag.contains("id", Tag.TAG_STRING)) {
                        inputs[slot] = ItemStack.parse(registries, inputTag).orElse(ItemStack.EMPTY);
                    } else if (!invalidInputReported) {
                        // 缺失标识的历史数据无法恢复物品，不猜测或迁移，也不重复刷屏。
                        TinkerFoundry.LOGGER.warn("[inventory-sync] missing item id at pos={} slot={} keys={}", worldPosition, slot, inputTag.getAllKeys());
                        invalidInputReported = true;
                    }
                }
            }
        }
        output = tag.contains("Output", 10) ? ItemStack.parse(registries, tag.getCompound("Output")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        remainder = tag.contains("Remainder", 10) ? ItemStack.parse(registries, tag.getCompound("Remainder")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        fuel = tag.contains("Fuel", 10) ? ItemStack.parse(registries, tag.getCompound("Fuel")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        fluid = FluidStack.parseOptional(registries, tag.getCompound("Fluid"));
        if (isStructureController()) {
            List<FluidStack> layers = new java.util.ArrayList<>();
            ListTag stored = tag.getList("StructureFluids", Tag.TAG_COMPOUND);
            for (int index = 0; index < stored.size(); index++) {
                layers.add(FluidStack.parseOptional(registries, stored.getCompound(index)));
            }
            structureFluids.restore(layers);
            fluid = FluidStack.EMPTY;
        }
        fuelFluid = FluidStack.parseOptional(registries, tag.getCompound("FuelFluid"));
        alloyInputs = emptyFluidInputs();
        if (tag.contains("AlloyInputs", Tag.TAG_LIST)) {
            ListTag alloyInputTags = tag.getList("AlloyInputs", Tag.TAG_COMPOUND);
            for (int index = 0; index < alloyInputTags.size(); index++) {
                CompoundTag alloyInputTag = alloyInputTags.getCompound(index);
                int slot = alloyInputTag.getInt("Slot");
                if (slot >= 0 && slot < alloyInputs.length) {
                    alloyInputs[slot] = FluidStack.parseOptional(registries, alloyInputTag.getCompound("Fluid"));
                }
            }
        }
        progress = tag.getInt("Progress");
        processTime = tag.getInt("ProcessTime");
        int[] storedProgress = tag.getIntArray("InputProgress");
        for (int index = 0; index < inputProgress.length; index++) {
            inputProgress[index] = index < storedProgress.length ? Math.max(0, storedProgress[index]) : 0;
        }
        burnTime = tag.getInt("BurnTime");
        fuelTemperature = tag.getInt("FuelTemperature");
        fuelHeatingRate = tag.getInt("FuelHeatingRate");
        fuelFluidConsumption = tag.getInt("FuelFluidConsumption");
        fuelFluidConsumed = tag.getInt("FuelFluidConsumed");
        fuelBurnDuration = tag.getInt("FuelBurnDuration");
        structureFuelRate = Math.max(1, tag.getInt("StructureFuelRate"));
        structureCapacity = tag.getInt("StructureCapacity");
        structureValid = tag.getBoolean("StructureValid");
        structureInteriorBlocks = tag.getInt("StructureInteriorBlocks");
        structureMin = tag.contains("StructureMin") ? BlockPos.of(tag.getLong("StructureMin")) : null;
        structureMax = tag.contains("StructureMax") ? BlockPos.of(tag.getLong("StructureMax")) : null;
        drainControllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
        structureDirty = true;
        transferCooldown = tag.getInt("TransferCooldown");
        // 世界更新包和菜单更新都应保留同一份物品，日志用于复测是否还会被清空。
        int loadedInputCount = java.util.Arrays.stream(inputs).mapToInt(ItemStack::getCount).sum();
        if (level != null && level.isClientSide && previousInputCount != loadedInputCount) {
            TinkerFoundry.LOGGER.debug("[inventory-sync] pos={} previousCount={} loadedCount={} entries={}",
                worldPosition, previousInputCount, loadedInputCount, tag.getList("Inputs", Tag.TAG_COMPOUND).size());
        }
    }

    /** 为客户端方块实体渲染器提供初始流体状态。 */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /** 为流体变化提供标准的方块实体同步包。 */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 发送流体槽变化并保留区块保存标记。 */
    private void markFluidChanged() {
        updateLanternLight();
        setChanged();
        if (level != null && !level.isClientSide) {
            worldSyncDirty = true;
            // GUI 有序流体列表与世界渲染更新同时发送，空闲时的注入和选择也必须同步。
            FoundryNetworking.sync(this);
        }
    }

    /** 根据灯笼内当前流体更新方块光照，普通设备不触发方块状态变化。 */
    private void updateLanternLight() {
        if (!isLanternBlock() || level == null || level.isClientSide) {
            return;
        }
        int light = FoundryLanternBlock.lightLevel(fluid);
        BlockState state = getBlockState();
        if (state.getValue(FoundryLanternBlock.LIGHT) != light) {
            level.setBlock(worldPosition, state.setValue(FoundryLanternBlock.LIGHT, light), Block.UPDATE_CLIENTS);
        }
    }

    /** 返回当前宿主是否是冶炼炉控制器。 */
    public boolean isSmelteryController() {
        return getBlockState().is(TFBlocks.SMELTERY_CONTROLLER.get());
    }

    /** 返回当前宿主是否是铸造炉控制器。 */
    public boolean isFoundryController() {
        return getBlockState().is(TFBlocks.FOUNDRY_CONTROLLER.get());
    }

    /** 判断当前方块实体是否是需要结构校验的控制器。 */
    public boolean isStructureController() {
        return isSmelteryController() || isFoundryController();
    }

    /** 返回当前宿主是否是合金炉。 */
    public boolean isAlloyer() {
        return getBlockState().is(TFBlocks.ALLOYER.get());
    }

    /** 在客户端应用专用状态载荷，不参与服务端配方或权限逻辑。 */
    public void applyStatePayload(FoundryStatePayload payload) {
        if (level == null || !level.isClientSide || !worldPosition.equals(payload.pos())) {
            return;
        }
        structureFluids.restore(payload.structureFluids());
        // 热量快照不覆盖物品，物品仍由原版容器协议同步。
        java.util.Arrays.fill(inputProgress, 0);
        java.util.Arrays.fill(inputRecipeTimes, 0);
        java.util.Arrays.fill(inputRequiredTemperatures, 0);
        java.util.Arrays.fill(inputStatuses, INPUT_STATUS_EMPTY);
        int[] heat = payload.heat();
        for (int offset = 0; offset < heat.length; offset += 5) {
            int slot = heat[offset];
            if (slot < 0 || slot >= inputs.length) continue;
            inputProgress[slot] = heat[offset + 1];
            inputRecipeTimes[slot] = heat[offset + 2];
            inputRequiredTemperatures[slot] = heat[offset + 3];
            inputStatuses[slot] = heat[offset + 4];
        }
        if (isHeater()) {
            fuelFluid = payload.fuelFluid().copy();
        } else {
            fluid = payload.fluid().copy();
        }
        alloyInputs = emptyFluidInputs();
        for (int index = 0; index < Math.min(alloyInputs.length, payload.alloyInputs().size()); index++) {
            alloyInputs[index] = payload.alloyInputs().get(index).copy();
        }
        progress = Math.max(0, payload.progress());
        processTime = Math.max(0, payload.processTime());
        structureCapacity = Math.max(0, payload.structureCapacity());
        structureValid = payload.structureValid();
    }

    /** 在客户端应用控制器结构错误载荷。 */
    public void applyStructureErrorPayload(FoundryStructureErrorPayload payload) {
        if (level == null || !level.isClientSide || !worldPosition.equals(payload.controllerPos())) {
            return;
        }
        structureErrorPos = payload.errorPos();
        structureErrorReason = payload.reason();
        structureErrorVisibleFor = structureErrorPos == null ? 0 : 200;
    }

    /** 返回客户端当前需要高亮的错误方块位置。 */
    public BlockPos structureErrorPos() {
        return structureErrorPos;
    }

    /** 返回最近一次结构检查的错误原因。 */
    public StructureErrorReason structureErrorReason() {
        return structureErrorReason;
    }

    /** 判断结构错误提示当前是否仍应显示。 */
    public boolean shouldShowStructureError() {
        return isStructureController() && structureErrorPos != null && structureErrorVisibleFor > 0;
    }

    /** 返回结构错误的本地化文本。 */
    public Component structureErrorMessage() {
        return structureErrorReason.component();
    }

    /** 返回当前宿主是否是加热器。 */
    public boolean isHeater() {
        return getBlockState().is(TFBlocks.HEATER.get());
    }

    /** 判断当前设备是否为独立燃料罐。 */
    public boolean isFuelTankBlock() {
        return getBlockState().is(TFBlocks.SEARED_FUEL_TANK.get()) || getBlockState().is(TFBlocks.SCORCHED_FUEL_TANK.get());
    }

    /** 判断当前设备是否为大容量金属储液罐。 */
    public boolean isIngotTankBlock() {
        return getBlockState().is(TFBlocks.SEARED_TANK.get()) || getBlockState().is(TFBlocks.SCORCHED_TANK.get());
    }

    /** 判断当前设备是否为浇注专用储液罐。 */
    public boolean isCastingTankBlock() {
        return getBlockState().is(TFBlocks.SEARED_CASTING_TANK.get()) || getBlockState().is(TFBlocks.SCORCHED_CASTING_TANK.get());
    }

    /** 判断当前设备是否为可保存 50 mB 流体的冶炼灯。 */
    public boolean isLanternBlock() {
        return getBlockState().is(TFBlocks.SEARED_LANTERN.get()) || getBlockState().is(TFBlocks.SCORCHED_LANTERN.get());
    }

    /** 判断当前设备是否为会在破坏时保存流体的专用储液罐。 */
    public boolean isTankBlock() {
        return isIngotTankBlock() || isFuelTankBlock() || isCastingTankBlock() || isLanternBlock();
    }

    /** 返回专用储液罐当前的可携带流体。 */
    public FluidStack getTankFluid() {
        return isTankBlock() ? fluid.copy() : FluidStack.EMPTY;
    }

    /** 从专用储液罐物品恢复流体，并按当前方块容量和类型执行安全校验。 */
    public void setTankFluid(FluidStack resource) {
        if (!isTankBlock()) {
            org.hp.tinker_foundry.TinkerFoundry.LOGGER.debug("[tank] rejected restore at {} because block is not a dedicated tank", worldPosition);
            return;
        }
        fluid = FluidStack.EMPTY;
        boolean accepted = !resource.isEmpty() && acceptsTankFluid(resource);
        org.hp.tinker_foundry.TinkerFoundry.LOGGER.debug("[tank] restoring {} mB into {} at {}, accepted={}",
            resource.getAmount(), resource.getFluid(), getBlockState().getBlock(), worldPosition, accepted);
        if (accepted) {
            int amount = Math.min(resource.getAmount(), capacity());
            if (amount > 0) {
                fluid = resource.copyWithAmount(amount);
            }
        }
        markFluidChanged();
    }

    /** 判断储液罐是否接受熔融金属或当前燃料配方支持的流体。 */
    private boolean acceptsTankFluid(FluidStack resource) {
        // 燃料罐允许保存不同燃料；能否燃烧由服务器燃料配方决定。
        return !resource.isEmpty();
    }

    /** 返回当前宿主是否是浇注设备。 */
    public boolean isCastingBlock() {
        return getBlockState().is(TFBlocks.CASTING_TABLE.get()) || getBlockState().is(TFBlocks.CASTING_BASIN.get());
    }

    /** 返回当前宿主是否是排液口。 */
    private boolean isDrain() {
        return getBlockState().is(TFBlocks.DRAIN.get());
    }

    /** 返回当前宿主是否是浇注口。 */
    private boolean isFaucet() {
        return getBlockState().is(TFBlocks.FAUCET.get());
    }

    /** 返回当前宿主是否应执行熔炼。 */
    public boolean isMeltingBlock() {
        return isSmelteryController() || isFoundryController() || getBlockState().is(TFBlocks.MELTER.get());
    }

    /** 管道横向从西向东传输，导流槽竖直从上向下传输。 */
    private boolean isTransferBlock() {
        return getBlockState().is(TFBlocks.DUCT.get()) || getBlockState().is(TFBlocks.CHUTE.get());
    }

    /** 判断当前设备是否属于排液、浇注或导流类流体设备。 */
    private boolean isFluidTransferBlock() {
        return isDrain() || isFaucet() || isTransferBlock() || getBlockState().is(TFBlocks.FLUID_GAUGE.get());
    }

    /** 判断当前方块是否应打开独立菜单，严格对应 1.20.1 的可开界面设备。 */
    public boolean hasMenuScreen() {
        return isMeltingBlock() || isAlloyer() || isHeater();
    }

    /** 排液口从朝向所指的相邻储液设备抽取流体，单次最多搬运一个桶。 */
    private void tickDrain(Level level) {
        if (drainController() != null) return;
        if (fluid.getAmount() >= capacity()) {
            return;
        }
        Direction sourceDirection = isDirectionalBlock() ? getBlockState().getValue(FoundryDirectionalBlock.FACING) : Direction.DOWN;
        pullFrom(level, sourceDirection);
    }

    /** 浇注口从背面取液并沿朝向输出，只有目标实际接收后才扣除来源。 */
    private void tickFaucet(Level level) {
        if (fluid.isEmpty()) {
            Direction output = outputDirection();
            pullFrom(level, output.getOpposite());
        }
        pushToTarget(level);
    }

    /** 手动触发浇注口，供玩家右键时立即执行一次传输。 */
    public boolean activateFaucet() {
        if (!isFaucet() || level == null || level.isClientSide) {
            return false;
        }
        if (fluid.isEmpty()) {
            Direction output = outputDirection();
            pullFrom(level, output.getOpposite());
        }
        return pushToTarget(level);
    }

    /** 从指定方向之外的相邻设备取出流体，避免从正下方的浇注目标回抽。 */
    private void pullFromAdjacent(Level level, Direction excludedDirection) {
        Direction preferred = isDirectionalBlock() ? getBlockState().getValue(FoundryDirectionalBlock.FACING) : Direction.NORTH;
        if (isFaucet()) {
            preferred = preferred.getOpposite();
        }
        if (preferred != excludedDirection && pullFrom(level, preferred)) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (direction == excludedDirection || direction == preferred) {
                continue;
            }
            if (!(level.getBlockEntity(worldPosition.relative(direction)) instanceof FoundryBlockEntity source) || source == this || source.isFaucet()) {
                continue;
            }
            FluidStack available = source.transferFluid();
            if (available.isEmpty()) {
                continue;
            }
            int requested = Math.min(available.getAmount(), FluidValues.BUCKET);
            int moved = fill(available.copyWithAmount(requested), FluidAction.SIMULATE);
            if (moved <= 0) {
                continue;
            }
            FluidStack drained = source.drain(moved, FluidAction.EXECUTE);
            if (drained.getAmount() == moved) {
                fill(drained, FluidAction.EXECUTE);
                return;
            }
        }
    }

    /** 从一个明确方向的相邻流体设备抽取最多一桶。 */
    private boolean pullFrom(Level level, Direction direction) {
        if (!(level.getBlockEntity(worldPosition.relative(direction)) instanceof FoundryBlockEntity source) || source == this || source.isFaucet()) {
            return false;
        }
        FluidStack available = source.transferFluid();
        if (available.isEmpty()) {
            return false;
        }
        int requested = Math.min(available.getAmount(), FluidValues.BUCKET);
        int moved = fill(available.copyWithAmount(requested), FluidAction.SIMULATE);
        if (moved <= 0) {
            return false;
        }
        FluidStack drained = source.drain(moved, FluidAction.EXECUTE);
        if (drained.getAmount() != moved) {
            return false;
        }
        fill(drained, FluidAction.EXECUTE);
        return true;
    }

    /** 沿设备朝向向相邻设备输出一个桶以内的流体，只有实际接收后才扣除来源。 */
    private boolean pushToTarget(Level level) {
        if (fluid.isEmpty()) {
            return false;
        }
        Direction direction = outputDirection();
        BlockEntity targetEntity = level.getBlockEntity(worldPosition.relative(direction));
        if (!(targetEntity instanceof FoundryBlockEntity)) {
            return false;
        }
        FoundryBlockEntity target = (FoundryBlockEntity) targetEntity;
        if (target == this) {
            return false;
        }
        int requested = Math.min(fluid.getAmount(), FluidValues.BUCKET);
        int moved = target.fill(fluid.copyWithAmount(requested), FluidAction.SIMULATE);
        if (moved <= 0) {
            return false;
        }
        FluidStack drained = drain(moved, FluidAction.EXECUTE);
        if (drained.getAmount() != moved) {
            return false;
        }
        target.fill(drained, FluidAction.EXECUTE);
        return true;
    }

    /** 返回可供排液口、浇注口和导流设备抽取的输出槽流体。 */
    private FluidStack transferFluid() {
        FoundryBlockEntity controller = drainController();
        if (controller != null) return controller.getFluidInTank(0);
        return isStructureController() ? structureFluids.get(0) : fluid.copy();
    }

    /** 只在相邻设备之间搬运当前输出槽位，避免全世界扫描和双向抖动。 */
    private void tickTransfer(Level level) {
        Direction outputSide = outputDirection();
        Direction inputSide = outputSide.getOpposite();
        if (level.getBlockEntity(worldPosition.relative(inputSide)) instanceof FoundryBlockEntity source) {
            FluidStack available = source.transferFluid();
            if (!available.isEmpty()) {
                int moved = fill(available, FluidAction.SIMULATE);
                if (moved > 0) {
                    source.drain(moved, FluidAction.EXECUTE);
                    fill(available.copyWithAmount(moved), FluidAction.EXECUTE);
                }
            }
        }
        if (level.getBlockEntity(worldPosition.relative(outputSide)) instanceof FoundryBlockEntity target) {
            FluidStack available = getFluidInTank(0);
            if (!available.isEmpty()) {
                int moved = target.fill(available, FluidAction.SIMULATE);
                if (moved > 0) {
                    drain(moved, FluidAction.EXECUTE);
                    target.fill(available.copyWithAmount(moved), FluidAction.EXECUTE);
                }
            }
        }
    }

    /** 返回普通槽容量或当前结构容量。 */
    private int capacity() {
        if ((isSmelteryController() || isFoundryController()) && !structureValid) {
            return 0;
        }
        if (isSmelteryController() || isFoundryController()) {
            return Math.max(DEFAULT_CAPACITY, structureCapacity);
        }
        if (isCastingTankBlock()) {
            return FluidValues.BUCKET;
        }
        if (isIngotTankBlock()) {
            return FluidValues.INGOT * 48;
        }
        if (isLanternBlock()) {
            return FoundryLanternBlock.CAPACITY;
        }
        return DEFAULT_CAPACITY;
    }

    /** 判断当前方块是否拥有方向状态。 */
    private boolean isDirectionalBlock() {
        return getBlockState().hasProperty(FoundryDirectionalBlock.FACING);
    }

    /** 返回设备的输出方向，旧式无方向状态默认向下。 */
    private Direction outputDirection() {
        if (isDirectionalBlock()) {
            return getBlockState().getValue(FoundryDirectionalBlock.FACING);
        }
        return Direction.DOWN;
    }

    /** 返回流体在当前注册表中的编号，供菜单客户端恢复流体类型。 */
    private static int fluidRegistryId(FluidStack stack) {
        return stack.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(stack.getFluid());
    }

    /** 返回当前设备对应的客户端界面样式。 */
    public int screenKind() {
        if (isAlloyer()) return 1;
        if (isHeater()) return 2;
        if (isSmelteryController() || isFoundryController()) return 3;
        return 0;
    }

    /** 返回界面应显示的燃料来源，结构控制器优先使用结构内燃料罐。 */
    public FoundryBlockEntity fuelSourceForMenu() {
        if (isHeater()) {
            return this;
        }
        if (isStructureController()) {
            List<FoundryBlockEntity> sources = structureFuelSources();
            // 保留上次燃料优先级；空罐不能遮蔽后面的非空罐。
            for (FoundryBlockEntity source : sources) {
                if (!source.getFluidInTank(0).isEmpty()) return source;
            }
            return sources.isEmpty() ? null : sources.getFirst();
        }
        if (level != null) {
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(direction)) instanceof FoundryBlockEntity source && source.isHeater()) {
                    return source;
                }
            }
        }
        return null;
    }

    /** 返回结构、加热器或相邻加热器当前应展示的燃料流体。 */
    public FluidStack fuelDisplayFluid() {
        FoundryBlockEntity source = fuelSourceForMenu();
        if (source != null) {
            if (isStructureController()) {
                FluidStack shown = source.getFluidInTank(0);
                if (shown.isEmpty()) return FluidStack.EMPTY;
                int amount = 0;
                for (FoundryBlockEntity tank : structureFuelSources()) {
                    FluidStack stored = tank.getFluidInTank(0);
                    if (FluidStack.isSameFluidSameComponents(shown, stored)) amount += stored.getAmount();
                }
                return shown.copyWithAmount(amount);
            }
            return source.isHeater() ? source.fuelFluid.copy() : source.getFluidInTank(0);
        }
        return FluidStack.EMPTY;
    }

    /** 返回燃料栏对应来源的容量。 */
    public int fuelDisplayCapacity() {
        FoundryBlockEntity source = fuelSourceForMenu();
        if (source != null) {
            if (isStructureController()) {
                FluidStack shown = source.getFluidInTank(0);
                int capacity = 0;
                for (FoundryBlockEntity tank : structureFuelSources()) {
                    FluidStack stored = tank.getFluidInTank(0);
                    if (stored.isEmpty() || FluidStack.isSameFluidSameComponents(shown, stored)) capacity += tank.getTankCapacity(0);
                }
                return capacity;
            }
            return source.getTankCapacity(0);
        }
        return 0;
    }

    /** 返回燃料配方温度，流体物理温度不能代替实际熔炼温度。 */
    public int fuelDisplayTemperature() {
        FluidStack display = fuelDisplayFluid();
        if (display.isEmpty() || level == null) return 0;
        return level.getRecipeManager().getAllRecipesFor(TFRecipes.FUEL.get()).stream()
            .filter(holder -> holder.value().matchesFluid(display)).mapToInt(holder -> holder.value().temperature()).findFirst().orElse(0);
    }

    /** 判断当前设备是否存在可以显示或交互的燃料来源。 */
    public boolean hasFuelSource() {
        return fuelSourceForMenu() != null;
    }

    /** 只从验证后的缓存位置取燃料罐，上次使用的罐排在最前。 */
    private List<FoundryBlockEntity> structureFuelSources() {
        List<FoundryBlockEntity> sources = new java.util.ArrayList<>();
        if (level == null) return sources;
        for (BlockPos pos : structureFuelTanks) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof FoundryBlockEntity source && source.isTankBlock()) {
                if (pos.equals(structureFuelTankPos)) sources.add(0, source);
                else sources.add(source);
            }
        }
        return sources;
    }

    /** 多罐同类燃料可联合抽取，模拟阶段不修改任何罐。 */
    public FluidStack drainStructureFuel(FluidStack requested, FluidAction action) {
        if (requested.isEmpty()) return FluidStack.EMPTY;
        int drained = 0;
        for (FoundryBlockEntity source : structureFuelSources()) {
            FluidStack stored = source.getFluidInTank(0);
            if (!FluidStack.isSameFluidSameComponents(stored, requested)) continue;
            drained += source.drainTank(0, requested.getAmount() - drained, action).getAmount();
            if (drained >= requested.getAmount()) break;
        }
        return drained == 0 ? FluidStack.EMPTY : requested.copyWithAmount(drained);
    }

    /** 向多个罐分配燃料，先补同类非空罐，再使用空罐。 */
    public int fillStructureFuel(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        int filled = 0;
        List<FoundryBlockEntity> sources = structureFuelSources();
        for (int pass = 0; pass < 2; pass++) {
            for (FoundryBlockEntity source : sources) {
                if (source.getFluidInTank(0).isEmpty() != (pass == 1)) continue;
                filled += source.fill(resource.copyWithAmount(resource.getAmount() - filled), action);
                if (filled >= resource.getAmount()) return filled;
            }
        }
        return filled;
    }

    /** 返回设备界面和流体计需要展示的主流体；合金炉使用输出槽，加热器使用燃料槽。 */
    public FluidStack getDisplayFluid() {
        if (isStructureController()) return structureFluids.get(0);
        if (isHeater()) {
            return fuelFluid.copy();
        }
        if (isAlloyer()) {
            return fluid.copy();
        }
        if (getBlockState().is(TFBlocks.FLUID_GAUGE.get())) {
            FoundryBlockEntity source = getGaugeSource();
            return source == null ? FluidStack.EMPTY : source.getDisplayFluid();
        }
        return fluid.copy();
    }

    /** 返回流体计读取的相邻设备容量，不改变任何设备的实际储量。 */
    public int getDisplayCapacity() {
        if (getBlockState().is(TFBlocks.FLUID_GAUGE.get())) {
            FoundryBlockEntity source = getGaugeSource();
            if (source == null) {
                return 0;
            }
            return source.isHeater() ? source.getTankCapacity(0)
                : source.isAlloyer() ? source.getTankCapacity(ALLOY_OUTPUT_TANK) : source.getTankCapacity(0);
        }
        return isHeater() ? getTankCapacity(0) : isAlloyer() ? getTankCapacity(ALLOY_OUTPUT_TANK) : getTankCapacity(0);
    }

    /** 返回流体计贴附的相邻冶炼设备，方向语义与 1.20.1 上游保持一致。 */
    public FoundryBlockEntity getGaugeSource() {
        if (!getBlockState().is(TFBlocks.FLUID_GAUGE.get()) || level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(FoundryDirectionalBlock.FACING);
        if (level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof FoundryBlockEntity source
            && !source.getBlockState().is(TFBlocks.FLUID_GAUGE.get())) {
            return source;
        }
        return null;
    }

    /** 兼容菜单内部旧调用，统一走动态展示流体。 */
    private FluidStack displayFluid() {
        return getDisplayFluid();
    }

    /** 合金输出槽使用独立容量限制。 */
    private int fillOutput(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || (!fluid.isEmpty() && !FluidStack.isSameFluid(fluid, resource))) return 0;
        int amount = Math.min(resource.getAmount(), 8000 - fluid.getAmount());
        if (amount > 0 && action.execute()) {
            fluid = fluid.isEmpty() ? resource.copyWithAmount(amount) : fluid.copyWithAmount(fluid.getAmount() + amount);
            markFluidChanged();
        }
        return amount;
    }

    /** Alloyer 暴露四个输入槽和一个输出槽，其余设备暴露一个槽。 */
    @Override
    public int getTanks() {
        FoundryBlockEntity controller = drainController();
        if (controller != null) return controller.getTanks();
        if (isStructureController()) return Math.max(1, structureFluids.size());
        return isAlloyer() ? MAX_ALLOY_INPUTS + 1 : 1;
    }

    /** 返回指定槽位的副本。 */
    @Override
    public FluidStack getFluidInTank(int tank) {
        FoundryBlockEntity controller = drainController();
        if (controller != null) return controller.getFluidInTank(tank);
        if (isStructureController()) return structureFluids.get(tank);
        if (isHeater()) return tank == 0 ? fuelFluid.copy() : FluidStack.EMPTY;
        if (isAlloyer()) return tank >= 0 && tank < MAX_ALLOY_INPUTS ? alloyInputs[tank].copy() : tank == ALLOY_OUTPUT_TANK ? fluid.copy() : FluidStack.EMPTY;
        return tank == 0 ? fluid.copy() : FluidStack.EMPTY;
    }

    /** 返回指定槽位容量。 */
    @Override
    public int getTankCapacity(int tank) {
        FoundryBlockEntity controller = drainController();
        if (controller != null) return controller.getTankCapacity(tank);
        if (tank < 0) return 0;
        if (isStructureController()) return tank < getTanks() ? capacity() : 0;
        if (isHeater()) return tank == 0 ? DEFAULT_CAPACITY : 0;
        if (isAlloyer()) return tank < MAX_ALLOY_INPUTS ? DEFAULT_CAPACITY : tank == ALLOY_OUTPUT_TANK ? 8000 : 0;
        return tank == 0 ? capacity() : 0;
    }

    /** 只有普通槽或合金输入槽接受流体。 */
    @Override
    public boolean isFluidValid(int tank, FluidStack resource) {
        FoundryBlockEntity controller = drainController();
        if (controller != null) return controller.isFluidValid(tank, resource);
        if (resource.isEmpty()) return false;
        if (isStructureController()) return tank >= 0 && tank < getTanks() && !resource.isEmpty();
        if (isHeater()) return tank == 0 && (fuelFluid.isEmpty() || FluidStack.isSameFluid(fuelFluid, resource));
        if (isFuelTankBlock()) return tank == 0 && acceptsTankFluid(resource) && (fluid.isEmpty() || FluidStack.isSameFluid(fluid, resource));
        if (resource.isEmpty()) return false;
        if (isAlloyer()) return tank >= 0 && tank < MAX_ALLOY_INPUTS && (alloyInputs[tank].isEmpty() || FluidStack.isSameFluid(alloyInputs[tank], resource));
        return tank == 0 && (fluid.isEmpty() || FluidStack.isSameFluid(fluid, resource));
    }

    /** 按指定槽位注入流体，供界面交互和合金炉多槽输入使用。 */
    public int fillTank(int tank, FluidStack resource, FluidAction action) {
        FoundryBlockEntity controller = drainController();
        if (controller != null) return controller.fillTank(tank, resource, action);
        if (resource.isEmpty() || tank < 0 || tank >= getTanks() || !isFluidValid(tank, resource)) {
            return 0;
        }
        if (isStructureController()) {
            int accepted = structureFluids.fill(resource, capacity(), action);
            if (accepted > 0 && action.execute()) markFluidChanged();
            return accepted;
        }
        FluidStack stored = getFluidInTank(tank);
        int amount = Math.min(resource.getAmount(), getTankCapacity(tank) - stored.getAmount());
        if (amount > 0 && action.execute()) {
            FluidStack updated = stored.isEmpty() ? resource.copyWithAmount(amount) : stored.copyWithAmount(stored.getAmount() + amount);
            if (isHeater()) {
                fuelFluid = updated;
            } else if (isAlloyer()) {
                alloyInputs[tank] = updated;
            } else {
                fluid = updated;
            }
            markFluidChanged();
        }
        return Math.max(0, amount);
    }

    /** 只接受仍有效且边界仍包含本排液口的控制器，避免拆炉后使用陈旧绑定。 */
    private FoundryBlockEntity drainController() {
        if (!(isDrain() || getBlockState().is(TFBlocks.DUCT.get())) ) return null;
        return attachedController();
    }

    /** 每次访问重新核实附件仍在有效墙面上，缓存能力不能绕过拆炉检查。 */
    public FoundryBlockEntity attachedController() {
        if (level == null || drainControllerPos == null || !level.isLoaded(drainControllerPos)) return null;
        if (!(level.getBlockEntity(drainControllerPos) instanceof FoundryBlockEntity controller)
            || !controller.isStructureController()) return null;
        controller.refreshStructureIfDirty();
        if (!controller.structureValid || controller.structureMin == null || controller.structureMax == null) return null;
        return controller.isStructurePart(worldPosition) ? controller : null;
    }

    /** 底板中心与墙面属于炉体，冶炼炉忽略底板外沿及墙角。 */
    public boolean isStructurePart(BlockPos pos) {
        if (structureMin == null || structureMax == null) return false;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (x < structureMin.getX() || x > structureMax.getX() || y < structureMin.getY()
            || y > structureMax.getY() || z < structureMin.getZ() || z > structureMax.getZ()) return false;
        boolean edgeX = x == structureMin.getX() || x == structureMax.getX();
        boolean edgeZ = z == structureMin.getZ() || z == structureMax.getZ();
        if (y == structureMin.getY()) return isFoundryController() || !edgeX && !edgeZ;
        return (edgeX || edgeZ) && (isFoundryController() || !(edgeX && edgeZ));
    }

    /** 世界显示和实体交互共享炉腔范围，顶部使用方块上边界。 */
    public net.minecraft.world.phys.AABB interiorBounds() {
        if (!structureValid || structureMin == null || structureMax == null) return null;
        return new net.minecraft.world.phys.AABB(structureMin.getX() + 1, structureMin.getY() + 1, structureMin.getZ() + 1,
            structureMax.getX(), structureMax.getY() + 1, structureMax.getZ());
    }

    /** 模拟或执行流体注入。 */
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (isAlloyer()) {
            int matchingTank = -1;
            for (int tank = 0; tank < MAX_ALLOY_INPUTS; tank++) {
                if (!alloyInputs[tank].isEmpty() && FluidStack.isSameFluid(alloyInputs[tank], resource)) {
                    matchingTank = tank;
                    break;
                }
            }
            if (matchingTank < 0) {
                for (int tank = 0; tank < MAX_ALLOY_INPUTS; tank++) {
                    if (alloyInputs[tank].isEmpty()) {
                        matchingTank = tank;
                        break;
                    }
                }
            }
            return matchingTank >= 0 ? fillTank(matchingTank, resource, action) : 0;
        }
        return fillTank(0, resource, action);
    }

    /** 按指定槽位抽取流体，供界面从合金炉输入槽或输出槽取液。 */
    public FluidStack drainTank(int tank, int maxDrain, FluidAction action) {
        FoundryBlockEntity controller = drainController();
        if (controller != null) return controller.drainTank(tank, maxDrain, action);
        if (tank < 0 || tank >= getTanks() || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        if (isStructureController()) {
            FluidStack drained = structureFluids.drain(tank, maxDrain, action);
            if (!drained.isEmpty() && action.execute()) markFluidChanged();
            return drained;
        }
        FluidStack stored = getFluidInTank(tank);
        if (stored.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int amount = Math.min(maxDrain, stored.getAmount());
        FluidStack result = stored.copyWithAmount(amount);
        if (action.execute()) {
            FluidStack updated = stored.copyWithAmount(stored.getAmount() - amount);
            if (updated.isEmpty()) {
                updated = FluidStack.EMPTY;
            }
            if (isHeater()) {
                fuelFluid = updated;
            } else if (isAlloyer()) {
                if (tank < MAX_ALLOY_INPUTS) {
                    alloyInputs[tank] = updated;
                } else {
                    fluid = updated;
                }
            } else {
                fluid = updated;
            }
            markFluidChanged();
        }
        return result;
    }

    /** 按流体类型抽取。 */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FoundryBlockEntity controller = drainController();
        if (controller != null) return controller.drain(resource, action);
        if (isStructureController()) return drainTank(structureFluids.indexOf(resource), resource.getAmount(), action);
        FluidStack stored = getFluidInTank(isAlloyer() ? ALLOY_OUTPUT_TANK : 0);
        if (resource.isEmpty() || stored.isEmpty() || !FluidStack.isSameFluid(stored, resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    /** 按数量抽取输出流体。 */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return drainTank(isAlloyer() ? ALLOY_OUTPUT_TANK : 0, maxDrain, action);
    }
}
