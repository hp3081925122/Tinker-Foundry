package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/** Mantle RenderItem 的本地等价实现，用于设备内部物品的中心、缩放和姿态变换。 */
public record FoundryRenderItem(Vector3f center, float size, float xRotation, float yRotation,
                                ItemDisplayContext displayContext) {
    /** 代理储罐内部物品的原版匠魂位置参数：中心为 8,9,8 像素，尺寸为 12 像素。 */
    public static final FoundryRenderItem PROXY_TANK = new FoundryRenderItem(
        new Vector3f(8.0F, 9.0F, 8.0F), 12.0F, 0.0F, 0.0F, ItemDisplayContext.FIXED);

    /** 熔化炉内部物品的原版匠魂位置参数：中心为 8,12,12 像素，尺寸为 7.5 像素。 */
    public static final FoundryRenderItem MELTER_INPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 12.0F, 12.0F), 7.5F, 0.0F, 0.0F, ItemDisplayContext.FIXED);

    /** 水平流体炮下半部内部物品的原版匠魂位置参数。 */
    public static final FoundryRenderItem FLUID_CANNON = new FoundryRenderItem(
        new Vector3f(8.0F, 4.0F, 16.0F), 7.5F, 0.0F, 0.0F, ItemDisplayContext.FIXED);

    /** 朝上流体炮内部物品的原版匠魂位置参数。 */
    public static final FoundryRenderItem FLUID_CANNON_UP = new FoundryRenderItem(
        new Vector3f(8.0F, 16.0F, 8.0F), 7.5F, 270.0F, 0.0F, ItemDisplayContext.FIXED);

    /** 朝下流体炮内部物品的原版匠魂位置参数。 */
    public static final FoundryRenderItem FLUID_CANNON_DOWN = new FoundryRenderItem(
        new Vector3f(8.0F, 0.0F, 8.0F), 7.5F, 90.0F, 0.0F, ItemDisplayContext.FIXED);

    /** 浇注台输入物品的原版匠魂中心、缩放和姿态。 */
    public static final FoundryRenderItem CASTING_TABLE_INPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 15.5F, 8.0F), 14.0F, 270.0F, 180.0F, ItemDisplayContext.FIXED);

    /** 浇注台输出物品的原版匠魂中心、缩放和姿态。 */
    public static final FoundryRenderItem CASTING_TABLE_OUTPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 15.5F, 8.0F), 14.1F, 270.0F, 180.0F, ItemDisplayContext.FIXED);

    /** 浇注盆输入物品的原版匠魂中心和缩放。 */
    public static final FoundryRenderItem CASTING_BASIN_INPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 10.0F, 8.0F), 11.95F, 0.0F, 0.0F, ItemDisplayContext.FIXED);

    /** 浇注盆输出物品的原版匠魂中心和缩放。 */
    public static final FoundryRenderItem CASTING_BASIN_OUTPUT = new FoundryRenderItem(
        new Vector3f(8.0F, 10.0F, 8.0F), 12.0F, 0.0F, 0.0F, ItemDisplayContext.FIXED);

    /** 在当前方块实体姿态下绘制一个内部物品，不修改调用者的矩阵栈。 */
    public void render(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
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
        renderInPlace(stack, displayContext, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    /** 按指定缩放和旋转绘制一个设备内部物品，供多方块炉腔复用。 */
    public static void renderAt(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                float scale, float xRotation, float yRotation, ItemDisplayContext displayContext) {
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
        renderInPlace(stack, displayContext, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    /** 在调用方已经完成定位和缩放后写入物品顶点。 */
    private static void renderInPlace(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                                      MultiBufferSource buffer, int packedLight) {
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, displayContext, packedLight,
            OverlayTexture.NO_OVERLAY, poseStack, buffer, Minecraft.getInstance().level, 0);
    }
}
