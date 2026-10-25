package org.hp.tinker_foundry.registry;

/** 1.20.1 Forge 不注册 1.21 数据组件，物品流体语义改由 ItemStack NBT 保存。 */
public final class TFDataComponents {
    /** 逻辑上保留原流体字段名，供便携容器的 NBT 迁移代码共用。 */
    public static final String FLUID_TAG = "Fluid";

    private TFDataComponents() {
    }
}
