package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** 26.1.2 提交式渲染使用的本地流体几何工具。 */
public final class FoundryFluidRenderer {
    /** 绘制完整的六面流体长方体。 */
    public static void renderCuboid(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                    float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                    int tint, int packedLight) {
        renderCuboid(pose, consumer, sprite, minX, minY, minZ, maxX, maxY, maxZ, tint, packedLight, 63);
    }

    /** 按顶、底、西、东、北、南的面掩码绘制流体长方体。 */
    public static void renderCuboid(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                    float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                    int tint, int packedLight, int faces) {
        int red = (tint >> 16) & 255;
        int green = (tint >> 8) & 255;
        int blue = tint & 255;
        int alpha = (tint >>> 24) & 255;
        if (alpha == 0) {
            alpha = 255;
        }
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        if ((faces & 1) != 0) {
            renderDoubleSidedQuad(consumer, pose, minX, maxY, minZ, minX, maxY, maxZ,
                maxX, maxY, maxZ, maxX, maxY, minZ, u0, v0, u1, v1,
                red, green, blue, alpha, packedLight, 0, 1, 0);
        }
        if ((faces & 2) != 0) {
            renderDoubleSidedQuad(consumer, pose, minX, minY, minZ, maxX, minY, minZ,
                maxX, minY, maxZ, minX, minY, maxZ, u0, v0, u1, v1,
                red, green, blue, alpha, packedLight, 0, -1, 0);
        }
        if ((faces & 4) != 0) {
            renderDoubleSidedQuad(consumer, pose, minX, minY, minZ, minX, minY, maxZ,
                minX, maxY, maxZ, minX, maxY, minZ, u0, v1, u1, v0,
                red, green, blue, alpha, packedLight, -1, 0, 0);
        }
        if ((faces & 8) != 0) {
            renderDoubleSidedQuad(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ,
                maxX, maxY, maxZ, maxX, minY, maxZ, u0, v1, u1, v0,
                red, green, blue, alpha, packedLight, 1, 0, 0);
        }
        if ((faces & 16) != 0) {
            renderDoubleSidedQuad(consumer, pose, minX, minY, minZ, minX, maxY, minZ,
                maxX, maxY, minZ, maxX, minY, minZ, u0, v1, u1, v0,
                red, green, blue, alpha, packedLight, 0, 0, -1);
        }
        if ((faces & 32) != 0) {
            renderDoubleSidedQuad(consumer, pose, minX, minY, maxZ, maxX, minY, maxZ,
                maxX, maxY, maxZ, minX, maxY, maxZ, u0, v1, u1, v0,
                red, green, blue, alpha, packedLight, 0, 0, 1);
        }
    }

    /** 同时绘制正面和反面，避免熔融流体因背面剔除消失。 */
    public static void renderDoubleSidedQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                              float ax, float ay, float az, float bx, float by, float bz,
                                              float cx, float cy, float cz, float dx, float dy, float dz,
                                              float u0, float v0, float u1, float v1,
                                              int red, int green, int blue, int alpha, int packedLight,
                                              float normalX, float normalY, float normalZ) {
        renderQuad(consumer, pose, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz,
            u0, v0, u1, v1, red, green, blue, alpha, packedLight, normalX, normalY, normalZ);
        renderQuad(consumer, pose, ax, ay, az, dx, dy, dz, cx, cy, cz, bx, by, bz,
            u0, v1, u1, v0, red, green, blue, alpha, packedLight, -normalX, -normalY, -normalZ);
    }

    /** 写入一个带纹理、颜色、光照和法线的面片。 */
    public static void renderQuad(VertexConsumer consumer, PoseStack.Pose pose,
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

    /** 写入流体网格的单个顶点。 */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int red, int green, int blue, int alpha, int packedLight,
                               float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    private FoundryFluidRenderer() {
    }
}
