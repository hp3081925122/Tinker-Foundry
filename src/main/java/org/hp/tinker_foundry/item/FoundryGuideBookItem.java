package org.hp.tinker_foundry.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.book.FoundryGuideBook;

/** 为自定义冶炼指南补充原版成书的服务端打开流程。 */
public final class FoundryGuideBookItem extends WrittenBookItem {
    /** 构造不可堆叠的冶炼指南物品。 */
    public FoundryGuideBookItem(Item.Properties properties) {
        super(properties);
    }

    /** 处理创造物品栏取得的空指南，并发送客户端打开书本界面的数据包。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!stack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
                ItemStack generatedBook = FoundryGuideBook.createBook(serverPlayer);
                stack.set(DataComponents.WRITTEN_BOOK_CONTENT, generatedBook.get(DataComponents.WRITTEN_BOOK_CONTENT));
                serverPlayer.containerMenu.broadcastChanges();
                TinkerFoundry.LOGGER.debug("Initialized the foundry guide book content for {}", serverPlayer.getGameProfile().getName());
            }
            serverPlayer.connection.send(new ClientboundOpenBookPacket(hand));
            TinkerFoundry.LOGGER.debug("Sent the foundry guide book open packet for {}", serverPlayer.getGameProfile().getName());
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
