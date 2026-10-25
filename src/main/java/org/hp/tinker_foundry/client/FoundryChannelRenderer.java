package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.block.FoundryChannelBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

/** 26.1.2 导流槽专用提交式渲染器，复刻 1.21.1 的中心液体和侧向液流。 */
public final class FoundryChannelRenderer {
    /** 提交导流槽中心液体、四个水平连接面和底部液柱。 */
    public static void submit(FoundryBlockEntity entity, PoseStack poseStack,
                              SubmitNodeCollector collector, int packedLight) {
        FluidStack fluid = entity.getDisplayFluid();
        if (fluid.isEmpty() || entity.getLevel() == null) {
            return;
        }
        TextureAtlasSprite still = FoundryFluidClient.stillSprite(fluid);
        if (still == null) {
            return;
        }
        TextureAtlasSprite flowing = FoundryFluidClient.flowingSprite(fluid);
        if (flowing == null) {
            flowing = still;
        }
        int tint = FoundryFluidClient.tint(fluid);
        int brightness = Math.max(packedLight & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
            | packedLight & 0xFFFF0000;
        BlockState state = entity.getBlockState();
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        boolean[] flowingSides = new boolean[sides.length];
        boolean[] sameChannelSides = new boolean[sides.length];
        Direction centerFlow = Direction.UP;
        for (int index = 0; index < sides.length; index++) {
            Direction side = sides[index];
            FoundryChannelBlock.ChannelConnection connection = state.getValue(FoundryChannelBlock.DIRECTION_MAP.get(side));
            flowingSides[index] = connection.canFlow() && entity.isChannelFlowing(side);
            sameChannelSides[index] = entity.getLevel().getBlockState(entity.getBlockPos().relative(side))
                .is(state.getBlock());
            if (connection == FoundryChannelBlock.ChannelConnection.OUT && flowingSides[index]) {
                if (centerFlow == Direction.UP) {
                    centerFlow = side;
                } else if (centerFlow != side) {
                    centerFlow = Direction.DOWN;
                }
            }
        }
        boolean flowingDown = state.getValue(FoundryChannelBlock.DOWN)
            && entity.isChannelFlowing(Direction.DOWN);
        BlockState belowState = entity.getLevel().getBlockState(entity.getBlockPos().below());
        Direction renderedCenterFlow = centerFlow;
        BlockState renderedBelowState = belowState;
        TextureAtlasSprite renderedStill = still;
        TextureAtlasSprite renderedFlowing = flowing;
        collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) -> {
            // 提交回调复制当前姿态，避免导流槽的水平旋转污染其他方块实体。
            PoseStack localPose = new PoseStack();
            localPose.last().set(entry);
            renderChannel(localPose, vertices, state, sides, flowingSides, sameChannelSides, flowingDown,
                renderedCenterFlow, renderedBelowState, renderedStill, renderedFlowing, tint, brightness);
        });
    }

    /** 按 1.21.1 的 Mantle 几何绘制导流槽液体。 */
    private static void renderChannel(PoseStack poseStack, VertexConsumer vertices, BlockState state,
                                      Direction[] sides, boolean[] flowingSides, boolean[] sameChannelSides,
                                      boolean flowingDown, Direction centerFlow, BlockState belowState,
                                      TextureAtlasSprite still, TextureAtlasSprite flowing,
                                      int tint, int brightness) {
        boolean centerRotated = centerFlow.getAxis().isHorizontal() && pushHorizontalRotation(poseStack, centerFlow);
        FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices,
            centerFlow.getAxis().isHorizontal() ? flowing : still,
            0.375F, 0.375F, 0.375F, 0.625F, 0.5F, 0.625F, tint, brightness, 3);
        if (centerRotated) {
            poseStack.popPose();
        }

        for (int index = 0; index < sides.length; index++) {
            Direction side = sides[index];
            FoundryChannelBlock.ChannelConnection connection = state.getValue(FoundryChannelBlock.DIRECTION_MAP.get(side));
            if (!connection.canFlow()) {
                continue;
            }
            renderSide(poseStack, vertices, side, connection, flowingSides[index], sameChannelSides[index],
                tint, brightness, still, flowing);
        }
        if (state.getValue(FoundryChannelBlock.DOWN) && flowingDown) {
            // 底部出口只绘制细液柱，并把液体继续接入下方设备的内部液面。
            FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices, flowing,
                0.375F, 0.0F, 0.375F, 0.625F, 0.375F, 0.625F, tint, brightness, 60);
            FoundryDeviceRenderer.renderFluidIntoBelow(poseStack, vertices, flowing,
                Direction.DOWN, belowState, tint, brightness);
        }
    }

    /** 绘制一个水平连接面及其未连接时的静止边界。 */
    private static void renderSide(PoseStack poseStack, VertexConsumer vertices, Direction side,
                                   FoundryChannelBlock.ChannelConnection connection, boolean isFlowing,
                                   boolean sameChannel, int tint, int brightness,
                                   TextureAtlasSprite still, TextureAtlasSprite flowing) {
        boolean rotated = pushHorizontalRotation(poseStack, side);
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
            // 输入和输出的流动面位于导流槽顶部，输入侧翻转纹理方向。
            renderTintedQuad(vertices, poseStack.last(),
                0.375F, 0.5F, 0.625F, 0.625F, 0.5F, 0.625F,
                0.625F, 0.5F, 1.0F, 0.375F, 0.5F, 1.0F,
                u0, v0, u1, v1, tint, brightness, 0, 1, 0);
            if (!sameChannel) {
                // 末端不是同材质导流槽时补绘端面，避免视角偏移看到断口。
                renderTintedQuad(vertices, poseStack.last(),
                    0.375F, 0.375F, 1.0F, 0.625F, 0.375F, 1.0F,
                    0.625F, 0.5F, 1.0F, 0.375F, 0.5F, 1.0F,
                    u0, v1, u1, v0, tint, brightness, 0, 0, 1);
            }
        } else {
            // 未流动时只保留中心边界的静止液面，不绘制整段粗液柱。
            renderTintedQuad(vertices, poseStack.last(),
                0.375F, 0.375F, 0.625F, 0.625F, 0.375F, 0.625F,
                0.625F, 0.5F, 0.625F, 0.375F, 0.5F, 0.625F,
                still.getU0(), still.getV1(), still.getU1(), still.getV0(), tint, brightness, 0, 0, 1);
        }
        if (rotated) {
            poseStack.popPose();
        }
    }

    /** 绘制双面的带色液体面片，保持自定义流体渲染器的透明面规则。 */
    private static void renderTintedQuad(VertexConsumer vertices, PoseStack.Pose pose,
                                         float ax, float ay, float az, float bx, float by, float bz,
                                         float cx, float cy, float cz, float dx, float dy, float dz,
                                         float u0, float v0, float u1, float v1,
                                         int tint, int brightness, float normalX, float normalY, float normalZ) {
        int red = (tint >> 16) & 255;
        int green = (tint >> 8) & 255;
        int blue = tint & 255;
        int alpha = (tint >>> 24) & 255;
        if (alpha == 0) {
            alpha = 255;
        }
        FoundryFluidRenderer.renderDoubleSidedQuad(vertices, pose,
            ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz,
            u0, v0, u1, v1, red, green, blue, alpha, brightness, normalX, normalY, normalZ);
    }

    /** 将局部南侧几何旋转到实际的水平连接方向。 */
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

    private FoundryChannelRenderer() {
    }
}
