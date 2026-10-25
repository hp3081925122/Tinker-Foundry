package org.hp.tinker_foundry.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFFluids;

/** 统一取得 26.1.2 流体模型的纹理和颜色，保持与 1.21.1 客户端扩展一致。 */
public final class FoundryFluidClient {
    /** 记录已经输出过诊断的流体，避免每帧重复刷日志。 */
    private static final Set<Identifier> LOGGED_FLUIDS = ConcurrentHashMap.newKeySet();

    /** 从当前流体模型的颜色源取得颜色，原版熔岩等非本模组流体也走原版颜色。 */
    public static int tint(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0xFFFFFFFF;
        }
        Fluid fluid = stack.getFluid();
        FluidModel model = model(fluid);
        if (model != null && model.tintSource() != null) {
            return model.tintSource().color(fluid.defaultFluidState().createLegacyBlock());
        }
        return fallbackTint(fluid);
    }

    /** 从当前流体模型取得静止纹理，原版熔岩不再错误使用本模组的灰色熔融纹理。 */
    public static TextureAtlasSprite stillSprite(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Fluid fluid = stack.getFluid();
        FluidModel model = model(fluid);
        if (model != null) {
            return model.stillMaterial().sprite();
        }
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        Identifier texture = id != null && TinkerFoundry.MOD_ID.equals(id.getNamespace())
            ? Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "block/fluid/molten_still")
            : Identifier.fromNamespaceAndPath("minecraft", "block/lava_still");
        return Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, texture));
    }

    /** 从当前流体模型取得流动纹理，供浇注口液柱复刻匠魂的动态纹理。 */
    public static TextureAtlasSprite flowingSprite(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Fluid fluid = stack.getFluid();
        FluidModel model = model(fluid);
        if (model != null) {
            return model.flowingMaterial().sprite();
        }
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        Identifier texture = id != null && TinkerFoundry.MOD_ID.equals(id.getNamespace())
            ? Identifier.fromNamespaceAndPath(TinkerFoundry.MOD_ID, "block/fluid/molten_flow")
            : Identifier.fromNamespaceAndPath("minecraft", "block/lava_flow");
        return Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, texture));
    }

    /** 解析当前流体注册到 26.1.2 模型管理器中的模型，并记录一次颜色来源诊断。 */
    private static FluidModel model(Fluid fluid) {
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        if (id != null && LOGGED_FLUIDS.add(id)) {
            int color = model == null || model.tintSource() == null
                ? fallbackTint(fluid)
                : model.tintSource().color(fluid.defaultFluidState().createLegacyBlock());
            TinkerFoundry.LOGGER.debug("[fluid-render] Resolved fluid model id={} hasTintSource={} tint={}",
                id, model != null && model.tintSource() != null, String.format("0x%08X", color));
        }
        return model;
    }

    /** 在模型重载尚未完成时，为本模组流体提供稳定的颜色回退。 */
    private static int fallbackTint(Fluid fluid) {
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null || !TinkerFoundry.MOD_ID.equals(id.getNamespace())) {
            return 0xFFFFFFFF;
        }
        Integer color = TFFluids.EXTRA_COLORS.get(id.getPath());
        return color == null ? 0xFFFFFFFF : color;
    }

    private FoundryFluidClient() {
    }
}
