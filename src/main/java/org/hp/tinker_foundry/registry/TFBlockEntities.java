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
        TFBlocks.SEARED_MELTER.get(),
        TFBlocks.SEARED_HEATER.get(),
        TFBlocks.SCORCHED_ALLOYER.get(),
        TFBlocks.SEARED_FUEL_TANK.get(),
        TFBlocks.SCORCHED_FUEL_TANK.get(),
        TFBlocks.SEARED_CASTING_TANK.get(),
        TFBlocks.SEARED_TABLE.get(),
        TFBlocks.SCORCHED_TABLE.get(),
        TFBlocks.SEARED_BASIN.get(),
        TFBlocks.SCORCHED_BASIN.get(),
        TFBlocks.SEARED_DRAIN.get(),
        TFBlocks.SCORCHED_DRAIN.get(),
        TFBlocks.SEARED_FAUCET.get(),
        TFBlocks.SCORCHED_FAUCET.get(),
        TFBlocks.SEARED_DUCT.get(),
        TFBlocks.SCORCHED_DUCT.get(),
        TFBlocks.SEARED_CHUTE.get(),
        TFBlocks.SCORCHED_CHUTE.get(),
        TFBlocks.SEARED_CHANNEL.get(),
        TFBlocks.SCORCHED_CHANNEL.get(),
        TFBlocks.SCORCHED_PROXY_TANK.get(),
        TFBlocks.SEARED_FLUID_CANNON.get(),
        TFBlocks.SCORCHED_FLUID_CANNON.get(),
        TFBlocks.COPPER_GAUGE.get(),
        TFBlocks.OBSIDIAN_GAUGE.get(),
        TFBlocks.SEARED_INGOT_GAUGE.get(),
        TFBlocks.SCORCHED_INGOT_GAUGE.get(),
        TFBlocks.SEARED_FUEL_GAUGE.get(),
        TFBlocks.SCORCHED_FUEL_GAUGE.get(),
        TFBlocks.SEARED_LANTERN.get(),
        TFBlocks.SCORCHED_LANTERN.get(),
        TFBlocks.SEARED_INGOT_TANK.get(),
        TFBlocks.SCORCHED_INGOT_TANK.get()
    ).build(null));

    private TFBlockEntities() {
    }
}
