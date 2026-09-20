package org.hp.tinker_foundry;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFEntities;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.registry.TFRecipes;
import org.hp.tinker_foundry.item.PortableTankFluidHandler;
import org.hp.tinker_foundry.item.FoundryTankItem;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.hp.tinker_foundry.common.FoundryWorldEvents;
import org.hp.tinker_foundry.network.FoundryNetworking;

/** 独立冶炼系统的模组入口。 */
@Mod(TinkerFoundry.MOD_ID)
public final class TinkerFoundry {
    /** 新模组只使用自己的命名空间。 */
    public static final String MOD_ID = "tinker_foundry";
    /** 统一记录加载和异常信息。 */
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 注册方块。 */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    /** 注册物品。 */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    /** 注册流体。 */
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MOD_ID);
    /** 注册流体类型。 */
    public static final DeferredRegister<net.neoforged.neoforge.fluids.FluidType> FLUID_TYPES = DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES, MOD_ID);
    /** 注册方块实体。 */
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    /** 注册流体炮的发射实体。 */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    /** 注册配方类型。 */
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MOD_ID);
    /** 注册配方序列化器。 */
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);
    /** 注册冶炼设备菜单。 */
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    /** 注册创造模式标签页。 */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /** 独立的冶炼系统物品标签页。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, CreativeModeTab> FOUNDRY_TAB = CREATIVE_MODE_TABS.register("foundry", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.tinker_foundry.foundry"))
        .icon(() -> TFItems.SEARED_BRICK.get().getDefaultInstance())
        .displayItems(TinkerFoundry::addFoundryTabItems)
        .build());

    /** 按照匠魂冶炼标签页的分组和顺序输出当前项目已经实现的物品。 */
    private static void addFoundryTabItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        // 输出基础材料，顺序对应匠魂的 crafting materials 分组。
        output.accept(TFItems.GROUT.get());
        output.accept(TFItems.SEARED_BRICK.get());
        output.accept(TFItems.NETHER_GROUT.get());
        output.accept(TFItems.SCORCHED_BRICK.get());
        output.accept(TFItems.COPPER_CANISTER.get());

        // 输出小型设备和冶炼炉控制器，顺序对应匠魂的 controllers 分组。
        output.accept(TFItems.SEARED_MELTER.get());
        output.accept(TFItems.SEARED_HEATER.get());
        output.accept(TFItems.SCORCHED_ALLOYER.get());
        output.accept(TFItems.SMELTERY_CONTROLLER.get());
        output.accept(TFItems.FOUNDRY_CONTROLLER.get());

        // 输出排液口、管道和导流槽，顺序对应匠魂的 IO blocks 分组。
        output.accept(TFItems.SEARED_DRAIN.get());
        output.accept(TFItems.SEARED_DUCT.get());
        output.accept(TFItems.SEARED_CHUTE.get());
        output.accept(TFItems.SCORCHED_DRAIN.get());
        output.accept(TFItems.SCORCHED_DUCT.get());
        output.accept(TFItems.SCORCHED_CHUTE.get());

        // 输出流体计、储液罐和灯，保留当前项目已经实现的额外计量器。
        output.accept(TFItems.COPPER_GAUGE.get());
        output.accept(TFItems.OBSIDIAN_GAUGE.get());
        output.accept(TFItems.SEARED_INGOT_GAUGE.get());
        output.accept(TFItems.SCORCHED_INGOT_GAUGE.get());
        output.accept(TFItems.SEARED_FUEL_GAUGE.get());
        output.accept(TFItems.SCORCHED_FUEL_GAUGE.get());
        output.accept(TFItems.SEARED_INGOT_TANK.get());
        output.accept(TFItems.SEARED_FUEL_TANK.get());
        output.accept(TFItems.SEARED_LANTERN.get());
        output.accept(TFItems.SCORCHED_INGOT_TANK.get());
        output.accept(TFItems.SCORCHED_FUEL_TANK.get());
        output.accept(TFItems.SCORCHED_LANTERN.get());

        // 输出浇注口和疏导槽，顺序对应匠魂的 fluid transfer 分组。
        output.accept(TFItems.SEARED_FAUCET.get());
        output.accept(TFItems.SCORCHED_FAUCET.get());
        output.accept(TFItems.variantBlock("seared_channel"));
        output.accept(TFItems.variantBlock("scorched_channel"));

        // 输出浇注台、浇注盆和当前项目保留的储液附件。
        output.accept(TFItems.SEARED_TABLE.get());
        output.accept(TFItems.SCORCHED_TABLE.get());
        output.accept(TFItems.SEARED_BASIN.get());
        output.accept(TFItems.SCORCHED_BASIN.get());
        output.accept(TFItems.SEARED_CASTING_TANK.get());
        output.accept(TFItems.SCORCHED_PROXY_TANK.get());

        // 输出项目保留的铜流体炮；钴流体炮已按当前内容范围移除。
        output.accept(TFItems.SEARED_FLUID_CANNON.get());

        // 输出冶炼方块变种，顺序对应匠魂的 seared blocks 分组。
        output.accept(TFItems.variantBlock("seared_bricks"));
        output.accept(TFItems.variantBlock("seared_bricks_stairs"));
        output.accept(TFItems.variantBlock("seared_bricks_slab"));
        output.accept(TFItems.variantBlock("seared_bricks_wall"));
        output.accept(TFItems.variantBlock("seared_stone"));
        output.accept(TFItems.variantBlock("seared_stone_stairs"));
        output.accept(TFItems.variantBlock("seared_stone_slab"));
        output.accept(TFItems.variantBlock("seared_cracked_bricks"));
        output.accept(TFItems.variantBlock("seared_fancy_bricks"));
        output.accept(TFItems.variantBlock("seared_triangle_bricks"));
        output.accept(TFItems.variantBlock("seared_cobble"));
        output.accept(TFItems.variantBlock("seared_cobble_stairs"));
        output.accept(TFItems.variantBlock("seared_cobble_slab"));
        output.accept(TFItems.variantBlock("seared_cobble_wall"));
        output.accept(TFItems.variantBlock("seared_paver"));
        output.accept(TFItems.variantBlock("seared_paver_stairs"));
        output.accept(TFItems.variantBlock("seared_paver_slab"));
        output.accept(TFItems.variantBlock("seared_lamp"));
        output.accept(TFItems.SEARED_LADDER.get());
        output.accept(TFItems.SEARED_GLASS.get());
        output.accept(TFItems.variantBlock("seared_tinted_glass"));
        output.accept(TFItems.variantBlock("seared_soul_glass"));
        output.accept(TFItems.variantBlock("seared_glass_pane"));
        output.accept(TFItems.variantBlock("seared_soul_glass_pane"));

        // 输出焦黑方块变种，顺序对应匠魂的 scorched blocks 分组。
        output.accept(TFItems.variantBlock("scorched_bricks"));
        output.accept(TFItems.variantBlock("scorched_bricks_stairs"));
        output.accept(TFItems.variantBlock("scorched_bricks_slab"));
        output.accept(TFItems.variantBlock("scorched_bricks_fence"));
        output.accept(TFItems.variantBlock("chiseled_scorched_bricks"));
        output.accept(TFItems.variantBlock("scorched_stone"));
        output.accept(TFItems.variantBlock("polished_scorched_stone"));
        output.accept(TFItems.variantBlock("scorched_road"));
        output.accept(TFItems.variantBlock("scorched_road_stairs"));
        output.accept(TFItems.variantBlock("scorched_road_slab"));
        output.accept(TFItems.variantBlock("scorched_lamp"));
        output.accept(TFItems.SCORCHED_LADDER.get());
        output.accept(TFItems.SCORCHED_GLASS.get());
        output.accept(TFItems.variantBlock("scorched_tinted_glass"));
        output.accept(TFItems.variantBlock("scorched_soul_glass"));
        output.accept(TFItems.variantBlock("scorched_glass_pane"));
        output.accept(TFItems.variantBlock("scorched_soul_glass_pane"));

        // 输出铸模，顺序对应匠魂的可重复使用铸模、砂模和红砂模分组。
        output.accept(TFItems.INGOT_CAST.get());
        output.accept(TFItems.NUGGET_CAST.get());
        output.accept(TFItems.GEM_CAST.get());
        output.accept(TFItems.INGOT_SAND_CAST.get());
        output.accept(TFItems.NUGGET_SAND_CAST.get());
        output.accept(TFItems.GEM_SAND_CAST.get());
        output.accept(TFItems.INGOT_RED_SAND_CAST.get());
        output.accept(TFItems.NUGGET_RED_SAND_CAST.get());
        output.accept(TFItems.GEM_RED_SAND_CAST.get());

        // 输出熔融流体桶，基础桶沿用原版金属顺序，附加桶沿用注册表稳定顺序。
        output.accept(TFItems.IRON_BUCKET.get());
        output.accept(TFItems.GOLD_BUCKET.get());
        output.accept(TFItems.COPPER_BUCKET.get());
        TFItems.EXTRA_BUCKETS.values().forEach(bucket -> output.accept(bucket.get()));

        // 输出本项目额外提供的铜粒，避免干扰前面的匠魂对照顺序。
        output.accept(TFItems.COPPER_NUGGET.get());
    }

    /** 注册全部独立系统。 */
    public TinkerFoundry(IEventBus modEventBus, ModContainer modContainer) {
        // 在注册事件触发前初始化所有静态注册表，避免能力事件阶段才延迟注册。
        TFBlocks.SEARED_BRICKS.getId();
        TFItems.SEARED_BRICK.getId();
        TFBlockEntities.GENERIC.getId();
        TFEntities.FLUID_CANNON_PROJECTILE.getId();
        TFRecipes.MELTING.getId();
        org.hp.tinker_foundry.registry.TFMenus.FOUNDRY.getId();
        modEventBus.addListener(this::registerCapabilities);
        // 客户端状态载荷只注册到当前模组协议，不改变原版容器包流程。
        modEventBus.addListener(FoundryNetworking::register);
        // 游戏事件总线只负责结构变化和区块载入标记，不让普通设备参与扫描。
        NeoForge.EVENT_BUS.register(FoundryWorldEvents.class);
        BLOCKS.register(modEventBus);
        org.hp.tinker_foundry.registry.TFDataComponents.REGISTER.register(modEventBus);
        ITEMS.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

    }

    /** 注册设备和便携容器的流体能力。 */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TFBlockEntities.GENERIC.get(), (entity, side) -> {
            // 浇注口只通过背面输入和下方输出工作，不应作为普通流体储罐暴露给外部管道。
            if (entity.isFaucetBlock()) return null;
            // 疏导槽按当前连接状态只暴露顶部输入或水平输入面，输出面由服务端逐面推送。
            if (entity.getBlockState().is(TFBlocks.SEARED_CHANNEL.get())
                || entity.getBlockState().is(TFBlocks.SCORCHED_CHANNEL.get())) {
                return org.hp.tinker_foundry.common.FoundryChannelFluidHandler.forSide(entity, side);
            }
            // 导入槽仅处理物品；排液口与导管始终通过有效控制器访问流体。
            if (entity.getBlockState().is(TFBlocks.SEARED_CHUTE.get())
                || entity.getBlockState().is(TFBlocks.SCORCHED_CHUTE.get())) return null;
            if (entity.getBlockState().is(TFBlocks.SEARED_DRAIN.get())
                || entity.getBlockState().is(TFBlocks.SCORCHED_DRAIN.get()) || entity.getBlockState().is(TFBlocks.SEARED_DUCT.get())
                || entity.getBlockState().is(TFBlocks.SCORCHED_DUCT.get())) {
                return new org.hp.tinker_foundry.common.FoundryPortFluidHandler(entity);
            }
            // 合金炉对外只暴露原版的单个输出罐，五个输入罐由自身的邻接模块读取。
            if (entity.isAlloyer()) {
                return entity.alloyerOutputHandler();
            }
            return entity;
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TFBlockEntities.GENERIC.get(), (entity, side) -> {
            // 无物品功能的储罐、排液口和导管不暴露通用的隐藏容器。
            if (entity.isMeltingBlock() || entity.isHeater() || entity.isFuelTankBlock() || entity.isCastingBlock() || entity.isCastingTankBlock()
                || entity.isProxyTankBlock() || entity.isFluidCannonBlock()
                || entity.getBlockState().is(TFBlocks.SEARED_CHUTE.get()) || entity.getBlockState().is(TFBlocks.SCORCHED_CHUTE.get())) {
                return new org.hp.tinker_foundry.common.FoundryItemHandler(entity);
            }
            return null;
        });
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, side) -> new PortableTankFluidHandler(stack, ((org.hp.tinker_foundry.item.PortableTankItem) stack.getItem()).capacity()), TFItems.COPPER_CANISTER.get());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, side) -> {
            FoundryTankItem tank = (FoundryTankItem) stack.getItem();
            return new PortableTankFluidHandler(stack, tank.capacity(), tank.allowsFuel());
        }, TFItems.SEARED_INGOT_TANK.get(), TFItems.SCORCHED_INGOT_TANK.get(),
            TFItems.SEARED_FUEL_TANK.get(), TFItems.SCORCHED_FUEL_TANK.get(), TFItems.SEARED_CASTING_TANK.get(),
            TFItems.SEARED_LANTERN.get(), TFItems.SCORCHED_LANTERN.get(), TFItems.SEARED_FLUID_CANNON.get());
    }
}
