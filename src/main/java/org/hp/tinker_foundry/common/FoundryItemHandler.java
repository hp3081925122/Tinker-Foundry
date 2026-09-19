package org.hp.tinker_foundry.common;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 物品能力只暴露设备真实输入与输出，导入槽动态代理控制器。 */
public final class FoundryItemHandler implements IItemHandler {
    private final FoundryBlockEntity owner;

    /** 保留附件而非控制器引用，拆炉后缓存能力立即失效。 */
    public FoundryItemHandler(FoundryBlockEntity owner) { this.owner = owner; }

    /** 导入槽每次访问重新解析归属，其余设备直接访问自身。 */
    private FoundryBlockEntity target() {
        return owner.getBlockState().is(TFBlocks.CHUTE.get()) ? owner.attachedController() : owner;
    }

    /** 加热器仅燃料槽，熔炼设备仅输入，浇注设备包含成品和容器返还。 */
    @Override
    public int getSlots() {
        FoundryBlockEntity entity = target();
        return entity == null ? 0 : entity.isFuelTankBlock() || entity.isHeater() ? 1
            : entity.inputSlotCount() + (entity.isCastingBlock() || entity.isCastingTankBlock() ? 2 : 0);
    }

    /** 将能力编号映射为容器编号，避开保留的隐藏槽。 */
    private int mapped(FoundryBlockEntity entity, int slot) {
        if (entity.isFuelTankBlock() || entity.isHeater()) return FoundryBlockEntity.FUEL_SLOT;
        int count = entity.inputSlotCount();
        return slot < count ? FoundryBlockEntity.inputContainerSlot(slot)
            : slot == count ? FoundryBlockEntity.OUTPUT_SLOT : FoundryBlockEntity.REMAINDER_SLOT;
    }

    /** 返回副本，不允许管道绕开插入和抽取规则修改物品。 */
    @Override
    public ItemStack getStackInSlot(int slot) {
        FoundryBlockEntity entity = target();
        return entity == null || slot < 0 || slot >= getSlots() ? ItemStack.EMPTY : entity.getItem(mapped(entity, slot)).copy();
    }

    /** 模拟只计算余量，执行才更新真实槽位。 */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
        FoundryBlockEntity entity = target();
        if (entity == null) return stack;
        int index = mapped(entity, slot);
        ItemStack stored = entity.getItem(index);
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, stack)) return stack;
        int amount = Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()) - stored.getCount());
        if (amount <= 0) return stack;
        if (!simulate) entity.setItem(index, stack.copyWithCount(stored.getCount() + amount));
        return stack.copyWithCount(stack.getCount() - amount);
    }

    /** 正常抽取真实槽内容，熔炼进度由容器写入方法同步清理。 */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stored = getStackInSlot(slot);
        if (amount <= 0 || stored.isEmpty()) return ItemStack.EMPTY;
        int taken = Math.min(amount, stored.getCount());
        FoundryBlockEntity entity = target();
        if (!simulate && entity != null) entity.setItem(mapped(entity, slot), stored.copyWithCount(stored.getCount() - taken));
        return stored.copyWithCount(taken);
    }

    /** 熔炼槽固定一个物品，加热器与浇注输出按普通堆叠。 */
    @Override
    public int getSlotLimit(int slot) {
        FoundryBlockEntity entity = target();
        return entity != null && entity.isMeltingBlock() ? 1 : 64;
    }

    /** 成品槽禁止插入，结构无效时附件不提供任何可用槽。 */
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        FoundryBlockEntity entity = target();
        return entity != null && slot >= 0 && slot < getSlots() && entity.canPlaceItem(mapped(entity, slot), stack);
    }
}
