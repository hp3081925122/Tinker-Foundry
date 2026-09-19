package org.hp.tinker_foundry.item;

import net.minecraft.world.item.Item;

/** 便携储液罐物品，容量由注册时固定。 */
public final class PortableTankItem extends Item {
    private final int capacity;

    /** 创建指定容量的便携储液罐。 */
    public PortableTankItem(int capacity, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    /** 返回单个物品的储液上限。 */
    public int capacity() {
        return capacity;
    }
}
