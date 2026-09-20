package org.hp.tinker_foundry.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFMenus;
import org.hp.tinker_foundry.registry.TFFluids;
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
    public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "tank"), TankModel.LOADER);
        event.register(ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "connected"), ConnectedGlassModel.LOADER);
    }

    /** 为所有熔融流体注册独立颜色和本模组命名空间的流体动画纹理。 */
    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        TFFluids.ORIGINAL_TYPES.forEach((name, type) -> event.registerFluidType(new MoltenFluidExtensions(TFFluids.ORIGINAL_COLORS.get(name)), type.get()));
        TFFluids.EXTRA_TYPES.forEach((name, type) -> event.registerFluidType(new MoltenFluidExtensions(TFFluids.EXTRA_COLORS.get(name)), type.get()));
    }

    /** 注册动态流体容器的流体色，保证创造栏和手持容器显示真实熔融颜色。 */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        DynamicFluidContainerModel.Colors colors = new DynamicFluidContainerModel.Colors();
        event.register(colors, TFItems.IRON_BUCKET.get(), TFItems.GOLD_BUCKET.get(), TFItems.COPPER_BUCKET.get());
        TFItems.EXTRA_BUCKETS.values().forEach(bucket -> event.register(colors, bucket.get()));
        event.register(colors, TFItems.COPPER_CANISTER.get());
        // 原版没有铜粒，独立铜粒沿用统一灰度物品模型和铜色着色器。
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFF47B45 : -1, TFItems.COPPER_NUGGET.get());
    }

    /** 保存一种熔融流体的颜色并提供原版流体动画资源。 */
    private record MoltenFluidExtensions(int tintColor) implements IClientFluidTypeExtensions {
        /** 返回本模组自己的静止熔融纹理。 */
        @Override
        public ResourceLocation getStillTexture() {
            return ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "block/fluid/molten_still");
        }

        /** 返回本模组自己的流动熔融纹理。 */
        @Override
        public ResourceLocation getFlowingTexture() {
            return ResourceLocation.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "block/fluid/molten_flow");
        }

        /** 返回流体颜色。 */
        @Override
        public int getTintColor() {
            return tintColor;
        }
    }

    private FoundryClientEvents() {
    }
}
