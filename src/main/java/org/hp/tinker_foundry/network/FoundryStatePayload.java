package org.hp.tinker_foundry.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 向已打开界面的客户端发送流体、进度和结构状态快照。 */
public record FoundryStatePayload(BlockPos pos, FluidStack fluid, FluidStack fuelFluid, List<FluidStack> alloyInputs, List<FluidStack> structureFluids,
                                  int progress, int processTime, int structureCapacity, boolean structureValid)
    implements CustomPacketPayload {
    /** 独立模组的客户端状态载荷类型。 */
    public static final Type<FoundryStatePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "foundry_state")
    );

    /** 1.21.1 RegistryFriendlyByteBuf 使用的状态流编码器。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, FoundryStatePayload> STREAM_CODEC = StreamCodec.of(
        FoundryStatePayload::write, FoundryStatePayload::read
    );

    /** 从网络读取一个设备状态快照。 */
    private static FoundryStatePayload read(RegistryFriendlyByteBuf buffer) {
        BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
        FluidStack fluid = readFluid(buffer);
        FluidStack fuelFluid = readFluid(buffer);
        int inputCount = buffer.readVarInt();
        if (inputCount < 0 || inputCount > FoundryBlockEntity.MAX_ALLOY_INPUTS) {
            throw new IllegalArgumentException("Invalid alloy input count: " + inputCount);
        }
        List<FluidStack> alloyInputs = new ArrayList<>(Math.min(inputCount, FoundryBlockEntity.MAX_ALLOY_INPUTS));
        for (int index = 0; index < inputCount; index++) {
            FluidStack input = readFluid(buffer);
            if (index < FoundryBlockEntity.MAX_ALLOY_INPUTS) {
                alloyInputs.add(input);
            }
        }
        // 有序多流体列表使用独立有界载荷，不通过单个注册表编号重建而丢失组件。
        int layers = buffer.readVarInt();
        if (layers < 0 || layers > org.hp.tinker_foundry.common.StructureFluidTank.MAX_LAYERS) throw new IllegalArgumentException("Invalid structure fluid count: " + layers);
        List<FluidStack> structureFluids = new ArrayList<>(layers);
        for (int index = 0; index < layers; index++) structureFluids.add(readFluid(buffer));
        return new FoundryStatePayload(pos, fluid, fuelFluid, alloyInputs, structureFluids, buffer.readVarInt(), buffer.readVarInt(),
            buffer.readVarInt(), buffer.readBoolean());
    }

    /** 向网络写入一个设备状态快照。 */
    private static void write(RegistryFriendlyByteBuf buffer, FoundryStatePayload payload) {
        BlockPos.STREAM_CODEC.encode(buffer, payload.pos);
        writeFluid(buffer, payload.fluid);
        writeFluid(buffer, payload.fuelFluid);
        buffer.writeVarInt(payload.alloyInputs.size());
        payload.alloyInputs.forEach(input -> writeFluid(buffer, input));
        buffer.writeVarInt(payload.structureFluids.size());
        payload.structureFluids.forEach(input -> writeFluid(buffer, input));
        buffer.writeVarInt(payload.progress);
        buffer.writeVarInt(payload.processTime);
        buffer.writeVarInt(payload.structureCapacity);
        buffer.writeBoolean(payload.structureValid);
    }

    /** 1.21.1 的 FluidStack 流编码器不接受空栈，因此单独写入存在标记。 */
    private static FluidStack readFluid(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? FluidStack.STREAM_CODEC.decode(buffer) : FluidStack.EMPTY;
    }

    /** 1.21.1 的 FluidStack 流编码器不接受空栈，因此单独写入存在标记。 */
    private static void writeFluid(RegistryFriendlyByteBuf buffer, FluidStack fluid) {
        boolean present = fluid != null && !fluid.isEmpty();
        buffer.writeBoolean(present);
        if (present) {
            FluidStack.STREAM_CODEC.encode(buffer, fluid);
        }
    }

    /** 从服务端设备构造状态载荷。 */
    public static FoundryStatePayload from(FoundryBlockEntity entity) {
        List<FluidStack> alloyInputs = new ArrayList<>(FoundryBlockEntity.MAX_ALLOY_INPUTS);
        for (int index = 0; index < FoundryBlockEntity.MAX_ALLOY_INPUTS; index++) {
            alloyInputs.add(entity.getFluidInTank(index));
        }
        FluidStack fluid = entity.isAlloyer()
            ? entity.getFluidInTank(FoundryBlockEntity.ALLOY_OUTPUT_TANK) : entity.getFluidInTank(0);
        FluidStack fuelFluid = entity.isHeater() ? fluid : FluidStack.EMPTY;
        return new FoundryStatePayload(entity.getBlockPos(), fluid, fuelFluid, alloyInputs, entity.structureFluidLayers(), entity.progress(),
            entity.processTime(), entity.structureCapacity(), entity.isStructureValid());
    }

    /** 返回独立网络载荷类型。 */
    @Override
    public Type<FoundryStatePayload> type() {
        return TYPE;
    }
}
