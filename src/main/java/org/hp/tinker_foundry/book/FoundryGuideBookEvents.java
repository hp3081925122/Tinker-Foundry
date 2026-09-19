package org.hp.tinker_foundry.book;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;

/** 处理教程书的首次登录赠送。 */
public final class FoundryGuideBookEvents {
    /** 玩家完成登录后在服务端赠送一次教程书。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FoundryGuideBook.giveOnce(player);
        }
    }

    private FoundryGuideBookEvents() {
    }
}
