package org.hp.tinker_foundry.item;

import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** 独立实现冶炼方块物品的可选 tooltip 机制。 */
public class FoundryTooltipBlockItem extends BlockItem {
    /** 创建一个支持物品描述键后缀 tooltip 的方块物品。 */
    public FoundryTooltipBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /** 在原版方块物品名称后追加对应的多行说明。 */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addOptionalTooltip(stack, tooltip);
    }

    /** 按匠魂约定读取当前物品描述键后的 tooltip 语言键。 */
    protected static void addOptionalTooltip(ItemStack stack, List<Component> tooltip) {
        String key = stack.getDescriptionId() + ".tooltip";
        if (!Language.getInstance().has(key)) {
            return;
        }
        for (String line : Component.translatable(key).getString().split("\\n", -1)) {
            tooltip.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
