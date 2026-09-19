package org.hp.tinker_foundry.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

/** 可放置、可携带流体并在破坏后保留内容的专用储液罐物品。 */
public final class FoundryTankItem extends FoundryTooltipBlockItem {
    private final int capacity;
    private final boolean allowFuel;

    /** 创建一个与指定方块绑定的储液罐物品。 */
    public FoundryTankItem(Block block, int capacity, boolean allowFuel, Item.Properties properties) {
        super(block, properties.stacksTo(1));
        this.capacity = capacity;
        this.allowFuel = allowFuel;
    }

    /** 返回储液罐单个物品的容量。 */
    public int capacity() {
        return capacity;
    }

    /** 返回该物品是否允许保存燃料流体。 */
    public boolean allowsFuel() {
        return allowFuel;
    }

    /** 从物品组件读取当前流体。 */
    public FluidStack getFluid(ItemStack stack) {
        return new PortableTankFluidHandler(stack, capacity, allowFuel).getFluidInTank(0);
    }

    /** 把流体写回物品组件，非法流体不会被写入。 */
    public static void setFluid(ItemStack stack, FluidStack fluid) {
        if (!(stack.getItem() instanceof FoundryTankItem tankItem)) {
            return;
        }
        PortableTankFluidHandler handler = new PortableTankFluidHandler(stack, tankItem.capacity, tankItem.allowFuel);
        handler.drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (!fluid.isEmpty()) {
            handler.fill(fluid, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** 显示罐内流体和当前储量，便于创造栏和普通物品栏识别填充状态。 */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.tinker_foundry.empty_tank").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(fluid.getDisplayName().copy().withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.tinker_foundry.tank_amount", fluid.getAmount(), capacity)
                .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

}
