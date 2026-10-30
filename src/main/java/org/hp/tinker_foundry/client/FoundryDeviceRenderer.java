package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.block.FoundryHorizontalBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 26.1.2 方块实体动态内容提交器，负责把熔融流体重新提交到新版渲染队列。 */
public final class FoundryDeviceRenderer {
    /** 将设备内的物品、流体、液面高度和光照快照提交为自定义几何。 */
    public static void submit(FoundryBlockEntity entity, PoseStack poseStack,
                              SubmitNodeCollector collector, int packedLight) {
        BlockState state = entity.getBlockState();
        boolean tank = state.is(TFBlocks.SEARED_INGOT_TANK.get()) || state.is(TFBlocks.SCORCHED_INGOT_TANK.get())
            || state.is(TFBlocks.SEARED_FUEL_TANK.get()) || state.is(TFBlocks.SCORCHED_FUEL_TANK.get())
            || state.is(TFBlocks.SEARED_INGOT_GAUGE.get()) || state.is(TFBlocks.SCORCHED_INGOT_GAUGE.get())
            || state.is(TFBlocks.SEARED_FUEL_GAUGE.get()) || state.is(TFBlocks.SCORCHED_FUEL_GAUGE.get());
        boolean castingTank = state.is(TFBlocks.SEARED_CASTING_TANK.get());
        boolean lantern = state.is(TFBlocks.SEARED_LANTERN.get()) || state.is(TFBlocks.SCORCHED_LANTERN.get());
        boolean proxyTank = state.is(TFBlocks.SCORCHED_PROXY_TANK.get());
        boolean fluidCannon = state.is(TFBlocks.SEARED_FLUID_CANNON.get());
        boolean basin = state.is(TFBlocks.SEARED_BASIN.get()) || state.is(TFBlocks.SCORCHED_BASIN.get());
        boolean table = state.is(TFBlocks.SEARED_TABLE.get()) || state.is(TFBlocks.SCORCHED_TABLE.get());
        boolean melter = state.is(TFBlocks.SEARED_MELTER.get());
        boolean alloyer = state.is(TFBlocks.SCORCHED_ALLOYER.get());
        boolean faucet = isFaucet(state);
        boolean gauge = state.is(TFBlocks.COPPER_GAUGE.get()) || state.is(TFBlocks.OBSIDIAN_GAUGE.get());
        // 只有匠魂对应的设备绘制动态内容，排液口、管道等方块不显示整块液体。
        if (!tank && !castingTank && !lantern && !proxyTank && !fluidCannon && !basin && !table
            && !melter && !alloyer && !faucet && !gauge) {
            return;
        }
        FluidStack fluid = entity.getDisplayFluid();
        int capacity = entity.getDisplayCapacity();
        int itemSeed = (int) entity.getBlockPos().asLong();

        // 浇注盆和浇注台的输入、输出物品仍使用匠魂布局，只把提交接口换成 26.1.2 状态对象。
        if (table) {
            FoundryRenderItem.CASTING_TABLE_INPUT.submit(
                entity.getInput(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
            FoundryRenderItem.CASTING_TABLE_OUTPUT.submit(
                entity.getOutput(), entity.getLevel(), itemSeed + 1, poseStack, collector, packedLight);
        } else if (basin) {
            FoundryRenderItem.CASTING_BASIN_INPUT.submit(
                entity.getInput(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
            FoundryRenderItem.CASTING_BASIN_OUTPUT.submit(
                entity.getOutput(), entity.getLevel(), itemSeed + 1, poseStack, collector, packedLight);
        }
        if (melter && !entity.getInput().isEmpty()) {
            // 熔化炉固体输入随方块朝向旋转，与 1.21.1 的 Mantle RenderItem 姿态一致。
            boolean rotated = pushHorizontalRotation(poseStack, facing(state));
            FoundryRenderItem.MELTER_INPUT.submit(
                entity.getInput(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
            if (rotated) {
                poseStack.popPose();
            }
        }
        if (proxyTank) {
            // 代理储罐中心空间保留给可交互容器，流体只占四个角落。
            FoundryRenderItem.PROXY_TANK.submit(
                entity.getSpecialItem(), entity.getLevel(), itemSeed, poseStack, collector, packedLight);
            submitProxyTankFluid(fluid, capacity, poseStack, collector, packedLight);
            return;
        }
        if (fluidCannon) {
            submitFluidCannonItem(entity, itemSeed, poseStack, collector, packedLight);
        }
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
            TextureAtlasSprite renderedFlowing = flowingSprite;
            collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) -> {
                // 提交回调重新复制姿态，确保水平浇注口的旋转只影响本次流体几何。
                PoseStack localPose = new PoseStack();
                localPose.last().set(entry);
                drawFaucetFluid(localPose, vertices, stillSprite, renderedFlowing,
                    facing, belowState, outputDistance, tint, brightness);
            });
            return;
        }

        float ratio = Math.min(1.0F, Math.max(0.0F, fluid.getAmount() / (float) capacity));
        if (gauge) {
            // 流体计只绘制贴合仪表盘的薄面，避免通用液柱穿过边框。
            Direction direction = state.hasProperty(FoundryDirectionalBlock.FACING)
                ? state.getValue(FoundryDirectionalBlock.FACING) : Direction.DOWN;
            collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) ->
                drawGaugeFluid(entry, vertices, stillSprite, direction, ratio, tint, brightness));
            return;
        }
        Bounds bounds = bounds(state, lantern, castingTank, fluidCannon);
        float maxY = bounds.minY + bounds.height * ratio;
        if (maxY <= bounds.minY) {
            return;
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) -> {
            // 自定义几何回调只持有不可变的提交姿态，复制后再绘制液面。
            PoseStack localPose = new PoseStack();
            localPose.last().set(entry);
            FoundryFluidRenderer.renderCuboid(localPose.last(), vertices, stillSprite,
                bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, maxY, bounds.maxZ,
                tint, brightness, bounds.faces);
        });
    }

    /** 水平朝向且不是南向时绕方块中心旋转，返回是否压入了新的姿态。 */
    private static boolean pushHorizontalRotation(PoseStack poseStack, Direction facing) {
        if (!facing.getAxis().isHorizontal() || facing == Direction.SOUTH) {
            return false;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * facing.get2DDataValue()));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        return true;
    }

    /** 按流体炮六向状态提交其下半部内部物品，上下炮口使用匠魂原版的独立姿态。 */
    private static void submitFluidCannonItem(FoundryBlockEntity entity, int seed, PoseStack poseStack,
                                              SubmitNodeCollector collector, int packedLight) {
        Direction facing = facing(entity.getBlockState());
        if (facing == Direction.UP) {
            FoundryRenderItem.FLUID_CANNON_UP.submit(
                entity.getSpecialItem(), entity.getLevel(), seed, poseStack, collector, packedLight);
            return;
        }
        if (facing == Direction.DOWN) {
            FoundryRenderItem.FLUID_CANNON_DOWN.submit(
                entity.getSpecialItem(), entity.getLevel(), seed, poseStack, collector, packedLight);
            return;
        }
        boolean rotated = pushHorizontalRotation(poseStack, facing);
        FoundryRenderItem.FLUID_CANNON.submit(
            entity.getSpecialItem(), entity.getLevel(), seed, poseStack, collector, packedLight);
        if (rotated) {
            poseStack.popPose();
        }
    }

    /** 提交匠魂代理储罐的四角流体体积。 */
    private static void submitProxyTankFluid(FluidStack fluid, int capacity, PoseStack poseStack,
                                             SubmitNodeCollector collector, int packedLight) {
        if (fluid.isEmpty() || fluid.getAmount() <= 0 || capacity <= 0) {
            return;
        }
        TextureAtlasSprite sprite = FoundryFluidClient.stillSprite(fluid);
        if (sprite == null) {
            return;
        }
        int tint = FoundryFluidClient.tint(fluid);
        int brightness = Math.max(packedLight & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
            | packedLight & 0xFFFF0000;
        float minY = 4.0F / 16.0F;
        float maxY = minY + (15.92F / 16.0F - minY)
            * Math.min(1.0F, Math.max(0.0F, fluid.getAmount() / (float) capacity));
        collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) -> {
            // 四个液柱的边界保持与匠魂 Mantle 生成数据一致，中心位置留给容器物品。
            FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                0.08F / 16.0F, minY, 0.08F / 16.0F, 4.92F / 16.0F, maxY, 4.92F / 16.0F, tint, brightness);
            FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                11.08F / 16.0F, minY, 0.08F / 16.0F, 15.92F / 16.0F, maxY, 4.92F / 16.0F, tint, brightness);
            FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                0.08F / 16.0F, minY, 11.08F / 16.0F, 4.92F / 16.0F, maxY, 15.92F / 16.0F, tint, brightness);
            FoundryFluidRenderer.renderCuboid(entry, vertices, sprite,
                11.08F / 16.0F, minY, 11.08F / 16.0F, 15.92F / 16.0F, maxY, 15.92F / 16.0F, tint, brightness);
        });
    }

    /** 按设备模型取内部可见液面区域，数值与 1.21.1 的匠魂布局一致。 */
    private static Bounds bounds(BlockState state, boolean lantern, boolean castingTank, boolean fluidCannon) {
        if (lantern) {
            float minY = state.getValue(LanternBlock.HANGING) ? 0.125F : 0.0625F;
            return new Bounds(0.315625F, minY, 0.315625F, 0.684375F, minY + 0.3125F, 0.684375F, 63);
        }
        if (castingTank) {
            return new Bounds(0.08F, 0.08F, 0.08F, 0.995F, 0.08F + 0.6025F, 0.995F, 63);
        }
        if (fluidCannon) {
            // 流体炮只在上半部绘制动态液面，边界与半储罐模型的 Mantle 等价范围一致。
            return new Bounds(0.08F / 16.0F, 8.08F / 16.0F, 0.08F / 16.0F,
                15.92F / 16.0F, 8.08F / 16.0F + 7.84F / 16.0F, 15.92F / 16.0F, 63);
        }
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

    /** 绘制贴合仪表盘朝向的双面薄液面。 */
    private static void drawGaugeFluid(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                       Direction facing, float ratio, int tint, int packedLight) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float faceMin = 5.0F / 16.0F;
        float faceMax = 11.0F / 16.0F;
        float fillMin = 6.0F / 16.0F;
        float fillMax = 11.0F / 16.0F;
        float high = fillMin + (fillMax - fillMin) * ratio;
        float upperFillMin = 5.0F / 16.0F;
        float upperFillMax = 10.0F / 16.0F;
        float upperHigh = upperFillMin + (upperFillMax - upperFillMin) * ratio;
        float northPlane = 15.5F / 16.0F;
        float southPlane = 0.5F / 16.0F;
        float westPlane = 15.5F / 16.0F;
        float eastPlane = 0.5F / 16.0F;
        float downPlane = 15.5F / 16.0F;
        float upPlane = 0.5F / 16.0F;
        int red = (tint >> 16) & 255;
        int green = (tint >> 8) & 255;
        int blue = tint & 255;
        int alpha = (tint >>> 24) & 255;
        if (alpha == 0) {
            alpha = 255;
        }

        // 六向仪表盘都补绘反向面，避免视角偏移导致液面空白。
        switch (facing) {
            case NORTH -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                faceMax, fillMin, northPlane, faceMin, fillMin, northPlane, faceMin, high, northPlane,
                faceMax, high, northPlane, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 0, -1);
            case SOUTH -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                faceMin, fillMin, southPlane, faceMax, fillMin, southPlane, faceMax, high, southPlane,
                faceMin, high, southPlane, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 0, 1);
            case WEST -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                westPlane, fillMin, faceMin, westPlane, fillMin, faceMax, westPlane, high, faceMax,
                westPlane, high, faceMin, u0, v1, u1, v0, red, green, blue, alpha, packedLight, -1, 0, 0);
            case EAST -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                eastPlane, fillMin, faceMax, eastPlane, fillMin, faceMin, eastPlane, high, faceMin,
                eastPlane, high, faceMax, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 1, 0, 0);
            case DOWN -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                faceMin, downPlane, fillMin, faceMax, downPlane, fillMin, faceMax, downPlane, high,
                faceMin, downPlane, high, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, -1, 0);
            case UP -> FoundryFluidRenderer.renderDoubleSidedQuad(consumer, pose,
                faceMin, upPlane, upperHigh, faceMax, upPlane, upperHigh, faceMax, upPlane, upperFillMin,
                faceMin, upPlane, upperFillMin, u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 1, 0);
        }
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
