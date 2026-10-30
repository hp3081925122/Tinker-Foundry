package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 26.1.2 方块实体渲染入口；设备静态外观由方块模型提供，状态提取保留给后续动态提交使用。 */
public final class FoundryBlockEntityRenderer implements BlockEntityRenderer<FoundryBlockEntity, FoundryBlockEntityRenderer.State> {
    /** 保存方块实体的基础渲染快照，避免新提交式渲染接口继续持有旧渲染缓冲。 */
    public static final class State extends BlockEntityRenderState {
        private FoundryBlockEntity entity;
    }

    /** 创建 26.1.2 的方块实体渲染状态。 */
    public FoundryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** 创建提交式渲染所需的设备快照。 */
    @Override
    public State createRenderState() {
        return new State();
    }

    /** 提取基础状态和当前设备引用，保留结构诊断动态渲染所需的数据入口。 */
    @Override
    public void extractRenderState(FoundryBlockEntity entity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);
        state.entity = entity;
    }

    /** 26.1.2 采用提交队列；设备的交互、流体和炉体状态仍由服务端逻辑与 GUI 驱动。 */
    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // 26.1.2 不能复用旧 MultiBufferSource，但可以通过自定义几何提交保留熔融流体液面。
        if (state.entity != null) {
            // 结构错误提示必须在没有打开 GUI 时也能显示，因此先于普通内容提交。
            FoundryStructureRenderer.submitError(state.entity, poseStack, collector, camera, state.lightCoords);
            if (state.entity.isStructureController()) {
                FoundryStructureRenderer.submitContents(state.entity, poseStack, collector, state.lightCoords);
            } else if (state.entity.getBlockState().is(TFBlocks.SEARED_CHANNEL.get())
                || state.entity.getBlockState().is(TFBlocks.SCORCHED_CHANNEL.get())) {
                // 导流槽必须使用独立的细液面和侧向液流几何，不能走普通设备的完整液体方块。
                FoundryChannelRenderer.submit(state.entity, poseStack, collector, state.lightCoords);
            } else {
                FoundryDeviceRenderer.submit(state.entity, poseStack, collector, state.lightCoords);
            }
        }
    }

    /** 控制器的结构范围可能超出单方块模型边界。 */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
