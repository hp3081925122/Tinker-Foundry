package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import org.hp.tinker_foundry.block.FoundryDirectionalBlock;
import org.hp.tinker_foundry.registry.TFBlocks;

/** 为可见储液设备绘制带真实流体纹理和高度比例的动态流体体积。 */
public final class FoundryBlockEntityRenderer implements BlockEntityRenderer<FoundryBlockEntity> {
    /** 防止每帧重复输出流体计诊断日志。 */
    private static boolean gaugeDiagnosticLogged;
    /** 防止重复输出储液罐液面和边界诊断日志。 */
    private static boolean tankDiagnosticLogged;

    /** 创建独立渲染器，当前不需要额外模型烘焙数据。 */
    public FoundryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** 根据客户端同步到的流体数量绘制储液罐和浇注盆内部液面。 */
    @Override
    public void render(FoundryBlockEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // 结构错误提示必须在没有打开 GUI 时也能显示，因此先于普通流体内容渲染。
        renderStructureError(entity, poseStack, buffer, packedLight);
        if (entity.isStructureController()) {
            renderStructureContents(entity, poseStack, buffer, packedLight);
            return;
        }
        BlockState state = entity.getBlockState();
        boolean tank = state.is(TFBlocks.SEARED_TANK.get()) || state.is(TFBlocks.SCORCHED_TANK.get())
            || state.is(TFBlocks.SEARED_FUEL_TANK.get()) || state.is(TFBlocks.SCORCHED_FUEL_TANK.get());
        boolean castingTank = state.is(TFBlocks.SEARED_CASTING_TANK.get()) || state.is(TFBlocks.SCORCHED_CASTING_TANK.get());
        boolean lanternTank = state.is(TFBlocks.SEARED_LANTERN.get()) || state.is(TFBlocks.SCORCHED_LANTERN.get());
        tank = tank || castingTank || lanternTank;
        boolean lantern = state.is(TFBlocks.SEARED_LANTERN.get()) || state.is(TFBlocks.SCORCHED_LANTERN.get());
        boolean basin = state.is(TFBlocks.CASTING_BASIN.get());
        boolean table = state.is(TFBlocks.CASTING_TABLE.get());
        boolean machineTank = state.is(TFBlocks.MELTER.get()) || state.is(TFBlocks.ALLOYER.get());
        boolean faucet = state.is(TFBlocks.FAUCET.get());
        boolean gauge = state.is(TFBlocks.FLUID_GAUGE.get());
        boolean fluidDevice = faucet || gauge;
        if (!tank && !basin && !table && !machineTank && !fluidDevice) {
            return;
        }
        if (table && !entity.getOutput().isEmpty()) {
            // 浇注台上的成品使用固定视角渲染，保证世界内结果与创造栏物品模型一致。
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.18F, 0.5F);
            poseStack.scale(0.55F, 0.55F, 0.55F);
            Minecraft.getInstance().getItemRenderer().renderStatic(entity.getOutput(), ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.getLevel(), 0);
            poseStack.popPose();
        }
        FluidStack fluid = entity.getDisplayFluid();
        int capacity = entity.getDisplayCapacity();
        if (fluid.isEmpty() || fluid.getAmount() <= 0 || capacity <= 0) {
            return;
        }

        // 计算设备内部可见区域，液面高度严格受服务端容量限制。
        float ratio = Math.min(1.0F, Math.max(0.0F, fluid.getAmount() / (float) capacity));
        float minX;
        float maxX;
        float minZ;
        float maxZ;
        float minY;
        float usableHeight;
        if (lantern) {
            minX = 0.315625F;
            maxX = 0.684375F;
            minZ = minX;
            maxZ = maxX;
            minY = state.getValue(LanternBlock.HANGING) ? 0.125F : 0.0625F;
            usableHeight = 0.3125F;
        } else if (castingTank) {
            minX = 0.08F;
            maxX = 0.995F;
            minZ = minX;
            maxZ = maxX;
            minY = 0.08F;
            usableHeight = 0.6025F;
        } else if (tank) {
            minX = 0.08F;
            maxX = 0.995F;
            minZ = minX;
            maxZ = maxX;
            minY = 0.08F;
            usableHeight = 0.915F;
        } else if (machineTank) {
            minX = 0.08F;
            maxX = 0.995F;
            minZ = minX;
            maxZ = maxX;
            minY = state.is(TFBlocks.ALLOYER.get()) ? 0.3175F : 0.505F;
            usableHeight = state.is(TFBlocks.ALLOYER.get()) ? 0.6775F : 0.49F;
        } else if (basin) {
            minX = 0.13125F;
            maxX = 0.86875F;
            minZ = minX;
            maxZ = maxX;
            minY = 0.25F;
            usableHeight = 0.74375F;
        } else {
            minX = 0.0625F;
            maxX = 0.9375F;
            minZ = minX;
            maxZ = maxX;
            minY = 0.9375F;
            usableHeight = 0.05625F;
        }
        float maxY = minY + usableHeight * ratio;
        // 使用流体类型提供的静止纹理和色调，保持客户端资源包的动画兼容性。
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
        int tint = extensions.getTintColor(fluid);
        if (fluidDevice) {
            Direction direction = state.hasProperty(FoundryDirectionalBlock.FACING)
                ? state.getValue(FoundryDirectionalBlock.FACING) : Direction.DOWN;
            if (gauge) {
                // 流体计使用贴合仪表盘的薄液面，避免通用立方体产生错误的透明遮挡面。
                if (!gaugeDiagnosticLogged) {
                    TinkerFoundry.LOGGER.debug("[client-render] fluid gauge fluid={} amount={} capacity={} facing={}",
                        fluid.getFluid(), fluid.getAmount(), capacity, direction);
                    gaugeDiagnosticLogged = true;
                }
                drawGaugeFluid(poseStack.last(), consumerFor(buffer), sprite, direction, ratio, tint, packedLight);
                return;
            }
            // 浇注口只在自己的真实管腔内显示流体，排液口、管道和导流槽不再伪造内部液体。
            drawFaucetFluid(poseStack, consumerFor(buffer), sprite, direction, tint, packedLight);
            return;
        }
        if (maxY <= minY) {
            return;
        }

        // 熔融流体纹理是完全不透明的，使用方块渲染层写入深度，避免地下方块被错误透视。
        VertexConsumer consumer = consumerFor(buffer);
        if (tank && !tankDiagnosticLogged) {
            TinkerFoundry.LOGGER.debug("[client-render] tank fluid={} amount={} capacity={} bounds={}..{} height={}",
                fluid.getFluid(), fluid.getAmount(), capacity, minY, maxY, usableHeight);
            tankDiagnosticLogged = true;
        }
        drawCuboid(poseStack.last(), consumer, sprite, minX, minY, minZ, maxX, maxY, maxZ, tint, packedLight);

    }

    /** 按炉腔比例绘制有序多层液体，物品编号映射到实际内部方块。 */
    private static void renderStructureContents(FoundryBlockEntity entity, PoseStack poseStack,
                                                 MultiBufferSource buffer, int light) {
        AABB bounds = entity.interiorBounds();
        if (bounds == null || entity.getLevel() == null) return;
        BlockPos origin = entity.getBlockPos();
        int width = (int) (bounds.maxX - bounds.minX), depth = (int) (bounds.maxZ - bounds.minZ);
        float height = (float) (bounds.maxY - bounds.minY);
        if (width <= 0 || depth <= 0 || height <= 0) return;
        poseStack.pushPose();
        poseStack.translate(bounds.minX - origin.getX(), bounds.minY - origin.getY(), bounds.minZ - origin.getZ());
        java.util.List<FluidStack> fluids = entity.structureFluidLayers();
        int amount = fluids.stream().mapToInt(FluidStack::getAmount).sum();
        int scale = Math.max(1, Math.max(entity.structureCapacity(), amount));
        float[] heights = new float[fluids.size()];
        float total = 0;
        for (int index = 0; index < heights.length; index++) {
            heights[index] = Math.max(0.1F, fluids.get(index).getAmount() * (height - 0.01F) / scale);
            total += heights[index];
        }
        // 极小流体也有可见厚度；层数过多时压缩厚度，绝不超出炉腔。
        float compression = total > height - 0.01F ? (height - 0.01F) / total : 1;
        float y = 0.005F;
        for (int index = 0; index < fluids.size(); index++) {
            FluidStack fluid = fluids.get(index);
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluid));
            float next = y + heights[index] * compression;
            int brightness = Math.max(light & 65535, fluid.getFluidType().getLightLevel(fluid) << 4) | light & 0xFFFF0000;
            // 按方块切分纹理，只绘制外表面，不在炉腔内部生成重复面片。
            for (int x = 0; x < width; x++) for (int z = 0; z < depth; z++) {
                for (float bottom = y; bottom < next; ) {
                    float top = Math.min(next, (float) Math.floor(bottom) + 1);
                    int faces = (top == next ? 1 : 0) | (bottom == y ? 2 : 0)
                        | (x == 0 ? 4 : 0) | (x == width - 1 ? 8 : 0)
                        | (z == 0 ? 16 : 0) | (z == depth - 1 ? 32 : 0);
                    if (faces != 0) drawCuboid(poseStack.last(), buffer.getBuffer(RenderType.translucent()), sprite,
                        x == 0 ? 0.005F : x, bottom, z == 0 ? 0.005F : z,
                        x == width - 1 ? width - 0.005F : x + 1, top,
                        z == depth - 1 ? depth - 0.005F : z + 1, extensions.getTintColor(fluid), brightness, faces);
                    bottom = top;
                }
            }
            y = next;
        }
        // 与上游相同按面片数控制物品显示预算，不影响真实熔炼库存。
        int quads = 0;
        var renderer = Minecraft.getInstance().getItemRenderer();
        for (int slot = 0; slot < entity.inputSlotCount() && quads <= 3500; slot++) {
            var item = entity.getInput(slot);
            if (item.isEmpty()) continue;
            int itemY = slot / (width * depth), itemX = slot % width, itemZ = slot / width % depth;
            poseStack.pushPose();
            poseStack.translate(itemX + 0.5F, itemY + 0.5F, itemZ + 0.5F);
            if (entity.getBlockState().hasProperty(org.hp.tinker_foundry.block.FoundryControllerBlock.FACING)) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90F * entity.getBlockState()
                    .getValue(org.hp.tinker_foundry.block.FoundryControllerBlock.FACING).get2DDataValue()));
            }
            poseStack.scale(0.9375F, 0.9375F, 0.9375F);
            renderer.renderStatic(item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, entity.getLevel(), 0);
            poseStack.popPose();
            var model = renderer.getModel(item, entity.getLevel(), null, 0);
            if (model.isCustomRenderer()) quads += 100;
            else {
                for (Direction face : Direction.values()) quads += model.getQuads(null, face,
                    entity.getLevel().getRandom(), net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null).size();
                quads += model.getQuads(null, null, entity.getLevel().getRandom(),
                    net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null).size();
            }
        }
        poseStack.popPose();
    }

    /** 绘制匠魂风格的错误方块红框和结构原因文字。 */
    private static void renderStructureError(FoundryBlockEntity entity, PoseStack poseStack,
                                             MultiBufferSource buffer, int packedLight) {
        if (!entity.shouldShowStructureError() || Minecraft.getInstance().player == null) {
            return;
        }
        BlockPos controllerPos = entity.getBlockPos();
        BlockPos errorPos = entity.structureErrorPos();
        int dx = Minecraft.getInstance().player.blockPosition().getX() - controllerPos.getX();
        int dz = Minecraft.getInstance().player.blockPosition().getZ() - controllerPos.getZ();
        if (dx * dx + dz * dz >= 512) {
            return;
        }

        BlockPos relative = errorPos.subtract(controllerPos);
        AABB box = new AABB(relative.getX(), relative.getY(), relative.getZ(),
            relative.getX() + 1.0D, relative.getY() + 1.0D, relative.getZ() + 1.0D);
        LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), box,
            1.0F, 0.0F, 0.0F, 0.85F);

        // 文字始终朝向镜头，并缩放到和原版世界内名称标签接近的大小。
        Minecraft minecraft = Minecraft.getInstance();
        Component message = entity.structureErrorMessage();
        Font font = minecraft.font;
        poseStack.pushPose();
        poseStack.translate(relative.getX() + 0.5D, relative.getY() + 1.2D, relative.getZ() + 0.5D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        float width = font.width(message);
        font.drawInBatch(message, -width / 2.0F, 0.0F, 0xFFFFFFFF, false, poseStack.last().pose(),
            buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    /** 从方块缓冲区取得不透明流体渲染层，统一保证深度测试和方块图集格式正确。 */
    private static VertexConsumer consumerFor(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.solid());
    }

    /** 按官方浇注口的两个管腔绘制流体，并复用方块模型的水平旋转。 */
    private static void drawFaucetFluid(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite,
                                        Direction facing, int tint, int packedLight) {
        boolean rotated = facing.getAxis().isHorizontal() && facing != Direction.SOUTH;
        if (rotated) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * facing.get2DDataValue()));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
        if (facing.getAxis().isHorizontal()) {
            // 横向浇注口由后段储液腔和前段出液腔组成，坐标与官方模型数据一致。
            drawCuboid(poseStack.last(), consumer, sprite, 0.375F, 0.375F, 0.0F,
                0.625F, 0.5625F, 0.375F, tint, packedLight);
            drawCuboid(poseStack.last(), consumer, sprite, 0.375F, 0.0F, 0.375F,
                0.625F, 0.5625F, 0.5F, tint, packedLight);
        } else {
            // 上下浇注口使用竖直模型的完整管腔。
            drawCuboid(poseStack.last(), consumer, sprite, 0.375F, 0.0F, 0.375F,
                0.625F, 1.0F, 0.625F, tint, packedLight);
        }
        if (rotated) {
            poseStack.popPose();
        }
    }

    /** 绘制与流体计朝向一致的双面薄液面，只覆盖仪表盘内部区域。 */
    private static void drawGaugeFluid(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                       Direction facing, float ratio, int tint, int packedLight) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float low = 0.28F;
        float high = low + 0.44F * ratio;
        int red = (tint >> 16) & 255;
        int green = (tint >> 8) & 255;
        int blue = tint & 255;
        int alpha = (tint >>> 24) & 255;
        if (alpha == 0) alpha = 255;

        // 按上游六向模型的面位置绘制液面，并补绘反向面避免实体层背面剔除。
        switch (facing) {
            case NORTH -> drawDoubleSidedQuad(consumer, pose,
                0.70F, low, 0.985F, 0.30F, low, 0.985F, 0.30F, high, 0.985F, 0.70F, high, 0.985F,
                u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 0, -1);
            case SOUTH -> drawDoubleSidedQuad(consumer, pose,
                0.30F, low, 0.015F, 0.70F, low, 0.015F, 0.70F, high, 0.015F, 0.30F, high, 0.015F,
                u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 0, 1);
            case WEST -> drawDoubleSidedQuad(consumer, pose,
                0.985F, low, 0.30F, 0.985F, low, 0.70F, 0.985F, high, 0.70F, 0.985F, high, 0.30F,
                u0, v1, u1, v0, red, green, blue, alpha, packedLight, -1, 0, 0);
            case EAST -> drawDoubleSidedQuad(consumer, pose,
                0.015F, low, 0.70F, 0.015F, low, 0.30F, 0.015F, high, 0.30F, 0.015F, high, 0.70F,
                u0, v1, u1, v0, red, green, blue, alpha, packedLight, 1, 0, 0);
            case DOWN -> drawDoubleSidedQuad(consumer, pose,
                0.30F, 0.985F, low, 0.70F, 0.985F, low, 0.70F, 0.985F, high, 0.30F, 0.985F, high,
                u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, -1, 0);
            case UP -> drawDoubleSidedQuad(consumer, pose,
                0.30F, 0.015F, high, 0.70F, 0.015F, high, 0.70F, 0.015F, low, 0.30F, 0.015F, low,
                u0, v1, u1, v0, red, green, blue, alpha, packedLight, 0, 1, 0);
        }
    }

    /** 绘制正向和反向两组四点面片，保留实体层深度测试并消除单面空白。 */
    private static void drawDoubleSidedQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                            float ax, float ay, float az, float bx, float by, float bz,
                                            float cx, float cy, float cz, float dx, float dy, float dz,
                                            float u0, float v0, float u1, float v1,
                                            int red, int green, int blue, int alpha, int packedLight,
                                            float normalX, float normalY, float normalZ) {
        drawQuad(consumer, pose, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, u0, v0, u1, v1,
            red, green, blue, alpha, packedLight, normalX, normalY, normalZ);
        drawQuad(consumer, pose, ax, ay, az, dx, dy, dz, cx, cy, cz, bx, by, bz, u0, v1, u1, v0,
            red, green, blue, alpha, packedLight, -normalX, -normalY, -normalZ);
    }

    /** 绘制一张四个顶点明确的仪表盘液面。 */
    private static void drawQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 float ax, float ay, float az, float bx, float by, float bz,
                                 float cx, float cy, float cz, float dx, float dy, float dz,
                                 float u0, float v0, float u1, float v1,
                                 int red, int green, int blue, int alpha, int packedLight,
                                 float normalX, float normalY, float normalZ) {
        vertex(consumer, pose, ax, ay, az, u0, v0, red, green, blue, alpha, packedLight, normalX, normalY, normalZ);
        vertex(consumer, pose, bx, by, bz, u1, v0, red, green, blue, alpha, packedLight, normalX, normalY, normalZ);
        vertex(consumer, pose, cx, cy, cz, u1, v1, red, green, blue, alpha, packedLight, normalX, normalY, normalZ);
        vertex(consumer, pose, dx, dy, dz, u0, v1, red, green, blue, alpha, packedLight, normalX, normalY, normalZ);
    }

    /** 绘制包含顶面、底面和四个侧面的流体体积，避免只显示一张平面。 */
    private static void drawCuboid(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                   float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                   int tint, int packedLight) {
        drawCuboid(pose, consumer, sprite, minX, minY, minZ, maxX, maxY, maxZ, tint, packedLight, 63);
    }

    /** 大型液体分块仅绘制指定外表面，避免内面遮挡透明流体。 */
    private static void drawCuboid(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                   float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                   int tint, int packedLight, int faces) {
        int red = (tint >> 16) & 255;
        int green = (tint >> 8) & 255;
        int blue = tint & 255;
        int alpha = (tint >>> 24) & 255;
        if (alpha == 0) alpha = 255;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // 绘制顶面和底面。
        if ((faces & 1) != 0) {
        vertex(consumer, pose, minX, maxY, minZ, u0, v0, red, green, blue, alpha, packedLight, 0, 1, 0);
        vertex(consumer, pose, minX, maxY, maxZ, u0, v1, red, green, blue, alpha, packedLight, 0, 1, 0);
        vertex(consumer, pose, maxX, maxY, maxZ, u1, v1, red, green, blue, alpha, packedLight, 0, 1, 0);
        vertex(consumer, pose, maxX, maxY, minZ, u1, v0, red, green, blue, alpha, packedLight, 0, 1, 0);
        }
        if ((faces & 2) != 0) {
        vertex(consumer, pose, minX, minY, minZ, u0, v0, red, green, blue, alpha, packedLight, 0, -1, 0);
        vertex(consumer, pose, maxX, minY, minZ, u1, v0, red, green, blue, alpha, packedLight, 0, -1, 0);
        vertex(consumer, pose, maxX, minY, maxZ, u1, v1, red, green, blue, alpha, packedLight, 0, -1, 0);
        vertex(consumer, pose, minX, minY, maxZ, u0, v1, red, green, blue, alpha, packedLight, 0, -1, 0);
        }

        // 绘制四个侧面，使罐体从外部各个角度都能看到液体。
        if ((faces & 4) != 0) {
        vertex(consumer, pose, minX, minY, minZ, u0, v1, red, green, blue, alpha, packedLight, -1, 0, 0);
        vertex(consumer, pose, minX, minY, maxZ, u1, v1, red, green, blue, alpha, packedLight, -1, 0, 0);
        vertex(consumer, pose, minX, maxY, maxZ, u1, v0, red, green, blue, alpha, packedLight, -1, 0, 0);
        vertex(consumer, pose, minX, maxY, minZ, u0, v0, red, green, blue, alpha, packedLight, -1, 0, 0);
        }
        if ((faces & 8) != 0) {
        vertex(consumer, pose, maxX, minY, minZ, u0, v1, red, green, blue, alpha, packedLight, 1, 0, 0);
        vertex(consumer, pose, maxX, maxY, minZ, u0, v0, red, green, blue, alpha, packedLight, 1, 0, 0);
        vertex(consumer, pose, maxX, maxY, maxZ, u1, v0, red, green, blue, alpha, packedLight, 1, 0, 0);
        vertex(consumer, pose, maxX, minY, maxZ, u1, v1, red, green, blue, alpha, packedLight, 1, 0, 0);
        }
        if ((faces & 16) != 0) {
        vertex(consumer, pose, minX, minY, minZ, u0, v1, red, green, blue, alpha, packedLight, 0, 0, -1);
        vertex(consumer, pose, minX, maxY, minZ, u0, v0, red, green, blue, alpha, packedLight, 0, 0, -1);
        vertex(consumer, pose, maxX, maxY, minZ, u1, v0, red, green, blue, alpha, packedLight, 0, 0, -1);
        vertex(consumer, pose, maxX, minY, minZ, u1, v1, red, green, blue, alpha, packedLight, 0, 0, -1);
        }
        if ((faces & 32) != 0) {
        vertex(consumer, pose, minX, minY, maxZ, u0, v1, red, green, blue, alpha, packedLight, 0, 0, 1);
        vertex(consumer, pose, maxX, minY, maxZ, u1, v1, red, green, blue, alpha, packedLight, 0, 0, 1);
        vertex(consumer, pose, maxX, maxY, maxZ, u1, v0, red, green, blue, alpha, packedLight, 0, 0, 1);
        vertex(consumer, pose, minX, maxY, maxZ, u0, v0, red, green, blue, alpha, packedLight, 0, 0, 1);
        }
    }

    /** 写入带姿态、颜色、纹理和光照的单个顶点。 */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int red, int green, int blue, int alpha, int packedLight,
                               float normalX, float normalY, float normalZ) {
        VertexConsumer vertex = consumer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha).setUv(u, v);
        vertex.setLight(packedLight);
        vertex.setNormal(pose, normalX, normalY, normalZ);
    }

    /** 结构错误位置可能超出控制器自身方块范围，关闭方块实体裁剪。 */
    @Override
    public boolean shouldRenderOffScreen(FoundryBlockEntity entity) {
        return entity.isStructureController() && (entity.isStructureValid() || entity.structureErrorPos() != null);
    }
}
