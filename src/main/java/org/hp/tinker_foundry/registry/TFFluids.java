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

    /** 保留原有铁、金、铜熔融流体的颜色。 */
    public static final Map<String, Integer> ORIGINAL_COLORS = Map.of(
        "iron", 0xFFE1E1E1,
        "gold", 0xFFFFD43B,
        "copper", 0xFFF47B45
    );

    /** 保留原有铁、金、铜熔融流体类型，避免原版金属配方失效。 */
    public static final DeferredHolder<FluidType, FluidType> IRON_TYPE = moltenType("iron", ORIGINAL_COLORS.get("iron"), 1100, 12);
    public static final DeferredHolder<FluidType, FluidType> GOLD_TYPE = moltenType("gold", ORIGINAL_COLORS.get("gold"), 1000, 12);
    public static final DeferredHolder<FluidType, FluidType> COPPER_TYPE = moltenType("copper", ORIGINAL_COLORS.get("copper"), 800, 12);

    /** 保留原有铁、金、铜源流体。 */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> IRON = source("iron", IRON_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> GOLD = source("gold", GOLD_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> COPPER = source("copper", COPPER_TYPE);

    /** 保留原有铁、金、铜流动流体。 */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> IRON_FLOWING = flowing("iron", IRON_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> GOLD_FLOWING = flowing("gold", GOLD_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> COPPER_FLOWING = flowing("copper", COPPER_TYPE);

    /** 保留原有铁、金、铜液体方块。 */
    public static final DeferredBlock<LiquidBlock> IRON_BLOCK = registerFluidBlock("iron", IRON);
    public static final DeferredBlock<LiquidBlock> GOLD_BLOCK = registerFluidBlock("gold", GOLD);
    public static final DeferredBlock<LiquidBlock> COPPER_BLOCK = registerFluidBlock("copper", COPPER);

    /** 客户端和测试共用的原有金属流体注册表。 */
    public static final Map<String, DeferredHolder<FluidType, FluidType>> ORIGINAL_TYPES = Map.of(
        "iron", IRON_TYPE,
        "gold", GOLD_TYPE,
        "copper", COPPER_TYPE
    );
    public static final Map<String, DeferredHolder<Fluid, BaseFlowingFluid.Source>> ORIGINAL_SOURCES = Map.of(
        "iron", IRON,
        "gold", GOLD,
        "copper", COPPER
    );

    /** 实体熔炼与矿物副产物所需的独立流体，不依赖匠魂本体注册表。 */
    public static final Map<String, Integer> EXTRA_COLORS = Map.ofEntries(
        Map.entry("cobalt", 0xFF3677BA),
        Map.entry("liquid_soul", 0xFF89F4D8),
        Map.entry("honey", 0xFFE6A42C),
        Map.entry("blazing_blood", 0xFFFFA323),
        Map.entry("molten_glass", 0xFFE4F0F0),
        Map.entry("ender", 0xFF18776D),
        Map.entry("seared_stone", 0xFF5C504E),
        Map.entry("molten_emerald", 0xFF29D581),
        Map.entry("magma", 0xFFEE5C16),
        Map.entry("meat_soup", 0xFFBC8F6A),
        Map.entry("potion", 0xFF7654C4),
        Map.entry("milk", 0xFFF5F5EC),
        Map.entry("slime", 0xFF7EBD5F),
        Map.entry("venom", 0xFF668C23)
    );
    public static final Map<String, DeferredHolder<FluidType, FluidType>> EXTRA_TYPES = new java.util.LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Fluid, BaseFlowingFluid.Source>> EXTRA_SOURCES = new java.util.LinkedHashMap<>();
    static {
        // 统一建立源流体、流动流体和方块，桶通过延迟供应器解析。
        for (String name : EXTRA_COLORS.keySet().stream().sorted().toList()) {
            DeferredHolder<FluidType, FluidType> type = moltenType(name, EXTRA_COLORS.get(name), 1000, 8);
            EXTRA_TYPES.put(name, type);
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source = source(name, type);
            EXTRA_SOURCES.put(name, source);
            flowing(name, type);
            registerFluidBlock(name, source);
        }
    }

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
