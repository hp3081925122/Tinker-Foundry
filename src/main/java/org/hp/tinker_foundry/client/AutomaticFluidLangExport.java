package org.hp.tinker_foundry.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.common.AutomaticFluidNames;
import org.hp.tinker_foundry.registry.TFFluids;

/** 客户端指令：导出缺少翻译的自动生成流体与桶的 lang，名称与游戏内回退显示一致。 */
@EventBusSubscriber(modid = TinkerFoundry.MOD_ID, value = Dist.CLIENT)
public final class AutomaticFluidLangExport {
    /** 默认导出的语言，另外加上当前选择的语言；名称模板取各语言的 tinker_foundry.automatic.* 键。 */
    private static final List<String> LANGUAGES = List.of("en_us", "zh_cn");

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
        java.util.Set<String> codes = new java.util.LinkedHashSet<>(LANGUAGES);
        codes.add(minecraft.getLanguageManager().getSelected());
        for (String code : codes) languages.put(code, ClientLanguage.loadFrom(minecraft.getResourceManager(), List.of(code), false));
        ClientLanguage english = languages.get("en_us");
        List<String> materials = TFFluids.EXTRA_SOURCES.keySet().stream()
            .filter(name -> name.startsWith("molten/")).map(name -> name.substring(7)).sorted().toList();
        int total = 0;
        try {
            Files.createDirectories(folder);
            for (Map.Entry<String, ClientLanguage> entry : languages.entrySet()) {
                ClientLanguage language = entry.getValue();
                JsonObject json = new JsonObject();
                for (String material : materials) {
                    String fluidKey = "fluid_type.tinker_foundry.molten." + material;
                    String bucketKey = "item.tinker_foundry.molten." + material + "_bucket";
                    if (language.has(fluidKey) && language.has(bucketKey)) continue;
                    // 已有流体译名时桶名沿用它，保证“流体名 + 桶”一致
                    String fluidName = language.has(fluidKey) ? language.getOrDefault(fluidKey, "")
                        : AutomaticFluidNames.fluidText(AutomaticFluidNames.derive(material, language, english), language, english);
                    if (!language.has(fluidKey)) json.addProperty(fluidKey, fluidName);
                    if (!language.has(bucketKey)) json.addProperty(bucketKey, AutomaticFluidNames.bucketText(fluidName, language, english));
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

    private AutomaticFluidLangExport() { }
}
