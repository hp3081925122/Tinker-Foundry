package org.hp.tinker_foundry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.multiblock.StructureErrorReason;

/** 向附近客户端同步控制器最近一次结构错误位置和提示类型。 */
public record FoundryStructureErrorPayload(BlockPos controllerPos, BlockPos errorPos, StructureErrorReason reason) {
    /** 从 Forge FriendlyByteBuf 读取结构错误载荷。 */
    public static FoundryStructureErrorPayload decode(FriendlyByteBuf buffer) {
        BlockPos controllerPos = buffer.readBlockPos();
        BlockPos errorPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
        StructureErrorReason reason = StructureErrorReason.fromId(buffer.readVarInt());
        return new FoundryStructureErrorPayload(controllerPos, errorPos, reason);
    }

    /** 向 Forge FriendlyByteBuf 写入结构错误载荷。 */
    public static void encode(FoundryStructureErrorPayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.controllerPos);
        buffer.writeBoolean(payload.errorPos != null);
        if (payload.errorPos != null) {
            buffer.writeBlockPos(payload.errorPos);
        }
        buffer.writeVarInt(payload.reason.ordinal());
    }

    /** 从控制器构造结构错误载荷。 */
    public static FoundryStructureErrorPayload from(FoundryBlockEntity entity) {
        return new FoundryStructureErrorPayload(entity.getBlockPos(), entity.structureErrorPos(), entity.structureErrorReason());
    }
}
