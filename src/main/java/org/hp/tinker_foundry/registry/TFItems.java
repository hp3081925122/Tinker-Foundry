package org.hp.tinker_foundry.registry;

import java.util.Map;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.item.PortableTankItem;
import org.hp.tinker_foundry.item.FoundryTankItem;
import org.hp.tinker_foundry.item.FoundryTooltipBlockItem;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 冶炼方块、桶和基础便携容器的物品注册表。 */
public final class TFItems {
    /** 普通砂浆方块物品。 */
    public static final DeferredItem<Item> GROUT = block("grout", TFBlocks.GROUT);
    /** 下界砂浆方块物品。 */
    public static final DeferredItem<Item> NETHER_GROUT = block("nether_grout", TFBlocks.NETHER_GROUT);
    /** 冶炼砖物品，原版匠魂将其作为材料物品而不是方块注册。 */
    public static final DeferredItem<Item> SEARED_BRICK = simple("seared_brick");
    /** 冶炼玻璃物品。 */
    public static final DeferredItem<Item> SEARED_GLASS = block("seared_glass", TFBlocks.SEARED_GLASS);
    /** 冶炼灯物品。 */
    public static final DeferredItem<FoundryTankItem> SEARED_LANTERN = tank("seared_lantern", TFBlocks.SEARED_LANTERN, org.hp.tinker_foundry.block.FoundryLanternBlock.CAPACITY, false);
    /** 冶炼梯子物品。 */
    public static final DeferredItem<Item> SEARED_LADDER = block("seared_ladder", TFBlocks.SEARED_LADDER);
    /** 焦黑砖物品，原版匠魂将其作为材料物品而不是方块注册。 */
    public static final DeferredItem<Item> SCORCHED_BRICK = simple("scorched_brick");
    /** 焦黑玻璃物品。 */
    public static final DeferredItem<Item> SCORCHED_GLASS = block("scorched_glass", TFBlocks.SCORCHED_GLASS);
    /** 焦黑灯物品。 */
    public static final DeferredItem<FoundryTankItem> SCORCHED_LANTERN = tank("scorched_lantern", TFBlocks.SCORCHED_LANTERN, org.hp.tinker_foundry.block.FoundryLanternBlock.CAPACITY, false);
    /** 焦黑梯子物品。 */
    public static final DeferredItem<Item> SCORCHED_LADDER = block("scorched_ladder", TFBlocks.SCORCHED_LADDER);
    /** 冶炼系统设备物品。 */
    public static final DeferredItem<Item> SMELTERY_CONTROLLER = block("smeltery_controller", TFBlocks.SMELTERY_CONTROLLER);
    /** 铸造炉设备物品。 */
    public static final DeferredItem<Item> FOUNDRY_CONTROLLER = block("foundry_controller", TFBlocks.FOUNDRY_CONTROLLER);
    /** 小型冶炼炉物品。 */
    public static final DeferredItem<Item> SEARED_MELTER = block("seared_melter", TFBlocks.SEARED_MELTER);
    /** 冶炼炉加热器物品。 */
    public static final DeferredItem<Item> SEARED_HEATER = block("seared_heater", TFBlocks.SEARED_HEATER);
    /** 焦黑合金炉物品。 */
    public static final DeferredItem<Item> SCORCHED_ALLOYER = block("scorched_alloyer", TFBlocks.SCORCHED_ALLOYER);
    /** 原版命名的大容量金属储罐物品。 */
    public static final DeferredItem<FoundryTankItem> SEARED_INGOT_TANK = tank("seared_ingot_tank", TFBlocks.SEARED_INGOT_TANK, FluidValues.INGOT * 48, false);
    public static final DeferredItem<FoundryTankItem> SCORCHED_INGOT_TANK = tank("scorched_ingot_tank", TFBlocks.SCORCHED_INGOT_TANK, FluidValues.INGOT * 48, false);
    /** 冶炼燃料罐物品。 */
    public static final DeferredItem<FoundryTankItem> SEARED_FUEL_TANK = tank("seared_fuel_tank", TFBlocks.SEARED_FUEL_TANK, FoundryBlockEntity.DEFAULT_CAPACITY, true);
    /** 焦黑燃料罐物品。 */
    public static final DeferredItem<FoundryTankItem> SCORCHED_FUEL_TANK = tank("scorched_fuel_tank", TFBlocks.SCORCHED_FUEL_TANK, FoundryBlockEntity.DEFAULT_CAPACITY, true);
    /** 冶炼小型浇注储液罐物品。 */
    public static final DeferredItem<FoundryTankItem> SEARED_CASTING_TANK = tank("seared_casting_tank", TFBlocks.SEARED_CASTING_TANK, FluidValues.BUCKET, false);
    /** 原版没有铜粒，因此由独立命名空间提供铜粒以闭合铜的浇注链。 */
    public static final DeferredItem<Item> COPPER_NUGGET = simple("copper_nugget");
    /** 冶炼浇注台物品。 */
    public static final DeferredItem<Item> SEARED_TABLE = block("seared_table", TFBlocks.SEARED_TABLE);
    /** 焦黑浇注台物品。 */
    public static final DeferredItem<Item> SCORCHED_TABLE = block("scorched_table", TFBlocks.SCORCHED_TABLE);
    /** 冶炼浇注盆物品。 */
    public static final DeferredItem<Item> SEARED_BASIN = block("seared_basin", TFBlocks.SEARED_BASIN);
    /** 焦黑浇注盆物品。 */
    public static final DeferredItem<Item> SCORCHED_BASIN = block("scorched_basin", TFBlocks.SCORCHED_BASIN);
    /** 冶炼排液口物品。 */
    public static final DeferredItem<Item> SEARED_DRAIN = block("seared_drain", TFBlocks.SEARED_DRAIN);
    /** 焦黑排液口物品。 */
    public static final DeferredItem<Item> SCORCHED_DRAIN = block("scorched_drain", TFBlocks.SCORCHED_DRAIN);
    /** 冶炼浇注口物品。 */
    public static final DeferredItem<Item> SEARED_FAUCET = block("seared_faucet", TFBlocks.SEARED_FAUCET);
    /** 焦黑浇注口物品。 */
    public static final DeferredItem<Item> SCORCHED_FAUCET = block("scorched_faucet", TFBlocks.SCORCHED_FAUCET);
    /** 冶炼流体管道物品。 */
    public static final DeferredItem<Item> SEARED_DUCT = block("seared_duct", TFBlocks.SEARED_DUCT);
    /** 焦黑流体管道物品。 */
    public static final DeferredItem<Item> SCORCHED_DUCT = block("scorched_duct", TFBlocks.SCORCHED_DUCT);
    /** 冶炼导流槽物品。 */
    public static final DeferredItem<Item> SEARED_CHUTE = block("seared_chute", TFBlocks.SEARED_CHUTE);
    /** 焦黑导流槽物品。 */
    public static final DeferredItem<Item> SCORCHED_CHUTE = block("scorched_chute", TFBlocks.SCORCHED_CHUTE);
    /** 铜制流体计物品。 */
    public static final DeferredItem<Item> COPPER_GAUGE = block("copper_gauge", TFBlocks.COPPER_GAUGE);
    /** 黑曜石流体计物品。 */
    public static final DeferredItem<Item> OBSIDIAN_GAUGE = block("obsidian_gauge", TFBlocks.OBSIDIAN_GAUGE);
    /** 冶炼锭容量量器物品，沿用储罐的流体组件和物品栏动态模型。 */
    public static final DeferredItem<FoundryTankItem> SEARED_INGOT_GAUGE = tank("seared_ingot_gauge", TFBlocks.SEARED_INGOT_GAUGE, FluidValues.INGOT * 48, false);
    /** 焦黑锭容量量器物品，沿用储罐的流体组件和物品栏动态模型。 */
    public static final DeferredItem<FoundryTankItem> SCORCHED_INGOT_GAUGE = tank("scorched_ingot_gauge", TFBlocks.SCORCHED_INGOT_GAUGE, FluidValues.INGOT * 48, false);
    /** 冶炼燃料量器物品，沿用储罐的流体组件和物品栏动态模型。 */
    public static final DeferredItem<FoundryTankItem> SEARED_FUEL_GAUGE = tank("seared_fuel_gauge", TFBlocks.SEARED_FUEL_GAUGE, FoundryBlockEntity.DEFAULT_CAPACITY, true);
    /** 焦黑燃料量器物品，沿用储罐的流体组件和物品栏动态模型。 */
    public static final DeferredItem<FoundryTankItem> SCORCHED_FUEL_GAUGE = tank("scorched_fuel_gauge", TFBlocks.SCORCHED_FUEL_GAUGE, FoundryBlockEntity.DEFAULT_CAPACITY, true);
    /** 流体炮物品，同时保留方块中的流体槽能力。 */
    public static final DeferredItem<FoundryTankItem> SEARED_FLUID_CANNON = tank("seared_fluid_cannon", TFBlocks.SEARED_FLUID_CANNON, FluidValues.BUCKET * 2, true);
    public static final DeferredItem<FoundryTankItem> SCORCHED_FLUID_CANNON = tank("scorched_fluid_cannon", TFBlocks.SCORCHED_FLUID_CANNON, FluidValues.BUCKET * 2, true);
    /** 原版命名的代理储罐物品。 */
    public static final DeferredItem<Item> SCORCHED_PROXY_TANK = block("scorched_proxy_tank", TFBlocks.SCORCHED_PROXY_TANK);

    /** 可重复使用的锭铸模。 */
    public static final DeferredItem<Item> INGOT_CAST = simple("ingot_cast");
    /** 可重复使用的粒铸模。 */
    public static final DeferredItem<Item> NUGGET_CAST = simple("nugget_cast");
    /** 一次性锭砂模。 */
    public static final DeferredItem<Item> INGOT_SAND_CAST = simple("ingot_sand_cast");
    /** 一次性粒砂模。 */
    public static final DeferredItem<Item> NUGGET_SAND_CAST = simple("nugget_sand_cast");
    /** 一次性锭红砂模。 */
    public static final DeferredItem<Item> INGOT_RED_SAND_CAST = simple("ingot_red_sand_cast");
    /** 一次性粒红砂模。 */
    public static final DeferredItem<Item> NUGGET_RED_SAND_CAST = simple("nugget_red_sand_cast");
    /** 可重复使用的宝石铸模，使用宝石与熔融金制作。 */
    public static final DeferredItem<Item> GEM_CAST = simple("gem_cast");
    /** 一次性宝石砂模，使用四角沙子制作。 */
    public static final DeferredItem<Item> GEM_SAND_CAST = simple("gem_sand_cast");
    /** 一次性宝石红砂模，使用四角红沙制作。 */
    public static final DeferredItem<Item> GEM_RED_SAND_CAST = simple("gem_red_sand_cast");

    /** 保留原有熔融铁桶，供原版铁的熔炼和浇注配方使用。 */
    public static final DeferredItem<BucketItem> IRON_BUCKET = bucket("iron", TFFluids.IRON);
    /** 保留原有熔融金桶，供原版金的熔炼和浇注配方使用。 */
    public static final DeferredItem<BucketItem> GOLD_BUCKET = bucket("gold", TFFluids.GOLD);
    /** 保留原有熔融铜桶，供原版铜的熔炼和浇注配方使用。 */
    public static final DeferredItem<BucketItem> COPPER_BUCKET = bucket("copper", TFFluids.COPPER);

    /** 铜制便携罐。 */
    public static final DeferredItem<PortableTankItem> COPPER_CANISTER = TinkerFoundry.ITEMS.register("copper_canister", () -> new PortableTankItem(FluidValues.INGOT, new Item.Properties()));

    /** 变种方块统一注册，避免为每个不参与逻辑判断的装饰方块复制一套 Java 字段。 */
    private static final Map<String, DeferredItem<Item>> VARIANT_BLOCK_ITEMS = registerVariantBlockItems();
    /** 注册方块物品。 */
    private static DeferredItem<Item> block(String name, net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return TinkerFoundry.ITEMS.register(name, () -> new FoundryTooltipBlockItem(block.get(), new Item.Properties()));
    }

    /** 注册可以携带流体的专用储液罐方块物品。 */
    private static DeferredItem<FoundryTankItem> tank(String name, net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block> block, int capacity, boolean allowFuel) {
        return TinkerFoundry.ITEMS.register(name, () -> new FoundryTankItem(block.get(), capacity, allowFuel, new Item.Properties()));
    }

    /** 注册普通铸模物品。 */
    private static DeferredItem<Item> simple(String name) {
        return TinkerFoundry.ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    /** 注册流体桶。 */
    private static DeferredItem<BucketItem> bucket(String name, java.util.function.Supplier<? extends net.minecraft.world.level.material.Fluid> fluid) {
        return TinkerFoundry.ITEMS.register(name + "_bucket", () -> new BucketItem(fluid.get(), new Item.Properties().stacksTo(1)));
    }

    /** 按注册名取得批量注册的装饰方块物品，供创造标签页保持匠魂顺序。 */
    public static Item variantBlock(String name) {
        return VARIANT_BLOCK_ITEMS.get(name).get();
    }

    /** 注册上游冶炼砖、玻璃、附件和方块结构变种。 */
    private static Map<String, DeferredItem<Item>> registerVariantBlockItems() {
        Map<String, DeferredItem<Item>> items = new java.util.LinkedHashMap<>();
        items.put("seared_lamp", block("seared_lamp", TFBlocks.SEARED_LAMP));
        items.put("seared_stone", block("seared_stone", TFBlocks.SEARED_STONE));
        items.put("seared_stone_stairs", block("seared_stone_stairs", TFBlocks.SEARED_STONE_STAIRS));
        items.put("seared_stone_slab", block("seared_stone_slab", TFBlocks.SEARED_STONE_SLAB));
        items.put("seared_cobble", block("seared_cobble", TFBlocks.SEARED_COBBLE));
        items.put("seared_cobble_stairs", block("seared_cobble_stairs", TFBlocks.SEARED_COBBLE_STAIRS));
        items.put("seared_cobble_slab", block("seared_cobble_slab", TFBlocks.SEARED_COBBLE_SLAB));
        items.put("seared_cobble_wall", block("seared_cobble_wall", TFBlocks.SEARED_COBBLE_WALL));
        items.put("seared_paver", block("seared_paver", TFBlocks.SEARED_PAVER));
        items.put("seared_paver_stairs", block("seared_paver_stairs", TFBlocks.SEARED_PAVER_STAIRS));
        items.put("seared_paver_slab", block("seared_paver_slab", TFBlocks.SEARED_PAVER_SLAB));
        items.put("seared_bricks", block("seared_bricks", TFBlocks.SEARED_BRICKS));
        items.put("seared_bricks_stairs", block("seared_bricks_stairs", TFBlocks.SEARED_BRICKS_STAIRS));
        items.put("seared_bricks_slab", block("seared_bricks_slab", TFBlocks.SEARED_BRICKS_SLAB));
        items.put("seared_bricks_wall", block("seared_bricks_wall", TFBlocks.SEARED_BRICKS_WALL));
        items.put("seared_cracked_bricks", block("seared_cracked_bricks", TFBlocks.SEARED_CRACKED_BRICKS));
        items.put("seared_fancy_bricks", block("seared_fancy_bricks", TFBlocks.SEARED_FANCY_BRICKS));
        items.put("seared_triangle_bricks", block("seared_triangle_bricks", TFBlocks.SEARED_TRIANGLE_BRICKS));
        items.put("seared_glass_pane", block("seared_glass_pane", TFBlocks.SEARED_GLASS_PANE));
        items.put("seared_tinted_glass", block("seared_tinted_glass", TFBlocks.SEARED_TINTED_GLASS));
        items.put("seared_soul_glass", block("seared_soul_glass", TFBlocks.SEARED_SOUL_GLASS));
        items.put("seared_soul_glass_pane", block("seared_soul_glass_pane", TFBlocks.SEARED_SOUL_GLASS_PANE));
        items.put("scorched_lamp", block("scorched_lamp", TFBlocks.SCORCHED_LAMP));
        items.put("scorched_stone", block("scorched_stone", TFBlocks.SCORCHED_STONE));
        items.put("polished_scorched_stone", block("polished_scorched_stone", TFBlocks.POLISHED_SCORCHED_STONE));
        items.put("scorched_bricks", block("scorched_bricks", TFBlocks.SCORCHED_BRICKS));
        items.put("scorched_bricks_stairs", block("scorched_bricks_stairs", TFBlocks.SCORCHED_BRICKS_STAIRS));
        items.put("scorched_bricks_slab", block("scorched_bricks_slab", TFBlocks.SCORCHED_BRICKS_SLAB));
        items.put("scorched_bricks_fence", block("scorched_bricks_fence", TFBlocks.SCORCHED_BRICKS_FENCE));
        items.put("scorched_road", block("scorched_road", TFBlocks.SCORCHED_ROAD));
        items.put("scorched_road_stairs", block("scorched_road_stairs", TFBlocks.SCORCHED_ROAD_STAIRS));
        items.put("scorched_road_slab", block("scorched_road_slab", TFBlocks.SCORCHED_ROAD_SLAB));
        items.put("chiseled_scorched_bricks", block("chiseled_scorched_bricks", TFBlocks.CHISELED_SCORCHED_BRICKS));
        items.put("scorched_glass_pane", block("scorched_glass_pane", TFBlocks.SCORCHED_GLASS_PANE));
        items.put("scorched_tinted_glass", block("scorched_tinted_glass", TFBlocks.SCORCHED_TINTED_GLASS));
        items.put("scorched_soul_glass", block("scorched_soul_glass", TFBlocks.SCORCHED_SOUL_GLASS));
        items.put("scorched_soul_glass_pane", block("scorched_soul_glass_pane", TFBlocks.SCORCHED_SOUL_GLASS_PANE));
        items.put("seared_channel", block("seared_channel", TFBlocks.SEARED_CHANNEL));
        items.put("scorched_channel", block("scorched_channel", TFBlocks.SCORCHED_CHANNEL));
        return items;
    }

    /** 实体产液和副产物使用相同的标准流体桶能力。 */
    public static final Map<String, DeferredItem<BucketItem>> EXTRA_BUCKETS = registerExtraBuckets();

    /** 延迟构造附加流体桶映射，防止注册时循环取值。 */
    private static Map<String, DeferredItem<BucketItem>> registerExtraBuckets() {
        Map<String, DeferredItem<BucketItem>> buckets = new java.util.LinkedHashMap<>();
        TFFluids.EXTRA_SOURCES.forEach((name, fluid) -> buckets.put(name, bucket(name, fluid)));
        return buckets;
    }

    /** 为流体基类提供对应桶的延迟查找。 */
    public static Item bucketFor(String name) {
        return switch (name) {
            case "iron" -> IRON_BUCKET.get();
            case "gold" -> GOLD_BUCKET.get();
            case "copper" -> COPPER_BUCKET.get();
            default -> EXTRA_BUCKETS.get(name).get();
        };
    }

    private TFItems() {
    }
}
