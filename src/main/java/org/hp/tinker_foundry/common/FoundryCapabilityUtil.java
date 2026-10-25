package org.hp.tinker_foundry.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/** 统一处理 Forge 1.20.1 的方块实体能力查询，避免调用目标版本不存在的 Level 定位重载。 */
public final class FoundryCapabilityUtil {
    /** 从指定方块实体查询能力；目标不存在时返回空能力。 */
    public static <T> LazyOptional<T> get(Level level, BlockPos pos, Capability<T> capability, Direction side) {
        if (level == null) return LazyOptional.empty();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? LazyOptional.empty() : blockEntity.getCapability(capability, side);
    }

    private FoundryCapabilityUtil() {
    }
}
