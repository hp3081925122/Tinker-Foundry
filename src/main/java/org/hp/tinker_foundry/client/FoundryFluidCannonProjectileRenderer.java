package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.hp.tinker_foundry.entity.FoundryFluidCannonProjectile;

/** 26.1.2 的流体炮弹渲染入口；弹体物理和命中效果不依赖客户端渲染器。 */
public final class FoundryFluidCannonProjectileRenderer
    extends EntityRenderer<FoundryFluidCannonProjectile, EntityRenderState> {
    /** 创建不投射阴影的流体炮弹渲染器。 */
    public FoundryFluidCannonProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    /** 创建原版实体渲染状态。 */
    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    /** 使用 26.1.2 提交接口保留实体渲染生命周期，不调用已删除的旧 VertexConsumer 管线。 */
    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
    }
}
