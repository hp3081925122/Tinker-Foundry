package org.hp.tinker_foundry.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFFluids;

/** 自动熔融流体的客户端取色：读取材料代表物品的材质，取占比最多的颜色区间作为流体颜色。 */
public final class AutomaticFluidColors {
    /** 已计算的材料颜色；取色失败记为 0 并使用注册时的默认颜色。 */
    private static final Map<String, Integer> CACHE = new ConcurrentHashMap<>();

    /** 返回流体颜色：显式指定颜色的材料和非自动流体保持原色，其余按材质取色。 */
    public static int tint(String fluidName, int fallback) {
        if (!fluidName.startsWith("molten/")) {
            return fallback;
        }
        String material = fluidName.substring(7);
        if (TFFluids.MATERIAL_TAGS.hasExplicitColor(material)) {
            return fallback;
        }
        Integer cached = CACHE.get(material);
        if (cached == null) {
            int extracted = extract(material);
            // 资源加载期间模型和材质可能还未就绪，此时不缓存失败结果
            if (extracted == 0 && Minecraft.getInstance().getOverlay() != null) {
                return fallback;
            }
            CACHE.put(material, extracted);
            cached = extracted;
        }
        return cached == 0 ? fallback : cached;
    }

    /** 资源重载后清空缓存，资源包更换材质时颜色随之更新。 */
    public static void clear() {
        CACHE.clear();
    }

    /** 依次尝试材料的代表物品，返回第一个能取到颜色的结果。 */
    private static int extract(String material) {
        Minecraft minecraft = Minecraft.getInstance();
        for (String id : TFFluids.MATERIAL_TAGS.colorSourceItems(material)) {
            Identifier itemId = Identifier.tryParse(id);
            Item item = itemId == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(itemId);
            if (item == Items.AIR) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            // 26.1.2 通过物品模型解析器取得粒子材质和第 0 层的染色结果
            ItemStackRenderState renderState = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(renderState, stack, ItemDisplayContext.GUI, null, null, 0);
            Material.Baked particle = renderState.pickParticleMaterial(RandomSource.create(0L));
            if (particle == null) {
                continue;
            }
            Identifier spriteName = particle.sprite().contents().name();
            if (spriteName.equals(MissingTextureAtlasSprite.getLocation())) {
                continue;
            }
            int tint = -1;
            if (renderState.activeLayerCount > 0) {
                IntList tints = renderState.layers[0].tintLayers();
                tint = tints.isEmpty() ? -1 : tints.getInt(0);
            }
            Identifier texture = spriteName.withPath(path -> "textures/" + path + ".png");
            Optional<Resource> resource = minecraft.getResourceManager().getResource(texture);
            if (resource.isEmpty()) {
                continue;
            }
            try (InputStream input = resource.get().open(); NativeImage image = NativeImage.read(input)) {
                // 动画材质纵向排列多帧，只取第一帧
                int size = Math.min(image.getWidth(), image.getHeight());
                int[] pixels = new int[size * size];
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        pixels[y * size + x] = image.getPixel(x, y);
                    }
                }
                int color = dominantColor(pixels, tint);
                if (color != 0) {
                    TinkerFoundry.LOGGER.debug("[fluid-color] material={} item={} texture={} color={}",
                        material, id, texture, String.format("0x%08X", color));
                    return color;
                }
            } catch (IOException | RuntimeException exception) {
                TinkerFoundry.LOGGER.debug("[fluid-color] Failed to read texture {} for material {}: {}", texture, material, exception.toString());
            }
        }
        TinkerFoundry.LOGGER.debug("[fluid-color] No usable texture for material {}, keeping default color", material);
        return 0;
    }

    /** 去掉透明、描边阴影和高光像素后，按色相与亮度分桶，取像素最多的桶求平均并适当提亮。 */
    static int dominantColor(int[] argb, int tint) {
        boolean tinted = tint != -1 && (tint & 0xFFFFFF) != 0xFFFFFF;
        int[] rgb = new int[argb.length];
        float[] luminance = new float[argb.length];
        int count = 0;
        for (int pixel : argb) {
            if ((pixel >>> 24) < 128) {
                continue;
            }
            int red = pixel >> 16 & 0xFF;
            int green = pixel >> 8 & 0xFF;
            int blue = pixel & 0xFF;
            // 灰度通用材质由物品染色器上色，取色前先乘上第 0 层染色
            if (tinted) {
                red = red * (tint >> 16 & 0xFF) / 255;
                green = green * (tint >> 8 & 0xFF) / 255;
                blue = blue * (tint & 0xFF) / 255;
            }
            rgb[count] = red << 16 | green << 8 | blue;
            luminance[count] = 0.299F * red + 0.587F * green + 0.114F * blue;
            count++;
        }
        if (count == 0) {
            return 0;
        }
        float[] sorted = Arrays.copyOf(luminance, count);
        Arrays.sort(sorted);
        float low = sorted[(int) (count * 0.15F)];
        float high = sorted[Math.min(count - 1, (int) (count * 0.9F))];
        int kept = 0;
        for (int index = 0; index < count; index++) {
            if (luminance[index] >= low && luminance[index] <= high) {
                kept++;
            }
        }
        // 像素太少时（例如纯色小图）不做亮度裁剪
        boolean trim = kept >= Math.max(4, count / 4);
        int[] bucketCount = new int[13 * 4];
        long[] bucketRed = new long[13 * 4];
        long[] bucketGreen = new long[13 * 4];
        long[] bucketBlue = new long[13 * 4];
        float[] hsb = new float[3];
        for (int index = 0; index < count; index++) {
            if (trim && (luminance[index] < low || luminance[index] > high)) {
                continue;
            }
            int red = rgb[index] >> 16 & 0xFF;
            int green = rgb[index] >> 8 & 0xFF;
            int blue = rgb[index] & 0xFF;
            toHsb(red, green, blue, hsb);
            int hue = hsb[1] < 0.18F ? 12 : (int) (hsb[0] * 12.0F) % 12;
            int bucket = hue * 4 + Math.min(3, (int) (hsb[2] * 4.0F));
            bucketCount[bucket]++;
            bucketRed[bucket] += red;
            bucketGreen[bucket] += green;
            bucketBlue[bucket] += blue;
        }
        int best = 0;
        for (int bucket = 1; bucket < bucketCount.length; bucket++) {
            if (bucketCount[bucket] > bucketCount[best]) {
                best = bucket;
            }
        }
        if (bucketCount[best] == 0) {
            return 0;
        }
        int red = (int) (bucketRed[best] / bucketCount[best]);
        int green = (int) (bucketGreen[best] / bucketCount[best]);
        int blue = (int) (bucketBlue[best] / bucketCount[best]);
        // 熔融纹理本身是灰度，乘色后偏暗，这里略微提高亮度和饱和度
        toHsb(red, green, blue, hsb);
        return 0xFF000000 | fromHsb(hsb[0], Math.min(1.0F, hsb[1] * 1.1F), Math.min(1.0F, hsb[2] * 1.1F + 0.06F));
    }

    /** RGB 转 HSB，结果写入 output。 */
    private static void toHsb(int red, int green, int blue, float[] output) {
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        float delta = max - min;
        float hue = 0.0F;
        if (delta > 0) {
            if (max == red) {
                hue = ((green - blue) / delta) / 6.0F;
            } else if (max == green) {
                hue = ((blue - red) / delta + 2.0F) / 6.0F;
            } else {
                hue = ((red - green) / delta + 4.0F) / 6.0F;
            }
            if (hue < 0.0F) {
                hue += 1.0F;
            }
        }
        output[0] = hue;
        output[1] = max == 0 ? 0.0F : delta / max;
        output[2] = max / 255.0F;
    }

    /** HSB 转 RGB。 */
    private static int fromHsb(float hue, float saturation, float brightness) {
        float sector = (hue - (float) Math.floor(hue)) * 6.0F;
        float fraction = sector - (float) Math.floor(sector);
        float p = brightness * (1.0F - saturation);
        float q = brightness * (1.0F - saturation * fraction);
        float t = brightness * (1.0F - saturation * (1.0F - fraction));
        float red;
        float green;
        float blue;
        switch ((int) sector) {
            case 0 -> { red = brightness; green = t; blue = p; }
            case 1 -> { red = q; green = brightness; blue = p; }
            case 2 -> { red = p; green = brightness; blue = t; }
            case 3 -> { red = p; green = q; blue = brightness; }
            case 4 -> { red = t; green = p; blue = brightness; }
            default -> { red = brightness; green = p; blue = q; }
        }
        return (int) (red * 255.0F + 0.5F) << 16 | (int) (green * 255.0F + 0.5F) << 8 | (int) (blue * 255.0F + 0.5F);
    }

    private AutomaticFluidColors() {
    }
}
