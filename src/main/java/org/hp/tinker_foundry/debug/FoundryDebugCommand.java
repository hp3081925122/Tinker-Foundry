package org.hp.tinker_foundry.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.FoundryControllerBlock;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.block.FoundryHorizontalBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFFluids;

/** 26.1.2 专用的服务端调试生成命令，生成可立即操作的冶炼测试场。 */
public final class FoundryDebugCommand {
    /** 注册 /tinker_foundry debug generate 命令树。 */
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tinker_foundry")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("debug")
                .then(Commands.literal("generate")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("smeltery");
                            builder.suggest("foundry");
                            builder.suggest("alloyer");
                            builder.suggest("all");
                            return builder.buildFuture();
                        })
                        .executes(context -> generate(context.getSource(), StringArgumentType.getString(context, "type")))))));
    }

    /** 在执行者前方生成指定类型的调试设备。 */
    private static int generate(CommandSourceStack source, String type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Direction facing = player.getDirection().getOpposite();
        BlockPos origin = player.blockPosition().relative(player.getDirection(), 8);
        String normalized = type.toLowerCase(java.util.Locale.ROOT);
        switch (normalized) {
            case "smeltery" -> generateSmeltery(level, origin, facing, false);
            case "foundry" -> generateSmeltery(level, origin, facing, true);
            case "alloyer" -> generateAlloyer(level, origin, facing);
            case "all" -> {
                generateSmeltery(level, origin, facing, false);
                generateSmeltery(level, origin.relative(facing.getClockWise(), 10), facing, true);
                generateAlloyer(level, origin.relative(facing.getClockWise(), -10), facing);
            }
            default -> {
                source.sendFailure(Component.literal("未知类型，可用：smeltery、foundry、alloyer、all"));
                return 0;
            }
        }
        TinkerFoundry.LOGGER.debug("[debug-command] generated type={} origin={} facing={} player={}", normalized, origin, facing, player.getGameProfile().name());
        source.sendSuccess(() -> Component.literal("已生成 " + normalized + " 调试设备；控制器位于 " + origin.toShortString()
            + "，前方为浇注口和浇注盆，可直接右键操作。"), true);
        return 1;
    }

    /** 生成冶炼炉或铸造炉的 5x5x3 最小有效结构，并预装燃料和熔融铁。 */
    private static void generateSmeltery(ServerLevel level, BlockPos controllerPos, Direction facing, boolean foundry) {
        Direction inside = facing.getOpposite();
        Direction side = inside.getClockWise();
        Block wall = foundry ? TFBlocks.SCORCHED_BRICKS.get() : TFBlocks.SEARED_BRICKS.get();
        Block floor = wall;
        Block controller = foundry ? TFBlocks.FOUNDRY_CONTROLLER.get() : TFBlocks.SMELTERY_CONTROLLER.get();
        Block fuelTank = foundry ? TFBlocks.SCORCHED_FUEL_TANK.get() : TFBlocks.SEARED_FUEL_TANK.get();
        Block faucet = foundry ? TFBlocks.SCORCHED_FAUCET.get() : TFBlocks.SEARED_FAUCET.get();
        Block basin = foundry ? TFBlocks.SCORCHED_BASIN.get() : TFBlocks.SEARED_BASIN.get();
        Block table = foundry ? TFBlocks.SCORCHED_TABLE.get() : TFBlocks.SEARED_TABLE.get();
        Block drain = foundry ? TFBlocks.SCORCHED_DRAIN.get() : TFBlocks.SEARED_DRAIN.get();

        // 结构检测器把控制器所在平面作为正面墙，下面一层作为底板，向内连续三格作为炉腔深度。
        for (int layer = 0; layer < 3; layer++) {
            int y = controllerPos.getY() - 1 + layer;
            for (int sideOffset = -2; sideOffset <= 2; sideOffset++) {
                for (int depth = 0; depth <= 4; depth++) {
                    boolean boundary = Math.abs(sideOffset) == 2 || depth == 0 || depth == 4;
                    BlockPos pos = controllerPos.relative(side, sideOffset).relative(inside, depth).atY(y);
                    if (layer == 1 && sideOffset == 0 && depth == 0) {
                        place(level, pos, controller.defaultBlockState().setValue(FoundryControllerBlock.FACING, facing));
                    } else if (boundary || layer == 0) {
                        place(level, pos, wall.defaultBlockState());
                    } else {
                        place(level, pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        // 燃料罐放在有效侧墙上，结构刷新后会把它登记为控制器燃料来源。
        BlockPos fuelPos = controllerPos.relative(side, 2).relative(inside, 2);
        place(level, fuelPos, fuelTank.defaultBlockState());
        BlockPos drainPos = controllerPos.relative(side, -2).relative(inside, 2).above();
        // 排液口注册为四向水平设备，必须使用与其状态定义相同的水平 FACING 属性。
        BlockState drainState = drain.defaultBlockState().setValue(FoundryHorizontalBlock.FACING, side);
        place(level, drainPos, drainState);
        // 记录调试结构中的排液口方向，确认生成器没有把六向属性写入四向方块。
        TinkerFoundry.LOGGER.debug("[debug-command] placed horizontal drain pos={} facing={} hasFacing={}",
            drainPos, drainState.getValue(FoundryHorizontalBlock.FACING), drainState.hasProperty(FoundryHorizontalBlock.FACING));

        // 浇注口位于正面墙外，正面朝向外侧；下方盆和侧方台都能直接承接浇注结果。
        BlockPos faucetPos = controllerPos.relative(facing);
        place(level, faucetPos, faucet.defaultBlockState().setValue(FoundryDirectionalBlock.FACING, facing));
        place(level, faucetPos.below(), basin.defaultBlockState());
        place(level, faucetPos.relative(side), table.defaultBlockState());

        if (level.getBlockEntity(controllerPos) instanceof FoundryBlockEntity controllerEntity) {
            controllerEntity.markStructureDirty();
            controllerEntity.refreshStructureIfDirty();
        }
        if (level.getBlockEntity(fuelPos) instanceof FoundryBlockEntity fuelEntity) {
            fuelEntity.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
        }
        if (level.getBlockEntity(controllerPos) instanceof FoundryBlockEntity controllerEntity) {
            controllerEntity.refreshStructureIfDirty();
            controllerEntity.fill(new FluidStack(TFFluids.metal("iron"), 1000), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** 生成合金炉、下方燃料罐和两侧输入罐，并预装一组熔融金属。 */
    private static void generateAlloyer(ServerLevel level, BlockPos alloyerPos, Direction facing) {
        Direction side = facing.getClockWise();
        BlockPos fuelPos = alloyerPos.below();
        BlockPos leftTank = alloyerPos.relative(side);
        BlockPos rightTank = alloyerPos.relative(side.getOpposite());
        place(level, fuelPos, TFBlocks.SCORCHED_FUEL_TANK.get().defaultBlockState());
        place(level, alloyerPos, TFBlocks.SCORCHED_ALLOYER.get().defaultBlockState());
        place(level, leftTank, TFBlocks.SEARED_INGOT_TANK.get().defaultBlockState());
        place(level, rightTank, TFBlocks.SEARED_INGOT_TANK.get().defaultBlockState());
        if (level.getBlockEntity(fuelPos) instanceof FoundryBlockEntity fuelEntity) {
            fuelEntity.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
        }
        if (level.getBlockEntity(leftTank) instanceof FoundryBlockEntity leftEntity) {
            leftEntity.fill(new FluidStack(TFFluids.metal("iron"), 1000), IFluidHandler.FluidAction.EXECUTE);
        }
        if (level.getBlockEntity(rightTank) instanceof FoundryBlockEntity rightEntity) {
            rightEntity.fill(new FluidStack(TFFluids.metal("gold"), 1000), IFluidHandler.FluidAction.EXECUTE);
        }
        if (level.getBlockEntity(alloyerPos) instanceof FoundryBlockEntity alloyerEntity) {
            alloyerEntity.markStructureDirty();
            alloyerEntity.refreshStructureIfDirty();
            alloyerEntity.refreshAlloyerInputsForMenu();
        }
    }

    /** 以客户端可见的方块更新标志放置调试结构方块。 */
    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private FoundryDebugCommand() {
    }
}
