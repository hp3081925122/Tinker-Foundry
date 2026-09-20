package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 方块实体渲染总调度器，只负责按设备类别分派到专用等价渲染器。 */
public final class FoundryBlockEntityRenderer implements BlockEntityRenderer<FoundryBlockEntity> {
    /** 创建共用注册器，实际绘制逻辑位于各设备专用渲染器。 */
    public FoundryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** 先绘制结构诊断，再按控制器、导流槽和单方块设备分派动态内容。 */
    @Override
    public void render(FoundryBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // 结构错误提示必须在没有打开 GUI 时也能显示，因此先于普通内容渲染。
        FoundryStructureRenderer.renderError(entity, poseStack, buffer, packedLight);
        if (entity.isStructureController()) {
            FoundryStructureRenderer.renderContents(entity, poseStack, buffer, packedLight);
            return;
        }

        BlockState state = entity.getBlockState();
        if (state.is(TFBlocks.SEARED_CHANNEL.get()) || state.is(TFBlocks.SCORCHED_CHANNEL.get())) {
            FoundryChannelRenderer.render(entity, poseStack, buffer, packedLight);
            return;
        }
        FoundryDeviceRenderer.render(entity, poseStack, buffer, packedLight);
    }

    /** 结构控制器的炉腔和诊断框可能超出自身方块范围，关闭方块实体裁剪。 */
    @Override
    public boolean shouldRenderOffScreen(FoundryBlockEntity entity) {
        return entity.isStructureController() && (entity.isStructureValid() || entity.structureErrorPos() != null);
    }
}
