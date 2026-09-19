package org.hp.tinker_foundry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.multiblock.StructureErrorReason;

/** 向附近客户端同步控制器最近一次结构错误位置和提示类型。 */
public record FoundryStructureErrorPayload(BlockPos controllerPos, BlockPos errorPos, StructureErrorReason reason)
    implements CustomPacketPayload {
    /** 独立结构错误载荷类型。 */
    public static final Type<FoundryStructureErrorPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "foundry_structure_error")
    );

    /** 1.21.1 RegistryFriendlyByteBuf 使用的结构错误流编码器。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, FoundryStructureErrorPayload> STREAM_CODEC = StreamCodec.of(
        FoundryStructureErrorPayload::write, FoundryStructureErrorPayload::read
    );

    /** 从网络读取结构错误载荷。 */
    private static FoundryStructureErrorPayload read(RegistryFriendlyByteBuf buffer) {
        BlockPos controllerPos = BlockPos.STREAM_CODEC.decode(buffer);
        BlockPos errorPos = buffer.readBoolean() ? BlockPos.STREAM_CODEC.decode(buffer) : null;
        StructureErrorReason reason = StructureErrorReason.fromId(buffer.readVarInt());
        return new FoundryStructureErrorPayload(controllerPos, errorPos, reason);
    }

    /** 向网络写入结构错误载荷。 */
    private static void write(RegistryFriendlyByteBuf buffer, FoundryStructureErrorPayload payload) {
        BlockPos.STREAM_CODEC.encode(buffer, payload.controllerPos);
        buffer.writeBoolean(payload.errorPos != null);
        if (payload.errorPos != null) {
            BlockPos.STREAM_CODEC.encode(buffer, payload.errorPos);
        }
        buffer.writeVarInt(payload.reason.ordinal());
    }

    /** 从控制器构造结构错误载荷。 */
    public static FoundryStructureErrorPayload from(FoundryBlockEntity entity) {
        return new FoundryStructureErrorPayload(entity.getBlockPos(), entity.structureErrorPos(), entity.structureErrorReason());
    }

    /** 返回独立网络载荷类型。 */
    @Override
    public Type<FoundryStructureErrorPayload> type() {
        return TYPE;
    }
}
