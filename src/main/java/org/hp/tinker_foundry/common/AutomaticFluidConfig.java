package org.hp.tinker_foundry.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.fml.loading.FMLPaths;
import org.hp.tinker_foundry.TinkerFoundry;

public final class AutomaticFluidConfig {
    public record Definition(String material, int color, int temperature) { }
    public record Result(Map<String, Definition> definitions, List<String> diagnostics) { }

    public static Map<String, Definition> load() {
        Path directory = FMLPaths.CONFIGDIR.get().resolve("tinker_foundry").resolve("fluids");
        Result result = readDirectory(directory);
        result.diagnostics().forEach(message -> TinkerFoundry.LOGGER.warn("Invalid fluid config: {}", message));
        TinkerFoundry.LOGGER.info("Fluid config: definitions={}, directory={}", result.definitions().size(), directory);
        return result.definitions();
    }

    public static Result readDirectory(Path directory) {
        Map<String, Definition> definitions = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();
        try {
            boolean missing = !Files.exists(directory);
            Files.createDirectories(directory);
            if (missing) writeExample(directory);
            try (var paths = Files.walk(directory)) {
                List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> directory.relativize(path).toString().replace('\\', '/'))).toList();
                for (Path file : files) {
                    try {
                        Definition definition = parse(JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)));
                        if (definitions.putIfAbsent(definition.material(), definition) != null) {
                            diagnostics.add("Duplicate material '" + definition.material() + "' skipped in " + file);
                        }
                    } catch (IOException | RuntimeException exception) {
                        diagnostics.add(file + ": " + exception.getMessage());
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            diagnostics.add(directory + ": " + exception.getMessage());
        }
        return new Result(Collections.unmodifiableMap(definitions), List.copyOf(diagnostics));
    }

    public static Definition parse(JsonElement json) {
        if (json == null || !json.isJsonObject()) throw new IllegalArgumentException("Expected one JSON object per fluid file");
        JsonObject object = json.getAsJsonObject();
        String material = string(object, "material").trim();
        if (material.startsWith("tinker_foundry:molten/")) material = material.substring(22);
        if (!material.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid material ID: " + material);
        String colorText = string(object, "color");
        if (!colorText.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            throw new IllegalArgumentException("Color must use #RRGGBB or #AARRGGBB");
        }
        int color = (int) Long.parseLong(colorText.substring(1), 16);
        if (colorText.length() == 7) color |= 0xFF000000;
        JsonElement temperatureValue = object.get("temperature");
        if (temperatureValue == null || !temperatureValue.isJsonPrimitive() || !temperatureValue.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Temperature must be an integer in kelvin");
        }
        int temperature;
        try {
            temperature = temperatureValue.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Temperature must be a valid 32-bit integer in kelvin");
        }
        if (temperature < 0) throw new IllegalArgumentException("Temperature cannot be below zero kelvin");
        return new Definition(material, color, temperature);
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing or invalid string field: " + name);
        }
        return value.getAsString();
    }

    private static void writeExample(Path directory) throws IOException {
        Files.writeString(directory.resolve("example.json.disabled"), """
            {
              "material": "tin",
              "color": "#FFD5DDE0",
              "temperature": 1100
            }
            """, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        Files.writeString(directory.resolve("说明.txt"), """
            将 example.json.disabled 复制为任意 .json 文件后编辑，支持子目录。
            每个 JSON 必须提供 material、color、temperature 三个字段。
            material 填材料 ID（tin）或完整本模组流体 ID（tinker_foundry:molten/tin）。
            color 支持 #RRGGBB 或 #AARRGGBB，temperature 是非负的开尔文整数。
            名称使用资源包 lang：
            fluid_type.tinker_foundry.molten.tin
            item.tinker_foundry.molten.tin_bucket
            双端配置必须一致，修改后完全重启，不能通过 /reload 注册新流体。
            文件按相对路径排序，同材料第一份有效配置生效；坏文件跳过并记录日志。
            已有带标准标签的外部流体优先复用，其颜色和温度不会被本配置修改。
            """, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private AutomaticFluidConfig() { }
}
