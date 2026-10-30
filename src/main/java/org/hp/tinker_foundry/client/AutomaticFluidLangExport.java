package org.hp.tinker_foundry.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.registry.TFFluids;

/** 客户端指令：导出缺少翻译的自动生成流体与桶的 lang，名称由对应锭、粒、块等物品的译名推导。 */
@EventBusSubscriber(modid = TinkerFoundry.MOD_ID, value = Dist.CLIENT)
public final class AutomaticFluidLangExport {
    /** 导出的语言，英文与简体中文分别按各自规则拼接名称。 */
    private static final List<String> LANGUAGES = List.of("en_us", "zh_cn");
    /** 英文物品名中的形态前后缀，例如 Tin Ingot、Block of Tin、Raw Tin。 */
    private static final List<Pattern> ENGLISH_FORMS = List.of(
        Pattern.compile("^(.+?) (?:Ingot|Nugget|Gem|Block|Dust|Crystal|Shard)$"),
        Pattern.compile("^Block of (.+)$"),
        Pattern.compile("^Raw (.+)$"));
    /** 中文物品名中的形态后缀；宝石名本身就是材料名，不去掉“宝石”。 */
    private static final List<String> CHINESE_SUFFIXES = List.of("锭", "粒", "块", "粉", "碎片", "晶体");

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tinker_foundry_client")
            .then(Commands.literal("export_lang").executes(context -> export(context.getSource()))));
    }

    private static int export(CommandSourceStack source) {
        Minecraft minecraft = Minecraft.getInstance();
        Path folder = minecraft.gameDirectory.toPath().resolve("tinker_foundry").resolve("lang_export");
        // 每种语言单独加载，只看该语言自身是否有翻译，不受英文回退影响
        Map<String, ClientLanguage> languages = new LinkedHashMap<>();
        for (String code : LANGUAGES) languages.put(code, ClientLanguage.loadFrom(minecraft.getResourceManager(), List.of(code), false));
        ClientLanguage english = languages.get("en_us");
        List<String> materials = TFFluids.EXTRA_SOURCES.keySet().stream()
            .filter(name -> name.startsWith("molten/")).map(name -> name.substring(7)).sorted().toList();
        int total = 0;
        try {
            Files.createDirectories(folder);
            for (Map.Entry<String, ClientLanguage> entry : languages.entrySet()) {
                boolean chinese = entry.getKey().startsWith("zh_");
                ClientLanguage language = entry.getValue();
                JsonObject json = new JsonObject();
                for (String material : materials) {
                    String fluidKey = "fluid_type.tinker_foundry.molten." + material;
                    String bucketKey = "item.tinker_foundry.molten." + material + "_bucket";
                    if (language.has(fluidKey) && language.has(bucketKey)) continue;
                    String label = materialName(material, chinese ? language : english, chinese, english);
                    if (!language.has(fluidKey)) json.addProperty(fluidKey, chinese ? "熔融" + label : "Molten " + label);
                    if (!language.has(bucketKey)) json.addProperty(bucketKey, chinese ? "熔融" + label + "桶" : "Molten " + label + " Bucket");
                }
                // 文件带 .export 后缀，开发者去掉后缀即可放进资源包的 lang 目录
                Files.writeString(folder.resolve(entry.getKey() + ".json.export"),
                    new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(json), StandardCharsets.UTF_8);
                total += json.size();
            }
        } catch (IOException exception) {
            TinkerFoundry.LOGGER.error("Failed to export automatic fluid lang", exception);
            source.sendFailure(Component.literal("导出失败：" + exception.getMessage()));
            return 0;
        }
        int count = total;
        source.sendSuccess(() -> Component.literal("已导出 " + count + " 条缺失翻译到 " + folder.toAbsolutePath()), false);
        return count;
    }

    /** 从材料的代表物品译名推导材料名；中文找不到时退回英文推导，再退回注册名。 */
    private static String materialName(String material, ClientLanguage language, boolean chinese, ClientLanguage english) {
        for (String id : TFFluids.MATERIAL_TAGS.colorSourceItems(material)) {
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(null);
            if (item == null || !language.has(item.getDescriptionId())) continue;
            String name = language.getOrDefault(item.getDescriptionId(), "");
            String stripped = chinese ? stripChinese(name) : stripEnglish(name);
            if (stripped != null) return stripped;
        }
        if (chinese) return materialName(material, english, false, english);
        StringBuilder builder = new StringBuilder();
        for (String word : material.split("_")) {
            if (word.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return builder.toString();
    }

    private static String stripEnglish(String name) {
        for (Pattern pattern : ENGLISH_FORMS) {
            Matcher matcher = pattern.matcher(name.trim());
            if (matcher.matches()) return matcher.group(1);
        }
        return null;
    }

    private static String stripChinese(String name) {
        String trimmed = name.trim();
        for (String suffix : CHINESE_SUFFIXES) {
            if (trimmed.length() > suffix.length() && trimmed.endsWith(suffix)) return trimmed.substring(0, trimmed.length() - suffix.length());
        }
        // 粗矿（粗铁、粗锡）去掉前缀
        if (trimmed.length() > 1 && trimmed.startsWith("粗")) return trimmed.substring(1);
        return null;
    }

    private AutomaticFluidLangExport() { }
}
