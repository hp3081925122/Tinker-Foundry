package org.hp.tinker_foundry.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.network.FoundryStatePayload;
import org.hp.tinker_foundry.network.FoundryStructureErrorPayload;

/** 客户端网络消息处理，避免服务端加载 Minecraft 客户端类。 */
public final class FoundryNetworkingClient {
    /** 在客户端主线程应用设备状态。 */
    public static void handleState(FoundryStatePayload payload) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.level().getBlockEntity(payload.pos()) instanceof FoundryBlockEntity entity)) {
            return;
        }
        entity.applyStatePayload(payload);
    }

    /** 在客户端主线程应用结构错误提示。 */
    public static void handleStructureError(FoundryStructureErrorPayload payload) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.level().getBlockEntity(payload.controllerPos()) instanceof FoundryBlockEntity entity)) {
            return;
        }
        entity.applyStructureErrorPayload(payload);
    }

    private FoundryNetworkingClient() {
    }
}
