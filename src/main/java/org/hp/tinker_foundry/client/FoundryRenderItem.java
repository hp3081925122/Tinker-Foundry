package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.hp.tinker_foundry.TinkerFoundry;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** 26.1.2 提交式渲染中的匠魂设备内部物品变换。 */
public record FoundryRenderItem(Vector3f center, float size, float xRotation, float yRotation,
                                ItemDisplayContext displayContext) {
    /** 避免缺失世界的诊断在每帧渲染时重复刷屏。 */
    private static boolean missingLevelLogged;

    /** 代理储罐内部物品位置。 */
    public static final FoundryRenderItem PROXY_TANK = new FoundryRenderItem(
        new Vector3f(8.0F, 9.0F, 8.0F), 12.0F, 0.0F, 0.0F, ItemDisplayContext.FIXED);
    /** 熔化炉内部输入物品位置。 */
    public static final FoundryRenderItem MELTER_INPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 12.0F, 12.0F), 7.5F, 0.0F, 0.0F, ItemDisplayContext.FIXED);
    /** 流体炮水平状态下的内部物品位置。 */
    public static final FoundryRenderItem FLUID_CANNON = new FoundryRenderItem(
        new Vector3f(8.0F, 4.0F, 16.0F), 7.5F, 0.0F, 0.0F, ItemDisplayContext.FIXED);
    /** 浇注台输入物品位置。 */
    public static final FoundryRenderItem CASTING_TABLE_INPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 15.5F, 8.0F), 14.0F, 270.0F, 180.0F, ItemDisplayContext.FIXED);
    /** 浇注台输出物品位置。 */
    public static final FoundryRenderItem CASTING_TABLE_OUTPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 15.5F, 8.0F), 14.1F, 270.0F, 180.0F, ItemDisplayContext.FIXED);
    /** 浇注盆输入物品位置。 */
    public static final FoundryRenderItem CASTING_BASIN_INPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 10.0F, 8.0F), 11.95F, 0.0F, 0.0F, ItemDisplayContext.NONE);
    /** 浇注盆输出物品位置。 */
    public static final FoundryRenderItem CASTING_BASIN_OUTPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 10.0F, 8.0F), 12.0F, 0.0F, 0.0F, ItemDisplayContext.NONE);

    /** 按匠魂设备的中心、缩放和姿态提交物品。 */
    public void submit(ItemStack stack, @Nullable Level level, int seed, PoseStack poseStack,
                       SubmitNodeCollector collector, int packedLight) {
        if (stack.isEmpty() || size <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(center.x() / 16.0F, center.y() / 16.0F, center.z() / 16.0F);
        poseStack.scale(size / 16.0F, size / 16.0F, size / 16.0F);
        if (xRotation != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(xRotation));
        }
        if (yRotation != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        }
        submitInPlace(stack, displayContext, level, seed, poseStack, collector, packedLight);
        poseStack.popPose();
    }

    /** 在调用方已经完成定位和缩放后提交物品。 */
    public static void submitAt(ItemStack stack, @Nullable Level level, int seed, PoseStack poseStack,
                                SubmitNodeCollector collector, int packedLight, float scale,
                                float xRotation, float yRotation, ItemDisplayContext displayContext) {
        if (stack.isEmpty() || scale <= 0.0F) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        if (xRotation != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(xRotation));
        }
        if (yRotation != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        }
        submitInPlace(stack, displayContext, level, seed, poseStack, collector, packedLight);
        poseStack.popPose();
    }

    /** 使用 26.1.2 的物品状态提取器代替已移除的 ItemRenderer.renderStatic。 */
    private static void submitInPlace(ItemStack stack, ItemDisplayContext displayContext,
                                      @Nullable Level level, int seed, PoseStack poseStack,
                                      SubmitNodeCollector collector, int packedLight) {
        ItemStackRenderState renderState = new ItemStackRenderState();
        if (level == null && !missingLevelLogged) {
            missingLevelLogged = true;
            TinkerFoundry.LOGGER.debug("[item-render] Rendering foundry block item without a client level item={} context={}",
                stack.getItem(), displayContext);
        }
        Minecraft.getInstance().getItemModelResolver()
            .updateForTopItem(renderState, stack, displayContext, level, null, seed);
        renderState.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
    }
}
