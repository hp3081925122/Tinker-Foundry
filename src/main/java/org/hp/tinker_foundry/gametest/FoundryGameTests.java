package org.hp.tinker_foundry.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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
import org.hp.tinker_foundry.block.FoundryHorizontalBlock;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.recipe.AlloyingRecipe;
import org.hp.tinker_foundry.recipe.FluidRecipeInput;
import org.hp.tinker_foundry.recipe.MeltingRecipe;
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
        FluidStack cobalt = new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), 1200);
        int accepted = entity.fill(cobalt, FluidAction.EXECUTE);
        helper.assertValueEqual(accepted, 1200, "accepted preserved fluid amount");
        helper.assertValueEqual(entity.getFluidInTank(0).getAmount(), 1200, "stored preserved fluid amount");
        helper.assertValueEqual(entity.getTankCapacity(0), FoundryBlockEntity.DEFAULT_CAPACITY, "melter capacity");
        helper.assertValueEqual(entity.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), 4000), FluidAction.SIMULATE), 2800, "remaining capacity");
        helper.assertValueEqual(entity.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("liquid_soul").get(), 1), FluidAction.SIMULATE), 0, "different fluid rejection");
        helper.succeed();
    }

    /** 验证 1.21.1 配方 Codec 能在带注册表上下文的 JSON 中往返熔融流体。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void meltingRecipeCodec(GameTestHelper helper) {
        // 构造一个仅用于验证 Codec 的最小熔炼配方，不写入或修改运行时配方注册表。
        MeltingRecipe expected = new MeltingRecipe(
            Ingredient.of(Items.IRON_INGOT),
            new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), FluidValues.INGOT),
            1000,
            60
        );
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());

        // 使用当前世界的注册表上下文编码和解码，确保自定义流体不会退化成未注册值。
        JsonElement encoded = MeltingRecipe.CODEC.codec().encodeStart(ops, expected).getOrThrow();
        MeltingRecipe decoded = MeltingRecipe.CODEC.codec().parse(ops, encoded).getOrThrow();
        helper.assertTrue(decoded.ingredient().test(new ItemStack(Items.IRON_INGOT)), "decoded ingredient does not match");
        helper.assertValueEqual(decoded.result().getAmount(), FluidValues.INGOT, "decoded fluid amount");
        helper.assertTrue(decoded.result().is(TFFluids.EXTRA_SOURCES.get("cobalt").get()), "decoded fluid does not match preserved fluid");
        helper.assertValueEqual(decoded.temperature(), expected.temperature(), "decoded temperature");
        helper.assertValueEqual(decoded.time(), expected.time(), "decoded processing time");
        helper.succeed();
    }

    /** 验证浇注和模具配方类型仍注册，但不再依赖已授权删除的金属流体配方。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void castingAndMoldingRecipeTypesRegistered(GameTestHelper helper) {
        // 删除金属流体配方后，浇注和模具的自定义配方类型仍应保留给后续附属内容使用。
        helper.assertTrue(TFRecipes.CASTING.get() != null, "casting recipe type is missing");
        helper.assertTrue(TFRecipes.MOLDING.get() != null, "molding recipe type is missing");
        helper.succeed();
    }

    /** 验证排液口、浇注口和浇注盆能够传输仍保留的副产物流体。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 120)
    public static void drainFaucetCastingChain(GameTestHelper helper) {
        // 摆放熔炼器、排液口、浇注口和浇注盆，按匠魂规则使用水平端口朝向。
        BlockPos sourcePos = new BlockPos(0, 0, 0);
        BlockPos drainPos = new BlockPos(1, 0, 0);
        BlockPos faucetPos = new BlockPos(2, 0, 0);
        // 浇注口的输出端固定在下方，盆必须放在浇注口正下方而不是朝向侧面。
        BlockPos basinPos = new BlockPos(2, -1, 0);
        helper.setBlock(sourcePos, TFBlocks.MELTER.get());
        helper.setBlock(drainPos, TFBlocks.DRAIN.get().defaultBlockState().setValue(FoundryHorizontalBlock.FACING, Direction.WEST));
        helper.setBlock(faucetPos, TFBlocks.FAUCET.get().defaultBlockState().setValue(FoundryDirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(basinPos, TFBlocks.CASTING_BASIN.get());

        // 向熔炼器注入一份仍保留的副产物流体作为传输源。
        FoundryBlockEntity source = helper.getBlockEntity(sourcePos);
        FoundryBlockEntity basin = helper.getBlockEntity(basinPos);
        FoundryBlockEntity faucet = helper.getBlockEntity(faucetPos);
        source.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), FluidValues.BUCKET), FluidAction.EXECUTE);
        // 浇注口不是常驻自动泵，按匠魂交互先手动启动一次，再由服务端逐 tick 输出。
        faucet.activateFaucet();

        // 等待传输，确认源流体被搬运到浇注盆，不再断言已经删除的金属浇注产物。
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(source.getFluidInTank(0).getAmount() < FluidValues.BUCKET, "drain did not pull source fluid");
            helper.assertTrue(basin.getFluidInTank(0).is(TFFluids.EXTRA_SOURCES.get("cobalt").get()), "casting basin did not receive preserved fluid");
            helper.succeed();
        });
    }

    /** 验证非固定尺寸的封闭矩形冶炼炉，以及结构驱动的输入槽数量。 */
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
        // 金属熔炼配方已按授权删除，因此这里只验证结构本身，不伪造已删除的输入和产物。
        helper.assertValueEqual(controller.inputSlotCount(), 12, "variable smeltery input slot count");
        helper.assertTrue(controller.isStructureValid(), "variable smeltery structure is not valid");
        helper.succeed();
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

    /** 验证合金炉仍支持多种独立流体输入槽。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 280)
    public static void alloyerConsumesInputs(GameTestHelper helper) {
        // 摆放合金炉和相邻加热器，使用仍保留的副产物流体验证多输入槽。
        BlockPos alloyerPos = new BlockPos(0, 0, 0);
        BlockPos heaterPos = new BlockPos(1, 0, 0);
        helper.setBlock(alloyerPos, TFBlocks.ALLOYER.get());
        helper.setBlock(heaterPos, TFBlocks.HEATER.get());
        FoundryBlockEntity alloyer = helper.getBlockEntity(alloyerPos);
        FoundryBlockEntity heater = helper.getBlockEntity(heaterPos);
        helper.assertValueEqual(alloyer.getTanks(), FoundryBlockEntity.MAX_ALLOY_INPUTS + 1, "alloyer tank count");
        helper.assertValueEqual(alloyer.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), 540), FluidAction.EXECUTE), 540,
            "alloyer first input amount");
        helper.assertValueEqual(alloyer.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("liquid_soul").get(), 180), FluidAction.EXECUTE), 180,
            "alloyer second input amount");
        helper.assertValueEqual(heater.fill(new FluidStack(Fluids.LAVA, FluidValues.BUCKET), FluidAction.EXECUTE), FluidValues.BUCKET,
            "alloyer lava fuel amount");
        // 合金金属配方已按授权删除，这里只确认输入没有被错误吞掉。
        helper.assertValueEqual(alloyer.getFluidInTank(0).getAmount(), 540, "alloyer first input was changed");
        helper.assertValueEqual(alloyer.getFluidInTank(1).getAmount(), 180, "alloyer second input was changed");
        helper.succeed();
    }

    /** 验证原有铁、金、铜熔融流体仍然注册，避免误删原版金属配方依赖。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void originalMetalFluidRegistrations(GameTestHelper helper) {
        // 三种原有基础金属流体仍由本模组注册，供熔炼和浇注配方使用。
        helper.assertTrue(TFFluids.ORIGINAL_SOURCES.size() == 3, "original metal fluid registration count changed");
        helper.assertTrue(TFFluids.ORIGINAL_SOURCES.containsKey("iron"), "missing original iron fluid registration");
        helper.assertTrue(TFFluids.ORIGINAL_SOURCES.containsKey("gold"), "missing original gold fluid registration");
        helper.assertTrue(TFFluids.ORIGINAL_SOURCES.containsKey("copper"), "missing original copper fluid registration");
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

    /** 验证浇注储液罐可以自动处理原版熔岩桶，并且转换失败时不扣除流体。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 40)
    public static void castingTankProcessesContainers(GameTestHelper helper) {
        // 先验证空桶从浇注储液罐取出一桶原版熔岩，转换结果进入输出槽。
        BlockPos pos = BlockPos.ZERO;
        helper.setBlock(pos, TFBlocks.SEARED_CASTING_TANK.get());
        FoundryBlockEntity tank = helper.getBlockEntity(pos);
        tank.fill(new FluidStack(Fluids.LAVA, FluidValues.BUCKET), FluidAction.EXECUTE);
        tank.setItem(0, new ItemStack(Items.BUCKET));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tank.getItem(FoundryBlockEntity.OUTPUT_SLOT).is(Items.LAVA_BUCKET),
                "casting tank did not fill an empty bucket");
            helper.assertValueEqual(tank.getFluidInTank(0).getAmount(), 0, "casting tank kept drained fluid");
            tank.setItem(FoundryBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
            tank.setItem(0, new ItemStack(Items.LAVA_BUCKET));
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
        org.hp.tinker_foundry.item.FoundryTankItem.setFluid(filled,
            new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), FluidValues.INGOT));
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

    /** 验证设备存档重载不会丢失容器型模具、保留流体与物品。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void moldingRemainderAndSaveReload(GameTestHelper helper) {
        // 金属浇注配方已删除，因此直接验证未加工状态下的物品和流体存档。
        BlockPos pos = BlockPos.ZERO;
        helper.setBlock(pos, TFBlocks.CASTING_TABLE.get());
        FoundryBlockEntity table = helper.getBlockEntity(pos);
        table.setItem(0, new ItemStack(TFItems.COPPER_CANISTER.get()));
        table.setItem(FoundryBlockEntity.REMAINDER_SLOT, new ItemStack(TFItems.COPPER_CANISTER.get()));
        table.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), FluidValues.INGOT * 2), FluidAction.EXECUTE);
        CompoundTag saved = table.saveWithoutMetadata(helper.getLevel().registryAccess());
        FoundryBlockEntity restored = new FoundryBlockEntity(TFBlockEntities.GENERIC.get(), new BlockPos(4, 0, 0),
            TFBlocks.CASTING_TABLE.get().defaultBlockState());
        restored.loadCustomOnly(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.getItem(FoundryBlockEntity.REMAINDER_SLOT).is(TFItems.COPPER_CANISTER.get()), "remainder changed after reload");
        helper.assertValueEqual(restored.getFluidInTank(0).getAmount(), FluidValues.INGOT * 2, "fluid amount changed after reload");
        helper.succeed();
    }

    /** 验证没有匹配配方时，满输出槽不会错误消耗输入。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void castingOutputFullProtection(GameTestHelper helper) {
        // 预先填满输出槽，设备不得在没有匹配配方时丢失流体和模具。
        BlockPos pos = BlockPos.ZERO;
        helper.setBlock(pos, TFBlocks.CASTING_TABLE.get());
        FoundryBlockEntity table = helper.getBlockEntity(pos);
        table.setItem(0, new ItemStack(TFItems.INGOT_CAST.get()));
        table.setItem(FoundryBlockEntity.OUTPUT_SLOT, new ItemStack(Items.IRON_INGOT, 64));
        table.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), FluidValues.INGOT), FluidAction.EXECUTE);
        helper.assertValueEqual(table.getFluidInTank(0).getAmount(), FluidValues.INGOT, "unmatched fluid was consumed");
        helper.assertTrue(table.getItem(0).is(TFItems.INGOT_CAST.get()), "unmatched mold was consumed");
        helper.assertValueEqual(table.getItem(FoundryBlockEntity.OUTPUT_SLOT).getCount(), 64, "full output stack changed");
        helper.succeed();
    }

    /** 验证合金配方支持流体催化输入，且催化标记能通过独立 Codec 往返。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void alloyingCatalystCodec(GameTestHelper helper) {
        // 使用仍保留的钴和灵魂液构造带催化标记的合金配方 JSON。
        JsonElement json = JsonParser.parseString("""
            {
              "ingredients": [
                {"ingredient": {"fluid": "tinker_foundry:cobalt"}, "amount": 90, "catalyst": true},
                {"ingredient": {"fluid": "tinker_foundry:liquid_soul"}, "amount": 90}
              ],
              "result": {"id": "tinker_foundry:venom", "amount": 180},
              "temperature": 700
            }
            """);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());

        // 解析后确认催化槽参与匹配，但该字段不会退化成物品催化剂。
        AlloyingRecipe recipe = AlloyingRecipe.CODEC.codec().parse(ops, json).getOrThrow();
        helper.assertValueEqual(recipe.ingredients().size(), 2, "alloy ingredient count");
        helper.assertTrue(recipe.ingredients().get(0).catalyst(), "alloy catalyst flag was lost");
        helper.assertTrue(recipe.matches(new FluidRecipeInput(List.of(
            new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), 90),
            new FluidStack(TFFluids.EXTRA_SOURCES.get("liquid_soul").get(), 90)
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
                {"ingredient": {"fluid": "tinker_foundry:cobalt"}, "amount": 90},
                {"ingredient": {"fluid": "tinker_foundry:liquid_soul"}, "amount": 90},
                {"ingredient": {"fluid": "tinker_foundry:honey"}, "amount": 90}
              ],
              "result": {"id": "tinker_foundry:venom", "amount": 270},
              "temperature": 900
            }
            """);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        AlloyingRecipe recipe = AlloyingRecipe.CODEC.codec().parse(ops, json).getOrThrow();
        helper.assertValueEqual(recipe.ingredients().size(), 3, "three-input alloy recipe was truncated");
        helper.assertTrue(recipe.matches(new FluidRecipeInput(List.of(
            new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), 90),
            new FluidStack(TFFluids.EXTRA_SOURCES.get("liquid_soul").get(), 90),
            new FluidStack(TFFluids.EXTRA_SOURCES.get("honey").get(), 90)
        )), helper.getLevel()), "three-input alloy recipe does not match");
        helper.succeed();
    }

    /** 检验共享容量、模拟不变、指定类型抽取与选择顺序，不依赖客户端渲染。 */
    @GameTest(templateNamespace = "minecraft", template = VANILLA_EMPTY_TEMPLATE, timeoutTicks = 20)
    public static void sharedMultiFluidStorage(GameTestHelper helper) {
        var tank = new org.hp.tinker_foundry.common.StructureFluidTank();
        FluidStack cobalt = new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), 900);
        FluidStack liquidSoul = new FluidStack(TFFluids.EXTRA_SOURCES.get("liquid_soul").get(), 900);
        helper.assertValueEqual(tank.fill(cobalt, 1000, FluidAction.SIMULATE), 900, "simulated fill");
        helper.assertValueEqual(tank.amount(), 0, "simulation mutated tank");
        tank.fill(cobalt, 1000, FluidAction.EXECUTE);
        helper.assertValueEqual(tank.fill(liquidSoul, 1000, FluidAction.EXECUTE), 100, "shared capacity exceeded");
        helper.assertValueEqual(tank.amount(), 1000, "shared capacity total");
        helper.assertTrue(tank.select(1), "selection did not reorder layers");
        helper.assertTrue(tank.get(0).is(TFFluids.EXTRA_SOURCES.get("liquid_soul").get()), "selected layer not at bottom");
        tank.drain(0, 100, FluidAction.SIMULATE);
        helper.assertValueEqual(tank.amount(), 1000, "simulated drain mutated tank");
        tank.drain(0, 100, FluidAction.EXECUTE);
        helper.assertValueEqual(tank.size(), 1, "empty layer was retained");
        helper.assertValueEqual(tank.fill(liquidSoul, 500, FluidAction.EXECUTE), 0, "shrunk structure accepted overflow");
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
        controller.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("cobalt").get(), 270), FluidAction.EXECUTE);
        controller.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("liquid_soul").get(), 90), FluidAction.EXECUTE);
        controller.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("honey").get(), 180), FluidAction.EXECUTE);
        helper.runAfterDelay(15, () -> {
            List<FluidStack> layers = controller.structureFluidLayers();
            helper.assertValueEqual(layers.stream().mapToInt(FluidStack::getAmount).sum(), 540, "alloy volume changed");
            helper.assertTrue(layers.stream().anyMatch(stack -> stack.is(TFFluids.EXTRA_SOURCES.get("cobalt").get()) && stack.getAmount() == 270), "preserved fluid layer missing");
            helper.assertValueEqual(first.getFluidInTank(0).getAmount() + second.getFluidInTank(0).getAmount(), 1000, "fuel tanks changed without a recipe");
            // 再装另一罐时应自动显示新来源，旧空罐仍贡献容量。
            third.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
            helper.assertValueEqual(controller.fuelDisplayFluid().getAmount(), 1000, "empty first tank hid later fuel");
            helper.assertValueEqual(controller.fuelDisplayCapacity(), 12000, "aggregate capacity changed after switch");
            CompoundTag saved = controller.saveWithoutMetadata(helper.getLevel().registryAccess());
            FoundryBlockEntity restored = new FoundryBlockEntity(controller.getBlockPos(), controller.getBlockState());
            restored.loadCustomOnly(saved, helper.getLevel().registryAccess());
            helper.assertValueEqual(restored.structureFluidLayers().size(), 3, "reload lost fluid layers");
            helper.assertTrue(restored.getFluidInTank(0).is(TFFluids.EXTRA_SOURCES.get("cobalt").get()), "reload lost selected fluid order");
            helper.assertValueEqual(restored.getFluidInTank(0).getAmount(), 270, "reload changed selected amount");
            helper.succeed();
        });
    }

    /** 防止游戏测试类被误实例化，NeoForge 会直接调用静态测试方法。 */
    private FoundryGameTests() {
    }
}
