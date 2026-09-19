package org.hp.tinker_foundry.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFMenus;
import org.hp.tinker_foundry.registry.TFFluids;
import org.hp.tinker_foundry.registry.TFItems;
import org.hp.tinker_foundry.registry.TFBlockEntities;
import org.hp.tinker_foundry.registry.TFBlocks;

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
    }

    /** 为所有熔融流体注册独立颜色和本模组命名空间的流体动画纹理。 */
    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        TFFluids.EXTRA_TYPES.forEach((name, type) -> event.registerFluidType(new MoltenFluidExtensions(TFFluids.EXTRA_COLORS.get(name)), type.get()));
    }

    /** 注册动态流体容器的流体色，保证创造栏和手持容器显示真实熔融颜色。 */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        DynamicFluidContainerModel.Colors colors = new DynamicFluidContainerModel.Colors();
        TFItems.EXTRA_BUCKETS.values().forEach(bucket -> event.register(colors, bucket.get()));
        event.register(colors, TFItems.PORTABLE_TANK.get(), TFItems.COPPER_CANISTER.get());
        // 金属锭和金属粒使用同一套灰度像素材质，由客户端颜色处理器注入对应金属色。
        for (String metal : TFBlocks.INTERNAL_METALS) {
            int tint = metalColor(metal);
            event.register((stack, tintIndex) -> tintIndex == 0 ? tint : -1,
                TFItems.METAL_INGOTS.get(metal).get(), TFItems.METAL_NUGGETS.get(metal).get(), TFItems.METAL_BLOCKS.get(metal).get());
        }
        // 原版没有铜粒，独立铜粒沿用统一灰度物品模型和铜色着色器。
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFF47B45 : -1, TFItems.COPPER_NUGGET.get());
    }

    /** 为独立金属存储方块注册与锭粒一致的世界和创造栏颜色。 */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for (String metal : TFBlocks.INTERNAL_METALS) {
            int tint = metalColor(metal);
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? tint : -1,
                TFBlocks.METAL_BLOCKS.get(metal).get());
        }
    }

    /** 返回基础金属和合金使用的稳定像素色。 */
    private static int metalColor(String metal) {
        return switch (metal) {
            case "tin" -> 0xFFD5E4E8;
            case "lead" -> 0xFF6B6D83;
            case "silver" -> 0xFFE7EDF2;
            case "nickel" -> 0xFFD6CDB7;
            case "zinc" -> 0xFFC6D4DA;
            case "aluminum" -> 0xFFD8D8D8;
            case "steel" -> 0xFF6F7784;
            case "bronze" -> 0xFFCD7842;
            case "brass" -> 0xFFF0B83F;
            case "electrum" -> 0xFFF5E276;
            case "invar" -> 0xFFB6B8AD;
            case "constantan" -> 0xFFD89456;
            default -> 0xFFFFFFFF;
        };
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
