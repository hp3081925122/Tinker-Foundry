package org.hp.tinker_foundry.registry;

import java.util.HashMap;
import java.util.Map;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FlowingFluid;
import org.hp.tinker_foundry.TinkerFoundry;

/** 注册基础熔融金属流体；所有流体都使用新的模组命名空间。 */
public final class TFFluids {
    /** 记录每种熔融流体对应的液体方块，供桶放置和流体属性回调使用。 */
    private static final Map<String, DeferredBlock<LiquidBlock>> FLUID_BLOCKS = new HashMap<>();

    /** 注册铁流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> IRON_TYPE = moltenType("iron", 0xFFE1E1E1, 1100, 12);
    /** 注册金流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> GOLD_TYPE = moltenType("gold", 0xFFFFD43B, 1000, 12);
    /** 注册铜流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> COPPER_TYPE = moltenType("copper", 0xFFF47B45, 800, 12);
    /** 注册锡流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> TIN_TYPE = moltenType("tin", 0xFFD5E4E8, 525, 12);
    /** 注册铅流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> LEAD_TYPE = moltenType("lead", 0xFF6B6D83, 630, 12);
    /** 注册银流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> SILVER_TYPE = moltenType("silver", 0xFFE7EDF2, 1090, 12);
    /** 注册镍流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> NICKEL_TYPE = moltenType("nickel", 0xFFD6CDB7, 1250, 12);
    /** 注册锌流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> ZINC_TYPE = moltenType("zinc", 0xFFC6D4DA, 720, 12);
    /** 注册铝流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> ALUMINUM_TYPE = moltenType("aluminum", 0xFFD8D8D8, 725, 12);
    /** 注册钢流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> STEEL_TYPE = moltenType("steel", 0xFF6F7784, 1250, 13);
    /** 注册青铜流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> BRONZE_TYPE = moltenType("bronze", 0xFFCD7842, 1000, 10);
    /** 注册黄铜流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> BRASS_TYPE = moltenType("brass", 0xFFF0B83F, 905, 10);
    /** 注册电金流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> ELECTRUM_TYPE = moltenType("electrum", 0xFFF5E276, 1060, 10);
    /** 注册殷钢流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> INVAR_TYPE = moltenType("invar", 0xFFB6B8AD, 1200, 10);
    /** 注册康铜流体类型。 */
    public static final DeferredHolder<FluidType, FluidType> CONSTANTAN_TYPE = moltenType("constantan", 0xFFD89456, 1220, 10);

    /** 注册铁源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> IRON = source("iron", IRON_TYPE);
    /** 注册铁流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> IRON_FLOWING = flowing("iron", IRON_TYPE);
    /** 注册金源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> GOLD = source("gold", GOLD_TYPE);
    /** 注册金流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> GOLD_FLOWING = flowing("gold", GOLD_TYPE);
    /** 注册铜源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> COPPER = source("copper", COPPER_TYPE);
    /** 注册铜流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> COPPER_FLOWING = flowing("copper", COPPER_TYPE);
    /** 注册锡源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> TIN = source("tin", TIN_TYPE);
    /** 注册锡流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> TIN_FLOWING = flowing("tin", TIN_TYPE);
    /** 注册铅源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> LEAD = source("lead", LEAD_TYPE);
    /** 注册铅流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> LEAD_FLOWING = flowing("lead", LEAD_TYPE);
    /** 注册银源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> SILVER = source("silver", SILVER_TYPE);
    /** 注册银流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> SILVER_FLOWING = flowing("silver", SILVER_TYPE);
    /** 注册镍源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> NICKEL = source("nickel", NICKEL_TYPE);
    /** 注册镍流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> NICKEL_FLOWING = flowing("nickel", NICKEL_TYPE);
    /** 注册锌源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> ZINC = source("zinc", ZINC_TYPE);
    /** 注册锌流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> ZINC_FLOWING = flowing("zinc", ZINC_TYPE);
    /** 注册铝源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> ALUMINUM = source("aluminum", ALUMINUM_TYPE);
    /** 注册铝流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> ALUMINUM_FLOWING = flowing("aluminum", ALUMINUM_TYPE);
    /** 注册钢源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> STEEL = source("steel", STEEL_TYPE);
    /** 注册钢流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> STEEL_FLOWING = flowing("steel", STEEL_TYPE);
    /** 注册青铜源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> BRONZE = source("bronze", BRONZE_TYPE);
    /** 注册青铜流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> BRONZE_FLOWING = flowing("bronze", BRONZE_TYPE);
    /** 注册黄铜源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> BRASS = source("brass", BRASS_TYPE);
    /** 注册黄铜流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> BRASS_FLOWING = flowing("brass", BRASS_TYPE);
    /** 注册电金源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> ELECTRUM = source("electrum", ELECTRUM_TYPE);
    /** 注册电金流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> ELECTRUM_FLOWING = flowing("electrum", ELECTRUM_TYPE);
    /** 注册殷钢源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> INVAR = source("invar", INVAR_TYPE);
    /** 注册殷钢流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> INVAR_FLOWING = flowing("invar", INVAR_TYPE);
    /** 注册康铜源流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> CONSTANTAN = source("constantan", CONSTANTAN_TYPE);
    /** 注册康铜流动流体。 */
    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> CONSTANTAN_FLOWING = flowing("constantan", CONSTANTAN_TYPE);

    /** 熔融铁液体方块。 */
    public static final DeferredBlock<LiquidBlock> IRON_BLOCK = registerFluidBlock("iron", IRON);
    /** 熔融金液体方块。 */
    public static final DeferredBlock<LiquidBlock> GOLD_BLOCK = registerFluidBlock("gold", GOLD);
    /** 熔融铜液体方块。 */
    public static final DeferredBlock<LiquidBlock> COPPER_BLOCK = registerFluidBlock("copper", COPPER);
    /** 熔融锡液体方块。 */
    public static final DeferredBlock<LiquidBlock> TIN_BLOCK = registerFluidBlock("tin", TIN);
    /** 熔融铅液体方块。 */
    public static final DeferredBlock<LiquidBlock> LEAD_BLOCK = registerFluidBlock("lead", LEAD);
    /** 熔融银液体方块。 */
    public static final DeferredBlock<LiquidBlock> SILVER_BLOCK = registerFluidBlock("silver", SILVER);
    /** 熔融镍液体方块。 */
    public static final DeferredBlock<LiquidBlock> NICKEL_BLOCK = registerFluidBlock("nickel", NICKEL);
    /** 熔融锌液体方块。 */
    public static final DeferredBlock<LiquidBlock> ZINC_BLOCK = registerFluidBlock("zinc", ZINC);
    /** 熔融铝液体方块。 */
    public static final DeferredBlock<LiquidBlock> ALUMINUM_BLOCK = registerFluidBlock("aluminum", ALUMINUM);
    /** 熔融钢液体方块。 */
    public static final DeferredBlock<LiquidBlock> STEEL_BLOCK = registerFluidBlock("steel", STEEL);
    /** 熔融青铜液体方块。 */
    public static final DeferredBlock<LiquidBlock> BRONZE_BLOCK = registerFluidBlock("bronze", BRONZE);
    /** 熔融黄铜液体方块。 */
    public static final DeferredBlock<LiquidBlock> BRASS_BLOCK = registerFluidBlock("brass", BRASS);
    /** 熔融电金液体方块。 */
    public static final DeferredBlock<LiquidBlock> ELECTRUM_BLOCK = registerFluidBlock("electrum", ELECTRUM);
    /** 熔融殷钢液体方块。 */
    public static final DeferredBlock<LiquidBlock> INVAR_BLOCK = registerFluidBlock("invar", INVAR);
    /** 熔融康铜液体方块。 */
    public static final DeferredBlock<LiquidBlock> CONSTANTAN_BLOCK = registerFluidBlock("constantan", CONSTANTAN);

    /** 注册流体类型，熔融金属不允许自然变成源流体。 */
    private static DeferredHolder<FluidType, FluidType> moltenType(String name, int color, int temperature, int lightLevel) {
        return TinkerFoundry.FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.tinker_foundry.molten_" + name)
            .density(5000)
            .viscosity(1500)
            .temperature(temperature)
            .lightLevel(lightLevel)
            .canConvertToSource(false)));
    }

    /** 注册静止流体。 */
    private static DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Source> source(String name, DeferredHolder<FluidType, FluidType> type) {
        return TinkerFoundry.FLUIDS.register(name, () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(type, () -> fluid(name), () -> fluid(name + "_flowing"))
            .bucket(() -> TFItems.bucketFor(name)).block(() -> fluidBlock(name)).slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(10)));
    }

    /** 注册流动流体。 */
    private static DeferredHolder<net.minecraft.world.level.material.Fluid, BaseFlowingFluid.Flowing> flowing(String name, DeferredHolder<FluidType, FluidType> type) {
        return TinkerFoundry.FLUIDS.register(name + "_flowing", () -> new BaseFlowingFluid.Flowing(new BaseFlowingFluid.Properties(type, () -> fluid(name), () -> fluid(name + "_flowing"))
            .bucket(() -> TFItems.bucketFor(name)).block(() -> fluidBlock(name)).slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(10)));
    }

    /** 注册不可掉落的液体方块，确保熔融流体桶可以正常放置。 */
    private static DeferredBlock<LiquidBlock> registerFluidBlock(String name, DeferredHolder<Fluid, ? extends FlowingFluid> source) {
        DeferredBlock<LiquidBlock> block = TinkerFoundry.BLOCKS.register(name + "_fluid", () -> new LiquidBlock(source.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.LAVA).noLootTable()));
        FLUID_BLOCKS.put(name, block);
        return block;
    }

    /** 延迟取得液体方块，避免流体和方块注册表的静态初始化顺序互相依赖。 */
    private static LiquidBlock fluidBlock(String name) {
        return FLUID_BLOCKS.get(name).get();
    }

    /** 判断流体是否属于本模组支持的熔融流体。 */
    public static boolean isFoundryFluid(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        return id != null && TinkerFoundry.MOD_ID.equals(id.getNamespace());
    }

    /** 判断当前首版燃料配方支持的流体，便携燃料罐与燃料罐共用此规则。 */
    public static boolean isFuelFluid(Fluid fluid) {
        return fluid == Fluids.LAVA;
    }

    /** 延迟从当前注册表获取源流体或流动流体，避免静态初始化顺序依赖。 */
    private static net.minecraft.world.level.material.Fluid fluid(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, name));
    }

    private TFFluids() {
    }
}
