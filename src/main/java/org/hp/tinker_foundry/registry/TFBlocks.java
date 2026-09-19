package org.hp.tinker_foundry.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.FoundryEntityBlock;
import org.hp.tinker_foundry.block.FoundryMachineBlock;
import org.hp.tinker_foundry.block.FoundryControllerBlock;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.block.FoundryFaucetBlock;
import org.hp.tinker_foundry.block.FoundryGaugeBlock;
import org.hp.tinker_foundry.block.FoundryLanternBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.material.MapColor;

/** 冶炼链全部方块注册表。 */
public final class TFBlocks {
    /** 独立模组提供的基础金属和合金存储方块名称。 */
    public static final String[] INTERNAL_METALS = {"tin", "lead", "silver", "nickel", "zinc", "aluminum", "steel", "bronze", "brass", "electrum", "invar", "constantan"};

    /** 普通冶炼砖。 */
    public static final DeferredBlock<Block> SEARED_BRICK = simple("seared_brick", MapColor.COLOR_GRAY);
    /** 冶炼玻璃。 */
    public static final DeferredBlock<Block> SEARED_GLASS = glass("seared_glass", MapColor.COLOR_LIGHT_GRAY);
    /** 冶炼灯。 */
    public static final DeferredBlock<Block> SEARED_LANTERN = lantern("seared_lantern", MapColor.COLOR_ORANGE);
    /** 冶炼梯子。 */
    public static final DeferredBlock<Block> SEARED_LADDER = ladder("seared_ladder", MapColor.COLOR_GRAY);
    /** 冶炼墙。 */
    public static final DeferredBlock<Block> SEARED_WALL = wall("seared_wall", MapColor.COLOR_GRAY);
    /** 冶炼装饰砖。 */
    public static final DeferredBlock<Block> SEARED_FANCY_BRICK = simple("seared_fancy_brick", MapColor.COLOR_GRAY);
    /** 焦黑冶炼砖。 */
    public static final DeferredBlock<Block> SCORCHED_BRICK = simple("scorched_brick", MapColor.COLOR_BLACK);
    /** 焦黑冶炼玻璃。 */
    public static final DeferredBlock<Block> SCORCHED_GLASS = glass("scorched_glass", MapColor.COLOR_BLACK);
    /** 焦黑冶炼灯。 */
    public static final DeferredBlock<Block> SCORCHED_LANTERN = lantern("scorched_lantern", MapColor.COLOR_ORANGE);
    /** 焦黑冶炼梯子。 */
    public static final DeferredBlock<Block> SCORCHED_LADDER = ladder("scorched_ladder", MapColor.COLOR_BLACK);
    /** 焦黑冶炼墙。 */
    public static final DeferredBlock<Block> SCORCHED_WALL = wall("scorched_wall", MapColor.COLOR_BLACK);
    /** 焦黑装饰砖。 */
    public static final DeferredBlock<Block> SCORCHED_FANCY_BRICK = simple("scorched_fancy_brick", MapColor.COLOR_BLACK);

    /** 冶炼炉控制器。 */
    public static final DeferredBlock<Block> SMELTERY_CONTROLLER = controller("smeltery_controller");
    /** 铸造炉控制器。 */
    public static final DeferredBlock<Block> FOUNDRY_CONTROLLER = controller("foundry_controller");
    /** 小型熔炼器。 */
    public static final DeferredBlock<Block> MELTER = machine("melter");
    /** 加热器。 */
    public static final DeferredBlock<Block> HEATER = machine("heater");
    /** 合金炉。 */
    public static final DeferredBlock<Block> ALLOYER = machine("alloyer");
    /** 冶炼储液罐。 */
    public static final DeferredBlock<Block> SEARED_TANK = entity("seared_tank");
    /** 焦黑储液罐。 */
    public static final DeferredBlock<Block> SCORCHED_TANK = entity("scorched_tank");
    /** 冶炼燃料罐，允许存放燃料配方支持的流体。 */
    public static final DeferredBlock<Block> SEARED_FUEL_TANK = entity("seared_fuel_tank");
    /** 焦黑燃料罐，允许存放燃料配方支持的流体。 */
    public static final DeferredBlock<Block> SCORCHED_FUEL_TANK = entity("scorched_fuel_tank");
    /** 冶炼专用小型浇注储液罐。 */
    public static final DeferredBlock<Block> SEARED_CASTING_TANK = entity("seared_casting_tank");
    /** 焦黑专用小型浇注储液罐。 */
    public static final DeferredBlock<Block> SCORCHED_CASTING_TANK = entity("scorched_casting_tank");
    /** 独立金属存储方块，供本模组完整浇注链使用。 */
    public static final Map<String, DeferredBlock<Block>> METAL_BLOCKS = registerMetalBlocks();
    /** 浇注台。 */
    public static final DeferredBlock<Block> CASTING_TABLE = entity("casting_table");
    /** 浇注盆。 */
    public static final DeferredBlock<Block> CASTING_BASIN = entity("casting_basin");
    /** 排液口。 */
    public static final DeferredBlock<Block> DRAIN = directional("drain");
    /** 浇注口。 */
    public static final DeferredBlock<Block> FAUCET = faucet("faucet");
    /** 流体管道。 */
    public static final DeferredBlock<Block> DUCT = directional("duct");
    /** 导流槽。 */
    public static final DeferredBlock<Block> CHUTE = directional("chute");
    /** 贴壁流体计，使用独立的薄片碰撞箱和存活规则。 */
    public static final DeferredBlock<Block> FLUID_GAUGE = gauge("fluid_gauge");

    /** 创建普通冶炼方块。 */
    private static DeferredBlock<Block> simple(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of().mapColor(color).strength(2.0f, 6.0f));
    }

    /** 注册可透光但保持完整碰撞箱的冶炼玻璃。 */
    private static DeferredBlock<Block> glass(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new TransparentBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GLASS)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册带光照和悬挂状态的冶炼灯。 */
    private static DeferredBlock<Block> lantern(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryLanternBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.LANTERN)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册可攀爬并依附墙面的冶炼梯子。 */
    private static DeferredBlock<Block> ladder(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new LadderBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.LADDER)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册会根据相邻方块连接的冶炼墙。 */
    private static DeferredBlock<Block> wall(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COBBLESTONE_WALL)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 创建需要持久化数据的设备方块。 */
    private static DeferredBlock<Block> entity(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryEntityBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册使用官方正面模型的水平朝向设备。 */
    private static DeferredBlock<Block> machine(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册带水平朝向的多方块控制器。 */
    private static DeferredBlock<Block> controller(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册带六向朝向的流体输入输出设备。 */
    private static DeferredBlock<Block> directional(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryDirectionalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册具有特殊连接面放置规则的浇注口。 */
    private static DeferredBlock<Block> faucet(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryFaucetBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册只占据贴附面薄片空间的流体计。 */
    private static DeferredBlock<Block> gauge(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryGaugeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册没有额外方块逻辑的金属存储方块。 */
    private static Map<String, DeferredBlock<Block>> registerMetalBlocks() {
        Map<String, DeferredBlock<Block>> blocks = new LinkedHashMap<>();
        for (String metal : INTERNAL_METALS) {
            blocks.put(metal, TinkerFoundry.BLOCKS.register(metal + "_block", () -> new Block(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0f, 6.0f)
            )));
        }
        return Map.copyOf(blocks);
    }

    private TFBlocks() {
    }
}
