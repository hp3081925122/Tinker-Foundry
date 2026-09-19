package org.hp.tinker_foundry;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFBlocks;
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
        .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
        .build());

    /** 注册全部独立系统。 */
    public TinkerFoundry(IEventBus modEventBus, ModContainer modContainer) {
        // 在注册事件触发前初始化所有静态注册表，避免能力事件阶段才延迟注册。
        TFBlocks.SEARED_BRICK.getId();
        TFItems.SEARED_BRICK.getId();
        TFBlockEntities.GENERIC.getId();
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
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

    }

    /** 注册设备和便携容器的流体能力。 */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TFBlockEntities.GENERIC.get(), (entity, side) -> {
            // 导入槽仅处理物品；排液口与导管始终通过有效控制器访问流体。
            if (entity.getBlockState().is(TFBlocks.CHUTE.get()) || entity.isHeater()) return null;
            if (entity.getBlockState().is(TFBlocks.DRAIN.get()) || entity.getBlockState().is(TFBlocks.DUCT.get())) {
                return new org.hp.tinker_foundry.common.FoundryPortFluidHandler(entity);
            }
            return entity;
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TFBlockEntities.GENERIC.get(), (entity, side) -> {
            // 无物品功能的储罐、排液口和导管不暴露通用的隐藏容器。
            if (entity.isMeltingBlock() || entity.isHeater() || entity.isFuelTankBlock() || entity.isCastingBlock() || entity.isCastingTankBlock()
                || entity.getBlockState().is(TFBlocks.CHUTE.get())) return new org.hp.tinker_foundry.common.FoundryItemHandler(entity);
            return null;
        });
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, side) -> new PortableTankFluidHandler(stack, ((org.hp.tinker_foundry.item.PortableTankItem) stack.getItem()).capacity()), TFItems.PORTABLE_TANK.get(), TFItems.COPPER_CANISTER.get());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, side) -> {
            FoundryTankItem tank = (FoundryTankItem) stack.getItem();
            return new PortableTankFluidHandler(stack, tank.capacity(), tank.allowsFuel());
        }, TFItems.SEARED_TANK.get(), TFItems.SCORCHED_TANK.get(), TFItems.SEARED_FUEL_TANK.get(), TFItems.SCORCHED_FUEL_TANK.get(), TFItems.SEARED_CASTING_TANK.get(), TFItems.SCORCHED_CASTING_TANK.get(), TFItems.SEARED_LANTERN.get(), TFItems.SCORCHED_LANTERN.get());
    }
}
