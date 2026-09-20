package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * 本地 Mantle 流体渲染等价层。
 *
 * <p>这里仅负责把带纹理的流体长方体和面片写入 Minecraft 顶点缓冲区，设备位置、容量和朝向由各专用渲染器决定。</p>
 */
public final class FoundryFluidRenderer {
    /** 创建不透明流体缓冲区，保证流体不会被方块内部错误裁掉。 */
    public static VertexConsumer solidConsumer(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.solid());
    }

    /** 创建结构内部使用的半透明流体缓冲区。 */
    public static VertexConsumer translucentConsumer(MultiBufferSource buffer) {
        return buffer.getBuffer(RenderType.translucent());
    }

    /** 绘制完整的六面流体长方体。 */
    public static void renderCuboid(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                    float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                    int tint, int packedLight) {
        renderCuboid(pose, consumer, sprite, minX, minY, minZ, maxX, maxY, maxZ, tint, packedLight, 63);
    }

    /**
     * 按面掩码绘制流体长方体。
     *
     * <p>掩码顺序与 Mantle FluidCuboid 等价：顶、底、西、东、北、南分别是 1、2、4、8、16、32。</p>
     */
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

        // 顶面和底面只在外表面掩码中存在时绘制。
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

        // 四个侧面使用双面面片，避免视角偏移或背面剔除造成液体消失。
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

    /** 绘制正向和反向两组四点面片，保留实体层深度测试。 */
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

    /** 绘制单个带纹理坐标和法线的四点面片。 */
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

    /** 写入带姿态、颜色、纹理和光照的单个顶点。 */
    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                              float u, float v, int red, int green, int blue, int alpha, int packedLight,
                              float normalX, float normalY, float normalZ) {
        VertexConsumer vertex = consumer.addVertex(pose, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v);
        vertex.setLight(packedLight);
        vertex.setNormal(pose, normalX, normalY, normalZ);
    }

    /** 防止把本地流体渲染工具误当作可实例化状态对象。 */
    private FoundryFluidRenderer() {
    }
}
