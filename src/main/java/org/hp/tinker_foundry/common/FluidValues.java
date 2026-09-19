package org.hp.tinker_foundry.common;

/** 冶炼系统统一采用的流体计量单位。 */
public final class FluidValues {
    /** 一桶流体的容量。 */
    public static final int BUCKET = 1000;
    /** 一个金属锭对应的熔融流体量。 */
    public static final int INGOT = 90;
    /** 一个金属粒对应的熔融流体量。 */
    public static final int NUGGET = 10;
    /** 一个金属块对应的熔融流体量。 */
    public static final int BLOCK = 810;

    private FluidValues() {
    }
}
