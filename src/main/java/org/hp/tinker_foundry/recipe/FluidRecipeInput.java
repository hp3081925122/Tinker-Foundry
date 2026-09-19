package org.hp.tinker_foundry.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** 为熔炼、合金和浇注配方提供服务端权威的流体输入。 */
public record FluidRecipeInput(List<FluidStack> fluids, ItemStack item) implements RecipeInput {
    /** 规范化输入副本，避免配方匹配过程修改菜单或方块实体数据。 */
    public FluidRecipeInput {
        fluids = fluids.stream().map(FluidStack::copy).toList();
        item = item.copy();
    }

    /** 创建不含物品模具的流体输入。 */
    public FluidRecipeInput(List<FluidStack> fluids) {
        this(fluids, ItemStack.EMPTY);
    }

    /** RecipeInput 的兼容槽位，流体不通过物品槽伪造。 */
    @Override
    public ItemStack getItem(int index) {
        if (index != 0 || item.isEmpty()) {
            throw new IllegalArgumentException("Foundry fluid input has no item at index " + index);
        }
        return item;
    }

    /** 模具存在时提供一个物品槽。 */
    @Override
    public int size() {
        return item.isEmpty() ? 0 : 1;
    }

    /** 流体或模具任意一个存在时输入才非空。 */
    @Override
    public boolean isEmpty() {
        return fluids.isEmpty() && item.isEmpty();
    }
}
