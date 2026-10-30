package org.hp.tinker_foundry;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFBlocks;
import org.hp.tinker_foundry.registry.TFEntities;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.registry.TFRecipes;
import org.hp.tinker_foundry.item.PortableTankFluidHandler;
import org.hp.tinker_foundry.item.FoundryTankItem;
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
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    /** 注册物品。 */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    /** 注册流体。 */
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MOD_ID);
    /** 注册流体类型。 */
    public static final DeferredRegister<net.minecraftforge.fluids.FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MOD_ID);
    /** 注册方块实体。 */
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    /** 注册流体炮的发射实体。 */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    /** 注册配方类型。 */
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MOD_ID);
    /** 注册配方序列化器。 */
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);
    /** 注册冶炼设备菜单。 */
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    /** 注册创造模式标签页。 */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /** 独立的冶炼系统物品标签页。 */
    public static final RegistryObject<CreativeModeTab> FOUNDRY_TAB = CREATIVE_MODE_TABS.register("foundry", () -> CreativeModeTab.builder()
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
        output.accept(TFItems.MEAT_SOUP.get());

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
        TFItems.EXTRA_BUCKETS.values().forEach(bucket -> output.accept(bucket.get()));

        // 输出本项目额外提供的铜粒，避免干扰前面的匠魂对照顺序。
        output.accept(TFItems.COPPER_NUGGET.get());
    }

    /** 注册全部独立系统。 */
    public TinkerFoundry(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        // 注册通用配置文件
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, org.hp.tinker_foundry.common.FoundryConfig.SPEC);
        // 在注册事件触发前初始化所有静态注册表，避免能力事件阶段才延迟注册。
        TFBlocks.SEARED_BRICKS.getId();
        TFItems.SEARED_BRICK.getId();
        TFBlockEntities.GENERIC.getId();
        TFEntities.FLUID_CANNON_PROJECTILE.getId();
        TFRecipes.MELTING.getId();
        org.hp.tinker_foundry.registry.TFMenus.FOUNDRY.getId();
        // 客户端状态载荷只注册到当前模组协议，不改变原版容器包流程。
        FoundryNetworking.register();
        // 游戏事件总线只负责结构变化和区块载入标记，不让普通设备参与扫描。
        MinecraftForge.EVENT_BUS.register(FoundryWorldEvents.class);
        MinecraftForge.EVENT_BUS.register(org.hp.tinker_foundry.recipe.AutomaticMeltingRecipes.class);
        // 调试命令只注册服务端命令，供测试文档快速搭建测试场景。
        MinecraftForge.EVENT_BUS.addListener(org.hp.tinker_foundry.debug.FoundryDebugCommand::register);
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, TinkerFoundry::attachItemCapabilities);
        org.hp.tinker_foundry.common.AutomaticFluidResources.register(modEventBus);
        BLOCKS.register(modEventBus);
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

    /** Forge 1.20.1 通过物品附加能力暴露便携罐，保存仍由 ItemStack NBT 负责。 */
    private static void attachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        int capacity;
        boolean allowFuel;
        if (stack.getItem() instanceof org.hp.tinker_foundry.item.PortableTankItem portable) {
            capacity = portable.capacity();
            allowFuel = false;
        } else if (stack.getItem() instanceof FoundryTankItem tank) {
            capacity = tank.capacity();
            allowFuel = tank.allowsFuel();
        } else {
            return;
        }
        PortableTankFluidHandler handler = new PortableTankFluidHandler(stack, capacity, allowFuel);
        event.addCapability(new net.minecraft.resources.ResourceLocation(MOD_ID, "fluid"), new ICapabilityProvider() {
            private final LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> optional = LazyOptional.of(() -> handler);

            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> capability, net.minecraft.core.Direction side) {
                return capability == ForgeCapabilities.FLUID_HANDLER_ITEM ? optional.cast() : LazyOptional.empty();
            }
        });
    }
}
