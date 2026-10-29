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
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFFluids;

public final class FoundryGameTests {
    private static final Identifier FUNCTION_ID = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "server_smoke");
    private static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "server_smoke");

    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, FUNCTION_ID, () -> FoundryGameTests::runServerSmoke);
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
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment,
            Identifier.withDefaultNamespace("bastion/blocks/air"),
            40,
            0,
            true
        );
        ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, FUNCTION_ID);
        event.registerTest(TEST_ID, new FunctionGameTestInstance(function, data));
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
        helper.succeed();
    }

    private FoundryGameTests() {
    }
}
