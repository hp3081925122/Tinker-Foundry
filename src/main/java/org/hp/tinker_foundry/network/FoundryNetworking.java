package org.hp.tinker_foundry.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.menu.FoundryMenu;

/** 注册并发送冶炼设备的独立客户端状态网络层。 */
public final class FoundryNetworking {
    /** 当前模组网络协议版本。 */
    private static final String PROTOCOL_VERSION = "6";

    /** 在 NeoForge 模组总线上注册客户端状态载荷。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToClient(FoundryStatePayload.TYPE, FoundryStatePayload.STREAM_CODEC,
            FoundryNetworking::handleState);
        event.registrar(PROTOCOL_VERSION).playToClient(FoundryStructureErrorPayload.TYPE,
            FoundryStructureErrorPayload.STREAM_CODEC, FoundryNetworking::handleStructureError);
    }

    /** 在主线程应用服务端发送的状态快照。 */
    private static void handleState(FoundryStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || !(player.level().getBlockEntity(payload.pos()) instanceof FoundryBlockEntity entity)) {
                return;
            }
            entity.applyStatePayload(payload);
        });
    }

    /** 在客户端主线程应用服务端同步的结构错误位置。 */
    private static void handleStructureError(FoundryStructureErrorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || !(player.level().getBlockEntity(payload.controllerPos()) instanceof FoundryBlockEntity entity)) {
                return;
            }
            entity.applyStructureErrorPayload(payload);
        });
    }

    /** 只向正在查看该设备菜单的玩家发送状态，避免向整个区块广播冶炼细节。 */
    public static void sync(FoundryBlockEntity entity) {
        if (!(entity.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        FoundryStatePayload payload = null;
        for (ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof FoundryMenu menu && menu.blockEntity() == entity) {
                // 无人查看时不构建热量数组；多名查看者复用同一份只读快照。
                if (payload == null) payload = FoundryStatePayload.from(entity);
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /** 向控制器附近玩家广播结构错误，保证未打开 GUI 时也能看到红框和文字。 */
    public static void syncStructureError(FoundryBlockEntity entity) {
        if (!(entity.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        FoundryStructureErrorPayload payload = FoundryStructureErrorPayload.from(entity);
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(entity.getBlockPos()) <= 64 * 64) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    private FoundryNetworking() {
    }
}
