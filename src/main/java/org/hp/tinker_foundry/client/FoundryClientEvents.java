package org.hp.tinker_foundry.client;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFMenus;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFEntities;
import org.hp.tinker_foundry.client.model.TankModel;
import org.hp.tinker_foundry.client.model.ConnectedGlassModel;
import org.hp.tinker_foundry.client.model.FoundryTankItemModel;

/** 注册熔融流体的客户端颜色和流体表面纹理。 */
@EventBusSubscriber(modid = TinkerFoundry.MOD_ID, value = Dist.CLIENT)
public final class FoundryClientEvents {
    /** 注册所有冶炼设备共用的菜单界面。 */
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TFMenus.FOUNDRY.get(), FoundryScreen::new);
    }

    /** 注册储液罐和浇注盆的动态流体方块实体渲染器。 */
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TFBlockEntities.GENERIC.get(), FoundryBlockEntityRenderer::new);
        event.registerEntityRenderer(TFEntities.FLUID_CANNON_PROJECTILE.get(), FoundryFluidCannonProjectileRenderer::new);
    }

    /** 注册匠魂兼容的储液罐几何加载器，负责物品栏动态流体和 GUI 简化模型。 */
    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
        event.register(Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "tank"), TankModel.LOADER);
        event.register(Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "connected"), ConnectedGlassModel.LOADER);
    }

    /** 注册方块状态层的连接玻璃模型，按相邻方块选择连接纹理变体。 */
    @SubscribeEvent
    public static void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "connected"), ConnectedGlassModel.BLOCK_STATE_CODEC);
    }

    /** 注册读取物品流体组件并按容量生成液面的新版物品模型。 */
    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "tank"), FoundryTankItemModel.MAP_CODEC);
        TinkerFoundry.LOGGER.debug("[item-model] Registered dynamic foundry tank item model");
    }

    /** 资源重载后清空自动流体取色缓存，按新材质重新取色。 */
    @SubscribeEvent
    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "automatic_fluid_colors"),
            (ResourceManagerReloadListener) resourceManager -> AutomaticFluidColors.clear());
    }

    /** 为所有熔融流体注册静止纹理、流动纹理和新版流体颜色源。 */
    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        TFFluids.EXTRA_SOURCES.forEach((name, source) -> registerFluidModel(event, name, source.get(), TFFluids.EXTRA_COLORS.get(name)));
    }

    /** 将一个源流体和对应流动流体绑定到同一套熔融纹理和固定颜色。 */
    private static void registerFluidModel(RegisterFluidModelsEvent event, String name, Fluid source, int color) {
        Identifier flowingId = Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, name + "_flowing");
        Fluid flowing = BuiltInRegistries.FLUID.getOptional(flowingId).orElse(null);
        if (flowing == null) {
            TinkerFoundry.LOGGER.warn("[fluid] Missing flowing fluid for {}", flowingId);
            return;
        }
        Material still = new Material(Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "block/fluid/molten_still"));
        Material flow = new Material(Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "block/fluid/molten_flow"));
        // 自动流体没有显式颜色时按代表物品材质取色，其余流体保持注册颜色
        FluidTintSource tintSource = new FluidTintSource() {
            @Override
            public int color(FluidState state) {
                return AutomaticFluidColors.tint(name, color);
            }
        };
        FluidModel.Unbaked model = new FluidModel.Unbaked(still, flow, null, tintSource);
        event.register(model, source, flowing);
    }

    private FoundryClientEvents() {
    }
}
