package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.block.FoundryChannelBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 导流槽专用渲染器，对应匠魂的中心液体、侧向连接和底部输出。 */
public final class FoundryChannelRenderer {
    /** 绘制客户端同步到的导流槽流体，调用方保证方块确实是导流槽。 */
    public static void render(FoundryBlockEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        FluidStack fluid = entity.getDisplayFluid();
        if (fluid.isEmpty() || entity.getLevel() == null) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite still = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extensions.getStillTexture(fluid));
        TextureAtlasSprite flowing = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extensions.getFlowingTexture(fluid));
        int tint = extensions.getTintColor(fluid);
        int brightness = Math.max(packedLight & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
            | packedLight & 0xFFFF0000;
        VertexConsumer consumer = FoundryFluidRenderer.solidConsumer(buffer);
        var state = entity.getBlockState();
        boolean anyFlowing = entity.isChannelFlowing(Direction.DOWN);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            anyFlowing |= entity.isChannelFlowing(direction);
        }
        TextureAtlasSprite centerSprite = anyFlowing ? flowing : still;

        // 中心液体与导流槽中心模型的四根立柱保持一致。
        FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, centerSprite,
            0.25F, 0.375F, 0.25F, 0.75F, 0.5F, 0.75F, tint, brightness);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FoundryChannelBlock.ChannelConnection connection = state.getValue(
                FoundryChannelBlock.DIRECTION_MAP.get(direction));
            if (!connection.canFlow()) {
                continue;
            }
            // 每个连接面独立切换静止或流动纹理，避免只有状态逻辑而没有视觉反馈。
            TextureAtlasSprite sprite = entity.isChannelFlowing(direction) ? flowing : still;
            switch (direction) {
                case NORTH -> FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
                    0.375F, 0.375F, 0.0F, 0.625F, 0.5F, 0.375F, tint, brightness);
                case SOUTH -> FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
                    0.375F, 0.375F, 0.625F, 0.625F, 0.5F, 1.0F, tint, brightness);
                case WEST -> FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
                    0.0F, 0.375F, 0.375F, 0.375F, 0.5F, 0.625F, tint, brightness);
                case EAST -> FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
                    0.625F, 0.375F, 0.375F, 1.0F, 0.5F, 0.625F, tint, brightness);
                default -> {
                    // 水平连接已在上面的四个方向分支处理，垂直面不在这里重复绘制。
                }
            }
        }
        if (state.getValue(FoundryChannelBlock.DOWN)) {
            // 底部输出使用独立液柱，保持中心孔与下方设备之间的连续性。
            TextureAtlasSprite downSprite = entity.isChannelFlowing(Direction.DOWN) ? flowing : still;
            FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, downSprite,
                0.375F, 0.0F, 0.375F, 0.625F, 0.375F, 0.625F, tint, brightness);
        }
    }

    /** 防止把专用导流槽渲染器误当作可实例化状态对象。 */
    private FoundryChannelRenderer() {
    }
}
