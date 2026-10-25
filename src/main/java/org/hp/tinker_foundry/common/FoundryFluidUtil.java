package org.hp.tinker_foundry.common;

import net.minecraftforge.fluids.FluidStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

/** 统一封装 Forge 1.20.1 FluidStack 的复制和完整相等判断。 */
public final class FoundryFluidUtil {
    /** 按新数量复制流体，并保留流体 NBT。 */
    public static FluidStack copy(FluidStack stack, int amount) {
        return stack.isEmpty() ? FluidStack.EMPTY : new FluidStack(stack, amount);
    }

    /** 将 Forge 流体写入 1.20.1 方块实体使用的 NBT 标签。 */
    public static CompoundTag toTag(FluidStack stack) {
        return stack.writeToNBT(new CompoundTag());
    }

    /** 判断流体类型与附加 NBT 都相同。 */
    public static boolean same(FluidStack first, FluidStack second) {
        return first.isFluidStackIdentical(second);
    }

    /** 判断流体类型相同但不要求附加 NBT 相同。 */
    public static boolean sameFluid(FluidStack first, FluidStack second) {
        return first.isFluidEqual(second);
    }

    /** 判断 Forge 流体栈是否包含指定流体类型。 */
    public static boolean is(FluidStack stack, Fluid fluid) {
        return !stack.isEmpty() && stack.getFluid() == fluid;
    }

    private FoundryFluidUtil() {
    }
}
