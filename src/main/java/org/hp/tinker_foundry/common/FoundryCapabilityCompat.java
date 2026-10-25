package org.hp.tinker_foundry.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.jetbrains.annotations.Nullable;

/** 26.1.2 的新资源能力与项目旧式处理器之间的兼容桥。 */
public final class FoundryCapabilityCompat {
    private FoundryCapabilityCompat() {
    }

    /** 通过 NeoForge 官方兼容入口读取方块流体能力。 */
    public static @Nullable IFluidHandler fluid(Level level, BlockPos pos, @Nullable Direction side) {
        return FluidUtil.getFluidHandler(level, pos, side).orElse(null);
    }

    /** 模组内部传输优先读取本模组的旧式处理器，避免模拟操作被新版事务桥转成实际写入。 */
    public static @Nullable IFluidHandler internalFluid(Level level, BlockPos pos, @Nullable Direction side) {
        if (!(level.getBlockEntity(pos) instanceof FoundryBlockEntity entity)) {
            return fluid(level, pos, side);
        }
        BlockState state = entity.getBlockState();
        if (entity.isFaucetBlock()
            || state.is(TFBlocks.SEARED_CHUTE.get()) || state.is(TFBlocks.SCORCHED_CHUTE.get())) {
            return null;
        }
        if (state.is(TFBlocks.SEARED_CHANNEL.get()) || state.is(TFBlocks.SCORCHED_CHANNEL.get())) {
            return FoundryChannelFluidHandler.forSide(entity, side);
        }
        if (state.is(TFBlocks.SEARED_DRAIN.get()) || state.is(TFBlocks.SCORCHED_DRAIN.get())
            || state.is(TFBlocks.SEARED_DUCT.get()) || state.is(TFBlocks.SCORCHED_DUCT.get())) {
            return new FoundryPortFluidHandler(entity);
        }
        if (entity.isAlloyer()) {
            return entity.alloyerOutputHandler();
        }
        return entity;
    }

    /** 通过新资源能力读取方块物品能力，并包装成旧逻辑使用的接口。 */
    public static @Nullable IItemHandler item(Level level, BlockPos pos, @Nullable Direction side) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        return handler == null ? null : IItemHandler.of(handler);
    }

    /** 把项目已有的流体处理器注册为 26.1.2 的资源处理器。 */
    public static ResourceHandler<FluidResource> fluidResource(IFluidHandler handler) {
        return new FluidResourceBridge(handler);
    }

    /** 把项目已有的物品处理器注册为 26.1.2 的资源处理器。 */
    public static ResourceHandler<ItemResource> itemResource(IItemHandler handler) {
        return new ItemResourceBridge(handler);
    }

    /** 旧式流体处理器的事务桥，保证新能力回滚时不留下半次传输。 */
    private static final class FluidResourceBridge extends SnapshotJournal<FluidStack[]> implements ResourceHandler<FluidResource> {
        private final IFluidHandler delegate;

        private FluidResourceBridge(IFluidHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.getTanks();
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.of(delegate.getFluidInTank(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return delegate.getFluidInTank(index).getAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return delegate.getTankCapacity(index);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return !resource.isEmpty() && delegate.isFluidValid(index, resource.toStack(1));
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || resource.isEmpty()) return 0;
            updateSnapshots(transaction);
            return delegate.fill(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || resource.isEmpty()) return 0;
            updateSnapshots(transaction);
            FluidStack extracted = delegate.drain(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
            return extracted.isEmpty() ? 0 : extracted.getAmount();
        }

        @Override
        protected FluidStack[] createSnapshot() {
            FluidStack[] snapshot = new FluidStack[size()];
            for (int index = 0; index < snapshot.length; index++) {
                snapshot[index] = delegate.getFluidInTank(index).copy();
            }
            return snapshot;
        }

        @Override
        protected void revertToSnapshot(FluidStack[] snapshot) {
            for (int index = 0; index < Math.min(snapshot.length, size()); index++) {
                FluidStack current = delegate.getFluidInTank(index).copy();
                if (!current.isEmpty()) delegate.drain(current, IFluidHandler.FluidAction.EXECUTE);
                if (!snapshot[index].isEmpty()) delegate.fill(snapshot[index], IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** 旧式物品处理器的事务桥，保留槽位和堆叠规则。 */
    private static final class ItemResourceBridge extends SnapshotJournal<ItemStack[]> implements ResourceHandler<ItemResource> {
        private final IItemHandler delegate;

        private ItemResourceBridge(IItemHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.getSlots();
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.of(delegate.getStackInSlot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return delegate.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return delegate.getSlotLimit(index);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty() && delegate.isItemValid(index, resource.toStack(1));
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || resource.isEmpty()) return 0;
            updateSnapshots(transaction);
            ItemStack remainder = delegate.insertItem(index, resource.toStack(amount), false);
            return amount - remainder.getCount();
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || resource.isEmpty()) return 0;
            ItemStack current = delegate.getStackInSlot(index);
            if (!resource.matches(current)) return 0;
            updateSnapshots(transaction);
            ItemStack extracted = delegate.extractItem(index, amount, false);
            return extracted.isEmpty() ? 0 : extracted.getCount();
        }

        @Override
        protected ItemStack[] createSnapshot() {
            ItemStack[] snapshot = new ItemStack[size()];
            for (int index = 0; index < snapshot.length; index++) {
                snapshot[index] = delegate.getStackInSlot(index).copy();
            }
            return snapshot;
        }

        @Override
        protected void revertToSnapshot(ItemStack[] snapshot) {
            for (int index = 0; index < Math.min(snapshot.length, size()); index++) {
                if (delegate instanceof IItemHandlerModifiable modifiable) {
                    modifiable.setStackInSlot(index, snapshot[index].copy());
                } else {
                    ItemStack current = delegate.getStackInSlot(index);
                    if (!current.isEmpty()) delegate.extractItem(index, current.getCount(), false);
                    if (!snapshot[index].isEmpty()) delegate.insertItem(index, snapshot[index].copy(), false);
                }
            }
        }
    }
}
