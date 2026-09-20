package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.hp.tinker_foundry.block.FoundryControllerBlock;
import org.hp.tinker_foundry.block.entity.FoundryBlockEntity;
import net.minecraft.world.phys.AABB;

/** 冶炼炉和铸造炉控制器的炉腔流体、内部物品和结构错误专用渲染器。 */
public final class FoundryStructureRenderer {
    /** 绘制有效结构内部按层排列的流体和物品。 */
    public static void renderContents(FoundryBlockEntity entity, PoseStack poseStack,
                                      MultiBufferSource buffer, int light) {
        AABB bounds = entity.interiorBounds();
        if (bounds == null || entity.getLevel() == null) {
            return;
        }
        BlockPos origin = entity.getBlockPos();
        int width = (int) (bounds.maxX - bounds.minX);
        int depth = (int) (bounds.maxZ - bounds.minZ);
        float height = (float) (bounds.maxY - bounds.minY);
        if (width <= 0 || depth <= 0 || height <= 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(bounds.minX - origin.getX(), bounds.minY - origin.getY(), bounds.minZ - origin.getZ());
        java.util.List<FluidStack> fluids = entity.structureFluidLayers();
        int amount = fluids.stream().mapToInt(FluidStack::getAmount).sum();
        int scale = Math.max(1, Math.max(entity.structureCapacity(), amount));
        float[] heights = new float[fluids.size()];
        float total = 0.0F;
        for (int index = 0; index < heights.length; index++) {
            heights[index] = Math.max(0.1F, fluids.get(index).getAmount() * (height - 0.01F) / scale);
            total += heights[index];
        }
        // 极小流体也有可见厚度；层数过多时压缩厚度，绝不超出炉腔。
        float compression = total > height - 0.01F ? (height - 0.01F) / total : 1.0F;
        float y = 0.005F;
        for (int index = 0; index < fluids.size(); index++) {
            FluidStack fluid = fluids.get(index);
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extensions.getStillTexture(fluid));
            float next = y + heights[index] * compression;
            int brightness = Math.max(light & 65535, fluid.getFluidType().getLightLevel(fluid) << 4)
                | light & 0xFFFF0000;
            VertexConsumer consumer = FoundryFluidRenderer.translucentConsumer(buffer);
            // 按方块切分纹理，只绘制炉腔外表面，避免内部生成重复面片。
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    for (float bottom = y; bottom < next;) {
                        float top = Math.min(next, (float) Math.floor(bottom) + 1.0F);
                        int faces = (top == next ? 1 : 0) | (bottom == y ? 2 : 0)
                            | (x == 0 ? 4 : 0) | (x == width - 1 ? 8 : 0)
                            | (z == 0 ? 16 : 0) | (z == depth - 1 ? 32 : 0);
                        if (faces != 0) {
                            FoundryFluidRenderer.renderCuboid(poseStack.last(), consumer, sprite,
                                x == 0 ? 0.005F : x, bottom, z == 0 ? 0.005F : z,
                                x == width - 1 ? width - 0.005F : x + 1.0F, top,
                                z == depth - 1 ? depth - 0.005F : z + 1.0F,
                                extensions.getTintColor(fluid), brightness, faces);
                        }
                        bottom = top;
                    }
                }
            }
            y = next;
        }

        // 物品变换保持匠魂 Mantle RenderItem 的中心、缩放、朝向和四边形预算语义。
        int quads = 0;
        var renderer = Minecraft.getInstance().getItemRenderer();
        for (int slot = 0; slot < entity.inputSlotCount() && quads <= 3500; slot++) {
            var item = entity.getInput(slot);
            if (item.isEmpty()) {
                continue;
            }
            int itemY = slot / (width * depth);
            int itemX = slot % width;
            int itemZ = slot / width % depth;
            poseStack.pushPose();
            poseStack.translate(itemX + 0.5F, itemY + 0.5F, itemZ + 0.5F);
            if (entity.getBlockState().hasProperty(FoundryControllerBlock.FACING)) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * entity.getBlockState()
                    .getValue(FoundryControllerBlock.FACING).get2DDataValue()));
            }
            FoundryRenderItem.renderAt(item, poseStack, buffer, light, 15.0F / 16.0F,
                0.0F, 0.0F, ItemDisplayContext.FIXED);
            poseStack.popPose();
            var model = renderer.getModel(item, entity.getLevel(), null, 0);
            if (model.isCustomRenderer()) {
                quads += 100;
            } else {
                for (Direction face : Direction.values()) {
                    quads += model.getQuads(null, face, entity.getLevel().getRandom(), ModelData.EMPTY, null).size();
                }
                quads += model.getQuads(null, null, entity.getLevel().getRandom(), ModelData.EMPTY, null).size();
            }
        }
        poseStack.popPose();
    }

    /** 绘制匠魂风格的结构错误红框和原因文字。 */
    public static void renderError(FoundryBlockEntity entity, PoseStack poseStack,
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

    /** 防止把结构渲染器误当作可实例化状态对象。 */
    private FoundryStructureRenderer() {
    }
}
