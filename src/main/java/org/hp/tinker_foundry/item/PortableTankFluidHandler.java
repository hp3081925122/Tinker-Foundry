package org.hp.tinker_foundry.item;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.registry.TFDataComponents;

/** 使用物品数据组件保存流体，不依赖旧版物品 NBT API。 */
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
        CustomData data = container.getOrDefault(TFDataComponents.FLUID.get(), CustomData.EMPTY);
        if (data.isEmpty()) {
            return FluidStack.EMPTY;
        }
        DataResult<FluidStack> result = FluidStack.CODEC.parse(NbtOps.INSTANCE, data.copyTag());
        return result.resultOrPartial(error -> TinkerFoundry.LOGGER.warn("Failed to decode portable tank fluid: {}", error))
            .orElse(FluidStack.EMPTY).copy();
    }

    /** 保存物品当前流体。 */
    private void setFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            container.remove(TFDataComponents.FLUID.get());
            return;
        }
        DataResult<Tag> result = FluidStack.CODEC.encodeStart(NbtOps.INSTANCE, stack.copy());
        result.resultOrPartial(error -> TinkerFoundry.LOGGER.warn("Failed to encode portable tank fluid: {}", error))
            .ifPresent(tag -> {
                // FluidStack.CODEC 的成功结果必须是复合标签，避免异常数据写入物品组件。
                if (tag instanceof CompoundTag compound) container.set(TFDataComponents.FLUID.get(), CustomData.of(compound));
            });
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
        if (!isFluidValid(0, resource) || !current.isEmpty() && !FluidStack.isSameFluidSameComponents(current, resource)) return 0;
        int amount = Math.min(resource.getAmount(), capacity - current.getAmount());
        if (amount > 0 && action.execute()) setFluid(current.isEmpty() ? resource.copyWithAmount(amount) : current.copyWithAmount(current.getAmount() + amount));
        return amount;
    }

    /** 按流体类型抽取。 */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack current = fluid();
        return resource.isEmpty() || current.isEmpty() || !FluidStack.isSameFluidSameComponents(current, resource) ? FluidStack.EMPTY : drain(resource.getAmount(), action);
    }

    /** 按数量抽取。 */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack current = fluid();
        if (current.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
        FluidStack result = current.copyWithAmount(Math.min(maxDrain, current.getAmount()));
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
