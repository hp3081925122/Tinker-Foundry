package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.FoundryChannelBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 导流槽专用渲染器，对应匠魂的中心液体、侧向连接和底部输出。 */
public final class FoundryChannelRenderer {
    /** 防止同一客户端会话重复输出导流槽几何诊断。 */
    private static boolean diagnosticLogged;

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
        if (!diagnosticLogged) {
            TinkerFoundry.LOGGER.debug("[client-render] channel fluid={} down={} north={} south={} west={} east={}",
                fluid.getFluid(), state.getValue(FoundryChannelBlock.DOWN),
                state.getValue(FoundryChannelBlock.NORTH), state.getValue(FoundryChannelBlock.SOUTH),
                state.getValue(FoundryChannelBlock.WEST), state.getValue(FoundryChannelBlock.EAST));
            diagnosticLogged = true;
        }

        // Mantle 先根据唯一输出方向决定中心液体是否使用流动纹理。
        Direction centerFlow = Direction.UP;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FoundryChannelBlock.ChannelConnection connection = state.getValue(
                FoundryChannelBlock.DIRECTION_MAP.get(direction));
            if (connection == FoundryChannelBlock.ChannelConnection.OUT && entity.isChannelFlowing(direction)) {
                if (centerFlow == Direction.UP) {
                    centerFlow = direction;
                } else if (centerFlow != direction) {
                    centerFlow = Direction.DOWN;
                }
            }
        }
        boolean centerRotated = centerFlow.getAxis().isHorizontal() && pushHorizontalRotation(poseStack, centerFlow);
        FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer,
            centerFlow.getAxis().isHorizontal() ? flowing : still,
            0.375F, 0.375F, 0.375F, 0.625F, 0.5F, 0.625F, tint, brightness, 3);
        if (centerRotated) {
            poseStack.popPose();
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FoundryChannelBlock.ChannelConnection connection = state.getValue(
                FoundryChannelBlock.DIRECTION_MAP.get(direction));
            if (!connection.canFlow()) {
                continue;
            }
            renderSide(entity, poseStack, consumer, still, flowing, direction, connection,
                entity.isChannelFlowing(direction), tint, brightness);
        }
        if (state.getValue(FoundryChannelBlock.DOWN) && entity.isChannelFlowing(Direction.DOWN)) {
            // 底部输出只绘制四个侧面，并把液体继续绘制到相邻浇注设备的内部。
            FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, flowing,
                0.375F, 0.0F, 0.375F, 0.625F, 0.375F, 0.625F, tint, brightness, 60);
            BlockPos belowPos = entity.getBlockPos().below();
            FoundryDeviceRenderer.renderFluidIntoBelow(poseStack, consumer, flowing,
                Direction.DOWN, entity.getLevel().getBlockState(belowPos), tint, brightness);
        }
    }

    /** 绘制一个按 Mantle 数据拆分的水平连接面和非导流边缘。 */
    private static void renderSide(FoundryBlockEntity entity, PoseStack poseStack, VertexConsumer consumer,
                                   TextureAtlasSprite still, TextureAtlasSprite flowing, Direction direction,
                                   FoundryChannelBlock.ChannelConnection connection, boolean isFlowing,
                                   int tint, int brightness) {
        boolean rotated = pushHorizontalRotation(poseStack, direction);
        TextureAtlasSprite sprite = isFlowing ? flowing : still;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        if (connection == FoundryChannelBlock.ChannelConnection.IN && isFlowing) {
            float swapU = u0;
            u0 = u1;
            u1 = swapU;
            float swapV = v0;
            v0 = v1;
            v1 = swapV;
        }
        if (isFlowing) {
            // 输入和输出的流动面都位于导流槽顶部，输入方向翻转纹理方向。
            FoundryFluidRenderer.renderDoubleSidedQuad(consumer, poseStack.last(),
                0.375F, 0.5F, 0.625F, 0.625F, 0.5F, 0.625F,
                0.625F, 0.5F, 1.0F, 0.375F, 0.5F, 1.0F,
                u0, v0, u1, v1, (tint >> 16) & 255, (tint >> 8) & 255, tint & 255,
                tint >>> 24 == 0 ? 255 : tint >>> 24, brightness, 0, 1, 0);
            BlockPos adjacentPos = entity.getBlockPos().relative(direction);
            if (!entity.getLevel().getBlockState(adjacentPos).is(entity.getBlockState().getBlock())) {
                // 相邻方块不是导流槽时补绘末端边缘，避免视角偏移看到断口。
                FoundryFluidRenderer.renderDoubleSidedQuad(consumer, poseStack.last(),
                    0.375F, 0.375F, 1.0F, 0.625F, 0.375F, 1.0F,
                    0.625F, 0.5F, 1.0F, 0.375F, 0.5F, 1.0F,
                    u0, v1, u1, v0, (tint >> 16) & 255, (tint >> 8) & 255, tint & 255,
                    tint >>> 24 == 0 ? 255 : tint >>> 24, brightness, 0, 0, 1);
            }
        } else {
            // 未发生流动时只保留中心边界的静止液面，不绘制一整段伪液柱。
            FoundryFluidRenderer.renderDoubleSidedQuad(consumer, poseStack.last(),
                0.375F, 0.375F, 0.625F, 0.625F, 0.375F, 0.625F,
                0.625F, 0.5F, 0.625F, 0.375F, 0.5F, 0.625F,
                still.getU0(), still.getV1(), still.getU1(), still.getV0(),
                (tint >> 16) & 255, (tint >> 8) & 255, tint & 255,
                tint >>> 24 == 0 ? 255 : tint >>> 24, brightness, 0, 0, 1);
        }
        if (rotated) {
            poseStack.popPose();
        }
    }

    /** 按 Mantle 的水平朝向把局部南侧模型旋转到实际连接方向。 */
    private static boolean pushHorizontalRotation(PoseStack poseStack, Direction direction) {
        if (direction.getAxis().isHorizontal() && direction != Direction.SOUTH) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * direction.get2DDataValue()));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
            return true;
        }
        return false;
    }

    /** 防止把专用导流槽渲染器误当作可实例化状态对象。 */
    private FoundryChannelRenderer() {
    }
}
