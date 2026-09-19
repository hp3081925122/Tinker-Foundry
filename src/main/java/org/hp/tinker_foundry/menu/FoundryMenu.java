package org.hp.tinker_foundry.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.item.PortableTankFluidHandler;
import org.hp.tinker_foundry.item.FoundryTankItem;
import org.hp.tinker_foundry.item.PortableTankItem;
import org.hp.tinker_foundry.multiblock.StructureErrorReason;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.registry.TFMenus;

/** 冶炼设备的统一菜单，物品槽和状态数据均由服务端方块实体驱动。 */
public final class FoundryMenu extends AbstractContainerMenu {
    /** 当前真实输入加三个功能槽的菜单长度，不包含玩家背包。 */
    private final int deviceSlots;
    /** 菜单打开时的输入数量，服务端变化后关闭旧菜单，保证槽号稳定。 */
    private final int menuInputs;
    /** 客户端侧栏滚动只影响可见性，不改变服务端输入编号。 */
    private final boolean clientSide;
    private int scrollRow;
    /** 服务端和客户端菜单同步的整数数量。 */
    private static final int DATA_COUNT = FoundryBlockEntity.MENU_DATA_COUNT;
    /** 菜单物品容器。 */
    private final Container container;
    /** 菜单状态同步容器。 */
    private final ContainerData data;
    /** 打开菜单时确定的布局类型，屏幕初始化不能等待后续状态同步。 */
    private final int screenKind;
    /** 每列包含四像素热量区和十八像素物品槽，背景与命中坐标共同使用。 */
    public static final int STRUCTURE_COLUMN_WIDTH = 22;
    /** 控制器中央的输入桶槽和输出桶槽数量。 */
    private static final int STRUCTURE_BUCKET_SLOTS = 2;
    /** 传输模式与匠魂原版按钮顺序保持一致。 */
    public enum TransferDirection {
        AUTO,
        EMPTY_ITEM,
        FILL_ITEM;

        /** 切换到下一个传输模式。 */
        public TransferDirection next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
    /** 控制器专用的桶输入输出容器。 */
    private final Container bucketContainer;
    /** 当前菜单的流体容器传输模式。 */
    private TransferDirection transferDirection = TransferDirection.AUTO;
    /** 同一游戏刻只自动处理一次，避免菜单重复广播加速流体传输。 */
    private long lastBucketProcessTick = Long.MIN_VALUE;
    /** 创建客户端镜像菜单。 */
    public FoundryMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(FoundryBlockEntity.CONTAINER_SIZE), new SimpleContainer(STRUCTURE_BUCKET_SLOTS),
            new SimpleContainerData(DATA_COUNT), 0, FoundryBlockEntity.BASE_INPUT_SLOTS);
    }

    /** 创建绑定真实设备的服务端菜单。 */
    public FoundryMenu(int containerId, Inventory inventory, FoundryBlockEntity entity) {
        this(containerId, inventory,
            entity == null ? new SimpleContainer(FoundryBlockEntity.CONTAINER_SIZE) : entity,
            new SimpleContainer(STRUCTURE_BUCKET_SLOTS),
            entity == null ? new SimpleContainerData(DATA_COUNT) : entity.menuData(),
            entity == null ? 0 : entity.screenKind(),
            entity != null && entity.isStructureController() ? entity.inputSlotCount() : FoundryBlockEntity.BASE_INPUT_SLOTS);
    }

    /** 从服务端打开菜单时读取方块位置，让客户端使用正确的设备槽位布局。 */
    public FoundryMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, readOpenData(inventory, buffer));
    }

    /** 创建带有服务端界面类型的客户端菜单，确保槽位在首次构造时就使用正确布局。 */
    private FoundryMenu(int containerId, Inventory inventory, OpenData open) {
        this(containerId, inventory,
            open.entity() == null ? new SimpleContainer(Math.max(FoundryBlockEntity.BASE_INPUT_SLOTS, open.inputs()) + 3) : open.entity(),
            new SimpleContainer(STRUCTURE_BUCKET_SLOTS),
            new SimpleContainerData(FoundryBlockEntity.menuDataCount(open.inputs())),
            open.kind(), open.inputs());
    }

    /** 打开载荷先携带槽数，不能等整数同步包到达后才决定菜单布局。 */
    private record OpenData(FoundryBlockEntity entity, int kind, int inputs) { }

    /** 校验网络槽数并准备客户端容器，避免服务端已有高编号槽而客户端数组未扩容。 */
    private static OpenData readOpenData(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        FoundryBlockEntity entity = findEntity(inventory, buffer.readBlockPos());
        int kind = buffer.readVarInt();
        int slots = buffer.readVarInt();
        if (slots < 0 || slots > FoundryBlockEntity.MAX_STRUCTURE_INPUTS) throw new IllegalArgumentException("Invalid structure input count: " + slots);
        if (kind == 3 && entity != null) entity.resizeStructureInputs(slots, false);
        return new OpenData(entity, kind, kind == 3 ? slots : FoundryBlockEntity.BASE_INPUT_SLOTS);
    }

    /** 根据菜单打开载荷查找客户端方块实体，找不到时安全退化为空菜单。 */
    private static FoundryBlockEntity findEntity(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof FoundryBlockEntity entity ? entity : null;
    }

    /** 返回菜单绑定的设备，供状态网络层限定发送目标。 */
    public FoundryBlockEntity blockEntity() {
        return container instanceof FoundryBlockEntity entity ? entity : null;
    }

    /** 初始化设备槽、玩家背包槽和服务端状态同步。 */
    private FoundryMenu(int containerId, Inventory inventory, Container container, Container bucketContainer,
                        ContainerData data, int screenKind, int menuInputs) {
        super(TFMenus.FOUNDRY.get(), containerId);
        checkContainerSize(container, Math.max(FoundryBlockEntity.BASE_INPUT_SLOTS, menuInputs) + 3);
        checkContainerSize(bucketContainer, STRUCTURE_BUCKET_SLOTS);
        checkContainerDataCount(data, FoundryBlockEntity.menuDataCount(menuInputs));
        this.container = container;
        this.bucketContainer = bucketContainer;
        this.data = data;
        this.menuInputs = menuInputs;
        this.deviceSlots = menuInputs + 3;
        this.clientSide = inventory.player.level().isClientSide;
        // 槽位布局和屏幕尺寸共同读取打开载荷中的类型，不受状态包到达顺序影响。
        this.screenKind = screenKind;
        TinkerFoundry.LOGGER.debug("[menu-layout] initialized screenKind={} syncedKind={}", screenKind, data.get(11));
        // 熔炼器和控制器显示固体输入槽，合金炉和加热器不显示伪造的固体输入槽。
        for (int index = 0; index < menuInputs; index++) {
            final int slotIndex = index;
            int x;
            int y;
            if (screenKind == 3) {
                // 多方块控制器的物品输入位于左侧，四列排列，中央区域只绘制流体和燃料模块。
                // 四像素边框之后先留热量条，物品从贴图内第五像素开始。
                x = 9 + (index % sideColumns()) * STRUCTURE_COLUMN_WIDTH;
                y = 5 + (index / sideColumns()) * 18;
            } else {
                x = screenKind == 0 ? 22 : 17 + (index % 3) * 18;
                y = screenKind == 0 ? 16 + index * 18 : 17 + (index / 3) * 18;
            }
            addSlot(new Slot(container, FoundryBlockEntity.inputContainerSlot(index), x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(FoundryBlockEntity.inputContainerSlot(slotIndex), stack);
                }

                /** 原版多方块熔炼每槽只放一个物品，快捷移动会继续寻找其余空槽。 */
                @Override
                public int getMaxStackSize() {
                    return screenKind == 3 ? 1 : super.getMaxStackSize();
                }

                @Override
                public boolean isActive() {
                    return inputSlotCount() > slotIndex && (screenKind != 3 || !clientSide
                        || slotIndex >= scrollRow * sideColumns() && slotIndex < (scrollRow + sideRows()) * sideColumns());
                }
            });
        }
        int contentOffset = screenKind == 3 ? sideWidth() : 0;
        int fuelX = (screenKind == 2 ? 80 : 153) + contentOffset;
        int fuelY = screenKind == 2 ? 20 : 31;
        if (screenKind == 3) {
            // 控制器使用中央原版燃料栏，不把燃料物品槽伪装成可交互槽位。
            fuelX = -100;
            fuelY = -100;
        }
        addSlot(new Slot(container, FoundryBlockEntity.FUEL_SLOT, fuelX, fuelY) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return screenKind != 3 && container.canPlaceItem(FoundryBlockEntity.FUEL_SLOT, stack);
            }

            @Override
            public boolean isActive() {
                return screenKind != 3;
            }
        });
        addSlot(new Slot(container, FoundryBlockEntity.OUTPUT_SLOT, 153 + contentOffset, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return screenKind != 3;
            }
        });
        addSlot(new Slot(container, FoundryBlockEntity.REMAINDER_SLOT, 125 + contentOffset, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return screenKind != 3;
            }
        });

        if (screenKind == 3) {
            // 控制器桶槽沿用 HeatingStructureContainerMenu 的 125,46 和 125,104 坐标。
            addSlot(new BucketInputSlot(bucketContainer, 0, sideWidth() + 125, 46));
            addSlot(new BucketOutputSlot(bucketContainer, 1, sideWidth() + 125, 104));
        }

        // 加热器使用 1.20.1 单物品界面高度，其余设备使用普通界面高度。
        int inventoryY = screenKind == 2 ? 51 : screenKind == 3 ? 138 : 84;
        int inventoryOffset = screenKind == 3 ? sideWidth() : 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, inventoryOffset + 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, inventoryOffset + 8 + column * 18, inventoryY + 58));
        }
        addDataSlots(data);
    }

    /** 控制器中央的物品输入槽，只接受当前模组支持的流体容器。 */
    private final class BucketInputSlot extends Slot {
        /** 创建桶输入槽。 */
        private BucketInputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        /** 限制普通方块和工具进入流体容器槽。 */
        @Override
        public boolean mayPlace(ItemStack stack) {
            return isFluidContainer(stack);
        }

        /** 服务器完成放入动作后立即按当前模式处理一个容器。 */
        @Override
        public void setByPlayer(ItemStack stack, ItemStack oldStack) {
            super.setByPlayer(stack, oldStack);
            if (FoundryMenu.this.container instanceof FoundryBlockEntity entity && entity.getLevel() != null && !entity.getLevel().isClientSide) {
                processBucketInput(entity);
            }
        }
    }

    /** 控制器中央的物品输出槽，禁止外部物品直接写入。 */
    private static final class BucketOutputSlot extends Slot {
        /** 创建桶输出槽。 */
        private BucketOutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        /** 输出槽不接受玩家放入的物品。 */
        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /** 判断一个物品是否属于菜单允许的流体容器。 */
    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof BucketItem || stack.getItem() instanceof PortableTankItem
            || stack.getItem() instanceof FoundryTankItem;
    }

    /** 返回菜单绑定的桶容器，供客户端槽位和调试使用。 */
    public Container bucketContainer() {
        return bucketContainer;
    }

    /** 返回当前桶传输模式。 */
    public TransferDirection transferDirection() {
        return transferDirection;
    }

    /** 返回当前菜单同步的工作进度。 */
    public int progress() {
        return Math.max(0, data.get(0));
    }

    /** 返回当前菜单同步的工作总时长。 */
    public int processTime() {
        return Math.max(0, data.get(1));
    }

    /** 返回当前燃料剩余 tick。 */
    public int burnTime() {
        return Math.max(0, data.get(2));
    }

    /** 返回本次燃料提供的总燃烧量，用于按剩余比例绘制火焰。 */
    public int fuelBurnDuration() {
        return Math.max(0, data.get(FoundryBlockEntity.MENU_FUEL_DURATION_INDEX));
    }

    /** 返回当前普通流体或合金输出容量。 */
    public int capacity() {
        return Math.max(0, data.get(3));
    }

    /** 返回当前结构有效标记。 */
    public boolean structureValid() {
        return data.get(4) != 0;
    }

    /** 返回服务端同步的结构错误类型，供控制器界面显示具体原因。 */
    public StructureErrorReason structureErrorReason() {
        return StructureErrorReason.fromId(data.get(17));
    }

    /** 返回普通流体或合金输出流体量。 */
    public int fluidAmount() {
        return Math.max(0, data.get(5));
    }

    /** 返回合金第一输入流体量。 */
    public int firstAlloyAmount() {
        return Math.max(0, data.get(6));
    }

    /** 返回合金第二输入流体量。 */
    public int secondAlloyAmount() {
        return Math.max(0, data.get(7));
    }

    /** 返回普通设备流体或合金炉输出流体的客户端镜像。 */
    public FluidStack fluidStack() {
        if (screenKind == 3 && blockEntity() != null) return blockEntity().getFluidInTank(0);
        return fluidFromData(8, 5);
    }

    /** 返回服务器同步的有序流体层，界面绘制与悬停共同使用同一快照。 */
    public java.util.List<FluidStack> structureFluids() {
        return blockEntity() == null ? java.util.List.of() : blockEntity().structureFluidLayers();
    }

    /** 返回合金炉第一输入流体的客户端镜像。 */
    public FluidStack firstAlloyFluid() {
        return fluidFromData(9, 6);
    }

    /** 返回合金炉第二输入流体的客户端镜像。 */
    public FluidStack secondAlloyFluid() {
        return fluidFromData(10, 7);
    }

    /** 返回指定合金输入槽的客户端流体镜像。 */
    public FluidStack alloyFluid(int tank) {
        if (tank < 0 || tank >= FoundryBlockEntity.MAX_ALLOY_INPUTS) {
            return FluidStack.EMPTY;
        }
        int amountIndex = switch (tank) {
            case 0 -> 6;
            case 1 -> 7;
            case 2 -> 13;
            case 3 -> 15;
            default -> -1;
        };
        int fluidIndex = switch (tank) {
            case 0 -> 9;
            case 1 -> 10;
            case 2 -> 14;
            case 3 -> 16;
            default -> -1;
        };
        return fluidFromData(fluidIndex, amountIndex);
    }

    /** 返回指定合金输入槽的服务端同步数量。 */
    public int alloyAmount(int tank) {
        if (tank < 0 || tank >= FoundryBlockEntity.MAX_ALLOY_INPUTS) {
            return 0;
        }
        return switch (tank) {
            case 0 -> Math.max(0, data.get(6));
            case 1 -> Math.max(0, data.get(7));
            case 2 -> Math.max(0, data.get(13));
            case 3 -> Math.max(0, data.get(15));
            default -> 0;
        };
    }

    /** 返回构造菜单时已经确定的界面样式。 */
    public int screenKind() {
        return screenKind;
    }

    /** 返回服务端启用的物品输入槽数量。 */
    public int inputSlotCount() {
        return screenKind == 3 ? menuInputs : Math.max(0, Math.min(menuInputs, data.get(12)));
    }

    /** 每七槽增加一列，最多四列，与上游列数规则相同。 */
    public int sideColumns() {
        return Math.max(1, Math.min(4, (menuInputs + 6) / 7));
    }

    /** 主面板高 220，侧栏在边框内最多显示十一行，其余通过滚动访问。 */
    public int sideRows() {
        return Math.min(11, Math.max(1, (menuInputs + sideColumns() - 1) / sideColumns()));
    }

    /** 当前滚动范围，按完整行滚动而不改变菜单槽索引。 */
    public int maxScrollRow() {
        return Math.max(0, (menuInputs + sideColumns() - 1) / sideColumns() - sideRows());
    }

    /** 侧栏为物品网格、边框和按需出现的六像素滚动条。 */
    public int sideWidth() {
        return 8 + sideColumns() * STRUCTURE_COLUMN_WIDTH + (maxScrollRow() > 0 ? 6 : 0);
    }

    /** 返回当前首行，供背景、热量条和滚动条共用。 */
    public int scrollRow() {
        return scrollRow;
    }

    /** 只在客户端更新槽位位置，服务端仍按稳定索引处理点击。 */
    public void scrollTo(int row) {
        if (!clientSide || screenKind != 3) return;
        int next = Math.max(0, Math.min(maxScrollRow(), row));
        if (next == scrollRow) return;
        scrollRow = next;
        for (int index = 0; index < menuInputs; index++) {
            slots.get(index).y = 5 + (index / sideColumns() - scrollRow) * 18;
        }
        TinkerFoundry.LOGGER.debug("[inventory-scroll] firstRow={} maxRow={} slots={}", scrollRow, maxScrollRow(), menuInputs);
    }

    /** 返回多方块炉当前燃料提供的温度。 */
    public int fuelTemperature() {
        return Math.max(0, data.get(FoundryBlockEntity.MENU_FUEL_TEMPERATURE_INDEX));
    }

    /** 返回燃料栏当前显示的流体数量。 */
    public int fuelAmount() {
        return Math.max(0, data.get(FoundryBlockEntity.MENU_FUEL_AMOUNT_INDEX));
    }

    /** 返回燃料栏当前显示来源的容量。 */
    public int fuelCapacity() {
        return Math.max(0, data.get(FoundryBlockEntity.MENU_FUEL_CAPACITY_INDEX));
    }

    /** 返回燃料栏当前流体的客户端镜像。 */
    public FluidStack fuelFluidStack() {
        return fluidFromData(FoundryBlockEntity.MENU_FUEL_FLUID_INDEX, FoundryBlockEntity.MENU_FUEL_AMOUNT_INDEX);
    }

    /** 返回结构是否有燃料罐、加热器或已缓存的燃料。 */
    public boolean hasFuelSource() {
        return data.get(FoundryBlockEntity.MENU_FUEL_SOURCE_INDEX) != 0;
    }

    /** 返回指定输入槽的当前熔炼进度。 */
    public int inputProgress(int slot) {
        return validInputIndex(slot) ? Math.max(0, data.get(inputDataIndex(slot, 0))) : 0;
    }

    /** 返回指定输入槽当前配方的总处理时间。 */
    public int inputProcessTime(int slot) {
        return validInputIndex(slot) ? Math.max(0, data.get(inputDataIndex(slot, 1))) : 0;
    }

    /** 返回指定输入槽当前配方的所需温度。 */
    public int inputRequiredTemperature(int slot) {
        return validInputIndex(slot) ? Math.max(0, data.get(inputDataIndex(slot, 2))) : 0;
    }

    /** 返回指定输入槽当前的服务端处理状态。 */
    public int inputStatus(int slot) {
        return validInputIndex(slot) ? data.get(inputDataIndex(slot, 3)) : FoundryBlockEntity.INPUT_STATUS_EMPTY;
    }

    /** 判断输入槽索引是否落在本次打开的真实输入范围。 */
    private boolean validInputIndex(int slot) {
        return slot >= 0 && slot < menuInputs;
    }

    /** 基础字段与扩展字段由同一索引函数读取，不截断二十八号以后的进度。 */
    private static int inputDataIndex(int slot, int field) {
        return slot < FoundryBlockEntity.BASE_INPUT_SLOTS
            ? FoundryBlockEntity.MENU_INPUT_PROGRESS_START + field * FoundryBlockEntity.BASE_INPUT_SLOTS + slot
            : FoundryBlockEntity.MENU_DATA_COUNT + (slot - FoundryBlockEntity.BASE_INPUT_SLOTS) * 4 + field;
    }

    /** 返回界面可交互的流体槽数量，合金炉为四个输入槽加一个输出槽。 */
    public int fluidTankCount() {
        return screenKind() == 1 ? FoundryBlockEntity.MAX_ALLOY_INPUTS + 1 : 1;
    }

    /** 使用注册表编号重建客户端流体堆栈，未知编号安全降级为空。 */
    private FluidStack fluidFromData(int fluidIndex, int amountIndex) {
        int amount = data.get(amountIndex);
        Fluid fluid = BuiltInRegistries.FLUID.byId(data.get(fluidIndex));
        return amount <= 0 || fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    /** 限制菜单只能在设备仍存在且玩家距离足够近时使用。 */
    @Override
    public boolean stillValid(Player player) {
        return container instanceof FoundryBlockEntity entity
            ? Container.stillValidBlockEntity(entity, player) && (screenKind != 3 || entity.isStructureValid() && entity.inputSlotCount() == menuInputs)
            : container.stillValid(player);
    }

    /** 缩容或关闭时归还临时桶槽，避免刷新菜单造成玩家物品丢失。 */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, bucketContainer);
    }

    /** 接收客户端按钮请求，覆盖结构控制器的模式、燃料和主流体三类交互。 */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.isSpectator()) {
            return false;
        }
        if (screenKind() == 3) {
            // 选层请求只在服务端校验并执行，空手点击不会凭客户端预测改变排液顺序。
            if (id >= 16 && id < 16 + org.hp.tinker_foundry.common.StructureFluidTank.MAX_LAYERS) {
                if (player.level().isClientSide) return true;
                if (!(container instanceof FoundryBlockEntity entity) || !entity.isStructureValid()) return false;
                boolean changed = getCarried().isEmpty() ? entity.selectStructureFluid(id - 16)
                    : transferHeldFluid(player, entity, getCarried(), id - 16, TransferDirection.FILL_ITEM);
                // 更换排液层后重新判断等待中的容器，不要求玩家取出再放入。
                if (changed) processBucketInput(entity);
                return changed;
            }
            if (id == 0) {
                transferDirection = transferDirection.next();
                if (!player.level().isClientSide) {
                    TinkerFoundry.LOGGER.debug("[menu] structure transfer mode changed to {}", transferDirection);
                    if (container instanceof FoundryBlockEntity entity) processBucketInput(entity);
                }
                return true;
            }
            if (id < 1 || id > 3) {
                return false;
            }
            if (player.level().isClientSide) {
                return true;
            }
            if (!(container instanceof FoundryBlockEntity entity) || getCarried().isEmpty()) {
                return false;
            }
            if (id <= 2) return transferStructureFuel(player, entity, id == 1);
            // 主槽空白处和右键只倒入容器；桶槽的模式按钮不改变鼠标直接交互规则。
            boolean moved = transferHeldFluid(player, entity, getCarried(), 0, TransferDirection.EMPTY_ITEM);
            if (moved) {
                TinkerFoundry.LOGGER.debug("[menu] structure fluid transfer id={} target={} amount={}",
                    id, entity.getBlockPos(), entity.getFluidInTank(0).getAmount());
            }
            return moved;
        }
        if (id < 0 || id >= fluidTankCount() * 2) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        if (!(container instanceof FoundryBlockEntity entity) || getCarried().isEmpty()) {
            return false;
        }
        int tank = id / 2;
        TransferDirection direction = (id & 1) == 0 ? TransferDirection.FILL_ITEM : TransferDirection.EMPTY_ITEM;
        return transferHeldFluid(player, entity, getCarried(), tank, direction);
    }

    /** 根据手持物品类型选择桶、便携储液罐或专用储液罐的传输实现。 */
    private boolean transferHeldFluid(Player player, FoundryBlockEntity entity, ItemStack held, int tank,
                                      TransferDirection direction) {
        ItemStack result = transferContainerStack(entity, held, tank, direction);
        if (result.isEmpty()) {
            return false;
        }
        if (held.getItem() instanceof BucketItem) {
            replaceCarried(player, held, result);
        } else {
            setCarried(held);
        }
        return true;
    }

    /** 燃料栏操作跨多个罐执行，先模拟整个容器，防止不足一桶时部分扣液。 */
    private boolean transferStructureFuel(Player player, FoundryBlockEntity entity, boolean fillItem) {
        ItemStack held = getCarried();
        if (held.getItem() instanceof BucketItem bucket) {
            if (fillItem) {
                FluidStack shown = entity.fuelDisplayFluid();
                if (bucket.content != Fluids.EMPTY || shown.isEmpty() || shown.getFluid().getBucket() == Items.AIR) return false;
                FluidStack requested = shown.copyWithAmount(1000);
                if (entity.drainStructureFuel(requested, FluidAction.SIMULATE).getAmount() != 1000) return false;
                entity.drainStructureFuel(requested, FluidAction.EXECUTE);
                replaceCarried(player, held, new ItemStack(shown.getFluid().getBucket()));
            } else {
                if (bucket.content == Fluids.EMPTY) return false;
                FluidStack offered = new FluidStack(bucket.content, 1000);
                if (entity.fillStructureFuel(offered, FluidAction.SIMULATE) != 1000) return false;
                entity.fillStructureFuel(offered, FluidAction.EXECUTE);
                replaceCarried(player, held, new ItemStack(Items.BUCKET));
            }
            return true;
        }
        // 便携罐沿用已有物品处理器，同样先确认两侧可搬运数量。
        int capacity;
        boolean fuelAllowed;
        if (held.getItem() instanceof FoundryTankItem item) {
            capacity = item.capacity();
            fuelAllowed = item.allowsFuel();
        } else if (held.getItem() instanceof PortableTankItem item) {
            capacity = item.capacity();
            fuelAllowed = false;
        } else return false;
        PortableTankFluidHandler item = new PortableTankFluidHandler(held, capacity, fuelAllowed);
        if (fillItem) {
            FluidStack shown = entity.fuelDisplayFluid();
            int accepted = item.fill(shown, FluidAction.SIMULATE);
            if (accepted <= 0) return false;
            FluidStack drained = entity.drainStructureFuel(shown.copyWithAmount(accepted), FluidAction.EXECUTE);
            item.fill(drained, FluidAction.EXECUTE);
        } else {
            FluidStack offered = item.getFluidInTank(0);
            int accepted = entity.fillStructureFuel(offered, FluidAction.SIMULATE);
            if (accepted <= 0) return false;
            entity.fillStructureFuel(item.drain(accepted, FluidAction.EXECUTE), FluidAction.EXECUTE);
        }
        setCarried(held);
        return true;
    }

    /** 对指定容器执行一次模拟确认后的实际流体传输，成功时返回已经更新的容器。 */
    private ItemStack transferContainerStack(FoundryBlockEntity entity, ItemStack stack, int tank,
                                              TransferDirection direction) {
        TransferDirection actual = resolveDirection(stack, direction);
        if (stack.getItem() instanceof BucketItem bucket) {
            return transferBucketStack(entity, bucket, tank, actual);
        }
        if (stack.getItem() instanceof PortableTankItem portableTank) {
            return transferPortableTankStack(entity, stack, portableTank.capacity(), false, tank, actual);
        }
        if (stack.getItem() instanceof FoundryTankItem foundryTank) {
            return transferPortableTankStack(entity, stack, foundryTank.capacity(), foundryTank.allowsFuel(), tank, actual);
        }
        return ItemStack.EMPTY;
    }

    /** 自动模式下空容器取液，满容器倒液，手动模式严格按按钮方向执行。 */
    private static TransferDirection resolveDirection(ItemStack stack, TransferDirection direction) {
        if (direction != TransferDirection.AUTO) {
            return direction;
        }
        if (stack.getItem() instanceof BucketItem bucket) {
            return bucket.content == Fluids.EMPTY ? TransferDirection.FILL_ITEM : TransferDirection.EMPTY_ITEM;
        }
        if (stack.getItem() instanceof PortableTankItem portable) {
            return portableFluidEmpty(stack, portable.capacity(), false) ? TransferDirection.FILL_ITEM : TransferDirection.EMPTY_ITEM;
        }
        if (stack.getItem() instanceof FoundryTankItem tank) {
            return portableFluidEmpty(stack, tank.capacity(), tank.allowsFuel()) ? TransferDirection.FILL_ITEM : TransferDirection.EMPTY_ITEM;
        }
        return direction;
    }

    /** 检查便携容器当前是否为空。 */
    private static boolean portableFluidEmpty(ItemStack stack, int capacity, boolean allowFuel) {
        return new PortableTankFluidHandler(stack, capacity, allowFuel).getFluidInTank(0).isEmpty();
    }

    /** 在便携储液罐和指定设备槽之间执行一次流体传输。 */
    private ItemStack transferPortableTankStack(FoundryBlockEntity entity, ItemStack stack, int capacity,
                                                 boolean allowFuel, int tank, TransferDirection direction) {
        PortableTankFluidHandler portableTank = new PortableTankFluidHandler(stack, capacity, allowFuel);
        if (direction == TransferDirection.FILL_ITEM) {
            FluidStack available = entity.getFluidInTank(tank);
            int accepted = portableTank.fill(available, FluidAction.SIMULATE);
            if (accepted <= 0) {
                return ItemStack.EMPTY;
            }
            FluidStack drained = entity.drainTank(tank, accepted, FluidAction.SIMULATE);
            if (drained.getAmount() != accepted) {
                return ItemStack.EMPTY;
            }
            entity.drainTank(tank, accepted, FluidAction.EXECUTE);
            portableTank.fill(drained, FluidAction.EXECUTE);
        } else {
            FluidStack contained = portableTank.getFluidInTank(0);
            int accepted = entity.fillTank(tank, contained, FluidAction.SIMULATE);
            if (accepted <= 0) {
                return ItemStack.EMPTY;
            }
            FluidStack drained = portableTank.drain(accepted, FluidAction.SIMULATE);
            if (drained.getAmount() != accepted) {
                return ItemStack.EMPTY;
            }
            portableTank.drain(accepted, FluidAction.EXECUTE);
            entity.fillTank(tank, drained, FluidAction.EXECUTE);
        }
        return stack;
    }

    /** 在桶和指定设备槽之间执行一桶流体传输。 */
    private ItemStack transferBucketStack(FoundryBlockEntity entity, BucketItem bucket, int tank,
                                          TransferDirection direction) {
        if (direction == TransferDirection.FILL_ITEM) {
            if (bucket.content != Fluids.EMPTY) {
                return ItemStack.EMPTY;
            }
            FluidStack available = entity.getFluidInTank(tank);
            if (available.getAmount() < 1000 || available.getFluid().getBucket() == null
                || available.getFluid().getBucket() == Items.AIR) {
                return ItemStack.EMPTY;
            }
            FluidStack drained = entity.drainTank(tank, 1000, FluidAction.SIMULATE);
            if (drained.getAmount() != 1000) {
                return ItemStack.EMPTY;
            }
            entity.drainTank(tank, 1000, FluidAction.EXECUTE);
            return new ItemStack(drained.getFluid().getBucket());
        }
        if (bucket.content == Fluids.EMPTY || !TFFluids.isFoundryFluid(bucket.content)
            && !entity.isHeater() && !entity.isFuelTankBlock()) {
            return ItemStack.EMPTY;
        }
        FluidStack resource = new FluidStack(bucket.content, 1000);
        if (entity.fillTank(tank, resource, FluidAction.SIMULATE) != 1000) {
            return ItemStack.EMPTY;
        }
        entity.fillTank(tank, resource, FluidAction.EXECUTE);
        return new ItemStack(Items.BUCKET);
    }

    /** 菜单持续同步时重试等待中的桶，覆盖新产液和玩家取走输出后的情况。 */
    @Override
    public void broadcastChanges() {
        if (!clientSide && container instanceof FoundryBlockEntity entity && entity.getLevel() != null) {
            long tick = entity.getLevel().getGameTime();
            if (lastBucketProcessTick != tick) {
                lastBucketProcessTick = tick;
                processBucketInput(entity);
            }
        }
        // 必须先完成传输，再让原版同步本轮变化的输入和输出槽。
        super.broadcastChanges();
    }

    /** 在桶槽中处理一个容器，并把处理结果放到输出槽，避免容器被吞掉。 */
    private void processBucketInput(FoundryBlockEntity entity) {
        if (clientSide || screenKind() != 3 || !entity.isStructureValid()) {
            return;
        }
        ItemStack input = bucketContainer.getItem(0);
        if (input.isEmpty() || !isFluidContainer(input) || !bucketContainer.getItem(1).isEmpty()) {
            return;
        }
        ItemStack unit = input.copyWithCount(1);
        ItemStack result = transferContainerStack(entity, unit, 0, transferDirection);
        if (result.isEmpty()) {
            return;
        }
        input.shrink(1);
        bucketContainer.setItem(0, input);
        bucketContainer.setItem(1, result);
        TinkerFoundry.LOGGER.debug("[menu] structure bucket slot processed direction={} result={}", transferDirection, result.getItem());
    }

    /** 替换手持的一个流体容器，并把剩余堆叠安全放回玩家背包。 */
    private void replaceCarried(Player player, ItemStack held, ItemStack result) {
        ItemStack remainder = held.copy();
        remainder.shrink(1);
        if (remainder.isEmpty()) {
            setCarried(result);
            return;
        }
        if (ItemStack.isSameItemSameComponents(remainder, result)
            && remainder.getCount() + result.getCount() <= remainder.getMaxStackSize()) {
            remainder.grow(result.getCount());
            setCarried(remainder);
        } else {
            setCarried(remainder);
            player.getInventory().placeItemBackInInventory(result);
        }
    }

    /** 快捷移动优先进入燃料槽，再进入当前启用的输入槽，禁止进入输出槽。 */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        int playerSlotStart = deviceSlots + (screenKind() == 3 ? STRUCTURE_BUCKET_SLOTS : 0);
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < deviceSlots) {
            if (!moveItemStackTo(stack, playerSlotStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < playerSlotStart) {
            if (!moveItemStackTo(stack, playerSlotStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (screenKind() == 3 && isFluidContainer(stack)) {
                moved = moveItemStackTo(stack, deviceSlots, deviceSlots + 1, false);
                if (moved && container instanceof FoundryBlockEntity entity) {
                    processBucketInput(entity);
                }
            }
            if (!moved && screenKind() != 3 && container.canPlaceItem(FoundryBlockEntity.FUEL_SLOT, stack)) {
                moved = moveItemStackTo(stack, FoundryBlockEntity.FUEL_SLOT, FoundryBlockEntity.FUEL_SLOT + 1, false);
            }
            if (!moved && screenKind() != 3) {
                for (int input = 0; input < inputSlotCount(); input++) {
                    if (container.canPlaceItem(FoundryBlockEntity.inputContainerSlot(input), stack) && moveItemStackTo(stack, input, input + 1, false)) {
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved && screenKind() == 3) {
                for (int input = 0; input < inputSlotCount(); input++) {
                    if (container.canPlaceItem(FoundryBlockEntity.inputContainerSlot(input), stack) && moveItemStackTo(stack, input, input + 1, false)) {
                        moved = true;
                        if (stack.isEmpty()) break;
                    }
                }
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
