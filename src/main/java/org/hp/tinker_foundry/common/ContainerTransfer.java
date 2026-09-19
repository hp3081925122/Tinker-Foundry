package org.hp.tinker_foundry.common;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.menu.FoundryMenu.TransferDirection;

/** 在独立液体副本和单个物品副本上计算整个传输，确认输出可接收后才提交。 */
public final class ContainerTransfer implements IFluidHandler {
    private final FoundryBlockEntity entity;
    private final int tank;
    private final StructureFluidTank copy = new StructureFluidTank();
    private final List<Operation> operations = new ArrayList<>();
    private final FluidStack selected;
    private final int capacity;
    private final boolean multiple;
    private ItemStack result;

    /** 保存操作顺序，自动模式可以先倒空再用底层流体装满。 */
    private record Operation(boolean fill, FluidStack stack) { }

    /** 多层设备复制全部流体，普通设备仅复制目标槽。 */
    private ContainerTransfer(FoundryBlockEntity entity, int tank) {
        this.entity = entity;
        this.tank = tank;
        multiple = entity.isStructureController();
        selected = entity.getFluidInTank(tank);
        capacity = entity.getTankCapacity(tank);
        copy.restore(multiple ? entity.structureFluidLayers() : List.of(selected));
    }

    /** 使用标准物品流体能力，不再按桶、便携罐和具体模组逐个判断。 */
    public static ContainerTransfer plan(FoundryBlockEntity entity, ItemStack input, int tank, TransferDirection direction) {
        if (input.isEmpty()) return null;
        ContainerTransfer transaction = new ContainerTransfer(entity, tank);
        ItemStack unit = input.copyWithCount(1);
        boolean moved = false;
        if (direction != TransferDirection.FILL_ITEM) {
            var emptied = FluidUtil.tryEmptyContainer(unit, transaction, Integer.MAX_VALUE, null, true);
            if (emptied.isSuccess()) {
                unit = emptied.getResult();
                moved = true;
            }
        }
        if (!unit.isEmpty() && direction != TransferDirection.EMPTY_ITEM) {
            var filled = FluidUtil.tryFillContainer(unit, transaction, Integer.MAX_VALUE, null, true);
            if (filled.isSuccess()) {
                unit = filled.getResult();
                moved = true;
            }
        }
        if (!moved) return null;
        transaction.result = unit;
        return transaction;
    }

    /** 允许调用者先检查输出物品和组件是否可堆叠。 */
    public ItemStack result() { return result.copy(); }

    /** 仅在服务端同一次菜单操作内提交，不存储跨刻的过期事务。 */
    public void commit() {
        for (Operation operation : operations) {
            if (operation.fill()) entity.fillTank(tank, operation.stack(), FluidAction.EXECUTE);
            else if (multiple) entity.drain(operation.stack(), FluidAction.EXECUTE);
            else entity.drainTank(tank, operation.stack().getAmount(), FluidAction.EXECUTE);
        }
    }

    /** 多层副本至少提供一个可注入空槽。 */
    @Override public int getTanks() { return Math.max(1, copy.size()); }
    /** 标准能力读取副本。 */
    @Override public FluidStack getFluidInTank(int index) { return copy.get(index); }
    /** 所有层共享设备容量。 */
    @Override public int getTankCapacity(int index) { return capacity; }
    /** 普通设备还需遵守指定输入槽的限制。 */
    @Override public boolean isFluidValid(int index, FluidStack stack) {
        return !stack.isEmpty() && entity.isFluidValid(tank, stack)
            && (multiple || copy.size() == 0 || FluidStack.isSameFluidSameComponents(copy.get(0), stack));
    }
    /** 执行模式只修改副本并记录，真实设备在输出确认前保持不变。 */
    @Override public int fill(FluidStack stack, FluidAction action) {
        if (!isFluidValid(0, stack)) return 0;
        int amount = copy.fill(stack, capacity, action);
        if (amount > 0 && action.execute()) operations.add(new Operation(true, stack.copyWithAmount(amount)));
        return amount;
    }
    /** 组件参与类型判断，防止药水等相同流体的不同内容串液。 */
    @Override public FluidStack drain(FluidStack stack, FluidAction action) {
        FluidStack drained = copy.drain(copy.indexOf(stack), stack.getAmount(), action);
        if (!drained.isEmpty() && action.execute()) operations.add(new Operation(false, drained.copy()));
        return drained;
    }
    /** 优先抽取点击层；空炉倒入后自动模式可以从新出现的底层装填。 */
    @Override public FluidStack drain(int amount, FluidAction action) {
        int index = selected.isEmpty() ? 0 : copy.indexOf(selected);
        FluidStack offered = copy.get(index);
        return offered.isEmpty() ? FluidStack.EMPTY : drain(offered.copyWithAmount(Math.min(amount, offered.getAmount())), action);
    }
}
