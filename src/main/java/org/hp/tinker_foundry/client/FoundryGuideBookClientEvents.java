package org.hp.tinker_foundry.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFItems;

/** 在客户端拦截指南右键并打开目录式页面。 */
@EventBusSubscriber(modid = TinkerFoundry.MOD_ID, value = Dist.CLIENT)
public final class FoundryGuideBookClientEvents {
    // 只在显式开发巡检启动时自动打开指南，不影响玩家正常启动。
    private static boolean qaOpened;

    /** 在资源加载完成后的标题界面启动图文巡检。 */
    @SubscribeEvent
    public static void inspectGuide(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (!qaOpened && "1".equals(System.getenv("TINKER_FOUNDRY_GUIDE_QA"))
            && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen && client.getOverlay() == null) {
            qaOpened = true;
            client.setScreen(new FoundryGuideScreen(true));
        }
    }
    /** 指南物品被右键时打开客户端书本界面。 */
    @SubscribeEvent
    public static void openGuideBook(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        // 单人游戏的服务端也发布此事件，物理客户端注解不能代替逻辑端判断。
        if (event.getLevel().isClientSide() && stack.is(TFItems.GUIDE_BOOK.get())) {
            Minecraft.getInstance().setScreen(new FoundryGuideScreen());
        }
    }

    private FoundryGuideBookClientEvents() {
    }
}
