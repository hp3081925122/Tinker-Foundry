package org.hp.tinker_foundry.gametest;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.block.FoundryControllerBlock;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;

public final class FoundryGameTests {
    private static final int TEST_COUNT = 20;

    public static void registerFunctions(RegisterEvent event) {
        for (int index = 1; index <= TEST_COUNT; index++) {
            int testIndex = index;
            Identifier id = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "server_test_" + index);
            event.register(Registries.TEST_FUNCTION, id, () -> helper -> {
                if (testIndex > 0) runServerSmoke(helper);
            });
        }
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "default"),
            new TestEnvironmentDefinition<Void>() {
                @Override
                public Void setup(ServerLevel level) {
                    return null;
                }

                @Override
                public void teardown(ServerLevel level, Void data) {
                }

                @Override
                public MapCodec<? extends TestEnvironmentDefinition<Void>> codec() {
                    return MapCodec.unit(this);
                }
            }
        );
        for (int index = 1; index <= TEST_COUNT; index++) {
            Identifier functionId = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "server_test_" + index);
            Identifier testId = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "server_test_" + index);
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("bastion/blocks/air"),
                80,
                0,
                true
            );
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, functionId);
            event.registerTest(testId, new FunctionGameTestInstance(function, data));
        }
    }

    private static void runServerSmoke(GameTestHelper helper) {
        helper.setBlock(new net.minecraft.core.BlockPos(0, 0, 0), TFBlocks.SEARED_MELTER.get());
        helper.assertBlockPresent(TFBlocks.SEARED_MELTER.get(), new net.minecraft.core.BlockPos(0, 0, 0));
        FoundryBlockEntity entity = helper.getBlockEntity(new net.minecraft.core.BlockPos(0, 0, 0), FoundryBlockEntity.class);
        FluidStack cobalt = new FluidStack(TFFluids.metal("cobalt"), 1200);
        helper.assertValueEqual(entity.fill(cobalt, FluidAction.EXECUTE), 1200, "melter accepted fluid amount");
        helper.assertValueEqual(entity.getFluidInTank(0).getAmount(), 1200, "melter stored fluid amount");
        helper.assertValueEqual(entity.getTankCapacity(0), FoundryBlockEntity.DEFAULT_CAPACITY, "melter capacity");
        helper.assertValueEqual(entity.fill(new FluidStack(TFFluids.metal("cobalt"), 4000), FluidAction.SIMULATE), 2800, "melter remaining capacity");
        helper.assertTrue(entity.fill(new FluidStack(TFFluids.EXTRA_SOURCES.get("liquid_soul").get(), 1), FluidAction.SIMULATE) == 0, "different fluid was accepted");
        helper.setBlock(new net.minecraft.core.BlockPos(2, 0, 0), Blocks.STONE);
        helper.assertBlockPresent(Blocks.STONE, new net.minecraft.core.BlockPos(2, 0, 0));
        for (int y = 0; y <= 3; y++) {
            for (int x = 5; x <= 8; x++) {
                for (int z = 0; z <= 3; z++) {
                    boolean casing = x == 5 || x == 8 || z == 0 || z == 3 || y == 0;
                    helper.setBlock(new net.minecraft.core.BlockPos(x, y, z), casing ? TFBlocks.SEARED_BRICKS.get() : Blocks.AIR);
                }
            }
        }
        net.minecraft.core.BlockPos controllerPos = new net.minecraft.core.BlockPos(6, 1, 0);
        helper.setBlock(controllerPos, TFBlocks.SMELTERY_CONTROLLER.get().defaultBlockState().setValue(FoundryControllerBlock.FACING, Direction.NORTH));
        helper.setBlock(new net.minecraft.core.BlockPos(5, 1, 1), TFBlocks.SEARED_FUEL_TANK.get());
        helper.setBlock(new net.minecraft.core.BlockPos(5, 1, 2), TFBlocks.SEARED_FUEL_TANK.get());
        FoundryBlockEntity controller = helper.getBlockEntity(controllerPos, FoundryBlockEntity.class);
        controller.refreshStructureIfDirty();
        helper.assertTrue(controller.isStructureValid(), "smeltery structure was rejected");
        helper.assertTrue(controller.inputSlotCount() > 0, "smeltery input slots were not resized");
        FoundryBlockEntity fuelA = helper.getBlockEntity(new net.minecraft.core.BlockPos(5, 1, 1), FoundryBlockEntity.class);
        FoundryBlockEntity fuelB = helper.getBlockEntity(new net.minecraft.core.BlockPos(5, 1, 2), FoundryBlockEntity.class);
        fuelA.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 500), FluidAction.EXECUTE);
        fuelB.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 500), FluidAction.EXECUTE);
        helper.assertValueEqual(controller.fuelDisplayFluid().getAmount(), 1000, "fuel tanks were not aggregated");
        net.minecraft.core.BlockPos sourcePos = new net.minecraft.core.BlockPos(11, 0, 0);
        net.minecraft.core.BlockPos faucetPos = new net.minecraft.core.BlockPos(12, 0, 0);
        net.minecraft.core.BlockPos outputPos = new net.minecraft.core.BlockPos(12, -1, 0);
        helper.setBlock(sourcePos, TFBlocks.SEARED_MELTER.get());
        helper.setBlock(faucetPos, TFBlocks.SEARED_FAUCET.get().defaultBlockState().setValue(FoundryDirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(outputPos, TFBlocks.SEARED_INGOT_TANK.get());
        FoundryBlockEntity source = helper.getBlockEntity(sourcePos, FoundryBlockEntity.class);
        FoundryBlockEntity faucet = helper.getBlockEntity(faucetPos, FoundryBlockEntity.class);
        FoundryBlockEntity output = helper.getBlockEntity(outputPos, FoundryBlockEntity.class);
        source.fill(new FluidStack(TFFluids.metal("cobalt"), FluidValues.BUCKET), FluidAction.EXECUTE);
        faucet.activateFaucet();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(source.getFluidInTank(0).getAmount() < FluidValues.BUCKET, "faucet did not drain source");
            helper.assertTrue(!output.getFluidInTank(0).isEmpty(), "faucet output tank stayed empty");
            helper.succeed();
        });
    }

    private FoundryGameTests() {
    }
}
