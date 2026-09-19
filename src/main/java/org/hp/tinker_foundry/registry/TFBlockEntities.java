package org.hp.tinker_foundry.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 冶炼设备方块实体注册表。 */
public final class TFBlockEntities {
    /** 所有冶炼设备共用一个数据类型，行为通过宿主方块区分。 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryBlockEntity>> GENERIC = TinkerFoundry.BLOCK_ENTITY_TYPES.register("foundry_device", () -> BlockEntityType.Builder.of(
        FoundryBlockEntity::new,
        TFBlocks.SMELTERY_CONTROLLER.get(),
        TFBlocks.FOUNDRY_CONTROLLER.get(),
        TFBlocks.MELTER.get(),
        TFBlocks.HEATER.get(),
        TFBlocks.ALLOYER.get(),
        TFBlocks.SEARED_TANK.get(),
        TFBlocks.SCORCHED_TANK.get(),
        TFBlocks.SEARED_FUEL_TANK.get(),
        TFBlocks.SCORCHED_FUEL_TANK.get(),
        TFBlocks.SEARED_CASTING_TANK.get(),
        TFBlocks.SCORCHED_CASTING_TANK.get(),
        TFBlocks.CASTING_TABLE.get(),
        TFBlocks.CASTING_BASIN.get(),
        TFBlocks.DRAIN.get(),
        TFBlocks.FAUCET.get(),
        TFBlocks.DUCT.get(),
        TFBlocks.CHUTE.get(),
        TFBlocks.FLUID_GAUGE.get(),
        TFBlocks.SEARED_LANTERN.get(),
        TFBlocks.SCORCHED_LANTERN.get()
    ).build(null));

    private TFBlockEntities() {
    }
}
