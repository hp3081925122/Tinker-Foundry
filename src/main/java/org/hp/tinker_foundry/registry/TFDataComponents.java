package org.hp.tinker_foundry.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.hp.tinker_foundry.TinkerFoundry;

/** 流体物品保存完整流体数据，组件值本身使用不可变的CustomData。 */
public final class TFDataComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTER =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TinkerFoundry.MOD_ID);
    /** 药水内容或第三方流体数据随物品保存和网络同步。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> FLUID = REGISTER.register("fluid",
        () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).build());
    private TFDataComponents() { }
}
