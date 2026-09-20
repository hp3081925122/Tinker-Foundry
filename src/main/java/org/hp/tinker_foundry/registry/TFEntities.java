package org.hp.tinker_foundry.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.entity.FoundryFluidCannonProjectile;

/** 冶炼系统实体注册表。 */
public final class TFEntities {
    /** 流体炮发射的本地流体弹实体，不依赖 Mantle 的实体类型。 */
    public static final DeferredHolder<EntityType<?>, EntityType<FoundryFluidCannonProjectile>> FLUID_CANNON_PROJECTILE =
        TinkerFoundry.ENTITY_TYPES.register("fluid_spit", () -> EntityType.Builder
            .<FoundryFluidCannonProjectile>of(FoundryFluidCannonProjectile::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .setShouldReceiveVelocityUpdates(false)
            .build("tinker_foundry:fluid_spit"));

    /** 防止把注册表误当作可实例化对象。 */
    private TFEntities() {
    }
}
