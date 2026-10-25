package org.hp.tinker_foundry.item;

import java.util.function.Consumer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** 独立实现冶炼方块物品的可选 tooltip 机制。 */
public class FoundryTooltipBlockItem extends BlockItem {
    /** 创建一个支持物品描述键后缀 tooltip 的方块物品。 */
    public FoundryTooltipBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /** 在原版方块物品名称后追加对应的多行说明。 */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        addOptionalTooltip(stack, tooltip);
    }

    /** 按匠魂约定读取当前物品描述键后的 tooltip 语言键。 */
    protected static void addOptionalTooltip(ItemStack stack, Consumer<Component> tooltip) {
        String key = stack.getItem().getDescriptionId() + ".tooltip";
        if (!Language.getInstance().has(key)) {
            return;
        }
        for (String line : Component.translatable(key).getString().split("\\n", -1)) {
            tooltip.accept(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
