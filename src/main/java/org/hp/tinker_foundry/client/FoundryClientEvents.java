package org.hp.tinker_foundry.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.model.DynamicFluidContainerModel;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFMenus;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFEntities;
import org.hp.tinker_foundry.client.model.TankModel;
import org.hp.tinker_foundry.client.model.ConnectedGlassModel;

/** 注册熔融流体的客户端颜色和流体表面纹理。 */
@EventBusSubscriber(modid = TinkerFoundry.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FoundryClientEvents {
    /** 注册所有冶炼设备共用的菜单界面。 */
    @SubscribeEvent
    public static void registerMenuScreens(FMLClientSetupEvent event) {
        // Forge 1.20.1 没有对应的 RegisterMenuScreensEvent，菜单屏幕必须在客户端初始化阶段排队注册。
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(TFMenus.FOUNDRY.get(), FoundryScreen::new));
    }

    /** 注册储液罐和浇注盆的动态流体方块实体渲染器。 */
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TFBlockEntities.GENERIC.get(), FoundryBlockEntityRenderer::new);
        event.registerEntityRenderer(TFEntities.FLUID_CANNON_PROJECTILE.get(), FoundryFluidCannonProjectileRenderer::new);
    }

    /** 注册匠魂兼容的储液罐几何加载器，负责物品栏动态流体和 GUI 简化模型。 */
    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("tank", TankModel.LOADER);
        event.register("connected", ConnectedGlassModel.LOADER);
    }

    /** 注册动态流体容器的流体色，保证创造栏和手持容器显示真实熔融颜色。 */
    /** 资源重载后清空自动流体取色缓存，按新材质重新取色。 */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> AutomaticFluidColors.clear());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        DynamicFluidContainerModel.Colors colors = new DynamicFluidContainerModel.Colors();
        TFItems.EXTRA_BUCKETS.values().forEach(bucket -> event.register(colors, bucket.get()));
        event.register(colors, TFItems.COPPER_CANISTER.get());
        // 原版没有铜粒，独立铜粒沿用统一灰度物品模型和铜色着色器。
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFF47B45 : -1, TFItems.COPPER_NUGGET.get());
    }

    private FoundryClientEvents() {
    }
}
