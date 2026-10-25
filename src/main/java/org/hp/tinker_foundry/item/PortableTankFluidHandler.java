package org.hp.tinker_foundry.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.hp.tinker_foundry.registry.TFDataComponents;

/** 使用 1.20.1 Forge 的 ItemStack NBT 保存流体，不注册目标版本不存在的数据组件。 */
public final class PortableTankFluidHandler implements IFluidHandlerItem {
    private final ItemStack container;
    private final int capacity;
    private final boolean allowFuel;

    /** 创建指定物品堆叠的流体能力。 */
    public PortableTankFluidHandler(ItemStack container, int capacity) {
        this(container, capacity, false);
    }

    /** 创建可选择是否接受燃料流体的物品能力。 */
    public PortableTankFluidHandler(ItemStack container, int capacity, boolean allowFuel) {
        this.container = container;
        this.capacity = capacity;
        this.allowFuel = allowFuel;
    }

    /** 读取物品当前流体。 */
    private FluidStack fluid() {
        if (!container.hasTag()) {
            return FluidStack.EMPTY;
        }
        CompoundTag data = container.getTagElement(TFDataComponents.FLUID_TAG);
        return data == null ? FluidStack.EMPTY : FluidStack.loadFluidStackFromNBT(data);
    }

    /** 保存物品当前流体，保持 FluidStack 的附加标签和旧物品持久化语义。 */
    private void setFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            container.removeTagKey(TFDataComponents.FLUID_TAG);
            return;
        }
        CompoundTag data = new CompoundTag();
        stack.writeToNBT(data);
        container.getOrCreateTag().put(TFDataComponents.FLUID_TAG, data);
    }

    /** 返回单槽能力。 */
    @Override
    public int getTanks() {
        return 1;
    }

    /** 返回流体副本。 */
    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? fluid().copy() : FluidStack.EMPTY;
    }

    /** 返回固定容量。 */
    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? capacity : 0;
    }

    /** 便携罐接受所有非空流体。 */
    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && !stack.isEmpty();
    }

    /** 注入流体并保存到物品组件。 */
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        FluidStack current = fluid();
        if (!isFluidValid(0, resource) || !current.isEmpty() && !current.isFluidEqual(resource)) return 0;
        int amount = Math.min(resource.getAmount(), capacity - current.getAmount());
        if (amount > 0 && action.execute()) {
            setFluid(current.isEmpty() ? new FluidStack(resource, amount) : new FluidStack(current, current.getAmount() + amount));
        }
        return amount;
    }

    /** 按流体类型抽取。 */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack current = fluid();
        return resource.isEmpty() || current.isEmpty() || !current.isFluidEqual(resource) ? FluidStack.EMPTY : drain(resource.getAmount(), action);
    }

    /** 按数量抽取。 */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack current = fluid();
        if (current.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
        FluidStack result = new FluidStack(current, Math.min(maxDrain, current.getAmount()));
        if (action.execute()) {
            current.shrink(result.getAmount());
            setFluid(current);
        }
        return result;
    }

    /** 返回保存数据的物品容器。 */
    @Override
    public ItemStack getContainer() {
        return container;
    }
}
