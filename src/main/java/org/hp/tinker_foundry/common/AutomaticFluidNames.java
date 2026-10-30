package org.hp.tinker_foundry.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.hp.tinker_foundry.registry.TFFluids;

/** 自动流体名称：lang 缺失时按材料对应物品的译名和通用 lang 模板拼出流体名与桶名。 */
public final class AutomaticFluidNames {
    /** 流体名模板，%s 为材料名，例如“熔融%s”“Molten %s”。 */
    public static final String FLUID_KEY = "tinker_foundry.automatic.fluid";
    /** 桶名模板，%s 为流体名，例如“%s桶”“%s Bucket”。 */
    public static final String BUCKET_KEY = "tinker_foundry.automatic.bucket";
    /** 物品形态模板，用 | 分隔，%s 为材料名，例如“%s锭|%s粒|粗%s”。 */
    public static final String FORMS_KEY = "tinker_foundry.automatic.item_forms";
    /** 语言文件缺少模板时使用的英文默认值。 */
    private static final String DEFAULT_FLUID = "Molten %s";
    private static final String DEFAULT_BUCKET = "%s Bucket";
    private static final String DEFAULT_FORMS = "%s Ingot|%s Nugget|%s Gem|%s Block|%s Dust|%s Crystal|%s Shard|Block of %s|Raw %s";
    /** 英文默认形态正则；运行时语言实例已合并英文回退，物品只有英文名时靠它兜底。 */
    private static final List<Pattern> DEFAULT_PATTERNS = compile(DEFAULT_FORMS);
    /** 按当前语言实例缓存推导结果，切换语言后整体失效。 */
    private static final Map<String, String> CACHE = new HashMap<>();
    private static Language cachedLanguage;

    /** 流体显示名：有 lang 用 lang，否则套用流体模板。 */
    public static Component fluid(String material, String key) {
        Language language = Language.getInstance();
        if (language.has(key)) return Component.translatable(key);
        return Component.translatableWithFallback(FLUID_KEY, DEFAULT_FLUID, label(material, language));
    }

    /** 桶显示名：有 lang 用 lang，否则把流体名套进桶模板。 */
    public static Component bucket(String material, String key) {
        Language language = Language.getInstance();
        if (language.has(key)) return Component.translatable(key);
        return Component.translatableWithFallback(BUCKET_KEY, DEFAULT_BUCKET, fluid(material, "fluid_type.tinker_foundry.molten." + material));
    }

    /** 用指定语言的模板拼出纯文本流体名，供导出使用。 */
    public static String fluidText(String label, Language language, Language english) {
        return template(FLUID_KEY, DEFAULT_FLUID, language, english).replace("%s", label);
    }

    /** 用指定语言的模板拼出纯文本桶名，供导出使用。 */
    public static String bucketText(String fluidName, Language language, Language english) {
        return template(BUCKET_KEY, DEFAULT_BUCKET, language, english).replace("%s", fluidName);
    }

    private static synchronized String label(String material, Language language) {
        if (language != cachedLanguage) {
            CACHE.clear();
            cachedLanguage = language;
        }
        return CACHE.computeIfAbsent(material, key -> derive(key, language, language));
    }

    /** 从材料的代表物品译名推导材料名：先用本语言形态模板，再用英文模板，最后退回注册名。 */
    public static String derive(String material, Language language, Language english) {
        List<Pattern> forms = forms(language, english);
        List<Pattern> englishForms = forms(english, english);
        for (Language source : language == english ? List.of(language) : List.of(language, english)) {
            for (String id : TFFluids.MATERIAL_TAGS.colorSourceItems(material)) {
                Item item = java.util.Optional.ofNullable(ResourceLocation.tryParse(id)).flatMap(BuiltInRegistries.ITEM::getOptional).orElse(null);
                if (item == null || !source.has(item.getDescriptionId())) continue;
                String name = source.getOrDefault(item.getDescriptionId(), "").trim();
                // 本语言物品可能回退成英文名，本语言模板不匹配时再试英文模板
                String stripped = strip(name, forms);
                if (stripped == null) stripped = strip(name, englishForms);
                if (stripped == null) stripped = strip(name, DEFAULT_PATTERNS);
                if (stripped != null) return stripped;
            }
        }
        StringBuilder builder = new StringBuilder();
        for (String word : material.split("_")) {
            if (word.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return builder.toString();
    }

    private static String template(String key, String fallback, Language language, Language english) {
        if (language.has(key)) return language.getOrDefault(key, fallback);
        return english.getOrDefault(key, fallback);
    }

    /** 把“%s锭|粗%s”这类模板转成正则，%s 处捕获材料名。 */
    private static List<Pattern> forms(Language language, Language english) {
        return compile(template(FORMS_KEY, DEFAULT_FORMS, language, english));
    }

    private static List<Pattern> compile(String template) {
        List<Pattern> patterns = new ArrayList<>();
        for (String form : template.split("\\|")) {
            int index = form.indexOf("%s");
            if (index < 0) continue;
            String before = form.substring(0, index);
            String after = form.substring(index + 2);
            patterns.add(Pattern.compile("^" + Pattern.quote(before) + "(.+?)" + Pattern.quote(after) + "$"));
        }
        return patterns;
    }

    private static String strip(String name, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches() && !matcher.group(1).isBlank()) return matcher.group(1).trim();
        }
        return null;
    }

    private AutomaticFluidNames() { }
}
