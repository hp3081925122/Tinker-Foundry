package org.hp.tinker_foundry.common;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 排液口和流体导管无独立缓存，所有读写都指向仍然有效的炉体。 */
public final class FoundryPortFluidHandler implements IFluidHandler {
    private final FoundryBlockEntity port;
    public FoundryPortFluidHandler(FoundryBlockEntity port) { this.port = port; }
    /** 无有效控制器时暴露零槽。 */
    @Override public int getTanks() {
        FoundryBlockEntity entity = port.attachedController();
        return entity == null ? 0 : entity.getTanks();
    }
    /** 返回控制器有序流体列表。 */
    @Override public FluidStack getFluidInTank(int tank) {
        FoundryBlockEntity entity = port.attachedController();
        return entity == null ? FluidStack.EMPTY : entity.getFluidInTank(tank);
    }
    /** 各层共享控制器总容量。 */
    @Override public int getTankCapacity(int tank) {
        FoundryBlockEntity entity = port.attachedController();
        return entity == null ? 0 : entity.getTankCapacity(tank);
    }
    /** 委托控制器判断可接收流体。 */
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        FoundryBlockEntity entity = port.attachedController();
        return entity != null && entity.isFluidValid(tank, stack);
    }
    /** 注入不会在失效附件内暂存。 */
    @Override public int fill(FluidStack stack, FluidAction action) {
        FoundryBlockEntity entity = port.attachedController();
        return entity == null ? 0 : entity.fill(stack, action);
    }
    /** 指定流体抽取保留完整组件。 */
    @Override public FluidStack drain(FluidStack stack, FluidAction action) {
        FoundryBlockEntity entity = port.attachedController();
        return entity == null ? FluidStack.EMPTY : entity.drain(stack, action);
    }
    /** 无指定流体时取控制器底层。 */
    @Override public FluidStack drain(int amount, FluidAction action) {
        FoundryBlockEntity entity = port.attachedController();
        return entity == null ? FluidStack.EMPTY : entity.drain(amount, action);
    }
}
