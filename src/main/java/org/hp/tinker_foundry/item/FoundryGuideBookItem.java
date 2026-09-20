package org.hp.tinker_foundry.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 使用独立客户端页面打开冶炼指南，避免退化为原版纯文字成书。 */
public final class FoundryGuideBookItem extends Item {
    /** 构造不可堆叠的冶炼指南物品。 */
    public FoundryGuideBookItem(Item.Properties properties) {
        super(properties);
    }

    /** 在客户端打开目录式指南，在服务端只完成物品使用统计。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
