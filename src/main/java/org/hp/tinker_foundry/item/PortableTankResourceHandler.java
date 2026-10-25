package org.hp.tinker_foundry.item;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** 通过 26.1.2 的 ItemAccess 正确替换玩家手中的便携储罐内容。 */
public final class PortableTankResourceHandler extends ItemAccessResourceHandler<FluidResource> {
    /** 单个储罐物品的容量。 */
    private final int capacity;
    /** 保留燃料储罐类型信息，供旧组件处理器使用相同规则。 */
    private final boolean allowFuel;

    /** 创建绑定到实际物品位置的单槽流体处理器。 */
    public PortableTankResourceHandler(ItemAccess itemAccess, int capacity, boolean allowFuel) {
        super(itemAccess, 1);
        this.capacity = capacity;
        this.allowFuel = allowFuel;
    }

    /** 创建不带燃料标记的普通便携罐处理器。 */
    public PortableTankResourceHandler(ItemAccess itemAccess, int capacity) {
        this(itemAccess, capacity, false);
    }

    /** 从物品组件读取当前流体类型。 */
    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        Objects.checkIndex(index, size());
        FluidStack fluid = readFluid(accessResource);
        return fluid.isEmpty() ? FluidResource.EMPTY : FluidResource.of(fluid);
    }

    /** 从物品组件读取当前流体数量。 */
    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        Objects.checkIndex(index, size());
        return readFluid(accessResource).getAmount();
    }

    /** 在物品副本上写入新流体，并把新物品资源交回 ItemAccess 统一交换。 */
    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        Objects.checkIndex(index, size());
        ItemStack updated = accessResource.toStack(1);
        PortableTankFluidHandler handler = new PortableTankFluidHandler(updated, capacity, allowFuel);
        handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        if (newAmount > 0) {
            int inserted = handler.fill(newResource.toStack(newAmount), IFluidHandler.FluidAction.EXECUTE);
            if (inserted != newAmount) {
                return ItemResource.EMPTY;
            }
        }
        return ItemResource.of(updated);
    }

    /** 所有非空流体均可由便携储罐保存。 */
    @Override
    public boolean isValid(int index, FluidResource resource) {
        return index == 0 && !resource.isEmpty();
    }

    /** 返回单个便携储罐的固定容量。 */
    @Override
    protected int getCapacity(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return capacity;
    }

    /** 从一个物品资源副本中读取完整流体组件。 */
    private FluidStack readFluid(ItemResource accessResource) {
        ItemStack stack = accessResource.toStack(1);
        return new PortableTankFluidHandler(stack, capacity, allowFuel).getFluidInTank(0);
    }
}
