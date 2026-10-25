package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.block.FoundryHorizontalBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 26.1.2 方块实体动态内容提交器，负责把熔融流体重新提交到新版渲染队列。 */
public final class FoundryDeviceRenderer {
    /** 将设备内的流体、液面高度和光照快照提交为自定义几何。 */
    public static void submit(FoundryBlockEntity entity, PoseStack poseStack,
                              SubmitNodeCollector collector, int packedLight) {
        BlockState state = entity.getBlockState();
        FluidStack fluid = entity.getDisplayFluid();
        int capacity = entity.getDisplayCapacity();
        int itemSeed = (int) entity.getBlockPos().asLong();

        // 浇注盆和浇注台的输入、输出物品仍使用匠魂布局，只把提交接口换成 26.1.2 状态对象。
        if (state.is(TFBlocks.SEARED_TABLE.get()) || state.is(TFBlocks.SCORCHED_TABLE.get())) {
            FoundryRenderItem.CASTING_TABLE_INPUT.submit(
                entity.getInput(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
            FoundryRenderItem.CASTING_TABLE_OUTPUT.submit(
                entity.getOutput(), entity.getLevel(), itemSeed + 1, poseStack, collector, packedLight);
        } else if (state.is(TFBlocks.SEARED_BASIN.get()) || state.is(TFBlocks.SCORCHED_BASIN.get())) {
            FoundryRenderItem.CASTING_BASIN_INPUT.submit(
                entity.getInput(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
            FoundryRenderItem.CASTING_BASIN_OUTPUT.submit(
                entity.getOutput(), entity.getLevel(), itemSeed + 1, poseStack, collector, packedLight);
        } else if (state.is(TFBlocks.SEARED_MELTER.get())) {
            FoundryRenderItem.MELTER_INPUT.submit(
                entity.getInput(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
        } else if (state.is(TFBlocks.SCORCHED_PROXY_TANK.get())) {
            FoundryRenderItem.PROXY_TANK.submit(
                entity.getSpecialItem(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
        } else if (state.is(TFBlocks.SEARED_FLUID_CANNON.get())) {
            FoundryRenderItem.FLUID_CANNON.submit(
                entity.getSpecialItem(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
        }
        boolean faucet = isFaucet(state);
        if (fluid.isEmpty() || fluid.getAmount() <= 0 || (!faucet && capacity <= 0)) {
            return;
        }
        if (faucet && (!entity.isFaucetPouring() || !entity.hasFaucetOutputTarget())) {
            return;
        }

        TextureAtlasSprite stillSprite = FoundryFluidClient.stillSprite(fluid);
        if (stillSprite == null) {
            return;
        }
        int tint = FoundryFluidClient.tint(fluid);
        int brightness = Math.max(packedLight & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
            | packedLight & 0xFFFF0000;
        if (faucet) {
            // 浇注口使用匠魂的细管、出口和下方连续液柱，而不是普通储罐的整块液面。
            Direction facing = facing(state);
            BlockPos outputPos = entity.faucetOutputPos();
            BlockState belowState = entity.getLevel() == null || outputPos == null ? null
                : entity.getLevel().getBlockState(outputPos);
            int outputDistance = outputPos == null ? 1 : entity.getBlockPos().getY() - outputPos.getY();
            TextureAtlasSprite flowingSprite = FoundryFluidClient.flowingSprite(fluid);
            if (flowingSprite == null) {
                flowingSprite = stillSprite;
            }
            TextureAtlasSprite renderedStill = stillSprite;
            TextureAtlasSprite renderedFlowing = flowingSprite;
            collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) -> {
                // 提交回调重新复制姿态，确保水平浇注口的旋转只影响本次流体几何。
                PoseStack localPose = new PoseStack();
                localPose.last().set(entry);
                drawFaucetFluid(localPose, vertices, renderedStill, renderedFlowing,
                    facing, belowState, outputDistance, tint, brightness);
            });
            return;
        }

        Bounds bounds = bounds(state);
        float ratio = Math.min(1.0F, Math.max(0.0F, fluid.getAmount() / (float) capacity));
        float maxY = bounds.minY + bounds.height * ratio;
        collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) -> {
            // 自定义几何回调只持有不可变的提交姿态，复制后再按设备朝向追加局部变换。
            PoseStack localPose = new PoseStack();
            localPose.last().set(entry);
            FoundryFluidRenderer.renderCuboid(localPose.last(), vertices, stillSprite,
                bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, maxY, bounds.maxZ,
                tint, brightness, bounds.faces);
        });
    }

    /** 按设备模型估算内部可见液面区域，保持 1.21.1 的匠魂布局比例。 */
    private static Bounds bounds(BlockState state) {
        if (state.is(TFBlocks.SEARED_BASIN.get()) || state.is(TFBlocks.SCORCHED_BASIN.get())) {
            return new Bounds(0.13125F, 0.25F, 0.13125F, 0.86875F, 0.99375F, 0.86875F, 61);
        }
        if (state.is(TFBlocks.SEARED_TABLE.get()) || state.is(TFBlocks.SCORCHED_TABLE.get())) {
            return new Bounds(0.0625F, 0.9375F, 0.0625F, 0.9375F, 0.99375F, 0.9375F, 1);
        }
        if (state.is(TFBlocks.SEARED_MELTER.get())) {
            return new Bounds(0.08F, 0.505F, 0.08F, 0.995F, 0.995F, 0.995F, 63);
        }
        if (state.is(TFBlocks.SCORCHED_ALLOYER.get())) {
            return new Bounds(0.08F, 0.3175F, 0.08F, 0.995F, 0.995F, 0.995F, 63);
        }
        if (state.is(TFBlocks.COPPER_GAUGE.get()) || state.is(TFBlocks.OBSIDIAN_GAUGE.get())) {
            return new Bounds(0.3125F, 0.375F, 0.3125F, 0.6875F, 0.6875F, 0.6875F, 1);
        }
        return new Bounds(0.08F, 0.08F, 0.08F, 0.995F, 0.995F, 0.995F, 63);
    }

    /** 判断方块是否为浇注口。 */
    private static boolean isFaucet(BlockState state) {
        return state.is(TFBlocks.SEARED_FAUCET.get()) || state.is(TFBlocks.SCORCHED_FAUCET.get());
    }

    /** 读取浇注口的六向朝向，兼容当前项目的通用方向方块和水平方向方块。 */
    private static Direction facing(BlockState state) {
        if (state.hasProperty(FoundryDirectionalBlock.FACING)) {
            return state.getValue(FoundryDirectionalBlock.FACING);
        }
        if (state.hasProperty(FoundryHorizontalBlock.FACING)) {
            return state.getValue(FoundryHorizontalBlock.FACING);
        }
        return Direction.DOWN;
    }

    /** 按 1.21.1 匠魂浇注口模型绘制内部细管、出液口和下方液柱。 */
    private static void drawFaucetFluid(PoseStack poseStack, VertexConsumer vertices,
                                        TextureAtlasSprite stillSprite, TextureAtlasSprite flowingSprite,
                                        Direction facing, BlockState belowState, int outputDistance,
                                        int tint, int packedLight) {
        boolean rotated = facing.getAxis().isHorizontal() && facing != Direction.SOUTH;
        if (rotated) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * facing.get2DDataValue()));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
        if (facing.getAxis().isHorizontal()) {
            // 横向浇注口由后段储液腔和前段出液腔组成，横截面保持四像素宽。
            FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices, stillSprite,
                0.375F, 0.375F, 0.0F, 0.625F, 0.5625F, 0.375F, tint, packedLight, 17);
            FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices, flowingSprite,
                0.375F, 0.375F, 0.0F, 0.625F, 0.5625F, 0.375F, tint, packedLight, 1);
            FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices, flowingSprite,
                0.375F, 0.0F, 0.375F, 0.625F, 0.5625F, 0.5F, tint, packedLight, 61);
        } else {
            // 竖向浇注口使用完整细管，避免旧版通用液面把管腔扩成方块。
            FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices, flowingSprite,
                0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F, tint, packedLight, 60);
            FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices, stillSprite,
                0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F, tint, packedLight, 1);
        }
        // 液柱继续进入下方浇注盆、浇注台或导流槽，避免两个方块之间出现断层。
        renderFluidIntoBelow(poseStack, vertices, flowingSprite, facing, belowState, outputDistance, tint, packedLight);
        if (rotated) {
            poseStack.popPose();
        }
    }

    /** 按匠魂的下方目标类型绘制连续液柱。 */
    static void renderFluidIntoBelow(PoseStack poseStack, VertexConsumer vertices,
                                     TextureAtlasSprite flowingSprite, Direction facing,
                                     BlockState belowState, int tint, int packedLight) {
        renderFluidIntoBelow(poseStack, vertices, flowingSprite, facing, belowState, 1, tint, packedLight);
    }

    /** 绘制中间空气间隙中的细液柱，再绘制最终目标内部的连续液面。 */
    static void renderFluidIntoBelow(PoseStack poseStack, VertexConsumer vertices,
                                     TextureAtlasSprite flowingSprite, Direction facing,
                                     BlockState belowState, int outputDistance, int tint, int packedLight) {
        if (belowState == null) {
            return;
        }
        boolean basin = belowState.is(TFBlocks.SEARED_BASIN.get()) || belowState.is(TFBlocks.SCORCHED_BASIN.get());
        boolean table = belowState.is(TFBlocks.SEARED_TABLE.get()) || belowState.is(TFBlocks.SCORCHED_TABLE.get());
        boolean channel = belowState.is(TFBlocks.SEARED_CHANNEL.get()) || belowState.is(TFBlocks.SCORCHED_CHANNEL.get());
        if (!basin && !table && !channel) {
            return;
        }
        float minY = channel ? 0.5F : basin ? 0.25F : 0.9375F;
        float maxZ = facing.getAxis().isVertical() ? 0.625F : 0.5F;
        int faces = basin || table ? 60 : 62;
        int distance = Math.max(1, outputDistance);
        for (int gap = 1; gap < distance; gap++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, -gap, 0.0F);
            FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices, flowingSprite,
                0.375F, 0.0F, 0.375F, 0.625F, 1.0F, maxZ, tint, packedLight, 60);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, -distance, 0.0F);
        FoundryFluidRenderer.renderCuboid(poseStack.last(), vertices, flowingSprite,
            0.375F, minY, 0.375F, 0.625F, 1.0F, maxZ, tint, packedLight, faces);
        poseStack.popPose();
    }

    /** 设备内部流体几何边界。 */
    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                          float height, int faces) {
        /** 使用指定下界和上界创建边界，并由渲染器计算实际液面高度。 */
        private Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int faces) {
            this(minX, minY, minZ, maxX, maxY, maxZ, maxY - minY, faces);
        }
    }

    private FoundryDeviceRenderer() {
    }
}
