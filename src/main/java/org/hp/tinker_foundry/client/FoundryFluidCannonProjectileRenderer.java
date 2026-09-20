package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.entity.FoundryFluidCannonProjectile;

/** 流体炮弹体渲染器，使用本地流体面片替代 Mantle 的 FluidEffectProjectileRenderer。 */
public final class FoundryFluidCannonProjectileRenderer extends EntityRenderer<FoundryFluidCannonProjectile> {
    /** 创建不投射阴影的流体弹渲染器。 */
    public FoundryFluidCannonProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    /** 按流体颜色和纹理绘制六段短流体喷射体。 */
    @Override
    public void render(FoundryFluidCannonProjectile entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        FluidStack fluid = entity.getFluid();
        if (!fluid.isEmpty()) {
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extensions.getStillTexture(fluid));
            int tint = extensions.getTintColor(fluid);
            int brightness = Math.max(packedLight & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
                | packedLight & 0xFFFF0000;
            float yRotation = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            float xRotation = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
            poseStack.pushPose();
            // 采用匠魂 FluidEffectProjectileRenderer 的旋转顺序，使六段流体弹朝向速度向量。
            poseStack.translate(0.0D, 0.15D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(yRotation - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(xRotation));
            VertexConsumer consumer = FoundryFluidRenderer.solidConsumer(buffer);
            drawSegment(poseStack, consumer, sprite, -0.25F, 0.0F, 0.0F, -0.125F, 0.125F, 0.125F,
                tint, brightness);
            drawSegment(poseStack, consumer, sprite, 0.0F, -0.25F, 0.0F, 0.125F, -0.125F, 0.125F,
                tint, brightness);
            drawSegment(poseStack, consumer, sprite, 0.0F, 0.0F, -0.25F, 0.125F, 0.125F, -0.125F,
                tint, brightness);
            drawSegment(poseStack, consumer, sprite, 0.125F, 0.0F, 0.0F, 0.25F, 0.125F, 0.125F,
                tint, brightness);
            drawSegment(poseStack, consumer, sprite, 0.0F, 0.0F, 0.0F, 0.125F, 0.25F, 0.125F,
                tint, brightness);
            drawSegment(poseStack, consumer, sprite, 0.0F, 0.0F, 0.125F, 0.125F, 0.125F, 0.25F,
                tint, brightness);
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /** 绘制一段带双面保护的流体立方体。 */
    private static void drawSegment(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                    float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                    int tint, int packedLight) {
        FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
            minX, minY, minZ, maxX, maxY, maxZ, tint, packedLight);
    }

    /** 仅用于满足实体渲染器的纹理接口，实际纹理来自流体类型。 */
    @Override
    public net.minecraft.resources.ResourceLocation getTextureLocation(FoundryFluidCannonProjectile entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
