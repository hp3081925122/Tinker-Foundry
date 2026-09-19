package org.hp.tinker_foundry.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.WrittenBookItem;
import net.neoforged.neoforge.registries.DeferredItem;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.common.FluidValues;
import org.hp.tinker_foundry.item.PortableTankItem;
import org.hp.tinker_foundry.item.FoundryTankItem;
import org.hp.tinker_foundry.item.FoundryTooltipBlockItem;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 冶炼方块、桶和基础便携容器的物品注册表。 */
public final class TFItems {
    /** 冶炼砖物品。 */
    public static final DeferredItem<Item> SEARED_BRICK = block("seared_brick", TFBlocks.SEARED_BRICK);
    /** 冶炼玻璃物品。 */
    public static final DeferredItem<Item> SEARED_GLASS = block("seared_glass", TFBlocks.SEARED_GLASS);
    /** 冶炼灯物品。 */
    public static final DeferredItem<FoundryTankItem> SEARED_LANTERN = tank("seared_lantern", TFBlocks.SEARED_LANTERN, org.hp.tinker_foundry.block.FoundryLanternBlock.CAPACITY, false);
    /** 冶炼梯子物品。 */
    public static final DeferredItem<Item> SEARED_LADDER = block("seared_ladder", TFBlocks.SEARED_LADDER);
    /** 冶炼墙物品。 */
    public static final DeferredItem<Item> SEARED_WALL = block("seared_wall", TFBlocks.SEARED_WALL);
    /** 冶炼装饰砖物品。 */
    public static final DeferredItem<Item> SEARED_FANCY_BRICK = block("seared_fancy_brick", TFBlocks.SEARED_FANCY_BRICK);
    /** 焦黑方块物品。 */
    public static final DeferredItem<Item> SCORCHED_BRICK = block("scorched_brick", TFBlocks.SCORCHED_BRICK);
    /** 焦黑玻璃物品。 */
    public static final DeferredItem<Item> SCORCHED_GLASS = block("scorched_glass", TFBlocks.SCORCHED_GLASS);
    /** 焦黑灯物品。 */
    public static final DeferredItem<FoundryTankItem> SCORCHED_LANTERN = tank("scorched_lantern", TFBlocks.SCORCHED_LANTERN, org.hp.tinker_foundry.block.FoundryLanternBlock.CAPACITY, false);
    /** 焦黑梯子物品。 */
    public static final DeferredItem<Item> SCORCHED_LADDER = block("scorched_ladder", TFBlocks.SCORCHED_LADDER);
    /** 焦黑墙物品。 */
    public static final DeferredItem<Item> SCORCHED_WALL = block("scorched_wall", TFBlocks.SCORCHED_WALL);
    /** 焦黑装饰砖物品。 */
    public static final DeferredItem<Item> SCORCHED_FANCY_BRICK = block("scorched_fancy_brick", TFBlocks.SCORCHED_FANCY_BRICK);
    /** 冶炼系统设备物品。 */
    public static final DeferredItem<Item> SMELTERY_CONTROLLER = block("smeltery_controller", TFBlocks.SMELTERY_CONTROLLER);
    /** 铸造炉设备物品。 */
    public static final DeferredItem<Item> FOUNDRY_CONTROLLER = block("foundry_controller", TFBlocks.FOUNDRY_CONTROLLER);
    /** 小型熔炼器物品。 */
    public static final DeferredItem<Item> MELTER = block("melter", TFBlocks.MELTER);
    /** 加热器物品。 */
    public static final DeferredItem<Item> HEATER = block("heater", TFBlocks.HEATER);
    /** 合金炉物品。 */
    public static final DeferredItem<Item> ALLOYER = block("alloyer", TFBlocks.ALLOYER);
    /** 储液罐物品。 */
    public static final DeferredItem<FoundryTankItem> SEARED_TANK = tank("seared_tank", TFBlocks.SEARED_TANK, FluidValues.INGOT * 48, false);
    /** 焦黑储液罐物品。 */
    public static final DeferredItem<FoundryTankItem> SCORCHED_TANK = tank("scorched_tank", TFBlocks.SCORCHED_TANK, FluidValues.INGOT * 48, false);
    /** 冶炼燃料罐物品。 */
    public static final DeferredItem<FoundryTankItem> SEARED_FUEL_TANK = tank("seared_fuel_tank", TFBlocks.SEARED_FUEL_TANK, FoundryBlockEntity.DEFAULT_CAPACITY, true);
    /** 焦黑燃料罐物品。 */
    public static final DeferredItem<FoundryTankItem> SCORCHED_FUEL_TANK = tank("scorched_fuel_tank", TFBlocks.SCORCHED_FUEL_TANK, FoundryBlockEntity.DEFAULT_CAPACITY, true);
    /** 冶炼小型浇注储液罐物品。 */
    public static final DeferredItem<FoundryTankItem> SEARED_CASTING_TANK = tank("seared_casting_tank", TFBlocks.SEARED_CASTING_TANK, FluidValues.BUCKET, false);
    /** 焦黑小型浇注储液罐物品。 */
    public static final DeferredItem<FoundryTankItem> SCORCHED_CASTING_TANK = tank("scorched_casting_tank", TFBlocks.SCORCHED_CASTING_TANK, FluidValues.BUCKET, false);
    /** 独立金属锭物品。 */
    public static final Map<String, DeferredItem<Item>> METAL_INGOTS = registerMetalItems("ingot");
    /** 独立金属粒物品。 */
    public static final Map<String, DeferredItem<Item>> METAL_NUGGETS = registerMetalItems("nugget");
    /** 原版没有铜粒，因此由独立命名空间提供铜粒以闭合铜的浇注链。 */
    public static final DeferredItem<Item> COPPER_NUGGET = simple("copper_nugget");
    /** 独立金属块物品。 */
    public static final Map<String, DeferredItem<Item>> METAL_BLOCKS = registerMetalBlocks();
    /** 浇注台物品。 */
    public static final DeferredItem<Item> CASTING_TABLE = block("casting_table", TFBlocks.CASTING_TABLE);
    /** 浇注盆物品。 */
    public static final DeferredItem<Item> CASTING_BASIN = block("casting_basin", TFBlocks.CASTING_BASIN);
    /** 排液口物品。 */
    public static final DeferredItem<Item> DRAIN = block("drain", TFBlocks.DRAIN);
    /** 浇注口物品。 */
    public static final DeferredItem<Item> FAUCET = block("faucet", TFBlocks.FAUCET);
    /** 流体管道物品。 */
    public static final DeferredItem<Item> DUCT = block("duct", TFBlocks.DUCT);
    /** 导流槽物品。 */
    public static final DeferredItem<Item> CHUTE = block("chute", TFBlocks.CHUTE);
    /** 流体计物品。 */
    public static final DeferredItem<Item> FLUID_GAUGE = block("fluid_gauge", TFBlocks.FLUID_GAUGE);

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

    /** 便携储液罐。 */
    public static final DeferredItem<PortableTankItem> PORTABLE_TANK = TinkerFoundry.ITEMS.register("portable_tank", () -> new PortableTankItem(8000, new Item.Properties()));
    /** 铜制便携罐。 */
    public static final DeferredItem<PortableTankItem> COPPER_CANISTER = TinkerFoundry.ITEMS.register("copper_canister", () -> new PortableTankItem(FluidValues.INGOT, new Item.Properties()));
    /** 仅包含本项目冶炼内容的教程书。 */
    public static final DeferredItem<WrittenBookItem> GUIDE_BOOK = TinkerFoundry.ITEMS.register("foundry_guide", () -> new WrittenBookItem(new Item.Properties().stacksTo(1)));

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

    /** 注册基础金属锭或粒，所有结果都保留在独立命名空间。 */
    private static Map<String, DeferredItem<Item>> registerMetalItems(String form) {
        Map<String, DeferredItem<Item>> items = new LinkedHashMap<>();
        for (String metal : TFBlocks.INTERNAL_METALS) {
            items.put(metal, TinkerFoundry.ITEMS.register(metal + "_" + form, () -> new Item(new Item.Properties())));
        }
        return Map.copyOf(items);
    }

    /** 为独立金属块创建对应的方块物品。 */
    private static Map<String, DeferredItem<Item>> registerMetalBlocks() {
        Map<String, DeferredItem<Item>> items = new LinkedHashMap<>();
        for (String metal : TFBlocks.INTERNAL_METALS) {
            items.put(metal, block(metal + "_block", TFBlocks.METAL_BLOCKS.get(metal)));
        }
        return Map.copyOf(items);
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
            default -> EXTRA_BUCKETS.get(name).get();
        };
    }

    private TFItems() {
    }
}
