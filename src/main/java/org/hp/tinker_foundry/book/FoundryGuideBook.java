package org.hp.tinker_foundry.book;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFItems;

/** 根据数据包章节文件构造只包含冶炼功能的教程书。 */
public final class FoundryGuideBook {
    /** 防止玩家数据键与其他模组或未来版本发生冲突。 */
    public static final String GIFTED_DATA_KEY = "tinker_foundry_guide_book_gifted";

    /** 创建仅包含物品身份的指南，具体页面由客户端目录界面绘制。 */
    public static ItemStack createBook(ServerPlayer player) {
        ItemStack book = new ItemStack(TFItems.GUIDE_BOOK.get());
        return book;
    }

    /** 仅允许首次登录赠送，之后由 NeoForgeData 持久保存状态。 */
    public static void giveOnce(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(GIFTED_DATA_KEY)) {
            return;
        }
        ItemStack book = createBook(player);
        if (!player.addItem(book)) {
            player.drop(book, false);
        }
        player.getPersistentData().putBoolean(GIFTED_DATA_KEY, true);
        TinkerFoundry.LOGGER.debug("Granted the foundry guide book to {}", player.getGameProfile().getName());
    }

    private FoundryGuideBook() {
    }
}
