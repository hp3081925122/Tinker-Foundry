package org.hp.tinker_foundry.registry;

import net.neoforged.neoforge.registries.DeferredBlock;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.FoundryChannelBlock;
import org.hp.tinker_foundry.block.FoundryEntityBlock;
import org.hp.tinker_foundry.block.FoundryAlloyerBlock;
import org.hp.tinker_foundry.block.FoundryFluidCannonBlock;
import org.hp.tinker_foundry.block.FoundryMachineBlock;
import org.hp.tinker_foundry.block.FoundryControllerBlock;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.block.FoundryHorizontalBlock;
import org.hp.tinker_foundry.block.FoundryFaucetBlock;
import org.hp.tinker_foundry.block.FoundryGaugeBlock;
import org.hp.tinker_foundry.block.FoundryLanternBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.material.MapColor;

/** 冶炼链全部方块注册表。 */
public final class TFBlocks {
    /** 普通砂浆，烧制为冶炼砖的基础材料。 */
    public static final DeferredBlock<Block> GROUT = simple("grout", MapColor.COLOR_LIGHT_GRAY);
    /** 下界砂浆，烧制为焦黑砖的基础材料。 */
    public static final DeferredBlock<Block> NETHER_GROUT = simple("nether_grout", MapColor.COLOR_BROWN);

    /** 冶炼石及其完整的楼梯、台阶变种。 */
    public static final DeferredBlock<Block> SEARED_STONE = simple("seared_stone", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_STONE_STAIRS = stairs("seared_stone_stairs", SEARED_STONE, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_STONE_SLAB = slab("seared_stone_slab", MapColor.COLOR_GRAY);
    /** 冶炼圆石及其楼梯、台阶、墙变种。 */
    public static final DeferredBlock<Block> SEARED_COBBLE = simple("seared_cobble", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_COBBLE_STAIRS = stairs("seared_cobble_stairs", SEARED_COBBLE, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_COBBLE_SLAB = slab("seared_cobble_slab", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_COBBLE_WALL = wall("seared_cobble_wall", MapColor.COLOR_GRAY);
    /** 冶炼铺路砖及其楼梯、台阶变种。 */
    public static final DeferredBlock<Block> SEARED_PAVER = simple("seared_paver", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_PAVER_STAIRS = stairs("seared_paver_stairs", SEARED_PAVER, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_PAVER_SLAB = slab("seared_paver_slab", MapColor.COLOR_GRAY);
    /** 冶炼砖墙及其楼梯、台阶、墙变种。 */
    public static final DeferredBlock<Block> SEARED_BRICKS = simple("seared_bricks", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_BRICKS_STAIRS = stairs("seared_bricks_stairs", SEARED_BRICKS, MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_BRICKS_SLAB = slab("seared_bricks_slab", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_BRICKS_WALL = wall("seared_bricks_wall", MapColor.COLOR_GRAY);
    /** 冶炼砖的裂纹、精致和三角装饰变种。 */
    public static final DeferredBlock<Block> SEARED_CRACKED_BRICKS = simple("seared_cracked_bricks", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_FANCY_BRICKS = simple("seared_fancy_bricks", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_TRIANGLE_BRICKS = simple("seared_triangle_bricks", MapColor.COLOR_GRAY);
    /** 冶炼玻璃。 */
    public static final DeferredBlock<Block> SEARED_GLASS = glass("seared_glass", MapColor.COLOR_LIGHT_GRAY);
    /** 冶炼灯。 */
    public static final DeferredBlock<Block> SEARED_LANTERN = lantern("seared_lantern", MapColor.COLOR_ORANGE);
    /** 冶炼梯子。 */
    public static final DeferredBlock<Block> SEARED_LADDER = ladder("seared_ladder", MapColor.COLOR_GRAY);
    /** 冶炼灯方块，不含储液功能；储液灯仍使用 seared_lantern。 */
    public static final DeferredBlock<Block> SEARED_LAMP = lightBlock("seared_lamp", MapColor.COLOR_ORANGE);
    /** 焦黑石柱、砖、道路及其完整变种。 */
    public static final DeferredBlock<Block> SCORCHED_STONE = pillar("scorched_stone", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> POLISHED_SCORCHED_STONE = pillar("polished_scorched_stone", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_BRICKS = simple("scorched_bricks", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_BRICKS_STAIRS = stairs("scorched_bricks_stairs", SCORCHED_BRICKS, MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_BRICKS_SLAB = slab("scorched_bricks_slab", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_BRICKS_FENCE = fence("scorched_bricks_fence", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_ROAD = simple("scorched_road", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_ROAD_STAIRS = stairs("scorched_road_stairs", SCORCHED_ROAD, MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_ROAD_SLAB = slab("scorched_road_slab", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> CHISELED_SCORCHED_BRICKS = simple("chiseled_scorched_bricks", MapColor.COLOR_BLACK);
    /** 焦黑冶炼玻璃。 */
    public static final DeferredBlock<Block> SCORCHED_GLASS = glass("scorched_glass", MapColor.COLOR_BLACK);
    /** 焦黑冶炼灯。 */
    public static final DeferredBlock<Block> SCORCHED_LANTERN = lantern("scorched_lantern", MapColor.COLOR_ORANGE);
    /** 焦黑冶炼梯子。 */
    public static final DeferredBlock<Block> SCORCHED_LADDER = ladder("scorched_ladder", MapColor.COLOR_BLACK);
    /** 焦黑灯方块，不含储液功能；储液灯仍使用 scorched_lantern。 */
    public static final DeferredBlock<Block> SCORCHED_LAMP = lightBlock("scorched_lamp", MapColor.COLOR_ORANGE);

    /** 冶炼透明方块的面板、染色玻璃和灵魂玻璃变种。 */
    public static final DeferredBlock<Block> SEARED_GLASS_PANE = pane("seared_glass_pane", MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredBlock<Block> SEARED_TINTED_GLASS = tintedGlass("seared_tinted_glass", MapColor.COLOR_GRAY);
    public static final DeferredBlock<Block> SEARED_SOUL_GLASS = soulGlass("seared_soul_glass", MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredBlock<Block> SEARED_SOUL_GLASS_PANE = pane("seared_soul_glass_pane", MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredBlock<Block> SCORCHED_GLASS_PANE = pane("scorched_glass_pane", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_TINTED_GLASS = tintedGlass("scorched_tinted_glass", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_SOUL_GLASS = soulGlass("scorched_soul_glass", MapColor.COLOR_BLACK);
    public static final DeferredBlock<Block> SCORCHED_SOUL_GLASS_PANE = pane("scorched_soul_glass_pane", MapColor.COLOR_BLACK);

    /** 冶炼炉控制器。 */
    public static final DeferredBlock<Block> SMELTERY_CONTROLLER = controller("smeltery_controller");
    /** 铸造炉控制器。 */
    public static final DeferredBlock<Block> FOUNDRY_CONTROLLER = controller("foundry_controller");
    /** 原版命名的小型冶炼器。 */
    public static final DeferredBlock<Block> SEARED_MELTER = machine("seared_melter");
    /** 原版命名的加热器。 */
    public static final DeferredBlock<Block> SEARED_HEATER = machine("seared_heater");
    /** 原版命名的合金炉。 */
    public static final DeferredBlock<Block> SCORCHED_ALLOYER = alloyer("scorched_alloyer");
    /** 冶炼燃料罐，允许存放燃料配方支持的流体。 */
    public static final DeferredBlock<Block> SEARED_FUEL_TANK = entity("seared_fuel_tank");
    /** 焦黑燃料罐，允许存放燃料配方支持的流体。 */
    public static final DeferredBlock<Block> SCORCHED_FUEL_TANK = entity("scorched_fuel_tank");
    /** 冶炼专用小型浇注储液罐。 */
    public static final DeferredBlock<Block> SEARED_CASTING_TANK = entity("seared_casting_tank");
    /** 可在六个方向自动连接和传输流体的导流槽。 */
    public static final DeferredBlock<Block> SEARED_CHANNEL = channel("seared_channel");
    public static final DeferredBlock<Block> SCORCHED_CHANNEL = channel("scorched_channel");
    /** 原版命名的两套排液附件。 */
    public static final DeferredBlock<Block> SEARED_DRAIN = horizontal("seared_drain");
    public static final DeferredBlock<Block> SCORCHED_DRAIN = horizontal("scorched_drain");
    public static final DeferredBlock<Block> SEARED_DUCT = horizontal("seared_duct");
    public static final DeferredBlock<Block> SCORCHED_DUCT = horizontal("scorched_duct");
    public static final DeferredBlock<Block> SEARED_CHUTE = horizontal("seared_chute");
    public static final DeferredBlock<Block> SCORCHED_CHUTE = horizontal("scorched_chute");
    public static final DeferredBlock<Block> SEARED_FAUCET = faucet("seared_faucet");
    public static final DeferredBlock<Block> SCORCHED_FAUCET = faucet("scorched_faucet");
    /** 两套材质的浇注台和浇注盆。 */
    public static final DeferredBlock<Block> SEARED_TABLE = entity("seared_table");
    public static final DeferredBlock<Block> SCORCHED_TABLE = entity("scorched_table");
    public static final DeferredBlock<Block> SEARED_BASIN = entity("seared_basin");
    public static final DeferredBlock<Block> SCORCHED_BASIN = entity("scorched_basin");
    /** 焦黑代理储罐，容器流体能力由方块实体内部的物品代理。 */
    public static final DeferredBlock<Block> SCORCHED_PROXY_TANK = entity("scorched_proxy_tank");
    /** 铜流体炮，红石触发后按匠魂原版参数发射流体弹。 */
    public static final DeferredBlock<Block> SEARED_FLUID_CANNON = fluidCannon("seared_fluid_cannon", 1.0F, 1.1F, 6.0F);
    /** 原版命名的铜、黑曜石贴壁流体计。 */
    public static final DeferredBlock<Block> COPPER_GAUGE = gauge("copper_gauge");
    public static final DeferredBlock<Block> OBSIDIAN_GAUGE = gauge("obsidian_gauge");
    /** 原版命名的独立锭储罐量器，外形与普通储罐一致。 */
    public static final DeferredBlock<Block> SEARED_INGOT_GAUGE = entity("seared_ingot_gauge");
    public static final DeferredBlock<Block> SCORCHED_INGOT_GAUGE = entity("scorched_ingot_gauge");
    /** 原版命名的独立燃料储罐量器，外形与普通储罐一致。 */
    public static final DeferredBlock<Block> SEARED_FUEL_GAUGE = entity("seared_fuel_gauge");
    public static final DeferredBlock<Block> SCORCHED_FUEL_GAUGE = entity("scorched_fuel_gauge");
    /** 原版命名的大容量金属储罐。 */
    public static final DeferredBlock<Block> SEARED_INGOT_TANK = entity("seared_ingot_tank");
    public static final DeferredBlock<Block> SCORCHED_INGOT_TANK = entity("scorched_ingot_tank");

    /** 创建普通冶炼方块。 */
    private static DeferredBlock<Block> simple(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of().mapColor(color).strength(2.0f, 6.0f));
    }

    /** 注册具有原版方块硬度的楼梯变种，并让楼梯继承对应基础方块的默认状态。 */
    private static DeferredBlock<Block> stairs(String name, java.util.function.Supplier<? extends Block> base, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new StairBlock(base.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册台阶变种。 */
    private static DeferredBlock<Block> slab(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册围栏变种。 */
    private static DeferredBlock<Block> fence(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICK_FENCE)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册轴向纹理砖，供焦黑石柱类变种使用。 */
    private static DeferredBlock<Block> pillar(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BASALT)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册可连接的玻璃面板。 */
    private static DeferredBlock<Block> pane(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new IronBarsBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册具有染色玻璃遮挡性质的冶炼玻璃。 */
    private static DeferredBlock<Block> tintedGlass(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new TintedGlassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TINTED_GLASS)
            .mapColor(color).strength(2.0f, 6.0f)));
    }

    /** 注册低碰撞、减速并阻挡视线的灵魂玻璃基础方块。 */
    private static DeferredBlock<Block> soulGlass(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new TransparentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
            .mapColor(color).strength(2.0f, 6.0f).noOcclusion().speedFactor(0.1f)));
    }

    /** 注册只提供光照的结构灯。 */
    private static DeferredBlock<Block> lightBlock(String name, MapColor color) {
        return TinkerFoundry.BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE)
            .mapColor(color).strength(2.0f, 6.0f).lightLevel(state -> 15)));
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
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryEntityBlock(nonSolidDeviceProperties(MapColor.COLOR_GRAY, 2.0f, 6.0f)));
    }

    /** 注册使用官方正面模型的水平朝向设备。 */
    private static DeferredBlock<Block> machine(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryMachineBlock(nonSolidDeviceProperties(MapColor.COLOR_GRAY, 2.0f, 6.0f)));
    }

    /** 注册带五向输入缓存和下方燃料结构状态的合金炉控制器。 */
    private static DeferredBlock<Block> alloyer(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryAlloyerBlock(nonSolidDeviceProperties(MapColor.COLOR_GRAY, 2.0f, 6.0f)));
    }

    /** 注册带水平朝向的多方块控制器。 */
    private static DeferredBlock<Block> controller(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryControllerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册带六向朝向的流体输入输出设备。 */
    private static DeferredBlock<Block> directional(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryDirectionalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册与上游一致、只能沿水平面放置的排液附件。 */
    private static DeferredBlock<Block> horizontal(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryHorizontalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 6.0f)));
    }

    /** 注册具备多面传输逻辑的导流槽。 */
    private static DeferredBlock<Block> channel(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryChannelBlock(nonSolidDeviceProperties(MapColor.COLOR_GRAY, 2.0f, 6.0f)));
    }

    /** 注册带红石触发状态和独立弹射参数的流体炮。 */
    private static DeferredBlock<Block> fluidCannon(String name, float power, float velocity, float inaccuracy) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryFluidCannonBlock(
            nonSolidDeviceProperties(MapColor.COLOR_GRAY, 2.0f, 6.0f), power, velocity, inaccuracy));
    }

    /** 注册具有特殊连接面放置规则的浇注口。 */
    private static DeferredBlock<Block> faucet(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryFaucetBlock(nonSolidDeviceProperties(MapColor.COLOR_GRAY, 2.0f, 6.0f)));
    }

    /** 注册只占据贴附面薄片空间的流体计。 */
    private static DeferredBlock<Block> gauge(String name) {
        return TinkerFoundry.BLOCKS.register(name, () -> new FoundryGaugeBlock(nonSolidDeviceProperties(MapColor.COLOR_GRAY, 2.0f, 6.0f).noCollission()));
    }

    /** 构造匠魂非实体设备使用的遮挡属性，避免透明模型面被方块渲染器裁掉。 */
    private static BlockBehaviour.Properties nonSolidDeviceProperties(MapColor color, float destroyTime, float explosionResistance) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(destroyTime, explosionResistance)
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false)
            .forceSolidOn();
    }

    private TFBlocks() {
    }
}
