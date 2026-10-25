package org.hp.tinker_foundry.network;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.menu.FoundryMenu;

/** 注册并发送冶炼设备的独立客户端状态网络层。 */
public final class FoundryNetworking {
    /** 当前模组网络协议版本；只保证同一迁移版本内部语义，不保证跨 Minecraft 版本字节互通。 */
    private static final String PROTOCOL_VERSION = "6";
    /** Forge SimpleChannel 使用的单一模组通道。 */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(TinkerFoundry.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    /** 注册包的稳定顺序。 */
    private static int packetId;
    /** 避免重复注册同一通道。 */
    private static boolean registered;

    /** 注册 Forge SimpleChannel 的两个服务端到客户端消息。 */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(packetId++, FoundryStatePayload.class, FoundryStatePayload::encode,
            FoundryStatePayload::decode, FoundryNetworking::handleState);
        CHANNEL.registerMessage(packetId++, FoundryStructureErrorPayload.class, FoundryStructureErrorPayload::encode,
            FoundryStructureErrorPayload::decode, FoundryNetworking::handleStructureError);
    }

    /** 在客户端主线程应用服务端发送的状态快照。 */
    private static void handleState(FoundryStatePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // Forge 服务端不能直接加载客户端处理类，使用 DistExecutor 保持专用服务器类路径干净。
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> org.hp.tinker_foundry.client.FoundryNetworkingClient.handleState(payload)));
        context.setPacketHandled(true);
    }

    /** 在客户端主线程应用服务端同步的结构错误位置。 */
    private static void handleStructureError(FoundryStructureErrorPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // Forge 服务端不能直接加载客户端处理类，使用 DistExecutor 保持专用服务器类路径干净。
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> org.hp.tinker_foundry.client.FoundryNetworkingClient.handleStructureError(payload)));
        context.setPacketHandled(true);
    }

    /** 只向正在查看该设备菜单的玩家发送状态，避免向整个区块广播冶炼细节。 */
    public static void sync(FoundryBlockEntity entity) {
        if (!(entity.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        FoundryStatePayload payload = null;
        for (ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof FoundryMenu menu && menu.blockEntity() == entity) {
                if (payload == null) {
                    payload = FoundryStatePayload.from(entity);
                }
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
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
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
            }
        }
    }

    private FoundryNetworking() {
    }
}
