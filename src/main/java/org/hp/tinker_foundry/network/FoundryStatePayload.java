package org.hp.tinker_foundry.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fluids.FluidStack;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 向已打开界面的客户端发送流体、进度和结构状态快照。 */
public record FoundryStatePayload(BlockPos pos, FluidStack fluid, FluidStack fuelFluid, List<FluidStack> alloyInputs, List<FluidStack> structureFluids,
                                  int progress, int processTime, int castingFluidCapacity, int structureCapacity, boolean structureValid, int[] heat) {
    /** 单个流体堆栈附加 NBT 的网络大小上限，防止状态同步携带无界数据。 */
    public static final int MAX_FLUID_TAG_BYTES = 16 * 1024;

    /** 从 Forge FriendlyByteBuf 读取状态快照，字段顺序是本模组内部协议的一部分。 */
    public static FoundryStatePayload decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        FluidStack fluid = readFluid(buffer);
        FluidStack fuelFluid = readFluid(buffer);
        int inputCount = buffer.readVarInt();
        if (inputCount < 0 || inputCount > FoundryBlockEntity.MAX_ALLOY_INPUTS) {
            throw new IllegalArgumentException("Invalid alloy input count: " + inputCount);
        }
        List<FluidStack> alloyInputs = new ArrayList<>(inputCount);
        for (int index = 0; index < inputCount; index++) {
            alloyInputs.add(readFluid(buffer));
        }
        int layers = buffer.readVarInt();
        if (layers < 0 || layers > org.hp.tinker_foundry.common.StructureFluidTank.MAX_LAYERS) {
            throw new IllegalArgumentException("Invalid structure fluid count: " + layers);
        }
        List<FluidStack> structureFluids = new ArrayList<>(layers);
        for (int index = 0; index < layers; index++) {
            structureFluids.add(readFluid(buffer));
        }
        int progress = readNonNegative(buffer, "progress");
        int time = readNonNegative(buffer, "process time");
        int castingCapacity = readNonNegative(buffer, "casting capacity");
        int capacity = readNonNegative(buffer, "structure capacity");
        boolean valid = buffer.readBoolean();
        int size = buffer.readVarInt();
        if (size < 0 || size > FoundryBlockEntity.MAX_STRUCTURE_INPUTS * 5 || size % 5 != 0) {
            throw new IllegalArgumentException("Invalid heat snapshot size: " + size);
        }
        int[] heat = new int[size];
        for (int index = 0; index < size; index++) {
            heat[index] = readNonNegative(buffer, "heat value");
        }
        return new FoundryStatePayload(pos, fluid, fuelFluid, alloyInputs, structureFluids, progress, time,
            castingCapacity, capacity, valid, heat);
    }

    /** 向 Forge FriendlyByteBuf 写入状态快照。 */
    public static void encode(FoundryStatePayload payload, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(payload.pos);
        writeFluid(buffer, payload.fluid);
        writeFluid(buffer, payload.fuelFluid);
        if (payload.alloyInputs.size() > FoundryBlockEntity.MAX_ALLOY_INPUTS) {
            throw new IllegalArgumentException("Too many alloy inputs: " + payload.alloyInputs.size());
        }
        buffer.writeVarInt(payload.alloyInputs.size());
        payload.alloyInputs.forEach(input -> writeFluid(buffer, input));
        if (payload.structureFluids.size() > org.hp.tinker_foundry.common.StructureFluidTank.MAX_LAYERS) {
            throw new IllegalArgumentException("Too many structure fluids: " + payload.structureFluids.size());
        }
        buffer.writeVarInt(payload.structureFluids.size());
        payload.structureFluids.forEach(input -> writeFluid(buffer, input));
        writeNonNegative(buffer, payload.progress, "progress");
        writeNonNegative(buffer, payload.processTime, "process time");
        writeNonNegative(buffer, payload.castingFluidCapacity, "casting capacity");
        writeNonNegative(buffer, payload.structureCapacity, "structure capacity");
        buffer.writeBoolean(payload.structureValid);
        if (payload.heat.length > FoundryBlockEntity.MAX_STRUCTURE_INPUTS * 5 || payload.heat.length % 5 != 0) {
            throw new IllegalArgumentException("Invalid heat snapshot size: " + payload.heat.length);
        }
        buffer.writeVarInt(payload.heat.length);
        for (int value : payload.heat) {
            writeNonNegative(buffer, value, "heat value");
        }
    }

    /** 1.20.1 Forge 的 FluidStack 允许直接使用 FriendlyByteBuf 读写。 */
    private static FluidStack readFluid(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return FluidStack.EMPTY;
        FluidStack fluid = FluidStack.readFromPacket(buffer);
        validateFluid(fluid);
        return fluid;
    }

    /** 1.20.1 Forge 的 FluidStack 允许直接使用 FriendlyByteBuf 读写。 */
    private static void writeFluid(FriendlyByteBuf buffer, FluidStack fluid) {
        boolean present = fluid != null && !fluid.isEmpty();
        buffer.writeBoolean(present);
        if (present) {
            validateFluid(fluid);
            fluid.writeToPacket(buffer);
        }
    }

    /** 校验网络中的流体堆栈和附加 NBT 边界。 */
    private static void validateFluid(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty() || fluid.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid fluid stack in state payload");
        }
        CompoundTag tag = fluid.getTag();
        if (tag != null && tag.sizeInBytes() > MAX_FLUID_TAG_BYTES) {
            throw new IllegalArgumentException("Fluid stack tag is too large: " + tag.sizeInBytes());
        }
    }

    /** 读取只允许非负值的 VarInt 字段。 */
    private static int readNonNegative(FriendlyByteBuf buffer, String field) {
        int value = buffer.readVarInt();
        if (value < 0) throw new IllegalArgumentException("Negative " + field + ": " + value);
        return value;
    }

    /** 写入只允许非负值的 VarInt 字段。 */
    private static void writeNonNegative(FriendlyByteBuf buffer, int value, String field) {
        if (value < 0) throw new IllegalArgumentException("Negative " + field + ": " + value);
        buffer.writeVarInt(value);
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
            entity.processTime(), entity.castingFluidCapacity(), entity.structureCapacity(), entity.isStructureValid(), entity.heatSnapshot());
    }
}
