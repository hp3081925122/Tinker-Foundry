package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.entity.FoundryFluidCannonProjectile;

/** 流体炮弹体渲染器，使用本地流体面片替代 Mantle 的 FluidEffectProjectileRenderer。 */
public final class FoundryFluidCannonProjectileRenderer
    extends EntityRenderer<FoundryFluidCannonProjectile, FoundryFluidCannonProjectileRenderer.State> {
    /** 流体弹的渲染快照：流体和插值后的朝向。 */
    public static final class State extends EntityRenderState {
        private FluidStack fluid = FluidStack.EMPTY;
        private float yRotation;
        private float xRotation;
    }

    /** 创建不投射阴影的流体弹渲染器。 */
    public FoundryFluidCannonProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    /** 创建流体弹渲染状态。 */
    @Override
    public State createRenderState() {
        return new State();
    }

    /** 提取流体和插值朝向，提交阶段不再访问实体。 */
    @Override
    public void extractRenderState(FoundryFluidCannonProjectile entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.fluid = entity.getFluid().copy();
        state.yRotation = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.xRotation = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
    }

    /** 按流体颜色和纹理提交六段短流体喷射体。 */
    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        FluidStack fluid = state.fluid;
        TextureAtlasSprite sprite = fluid.isEmpty() ? null : FoundryFluidClient.stillSprite(fluid);
        if (sprite != null) {
            int tint = FoundryFluidClient.tint(fluid);
            int brightness = Math.max(state.lightCoords & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
                | state.lightCoords & 0xFFFF0000;
            poseStack.pushPose();
            // 采用匠魂 FluidEffectProjectileRenderer 的旋转顺序，使六段流体弹朝向速度向量。
            poseStack.translate(0.0D, 0.15D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.yRotation - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRotation));
            collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) -> {
                FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                    -0.25F, 0.0F, 0.0F, -0.125F, 0.125F, 0.125F, tint, brightness);
                FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                    0.0F, -0.25F, 0.0F, 0.125F, -0.125F, 0.125F, tint, brightness);
                FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                    0.0F, 0.0F, -0.25F, 0.125F, 0.125F, -0.125F, tint, brightness);
                FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                    0.125F, 0.0F, 0.0F, 0.25F, 0.125F, 0.125F, tint, brightness);
                FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                    0.0F, 0.0F, 0.0F, 0.125F, 0.25F, 0.125F, tint, brightness);
                FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                    0.0F, 0.0F, 0.125F, 0.125F, 0.125F, 0.25F, tint, brightness);
            });
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}
