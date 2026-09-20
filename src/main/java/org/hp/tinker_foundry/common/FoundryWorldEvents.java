package org.hp.tinker_foundry.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.multiblock.StructureTags;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 用事件标记附近控制器，避免每个控制器固定周期扫描整个结构。 */
public final class FoundryWorldEvents {
    /** 放置炉壁、设备或替换方块后，只标记可能受影响的已加载控制器。 */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            markNearbyControllers(level, event.getPos());
        }
    }

    /** 玩家破坏结构方块后，立即让附近控制器重新检查。 */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            markNearbyControllers(level, event.getPos());
        }
    }

    /** 命令、活塞或方块物理更新触发邻居通知时，复用同一局部标记逻辑。 */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide) {
            // 流体同步和普通机器更新也会触发邻居通知，只有可能参与多方块的方块才需要标记结构。
            if (!isStructureRelevant(event.getState())) {
                return;
            }
            markNearbyControllers(level, event.getPos());
        }
    }

    /** 判断邻居通知是否可能改变冶炼炉的几何结构。 */
    private static boolean isStructureRelevant(net.minecraft.world.level.block.state.BlockState state) {
        // 结构标签覆盖所有已迁移的石材、砖块、玻璃和储罐变种，避免新增变种遗漏更新控制器。
        if (state.is(StructureTags.SMELTERY_WALL) || state.is(StructureTags.SMELTERY_FLOOR)
            || state.is(StructureTags.SMELTERY_TANKS) || state.is(StructureTags.FOUNDRY_WALL)
            || state.is(StructureTags.FOUNDRY_FLOOR) || state.is(StructureTags.FOUNDRY_TANKS)) {
            return true;
        }
        return state.is(TFBlocks.SEARED_BRICK.get()) || state.is(TFBlocks.SEARED_GLASS.get())
            || state.is(TFBlocks.SEARED_LANTERN.get()) || state.is(TFBlocks.SEARED_LADDER.get())
            || state.is(TFBlocks.SEARED_WALL.get()) || state.is(TFBlocks.SEARED_FANCY_BRICK.get())
            || state.is(TFBlocks.SCORCHED_BRICK.get()) || state.is(TFBlocks.SCORCHED_GLASS.get())
            || state.is(TFBlocks.SCORCHED_LANTERN.get()) || state.is(TFBlocks.SCORCHED_LADDER.get())
            || state.is(TFBlocks.SCORCHED_WALL.get()) || state.is(TFBlocks.SCORCHED_FANCY_BRICK.get())
            || state.is(TFBlocks.SMELTERY_CONTROLLER.get()) || state.is(TFBlocks.FOUNDRY_CONTROLLER.get())
            || state.is(TFBlocks.SEARED_TANK.get()) || state.is(TFBlocks.SCORCHED_TANK.get())
            || state.is(TFBlocks.SEARED_FUEL_TANK.get()) || state.is(TFBlocks.SCORCHED_FUEL_TANK.get())
            || state.is(TFBlocks.SEARED_CASTING_TANK.get()) || state.is(TFBlocks.SCORCHED_CASTING_TANK.get())
            || state.is(TFBlocks.MELTER.get()) || state.is(TFBlocks.HEATER.get())
            || state.is(TFBlocks.ALLOYER.get()) || state.is(TFBlocks.CASTING_TABLE.get())
            || state.is(TFBlocks.CASTING_BASIN.get()) || state.is(TFBlocks.DRAIN.get())
            || state.is(TFBlocks.FAUCET.get()) || state.is(TFBlocks.DUCT.get())
            || state.is(TFBlocks.CHUTE.get()) || state.is(TFBlocks.FLUID_GAUGE.get());
    }

    /** 区块载入时只标记该区块中的控制器，设备 tick 会在区块真正可用后验证。 */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && !level.isClientSide && event.getChunk() instanceof LevelChunk chunk) {
            for (var blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof FoundryBlockEntity foundry) {
                    foundry.markStructureDirty();
                }
            }
        }
    }

    /** 在三乘三已加载区块范围内查找控制器，结构最大跨度不会超出该范围。 */
    private static void markNearbyControllers(Level level, BlockPos changedPos) {
        int chunkX = changedPos.getX() >> 4;
        int chunkZ = changedPos.getZ() >> 4;
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                ChunkAccess chunk = level.getChunk(chunkX + offsetX, chunkZ + offsetZ, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk levelChunk)) {
                    continue;
                }
                for (var blockEntity : levelChunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof FoundryBlockEntity foundry) || !foundry.isStructureController()) {
                        continue;
                    }
                    BlockPos controller = foundry.getBlockPos();
                    if (Math.abs(controller.getX() - changedPos.getX()) <= 16
                        && Math.abs(controller.getY() - changedPos.getY()) <= 64
                        && Math.abs(controller.getZ() - changedPos.getZ()) <= 16) {
                        foundry.markStructureDirty();
                    }
                }
            }
        }
    }

    private FoundryWorldEvents() {
    }
}
