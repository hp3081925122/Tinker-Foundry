package org.hp.tinker_foundry.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.block.FoundryControllerBlock;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.recipe.AlloyingRecipe;
import org.hp.tinker_foundry.recipe.CastingRecipe;
import org.hp.tinker_foundry.recipe.FluidRecipeInput;
import org.hp.tinker_foundry.recipe.MeltingRecipe;
import org.hp.tinker_foundry.recipe.MoldingRecipe;
import org.hp.tinker_foundry.multiblock.FoundryMultiblock;
import org.hp.tinker_foundry.multiblock.SmelteryMultiblock;
import org.hp.tinker_foundry.multiblock.StructureErrorReason;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.registry.TFRecipes;

/** 为独立冶炼系统提供可由 NeoForge 自动发现的最小游戏内验证。 */
@GameTestHolder(TinkerFoundry.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FoundryGameTests {
    /** 使用原版稳定存在的空结构作为测试容器，测试体在运行时放置自己的方块。 */
    private static final String VANILLA_EMPTY_TEMPLATE = "bastion/blocks/air";

    /** 验证两种冶炼梯子同时具有原版攀爬标签，确保 NeoForge 的攀爬判定会识别它们。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void ladderClimbableTag(GameTestHelper helper) {
        // 先放置坚固支撑面，再按原版梯子朝向放置两个待测方块。
        BlockPos supportPos = new BlockPos(0, 0, 1);
        BlockPos searedPos = new BlockPos(0, 0, 0);
        BlockPos scorchedPos = new BlockPos(2, 0, 0);
        helper.setBlock(supportPos, net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(supportPos.offset(2, 0, 0), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(searedPos, TFBlocks.SEARED_LADDER.get().defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.NORTH));
        helper.setBlock(scorchedPos, TFBlocks.SCORCHED_LADDER.get().defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.NORTH));

        // NeoForge 1.21.1 的默认梯子判定通过 minecraft:climbable 标签识别方块。
        helper.assertTrue(helper.getBlockState(searedPos).is(BlockTags.CLIMBABLE), "seared ladder is not climbable");
        helper.assertTrue(helper.getBlockState(scorchedPos).is(BlockTags.CLIMBABLE), "scorched ladder is not climbable");
        helper.succeed();
    }

    /** 验证只有 1.20.1 对应的熔炼设备拥有菜单，浇注和流体附件不会被统一界面接管。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void menuScreenClassification(GameTestHelper helper) {
        // 放置全部需要区分交互方式的设备，直接检查服务端分类结果。
        BlockPos melterPos = new BlockPos(0, 0, 0);
        BlockPos heaterPos = new BlockPos(1, 0, 0);
        BlockPos alloyerPos = new BlockPos(2, 0, 0);
        BlockPos tablePos = new BlockPos(3, 0, 0);
        BlockPos tankPos = new BlockPos(4, 0, 0);
        BlockPos gaugePos = new BlockPos(5, 0, 0);
        helper.setBlock(melterPos, TFBlocks.MELTER.get());
        helper.setBlock(heaterPos, TFBlocks.HEATER.get());
        helper.setBlock(alloyerPos, TFBlocks.ALLOYER.get());
        helper.setBlock(tablePos, TFBlocks.CASTING_TABLE.get());
        helper.setBlock(tankPos, TFBlocks.SEARED_TANK.get());
        helper.setBlock(gaugePos, TFBlocks.FLUID_GAUGE.get().defaultBlockState()
            .setValue(FoundryDirectionalBlock.FACING, Direction.EAST));

        // 三类真正的设备使用专用界面分类，其余方块必须返回无界面分类。
        FoundryBlockEntity melter = helper.getBlockEntity(melterPos);
        FoundryBlockEntity heater = helper.getBlockEntity(heaterPos);
        FoundryBlockEntity alloyer = helper.getBlockEntity(alloyerPos);
        FoundryBlockEntity table = helper.getBlockEntity(tablePos);
        FoundryBlockEntity tank = helper.getBlockEntity(tankPos);
        FoundryBlockEntity gauge = helper.getBlockEntity(gaugePos);
        helper.assertTrue(melter.hasMenuScreen(), "melter should have a menu screen");
        helper.assertTrue(heater.hasMenuScreen(), "heater should have a menu screen");
        helper.assertTrue(alloyer.hasMenuScreen(), "alloyer should have a menu screen");
        helper.assertValueEqual(melter.screenKind(), 0, "melter screen kind");
        helper.assertValueEqual(heater.screenKind(), 2, "heater screen kind");
        helper.assertValueEqual(alloyer.screenKind(), 1, "alloyer screen kind");
        helper.assertFalse(table.hasMenuScreen(), "casting table should not have a menu screen");
        helper.assertFalse(tank.hasMenuScreen(), "tank should not have a menu screen");
        helper.assertFalse(gauge.hasMenuScreen(), "fluid gauge should not have a menu screen");
        // 流体计朝向东侧时读取西侧储罐，并且只保留贴壁薄片碰撞箱。
        helper.assertTrue(helper.getBlockState(gaugePos).canSurvive(
            helper.getLevel(), helper.absolutePos(gaugePos)),
            "fluid gauge should attach to the adjacent tank");
        helper.assertValueEqual(gauge.getDisplayCapacity(), tank.getTankCapacity(0),
            "fluid gauge should read the attached tank capacity");
        helper.succeed();
    }

    /** 验证设备方块实体注册、熔融流体类型隔离和默认容量。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void fluidCapacity(GameTestHelper helper) {
        // 在空测试结构中放置一个独立模组的熔炼器，并确认方块实体能被创建。
        BlockPos pos = BlockPos.ZERO;
        helper.setBlock(pos, TFBlocks.MELTER.get());
        helper.assertBlockPresent(TFBlocks.MELTER.get(), pos);
        FoundryBlockEntity entity = helper.getBlockEntity(pos);

        // 执行注入后检查容量、流体数量和不同流体之间的匹配限制。
        FluidStack iron = new FluidStack(TFFluids.IRON.get(), 1200);
        int accepted = entity.fill(iron, FluidAction.EXECUTE);
        helper.assertValueEqual(accepted, 1200, "accepted molten iron amount");
        helper.assertValueEqual(entity.getFluidInTank(0).getAmount(), 1200, "stored molten iron amount");
        helper.assertValueEqual(entity.getTankCapacity(0), FoundryBlockEntity.DEFAULT_CAPACITY, "melter capacity");
        helper.assertValueEqual(entity.fill(new FluidStack(TFFluids.IRON.get(), 4000), FluidAction.SIMULATE), 2800, "remaining capacity");
        helper.assertValueEqual(entity.fill(new FluidStack(TFFluids.GOLD.get(), 1), FluidAction.SIMULATE), 0, "different fluid rejection");
        helper.succeed();
    }

    /** 验证 1.21.1 配方 Codec 能在带注册表上下文的 JSON 中往返熔融流体。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void meltingRecipeCodec(GameTestHelper helper) {
        // 构造一个仅用于验证 Codec 的最小熔炼配方，不写入或修改运行时配方注册表。
        MeltingRecipe expected = new MeltingRecipe(
            Ingredient.of(Items.IRON_INGOT),
            new FluidStack(TFFluids.IRON.get(), FluidValues.INGOT),
            1000,
            60
        );
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());

        // 使用当前世界的注册表上下文编码和解码，确保自定义流体不会退化成未注册值。
        JsonElement encoded = MeltingRecipe.CODEC.codec().encodeStart(ops, expected).getOrThrow();
        MeltingRecipe decoded = MeltingRecipe.CODEC.codec().parse(ops, encoded).getOrThrow();
        helper.assertTrue(decoded.ingredient().test(new ItemStack(Items.IRON_INGOT)), "decoded ingredient does not match");
        helper.assertValueEqual(decoded.result().getAmount(), FluidValues.INGOT, "decoded fluid amount");
        helper.assertTrue(decoded.result().is(TFFluids.IRON.get()), "decoded fluid does not match molten iron");
        helper.assertValueEqual(decoded.temperature(), expected.temperature(), "decoded temperature");
        helper.assertValueEqual(decoded.time(), expected.time(), "decoded processing time");
        helper.succeed();
    }

    /** 验证 RecipeManager 能实际读取并匹配可重复铸模与一次性砂模配方。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void castingAndMoldingRecipesMatch(GameTestHelper helper) {
        // 使用已注册的熔融铁和可重复锭铸模构造 casting 查询输入。
        FluidStack moltenIron = new FluidStack(TFFluids.IRON.get(), FluidValues.INGOT);
        FluidRecipeInput reusableCastInput = new FluidRecipeInput(
            List.of(moltenIron),
            new ItemStack(TFItems.INGOT_CAST.get())
        );
        Optional<RecipeHolder<CastingRecipe>> casting = helper.getLevel().getRecipeManager().getRecipeFor(
            TFRecipes.CASTING.get(), reusableCastInput, helper.getLevel()
        );

        // 断言可重复铸模配方确实被读取，并且输入模具与输出铁锭都匹配。
        helper.assertTrue(casting.isPresent(), "missing reusable ingot casting recipe");
        helper.assertTrue(casting.get().value().matches(reusableCastInput, helper.getLevel()), "reusable casting recipe does not match");
        helper.assertTrue(casting.get().value().mold().isPresent(), "casting recipe lost reusable mold");
        helper.assertTrue(casting.get().value().mold().get().test(new ItemStack(TFItems.INGOT_CAST.get())), "reusable mold does not match");
        helper.assertTrue(casting.get().value().result().is(Items.IRON_INGOT), "casting result is not iron ingot");
        helper.assertValueEqual(casting.get().value().result().getCount(), 1, "casting result count");

        // 使用已注册的一次性锭砂模构造 molding 查询输入。
        FluidRecipeInput sandCastInput = new FluidRecipeInput(
            List.of(moltenIron),
            new ItemStack(TFItems.INGOT_SAND_CAST.get())
        );
        Optional<RecipeHolder<MoldingRecipe>> molding = helper.getLevel().getRecipeManager().getRecipeFor(
            TFRecipes.MOLDING.get(), sandCastInput, helper.getLevel()
        );

        // 断言一次性砂模配方确实被读取，并且砂模、熔融铁和输出铁锭都匹配。
        helper.assertTrue(molding.isPresent(), "missing single-use sand casting recipe");
        helper.assertTrue(molding.get().value().matches(sandCastInput, helper.getLevel()), "sand molding recipe does not match");
        helper.assertTrue(molding.get().value().mold().test(new ItemStack(TFItems.INGOT_SAND_CAST.get())), "sand mold does not match");
        helper.assertTrue(molding.get().value().fluid().test(moltenIron), "molding fluid does not match molten iron");
        helper.assertTrue(molding.get().value().result().is(Items.IRON_INGOT), "molding result is not iron ingot");
        helper.assertValueEqual(molding.get().value().result().getCount(), 1, "molding result count");
        helper.succeed();
    }

    /** 验证排液口、浇注口和浇注盆能够组成服务端权威的完整传输链。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 120)
    public static void drainFaucetCastingChain(GameTestHelper helper) {
        // 摆放熔炼器、排液口、浇注口和浇注盆，使用固定相邻关系避免依赖方块朝向。
        BlockPos sourcePos = new BlockPos(0, 3, 0);
        BlockPos drainPos = new BlockPos(0, 2, 0);
        BlockPos faucetPos = new BlockPos(0, 1, 0);
        BlockPos basinPos = new BlockPos(0, 0, 0);
        helper.setBlock(sourcePos, TFBlocks.MELTER.get());
        helper.setBlock(drainPos, TFBlocks.DRAIN.get().defaultBlockState().setValue(FoundryDirectionalBlock.FACING, Direction.UP));
        helper.setBlock(faucetPos, TFBlocks.FAUCET.get().defaultBlockState().setValue(FoundryDirectionalBlock.FACING, Direction.DOWN));
        helper.setBlock(basinPos, TFBlocks.CASTING_BASIN.get());

        // 为浇注盆放入可重复铸模，并向熔炼器注入一份熔融铁作为传输源。
        FoundryBlockEntity source = helper.getBlockEntity(sourcePos);
        FoundryBlockEntity basin = helper.getBlockEntity(basinPos);
        basin.setItem(0, new ItemStack(TFItems.INGOT_CAST.get()));
        source.fill(new FluidStack(TFFluids.IRON.get(), FluidValues.BUCKET), FluidAction.EXECUTE);

        // 等待传输和冷却完成，确认源流体被搬运并产出铁锭。
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(source.getFluidInTank(0).getAmount() < FluidValues.BUCKET, "drain did not pull source fluid");
            helper.assertTrue(basin.getFluidInTank(0).getAmount() < FluidValues.BUCKET,
                "casting basin did not consume fluid: amount=" + basin.getFluidInTank(0).getAmount()
                    + ", progress=" + basin.progress() + ", processTime=" + basin.processTime()
                    + ", input=" + basin.getItem(0).getItem().toString());
            helper.assertTrue(basin.getItem(FoundryBlockEntity.OUTPUT_SLOT).is(Items.IRON_INGOT), "faucet casting did not produce an iron ingot");
            helper.succeed();
        });
    }

    /** 验证非固定尺寸的封闭矩形冶炼炉，以及两个物品输入槽的并行熔炼。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 150)
    public static void variableSmelteryParallelMelting(GameTestHelper helper) {
        // 构造四乘四乘四的侧壁控制器结构，炉腔内部为两乘两乘二。
        BlockPos controllerPos = new BlockPos(0, 0, 0);
        for (int y = 0; y <= 3; y++) {
            for (int x = -1; x <= 2; x++) {
                for (int z = 0; z <= 3; z++) {
                    // 顶部只保留一圈侧壁，中心保持空气，模拟 1.20.1 官方开放式冶炼炉。
                    boolean boundary = x == -1 || x == 2 || z == 0 || z == 3 || y == 0
                        || (y == 3 && (x == -1 || x == 2 || z == 0 || z == 3));
                    if (boundary) {
                        helper.setBlock(new BlockPos(x, y, z), TFBlocks.SEARED_BRICK.get());
                    } else {
                        // 官方冶炼炉的炉腔必须保持空气，储罐应放在墙体位置而不是炉腔内部。
                        helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
                    }
                }
            }
        }
        helper.setBlock(controllerPos, TFBlocks.SMELTERY_CONTROLLER.get().defaultBlockState().setValue(FoundryControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(new BlockPos(1, 0, 0), TFBlocks.SEARED_FUEL_TANK.get());
        FoundryBlockEntity controller = helper.getBlockEntity(controllerPos);
        // 直接确认炉腔中心为空气，防止测试再次把储罐误放进官方不允许的内部区域。
        helper.assertTrue(helper.getBlockState(new BlockPos(0, 1, 1)).isAir(), "smeltery interior must be air");
        helper.assertTrue(SmelteryMultiblock.validate(helper.getLevel(), helper.absolutePos(controllerPos)).valid(), "variable smeltery structure was rejected");
        controller.setItem(0, new ItemStack(Items.IRON_INGOT));
        controller.setItem(1, new ItemStack(Items.IRON_INGOT));
        FoundryBlockEntity structureFuel = helper.getBlockEntity(new BlockPos(1, 0, 0));
        structureFuel.fill(new FluidStack(Fluids.LAVA, FluidValues.BUCKET), FluidAction.EXECUTE);

        // 等待结构校验和两个独立进度同时完成，确认结果数量为两个锭的流体值。
        helper.runAfterDelay(100, () -> {
            // 开放顶部的侧壁环下方仍属于炉腔，四乘四外壳对应十二个内部方块，十二个槽位都应启用。
            helper.assertValueEqual(controller.inputSlotCount(), 12, "variable smeltery input slot count");
            helper.assertTrue(controller.isStructureValid(), "variable smeltery became invalid during processing");
            helper.assertTrue(controller.getFluidInTank(0).is(TFFluids.IRON.get()), "parallel melting did not produce molten iron");
            helper.assertValueEqual(controller.getFluidInTank(0).getAmount(), FluidValues.INGOT * 2, "parallel melting output amount");
            helper.assertTrue(controller.getItem(0).isEmpty() && controller.getItem(1).isEmpty(), "parallel melting left input items");
            helper.succeed();
        });
    }

    /** 验证铸造炉使用独立封闭炉腔，并且不再把固定三乘三结构当作唯一尺寸。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 60)
    public static void variableFoundryStructure(GameTestHelper helper) {
        // 构造三乘三乘三的灼热炉侧壁结构，控制器位于正面外壳。
        BlockPos controllerPos = new BlockPos(0, 0, 0);
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = 0; z <= 2; z++) {
                    // 铸造炉同样只需要开放顶部的侧壁环，不应把炉腔中心封成焦黑方块。
                    boolean boundary = x == -1 || x == 1 || z == 0 || z == 2 || y == 0
                        || (y == 2 && (x == -1 || x == 1 || z == 0 || z == 2));
                    if (boundary) {
                        helper.setBlock(new BlockPos(x, y, z), TFBlocks.SCORCHED_BRICK.get());
                    } else {
                        // 官方铸造炉的炉腔必须保持空气，内部放置外壳方块会被判定为无效结构。
                        helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
                    }
                }
            }
        }
        helper.setBlock(controllerPos, TFBlocks.FOUNDRY_CONTROLLER.get().defaultBlockState().setValue(FoundryControllerBlock.FACING, Direction.NORTH));

        // 等待一次服务端结构检查，确认铸造炉容量和腔体统计已建立。
        helper.runAfterDelay(8, () -> {
            FoundryBlockEntity controller = helper.getBlockEntity(controllerPos);
            helper.assertTrue(controller.isStructureValid(), "variable foundry structure was rejected");
            helper.assertTrue(controller.structureCapacity() > FoundryMultiblock.MINIMUM_CAPACITY, "foundry capacity did not use shell size");
            // 三乘三铸造炉的开放炉腔有上下两个内部层，输入槽数量应随结构容量变为两个。
            helper.assertValueEqual(controller.inputSlotCount(), 2, "foundry input slot count");
            // 临时移除一块炉壁，确认结构变化会让控制器失效而不是继续沿用旧缓存。
            helper.setBlock(new BlockPos(-1, 1, 1), net.minecraft.world.level.block.Blocks.AIR);
            controller.markStructureDirty();
            helper.runAfterDelay(8, () -> {
                helper.assertFalse(controller.isStructureValid(), "broken foundry structure remained valid");
                helper.assertValueEqual(controller.structureErrorPos(), helper.absolutePos(new BlockPos(-1, 1, 1)), "foundry error position");
                helper.assertValueEqual(controller.structureErrorReason(), StructureErrorReason.INVALID_WALL_BLOCK,
                    "foundry error reason");
                // 恢复炉壁后再次等待校验，确认结构可以重新激活。
                helper.setBlock(new BlockPos(-1, 1, 1), TFBlocks.SCORCHED_BRICK.get());
                controller.markStructureDirty();
                helper.runAfterDelay(8, () -> {
                    helper.assertTrue(controller.isStructureValid(), "restored foundry structure remained invalid");
                    helper.assertTrue(controller.structureErrorPos() == null, "restored foundry error position");
                    helper.succeed();
                });
            });
        });
    }

    /** 验证加热器能够读取流体燃料配方，并为相邻熔炼器提供热量。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 140)
    public static void fluidFuelHeatsMelter(GameTestHelper helper) {
        // 摆放相邻熔炼器和加热器，熔炼器输入铁锭，加热器输入一桶原版熔岩。
        BlockPos melterPos = new BlockPos(0, 0, 0);
        BlockPos heaterPos = new BlockPos(1, 0, 0);
        helper.setBlock(melterPos, TFBlocks.MELTER.get());
        helper.setBlock(heaterPos, TFBlocks.HEATER.get());
        FoundryBlockEntity melter = helper.getBlockEntity(melterPos);
        FoundryBlockEntity heater = helper.getBlockEntity(heaterPos);
        melter.setItem(0, new ItemStack(Items.IRON_INGOT));
        helper.assertValueEqual(heater.fill(new FluidStack(Fluids.LAVA, FluidValues.BUCKET), FluidAction.EXECUTE), FluidValues.BUCKET, "lava fuel fill amount");

        // 等待自定义流体燃料配方启动并完成铁锭熔炼。
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(melter.getFluidInTank(0).is(TFFluids.IRON.get()), "fluid fuel did not heat the melter");
            helper.assertValueEqual(melter.getFluidInTank(0).getAmount(), FluidValues.INGOT, "molten iron output amount");
            helper.assertTrue(heater.getFluidInTank(0).getAmount() > 0 && heater.getFluidInTank(0).getAmount() < FluidValues.BUCKET,
                "fluid fuel was not consumed continuously");
            helper.succeed();
        });
    }

    /** 验证加热器能从相邻专用燃料罐取用流体燃料，而不要求玩家先手动倒入加热器。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 160)
    public static void fuelTankFeedsHeater(GameTestHelper helper) {
        // 让专用燃料罐、加热器和熔炼器组成一条相邻燃料链。
        BlockPos melterPos = new BlockPos(0, 0, 0);
        BlockPos heaterPos = new BlockPos(1, 0, 0);
        BlockPos fuelTankPos = new BlockPos(2, 0, 0);
        helper.setBlock(melterPos, TFBlocks.MELTER.get());
        helper.setBlock(heaterPos, TFBlocks.HEATER.get());
        helper.setBlock(fuelTankPos, TFBlocks.SEARED_FUEL_TANK.get());
        FoundryBlockEntity melter = helper.getBlockEntity(melterPos);
        FoundryBlockEntity fuelTank = helper.getBlockEntity(fuelTankPos);
        melter.setItem(0, new ItemStack(Items.IRON_INGOT));
        fuelTank.fill(new FluidStack(Fluids.LAVA, FluidValues.BUCKET), FluidAction.EXECUTE);

        // 熔炼器请求热量后，加热器应自动抽取燃料罐内容并完成熔炼。
        helper.runAfterDelay(130, () -> {
            helper.assertTrue(melter.getFluidInTank(0).is(TFFluids.IRON.get()), "fuel tank did not feed heater");
            helper.assertTrue(fuelTank.getFluidInTank(0).getAmount() < FluidValues.BUCKET, "fuel tank fluid was not consumed");
            helper.succeed();
        });
    }

    /** 验证合金炉能够按上游基线消耗两个流体输入并生成青铜。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 280)
    public static void alloyerConsumesInputs(GameTestHelper helper) {
        // 摆放合金炉和相邻加热器，使用熔融铜与熔融锡组成青铜。
        BlockPos alloyerPos = new BlockPos(0, 0, 0);
        BlockPos heaterPos = new BlockPos(1, 0, 0);
        helper.setBlock(alloyerPos, TFBlocks.ALLOYER.get());
        helper.setBlock(heaterPos, TFBlocks.HEATER.get());
        FoundryBlockEntity alloyer = helper.getBlockEntity(alloyerPos);
        FoundryBlockEntity heater = helper.getBlockEntity(heaterPos);
        helper.assertValueEqual(alloyer.getTanks(), FoundryBlockEntity.MAX_ALLOY_INPUTS + 1, "alloyer tank count");
        helper.assertValueEqual(alloyer.fill(new FluidStack(TFFluids.COPPER.get(), 540), FluidAction.EXECUTE), 540,
            "alloyer copper input amount");
        helper.assertValueEqual(alloyer.fill(new FluidStack(TFFluids.TIN.get(), 180), FluidAction.EXECUTE), 180,
            "alloyer tin input amount");
        helper.assertValueEqual(heater.fill(new FluidStack(Fluids.LAVA, FluidValues.BUCKET), FluidAction.EXECUTE), FluidValues.BUCKET,
            "alloyer lava fuel amount");

        // 等待两个合金周期，确认同种输出可以连续累积而不会被第一周期卡住。
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(alloyer.getFluidInTank(FoundryBlockEntity.ALLOY_OUTPUT_TANK).is(TFFluids.BRONZE.get()), "alloyer did not produce molten bronze");
            helper.assertValueEqual(alloyer.getFluidInTank(FoundryBlockEntity.ALLOY_OUTPUT_TANK).getAmount(), 720, "molten bronze output amount");
            helper.assertTrue(alloyer.getFluidInTank(0).isEmpty(), "alloyer copper input was not consumed");
            helper.assertTrue(alloyer.getFluidInTank(1).isEmpty(), "alloyer tin input was not consumed");
            helper.succeed();
        });
    }

    /** 验证独立金属产物拥有完整注册项，并且锡的锭、块、普通砂模和红砂模配方都能读取。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void internalMetalRecipeChain(GameTestHelper helper) {
        // 先确认 12 种非原版产物全部注册，避免只迁移铁、金、铜造成配方链断裂。
        for (String metal : TFBlocks.INTERNAL_METALS) {
            helper.assertTrue(TFItems.METAL_INGOTS.containsKey(metal), "missing internal ingot registration: " + metal);
            helper.assertTrue(TFItems.METAL_NUGGETS.containsKey(metal), "missing internal nugget registration: " + metal);
            helper.assertTrue(TFItems.METAL_BLOCKS.containsKey(metal), "missing internal block registration: " + metal);
        }

        // 用锡验证不依赖原版物品的完整浇注链和一次性红砂模路径。
        FluidStack moltenTin = new FluidStack(TFFluids.TIN.get(), FluidValues.BLOCK);
        FluidRecipeInput blockInput = new FluidRecipeInput(List.of(moltenTin));
        Optional<RecipeHolder<CastingRecipe>> blockRecipe = helper.getLevel().getRecipeManager().getRecipeFor(
            TFRecipes.CASTING.get(), blockInput, helper.getLevel());
        helper.assertTrue(blockRecipe.isPresent(), "missing internal metal block casting recipe");
        helper.assertTrue(blockRecipe.get().value().result().is(TFItems.METAL_BLOCKS.get("tin").get()), "tin block casting result mismatch");

        FluidRecipeInput redSandInput = new FluidRecipeInput(List.of(new FluidStack(TFFluids.TIN.get(), FluidValues.INGOT)),
            new ItemStack(TFItems.INGOT_RED_SAND_CAST.get()));
        Optional<RecipeHolder<MoldingRecipe>> redSandRecipe = helper.getLevel().getRecipeManager().getRecipeFor(
            TFRecipes.MOLDING.get(), redSandInput, helper.getLevel());
        helper.assertTrue(redSandRecipe.isPresent(), "missing internal metal red sand mold recipe");
        helper.assertTrue(redSandRecipe.get().value().result().is(TFItems.METAL_INGOTS.get("tin").get()), "tin ingot molding result mismatch");
        helper.succeed();
    }

    /** 验证专用储液罐的容量隔离，并允许燃料罐接受原版熔岩。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void dedicatedTankCapacities(GameTestHelper helper) {
        // 三类储液罐使用独立容量，燃料罐不再被普通熔融金属流体限制。
        BlockPos ingotPos = new BlockPos(0, 0, 0);
        BlockPos fuelPos = new BlockPos(1, 0, 0);
        BlockPos castingPos = new BlockPos(2, 0, 0);
        helper.setBlock(ingotPos, TFBlocks.SEARED_TANK.get());
        helper.setBlock(fuelPos, TFBlocks.SEARED_FUEL_TANK.get());
        helper.setBlock(castingPos, TFBlocks.SEARED_CASTING_TANK.get());
        FoundryBlockEntity ingotTank = helper.getBlockEntity(ingotPos);
        FoundryBlockEntity fuelTank = helper.getBlockEntity(fuelPos);
        FoundryBlockEntity castingTank = helper.getBlockEntity(castingPos);
        helper.assertValueEqual(ingotTank.getTankCapacity(0), FluidValues.INGOT * 48, "ingot tank capacity");
        helper.assertValueEqual(fuelTank.getTankCapacity(0), FoundryBlockEntity.DEFAULT_CAPACITY, "fuel tank capacity");
        helper.assertValueEqual(castingTank.getTankCapacity(0), FluidValues.BUCKET, "casting tank capacity");
        helper.assertValueEqual(fuelTank.fill(new FluidStack(Fluids.LAVA, FluidValues.BUCKET), FluidAction.EXECUTE), FluidValues.BUCKET,
            "fuel tank did not accept lava");
        helper.succeed();
    }

    /** 验证浇注储液罐可以自动处理桶和便携储液罐，并且转换失败时不扣除流体。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void castingTankProcessesContainers(GameTestHelper helper) {
        // 先验证空桶从浇注储液罐取出一桶熔融铁，转换结果进入输出槽。
        BlockPos pos = BlockPos.ZERO;
        helper.setBlock(pos, TFBlocks.SEARED_CASTING_TANK.get());
        FoundryBlockEntity tank = helper.getBlockEntity(pos);
        tank.fill(new FluidStack(TFFluids.IRON.get(), FluidValues.BUCKET), FluidAction.EXECUTE);
        tank.setItem(0, new ItemStack(Items.BUCKET));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tank.getItem(FoundryBlockEntity.OUTPUT_SLOT).is(TFItems.IRON_BUCKET.get()),
                "casting tank did not fill an empty bucket");
            helper.assertValueEqual(tank.getFluidInTank(0).getAmount(), 0, "casting tank kept drained fluid");
            tank.setItem(FoundryBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
            tank.setItem(0, new ItemStack(TFItems.IRON_BUCKET.get()));
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(tank.getItem(FoundryBlockEntity.OUTPUT_SLOT).is(Items.BUCKET),
                    "casting tank did not empty a molten bucket");
                helper.assertValueEqual(tank.getFluidInTank(0).getAmount(), FluidValues.BUCKET,
                    "casting tank did not receive molten bucket fluid");
                helper.succeed();
            });
        });
    }

    /** 验证专用储液罐放置、取块和物品组件读写均保留流体。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void dedicatedTankItemRetention(GameTestHelper helper) {
        // 直接走当前 1.21.1 Block.setPlacedBy 和 getCloneItemStack API，覆盖存档外的物品往返路径。
        BlockPos pos = BlockPos.ZERO;
        ItemStack filled = new ItemStack(TFItems.SEARED_TANK.get());
        org.hp.tinker_foundry.item.FoundryTankItem.setFluid(filled, new FluidStack(TFFluids.COPPER.get(), FluidValues.INGOT));
        helper.assertValueEqual(((org.hp.tinker_foundry.item.FoundryTankItem) filled.getItem()).getFluid(filled).getAmount(),
            FluidValues.INGOT, "filled tank item did not store fluid component");
        helper.setBlock(pos, TFBlocks.SEARED_TANK.get());
        BlockPos absolutePos = helper.absolutePos(pos);
        TFBlocks.SEARED_TANK.get().setPlacedBy(helper.getLevel(), absolutePos, helper.getLevel().getBlockState(absolutePos), null, filled);
        FoundryBlockEntity tank = helper.getBlockEntity(pos);
        helper.assertValueEqual(tank.getFluidInTank(0).getAmount(), FluidValues.INGOT, "filled tank lost fluid on placement");
        ItemStack clone = TFBlocks.SEARED_TANK.get().getCloneItemStack(helper.getLevel(), absolutePos, helper.getLevel().getBlockState(absolutePos));
        helper.assertValueEqual(((org.hp.tinker_foundry.item.FoundryTankItem) clone.getItem()).getFluid(clone).getAmount(),
            FluidValues.INGOT, "clone tank lost fluid component");
        helper.succeed();
    }

    /** 验证容器型模具完成后返还物品，并且设备存档重载不会丢失流体与物品。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void moldingRemainderAndSaveReload(GameTestHelper helper) {
        // 铜罐作为容器模具输入，完成后必须在独立返还槽得到铜罐。
        BlockPos pos = BlockPos.ZERO;
        helper.setBlock(pos, TFBlocks.CASTING_TABLE.get());
        FoundryBlockEntity table = helper.getBlockEntity(pos);
        table.setItem(0, new ItemStack(TFItems.COPPER_CANISTER.get()));
        table.fill(new FluidStack(TFFluids.IRON.get(), FluidValues.INGOT), FluidAction.EXECUTE);
        helper.runAfterDelay(70, () -> {
            helper.assertTrue(table.getItem(FoundryBlockEntity.OUTPUT_SLOT).is(Items.IRON_INGOT), "container molding did not create iron ingot");
            helper.assertTrue(table.getItem(FoundryBlockEntity.REMAINDER_SLOT).is(TFItems.COPPER_CANISTER.get()), "container mold remainder was lost");
            helper.setBlock(new BlockPos(2, 0, 0), TFBlocks.MELTER.get());
            FoundryBlockEntity melter = helper.getBlockEntity(new BlockPos(2, 0, 0));
            melter.fill(new FluidStack(TFFluids.IRON.get(), FluidValues.INGOT * 2), FluidAction.EXECUTE);
            CompoundTag saved = table.saveWithoutMetadata(helper.getLevel().registryAccess());
            CompoundTag savedFluid = melter.saveWithoutMetadata(helper.getLevel().registryAccess());
            FoundryBlockEntity restored = new FoundryBlockEntity(TFBlockEntities.GENERIC.get(), new BlockPos(4, 0, 0),
                TFBlocks.CASTING_TABLE.get().defaultBlockState());
            restored.loadCustomOnly(saved, helper.getLevel().registryAccess());
            helper.assertTrue(restored.getItem(FoundryBlockEntity.REMAINDER_SLOT).is(TFItems.COPPER_CANISTER.get()), "remainder changed after reload");
            FoundryBlockEntity restoredFluid = new FoundryBlockEntity(TFBlockEntities.GENERIC.get(), new BlockPos(5, 0, 0),
                TFBlocks.MELTER.get().defaultBlockState());
            restoredFluid.loadCustomOnly(savedFluid, helper.getLevel().registryAccess());
            helper.assertValueEqual(restoredFluid.getFluidInTank(0).getAmount(), FluidValues.INGOT * 2, "fluid amount changed after reload");
            helper.succeed();
        });
    }

    /** 验证输出槽满时浇注不会扣液或消耗模具，覆盖服务端保护分支。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void castingOutputFullProtection(GameTestHelper helper) {
        // 预先填满铁锭输出槽，设备必须等待空间而不是丢失流体和模具。
        BlockPos pos = BlockPos.ZERO;
        helper.setBlock(pos, TFBlocks.CASTING_TABLE.get());
        FoundryBlockEntity table = helper.getBlockEntity(pos);
        table.setItem(0, new ItemStack(TFItems.INGOT_CAST.get()));
        table.setItem(FoundryBlockEntity.OUTPUT_SLOT, new ItemStack(Items.IRON_INGOT, 64));
        table.fill(new FluidStack(TFFluids.IRON.get(), FluidValues.INGOT), FluidAction.EXECUTE);
        helper.runAfterDelay(70, () -> {
            helper.assertValueEqual(table.getFluidInTank(0).getAmount(), FluidValues.INGOT, "full output consumed molten iron");
            helper.assertTrue(table.getItem(0).is(TFItems.INGOT_CAST.get()), "full output consumed reusable cast");
            helper.assertValueEqual(table.getItem(FoundryBlockEntity.OUTPUT_SLOT).getCount(), 64, "full output stack changed");
            helper.succeed();
        });
    }

    /** 验证合金配方支持流体催化输入，且催化标记能通过独立 Codec 往返。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void alloyingCatalystCodec(GameTestHelper helper) {
        // 使用独立命名空间的铜、锡流体构造带催化标记的合金配方 JSON。
        JsonElement json = JsonParser.parseString("""
            {
              "ingredients": [
                {"ingredient": {"fluid": "tinker_foundry:copper"}, "amount": 90, "catalyst": true},
                {"ingredient": {"fluid": "tinker_foundry:tin"}, "amount": 90}
              ],
              "result": {"id": "tinker_foundry:bronze", "amount": 180},
              "temperature": 700
            }
            """);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());

        // 解析后确认催化槽参与匹配，但该字段不会退化成物品催化剂。
        AlloyingRecipe recipe = AlloyingRecipe.CODEC.codec().parse(ops, json).getOrThrow();
        helper.assertValueEqual(recipe.ingredients().size(), 2, "alloy ingredient count");
        helper.assertTrue(recipe.ingredients().get(0).catalyst(), "alloy catalyst flag was lost");
        helper.assertTrue(recipe.matches(new FluidRecipeInput(List.of(
            new FluidStack(TFFluids.COPPER.get(), 90), new FluidStack(TFFluids.TIN.get(), 90)
        )), helper.getLevel()), "catalyst alloy recipe does not match fluid inputs");
        helper.succeed();
    }

    /** 验证公开合金配方 API 可以实际匹配三种流体，而不仅是保存两个输入槽。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void alloyingThreeInputCodec(GameTestHelper helper) {
        // 通过独立 Codec 构造三输入配方，确保三元输入在网络和数据文件接口中保持可用。
        JsonElement json = JsonParser.parseString("""
            {
              "ingredients": [
                {"ingredient": {"fluid": "tinker_foundry:copper"}, "amount": 90},
                {"ingredient": {"fluid": "tinker_foundry:tin"}, "amount": 90},
                {"ingredient": {"fluid": "tinker_foundry:gold"}, "amount": 90}
              ],
              "result": {"id": "tinker_foundry:brass", "amount": 270},
              "temperature": 900
            }
            """);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        AlloyingRecipe recipe = AlloyingRecipe.CODEC.codec().parse(ops, json).getOrThrow();
        helper.assertValueEqual(recipe.ingredients().size(), 3, "three-input alloy recipe was truncated");
        helper.assertTrue(recipe.matches(new FluidRecipeInput(List.of(
            new FluidStack(TFFluids.COPPER.get(), 90), new FluidStack(TFFluids.TIN.get(), 90), new FluidStack(TFFluids.GOLD.get(), 90)
        )), helper.getLevel()), "three-input alloy recipe does not match");
        helper.succeed();
    }

    /** 检验共享容量、模拟不变、指定类型抽取与选择顺序，不依赖客户端渲染。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void sharedMultiFluidStorage(GameTestHelper helper) {
        var tank = new org.hp.tinker_foundry.common.StructureFluidTank();
        FluidStack iron = new FluidStack(TFFluids.IRON.get(), 900);
        FluidStack gold = new FluidStack(TFFluids.GOLD.get(), 900);
        helper.assertValueEqual(tank.fill(iron, 1000, FluidAction.SIMULATE), 900, "simulated fill");
        helper.assertValueEqual(tank.amount(), 0, "simulation mutated tank");
        tank.fill(iron, 1000, FluidAction.EXECUTE);
        helper.assertValueEqual(tank.fill(gold, 1000, FluidAction.EXECUTE), 100, "shared capacity exceeded");
        helper.assertValueEqual(tank.amount(), 1000, "shared capacity total");
        helper.assertTrue(tank.select(1), "selection did not reorder layers");
        helper.assertTrue(tank.get(0).is(TFFluids.GOLD.get()), "selected layer not at bottom");
        tank.drain(0, 100, FluidAction.SIMULATE);
        helper.assertValueEqual(tank.amount(), 1000, "simulated drain mutated tank");
        tank.drain(0, 100, FluidAction.EXECUTE);
        helper.assertValueEqual(tank.size(), 1, "empty layer was retained");
        helper.assertValueEqual(tank.fill(gold, 500, FluidAction.EXECUTE), 0, "shrunk structure accepted overflow");
        helper.assertValueEqual(tank.amount(), 900, "shrinking deleted stored fluid");
        helper.succeed();
    }

    /** 检验真实冶炼炉内合金、多罐联合供料、排液口选择和新格式存档。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void structureAlloyFuelAndPersistence(GameTestHelper helper) {
        // 构造开放顶的小炉，三个燃料罐中两个各存半桶，第三个用于验证空罐容量汇总。
        for (int y = 0; y <= 2; y++) {
            for (int x = 0; x <= 3; x++) {
                for (int z = 0; z <= 3; z++) {
                    helper.setBlock(new BlockPos(x, y, z), y == 0 || x == 0 || x == 3 || z == 0 || z == 3
                        ? TFBlocks.SEARED_BRICK.get() : net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }
        BlockPos controllerPos = new BlockPos(1, 1, 0);
        helper.setBlock(controllerPos, TFBlocks.SMELTERY_CONTROLLER.get().defaultBlockState().setValue(FoundryControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(new BlockPos(0, 1, 1), TFBlocks.SEARED_FUEL_TANK.get());
        helper.setBlock(new BlockPos(0, 1, 2), TFBlocks.SEARED_FUEL_TANK.get());
        helper.setBlock(new BlockPos(3, 1, 1), TFBlocks.SEARED_FUEL_TANK.get());
        helper.setBlock(new BlockPos(3, 1, 2), TFBlocks.DRAIN.get());
        FoundryBlockEntity first = helper.getBlockEntity(new BlockPos(0, 1, 1));
        FoundryBlockEntity second = helper.getBlockEntity(new BlockPos(0, 1, 2));
        FoundryBlockEntity third = helper.getBlockEntity(new BlockPos(3, 1, 1));
        FoundryBlockEntity controller = helper.getBlockEntity(controllerPos);
        FoundryBlockEntity drain = helper.getBlockEntity(new BlockPos(3, 1, 2));
        controller.refreshStructureIfDirty();
        helper.assertTrue(controller.isStructureValid(), "test structure invalid");
        first.fill(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE);
        second.fill(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE);
        helper.assertValueEqual(controller.fuelDisplayFluid().getAmount(), 1000, "fuel amount not aggregated");
        helper.assertValueEqual(controller.fuelDisplayCapacity(), 12000, "empty tank capacity missing");
        // 不同流体不应被计入当前熔岩的数量与容量。
        third.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        helper.assertValueEqual(controller.fuelDisplayCapacity(), 8000, "different fuel capacity was mixed");
        third.drain(500, FluidAction.EXECUTE);
        helper.assertValueEqual(controller.drainStructureFuel(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE).getAmount(), 1000, "split fuel simulation failed");
        helper.assertValueEqual(first.getFluidInTank(0).getAmount(), 500, "fuel simulation mutated source");
        controller.fill(new FluidStack(TFFluids.COPPER.get(), 270), FluidAction.EXECUTE);
        controller.fill(new FluidStack(TFFluids.TIN.get(), 90), FluidAction.EXECUTE);
        controller.fill(new FluidStack(TFFluids.GOLD.get(), 180), FluidAction.EXECUTE);
        helper.runAfterDelay(15, () -> {
            List<FluidStack> layers = controller.structureFluidLayers();
            helper.assertValueEqual(layers.stream().mapToInt(FluidStack::getAmount).sum(), 540, "alloy volume changed");
            helper.assertTrue(layers.stream().anyMatch(stack -> stack.is(TFFluids.BRONZE.get()) && stack.getAmount() == 360), "in-place bronze alloy missing");
            helper.assertValueEqual(first.getFluidInTank(0).getAmount() + second.getFluidInTank(0).getAmount(), 0, "split tanks did not fuel alloying");
            // 再装另一罐时应自动显示新来源，旧空罐仍贡献容量。
            third.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
            helper.assertValueEqual(controller.fuelDisplayFluid().getAmount(), 1000, "empty first tank hid later fuel");
            helper.assertValueEqual(controller.fuelDisplayCapacity(), 12000, "aggregate capacity changed after switch");
            controller.selectStructureFluid(1);
            helper.assertTrue(drain.getFluidInTank(0).is(TFFluids.BRONZE.get()), "drain did not follow selected layer");
            helper.assertValueEqual(drain.drain(90, FluidAction.EXECUTE).getAmount(), 90, "drain extraction failed");
            CompoundTag saved = controller.saveWithoutMetadata(helper.getLevel().registryAccess());
            FoundryBlockEntity restored = new FoundryBlockEntity(controller.getBlockPos(), controller.getBlockState());
            restored.loadCustomOnly(saved, helper.getLevel().registryAccess());
            helper.assertValueEqual(restored.structureFluidLayers().size(), 2, "reload lost fluid layers");
            helper.assertTrue(restored.getFluidInTank(0).is(TFFluids.BRONZE.get()), "reload lost selected fluid order");
            helper.assertValueEqual(restored.getFluidInTank(0).getAmount(), 270, "reload changed selected amount");
            helper.succeed();
        });
    }

    /** 防止游戏测试类被误实例化，NeoForge 会直接调用静态测试方法。 */
    private FoundryGameTests() {
    }
}
