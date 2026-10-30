package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import org.hp.tinker_foundry.block.FoundryControllerBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;

import java.util.List;

/** 26.1.2 冶炼炉腔体的提交式流体渲染器。 */
public final class FoundryStructureRenderer {
    /** 把有效多方块结构内的分层流体提交到新版自定义几何队列。 */
    public static void submitContents(FoundryBlockEntity entity, PoseStack poseStack,
                                      SubmitNodeCollector collector, int packedLight) {
        AABB bounds = entity.interiorBounds();
        if (bounds == null || entity.getLevel() == null) {
            return;
        }
        int width = (int) (bounds.maxX - bounds.minX);
        int depth = (int) (bounds.maxZ - bounds.minZ);
        float height = (float) (bounds.maxY - bounds.minY);
        List<FluidStack> fluids = entity.structureFluidLayers();
        if (width <= 0 || depth <= 0 || height <= 0) {
            return;
        }
        submitItems(entity, bounds, width, depth, poseStack, collector, packedLight);
        if (fluids.isEmpty()) {
            return;
        }
        int amount = fluids.stream().mapToInt(FluidStack::getAmount).sum();
        int scale = Math.max(1, Math.max(entity.structureCapacity(), amount));
        BlockOrigin origin = new BlockOrigin(entity.getBlockPos().getX(), entity.getBlockPos().getY(), entity.getBlockPos().getZ());
        collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (entry, vertices) -> {
            // 提交回调需要使用快照姿态，炉腔坐标仍按控制器方块原点进行平移。
            PoseStack localPose = new PoseStack();
            localPose.last().set(entry);
            localPose.translate((float) bounds.minX - origin.x, (float) bounds.minY - origin.y,
                (float) bounds.minZ - origin.z);
            float[] layerHeights = new float[fluids.size()];
            float total = 0.0F;
            for (int index = 0; index < layerHeights.length; index++) {
                layerHeights[index] = Math.max(0.1F, fluids.get(index).getAmount() * (height - 0.01F) / scale);
                total += layerHeights[index];
            }
            float compression = total > height - 0.01F ? (height - 0.01F) / total : 1.0F;
            float y = 0.005F;
            for (int index = 0; index < fluids.size(); index++) {
                FluidStack fluid = fluids.get(index);
                TextureAtlasSprite sprite = FoundryFluidClient.stillSprite(fluid);
                float next = y + layerHeights[index] * compression;
                int brightness = Math.max(packedLight & 65535, fluid.getFluid().getFluidType().getLightLevel(fluid) << 4)
                    | packedLight & 0xFFFF0000;
                int tint = FoundryFluidClient.tint(fluid);
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < depth; z++) {
                        for (float bottom = y; bottom < next;) {
                            float top = Math.min(next, (float) Math.floor(bottom) + 1.0F);
                            int faces = (top == next ? 1 : 0) | (bottom == y ? 2 : 0)
                                | (x == 0 ? 4 : 0) | (x == width - 1 ? 8 : 0)
                                | (z == 0 ? 16 : 0) | (z == depth - 1 ? 32 : 0);
                            if (faces != 0) {
                                FoundryFluidRenderer.renderCuboid(localPose.last(), vertices, sprite,
                                    x == 0 ? 0.005F : x, bottom, z == 0 ? 0.005F : z,
                                    x == width - 1 ? width - 0.005F : x + 1.0F, top,
                                    z == depth - 1 ? depth - 0.005F : z + 1.0F,
                                    tint, brightness, faces);
                            }
                            bottom = top;
                        }
                    }
                }
                y = next;
            }
        });
    }

    /** 按匠魂 Mantle RenderItem 的中心、缩放、朝向和四边形预算提交炉腔内的输入物品。 */
    private static void submitItems(FoundryBlockEntity entity, AABB bounds, int width, int depth,
                                    PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
        BlockPos origin = entity.getBlockPos();
        float rotation = entity.getBlockState().hasProperty(FoundryControllerBlock.FACING)
            ? -90.0F * entity.getBlockState().getValue(FoundryControllerBlock.FACING).get2DDataValue() : 0.0F;
        int seed = (int) origin.asLong();
        int quads = 0;
        poseStack.pushPose();
        poseStack.translate(bounds.minX - origin.getX(), bounds.minY - origin.getY(), bounds.minZ - origin.getZ());
        for (int slot = 0; slot < entity.inputSlotCount() && quads <= 3500; slot++) {
            ItemStack item = entity.getInput(slot);
            if (item.isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(slot % width + 0.5F, slot / (width * depth) + 0.5F, slot / width % depth + 0.5F);
            if (rotation != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            }
            quads += FoundryRenderItem.submitAt(item, entity.getLevel(), seed + slot, poseStack, collector, packedLight,
                15.0F / 16.0F, 0.0F, 0.0F, ItemDisplayContext.FIXED);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** 绘制匠魂风格的结构错误红框和朝向镜头的原因文字。 */
    public static void submitError(FoundryBlockEntity entity, PoseStack poseStack, SubmitNodeCollector collector,
                                   CameraRenderState camera, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!entity.shouldShowStructureError() || minecraft.player == null) {
            return;
        }
        BlockPos controllerPos = entity.getBlockPos();
        BlockPos errorPos = entity.structureErrorPos();
        int dx = minecraft.player.blockPosition().getX() - controllerPos.getX();
        int dz = minecraft.player.blockPosition().getZ() - controllerPos.getZ();
        if (dx * dx + dz * dz >= 512) {
            return;
        }
        BlockPos relative = errorPos.subtract(controllerPos);
        float lineWidth = minecraft.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (entry, vertices) -> {
            PoseStack localPose = new PoseStack();
            localPose.last().set(entry);
            ShapeRenderer.renderShape(localPose, vertices, Shapes.block(),
                relative.getX(), relative.getY(), relative.getZ(), 0xD9FF0000, lineWidth);
        });

        Component message = entity.structureErrorMessage();
        Font font = minecraft.font;
        poseStack.pushPose();
        poseStack.translate(relative.getX() + 0.5D, relative.getY() + 1.2D, relative.getZ() + 0.5D);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.025F, -0.025F, 0.025F);
        collector.submitText(poseStack, -font.width(message) / 2.0F, 0.0F, message.getVisualOrderText(), false,
            Font.DisplayMode.NORMAL, packedLight, 0xFFFFFFFF, 0, 0);
        poseStack.popPose();
    }

    /** 避免提交回调捕获可变方块坐标对象。 */
    private record BlockOrigin(int x, int y, int z) {
    }

    private FoundryStructureRenderer() {
    }
}
