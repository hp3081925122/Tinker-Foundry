package org.hp.tinker_foundry.recipe;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** 为 Forge 1.20.1 Recipe 提供流体列表和可选模具物品的 Container 输入。 */
public final class FluidRecipeInput implements Container {
    private final List<FluidStack> fluids;
    private ItemStack item;
    private final boolean requireMold;

    /** 创建服务端权威的流体输入副本。 */
    public FluidRecipeInput(List<FluidStack> fluids, ItemStack item, boolean requireMold) {
        this.fluids = fluids.stream().map(FluidStack::copy).toList();
        this.item = item.copy();
        this.requireMold = requireMold;
    }

    /** 创建不含物品模具的流体输入。 */
    public FluidRecipeInput(List<FluidStack> fluids) {
        this(fluids, ItemStack.EMPTY, false);
    }

    /** 创建带物品但不强制使用铸模的流体输入。 */
    public FluidRecipeInput(List<FluidStack> fluids, ItemStack item) {
        this(fluids, item, false);
    }

    /** 返回流体快照。 */
    public List<FluidStack> fluids() {
        return fluids;
    }

    /** 返回是否需要模具配方。 */
    public boolean requireMold() {
        return requireMold;
    }

    /** Container 的逻辑物品槽数量。 */
    @Override
    public int getContainerSize() {
        return item.isEmpty() ? 0 : 1;
    }

    /** 返回模具槽内容副本。 */
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? item : ItemStack.EMPTY;
    }

    /** 读取并移除少量模具。 */
    @Override
    public ItemStack removeItem(int index, int count) {
        if (index != 0 || item.isEmpty()) return ItemStack.EMPTY;
        return item.split(count);
    }

    /** 读取并清空模具槽。 */
    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index != 0) return ItemStack.EMPTY;
        ItemStack result = item;
        item = ItemStack.EMPTY;
        return result;
    }

    /** 配方匹配输入不可被外部改写。 */
    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0) item = stack.copy();
    }

    /** 流体输入只读，容器变化不需要回写方块实体。 */
    @Override
    public void setChanged() {
    }

    /** 配方临时输入始终有效。 */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 清除临时模具引用。 */
    @Override
    public void clearContent() {
        item = ItemStack.EMPTY;
    }

    /** 流体或模具任意一个存在时输入才非空。 */
    @Override
    public boolean isEmpty() {
        return fluids.isEmpty() && item.isEmpty();
    }
}
