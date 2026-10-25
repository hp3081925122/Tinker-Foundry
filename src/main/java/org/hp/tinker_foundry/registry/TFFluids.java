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

    public static final org.hp.tinker_foundry.common.AutomaticMaterialTags MATERIAL_TAGS = new org.hp.tinker_foundry.common.AutomaticMaterialTags(
        net.neoforged.fml.ModList.get().getModFiles().stream().map(info -> info.getFile().findResource("data")).distinct().toList());
    public static final Map<String, Integer> EXTRA_COLORS = new java.util.LinkedHashMap<>(Map.ofEntries(
        Map.entry("liquid_soul", 0xFF89F4D8), Map.entry("honey", 0xFFE6A42C),
        Map.entry("blazing_blood", 0xFFFFA323), Map.entry("molten_glass", 0xFFE4F0F0),
        Map.entry("ender", 0xFF18776D), Map.entry("seared_stone", 0xFF5C504E),
        Map.entry("scorched_stone", 0xFF2B2423), Map.entry("molten_clay", 0xFF9B6045),
        Map.entry("molten_emerald", 0xFF29D581), Map.entry("molten_diamond", 0xFF5DCEFF),
        Map.entry("magma", 0xFFEE5C16), Map.entry("meat_soup", 0xFFBC8F6A),
        Map.entry("potion", 0xFF7654C4), Map.entry("milk", 0xFFF5F5EC),
        Map.entry("slime", 0xFF7EBD5F), Map.entry("venom", 0xFF668C23)));
    public static final Map<String, DeferredHolder<FluidType, FluidType>> EXTRA_TYPES = new java.util.LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Fluid, BaseFlowingFluid.Source>> EXTRA_SOURCES = new java.util.LinkedHashMap<>();
    static {
        MATERIAL_TAGS.materials().values().stream().filter(material -> material.fluids().isEmpty())
            .forEach(material -> EXTRA_COLORS.put(material.registryName(), material.color()));
        for (String name : EXTRA_COLORS.keySet().stream().sorted().toList()) {
            var material = MATERIAL_TAGS.materials().get(name.startsWith("molten/") ? name.substring(7) : "");
            int temperature = material != null ? material.temperature() : name.equals("molten_clay") ? 750 : 1000;
            int lightLevel = name.equals("molten_clay") ? 3 : 8;
            var type = moltenType(name, EXTRA_COLORS.get(name), temperature, lightLevel);
            EXTRA_TYPES.put(name, type);
            var source = source(name, type);
            EXTRA_SOURCES.put(name, source);
            flowing(name, type);
            registerFluidBlock(name, source);
        }
    }

    public static Fluid metal(String name) {
        var material = MATERIAL_TAGS.materials().get(name);
        if (material == null) return Fluids.EMPTY;
        java.util.Set<Fluid> found = new java.util.LinkedHashSet<>();
        for (String id : material.fluids()) {
            Fluid value = registered(id);
            if (value != Fluids.EMPTY) found.add(value instanceof FlowingFluid flowing ? flowing.getSource() : value);
        }
        if (found.size() == 1) return found.iterator().next();
        if (found.size() > 1) return material.declared()
            ? found.stream().min(java.util.Comparator.comparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString())).orElse(Fluids.EMPTY) : Fluids.EMPTY;
        return registered(material.generatedId());
    }

    public static boolean isMetal(Fluid fluid) {
        return fluid != Fluids.EMPTY && MATERIAL_TAGS.materials().keySet().stream().filter(name -> !MATERIAL_TAGS.isGemstone(name)).anyMatch(name -> metal(name) == fluid);
    }

    private static Fluid registered(String id) {
        return BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(id)).orElse(Fluids.EMPTY);
    }

    public static Map<ResourceLocation, byte[]> automaticResources(boolean client) {
        Map<ResourceLocation, byte[]> resources = new java.util.LinkedHashMap<>();
        if (client) {
            com.google.gson.JsonObject english = new com.google.gson.JsonObject();
            com.google.gson.JsonObject chinese = new com.google.gson.JsonObject();
            for (var material : MATERIAL_TAGS.materials().values()) {
                String name = material.registryName();
                if (!EXTRA_SOURCES.containsKey(name)) continue;
                putResource(resources, "tinker_foundry:models/item/" + name + "_bucket.json", "{\n  \"parent\": \"neoforge:item/default\",\n  \"loader\": \"neoforge:fluid_container\",\n  \"fluid\": \"minecraft:empty\",\n  \"textures\": {\n    \"base\": \"minecraft:item/bucket\",\n    \"fluid\": \"neoforge:item/mask/bucket_fluid\"\n  }\n}");
                putResource(resources, "tinker_foundry:blockstates/" + name + "_fluid.json", "{\n  \"variants\": {\n    \"level=0\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=1\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=2\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=3\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=4\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=5\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=6\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=7\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=8\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=9\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=10\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=11\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=12\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=13\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=14\":{\"model\":\"tinker_foundry:block/molten_fluid\"},\n    \"level=15\":{\"model\":\"tinker_foundry:block/molten_fluid\"}\n  }\n}");
                if (MATERIAL_TAGS.isConfigured(material.name())) continue;
                String fluidKey = "fluid_type.tinker_foundry." + name.replace('/', '.');
                String bucketKey = "item.tinker_foundry." + name.replace('/', '.') + "_bucket";
                english.addProperty(fluidKey, "Molten " + material.displayName("en_us"));
                english.addProperty(bucketKey, "Molten " + material.displayName("en_us") + " Bucket");
                chinese.addProperty(fluidKey, "熔融" + material.displayName("zh_cn"));
                chinese.addProperty(bucketKey, "熔融" + material.displayName("zh_cn") + "桶");
            }
            putResource(resources, "tinker_foundry:lang/en_us.json", english.toString());
            putResource(resources, "tinker_foundry:lang/zh_cn.json", chinese.toString());
        } else {
            for (var material : MATERIAL_TAGS.materials().values()) if (EXTRA_SOURCES.containsKey(material.registryName())) {
                putResource(resources, "c:tags/fluid/molten_" + material.name() + ".json",
                    "{\"replace\":false,\"values\":[\"" + material.generatedId() + "\"]}");
            }
        }
        return resources;
    }

    public static com.google.gson.JsonObject remapRecipe(com.google.gson.JsonObject json) {
        var copy = json.deepCopy();
        rewriteFluids(copy);
        return copy;
    }

    private static void rewriteFluids(com.google.gson.JsonElement json) {
        if (json.isJsonObject()) {
            var object = json.getAsJsonObject();
            for (var entry : new java.util.ArrayList<>(object.entrySet())) {
                var value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && value.getAsString().startsWith("tinker_foundry:molten/")) {
                    Fluid fluid = metal(value.getAsString().substring(22));
                    if (fluid != Fluids.EMPTY) object.addProperty(entry.getKey(), BuiltInRegistries.FLUID.getKey(fluid).toString());
                } else rewriteFluids(value);
            }
        } else if (json.isJsonArray()) json.getAsJsonArray().forEach(TFFluids::rewriteFluids);
    }

    private static void putResource(Map<ResourceLocation, byte[]> resources, String id, String text) {
        resources.put(ResourceLocation.parse(id), text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** 注册流体类型，熔融金属不允许自然变成源流体。 */
    private static DeferredHolder<FluidType, FluidType> moltenType(String name, int color, int temperature, int lightLevel) {
        return TinkerFoundry.FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
            .descriptionId(name.startsWith("molten/") ? "fluid_type.tinker_foundry." + name.replace('/', '.') : "fluid_type.tinker_foundry.molten_" + name)
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
