package org.hp.tinker_foundry.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

/** NeoForge 1.21.1 的服务端调试生成命令，生成可立即操作的冶炼测试场。 */
public final class FoundryDebugCommand {
    /** 注册 /tinker_foundry debug generate 命令树。 */
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tinker_foundry")
            .requires(source -> source.hasPermission(2))
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
                        .executes(context -> generate(context.getSource(), StringArgumentType.getString(context, "type")))))
                .then(Commands.literal("fill")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                        .then(Commands.argument("fluid", StringArgumentType.greedyString())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(sourceFluidIds(), builder))
                            .executes(context -> fill(context.getSource(), IntegerArgumentType.getInteger(context, "amount"),
                                StringArgumentType.getString(context, "fluid"))))))
                .then(Commands.literal("potion")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                        .then(Commands.argument("potion", StringArgumentType.greedyString())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                BuiltInRegistries.POTION.keySet().stream().map(ResourceLocation::toString), builder))
                            .executes(context -> fillPotion(context.getSource(), IntegerArgumentType.getInteger(context, "amount"),
                                StringArgumentType.getString(context, "potion"))))))
                .then(Commands.literal("drain")
                    .executes(context -> drain(context.getSource())))
                .then(Commands.literal("kit")
                    .executes(context -> giveKit(context.getSource())))));
    }

    /** 测试物品包：物品 ID 与数量，缺失的物品会被跳过。 */
    private static final String[][] KIT = {
        {"tinker_foundry:ingot_cast", "1"}, {"tinker_foundry:nugget_cast", "1"}, {"tinker_foundry:gem_cast", "1"},
        {"tinker_foundry:copper_canister", "1"}, {"tinker_foundry:seared_brick", "64"}, {"tinker_foundry:grout", "32"},
        {"minecraft:glass_bottle", "16"}, {"minecraft:bowl", "16"}, {"minecraft:bucket", "4"},
        {"minecraft:cobblestone", "16"}, {"minecraft:gravel", "8"}, {"minecraft:cobblestone_slab", "8"},
        {"minecraft:cobblestone_stairs", "8"}, {"minecraft:cobblestone_wall", "8"}, {"minecraft:stone", "8"},
        {"minecraft:smooth_stone", "8"}, {"minecraft:stone_slab", "8"}, {"minecraft:stone_stairs", "8"},
        {"minecraft:raw_iron", "16"}, {"minecraft:iron_ore", "8"}, {"minecraft:iron_ingot", "16"},
        {"minecraft:gold_ingot", "16"}, {"minecraft:copper_ingot", "16"}, {"minecraft:raw_copper", "16"},
        {"minecraft:diamond", "4"}, {"minecraft:emerald", "4"}, {"minecraft:glass", "16"},
        {"minecraft:soul_sand", "8"}, {"minecraft:coal", "32"}, {"minecraft:clay_ball", "32"},
        {"minecraft:iron_sword", "1"}, {"minecraft:iron_chestplate", "1"}
    };

    /** 取得准星指向的本模组设备，找不到时向执行者报错。 */
    private static FoundryBlockEntity target(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(8.0D, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
            && source.getLevel().getBlockEntity(blockHit.getBlockPos()) instanceof FoundryBlockEntity entity) {
            return entity;
        }
        source.sendFailure(Component.literal("请把准星对准 8 格内的冶炼设备（储罐、控制器、浇注台等）"));
        return null;
    }

    /** 列出所有可注入的静止流体 ID，供命令补全使用。 */
    private static java.util.stream.Stream<String> sourceFluidIds() {
        return BuiltInRegistries.FLUID.stream()
            .filter(fluid -> fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState()))
            .map(fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString());
    }

    /** 向准星指向的设备注入指定流体，浇注台和浇注盆会立刻开始浇注。 */
    private static int fill(CommandSourceStack source, int amount, String fluidId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ResourceLocation id = ResourceLocation.tryParse(fluidId.trim());
        Fluid fluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getOptional(id).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) {
            source.sendFailure(Component.literal("未知流体：" + fluidId));
            return 0;
        }
        return fillTarget(source, new FluidStack(fluid, amount));
    }

    /** 向准星指向的设备注入带药水数据的药水流体。 */
    private static int fillPotion(CommandSourceStack source, int amount, String potionId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ResourceLocation id = ResourceLocation.tryParse(potionId.trim());
        if (id == null || BuiltInRegistries.POTION.getOptional(id).isEmpty() || !TFFluids.EXTRA_SOURCES.containsKey("potion")) {
            source.sendFailure(Component.literal("未知药水：" + potionId));
            return 0;
        }
        FluidStack stack = new FluidStack(TFFluids.EXTRA_SOURCES.get("potion").get(), amount);
        stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(
            BuiltInRegistries.POTION.wrapAsHolder(BuiltInRegistries.POTION.getOptional(id).get())));
        return fillTarget(source, stack);
    }

    /** 执行注入并回报实际注入量。 */
    private static int fillTarget(CommandSourceStack source, FluidStack stack) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FoundryBlockEntity entity = target(source);
        if (entity == null) return 0;
        int filled = entity.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        TinkerFoundry.LOGGER.debug("[debug-command] fill pos={} fluid={} requested={} filled={}",
            entity.getBlockPos(), BuiltInRegistries.FLUID.getKey(stack.getFluid()), stack.getAmount(), filled);
        source.sendSuccess(() -> Component.literal("已注入 " + filled + " / " + stack.getAmount() + " mB"), false);
        return filled;
    }

    /** 清空准星指向设备中的全部可抽取流体。 */
    private static int drain(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FoundryBlockEntity entity = target(source);
        if (entity == null) return 0;
        int total = 0;
        for (int i = 0; i < 64; i++) {
            FluidStack drained = entity.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) break;
            total += drained.getAmount();
        }
        int result = total;
        TinkerFoundry.LOGGER.debug("[debug-command] drain pos={} drained={}", entity.getBlockPos(), result);
        source.sendSuccess(() -> Component.literal("已抽出 " + result + " mB"), false);
        return result;
    }

    /** 发放覆盖铸造、熔炼和模具测试的物品包。 */
    private static int giveKit(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int given = 0;
        for (String[] entry : KIT) {
            ResourceLocation id = ResourceLocation.tryParse(entry[0]);
            Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
            player.getInventory().placeItemBackInInventory(new ItemStack(item, Integer.parseInt(entry[1])));
            given++;
        }
        int result = given;
        source.sendSuccess(() -> Component.literal("已发放 " + result + " 种测试物品"), false);
        return result;
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
                source.sendFailure(Component.literal("Unknown type. Use smeltery, foundry, alloyer, or all."));
                return 0;
            }
        }
        TinkerFoundry.LOGGER.debug("[debug-command] generated type={} origin={} facing={} player={}", normalized, origin, facing, player.getGameProfile().getName());
        source.sendSuccess(() -> Component.literal("Generated " + normalized + " debug structure at " + origin.toShortString()), true);
        return 1;
    }

    /** 生成冶炼炉或铸造炉的最小有效结构，并预装燃料和熔融铁。 */
    private static void generateSmeltery(ServerLevel level, BlockPos controllerPos, Direction facing, boolean foundry) {
        Direction inside = facing.getOpposite();
        Direction side = inside.getClockWise();
        Block wall = foundry ? TFBlocks.SCORCHED_BRICKS.get() : TFBlocks.SEARED_BRICKS.get();
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
            controllerEntity.fill(new FluidStack(TFFluids.metal("iron"), 1000), IFluidHandler.FluidAction.EXECUTE);
        }
        if (level.getBlockEntity(fuelPos) instanceof FoundryBlockEntity fuelEntity) {
            fuelEntity.fill(new FluidStack(Fluids.LAVA, 8000), IFluidHandler.FluidAction.EXECUTE);
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
