package org.hp.tinker_foundry.common;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** 共享总容量的有序多流体槽，第一层同时是默认排液目标。 */
public final class StructureFluidTank {
    /** 限制不同流体层数，避免异常存档或数据包产生无界网络列表。 */
    public static final int MAX_LAYERS = 128;
    /** 流体顺序随存档和网络同步保留。 */
    private final List<FluidStack> fluids = new ArrayList<>();

    /** 返回独立副本，外部模拟和界面不能修改真实储量。 */
    public List<FluidStack> snapshot() {
        return fluids.stream().map(FluidStack::copy).toList();
    }

    /** 从可信存档或服务器快照恢复，不因结构暂时缩小而丢弃已有流体。 */
    public void restore(List<FluidStack> values) {
        fluids.clear();
        for (FluidStack value : values) {
            if (!value.isEmpty()) {
                fill(value, Integer.MAX_VALUE, FluidAction.EXECUTE);
            }
        }
    }

    /** 返回全部流体占用的共享空间。 */
    public int amount() {
        return fluids.stream().mapToInt(FluidStack::getAmount).sum();
    }

    /** 返回当前不同流体的数量。 */
    public int size() {
        return fluids.size();
    }

    /** 读取某层，越界时返回空栈。 */
    public FluidStack get(int index) {
        return index >= 0 && index < fluids.size() ? fluids.get(index).copy() : FluidStack.EMPTY;
    }

    /** 按流体和组件匹配已有层，防止不同组件的数据被合并。 */
    public int indexOf(FluidStack stack) {
        for (int index = 0; index < fluids.size(); index++) {
            if (FluidStack.isSameFluidSameComponents(fluids.get(index), stack)) return index;
        }
        return -1;
    }

    /** 模拟或执行注入，同类合并，新类型追加，所有层共享容量。 */
    public int fill(FluidStack stack, int capacity, FluidAction action) {
        if (stack.isEmpty()) return 0;
        int index = indexOf(stack);
        if (index < 0 && fluids.size() >= MAX_LAYERS) return 0;
        int accepted = Math.min(stack.getAmount(), Math.max(0, capacity - amount()));
        if (accepted > 0 && action.execute()) {
            if (index < 0) fluids.add(stack.copyWithAmount(accepted));
            else fluids.set(index, stack.copyWithAmount(fluids.get(index).getAmount() + accepted));
        }
        return accepted;
    }

    /** 模拟或执行指定层抽取，空层立即移除。 */
    public FluidStack drain(int index, int requested, FluidAction action) {
        FluidStack stored = get(index);
        if (stored.isEmpty() || requested <= 0) return FluidStack.EMPTY;
        int drained = Math.min(requested, stored.getAmount());
        if (action.execute()) {
            if (drained == stored.getAmount()) fluids.remove(index);
            else fluids.set(index, stored.copyWithAmount(stored.getAmount() - drained));
        }
        return stored.copyWithAmount(drained);
    }

    /** 把点击的层置底，使手动取液和自动排液使用相同优先顺序。 */
    public boolean select(int index) {
        if (index <= 0 || index >= fluids.size()) return false;
        fluids.add(0, fluids.remove(index));
        return true;
    }
}
