package org.hp.tinker_foundry.common;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.hp.tinker_foundry.block.FoundryChannelBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

import javax.annotation.Nullable;

/** 不依赖 Mantle 的疏导槽分面流体能力，实现匠魂的顶部输入和侧面输入语义。 */
public final class FoundryChannelFluidHandler implements IFluidHandler {
    /** 当前能力所属的疏导槽方块实体。 */
    private final FoundryBlockEntity channel;
    /** 查询能力时的相邻面；空值和上方都表示顶部直接输入。 */
    private final Direction side;

    /** 创建指定面的疏导槽能力包装器。 */
    private FoundryChannelFluidHandler(FoundryBlockEntity channel, @Nullable Direction side) {
        this.channel = channel;
        this.side = side;
    }

    /** 只向真正允许输入的面暴露能力，输出面和未连接面保持没有能力。 */
    @Nullable
    public static IFluidHandler forSide(FoundryBlockEntity channel, @Nullable Direction side) {
        if (side == null || side == Direction.UP) {
            return new FoundryChannelFluidHandler(channel, side);
        }
        if (side == Direction.DOWN) {
            return null;
        }
        BlockState state = channel.getBlockState();
        if (!state.hasProperty(FoundryChannelBlock.DIRECTION_MAP.get(side))) {
            return null;
        }
        return state.getValue(FoundryChannelBlock.DIRECTION_MAP.get(side)) == FoundryChannelBlock.ChannelConnection.IN
            ? new FoundryChannelFluidHandler(channel, side) : null;
    }

    /** 每次能力调用重新读取状态，避免缓存能力跨越玩家切换连接模式。 */
    private boolean canFill() {
        if (side == null || side == Direction.UP) {
            return true;
        }
        if (side == Direction.DOWN) {
            return false;
        }
        BlockState state = channel.getBlockState();
        return state.hasProperty(FoundryChannelBlock.DIRECTION_MAP.get(side))
            && state.getValue(FoundryChannelBlock.DIRECTION_MAP.get(side)) == FoundryChannelBlock.ChannelConnection.IN;
    }

    /** 顶部或输入侧只有一个内部流体槽。 */
    @Override
    public int getTanks() {
        return canFill() ? 1 : 0;
    }

    /** 输出能力只允许外部注入，不允许绕过连接状态抽取内部流体。 */
    @Override
    public FluidStack getFluidInTank(int tank) {
        return canFill() && tank == 0 ? channel.getFluidInTank(0) : FluidStack.EMPTY;
    }

    /** 返回当前疏导槽四个金属粒的等价容量。 */
    @Override
    public int getTankCapacity(int tank) {
        return canFill() && tank == 0 ? channel.getTankCapacity(0) : 0;
    }

    /** 复用方块实体的流体类型校验，保持燃料和熔融流体的现有约束。 */
    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return canFill() && tank == 0 && channel.isFluidValid(0, stack);
    }

    /** 向顶部或输入侧注入流体，输出侧不接受任何反向写入。 */
    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!canFill()) return 0;
        int accepted = channel.fill(resource, action);
        if (accepted > 0 && action.execute() && side != null && side != Direction.UP) {
            // 输入侧刚接收流体时标记对应液柱为流动状态，保持 Mantle ChannelSideTank 语义。
            channel.markChannelInputFlow(side);
        }
        return accepted;
    }

    /** 疏导槽没有通过能力直接抽取流体的路径，所有输出由服务端逐面传输。 */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    /** 疏导槽没有通过能力直接抽取流体的路径，所有输出由服务端逐面传输。 */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
