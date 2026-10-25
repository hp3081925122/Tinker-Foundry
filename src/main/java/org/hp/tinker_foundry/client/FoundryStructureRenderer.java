package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
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
        if (width <= 0 || depth <= 0 || height <= 0 || fluids.isEmpty()) {
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

    /** 避免提交回调捕获可变方块坐标对象。 */
    private record BlockOrigin(int x, int y, int z) {
    }

    private FoundryStructureRenderer() {
    }
}
